/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.connectedcar.internal.api.carnet;

import static org.openhab.binding.connectedcar.internal.BindingConstants.*;
import static org.openhab.binding.connectedcar.internal.api.carnet.CarNetApiGSonDTO.CNAPI_SERVICE_CAR_FINDER;
import static org.openhab.binding.connectedcar.internal.util.Helpers.getString;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.connectedcar.internal.api.ApiBase;
import org.openhab.binding.connectedcar.internal.api.ApiBaseService;
import org.openhab.binding.connectedcar.internal.api.ApiDataTypesDTO.GeoPosition;
import org.openhab.binding.connectedcar.internal.api.ApiException;
import org.openhab.binding.connectedcar.internal.handler.CarNetVehicleHandler;
import org.openhab.binding.connectedcar.internal.provider.ChannelDefinitions.ChannelIdMapEntry;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link CarNetServiceCarFinder} implements the carFinder service.
 *
 * @author Markus Michels - Initial contribution
 * @author Thomas Knaller - Maintainer
 * @author Dr. Yves Kreis - Maintainer
 */
@NonNullByDefault
public class CarNetServiceCarFinder extends ApiBaseService {
    private final Logger logger = LoggerFactory.getLogger(CarNetServiceCarFinder.class);

    public CarNetServiceCarFinder(CarNetVehicleHandler thingHandler, ApiBase api) {
        super(CNAPI_SERVICE_CAR_FINDER, thingHandler, api);
    }

    @Override
    public boolean createChannels(Map<String, ChannelIdMapEntry> ch) throws ApiException {
        addChannels(ch, CHANNEL_GROUP_LOCATION, true, CHANNEL_LOCATION_GEO, CHANNEL_LOCATION_TIME,
                CHANNEL_LOCATION_ADDRESS, CHANNEL_PARK_LOCATION, CHANNEL_PARK_ADDRESS, CHANNEL_PARK_TIME,
                CHANNEL_CAR_MOVING);
        return true;
    }

    @Override
    public boolean serviceUpdate() throws ApiException {
        boolean updated = false;
        try {
            logger.debug("{}: Get Vehicle Position", thingId);
            GeoPosition position = api.getVehiclePosition();
            logger.trace("{}: Get Vehicle Position: {}", thingId, position);
            updated |= updateChannel(CHANNEL_LOCATION_GEO, position.asPointType());

            logger.trace("{}: Get Car Sent Time", thingId);
            String time = position.getCarSentTime();
            logger.trace("{}: Update Car Sent Time: {}", thingId, time);
            updated |= updateChannel(CHANNEL_LOCATION_TIME, new DateTimeType(time));
            logger.trace("{}: Update Location Address: {}", thingId, position.asPointType());
            updated |= updateLocationAddress(position.asPointType(), CHANNEL_LOCATION_ADDRESS);

            logger.debug("{}: Get Stored Position", thingId);
            position = api.getStoredPosition();
            logger.trace("{}: Update Park Location: {}", thingId, position);
            updated |= updateChannel(CHANNEL_PARK_LOCATION, position.asPointType());
            logger.trace("{}: Update Park Time: {}", thingId, time);
            updated |= updateChannel(CHANNEL_LOCATION_TIME, new DateTimeType(time));
            logger.trace("{}: Update Location Address: {}", thingId, position.asPointType());
            updated |= updateLocationAddress(position.asPointType(), CHANNEL_PARK_ADDRESS);
            String parkingTime = getString(position.getParkingTime());
            updated |= updateChannel(CHANNEL_PARK_TIME,
                    !parkingTime.isEmpty() ? getDateTime(parkingTime) : UnDefType.UNDEF);
            updated |= updateChannel(CHANNEL_CAR_MOVING, OnOffType.OFF);
        } catch (ApiException e) {
            logger.trace("{}: ApiException: {}", thingId, e.getMessage());
            updateChannel(CHANNEL_LOCATION_GEO, UnDefType.UNDEF);
            updateChannel(CHANNEL_LOCATION_TIME, UnDefType.UNDEF);
            logger.trace("{}: Http Code {}", thingId, e.getApiResult().httpCode);
            if (e.getApiResult().httpCode == HttpStatus.NO_CONTENT_204) {
                updated |= updateChannel(CHANNEL_CAR_MOVING, OnOffType.ON);
            } else {
                throw e;
            }
        }
        return updated;
    }
}

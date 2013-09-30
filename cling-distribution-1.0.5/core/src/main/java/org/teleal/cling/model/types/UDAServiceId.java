/*
 * Copyright (C) 2010 Teleal GmbH, Switzerland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.teleal.cling.model.types;

import org.teleal.cling.model.Constants;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Service identifier with a fixed <code>upnp-org</code> namespace.
 * <p>
 * Also accepts the namespace sometimes used by broken devices, <code>schemas-upnp-org</code>.
 * </p>
 *
 * @author Christian Bauer
 */
public class UDAServiceId extends ServiceId {

    public static final String DEFAULT_NAMESPACE = "upnp-org";
    public static final String BROKEN_DEFAULT_NAMESPACE = "schemas-upnp-org"; // TODO: UPNP VIOLATION: Intel UPnP tools!

    public static final Pattern PATTERN =
            Pattern.compile("urn:" + DEFAULT_NAMESPACE + ":serviceId:(" + Constants.REGEX_ID+ ")");

    public static final Pattern BROKEN_PATTERN =
            Pattern.compile("urn:" + BROKEN_DEFAULT_NAMESPACE + ":service:(" + Constants.REGEX_ID+ ")"); // Note: 'service' vs. 'serviceId'

    public UDAServiceId(String id) {
        super(DEFAULT_NAMESPACE, id);
    }

    public static UDAServiceId valueOf(String s) throws InvalidValueException {
        Matcher matcher = UDAServiceId.PATTERN.matcher(s);
        if (matcher.matches()) {
            return new UDAServiceId(matcher.group(1));
        } else {
            matcher = UDAServiceId.BROKEN_PATTERN.matcher(s);
            if (matcher.matches()) {
                return new UDAServiceId(matcher.group(1));
            } else {
                throw new InvalidValueException("Can't parse UDA service ID string (upnp-org/id): " + s);
            }
        }
    }

}

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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a device type, for example <code>urn:my-domain-namespace:device:MyDevice:1</code>.
 * <p>
 * Although decimal versions are accepted and parsed, the version used for
 * comparison is only the integer withou the fraction.
 * </p>
 *
 * @author Christian Bauer
 */
public class DeviceType {
    
    public static final Pattern PATTERN =
            Pattern.compile("urn:(" + Constants.REGEX_NAMESPACE + "):device:(" + Constants.REGEX_TYPE + "):([0-9]+).*");

    private String namespace;
    private String type;
    private int version = 1;

    public DeviceType(String namespace, String type) {
        this(namespace, type, 1);
    }

    public DeviceType(String namespace, String type, int version) {
        if (namespace != null && !namespace.matches(Constants.REGEX_NAMESPACE)) {
            throw new IllegalArgumentException("Device type namespace contains illegal characters");
        }
        this.namespace = namespace;

        if (type != null && !type.matches(Constants.REGEX_TYPE)) {
            throw new IllegalArgumentException("Device type suffix too long (64) or contains illegal characters");
        }
        this.type = type;

        this.version = version;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getType() {
        return type;
    }

    public int getVersion() {
        return version;
    }

    /**
     * @return Either a {@link UDADeviceType} or a more generic {@link DeviceType}.
     */
    public static DeviceType valueOf(String s) throws InvalidValueException {

        DeviceType deviceType = null;

        // Sometimes crazy UPnP devices deliver spaces in a URN, don't ask...
        s = s.replaceAll("\\s", "");

        // First try UDADeviceType parse
        try {
            deviceType = UDADeviceType.valueOf(s);
        } catch (Exception ex) {
            // Ignore
        }

        // Now try a generic DeviceType parse
        if (deviceType == null) {
            Matcher matcher = PATTERN.matcher(s);
            if (matcher.matches()) {
                return new DeviceType(matcher.group(1), matcher.group(2), Integer.valueOf(matcher.group(3)));
            } else {
                throw new InvalidValueException("Can't parse device type string (namespace/type/version): " + s);
            }
        }
        return deviceType;
    }

    public boolean implementsVersion(DeviceType that) {
        if (!namespace.equals(that.namespace)) return false;
        if (!type.equals(that.type)) return false;
        if (version < that.version) return false;
        return true;
    }

    public String getDisplayString() {
        return getType();
    }

    @Override
    public String toString() {
        return "urn:" + getNamespace() + ":device:" + getType()+ ":" + getVersion();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof DeviceType)) return false;

        DeviceType that = (DeviceType) o;

        if (version != that.version) return false;
        if (!namespace.equals(that.namespace)) return false;
        if (!type.equals(that.type)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = namespace.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + version;
        return result;
    }
}

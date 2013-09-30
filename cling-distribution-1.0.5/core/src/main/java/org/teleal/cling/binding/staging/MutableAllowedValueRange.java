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

package org.teleal.cling.binding.staging;

/**
 * @author Christian Bauer
 */
public class MutableAllowedValueRange {

    // TODO: UPNP VIOLATION: Some devices (Netgear Router again...) send empty elements, so use some sane defaults
    // TODO: UPNP VIOLATION: The WANCommonInterfaceConfig example XML is even wrong, it does not include a <maximum> element!
    public Long minimum = 0l;
    public Long maximum = Long.MAX_VALUE;
    public Long step = 1l;

}

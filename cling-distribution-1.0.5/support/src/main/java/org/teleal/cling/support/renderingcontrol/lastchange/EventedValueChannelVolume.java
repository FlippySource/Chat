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

package org.teleal.cling.support.renderingcontrol.lastchange;

import org.teleal.cling.model.types.Datatype;
import org.teleal.cling.model.types.InvalidValueException;
import org.teleal.cling.model.types.UnsignedIntegerTwoBytes;
import org.teleal.cling.model.types.UnsignedIntegerTwoBytesDatatype;
import org.teleal.cling.support.lastchange.EventedValue;
import org.teleal.cling.support.model.Channel;
import org.teleal.cling.support.shared.AbstractMap;

import java.util.Map;

/**
 * @author Christian Bauer
 */
public class EventedValueChannelVolume extends EventedValue<ChannelVolume> {

    public EventedValueChannelVolume(ChannelVolume value) {
        super(value);
    }

    public EventedValueChannelVolume(Map.Entry<String, String>[] attributes) {
        super(attributes);
    }

    @Override
    protected ChannelVolume valueOf(Map.Entry<String, String>[] attributes) throws InvalidValueException {
        Channel channel = null;
        Integer volume = null;
        for (Map.Entry<String, String> attribute : attributes) {
            if (attribute.getKey().equals("channel"))
                channel = Channel.valueOf(attribute.getValue());
            if (attribute.getKey().equals("val"))
                volume = (new UnsignedIntegerTwoBytesDatatype()
                        .valueOf(attribute.getValue()))
                        .getValue().intValue(); // Java is fun!
        }
        return channel != null && volume != null ? new ChannelVolume(channel, volume) : null;
    }

    @Override
    public Map.Entry<String, String>[] getAttributes() {
        return new Map.Entry[]{
                new AbstractMap.SimpleEntry<String, String>(
                        "val",
                        new UnsignedIntegerTwoBytesDatatype().getString(
                                new UnsignedIntegerTwoBytes(getValue().getVolume())
                        )
                ),
                new AbstractMap.SimpleEntry<String, String>(
                        "channel",
                        getValue().getChannel().name()
                )
        };
    }

    @Override
    public String toString() {
        return getValue().toString();
    }

    @Override
    protected Datatype getDatatype() {
        return null; // Not needed
    }
}

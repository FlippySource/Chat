/*
 * Copyright (C) 2011 Teleal GmbH, Switzerland
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

package example.mediarenderer;

import org.teleal.cling.model.types.UnsignedIntegerFourBytes;
import org.teleal.cling.model.types.UnsignedIntegerTwoBytes;
import org.teleal.cling.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.teleal.cling.support.avtransport.lastchange.AVTransportVariable;
import org.teleal.cling.support.contentdirectory.DIDLParser;
import org.teleal.cling.support.lastchange.Event;
import org.teleal.cling.support.lastchange.LastChange;
import org.teleal.cling.support.lastchange.LastChangeParser;
import org.teleal.cling.support.model.Channel;
import org.teleal.cling.support.model.DIDLContent;
import org.teleal.cling.support.model.TransportState;
import org.teleal.cling.support.renderingcontrol.lastchange.ChannelVolume;
import org.teleal.cling.support.renderingcontrol.lastchange.RenderingControlLastChangeParser;
import org.teleal.cling.support.renderingcontrol.lastchange.RenderingControlVariable;
import org.testng.annotations.Test;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import static org.testng.Assert.assertEquals;

public class LastChangeTest {

    @Test
    public void setFireGet() throws Exception {

        LastChange lc = new LastChange(new RenderingControlLastChangeParser());

        lc.setEventedValue(0, new RenderingControlVariable.PresetNameList("foo"));
        lc.setEventedValue(0, new RenderingControlVariable.PresetNameList("foobar")); // Double set!

        lc.setEventedValue(
                0,
                new RenderingControlVariable.Volume(
                        new ChannelVolume(Channel.Master, 123)
                )
        );

        lc.setEventedValue(1, new RenderingControlVariable.Brightness(new UnsignedIntegerTwoBytes(456)));

        final String[] lcValue = new String[1];
        PropertyChangeSupport pcs = new PropertyChangeSupport(this);
        pcs.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
                if (ev.getPropertyName().equals("LastChange"))
                    lcValue[0] = (String) ev.getNewValue();
            }
        });
        lc.fire(pcs);

        // Check it's clear
        assertEquals(
                lc.getEventedValue(0, RenderingControlVariable.PresetNameList.class),
                null
        );
        assertEquals(lc.toString(), "");

        // Set something again, it's not fired, so it has no consequence on further assertions
        lc.setEventedValue(0, new RenderingControlVariable.PresetNameList("foo"));

        // Read the XML string instead
        lc = new LastChange(new RenderingControlLastChangeParser(), lcValue[0]);

        assertEquals(
                lc.getEventedValue(0, RenderingControlVariable.PresetNameList.class).getValue(),
                "foobar"
        );

        assertEquals(
                lc.getEventedValue(0, RenderingControlVariable.Volume.class).getValue().getChannel(),
                Channel.Master
        );
        assertEquals(
                lc.getEventedValue(0, RenderingControlVariable.Volume.class).getValue().getVolume(),
                new Integer(123)
        );

        assertEquals(
                lc.getEventedValue(1, RenderingControlVariable.Brightness.class).getValue(),
                new UnsignedIntegerTwoBytes(456)
        );

    }

    @Test
    public void parseLastChangeXML() throws Exception {

        LastChangeParser avTransportParser = new AVTransportLastChangeParser();

        Event event = avTransportParser.parseResource("org/teleal/cling/test/support/lastchange/samples/avtransport-roku.xml");
        assertEquals(event.getInstanceIDs().size(), 1);
        UnsignedIntegerFourBytes instanceId = new UnsignedIntegerFourBytes(0);
        assertEquals(
                event.getEventedValue(instanceId, AVTransportVariable.TransportState.class).getValue(),
                TransportState.STOPPED
        );

        String trackMetaDataXML = event.getEventedValue(instanceId, AVTransportVariable.CurrentTrackMetaData.class).getValue();
        DIDLContent trackMetaData = new DIDLParser().parse(trackMetaDataXML);
        assertEquals(trackMetaData.getContainers().size(), 0);
        assertEquals(trackMetaData.getItems().size(), 1);
    }

}

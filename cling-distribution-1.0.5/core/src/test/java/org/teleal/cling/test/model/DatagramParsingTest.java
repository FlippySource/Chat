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

package org.teleal.cling.test.model;

import org.teleal.cling.model.Constants;
import org.teleal.cling.model.Location;
import org.teleal.cling.model.NetworkAddress;
import org.teleal.cling.model.types.NotificationSubtype;
import org.teleal.cling.model.message.header.HostHeader;
import org.teleal.cling.model.message.header.MaxAgeHeader;
import org.teleal.cling.model.message.header.USNRootDeviceHeader;
import org.teleal.cling.model.message.header.UpnpHeader;
import org.teleal.cling.model.message.header.ServerHeader;
import org.teleal.cling.model.message.header.EXTHeader;
import org.teleal.cling.model.message.header.InterfaceMacHeader;
import org.teleal.cling.model.message.UpnpMessage;
import org.teleal.cling.model.message.UpnpRequest;
import org.teleal.cling.model.message.OutgoingDatagramMessage;
import org.teleal.cling.model.message.discovery.OutgoingNotificationRequestRootDevice;
import org.teleal.cling.test.data.SampleData;
import org.teleal.cling.test.data.SampleDeviceRoot;
import org.teleal.cling.transport.spi.DatagramProcessor;
import org.teleal.cling.transport.impl.NetworkAddressFactoryImpl;
import org.teleal.cling.DefaultUpnpServiceConfiguration;
import org.teleal.common.util.HexBin;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;


public class DatagramParsingTest {

    @Test
    public void readSource() throws Exception {

        String source = "NOTIFY * HTTP/1.1\r\n" +
                        "HOST: 239.255.255.250:1900\r\n" +
                        "CACHE-CONTROL: max-age=2000\r\n" +
                        "LOCATION: http://localhost:0/some/path/123/desc.xml\r\n" +
                        "X-CLING-IFACE-MAC: 00:17:ab:e9:65:a0\r\n" +
                        "NT: upnp:rootdevice\r\n" +
                        "NTS: ssdp:alive\r\n" +
                        "EXT:\r\n" +
                        "SERVER: foo/1 UPnP/1.0" + // FOLDED HEADER LINE!
                        " bar/2\r\n" +
                        "USN: " + SampleDeviceRoot.getRootUDN().toString()+"::upnp:rootdevice\r\n\r\n";

        DatagramPacket packet = new DatagramPacket(source.getBytes(), source.getBytes().length, new InetSocketAddress("123.123.123.123", 1234));

        DatagramProcessor processor = new DefaultUpnpServiceConfiguration().getDatagramProcessor();

        UpnpMessage<UpnpRequest> msg = processor.read(InetAddress.getByName("127.0.0.1"), packet);

        Assert.assertEquals(msg.getOperation().getMethod(), UpnpRequest.Method.NOTIFY);

        Assert.assertEquals(msg.getHeaders().getFirstHeader(UpnpHeader.Type.HOST, HostHeader.class).getValue().getHost(), Constants.IPV4_UPNP_MULTICAST_GROUP);
        Assert.assertEquals(msg.getHeaders().getFirstHeader(UpnpHeader.Type.HOST, HostHeader.class).getValue().getPort(), Constants.UPNP_MULTICAST_PORT);
        Assert.assertEquals(
                msg.getHeaders().getFirstHeader(UpnpHeader.Type.USN, USNRootDeviceHeader.class).getValue().getIdentifierString(),
                SampleDeviceRoot.getRootUDN().getIdentifierString()
        );
        Assert.assertEquals(msg.getHeaders().getFirstHeader(UpnpHeader.Type.MAX_AGE, MaxAgeHeader.class).getValue().toString(), "2000");
        Assert.assertEquals(msg.getHeaders().getFirstHeader(UpnpHeader.Type.SERVER, ServerHeader.class).getValue().getOsName(), "foo");
        Assert.assertEquals(msg.getHeaders().getFirstHeader(UpnpHeader.Type.SERVER, ServerHeader.class).getValue().getOsVersion(), "1");
        Assert.assertEquals(msg.getHeaders().getFirstHeader(UpnpHeader.Type.SERVER, ServerHeader.class).getValue().getMajorVersion(), 1);
        Assert.assertEquals(msg.getHeaders().getFirstHeader(UpnpHeader.Type.SERVER, ServerHeader.class).getValue().getMinorVersion(), 0);
        Assert.assertEquals(msg.getHeaders().getFirstHeader(UpnpHeader.Type.SERVER, ServerHeader.class).getValue().getProductName(), "bar");
        Assert.assertEquals(msg.getHeaders().getFirstHeader(UpnpHeader.Type.SERVER, ServerHeader.class).getValue().getProductVersion(), "2");

        // Doesn't belong in this message but we need to test empty header values
        assert msg.getHeaders().getFirstHeader(UpnpHeader.Type.EXT) != null;

        Assert.assertEquals(msg.getHeaders().getFirstHeader(UpnpHeader.Type.EXT_IFACE_MAC, InterfaceMacHeader.class).getString(), "00:17:AB:E9:65:A0");

    }

    @Test
    public void parseRoundtrip() throws Exception {
        Location location = new Location(
                new NetworkAddress(
                        InetAddress.getByName("localhost"),
                        NetworkAddressFactoryImpl.DEFAULT_TCP_HTTP_LISTEN_PORT,
                        HexBin.stringToBytes("00:17:AB:E9:65:A0", ":")
                ),
                URI.create("/some/path/123/desc/xml")
        );

        OutgoingDatagramMessage msg =
                new OutgoingNotificationRequestRootDevice(
                        location,
                        SampleData.createLocalDevice(),
                        NotificationSubtype.ALIVE
                );

        msg.getHeaders().add(UpnpHeader.Type.EXT, new EXTHeader()); // Again, the empty header value

        DatagramProcessor processor = new DefaultUpnpServiceConfiguration().getDatagramProcessor();

        DatagramPacket packet = processor.write(msg);

        Assert.assertTrue(new String(packet.getData()).endsWith("\r\n\r\n"));

        UpnpMessage readMsg = processor.read(InetAddress.getByName("127.0.0.1"), packet);

        Assert.assertEquals(readMsg.getHeaders().getFirstHeader(UpnpHeader.Type.HOST).getString(), msg.getHeaders().getFirstHeader(UpnpHeader.Type.HOST).getString());
        Assert.assertEquals(readMsg.getHeaders().getFirstHeader(UpnpHeader.Type.MAX_AGE).getString(), msg.getHeaders().getFirstHeader(UpnpHeader.Type.MAX_AGE).getString());
        Assert.assertEquals(readMsg.getHeaders().getFirstHeader(UpnpHeader.Type.LOCATION).getString(), msg.getHeaders().getFirstHeader(UpnpHeader.Type.LOCATION).getString());
        Assert.assertEquals(readMsg.getHeaders().getFirstHeader(UpnpHeader.Type.NT).getString(), msg.getHeaders().getFirstHeader(UpnpHeader.Type.NT).getString());
        Assert.assertEquals(readMsg.getHeaders().getFirstHeader(UpnpHeader.Type.NTS).getString(), msg.getHeaders().getFirstHeader(UpnpHeader.Type.NTS).getString());
        Assert.assertEquals(readMsg.getHeaders().getFirstHeader(UpnpHeader.Type.SERVER).getString(), msg.getHeaders().getFirstHeader(UpnpHeader.Type.SERVER).getString());
        Assert.assertEquals(readMsg.getHeaders().getFirstHeader(UpnpHeader.Type.USN).getString(), msg.getHeaders().getFirstHeader(UpnpHeader.Type.USN).getString());
        assert readMsg.getHeaders().getFirstHeader(UpnpHeader.Type.EXT) != null;

        Assert.assertEquals(readMsg.getHeaders().getFirstHeader(UpnpHeader.Type.EXT_IFACE_MAC).getString(), msg.getHeaders().getFirstHeader(UpnpHeader.Type.EXT_IFACE_MAC).getString());
    }

}

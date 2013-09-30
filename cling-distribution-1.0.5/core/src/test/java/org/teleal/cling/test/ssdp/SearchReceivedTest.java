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

package org.teleal.cling.test.ssdp;

import org.teleal.cling.UpnpService;
import org.teleal.cling.model.Constants;
import org.teleal.cling.model.Namespace;
import org.teleal.cling.model.message.IncomingDatagramMessage;
import org.teleal.cling.model.message.OutgoingDatagramMessage;
import org.teleal.cling.model.message.UpnpMessage;
import org.teleal.cling.model.message.UpnpRequest;
import org.teleal.cling.model.message.discovery.IncomingSearchRequest;
import org.teleal.cling.model.message.header.DeviceTypeHeader;
import org.teleal.cling.model.message.header.DeviceUSNHeader;
import org.teleal.cling.model.message.header.EXTHeader;
import org.teleal.cling.model.message.header.HostHeader;
import org.teleal.cling.model.message.header.MANHeader;
import org.teleal.cling.model.message.header.MXHeader;
import org.teleal.cling.model.message.header.MaxAgeHeader;
import org.teleal.cling.model.message.header.RootDeviceHeader;
import org.teleal.cling.model.message.header.STAllHeader;
import org.teleal.cling.model.message.header.ServiceTypeHeader;
import org.teleal.cling.model.message.header.ServiceUSNHeader;
import org.teleal.cling.model.message.header.UDNHeader;
import org.teleal.cling.model.message.header.UpnpHeader;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.NotificationSubtype;
import org.teleal.cling.protocol.async.ReceivingSearch;
import org.teleal.cling.mock.MockUpnpService;
import org.teleal.cling.test.data.SampleData;
import org.teleal.cling.test.data.SampleUSNHeaders;
import org.teleal.common.util.URIUtil;
import org.testng.annotations.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.testng.Assert.assertEquals;


public class SearchReceivedTest {

    @Test
    public void receivedSearchAll() throws Exception {

        MockUpnpService upnpService = new MockUpnpService();

        LocalDevice localDevice = SampleData.createLocalDevice();
        LocalDevice embeddedDevice = localDevice.getEmbeddedDevices()[0];
        upnpService.getRegistry().addDevice(localDevice);

        IncomingSearchRequest searchMsg = createRequestMessage();
        searchMsg.getHeaders().add(UpnpHeader.Type.MAN, new MANHeader(NotificationSubtype.DISCOVER.getHeaderString()));
        searchMsg.getHeaders().add(UpnpHeader.Type.MX, new MXHeader(1));
        searchMsg.getHeaders().add(UpnpHeader.Type.ST, new STAllHeader());
        searchMsg.getHeaders().add(UpnpHeader.Type.HOST, new HostHeader());

        ReceivingSearch prot = createProtocol(upnpService, searchMsg);
        prot.run();

        assertEquals(upnpService.getOutgoingDatagramMessages().size(), 10);

        for (OutgoingDatagramMessage msg : upnpService.getOutgoingDatagramMessages()) {
            //SampleData.debugMsg(msg);
            assertSearchResponseBasics(upnpService.getConfiguration().getNamespace(), msg, localDevice);
        }

        SampleUSNHeaders.assertUSNHeaders(upnpService.getOutgoingDatagramMessages(), localDevice, embeddedDevice, UpnpHeader.Type.ST);
    }

    @Test
    public void receivedSearchRoot() throws Exception {

        MockUpnpService upnpService = new MockUpnpService();

        LocalDevice localDevice = SampleData.createLocalDevice();
        upnpService.getRegistry().addDevice(localDevice);

        IncomingSearchRequest searchMsg = createRequestMessage();
        searchMsg.getHeaders().add(UpnpHeader.Type.MAN, new MANHeader(NotificationSubtype.DISCOVER.getHeaderString()));
        searchMsg.getHeaders().add(UpnpHeader.Type.MX, new MXHeader(1));
        searchMsg.getHeaders().add(UpnpHeader.Type.ST, new RootDeviceHeader());
        searchMsg.getHeaders().add(UpnpHeader.Type.HOST, new HostHeader());

        ReceivingSearch prot = createProtocol(upnpService, searchMsg);
        prot.run();

        assert upnpService.getOutgoingDatagramMessages().size() == 1;
        assertSearchResponseBasics(
                upnpService.getConfiguration().getNamespace(),
                upnpService.getOutgoingDatagramMessages().get(0),
                localDevice
        );
        assertEquals(
                upnpService.getOutgoingDatagramMessages().get(0).getHeaders().getFirstHeader(UpnpHeader.Type.ST).getString(),
                new RootDeviceHeader().getString()
        );
        assertEquals(
                upnpService.getOutgoingDatagramMessages().get(0).getHeaders().getFirstHeader(UpnpHeader.Type.USN).getString(),
                new UDNHeader(localDevice.getIdentity().getUdn()).getString()
        );
    }

    @Test
    public void receivedSearchUDN() throws Exception {

        MockUpnpService upnpService = new MockUpnpService();

        LocalDevice localDevice = SampleData.createLocalDevice();
        upnpService.getRegistry().addDevice(localDevice);

        IncomingSearchRequest searchMsg = createRequestMessage();
        searchMsg.getHeaders().add(UpnpHeader.Type.MAN, new MANHeader(NotificationSubtype.DISCOVER.getHeaderString()));
        searchMsg.getHeaders().add(UpnpHeader.Type.MX, new MXHeader(1));
        searchMsg.getHeaders().add(UpnpHeader.Type.ST, new UDNHeader(localDevice.getIdentity().getUdn()));
        searchMsg.getHeaders().add(UpnpHeader.Type.HOST, new HostHeader());

        ReceivingSearch prot = createProtocol(upnpService, searchMsg);
        prot.run();


        assert upnpService.getOutgoingDatagramMessages().size() == 1;
        assertSearchResponseBasics(
                upnpService.getConfiguration().getNamespace(),
                upnpService.getOutgoingDatagramMessages().get(0),
                localDevice
        );
        assertEquals(
                upnpService.getOutgoingDatagramMessages().get(0).getHeaders().getFirstHeader(UpnpHeader.Type.ST).getString(),
                new UDNHeader(localDevice.getIdentity().getUdn()).getString()
        );
        assertEquals(
                upnpService.getOutgoingDatagramMessages().get(0).getHeaders().getFirstHeader(UpnpHeader.Type.USN).getString(),
                new UDNHeader(localDevice.getIdentity().getUdn()).getString()
        );
    }

    @Test
    public void receivedSearchDeviceType() throws Exception {

        MockUpnpService upnpService = new MockUpnpService();

        LocalDevice localDevice = SampleData.createLocalDevice();
        upnpService.getRegistry().addDevice(localDevice);

        IncomingSearchRequest searchMsg = createRequestMessage();
        searchMsg.getHeaders().add(UpnpHeader.Type.MAN, new MANHeader(NotificationSubtype.DISCOVER.getHeaderString()));
        searchMsg.getHeaders().add(UpnpHeader.Type.MX, new MXHeader(1));
        searchMsg.getHeaders().add(UpnpHeader.Type.ST, new DeviceTypeHeader(localDevice.getType()));
        searchMsg.getHeaders().add(UpnpHeader.Type.HOST, new HostHeader());

        ReceivingSearch prot = createProtocol(upnpService, searchMsg);
        prot.run();

        assert upnpService.getOutgoingDatagramMessages().size() == 1;
        assertSearchResponseBasics(
                upnpService.getConfiguration().getNamespace(),
                upnpService.getOutgoingDatagramMessages().get(0),
                localDevice
        );
        assertEquals(
                upnpService.getOutgoingDatagramMessages().get(0).getHeaders().getFirstHeader(UpnpHeader.Type.ST).getString(),
                new DeviceTypeHeader(localDevice.getType()).getString()
        );
        assertEquals(
                upnpService.getOutgoingDatagramMessages().get(0).getHeaders().getFirstHeader(UpnpHeader.Type.USN).getString(),
                new DeviceUSNHeader(localDevice.getIdentity().getUdn(), localDevice.getType()).getString()
        );
    }

    @Test
    public void receivedSearchServiceType() throws Exception {

        MockUpnpService upnpService = new MockUpnpService();

        LocalDevice localDevice = SampleData.createLocalDevice();
        Service service = localDevice.getServices()[0];
        upnpService.getRegistry().addDevice(localDevice);

        IncomingSearchRequest searchMsg = createRequestMessage();
        searchMsg.getHeaders().add(UpnpHeader.Type.MAN, new MANHeader(NotificationSubtype.DISCOVER.getHeaderString()));
        searchMsg.getHeaders().add(UpnpHeader.Type.MX, new MXHeader(1));
        searchMsg.getHeaders().add(UpnpHeader.Type.ST, new ServiceTypeHeader(service.getServiceType()));
        searchMsg.getHeaders().add(UpnpHeader.Type.HOST, new HostHeader());

        ReceivingSearch prot = createProtocol(upnpService, searchMsg);
        prot.run();

        assert upnpService.getOutgoingDatagramMessages().size() == 1;

        assertSearchResponseBasics(
                upnpService.getConfiguration().getNamespace(),
                upnpService.getOutgoingDatagramMessages().get(0),
                localDevice
        );
        assertEquals(
                upnpService.getOutgoingDatagramMessages().get(0).getHeaders().getFirstHeader(UpnpHeader.Type.ST).getString(),
                new ServiceTypeHeader(service.getServiceType()).getString()
        );
        assertEquals(
                upnpService.getOutgoingDatagramMessages().get(0).getHeaders().getFirstHeader(UpnpHeader.Type.USN).getString(),
                new ServiceUSNHeader(localDevice.getIdentity().getUdn(), service.getServiceType()).getString()
        );
    }

    @Test
    public void receivedInvalidST() throws Exception {

        MockUpnpService upnpService = new MockUpnpService();

        IncomingSearchRequest searchMsg = createRequestMessage();
        searchMsg.getHeaders().add(UpnpHeader.Type.MAN, new MANHeader(NotificationSubtype.DISCOVER.getHeaderString()));
        searchMsg.getHeaders().add(UpnpHeader.Type.MX, new MXHeader(1));
        searchMsg.getHeaders().add(UpnpHeader.Type.HOST, new HostHeader());

        ReceivingSearch prot = createProtocol(upnpService, searchMsg);
        prot.run();

        assert upnpService.getOutgoingDatagramMessages().size() == 0;
    }

    @Test
    public void receivedInvalidMX() throws Exception {

        MockUpnpService upnpService = new MockUpnpService();

        IncomingSearchRequest searchMsg = createRequestMessage();
        searchMsg.getHeaders().add(UpnpHeader.Type.MAN, new MANHeader(NotificationSubtype.DISCOVER.getHeaderString()));
        searchMsg.getHeaders().add(UpnpHeader.Type.ST, new STAllHeader());
        searchMsg.getHeaders().add(UpnpHeader.Type.HOST, new HostHeader());

        ReceivingSearch prot = createProtocol(upnpService, searchMsg);
        prot.run();

        assert upnpService.getOutgoingDatagramMessages().size() == 0;
    }

    protected ReceivingSearch createProtocol(UpnpService upnpService, IncomingSearchRequest searchMsg) throws Exception {
        return new ReceivingSearch(upnpService, searchMsg);
    }

    protected void assertSearchResponseBasics(Namespace namespace, UpnpMessage msg, LocalDevice rootDevice) {
        assertEquals(
                msg.getHeaders().getFirstHeader(UpnpHeader.Type.MAX_AGE).getString(),
                new MaxAgeHeader(rootDevice.getIdentity().getMaxAgeSeconds()).getString()
        );
        assertEquals(msg.getHeaders().getFirstHeader(UpnpHeader.Type.EXT).getString(), new EXTHeader().getString());
        assertEquals(
                msg.getHeaders().getFirstHeader(UpnpHeader.Type.LOCATION).getString(),
                URIUtil.createAbsoluteURL(SampleData.getLocalBaseURL(), namespace.getDescriptorPath(rootDevice)).toString()
        );
        assert msg.getHeaders().getFirstHeader(UpnpHeader.Type.SERVER).getString() != null;
    }

    protected IncomingSearchRequest createRequestMessage() throws UnknownHostException {
        return new IncomingSearchRequest(
                new IncomingDatagramMessage<UpnpRequest>(
                        new UpnpRequest(UpnpRequest.Method.MSEARCH),
                        InetAddress.getByName("127.0.0.1"),
                        Constants.UPNP_MULTICAST_PORT,
                        InetAddress.getByName("127.0.0.1")
                )
        );

    }

}
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

package example.controlpoint;

import org.teleal.cling.mock.MockUpnpService;
import org.teleal.cling.model.message.UpnpMessage;
import org.teleal.cling.model.message.header.DeviceTypeHeader;
import org.teleal.cling.model.message.header.HostHeader;
import org.teleal.cling.model.message.header.MANHeader;
import org.teleal.cling.model.message.header.MXHeader;
import org.teleal.cling.model.message.header.RootDeviceHeader;
import org.teleal.cling.model.message.header.STAllHeader;
import org.teleal.cling.model.message.header.ServiceTypeHeader;
import org.teleal.cling.model.message.header.UDADeviceTypeHeader;
import org.teleal.cling.model.message.header.UDAServiceTypeHeader;
import org.teleal.cling.model.message.header.UDNHeader;
import org.teleal.cling.model.message.header.UpnpHeader;
import org.teleal.cling.model.types.DeviceType;
import org.teleal.cling.model.types.NotificationSubtype;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.model.types.UDADeviceType;
import org.teleal.cling.model.types.UDAServiceType;
import org.teleal.cling.model.types.UDN;
import org.teleal.cling.protocol.async.SendingSearch;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.UUID;


/**
 * Searching the network
 * <p>
 * When your control point joins the network it probably won't know any UPnP devices and services that
 * might be available. To learn about the present devices it can broadcast - actually with UDP multicast
 * datagrams - a search message which will be received by every device. Each receiver then inspects the
 * search message and decides if it should reply directly (with notification UDP datagrams) to the
 * sending control point.
 * </p>
 * <p>
 * Search messages carry a <em>search type</em> header and receivers consider this header when they
 * evaluate a potential response. The Cling <code>ControlPoint</code> API accepts a
 * <code>UpnpHeader</code> argument when creating outgoing search messages.
 * </p>
 * <a class="citation" href="javadoc://this#searchAll" style="read-title: false;"/>
 * <a class="citation" href="javadoc://this#searchUDN" style="read-title: false;"/>
 * <a class="citation" href="javadoc://this#searchDeviceType" style="read-title: false;"/>
 * <a class="citation" href="javadoc://this#searchServiceType" style="read-title: false;"/>
 */
public class SearchExecuteTest {

    /**
     * <p>
     * Most of the time you'd like all devices to respond to your search, this is what the
     * dedicated <code>STAllHeader</code> is used for:
     * </p>
     * <a class="citation" href="javacode://this" style="include: SEARCH"/>
     * <p>
     * Notification messages will be received by your control point and you can listen to
     * the <code>Registry</code> and inspect the found devices and their services. (By the
     * way, if you call <code>search()</code> without any argument, that's the same.)
     * </p>
     */
    @Test
    public void searchAll() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();

        upnpService.getControlPoint().search(       // DOC: SEARCH
                new STAllHeader()
        );                                          // DOC: SEARCH

        assertMessages(upnpService, new STAllHeader());
    }

    /**
     * <p>
     * On the other hand, when you already know the unique device name (UDN) of the device you
     * are searching for - maybe because your control point remembered it while it was turned off - you
     * can send a message which will trigger a response from only a particular device:
     * </p>
     * <a class="citation" href="javacode://this" style="include: SEARCH"/>
     * <p>
     * This is mostly useful to avoid network congestion when dozens of devices might <em>all</em>
     * respond to a search request. Your <code>Registry</code> listener code however still has to
     * inspect each newly found device, as registrations might occur independently from searches.
     * </p>
     */
    @Test
    public void searchUDN() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();

        UDN udn = new UDN(UUID.randomUUID());
        upnpService.getControlPoint().search(       // DOC: SEARCH
                new UDNHeader(udn)
        );                                          // DOC: SEARCH

        assertMessages(upnpService, new UDNHeader(udn));
    }

    /**
     * <p>
     * You can also search by device or service type. This search request will trigger responses
     * from all devices of type "<code>urn:schemas-upnp-org:device:BinaryLight:1</code>":
     * </p>
     * <a class="citation" href="javacode://this" style="include: SEARCH_UDA"/>
     * <p>
     * If the desired device type is of a custom namespace, use this variation:
     * </p>
     * <a class="citation" id="javacode_dt_search_custom" href="javacode://this" style="include: SEARCH_CUSTOM"/>
     */
    @Test
    public void searchDeviceType() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();

        UDADeviceType udaType = new UDADeviceType("BinaryLight");       // DOC: SEARCH_UDA
        upnpService.getControlPoint().search(
                new UDADeviceTypeHeader(udaType)
        );                                                              // DOC: SEARCH_UDA

        assertMessages(upnpService, new UDADeviceTypeHeader(udaType));

        upnpService.getOutgoingDatagramMessages().clear();

        DeviceType type = new DeviceType("org-mydomain", "MyDeviceType", 1);    // DOC: SEARCH_CUSTOM
        upnpService.getControlPoint().search(
                new DeviceTypeHeader(type)
        );                                                                      // DOC: SEARCH_CUSTOM

        assertMessages(upnpService, new DeviceTypeHeader(type));
    }

    /**
     * <p>
     * Or, you can search for all devices which implement a particular service type:
     * </p>
     * <a class="citation" href="javacode://this" style="include: SEARCH_UDA"/>
     * <a class="citation" id="javacode_st_search_custom" href="javacode://this" style="include: SEARCH_CUSTOM"/>
     */
    @Test
    public void searchServiceType() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();

        UDAServiceType udaType = new UDAServiceType("SwitchPower");      // DOC: SEARCH_UDA
        upnpService.getControlPoint().search(
                new UDAServiceTypeHeader(udaType)
        );                                                               // DOC: SEARCH_UDA

        assertMessages(upnpService, new UDAServiceTypeHeader(udaType));

        upnpService.getOutgoingDatagramMessages().clear();

        ServiceType type = new ServiceType("org-mydomain", "MyServiceType", 1);    // DOC: SEARCH_CUSTOM
        upnpService.getControlPoint().search(
                new ServiceTypeHeader(type)
        );                                                                        // DOC: SEARCH_CUSTOM

        assertMessages(upnpService, new ServiceTypeHeader(type));
    }


    @Test
    public void searchRoot() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.getControlPoint().search(new RootDeviceHeader());
        assertMessages(upnpService, new RootDeviceHeader());
    }


    @Test
    public void searchDefaults() {
        SendingSearch search = new SendingSearch(new MockUpnpService());
        Assert.assertEquals(search.getSearchTarget().getString(), new STAllHeader().getString());
    }

    @Test(expectedExceptions = java.lang.IllegalArgumentException.class)
    public void searchInvalidST() {
        SendingSearch search = new SendingSearch(new MockUpnpService(), new MXHeader());
    }

    protected void assertMessages(MockUpnpService upnpService, UpnpHeader header) throws Exception {
        Assert.assertEquals(upnpService.getOutgoingDatagramMessages().size(), 2);
        for (UpnpMessage msg : upnpService.getOutgoingDatagramMessages()) {
            assertSearchMessage(msg, header);
        }
    }

    protected void assertSearchMessage(UpnpMessage msg, UpnpHeader searchTarget) {
        Assert.assertEquals(msg.getHeaders().getFirstHeader(UpnpHeader.Type.MAN).getString(), new MANHeader(NotificationSubtype.DISCOVER.getHeaderString()).getString());
        Assert.assertEquals(msg.getHeaders().getFirstHeader(UpnpHeader.Type.MX).getString(), new MXHeader().getString());
        Assert.assertEquals(msg.getHeaders().getFirstHeader(UpnpHeader.Type.ST).getString(), searchTarget.getString());
        Assert.assertEquals(msg.getHeaders().getFirstHeader(UpnpHeader.Type.HOST).getString(), new HostHeader().getString());
    }
}

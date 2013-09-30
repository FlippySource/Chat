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

package org.teleal.cling.test.resources;

import org.teleal.cling.binding.xml.DeviceDescriptorBinder;
import org.teleal.cling.mock.MockUpnpService;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.message.StreamRequestMessage;
import org.teleal.cling.model.message.StreamResponseMessage;
import org.teleal.cling.model.message.UpnpRequest;
import org.teleal.cling.model.message.header.ContentTypeHeader;
import org.teleal.cling.model.message.header.HostHeader;
import org.teleal.cling.model.message.header.UpnpHeader;
import org.teleal.cling.protocol.sync.ReceivingRetrieval;
import org.teleal.cling.test.data.SampleData;
import org.teleal.cling.test.data.SampleDeviceRoot;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;


public class DeviceDescriptorRetrievalTest {

    @Test
    public void registerAndRetrieveDescriptor() throws Exception {

        MockUpnpService upnpService = new MockUpnpService();

        // Register a device
        LocalDevice localDevice = SampleData.createLocalDevice();
        upnpService.getRegistry().addDevice(localDevice);

        // Retrieve the descriptor
        StreamRequestMessage descRetrievalMessage = new StreamRequestMessage(UpnpRequest.Method.GET, SampleDeviceRoot.getDeviceDescriptorURI());
        descRetrievalMessage.getHeaders().add(UpnpHeader.Type.HOST, new HostHeader("localhost", 1234));
        ReceivingRetrieval prot = new ReceivingRetrieval(upnpService, descRetrievalMessage);
        prot.run();
        StreamResponseMessage descriptorMessage = prot.getOutputMessage();

        // UDA 1.0 spec days this musst be 'text/xml'
        assertEquals(
                descriptorMessage.getHeaders().getFirstHeader(UpnpHeader.Type.CONTENT_TYPE).getValue(),
                ContentTypeHeader.DEFAULT_CONTENT_TYPE
        );

        // Read the response and compare the returned device descriptor (test with LocalDevice for correct assertions)
        DeviceDescriptorBinder binder = upnpService.getConfiguration().getDeviceDescriptorBinderUDA10();

        RemoteDevice returnedDevice =
                new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        returnedDevice = binder.describe(returnedDevice, descriptorMessage.getBodyString());

        SampleDeviceRoot.assertLocalResourcesMatch(
                upnpService.getConfiguration().getNamespace().getResources(returnedDevice)
        );
    }

    @Test
    public void retrieveNonExistentDescriptor() throws Exception {

        MockUpnpService upnpService = new MockUpnpService();

        // Retrieve the descriptor
        StreamRequestMessage descRetrievalMessage = new StreamRequestMessage(UpnpRequest.Method.GET, SampleDeviceRoot.getDeviceDescriptorURI());
        descRetrievalMessage.getHeaders().add(UpnpHeader.Type.HOST, new HostHeader("localhost", 1234));
        ReceivingRetrieval prot = new ReceivingRetrieval(upnpService, descRetrievalMessage);
        prot.run();
        StreamResponseMessage descriptorMessage = prot.getOutputMessage();

        assert descriptorMessage == null;
    }

}

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

import org.teleal.cling.binding.xml.ServiceDescriptorBinder;
import org.teleal.cling.mock.MockUpnpService;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.meta.RemoteService;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.message.StreamRequestMessage;
import org.teleal.cling.model.message.StreamResponseMessage;
import org.teleal.cling.model.message.UpnpRequest;
import org.teleal.cling.model.message.header.ContentTypeHeader;
import org.teleal.cling.model.message.header.HostHeader;
import org.teleal.cling.model.message.header.UpnpHeader;
import org.teleal.cling.protocol.sync.ReceivingRetrieval;
import org.teleal.cling.test.data.SampleData;
import org.teleal.cling.test.data.SampleServiceOne;
import org.testng.annotations.Test;

import java.net.URI;

import static org.testng.Assert.assertEquals;


public class ServiceDescriptorRetrievalTest {

    @Test
    public void registerAndRetrieveDescriptor() throws Exception {

        MockUpnpService upnpService = new MockUpnpService();

        // Register a device
        LocalDevice localDevice = SampleData.createLocalDevice();
        LocalService service = SampleData.getFirstService(localDevice);
        upnpService.getRegistry().addDevice(localDevice);

        // Retrieve the descriptor
        URI descriptorURI = upnpService.getConfiguration().getNamespace().getDescriptorPath(service);
        StreamRequestMessage descRetrievalMessage = new StreamRequestMessage(UpnpRequest.Method.GET, descriptorURI);
        descRetrievalMessage.getHeaders().add(UpnpHeader.Type.HOST, new HostHeader("localhost", 1234));
        ReceivingRetrieval prot = new ReceivingRetrieval(upnpService, descRetrievalMessage);
        prot.run();
        StreamResponseMessage descriptorMessage = prot.getOutputMessage();

        // UDA 1.0 spec days this musst be 'text/xml'
        assertEquals(
                descriptorMessage.getHeaders().getFirstHeader(UpnpHeader.Type.CONTENT_TYPE).getValue(),
                ContentTypeHeader.DEFAULT_CONTENT_TYPE
        );

        // Read the response and compare the returned device descriptor
        ServiceDescriptorBinder binder = upnpService.getConfiguration().getServiceDescriptorBinderUDA10();

        RemoteService remoteService = SampleData.createUndescribedRemoteService();

        remoteService = binder.describe(remoteService, descriptorMessage.getBodyString());
        SampleServiceOne.assertMatch(remoteService, service);
    }

    @Test
    public void retrieveNonExistentDescriptor() throws Exception {

        MockUpnpService upnpService = new MockUpnpService();

        // Retrieve the descriptor
        LocalDevice localDevice = SampleData.createLocalDevice();
        Service service = SampleData.getFirstService(localDevice);

        URI descriptorURI = upnpService.getConfiguration().getNamespace().getDescriptorPath(service);
        StreamRequestMessage descRetrievalMessage =
                new StreamRequestMessage(UpnpRequest.Method.GET, descriptorURI);
        descRetrievalMessage.getHeaders().add(UpnpHeader.Type.HOST, new HostHeader("localhost", 1234));
        ReceivingRetrieval prot = new ReceivingRetrieval(upnpService, descRetrievalMessage);
        prot.run();
        StreamResponseMessage descriptorMessage = prot.getOutputMessage();

        // Should be null because it can't be found
        assert descriptorMessage == null;

    }

}
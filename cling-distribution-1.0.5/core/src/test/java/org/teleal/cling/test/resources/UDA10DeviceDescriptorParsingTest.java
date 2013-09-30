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

import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.message.UpnpHeaders;
import org.teleal.cling.binding.xml.DeviceDescriptorBinder;
import org.teleal.cling.binding.xml.UDA10DeviceDescriptorBinderImpl;
import org.teleal.cling.binding.xml.UDA10DeviceDescriptorBinderSAXImpl;
import org.teleal.cling.mock.MockUpnpService;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.profile.ControlPointInfo;
import org.teleal.cling.test.data.SampleData;
import org.teleal.cling.test.data.SampleDeviceRoot;
import org.teleal.common.io.IO;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


public class UDA10DeviceDescriptorParsingTest {

    @Test
    public void readUDA10DescriptorDOM() throws Exception {

        DeviceDescriptorBinder binder = new UDA10DeviceDescriptorBinderImpl();

        RemoteDevice device = new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        device = binder.describe(device, IO.readLines(getClass().getResourceAsStream("/test-dvc-uda10.xml")));

        SampleDeviceRoot.assertLocalResourcesMatch(
                new MockUpnpService().getConfiguration().getNamespace().getResources(device)
        );
        SampleDeviceRoot.assertMatch(device, SampleData.createRemoteDevice());

    }

    @Test
    public void readUDA10DescriptorSAX() throws Exception {

        DeviceDescriptorBinder binder = new UDA10DeviceDescriptorBinderSAXImpl();

        RemoteDevice device = new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        device = binder.describe(device, IO.readLines(getClass().getResourceAsStream("/test-dvc-uda10.xml")));

        SampleDeviceRoot.assertLocalResourcesMatch(
                new MockUpnpService().getConfiguration().getNamespace().getResources(device)
        );
        SampleDeviceRoot.assertMatch(device, SampleData.createRemoteDevice());

    }

    @Test
    public void writeUDA10Descriptor() throws Exception {

        MockUpnpService upnpService = new MockUpnpService();
        DeviceDescriptorBinder binder = new UDA10DeviceDescriptorBinderImpl();
        
        RemoteDevice device = SampleData.createRemoteDevice();
        String descriptorXml = binder.generate(
                device,
                new ControlPointInfo(),
                upnpService.getConfiguration().getNamespace()
        );

/*
        System.out.println("#######################################################################################");
        System.out.println(descriptorXml);
        System.out.println("#######################################################################################");
*/

        RemoteDevice hydratedDevice = new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        hydratedDevice = binder.describe(hydratedDevice, descriptorXml);

        SampleDeviceRoot.assertLocalResourcesMatch(
                upnpService.getConfiguration().getNamespace().getResources(hydratedDevice)

        );
        SampleDeviceRoot.assertMatch(hydratedDevice, device);

    }

    @Test
    public void writeUDA10DescriptorWithProvider() throws Exception {

        MockUpnpService upnpService = new MockUpnpService();
        DeviceDescriptorBinder binder = new UDA10DeviceDescriptorBinderImpl();

        LocalDevice device = SampleData.createLocalDevice(true);
        String descriptorXml = binder.generate(
                device,
                new ControlPointInfo(),
                upnpService.getConfiguration().getNamespace()
        );


        //System.out.println("#######################################################################################");
        //System.out.println(descriptorXml);
        //System.out.println("#######################################################################################");


        RemoteDevice hydratedDevice = new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        hydratedDevice = binder.describe(hydratedDevice, descriptorXml);

        SampleDeviceRoot.assertLocalResourcesMatch(
                upnpService.getConfiguration().getNamespace().getResources(hydratedDevice)

        );
        //SampleDeviceRoot.assertMatch(hydratedDevice, device, false);

    }

    @Test
    public void readUDA10DescriptorWithURLBase() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        DeviceDescriptorBinder binder = upnpService.getConfiguration().getDeviceDescriptorBinderUDA10();

        RemoteDevice device = new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        device = binder.describe(device, IO.readLines(getClass().getResourceAsStream("/test-dvc-uda10-withbase.xml")));

        assertEquals(device.normalizeURI(device.getDetails().getManufacturerDetails().getManufacturerURI()).toString(), SampleData.getLocalBaseURL().toString() + "mfc.html");
        assertEquals(device.normalizeURI(device.getDetails().getModelDetails().getModelURI()).toString(), SampleData.getLocalBaseURL().toString() + "someotherbase/MY-DEVICE-123/model.html");
        assertEquals(device.normalizeURI(device.getDetails().getPresentationURI()).toString(), "http://www.teleal.org/some_ui");

        assertEquals(device.normalizeURI(device.getIcons()[0].getUri()).toString(), SampleData.getLocalBaseURL().toString() + "someotherbase/MY-DEVICE-123/icon.png");

        assertEquals(device.normalizeURI(device.getServices()[0].getDescriptorURI()).toString(), SampleData.getLocalBaseURL().toString() + "someotherbase/MY-DEVICE-123/svc/upnp-org/MY-SERVICE-123/desc.xml");
        assertEquals(device.normalizeURI(device.getServices()[0].getControlURI()).toString(), SampleData.getLocalBaseURL().toString() + "someotherbase/MY-DEVICE-123/svc/upnp-org/MY-SERVICE-123/control");
        assertEquals(device.normalizeURI(device.getServices()[0].getEventSubscriptionURI()).toString(), SampleData.getLocalBaseURL().toString() + "someotherbase/MY-DEVICE-123/svc/upnp-org/MY-SERVICE-123/events");

        assertTrue(device.isRoot());
    }

}


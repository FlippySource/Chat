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

package org.teleal.cling.test.local;

import org.teleal.cling.binding.LocalServiceBinder;
import org.teleal.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.teleal.cling.binding.annotations.UpnpAction;
import org.teleal.cling.binding.annotations.UpnpInputArgument;
import org.teleal.cling.binding.annotations.UpnpOutputArgument;
import org.teleal.cling.binding.annotations.UpnpService;
import org.teleal.cling.binding.annotations.UpnpServiceId;
import org.teleal.cling.binding.annotations.UpnpServiceType;
import org.teleal.cling.binding.annotations.UpnpStateVariable;
import org.teleal.cling.binding.xml.DeviceDescriptorBinder;
import org.teleal.cling.binding.xml.ServiceDescriptorBinder;
import org.teleal.cling.mock.MockUpnpService;
import org.teleal.cling.model.Namespace;
import org.teleal.cling.model.ServerClientTokens;
import org.teleal.cling.model.message.UpnpHeaders;
import org.teleal.cling.model.message.UpnpMessage;
import org.teleal.cling.model.message.header.UpnpHeader;
import org.teleal.cling.model.meta.DeviceDetails;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.profile.ControlPointInfo;
import org.teleal.cling.model.types.NotificationSubtype;
import org.teleal.cling.model.types.UDADeviceType;
import org.teleal.cling.test.data.SampleData;
import org.teleal.common.util.URIUtil;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class LocalDeviceBindingAdvertisementTest {

    @Test
    public void registerLocalDevice() throws Exception {

        MockUpnpService upnpService = new MockUpnpService(true, true);

        LocalDevice binaryLight = DemoBinaryLight.createTestDevice();

        upnpService.getRegistry().addDevice(binaryLight);

        Thread.sleep(2000);

        assert upnpService.getOutgoingDatagramMessages().size() == 12;
        for (UpnpMessage msg : upnpService.getOutgoingDatagramMessages()) {
            assertAliveMsgBasics(upnpService.getConfiguration().getNamespace(), msg, binaryLight, 1800);
        }

        upnpService.shutdown();

        DeviceDescriptorBinder dvcBinder = upnpService.getConfiguration().getDeviceDescriptorBinderUDA10();
        String descriptorXml = dvcBinder.generate(
                binaryLight,
                new ControlPointInfo(),
                upnpService.getConfiguration().getNamespace()
         );

        RemoteDevice testDevice = new RemoteDevice(SampleData.createRemoteDeviceIdentity());

        testDevice = dvcBinder.describe(testDevice, descriptorXml);
        assertEquals(testDevice.getDetails().getFriendlyName(), "Example Binary Light");

        // TODO: more tests

        ServiceDescriptorBinder svcBinder = upnpService.getConfiguration().getServiceDescriptorBinderUDA10();
        String serviceXml = svcBinder.generate(binaryLight.getServices()[0]);

        // TODO: more tests

/*
        System.out.println("#######################################################################################");
        System.out.println(descriptorXml);
        System.out.println("#######################################################################################");
        System.out.println(serviceXml);
        System.out.println("#######################################################################################");
*/
    }

    @Test
    public void waitForRefresh() throws Exception {

        MockUpnpService upnpService = new MockUpnpService(true, true);

        LocalDevice ld =
                SampleData.createLocalDevice(
                        SampleData.createLocalDeviceIdentity(2)
                );
        
        upnpService.getRegistry().addDevice(ld);
        assertEquals(upnpService.getRegistry().getLocalDevices().size(), 1);

        Thread.sleep(5000);

        assertEquals(upnpService.getRegistry().getLocalDevices().size(), 1);

        assert upnpService.getOutgoingDatagramMessages().size() >= 60;
        for (UpnpMessage msg : upnpService.getOutgoingDatagramMessages()) {
            assertAliveMsgBasics(upnpService.getConfiguration().getNamespace(), msg, ld, 2);
        }

        upnpService.shutdown();
    }


    protected void assertAliveMsgBasics(Namespace namespace, UpnpMessage msg, LocalDevice device, Integer maxAge) {
        assertEquals(msg.getHeaders().getFirstHeader(UpnpHeader.Type.NTS).getValue(), NotificationSubtype.ALIVE);
        assertEquals(
                msg.getHeaders().getFirstHeader(UpnpHeader.Type.LOCATION).getValue().toString(),
                URIUtil.createAbsoluteURL(SampleData.getLocalBaseURL(), namespace.getDescriptorPath(device)).toString()
        );
        assertEquals(msg.getHeaders().getFirstHeader(UpnpHeader.Type.MAX_AGE).getValue(), maxAge);
        assertEquals(msg.getHeaders().getFirstHeader(UpnpHeader.Type.SERVER).getValue(), new ServerClientTokens());
    }

    @UpnpService(
            serviceId = @UpnpServiceId("SwitchPower"),
            serviceType = @UpnpServiceType(value = "SwitchPower", version = 1)
    )
    public static class DemoBinaryLight {

        private static LocalDevice createTestDevice() throws Exception {
            LocalServiceBinder binder = new AnnotationLocalServiceBinder();
            return new LocalDevice(
                    SampleData.createLocalDeviceIdentity(),
                    new UDADeviceType("BinaryLight", 1),
                    new DeviceDetails("Example Binary Light"),
                    binder.read(DemoBinaryLight.class)
            );
        }

        @UpnpStateVariable(defaultValue = "0", sendEvents = false)
        private boolean target = false;

        @UpnpStateVariable(defaultValue = "0")
        private boolean status = false;

        @UpnpAction
        public void setTarget(@UpnpInputArgument(name = "NewTargetValue") boolean newTargetValue) {
            target = newTargetValue;
            status = newTargetValue;
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "RetTargetValue"))
        public boolean getTarget() {
            return target;
        }

        @UpnpAction(out = {@UpnpOutputArgument(name = "ResultStatus")})
        public boolean getStatus() {
            return status;
        }

    }

}


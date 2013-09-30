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

import org.teleal.cling.binding.LocalServiceBinder;
import org.teleal.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.teleal.cling.binding.annotations.UpnpAction;
import org.teleal.cling.binding.annotations.UpnpOutputArgument;
import org.teleal.cling.binding.annotations.UpnpService;
import org.teleal.cling.binding.annotations.UpnpServiceId;
import org.teleal.cling.binding.annotations.UpnpServiceType;
import org.teleal.cling.binding.annotations.UpnpStateVariable;
import org.teleal.cling.model.meta.ActionArgument;
import org.teleal.cling.model.meta.DeviceDetails;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.types.Datatype;
import org.teleal.cling.model.types.UDADeviceType;
import org.teleal.cling.test.data.SampleData;
import org.teleal.common.util.ByteArray;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Random;

import static org.testng.Assert.assertEquals;

/**
 * @author Christian Bauer
 */
public class LocalServiceBindingDatatypesTest {

    public LocalDevice createTestDevice(LocalService service) throws Exception {
        return new LocalDevice(
                SampleData.createLocalDeviceIdentity(),
                new UDADeviceType("TestDevice", 1),
                new DeviceDetails("Test Device"),
                service
        );
    }

    @DataProvider(name = "devices")
    public Object[][] getDevices() throws Exception {

        // This is what we are actually testing
        LocalServiceBinder binder = new AnnotationLocalServiceBinder();

        return new LocalDevice[][]{
                {createTestDevice(binder.read(TestServiceOne.class))},
        };
    }

    @Test(dataProvider = "devices")
    public void validateBinding(LocalDevice device) {

        LocalService svc = SampleData.getFirstService(device);

        //System.out.println("############################################################################");
        //ServiceDescriptorBinder binder = new DefaultRouterConfiguration().getServiceDescriptorBinderUDA10();
        //System.out.println(binder.generate(svc));
        //System.out.println("############################################################################");

        assertEquals(svc.getStateVariables().length, 1);
        assertEquals(svc.getStateVariable("Data").getTypeDetails().getDatatype().getBuiltin(), Datatype.Builtin.BIN_BASE64);
        assertEquals(svc.getStateVariable("Data").getEventDetails().isSendEvents(), false);

        assertEquals(svc.getActions().length, 1);

        assertEquals(svc.getAction("GetData").getName(), "GetData");
        assertEquals(svc.getAction("GetData").getArguments().length, 1);
        assertEquals(svc.getAction("GetData").getArguments()[0].getName(), "RandomData");
        assertEquals(svc.getAction("GetData").getArguments()[0].getDirection(), ActionArgument.Direction.OUT);
        assertEquals(svc.getAction("GetData").getArguments()[0].getRelatedStateVariableName(), "Data");
        assertEquals(svc.getAction("GetData").getArguments()[0].isReturnValue(), true);

    }

    /* ####################################################################################################### */

    @UpnpService(
            serviceId = @UpnpServiceId("SomeService"),
            serviceType = @UpnpServiceType(value = "SomeService", version = 1),
            supportsQueryStateVariables = false
    )
    public static class TestServiceOne {

        public TestServiceOne() {
            byte[] rawData = new byte[8];
            new Random().nextBytes(rawData);
            data = ByteArray.toWrapper(rawData);
        }

        @UpnpStateVariable(sendEvents = false)
        private Byte[] data;

        @UpnpAction(out = @UpnpOutputArgument(name = "RandomData"))
        public Byte[] getData() {
            return data;
        }
    }


}

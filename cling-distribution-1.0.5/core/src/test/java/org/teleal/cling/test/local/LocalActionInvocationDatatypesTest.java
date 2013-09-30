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

import org.teleal.cling.binding.annotations.UpnpAction;
import org.teleal.cling.binding.annotations.UpnpOutputArgument;
import org.teleal.cling.binding.annotations.UpnpService;
import org.teleal.cling.binding.annotations.UpnpServiceId;
import org.teleal.cling.binding.annotations.UpnpServiceType;
import org.teleal.cling.binding.annotations.UpnpStateVariable;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.meta.DeviceDetails;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.types.ErrorCode;
import org.teleal.cling.model.types.UDADeviceType;
import org.teleal.cling.test.data.SampleData;
import org.teleal.common.util.ByteArray;
import org.testng.annotations.Test;

import java.util.Random;

import static org.testng.Assert.assertEquals;

/**
 * @author Christian Bauer
 */
public class LocalActionInvocationDatatypesTest {

    @Test
    public void invokeActions() throws Exception {

        LocalDevice device = new LocalDevice(
                SampleData.createLocalDeviceIdentity(),
                new UDADeviceType("SomeDevice", 1),
                new DeviceDetails("Some Device"),
                SampleData.readService(LocalTestServiceOne.class)
        );
        LocalService svc = SampleData.getFirstService(device);

        ActionInvocation getDataInvocation = new ActionInvocation(svc.getAction("GetData"));
        svc.getExecutor(getDataInvocation.getAction()).execute(getDataInvocation);
        assertEquals(getDataInvocation.getFailure(), null);
        assertEquals(getDataInvocation.getOutput().length, 1);
        assertEquals(((Byte[]) getDataInvocation.getOutput()[0].getValue()).length, 512);

        // This fails, we can't put arbitrary bytes into a String and hope it will be valid unicode characters!
        ActionInvocation getStringDataInvocation = new ActionInvocation(svc.getAction("GetDataString"));
        svc.getExecutor(getStringDataInvocation.getAction()).execute(getStringDataInvocation);
        assertEquals(getStringDataInvocation.getFailure().getErrorCode(), ErrorCode.ARGUMENT_VALUE_INVALID.getCode());
        assertEquals(
                getStringDataInvocation.getFailure().getMessage(),
                "The argument value is invalid. Wrong type or invalid value for 'RandomDataString': " +
                        "Invalid characters in string value (XML 1.0, section 2.2) produced by (StringDatatype)."
        );

        ActionInvocation invocation = new ActionInvocation(svc.getAction("GetStrings"));
        svc.getExecutor(invocation.getAction()).execute(invocation);
        assertEquals(invocation.getFailure(), null);
        assertEquals(invocation.getOutput().length, 2);
        assertEquals(invocation.getOutput("One").toString(), "foo");
        assertEquals(invocation.getOutput("Two").toString(), "bar");

        invocation = new ActionInvocation(svc.getAction("GetThree"));
        assertEquals(svc.getAction("GetThree").getOutputArguments()[0].getDatatype().getBuiltin().getDescriptorName(), "i2");
        svc.getExecutor(invocation.getAction()).execute(invocation);
        assertEquals(invocation.getFailure(), null);
        assertEquals(invocation.getOutput().length, 1);
        assertEquals(invocation.getOutput("three").toString(), "123");

        invocation = new ActionInvocation(svc.getAction("GetFour"));
        assertEquals(svc.getAction("GetFour").getOutputArguments()[0].getDatatype().getBuiltin().getDescriptorName(), "int");
        svc.getExecutor(invocation.getAction()).execute(invocation);
        assertEquals(invocation.getFailure(), null);
        assertEquals(invocation.getOutput().length, 1);
        assertEquals(invocation.getOutput("four").toString(), "456");

        invocation = new ActionInvocation(svc.getAction("GetFive"));
        assertEquals(svc.getAction("GetFive").getOutputArguments()[0].getDatatype().getBuiltin().getDescriptorName(), "int");
        svc.getExecutor(invocation.getAction()).execute(invocation);
        assertEquals(invocation.getFailure(), null);
        assertEquals(invocation.getOutput().length, 1);
        assertEquals(invocation.getOutput("five").toString(), "456");
    }

    @UpnpService(
            serviceId = @UpnpServiceId("SomeService"),
            serviceType = @UpnpServiceType(value = "SomeService", version = 1),
            supportsQueryStateVariables = false
    )
    public static class LocalTestServiceOne {

        @UpnpStateVariable(sendEvents = false)
        private Byte[] data;

        @UpnpStateVariable(sendEvents = false, datatype = "string")
        private String dataString;

        @UpnpStateVariable(sendEvents = false)
        private String one;

        @UpnpStateVariable(sendEvents = false)
        private String two;

        @UpnpStateVariable(sendEvents = false)
        private short three;

        @UpnpStateVariable(sendEvents = false, datatype = "int")
        private int four; // Defaults to "i4"

        public LocalTestServiceOne() {
            byte[] rawData = new byte[512];
            new Random().nextBytes(rawData);
            data = ByteArray.toWrapper(rawData);

            try {
                dataString = new String(ByteArray.toPrimitive(data), "UTF-8");
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        // This works and the Byte[] should not interfere with any Object[] handling in the executors
        @UpnpAction(out = @UpnpOutputArgument(name = "RandomData"))
        public Byte[] getData() {
            return data;
        }

        // This fails, we can't just put random data into a string
        @UpnpAction(out = @UpnpOutputArgument(name = "RandomDataString"))
        public String getDataString() {
            return dataString;
        }

        // We are testing _several_ output arguments returned in a bean, access through getters
        @UpnpAction(out = {
                @UpnpOutputArgument(name = "One", getterName = "getOne"),
                @UpnpOutputArgument(name = "Two", getterName = "getTwo")
        })
        public StringsHolder getStrings() {
            return new StringsHolder();
        }

        // Conversion of short into integer/UPnP "i2" datatype
        @UpnpAction(out = @UpnpOutputArgument(name = "three"))
        public short getThree() {
            return 123;
        }

        // Conversion of int into integer/UPnP "int" datatype
        @UpnpAction(out = @UpnpOutputArgument(name = "four"))
        public Integer getFour() {
            return 456;
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "five", stateVariable = "four"))
        public int getFive() {
            return 456;
        }
    }

    public static class StringsHolder {
        String one = "foo";
        String two = "bar";

        public String getOne() {
            return one;
        }

        public String getTwo() {
            return two;
        }
    }
}

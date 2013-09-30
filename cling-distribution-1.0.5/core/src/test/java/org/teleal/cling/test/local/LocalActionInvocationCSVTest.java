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

import org.teleal.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.teleal.cling.binding.annotations.UpnpAction;
import org.teleal.cling.binding.annotations.UpnpInputArgument;
import org.teleal.cling.binding.annotations.UpnpOutputArgument;
import org.teleal.cling.binding.annotations.UpnpService;
import org.teleal.cling.binding.annotations.UpnpServiceId;
import org.teleal.cling.binding.annotations.UpnpServiceType;
import org.teleal.cling.binding.annotations.UpnpStateVariable;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.meta.DeviceDetails;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.types.UDADeviceType;
import org.teleal.cling.model.types.UnsignedIntegerFourBytes;
import org.teleal.cling.model.types.csv.CSV;
import org.teleal.cling.model.types.csv.CSVBoolean;
import org.teleal.cling.model.types.csv.CSVInteger;
import org.teleal.cling.model.types.csv.CSVString;
import org.teleal.cling.model.types.csv.CSVUnsignedIntegerFourBytes;
import org.teleal.cling.test.data.SampleData;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class LocalActionInvocationCSVTest {

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
        return new LocalDevice[][]{
                {createTestDevice(
                        SampleData.readService(
                                new AnnotationLocalServiceBinder(), TestServiceOne.class
                        )
                )},
        };
    }

    @Test(dataProvider = "devices")
    public void invokeActions(LocalDevice device) throws Exception {

        LocalService svc = SampleData.getFirstService(device);

        List<String> testStrings = new CSVString();
        testStrings.add("f\\oo");
        testStrings.add("bar");
        testStrings.add("b,az");
        String result = executeActions(svc, "SetStringVar", "GetStringVar", testStrings);
        List<String> csvString = new CSVString(result);
        assert csvString.size() == 3;
        assertEquals(csvString.get(0), "f\\oo");
        assertEquals(csvString.get(1), "bar");
        assertEquals(csvString.get(2), "b,az");

        List<Integer> testIntegers = new CSVInteger();
        testIntegers.add(123);
        testIntegers.add(-456);
        testIntegers.add(789);
        result = executeActions(svc, "SetIntVar", "GetIntVar", testIntegers);
        List<Integer> csvInteger = new CSVInteger(result);
        assert csvInteger.size() == 3;
        assertEquals(csvInteger.get(0), new Integer(123));
        assertEquals(csvInteger.get(1), new Integer(-456));
        assertEquals(csvInteger.get(2), new Integer(789));

        List<Boolean> testBooleans = new CSVBoolean();
        testBooleans.add(true);
        testBooleans.add(true);
        testBooleans.add(false);
        result = executeActions(svc, "SetBooleanVar", "GetBooleanVar", testBooleans);
        List<Boolean> csvBoolean = new CSVBoolean(result);
        assert csvBoolean.size() == 3;
        assertEquals(csvBoolean.get(0), new Boolean(true));
        assertEquals(csvBoolean.get(1), new Boolean(true));
        assertEquals(csvBoolean.get(2), new Boolean(false));

        List<UnsignedIntegerFourBytes> testUifour = new CSVUnsignedIntegerFourBytes();
        testUifour.add(new UnsignedIntegerFourBytes(123));
        testUifour.add(new UnsignedIntegerFourBytes(456));
        testUifour.add(new UnsignedIntegerFourBytes(789));
        result = executeActions(svc, "SetUifourVar", "GetUifourVar", testUifour);
        List<UnsignedIntegerFourBytes> csvUifour = new CSVUnsignedIntegerFourBytes(result);
        assert csvUifour.size() == 3;
        assertEquals(csvUifour.get(0), new UnsignedIntegerFourBytes(123));
        assertEquals(csvUifour.get(1), new UnsignedIntegerFourBytes(456));
        assertEquals(csvUifour.get(2), new UnsignedIntegerFourBytes(789));
    }

    protected String executeActions(LocalService svc, String setAction, String getAction, List input) throws Exception {
        ActionInvocation setActionInvocation = new ActionInvocation(svc.getAction(setAction));
        setActionInvocation.setInput(svc.getAction(setAction).getFirstInputArgument().getName(), input.toString());
        svc.getExecutor(setActionInvocation.getAction()).execute(setActionInvocation);
        assertEquals(setActionInvocation.getFailure(), null);
        assertEquals(setActionInvocation.getOutput().length, 0);

        ActionInvocation getActionInvocation = new ActionInvocation(svc.getAction(getAction));
        svc.getExecutor(getActionInvocation.getAction()).execute(getActionInvocation);
        assertEquals(getActionInvocation.getFailure(), null);
        assertEquals(getActionInvocation.getOutput().length, 1);
        return getActionInvocation.getOutput(svc.getAction(getAction).getFirstOutputArgument()).toString();
    }


    /* ####################################################################################################### */


    @UpnpService(
            serviceId = @UpnpServiceId("TestService"),
            serviceType = @UpnpServiceType(value = "TestService", version = 1)
    )
    public static class TestServiceOne {

        @UpnpStateVariable(sendEvents = false)
        private CSV<String> stringVar;

        @UpnpStateVariable(sendEvents = false)
        private CSV<Integer> intVar;

        @UpnpStateVariable(sendEvents = false)
        private CSV<Boolean> booleanVar;

        @UpnpStateVariable(sendEvents = false)
        private CSV<UnsignedIntegerFourBytes> uifourVar;

        @UpnpAction
        public void setStringVar(@UpnpInputArgument(name = "StringVar") CSVString stringVar) {
            this.stringVar = stringVar;
            assertEquals(stringVar.size(), 3);
            assertEquals(stringVar.get(0), "f\\oo");
            assertEquals(stringVar.get(1), "bar");
            assertEquals(stringVar.get(2), "b,az");
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "StringVar"))
        public CSV<String> getStringVar() {
            return stringVar;
        }

        @UpnpAction
        public void setIntVar(@UpnpInputArgument(name = "IntVar") CSVInteger intVar) {
            this.intVar = intVar;
            assertEquals(intVar.size(), 3);
            assertEquals(intVar.get(0), new Integer(123));
            assertEquals(intVar.get(1), new Integer(-456));
            assertEquals(intVar.get(2), new Integer(789));
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "IntVar"))
        public CSV<Integer> getIntVar() {
            return intVar;
        }

        @UpnpAction
        public void setBooleanVar(@UpnpInputArgument(name = "BooleanVar") CSVBoolean booleanVar) {
            this.booleanVar = booleanVar;
            assertEquals(booleanVar.size(), 3);
            assertEquals(booleanVar.get(0), new Boolean(true));
            assertEquals(booleanVar.get(1), new Boolean(true));
            assertEquals(booleanVar.get(2), new Boolean(false));
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "BooleanVar"))
        public CSV<Boolean> getBooleanVar() {
            return booleanVar;
        }

        @UpnpAction
        public void setUifourVar(@UpnpInputArgument(name = "UifourVar") CSVUnsignedIntegerFourBytes uifourVar) {
            this.uifourVar = uifourVar;
            assertEquals(uifourVar.size(), 3);
            assertEquals(uifourVar.get(0), new UnsignedIntegerFourBytes(123));
            assertEquals(uifourVar.get(1), new UnsignedIntegerFourBytes(456));
            assertEquals(uifourVar.get(2), new UnsignedIntegerFourBytes(789));
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "UifourVar"))
        public CSV<UnsignedIntegerFourBytes> getUifourVar() {
            return uifourVar;
        }

    }

}
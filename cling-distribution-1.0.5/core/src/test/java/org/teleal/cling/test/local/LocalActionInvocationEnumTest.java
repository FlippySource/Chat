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
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.meta.DeviceDetails;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.types.UDADeviceType;
import org.teleal.cling.test.data.SampleData;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class LocalActionInvocationEnumTest {

    public LocalDevice createTestDevice(LocalService service) throws Exception {
        return new LocalDevice(
                SampleData.createLocalDeviceIdentity(),
                new UDADeviceType("BinaryLight", 1),
                new DeviceDetails("Example Binary Light"),
                service
        );
    }

    @DataProvider(name = "devices")
    public Object[][] getDevices() throws Exception {
        LocalServiceBinder binder = new AnnotationLocalServiceBinder();
        return new LocalDevice[][]{
                {createTestDevice(SampleData.readService(binder, TestServiceOne.class))},
                {createTestDevice(SampleData.readService(binder,TestServiceTwo.class))},
                {createTestDevice(SampleData.readService(binder, TestServiceThree.class))},
        };
    }

    @Test(dataProvider = "devices")
    public void invokeActions(LocalDevice device) throws Exception {

        LocalService svc = SampleData.getFirstService(device);

        ActionInvocation checkTargetInvocation = new ActionInvocation(svc.getAction("GetTarget"));
        svc.getExecutor(checkTargetInvocation.getAction()).execute(checkTargetInvocation);
        assertEquals(checkTargetInvocation.getFailure(), null);
        assertEquals(checkTargetInvocation.getOutput().length, 1);
        assertEquals(checkTargetInvocation.getOutput()[0].toString(), "UNKNOWN");

        ActionInvocation setTargetInvocation = new ActionInvocation(svc.getAction("SetTarget"));
        setTargetInvocation.setInput("NewTargetValue", "ON");
        svc.getExecutor(setTargetInvocation.getAction()).execute(setTargetInvocation);
        assertEquals(setTargetInvocation.getFailure(), null);
        assertEquals(setTargetInvocation.getOutput().length, 0);

        ActionInvocation getTargetInvocation = new ActionInvocation(svc.getAction("GetTarget"));
        svc.getExecutor(getTargetInvocation.getAction()).execute(getTargetInvocation);
        assertEquals(getTargetInvocation.getFailure(), null);
        assertEquals(getTargetInvocation.getOutput().length, 1);
        assertEquals(getTargetInvocation.getOutput()[0].toString(), "ON");

        ActionInvocation getStatusInvocation = new ActionInvocation(svc.getAction("GetStatus"));
        svc.getExecutor(getStatusInvocation.getAction()).execute(getStatusInvocation);
        assertEquals(getStatusInvocation.getFailure(), null);
        assertEquals(getStatusInvocation.getOutput().length, 1);
        assertEquals(getStatusInvocation.getOutput()[0].toString(), "1");

    }

    /* ####################################################################################################### */

    @UpnpService(
            serviceId = @UpnpServiceId("SwitchPower"),
            serviceType = @UpnpServiceType(value = "SwitchPower", version = 1)
    )
    public static class TestServiceOne {

        public enum Target {
            ON,
            OFF,
            UNKNOWN
        }

        @UpnpStateVariable(sendEvents = false)
        private Target target = Target.UNKNOWN;

        @UpnpStateVariable
        private boolean status = false;

        @UpnpAction
        public void setTarget(@UpnpInputArgument(name = "NewTargetValue") String newTargetValue) {
            target = Target.valueOf(newTargetValue);

            status = target == Target.ON;
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "RetTargetValue"))
        public Target getTarget() {
            return target;
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "ResultStatus"))
        public boolean getStatus() {
            return status;
        }
    }


    /* ####################################################################################################### */

    @UpnpService(
            serviceId = @UpnpServiceId("SwitchPower"),
            serviceType = @UpnpServiceType(value = "SwitchPower", version = 1)
    )
    public static class TestServiceTwo {

        public enum Target {
            ON,
            OFF,
            UNKNOWN
        }

        @UpnpStateVariable(sendEvents = false)
        private Target target = Target.UNKNOWN;

        @UpnpStateVariable
        private boolean status = false;

        @UpnpAction
        public void setTarget(@UpnpInputArgument(name = "NewTargetValue") String newTargetValue) {
            target = Target.valueOf(newTargetValue);

            status = target == Target.ON;
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "RetTargetValue", stateVariable = "Target", getterName = "getRealTarget"))
        public void getTarget() {
        }

        public Target getRealTarget() {
            return target;
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "ResultStatus"))
        public boolean getStatus() {
            return status;
        }
    }

    /* ####################################################################################################### */

    @UpnpService(
            serviceId = @UpnpServiceId("SwitchPower"),
            serviceType = @UpnpServiceType(value = "SwitchPower", version = 1)
    )
    public static class TestServiceThree {

        public enum Target {
            ON,
            OFF,
            UNKNOWN
        }

        public class TargetHolder {
            private Target t;

            public TargetHolder(Target t) {
                this.t = t;
            }

            public Target getTarget() {
                return t;
            }
        }

        @UpnpStateVariable(sendEvents = false)
        private Target target = Target.UNKNOWN;

        @UpnpStateVariable
        private boolean status = false;

        @UpnpAction
        public void setTarget(@UpnpInputArgument(name = "NewTargetValue") String newTargetValue) {
            target = Target.valueOf(newTargetValue);

            status = target == Target.ON;
        }

        @UpnpAction(name = "GetTarget", out = @UpnpOutputArgument(name = "RetTargetValue", getterName = "getTarget"))
        public TargetHolder getTargetHolder() {
            return new TargetHolder(target);
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "ResultStatus"))
        public boolean getStatus() {
            return status;
        }
    }

    /* ####################################################################################################### */

    @UpnpService(
            serviceId = @UpnpServiceId("SwitchPower"),
            serviceType = @UpnpServiceType(value = "SwitchPower", version = 1)
    )
    public static class TestServiceFour {

        public enum Target {
            ON,
            OFF,
            UNKNOWN
        }

        public class TargetHolder {
            private Target t;

            public TargetHolder(Target t) {
                this.t = t;
            }

            public Target getT() {
                return t;
            }
        }

        @UpnpStateVariable(sendEvents = false)
        private Target target = Target.UNKNOWN;

        @UpnpStateVariable
        private boolean status = false;

        @UpnpAction
        public void setTarget(@UpnpInputArgument(name = "NewTargetValue") String newTargetValue) {
            target = Target.valueOf(newTargetValue);

            status = target == Target.ON;
        }

        @UpnpAction(name = "GetTarget", out = @UpnpOutputArgument(name = "RetTargetValue", stateVariable = "Target", getterName = "getT"))
        public TargetHolder getTargetHolder() {
            return new TargetHolder(target);
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "ResultStatus"))
        public boolean getStatus() {
            return status;
        }
    }

}
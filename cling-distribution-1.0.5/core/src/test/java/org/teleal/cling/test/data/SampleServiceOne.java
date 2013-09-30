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

package org.teleal.cling.test.data;

import org.teleal.cling.model.meta.Action;
import org.teleal.cling.model.meta.ActionArgument;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.meta.StateVariable;
import org.teleal.cling.model.meta.StateVariableAllowedValueRange;
import org.teleal.cling.model.meta.StateVariableEventDetails;
import org.teleal.cling.model.meta.StateVariableTypeDetails;
import org.teleal.cling.model.types.Datatype;
import org.teleal.cling.model.types.ServiceId;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.model.types.UDAServiceId;
import org.teleal.cling.model.types.UDAServiceType;
import org.teleal.common.util.URIUtil;

import java.net.URI;
import java.net.URL;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Christian Bauer
 */
public class SampleServiceOne extends SampleService {

    public static URL getDescriptorURL() {
        return URIUtil.createAbsoluteURL(SampleDeviceRoot.getDeviceDescriptorURL(), getThisDescriptorURI());
    }

    public static URI getThisDescriptorURI() {
        return URI.create("service/upnp-org/MY-SERVICE-123/desc.xml");
    }

    public static URI getThisControlURI() {
        return URI.create("service/upnp-org/MY-SERVICE-123/control");
    }

    public static URI getThisEventSubscriptionURI() {
        return URI.create("service/upnp-org/MY-SERVICE-123/events");
    }

    public static ServiceId getThisServiceId() {
        return new UDAServiceId("MY-SERVICE-123");
    }

    public static ServiceType getThisServiceType() {
        return new UDAServiceType("MY-SERVICE-TYPE-ONE", 1);
    }

    @Override
    public ServiceType getServiceType() {
        return getThisServiceType();
    }

    @Override
    public ServiceId getServiceId() {
        return getThisServiceId();
    }

    @Override
    public URI getDescriptorURI() {
        return getThisDescriptorURI();
    }

    @Override
    public URI getControlURI() {
        return getThisControlURI();
    }

    @Override
    public URI getEventSubscriptionURI() {
        return getThisEventSubscriptionURI();
    }

    @Override
    public Action[] getActions() {
        return new Action[]{
                new Action(
                        "SetTarget",
                        new ActionArgument[]{
                                new ActionArgument("NewTargetValue", "Target", ActionArgument.Direction.IN)
                        }
                ),
                new Action(
                        "GetTarget",
                        new ActionArgument[]{
                                new ActionArgument("RetTargetValue", "Target", ActionArgument.Direction.OUT, true)
                        }
                ),
                new Action(
                        "GetStatus",
                        new ActionArgument[]{
                                new ActionArgument("ResultStatus", "Status", ActionArgument.Direction.OUT)
                        }
                )
        };
    }

    @Override
    public StateVariable[] getStateVariables() {
        return new StateVariable[]{
                new StateVariable(
                        "Target",
                        new StateVariableTypeDetails(Datatype.Builtin.BOOLEAN.getDatatype(), "0"),
                        new StateVariableEventDetails(false)
                ),
                new StateVariable(
                        "Status",
                        new StateVariableTypeDetails(Datatype.Builtin.BOOLEAN.getDatatype(), "0")
                ),
                new StateVariable(
                        "SomeVar",
                        new StateVariableTypeDetails(Datatype.Builtin.STRING.getDatatype(), "foo", new String[]{"foo", "bar"}, null)
                ),
                new StateVariable(
                        "AnotherVar",
                        new StateVariableTypeDetails(Datatype.Builtin.UI4.getDatatype(), null, null, new StateVariableAllowedValueRange(0, 10, 2)),
                        new StateVariableEventDetails(false)
                ),
                new StateVariable(
                        "ModeratedMaxRateVar",
                        new StateVariableTypeDetails(Datatype.Builtin.STRING.getDatatype()),
                        new StateVariableEventDetails(true, 500, 0)
                ),
                new StateVariable(
                        "ModeratedMinDeltaVar",
                        new StateVariableTypeDetails(Datatype.Builtin.I4.getDatatype()),
                        new StateVariableEventDetails(true, 0, 3)
                ),
        };
    }

    public static void assertMatch(Service a, Service b) {

        assertEquals(a.getActions().length, b.getActions().length);

        assertEquals(a.getAction("SetTarget").getName(), b.getAction("SetTarget").getName());
        assertEquals(a.getAction("SetTarget").getArguments().length, b.getAction("SetTarget").getArguments().length);
        assertEquals(a.getAction("SetTarget").getArguments()[0].getName(), a.getAction("SetTarget").getArguments()[0].getName());
        assertEquals(a.getAction("SetTarget").getArguments()[0].getDirection(), b.getAction("SetTarget").getArguments()[0].getDirection());
        assertEquals(a.getAction("SetTarget").getArguments()[0].getRelatedStateVariableName(), b.getAction("SetTarget").getArguments()[0].getRelatedStateVariableName());

        assertEquals(a.getAction("GetTarget").getArguments()[0].getName(), b.getAction("GetTarget").getArguments()[0].getName());
        assertEquals(a.getAction("GetTarget").getArguments()[0].isReturnValue(), b.getAction("GetTarget").getArguments()[0].isReturnValue());

        assertEquals(a.getStateVariables().length, b.getStateVariables().length);
        assertTrue(a.getStateVariable("Target") != null);
        assertTrue(b.getStateVariable("Target") != null);
        assertTrue(a.getStateVariable("Status") != null);
        assertTrue(b.getStateVariable("Status") != null);
        assertTrue(a.getStateVariable("SomeVar") != null);
        assertTrue(b.getStateVariable("SomeVar") != null);

        assertEquals(a.getStateVariable("Target").getName(), "Target");
        assertEquals(a.getStateVariable("Target").getEventDetails().isSendEvents(), b.getStateVariable("Target").getEventDetails().isSendEvents());

        assertEquals(a.getStateVariable("Status").getName(), "Status");
        assertEquals(a.getStateVariable("Status").getEventDetails().isSendEvents(), b.getStateVariable("Status").getEventDetails().isSendEvents());
        assertEquals(a.getStateVariable("Status").getTypeDetails().getDatatype(), Datatype.Builtin.BOOLEAN.getDatatype());

        assertEquals(a.getStateVariable("SomeVar").getTypeDetails().getAllowedValues().length, b.getStateVariable("SomeVar").getTypeDetails().getAllowedValues().length);
        assertEquals(a.getStateVariable("SomeVar").getTypeDetails().getDefaultValue(), b.getStateVariable("SomeVar").getTypeDetails().getDefaultValue());
        assertEquals(a.getStateVariable("SomeVar").getTypeDetails().getAllowedValues()[0], b.getStateVariable("SomeVar").getTypeDetails().getAllowedValues()[0]);
        assertEquals(a.getStateVariable("SomeVar").getTypeDetails().getAllowedValues()[1], b.getStateVariable("SomeVar").getTypeDetails().getAllowedValues()[1]);
        assertEquals(a.getStateVariable("SomeVar").getEventDetails().isSendEvents(), b.getStateVariable("SomeVar").getEventDetails().isSendEvents());

        assertEquals(a.getStateVariable("AnotherVar").getTypeDetails().getAllowedValueRange().getMinimum(), b.getStateVariable("AnotherVar").getTypeDetails().getAllowedValueRange().getMinimum());
        assertEquals(a.getStateVariable("AnotherVar").getTypeDetails().getAllowedValueRange().getMaximum(), b.getStateVariable("AnotherVar").getTypeDetails().getAllowedValueRange().getMaximum());
        assertEquals(a.getStateVariable("AnotherVar").getTypeDetails().getAllowedValueRange().getStep(), b.getStateVariable("AnotherVar").getTypeDetails().getAllowedValueRange().getStep());
        assertEquals(a.getStateVariable("AnotherVar").getEventDetails().isSendEvents(), b.getStateVariable("AnotherVar").getEventDetails().isSendEvents());
    }

}
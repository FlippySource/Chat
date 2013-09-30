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
import org.teleal.cling.model.meta.StateVariable;
import org.teleal.cling.model.types.ServiceId;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.model.types.UDAServiceId;
import org.teleal.cling.model.types.UDAServiceType;
import org.teleal.common.util.URIUtil;

import java.net.URI;
import java.net.URL;

/**
 * @author Christian Bauer
 */
public class SampleServiceThree extends SampleService {

    public static URI getThisDescriptorURI() {
        return URI.create("service/upnp-org/MY-SERVICE-789/desc.xml");
    }

    public static URL getDescriptorURL() {
        return URIUtil.createAbsoluteURL(SampleDeviceRoot.getDeviceDescriptorURL(), getThisDescriptorURI());
    }

    public static ServiceId getThisServiceId() {
        return new UDAServiceId("MY-SERVICE-789");
    }

    public static ServiceType getThisServiceType() {
        return new UDAServiceType("MY-SERVICE-TYPE-THREE", 3);
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
        return URI.create("service/upnp-org/MY-SERVICE-789/control");
    }

    @Override
    public URI getEventSubscriptionURI() {
        return URI.create("service/upnp-org/MY-SERVICE-789/events");
    }

    @Override
    public Action[] getActions() {
        return new Action[0];
    }

    @Override
    public StateVariable[] getStateVariables() {
        return new StateVariable[0];
    }

}

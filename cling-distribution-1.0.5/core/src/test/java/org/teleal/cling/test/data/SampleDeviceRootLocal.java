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

import org.teleal.cling.model.resource.DeviceDescriptorResource;
import org.teleal.cling.model.resource.IconResource;
import org.teleal.cling.model.resource.ServiceControlResource;
import org.teleal.cling.model.resource.ServiceDescriptorResource;
import org.teleal.cling.model.resource.ServiceEventSubscriptionResource;
import org.teleal.cling.model.resource.Resource;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.DeviceIdentity;
import org.teleal.cling.model.meta.LocalService;

import java.net.URI;

import static org.testng.Assert.assertEquals;

/**
 * @author Christian Bauer
 */
public class SampleDeviceRootLocal extends SampleDeviceRoot {

    public SampleDeviceRootLocal(DeviceIdentity identity, LocalService service, Device embeddedDevice) {
        super(identity, service, embeddedDevice);
    }

    public static void assertLocalResourcesMatch(Resource[] resources){
        assertEquals(
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-123/desc.xml")).getClass(),
                DeviceDescriptorResource.class
        );
        assertEquals(
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-123/icon.png")).getClass(),
                IconResource.class
        );
        assertEquals(
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-123/icon2.png")).getClass(),
                IconResource.class
        );
        assertEquals(
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-123/svc/upnp-org/MY-SERVICE-123/desc.xml")).getClass(),
                ServiceDescriptorResource.class
        );
        assertEquals(
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-123/svc/upnp-org/MY-SERVICE-123/action")).getClass(),
                ServiceControlResource.class
        );
        assertEquals(
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-123/svc/upnp-org/MY-SERVICE-123/event")).getClass(),
                ServiceEventSubscriptionResource.class
        );
        assertEquals(
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-456/icon3.png")).getClass(),
                IconResource.class
        );
        assertEquals(
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-456/svc/upnp-org/MY-SERVICE-456/desc.xml")).getClass(),
                ServiceDescriptorResource.class
        );
        assertEquals(
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-456/svc/upnp-org/MY-SERVICE-456/action")).getClass(),
                ServiceControlResource.class
        );
        assertEquals(
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-456/svc/upnp-org/MY-SERVICE-456/event")).getClass(),
                ServiceEventSubscriptionResource.class
        );
        assertEquals(
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-789/svc/upnp-org/MY-SERVICE-789/desc.xml")).getClass(),
                ServiceDescriptorResource.class
        );
        assertEquals(
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-789/svc/upnp-org/MY-SERVICE-789/action")).getClass(),
                ServiceControlResource.class
        );
        assertEquals(
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-789/svc/upnp-org/MY-SERVICE-789/event")).getClass(),
                ServiceEventSubscriptionResource.class
        );

    }

}

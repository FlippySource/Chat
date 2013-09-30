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

package org.teleal.cling.test.ssdp;

import org.teleal.cling.mock.MockUpnpService;
import org.teleal.cling.model.ExpirationDetails;
import org.teleal.cling.model.resource.Resource;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.test.data.SampleData;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.List;

public class RegistryExpirationTest {

    @Test
    public void addAndExpire() throws Exception {

        MockUpnpService upnpService = new MockUpnpService(false, true);

        RemoteDevice rd = SampleData.createRemoteDevice(
                SampleData.createRemoteDeviceIdentity(1)
        );
        upnpService.getRegistry().addDevice(rd);
        
        Assert.assertEquals(upnpService.getRegistry().getRemoteDevices().size(), 1);

        Thread.sleep(3000);

        Assert.assertEquals(upnpService.getRegistry().getRemoteDevices().size(), 0);

        upnpService.shutdown();
    }

    @Test
    public void addAndUpdateAndExpire() throws Exception {

        MockUpnpService upnpService = new MockUpnpService(false, true);

        RemoteDevice rd = SampleData.createRemoteDevice(
                SampleData.createRemoteDeviceIdentity(2)
        );

        // Add it to registry
        upnpService.getRegistry().addDevice(rd);
        Thread.sleep(1000);
        Assert.assertEquals(upnpService.getRegistry().getRemoteDevices().size(), 1);

        // Update it in registry
        upnpService.getRegistry().addDevice(rd);
        Thread.sleep(1000);
        Assert.assertEquals(upnpService.getRegistry().getRemoteDevices().size(), 1);

        // Update again
        upnpService.getRegistry().update(rd.getIdentity());
        Thread.sleep(1000);
        Assert.assertEquals(upnpService.getRegistry().getRemoteDevices().size(), 1);

        // Wait for expiration
        Thread.sleep(3000);
        Assert.assertEquals(upnpService.getRegistry().getRemoteDevices().size(), 0);


        upnpService.shutdown();
    }

    @Test
    public void addResourceAndExpire() throws Exception {

        MockUpnpService upnpService = new MockUpnpService(false, true);

        Resource resource = new Resource(URI.create("/this/is/a/test"), "foo");
        upnpService.getRegistry().addResource(resource, 2);

        Assert.assertEquals(upnpService.getRegistry().getResources().size(), 1);

        Thread.sleep(4000);

        Assert.assertEquals(upnpService.getRegistry().getResources().size(), 0);

        upnpService.shutdown();
    }

    @Test
    public void addResourceAndMaintain() throws Exception {

        MockUpnpService upnpService = new MockUpnpService(false, true);

        final TestRunnable testRunnable = new TestRunnable();

        Resource resource = new Resource<String>(URI.create("/this/is/a/test"), "foo") {
            @Override
            public void maintain(List<Runnable> pendingExecutions, ExpirationDetails expirationDetails) {
                if (expirationDetails.getSecondsUntilExpiration() == 1) {
                    pendingExecutions.add(testRunnable);
                }
            }
        };
        upnpService.getRegistry().addResource(resource, 2);

        Assert.assertEquals(upnpService.getRegistry().getResources().size(), 1);

        Thread.sleep(2000);

        Assert.assertEquals(testRunnable.wasExecuted, true);

        upnpService.shutdown();
    }

    protected class TestRunnable implements Runnable {
        boolean wasExecuted = false;

        public void run() {
            wasExecuted = true;
        }
    }

}

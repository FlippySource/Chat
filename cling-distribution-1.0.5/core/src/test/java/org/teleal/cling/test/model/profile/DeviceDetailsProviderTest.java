/*
 * Copyright (C) 2011 Teleal GmbH, Switzerland
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
package org.teleal.cling.test.model.profile;

import org.teleal.cling.model.meta.DeviceDetails;
import org.teleal.cling.model.profile.ControlPointInfo;
import org.teleal.cling.model.profile.HeaderDeviceDetailsProvider;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class DeviceDetailsProviderTest {

    @Test
    public void headerRegexMatch() throws Exception {

        ControlPointInfo controlPointInfo = new ControlPointInfo();

        DeviceDetails dd1 = new DeviceDetails("My Testdevice 1");
        DeviceDetails dd2 = new DeviceDetails("My Testdevice 2");

        Map<HeaderDeviceDetailsProvider.Key, DeviceDetails> headerDetails = new HashMap();

        headerDetails.put(new HeaderDeviceDetailsProvider.Key("User-Agent", "Xbox.*"), dd1);
        headerDetails.put(new HeaderDeviceDetailsProvider.Key("X-AV-Client-Info", ".*PLAYSTATION 3.*"), dd2);

        HeaderDeviceDetailsProvider provider = new HeaderDeviceDetailsProvider(dd1, headerDetails);

        // No match, test default behavior
        controlPointInfo.getHeaders().clear();
        controlPointInfo.getHeaders().add(
                "User-Agent",
                "Microsoft-Windows/6.1 UPnP/1.0 Windows-Media-Player-DMS/12.0.7600.16385 DLNADOC/1.50"
        );
        Assert.assertEquals(provider.provide(controlPointInfo), dd1);

        controlPointInfo.getHeaders().clear();
        controlPointInfo.getHeaders().add(
                "User-Agent",
                "UPnP/1.0"
        );
        controlPointInfo.getHeaders().add(
                "X-AV-Client-Info",
                "av=5.0; cn=\"Sony Computer Entertainment Inc.\"; mn=\"PLAYSTATION 3\"; mv=\"1.0\";"
        );
        Assert.assertEquals(provider.provide(controlPointInfo), dd2);

        controlPointInfo.getHeaders().clear();
        controlPointInfo.getHeaders().add(
                "User-Agent",
                "Xbox/2.0.4548.0 UPnP/1.0 Xbox/2.0.4548.0"
        );
        Assert.assertEquals(provider.provide(controlPointInfo), dd1);
    }
}

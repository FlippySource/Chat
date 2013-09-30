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

package example.igd;

import org.teleal.cling.UpnpService;
import org.teleal.cling.mock.MockUpnpService;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.types.UDAServiceId;
import org.teleal.cling.support.igd.callback.GetExternalIP;
import org.teleal.cling.support.model.Connection;
import org.teleal.cling.support.igd.callback.GetStatusInfo;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Getting connection information
 * <p>
 * The current connection information, including status, uptime, and last error message can be
 * retrieved from a <em>WAN*Connection</em> service with the following callback:
 * </p>
 * <a class="citation" href="javacode://this#testStatusInfo" style="include: DOC1; exclude: EXC1"/>
 * <p>
 * Additionally, a callback for obtaining the external IP address of a connection is available:
 * </p>
 * <a class="citation" href="javacode://this#testIPAddress" style="include: DOC1; exclude: EXC1"/>
 */
public class ConnectionInfoTest {

    @Test
    public void testStatusInfo() throws Exception {

        final boolean[] tests = new boolean[1];

        UpnpService upnpService = new MockUpnpService();

        LocalDevice device = IGDSampleData.createIGDevice(TestConnection.class);
        upnpService.getRegistry().addDevice(device);

        LocalService service = device.findService(new UDAServiceId("WANIPConnection"));         // DOC: DOC1

        upnpService.getControlPoint().execute(
            new GetStatusInfo(service) {

                @Override
                protected void success(Connection.StatusInfo statusInfo) {
                    assertEquals(statusInfo.getStatus(), Connection.Status.Connected);
                    assertEquals(statusInfo.getUptimeSeconds(), 1000);
                    assertEquals(statusInfo.getLastError(), Connection.Error.ERROR_NONE);
                    tests[0] = true;                                                        // DOC: EXC1
                }

                @Override
                public void failure(ActionInvocation invocation,
                                    UpnpResponse operation,
                                    String defaultMsg) {
                    // Something is wrong
                }
            }
        );                                                                                      // DOC: DOC1

        for (boolean test : tests) {
            assert test;
        }
        for (boolean test : ((LocalService<TestConnection>) service).getManager().getImplementation().tests) {
            assert test;
        }

    }

    @Test
    public void testIPAddress() throws Exception {

        final boolean[] tests = new boolean[1];

        UpnpService upnpService = new MockUpnpService();

        LocalDevice device = IGDSampleData.createIGDevice(TestConnection.class);
        upnpService.getRegistry().addDevice(device);

        LocalService service = device.findService(new UDAServiceId("WANIPConnection"));         // DOC: DOC1

        upnpService.getControlPoint().execute(
            new GetExternalIP(service) {

                @Override
                protected void success(String externalIPAddress) {
                    assertEquals(externalIPAddress, "123.123.123.123");
                    tests[0] = true;                                                        // DOC: EXC1
                }

                @Override
                public void failure(ActionInvocation invocation,
                                    UpnpResponse operation,
                                    String defaultMsg) {
                    // Something is wrong
                }
            }
        );                                                                                      // DOC: DOC1

        for (boolean test : tests) {
            assert test;
        }
        for (boolean test : ((LocalService<TestConnection>) service).getManager().getImplementation().tests) {
            assert test;
        }

    }
    public static class TestConnection extends IGDSampleData.WANIPConnectionService {

        boolean[] tests = new boolean[1];

        @Override
        public Connection.StatusInfo getStatusInfo() {
            tests[0] = true;
            return new Connection.StatusInfo(
                    Connection.Status.Connected,
                    1000,
                    Connection.Error.ERROR_NONE
            );
        }

        @Override
        public String getExternalIPAddress() {
            tests[0] = true;
            return "123.123.123.123";
        }
    }

}

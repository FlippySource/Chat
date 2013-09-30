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
import org.teleal.cling.registry.RegistryListener;
import org.teleal.cling.support.igd.callback.PortMappingAdd;
import org.teleal.cling.support.model.PortMapping;
import org.teleal.cling.support.igd.callback.PortMappingDelete;
import org.teleal.cling.support.igd.PortMappingListener;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Mapping a NAT port
 * <p>
 * Cling Support contains all the neccessary functionality, creating a port mapping
 * on all NAT routers on a network requires only three lines of code:
 * </p>
 * <a class="citation" href="javacode://this#addDeleteWithListener" style="include: PM1;"/>
 * <p>
 * The first line creates a port mapping configuration with the external/internal port, an
 * internal host IP, the protocol and an optional description.
 * </p>
 * <p>
 * The second line starts the UPnP service with a special listener. This listener
 * will add the port mapping on any <em>InternetGatewayDevice</em> with a <em>WANIPConnection</em>
 * or a <em>WANPPPConnection</em> service as soon as it is discovered. You should immediately start
 * a <code>ControlPoint#search()</code> for all devices on your network, this triggers a response
 * and discovery of all NAT routers, activating the port mapping.
 * </p>
 * <p>
 * The listener will also delete the port mapping when you stop the UPnP stack through
 * <code>UpnpService#shutdown()</code>, usually before your application quits. If you forget
 * to shutdown the stack the port mapping will remain on the <em>InternetGatewayDevice</em>
 * - the default lease duration is <code>0</code>!
 * </p>
 * <p>
 * If anything goes wrong, log messages with <code>WARNING</code> level will be created on the
 * category <code>org.teleal.cling.support.igd.PortMappingListener</code>. You can override the
 * <code>PortMappingListener#handleFailureMessage(String)</code> method to customize this behavior.
 * </p>
 * <p>
 * Alternatively, you can manually add and delete port mappings on an already discovered device with
 * the following ready-to-use action callbacks:
 * </p>
 * <a class="citation" href="javacode://this#addDeleteManually" style="include: PM1; exclude: EXC1, EXC2"/>
 *
 */
public class PortMappingTest {

    @Test
    public void addDeleteWithListener() throws Exception {

        PortMapping desiredMapping =                                    // DOC: PM1
                new PortMapping(
                        8123,
                        "192.168.0.123",
                        PortMapping.Protocol.TCP,
                        "My Port Mapping"
                );

        UpnpService upnpService =
                new UpnpServiceImpl(
                        new PortMappingListener(desiredMapping)
                );

        upnpService.getControlPoint().search();                         // DOC: PM1

        LocalDevice device = IGDSampleData.createIGDevice(TestConnection.class);
        upnpService.getRegistry().addDevice(device);

        upnpService.shutdown();

        LocalService<TestConnection> service = device.findService(new UDAServiceId("WANIPConnection"));
        for (boolean test : service.getManager().getImplementation().tests) {
            assert test;
        }

    }

    @Test
    public void addDeleteManually() throws Exception {

        final boolean[] tests = new boolean[2];

        PortMapping desiredMapping =
                new PortMapping(
                        8123,
                        "192.168.0.123",
                        PortMapping.Protocol.TCP,
                        "My Port Mapping"
                );

        UpnpService upnpService = new UpnpServiceImpl();

        LocalDevice device = IGDSampleData.createIGDevice(TestConnection.class);
        upnpService.getRegistry().addDevice(device);

        LocalService service = device.findService(new UDAServiceId("WANIPConnection"));         // DOC: PM1

        upnpService.getControlPoint().execute(
            new PortMappingAdd(service, desiredMapping) {

                @Override
                public void success(ActionInvocation invocation) {
                    // All OK
                    tests[0] = true;                                                        // DOC: EXC1
                }

                @Override
                public void failure(ActionInvocation invocation,
                                    UpnpResponse operation,
                                    String defaultMsg) {
                    // Something is wrong
                }
            }
        );

        upnpService.getControlPoint().execute(
            new PortMappingDelete(service, desiredMapping) {

                @Override
                public void success(ActionInvocation invocation) {
                    // All OK
                    tests[1] = true;                                                        // DOC: EXC2
                }

                @Override
                public void failure(ActionInvocation invocation,
                                    UpnpResponse operation,
                                    String defaultMsg) {
                    // Something is wrong
                }
            }
        );                                                                                      // DOC: PM1

        for (boolean test : tests) {
            assert test;
        }
        for (boolean test : ((LocalService<TestConnection>)service).getManager().getImplementation().tests) {
            assert test;
        }

    }

    public static class TestConnection extends IGDSampleData.WANIPConnectionService {

        boolean[] tests = new boolean[2];

        @Override
        protected void addPortMapping(PortMapping portMapping) {
            assertEquals(portMapping.getExternalPort().getValue(), new Long(8123));
            assertEquals(portMapping.getInternalPort().getValue(), new Long(8123));
            assertEquals(portMapping.getProtocol(), PortMapping.Protocol.TCP);
            assertEquals(portMapping.getDescription(), "My Port Mapping");
            assertEquals(portMapping.getInternalClient(), "192.168.0.123");
            assertEquals(portMapping.getLeaseDurationSeconds().getValue(), new Long(0));
            assertEquals(portMapping.hasRemoteHost(), false);
            assertEquals(portMapping.hasDescription(), true);
            tests[0] = true;
        }

        @Override
        protected void deletePortMapping(PortMapping portMapping) {
            assertEquals(portMapping.getExternalPort().getValue(), new Long(8123));
            assertEquals(portMapping.getProtocol(), PortMapping.Protocol.TCP);
            assertEquals(portMapping.hasRemoteHost(), false);
            tests[1] = true;
        }
    }

    class UpnpServiceImpl extends MockUpnpService {
        UpnpServiceImpl(RegistryListener... registryListeners) {
            super();
            for (RegistryListener registryListener : registryListeners) {
                getRegistry().addListener(registryListener);
            }
        }
    }

}

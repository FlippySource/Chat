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

package example.registry;

import org.teleal.cling.mock.MockUpnpService;
import org.teleal.cling.model.message.StreamResponseMessage;
import org.teleal.cling.model.message.header.ContentTypeHeader;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.meta.RemoteService;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.profile.ControlPointInfo;
import org.teleal.cling.model.types.UDAServiceId;
import org.teleal.cling.protocol.RetrieveRemoteDescriptors;
import org.teleal.cling.registry.DefaultRegistryListener;
import org.teleal.cling.registry.Registry;
import org.teleal.cling.test.data.SampleData;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Listening to registry changes
 * <p>
 * The <code>RegistryListener</code> is your primary API when discovering devices and services with your
 * control point. UPnP operates asynchronous, so advertisements (either <em>alive</em> or <em>byebye</em>)
 * of devices can occur at any time. Responses to your network search messages are also asynchronous.
 * </p>
 * <p>
 * This is the interface:
 * </p>
 * <a class="citation" href="javacode://example.registry.RegistryListenerTest.RegistryListener"/>
 * <p>
 * Typically you don't want to implement all of these methods. Some are only useful if you write
 * a service or a generic control point. Most of the time you want to be notified when a particular
 * device with a particular service appears on your network. So it is much easier to extend
 * the <code>DefaultRegistryListener</code>, which has empty implementations for all methods of
 * the interface, and only override the methods you need.
 * </p>
 * <a class="citation" href="javadoc://this#quickstartListener" style="read-title: false"/>
 * <a class="citation" href="javadoc://this#regularListener" style="read-title: false"/>
 */
public class RegistryListenerTest {

    // Just for documentation inclusion!
    public interface RegistryListener {

        public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device);
        public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception ex);

        public void remoteDeviceAdded(Registry registry, RemoteDevice device);
        public void remoteDeviceUpdated(Registry registry, RemoteDevice device);
        public void remoteDeviceRemoved(Registry registry, RemoteDevice device);

        public void localDeviceAdded(Registry registry, LocalDevice device);
        public void localDeviceRemoved(Registry registry, LocalDevice device);

    }

    /**
     * <p>
     * The <code>remoteDeviceDiscoveryStarted()</code> and <code>remoteDeviceDiscoveryFailed()</code>
     * methods are completely optional but useful on slow machines (such as Android handsets). Cling
     * will retrieve and initialize all device metadata for each UPnP device before it will announce
     * it on the <code>Registry</code>. UPnP metadata is split into several XML descriptors, so retrieval
     * via HTTP of these descriptors, parsing, and validating all metadata for a complex UPnP device
     * and service model can take several seconds. These two methods allow you to access the device
     * as soon as possible, after the first descriptor has been retrieved and parsed. At this time
     * the services metadata is however not available:
     * </p>
     * <a class="citation" href="javacode://example.registry.RegistryListenerTest.QuickstartRegistryListener" style="exclude: EXC1, EXC2;"/>
     * <p>
     * This is how you register and activate a listener:
     * </p>
     * <a class="citation" href="javacode://this" style="include: INC1"/>
     */
    @Test
    public void quickstartListener() throws Exception {

        final RemoteDevice discoveredDevice = new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        final RemoteDevice hydratedDevice = SampleData.createRemoteDevice();

        MockUpnpService upnpService = new MockUpnpService() {
            @Override
            public StreamResponseMessage[] getStreamResponseMessages() {
                try {
                    String deviceDescriptorXML =
                            getConfiguration().getDeviceDescriptorBinderUDA10().generate(
                                    hydratedDevice,
                                    new ControlPointInfo(),
                                    getConfiguration().getNamespace()
                            );
                    String serviceOneXML =
                            getConfiguration().getServiceDescriptorBinderUDA10().generate(hydratedDevice.findServices()[0]);
                    String serviceTwoXML =
                            getConfiguration().getServiceDescriptorBinderUDA10().generate(hydratedDevice.findServices()[1]);
                    String serviceThreeXML =
                            getConfiguration().getServiceDescriptorBinderUDA10().generate(hydratedDevice.findServices()[2]);
                    return new StreamResponseMessage[] {
                            new StreamResponseMessage(deviceDescriptorXML, ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8),
                            new StreamResponseMessage(serviceOneXML, ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8),
                            new StreamResponseMessage(serviceTwoXML, ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8),
                            new StreamResponseMessage(serviceThreeXML, ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8)
                    };
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        };

        QuickstartRegistryListener listener = new QuickstartRegistryListener(); // DOC: INC1
        upnpService.getRegistry().addListener(listener);                        // DOC: INC1

        RetrieveRemoteDescriptors retrieveDescriptors = new RetrieveRemoteDescriptors(upnpService, discoveredDevice);
        retrieveDescriptors.run();

        assertEquals(listener.valid, true);
    }

    @Test
    public void failureQuickstartListener() throws Exception {

        final RemoteDevice discoveredDevice = new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        final RemoteDevice hydratedDevice = SampleData.createRemoteDevice();

        MockUpnpService upnpService = new MockUpnpService() {
            @Override
            public StreamResponseMessage[] getStreamResponseMessages() {
                try {
                    String deviceDescriptorXML =
                            getConfiguration().getDeviceDescriptorBinderUDA10().generate(
                                    hydratedDevice,
                                    new ControlPointInfo(),
                                    getConfiguration().getNamespace()
                            );
                    return new StreamResponseMessage[] {
                            new StreamResponseMessage(deviceDescriptorXML, ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8),
                            null,
                            null,
                            null // Don't return any service descriptors, make it fail
                    };
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        };

        FailureQuickstartRegistryListener listener = new FailureQuickstartRegistryListener();
        upnpService.getRegistry().addListener(listener);

        RetrieveRemoteDescriptors retrieveDescriptors = new RetrieveRemoteDescriptors(upnpService, discoveredDevice);
        retrieveDescriptors.run();

        assertEquals(listener.valid, true);
    }

    public class QuickstartRegistryListener extends DefaultRegistryListener {
        public boolean valid = false; // DOC: EXC1

        @Override
        public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {

            // You can already use the device here and you can see which services it will have
            assertEquals(device.findServices().length, 3);

            // But you can't use the services
            for (RemoteService service: device.findServices()) {
                assertEquals(service.getActions().length, 0);
                assertEquals(service.getStateVariables().length, 0);
            }
            valid = true; // DOC: EXC2
        }

        @Override
        public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception ex) {
            // You might want to drop the device, its services couldn't be hydrated
        }
    }

    public class FailureQuickstartRegistryListener extends DefaultRegistryListener {
        public boolean valid = false;

        @Override
        public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception ex) {
            valid = true;
        }
    }

    /**
     * <p>
     * Most of the time, on any device that is faster than a cellphone, your listeners will look
     * like this:
     * </p>
     * <a class="citation" href="javacode://example.registry.RegistryListenerTest.MyListener" style="exclude: EXC1, EXC2, EXC3;"/>
     * <p>
     * The device metadata of the parameter to <code>remoteDeviceAdded()</code> is fully hydrated, all
     * of its services, actions, and state variables are available. You can continue with this metadata,
     * writing action invocations and event monitoring callbacks. You also might want to react accordingly
     * when the device disappears from the network.
     * </p>
     */
    @Test
    public void regularListener() throws Exception {

        final RemoteDevice discoveredDevice = new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        final RemoteDevice hydratedDevice = SampleData.createRemoteDevice();

        MockUpnpService upnpService = new MockUpnpService() {
            @Override
            public StreamResponseMessage[] getStreamResponseMessages() {
                try {
                    String deviceDescriptorXML =
                            getConfiguration().getDeviceDescriptorBinderUDA10().generate(
                                    hydratedDevice,
                                    new ControlPointInfo(),
                                    getConfiguration().getNamespace()
                            );
                    String serviceOneXML =
                            getConfiguration().getServiceDescriptorBinderUDA10().generate(hydratedDevice.findServices()[0]);
                    String serviceTwoXML =
                            getConfiguration().getServiceDescriptorBinderUDA10().generate(hydratedDevice.findServices()[1]);
                    String serviceThreeXML =
                            getConfiguration().getServiceDescriptorBinderUDA10().generate(hydratedDevice.findServices()[2]);
                    return new StreamResponseMessage[] {
                            new StreamResponseMessage(deviceDescriptorXML, ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8),
                            new StreamResponseMessage(serviceOneXML, ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8),
                            new StreamResponseMessage(serviceTwoXML, ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8),
                            new StreamResponseMessage(serviceThreeXML, ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8)
                    };
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        };

        MyListener listener = new MyListener();
        upnpService.getRegistry().addListener(listener);

        RetrieveRemoteDescriptors retrieveDescriptors = new RetrieveRemoteDescriptors(upnpService, discoveredDevice);
        retrieveDescriptors.run();

        upnpService.getRegistry().removeAllRemoteDevices();

        assertEquals(listener.added, true);
        assertEquals(listener.removed, true);
    }

    public class MyListener extends DefaultRegistryListener {
        public boolean added = false; // DOC: EXC1
        public boolean removed = false; // DOC: EXC1

        @Override
        public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
            Service myService = device.findService(new UDAServiceId("MY-SERVICE-123"));
            if (myService != null) {
                // Do something with the discovered service
                added = true; // DOC: EXC2
            }
        }

        @Override
        public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
            // Stop using the service if this is the same device, it's gone now
            removed = true; // DOC: EXC3
        }
    }

}

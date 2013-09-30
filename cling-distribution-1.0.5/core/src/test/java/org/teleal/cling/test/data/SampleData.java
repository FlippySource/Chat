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

import org.teleal.cling.DefaultUpnpServiceConfiguration;
import org.teleal.cling.binding.LocalServiceBinder;
import org.teleal.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.teleal.cling.model.DefaultServiceManager;
import org.teleal.cling.model.message.OutgoingDatagramMessage;
import org.teleal.cling.model.meta.Action;
import org.teleal.cling.model.meta.DeviceDetails;
import org.teleal.cling.model.meta.DeviceIdentity;
import org.teleal.cling.model.meta.Icon;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.meta.RemoteDeviceIdentity;
import org.teleal.cling.model.meta.RemoteService;
import org.teleal.cling.model.meta.StateVariable;
import org.teleal.cling.model.types.DeviceType;
import org.teleal.cling.model.types.ServiceId;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.transport.impl.NetworkAddressFactoryImpl;
import org.teleal.cling.transport.spi.DatagramProcessor;

import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import org.teleal.cling.model.profile.DeviceDetailsProvider;


public class SampleData {

    private static Logger log = Logger.getLogger(SampleData.class.getName());

    /* ###################################################################################### */

    public static InetAddress getLocalBaseAddress() {
        try {
            return InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static URL getLocalBaseURL() {
        try {
            return new URL("http://127.0.0.1:" + NetworkAddressFactoryImpl.DEFAULT_TCP_HTTP_LISTEN_PORT + "/");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /* ###################################################################################### */

    public static DeviceIdentity createLocalDeviceIdentity() {
        return createLocalDeviceIdentity(1800);
    }

    public static DeviceIdentity createLocalDeviceIdentity(int maxAgeSeconds) {
        return new DeviceIdentity(SampleDeviceRoot.getRootUDN(), maxAgeSeconds);
    }

    public static LocalDevice createLocalDevice() {
        return createLocalDevice(false);
    }

    public static LocalDevice createLocalDevice(boolean useProvider) {
        return createLocalDevice(createLocalDeviceIdentity(), useProvider);
    }

    public static Constructor<LocalDevice> getLocalDeviceConstructor() {
        try {
            return LocalDevice.class.getConstructor(
                    DeviceIdentity.class, DeviceType.class, DeviceDetails.class,
                    Icon[].class, LocalService.class, LocalDevice.class
            );
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Constructor<LocalDevice> getLocalDeviceWithProviderConstructor() {
        try {
            return LocalDevice.class.getConstructor(
                    DeviceIdentity.class, DeviceType.class, DeviceDetailsProvider.class,
                    Icon[].class, LocalService.class, LocalDevice.class
            );
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Constructor<LocalService> getLocalServiceConstructor() {
        try {
            return LocalService.class.getConstructor(
                    ServiceType.class, ServiceId.class,
                    Action[].class, StateVariable[].class
            );
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static LocalDevice createLocalDevice(DeviceIdentity identity) {
        return createLocalDevice(identity, false);
    }

    public static LocalDevice createLocalDevice(DeviceIdentity identity, boolean useProvider) {
        try {

            Constructor<LocalDevice> ctor =
                    useProvider
                            ? getLocalDeviceWithProviderConstructor()
                            : getLocalDeviceConstructor();

            Constructor<LocalService> serviceConstructor = getLocalServiceConstructor();

            return new SampleDeviceRootLocal(
                    identity,
                    new SampleServiceOne().newInstanceLocal(serviceConstructor),
                    new SampleDeviceEmbeddedOne(
                            new DeviceIdentity(SampleDeviceEmbeddedOne.getEmbeddedOneUDN(), identity),
                            new SampleServiceTwo().newInstanceLocal(serviceConstructor),
                            new SampleDeviceEmbeddedTwo(
                                    new DeviceIdentity(SampleDeviceEmbeddedTwo.getEmbeddedTwoUDN(), identity),
                                    new SampleServiceThree().newInstanceLocal(serviceConstructor),
                                    null
                            ).newInstance(ctor, useProvider)
                    ).newInstance(ctor, useProvider)
            ).newInstance(ctor, useProvider);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static LocalService getFirstService(LocalDevice device) {
        return device.getServices()[0];
    }

    /* ###################################################################################### */

    public static RemoteDeviceIdentity createRemoteDeviceIdentity() {
        return createRemoteDeviceIdentity(1800);
    }

    public static RemoteDeviceIdentity createRemoteDeviceIdentity(int maxAgeSeconds) {
        return new RemoteDeviceIdentity(
                SampleDeviceRoot.getRootUDN(),
                maxAgeSeconds,
                SampleDeviceRoot.getDeviceDescriptorURL(),
                null,
                getLocalBaseAddress()
        );
    }

    public static RemoteDevice createRemoteDevice() {
        return createRemoteDevice(createRemoteDeviceIdentity());
    }

    public static Constructor<RemoteDevice> getRemoteDeviceConstructor() {
        try {
            return RemoteDevice.class.getConstructor(
                    RemoteDeviceIdentity.class, DeviceType.class, DeviceDetails.class,
                    Icon[].class, RemoteService.class, RemoteDevice.class
            );
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Constructor<RemoteService> getRemoteServiceConstructor() {
        try {
            return RemoteService.class.getConstructor(
                    ServiceType.class, ServiceId.class,
                    URI.class, URI.class, URI.class,
                    Action[].class, StateVariable[].class
            );
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static RemoteDevice createRemoteDevice(RemoteDeviceIdentity identity) {
        try {

            Constructor<RemoteDevice> ctor = getRemoteDeviceConstructor();
            Constructor<RemoteService> serviceConstructor = getRemoteServiceConstructor();

            return new SampleDeviceRoot(
                    identity,
                    new SampleServiceOne().newInstanceRemote(serviceConstructor),
                    new SampleDeviceEmbeddedOne(
                            new RemoteDeviceIdentity(SampleDeviceEmbeddedOne.getEmbeddedOneUDN(), identity),
                            new SampleServiceTwo().newInstanceRemote(serviceConstructor),
                            new SampleDeviceEmbeddedTwo(
                                    new RemoteDeviceIdentity(SampleDeviceEmbeddedTwo.getEmbeddedTwoUDN(), identity),
                                    new SampleServiceThree().newInstanceRemote(serviceConstructor),
                                    null
                            ).newInstance(ctor)
                    ).newInstance(ctor)
            ).newInstance(ctor);

        } catch (Exception e) {
/*
            Throwable cause = Exceptions.unwrap(e);
            if (cause instanceof ValidationException) {
                ValidationException ex = (ValidationException) cause;
                for (ValidationError validationError : ex.getErrors()) {
                    log.severe(validationError.toString());
                }
            }
*/
            throw new RuntimeException(e);
        }
    }

    public static RemoteService getFirstService(RemoteDevice device) {
        return device.getServices()[0];
    }

    public static RemoteService createUndescribedRemoteService() {
        RemoteService service =
                new SampleServiceOneUndescribed().newInstanceRemote(SampleData.getRemoteServiceConstructor());
        new SampleDeviceRoot(
                SampleData.createRemoteDeviceIdentity(),
                service,
                null
        ).newInstance(SampleData.getRemoteDeviceConstructor());
        return service;
    }

    /* ###################################################################################### */

    public static <T> LocalService<T> readService(Class<T> clazz) {
        return readService(new AnnotationLocalServiceBinder().read(clazz), clazz);
    }

    public static <T> LocalService<T> readService(LocalServiceBinder binder, Class<T> clazz) {
        return readService(binder.read(clazz), clazz);
    }

    public static <T> LocalService<T> readService(LocalService<T> service, Class<T> clazz) {
        service.setManager(
                new DefaultServiceManager(service, clazz)
        );
        return service;
    }

    /* ###################################################################################### */

    /*   public static void assertTestDataMatchServiceTwo(Service svc) {

            Service<DeviceService> service = svc;

            Device sampleDevice = (service.getDeviceService().getDevice().isLocal()) ? getLocalDevice() : getRemoteDevice();
            Service<DeviceService> sampleService = getServiceTwo((Device) sampleDevice.getEmbeddedDevices().get(0));

            assertEquals(service.getActions().size(), sampleService.getActions().size());

            assertEquals(service.getActions().get("GetFoo").getName(), sampleService.getActions().get("GetFoo").getName());
            assertEquals(service.getActions().get("GetFoo").getArguments().size(), sampleService.getActions().get("GetFoo").getArguments().size());
            assertEquals(service.getActions().get("GetFoo").getArguments().get(0).getName(), service.getActions().get("GetFoo").getArguments().get(0).getName());
            assertEquals(service.getActions().get("GetFoo").getArguments().get(0).getDirection(), sampleService.getActions().get("GetFoo").getArguments().get(0).getDirection());
            assertEquals(service.getActions().get("GetFoo").getArguments().get(0).getRelatedStateVariableName(), sampleService.getActions().get("GetFoo").getArguments().get(0).getRelatedStateVariableName());

            assertEquals(service.getStateVariables().size(), sampleService.getStateVariables().size());
            assertTrue(service.getStateVariables().containsKey("Foo"));

            assertEquals(service.getStateVariables().get("Foo").getName(), "Foo");
            assertTrue(service.getStateVariables().get("Foo").isSendEvents());
            assertEquals(service.getStateVariables().get("Foo").getDatatype(), Datatype.Builtin.BOOLEAN.getDatatype());

        }

        public static void assertTestDataMatchServiceThree(Service svc) {
            assertTestDataMatchServiceTwo(svc);
        }
    */

    public static void debugMsg(OutgoingDatagramMessage msg) {
        DatagramProcessor proc = new DefaultUpnpServiceConfiguration().getDatagramProcessor();
        proc.write(msg);
    }


}

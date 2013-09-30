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

import org.teleal.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.teleal.cling.binding.annotations.UpnpAction;
import org.teleal.cling.binding.annotations.UpnpInputArgument;
import org.teleal.cling.binding.annotations.UpnpOutputArgument;
import org.teleal.cling.binding.annotations.UpnpService;
import org.teleal.cling.binding.annotations.UpnpServiceId;
import org.teleal.cling.binding.annotations.UpnpServiceType;
import org.teleal.cling.binding.annotations.UpnpStateVariable;
import org.teleal.cling.binding.annotations.UpnpStateVariables;
import org.teleal.cling.model.DefaultServiceManager;
import org.teleal.cling.model.action.ActionException;
import org.teleal.cling.model.meta.DeviceDetails;
import org.teleal.cling.model.meta.DeviceIdentity;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.types.ErrorCode;
import org.teleal.cling.model.types.UDADeviceType;
import org.teleal.cling.model.types.UDN;
import org.teleal.cling.model.types.UnsignedIntegerFourBytes;
import org.teleal.cling.model.types.UnsignedIntegerTwoBytes;
import org.teleal.cling.support.model.Connection;
import org.teleal.cling.support.model.PortMapping;

/**
 * @author Christian Bauer
 */
public class IGDSampleData {

    public static LocalService readService(Class<?> serviceClass) throws Exception {
        LocalService service = new AnnotationLocalServiceBinder().read(serviceClass);
        service.setManager(
                new DefaultServiceManager(service, serviceClass)
        );
        return service;
    }

    public static LocalDevice createIGDevice(Class<?> serviceClass) throws Exception {
        return createIGDevice(
                null,
                new LocalDevice[]{
                        createWANDevice(
                                null,
                                new LocalDevice[]{
                                        createWANConnectionDevice(new LocalService[]{readService(serviceClass)}, null)
                                }
                        )
                });
    }

    public static LocalDevice createIGDevice(LocalService[] services, LocalDevice[] embedded) throws Exception {
        return new LocalDevice(
                new DeviceIdentity(new UDN("1111")),
                new UDADeviceType("InternetGatewayDevice", 1),
                new DeviceDetails("Example Router"),
                services,
                embedded
        );
    }

    public static LocalDevice createWANDevice(LocalService[] services, LocalDevice[] embedded) throws Exception {
        return new LocalDevice(
                new DeviceIdentity(new UDN("2222")),
                new UDADeviceType("WANDevice", 1),
                new DeviceDetails("Example WAN Device"),
                services,
                embedded
        );
    }

    public static LocalDevice createWANConnectionDevice(LocalService[] services, LocalDevice[] embedded) throws Exception {
        return new LocalDevice(
                new DeviceIdentity(new UDN("3333")),
                new UDADeviceType("WANConnectionDevice", 1),
                new DeviceDetails("Example WAN Connection Device"),
                services,
                embedded
        );
    }

    @UpnpService(
            serviceId = @UpnpServiceId("WANIPConnection"),
            serviceType = @UpnpServiceType("WANIPConnection")
    )
    @UpnpStateVariables({
            @UpnpStateVariable(name = "RemoteHost", datatype = "string", sendEvents = false),
            @UpnpStateVariable(name = "ExternalPort", datatype = "ui2", sendEvents = false),
            @UpnpStateVariable(name = "PortMappingProtocol", datatype = "string", sendEvents = false, allowedValuesEnum = PortMapping.Protocol.class),
            @UpnpStateVariable(name = "InternalPort", datatype = "ui2", sendEvents = false),
            @UpnpStateVariable(name = "InternalClient", datatype = "string", sendEvents = false),
            @UpnpStateVariable(name = "PortMappingEnabled", datatype = "boolean", sendEvents = false),
            @UpnpStateVariable(name = "PortMappingDescription", datatype = "string", sendEvents = false),
            @UpnpStateVariable(name = "PortMappingLeaseDuration", datatype = "ui4", sendEvents = false),
            @UpnpStateVariable(name = "ConnectionStatus", datatype = "string", sendEvents = false),
            @UpnpStateVariable(name = "LastConnectionError", datatype = "string", sendEvents = false),
            @UpnpStateVariable(name = "Uptime", datatype = "ui4", sendEvents = false),
            @UpnpStateVariable(name = "ExternalIPAddress", datatype = "string", sendEvents = false)

    })
    public static class WANIPConnectionService {

        @UpnpAction
        public void addPortMapping(
                @UpnpInputArgument(name = "NewRemoteHost", stateVariable = "RemoteHost") String remoteHost,
                @UpnpInputArgument(name = "NewExternalPort", stateVariable = "ExternalPort") UnsignedIntegerTwoBytes externalPort,
                @UpnpInputArgument(name = "NewProtocol", stateVariable = "PortMappingProtocol") String protocol,
                @UpnpInputArgument(name = "NewInternalPort", stateVariable = "InternalPort") UnsignedIntegerTwoBytes internalPort,
                @UpnpInputArgument(name = "NewInternalClient", stateVariable = "InternalClient") String internalClient,
                @UpnpInputArgument(name = "NewEnabled", stateVariable = "PortMappingEnabled") Boolean enabled,
                @UpnpInputArgument(name = "NewPortMappingDescription", stateVariable = "PortMappingDescription") String description,
                @UpnpInputArgument(name = "NewLeaseDuration", stateVariable = "PortMappingLeaseDuration") UnsignedIntegerFourBytes leaseDuration
        ) throws ActionException {
            try {
                addPortMapping(new PortMapping(
                        enabled,
                        leaseDuration,
                        remoteHost,
                        externalPort,
                        internalPort,
                        internalClient,
                        PortMapping.Protocol.valueOf(protocol),
                        description
                ));
            } catch (Exception ex) {
                throw new ActionException(ErrorCode.ACTION_FAILED, "Can't convert port mapping: " + ex.toString(), ex);
            }
        }

        @UpnpAction
        public void deletePortMapping(
                @UpnpInputArgument(name = "NewRemoteHost", stateVariable = "RemoteHost") String remoteHost,
                @UpnpInputArgument(name = "NewExternalPort", stateVariable = "ExternalPort") UnsignedIntegerTwoBytes externalPort,
                @UpnpInputArgument(name = "NewProtocol", stateVariable = "PortMappingProtocol") String protocol
        ) throws ActionException {
            try {
                deletePortMapping(new PortMapping(
                        remoteHost,
                        externalPort,
                        PortMapping.Protocol.valueOf(protocol)
                ));
            } catch (Exception ex) {
                throw new ActionException(ErrorCode.ACTION_FAILED, "Can't convert port mapping: " + ex.toString(), ex);
            }
        }

        protected void addPortMapping(PortMapping portMapping) {
        }

        protected void deletePortMapping(PortMapping portMapping) {
        }

        @UpnpAction(out = {
                @UpnpOutputArgument(name = "NewConnectionStatus", stateVariable = "ConnectionStatus", getterName = "getStatus"),
                @UpnpOutputArgument(name = "NewLastConnectionError", stateVariable = "LastConnectionError", getterName = "getLastError"),
                @UpnpOutputArgument(name = "NewUptime", stateVariable = "Uptime", getterName = "getUptime")
        })
        public Connection.StatusInfo getStatusInfo() {
            return null;
        }

        @UpnpAction(out = {
                @UpnpOutputArgument(name = "NewExternalIPAddress", stateVariable = "ExternalIPAddress")
        })
        public String getExternalIPAddress() {
            return null;
        }

    }

}

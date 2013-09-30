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

package example.messagebox;

import org.teleal.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.teleal.cling.binding.annotations.UpnpAction;
import org.teleal.cling.binding.annotations.UpnpInputArgument;
import org.teleal.cling.binding.annotations.UpnpService;
import org.teleal.cling.binding.annotations.UpnpServiceId;
import org.teleal.cling.binding.annotations.UpnpServiceType;
import org.teleal.cling.binding.annotations.UpnpStateVariable;
import org.teleal.cling.binding.annotations.UpnpStateVariables;
import org.teleal.cling.model.DefaultServiceManager;
import org.teleal.cling.model.meta.DeviceDetails;
import org.teleal.cling.model.meta.DeviceIdentity;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.types.DeviceType;
import org.teleal.cling.model.types.UDN;

/**
 * @author Christian Bauer
 */
public class MessageBoxSampleData {

    public static LocalService readService(Class<?> serviceClass) throws Exception {
        LocalService service = new AnnotationLocalServiceBinder().read(serviceClass);
        service.setManager(
                new DefaultServiceManager(service, serviceClass)
        );
        return service;
    }

    public static LocalDevice createDevice(Class<?> serviceClass) throws Exception {
        return new LocalDevice(
                new DeviceIdentity(new UDN("1111")),
                new DeviceType("samsung.com", "PersonalMessageReceiver"),
                new DeviceDetails("My TV"),
                readService(serviceClass)
        );
    }

    @UpnpService(
            serviceId = @UpnpServiceId(namespace = "samsung.com", value = "MessageBoxService"),
            serviceType = @UpnpServiceType(namespace = "samsung.com", value = "MessageBoxService")
    )
    @UpnpStateVariables({
            @UpnpStateVariable(name ="A_ARG_TYPE_MessageID", datatype = "string", sendEvents = false),
            @UpnpStateVariable(name ="A_ARG_TYPE_MessageType", datatype = "string", sendEvents = false, defaultValue = "text/xml; charset=\"utf-8\""),
            @UpnpStateVariable(name ="A_ARG_TYPE_Message", datatype = "string", sendEvents = false)
    })
    public static class MessageBoxService {

        @UpnpAction
        public void addMessage(@UpnpInputArgument(name = "MessageID") String id,
                               @UpnpInputArgument(name = "MessageType") String type,
                               @UpnpInputArgument(name = "Message") String messageText) {
            checkMessage(id, type, messageText);
        }

        protected void checkMessage(String id, String type, String messageText) {

        }

    }
}

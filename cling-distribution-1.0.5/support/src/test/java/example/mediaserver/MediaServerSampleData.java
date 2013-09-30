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

package example.mediaserver;

import org.teleal.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.teleal.cling.model.DefaultServiceManager;
import org.teleal.cling.model.meta.DeviceDetails;
import org.teleal.cling.model.meta.DeviceIdentity;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.types.UDADeviceType;
import org.teleal.cling.model.types.UDN;
import org.teleal.cling.support.model.Protocol;
import org.teleal.cling.support.model.ProtocolInfo;
import org.teleal.cling.support.model.ProtocolInfos;

/**
 * @author Christian Bauer
 */
public class MediaServerSampleData {

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
                new UDADeviceType("MediaServer"),
                new DeviceDetails("My MediaServer"),
                readService(serviceClass)
        );
    }

    public static ProtocolInfos createSourceProtocols() {
        final ProtocolInfos sourceProtocols =                                           // DOC: PROT
                new ProtocolInfos(
                        new ProtocolInfo(
                                Protocol.HTTP_GET,
                                ProtocolInfo.WILDCARD,
                                "audio/mpeg",
                                "DLNA.ORG_PN=MP3;DLNA.ORG_OP=01"
                        ),
                        new ProtocolInfo(
                                Protocol.HTTP_GET,
                                ProtocolInfo.WILDCARD,
                                "video/mpeg",
                                "DLNA.ORG_PN=MPEG1;DLNA.ORG_OP=01;DLNA.ORG_CI=0"
                        )
                );                                                                      // DOC: PROT
        return sourceProtocols;
    }

    
}

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
import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.model.DefaultServiceManager;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.support.connectionmanager.ConnectionManagerService;
import org.teleal.cling.support.connectionmanager.callback.GetProtocolInfo;
import org.teleal.cling.support.model.ProtocolInfo;
import org.teleal.cling.support.model.ProtocolInfos;
import org.teleal.common.util.MimeType;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * A simple ConnectionManager for HTTP-GET
 * <p>
 * If your transmission protocol is based on GET requests with HTTP - that is, your
 * media player will download or stream the media file from an HTTP server - all
 * you need to provide with your <em>MediaServer:1</em> is a very simple
 * <em>ConnectionManager:1</em>.
 * </p>
 * <p>
 * This connection manager doesn't actually manage any connections, in fact, it doesn't
 * have to provide any functionality at all. This is how you can create and bind this
 * simple service with the Cling Support bundled <code>ConnectionManagerService</code>:
 * </p>
 * <a class="citation" href="javacode://this#retrieveProtocolInfo" style="include: BIND1;"/>
 * <p>
 * You can now add this service to your <em>MediaServer:1</em> device and everything will work.
 * </p>
 * <p>
 * Many media servers however provide at least a list of "source" protocols. This list contains
 * all the (MIME) protocol types your media server might potentially have resources for.
 * A sink (renderer) would obtain this protocol information and decide upfront if
 * any resource from your media server can be played at all, without having to browse
 * the content and looking at each resource's type.
 * </p>
 * <p>
 * First, create a list of protocol information that is supported:
 * </p>
 * <a class="citation" href="javacode://example.mediaserver.MediaServerSampleData#createSourceProtocols()" style="include: PROT;"/>
 * <p>
 * You now have to customize the instantiation of the connection manager service,
 * passing the list of procotols as a constructor argument:
 * </p>
 * <a class="citation" href="javacode://this#retrieveProtocolInfo" style="include: BIND2;" id="bind2"/>
 *
 */
public class ConnectionManagerSimpleTest {

    @Test
    public void retrieveProtocolInfo() {
        final ProtocolInfos sourceProtocols = MediaServerSampleData.createSourceProtocols();

        LocalService<ConnectionManagerService> service =                                                // DOC: BIND1
                new AnnotationLocalServiceBinder().read(ConnectionManagerService.class);

        service.setManager(
                new DefaultServiceManager<ConnectionManagerService>(
                        service,
                        ConnectionManagerService.class
                )
        );                                                                                              // DOC: BIND1

        service = new AnnotationLocalServiceBinder().read(ConnectionManagerService.class);

        service.setManager(                                                                             // DOC: BIND2
            new DefaultServiceManager<ConnectionManagerService>(service, null) {
                @Override
                protected ConnectionManagerService createServiceInstance() throws Exception {
                    return new ConnectionManagerService(sourceProtocols, null);
                }
            }
        );                                                                                              // DOC: BIND2

        final boolean[] assertions = new boolean[1];

        ActionCallback getProtInfo = new GetProtocolInfo(service) {                                     // DOC: CALL

            @Override
            public void received(ActionInvocation actionInvocation,
                                 ProtocolInfos sinkProtocolInfos,
                                 ProtocolInfos sourceProtocolInfos) {

                assertEquals(sourceProtocolInfos.size(), 2);
                assertEquals(
                        sourceProtocolInfos.get(0).getContentFormatMimeType(),
                        MimeType.valueOf("audio/mpeg")
                );

                MimeType supportedMimeType = MimeType.valueOf("video/mpeg");

                for (ProtocolInfo source : sourceProtocolInfos) {
                    if (source.getContentFormatMimeType().isCompatible(supportedMimeType))
                        // ... It's supported!
                        assertions[0] = true; // DOC: EXC1
                }
            }

            @Override
            public void failure(ActionInvocation invocation,
                                UpnpResponse operation,
                                String defaultMsg) {
                // Something is wrong
            }
        };                                                                                              // DOC: CALL

        getProtInfo.run();
        
        for (boolean assertion : assertions) {
            assertEquals(assertion, true);
        }
    }
}

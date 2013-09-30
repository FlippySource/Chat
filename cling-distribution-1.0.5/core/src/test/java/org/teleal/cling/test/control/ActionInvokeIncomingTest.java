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

package org.teleal.cling.test.control;

import org.teleal.cling.UpnpService;
import org.teleal.cling.mock.MockUpnpService;
import org.teleal.cling.model.action.ActionException;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.StreamRequestMessage;
import org.teleal.cling.model.message.StreamResponseMessage;
import org.teleal.cling.model.message.UpnpMessage;
import org.teleal.cling.model.message.UpnpRequest;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.message.control.IncomingActionResponseMessage;
import org.teleal.cling.model.message.header.ContentTypeHeader;
import org.teleal.cling.model.message.header.EXTHeader;
import org.teleal.cling.model.message.header.ServerHeader;
import org.teleal.cling.model.message.header.SoapActionHeader;
import org.teleal.cling.model.message.header.UpnpHeader;
import org.teleal.cling.model.meta.Action;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.meta.QueryStateVariableAction;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.ErrorCode;
import org.teleal.cling.model.types.SoapActionType;
import org.teleal.cling.protocol.sync.ReceivingAction;
import org.teleal.common.util.MimeType;
import org.testng.annotations.Test;

import java.net.URI;

import static org.testng.Assert.assertEquals;


public class ActionInvokeIncomingTest {

    public static final String SET_REQUEST = "<?xml version=\"1.0\"?>\n" +
            " <s:Envelope\n" +
            "     xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "     s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
            "   <s:Body>\n" +
            "     <u:SetTarget xmlns:u=\"urn:schemas-upnp-org:service:SwitchPower:1\">\n" +
            "       <NewTargetValue>1</NewTargetValue>\n" +
            "     </u:SetTarget>\n" +
            "   </s:Body>\n" +
            " </s:Envelope>";

    public static final String GET_REQUEST = "<?xml version=\"1.0\"?>\n" +
            " <s:Envelope\n" +
            "     xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "     s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
            "   <s:Body>\n" +
            "     <u:GetTarget xmlns:u=\"urn:schemas-upnp-org:service:SwitchPower:1\"/>\n" +
            "   </s:Body>\n" +
            " </s:Envelope>";

    public static final String QUERY_STATE_VARIABLE_REQUEST = "<?xml version=\"1.0\"?>\n" +
            " <s:Envelope\n" +
            "     xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "     s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
            "   <s:Body>\n" +
            "     <u:QueryStateVariable xmlns:u=\"urn:schemas-upnp-org:control-1-0\">\n" +
            "       <varName>Status</varName>\n" +
            "     </u:QueryStateVariable>\n" +
            "   </s:Body>\n" +
            " </s:Envelope>";

    @Test
    public void incomingRemoteCallGet() throws Exception {
        incomingRemoteCallGet(ActionSampleData.createTestDevice());
    }

    @Test
    public void incomingRemoteCallGetExtraHeaders() throws Exception {
        UpnpMessage response =
                incomingRemoteCallGet(ActionSampleData.createTestDevice(ActionSampleData.LocalTestServiceExtraHeaders.class));

        assertEquals(response.getHeaders().size(), 4);
        assertEquals(response.getHeaders().getFirstHeader("X-MY-HEADER"), "foobar");
    }

    public IncomingActionResponseMessage incomingRemoteCallGet(LocalDevice ld) throws Exception {

        MockUpnpService upnpService = new MockUpnpService();
        LocalService service = ld.getServices()[0];
        upnpService.getRegistry().addDevice(ld);

        Action action = service.getAction("GetTarget");

        URI controlURI = upnpService.getConfiguration().getNamespace().getControlPath(service);
        StreamRequestMessage request = new StreamRequestMessage(UpnpRequest.Method.POST, controlURI);
        addMandatoryRequestHeaders(service, action, request);
        request.setBody(UpnpMessage.BodyType.STRING, GET_REQUEST);

        ReceivingAction prot = new ReceivingAction(upnpService, request);

        prot.run();

        StreamResponseMessage response = prot.getOutputMessage();

        assert response != null;
        assert response.getHeaders().getFirstHeader(UpnpHeader.Type.CONTENT_TYPE, ContentTypeHeader.class).isUDACompliantXML();
        assert response.getHeaders().getFirstHeader(UpnpHeader.Type.EXT, EXTHeader.class) != null;
        assert response.getHeaders().getFirstHeader(UpnpHeader.Type.SERVER, ServerHeader.class).getValue().equals(new ServerHeader().getValue());

        IncomingActionResponseMessage responseMessage = new IncomingActionResponseMessage(response);
        ActionInvocation responseInvocation = new ActionInvocation(action);
        upnpService.getConfiguration().getSoapActionProcessor().readBody(responseMessage, responseInvocation);

        assert responseInvocation.getOutput("RetTargetValue") != null;
        return responseMessage;
    }

    @Test
    public void incomingRemoteCallGetConcurrent() throws Exception {

        // Registery local device and its service
        MockUpnpService upnpService = new MockUpnpService(false, false, true);
        LocalDevice ld = ActionSampleData.createTestDevice(ActionSampleData.LocalTestServiceThrowsException.class);
        LocalService service = ld.getServices()[0];
        upnpService.getRegistry().addDevice(ld);

        int i = 0;
        while (i < 10) {
            new Thread(new ConcurrentGetTest(upnpService, service)).start();
            i++;
        }

        // Wait for the threads to finish
        Thread.sleep(2000);
    }

    static class ConcurrentGetTest implements Runnable {
        private UpnpService upnpService;
        private LocalService service;

        ConcurrentGetTest(UpnpService upnpService, LocalService service) {
            this.upnpService = upnpService;
            this.service = service;
        }

        public void run() {
            Action action = service.getAction("GetTarget");

            URI controlURI = upnpService.getConfiguration().getNamespace().getControlPath(service);
            StreamRequestMessage request = new StreamRequestMessage(UpnpRequest.Method.POST, controlURI);
            request.getHeaders().add(
                    UpnpHeader.Type.CONTENT_TYPE,
                    new ContentTypeHeader(ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8)
            );

            SoapActionType actionType = new SoapActionType(service.getServiceType(), action.getName());
            request.getHeaders().add(UpnpHeader.Type.SOAPACTION, new SoapActionHeader(actionType));
            request.setBody(UpnpMessage.BodyType.STRING, GET_REQUEST);

            ReceivingAction prot = new ReceivingAction(upnpService, request);

            prot.run();

            StreamResponseMessage response = prot.getOutputMessage();

            assert response != null;
            assert response.getHeaders().getFirstHeader(UpnpHeader.Type.CONTENT_TYPE, ContentTypeHeader.class).isUDACompliantXML();
            assert response.getHeaders().getFirstHeader(UpnpHeader.Type.EXT, EXTHeader.class) != null;
            assert response.getHeaders().getFirstHeader(UpnpHeader.Type.SERVER, ServerHeader.class).getValue().equals(new ServerHeader().getValue());

            IncomingActionResponseMessage responseMessage = new IncomingActionResponseMessage(response);
            ActionInvocation responseInvocation = new ActionInvocation(action);
            upnpService.getConfiguration().getSoapActionProcessor().readBody(responseMessage, responseInvocation);

            assert responseInvocation.getOutput("RetTargetValue") != null;
        }
    }


    @Test
    public void incomingRemoteCallSet() throws Exception {

        // Registery local device and its service
        MockUpnpService upnpService = new MockUpnpService();
        LocalDevice ld = ActionSampleData.createTestDevice();
        LocalService service = ld.getServices()[0];
        upnpService.getRegistry().addDevice(ld);

        Action action = service.getAction("SetTarget");

        URI controlURI = upnpService.getConfiguration().getNamespace().getControlPath(service);
        StreamRequestMessage request = new StreamRequestMessage(UpnpRequest.Method.POST, controlURI);
        addMandatoryRequestHeaders(service, action, request);
        request.setBody(UpnpMessage.BodyType.STRING, SET_REQUEST);

        ReceivingAction prot = new ReceivingAction(upnpService, request);

        prot.run();

        StreamResponseMessage response = prot.getOutputMessage();

        assert response != null;
        assert response.getHeaders().getFirstHeader(UpnpHeader.Type.CONTENT_TYPE, ContentTypeHeader.class).isUDACompliantXML();
        assert response.getHeaders().getFirstHeader(UpnpHeader.Type.EXT, EXTHeader.class) != null;
        assert response.getHeaders().getFirstHeader(UpnpHeader.Type.SERVER, ServerHeader.class).getValue().equals(new ServerHeader().getValue());

        IncomingActionResponseMessage responseMessage = new IncomingActionResponseMessage(response);
        ActionInvocation responseInvocation = new ActionInvocation(action);
        upnpService.getConfiguration().getSoapActionProcessor().readBody(responseMessage, responseInvocation);

        assertEquals(responseInvocation.getOutput().length, 0);

    }

    @Test
    public void incomingRemoteCallControlURINotFound() throws Exception {

        // Registery local device and its service
        MockUpnpService upnpService = new MockUpnpService();
        LocalDevice ld = ActionSampleData.createTestDevice();
        LocalService service = ld.getServices()[0];
        upnpService.getRegistry().addDevice(ld);

        Action action = service.getAction("SetTarget");

        StreamRequestMessage request = new StreamRequestMessage(UpnpRequest.Method.POST, URI.create("/some/random/123/uri"));
        addMandatoryRequestHeaders(service, action, request);
        request.setBody(UpnpMessage.BodyType.STRING, SET_REQUEST);

        ReceivingAction prot = new ReceivingAction(upnpService, request);

        prot.run();

        UpnpMessage response = prot.getOutputMessage();

        assert response == null;
        // The StreamServer will send a 404 response
    }

    @Test
    public void incomingRemoteCallMethodException() throws Exception {

        // Registery local device and its service
        MockUpnpService upnpService = new MockUpnpService();
        LocalDevice ld = ActionSampleData.createTestDevice(ActionSampleData.LocalTestServiceThrowsException.class);
        LocalService service = ld.getServices()[0];
        upnpService.getRegistry().addDevice(ld);

        Action action = service.getAction("SetTarget");

        URI controlURI = upnpService.getConfiguration().getNamespace().getControlPath(service);
        StreamRequestMessage request = new StreamRequestMessage(UpnpRequest.Method.POST, controlURI);
        addMandatoryRequestHeaders(service, action, request);

        request.setBody(UpnpMessage.BodyType.STRING, SET_REQUEST);

        ReceivingAction prot = new ReceivingAction(upnpService, request);

        prot.run();

        StreamResponseMessage response = prot.getOutputMessage();

        assert response != null;
        assert response.getHeaders().getFirstHeader(UpnpHeader.Type.CONTENT_TYPE, ContentTypeHeader.class).isUDACompliantXML();
        assert response.getHeaders().getFirstHeader(UpnpHeader.Type.EXT, EXTHeader.class) != null;
        assert response.getHeaders().getFirstHeader(UpnpHeader.Type.SERVER, ServerHeader.class).getValue().equals(new ServerHeader().getValue());

        IncomingActionResponseMessage responseMessage = new IncomingActionResponseMessage(response);
        ActionInvocation responseInvocation = new ActionInvocation(action);
        upnpService.getConfiguration().getSoapActionProcessor().readBody(responseMessage, responseInvocation);

        ActionException ex = responseInvocation.getFailure();
        assert ex != null;

        assertEquals(ex.getMessage(), ErrorCode.ACTION_FAILED.getDescription() + ". Action method invocation failed: Something is wrong.");

    }

    @Test
    public void incomingRemoteCallNoContentType() throws Exception {

        // Registery local device and its service
        MockUpnpService upnpService = new MockUpnpService();
        LocalDevice ld = ActionSampleData.createTestDevice();
        LocalService service = ld.getServices()[0];
        upnpService.getRegistry().addDevice(ld);

        Action action = service.getAction("GetTarget");

        URI controlURI = upnpService.getConfiguration().getNamespace().getControlPath(service);
        StreamRequestMessage request = new StreamRequestMessage(UpnpRequest.Method.POST, controlURI);
        SoapActionType actionType = new SoapActionType(service.getServiceType(), action.getName());
        request.getHeaders().add(UpnpHeader.Type.SOAPACTION, new SoapActionHeader(actionType));
        // NO CONTENT TYPE!
        request.setBody(UpnpMessage.BodyType.STRING, GET_REQUEST);

        ReceivingAction prot = new ReceivingAction(upnpService, request);

        prot.run();

        StreamResponseMessage response = prot.getOutputMessage();

        assert response != null;
        assert response.getHeaders().getFirstHeader(UpnpHeader.Type.CONTENT_TYPE, ContentTypeHeader.class).isUDACompliantXML();
        assert response.getHeaders().getFirstHeader(UpnpHeader.Type.EXT, EXTHeader.class) != null;
        assert response.getHeaders().getFirstHeader(UpnpHeader.Type.SERVER, ServerHeader.class).getValue().equals(new ServerHeader().getValue());

        IncomingActionResponseMessage responseMessage = new IncomingActionResponseMessage(response);
        ActionInvocation responseInvocation = new ActionInvocation(action);
        upnpService.getConfiguration().getSoapActionProcessor().readBody(responseMessage, responseInvocation);

        assert responseInvocation.getOutput("RetTargetValue") != null;

    }

    @Test
    public void incomingRemoteCallWrongContentType() throws Exception {

        MockUpnpService upnpService = new MockUpnpService();

        StreamRequestMessage request = new StreamRequestMessage(UpnpRequest.Method.POST, URI.create("/some/random/123/uri"));
        request.getHeaders().add(
                UpnpHeader.Type.CONTENT_TYPE,
                new ContentTypeHeader(MimeType.valueOf("some/randomtype"))
        );
        request.setBody(UpnpMessage.BodyType.STRING, SET_REQUEST);

        ReceivingAction prot = new ReceivingAction(upnpService, request);

        prot.run();

        StreamResponseMessage response = prot.getOutputMessage();

        assert response != null;

        assertEquals(response.getOperation().getStatusCode(), UpnpResponse.Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    @Test
    public void incomingRemoteCallQueryStateVariable() throws Exception {

        // Registery local device and its service
        MockUpnpService upnpService = new MockUpnpService();
        LocalDevice ld = ActionSampleData.createTestDevice();
        LocalService service = ld.getServices()[0];
        upnpService.getRegistry().addDevice(ld);

        Action action = service.getAction(QueryStateVariableAction.ACTION_NAME);

        URI controlURI = upnpService.getConfiguration().getNamespace().getControlPath(service);
        StreamRequestMessage request = new StreamRequestMessage(UpnpRequest.Method.POST, controlURI);
        request.getHeaders().add(
                UpnpHeader.Type.CONTENT_TYPE,
                new ContentTypeHeader(ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8)
        );
        request.getHeaders().add(
                UpnpHeader.Type.SOAPACTION,
                new SoapActionHeader(
                        new SoapActionType(
                                SoapActionType.MAGIC_CONTROL_NS, SoapActionType.MAGIC_CONTROL_TYPE, null, action.getName()
                        )
                )

        );
        request.setBody(UpnpMessage.BodyType.STRING, QUERY_STATE_VARIABLE_REQUEST);

        ReceivingAction prot = new ReceivingAction(upnpService, request);

        prot.run();

        StreamResponseMessage response = prot.getOutputMessage();

        assert response != null;
        assert response.getHeaders().getFirstHeader(UpnpHeader.Type.CONTENT_TYPE, ContentTypeHeader.class).isUDACompliantXML();
        assert response.getHeaders().getFirstHeader(UpnpHeader.Type.EXT, EXTHeader.class) != null;
        assert response.getHeaders().getFirstHeader(UpnpHeader.Type.SERVER, ServerHeader.class).getValue().equals(new ServerHeader().getValue());

        IncomingActionResponseMessage responseMessage = new IncomingActionResponseMessage(response);
        ActionInvocation responseInvocation = new ActionInvocation(action);
        upnpService.getConfiguration().getSoapActionProcessor().readBody(responseMessage, responseInvocation);

        assertEquals(responseInvocation.getOutput()[0].getArgument().getName(), "return");
        assertEquals(responseInvocation.getOutput()[0].toString(), "0");
    }


    protected void addMandatoryRequestHeaders(Service service, Action action, StreamRequestMessage request) {
        request.getHeaders().add(
                UpnpHeader.Type.CONTENT_TYPE,
                new ContentTypeHeader(ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8)
        );

        SoapActionType actionType = new SoapActionType(service.getServiceType(), action.getName());
        request.getHeaders().add(UpnpHeader.Type.SOAPACTION, new SoapActionHeader(actionType));
    }

}
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

import org.teleal.cling.mock.MockUpnpService;
import org.teleal.cling.model.action.ActionException;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.StreamRequestMessage;
import org.teleal.cling.model.message.StreamResponseMessage;
import org.teleal.cling.model.message.UpnpMessage;
import org.teleal.cling.model.message.UpnpRequest;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.message.control.IncomingActionRequestMessage;
import org.teleal.cling.model.message.control.IncomingActionResponseMessage;
import org.teleal.cling.model.message.control.OutgoingActionRequestMessage;
import org.teleal.cling.model.message.control.OutgoingActionResponseMessage;
import org.teleal.cling.model.message.header.ContentTypeHeader;
import org.teleal.cling.model.message.header.SoapActionHeader;
import org.teleal.cling.model.message.header.UpnpHeader;
import org.teleal.cling.model.meta.Action;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.types.ErrorCode;
import org.teleal.cling.model.types.SoapActionType;
import org.teleal.cling.test.data.SampleData;
import org.testng.annotations.Test;

import java.net.URI;

import static org.testng.Assert.assertEquals;

public class ActionXMLProcessingTest {


    public static final String ENCODED_REQUEST = "<?xml version=\"1.0\"?>\n" +
            " <s:Envelope\n" +
            "     xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "     s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
            "   <s:Body>\n" +
            "     <u:SetSomeValue xmlns:u=\"urn:schemas-upnp-org:service:SwitchPower:1\">\n" +
            "       <SomeValue>This is encoded: &lt;</SomeValue>\n" +
            "     </u:SetSomeValue>\n" +
            "   </s:Body>\n" +
            " </s:Envelope>";

    public static final String ALIAS_ENCODED_REQUEST = "<?xml version=\"1.0\"?>\n" +
            " <s:Envelope\n" +
            "     xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "     s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
            "   <s:Body>\n" +
            "     <u:SetSomeValue xmlns:u=\"urn:schemas-upnp-org:service:SwitchPower:1\">\n" +
            "       <SomeValue1>This is encoded: &lt;</SomeValue1>\n" +
            "     </u:SetSomeValue>\n" +
            "   </s:Body>\n" +
            " </s:Envelope>";

    @Test
    public void writeReadRequest() throws Exception {

        LocalDevice ld = ActionSampleData.createTestDevice(ActionSampleData.LocalTestServiceExtended.class);
        LocalService svc = ld.getServices()[0];

        Action action = svc.getAction("SetTarget");
        ActionInvocation actionInvocation = new ActionInvocation(action);
        actionInvocation.setInput("NewTargetValue", true);

        // The control URL doesn't matter
        OutgoingActionRequestMessage outgoingCall = new OutgoingActionRequestMessage(actionInvocation, SampleData.getLocalBaseURL());

        MockUpnpService upnpService = new MockUpnpService();
        upnpService.getConfiguration().getSoapActionProcessor().writeBody(outgoingCall, actionInvocation);

        StreamRequestMessage incomingStream = new StreamRequestMessage(outgoingCall);
        IncomingActionRequestMessage incomingCall = new IncomingActionRequestMessage(incomingStream, svc);

        actionInvocation = new ActionInvocation(incomingCall.getAction());

        upnpService.getConfiguration().getSoapActionProcessor().readBody(incomingCall, actionInvocation);

        assert actionInvocation.getInput().length == 1;
        assertEquals(actionInvocation.getInput()[0].getArgument().getName(), "NewTargetValue");
    }

    @Test
    public void writeReadResponse() throws Exception {

        LocalDevice ld = ActionSampleData.createTestDevice(ActionSampleData.LocalTestServiceExtended.class);
        LocalService svc = ld.getServices()[0];

        Action action = svc.getAction("GetTarget");
        ActionInvocation actionInvocation = new ActionInvocation(action);

        OutgoingActionResponseMessage outgoingCall = new OutgoingActionResponseMessage(action);
        actionInvocation.setOutput("RetTargetValue", true);

        MockUpnpService upnpService = new MockUpnpService();
        upnpService.getConfiguration().getSoapActionProcessor().writeBody(outgoingCall, actionInvocation);

        StreamResponseMessage incomingStream = new StreamResponseMessage(outgoingCall);
        IncomingActionResponseMessage incomingCall = new IncomingActionResponseMessage(incomingStream);

        actionInvocation = new ActionInvocation(action);
        upnpService.getConfiguration().getSoapActionProcessor().readBody(incomingCall, actionInvocation);

        assertEquals(actionInvocation.getOutput()[0].getArgument().getName(), "RetTargetValue");
    }

    @Test
    public void writeFailureReadFailure() throws Exception {

        LocalDevice ld = ActionSampleData.createTestDevice(ActionSampleData.LocalTestServiceExtended.class);
        LocalService svc = ld.getServices()[0];

        Action action = svc.getAction("GetTarget");
        ActionInvocation actionInvocation = new ActionInvocation(action);
        actionInvocation.setFailure(new ActionException(ErrorCode.ACTION_FAILED, "A test string"));

        OutgoingActionResponseMessage outgoingCall = new OutgoingActionResponseMessage(UpnpResponse.Status.INTERNAL_SERVER_ERROR);

        MockUpnpService upnpService = new MockUpnpService();
        upnpService.getConfiguration().getSoapActionProcessor().writeBody(outgoingCall, actionInvocation);

        StreamResponseMessage incomingStream = new StreamResponseMessage(outgoingCall);
        IncomingActionResponseMessage incomingCall = new IncomingActionResponseMessage(incomingStream);

        actionInvocation = new ActionInvocation(action);
        upnpService.getConfiguration().getSoapActionProcessor().readBody(incomingCall, actionInvocation);

        assertEquals(actionInvocation.getFailure().getErrorCode(), ErrorCode.ACTION_FAILED.getCode());
        // Note the period at the end of the test string!
        assertEquals(actionInvocation.getFailure().getMessage(), ErrorCode.ACTION_FAILED.getDescription() + ". A test string.");
    }

    @Test
    public void readEncodedRequest() throws Exception {

        LocalDevice ld = ActionSampleData.createTestDevice(ActionSampleData.LocalTestServiceExtended.class);
        LocalService svc = ld.getServices()[0];

        Action action = svc.getAction("SetSomeValue");
        ActionInvocation actionInvocation = new ActionInvocation(action);

        MockUpnpService upnpService = new MockUpnpService();

        StreamRequestMessage streamRequest = new StreamRequestMessage(UpnpRequest.Method.POST, URI.create("http://some.uri"));
        streamRequest.getHeaders().add(
                UpnpHeader.Type.CONTENT_TYPE,
                new ContentTypeHeader(ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8)
        );
        streamRequest.getHeaders().add(
                UpnpHeader.Type.SOAPACTION,
                new SoapActionHeader(
                        new SoapActionType(
                                action.getService().getServiceType(),
                                action.getName()
                        )
                )
        );
        streamRequest.setBody(UpnpMessage.BodyType.STRING, ENCODED_REQUEST);

        IncomingActionRequestMessage request = new IncomingActionRequestMessage(streamRequest, svc);

        upnpService.getConfiguration().getSoapActionProcessor().readBody(request, actionInvocation);

        assertEquals(actionInvocation.getInput()[0].toString(), "This is encoded: <");

    }

    @Test
    public void readEncodedRequestWithAlias() throws Exception {

        LocalDevice ld = ActionSampleData.createTestDevice(ActionSampleData.LocalTestServiceExtended.class);
        LocalService svc = ld.getServices()[0];

        Action action = svc.getAction("SetSomeValue");
        ActionInvocation actionInvocation = new ActionInvocation(action);

        MockUpnpService upnpService = new MockUpnpService();

        StreamRequestMessage streamRequest = new StreamRequestMessage(UpnpRequest.Method.POST, URI.create("http://some.uri"));
        streamRequest.getHeaders().add(
                UpnpHeader.Type.CONTENT_TYPE,
                new ContentTypeHeader(ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8)
        );
        streamRequest.getHeaders().add(
                UpnpHeader.Type.SOAPACTION,
                new SoapActionHeader(
                        new SoapActionType(
                                action.getService().getServiceType(),
                                action.getName()
                        )
                )
        );
        streamRequest.setBody(UpnpMessage.BodyType.STRING, ALIAS_ENCODED_REQUEST);

        IncomingActionRequestMessage request = new IncomingActionRequestMessage(streamRequest, svc);

        upnpService.getConfiguration().getSoapActionProcessor().readBody(request, actionInvocation);

        assertEquals(actionInvocation.getInput()[0].toString(), "This is encoded: <");
        assertEquals(actionInvocation.getInput("SomeValue").toString(), "This is encoded: <");

    }

    @Test
    public void writeDecodedResponse() throws Exception {

        LocalDevice ld = ActionSampleData.createTestDevice(ActionSampleData.LocalTestServiceExtended.class);
        LocalService svc = ld.getServices()[0];

        Action action = svc.getAction("GetSomeValue");
        ActionInvocation actionInvocation = new ActionInvocation(action);

        MockUpnpService upnpService = new MockUpnpService();

        OutgoingActionResponseMessage response = new OutgoingActionResponseMessage(action);
        actionInvocation.setOutput("SomeValue", "This is decoded: &<>'\"");

        upnpService.getConfiguration().getSoapActionProcessor().writeBody(response, actionInvocation);

        assert response.getBodyString().contains("<SomeValue>This is decoded: &amp;&lt;&gt;&apos;&quot;</SomeValue>");
    }
}

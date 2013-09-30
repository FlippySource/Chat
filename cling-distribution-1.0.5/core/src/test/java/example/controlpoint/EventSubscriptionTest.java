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

package example.controlpoint;

import example.binarylight.BinaryLightSampleData;
import example.binarylight.SwitchPower;
import org.teleal.cling.controlpoint.SubscriptionCallback;
import org.teleal.cling.mock.MockUpnpService;
import org.teleal.cling.model.gena.CancelReason;
import org.teleal.cling.model.gena.GENASubscription;
import org.teleal.cling.model.message.StreamResponseMessage;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.message.header.SubscriptionIdHeader;
import org.teleal.cling.model.message.header.TimeoutHeader;
import org.teleal.cling.model.message.header.UpnpHeader;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.state.StateVariableValue;
import org.teleal.cling.model.types.BooleanDatatype;
import org.teleal.cling.model.types.Datatype;
import org.teleal.common.util.Reflections;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * Receiving events from services
 * <p>
 * The UPnP specification defines a general event notification system (GENA) which is based on a
 * publish/subscribe paradigm. Your control point subscribes with a service in order to receive
 * events. When the service state changes, an event message will be delivered to the callback
 * of your control point. Subscriptions are periodically refreshed until you unsubscribe from
 * the service. If you do not unsubscribe and if a refresh of the subscription fails, maybe
 * because the control point was turned off without proper shutdown, the subscription will
 * timeout on the publishing service's side.
 * </p>
 * <p>
 * This is an example subscription on a service that sends events for a state variable named
 * <code>Status</code> (e.g. the previously shown <a href="#section.SwitchPower">SwitchPower</a>
 * service). The subscription's refresh and timeout period is 600 seconds:
 * </p>
 * <a class="citation" href="javacode://this#subscriptionLifecycle" style="include: SUBSCRIBE; exclude: EXC1, EXC2, EXC3, EXC4, EXC5;"/>
 * <p>
 * The <code>SubscriptionCallback</code> offers the methods <code>failed()</code>,
 * <code>established()</code>, and <code>ended()</code> which are called during a subscription's lifecycle.
 * When a subscription ends you will be notified with a <code>CancelReason</code> whenever the termination
 * of the subscription was irregular. See the Javadoc of these methods for more details.
 * </p>
 * <p>
 * Every event message from the service will be passed to the <code>eventReceived()</code> method,
 * and every message will carry a sequence number. Variable values are not transmitted individually,
 * each message contains <code>StateVariableValue</code> instances for <em>all</em> evented
 * variables of a service. You'll receive a snapshot of the state of the service at the time the
 * event was triggered.
 * </p>
 * <p>
 * Whenever the receiving UPnP stack detects an event message that is out of sequence, e.g. because
 * some messages were lost during transport, the <code>eventsMissed()</code> method will be called
 * before you receive the event. You then decide if missing events is important for the correct
 * behavior of your application, or if you can silently ignore it and continue processing events
 * with non-consecutive sequence numbers.
 * </p>
 * <p>
 * You end a subscription regularly by calling <code>callback.end()</code>, which will unsubscribe
 * your control point from the service.
 * </p>
 */
public class EventSubscriptionTest {

    @Test
    public void subscriptionLifecycle() throws Exception {

        MockUpnpService upnpService = createMockUpnpService();

        final List<Boolean> testAssertions = new ArrayList();

        // Register local device and its service
        LocalDevice device = BinaryLightSampleData.createDevice(SwitchPower.class);
        upnpService.getRegistry().addDevice(device);

        LocalService service = device.getServices()[0];

        SubscriptionCallback callback = new SubscriptionCallback(service, 600) {            // DOC: SUBSCRIBE

            @Override
            public void established(GENASubscription sub) {
                System.out.println("Established: " + sub.getSubscriptionId());
                testAssertions.add(true); // DOC: EXC2
            }

            @Override
            protected void failed(GENASubscription subscription,
                                  UpnpResponse responseStatus,
                                  Exception exception,
                                  String defaultMsg) {
                System.err.println(defaultMsg);
                testAssertions.add(false); // DOC: EXC1
            }

            @Override
            public void ended(GENASubscription sub,
                              CancelReason reason,
                              UpnpResponse response) {
                assert reason == null;
                assert sub != null;  // DOC: EXC3
                assert response == null;
                testAssertions.add(true);     // DOC: EXC3
            }

            public void eventReceived(GENASubscription sub) {

                System.out.println("Event: " + sub.getCurrentSequence().getValue());

                Map<String, StateVariableValue> values = sub.getCurrentValues();
                StateVariableValue status = values.get("Status");

                assertEquals(status.getDatatype().getClass(), BooleanDatatype.class);
                assertEquals(status.getDatatype().getBuiltin(), Datatype.Builtin.BOOLEAN);

                System.out.println("Status is: " + status.toString());

                if (sub.getCurrentSequence().getValue() == 0) {                             // DOC: EXC4
                    assertEquals(sub.getCurrentValues().get("Status").toString(), "0");
                    testAssertions.add(true);
                } else if (sub.getCurrentSequence().getValue() == 1) {
                    assertEquals(sub.getCurrentValues().get("Status").toString(), "1");
                    testAssertions.add(true);
                } else {
                    testAssertions.add(false);
                }                                                                           // DOC: EXC4
            }

            public void eventsMissed(GENASubscription sub, int numberOfMissedEvents) {
                System.out.println("Missed events: " + numberOfMissedEvents);
                testAssertions.add(false);                                                  // DOC: EXC5
            }

        };

        upnpService.getControlPoint().execute(callback);                                    // DOC: SUBSCRIBE

        // Modify the state of the service and trigger event
        Object serviceImpl = service.getManager().getImplementation();
        Reflections.set(Reflections.getField(serviceImpl.getClass(), "status"), serviceImpl, true);
        service.getManager().getPropertyChangeSupport().firePropertyChange("Status", false, true);

        assertEquals(callback.getSubscription().getCurrentSequence().getValue(), Long.valueOf(2)); // It's the NEXT sequence!
        assert callback.getSubscription().getSubscriptionId().startsWith("uuid:");
        
        // Actually, the local subscription we are testing here has an "unlimited" duration
        assertEquals(callback.getSubscription().getActualDurationSeconds(), Integer.MAX_VALUE);

        callback.end();

        assertEquals(testAssertions.size(), 4);
        for (Boolean testAssertion : testAssertions) {
            assert testAssertion;
        }

        assertEquals(upnpService.getSentStreamRequestMessages().size(), 0);
    }

    protected MockUpnpService createMockUpnpService() {
        return new MockUpnpService() {
            @Override
            public StreamResponseMessage[] getStreamResponseMessages() {
                return new StreamResponseMessage[]{
                        createSubscribeResponseMessage(),
                        createUnsubscribeResponseMessage()
                };
            }
        };
    }

    protected StreamResponseMessage createSubscribeResponseMessage() {
        StreamResponseMessage msg = new StreamResponseMessage(new UpnpResponse(UpnpResponse.Status.OK));
        msg.getHeaders().add(
                UpnpHeader.Type.SID, new SubscriptionIdHeader("uuid:1234")
        );
        msg.getHeaders().add(
                UpnpHeader.Type.TIMEOUT, new TimeoutHeader(180)
        );
        return msg;
    }

    protected StreamResponseMessage createUnsubscribeResponseMessage() {
        return new StreamResponseMessage(new UpnpResponse(UpnpResponse.Status.OK));
    }


}
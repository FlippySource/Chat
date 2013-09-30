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

package org.teleal.cling.protocol.sync;

import org.teleal.cling.UpnpService;
import org.teleal.cling.model.gena.CancelReason;
import org.teleal.cling.model.gena.LocalGENASubscription;
import org.teleal.cling.model.message.StreamRequestMessage;
import org.teleal.cling.model.message.StreamResponseMessage;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.message.gena.IncomingSubscribeRequestMessage;
import org.teleal.cling.model.message.gena.OutgoingSubscribeResponseMessage;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.resource.ServiceEventSubscriptionResource;
import org.teleal.cling.protocol.ReceivingSync;
import org.teleal.common.util.Exceptions;

import java.util.logging.Logger;

/**
 * Handles reception of GENA event subscription (initial and renewal) messages.
 * <p>
 * This protocol tries to find a local event subscription URI matching the requested URI,
 * then creates a new {@link org.teleal.cling.model.gena.LocalGENASubscription} if no
 * subscription identifer was supplied.
 * </p>
 * <p>
 * The subscription is however only registered with the local service, and monitoring
 * of state changes is established, if the response of this protocol was successfully
 * delivered to the client which requested the subscription.
 * </p>
 * <p>
 * Once registration and monitoring is active, an initial event with the current
 * state of the service is send to the subscriber. This will only happen after the
 * subscription response message was successfully delivered to the subscriber.
 * </p>
 *
 * @author Christian Bauer
 */
public class ReceivingSubscribe extends ReceivingSync<StreamRequestMessage, OutgoingSubscribeResponseMessage> {

    final private static Logger log = Logger.getLogger(ReceivingSubscribe.class.getName());

    protected LocalGENASubscription subscription;

    public ReceivingSubscribe(UpnpService upnpService, StreamRequestMessage inputMessage) {
        super(upnpService, inputMessage);
    }

    protected OutgoingSubscribeResponseMessage executeSync() {

        ServiceEventSubscriptionResource resource =
                getUpnpService().getRegistry().getResource(
                        ServiceEventSubscriptionResource.class,
                        getInputMessage().getUri()
        );

        if (resource == null) {
            log.fine("No local resource found: " + getInputMessage());
            return null;
        }

        log.fine("Found local event subscription matching relative request URI: " + getInputMessage().getUri());

        IncomingSubscribeRequestMessage requestMessage =
                new IncomingSubscribeRequestMessage(getInputMessage(), resource.getModel());

        // Error conditions UDA 1.0 section 4.1.1 and 4.1.2
        if (requestMessage.getSubscriptionId() != null &&
                (requestMessage.hasNotificationHeader() || requestMessage.getCallbackURLs() != null)) {
            log.fine("Subscription ID and NT or Callback in subscribe request: " + getInputMessage());
            return new OutgoingSubscribeResponseMessage(UpnpResponse.Status.BAD_REQUEST);
        }

        if (requestMessage.getSubscriptionId() != null) {
            return processRenewal(resource.getModel(), requestMessage);
        } else if (requestMessage.hasNotificationHeader() && requestMessage.getCallbackURLs() != null){
            return processNewSubscription(resource.getModel(), requestMessage);
        } else {
            log.fine("No subscription ID, no NT or Callback, neither subscription or renewal: " + getInputMessage());
            return new OutgoingSubscribeResponseMessage(UpnpResponse.Status.PRECONDITION_FAILED);
        }

    }

    protected OutgoingSubscribeResponseMessage processRenewal(LocalService service,
                                                              IncomingSubscribeRequestMessage requestMessage) {

        subscription = getUpnpService().getRegistry().getLocalSubscription(requestMessage.getSubscriptionId());

        // Error conditions UDA 1.0 section 4.1.1 and 4.1.2
        if (subscription == null) {
            log.fine("Invalid subscription ID for renewal request: " + getInputMessage());
            return new OutgoingSubscribeResponseMessage(UpnpResponse.Status.PRECONDITION_FAILED);
        }

        log.fine("Renewing subscription: " + subscription);
        subscription.setSubscriptionDuration(requestMessage.getRequestedTimeoutSeconds());
        if (getUpnpService().getRegistry().updateLocalSubscription(subscription)) {
            return new OutgoingSubscribeResponseMessage(subscription);
        } else {
            log.fine("Subscription went away before it could be renewed: " + getInputMessage());
            return new OutgoingSubscribeResponseMessage(UpnpResponse.Status.PRECONDITION_FAILED);
        }
    }

    protected OutgoingSubscribeResponseMessage processNewSubscription(LocalService service,
                                                                      IncomingSubscribeRequestMessage requestMessage) {
        // Error conditions UDA 1.0 section 4.1.1 and 4.1.2
        if (requestMessage.getCallbackURLs() == null) {
            log.fine("Missing or invalid Callback URLs in subscribe request: " + getInputMessage());
            return new OutgoingSubscribeResponseMessage(UpnpResponse.Status.PRECONDITION_FAILED);
        }

        if (!requestMessage.hasNotificationHeader()) {
            log.fine("Missing or invalid NT header in subscribe request: " + getInputMessage());
            return new OutgoingSubscribeResponseMessage(UpnpResponse.Status.PRECONDITION_FAILED);
        }

        Integer timeoutSeconds = requestMessage.getRequestedTimeoutSeconds();

        try {
            subscription = new LocalGENASubscription(service, timeoutSeconds, requestMessage.getCallbackURLs()) {
                public void established() {
                }

                public void ended(CancelReason reason) {
                }

                public void eventReceived() {
                    // The only thing we are interested in, sending an event when the state changes
                    getUpnpService().getConfiguration().getSyncProtocolExecutor().execute(
                            getUpnpService().getProtocolFactory().createSendingEvent(this)
                    );
                }
            };
        } catch (Exception ex) {
            log.warning("Couldn't create local subscription to service: " + Exceptions.unwrap(ex));
            return new OutgoingSubscribeResponseMessage(UpnpResponse.Status.INTERNAL_SERVER_ERROR);
        }

        log.fine("Adding subscription to registry: " + subscription);
        getUpnpService().getRegistry().addLocalSubscription(subscription);

        log.fine("Returning subscription response, waiting to send initial event");
        return new OutgoingSubscribeResponseMessage(subscription);
    }

    @Override
    public void responseSent(StreamResponseMessage responseMessage) {
        if (subscription == null) return; // Preconditions failed very early on
        if (responseMessage != null
                && !responseMessage.getOperation().isFailed()
                && subscription.getCurrentSequence().getValue() == 0) { // Note that renewals should not have 0

            // This is a minor concurrency issue: If we now register on the service and henceforth send a new
            // event message whenever the state of the service changes, there is still a chance that the initial
            // event message arrives later than the first on-change event message. Shouldn't be a problem as the
            // subscriber is supposed to figure out what to do with out-of-sequence messages. I would be
            // surprised though if actual implementations won't crash!
            log.fine("Establishing subscription");
            subscription.registerOnService();
            subscription.establish();

            log.fine("Response to subscription sent successfully, now sending initial event asynchronously");
            getUpnpService().getConfiguration().getAsyncProtocolExecutor().execute(
                    getUpnpService().getProtocolFactory().createSendingEvent(subscription)
            );

        } else if (subscription.getCurrentSequence().getValue() == 0) {
            log.fine("Subscription request's response aborted, not sending initial event");
            if (responseMessage == null) {
                log.fine("Reason: No response at all from subscriber");
            } else {
                log.fine("Reason: " + responseMessage.getOperation());
            }
            log.fine("Removing subscription from registry: " + subscription);
            getUpnpService().getRegistry().removeLocalSubscription(subscription);
        }
    }

    @Override
    public void responseException(Throwable t) {
        if (subscription == null) return; // Nothing to do, we didn't get that far
        log.fine("Response could not be send to subscriber, removing local GENA subscription: " + subscription);
        getUpnpService().getRegistry().removeLocalSubscription(subscription);
    }
}
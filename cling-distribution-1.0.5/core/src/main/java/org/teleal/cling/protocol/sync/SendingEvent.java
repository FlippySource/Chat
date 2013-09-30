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

import java.util.logging.Logger;
import org.teleal.cling.UpnpService;
import org.teleal.cling.model.gena.LocalGENASubscription;
import org.teleal.cling.model.message.StreamResponseMessage;
import org.teleal.cling.model.message.gena.OutgoingEventRequestMessage;
import org.teleal.cling.model.types.UnsignedIntegerFourBytes;
import org.teleal.cling.protocol.SendingSync;

import java.net.URL;

/**
 * Sending GENA event messages to remote subscribers.
 * <p>
 * Any {@link org.teleal.cling.model.gena.LocalGENASubscription} instantiates and executes this protocol
 * when the state of a local service changes. However, a remote subscriber might require event
 * notification messages on more than one callback URL, so this protocol potentially sends
 * many messages. What is returned is always the last response, that is, the response for the
 * message sent to the last callback URL in the list of the subscriber.
 * </p>
 *
 * @author Christian Bauer
 */
public class SendingEvent extends SendingSync<OutgoingEventRequestMessage, StreamResponseMessage> {

    final private static Logger log = Logger.getLogger(SendingEvent.class.getName());

    final protected String subscriptionId;
    final protected OutgoingEventRequestMessage[] requestMessages;
    final protected UnsignedIntegerFourBytes currentSequence;

    public SendingEvent(UpnpService upnpService, LocalGENASubscription subscription) {
        super(upnpService, null); // Special case, we actually need to send several messages to each callback URL

        // TODO: Ugly design! It is critical (concurrency) that we prepare the event messages here, in the constructor thread!

        subscriptionId = subscription.getSubscriptionId();

        requestMessages = new OutgoingEventRequestMessage[subscription.getCallbackURLs().size()];
        int i = 0;
        for (URL url : subscription.getCallbackURLs()) {
            requestMessages[i] = new OutgoingEventRequestMessage(subscription, url);
            getUpnpService().getConfiguration().getGenaEventProcessor().writeBody(requestMessages[i]);
            i++;
        }

        currentSequence = subscription.getCurrentSequence();

        // Always increment sequence now, as (its value) has already been set on the headers and the
        // next event will use the incremented value
        subscription.incrementSequence();
    }

    protected StreamResponseMessage executeSync() {

        log.fine("Sending event for subscription: " + subscriptionId);

        StreamResponseMessage lastResponse = null;

        for (OutgoingEventRequestMessage requestMessage : requestMessages) {

            if (currentSequence.getValue() == 0) {
                log.fine("Sending initial event message to callback URL: " + requestMessage.getUri());
            } else {
                log.fine("Sending event message '"+currentSequence+"' to callback URL: " + requestMessage.getUri());
            }


            // Send request
            lastResponse = getUpnpService().getRouter().send(requestMessage);
            log.fine("Received event callback response: " + lastResponse);

        }

        // It's not really used, so just return the last one - we have only one callback URL most of the
        // time anyway
        return lastResponse;

    }
}
/*
 * Copyright (C) 2011 Teleal GmbH, Switzerland
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

import org.teleal.cling.model.gena.RemoteGENASubscription;
import org.teleal.cling.model.message.StreamResponseMessage;
import org.teleal.cling.model.message.gena.IncomingSubscribeResponseMessage;
import org.teleal.cling.model.message.gena.OutgoingSubscribeRequestMessage;
import org.teleal.cling.UpnpService;
import org.teleal.cling.protocol.SendingSync;

import java.util.logging.Logger;

/**
 * Establishing a GENA event subscription with a remote host.
 * <p>
 * Calls the {@link org.teleal.cling.model.gena.RemoteGENASubscription#establish()} method
 * if the subscription request was responded to correctly.
 * </p>
 * <p>
 * The {@link org.teleal.cling.model.gena.RemoteGENASubscription#fail(org.teleal.cling.model.message.UpnpResponse)}
 * method will be called if the request failed. No response from the remote host is indicated with
 * a <code>null</code> argument value. Note that this is also the response if the subscription has
 * to be aborted early, when no local stream server for callback URL creation is available. This is
 * the case when the local network transport layer is switched off, subscriptions will fail
 * immediately with no response.
 * </p>
 *
 * @author Christian Bauer
 */
public class SendingSubscribe extends SendingSync<OutgoingSubscribeRequestMessage, IncomingSubscribeResponseMessage> {

    final private static Logger log = Logger.getLogger(SendingSubscribe.class.getName());

    final protected RemoteGENASubscription subscription;

    public SendingSubscribe(UpnpService upnpService, RemoteGENASubscription subscription) {
        super(upnpService,
              new OutgoingSubscribeRequestMessage(
                      subscription,
                      subscription.getEventCallbackURLs(
                              upnpService.getRouter().getActiveStreamServers(
                                      subscription.getService().getDevice().getIdentity().getDiscoveredOnLocalAddress()
                              ),
                              upnpService.getConfiguration().getNamespace()
                      )
              )
        );

        this.subscription = subscription;
    }

    protected IncomingSubscribeResponseMessage executeSync() {

        if (!getInputMessage().hasCallbackURLs()) {
            log.fine("Subscription failed, no active local callback URLs available (network disabled?)");
            getUpnpService().getConfiguration().getRegistryListenerExecutor().execute(
                    new Runnable() {
                        public void run() {
                            subscription.fail(null);
                        }
                    }
            );
            return null;
        }

        log.fine("Sending subscription request: " + getInputMessage());

        try {
            // Block incoming (initial) event messages until the subscription is fully registered
            getUpnpService().getRegistry().lockRemoteSubscriptions();

            StreamResponseMessage response = getUpnpService().getRouter().send(getInputMessage());

            if (response == null) {
                log.fine("Subscription failed, no response received");
                getUpnpService().getConfiguration().getRegistryListenerExecutor().execute(
                        new Runnable() {
                            public void run() {
                                subscription.fail(null);
                            }
                        }
                );
                return null;
            }

            final IncomingSubscribeResponseMessage responseMessage = new IncomingSubscribeResponseMessage(response);

            if (response.getOperation().isFailed()) {
                log.fine("Subscription failed, response was: " + responseMessage);
                getUpnpService().getConfiguration().getRegistryListenerExecutor().execute(
                        new Runnable() {
                            public void run() {
                                subscription.fail(responseMessage.getOperation());
                            }
                        }
                );
            } else if (!responseMessage.isVaildHeaders()) {
                log.severe("Subscription failed, invalid or missing (SID, Timeout) response headers");
                getUpnpService().getConfiguration().getRegistryListenerExecutor().execute(
                        new Runnable() {
                            public void run() {
                                subscription.fail(responseMessage.getOperation());
                            }
                        }
                );
            } else {

                log.fine("Subscription established, adding to registry, response was: " + response);
                subscription.setSubscriptionId(responseMessage.getSubscriptionId());
                subscription.setActualSubscriptionDurationSeconds(responseMessage.getSubscriptionDurationSeconds());

                getUpnpService().getRegistry().addRemoteSubscription(subscription);

                getUpnpService().getConfiguration().getRegistryListenerExecutor().execute(
                        new Runnable() {
                            public void run() {
                                subscription.establish();
                            }
                        }
                );

            }
            return responseMessage;
        } finally {
            getUpnpService().getRegistry().unlockRemoteSubscriptions();
        }
    }
}

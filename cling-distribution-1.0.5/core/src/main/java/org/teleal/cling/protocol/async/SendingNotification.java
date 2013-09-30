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

package org.teleal.cling.protocol.async;

import org.teleal.cling.UpnpService;
import org.teleal.cling.model.Location;
import org.teleal.cling.model.NetworkAddress;
import org.teleal.cling.model.message.discovery.OutgoingNotificationRequest;
import org.teleal.cling.model.message.discovery.OutgoingNotificationRequestDeviceType;
import org.teleal.cling.model.message.discovery.OutgoingNotificationRequestRootDevice;
import org.teleal.cling.model.message.discovery.OutgoingNotificationRequestServiceType;
import org.teleal.cling.model.message.discovery.OutgoingNotificationRequestUDN;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.types.NotificationSubtype;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.protocol.SendingAsync;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Sending notification messages for a registered local device.
 * <p>
 * Sends all required (dozens) of messages three times, waits between 0 and 150
 * milliseconds between each bulk sending procedure.
 * </p>
 *
 * @author Christian Bauer
 */
public abstract class SendingNotification extends SendingAsync {

    final private static Logger log = Logger.getLogger(SendingNotification.class.getName());

    private LocalDevice device;

    public SendingNotification(UpnpService upnpService, LocalDevice device) {
        super(upnpService);
        this.device = device;
    }

    public LocalDevice getDevice() {
        return device;
    }

    protected void execute() {

        List<NetworkAddress> activeStreamServers =
            getUpnpService().getRouter().getActiveStreamServers(null);
        if (activeStreamServers.size() == 0) {
            log.fine("Aborting notifications, no active stream servers found (network disabled?)");
            return;
        }

        // Prepare it once, it's the same for each repetition
        List<Location> descriptorLocations = new ArrayList();
        for (NetworkAddress activeStreamServer : activeStreamServers) {
            descriptorLocations.add(
                    new Location(
                            activeStreamServer,
                            getUpnpService().getConfiguration().getNamespace().getDescriptorPath(getDevice())
                    )
            );
        }

        for (int i = 0; i < getBulkRepeat(); i++) {
            try {

                for (Location descriptorLocation : descriptorLocations) {
                    sendMessages(descriptorLocation);
                }

                // UDA 1.0 is silent about this but UDA 1.1 recomments "a few hundred milliseconds"
                log.finer("Sleeping " + getBulkIntervalMilliseconds() + " milliseconds");
                Thread.sleep(getBulkIntervalMilliseconds());

            } catch (InterruptedException ex) {
                log.warning("Advertisement thread was interrupted: " + ex);
            }
        }
    }

    protected int getBulkRepeat() {
        return 3; // UDA 1.0 says maximum 3 times for alive messages, let's just do it for all
    }

    protected int getBulkIntervalMilliseconds() {
        return 150;
    }

    public void sendMessages(Location descriptorLocation) {
        log.finer("Sending root device messages: " + getDevice());
        List<OutgoingNotificationRequest> rootDeviceMsgs =
                createDeviceMessages(getDevice(), descriptorLocation);
        for (OutgoingNotificationRequest upnpMessage : rootDeviceMsgs) {
            getUpnpService().getRouter().send(upnpMessage);
        }

        if (getDevice().hasEmbeddedDevices()) {
            for (LocalDevice embeddedDevice : getDevice().findEmbeddedDevices()) {
                log.finer("Sending embedded device messages: " + embeddedDevice);
                List<OutgoingNotificationRequest> embeddedDeviceMsgs =
                        createDeviceMessages(embeddedDevice, descriptorLocation);
                for (OutgoingNotificationRequest upnpMessage : embeddedDeviceMsgs) {
                    getUpnpService().getRouter().send(upnpMessage);
                }
            }
        }

        List<OutgoingNotificationRequest> serviceTypeMsgs =
                createServiceTypeMessages(getDevice(), descriptorLocation);
        if (serviceTypeMsgs.size() > 0) {
            log.finer("Sending service type messages");
            for (OutgoingNotificationRequest upnpMessage : serviceTypeMsgs) {
                getUpnpService().getRouter().send(upnpMessage);
            }
        }
    }

    protected List<OutgoingNotificationRequest> createDeviceMessages(LocalDevice device,
                                                                     Location descriptorLocation) {
        List<OutgoingNotificationRequest> msgs = new ArrayList();

        // See the tables in UDA 1.0 section 1.1.2

        if (device.isRoot()) {
            msgs.add(
                    new OutgoingNotificationRequestRootDevice(
                            descriptorLocation,
                            device,
                            getNotificationSubtype()
                    )
            );
        }

        msgs.add(
                new OutgoingNotificationRequestUDN(
                        descriptorLocation, device, getNotificationSubtype()
                )
        );
        msgs.add(
                new OutgoingNotificationRequestDeviceType(
                        descriptorLocation, device, getNotificationSubtype()
                )
        );

        return msgs;
    }

    protected List<OutgoingNotificationRequest> createServiceTypeMessages(LocalDevice device,
                                                                          Location descriptorLocation) {
        List<OutgoingNotificationRequest> msgs = new ArrayList();

        for (ServiceType serviceType : device.findServiceTypes()) {
            msgs.add(
                    new OutgoingNotificationRequestServiceType(
                            descriptorLocation, device,
                            getNotificationSubtype(), serviceType
                    )
            );
        }

        return msgs;
    }

    protected abstract NotificationSubtype getNotificationSubtype();

}

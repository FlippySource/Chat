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

package org.teleal.cling.transport.impl;

import org.teleal.cling.transport.Router;
import org.teleal.cling.transport.spi.DatagramProcessor;
import org.teleal.cling.transport.spi.InitializationException;
import org.teleal.cling.transport.spi.MulticastReceiver;
import org.teleal.cling.transport.spi.UnsupportedDataException;

import java.net.DatagramPacket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.logging.Logger;

/**
 * Default implementation based on a UDP <code>MulticastSocket</code>.
 * <p>
 * Thread-safety is guaranteed through synchronization of methods of this service and
 * by the thread-safe underlying socket.
 * </p>
 * @author Christian Bauer
 */
public class MulticastReceiverImpl implements MulticastReceiver<MulticastReceiverConfigurationImpl> {

    private static Logger log = Logger.getLogger(MulticastReceiver.class.getName());

    final protected MulticastReceiverConfigurationImpl configuration;

    protected Router router;
    protected DatagramProcessor datagramProcessor;

    protected NetworkInterface multicastInterface;
    protected InetSocketAddress multicastAddress;
    private MulticastSocket socket;

    public MulticastReceiverImpl(MulticastReceiverConfigurationImpl configuration) {
        this.configuration = configuration;
    }

    public MulticastReceiverConfigurationImpl getConfiguration() {
        return configuration;
    }

    synchronized public void init(NetworkInterface networkInterface, Router router, DatagramProcessor datagramProcessor) throws InitializationException {

        this.router = router;
        this.datagramProcessor = datagramProcessor;
        this.multicastInterface = networkInterface;

        try {

            log.info("Creating wildcard socket (for receiving multicast datagrams) on port: " + configuration.getPort());
            multicastAddress = new InetSocketAddress(configuration.getGroup(), configuration.getPort());

            socket = new MulticastSocket(configuration.getPort());
            socket.setReuseAddress(true);
            socket.setReceiveBufferSize(32768); // Keep a backlog of incoming datagrams if we are not fast enough

            log.info("Joining multicast group: " + multicastAddress + " on network interface: " + multicastInterface.getDisplayName());
            socket.joinGroup(multicastAddress, multicastInterface);

        } catch (Exception ex) {
            throw new InitializationException("Could not initialize " + getClass().getSimpleName() + ": " + ex);
        }
    }

    synchronized public void stop() {
        if (socket != null && !socket.isClosed()) {
            try {
                log.fine("Leaving multicast group");
                socket.leaveGroup(multicastAddress, multicastInterface);
                // Well this doesn't work and I have no idea why I get "java.net.SocketException: Can't assign requested address"
            } catch (Exception ex) {
                log.fine("Could not leave multicast group: " + ex);
            }
            // So... just close it and ignore the log messages
            socket.close();
        }
    }

    public void run() {

        log.fine("Entering blocking receiving loop, listening for UDP datagrams on: " + socket.getLocalAddress());
        while (true) {

            try {
                byte[] buf = new byte[getConfiguration().getMaxDatagramBytes()];
                DatagramPacket datagram = new DatagramPacket(buf, buf.length);

                socket.receive(datagram);

                InetAddress receivedOnLocalAddress =
                        router.getNetworkAddressFactory().getLocalAddress(
                                multicastInterface,
                                multicastAddress.getAddress() instanceof Inet6Address,
                                datagram.getAddress()
                        );

                log.fine(
                        "UDP datagram received from: " + datagram.getAddress().getHostAddress() 
                                + ":" + datagram.getPort()
                                + " on local interface: " + multicastInterface.getDisplayName()
                                + " and address: " + receivedOnLocalAddress.getHostAddress()
                );

                router.received(datagramProcessor.read(receivedOnLocalAddress, datagram));

            } catch (SocketException ex) {
                log.fine("Socket closed");
                break;
            } catch (UnsupportedDataException ex) {
                log.info("Could not read datagram: " + ex.getMessage());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        try {
            if (!socket.isClosed()) {
                log.fine("Closing multicast socket");
                socket.close();
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


}

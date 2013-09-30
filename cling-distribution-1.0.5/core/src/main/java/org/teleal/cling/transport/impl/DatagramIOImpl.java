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

import org.teleal.cling.model.message.OutgoingDatagramMessage;
import org.teleal.cling.transport.Router;
import org.teleal.cling.transport.spi.DatagramIO;
import org.teleal.cling.transport.spi.DatagramProcessor;
import org.teleal.cling.transport.spi.InitializationException;
import org.teleal.cling.transport.spi.UnsupportedDataException;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.logging.Logger;

/**
 * Default implementation based on a single shared (receive/send) UDP <code>MulticastSocket</code>.
 * <p>
 * Although we do not receive multicast datagrams with this service, sending multicast
 * datagrams with a configuration time-to-live requires a <code>MulticastSocket</code>.
 * </p>
 * <p>
 * Thread-safety is guaranteed through synchronization of methods of this service and
 * by the thread-safe underlying socket.
 * </p>
 * @author Christian Bauer
 */
public class DatagramIOImpl implements DatagramIO<DatagramIOConfigurationImpl> {

    private static Logger log = Logger.getLogger(DatagramIO.class.getName());

    /* Implementation notes for unicast/multicast UDP:

    http://forums.sun.com/thread.jspa?threadID=771852
    http://mail.openjdk.java.net/pipermail/net-dev/2008-December/000497.html
    https://jira.jboss.org/jira/browse/JGRP-978
    http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4701650

     */

    final protected DatagramIOConfigurationImpl configuration;

    protected Router router;
    protected DatagramProcessor datagramProcessor;

    protected InetSocketAddress localAddress;
    protected MulticastSocket socket; // For sending unicast & multicast, and reveiving unicast

    public DatagramIOImpl(DatagramIOConfigurationImpl configuration) {
        this.configuration = configuration;
    }

    public DatagramIOConfigurationImpl getConfiguration() {
        return configuration;
    }

    synchronized public void init(InetAddress bindAddress, Router router, DatagramProcessor datagramProcessor) throws InitializationException {

        this.router = router;
        this.datagramProcessor = datagramProcessor;

        try {

            // TODO: UPNP VIOLATION: The spec does not prohibit using the 1900 port here again, however, the
            // Netgear ReadyNAS miniDLNA implementation will no longer answer if it has to send search response
            // back via UDP unicast to port 1900... so we use an ephemeral port
            log.info("Creating bound socket (for datagram input/output) on: " + bindAddress);
            localAddress = new InetSocketAddress(bindAddress, 0);
            socket = new MulticastSocket(localAddress);
            socket.setTimeToLive(configuration.getTimeToLive());
            socket.setReceiveBufferSize(32768); // Keep a backlog of incoming datagrams if we are not fast enough

        } catch (Exception ex) {
            throw new InitializationException("Could not initialize " + getClass().getSimpleName() + ": " + ex);
        }
    }

    synchronized public void stop() {
        if (socket != null && !socket.isClosed()) {
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

                log.fine(
                        "UDP datagram received from: "
                                + datagram.getAddress().getHostAddress()
                                + ":" + datagram.getPort()
                                + " on: " + localAddress
                );


                router.received(datagramProcessor.read(localAddress.getAddress(), datagram));

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
                log.fine("Closing unicast socket");
                socket.close();
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    synchronized public void send(OutgoingDatagramMessage message) {
        log.fine("Sending message from address: " + localAddress);
        DatagramPacket packet = datagramProcessor.write(message);
        log.fine("Sending UDP datagram packet to: " + message.getDestinationAddress() + ":" + message.getDestinationPort());
        send(packet);
    }

    synchronized public void send(DatagramPacket datagram) {
        log.fine("Sending message from address: " + localAddress);

        try {
            socket.send(datagram);
        } catch (SocketException ex) {
            log.fine("Socket closed, aborting datagram send to: " + datagram.getAddress());
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}

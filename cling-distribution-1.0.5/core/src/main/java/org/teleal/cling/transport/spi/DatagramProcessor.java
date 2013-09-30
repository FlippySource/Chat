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

package org.teleal.cling.transport.spi;


import org.teleal.cling.model.message.IncomingDatagramMessage;
import org.teleal.cling.model.message.OutgoingDatagramMessage;

import java.net.DatagramPacket;
import java.net.InetAddress;

/**
 * Reads and creates UDP datagrams from and into UPnP messages.
 * <p>
 * An implementation of this interface has to be thread-safe.
 * </p>
 *
 * @author Christian Bauer
 */
public interface DatagramProcessor {

    /**
     * Reads the datagram and instantiates a message.
     * <p>
     * The message is either a {@link org.teleal.cling.model.message.UpnpRequest} or
     * a {@link org.teleal.cling.model.message.UpnpResponse} operation type.
     * </p>
     *
     * @param receivedOnAddress The address of the socket on which this datagram was received.
     * @param datagram The received UDP datagram.
     * @return The populated instance.
     * @throws UnsupportedDataException If the datagram could not be read, or didn't contain required data.
     */
    public IncomingDatagramMessage read(InetAddress receivedOnAddress, DatagramPacket datagram) throws UnsupportedDataException;

    /**
     * Creates a UDP datagram with the content of a message.
     * <p>
     * The outgoing message might be a {@link org.teleal.cling.model.message.UpnpRequest} or a
     * {@link org.teleal.cling.model.message.UpnpResponse}.
     * </p>
     *
     * @param message The outgoing datagram message.
     * @return An actual UDP datagram.
     * @throws UnsupportedDataException If the datagram could not be created.
     */
    public DatagramPacket write(OutgoingDatagramMessage message) throws UnsupportedDataException;

}



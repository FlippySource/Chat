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

import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.control.ActionRequestMessage;
import org.teleal.cling.model.message.control.ActionResponseMessage;

/**
 * Converts UPnP SOAP messages from/to action invocations.
 * <p>
 * The UPnP protocol layer processes local and remote {@link org.teleal.cling.model.action.ActionInvocation}
 * instances. The UPnP transport layer accepts and returns {@link org.teleal.cling.model.message.StreamRequestMessage}s
 * and {@link org.teleal.cling.model.message.StreamResponseMessage}s. This processor is an adapter between the
 * two layers, reading and writing SOAP content.
 * </p>
 *
 * @author Christian Bauer
 */
public interface SOAPActionProcessor {

    /**
     * Converts the given invocation input into SOAP XML content, setting on the given request message.
     *
     * @param requestMessage The request message on which the SOAP content is set.
     * @param actionInvocation The action invocation from which input argument values are read.
     * @throws UnsupportedDataException
     */
    public void writeBody(ActionRequestMessage requestMessage, ActionInvocation actionInvocation) throws UnsupportedDataException;

    /**
     * Converts the given invocation output into SOAP XML content, setting on the given response message.
     *
     * @param responseMessage The response message on which the SOAP content is set.
     * @param actionInvocation The action invocation from which output argument values are read.
     * @throws UnsupportedDataException
     */
    public void writeBody(ActionResponseMessage responseMessage, ActionInvocation actionInvocation) throws UnsupportedDataException;

    /**
     * Converts SOAP XML content of the request message and sets input argument values on the given invocation.
     *
     * @param requestMessage The request message from which SOAP content is read.
     * @param actionInvocation The action invocation on which input argument values are set.
     * @throws UnsupportedDataException
     */
    public void readBody(ActionRequestMessage requestMessage, ActionInvocation actionInvocation) throws UnsupportedDataException;

    /**
     * Converts SOAP XML content of the response message and sets output argument values on the given invocation.
     *
     * @param responseMsg The response message from which SOAP content is read.
     * @param actionInvocation The action invocation on which output argument values are set.
     * @throws UnsupportedDataException
     */
    public void readBody(ActionResponseMessage responseMsg, ActionInvocation actionInvocation) throws UnsupportedDataException;

}

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

package org.teleal.cling.model.message.control;

import org.teleal.cling.model.message.UpnpMessage;

/**
 * A narrowing interface for action request/response messages.
 *
 * @author Christian Bauer
 */
public interface ActionMessage {

    public String getActionNamespace();

    public Object getBody();
    public UpnpMessage.BodyType getBodyType();
    public String getBodyString();
    public byte[] getBodyBytes();

    public void setBody(UpnpMessage.BodyType bodyType, Object body);
    
}

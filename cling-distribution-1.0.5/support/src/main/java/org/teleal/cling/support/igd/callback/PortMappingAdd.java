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

package org.teleal.cling.support.igd.callback;

import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.controlpoint.ControlPoint;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.support.model.PortMapping;

/**
 * @author Christian Bauer
 */
public abstract class PortMappingAdd extends ActionCallback {

    final protected PortMapping portMapping;

    public PortMappingAdd(Service service, PortMapping portMapping) {
        this(service, null, portMapping);
    }

    protected PortMappingAdd(Service service, ControlPoint controlPoint, PortMapping portMapping) {
        super(new ActionInvocation(service.getAction("AddPortMapping")), controlPoint);

        this.portMapping = portMapping;

        getActionInvocation().setInput("NewExternalPort", portMapping.getExternalPort());
        getActionInvocation().setInput("NewProtocol", portMapping.getProtocol());
        getActionInvocation().setInput("NewInternalClient", portMapping.getInternalClient());
        getActionInvocation().setInput("NewInternalPort", portMapping.getInternalPort());
        getActionInvocation().setInput("NewLeaseDuration", portMapping.getLeaseDurationSeconds());
        getActionInvocation().setInput("NewEnabled", portMapping.isEnabled());
        if (portMapping.hasRemoteHost())
            getActionInvocation().setInput("NewRemoteHost", portMapping.getRemoteHost());
        if (portMapping.hasDescription())
            getActionInvocation().setInput("NewPortMappingDescription", portMapping.getDescription());

    }

}

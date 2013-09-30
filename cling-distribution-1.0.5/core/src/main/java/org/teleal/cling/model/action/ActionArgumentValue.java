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

package org.teleal.cling.model.action;

import org.teleal.cling.model.VariableValue;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.meta.ActionArgument;
import org.teleal.cling.model.types.InvalidValueException;

/**
 * Represents the value of an action input or output argument.
 *
 * @author Christian Bauer
 */
public class ActionArgumentValue<S extends Service> extends VariableValue {

    final private ActionArgument<S> argument;

    public ActionArgumentValue(ActionArgument<S> argument, Object value) throws InvalidValueException {
        super(argument.getDatatype(), value != null && value.getClass().isEnum() ? value.toString() : value);
        this.argument = argument;
    }

    public ActionArgument<S> getArgument() {
        return argument;
    }

}
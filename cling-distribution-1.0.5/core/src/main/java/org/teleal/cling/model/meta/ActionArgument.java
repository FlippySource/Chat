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

package org.teleal.cling.model.meta;

import org.teleal.cling.model.Validatable;
import org.teleal.cling.model.ValidationError;
import org.teleal.cling.model.types.Datatype;
import org.teleal.cling.model.ModelUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Describes a single action argument, either input or output.
 * <p>
 * No, I haven't  figured out so far what the "return value" thingy is good for.
 * </p>
 *
 * @author Christian Bauer
 */
public class ActionArgument<S extends Service> implements Validatable {

    final private static Logger log = Logger.getLogger(ActionArgument.class.getName());

    public enum Direction {
        IN, OUT
    }

    final private String name;
    final private String[] aliases;
    final private String relatedStateVariableName;
    final private Direction direction;
    final private boolean returnValue;     // TODO: What is this stuff good for anyway?

    // Package mutable state
    private Action<S> action;

    public ActionArgument(String name, String relatedStateVariableName, Direction direction) {
        this(name, new String[0], relatedStateVariableName, direction, false);
    }

    public ActionArgument(String name, String[] aliases, String relatedStateVariableName, Direction direction) {
        this(name, aliases, relatedStateVariableName, direction, false);
    }
    
    public ActionArgument(String name, String relatedStateVariableName, Direction direction, boolean returnValue) {
        this(name, new String[0], relatedStateVariableName, direction, returnValue);
    }

    public ActionArgument(String name, String[] aliases, String relatedStateVariableName, Direction direction, boolean returnValue) {
        this.name = name;
        this.aliases = aliases;
        this.relatedStateVariableName = relatedStateVariableName;
        this.direction = direction;
        this.returnValue = returnValue;
    }

    public String getName() {
        return name;
    }

    public String[] getAliases() {
        return aliases;
    }

    public boolean isNameOrAlias(String name) {
        if (getName().equals(name)) return true;
        for (String alias : aliases) {
            if (alias.equals(name)) return true;
        }
        return false;
    }

    public String getRelatedStateVariableName() {
        return relatedStateVariableName;
    }

    public Direction getDirection() {
        return direction;
    }

    public boolean isReturnValue() {
        return returnValue;
    }

    public Action<S> getAction() {
        return action;
    }

    void setAction(Action<S> action) {
        if (this.action != null)
            throw new IllegalStateException("Final value has been set already, model is immutable");
        this.action = action;
    }

    public Datatype getDatatype() {
        return getAction().getService().getDatatype(this);
    }

    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList();

        if (getName() == null || getName().length() == 0) {
            errors.add(new ValidationError(
                    getClass(),
                    "name",
                    "Argument without name of: " + getAction()
            ));
        } else if (!ModelUtil.isValidUDAName(getName())) {
            log.warning("UPnP specification violation of: " + getAction().getService().getDevice());
            log.warning("Invalid argument name: " + this);
        } else if (getName().length() > 32) {
            log.warning("UPnP specification violation of: " + getAction().getService().getDevice());
            log.warning("Argument name should be less than 32 characters: " + this);
        }

        if (getDirection() == null) {
            errors.add(new ValidationError(
                    getClass(),
                    "direction",
                    "Argument '"+getName()+"' requires a direction, either IN or OUT"
            ));
        }

        if (isReturnValue() && getDirection() != ActionArgument.Direction.OUT) {
            errors.add(new ValidationError(
                    getClass(),
                    "direction",
                    "Return value argument '" + getName() + "' must be direction OUT"
            ));
        }

        return errors;
    }

    public ActionArgument<S> deepCopy() {
        return new ActionArgument<S>(
                getName(),
                getAliases(),
                getRelatedStateVariableName(),
                getDirection(),
                isReturnValue()
        );
    }

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ", " + getDirection() + ") " + getName();
    }
}

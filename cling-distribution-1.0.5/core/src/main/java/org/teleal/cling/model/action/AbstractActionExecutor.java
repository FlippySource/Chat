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

import org.teleal.cling.model.Command;
import org.teleal.cling.model.ServiceManager;
import org.teleal.cling.model.meta.Action;
import org.teleal.cling.model.meta.ActionArgument;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.state.StateVariableAccessor;
import org.teleal.cling.model.types.ErrorCode;
import org.teleal.cling.model.types.InvalidValueException;
import org.teleal.common.util.Exceptions;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shared procedures for action executors based on an actual service implementation instance.
 *
 * @author Christian Bauer
 */
public abstract class AbstractActionExecutor implements ActionExecutor {

    private static Logger log = Logger.getLogger(AbstractActionExecutor.class.getName());

    protected Map<ActionArgument<LocalService>, StateVariableAccessor> outputArgumentAccessors = new HashMap();

    protected AbstractActionExecutor() {
    }

    protected AbstractActionExecutor(Map<ActionArgument<LocalService>, StateVariableAccessor> outputArgumentAccessors) {
        this.outputArgumentAccessors = outputArgumentAccessors;
    }

    public Map<ActionArgument<LocalService>, StateVariableAccessor> getOutputArgumentAccessors() {
        return outputArgumentAccessors;
    }

    /**
     * Obtains the service implementation instance from the {@link org.teleal.cling.model.ServiceManager}, handles exceptions.
     */
    public void execute(final ActionInvocation<LocalService> actionInvocation) {

        log.fine("Invoking on local service: " + actionInvocation);

        final LocalService service = actionInvocation.getAction().getService();

        try {

            if (service.getManager() == null) {
                throw new IllegalStateException("Service has no implementation factory, can't get service instance");
            }

            service.getManager().execute(new Command() {
                public void execute(ServiceManager serviceManager) throws Exception {
                    AbstractActionExecutor.this.execute(
                            actionInvocation,
                            serviceManager.getImplementation()
                    );
                }

                @Override
                public String toString() {
                    return "Action invocation: " + actionInvocation.getAction();
                }
            });

        } catch (ActionException ex) {
            log.fine("ActionException thrown by service method, wrapping in invocation and returning: " + ex);
            log.log(Level.FINE, "Exception root cause: ", Exceptions.unwrap(ex));
            actionInvocation.setFailure(ex);
        } catch (Exception ex) {
            log.fine("Exception thrown by execution, wrapping in ActionException and returning: " + ex);
            log.log(Level.FINE, "Exception root cause: ", Exceptions.unwrap(ex));
            actionInvocation.setFailure(
                    new ActionException(
                            ErrorCode.ACTION_FAILED,
                            "Action method invocation failed: " + (ex.getMessage() != null ? ex.getMessage() : ex.toString()),
                            ex
                    )
            );
        }
    }

    protected abstract void execute(ActionInvocation<LocalService> actionInvocation, Object serviceImpl) throws Exception;

    /**
     * Reads the output arguments after an action execution using accessors.
     *
     * @param action The action of which the output arguments are read.
     * @param instance The instance on which the accessors will be invoked.
     * @return <code>null</code> if the action has no output arguments, a single instance if it has one, an
     *         <code>Object[]</code> otherwise.
     * @throws Exception
     */
    protected Object readOutputArgumentValues(Action<LocalService> action, Object instance) throws Exception {
        Object[] results = new Object[action.getOutputArguments().length];
        log.fine("Attempting to retrieve output argument values using accessor: " + results.length);

        int i = 0;
        for (ActionArgument outputArgument : action.getOutputArguments()) {
            log.finer("Calling acccessor method for: " + outputArgument);

            StateVariableAccessor accessor = getOutputArgumentAccessors().get(outputArgument);
            if (accessor != null) {
                log.fine("Calling accessor to read output argument value: " + accessor);
                results[i++] = accessor.read(instance);
            } else {
                throw new IllegalStateException("No accessor bound for: " + outputArgument);
            }
        }

        if (results.length == 1) {
            return results[0];
        }
        return results.length > 0 ? results : null;
    }

    /**
     * Sets the output argument value on the {@link org.teleal.cling.model.action.ActionInvocation}, considers string conversion.
     */
    protected void setOutputArgumentValue(ActionInvocation<LocalService> actionInvocation, ActionArgument<LocalService> argument, Object result)
            throws ActionException {

        LocalService service = actionInvocation.getAction().getService();

        if (result != null) {
            try {
                if (service.isStringConvertibleType(result)) {
                    log.fine("Result of invocation matches convertible type, setting toString() single output argument value");
                    actionInvocation.setOutput(new ActionArgumentValue(argument, result.toString()));
                } else {
                    log.fine("Result of invocation is Object, setting single output argument value");
                    actionInvocation.setOutput(new ActionArgumentValue(argument, result));
                }
            } catch (InvalidValueException ex) {
                throw new ActionException(
                        ErrorCode.ARGUMENT_VALUE_INVALID,
                        "Wrong type or invalid value for '" + argument.getName() + "': " + ex.getMessage(),
                        ex
                );
            }
        } else {

            log.fine("Result of invocation is null, not setting any output argument value(s)");
        }

    }

}

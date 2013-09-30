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

import org.teleal.cling.model.meta.ActionArgument;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.state.StateVariableAccessor;
import org.teleal.cling.model.types.ErrorCode;
import org.teleal.common.util.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Invokes methods on a service implementation instance with reflection.
 *
 * @author Christian Bauer
 */
public class MethodActionExecutor extends AbstractActionExecutor {

    private static Logger log = Logger.getLogger(MethodActionExecutor.class.getName());

    protected Method method;

    public MethodActionExecutor(Method method) {
        this.method = method;
    }

    public MethodActionExecutor(Map<ActionArgument<LocalService>, StateVariableAccessor> outputArgumentAccessors, Method method) {
        super(outputArgumentAccessors);
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }

    @Override
    protected void execute(ActionInvocation<LocalService> actionInvocation, Object serviceImpl) throws Exception {

        // Find the "real" parameters of the method we want to call, and create arguments
        Object[] inputArgumentValues = createInputArgumentValues(actionInvocation, method);

        // Simple case: no output arguments
        if (!actionInvocation.getAction().hasOutputArguments()) {
            log.fine("Calling local service method with no output arguments: " + method);
            Reflections.invoke(method, serviceImpl, inputArgumentValues);
            return;
        }

        boolean isVoid = method.getReturnType().equals(Void.TYPE);

        log.fine("Calling local service method with output arguments: " + method);
        Object result;
        boolean isArrayResultProcessed = true;
        if (isVoid) {

            log.fine("Action method is void, calling declared accessors(s) on service instance to retrieve ouput argument(s)");
            Reflections.invoke(method, serviceImpl, inputArgumentValues);
            result = readOutputArgumentValues(actionInvocation.getAction(), serviceImpl);

        } else if (isUseOutputArgumentAccessors(actionInvocation)) {

            log.fine("Action method is not void, calling declared accessor(s) on returned instance to retrieve ouput argument(s)");
            Object returnedInstance = Reflections.invoke(method, serviceImpl, inputArgumentValues);
            result = readOutputArgumentValues(actionInvocation.getAction(), returnedInstance);

        } else {

            log.fine("Action method is not void, using returned value as (single) output argument");
            result = Reflections.invoke(method, serviceImpl, inputArgumentValues);
            isArrayResultProcessed = false; // We never want to process e.g. Byte[] as individual variable values
        }

        ActionArgument<LocalService>[] outputArgs = actionInvocation.getAction().getOutputArguments();

        if (isArrayResultProcessed && result instanceof Object[]) {
            Object[] results = (Object[]) result;
            log.fine("Accessors returned Object[], setting output argument values: " + results.length);
            for (int i = 0; i < outputArgs.length; i++) {
                setOutputArgumentValue(actionInvocation, outputArgs[i], results[i]);
            }
        } else if (outputArgs.length == 1) {
            setOutputArgumentValue(actionInvocation, outputArgs[0], result);
        } else {
            throw new ActionException(
                    ErrorCode.ACTION_FAILED,
                    "Method return does not match required number of output arguments: " + outputArgs.length
            );
        }

    }

    protected boolean isUseOutputArgumentAccessors(ActionInvocation<LocalService> actionInvocation) {
        for (ActionArgument argument : actionInvocation.getAction().getOutputArguments()) {
            // If there is one output argument for which we have an accessor, all arguments need accessors
            if (getOutputArgumentAccessors().get(argument) != null) {
                return true;
            }
        }
        return false;
    }

    protected Object[] createInputArgumentValues(ActionInvocation<LocalService> actionInvocation, Method method) throws ActionException {

        LocalService service = actionInvocation.getAction().getService();

        Object[] values = new Object[actionInvocation.getAction().getInputArguments().length];
        int i = 0;
        for (ActionArgument<LocalService> argument : actionInvocation.getAction().getInputArguments()) {

            Class methodParameterType = method.getParameterTypes()[i];

            ActionArgumentValue<LocalService> inputValue = actionInvocation.getInput(argument);

            // If it's a primitive argument, we need a value
            if (methodParameterType.isPrimitive() && (inputValue == null || inputValue.toString().length() == 0))
                throw new ActionException(
                        ErrorCode.ARGUMENT_VALUE_INVALID,
                        "Primitive action method argument '" + argument.getName() + "' requires input value, can't be null or empty string"
                );

            // It's not primitive and we have no value, that's fine too
            if (inputValue == null) {
                values[i++] = null;
                continue;
            }

            // If it's not null, maybe it was a string-convertible type, if so, try to instantiate it
            String inputCallValueString = inputValue.toString();
            // Empty string means null and we can't instantiate Enums!
            if (inputCallValueString.length() > 0 && service.isStringConvertibleType(methodParameterType) && !methodParameterType.isEnum()) {
                try {
                    Constructor<String> ctor = methodParameterType.getConstructor(String.class);
                    log.finer("Creating new input argument value instance with String.class constructor of type: " + methodParameterType);
                    Object o = ctor.newInstance(inputCallValueString);
                    values[i++] = o;
                } catch (Exception ex) {
                    ex.printStackTrace(System.err);
                    throw new ActionException(
                            ErrorCode.ARGUMENT_VALUE_INVALID, "Can't convert input argment string to desired type of '" + argument.getName() + "': " + ex
                    );
                }
            } else {
                // Or if it wasn't, just use the value without any conversion
                values[i++] = inputValue.getValue();
            }
        }
        return values;
    }

}

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

package org.teleal.cling.model.gena;

import org.teleal.cling.model.Constants;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.state.StateVariableValue;
import org.teleal.cling.model.types.UnsignedIntegerFourBytes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An established subscription, with identifer, expiration duration, sequence handling, and state variable values.
 * <p>
 * For every subscription, no matter if it's an incoming subscription to a local service,
 * or a local control point subscribing to a remote servce, an instance is maintained by
 * the {@link org.teleal.cling.registry.Registry}.
 * </p>
 *
 * @author Christian Bauer
 */
public abstract class GENASubscription<S extends Service> {

    protected S service;
    protected String subscriptionId;
    protected int requestedDurationSeconds = Constants.DEFAULT_SUBSCRIPTION_DURATION_SECONDS;
    protected int actualDurationSeconds;
    protected UnsignedIntegerFourBytes currentSequence;
    protected Map<String, StateVariableValue<S>> currentValues = new LinkedHashMap();

    /**
     * Defaults to {@link org.teleal.cling.model.Constants#DEFAULT_SUBSCRIPTION_DURATION_SECONDS}.
     */
    protected GENASubscription(S  service) {
        this.service = service;
    }

    public GENASubscription(S service, int requestedDurationSeconds) {
        this(service);
        this.requestedDurationSeconds = requestedDurationSeconds;
    }

    synchronized public S getService() {
        return service;
    }

    synchronized public String getSubscriptionId() {
        return subscriptionId;
    }

    synchronized public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    synchronized public int getRequestedDurationSeconds() {
        return requestedDurationSeconds;
    }

    synchronized public int getActualDurationSeconds() {
        return actualDurationSeconds;
    }

    synchronized public void setActualSubscriptionDurationSeconds(int seconds) {
        this.actualDurationSeconds = seconds;
    }

    synchronized public UnsignedIntegerFourBytes getCurrentSequence() {
        return currentSequence;
    }

    synchronized public Map<String, StateVariableValue<S>> getCurrentValues() {
        return currentValues;
    }

    public abstract void established();
    public abstract void eventReceived();

    @Override
    public String toString() {
        return "(GENASubscription, SID: " + getSubscriptionId() + ", SEQUENCE: " + getCurrentSequence() + ")";
    }
}

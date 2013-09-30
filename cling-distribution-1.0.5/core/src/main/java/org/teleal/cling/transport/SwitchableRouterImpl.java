/*
 * Copyright (C) 2011 Teleal GmbH, Switzerland
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

package org.teleal.cling.transport;

import org.teleal.cling.UpnpServiceConfiguration;
import org.teleal.cling.model.NetworkAddress;
import org.teleal.cling.model.message.IncomingDatagramMessage;
import org.teleal.cling.model.message.OutgoingDatagramMessage;
import org.teleal.cling.model.message.StreamRequestMessage;
import org.teleal.cling.model.message.StreamResponseMessage;
import org.teleal.cling.protocol.ProtocolFactory;
import org.teleal.cling.transport.spi.InitializationException;
import org.teleal.cling.transport.spi.NetworkAddressFactory;
import org.teleal.cling.transport.spi.UpnpStream;
import org.teleal.common.util.Exceptions;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

/**
 * Default implementation of switchable network message router.
 * <p>
 * This implementation is actually wrapping a regular
 * {@link org.teleal.cling.transport.RouterImpl}, making it on/off
 * switchable.
 * </p>
 * <p>
 * If the router can't be enabled (e.g. an exception occurs during
 * socket binding), a warning log message will be printed. You can customize this behavior by
 * overriding {@link #handleStartFailure(org.teleal.cling.transport.spi.InitializationException)}.
 * </p>
 * <p>
 * You have to expect that {@link #disable()} and @{link #enable()} will throw a
 * {@link RouterLockAcquisitionException}. We do not manually abort ongoing HTTP stream
 * connections, they might block access to the underlying synchronized router instance
 * when you try to disable/enable it. However, the only situation in which you should get this
 * exception is when an the response from an HTTP server is taking longer than the timeout
 * of this classes' lock acquisition routine, see {@link #getLockTimeoutMillis()}. This
 * might be the case if the response is large and/or the network connection is slow, but
 * not slow enough to run into connection/data read timeouts.
 * </p>
 *
 * @author Christian Bauer
 */
public class SwitchableRouterImpl implements SwitchableRouter {

    final private static Logger log = Logger.getLogger(Router.class.getName());

    final private UpnpServiceConfiguration configuration;
    final private ProtocolFactory protocolFactory;

    private Router router;
    protected ReentrantReadWriteLock routerLock = new ReentrantReadWriteLock(true);
    protected Lock readLock = routerLock.readLock();
    protected Lock writeLock = routerLock.writeLock();

    public SwitchableRouterImpl(UpnpServiceConfiguration configuration, ProtocolFactory protocolFactory) {
        this.configuration = configuration;
        this.protocolFactory = protocolFactory;
    }

    public UpnpServiceConfiguration getConfiguration() {
        return configuration;
    }

    public ProtocolFactory getProtocolFactory() {
        return protocolFactory;
    }

    public boolean isEnabled() throws RouterLockAcquisitionException {
        lock(readLock);
        try {
            return router != null;
        } finally {
            unlock(readLock);
        }
    }

    public boolean enable() throws RouterLockAcquisitionException {
        lock(writeLock);
        try {
            if (router == null) {
                try {
                    log.fine("Enabling network transport router");
                    router = new RouterImpl(getConfiguration(), getProtocolFactory());
                    return true;
                } catch (InitializationException ex) {
                    handleStartFailure(ex);
                }
            }
            return false;
        } finally {
            unlock(writeLock);
        }
    }

    public void handleStartFailure(InitializationException ex) {
        log.severe("Unable to initialize network router: " + ex);
        log.severe("Cause: " + Exceptions.unwrap(ex));
    }

    public boolean disable() throws RouterLockAcquisitionException {
        lock(writeLock);
        try {
            if (router != null) {
                log.fine("Disabling network transport router");
                router.shutdown();
                router = null;
                return true;
            }
            return false;
        } finally {
            unlock(writeLock);
        }
    }

    public NetworkAddressFactory getNetworkAddressFactory() throws RouterLockAcquisitionException {
        lock(readLock);
        try {
            return router != null ? router.getNetworkAddressFactory() : new DisabledNetworkAddressFactory();
        } finally {
            unlock(readLock);
        }
    }

    public List<NetworkAddress> getActiveStreamServers(InetAddress preferredAddress) throws RouterLockAcquisitionException {
        lock(readLock);
        try {
            return router != null ? router.getActiveStreamServers(preferredAddress) : Collections.EMPTY_LIST;
        } finally {
            unlock(readLock);
        }
    }

    public void shutdown() throws RouterLockAcquisitionException {
        disable();
    }

    public void received(IncomingDatagramMessage msg) throws RouterLockAcquisitionException {
        lock(readLock);
        try {
            if (router != null) router.received(msg);
        } finally {
            unlock(readLock);
        }
    }

    public void received(UpnpStream stream) throws RouterLockAcquisitionException {
        lock(readLock);
        try {
            if (router != null) router.received(stream);
        } finally {
            unlock(readLock);
        }
    }

    public void send(OutgoingDatagramMessage msg) throws RouterLockAcquisitionException {
        lock(readLock);
        try {
            if (router != null) router.send(msg);
        } finally {
            unlock(readLock);
        }
    }

    public StreamResponseMessage send(StreamRequestMessage msg) throws RouterLockAcquisitionException {
        lock(readLock);
        try {
            return router != null ? router.send(msg) : null;
        } finally {
            unlock(readLock);
        }
    }

    public void broadcast(byte[] bytes) throws RouterLockAcquisitionException {
        lock(readLock);
        try {
            if (router != null) router.broadcast(bytes);
        } finally {
            unlock(readLock);
        }
    }

    protected void lock(Lock lock, int timeoutMilliseconds) throws RouterLockAcquisitionException {
        try {
            log.finest("Trying to obtain lock with timeout milliseconds '" + timeoutMilliseconds + "': " + lock.getClass().getSimpleName());
            if (lock.tryLock(timeoutMilliseconds, TimeUnit.MILLISECONDS)) {
                log.finest("Acquired router lock: " + lock.getClass().getSimpleName());
            } else {
                throw new RouterLockAcquisitionException("Failed to acquire router lock: " + lock.getClass().getSimpleName());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to acquire router lock: " + lock.getClass().getSimpleName(), e);
        }
    }

    protected void lock(Lock lock) throws RouterLockAcquisitionException {
        lock(lock, getLockTimeoutMillis());
    }

    protected void unlock(Lock lock) {
        log.finest("Releasing router lock: " + lock.getClass().getSimpleName());
        lock.unlock();
    }

    /**
     * @return Defaults to 6 seconds, should be longer than the HTTP client
     *         request connection/data read timeouts. Should be longer than
     *         it takes the router to be started/shutdown.
     */
    protected int getLockTimeoutMillis() {
        return 6000;
    }

    class DisabledNetworkAddressFactory implements NetworkAddressFactory {
        public InetAddress getMulticastGroup() {
            return null;
        }

        public int getMulticastPort() {
            return 0;
        }

        public int getStreamListenPort() {
            return 0;
        }

        public NetworkInterface[] getNetworkInterfaces() {
            return new NetworkInterface[0];
        }

        public InetAddress[] getBindAddresses() {
            return new InetAddress[0];
        }

        public byte[] getHardwareAddress(InetAddress inetAddress) {
            return new byte[0];
        }

        public InetAddress getBroadcastAddress(InetAddress inetAddress) {
            return null;
        }

        public InetAddress getLocalAddress(NetworkInterface networkInterface,
                                           boolean isIPv6,
                                           InetAddress remoteAddress)
                throws IllegalStateException {
            return null;
        }
    }

    public static class RouterLockAcquisitionException extends RuntimeException {
        public RouterLockAcquisitionException() {
            super();
        }

        public RouterLockAcquisitionException(String s) {
            super(s);
        }

        public RouterLockAcquisitionException(String s, Throwable throwable) {
            super(s, throwable);
        }

        public RouterLockAcquisitionException(Throwable throwable) {
            super(throwable);
        }
    }
}

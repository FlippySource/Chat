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

package org.teleal.cling.transport.impl.apache;

import org.apache.http.HttpRequestFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.teleal.cling.transport.Router;
import org.teleal.cling.transport.spi.InitializationException;
import org.teleal.cling.transport.spi.StreamServer;
import org.teleal.cling.transport.spi.UpnpStream;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Logger;

/**
 * Implementation based on <a href="http://hc.apache.org/">Apache HTTP Components</a>.
 * <p>
 * This implementation works on Android.
 * </p>
 * <p>
 * Thread-safety is guaranteed through synchronization of methods of this service and
 * by the thread-safe underlying socket.
 * </p>
 *
 * @author Christian Bauer
 */
public class StreamServerImpl implements StreamServer<StreamServerConfigurationImpl> {

    final private static Logger log = Logger.getLogger(StreamServer.class.getName());

    final protected StreamServerConfigurationImpl configuration;

    protected Router router;
    protected ServerSocket serverSocket;
    protected HttpParams globalParams = new BasicHttpParams();
    private volatile boolean stopped = false;

    public StreamServerImpl(StreamServerConfigurationImpl configuration) {
        this.configuration = configuration;
    }

    public StreamServerConfigurationImpl getConfiguration() {
        return configuration;
    }

    synchronized public void init(InetAddress bindAddress, Router router) throws InitializationException {

        try {

            this.router = router;

            this.serverSocket =
                    new ServerSocket(
                            configuration.getListenPort(),
                            configuration.getTcpConnectionBacklog(),
                            bindAddress
                    );

            log.info("Created socket (for receiving TCP streams) on: " + serverSocket.getLocalSocketAddress());

            this.globalParams
                    .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, configuration.getDataWaitTimeoutSeconds() * 1000)
                    .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, configuration.getBufferSizeKilobytes() * 1024)
                    .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, configuration.isStaleConnectionCheck())
                    .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, configuration.isTcpNoDelay());

        } catch (Exception ex) {
            throw new InitializationException("Could not initialize "+getClass().getSimpleName()+": " + ex.toString(), ex);
        }

    }

    synchronized public int getPort() {
        return this.serverSocket.getLocalPort();
    }

    synchronized public void stop() {
        stopped = true;
        try {
            serverSocket.close();
        } catch (IOException ex) {
            log.fine("Exception closing streaming server socket: " + ex);
        }
    }

    public void run() {

        log.fine("Entering blocking receiving loop, listening for HTTP stream requests on: " + serverSocket.getLocalSocketAddress());
        while (!stopped) {

            try {

                // Block until we have a connection
                Socket clientSocket = serverSocket.accept();

                // We have to force this fantastic library to accept HTTP methods which are not in the holy RFCs.
                DefaultHttpServerConnection httpServerConnection = new DefaultHttpServerConnection() {
                    @Override
                    protected HttpRequestFactory createHttpRequestFactory() {
                        return new UpnpHttpRequestFactory();
                    }
                };

                log.fine("Incoming connection from: " + clientSocket.getInetAddress());
                httpServerConnection.bind(clientSocket, globalParams);

                // Wrap the processing of the request in a UpnpStream
                UpnpStream connectionStream =
                        new HttpServerConnectionUpnpStream(
                                router.getProtocolFactory(),
                                httpServerConnection,
                                globalParams
                        );

                router.received(connectionStream);

            } catch (InterruptedIOException ex) {
                log.fine("I/O has been interrupted, stopping receiving loop, bytes transfered: " + ex.bytesTransferred);
                break;
            } catch (SocketException ex) {
                if (!stopped) {
                    // That's not good, could be anything
                    log.fine("Exception using server socket: " + ex.getMessage());
                } else {
                    // Well, it's just been stopped so that's totally fine and expected
                }
                break;
            } catch (IOException ex) {
                log.fine("Exception initializing receiving loop: " + ex.getMessage());
                break;
            }
        }

        try {
            log.fine("Receiving loop stopped");
            if (!serverSocket.isClosed()) {
                log.fine("Closing streaming server socket");
                serverSocket.close();
            }
        } catch (Exception ex) {
            log.info("Exception closing streaming server socket: " + ex.getMessage());
        }

    }

}

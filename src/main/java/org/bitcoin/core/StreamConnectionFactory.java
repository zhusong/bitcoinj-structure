package org.bitcoin.core;

import java.net.InetAddress;

public interface StreamConnectionFactory {
    /**
     * Returns a new handler or null to have the connection close.
     * @param inetAddress The client's (IP) address
     * @param port The remote port on the client side
     */
    public StreamConnection getNewConnection(InetAddress inetAddress, int port);
}

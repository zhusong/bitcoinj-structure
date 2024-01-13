package org.bitcoin.core;

import java.net.InetAddress;

public interface StreamConnectionFactory {
    public StreamConnection getNewConnection(InetAddress inetAddress, int port);
}

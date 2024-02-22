package org.bitcoin.core;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * A PeerDiscovery object is responsible for finding addresses of other nodes in the Bitcoin P2P network. Note that
 * the addresses returned may or may not be accepting connections.
 */
public interface PeerDiscovery {
    // TODO: Flesh out this interface a lot more.

    /**
     * Queries the addresses. This method may block.
     * @param services Required services as a bitmask, e.g. {@link VersionMessage#NODE_NETWORK}.
     */
    List<InetSocketAddress> getPeers(long services, long timeoutValue, TimeUnit timeoutUnit) throws PeerDiscoveryException;

    /** Stops any discovery in progress when we want to shut down quickly. */
    void shutdown();
}

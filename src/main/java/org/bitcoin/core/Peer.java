package org.bitcoin.core;

import java.net.InetSocketAddress;

public class Peer extends PeerSocketHandler {

    public Peer(NetworkParameters params, InetSocketAddress remoteIp) {
        super(params, remoteIp);
    }

    @Override
    protected void processMessage(Message m) throws Exception {

    }

    @Override
    public void connectionClosed() {

    }

    @Override
    public void connectionOpened() {

    }
}

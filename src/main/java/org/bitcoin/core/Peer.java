package org.bitcoin.core;

import com.sun.javafx.binding.StringFormatter;

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
        // Announce ourselves. This has to come first to connect to clients beyond v0.3.20.2 which wait to hear
        // from us until they send their version message back.
        PeerAddress address = getAddress();
        System.out.println("链接成功" + address.toSocketAddress());
        //TODO
    }
}

package org.bitcoin.core;

import com.sun.javafx.binding.StringFormatter;

import java.net.InetSocketAddress;

public class Peer extends PeerSocketHandler {

    public Peer(NetworkParameters params, InetSocketAddress remoteIp) {
        super(params, remoteIp);
    }

    @Override
    protected void processMessage(Message m) throws Exception {
        // Allow event listeners to filter the message stream. Listeners are allowed to drop messages by

        if (m instanceof Ping) {
            processPing((Ping) m);
        } else if (m instanceof Pong) {
            processPong((Pong) m);
        } else {
            System.out.println("Received unhandled message:" + this + "," + m);
        }
    }

    private void processPong(Pong m) {
        // Iterates over a snapshot of the list, so we can run unlocked here.
//        for (PendingPing ping : pendingPings) {
//            if (m.getNonce() == ping.nonce) {
//                pendingPings.remove(ping);
//                // This line may trigger an event listener that re-runs ping().
//                ping.complete();
//                return;
//            }
//        }

        System.out.println("process pong");
    }

    private void processPing(Ping m) {
        if (m.hasNonce())
            sendMessage(new Pong(m.getNonce()));
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

package org.bitcoin.core;

import com.google.common.util.concurrent.SettableFuture;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

public abstract class InboundMessageQueuer extends PeerSocketHandler {
    public final BlockingQueue<Message> inboundMessages = new ArrayBlockingQueue<>(1000);
    public final Map<Long, SettableFuture<Void>> mapPingFutures = new HashMap<>();

    public Peer peer;
    public BloomFilter lastReceivedFilter;

    protected InboundMessageQueuer(NetworkParameters params) {
        super(params, new InetSocketAddress(InetAddress.getLoopbackAddress(), 2000));
    }

    public Message nextMessage() {
        return inboundMessages.poll();
    }

    public Message nextMessageBlocking() throws InterruptedException {
        return inboundMessages.take();
    }

    @Override
    protected void processMessage(Message m) throws Exception {
        if (m instanceof Ping) {
//            SettableFuture<Void> future = mapPingFutures.get(((Ping) m).getNonce());
//            if (future != null) {
//                future.set(null);
//                return;
//            }
            super.sendMessage(new Pong(((Ping) m).getNonce() + ThreadLocalRandom.current().nextInt(300)));
        }
        if (m instanceof BloomFilter) {
            lastReceivedFilter = (BloomFilter) m;
        }
        inboundMessages.offer(m);
    }
}
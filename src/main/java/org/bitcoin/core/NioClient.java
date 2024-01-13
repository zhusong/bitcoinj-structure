package org.bitcoin.core;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public class NioClient implements MessageWriteTarget {

    private static final Logger log = LoggerFactory.getLogger(NioClient.class);

    private final Handler handler;
    private final NioClientManager manager = new NioClientManager();

    public NioClient(final SocketAddress serverAddress, final StreamConnection parser,
                     final int connectTimeoutMillis) throws IOException {
        manager.startAsync();
        manager.awaitRunning();
        handler = new Handler(parser, connectTimeoutMillis);
        Futures.addCallback(manager.openConnection(serverAddress, handler), new FutureCallback<SocketAddress>() {
            @Override
            public void onSuccess(SocketAddress result) {
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("Connect to {} failed: {}", serverAddress, Throwables.getRootCause(t));
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public void closeConnection() {
        handler.writeTarget.closeConnection();
    }

    @Override
    public synchronized ListenableFuture writeBytes(byte[] message) throws IOException {
        return handler.writeTarget.writeBytes(message);
    }

    class Handler extends AbstractTimeoutHandler implements StreamConnection {
        private final StreamConnection upstreamConnection;
        private MessageWriteTarget writeTarget;
        private boolean closeOnOpen = false;
        private boolean closeCalled = false;
        Handler(StreamConnection upstreamConnection, int connectTimeoutMillis) {
            this.upstreamConnection = upstreamConnection;
            setSocketTimeout(connectTimeoutMillis);
            setTimeoutEnabled(true);
        }

        @Override
        protected synchronized void timeoutOccurred() {
            closeOnOpen = true;
            connectionClosed();
        }

        @Override
        public synchronized void connectionClosed() {
            manager.stopAsync();
            if (!closeCalled) {
                closeCalled = true;
                upstreamConnection.connectionClosed();
            }
        }

        @Override
        public synchronized void connectionOpened() {
            if (!closeOnOpen)
                upstreamConnection.connectionOpened();
        }

        @Override
        public int receiveBytes(ByteBuffer buff) throws Exception {
            return upstreamConnection.receiveBytes(buff);
        }

        @Override
        public synchronized void setWriteTarget(MessageWriteTarget writeTarget) {
            if (closeOnOpen)
                writeTarget.closeConnection();
            else {
                setTimeoutEnabled(false);
                this.writeTarget = writeTarget;
                upstreamConnection.setWriteTarget(writeTarget);
            }
        }

        @Override
        public int getMaxMessageSize() {
            return upstreamConnection.getMaxMessageSize();
        }
    }

}

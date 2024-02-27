package org.bitcoin.core;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class NioClientManager extends AbstractExecutionThreadService implements ClientConnectionManager{
    private final Logger log = LoggerFactory.getLogger(getClass());

    final Queue<PendingConnect> newConnectionChannels = new LinkedBlockingQueue<>();
    private final Selector selector;
    private final Set<ConnectionHandler> connectedHandlers = Collections.synchronizedSet(new HashSet<ConnectionHandler>());

    public NioClientManager() {
        try {
            selector = SelectorProvider.provider().openSelector();
        } catch (IOException e) {
            throw new RuntimeException(e); // Shouldn't ever happen
        }
    }

    @Override
    protected void run() throws Exception {
        try {
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
            while (isRunning()) {
                PendingConnect conn;

                // 从newConnectionChannels队列中获取连接通道，然后将其注册到选择器（selector）中以进行连接操作。
                // 如果在注册之前通道已关闭，则会捕获ClosedChannelException异常并打印警告信息。
                while ((conn = newConnectionChannels.poll()) != null) {
                    try {
                        SelectionKey key = conn.sc.register(selector, SelectionKey.OP_CONNECT);
                        key.attach(conn);
                    } catch (ClosedChannelException e) {
                        System.out.println("SocketChannel was closed before it could be registered");
                    }
                }

                //通过使用 Selector，可以在一个线程中同时处理多个通道的 I/O 事件，而无需为每个通道分配一个独立的线程。
                //selector.select() 方法会阻塞当前线程，直到至少有一个注册的通道有就绪事件发生，或者超时时间到达。
                selector.select();

                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();

                //遍历SelectionKey，已经就绪的key
                while (keyIterator.hasNext()) {
                    //按顺序获取其中一个
                    SelectionKey key = keyIterator.next();

                    //遍历了就从迭代里删除
                    keyIterator.remove();

                    //处理SelectionKey
                    handleKey(key);
                }
            }
        } catch (Exception e) {
            System.out.println("Error trying to open/read from connection: " + e);
        } finally {
            // Go through and close everything, without letting IOExceptions get in our way
            for (SelectionKey key : selector.keys()) {
                try {
                    System.out.println("close selection key channel");
                    key.channel().close();
                } catch (IOException e) {
                    System.out.println("Error closing channel" + e);
                }
                key.cancel();
                if (key.attachment() instanceof ConnectionHandler)
                    ConnectionHandler.handleKey(key); // Close connection if relevant
            }
            try {
                selector.close();
            } catch (IOException e) {
                System.out.println("Error closing client manager selector" + e);
            }
        }
    }

    // Handle a SelectionKey which was selected
    //在Java NIO中，Channel的就绪状态表示该通道已经准备好进行某种I/O操作。就绪状态取决于通道的类型和具体的操作。
    //以下是一些常见的Channel就绪状态及其对应的情况：
    //OP_READ（可读就绪）：当通道中有数据可供读取时，通道会被标记为可读就绪状态。例如，在SocketChannel中，当远程主机发送数据到通道时，该通道就会变为可读就绪状态。
    //OP_WRITE（可写就绪）：当可以向通道写入数据时，通道会被标记为可写就绪状态。例如，在SocketChannel中，当缓冲区有足够的空间可以写入数据时，该通道就会变为可写就绪状态。
    //OP_CONNECT（连接就绪）：当SocketChannel正在进行非阻塞连接时，通道会被标记为连接就绪状态。当连接已经建立或正在建立中时，通道就会变为连接就绪状态。
    //OP_ACCEPT（接收就绪）：在ServerSocketChannel上调用accept()方法时，如果有客户端连接请求到达，该通道会被标记为接收就绪状态。
    //可以使用SelectionKey对象的isReadable()、isWritable()、isConnectable()和isAcceptable()方法来检查通道的就绪状态。
    //请注意，就绪状态是针对非阻塞I/O操作的，它允许您在没有阻塞的情况下选择处理已经准备好的操作。
    private void handleKey(SelectionKey key) throws IOException {
        // We could have a !isValid() key here if the connection is already closed at this point

        // 在Java NIO中，当SocketChannel正在进行非阻塞连接时，通道会被标记为连接就绪状态是因为非阻塞连接的过程是异步的。
        // 当我们调用SocketChannel的connect()方法进行连接时，它会立即返回，而不会等待连接完成。在后台，操作系统会继续尝试建立连接，
        // 同时SocketChannel会返回一个SelectionKey，标记为"连接就绪"，以便我们可以通过选择器（Selector）来检查连接是否已经建立成功。
        //
        // 在选择器的select()方法中，我们可以使用SelectionKey的isConnectable()方法来检查连接是否已经建立成功。
        // 如果返回true，表示连接已经就绪，我们可以调用finishConnect()方法来完成连接过程。
        // 这种方式允许我们在连接过程中执行其他操作，而不会阻塞线程，提高了程序的并发性能。

        if (key.isValid() && key.isConnectable()) { // ie a client connection which has finished the initial connect process
            // Create a ConnectionHandler and hook everything together
            PendingConnect data = (PendingConnect) key.attachment();
            StreamConnection connection = data.connection;
            SocketChannel sc = (SocketChannel) key.channel();
            ConnectionHandler handler = new ConnectionHandler(connection, key, connectedHandlers);
            try {
                //链接确实建立
                if (sc.finishConnect()) {
                    //从SocketChannel中获取远程socket地址
                    System.out.println("Connected to {}" + sc.socket().getRemoteSocketAddress());
                    //感兴趣的事件中，再加上读时间，并且去掉链接事件，并且attach ConnectionHandler
                    key.interestOps((key.interestOps() | SelectionKey.OP_READ) & ~SelectionKey.OP_CONNECT).attach(handler);
                    connection.connectionOpened();
                    data.future.set(data.address);
                } else {
                    //链接仍然没有建立
                    System.out.println("Failed to connect to {}" + sc.socket().getRemoteSocketAddress());
                    handler.closeConnection(); // Failed to connect for some reason
                    data.future.setException(new ConnectException("Unknown reason"));
                    data.future = null;
                }
            } catch (Exception e) {
                // If e is a CancelledKeyException, there is a race to get to interestOps after finishConnect() which
                // may cause this. Otherwise it may be any arbitrary kind of connection failure.
                // Calling sc.socket().getRemoteSocketAddress() here throws an exception, so we can only log the error itself
                Throwable cause = Throwables.getRootCause(e);
                System.out.println("Failed to connect with exception: {}: {}" + cause.getClass().getName()+ cause.getMessage()+ e);
                handler.closeConnection();
                data.future.setException(cause);
                data.future = null;
            }
        } else // Process bytes read
            ConnectionHandler.handleKey(key);
    }

    class PendingConnect {
        SocketChannel sc;
        StreamConnection connection;
        SocketAddress address;
        SettableFuture<SocketAddress> future = SettableFuture.create();

        PendingConnect(SocketChannel sc, StreamConnection connection, SocketAddress address) { this.sc = sc; this.connection = connection; this.address = address; }
    }

    public ListenableFuture<SocketAddress> openConnection(SocketAddress serverAddress, StreamConnection connection) {
        if (!isRunning())
            //没有在运行，直接异常
            throw new IllegalStateException();
        // Create a new connection, give it a connection as an attachment
        try {
            // 连接服务器，SocketChannel.open是nio建立连接的初始操作，SocketChannel.open()方法可以调用多次创建多个不同的SocketChannel实例。
            // 每次调用open()方法都会返回一个新的SocketChannel对象，它代表一个新的套接字通道。
            // SocketChannel channel1 = SocketChannel.open();
            // SocketChannel channel2 = SocketChannel.open();
            // SocketChannel channel3 = SocketChannel.open();
            // 在这个示例中，我们分别创建了3个不同的SocketChannel实例：channel1、channel2和channel3。
            // 每个实例都代表一个独立的套接字通道，可以用于进行网络通信。
            //
            // 需要注意的是，每个SocketChannel实例都需要进行适当的配置和操作，例如设置非阻塞模式、注册到Selector等。
            // 同时，创建多个SocketChannel实例时，需要根据具体的需求和逻辑进行管理和使用。
            SocketChannel sc = SocketChannel.open();
            //非阻塞
            sc.configureBlocking(false);
            //连接
            sc.connect(serverAddress);
            //处理连接
            PendingConnect data = new PendingConnect(sc, connection, serverAddress);
            //加入到队列中
            newConnectionChannels.offer(data);
            //唤起selector
            selector.wakeup();
            return data.future;
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    @Override
    public void triggerShutdown() {
        selector.wakeup();
    }

    @Override
    public int getConnectedClientCount() {
        return connectedHandlers.size();
    }

    @Override
    public void closeConnections(int n) {
        while (n-- > 0) {
            ConnectionHandler handler;
            //为什么这里需要同步
            synchronized (connectedHandlers) {
                handler = connectedHandlers.iterator().next();
            }
            if (handler != null)
                //关闭连接
                handler.closeConnection(); // Removes handler from connectedHandlers before returning
        }
    }
}

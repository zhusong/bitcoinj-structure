package org.bitcoin.core;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.sun.javafx.binding.StringFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

public class NioServer extends AbstractExecutionThreadService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ServerSocketChannel sc;
    final Selector selector;
    private final StreamConnectionFactory connectionFactory;

    public NioServer(final StreamConnectionFactory connectionFactory, InetSocketAddress bindAddress) throws IOException {
        //链接工厂
        this.connectionFactory = connectionFactory;

        // 会创建一个未绑定的 ServerSocketChannel 对象，
        // 你需要通过调用其 bind() 方法将其绑定到特定的 IP 地址和端口上。一旦绑定成功，ServerSocketChannel 就可以开始监听来自客户端的连接请求。
        sc = ServerSocketChannel.open();
        // 设置为非阻塞模式
        sc.configureBlocking(false);

        //绑定地址和端口，一般是127.0.0.1 8379
        sc.socket().bind(bindAddress);

        // Selector.open() 方法来创建 Selector 对象，这是因为 Selector 类提供了一个静态工厂方法 open() 来创建 Selector 实例。
        // 这个方法会根据操作系统的不同，使用适当的 SelectorProvider 来创建 Selector。
        //使用 Selector.open() 来创建 Selector 是可行的，它会根据当前的操作系统选择合适的底层实现。但是，如果你需要更精确地控制
        // Selector 的创建过程，或者需要使用特定的 SelectorProvider，那么你可以使用 SelectorProvider.provider().openSelector() 方法来创建 Selector。
        selector = SelectorProvider.provider().openSelector();

        //也就是说，ServerSocketChannel对accept感兴趣，而socketChannel对read和write感兴趣
        sc.register(selector, SelectionKey.OP_ACCEPT);
    }

    // Handle a SelectionKey which was selected
    private void handleKey(Selector selector, SelectionKey key) throws IOException {
        if (key.isValid() && key.isAcceptable()) {
            // Accept a new connection, give it a stream connection as an attachment
            SocketChannel newChannel = sc.accept();
            newChannel.configureBlocking(false);
            SelectionKey newKey = newChannel.register(selector, SelectionKey.OP_READ);
            try {
                //每建立一个链接，链接处理就通过工厂创建
                ConnectionHandler handler = new ConnectionHandler(connectionFactory, newKey);
                newKey.attach(handler);
                handler.connection.connectionOpened();
            } catch (IOException e) {
                // This can happen if ConnectionHandler's call to get a new handler returned null
                System.out.println(StringFormatter.format("Error handling new connection", Throwables.getRootCause(e).getMessage()));
                newKey.channel().close();
            }
        } else { // Got a closing channel or a channel to a client connection
            ConnectionHandler.handleKey(key);
        }
    }


    protected void run() throws Exception {
        try {
            while (isRunning()) {

                selector.select();

                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();

                    handleKey(selector, key);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(StringFormatter.format("Error trying to open/read from connection: {}", e));
        } finally {
            // Go through and close everything, without letting IOExceptions get in our way
            for (SelectionKey key : selector.keys()) {
                try {
                    //close所有的channel
                    key.channel().close();
                } catch (IOException e) {
                    System.out.println(StringFormatter.format("Error closing channel", e));
                }
                try {
                    //当我们关闭一个 Channel 时，Selector 会在下一次的选择操作中注意到该 Channel 已经关闭，
                    // 并将其对应的 SelectionKey 标记为无效。
                    // 但是，如果我们不手动调用 key.cancel() 取消该 SelectionKey，
                    // 它仍然会留在 Selector 的键集合中，可能会导致不必要的迭代和处理。
                    key.cancel();
                    handleKey(selector, key);
                } catch (IOException e) {
                    System.out.println(StringFormatter.format("Error closing selection key", e));
                }
            }
            try {
                selector.close(); // 关闭 Selector
            } catch (IOException e) {
                System.out.println(StringFormatter.format("Error closing server selector", e));
            }
            try {
                sc.close();// 关闭 ServerSocketChannel
            } catch (IOException e) {
                System.out.println(StringFormatter.format("Error closing server channel", e));
            }
        }
    }

}





class NIODemo {
    public static void main(String[] args) throws IOException {
        startServer();
        startClient();
    }

    public static void startServer() throws IOException {
        // 创建ServerSocketChannel
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false); // 设置为非阻塞模式
        serverSocketChannel.socket().bind(new InetSocketAddress(8080));

        // 创建Selector并注册ServerSocketChannel
        Selector selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            // 等待事件发生
            selector.select();

            // 处理事件
            Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                keyIterator.remove();

                if (key.isAcceptable()) {
                    // 接受连接
                    ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                    SocketChannel socketChannel = serverChannel.accept();
                    socketChannel.configureBlocking(false); // 设置为非阻塞模式
                    socketChannel.register(selector, SelectionKey.OP_READ);
                } else if (key.isReadable()) {
                    // 读取数据
                    SocketChannel socketChannel = (SocketChannel) key.channel();
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    int bytesRead = socketChannel.read(buffer);
                    while (bytesRead != -1) {
                        buffer.flip(); // 切换为读模式

                        // 读取数据
                        while (buffer.hasRemaining()) {
                            System.out.print((char) buffer.get());
                        }

                        buffer.clear(); // 切换为写模式
                        bytesRead = socketChannel.read(buffer);
                    }

                    socketChannel.close(); // 关闭连接
                }
            }
        }
    }

    public static void startClient() throws IOException {
        // 创建SocketChannel
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false); // 设置为非阻塞模式
        socketChannel.connect(new InetSocketAddress("localhost", 8080));

        while (!socketChannel.finishConnect()) {
            // 等待连接完成
        }

        // 发送数据
        String message = "Hello, Server!";
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
        socketChannel.write(buffer);

        socketChannel.close(); // 关闭连接
    }
}


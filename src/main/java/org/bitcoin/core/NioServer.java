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




    protected void run() throws Exception {
        try {
            while (isRunning()) {

                selector.select();

                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();

                    //处理
                    this.handleKey(selector, key);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Go through and close everything, without letting IOExceptions get in our way
            for (SelectionKey key : selector.keys()) {
                try {
                    //close所有的channel
                    key.channel().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    //当我们关闭一个 Channel 时，Selector 会在下一次的选择操作中注意到该 Channel 已经关闭，
                    // 并将其对应的 SelectionKey 标记为无效。
                    // 但是，如果我们不手动调用 key.cancel() 取消该 SelectionKey，
                    // 它仍然会留在 Selector 的键集合中，可能会导致不必要的迭代和处理。
                    key.cancel();
                    this.handleKey(selector, key);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                selector.close(); // 关闭 Selector
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                sc.close();// 关闭 ServerSocketChannel
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
                //回调连接建立
                handler.connection.connectionOpened();
            } catch (IOException e) {
                // This can happen if ConnectionHandler's call to get a new handler returned null
                e.printStackTrace();
                newKey.channel().close();
            }
        } else { // Got a closing channel or a channel to a client connection
            //System.out.println("server handle key else called ");
            ConnectionHandler.handleKey(key);
        }
    }

}



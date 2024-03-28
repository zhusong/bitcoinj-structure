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
import java.nio.Buffer;
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

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

    //处理SelectionKey
    private void handleKey(Selector selector, SelectionKey key) throws IOException {
        //如果SelectionKey是合法的，并且能接受一个新的链接
        if (key.isValid() && key.isAcceptable()) {
            this.handleNewConnection(selector, key);
        } else {
            //处理正在关闭的channel或者正常链接的channel
            this.handleConnection(key);
        }
    }

    // 处理被选中的 SelectionKey
    // 以非锁定状态运行，因为调用方是单线程的（或者如果不是，应该强制要求对于给定的 ConnectionHandler，handleKey 只能原子地调用）
    public void handleConnection(SelectionKey key) {
        //只有成功才attach了
        ConnectionHandler handler = ((ConnectionHandler)key.attachment());
        try {
            if (handler == null) {
                return;
            }
            if (!key.isValid()) {
                handler.closeConnection(); // Key has been cancelled, make sure the socket gets closed
                return;
            }
            // key.isReadable()方法用于测试该SelectionKey关联的通道是否准备好进行读取操作。
            // 当通道的输入缓冲区中有数据可供读取时，通道处于可读就绪状态。这意味着缓冲区中存在数据，可以立即读取。
            //
            // 然而，通道的就绪状态不仅仅取决于缓冲区中是否有数据，还取决于其他因素，如操作系统内核的调度和网络传输的状态。
            // 即使缓冲区中已经有数据，但如果网络传输尚未完成或操作系统内核尚未通知通道就绪，通道可能仍然不处于就绪状态。
            //
            // 因此，仅仅有数据存在于缓冲区并不意味着通道一定处于就绪状态。就绪状态是由操作系统内核来决定的，它需要综合考虑缓冲区的状态、网络传输状态以及其他因素。
            //即使操作系统将数据放入缓冲区，缓冲区中有数据可供读取，但操作系统不会立即将通道设置为已经就绪的状态。操作系统内核可能会根据一些策略和条件来决定何时将通道设置为就绪状态。
            //这些策略和条件可能包括：
            //
            //缓冲区的填充程度：即使缓冲区中有数据，但如果缓冲区的填充程度不满足一定的条件（如达到一定的阈值），操作系统可能不会将通道设置为就绪状态。
            //
            //传输状态：如果数据的传输尚未完成，操作系统可能会等待数据完全传输后才将通道设置为就绪状态。
            //
            //内核调度：操作系统内核可能会根据调度算法和其他优先级决策，决定何时将通道设置为就绪状态。
            //
            //因此，即使缓冲区中有数据可供读取，操作系统内核可能仍然需要一些时间和条件来决定将通道设置为已经就绪的状态。应用程序需要通过选择器和相应的方法来检查通道的就绪状态，并在通道处于就绪状态时进行读取操作。

            if (key.isReadable()) { //缓冲区有数据了
                // Do a socket read and invoke the connection's receiveBytes message
                //从channel中读取数据，并写入到readBuff
                int read = handler.getChannel().read(handler.getReadBuff());
                if (read == 0)
                    //如果返回值为0，则可能是在等待写入操作，直接返回。
                    return; // Was probably waiting on a write
                else if (read == -1) { // Socket was closed
                    //该代码段中的key.cancel()方法用于取消与该SelectionKey关联的通道在选择器中的注册。调用此方法后，
                    // 该SelectionKey将变为无效，并将被添加到选择器的已取消键集中。在下一次选择操作期间，
                    // 该SelectionKey将从选择器的所有键集中移除。
                    //
                    //如果该SelectionKey已经被取消，则调用此方法没有任何效果。一旦取消，SelectionKey将永久无效。
                    //
                    //该方法可以在任何时间调用。它在选择器的已取消键集上进行同步，
                    // 并且如果与涉及相同选择器的取消或选择操作同时调用，则可能会短暂地阻塞。
                    key.cancel();
                    handler.closeConnection();
                    return;
                }
                // "flip" the buffer - setting the limit to the current position and setting position to 0
                ((Buffer) handler.getReadBuff()).flip();
                // Use connection.receiveBytes's return value as a check that it stopped reading at the right location
                int bytesConsumed = checkNotNull(handler.connection).receiveBytes(handler.getReadBuff());
                checkState(handler.getReadBuff().position() == bytesConsumed);
                // Now drop the bytes which were read by compacting readBuff (resetting limit and keeping relative
                // position)
                handler.getReadBuff().compact();
            }
            if (key.isWritable()) {
                handler.tryWriteBytes();
            }
        } catch (Exception e) {
            // This can happen eg if the channel closes while the thread is about to get killed
            // (ClosedByInterruptException), or if handler.connection.receiveBytes throws something
            Throwable t = Throwables.getRootCause(e);
            System.out.println("Error handling SelectionKey: {} {}" + t.getClass().getName() + t.getMessage() != null ? t.getMessage() : "");
            handler.closeConnection();
        }
    }


    public void handleNewConnection(Selector selector, SelectionKey key) throws IOException {
        // 接受一个新的链接，返回的是Server的SocketChannel
        SocketChannel newChannel = sc.accept();
        //SocketChannel设置为非阻塞
        newChannel.configureBlocking(false);
        //因为是server，所以对读感兴趣
        SelectionKey newKey = newChannel.register(selector, SelectionKey.OP_READ);
        //每建立一个链接，链接处理服务就通过工厂创建
        ConnectionHandler handler = new ConnectionHandler(this.newConnection(key), newKey);
        //将该链接处理类attach到对读感兴趣的SelectionKey上
        newKey.attach(handler);
        //通知
        handler.connection.connectionOpened();
    }

    public StreamConnection newConnection(SelectionKey key){
        return connectionFactory.getNewConnection(this.inetAddress(key), this.port(key));
    }

    public InetAddress inetAddress(SelectionKey key){
        return ((SocketChannel) key.channel()).socket().getInetAddress();
    }

    public int port(SelectionKey key){
        return ((SocketChannel) key.channel()).socket().getPort();
    }

    @Override
    protected void run() throws Exception {
        try {
            while (super.isRunning()) {
                //阻塞等待
                selector.select();
                //获取已经被唤醒的SelectionKey
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();
                    //处理新的链接
                    this.handleKey(selector, key);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(StringFormatter.format("Error trying to open/read from connection: {}", e));
        } finally {
            // 关闭所有东西，屏蔽所有IOException
            for (SelectionKey key : selector.keys()) {
                try {
                    //关闭所有的channel
                    key.channel().close();
                } catch (IOException e) {
                    System.out.println(StringFormatter.format("Error closing channel", e));
                }
                try {
                    //在 Java NIO 中，当你关闭一个 Channel 后，仍需要调用一次 SelectionKey 的 cancel 方法，
                    //这是因为 SelectionKey 仍然保留在 Selector 中。
                    //SelectionKey 的 cancel 方法用于将其从关联的 Selector 中移除，以避免在下一次 Selector 的选择操作中再次处理已关闭的 Channel。
                    key.cancel();
                    handleKey(selector, key);
                } catch (IOException e) {
                    System.out.println(StringFormatter.format("Error closing selection key", e));
                }
            }
            try {
                // 关闭 Selector
                selector.close();
            } catch (IOException e) {
                System.out.println(StringFormatter.format("Error closing server selector", e));
            }
            try {
                // 关闭 ServerSocketChannel
                sc.close();
            } catch (IOException e) {
                System.out.println(StringFormatter.format("Error closing server channel", e));
            }
        }
    }

}



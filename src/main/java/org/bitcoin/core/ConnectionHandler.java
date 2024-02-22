package org.bitcoin.core;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class ConnectionHandler implements MessageWriteTarget {

    //connection handler中包含connection，connection是由工厂创建的，工厂是在初始化NIOServer的时候给的。connection中包含开、关、
    public StreamConnection connection;
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ConnectionHandler.class);
    // We lock when touching local flags and when writing data, but NEVER when calling any methods which leave this
    // class into non-Java classes.
    private final ReentrantLock lock = Threading.lock(ConnectionHandler.class);

    private static final int BUFFER_SIZE_LOWER_BOUND = 4096;
    private static final int BUFFER_SIZE_UPPER_BOUND = 65536;

    private static final int OUTBOUND_BUFFER_BYTE_COUNT = Message.MAX_SIZE + 24; // 24 byte message header

    //读取的buf数据
    private final ByteBuffer readBuff;
    //channel
    private final SocketChannel channel;
    //对应的SelectionKey
    private final SelectionKey key;

    private boolean closeCalled = false;

    private long bytesToWriteRemaining = 0;
    private final LinkedList<BytesAndFuture> bytesToWrite = new LinkedList<>();

    private Set<ConnectionHandler> connectedHandlers;

    private static class BytesAndFuture {
        public final ByteBuffer bytes;
        public final SettableFuture future;

        public BytesAndFuture(ByteBuffer bytes, SettableFuture future) {
            this.bytes = bytes;
            this.future = future;
        }
    }

    //NioServer、NioClientManager用，工厂创建的streamConnection，里面对于建立链接、关闭链接、读数据的回调
    public ConnectionHandler(StreamConnection connection, SelectionKey key) {
        this.key = key;
        this.channel = checkNotNull(((SocketChannel)key.channel()));
        if (connection == null) {
            readBuff = null;
            return;
        }
        this.connection = connection;
        //分配内存空间，直接内存
        readBuff = ByteBuffer.allocateDirect(Math.min(Math.max(connection.getMaxMessageSize(), BUFFER_SIZE_LOWER_BOUND), BUFFER_SIZE_UPPER_BOUND));
        connection.setWriteTarget(this); // May callback into us (eg closeConnection() now)
        connectedHandlers = null;
    }

    //NioClientManager用
    public ConnectionHandler(StreamConnection connection, SelectionKey key, Set<ConnectionHandler> connectedHandlers) {
        this(checkNotNull(connection), key);

        // closeConnection() may have already happened because we invoked the other c'tor above, which called
        // connection.setWriteTarget which might have re-entered already. In this case we shouldn't add ourselves
        // to the connectedHandlers set.
        lock.lock();
        try {
            this.connectedHandlers = connectedHandlers;
            if (!closeCalled)
                checkState(this.connectedHandlers.add(this));
        } finally {
            lock.unlock();
        }
    }

    //NioServer用
    public ConnectionHandler(StreamConnectionFactory connectionFactory, SelectionKey key) throws IOException {
        this(connectionFactory.getNewConnection(((SocketChannel) key.channel()).socket().getInetAddress(), ((SocketChannel) key.channel()).socket().getPort()), key);
        if (connection == null) {
            throw new IOException("Parser factory.getNewConnection returned null");
        }
    }

    //对写感兴趣
    private void setWriteOps() {
        // Make sure we are registered to get updated when writing is available again
        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        // Refresh the selector to make sure it gets the new interestOps
        key.selector().wakeup();
    }

    @Override
    public ListenableFuture writeBytes(byte[] message) throws IOException {
        boolean andUnlock = true;
        lock.lock();
        try {
            if (bytesToWriteRemaining + message.length > OUTBOUND_BUFFER_BYTE_COUNT)
                throw new IOException("Outbound buffer overflowed");
            // Just dump the message onto the write buffer and call tryWriteBytes
            // TODO: Kill the needless message duplication when the write completes right away
            final SettableFuture<Object> future = SettableFuture.create();
            //写的话
            bytesToWrite.offer(new BytesAndFuture(ByteBuffer.wrap(Arrays.copyOf(message, message.length)), future));
            bytesToWriteRemaining += message.length;
            setWriteOps();
            return future;
        } catch (IOException e) {
            lock.unlock();
            andUnlock = false;
            log.warn("Error writing message to connection, closing connection", e);
            closeConnection();
            throw e;
        } catch (CancelledKeyException e) {
            lock.unlock();
            andUnlock = false;
            log.warn("Error writing message to connection, closing connection", e);
            closeConnection();
            throw new IOException(e);
        } finally {
            if (andUnlock)
                lock.unlock();
        }
    }

    @Override
    public void closeConnection() {
        checkState(!lock.isHeldByCurrentThread());
        try {
            //首先channel close
            channel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //然后peer回调
        connectionClosed();
    }

    private void connectionClosed() {
        boolean callClosed = false;
        lock.lock();
        try {
            callClosed = !closeCalled;
            closeCalled = true;
        } finally {
            lock.unlock();
        }
        if (callClosed) {
            checkState(connectedHandlers == null || connectedHandlers.remove(this));
            connection.connectionClosed();
        }
    }

    // Handle a SelectionKey which was selected
    // Runs unlocked as the caller is single-threaded (or if not, should enforce that handleKey is only called
    // atomically for a given ConnectionHandler)
    public static void handleKey(SelectionKey key) {
        //只有成功才attach了
        ConnectionHandler handler = ((ConnectionHandler)key.attachment());
        try {
            if (handler == null)
                return;
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
                int read = handler.channel.read(handler.readBuff);
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
                ((Buffer) handler.readBuff).flip();
                // Use connection.receiveBytes's return value as a check that it stopped reading at the right location
                int bytesConsumed = checkNotNull(handler.connection).receiveBytes(handler.readBuff);
                checkState(handler.readBuff.position() == bytesConsumed);
                // Now drop the bytes which were read by compacting readBuff (resetting limit and keeping relative
                // position)
                handler.readBuff.compact();
            }
            if (key.isWritable()) {
                handler.tryWriteBytes();
            }
        } catch (Exception e) {
            // This can happen eg if the channel closes while the thread is about to get killed
            // (ClosedByInterruptException), or if handler.connection.receiveBytes throws something
            Throwable t = Throwables.getRootCause(e);
            log.warn("Error handling SelectionKey: {} {}", t.getClass().getName(), t.getMessage() != null ? t.getMessage() : "", e);
            handler.closeConnection();
        }
    }

    private void tryWriteBytes() throws IOException  {
        lock.lock();
        try {
            // Iterate through the outbound ByteBuff queue, pushing as much as possible into the OS' network buffer.
            Iterator<BytesAndFuture> iterator = bytesToWrite.iterator();
            while (iterator.hasNext()) {
                BytesAndFuture bytesAndFuture = iterator.next();
                bytesToWriteRemaining -= channel.write(bytesAndFuture.bytes);
                //如果没有剩余的
                if (!bytesAndFuture.bytes.hasRemaining()) {
                    iterator.remove();
                    bytesAndFuture.future.set(null);
                } else {
                    //有剩余的，设置对写感兴趣
                    setWriteOps();
                    break;
                }
            }
            // If we are done writing, clear the OP_WRITE interestOps
            if (bytesToWrite.isEmpty())
                //如果所有字节都已写入完成，则清除对写操作（OP_WRITE）的兴趣
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
            // Don't bother waking up the selector here, since we're just removing an op, not adding
        } finally {
            lock.unlock();
        }
    }
}

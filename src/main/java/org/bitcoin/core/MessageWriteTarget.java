package org.bitcoin.core;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;

public interface MessageWriteTarget {
    /**
     *
     * 该代码注释表明该函数将给定的字节写入远程服务器，并返回一个 future（未来对象），
     * 该 future（未来对象）在所有字节都被写入操作系统的网络缓冲区后完成。
     * 根据这个注释，该函数似乎执行了一个异步写操作，用于将数据发送到远程服务器。
     * 返回的 future（未来对象）可以用于跟踪写操作的进度和完成情况。
     * 由于没有实际的代码实现，很难提供更多细节或具体的代码示例。
     *
     * 因为写是主动的，read不是主动的，所以不需要方法，妙啊
     */
    ListenableFuture writeBytes(byte[] message) throws IOException;
    /**
     *
     * 该函数关闭与服务器的连接，并在处理网络的线程上触发connectionClosed()事件，所有回调都在该线程上执行。
     *
     * 根据这个注释，该函数负责关闭与服务器的网络连接。当连接关闭时，它会触发connectionClosed()事件，
     * 该事件可能通知相关组件或监听器连接已关闭。
     *
     * 因为关闭链接也是主动的，建立链接是被动的，所以不需要方法
     */
    void closeConnection();
}
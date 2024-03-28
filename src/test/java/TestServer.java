import com.sun.istack.internal.Nullable;
import org.bitcoin.core.NioServer;
import org.bitcoin.core.StreamConnection;
import org.bitcoin.core.StreamConnectionFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class TestServer {

    public static void main(String[] args) throws IOException {
        NioServer nioServer = new NioServer(new StreamConnectionFactory() {
            @Nullable
            @Override
            public StreamConnection getNewConnection(InetAddress inetAddress, int port) {
                return new StreamConnectionImpl(inetAddress, port);
            }
        }, new InetSocketAddress(InetAddress.getLoopbackAddress(), 20000));

        nioServer.startAsync();
        nioServer.awaitRunning();


    }
}

import com.sun.istack.internal.Nullable;
import org.bitcoin.core.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class TestServer {

    public static void main(String[] args) throws IOException {
        NioServer nioServer = new NioServer(new StreamConnectionFactory() {
            @Nullable
            @Override
            public StreamConnection getNewConnection(InetAddress inetAddress, int port) {
                return new InboundMessageQueuer(MainNetParams.get()) {
                    @Override
                    public void connectionClosed() {
                    }

                    @Override
                    public void connectionOpened() {

                    }
                };
            }
        }, new InetSocketAddress(InetAddress.getLoopbackAddress(), 2000));

        nioServer.startAsync();
        nioServer.awaitRunning();

    }
}

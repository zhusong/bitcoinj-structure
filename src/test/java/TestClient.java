import com.sun.istack.internal.Nullable;
import org.bitcoin.core.*;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestClient {


    @Test
    public void testPeer() throws IOException {

        Peer peer = new Peer(NetworkParameters.fromID(NetworkParameters.ID_MAINNET), new InetSocketAddress(InetAddress.getLoopbackAddress(), 2000));

        NioClient nioClient = new NioClient(new InetSocketAddress(InetAddress.getLoopbackAddress(), 20000),
                peer, 100000);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
       // nioClient.writeBytes(new byte[]{3, 4});

        peer.sendMessage(this.buildMessage());
    }

    public Message buildMessage(){
        return new Ping(100);
    }
}

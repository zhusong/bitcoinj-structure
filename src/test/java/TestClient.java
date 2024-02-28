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

        NioClient nioClient = new NioClient(new InetSocketAddress(InetAddress.getLoopbackAddress(), 2000), peer, 100000);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
       // nioClient.writeBytes(new byte[]{3, 4});

        while (true) {

            peer.sendMessage(this.buildMessage());
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //发完就直接close了，所以不太好整，是守护线程，非守护线程才行。
    }

    public Message buildMessage(){
        return new Ping(100);
    }
}

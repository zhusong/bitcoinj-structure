import com.sun.istack.internal.Nullable;
import org.bitcoin.core.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestClient {

    public static void main(String[] args) throws IOException {

        Peer peer = new Peer(new NetworkParameters() {
            @Override
            public String getPaymentProtocolId() {
                return null;
            }
        }, new InetSocketAddress(InetAddress.getLoopbackAddress(), 2000));

        NioClient nioClient = new NioClient(new InetSocketAddress(InetAddress.getLoopbackAddress(), 20000),
                peer, 3900);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        nioClient.writeBytes(new byte[]{3, 4});
    }
}

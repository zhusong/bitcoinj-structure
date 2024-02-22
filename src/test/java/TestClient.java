import com.sun.istack.internal.Nullable;
import org.bitcoin.core.NioClient;
import org.bitcoin.core.NioServer;
import org.bitcoin.core.StreamConnection;
import org.bitcoin.core.StreamConnectionFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestClient {

    public static void main(String[] args) throws IOException {
        NioClient nioClient = new NioClient(new InetSocketAddress(InetAddress.getLoopbackAddress(), 20000), new StreamConnectionImpl(), 3900);

        nioClient.writeBytes(new byte[]{1});
    }
}

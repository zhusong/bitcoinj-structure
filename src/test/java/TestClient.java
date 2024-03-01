import com.sun.istack.internal.Nullable;
import org.bitcoin.core.*;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestClient {
    private static final String MESSAGE_HEX =
            "04" // number of entries

                    + "61bc6649" // time, Fri Jan  9 02:54:25 UTC 2009
                    + "0000000000000000" // service flags, NODE_NONE
                    + "00000000000000000000ffff00000001" // address, fixed 16 bytes (IPv4 embedded in IPv6)
                    + "0000" // port

                    + "79627683" // time, Tue Nov 22 11:22:33 UTC 2039
                    + "0100000000000000"  // service flags, NODE_NETWORK
                    + "00000000000000000000000000000001" // address, fixed 16 bytes (IPv6)
                    + "00f1" // port

                    + "ffffffff" // time, Sun Feb  7 06:28:15 UTC 2106
                    + "4804000000000000" // service flags, NODE_WITNESS | NODE_COMPACT_FILTERS | NODE_NETWORK_LIMITED
                    + "00000000000000000000000000000001" // address, fixed 16 bytes (IPv6)
                    + "f1f2" // port

                    + "00000000" // time
                    + "0000000000000000" // service flags, NODE_NONE
                    + "fd87d87eeb43f1f2f3f4f5f6f7f8f9fa" // address, fixed 16 bytes (TORv2)
                    + "0000"; // port

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

//        while (true) {
//
//            peer.sendMessage(this.buildMessage());

            peer.sendMessage(this.buildAddress1Message());
//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }

        //发完就直接close了，所以不太好整，是守护线程，非守护线程才行。
        while(true){}
    }

    public Message buildAddress1Message() {
        AddressMessage message = new AddressV1Message(MainNetParams.get(), Utils.HEX.decode(MESSAGE_HEX));
        return message;
    }

    public Message buildMessage(){
        return new Ping(100);
    }
}

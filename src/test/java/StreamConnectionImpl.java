import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import org.bitcoin.core.MessageWriteTarget;
import org.bitcoin.core.StreamConnection;
import org.bitcoin.core.Utils;
import org.bouncycastle.util.encoders.Hex;

import java.nio.ByteBuffer;

public class StreamConnectionImpl implements StreamConnection {

    @Override
    public void connectionClosed() {
        System.out.println("connectionClosed called");
    }

    @Override
    public void connectionOpened() {
        System.out.println("connectionOpened called");
    }

    @Override
    public int receiveBytes(ByteBuffer buff) throws Exception {
        byte[] arr = new byte[buff.limit()];
        int i = 0;
        // 将ByteBuffer的内容写入到byte数组中
        while(buff.hasRemaining()){
            arr[i++] = buff.get();
        }

        System.out.println("receive bytes " + Utils.HEX.encode(arr));
        return buff.position();
    }

    @Override
    public void setWriteTarget(MessageWriteTarget writeTarget) {
        //System.out.println("setWriteTarget called");
    }

    @Override
    public int getMaxMessageSize() {
        //System.out.println("getMaxMessageSize called");
        return 0;
    }
}

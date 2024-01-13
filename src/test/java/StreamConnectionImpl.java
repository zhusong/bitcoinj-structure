import org.bitcoin.core.MessageWriteTarget;
import org.bitcoin.core.StreamConnection;

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
        System.out.println("receiveBytes called, buff size " + buff.array().length);
        return 0;
    }

    @Override
    public void setWriteTarget(MessageWriteTarget writeTarget) {
        System.out.println("setWriteTarget called");
    }

    @Override
    public int getMaxMessageSize() {
        System.out.println("getMaxMessageSize called");
        return 0;
    }
}

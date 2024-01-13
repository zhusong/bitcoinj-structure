import org.bitcoin.core.MessageWriteTarget;
import org.bitcoin.core.StreamConnection;

import java.nio.ByteBuffer;

public class StreamConnectionImpl implements StreamConnection {

    @Override
    public void connectionClosed() {

    }

    @Override
    public void connectionOpened() {

    }

    @Override
    public int receiveBytes(ByteBuffer buff) throws Exception {
        return 0;
    }

    @Override
    public void setWriteTarget(MessageWriteTarget writeTarget) {

    }

    @Override
    public int getMaxMessageSize() {
        return 0;
    }
}

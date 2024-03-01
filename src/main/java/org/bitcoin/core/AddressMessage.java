package org.bitcoin.core;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

public abstract class AddressMessage extends Message {

    protected static final long MAX_ADDRESSES = 1000;
    protected List<PeerAddress> addresses;

    AddressMessage(NetworkParameters params, byte[] payload, int offset, MessageSerializer serializer, int length) throws ProtocolException {
        super(params, payload, offset, serializer, length);
    }

    @Override
    protected void bitcoinSerializeToStream(OutputStream stream) throws IOException {
        if (addresses == null)
            return;
        stream.write(new VarInt(addresses.size()).encode());
        for (PeerAddress addr : addresses) {
            addr.bitcoinSerialize(stream);
        }
    }

    public abstract void addAddress(PeerAddress address);

    public void removeAddress(int index) {
        unCache();
        PeerAddress address = addresses.remove(index);
        address.setParent(null);
        length = UNKNOWN_LENGTH;
    }

    /**
     * @return An unmodifiableList view of the backing List of addresses. Addresses contained within the list may be
     * safely modified.
     */
    public List<PeerAddress> getAddresses() {
        return Collections.unmodifiableList(addresses);
    }
}

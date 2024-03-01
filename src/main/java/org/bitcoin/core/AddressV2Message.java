package org.bitcoin.core;

import java.util.ArrayList;

/**
 * <p>Represents an "addrv2" message on the P2P network, which contains broadcast IP addresses of other peers. This is
 * one of the ways peers can find each other without using the DNS or IRC discovery mechanisms. However storing and
 * using addrv2 messages is not presently implemented.</p>
 *
 * <p>See <a href="https://github.com/bitcoin/bips/blob/master/bip-0155.mediawiki">BIP155</a> for details.</p>
 *
 * <p>Instances of this class are not safe for use by multiple threads.</p>
 */
public class AddressV2Message extends AddressMessage {

    /**
     * Construct a new 'addrv2' message.
     * @param params NetworkParameters object.
     * @param offset The location of the first payload byte within the array.
     * @param serializer the serializer to use for this block.
     * @param length The length of message if known.  Usually this is provided when deserializing of the wire
     * as the length will be provided as part of the header.  If unknown then set to Message.UNKNOWN_LENGTH
     * @throws ProtocolException
     */
    AddressV2Message(NetworkParameters params, byte[] payload, int offset, MessageSerializer serializer, int length) throws ProtocolException {
        super(params, payload, offset, serializer, length);
    }

    /**
     * Construct a new 'addrv2' message.
     * @param params NetworkParameters object.
     * @param serializer the serializer to use for this block.
     * @param length The length of message if known.  Usually this is provided when deserializing of the wire
     * as the length will be provided as part of the header.  If unknown then set to Message.UNKNOWN_LENGTH
     * @throws ProtocolException
     */
    AddressV2Message(NetworkParameters params, byte[] payload, MessageSerializer serializer, int length) throws ProtocolException {
        super(params, payload, 0, serializer, length);
    }

    AddressV2Message(NetworkParameters params, byte[] payload) throws ProtocolException {
        super(params, payload, 0, params.getDefaultSerializer(), UNKNOWN_LENGTH);
    }

    @Override
    protected void parse() throws ProtocolException {
        final VarInt numAddressesVarInt = readVarInt();
        int numAddresses = numAddressesVarInt.intValue();
        // Guard against ultra large messages that will crash us.
        if (numAddresses > MAX_ADDRESSES)
            throw new ProtocolException("Address message too large.");
        addresses = new ArrayList<>(numAddresses);
        MessageSerializer serializer = this.serializer.withProtocolVersion(2);
        length = numAddressesVarInt.getSizeInBytes();
        for (int i = 0; i < numAddresses; i++) {
            PeerAddress addr = new PeerAddress(params, payload, cursor, this, serializer);
            addresses.add(addr);
            cursor += addr.getMessageSize();
            length += addr.getMessageSize();
        }
    }

    public void addAddress(PeerAddress address) {
        int protocolVersion = address.serializer.getProtocolVersion();
        if (protocolVersion != 2)
            throw new IllegalStateException("invalid protocolVersion: " + protocolVersion);

        unCache();
        address.setParent(this);
        addresses.add(address);
        length = UNKNOWN_LENGTH;
    }

    @Override
    public String toString() {
        return "addrv2: " + Utils.SPACE_JOINER.join(addresses);
    }
}

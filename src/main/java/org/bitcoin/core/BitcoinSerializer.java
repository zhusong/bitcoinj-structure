package org.bitcoin.core;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class BitcoinSerializer extends MessageSerializer{

    public BitcoinSerializer(AbstractBitcoinNetParams abstractBitcoinNetParams, boolean parseRetain) {
        super();
    }

    @Override
    public MessageSerializer withProtocolVersion(int protocolVersion) {
        return null;
    }

    @Override
    public int getProtocolVersion() {
        return 0;
    }

    @Override
    public Message deserialize(ByteBuffer in) throws ProtocolException, IOException, UnsupportedOperationException {
        return null;
    }

    @Override
    public BitcoinPacketHeader deserializeHeader(ByteBuffer in) throws ProtocolException, IOException, UnsupportedOperationException {
        return null;
    }

    @Override
    public Message deserializePayload(BitcoinPacketHeader header, ByteBuffer in) throws ProtocolException, BufferUnderflowException, UnsupportedOperationException {
        return null;
    }

    @Override
    public boolean isParseRetainMode() {
        return false;
    }

    @Override
    public AddressV1Message makeAddressV1Message(byte[] payloadBytes, int length) throws ProtocolException, UnsupportedOperationException {
        return null;
    }

    @Override
    public AddressV2Message makeAddressV2Message(byte[] payloadBytes, int length) throws ProtocolException, UnsupportedOperationException {
        return null;
    }

    @Override
    public Block makeBlock(byte[] payloadBytes, int offset, int length) throws ProtocolException, UnsupportedOperationException {
        return null;
    }

    @Override
    public Message makeBloomFilter(byte[] payloadBytes) throws ProtocolException, UnsupportedOperationException {
        return null;
    }

    @Override
    public FilteredBlock makeFilteredBlock(byte[] payloadBytes) throws ProtocolException, UnsupportedOperationException {
        return null;
    }

    @Override
    public InventoryMessage makeInventoryMessage(byte[] payloadBytes, int length) throws ProtocolException, UnsupportedOperationException {
        return null;
    }

    @Override
    public Transaction makeTransaction(byte[] payloadBytes, int offset, int length, byte[] hash) throws ProtocolException, UnsupportedOperationException {
        return null;
    }

    @Override
    public void seekPastMagicBytes(ByteBuffer in) throws BufferUnderflowException {

    }

    @Override
    public void serialize(String name, byte[] message, OutputStream out) throws IOException, UnsupportedOperationException {

    }

    @Override
    public void serialize(Message message, OutputStream out) throws IOException, UnsupportedOperationException {

    }

    public class BitcoinPacketHeader {

        public static final int HEADER_LENGTH = 4 + 4 + 4;
        public int size;
    }
}

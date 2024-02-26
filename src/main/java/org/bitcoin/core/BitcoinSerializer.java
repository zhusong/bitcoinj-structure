package org.bitcoin.core;

import com.sun.javafx.binding.StringFormatter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class BitcoinSerializer extends MessageSerializer{
    private static final int COMMAND_LEN = 12;

    private final NetworkParameters params;
    private final int protocolVersion;
    private final boolean parseRetain;

    private static final Map<Class<? extends Message>, String> names = new HashMap<>();
    static {
        names.put(Ping.class, "ping");
        names.put(Pong.class, "pong");
    }
    /**
     * Constructs a BitcoinSerializer with the given behavior.
     *
     * @param params           networkParams used to create Messages instances and determining packetMagic
     * @param parseRetain      retain the backing byte array of a message for fast reserialization.
     */
    public BitcoinSerializer(NetworkParameters params, boolean parseRetain) {
        this(params, params.getProtocolVersionNum(NetworkParameters.ProtocolVersion.CURRENT), parseRetain);
    }

    /**
     * Constructs a BitcoinSerializer with the given behavior.
     *
     * @param params           networkParams used to create Messages instances and determining packetMagic
     * @param protocolVersion  the protocol version to use
     * @param parseRetain      retain the backing byte array of a message for fast reserialization.
     */
    public BitcoinSerializer(NetworkParameters params, int protocolVersion, boolean parseRetain) {
        this.params = params;
        this.protocolVersion = protocolVersion;
        this.parseRetain = parseRetain;
    }

    @Override
    public BitcoinSerializer withProtocolVersion(int protocolVersion) {
        return protocolVersion == this.protocolVersion ?
                this : new BitcoinSerializer(params, protocolVersion, parseRetain);
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

    /**
     * Writes message to to the output stream.
     */
    @Override
    public void serialize(Message message, OutputStream out) throws IOException {
        String name = names.get(message.getClass());
        if (name == null) {
            throw new Error("BitcoinSerializer doesn't currently know how to serialize " + message.getClass());
        }
        serialize(name, message.bitcoinSerialize(), out);
    }

    /**
     * Writes message to to the output stream.
     */
    @Override
    public void serialize(String name, byte[] message, OutputStream out) throws IOException {
        //数据头：4（magic） + 12（command） + 4（payload length） + 4（checksum）
        byte[] header = new byte[4 + COMMAND_LEN + 4 + 4 /* checksum */];
        Utils.uint32ToByteArrayBE(params.getPacketMagic(), header, 0);

        // The header array is initialized to zero by Java so we don't have to worry about
        // NULL terminating the string here.
        for (int i = 0; i < name.length() && i < COMMAND_LEN; i++) {
            header[4 + i] = (byte) (name.codePointAt(i) & 0xFF);
        }

        Utils.uint32ToByteArrayLE(message.length, header, 4 + COMMAND_LEN);

        byte[] hash = Sha256Hash.hashTwice(message);
        System.arraycopy(hash, 0, header, 4 + COMMAND_LEN + 4, 4);
        out.write(header);
        out.write(message);

        System.out.println("Sending " + name + ", message:" + Utils.HEX.encode(header) + ", " +Utils.HEX.encode(message));
    }

    public class BitcoinPacketHeader {

        public static final int HEADER_LENGTH = 4 + 4 + 4;
        public int size;
    }
}

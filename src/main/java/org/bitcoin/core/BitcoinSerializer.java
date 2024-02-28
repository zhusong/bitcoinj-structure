package org.bitcoin.core;

import com.sun.javafx.binding.StringFormatter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.bitcoin.core.Utils.HEX;
import static org.bitcoin.core.Utils.readUint32;

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
    public Message deserialize(ByteBuffer in) throws ProtocolException, IOException {
        // A Bitcoin protocol message has the following format.
        //
        //   - 4 byte magic number: 0xfabfb5da for the testnet or
        //                          0xf9beb4d9 for production
        //   - 12 byte command in ASCII
        //   - 4 byte payload size
        //   - 4 byte checksum
        //   - Payload data
        //
        // The checksum is the first 4 bytes of a SHA256 hash of the message payload. It isn't
        // present for all messages, notably, the first one on a connection.
        //
        // Bitcoin Core ignores garbage before the magic header bytes. We have to do the same because
        // sometimes it sends us stuff that isn't part of any message.
        seekPastMagicBytes(in);
        BitcoinPacketHeader header = new BitcoinPacketHeader(in);
        // Now try to read the whole message.
        return deserializePayload(header, in);
    }

    @Override
    public BitcoinPacketHeader deserializeHeader(ByteBuffer in) throws ProtocolException, IOException, UnsupportedOperationException {
        return null;
    }

    @Override
    public Message deserializePayload(BitcoinPacketHeader header, ByteBuffer in) throws ProtocolException, BufferUnderflowException, UnsupportedOperationException {
        byte[] payloadBytes = new byte[header.size];
        in.get(payloadBytes, 0, header.size);

        // Verify the checksum.
        byte[] hash;
        hash = Sha256Hash.hashTwice(payloadBytes);
        if (header.checksum[0] != hash[0] || header.checksum[1] != hash[1] ||
                header.checksum[2] != hash[2] || header.checksum[3] != hash[3]) {

            throw new ProtocolException("Checksum failed to verify, actual " +
                    HEX.encode(hash) +
                    " vs " + HEX.encode(header.checksum));
        }

        System.out.println("Received {} byte '{}' message: {}" + header.size + header.command + HEX.encode(payloadBytes));
        try {
            return makeMessage(header.command, header.size, payloadBytes, hash, header.checksum);
        } catch (Exception e) {
            throw new ProtocolException("Error deserializing message " + HEX.encode(payloadBytes) + "\n", e);
        }
    }

    private Message makeMessage(String command, int length, byte[] payloadBytes, byte[] hash, byte[] checksum) throws ProtocolException {
        // We use an if ladder rather than reflection because reflection is very slow on Android.
        if (command.equals("ping")) {
            return new Ping(params, payloadBytes);
        } else if (command.equals("pong")) {
            return new Pong(params, payloadBytes);
        } else {
            return new UnknownMessage(params, command, payloadBytes);
        }
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

        System.out.println("Sending " + name + ", message:" + HEX.encode(header) + ", " + HEX.encode(message));
    }


    public static class BitcoinPacketHeader {
        /** The largest number of bytes that a header can represent */
        public static final int HEADER_LENGTH = COMMAND_LEN + 4 + 4;

        public final byte[] header;
        public final String command;
        public final int size;
        public final byte[] checksum;

        public BitcoinPacketHeader(ByteBuffer in) throws ProtocolException, BufferUnderflowException {
            //头大小
            header = new byte[HEADER_LENGTH];
            in.get(header, 0, header.length);

            int cursor = 0;

            // The command is a NULL terminated string, unless the command fills all twelve bytes
            // in which case the termination is implicit.
            for (; header[cursor] != 0 && cursor < COMMAND_LEN; cursor++) ;
            byte[] commandBytes = new byte[cursor];
            System.arraycopy(header, 0, commandBytes, 0, cursor);


            command = new String(commandBytes, StandardCharsets.US_ASCII);
            cursor = COMMAND_LEN;


            //4字节长度
            size = (int) readUint32(header, cursor);
            cursor += 4;

            if (size > Message.MAX_SIZE || size < 0)
                throw new ProtocolException("Message size too large: " + size);

            // Old clients don't send the checksum.
            checksum = new byte[4];
            // Note that the size read above includes the checksum bytes.
            System.arraycopy(header, cursor, checksum, 0, 4);
            cursor += 4;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof BitcoinSerializer)) return false;
        BitcoinSerializer other = (BitcoinSerializer) o;
        return Objects.equals(params, other.params) &&
                protocolVersion == other.protocolVersion &&
                parseRetain == other.parseRetain;
    }

    @Override
    public int hashCode() {
        return Objects.hash(params, protocolVersion, parseRetain);
    }
}

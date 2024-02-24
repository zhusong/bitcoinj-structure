package org.bitcoin.core;

import com.google.common.io.BaseEncoding;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * <p>A PeerAddress holds an IP address and port number representing the network location of
 * a peer in the Bitcoin P2P network. It exists primarily for serialization purposes.</p>
 *
 * <p>This class abuses the protocol version contained in its serializer. It can only contain 0 (format within
 * {@link VersionMessage}), 1 ({@link AddressV1Message}) or 2 ({@link AddressV2Message}).</p>
 *
 * <p>Instances of this class are not safe for use by multiple threads.</p>
 *
 * 定义了一个PeerAddress类，用于表示比特币P2P网络中对等节点的网络位置，包括IP地址和端口号。它主要用于序列化目的。
 *
 * 该类滥用了其序列化器中包含的协议版本。它只能包含0（{@link VersionMessage}中的格式）、
 * 1（{@link AddressV1Message}）或2（{@link AddressV2Message}）。
 *
 * 这个类的实例不适用于多线程环境。
 */
public class PeerAddress extends ChildMessage{
    private InetAddress addr;
    private String hostname; // Used for .onion addresses
    private int port;
    private BigInteger services;
    private long time;

    private static final BaseEncoding BASE32 = BaseEncoding.base32().lowerCase();
    private static final byte[] ONIONCAT_PREFIX = Utils.HEX.decode("fd87d87eeb43");

    /**
     * Construct a peer address from a serialized payload.
     * @param params NetworkParameters object.
     * @param payload Bitcoin protocol formatted byte array containing message content.
     * @param offset The location of the first payload byte within the array.
     * @param serializer the serializer to use for this message.
     * @throws ProtocolException
     */
    public PeerAddress(NetworkParameters params, byte[] payload, int offset, Message parent, MessageSerializer serializer) throws ProtocolException {
        super(params, payload, offset, parent, serializer, UNKNOWN_LENGTH);
    }

    /**
     * Construct a peer address from a memorized or hardcoded address.
     */
    public PeerAddress(NetworkParameters params, InetAddress addr, int port, BigInteger services, MessageSerializer serializer) {
        super(params);
        this.addr = checkNotNull(addr);
        this.port = port;
        setSerializer(serializer);
        this.services = services;
        this.time = Utils.currentTimeSeconds();
    }

    /**
     * Constructs a peer address from the given IP address, port and services. Version number is default for the given parameters.
     */
    public PeerAddress(NetworkParameters params, InetAddress addr, int port, BigInteger services) {
        this(params, addr, port, services, params.getDefaultSerializer().withProtocolVersion(0));
    }

    /**
     * Constructs a peer address from the given IP address and port. Version number is default for the given parameters.
     */
    public PeerAddress(NetworkParameters params, InetAddress addr, int port) {
        this(params, addr, port, BigInteger.ZERO);
    }

    /**
     * Constructs a peer address from the given IP address. Port and version number are default for the given
     * parameters.
     */
    public PeerAddress(NetworkParameters params, InetAddress addr) {
        this(params, addr, params.getPort());
    }

    /**
     * Constructs a peer address from an {@link InetSocketAddress}. An InetSocketAddress can take in as parameters an
     * InetAddress or a String hostname. If you want to connect to a .onion, set the hostname to the .onion address.
     */
    public PeerAddress(NetworkParameters params, InetSocketAddress addr) {
        this(params, addr.getAddress(), addr.getPort());
    }

    /**
     * Constructs a peer address from a stringified hostname+port. Use this if you want to connect to a Tor .onion address.
     */
    public PeerAddress(NetworkParameters params, String hostname, int port) {
        super(params);
        this.hostname = hostname;
        this.port = port;
        this.services = BigInteger.ZERO;
        this.time = Utils.currentTimeSeconds();
    }


    /**
     * Overrides the message serializer.
     * @param serializer the new serializer
     */
    public void setSerializer(MessageSerializer serializer) {
        if (!this.serializer.equals(serializer)) {
            this.serializer = serializer;
            unCache();
        }
    }

    @Override
    protected void parse() throws ProtocolException {
        //TODO
    }

    public InetSocketAddress toSocketAddress() {
        // Reconstruct the InetSocketAddress properly
        if (hostname != null) {
            return InetSocketAddress.createUnresolved(hostname, port);
        } else {
            return new InetSocketAddress(addr, port);
        }
    }
}

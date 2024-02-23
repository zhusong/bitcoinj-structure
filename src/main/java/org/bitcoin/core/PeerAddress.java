package org.bitcoin.core;

import com.google.common.io.BaseEncoding;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import static com.google.common.base.Preconditions.checkNotNull;

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

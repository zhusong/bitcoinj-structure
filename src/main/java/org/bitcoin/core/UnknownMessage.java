package org.bitcoin.core;

public class UnknownMessage extends EmptyMessage {

    private String name;

    public UnknownMessage(NetworkParameters params, String name, byte[] payloadBytes) throws ProtocolException {
        super(params, payloadBytes, 0);
        this.name = name;
    }

    @Override
    public String toString() {
        return "Unknown message [" + name + "]" + (payload == null ? "" : ": " + Utils.HEX.encode(payload));
    }
}
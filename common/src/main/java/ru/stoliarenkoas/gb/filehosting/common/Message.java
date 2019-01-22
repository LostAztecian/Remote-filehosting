package ru.stoliarenkoas.gb.filehosting.common;

import lombok.Getter;

public class Message {

    private static final Type[] TYPE_VALUES = Type.values();
    @Getter private final byte[] bytes;
    @Getter private final int part;
    @Getter private final int parts;

    public Message(byte type, byte[] payload, int part, int parts) {
        bytes = new byte[11 + payload.length];
        bytes[0] = type;
        bytes[1] = (byte)(payload.length >>> 8);
        bytes[2] = (byte)(payload.length);

        this.part = part;
        bytes[3] = (byte)(part >>> 24);
        bytes[4] = (byte)(part >>> 16);
        bytes[5] = (byte)(part >>> 8);
        bytes[6] = (byte)(part);

        this.parts = parts;
        bytes[7] = (byte)(parts >>> 24);
        bytes[8] = (byte)(parts >>> 16);
        bytes[9] = (byte)(parts >>> 8);
        bytes[10] = (byte)(parts);

        System.arraycopy(payload, 0, bytes, 11, payload.length);
    }

    public Message(byte type, byte[] payload) {
        this(type, payload, 1, 1);
    }

    public Type getType() {
        return TYPE_VALUES[bytes[0]];
    }

    public enum Type{
        HANDSHAKE,
        HANDSHAKE_RESPONSE,
        BYTES_SOLID,
        BYTES_PARTITIONED
    }
}

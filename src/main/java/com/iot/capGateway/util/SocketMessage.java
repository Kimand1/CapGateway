package com.iot.capGateway.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public record SocketMessage(MessageId messageId, DataFormat dataFormat, String data) {
    private static final int MAGIC = 0xF020190F;

    public byte[] toBytes() {
        byte[] body = (data == null) ? new byte[0] : data.getBytes(StandardCharsets.UTF_8);
        return ByteBuffer.allocate(16 + body.length)
                .putInt(messageId().code())     // record accessor + enum 메서드
                .putInt(dataFormat().code())    // record accessor + enum 메서드
                .putInt(MAGIC)
                .putInt(body.length)
                .put(body)
                .array();
    }

    public static SocketMessage fromBytes(byte[] frame) {
        var b = ByteBuffer.wrap(frame);
        int mid = b.getInt(), fmt = b.getInt(), magic = b.getInt(), len = b.getInt();
        if (magic != MAGIC) throw new IllegalStateException("Bad MAGIC: " + Integer.toHexString(magic));
        byte[] body = new byte[len]; b.get(body);
        return new SocketMessage(MessageId.from(mid), DataFormat.from(fmt), new String(body, StandardCharsets.UTF_8));
    }
}

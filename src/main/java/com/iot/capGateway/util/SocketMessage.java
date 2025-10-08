package com.iot.capGateway.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public record SocketMessage(MessageId messageId, DataFormat dataFormat, String data) {
    private static final int MAGIC = 0xF020190F;

    public byte[] toBytes() {
        byte[] body = data == null ? new byte[0] : data.getBytes(StandardCharsets.UTF_8);
        return ByteBuffer.allocate(16 + body.length)
                .order(ByteOrder.BIG_ENDIAN)          // ✅ C#과 호환(HEX→바이트 순서)
                .putInt(messageId().code())
                .putInt(dataFormat().code())
                .putInt(MAGIC)
                .putInt(body.length)
                .put(body)
                .array();
    }

    public static SocketMessage fromHeaderAndBody(byte[] header16, byte[] body) {
        var h = ByteBuffer.wrap(header16).order(ByteOrder.BIG_ENDIAN);
        int mid = h.getInt();
        int fmt = h.getInt();
        int magic = h.getInt();
        int len = h.getInt();
        if (magic != MAGIC) throw new IllegalStateException("Bad MAGIC: 0x" + Integer.toHexString(magic));
        if (len < 0 || len > 4_194_304) throw new IllegalStateException("Invalid length: " + len);
        if (body.length != len) throw new IllegalStateException("Body size mismatch: " + body.length + " != " + len);
        return new SocketMessage(
                MessageId.from(mid),
                DataFormat.from(fmt),
                new String(body, StandardCharsets.UTF_8)
        );
    }
}

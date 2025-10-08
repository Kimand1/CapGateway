package com.iot.capGateway.util;

public enum MessageId {
    ETS_REQ_SYS_CON(0xFFEE1001),
    ETS_RES_SYS_CON(0xFFEE2001),
    ETS_REQ_SYS_STS(0xFFEE1010),
    ETS_RES_SYS_STS(0xFFEE2010),
    ETS_NFY_DIS_INFO(0xFFEE3020),
    ETS_CNF_DIS_INFO(0xFFEE4020);

    private final int code;
    MessageId(int c) { this.code = c; }

    public int code() { return code; } // ✅ 추가

    public static MessageId from(int v) {
        for (var m : values()) if (m.code == v) return m;
        throw new IllegalArgumentException("Unknown MessageId: 0x" + Integer.toHexString(v));
    }
}

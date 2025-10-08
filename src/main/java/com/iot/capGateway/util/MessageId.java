package com.iot.capGateway.util;

public enum MessageId {
    ETS_REQ_SYS_CON(0xFFEE1020),
    ETS_RES_SYS_CON(0xFFEE2020),
    ETS_REQ_SYS_STS(0xFFEE3020),
    ETS_RES_SYS_STS(0xFFEE3120),
    ETS_NFY_DIS_INFO(0xFFEE4020),
    ETS_CNF_DIS_INFO(0xFFEE5020);

    private final int code;
    MessageId(int code) { this.code = code; }
    public int code() { return code; }   // ✅ 메서드 제공
    public static MessageId from(int v) {
        for (var m : values()) if (m.code == v) return m;
        throw new IllegalArgumentException("Unknown MessageId: " + v);
    }
}

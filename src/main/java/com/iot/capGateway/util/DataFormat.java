package com.iot.capGateway.util;

public enum DataFormat {
    XML(1), JSON(2);

    private final int code;
    DataFormat(int code) { this.code = code; }
    public int code() { return code; }   // ✅ 메서드 제공
    public static DataFormat from(int v) { return v == 2 ? JSON : XML; }
}

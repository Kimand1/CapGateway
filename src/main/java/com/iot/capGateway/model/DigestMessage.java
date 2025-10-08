package com.iot.capGateway.model;

import com.iot.capGateway.util.XmlUtil;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "data")          // C# 원본과 동일
public class DigestMessage {

    @XmlElement(name = "destId")
    private String DestId;

    @XmlElement(name = "resultCode")
    private String ResultCode;

    @XmlElement(name = "result")
    private String Result;

    @XmlElement(name = "nonce")
    private String Nonce;

    @XmlElement(name = "realm")
    private String Realm;

    @XmlElement(name = "response")
    private String Response;

    public String toXml() { return XmlUtil.toXml(this); }
    public static DigestMessage fromXml(String xml) { return XmlUtil.fromXml(xml, DigestMessage.class); }

    /**
     * C# 원본과 동일한 Digest 수식:
     *   a = MD5( DestId + ":" + Realm + ":" + userPass )
     *   Response = MD5( a + ":" + Nonce )
     * 인코딩: ASCII, 출력: 소문자 hex
     */
    public void setResponseValue(String userPass) {
        String a = md5Hex(DestId + ":" + Realm + ":" + userPass);
        this.Response = md5Hex(a + ":" + Nonce);
    }

    private static String md5Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] out = md.digest(s.getBytes(StandardCharsets.US_ASCII));
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

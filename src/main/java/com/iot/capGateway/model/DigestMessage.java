package com.iot.capGateway.model;

import com.iot.capGateway.util.XmlUtil;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "DigestMessage")
@XmlAccessorType(XmlAccessType.FIELD)
public class DigestMessage {
    private String destId;
    private String realm;
    private String nonce;
    private String result;
    private String resultCode;

    public DigestMessage(String id) {
        this.destId = id;
    }

    public void setResponseValue(String password) {
        // 간단한 응답 생성 예시
        this.result = "OK";
        this.resultCode = "200";
    }

    public String toXml() {
        return XmlUtil.toXml(this);
    }

    public static DigestMessage fromXml(String xml) {
        return XmlUtil.fromXml(xml, DigestMessage.class);
    }
}

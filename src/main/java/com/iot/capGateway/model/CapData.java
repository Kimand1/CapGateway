package com.iot.capGateway.model;

import com.iot.capGateway.util.XmlUtil;
import jakarta.xml.bind.annotation.*;
import lombok.Data;

@Data
@XmlRootElement(name = "CapData")
@XmlAccessorType(XmlAccessType.FIELD)
public class CapData {
    private String identifier;
    private String result;
    private String resultCode;
    private CapInfo capInfo;

    public String toXml() { return XmlUtil.toXml(this); }

    public static CapData fromXml(String xml) {
        try {
            // 1) 먼저 CapData 형태로 시도 (기존 포맷)
            return XmlUtil.fromXml(xml, CapData.class);
        } catch (Exception ignore) {
            // 2) 실패하면 CAP 1.2의 <alert>로 파싱 후 래핑
            Alert alert = XmlUtil.fromXml(xml, Alert.class);
            CapInfo capInfo = new CapInfo();
            capInfo.setAlert(alert);

            CapData cd = new CapData();
            cd.setIdentifier(alert.getIdentifier());
            cd.setCapInfo(capInfo);
            return cd;
        }
    }

    public boolean isValid(String locationCode) {
        if (capInfo == null || capInfo.getAlert() == null) return false;
        var codes = capInfo.getAlert().getGeoCodes();
        if (codes == null || codes.isEmpty()) return false;
        return codes.stream().anyMatch(code ->
                locationCode.contains(code) || "4300000000".equals(code));
    }
}

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

    // 수신 원문 XML 보관
    @XmlTransient
    private String rawXml;

    /** 편의 메서드: 내부 capInfo에서 Alert 바로 꺼내기 */
    @XmlTransient
    public Alert getAlert() {
        return (capInfo != null) ? capInfo.getAlert() : null;
    }

    /** 편의 메서드: Alert를 넣으면 내부 capInfo를 자동 생성/설정 */
    @XmlTransient
    public void setAlert(Alert alert) {
        if (this.capInfo == null) this.capInfo = new CapInfo();
        this.capInfo.setAlert(alert);
        if (alert != null && (this.identifier == null || this.identifier.isBlank())) {
            this.identifier = alert.getIdentifier();
        }
    }

    public String toXml() { return XmlUtil.toXml(this); }

    public static CapData fromXml(String xml) {
        try {
            // 1) CapData 형태로 시도
            CapData cd = XmlUtil.fromXml(xml, CapData.class);
            // 원문 보관
            cd.setRawXml(xml);
            return cd;
        } catch (Exception ignore) {
            // 2) 실패하면 CAP 1.2 <alert> 로 파싱 후 래핑
            Alert alert = XmlUtil.fromXml(xml, Alert.class);
            CapData cd = new CapData();
            cd.setAlert(alert);               // 내부 capInfo 자동 생성됨
            cd.setIdentifier(alert.getIdentifier());
            // (선택) 원문 보관
            cd.setRawXml(xml);
            return cd;
        }
    }

    public boolean isValid(String locationCode) {
        Alert alert = getAlert();
        if (alert == null) return false;
        var codes = alert.getGeoCodes();
        if (codes == null || codes.isEmpty()) return false;
        return codes.stream().anyMatch(code ->
                locationCode.contains(code) || "4300000000".equals(code));
    }
}

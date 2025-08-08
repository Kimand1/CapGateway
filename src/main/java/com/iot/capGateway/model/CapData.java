package com.iot.capGateway.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "CapData")
@XmlAccessorType(XmlAccessType.FIELD)
public class CapData {
    private String identifier;
    private String result;
    private String resultCode;
    private CapInfo capInfo;

    public String toXml() {
        return XmlUtil.toXml(this);
    }

    public static CapData fromXml(String xml) {
        return XmlUtil.fromXml(xml, CapData.class);
    }

    public boolean isValid(String locationCode) {
        if (capInfo == null || capInfo.getAlert() == null || capInfo.getAlert().getGeoCodes() == null)
            return false;

        return capInfo.getAlert().getGeoCodes().stream().anyMatch(code ->
                locationCode.contains(code) || "4300000000".equals(code));
    }
}

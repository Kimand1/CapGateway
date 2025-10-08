package com.iot.capGateway.model;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "info", propOrder = {
        "language", "category", "event", "responseType",
        "urgency", "severity", "certainty",
        "audience", "eventCode", "effective",
        "onset", "expires", "senderName",
        "headline", "description", "instruction",
        "web", "contact", "parameter", "area"
})
public class CapInfo {

    // === Getter/Setter ===
    private String language;
    private String category;
    private String event;
    private String responseType;
    private String urgency;
    private String severity;
    private String certainty;
    private String audience;
    private String eventCode;
    private String effective;
    private String onset;
    private String expires;
    private String senderName;
    private String headline;
    private String description;
    private String instruction;
    private String web;
    private String contact;
    /** CAP Alert 블록 (필수) */
    @XmlElement(name = "alert", namespace = Alert.CAP_NS)
    private Alert alert;

    @XmlElement(name = "parameter")
    private List<CapParameter> parameter;

    @XmlElement(name = "area")
    private List<CapArea> area;

    // 기타 필드 생략 가능
}

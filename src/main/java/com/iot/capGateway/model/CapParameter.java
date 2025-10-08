package com.iot.capGateway.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class CapParameter {

    @XmlElement(name = "valueName", namespace = Alert.CAP_NS)
    private String valueName;

    @XmlElement(name = "value", namespace = Alert.CAP_NS)
    private String value;
}

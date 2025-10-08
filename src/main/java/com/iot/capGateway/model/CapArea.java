package com.iot.capGateway.model;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"areaDesc","polygon","geocode"})
public class CapArea {

    @XmlElement(namespace = Alert.CAP_NS)
    private String areaDesc;

    /** "lat,lon lat,lon ..." 형태의 폴리곤 문자열 */
    @XmlElement(namespace = Alert.CAP_NS)
    private String polygon;

    /** 표준 CAP의 geocode 구조 (valueName/value) */
    @XmlElement(name = "geocode", namespace = Alert.CAP_NS)
    private List<CapGeocode> geocode = new ArrayList<>();
}

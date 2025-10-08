package com.iot.capGateway.model;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {
        "language","category","event","urgency","severity","certainty",
        "eventCode","senderName","headline","description","instruction",
        "parameter","area"
})
public class CapInfoBlock {

    @XmlElement(namespace = Alert.CAP_NS)
    private String language;

    @XmlElement(namespace = Alert.CAP_NS)
    private String category;

    @XmlElement(namespace = Alert.CAP_NS)
    private String event;

    @XmlElement(namespace = Alert.CAP_NS)
    private String urgency;

    @XmlElement(namespace = Alert.CAP_NS)
    private String severity;

    @XmlElement(namespace = Alert.CAP_NS)
    private String certainty;

    @XmlElement(name = "eventCode", namespace = Alert.CAP_NS)
    private List<CapEventCode> eventCode = new ArrayList<>();

    @XmlElement(namespace = Alert.CAP_NS)
    private String senderName;

    @XmlElement(namespace = Alert.CAP_NS)
    private String headline;

    @XmlElement(namespace = Alert.CAP_NS)
    private String description;

    @XmlElement(namespace = Alert.CAP_NS)
    private String instruction;

    @XmlElement(name = "parameter", namespace = Alert.CAP_NS)
    private List<CapParameter> parameter = new ArrayList<>();

    @XmlElement(name = "area", namespace = Alert.CAP_NS)
    private List<CapArea> area = new ArrayList<>();

    /** info 레벨에서 직접 geocode를 두는 경우 대비(표준 확장 대응) */
    @XmlElement(name = "geocode", namespace = Alert.CAP_NS)
    private List<CapGeocode> geocode = new ArrayList<>();

    /** info 레벨의 geocode value만 추출 */
    public Stream<String> getGeocodeValuesFromInfoLevel() {
        return geocode == null ? Stream.empty()
                : geocode.stream()
                .filter(Objects::nonNull)
                .map(CapGeocode::getValue)
                .filter(Objects::nonNull);
    }
}

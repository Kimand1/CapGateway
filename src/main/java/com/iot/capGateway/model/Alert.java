// src/main/java/com/iot/capGateway/model/Alert.java
package com.iot.capGateway.model;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
// ✅ 추가
import java.util.stream.Collectors;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "alert", namespace = Alert.CAP_NS)
@XmlType(propOrder = {
        "identifier","sender","sent","status","msgType","scope",
        "code", "info"
})
public class Alert {
    public static final String CAP_NS = "urn:oasis:names:tc:emergency:cap:1.2";

    @XmlElement(namespace = CAP_NS) private String identifier;
    @XmlElement(namespace = CAP_NS) private String sender;
    @XmlElement(namespace = CAP_NS) private String sent;
    @XmlElement(namespace = CAP_NS) private String status;
    @XmlElement(namespace = CAP_NS) private String msgType;
    @XmlElement(namespace = CAP_NS) private String scope;

    @XmlElement(name = "code", namespace = CAP_NS)
    private List<String> code = new ArrayList<>();

    @XmlElement(name = "info", namespace = CAP_NS)
    private List<CapInfoBlock> info = new ArrayList<>();

    /** ✅ 수정된 버전: 불필요한 .stream() 제거, Collectors 임포트 */
    public List<String> getGeoCodes() {
        if (info == null) return List.of();

        return info.stream()
                .filter(Objects::nonNull)
                .flatMap(i -> {
                    // area가 없으면 info 레벨의 geocode Stream을 그대로 사용
                    if (i.getArea() == null || i.getArea().isEmpty()) {
                        return i.getGeocodeValuesFromInfoLevel();
                    }
                    // area가 있으면 area의 geocode value 우선
                    return i.getArea().stream()
                            .filter(Objects::nonNull)
                            .flatMap(a -> {
                                if (a.getGeocode() == null || a.getGeocode().isEmpty()) {
                                    return i.getGeocodeValuesFromInfoLevel();
                                }
                                return a.getGeocode().stream()
                                        .map(CapGeocode::getValue)
                                        .filter(Objects::nonNull);
                            });
                })
                .filter(v -> v != null && !v.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }
}

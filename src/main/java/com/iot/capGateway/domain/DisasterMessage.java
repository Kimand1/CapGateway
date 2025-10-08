package com.iot.capGateway.domain;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name="disaster_message")
@Getter @Setter
public class DisasterMessage {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long seq;
    @Column(unique=true) private String identifier;
    @Lob private String capXml;
    private String resultCode;
    private LocalDateTime createdAt = LocalDateTime.now();
}

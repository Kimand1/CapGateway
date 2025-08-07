package com.iot.capGateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class AppConfiguration {
    private String dbId;
    private String dbPw;
    private String serverId;
    private String serverPw;
}

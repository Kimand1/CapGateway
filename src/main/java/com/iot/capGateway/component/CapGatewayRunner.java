package com.iot.capGateway.component;

import com.iot.capGateway.config.AppConfiguration;
import com.iot.capGateway.service.GatewayManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
@ConditionalOnProperty(name = "gateway.enabled", havingValue = "true", matchIfMissing = true)
public class CapGatewayRunner implements CommandLineRunner {

    private final GatewayManager gatewayManager;
    private final AppConfiguration config;

    @Override
    public void run(String... args) {
        String nagIp = config.getNagIp();
        Integer nagPort = config.getNagPort();
        String nagId = config.getNAGAuthId();
        String nagPw = config.getNAGAuthPw();

        if (nagIp == null || nagPort == null || nagId == null || nagPw == null) {
            log.error("NAG 연결 정보가 부족합니다. (nagIp/nagPort/NAGAuthId/NAGAuthPw). AppConfig.json 또는 환경변수를 확인하세요.");
            return;
        }

        log.info("Starting CAP Gateway -> {}:{} (id={})", nagIp, nagPort, nagId);
        gatewayManager.run(nagIp, nagPort, nagId, nagPw);
    }
}

package com.iot.capGateway.component;

import com.iot.capGateway.config.AppConfiguration;
import com.iot.capGateway.service.GatewayManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CapGatewayRunner implements CommandLineRunner {

    private final GatewayManager gatewayManager;     // ✅ 스프링 빈 주입
    private final AppConfiguration appConfiguration; // AppConfig.json 파싱 결과(이미 기존 코드에 있음)

    @Override
    public void run(String... args) {
        // 실행 인자: <NAG_IP> <NAG_PORT> <DB_IP> <DB_PORT> ... 식으로 들어오는 기존 방식을 유지하되,
        // NAG 인증 계정은 AppConfig.json에서 읽어온 값을 우선 사용
        String nagIp   = args.length > 0 ? args[0] : "127.0.0.1";
        int nagPort    = args.length > 1 ? Integer.parseInt(args[1]) : 9404;

        // AppConfig.json 내 계정 사용
        String nagId   = appConfiguration.getNAGAuthId();
        String nagPw   = appConfiguration.getNAGAuthPw();

        log.info("Starting CAP Gateway with NAG {}:{} (id={})", nagIp, nagPort, nagId);
        gatewayManager.run(nagIp, nagPort, nagId, nagPw);   // ✅ 단 한 줄로 진입
    }
}

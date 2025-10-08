package com.iot.capGateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Getter
@Setter
@Component
@Slf4j
public class AppConfiguration {

    /** 직접 보유하는 설정 필드들 (json/환경변수/프로퍼티로 채워짐) */
    private String dbId;
    private String dbPw;
    private String serverId;
    private String serverPw;
    private String NAGAuthId;  // 대소문자 유지 (프로젝트 내 기존 사용을 고려)
    private String NAGAuthPw;

    private final ObjectMapper mapper = new ObjectMapper();

    /** 애플리케이션 시작 시 AppConfig.json을 (있다면) 로드하여 빈 필드를 채운다 */
    @PostConstruct
    public void init() {
        try {
            // 1) 실행 디렉터리
            Path p = Paths.get("AppConfig.json");
            if (Files.notExists(p)) {
                // 2) classpath:/AppConfig.json (src/main/resources) 시도
                URL res = getClass().getClassLoader().getResource("AppConfig.json");
                if (res != null) {
                    p = Paths.get(res.toURI());
                }
            }
            if (Files.exists(p)) {
                var json = Files.readString(p);
                LocalConfig lc = mapper.readValue(json, LocalConfig.class);
                // 파일에 값이 있으면 내 필드가 비어 있을 때만 채움
                this.dbId      = firstNonBlank(this.dbId, lc.dbId);
                this.dbPw      = firstNonBlank(this.dbPw, lc.dbPw);
                this.serverId  = firstNonBlank(this.serverId, lc.serverId);
                this.serverPw  = firstNonBlank(this.serverPw, lc.serverPw);
                this.NAGAuthId = firstNonBlank(this.NAGAuthId, lc.NAGAuthId);
                this.NAGAuthPw = firstNonBlank(this.NAGAuthPw, lc.NAGAuthPw);
                log.info("AppConfiguration loaded from {}", p.toAbsolutePath());
            } else {
                log.warn("AppConfig.json not found. Falling back to env/system properties.");
            }
        } catch (Exception e) {
            log.warn("Failed to load AppConfig.json (will use env/system properties). {}", e.toString());
        }

        // 최종적으로도 비어 있으면, 시스템 프로퍼티/환경변수에서 보완
        this.NAGAuthId = firstNonBlank(this.NAGAuthId,
                System.getProperty("nag.id"), System.getenv("NAG_ID"));
        this.NAGAuthPw = firstNonBlank(this.NAGAuthPw,
                System.getProperty("nag.pw"), System.getenv("NAG_PW"));

        this.dbId = firstNonBlank(this.dbId,
                System.getProperty("db.id"), System.getenv("DB_ID"));
        this.dbPw = firstNonBlank(this.dbPw,
                System.getProperty("db.pw"), System.getenv("DB_PW"));

        this.serverId = firstNonBlank(this.serverId,
                System.getProperty("server.id"), System.getenv("SERVER_ID"));
        this.serverPw = firstNonBlank(this.serverPw,
                System.getProperty("server.pw"), System.getenv("SERVER_PW"));
    }

    /** NAG 계정은 null-safe 게터로 사용 (Runner 등에서 바로 호출해도 NPE 없음) */
    public String getNAGAuthId() {
        return firstNonBlank(this.NAGAuthId,
                System.getProperty("nag.id"), System.getenv("NAG_ID"));
    }

    public String getNAGAuthPw() {
        return firstNonBlank(this.NAGAuthPw,
                System.getProperty("nag.pw"), System.getenv("NAG_PW"));
    }

    // ===== 유틸 =====
    private static String firstNonBlank(String... candidates) {
        if (candidates == null) return null;
        for (String c : candidates) {
            if (c != null && !c.isBlank()) return c;
        }
        return null;
    }

    /** AppConfig.json 매핑용 내부 DTO */
    @Getter
    @Setter
    private static class LocalConfig {
        private String dbId;
        private String dbPw;
        private String serverId;
        private String serverPw;
        private String NAGAuthId;
        private String NAGAuthPw;
    }
}

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

    // === 실행에 사용하는 설정값들 ===
    private String dbId;
    private String dbPw;
    private String serverId;
    private String serverPw;
    private String NAGAuthId;
    private String NAGAuthPw;
    private String nagIp;           // ✅ 추가
    private Integer nagPort;        // ✅ 추가
    private Integer reconnectIntervalSec; // 선택

    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        // 1) 실행 디렉터리 ./AppConfig.json → 2) classpath:/AppConfig.json 순으로 시도
        try {
            Path p = Paths.get("AppConfig.json");
            if (Files.notExists(p)) {
                URL res = getClass().getClassLoader().getResource("AppConfig.json");
                if (res != null) p = Paths.get(res.toURI());
            }
            if (Files.exists(p)) {
                var local = mapper.readValue(Files.readString(p), LocalConfig.class);
                this.dbId      = first(this.dbId, local.dbId);
                this.dbPw      = first(this.dbPw, local.dbPw);
                this.serverId  = first(this.serverId, local.serverId);
                this.serverPw  = first(this.serverPw, local.serverPw);
                this.NAGAuthId = first(this.NAGAuthId, local.NAGAuthId);
                this.NAGAuthPw = first(this.NAGAuthPw, local.NAGAuthPw);
                this.nagIp     = first(this.nagIp, local.nagIp);
                this.nagPort   = first(this.nagPort, local.nagPort);
                this.reconnectIntervalSec = first(this.reconnectIntervalSec, local.reconnectIntervalSec);
                log.info("AppConfiguration loaded from {}", p.toAbsolutePath());
            } else {
                log.warn("AppConfig.json not found. Using env/system properties if provided.");
            }
        } catch (Exception e) {
            log.warn("Failed to load AppConfig.json: {}", e.toString());
        }

        // 2) 환경변수/시스템프로퍼티 폴백
        this.NAGAuthId = first(this.NAGAuthId, sys("nag.id"), env("NAG_ID"));
        this.NAGAuthPw = first(this.NAGAuthPw, sys("nag.pw"), env("NAG_PW"));
        this.nagIp     = first(this.nagIp, sys("nag.ip"), env("NAG_IP"));
        this.nagPort   = first(this.nagPort, intOrNull(sys("nag.port")), intOrNull(env("NAG_PORT")));
        this.dbId      = first(this.dbId, sys("db.id"), env("DB_ID"));
        this.dbPw      = first(this.dbPw, sys("db.pw"), env("DB_PW"));
        this.reconnectIntervalSec = first(this.reconnectIntervalSec, intOrNull(sys("reconnect.sec")), intOrNull(env("RECONNECT_SEC")));
    }

    // === null-safe 게터 (Runner에서 바로 써도 NPE 없음) ===
    public String getNAGAuthId() { return first(this.NAGAuthId, sys("nag.id"), env("NAG_ID")); }
    public String getNAGAuthPw() { return first(this.NAGAuthPw, sys("nag.pw"), env("NAG_PW")); }
    public String getNagIp()     { return first(this.nagIp, sys("nag.ip"), env("NAG_IP")); }
    public Integer getNagPort()  { return first(this.nagPort, intOrNull(sys("nag.port")), intOrNull(env("NAG_PORT"))); }

    // ===== 유틸 =====
    private static String env(String k){ return System.getenv(k); }
    private static String sys(String k){ return System.getProperty(k); }

    private static <T> T first(T... values) {
        if (values == null) return null;
        for (T v : values) {
            if (v == null) continue;
            if (v instanceof String s) { if (!s.isBlank()) return v; }
            else return v;
        }
        return null;
    }
    private static Integer intOrNull(String s){
        try { return (s==null||s.isBlank()) ? null : Integer.parseInt(s.trim()); }
        catch (Exception ignore){ return null; }
    }

    /** AppConfig.json 매핑용 DTO */
    @Getter @Setter
    private static class LocalConfig {
        public String dbId;
        public String dbPw;
        public String serverId;
        public String serverPw;
        public String NAGAuthId;
        public String NAGAuthPw;
        public String nagIp;
        public Integer nagPort;
        public Integer reconnectIntervalSec;
    }
}

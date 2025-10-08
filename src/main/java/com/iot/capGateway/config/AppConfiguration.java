// src/main/java/com/iot/capGateway/config/AppConfiguration.java
package com.iot.capGateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.nio.file.*;

@Getter @Setter
@Component @Slf4j
public class AppConfiguration {
    private String dbId; private String dbPw;
    private String serverId; private String serverPw;
    private String NAGAuthId; private String NAGAuthPw;

    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void init() {
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
                log.info("AppConfiguration loaded from {}", p.toAbsolutePath());
            } else {
                log.warn("AppConfig.json not found. Using env/system properties if provided.");
            }
        } catch (Exception e) {
            log.warn("Failed to load AppConfig.json: {}", e.toString());
        }
        // Fallbacks
        this.NAGAuthId = first(this.NAGAuthId, System.getProperty("nag.id"), System.getenv("NAG_ID"));
        this.NAGAuthPw = first(this.NAGAuthPw, System.getProperty("nag.pw"), System.getenv("NAG_PW"));
        this.dbId      = first(this.dbId, System.getProperty("db.id"), System.getenv("DB_ID"));
        this.dbPw      = first(this.dbPw, System.getProperty("db.pw"), System.getenv("DB_PW"));
    }

    public String getNAGAuthId() { return first(this.NAGAuthId, System.getProperty("nag.id"), System.getenv("NAG_ID")); }
    public String getNAGAuthPw() { return first(this.NAGAuthPw, System.getProperty("nag.pw"), System.getenv("NAG_PW")); }

    private static String first(String... cs){ if(cs==null) return null; for(var c:cs) if(c!=null && !c.isBlank()) return c; return null; }

    @Getter @Setter
    private static class LocalConfig {
        private String dbId, dbPw, serverId, serverPw, NAGAuthId, NAGAuthPw;
    }
}

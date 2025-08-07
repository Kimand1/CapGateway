package com.iot.capGateway.config;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogProvider {
    private static final LogProvider instance = new LogProvider();

    public static LogProvider getInstance() {
        return instance;
    }

    public void fatal(String message) {
        log.error("[FATAL] {}", message);
    }

    public void fatal(String message, Exception e) {
        log.error("[FATAL] " + message, e);
    }
}

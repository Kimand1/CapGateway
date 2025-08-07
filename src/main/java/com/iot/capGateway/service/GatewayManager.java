package com.iot.capGateway.service;

import com.iot.capGateway.config.GlobalValues;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class GatewayManager {
        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
        private final SocketClient socketClient = new SocketClient(); // 직접 구현 필요
        private volatile boolean isAuthenticated = false;

        private final DbService dbService;
        private final GlobalValues globalValues;

        private String serverIp;
        private int serverPort;
        private String id;
        private String pass;

        public GatewayManager(DbService dbService, GlobalValues globalValues) {
            this.dbService = dbService;
            this.globalValues = globalValues;
        }

        public void initialize(String ip, int port, String id, String pass) {
            this.serverIp = ip;
            this.serverPort = port;
            this.id = id;
            this.pass = pass;

            startTimers();
            socketClient.setMessageListener(this::onMessageReceived);
        }

        public void run() {
            authenticateNag(null);
        }

        private void startTimers() {
            scheduler.scheduleAtFixedRate(this::onCollectResponse, 0, TimerIntervals.T5_AlertResponseTimeout, TimeUnit.MILLISECONDS);
            scheduler.scheduleAtFixedRate(this::onReconnectTimer, 0, TimerIntervals.T4_ReconnectionInterval, TimeUnit.MILLISECONDS);
            scheduler.scheduleAtFixedRate(this::onCheckSessionTimer, 0, TimerIntervals.T2_SessionCheckInterval, TimeUnit.MILLISECONDS);
        }
}

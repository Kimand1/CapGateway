package com.iot.capGateway.service;

import com.iot.capGateway.model.*;
import com.iot.capGateway.repository.DbService;
import com.iot.capGateway.config.GlobalValues;
import com.iot.capGateway.config.LogProvider;
import com.iot.capGateway.config.SocketClient;
import com.iot.capGateway.config.TimerIntervals;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.*;

@Component
public class GatewayManager {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    private final SocketClient socketClient = new SocketClient();
    private final DbService dbService;
    private final GlobalValues globalValues;

    private String serverIp;
    private int serverPort;
    private String id;
    private String pass;
    private volatile boolean isAuthenticated = false;

    public GatewayManager(DbService dbService, GlobalValues globalValues) {
        this.dbService = dbService;
        this.globalValues = globalValues;
    }

    public void initialize(String ip, int port, String id, String pass) {
        this.serverIp = ip;
        this.serverPort = port;
        this.id = id;
        this.pass = pass;

        socketClient.setMessageListener(this::onMessageReceived);
        runTimers();
    }

    private void runTimers() {
        scheduler.scheduleAtFixedRate(this::onCollectResponse, 0,
                TimerIntervals.T5_ALERT_RESPONSE_TIMEOUT, TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(this::onReconnectTimer, 0,
                TimerIntervals.T4_RECONNECTION_INTERVAL, TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(this::onCheckSessionTimer, 0,
                TimerIntervals.T2_SESSION_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
    }

    public void run() {
        authenticateNag(null);
    }

    private void connect() {
        try {
            if (socketClient.isConnected()) return;

            while (!socketClient.connect(serverIp, serverPort)) {
                LogProvider.error("NAG 연결 실패, 재시도 중...");
                Thread.sleep(TimerIntervals.T4_RECONNECTION_INTERVAL);
            }
            LogProvider.info("NAG 연결 성공");

        } catch (Exception e) {
            LogProvider.fatal("NAG 연결 예외 발생", e);
        }
    }

    private void authenticateNag(DigestMessage digestMessage) {
        try {
            connect();
            DigestMessage authData = (digestMessage != null) ? digestMessage : new DigestMessage(id);
            SocketMessage authRequest = new SocketMessage(MessageId.ETS_REQ_SYS_CON, DataFormat.XML, authData.toXml());
            socketClient.send(authRequest.toBytes());
            if (digestMessage == null) socketClient.receive();
        } catch (Exception e) {
            LogProvider.error("인증 실패", e);
        }
    }

    private void onReconnectTimer() {
        if (!isAuthenticated) {
            LogProvider.error("세션 끊김 감지, 재접속 시도");
            authenticateNag(null);
        }
    }

    private void onCheckSessionTimer() {
        if (isAuthenticated) {
            LogProvider.trace("세션 KeepAlive 체크 중");
            CheckSession();
        }
    }

    private void CheckSession() {
        connect();
        Session session = new Session(id, "alive");
        SocketMessage keepAlive = new SocketMessage(MessageId.ETS_REQ_SYS_STS, DataFormat.XML, session.toXml());
        socketClient.send(keepAlive.toBytes());
    }

    private void onCollectResponse() {
        try {
            List<ResponseData> responses = dbService.getPendingResponses();
            for (ResponseData response : responses) {
                CapData cap = CapData.fromXml(response.getCapMessage());
                cap.setResult("OK");
                cap.setResultCode("200");
                cap.getCapInfo().getAlert().setMsgType("Ack");
                cap.getCapInfo().getAlert().setNote("800");

                SocketMessage msg = new SocketMessage(MessageId.ETS_CNF_DIS_INFO, DataFormat.XML, cap.toXml());
                socketClient.send(msg.toBytes());

                dbService.updateResponseStatus(response.getMessageSeq());
                LogProvider.trace("응답 전송 완료: " + cap.getIdentifier());
            }
        } catch (Exception e) {
            LogProvider.error("응답 수집 중 오류", e);
        }
    }

    private void onMessageReceived(SocketMessage msg) {
        switch (msg.getMessageId()) {
            case ETS_RES_SYS_CON -> handleAuthResponse(msg);
            case ETS_RES_SYS_STS -> handleSessionStatus(msg);
            case ETS_NFY_DIS_INFO -> handleCapMessage(msg.getData());
            default -> LogProvider.warn("알 수 없는 메시지 수신됨: " + msg.getMessageId());
        }
    }

    private void handleAuthResponse(SocketMessage msg) {
        DigestMessage result = DigestMessage.fromXml(msg.getData());
        if (!isAuthenticate(result)) {
            DigestMessage retry = new DigestMessage(id, result.getRealm(), result.getNonce());
            retry.setResponseValue(pass);
            authenticateNag(retry);
        } else {
            LogProvider.trace("인증 성공: " + result.getResultCode());
            CheckSession();
        }
    }

    private void handleSessionStatus(SocketMessage msg) {
        Session session = Session.fromXml(msg.getData());
        isAuthenticated = "200".equals(session.getResultCode());
    }

    private void handleCapMessage(String data) {
        try {
            CapData cap = CapData.fromXml(data);
            if (!cap.isValid(globalValues.getLocationCode())) {
                cap.setResult("Bad Request");
                cap.setResultCode("400");
                cap.getCapInfo().getAlert().setMsgType("Error");
                cap.getCapInfo().getAlert().setNote("510");
                socketClient.send(new SocketMessage(MessageId.ETS_CNF_DIS_INFO, DataFormat.XML, cap.toXml()).toBytes());
                return;
            }

            dbService.saveCapData(cap);
            LogProvider.info("재난 메시지 저장 완료: " + cap.getIdentifier());
        } catch (Exception e) {
            LogProvider.error("CAP 메시지 처리 실패", e);
        }
    }

    private boolean isAuthenticate(DigestMessage msg) {
        boolean success = "200".equals(msg.getResultCode());
        this.isAuthenticated = success;
        return success;
    }
}

package com.iot.capGateway.service;

import com.iot.capGateway.config.GlobalValues;
import com.iot.capGateway.model.CapData;
import com.iot.capGateway.model.DigestMessage;
import com.iot.capGateway.util.*;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GatewayManager {

    private final SocketClient socketClient;   // ✅ 주입
    private final DbService dbService;         // ✅ 주입 (아래 5번에서 생성)
    private final GlobalValues globalValues;   // ✅ 주입 (이미 있음)

    private volatile boolean authenticated = false;
    private String nagId, nagPw;

    /** Runner에서 호출 */
    public void run(String nagIp, int nagPort, String nagId, String nagPw) {
        this.nagId = nagId; this.nagPw = nagPw;

        if (!socketClient.connect(nagIp, nagPort)) {
            log.error("NAG 연결 실패: {}:{}", nagIp, nagPort);
            return;
        }
        log.info("NAG 연결 성공: {}:{}", nagIp, nagPort);

        socketClient.onMessage(this::onMessage);
        socketClient.receive();

        sendAuthRequest();
    }

    private void sendAuthRequest() {
        try {
            DigestMessage dm = new DigestMessage();
            dm.setDestId(nagId);
            // nonce/realm 수신 후 response 계산 필요 시, handleAuthResponse에서 재송신
            String xml = XmlUtil.toXml(dm);
            socketClient.send(new SocketMessage(MessageId.ETS_REQ_SYS_CON, DataFormat.XML, xml));
            log.info("[TX] ETS_REQ_SYS_CON");
        } catch (Exception e) {
            log.error("인증 요청 송신 실패", e);
        }
    }

    private void onMessage(SocketMessage msg) {
        try {
            switch (msg.messageId()) {
                case ETS_RES_SYS_CON -> handleAuthResponse(msg.data());
                case ETS_NFY_DIS_INFO -> handleCap(msg.data());
                default -> log.debug("[RX] {} {}B", msg.messageId(), msg.data()==null?0:msg.data().length());
            }
        } catch (Exception e) {
            log.error("수신 처리 오류", e);
        }
    }

    private void handleAuthResponse(String xml) {
        try {
            var res = XmlUtil.fromXml(xml, DigestMessage.class);
            // 필요 시: res.getNonce(), res.getRealm()으로 response 계산 후 재요청
            // DigestMessage dm = new DigestMessage(); dm.setDestId(nagId); dm.setNonce(res.getNonce()); dm.setRealm(res.getRealm());
            // dm.setResponseValue(nagPw); socketClient.send(new SocketMessage(MessageId.ETS_REQ_SYS_CON, DataFormat.XML, XmlUtil.toXml(dm)));

            if ("200".equals(res.getResultCode()) || "OK".equalsIgnoreCase(res.getResult())) {
                authenticated = true;
                log.info("NAG 인증 성공");
            } else {
                log.warn("NAG 인증 실패: {} / {}", res.getResultCode(), res.getResult());
            }
        } catch (Exception e) {
            log.error("인증 응답 파싱 실패", e);
        }
    }

    private void handleCap(String xml) {
        log.info("[RX] ETS_NFY_DIS_INFO: {}B", xml==null?0:xml.length());
        CapData cap = XmlUtil.fromXml(xml, CapData.class);

        dbService.saveCapData(cap); // CAP 저장

        try {
            socketClient.send(new SocketMessage(MessageId.ETS_CNF_DIS_INFO, DataFormat.XML, XmlUtil.toXml(cap)));
            log.info("[TX] ETS_CNF_DIS_INFO");
        } catch (Exception e) {
            log.error("확인응답 송신 실패", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        socketClient.closeQuietly();
    }
}

package com.iot.capGateway.service;

import com.iot.capGateway.config.GlobalValues;
import com.iot.capGateway.model.CapData;
import com.iot.capGateway.model.DigestMessage;
import com.iot.capGateway.util.DataFormat;
import com.iot.capGateway.util.MessageId;
import com.iot.capGateway.util.SocketClient;
import com.iot.capGateway.util.SocketMessage;
import com.iot.capGateway.util.XmlUtil;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GatewayManager {

    private final SocketClient socketClient;   // ✅ 빈 주입
    private final DbService dbService;         // ✅ 빈 주입
    private final GlobalValues globalValues;   // 선택 사용 (locationCode/level 등)

    private String nagId;
    private String nagPw;
    private volatile boolean authenticated = false;

    public void run(String nagIp, int nagPort, String nagId, String nagPw) {
        this.nagId = nagId;
        this.nagPw = nagPw;

        if (!socketClient.connect(nagIp, nagPort)) {
            log.error("NAG 연결 실패: {}:{}", nagIp, nagPort);
            return;
        }
        log.info("NAG 연결 성공: {}:{}", nagIp, nagPort);

        // 수신 핸들러 등록 & 수신루프 시작
        socketClient.onMessage(this::onMessage);
        socketClient.receive();

        // 1차 인증 요청(빈 response) 전송 → 서버의 nonce/realm 수신 대기
        sendAuthRequest(false, null);
    }

    private void onMessage(SocketMessage msg) {
        try {
            switch (msg.messageId()) {
                case ETS_RES_SYS_CON -> handleAuthResponse(msg.data());
                case ETS_NFY_DIS_INFO -> handleCap(msg.data());
                case ETS_RES_SYS_STS   -> log.debug("[RX] SYS_STS: {}", trim(msg.data()));
                default -> log.debug("[RX] {} {}B", msg.messageId(), msg.data() == null ? 0 : msg.data().length());
            }
        } catch (Exception e) {
            log.error("수신 처리 오류", e);
        }
    }

    /** 인증 응답 처리 */
    private void handleAuthResponse(String xml) {
        DigestMessage res = DigestMessage.fromXml(xml);
        log.info("[RX] RES_SYS_CON rc={} result={}", res.getResultCode(), res.getResult());

        // 1) 서버가 nonce/realm을 내려준 초기 응답 → 실제 응답 해시 계산 후 재요청
        if ((res.getNonce() != null && !res.getNonce().isBlank())
                && (res.getRealm() != null && !res.getRealm().isBlank())
                && (res.getResultCode() == null || !"200".equals(res.getResultCode()))) {

            DigestMessage dm = new DigestMessage();
            dm.setDestId(nagId);
            dm.setNonce(res.getNonce());
            dm.setRealm(res.getRealm());
            dm.setResponseValue(nagPw);  // ✅ C#과 동일 수식으로 response 생성
            sendAuthRequest(true, dm);
            return;
        }

        // 2) 최종 성공 여부
        if ("200".equals(res.getResultCode()) || "OK".equalsIgnoreCase(res.getResult())) {
            authenticated = true;
            log.info("NAG 인증 성공");
        } else {
            log.warn("NAG 인증 실패: {} / {}", res.getResultCode(), res.getResult());
        }
    }

    /** CAP 수신 처리 & 확인응답 */
    private void handleCap(String xml) {
        log.info("[RX] ETS_NFY_DIS_INFO CAP XML {}B", xml == null ? 0 : xml.length());

        // 파싱은 identifier 추출 등 최소한으로만 사용
        CapData cap = CapData.fromXml(xml);
        String identifier = cap.getAlert() != null ? cap.getAlert().getIdentifier() : null;

        // ✅ DB에는 '원문 xml'을 저장
        dbService.saveCapDataRaw(identifier, xml);

        // ✅ 확인응답은 '원문 xml'을 그대로 에코(계약이 원문 에코라면)
        try {
            socketClient.send(new SocketMessage(
                    MessageId.ETS_CNF_DIS_INFO, DataFormat.XML, xml   // ← 여기!
            ));
            log.info("[TX] ETS_CNF_DIS_INFO (원문 에코)");
        } catch (Exception e) {
            log.error("확인응답 송신 실패", e);
        }
    }

    /** 인증 요청 송신 (초기/재요청 공용) */
    private void sendAuthRequest(boolean withResponse, DigestMessage dm) {
        try {
            DigestMessage payload;
            if (withResponse && dm != null) {
                payload = dm;
                if (payload.getDestId() == null) payload.setDestId(nagId);
            } else {
                payload = new DigestMessage();
                payload.setDestId(nagId);
                // response/nonce/realm 없이 최초 요청
            }
            String xml = payload.toXml();
            socketClient.send(new SocketMessage(MessageId.ETS_REQ_SYS_CON, DataFormat.XML, xml));
            log.info("[TX] ETS_REQ_SYS_CON {}", withResponse ? "(with response)" : "(initial)");
        } catch (Exception e) {
            log.error("인증 요청 송신 실패", e);
        }
    }

    private static String trim(String s) {
        if (s == null) return null;
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }

    @PreDestroy
    public void shutdown() {
        socketClient.closeQuietly();
        log.info("GatewayManager 종료");
    }
}

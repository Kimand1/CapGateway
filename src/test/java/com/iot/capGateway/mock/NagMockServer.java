package com.iot.capGateway.mock;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.Executors;

/**
 * 테스트용 NAG 서버 (프로토콜 모의)
 * - BIG_ENDIAN 16B 헤더: [msgId][fmt][MAGIC][len] + body(UTF-8)
 * - MessageId: C# 원본과 동일
 * - 인증 수식: a=MD5(DestId:Realm:Password), response=MD5(a:Nonce)
 */
public class NagMockServer {

    // ===== 프로토콜 상수 =====
    private static final int MAGIC = 0xF020190F;
    private static final int DF_XML = 1;
    private static final int ETS_REQ_SYS_CON = 0xFFEE1001;
    private static final int ETS_RES_SYS_CON = 0xFFEE2001;
    private static final int ETS_REQ_SYS_STS = 0xFFEE1010;
    private static final int ETS_RES_SYS_STS = 0xFFEE2010;
    private static final int ETS_NFY_DIS_INFO = 0xFFEE3020;
    private static final int ETS_CNF_DIS_INFO = 0xFFEE4020;

    // ===== 설정 =====
    private final int port;
    private final String realm;        // 예: "NAG-REALM"
    private final String nonce;        // 예: "ABCDEF0123456789"
    private final String userPassMapId; // 게이트웨이 DestId (NAGAuthId)
    private final String userPass;     // 해당 ID의 패스워드 (NAGAuthPw)

    public NagMockServer(int port, String realm, String nonce, String userPassMapId, String userPass) {
        this.port = port;
        this.realm = realm;
        this.nonce = nonce;
        this.userPassMapId = userPassMapId;
        this.userPass = userPass;
    }

    public void start() throws IOException {
        var boss = new ServerSocket(port);
        System.out.println("[NAG-MOCK] Listening on " + port);

        var pool = Executors.newCachedThreadPool();
        while (true) {
            Socket s = boss.accept();
            pool.submit(() -> handleClient(s));
        }
    }

    private void handleClient(Socket sock) {
        String remote = sock.getRemoteSocketAddress().toString();
        System.out.println("[NAG-MOCK] Client connected: " + remote);
        try (sock;
             var in = new DataInputStream(sock.getInputStream());
             var out = new DataOutputStream(sock.getOutputStream())) {

            boolean authenticated = false;

            outer: while (true) {
                byte[] header = readExact(in, 16);
                if (header == null) {
                    System.out.println("[NAG-MOCK] " + remote + " closed");
                    break;
                }
                var hb = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN);
                int mid = hb.getInt();
                int fmt = hb.getInt();
                int magic = hb.getInt();
                int len = hb.getInt();
                if (magic != MAGIC) throw new IOException("Bad MAGIC");
                byte[] body = len > 0 ? readExact(in, len) : new byte[0];
                String xml = new String(body, StandardCharsets.UTF_8);

                switch (mid) {
                    case ETS_REQ_SYS_CON -> {
                        // 최초: destId만 옴 → nonce/realm 내려줌
                        // 재요청: response 포함 → 검증 후 200
                        if (!authenticated) {
                            // destId 파싱
                            String destId = parseTag(xml, "destId");
                            String response = parseTag(xml, "response"); // 있을 수도, 없을 수도
                            if (response == null || response.isBlank()) {
                                // 1차 응답: nonce/realm 제공
                                String resXml = "<data>"
                                        + "<destId>" + safe(destId) + "</destId>"
                                        + "<resultCode>401</resultCode><result>UNA</result>"
                                        + "<nonce>" + nonce + "</nonce>"
                                        + "<realm>" + realm + "</realm>"
                                        + "</data>";
                                send(out, ETS_RES_SYS_CON, DF_XML, resXml);
                                System.out.println("[NAG-MOCK] >> RES_SYS_CON (nonce/realm)");
                            } else {
                                // 2차: response 검증
                                boolean ok = verifyResponse(destId, response);
                                String resXml = ok
                                        ? "<data><destId>" + safe(destId) + "</destId><resultCode>200</resultCode><result>OK</result></data>"
                                        : "<data><destId>" + safe(destId) + "</destId><resultCode>410</resultCode><result>FAIL</result></data>";
                                send(out, ETS_RES_SYS_CON, DF_XML, resXml);
                                System.out.println("[NAG-MOCK] >> RES_SYS_CON " + (ok ? "200" : "410"));
                                authenticated = ok;

                                // 인증 성공 직후 테스트 CAP 한 건 푸시(옵션)
                                if (ok) {
                                    String cap = sampleCap();
                                    send(out, ETS_NFY_DIS_INFO, DF_XML, cap);
                                    System.out.println("[NAG-MOCK] >> NFY_DIS_INFO (sample CAP)");
                                }
                            }
                        } else {
                            // 이미 인증됨 → OK
                            String resXml = "<data><resultCode>200</resultCode><result>OK</result></data>";
                            send(out, ETS_RES_SYS_CON, DF_XML, resXml);
                        }
                    }
                    case ETS_REQ_SYS_STS -> {
                        String resXml = "<data><resultCode>200</resultCode><result>RUNNING</result></data>";
                        send(out, ETS_RES_SYS_STS, DF_XML, resXml);
                        System.out.println("[NAG-MOCK] >> RES_SYS_STS");
                    }
                    case ETS_CNF_DIS_INFO -> {
                        System.out.println("[NAG-MOCK] << CNF_DIS_INFO ack received " + body.length + "B");
                    }
                    default -> {
                        System.out.println("[NAG-MOCK] << Unknown MID=0x" + Integer.toHexString(mid) + ", len=" + len);
                        // 필요시 break outer;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[NAG-MOCK] Client error: " + e);
        }
    }

    // Digest 검증: a=MD5(destId:realm:password), response=MD5(a:nonce)
    private boolean verifyResponse(String destId, String response) {
        if (!userPassMapId.equals(destId)) return false;
        String a = md5Hex(destId + ":" + realm + ":" + userPass);
        String expect = md5Hex(a + ":" + nonce);
        return expect.equalsIgnoreCase(response);
    }

    // CAP 샘플 (네가 준 1.2 예시를 약간 줄인 버전)
    private static String sampleCap() {
        return """
        <alert xmlns="urn:oasis:names:tc:emergency:cap:1.2">
          <identifier>KR.SAFEPORTAL-TEST-0001</identifier>
          <sender>mock.nag.local</sender>
          <sent>2025-10-08T12:00:00+09:00</sent>
          <status>Actual</status><msgType>Alert</msgType><scope>Public</scope>
          <info>
            <language>ko-KR</language>
            <category>Met</category>
            <event>Mock Heavy Rain Warning</event>
            <urgency>Immediate</urgency><severity>Severe</severity><certainty>Observed</certainty>
            <headline>모의 호우 경보</headline>
            <description>이것은 테스트용 CAP 메시지입니다.</description>
            <area><areaDesc>테스트 지역</areaDesc></area>
          </info>
        </alert>
        """.trim();
    }

    // ===== 전송/수신 유틸 =====
    private static void send(DataOutputStream out, int msgId, int fmt, String data) throws IOException {
        byte[] body = data == null ? new byte[0] : data.getBytes(StandardCharsets.UTF_8);
        ByteBuffer b = ByteBuffer.allocate(16 + body.length).order(ByteOrder.BIG_ENDIAN);
        b.putInt(msgId).putInt(fmt).putInt(MAGIC).putInt(body.length).put(body);
        out.write(b.array());
        out.flush();
    }

    private static byte[] readExact(DataInputStream in, int n) throws IOException {
        byte[] buf = new byte[n];
        int off = 0;
        while (off < n) {
            int r = in.read(buf, off, n - off);
            if (r == -1) return null;
            off += r;
        }
        return buf;
    }

    private static String parseTag(String xml, String tag) {
        // 아주 단순한 파서 (테스트용). 실제는 JAXB/DOM 권장
        String open = "<" + tag + ">";
        String close = "</" + tag + ">";
        int i = xml.indexOf(open);
        int j = xml.indexOf(close);
        if (i < 0 || j < 0 || j <= i) return null;
        return xml.substring(i + open.length(), j).trim();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String md5Hex(String s) {
        try {
            var md = MessageDigest.getInstance("MD5");
            byte[] h = md.digest(s.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().withLowerCase().formatHex(h);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    // ===== 단독 실행 =====
    public static void main(String[] args) throws Exception {
        int port = 25001;
        String realm = "NAG-REALM";
        String nonce = "ABCDEF0123456789";
        String destId = System.getProperty("nag.id", "NAG_GATEWAY_001");
        String pass   = System.getProperty("nag.pw", "NAG_SECRET_1234");

        new NagMockServer(port, realm, nonce, destId, pass).start();
    }
}

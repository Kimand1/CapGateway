NAG 서버를 목업서버로 테스트용으로 기동하는법

1. 상단 메뉴 Run → Edit Configurations… 열기
2. 좌측 상단의 + 버튼 → Application 선택
3. Name: NagMockServer
4. Main class: com.iot.capGateway.mock.NagMockServer

이후  NagMockServer 를 기동하면 127.0.0.1 에 25001 포트로 기동함

기본정보
{
"nagIp": "127.0.0.1",
"nagPort": 25001,
"NAGAuthId": "NAG_GATEWAY_001",
"NAGAuthPw": "NAG_SECRET_1234"
}

콘솔에 다음이 찍히면 정상통신
[Runner] CapGatewayRunner started.
[SocketClient] Connected to 127.0.0.1:25001
[GatewayManager] -> ETS_REQ_SYS_CON (1차)
[GatewayManager] <- RES_SYS_CON (nonce/realm)
[GatewayManager] -> ETS_REQ_SYS_CON (2차)
[GatewayManager] <- RES_SYS_CON 200 OK
[GatewayManager] <- NFY_DIS_INFO (CAP 수신)
[GatewayManager] -> CNF_DIS_INFO (확인응답)
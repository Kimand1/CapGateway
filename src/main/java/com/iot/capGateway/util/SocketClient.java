package com.iot.capGateway.util;

import org.springframework.stereotype.Component;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class SocketClient {

    private static final int MAGIC = 0xF020190F;
    private static final int MAX_BODY = 4 * 1024 * 1024;
    private static final int READ_TIMEOUT_MS = 30_000;

    private final ExecutorService reader = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "socket-reader");
        //t.setDaemon(true);
        return t;
    });
    private final ScheduledExecutorService reconnector = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "socket-reconnector");
        return t;
    });

    private volatile String lastIp;
    private volatile int lastPort;

    private volatile boolean running = false;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private Consumer<SocketMessage> listener;

    public void onMessage(Consumer<SocketMessage> l) { this.listener = l; }

    public synchronized boolean connect(String ip, int port) {
        closeQuietly();
        try {
            this.socket = new Socket(ip, port);
            this.socket.setTcpNoDelay(true);
            this.socket.setSoTimeout(READ_TIMEOUT_MS);
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.lastIp = ip;
            this.lastPort = port;
            this.running = true;
            return true;
        } catch (IOException e) {
            closeQuietly();
            return false;
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public synchronized void send(SocketMessage msg) throws IOException {
        Objects.requireNonNull(out, "Not connected");
        byte[] buf = msg.toBytes();
        out.write(buf);
        out.flush();
    }

    public void receive() {
        reader.submit(() -> {
            try {
                while (running && isConnected()) {
                    // 1) header(16B)
                    byte[] header = readExact(in, 16);
                    if (header == null) break;

                    ByteBuffer hb = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN);
                    int messageId = hb.getInt();
                    int dataFormat = hb.getInt();
                    int magic = hb.getInt();
                    int len = hb.getInt();

                    if (magic != MAGIC) throw new IOException("Bad MAGIC: 0x" + Integer.toHexString(magic));
                    if (len < 0 || len > MAX_BODY) throw new IOException("Invalid body length: " + len);

                    // 2) body(len)
                    byte[] body = (len == 0) ? new byte[0] : readExact(in, len);
                    if (body == null) throw new EOFException("EOF while reading body");

                    // 3) 메시지 조립 (✅ fromHeaderAndBody 사용)
                    SocketMessage sm = SocketMessage.fromHeaderAndBody(header, body);
                    if (listener != null) listener.accept(sm);
                }
            } catch (SocketTimeoutException ignore) {
                // 필요 시 keep-alive 설계 (여기선 무시)
            } catch (IOException ignore) {
                // 연결 종료/오류 → finally에서 정리
            } finally {
                closeQuietly();
                // 마지막 주소가 있으면 재접속 예약
                if (lastIp != null && lastPort > 0) {
                    reconnector.schedule(() -> {
                        if (connect(lastIp, lastPort)) {
                            // 성공 시 수신 재개
                            if (listener != null) receive();
                        }
                    }, 15, TimeUnit.SECONDS); // 15초 후 재시도 (원하면 설정값으로)
                }
            }
        });
    }

    private static byte[] readExact(DataInputStream in, int len) throws IOException {
        byte[] buf = new byte[len];
        int off = 0;
        while (off < len) {
            int r;
            try {
                r = in.read(buf, off, len - off);
            } catch (SocketTimeoutException e) {
                continue; // 타임아웃은 재시도
            }
            if (r == -1) return null;
            off += r;
        }
        return buf;
    }

    public synchronized void closeQuietly() {
        running = false;
        try { if (in != null) in.close(); } catch (Exception ignore) {}
        try { if (out != null) out.close(); } catch (Exception ignore) {}
        try { if (socket != null) socket.close(); } catch (Exception ignore) {}
        in = null; out = null; socket = null;
    }
}

package com.iot.capGateway.util;

import org.springframework.stereotype.Component;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Component
public class SocketClient {
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private Consumer<SocketMessage> listener;
    private final ExecutorService reader = Executors.newSingleThreadExecutor();

    public boolean connect(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            out = new DataOutputStream(socket.getOutputStream());
            in  = new DataInputStream(socket.getInputStream());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void send(SocketMessage msg) throws IOException {
        var buf = msg.toBytes();
        out.write(buf); out.flush();
    }

    /** 16바이트 헤더 기반 지속 수신 */
    public void receive() {
        reader.submit(() -> {
            try {
                while (isConnected()) {
                    byte[] header = in.readNBytes(16);
                    if (header.length < 16) break;
                    var hb = ByteBuffer.wrap(header);
                    int mid=hb.getInt(), fmt=hb.getInt(), magic=hb.getInt(), len=hb.getInt();
                    byte[] body = in.readNBytes(len);
                    byte[] frame = ByteBuffer.allocate(16+len).putInt(mid).putInt(fmt).putInt(magic).putInt(len).put(body).array();
                    if (listener != null) listener.accept(SocketMessage.fromBytes(frame));
                }
            } catch (IOException ignore) { /* 연결 끊김 */ }
        });
    }

    public void onMessage(Consumer<SocketMessage> l){ this.listener = l; }

    public void closeQuietly() {
        try { if (socket != null) socket.close(); } catch(Exception ignore) {}
        reader.shutdownNow();
    }
}

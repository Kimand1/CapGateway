package com.iot.capGateway.util;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class SocketClient {
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private Consumer<SocketMessage> messageListener;

    public boolean connect(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void send(byte[] message) throws IOException {
        out.write(message);
        out.flush();
    }

    public void receive() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[4096];
                int length = in.read(buffer);
                if (length > 0 && messageListener != null) {
                    String msgStr = new String(buffer, 0, length);
                    SocketMessage msg = SocketMessage.fromXml(msgStr);
                    messageListener.accept(msg);
                }
            } catch (IOException e) {
                LogProvider.error("수신 오류", e);
            }
        }).start();
    }

    public void setMessageListener(Consumer<SocketMessage> listener) {
        this.messageListener = listener;
    }
}

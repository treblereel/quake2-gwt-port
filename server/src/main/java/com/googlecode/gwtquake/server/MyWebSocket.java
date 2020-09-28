package com.googlecode.gwtquake.server;

import java.io.IOException;
import java.util.LinkedList;

import javax.websocket.Session;

import static com.googlecode.gwtquake.server.ServerWebSocketImpl.*;

/**
 * @author Dmitrii Tikhomirov
 * Created by treblereel 5/25/20
 */
public class MyWebSocket {
    private LinkedList<String> outQueue = new LinkedList<>();
    private Session outbound;
    private LinkedList<Msg> msgQueue;
    byte[] fromIp;
    int fromPort;

    public MyWebSocket(byte[] fromIp, int fromPort, LinkedList<Msg> msgQueue) {
        this.fromIp = fromIp;
        this.fromPort = fromPort;
        this.msgQueue = msgQueue;
    }

    public void onConnect(Session outbound) {
        this.outbound = outbound;

        if (!outQueue.isEmpty()) {
            for (String msg : outQueue) {
                sendMessage(msg);
            }
            outQueue.clear();
        }
    }

    public void onDisconnect() {
    }

    // If you know this: Please add a comment about the required Jetty version at the top
    // @Override
    public void onFragment(boolean arg0, byte arg1, byte[] arg2, int arg3,
                           int arg4) {
        assert false : "Why is this method separate from the other onMessage()?";
    }

    public void onMessage(byte frame, String data) {
        synchronized (msgQueue) {
            msgQueue.add(new Msg(fromIp, fromPort, data));
        }
    }

    public void onMessage(byte frame, byte[] data, int offset, int length) {
        assert false : "Why is this method separate from the other onMessage()?";
    }

    public void sendMessage(String msg) {
        if (outbound == null) {
            outQueue.add(msg);
            return;
        }

        try {
            outbound.getBasicRemote().sendText(msg);
        } catch (IOException e) {
            //System.out.println("sendMessage failed (" + outbound.getId() + "): " + e.getMessage());
            outQueue.add(msg);
            //outbound = null;
        }
    }
}

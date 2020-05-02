package com.googlecode.gwtquake.server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.googlecode.gwtquake.shared.common.Compatibility;
import com.googlecode.gwtquake.shared.common.NetworkAddress;
import com.googlecode.gwtquake.shared.sys.QuakeSocket;
import com.googlecode.gwtquake.shared.sys.QuakeSocketFactory;
import com.googlecode.gwtquake.shared.util.Lib;

/**
 * @author Dmitrii Tikhomirov
 * Created by treblereel 5/24/20
 */
public class ServerWebSocketFactoryImpl implements QuakeSocketFactory {

    public QuakeSocket bind(String ip, int port) {
        return new ServerWebSocketImpl(ip, port);
    }
}

class ServerWebSocketImpl implements QuakeSocket {

    public Map<String, MyWebSocket> sockets = new HashMap<>();

    public LinkedList<Msg> msgQueue = new LinkedList<>();

    public ServerWebSocketImpl(String ip, int port) {
    }

    public int receive(NetworkAddress fromAdr, byte[] buf) throws IOException {
        synchronized (msgQueue) {
            if (msgQueue.isEmpty()) {
                return -1;
            }

            Msg msg = msgQueue.removeFirst();
            String data = msg.data;

            fromAdr.ip = new byte[4];
            System.arraycopy(msg.fromIp, 0, fromAdr.ip, 0, 4);
            fromAdr.port = msg.fromPort;

            int len = Compatibility.stringToBytes(data, buf);

            //System.out.println("receiving " + Lib.hexDump(buf, len, true));

            return len;
        }
    }

    public void send(NetworkAddress dstSocket, byte[] data, int len) throws IOException {
        String targetAddress = InetAddress.getByAddress(dstSocket.ip).getHostAddress()
                + ":" + dstSocket.port;

        MyWebSocket target = sockets.get(targetAddress);

        if (target == null) {
            System.out.println("Trying to send message to " + dstSocket.toString()
                                       + "; address not found. Available addresses: " + sockets.keySet());
            return;
        }
        target.sendMessage(Compatibility.bytesToString(data, len));
    }

    public void close() {
        sockets = null;
    }

    public void Shutdown() {
        try {
            //server.stop();
        } catch (Exception e) {
        }
    }

    static class Msg {

        public byte[] fromIp;
        public int fromPort;
        public String data;

        public Msg(byte[] fromIp, int fromPort, String data) {
            this.fromIp = fromIp;
            this.fromPort = fromPort;
            this.data = data;
        }
    }
}

package com.googlecode.gwtquake.server;

import java.io.IOException;
import java.net.InetAddress;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.sys.NET;
import com.googlecode.gwtquake.shared.util.IpAddrGenerator;

/**
 * @author Dmitrii Tikhomirov
 * Created by treblereel 5/24/20
 */
@ApplicationScoped
@ServerEndpoint("/quake2/net")
public class Quake2Socket {

    @Inject
    GwtQuakeServer server;

    @OnMessage
    public void onMessage(Session session, String msg) {
        server.getHandler().get(session).onMessage((byte) 1, msg);
    }

    @OnOpen
    public void onConnect(Session session) throws IOException {
        if (!server.getHandler().containsKey(session)) {
            String fakeIp = IpAddrGenerator.get(100, 200);
            MyWebSocket socket = new MyWebSocket(InetAddress.getByName(fakeIp).getAddress(), 27010, ((ServerWebSocketImpl) NET.ip_sockets[Constants.NS_SERVER]).msgQueue);
            socket.onConnect(session);
            server.getHandler().put(session, socket);

            ((ServerWebSocketImpl) NET.ip_sockets[Constants.NS_SERVER]).sockets.put(fakeIp + ":" + 27010, socket);

            System.out.println(session.getId() + " connected!");
        } else {
            System.out.println(session.getId() + " reconnected!");
        }
    }

    @OnClose
    public void onClose(Session session) {
        server.getHandler().get(session).onDisconnect();
        server.getHandler().remove(session);
        System.out.println(session.getId() + " closed!");
    }
}

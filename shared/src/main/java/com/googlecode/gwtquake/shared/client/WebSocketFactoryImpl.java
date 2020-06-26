/*
Copyright (C) 2010 Copyright 2010 Google Inc.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

*/
package com.googlecode.gwtquake.shared.client;

import java.io.IOException;
import java.util.LinkedList;

import com.googlecode.gwtquake.shared.common.Compatibility;
import com.googlecode.gwtquake.shared.common.NetworkAddress;
import com.googlecode.gwtquake.shared.sys.QuakeSocket;
import com.googlecode.gwtquake.shared.sys.QuakeSocketFactory;
import com.googlecode.gwtquake.shared.util.Lib;
import elemental2.dom.WebSocket;

public class WebSocketFactoryImpl implements QuakeSocketFactory {

    public QuakeSocket bind(String addr, int port) {
        return new GwtWebSocketImpl(addr);
    }
}

class GwtWebSocketImpl implements QuakeSocket {

    private String addr = "ws://127.0.0.1:8080/quake2/net";

    private WebSocket socket;
    private boolean connected;
    private LinkedList<String> msgQueue = new LinkedList<>();

    public GwtWebSocketImpl(String addr) {
        this.addr = addr;
        System.out.println("Creating GwtWebSocketImpl(" + addr + ")");
    }

    public int receive(NetworkAddress address, byte[] buf) throws IOException {
        if (msgQueue.isEmpty()) {
            return -1;
        }

        String s = msgQueue.remove();
        int len = Compatibility.stringToBytes(s, buf);
        //System.out.println("receiving: " + Lib.hexDump(buf, len, false));
        return len;
    }

    public void send(NetworkAddress adr, byte[] buf, int len) throws IOException {
        // TODO(haustein): check if addess still matches?

        if (socket == null) {
            System.out.println("connect for send to: " + addr);

            socket = new WebSocket(addr);

            socket.onopen = event -> connected = true;

            socket.onmessage = event -> {
                String data = event.data.toString();
                msgQueue.add(data);
            };
            socket.onclose = event -> connected = false;
        }

        //System.out.println("sending: " + connected+ " " + Lib.hexDump(buf, len, false));
        if (connected) {
            socket.send(Compatibility.bytesToString(buf, len));
        }
    }

    public void close() throws IOException {
        System.out.println("closing");
        if (socket != null) {
            socket.onopen = null;
            socket.onclose = null;
            socket.onmessage = null;
        }
        if (connected) {
            socket.close();
            connected = false;
        }
        socket = null;
    }
}

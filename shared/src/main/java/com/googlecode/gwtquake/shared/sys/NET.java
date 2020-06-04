/*
 * Copyright (C) 1997-2001 Id Software, Inc.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 *
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */
/* Modifications
   Copyright 2003-2004 Bytonic Software
   Copyright 2010 Google Inc.
*/
package com.googlecode.gwtquake.shared.sys;


import java.io.IOException;
import java.net.InetAddress;

import com.googlecode.gwtquake.shared.common.Buffer;
import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.Compatibility;
import com.googlecode.gwtquake.shared.common.ConsoleVariables;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.Globals;
import com.googlecode.gwtquake.shared.common.NetworkAddress;
import com.googlecode.gwtquake.shared.game.ConsoleVariable;
import com.googlecode.gwtquake.shared.util.Lib;
import elemental2.dom.DomGlobal;

public final class NET {

    public static QuakeSocketFactory socketFactory;

    private final static int MAX_LOOPBACK = 4;

    /** Local loopback adress. */
    private static NetworkAddress net_local_adr = new NetworkAddress();

    public static class loopmsg_t {
        byte data[] = new byte[Constants.MAX_MSGLEN];

        int datalen;
    };

    public static class loopback_t {
        public loopback_t() {
            msgs = new loopmsg_t[MAX_LOOPBACK];
            for (int n = 0; n < MAX_LOOPBACK; n++) {
                msgs[n] = new loopmsg_t();
            }
        }

        loopmsg_t msgs[];

        int get, send;
    };

    public static loopback_t loopbacks[] = new loopback_t[2];
    static {
        loopbacks[0] = new loopback_t();
        loopbacks[1] = new loopback_t();
    }


    public static QuakeSocket[] ip_sockets = { null, null };

    /**
     * Compares ip address and port.
     */
    public static boolean CompareAdr(NetworkAddress a, NetworkAddress b) {
        return (a.ip[0] == b.ip[0] && a.ip[1] == b.ip[1] && a.ip[2] == b.ip[2]
                && a.ip[3] == b.ip[3] && a.port == b.port);
    }

    /**
     * Compares ip address without the port.
     */
    public static boolean CompareBaseAdr(NetworkAddress a, NetworkAddress b) {
        if (a.type != b.type)
            return false;

        if (a.type == Constants.NA_LOOPBACK)
            return true;

        if (a.type == Constants.NA_IP) {
            return (a.ip[0] == b.ip[0] && a.ip[1] == b.ip[1]
                    && a.ip[2] == b.ip[2] && a.ip[3] == b.ip[3]);
        }
        return false;
    }

    /**
     * Returns a string holding ip address and port like "ip0.ip1.ip2.ip3:port".
     */
    public static String AdrToString(NetworkAddress a) {
        StringBuffer sb = new StringBuffer();
        sb.append(a.ip[0] & 0xFF).append('.').append(a.ip[1] & 0xFF);
        sb.append('.');
        sb.append(a.ip[2] & 0xFF).append('.').append(a.ip[3] & 0xFF);
        sb.append(':').append(a.port);
        return sb.toString();
    }

    /**
     * Returns IP address without the port as string.
     */
    public static String BaseAdrToString(NetworkAddress a) {
        StringBuffer sb = new StringBuffer();
        sb.append(a.ip[0] & 0xFF).append('.').append(a.ip[1] & 0xFF);
        sb.append('.');
        sb.append(a.ip[2] & 0xFF).append('.').append(a.ip[3] & 0xFF);
        return sb.toString();
    }

    public static boolean isRunning = true;

    /**
     * Creates an netadr_t from an string.
     */
    public static boolean StringToAdr(String s, NetworkAddress a) {
        if (s.equalsIgnoreCase("localhost") || s.equalsIgnoreCase("loopback")) {
            a.set(net_local_adr);
            return true;
        }
        try {
            String[] address = s.split(":");
            InetAddress ia = InetAddress.getByName(address[0]);
            a.ip = ia.getAddress();
            a.type = Constants.NA_IP;
            if (address.length == 2)
                a.port = Lib.atoi(address[1]);
            return true;
        } catch (Exception e) {
            Com.Println(e.getMessage());
            return false;
        }
    }

    /**
     * Seems to return true, if the address is is on 127.0.0.1.
     */
    public static boolean IsLocalAddress(NetworkAddress adr) {
        return CompareAdr(adr, net_local_adr);
    }

    /*
     * ==================================================
     *
     * LOOPBACK BUFFERS FOR LOCAL PLAYER
     *
     * ==================================================
     */

    /**
     * Gets a packet from internal loopback.
     */
    public static boolean GetLoopPacket(int sock, NetworkAddress net_from,
                                        Buffer net_message) {
        loopback_t loop;
        loop = loopbacks[sock];

        if (loop.send - loop.get > MAX_LOOPBACK)
            loop.get = loop.send - MAX_LOOPBACK;

        if (loop.get >= loop.send)
            return false;

        int i = loop.get & (MAX_LOOPBACK - 1);
        loop.get++;

        System.arraycopy(loop.msgs[i].data, 0, net_message.data, 0,
                         loop.msgs[i].datalen);
        net_message.cursize = loop.msgs[i].datalen;

        net_from.set(net_local_adr);
        return true;
    }

    /**
     * Sends a packet via internal loopback.
     */
    public static void SendLoopPacket(int sock, int length, byte[] data,
                                      NetworkAddress to) {
        int i;
        loopback_t loop;

        loop = loopbacks[sock ^ 1];

        // modulo 4
        i = loop.send & (MAX_LOOPBACK - 1);
        loop.send++;

        System.arraycopy(data, 0, loop.msgs[i].data, 0, length);
        loop.msgs[i].datalen = length;
    }

    /**
     * Gets a packet from a network channel
     */
    public static boolean GetPacket(int sock, NetworkAddress net_from,
                                    Buffer net_message) {

        //System.out.println("GetPacket " + sock + " from " + net_from + " running ? " + isRunning);

        if (GetLoopPacket(sock, net_from, net_message)) {
            return true && isRunning;
        }

        if (ip_sockets[sock] == null)
            return false;

        try {
//            ByteBuffer receiveBuffer = ByteBuffer.wrap(net_message.data);

            int packetLength = ip_sockets[sock].receive(net_from, net_message.data);
            if (packetLength == -1)
                return false;

//            net_from.ip = srcSocket.getAddress().getAddress();
//            net_from.port = srcSocket.getPort();
            net_from.type = Constants.NA_IP;

//            int packetLength = receiveBuffer.position();

            if (packetLength > net_message.maxsize) {
                Com.Println("Oversize packet from " + AdrToString(net_from));
                return false;
            }

            // set the size
            net_message.cursize = packetLength;
            // set the sentinel
            net_message.data[packetLength] = 0;
            return true;

        } catch (IOException e) {
            Com.DPrintf("NET_GetPacket: " + e + " from "
                                + AdrToString(net_from) + "\n");
            return false;
        }
    }

    /**
     * Sends a Packet.
     */
    public static void SendPacket(int sock, int length, byte[] data, NetworkAddress to) {

        if (to.type == Constants.NA_LOOPBACK) {
            SendLoopPacket(sock, length, data, to);
            return;
        }

        if (ip_sockets[sock] == null) {
            return;
        }

        if (to.type != Constants.NA_BROADCAST && to.type != Constants.NA_IP) {
            Com.Error(Constants.ERR_FATAL, "NET_SendPacket: bad address type");
            return;
        }

        try {
            ip_sockets[sock].send(to, data, length);
        } catch (Exception e) {
            Com.Println("NET_SendPacket ERROR: " + e + " to " + AdrToString(to));
        }
    }

    /**
     * OpenIP, creates the network sockets.
     */
    private static void OpenIP() {
        System.out.println("OpenIP " + Constants.NS_SERVER);

        ConsoleVariable port, ip, clientport;

        port = ConsoleVariables.Get("port", "" + Constants.PORT_SERVER, Constants.CVAR_NOSET);
        ip = ConsoleVariables.Get("ip", "localhost", Constants.CVAR_NOSET);
        clientport = ConsoleVariables.Get("clientport", "" + Constants.PORT_CLIENT, Constants.CVAR_NOSET);

        if (ip_sockets[Constants.NS_SERVER] == null)
            ip_sockets[Constants.NS_SERVER] = Socket(Constants.NS_SERVER,
                                                     ip.string, (int) port.value);

        if (ip_sockets[Constants.NS_CLIENT] == null)
            ip_sockets[Constants.NS_CLIENT] = Socket(Constants.NS_CLIENT,
                                                     ip.string, (int) clientport.value);
        if (ip_sockets[Constants.NS_CLIENT] == null)
            ip_sockets[Constants.NS_CLIENT] = Socket(Constants.NS_CLIENT,
                                                     ip.string, Constants.PORT_ANY);
    }

    /**
     * Config multi or singlepalyer - A single player game will only use the loopback code.
     */
    public static void Config(boolean multiplayer) {
        if (!multiplayer) {
            // shut down any existing sockets
            for (int i = 0; i < 2; i++) {
                if (ip_sockets[i] != null) {
                    try {
                        ip_sockets[i].close();
                    } catch (IOException e) {

                        e.printStackTrace();
                    }
                    ip_sockets[i] = null;
                }
            }
        } else {
            // open sockets
            OpenIP();
        }
    }

    /**
     * Init
     */
    public static void Init() {
        // nothing to do
    }

    /*
     * Socket
     */
    private static QuakeSocket Socket(int sock, String ip, int port) {

        QuakeSocket newsocket = null;
        try {
            if (ip == null || ip.length() == 0 || ip.equals("localhost")) {
                if (port == Constants.PORT_ANY) {
                    newsocket = socketFactory.bind(null, 0);
                } else {
                    newsocket = socketFactory.bind(null, port);
                }
            } else {
                newsocket = socketFactory.bind(ip, port);
            }

            // nonblocking channel
//            ip_channels[sock].configureBlocking(false);
            // the socket have to be broadcastable
//            newsocket.setBroadcast(true);
        } catch (Exception e) {
            Com.Println("Error: " + e.toString());
            Compatibility.printStackTrace(e);
            newsocket = null;
        }
        return newsocket;
    }

    /**
     * Shutdown - closes the sockets
     */
    public static void Shutdown() {
        // close sockets
        Config(false);
    }

    /** Sleeps msec or until net socket is ready. */
    public static void Sleep(int msec) {
        if (ip_sockets[Constants.NS_SERVER] == null
                || (Globals.dedicated != null && Globals.dedicated.value == 0))
            return; // we're not a server, just run full speed


        //TODO: check for timeout
        Compatibility.sleep(msec);
        //ip_sockets[NS_SERVER].

        // this should wait up to 100ms until a packet
        /*
         * struct timeval timeout;
         * fd_set fdset;
         * extern cvar_t *dedicated;
         * extern qboolean stdin_active;
         *
         * if (!ip_sockets[NS_SERVER] || (dedicated && !dedicated.value))
         * 		return; // we're not a server, just run full speed
         *
         * FD_ZERO(&fdset);
         *
         * if (stdin_active)
         * 		FD_SET(0, &fdset); // stdin is processed too
         *
         * FD_SET(ip_sockets[NS_SERVER], &fdset); // network socket
         *
         * timeout.tv_sec = msec/1000;
         * timeout.tv_usec = (msec%1000)*1000;
         *
         * select(ip_sockets[NS_SERVER]+1, &fdset, NULL, NULL, &timeout);
         */
    }
}

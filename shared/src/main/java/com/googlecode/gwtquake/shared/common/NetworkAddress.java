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
package com.googlecode.gwtquake.shared.common;


import java.net.InetAddress;
import java.net.UnknownHostException;

import com.googlecode.gwtquake.shared.sys.NET;

public class NetworkAddress {

    public int type;

    public int port;

    public byte ip[];

    public NetworkAddress() {
        this.type = Constants.NA_LOOPBACK;
        this.port = 0; // any
        try {
        	// localhost / 127.0.0.1
            this.ip = InetAddress.getByName(null).getAddress();
        } catch (UnknownHostException e) {
        }
    }

    public InetAddress getInetAddress() throws UnknownHostException {
        switch (type) {
        case Constants.NA_BROADCAST:
            return InetAddress.getByName("255.255.255.255");
        case Constants.NA_LOOPBACK:
        	// localhost / 127.0.0.1
            return InetAddress.getByName(null);
        case Constants.NA_IP:
            return InetAddress.getByAddress(ip);
        default:
            return null;
        }
    }

    public void set(NetworkAddress from) {
        type = from.type;
        port = from.port;
        ip[0] = from.ip[0];
        ip[1] = from.ip[1];
        ip[2] = from.ip[2];
        ip[3] = from.ip[3];
    }

    public String toString() {
        return (type == Constants.NA_LOOPBACK) ? "loopback" : NET
                .AdrToString(this);
    }
}

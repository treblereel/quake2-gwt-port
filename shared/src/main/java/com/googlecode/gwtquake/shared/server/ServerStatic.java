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
package com.googlecode.gwtquake.shared.server;


import java.io.RandomAccessFile;

import com.googlecode.gwtquake.shared.common.Buffer;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.game.EntityState;

public class ServerStatic {
    public ServerStatic() {
        for (int n = 0; n < Constants.MAX_CHALLENGES; n++) {
            challenges[n] = new Challenge();
        }
    }

    boolean initialized; // sv_init has completed

    int realtime; // always increasing, no clamping, etc

    String mapcmd = ""; // ie: *intro.cin+base

    int spawncount; // incremented each server start

    // used to check late spawns

    ClientData clients[]; // [maxclients->value];

    int num_client_entities; // maxclients->value*UPDATE_BACKUP*MAX_PACKET_ENTITIES

    int next_client_entities; // next client_entity to use

    EntityState client_entities[]; // [num_client_entities]

    int last_heartbeat;

    Challenge challenges[] = new Challenge[Constants.MAX_CHALLENGES]; // to
                                                                        // prevent
                                                                        // invalid
                                                                        // IPs
                                                                        // from
                                                                        // connecting

    // serverrecord values
    RandomAccessFile demofile;

    Buffer demo_multicast;

    byte demo_multicast_buf[] = new byte[Constants.MAX_MSGLEN];
}

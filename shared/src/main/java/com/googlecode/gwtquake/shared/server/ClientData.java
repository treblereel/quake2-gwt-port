/*
Copyright (C) 1997-2001 Id Software, Inc.

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
/* Modifications
   Copyright 2003-2004 Bytonic Software
   Copyright 2010 Google Inc.
*/
package com.googlecode.gwtquake.shared.server;

import com.googlecode.gwtquake.shared.common.Buffer;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.NetworkChannel;
import com.googlecode.gwtquake.shared.game.Entity;
import com.googlecode.gwtquake.shared.game.UserCommand;

public class ClientData {

    public static final int LATENCY_COUNTS = 16;
    public static final int RATE_MESSAGES = 10;
    int state;
    String userinfo = "";
    int lastframe; // for delta compression
    UserCommand lastcmd = new UserCommand(); // for filling in big drops
    int commandMsec; // every seconds this is reset, if user
    int frame_latency[] = new int[LATENCY_COUNTS];
    // commands exhaust it, assume time cheating
    int ping;
    int message_size[] = new int[RATE_MESSAGES]; // used to rate drop packets
    int rate;
    int surpressCount; // number of messages rate supressed
    // pointer
    Entity edict; // EDICT_NUM(clientnum+1)
    //char				name[32];			// extracted from userinfo, high bits masked
    String name = ""; // extracted from userinfo, high bits masked
    int messagelevel; // for filtering printed messages
    // The datagram is written to by sound calls, prints, temp ents, etc.
    // It can be harmlessly overflowed.
    Buffer datagram = Buffer.allocate(Constants.MAX_MSGLEN);
    ClientFrame frames[] = new ClientFrame[Constants.UPDATE_BACKUP]; // updates can be delta'd from here
    byte download[]; // file being downloaded
    int downloadsize; // total bytes (can't use EOF because of paks)
    int downloadcount; // bytes sent
    int lastmessage; // sv.framenum when packet was last received
    int lastconnect;
    int challenge; // challenge of this user, randomly generated
    NetworkChannel netchan = new NetworkChannel();
    //this was introduced by rst, since java can't calculate the index out of the address.
    int serverindex;

    public ClientData() {
        for (int n = 0; n < Constants.UPDATE_BACKUP; n++) {
            frames[n] = new ClientFrame();
        }
    }
}

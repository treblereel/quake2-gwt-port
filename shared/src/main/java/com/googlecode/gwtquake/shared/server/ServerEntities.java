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


import java.io.IOException;
import java.nio.ByteOrder;

import com.googlecode.gwtquake.shared.common.*;
import com.googlecode.gwtquake.shared.game.*;
import com.googlecode.gwtquake.shared.util.Math3D;

public class ServerEntities {

    /**
     * =============================================================================
     * 
     * Build a client frame structure
     * 
     * =============================================================================
     */

    public static byte fatpvs[] = new byte[65536 / 8]; // 32767 is MAX_MAP_LEAFS

    /*
     * =============================================================================
     * 
     * Encode a client frame onto the network channel
     * 
     * =============================================================================
     */

    /**
     * Writes a delta update of an entity_state_t list to the message.
     */
    static void SV_EmitPacketEntities(ClientFrame from, ClientFrame to,
            Buffer msg) {
        EntityState oldent = null, newent = null;
        int oldindex, newindex;
        int oldnum, newnum;
        int from_num_entities;
        int bits;

        Buffers.writeByte(msg, Constants.svc_packetentities);

        if (from == null)
            from_num_entities = 0;
        else
            from_num_entities = from.num_entities;

        newindex = 0;
        oldindex = 0;
        while (newindex < to.num_entities || oldindex < from_num_entities) {
            if (newindex >= to.num_entities)
                newnum = 9999;
            else {
                newent = ServerInit.svs.client_entities[(to.first_entity + newindex)
                        % ServerInit.svs.num_client_entities];
                newnum = newent.number;
            }

            if (oldindex >= from_num_entities)
                oldnum = 9999;
            else {
                oldent = ServerInit.svs.client_entities[(from.first_entity + oldindex)
                        % ServerInit.svs.num_client_entities];
                oldnum = oldent.number;
            }

            if (newnum == oldnum) { 
            	// delta update from old position
                // because the force parm is false, this will not result
                // in any bytes being emited if the entity has not changed at
                // all note that players are always 'newentities', this updates
                // their oldorigin always
                // and prevents warping
                Delta.WriteDeltaEntity(oldent, newent, msg, false,
                        newent.number <= ServerMain.maxclients.value);
                oldindex++;
                newindex++;
                continue;
            }

            if (newnum < oldnum) { 
            	// this is a new entity, send it from the baseline
                Delta.WriteDeltaEntity(ServerInit.sv.baselines[newnum], newent, msg,
                        true, true);
                newindex++;
                continue;
            }

            if (newnum > oldnum) { 
            	// the old entity isn't present in the new message
                bits = Constants.U_REMOVE;
                if (oldnum >= 256)
                    bits |= Constants.U_NUMBER16 | Constants.U_MOREBITS1;

                Buffers.writeByte(msg, bits & 255);
                if ((bits & 0x0000ff00) != 0)
                    Buffers.writeByte(msg, (bits >> 8) & 255);

                if ((bits & Constants.U_NUMBER16) != 0)
                    msg.WriteShort(oldnum);
                else
                    Buffers.writeByte(msg, oldnum);

                oldindex++;
                continue;
            }
        }

        msg.WriteShort(0); // end of packetentities

    }

    /** 
     * Writes the status of a player to a client system.
     */
    static void SV_WritePlayerstateToClient(ClientFrame from,
            ClientFrame to, Buffer msg) {
        int i;
        int pflags;
        // ptr
        PlayerState ps, ops;
        // mem
        PlayerState dummy;
        int statbits;

        ps = to.ps;
        if (from == null) {
            //memset (dummy, 0, sizeof(dummy));
            dummy = new PlayerState();
            ops = dummy;
        } else
            ops = from.ps;

        // determine what needs to be sent
        pflags = 0;

        if (ps.pmove.pm_type != ops.pmove.pm_type)
            pflags |= Constants.PS_M_TYPE;

        if (ps.pmove.origin[0] != ops.pmove.origin[0]
                || ps.pmove.origin[1] != ops.pmove.origin[1]
                || ps.pmove.origin[2] != ops.pmove.origin[2])
            pflags |= Constants.PS_M_ORIGIN;

        if (ps.pmove.velocity[0] != ops.pmove.velocity[0]
                || ps.pmove.velocity[1] != ops.pmove.velocity[1]
                || ps.pmove.velocity[2] != ops.pmove.velocity[2])
            pflags |= Constants.PS_M_VELOCITY;

        if (ps.pmove.pm_time != ops.pmove.pm_time)
            pflags |= Constants.PS_M_TIME;

        if (ps.pmove.pm_flags != ops.pmove.pm_flags)
            pflags |= Constants.PS_M_FLAGS;

        if (ps.pmove.gravity != ops.pmove.gravity)
            pflags |= Constants.PS_M_GRAVITY;

        if (ps.pmove.delta_angles[0] != ops.pmove.delta_angles[0]
                || ps.pmove.delta_angles[1] != ops.pmove.delta_angles[1]
                || ps.pmove.delta_angles[2] != ops.pmove.delta_angles[2])
            pflags |= Constants.PS_M_DELTA_ANGLES;

        if (ps.viewoffset[0] != ops.viewoffset[0]
                || ps.viewoffset[1] != ops.viewoffset[1]
                || ps.viewoffset[2] != ops.viewoffset[2])
            pflags |= Constants.PS_VIEWOFFSET;

        if (ps.viewangles[0] != ops.viewangles[0]
                || ps.viewangles[1] != ops.viewangles[1]
                || ps.viewangles[2] != ops.viewangles[2])
            pflags |= Constants.PS_VIEWANGLES;

        if (ps.kick_angles[0] != ops.kick_angles[0]
                || ps.kick_angles[1] != ops.kick_angles[1]
                || ps.kick_angles[2] != ops.kick_angles[2])
            pflags |= Constants.PS_KICKANGLES;

        if (ps.blend[0] != ops.blend[0] || ps.blend[1] != ops.blend[1]
                || ps.blend[2] != ops.blend[2] || ps.blend[3] != ops.blend[3])
            pflags |= Constants.PS_BLEND;

        if (ps.fov != ops.fov)
            pflags |= Constants.PS_FOV;

        if (ps.rdflags != ops.rdflags)
            pflags |= Constants.PS_RDFLAGS;

        if (ps.gunframe != ops.gunframe)
            pflags |= Constants.PS_WEAPONFRAME;

        pflags |= Constants.PS_WEAPONINDEX;

        // write it
        Buffers.writeByte(msg, Constants.svc_playerinfo);
        msg.WriteShort(pflags);

        // write the pmove_state_t
        if ((pflags & Constants.PS_M_TYPE) != 0)
            Buffers.writeByte(msg, ps.pmove.pm_type);

        if ((pflags & Constants.PS_M_ORIGIN) != 0) {
            msg.WriteShort(ps.pmove.origin[0]);
            msg.WriteShort(ps.pmove.origin[1]);
            msg.WriteShort(ps.pmove.origin[2]);
        }

        if ((pflags & Constants.PS_M_VELOCITY) != 0) {
            msg.WriteShort(ps.pmove.velocity[0]);
            msg.WriteShort(ps.pmove.velocity[1]);
            msg.WriteShort(ps.pmove.velocity[2]);
        }

        if ((pflags & Constants.PS_M_TIME) != 0)
            Buffers.writeByte(msg, ps.pmove.pm_time);

        if ((pflags & Constants.PS_M_FLAGS) != 0)
            Buffers.writeByte(msg, ps.pmove.pm_flags);

        if ((pflags & Constants.PS_M_GRAVITY) != 0)
            msg.WriteShort(ps.pmove.gravity);

        if ((pflags & Constants.PS_M_DELTA_ANGLES) != 0) {
            msg.WriteShort(ps.pmove.delta_angles[0]);
            msg.WriteShort(ps.pmove.delta_angles[1]);
            msg.WriteShort(ps.pmove.delta_angles[2]);
        }

        // write the rest of the player_state_t
        if ((pflags & Constants.PS_VIEWOFFSET) != 0) {
            Buffers.writeByte(msg, (int) (ps.viewoffset[0] * 4));
            Buffers.writeByte(msg, (int) (ps.viewoffset[1] * 4));
            Buffers.writeByte(msg, (int) (ps.viewoffset[2] * 4));
        }

        if ((pflags & Constants.PS_VIEWANGLES) != 0) {
            Buffers.WriteAngle16(msg, ps.viewangles[0]);
            Buffers.WriteAngle16(msg, ps.viewangles[1]);
            Buffers.WriteAngle16(msg, ps.viewangles[2]);
        }

        if ((pflags & Constants.PS_KICKANGLES) != 0) {
            Buffers.writeByte(msg, (int) (ps.kick_angles[0] * 4));
            Buffers.writeByte(msg, (int) (ps.kick_angles[1] * 4));
            Buffers.writeByte(msg, (int) (ps.kick_angles[2] * 4));
        }

        if ((pflags & Constants.PS_WEAPONINDEX) != 0) {
            Buffers.writeByte(msg, ps.gunindex);
        }

        if ((pflags & Constants.PS_WEAPONFRAME) != 0) {
            Buffers.writeByte(msg, ps.gunframe);
            Buffers.writeByte(msg, (int) (ps.gunoffset[0] * 4));
            Buffers.writeByte(msg, (int) (ps.gunoffset[1] * 4));
            Buffers.writeByte(msg, (int) (ps.gunoffset[2] * 4));
            Buffers.writeByte(msg, (int) (ps.gunangles[0] * 4));
            Buffers.writeByte(msg, (int) (ps.gunangles[1] * 4));
            Buffers.writeByte(msg, (int) (ps.gunangles[2] * 4));
        }

        if ((pflags & Constants.PS_BLEND) != 0) {
            Buffers.writeByte(msg, (int) (ps.blend[0] * 255));
            Buffers.writeByte(msg, (int) (ps.blend[1] * 255));
            Buffers.writeByte(msg, (int) (ps.blend[2] * 255));
            Buffers.writeByte(msg, (int) (ps.blend[3] * 255));
        }
        if ((pflags & Constants.PS_FOV) != 0)
			Buffers.writeByte(msg, (int) ps.fov);
        if ((pflags & Constants.PS_RDFLAGS) != 0)
            Buffers.writeByte(msg, ps.rdflags);

        // send stats
        statbits = 0;
        for (i = 0; i < Constants.MAX_STATS; i++)
            if (ps.stats[i] != ops.stats[i])
                statbits |= 1 << i;
        msg.putInt(statbits);
        for (i = 0; i < Constants.MAX_STATS; i++)
            if ((statbits & (1 << i)) != 0)
                msg.WriteShort(ps.stats[i]);
    }

    /**
     * Writes a frame to a client system.
     */
    public static void SV_WriteFrameToClient(ClientData client, Buffer msg) {
        //ptr
        ClientFrame frame, oldframe;
        int lastframe;

        //Com.Printf ("%i . %i\n", new
        // Vargs().add(client.lastframe).add(sv.framenum));
        // this is the frame we are creating
        frame = client.frames[ServerInit.sv.framenum & Constants.UPDATE_MASK];
        if (client.lastframe <= 0) { // client is asking for a retransmit
            oldframe = null;
            lastframe = -1;
        } else if (ServerInit.sv.framenum - client.lastframe >= (Constants.UPDATE_BACKUP - 3)) {
            // client hasn't gotten a good message through in a long time
            // Com_Printf ("%s: Delta request from out-of-date packet.\n",
            // client.name);
            oldframe = null;
            lastframe = -1;
        } else { // we have a valid message to delta from
            oldframe = client.frames[client.lastframe & Constants.UPDATE_MASK];
            lastframe = client.lastframe;
        }

        Buffers.writeByte(msg, Constants.svc_frame);
        msg.putInt(ServerInit.sv.framenum);
        msg.putInt(lastframe); // what we are delta'ing from
        Buffers.writeByte(msg, client.surpressCount); // rate dropped packets
        client.surpressCount = 0;

        // send over the areabits
        Buffers.writeByte(msg, frame.areabytes);
        Buffers.Write(msg, frame.areabits, frame.areabytes);

        // delta encode the playerstate
        SV_WritePlayerstateToClient(oldframe, frame, msg);

        // delta encode the entities
        SV_EmitPacketEntities(oldframe, frame, msg);
    }

    /** 
     * The client will interpolate the view position, so we can't use a single
     * PVS point. 
     */
    public static void SV_FatPVS(float[] org) {
        int leafs[] = new int[64];
        int i, j, count;
        int longs;
        byte src[];
        float[] mins = { 0, 0, 0 }, maxs = { 0, 0, 0 };

        for (i = 0; i < 3; i++) {
            mins[i] = org[i] - 8;
            maxs[i] = org[i] + 8;
        }

        count = CM.CM_BoxLeafnums(mins, maxs, leafs, 64, null);

        if (count < 1)
            Com.Error(Constants.ERR_FATAL, "SV_FatPVS: count < 1");

        longs = (CM.CM_NumClusters() + 31) >> 5;

        // convert leafs to clusters
        for (i = 0; i < count; i++)
            leafs[i] = CM.CM_LeafCluster(leafs[i]);

        System.arraycopy(CM.CM_ClusterPVS(leafs[0]), 0, ServerEntities.fatpvs, 0,
                longs << 2);
        // or in all the other leaf bits
        for (i = 1; i < count; i++) {
            for (j = 0; j < i; j++)
                if (leafs[i] == leafs[j])
                    break;
            if (j != i)
                continue; // already have the cluster we want

            src = CM.CM_ClusterPVS(leafs[i]);

            //for (j=0 ; j<longs ; j++)
            //	((long *)fatpvs)[j] |= ((long *)src)[j];
            int k = 0;
            for (j = 0; j < longs; j++) {
                ServerEntities.fatpvs[k] |= src[k++];
                ServerEntities.fatpvs[k] |= src[k++];
                ServerEntities.fatpvs[k] |= src[k++];
                ServerEntities.fatpvs[k] |= src[k++];
            }
        }
    }

    /**
     * Decides which entities are going to be visible to the client, and copies
     * off the playerstat and areabits.
     */
    public static void SV_BuildClientFrame(ClientData client) {
        int e, i;
        float[] org = { 0, 0, 0 };
        Entity ent;
        Entity clent;
        ClientFrame frame;
        EntityState state;
        int l;
        int clientarea, clientcluster;
        int leafnum;
        int c_fullsend;
        byte clientphs[];
        byte bitvector[];

        clent = client.edict;
        if (clent.client == null)
            return; // not in game yet

        // this is the frame we are creating
        frame = client.frames[ServerInit.sv.framenum & Constants.UPDATE_MASK];

        frame.senttime = ServerInit.svs.realtime; // save it for ping calc later

        // find the client's PVS
        for (i = 0; i < 3; i++)
            org[i] = clent.client.ps.pmove.origin[i] * 0.125f
                    + clent.client.ps.viewoffset[i];

        leafnum = CM.CM_PointLeafnum(org);
        clientarea = CM.CM_LeafArea(leafnum);
        clientcluster = CM.CM_LeafCluster(leafnum);

        // calculate the visible areas
        frame.areabytes = CM.CM_WriteAreaBits(frame.areabits, clientarea);

        // grab the current player_state_t
        frame.ps.set(clent.client.ps);

        SV_FatPVS(org);
        clientphs = CM.CM_ClusterPHS(clientcluster);

        // build up the list of visible entities
        frame.num_entities = 0;
        frame.first_entity = ServerInit.svs.next_client_entities;

        c_fullsend = 0;

        for (e = 1; e < GameBase.num_edicts; e++) {
            ent = GameBase.g_edicts[e];

            // ignore ents without visible models
            if ((ent.svflags & Constants.SVF_NOCLIENT) != 0)
                continue;

            // ignore ents without visible models unless they have an effect
            if (0 == ent.s.modelindex && 0 == ent.s.effects && 0 == ent.s.sound
                    && 0 == ent.s.event)
                continue;

            // ignore if not touching a PV leaf
            // check area
            if (ent != clent) {
                if (!CM.CM_AreasConnected(clientarea, ent.areanum)) {
                	// doors can legally straddle two areas, so we may need to check another one
                    if (0 == ent.areanum2 || !CM.CM_AreasConnected(clientarea, ent.areanum2))
                        continue; // blocked by a door
                }

                // beams just check one point for PHS
                if ((ent.s.renderfx & Constants.RF_BEAM) != 0) {
                    l = ent.clusternums[0];
                    if (0 == (clientphs[l >> 3] & (1 << (l & 7))))
                        continue;
                } else {
                    // FIXME: if an ent has a model and a sound, but isn't
                    // in the PVS, only the PHS, clear the model
                    if (ent.s.sound == 0) {
                        bitvector = ServerEntities.fatpvs; //clientphs;
                    } else
                        bitvector = ServerEntities.fatpvs;

                    if (ent.num_clusters == -1) { // too many leafs for
                                                  // individual check, go by
                                                  // headnode
                        if (!CM.CM_HeadnodeVisible(ent.headnode, bitvector))
                            continue;
                        c_fullsend++;
                    } else { // check individual leafs
                        for (i = 0; i < ent.num_clusters; i++) {
                            l = ent.clusternums[i];
                            if ((bitvector[l >> 3] & (1 << (l & 7))) != 0)
                                break;
                        }
                        if (i == ent.num_clusters)
                            continue; // not visible
                    }

                    if (ent.s.modelindex == 0) { // don't send sounds if they
                                                 // will be attenuated away
                        float[] delta = { 0, 0, 0 };
                        float len;

                        Math3D.VectorSubtract(org, ent.s.origin, delta);
                        len = Math3D.VectorLength(delta);
                        if (len > 400)
                            continue;
                    }
                }
            }

            // add it to the circular client_entities array
            int ix = ServerInit.svs.next_client_entities
                    % ServerInit.svs.num_client_entities;
            state = ServerInit.svs.client_entities[ix];
            if (ent.s.number != e) {
                Com.DPrintf("FIXING ENT.S.NUMBER!!!\n");
                ent.s.number = e;
            }

            //*state = ent.s;
            ServerInit.svs.client_entities[ix].set(ent.s);

            // don't mark players missiles as solid
            if (ent.owner == client.edict)
                state.solid = 0;

            ServerInit.svs.next_client_entities++;
            frame.num_entities++;
        }
    }

    /**
     * Save everything in the world out without deltas. Used for recording
     * footage for merged or assembled demos.
     */
    public static void SV_RecordDemoMessage() {
        int e;
        Entity ent;
        EntityState nostate = new EntityState(null);
        int len;

        if (ServerInit.svs.demofile == null)
            return;

        //memset (nostate, 0, sizeof(nostate));
        Buffer buf = Buffer.allocate(32768).order(ByteOrder.LITTLE_ENDIAN);

        // write a frame message that doesn't contain a player_state_t
        Buffers.writeByte(buf, Constants.svc_frame);
        buf.putInt(ServerInit.sv.framenum);

        Buffers.writeByte(buf, Constants.svc_packetentities);

        e = 1;
        ent = GameBase.g_edicts[e];

        while (e < GameBase.num_edicts) {
            // ignore ents without visible models unless they have an effect
            if (ent.inuse
                    && ent.s.number != 0
                    && (ent.s.modelindex != 0 || ent.s.effects != 0
                            || ent.s.sound != 0 || ent.s.event != 0)
                    && 0 == (ent.svflags & Constants.SVF_NOCLIENT))
                Delta.WriteDeltaEntity(nostate, ent.s, buf, false, true);

            e++;
            ent = GameBase.g_edicts[e];
        }

        buf.WriteShort(0); // end of packetentities

        // now add the accumulated multicast information
        Buffers.Write(buf, ServerInit.svs.demo_multicast.data,
                ServerInit.svs.demo_multicast.cursize);
        ServerInit.svs.demo_multicast.clear();

        // now write the entire message to the file, prefixed by the length
        len = EndianHandler.swapInt(buf.cursize);

        try {
            //fwrite (len, 4, 1, svs.demofile);
            ServerInit.svs.demofile.writeInt(len);
            //fwrite (buf.data, buf.cursize, 1, svs.demofile);
            ServerInit.svs.demofile.write(buf.data, 0, buf.cursize);
        } catch (IOException e1) {
            Com.Printf("Error writing demo file:" + e);
        	Compatibility.printStackTrace(e1);
        }
    }
}

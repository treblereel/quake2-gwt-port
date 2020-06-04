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

import com.googlecode.gwtquake.shared.game.*;


public class Delta {

    //
    // writing functions
    //

    public static void WriteDeltaUsercmd(Buffer buf, UserCommand from,
            UserCommand cmd) {
        int bits;

        //
        // send the movement message
        //
        bits = 0;
        if (cmd.angles[0] != from.angles[0])
            bits |= Constants.CM_ANGLE1;
        if (cmd.angles[1] != from.angles[1])
            bits |= Constants.CM_ANGLE2;
        if (cmd.angles[2] != from.angles[2])
            bits |= Constants.CM_ANGLE3;
        if (cmd.forwardmove != from.forwardmove)
            bits |= Constants.CM_FORWARD;
        if (cmd.sidemove != from.sidemove)
            bits |= Constants.CM_SIDE;
        if (cmd.upmove != from.upmove)
            bits |= Constants.CM_UP;
        if (cmd.buttons != from.buttons)
            bits |= Constants.CM_BUTTONS;
        if (cmd.impulse != from.impulse)
            bits |= Constants.CM_IMPULSE;

        Buffers.writeByte(buf, bits);

        if ((bits & Constants.CM_ANGLE1) != 0)
            buf.WriteShort(cmd.angles[0]);
        if ((bits & Constants.CM_ANGLE2) != 0)
            buf.WriteShort(cmd.angles[1]);
        if ((bits & Constants.CM_ANGLE3) != 0)
            buf.WriteShort(cmd.angles[2]);

        if ((bits & Constants.CM_FORWARD) != 0)
            buf.WriteShort(cmd.forwardmove);
        if ((bits & Constants.CM_SIDE) != 0)
            buf.WriteShort(cmd.sidemove);
        if ((bits & Constants.CM_UP) != 0)
            buf.WriteShort(cmd.upmove);

        if ((bits & Constants.CM_BUTTONS) != 0)
            Buffers.writeByte(buf, cmd.buttons);
        if ((bits & Constants.CM_IMPULSE) != 0)
            Buffers.writeByte(buf, cmd.impulse);

        Buffers.writeByte(buf, cmd.msec);
        Buffers.writeByte(buf, cmd.lightlevel);
    }

    /*
     * ================== WriteDeltaEntity
     * 
     * Writes part of a packetentities message. Can delta from either a baseline
     * or a previous packet_entity ==================
     */
    public static void WriteDeltaEntity(EntityState from, EntityState to,
            Buffer msg, boolean force, boolean newentity) {
        int bits;

        if (0 == to.number)
            Com.Error(Constants.ERR_FATAL, "Unset entity number");
        if (to.number >= Constants.MAX_EDICTS)
            Com.Error(Constants.ERR_FATAL, "Entity number >= MAX_EDICTS");

        // send an update
        bits = 0;

        if (to.number >= 256)
            bits |= Constants.U_NUMBER16; // number8 is implicit otherwise

        if (to.origin[0] != from.origin[0])
            bits |= Constants.U_ORIGIN1;
        if (to.origin[1] != from.origin[1])
            bits |= Constants.U_ORIGIN2;
        if (to.origin[2] != from.origin[2])
            bits |= Constants.U_ORIGIN3;

        if (to.angles[0] != from.angles[0])
            bits |= Constants.U_ANGLE1;
        if (to.angles[1] != from.angles[1])
            bits |= Constants.U_ANGLE2;
        if (to.angles[2] != from.angles[2])
            bits |= Constants.U_ANGLE3;

        if (to.skinnum != from.skinnum) {
            if (to.skinnum < 256)
                bits |= Constants.U_SKIN8;
            else if (to.skinnum < 0x10000)
                bits |= Constants.U_SKIN16;
            else
                bits |= (Constants.U_SKIN8 | Constants.U_SKIN16);
        }

        if (to.frame != from.frame) {
            if (to.frame < 256)
                bits |= Constants.U_FRAME8;
            else
                bits |= Constants.U_FRAME16;
        }

        if (to.effects != from.effects) {
            if (to.effects < 256)
                bits |= Constants.U_EFFECTS8;
            else if (to.effects < 0x8000)
                bits |= Constants.U_EFFECTS16;
            else
                bits |= Constants.U_EFFECTS8 | Constants.U_EFFECTS16;
        }

        if (to.renderfx != from.renderfx) {
            if (to.renderfx < 256)
                bits |= Constants.U_RENDERFX8;
            else if (to.renderfx < 0x8000)
                bits |= Constants.U_RENDERFX16;
            else
                bits |= Constants.U_RENDERFX8 | Constants.U_RENDERFX16;
        }

        if (to.solid != from.solid)
            bits |= Constants.U_SOLID;

        // event is not delta compressed, just 0 compressed
        if (to.event != 0)
            bits |= Constants.U_EVENT;

        if (to.modelindex != from.modelindex)
            bits |= Constants.U_MODEL;
        if (to.modelindex2 != from.modelindex2)
            bits |= Constants.U_MODEL2;
        if (to.modelindex3 != from.modelindex3)
            bits |= Constants.U_MODEL3;
        if (to.modelindex4 != from.modelindex4)
            bits |= Constants.U_MODEL4;

        if (to.sound != from.sound)
            bits |= Constants.U_SOUND;

        if (newentity || (to.renderfx & Constants.RF_BEAM) != 0)
            bits |= Constants.U_OLDORIGIN;

        //
        // write the message
        //
        if (bits == 0 && !force)
            return; // nothing to send!

        //----------

        if ((bits & 0xff000000) != 0)
            bits |= Constants.U_MOREBITS3 | Constants.U_MOREBITS2 | Constants.U_MOREBITS1;
        else if ((bits & 0x00ff0000) != 0)
            bits |= Constants.U_MOREBITS2 | Constants.U_MOREBITS1;
        else if ((bits & 0x0000ff00) != 0)
            bits |= Constants.U_MOREBITS1;

        Buffers.writeByte(msg, bits & 255);

        if ((bits & 0xff000000) != 0) {
            Buffers.writeByte(msg, (bits >>> 8) & 255);
            Buffers.writeByte(msg, (bits >>> 16) & 255);
            Buffers.writeByte(msg, (bits >>> 24) & 255);
        } else if ((bits & 0x00ff0000) != 0) {
            Buffers.writeByte(msg, (bits >>> 8) & 255);
            Buffers.writeByte(msg, (bits >>> 16) & 255);
        } else if ((bits & 0x0000ff00) != 0) {
            Buffers.writeByte(msg, (bits >>> 8) & 255);
        }

        //----------

        if ((bits & Constants.U_NUMBER16) != 0)
            msg.WriteShort(to.number);
        else
            Buffers.writeByte(msg, to.number);

        if ((bits & Constants.U_MODEL) != 0)
            Buffers.writeByte(msg, to.modelindex);
        if ((bits & Constants.U_MODEL2) != 0)
            Buffers.writeByte(msg, to.modelindex2);
        if ((bits & Constants.U_MODEL3) != 0)
            Buffers.writeByte(msg, to.modelindex3);
        if ((bits & Constants.U_MODEL4) != 0)
            Buffers.writeByte(msg, to.modelindex4);

        if ((bits & Constants.U_FRAME8) != 0)
            Buffers.writeByte(msg, to.frame);
        if ((bits & Constants.U_FRAME16) != 0)
            msg.WriteShort(to.frame);

        if ((bits & Constants.U_SKIN8) != 0 && (bits & Constants.U_SKIN16) != 0) //used for laser
                                                             // colors
            msg.putInt(to.skinnum);
        else if ((bits & Constants.U_SKIN8) != 0)
            Buffers.writeByte(msg, to.skinnum);
        else if ((bits & Constants.U_SKIN16) != 0)
            msg.WriteShort(to.skinnum);

        if ((bits & (Constants.U_EFFECTS8 | Constants.U_EFFECTS16)) == (Constants.U_EFFECTS8 | Constants.U_EFFECTS16))
            msg.putInt(to.effects);
        else if ((bits & Constants.U_EFFECTS8) != 0)
            Buffers.writeByte(msg, to.effects);
        else if ((bits & Constants.U_EFFECTS16) != 0)
            msg.WriteShort(to.effects);

        if ((bits & (Constants.U_RENDERFX8 | Constants.U_RENDERFX16)) == (Constants.U_RENDERFX8 | Constants.U_RENDERFX16))
            msg.putInt(to.renderfx);
        else if ((bits & Constants.U_RENDERFX8) != 0)
            Buffers.writeByte(msg, to.renderfx);
        else if ((bits & Constants.U_RENDERFX16) != 0)
            msg.WriteShort(to.renderfx);

        if ((bits & Constants.U_ORIGIN1) != 0)
            Buffers.WriteCoord(msg, to.origin[0]);
        if ((bits & Constants.U_ORIGIN2) != 0)
            Buffers.WriteCoord(msg, to.origin[1]);
        if ((bits & Constants.U_ORIGIN3) != 0)
            Buffers.WriteCoord(msg, to.origin[2]);

        if ((bits & Constants.U_ANGLE1) != 0)
            Buffers.WriteAngle(msg, to.angles[0]);
        if ((bits & Constants.U_ANGLE2) != 0)
            Buffers.WriteAngle(msg, to.angles[1]);
        if ((bits & Constants.U_ANGLE3) != 0)
            Buffers.WriteAngle(msg, to.angles[2]);

        if ((bits & Constants.U_OLDORIGIN) != 0) {
            Buffers.WriteCoord(msg, to.old_origin[0]);
            Buffers.WriteCoord(msg, to.old_origin[1]);
            Buffers.WriteCoord(msg, to.old_origin[2]);
        }

        if ((bits & Constants.U_SOUND) != 0)
            Buffers.writeByte(msg, to.sound);
        if ((bits & Constants.U_EVENT) != 0)
            Buffers.writeByte(msg, to.event);
        if ((bits & Constants.U_SOLID) != 0)
            msg.WriteShort(to.solid);
    }

    //============================================================

    //
    // reading functions
    //

    public static void ReadDeltaUsercmd(Buffer msg_read, UserCommand from,
            UserCommand move) {
        int bits;

        //memcpy(move, from, sizeof(* move));
        // IMPORTANT!! copy without new
        move.set(from);
        bits = Buffers.readUnsignedByte(msg_read);

        // read current angles
        if ((bits & Constants.CM_ANGLE1) != 0)
            move.angles[0] = msg_read.getShort();
        if ((bits & Constants.CM_ANGLE2) != 0)
            move.angles[1] = msg_read.getShort();
        if ((bits & Constants.CM_ANGLE3) != 0)
            move.angles[2] = msg_read.getShort();

        // read movement
        if ((bits & Constants.CM_FORWARD) != 0)
            move.forwardmove = msg_read.getShort();
        if ((bits & Constants.CM_SIDE) != 0)
            move.sidemove = msg_read.getShort();
        if ((bits & Constants.CM_UP) != 0)
            move.upmove = msg_read.getShort();

        // read buttons
        if ((bits & Constants.CM_BUTTONS) != 0)
            move.buttons = (byte) Buffers.readUnsignedByte(msg_read);

        if ((bits & Constants.CM_IMPULSE) != 0)
            move.impulse = (byte) Buffers.readUnsignedByte(msg_read);

        // read time to run command
        move.msec = (byte) Buffers.readUnsignedByte(msg_read);

        // read the light level
        move.lightlevel = (byte) Buffers.readUnsignedByte(msg_read);

    }    
            
}

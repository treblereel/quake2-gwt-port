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
package com.googlecode.gwtquake.shared.game;


import java.io.IOException;

import com.googlecode.gwtquake.shared.util.Math3D;
import com.googlecode.gwtquake.shared.util.QuakeFile;

/** Client data that stays across deathmatch respawns.*/
public class ClientRespawn

{
	/** What to set client->pers to on a respawn */
	protected ClientPersistentState coop_respawn = new ClientPersistentState();
	 
	/** Level.framenum the client entered the game. */
	protected int enterframe;
	 		
	/** frags, etc. */
	protected int score; 
	
	/** angles sent over in the last command. */
	protected float cmd_angles[] = { 0, 0, 0 };
	 
	/** client is a spectator. */
	protected boolean spectator; 

	
	/** Copies the client respawn data. */
	public void set(ClientRespawn from)
	{
		coop_respawn.set(from.coop_respawn);
		enterframe = from.enterframe;
		score = from.score;
		Math3D.VectorCopy(from.cmd_angles, cmd_angles);
		spectator = from.spectator;
	}

	/** Clears the client reaspawn informations. */
	public void clear()
	{
		coop_respawn = new ClientPersistentState();
		enterframe = 0;
		score = 0;
		Math3D.VectorClear(cmd_angles);
		spectator = false;
	}

	/** Reads a client_respawn from a file. */
	public void read(QuakeFile f) throws IOException
	{
		coop_respawn.read(f);
		enterframe = f.readInt();
		score = f.readInt();
		cmd_angles[0] = f.readFloat();
		cmd_angles[1] = f.readFloat();
		cmd_angles[2] = f.readFloat();
		spectator = f.readInt() != 0;
	}
	
	/** Writes a client_respawn to a file. */
	public void write(QuakeFile f) throws IOException
	{
		coop_respawn.write(f);
		f.writeInt(enterframe);
		f.writeInt(score);
		f.writeFloat(cmd_angles[0]);
		f.writeFloat(cmd_angles[1]);
		f.writeFloat(cmd_angles[2]);
		f.writeInt(spectator?1:0);
	}
}

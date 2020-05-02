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
import java.io.RandomAccessFile;

import com.googlecode.gwtquake.shared.game.adapters.EntInteractAdapter;
import com.googlecode.gwtquake.shared.game.adapters.EntityDodgeAdapter;
import com.googlecode.gwtquake.shared.game.adapters.EntityThinkAdapter;
import com.googlecode.gwtquake.shared.util.QuakeFile;

public class MonsterInfo {

	public MonsterMove currentmove;
	public int aiflags;
	public int nextframe;
	public float scale;

	public EntityThinkAdapter stand;
	public EntityThinkAdapter idle;
	public EntityThinkAdapter search;
	public EntityThinkAdapter walk;
	public EntityThinkAdapter run;

	public EntityDodgeAdapter dodge;

	public EntityThinkAdapter attack;
	public EntityThinkAdapter melee;

	public EntInteractAdapter sight;

	public EntityThinkAdapter checkattack;

	public float pausetime;
	public float attack_finished;

	public float[] saved_goal= { 0, 0, 0 };
	public float search_time;
	public float trail_time;
	public float[] last_sighting= { 0, 0, 0 };
	public int attack_state;
	public int lefty;
	public float idle_time;
	public int linkcount;

	public int power_armor_type;
	public int power_armor_power;

	/** Writes the monsterinfo to the file.*/
	public void write(QuakeFile f) throws IOException
	{
		f.writeBoolean(currentmove != null);
		if (currentmove != null)
			currentmove.write(f);
		f.writeInt(aiflags);
		f.writeInt(nextframe);
		f.writeFloat(scale);
		f.writeAdapter(stand);
		f.writeAdapter(idle);
		f.writeAdapter(search);
		f.writeAdapter(walk);
		f.writeAdapter(run);
		
		f.writeAdapter(dodge);
		
		f.writeAdapter(attack);
		f.writeAdapter(melee);
		
		f.writeAdapter(sight);
		
		f.writeAdapter(checkattack);
		
 		f.writeFloat(pausetime);
 		f.writeFloat(attack_finished);
 	
		f.writeVector(saved_goal);
		
		f.writeFloat(search_time);
		f.writeFloat(trail_time);
		
		f.writeVector(last_sighting);
 
		f.writeInt(attack_state);
		f.writeInt(lefty);
	
		f.writeFloat(idle_time);
		f.writeInt(linkcount);
		
		f.writeInt(power_armor_power);
		f.writeInt(power_armor_type);
	}

	/** Writes the monsterinfo to the file.*/
	public void read(QuakeFile f) throws IOException
	{
		if (f.readBoolean())
		{
			currentmove= new MonsterMove();
			currentmove.read(f);
		}
		else
			currentmove= null; 
		aiflags = f.readInt();
		nextframe = f.readInt();
		scale = f.readFloat();
		stand = (EntityThinkAdapter) f.readAdapter();
		idle = (EntityThinkAdapter) f.readAdapter();
		search = (EntityThinkAdapter) f.readAdapter();
		walk = (EntityThinkAdapter) f.readAdapter();
		run = (EntityThinkAdapter) f.readAdapter();
		
		dodge = (EntityDodgeAdapter) f.readAdapter();
		
		attack = (EntityThinkAdapter) f.readAdapter();
		melee = (EntityThinkAdapter) f.readAdapter();
		
		sight = (EntInteractAdapter) f.readAdapter();
		
		checkattack = (EntityThinkAdapter) f.readAdapter();
		
 		pausetime = f.readFloat();
 		attack_finished = f.readFloat();
 	
		saved_goal = f.readVector();
		
		search_time = f.readFloat();
		trail_time = f.readFloat();
		
		last_sighting = f.readVector();
 
		attack_state = f.readInt();
		lefty = f.readInt();
	
		idle_time = f.readFloat();
		linkcount = f.readInt();
		
		power_armor_power = f.readInt();
		power_armor_type = f.readInt();

	}


}

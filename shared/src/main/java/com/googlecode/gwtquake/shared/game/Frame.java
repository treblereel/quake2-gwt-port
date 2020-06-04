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

import com.googlecode.gwtquake.shared.game.adapters.AIAdapter;
import com.googlecode.gwtquake.shared.game.adapters.EntityThinkAdapter;
import com.googlecode.gwtquake.shared.util.Lib;
import com.googlecode.gwtquake.shared.util.QuakeFile;

public class Frame
{
	public Frame(AIAdapter ai, float dist, EntityThinkAdapter think)
	{
		this.ai= ai;
		this.dist= dist;
		this.think= think;
	}
	
	/** Empty constructor. */	
	public Frame()
	{}

	public AIAdapter ai;
	public float dist;
	public EntityThinkAdapter think;

	public void write(QuakeFile f) throws IOException
	{
		f.writeAdapter(ai);
		f.writeFloat(dist);
		f.writeAdapter(think);
	}

	public void read(QuakeFile f) throws IOException
	{
		ai= (AIAdapter) f.readAdapter();
		dist= f.readFloat();
		think= (EntityThinkAdapter) f.readAdapter();
	}
}

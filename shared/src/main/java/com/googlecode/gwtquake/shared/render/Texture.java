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
package com.googlecode.gwtquake.shared.render;

import java.util.Arrays;

public class Texture {
	// [s/t][xyz offset]
	public float vecs[][] = {
		 { 0, 0, 0, 0 },
		 { 0, 0, 0, 0 }
	};
	public int flags;
	public int numframes;
	public Texture next; // animation chain
	public Image image;
	
	public void clear() {
		Arrays.fill(vecs[0], 0);
		Arrays.fill(vecs[1], 0);
		
		flags = 0;
		numframes = 0;
		next = null;
		image = null;
	}

	/**
	 * R_TextureAnimation
	 * Returns the proper texture for a given time and base texture
	 */
	public static Image R_TextureAnimation(Texture tex)
	{
		if (tex.next == null)
			return tex.image;
	
		int c = GlState.currententity.frame % tex.numframes;
		while (c != 0)
		{
			tex = tex.next;
			c--;
		}
	
		return tex.image;
	}
	
}

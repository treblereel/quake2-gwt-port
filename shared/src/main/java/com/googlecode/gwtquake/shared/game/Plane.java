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

import com.googlecode.gwtquake.shared.util.Math3D;

public class Plane
{
	public float normal[] = new float[3];
	public float dist;
	/** This is for fast side tests, 0=xplane, 1=yplane, 2=zplane and 3=arbitrary. */
	public byte type;
	/** This represents signx + (signy<<1) + (signz << 1). */
	public byte signbits; // signx + (signy<<1) + (signz<<1)
	public byte pad[] = { 0, 0 };
	
	public void set(Plane c) {
		Math3D.set(normal, c.normal);
		dist = c.dist;
		type = c.type;
		signbits = c.signbits;
		pad[0] = c.pad[0];
		pad[1] = c.pad[1];
	}

	public void clear() {
		Math3D.VectorClear(normal);
		dist = 0;
		type = 0;
		signbits = 0;
		pad[0] = 0;
		pad[1] = 0;
	}

	/**
	 * SignbitsForPlane
	 */
	public static int SignbitsForPlane(Plane out) {
		// for fast box on planeside test
		int bits = 0;
		for (int j = 0; j < 3; j++) {
			if (out.normal[j] < 0)
				bits |= (1 << j);
		}
		return bits;
	}
}

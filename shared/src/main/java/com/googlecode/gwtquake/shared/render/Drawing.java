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



import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import com.googlecode.gwtquake.shared.client.Dimension;
import com.googlecode.gwtquake.shared.client.Window;
import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.QuakeImage;
import com.googlecode.gwtquake.shared.util.Lib;

/**
 * Draw
 * (gl_draw.c)
 * 
 * @author cwei
 */
public class Drawing  {

	/*
	===============
	Draw_InitLocal
	===============
	*/
	static void Draw_InitLocal() {
		// load console characters
		Images.draw_chars = Images.findTexture("pics/conchars.pcx", QuakeImage.it_pic);
	}

	/*
	================
	Draw_Char

	Draws one 8*8 graphics character with 0 being transparent.
	It can be clipped to the top of the screen to allow the console to be
	smoothly scrolled off.
	================
    */
	
	
	
	
	


	
	
// ====================================================================

    // allocate a 256 * 256 texture buffer
    private static ByteBuffer image8 = Lib.newByteBuffer(256 * 256 * Constants.SIZE_OF_INT);
    // share the buffer
    static IntBuffer image32 = image8.asIntBuffer();

	
}

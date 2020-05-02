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

import com.googlecode.gwtquake.shared.common.QuakeImage;

public class Image {
	
	// used to get the pos in array
	// added by cwei
	private int id;
	
	// quake 2 variables
	public String name=""; // game path, including extension
	// enum imagetype_t
	public int type;
	public int width, height; // source image
	public int upload_width, upload_height; // after power of two and picmip
	public int registration_sequence; // 0 = free
	public Surface texturechain; // for sort-by-texture world drawing
	public int texnum; // gl texture binding
	public boolean has_alpha;

	public boolean paletted;
	public boolean complete;
	
	public Image(int id) {
		this.id = id;
	}

    public void setData(byte[] pic, int width, int height, int bits) {
        width = width;
        height = height;
        complete = true;

        int i;

        if (type == QuakeImage.it_skin && bits == 8) {
            Images.R_FloodFillSkin(pic, width, height);
        }

        //image.texnum = TEXNUM_IMAGES + image.getId(); //image pos in array
        Images.GL_Bind(texnum);

        if (bits == 8) {
            has_alpha = Images.GL_Upload8(pic, width, height, (type != QuakeImage.it_pic && type != QuakeImage.it_sky), type == QuakeImage.it_sky);
        }
        else {
            int[] tmp = QuakeImage.bytesToIntsAbgr(pic);
            has_alpha = Images.GL_Upload32(tmp, width, height, (type != QuakeImage.it_pic && type != QuakeImage.it_sky));
        }
        upload_width = Images.upload_width; // after power of 2 and scales
        upload_height = Images.upload_height;
    }

    public void clear() {
		// don't clear the id
		// wichtig !!!
		name = "";
		type = 0;
		width = height = 0;
		upload_width = upload_height = 0;
		registration_sequence = 0; // 0 = free
		texturechain = null;
		texnum = 0; // gl texture binding
		has_alpha = false;
		paletted = false;
		complete = false;
	}

	public int getId() {
		return id;
	}
	
	
	public String toString() {
		return name + ":" + texnum;
	}

}

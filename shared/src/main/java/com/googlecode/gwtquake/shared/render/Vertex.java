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
package com.googlecode.gwtquake.shared.render;

import java.nio.ByteBuffer;

import com.googlecode.gwtquake.shared.common.Constants;

public class Vertex {
  public static final int DISK_SIZE = 3 * Constants.SIZE_OF_FLOAT;
  public static final int MEM_SIZE = 3 * Constants.SIZE_OF_FLOAT;

  public float[] position = { 0, 0, 0 };

  public Vertex(ByteBuffer b) {
    position[0] = b.getFloat();
    position[1] = b.getFloat();
    position[2] = b.getFloat();
  }
}

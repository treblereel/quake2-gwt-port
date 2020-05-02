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

public class Leaf extends Node {
  // leaf specific
  public int cluster;
  public int area;

  // public msurface_t firstmarksurface;
  public int nummarksurfaces;

  // added by cwei
  int markIndex;
  Surface[] markSurfaces;

  public void setMarkSurface(int markIndex, Surface[] markSurfaces) {
    this.markIndex = markIndex;
    this.markSurfaces = markSurfaces;
  }

  public Surface getMarkSurface(int index) {
    assert (index >= 0 && index <= nummarksurfaces) : "mleaf: markSurface bug (index = "
        + index + "; num = " + nummarksurfaces + ")";
    // TODO code in Surf.R_RecursiveWorldNode aendern (der Pointer wird wie in C
    // zu weit gezaehlt)
    return (index < nummarksurfaces) ? markSurfaces[markIndex + index] : null;
  }

}

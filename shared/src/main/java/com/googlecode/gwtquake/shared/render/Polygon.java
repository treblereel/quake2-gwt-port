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

public class Polygon {
  Polygon next;
  Polygon chain;
  int numverts;
  int flags; // for SURF_UNDERWATER (not needed anymore?)

  /** The position inside Polygons.buffer */
  int pos = 0;

  Polygon() {
  }

    /**
   * DrawGLPoly
   */
  public void draw() {
    GlState.gl.glDrawArrays(Gl1Context._GL_POLYGON, pos, numverts);
  }

    /**
     * DrawGLFlowingPoly version that handles scrolling texture
     */
    public void drawFlowing() {
      float scroll = -64 * ((GlState.r_newrefdef.time / 40.0f) -
          (int) (GlState.r_newrefdef.time / 40.0f));
      if (scroll == 0.0f) {
        scroll = -64.0f;
      }
      beginScrolling(scroll);
      GlState.gl.glDrawArrays(Gl1Context._GL_POLYGON, pos, numverts);
      endScrolling();
    }

    final void clear() {
    next = null;
    chain = null;
    numverts = 0;
    flags = 0;
  }

  public final float getX(int index) {
    return Polygons.buffer.get((index + pos) * 7 + 2);
  }

  public final void setX(int index, float value) {
    Polygons.buffer.put((index + pos) * 7 + 2, value);
  }

  public final float getY(int index) {
    return Polygons.buffer.get((index + pos) * 7 + 3);
  }

  public final void setY(int index, float value) {
    Polygons.buffer.put((index + pos) * 7 + 3, value);
  }

  public final float getZ(int index) {
    return Polygons.buffer.get((index + pos) * 7 + 4);
  }

  public final void setZ(int index, float value) {
    Polygons.buffer.put((index + pos) * 7 + 4, value);
  }

  public final float getS1(int index) {
    return Polygons.buffer.get((index + pos) * 7 + 0);
  }

  public final void setS1(int index, float value) {
    Polygons.buffer.put((index + pos) * 7 + 0, value);
  }

  public final float getT1(int index) {
    return Polygons.buffer.get((index + pos) * 7 + 1);
  }

  public final void setT1(int index, float value) {
    Polygons.buffer.put((index + pos) * 7 + 1, value);
  }

  public final float getS2(int index) {
    return Polygons.buffer.get((index + pos) * 7 + 5);
  }

  public final void setS2(int index, float value) {
    Polygons.buffer.put((index + pos) * 7 + 5, value);
  }

  public final float getT2(int index) {
    return Polygons.buffer.get((index + pos) * 7 + 6);
  }

  public final void setT2(int index, float value) {
    Polygons.buffer.put((index + pos) * 7 + 6, value);
  }

  public final void beginScrolling(float scroll) {
    int index = pos * 7;
    for (int i = 0; i < numverts; i++, index += 7) {
      scroll += Polygons.s1_old[i] = Polygons.buffer.get(index);
      Polygons.buffer.put(index, scroll);
    }
  }

  public final void endScrolling() {
    int index = pos * 7;
    for (int i = 0; i < numverts; i++, index += 7) {
      Polygons.buffer.put(index, Polygons.s1_old[i]);
    }
  }

}

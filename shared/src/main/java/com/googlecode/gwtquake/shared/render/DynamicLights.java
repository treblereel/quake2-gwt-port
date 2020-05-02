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

import com.googlecode.gwtquake.shared.common.Globals;
import com.googlecode.gwtquake.shared.game.Plane;
import com.googlecode.gwtquake.shared.util.Math3D;

/**
 * @author cwei
 */
public class DynamicLights {
  static final float[] impact = { 0, 0, 0 };
  static float[] pointcolor = { 0, 0, 0 }; // vec3_t
  static Plane lightplane; // used as shadow plane
  static float[] lightspot = { 0, 0, 0 }; // vec3_t

  /**
   * Original name: R_RenderDlights.
   */
  static void render() {
    if (GlConfig.gl_flashblend.value == 0) {
      return;
    }
    
    DynamicLights.r_dlightframecount = GlState.r_framecount + 1; // because the count
                                                           // hasn't
    // advanced yet for this frame
    GlState.gl.glDepthMask(false);
    GlState.gl.glDisable(Gl1Context.GL_TEXTURE_2D);
    GlState.gl.glShadeModel(Gl1Context.GL_SMOOTH);
    GlState.gl.glEnable(Gl1Context.GL_BLEND);
    GlState.gl.glBlendFunc(Gl1Context.GL_ONE, Gl1Context.GL_ONE);

    for (int i = 0; i < GlState.r_newrefdef.num_dlights; i++) {
      GlState.r_newrefdef.dlights[i].render(GlState.gl);
    }

    GlState.gl.glColor3f(1, 1, 1);
    GlState.gl.glDisable(Gl1Context.GL_BLEND);
    GlState.gl.glEnable(Gl1Context.GL_TEXTURE_2D);
    GlState.gl.glBlendFunc(Gl1Context.GL_SRC_ALPHA, Gl1Context.GL_ONE_MINUS_SRC_ALPHA);
    GlState.gl.glDepthMask(true);
  }

  /**
   * Original name: R_PushDlights
   */
  static void push() {
    if (GlConfig.gl_flashblend.value != 0) {
      return;
    }

    DynamicLights.r_dlightframecount = GlState.r_framecount + 1; // because the count
                                                           // hasn't
    // advanced yet for this frame
    DynamicLight l;
    for (int i = 0; i < GlState.r_newrefdef.num_dlights; i++) {
      l = GlState.r_newrefdef.dlights[i];
      l.mark(1 << i, GlState.r_worldmodel.nodes[0]);
    }
  }


  // stack variable
  static private final float[] end = { 0, 0, 0 };
  /**
   * Original name: R_LightPoint
   */
  static void R_LightPoint(float[] p, float[] color) {
    assert (p.length == 3) : "vec3_t bug";
    assert (color.length == 3) : "rgb bug";

    if (GlState.r_worldmodel.lightdata == null) {
      color[0] = color[1] = color[2] = 1.0f;
      return;
    }

    end[0] = p[0];
    end[1] = p[1];
    end[2] = p[2] - 2048;

    float r = Node.RecursiveLightPoint(GlState.r_worldmodel.nodes[0], p, end);

    if (r == -1) {
      Math3D.VectorCopy(Globals.vec3_origin, color);
    } else {
      Math3D.VectorCopy(pointcolor, color);
    }

    //
    // add dynamic lights
    //
    DynamicLight dl;
    float add;
    for (int lnum = 0; lnum < GlState.r_newrefdef.num_dlights; lnum++) {
      dl = GlState.r_newrefdef.dlights[lnum];

      Math3D.VectorSubtract(GlState.currententity.origin, dl.origin, end);
      add = dl.intensity - Math3D.VectorLength(end);
      add *= (1.0f / 256);
      if (add > 0) {
        Math3D.VectorMA(color, add, dl.color, color);
      }
    }
    Math3D.VectorScale(color, GlConfig.gl_modulate.value, color);
  }


  static int r_dlightframecount;
}

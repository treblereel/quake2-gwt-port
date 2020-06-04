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

import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.game.Plane;
import com.googlecode.gwtquake.shared.util.Math3D;

public class DynamicLight {
  public float origin[] = { 0, 0, 0 };
  public float color[] = { 0, 0, 0 };
  public float intensity;
  
  // stack variable
  private static final float[] v = {0, 0, 0};

  void render(Gl1Context gl) {
    float rad = intensity * 0.35f;

    Math3D.VectorSubtract(origin, GlState.r_origin, DynamicLight.v);

    gl.glBegin(Gl1Context.GL_TRIANGLE_FAN);
    gl.glColor3f(color[0] * 0.2f, color[1] * 0.2f, color[2] * 0.2f);
    int i;
    for (i = 0; i < 3; i++) {
      DynamicLight.v[i] = origin[i] - GlState.vpn[i] * rad;
    }

    gl.glVertex3f(DynamicLight.v[0], DynamicLight.v[1], DynamicLight.v[2]);
    gl.glColor3f(0, 0, 0);

    int j;
    float a;
    for (i = 16; i >= 0; i--) {
      a = (float) (i / 16.0f * Math.PI * 2);
      for (j = 0; j < 3; j++)
        DynamicLight.v[j] = (float) (origin[j] + GlState.vright[j]
            * Math.cos(a) * rad + GlState.vup[j] * Math.sin(a) * rad);
      gl.glVertex3f(DynamicLight.v[0], DynamicLight.v[1], DynamicLight.v[2]);
    }
    gl.glEnd();
  }

  public void mark(int bit, Node node) {
    if (node.contents != -1) {
      return;
    }
    Plane splitplane = node.plane;
    float dist = Math3D.DotProduct(origin, splitplane.normal) - splitplane.dist;
  
    if (dist > intensity - GlConstants.DLIGHT_CUTOFF) {
      mark(bit, node.children[0]);
      return;
    }
    if (dist < -intensity + GlConstants.DLIGHT_CUTOFF) {
      mark(bit, node.children[1]);
      return;
    }
  
    // mark the polygons
    Surface surf;
    int sidebit;
    for (int i = 0; i < node.numsurfaces; i++) {
      surf = GlState.r_worldmodel.surfaces[node.firstsurface + i];
  
      /*
       * cwei bugfix for dlight behind the walls
       */
      dist = Math3D.DotProduct(origin, surf.plane.normal) - surf.plane.dist;
      sidebit = (dist >= 0) ? 0 : Constants.SURF_PLANEBACK;
      if ((surf.flags & Constants.SURF_PLANEBACK) != sidebit) {
        continue;
      }
      /*
       * cwei bugfix end
       */
  
      if (surf.dlightframe != DynamicLights.r_dlightframecount) {
        surf.dlightbits = 0;
        surf.dlightframe = DynamicLights.r_dlightframecount;
      }
      surf.dlightbits |= bit;
    }
  
    mark(bit, node.children[0]);
    mark(bit, node.children[1]);
  }
}

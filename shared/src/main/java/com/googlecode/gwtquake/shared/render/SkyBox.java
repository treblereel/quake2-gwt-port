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

import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.Globals;
import com.googlecode.gwtquake.shared.common.QuakeImage;
import com.googlecode.gwtquake.shared.util.Math3D;

/**
 * Warp
 * 
 * @author cwei
 */
public class SkyBox {
  
  static float[] skyaxis = { 0, 0, 0 };
  static String skyname;
  static float skyrotate;
  static Image[] sky_images = new Image[6];
  static float[] dists = new float[GlConstants.MAX_CLIP_VERTS];

  
  /**
   * BoundPoly
   * 
   * @param numverts
   * @param verts
   * @param mins
   * @param maxs
   */
  static void BoundPoly(int numverts, float[][] verts, float[] mins,
      float[] maxs) {
    mins[0] = mins[1] = mins[2] = 9999;
    maxs[0] = maxs[1] = maxs[2] = -9999;

    int j;
    float[] v;
    for (int i = 0; i < numverts; i++) {
      v = verts[i];
      for (j = 0; j < 3; j++) {
        if (v[j] < mins[j]) {
          mins[j] = v[j];
        }
        if (v[j] > maxs[j]) {
          maxs[j] = v[j];
        }
      }
    }
  }


  

  // ===================================================================

  static float[][] skyclip = { { 1, 1, 0 }, { 1, -1, 0 }, { 0, -1, 1 },
      { 0, 1, 1 }, { 1, 0, 1 }, { -1, 0, 1 } };

  static int c_sky;

  // 1 = s, 2 = t, 3 = 2048
  static int[][] st_to_vec = { { 3, -1, 2 }, { -3, 1, 2 },

  { 1, 3, 2 }, { -1, -3, 2 },

  { -2, -1, 3 }, // 0 degrees yaw, look straight up
      { 2, -1, -3 } // look straight down

  };

  static int[][] vec_to_st = { { -2, 3, 1 }, { 2, 3, -1 },

  { 1, 3, 2 }, { -1, 3, -2 },

  { -2, -1, 3 }, { -2, 1, -3 }

  };

  static float[][] skymins = new float[2][6];
  static float[][] skymaxs = new float[2][6];
  static float sky_min, sky_max;

  // stack variable
  static float[] v = { 0, 0, 0 };
  static float[] av = { 0, 0, 0 };

  /**
   * DrawSkyPolygon
   * 
   * @param nump
   * @param vecs
   */
  static void DrawSkyPolygon(int nump, float[][] vecs) {
    c_sky++;
    // decide which face it maps to
    Math3D.VectorCopy(Globals.vec3_origin, v);
    int i, axis;
    for (i = 0; i < nump; i++) {
      Math3D.VectorAdd(vecs[i], v, v);
    }
    av[0] = Math.abs(v[0]);
    av[1] = Math.abs(v[1]);
    av[2] = Math.abs(v[2]);
    if (av[0] > av[1] && av[0] > av[2]) {
      if (v[0] < 0)
        axis = 1;
      else
        axis = 0;
    } else if (av[1] > av[2] && av[1] > av[0]) {
      if (v[1] < 0)
        axis = 3;
      else
        axis = 2;
    } else {
      if (v[2] < 0)
        axis = 5;
      else
        axis = 4;
    }

    // project new texture coords
    float s, t, dv;
    int j;
    for (i = 0; i < nump; i++) {
      j = vec_to_st[axis][2];
      if (j > 0)
        dv = vecs[i][j - 1];
      else
        dv = -vecs[i][-j - 1];
      if (dv < 0.001f)
        continue; // don't divide by zero
      j = vec_to_st[axis][0];
      if (j < 0)
        s = -vecs[i][-j - 1] / dv;
      else
        s = vecs[i][j - 1] / dv;
      j = vec_to_st[axis][1];
      if (j < 0)
        t = -vecs[i][-j - 1] / dv;
      else
        t = vecs[i][j - 1] / dv;

      if (s < skymins[0][axis])
        skymins[0][axis] = s;
      if (t < skymins[1][axis])
        skymins[1][axis] = t;
      if (s > skymaxs[0][axis])
        skymaxs[0][axis] = s;
      if (t > skymaxs[1][axis])
        skymaxs[1][axis] = t;
    }
  }

  static final int SIDE_BACK = 1;
  static final int SIDE_FRONT = 0;
  static final int SIDE_ON = 2;

  static int[] sides = new int[GlConstants.MAX_CLIP_VERTS];
  static float[][][][] newv = new float[6][2][GlConstants.MAX_CLIP_VERTS][3];

  /**
   * ClipSkyPolygon
   * 
   * @param nump
   * @param vecs
   * @param stage
   */
  static void ClipSkyPolygon(int nump, float[][] vecs, int stage) {
    if (nump > GlConstants.MAX_CLIP_VERTS - 2)
      Com.Error(Constants.ERR_DROP, "ClipSkyPolygon: MAX_CLIP_VERTS");
    if (stage == 6) { // fully clipped, so draw it
      DrawSkyPolygon(nump, vecs);
      return;
    }

    boolean front = false;
    boolean back = false;
    float[] norm = skyclip[stage];

    int i;
    float d;
    for (i = 0; i < nump; i++) {
      d = Math3D.DotProduct(vecs[i], norm);
      if (d > GlConstants.ON_EPSILON) {
        front = true;
        sides[i] = SIDE_FRONT;
      } else if (d < -GlConstants.ON_EPSILON) {
        back = true;
        sides[i] = SIDE_BACK;
      } else
        sides[i] = SIDE_ON;
      SkyBox.dists[i] = d;
    }

    if (!front || !back) { // not clipped
      ClipSkyPolygon(nump, vecs, stage + 1);
      return;
    }

    // clip it
    sides[i] = sides[0];
    SkyBox.dists[i] = SkyBox.dists[0];
    Math3D.VectorCopy(vecs[0], vecs[i]);

    int newc0 = 0;
    int newc1 = 0;
    float[] v;
    float e;
    int j;
    for (i = 0; i < nump; i++) {
      v = vecs[i];
      switch (sides[i]) {
      case SIDE_FRONT:
        Math3D.VectorCopy(v, newv[stage][0][newc0]);
        newc0++;
        break;
      case SIDE_BACK:
        Math3D.VectorCopy(v, newv[stage][1][newc1]);
        newc1++;
        break;
      case SIDE_ON:
        Math3D.VectorCopy(v, newv[stage][0][newc0]);
        newc0++;
        Math3D.VectorCopy(v, newv[stage][1][newc1]);
        newc1++;
        break;
      }

      if (sides[i] == SIDE_ON || sides[i + 1] == SIDE_ON
          || sides[i + 1] == sides[i])
        continue;

      d = SkyBox.dists[i] / (SkyBox.dists[i] - SkyBox.dists[i + 1]);
      for (j = 0; j < 3; j++) {
        e = v[j] + d * (vecs[i + 1][j] - v[j]);
        newv[stage][0][newc0][j] = e;
        newv[stage][1][newc1][j] = e;
      }
      newc0++;
      newc1++;
    }

    // continue
    ClipSkyPolygon(newc0, newv[stage][0], stage + 1);
    ClipSkyPolygon(newc1, newv[stage][1], stage + 1);
  }

  static float[][] verts = new float[GlConstants.MAX_CLIP_VERTS][3];

  /**
   * R_AddSkySurface
   */
  static void R_AddSkySurface(Surface fa) {
    // calculate vertex values for sky box
    for (Polygon p = fa.polys; p != null; p = p.next) {
      for (int i = 0; i < p.numverts; i++) {
        verts[i][0] = p.getX(i) - GlState.r_origin[0];
        verts[i][1] = p.getY(i) - GlState.r_origin[1];
        verts[i][2] = p.getZ(i) - GlState.r_origin[2];
      }
      ClipSkyPolygon(p.numverts, verts, 0);
    }
  }

  /**
   * R_ClearSkyBox
   */
  static void R_ClearSkyBox() {
    float[] skymins0 = skymins[0];
    float[] skymins1 = skymins[1];
    float[] skymaxs0 = skymaxs[0];
    float[] skymaxs1 = skymaxs[1];

    for (int i = 0; i < 6; i++) {
      skymins0[i] = skymins1[i] = 9999;
      skymaxs0[i] = skymaxs1[i] = -9999;
    }
  }

  // stack variable
  static float[] v1 = { 0, 0, 0 };
  static float[] b = { 0, 0, 0 };

  /**
   * MakeSkyVec
   * 
   * @param s
   * @param t
   * @param axis
   */
  static void MakeSkyVec(float s, float t, int axis) {
    b[0] = s * 2300;
    b[1] = t * 2300;
    b[2] = 2300;

    int j, k;
    for (j = 0; j < 3; j++) {
      k = st_to_vec[axis][j];
      if (k < 0)
        v1[j] = -b[-k - 1];
      else
        v1[j] = b[k - 1];
    }

    // avoid bilerp seam
    s = (s + 1) * 0.5f;
    t = (t + 1) * 0.5f;

    if (s < sky_min)
      s = sky_min;
    else if (s > sky_max)
      s = sky_max;
    if (t < sky_min)
      t = sky_min;
    else if (t > sky_max)
      t = sky_max;

    t = 1.0f - t;
    GlState.gl.glTexCoord2f(s, t);
    GlState.gl.glVertex3f(v1[0], v1[1], v1[2]);
  }

  static int[] skytexorder = { 0, 2, 1, 3, 4, 5 };

  /**
   * R_DrawSkyBox
   */
  static void R_DrawSkyBox() {
    int i;

    if (SkyBox.skyrotate != 0) { // check for no sky at all
      for (i = 0; i < 6; i++)
        if (skymins[0][i] < skymaxs[0][i] && skymins[1][i] < skymaxs[1][i])
          break;
      if (i == 6)
        return; // nothing visible
    }

    GlState.gl.glPushMatrix();
    GlState.gl.glTranslatef(GlState.r_origin[0], GlState.r_origin[1],
        GlState.r_origin[2]);
    GlState.gl.glRotatef(GlState.r_newrefdef.time * SkyBox.skyrotate,
        SkyBox.skyaxis[0], SkyBox.skyaxis[1], SkyBox.skyaxis[2]);

    for (i = 0; i < 6; i++) {
      if (SkyBox.skyrotate != 0) { // hack, forces full sky to draw when
                                   // rotating
        skymins[0][i] = -1;
        skymins[1][i] = -1;
        skymaxs[0][i] = 1;
        skymaxs[1][i] = 1;
      }

      if (skymins[0][i] >= skymaxs[0][i] || skymins[1][i] >= skymaxs[1][i])
        continue;

      Images.GL_Bind(SkyBox.sky_images[skytexorder[i]].texnum);

      GlState.gl.glBegin(Gl1Context._GL_QUADS);
      MakeSkyVec(skymins[0][i], skymins[1][i], i);
      MakeSkyVec(skymins[0][i], skymaxs[1][i], i);
      MakeSkyVec(skymaxs[0][i], skymaxs[1][i], i);
      MakeSkyVec(skymaxs[0][i], skymins[1][i], i);
      GlState.gl.glEnd();
    }
    GlState.gl.glPopMatrix();
  }

  // 3dstudio environment map names
  static String[] suf = { "rt", "bk", "lf", "ft", "up", "dn" };

  /**
   * R_SetSky
   * 
   * @param name
   * @param rotate
   * @param axis
   */
  static void R_SetSky(String name, float rotate, float[] axis) {
    assert (axis.length == 3) : "vec3_t bug";
    String pathname;
    SkyBox.skyname = name;

    SkyBox.skyrotate = rotate;
    Math3D.VectorCopy(axis, SkyBox.skyaxis);

    for (int i = 0; i < 6; i++) {
      // chop down rotating skies for less memory
      if (GlConfig.gl_skymip.value != 0 || SkyBox.skyrotate != 0)
        GlConfig.gl_picmip.value++;

      // Com_sprintf (pathname, sizeof(pathname), "env/%s%s.tga", skyname,
      // suf[i]);
      pathname = "env/" + SkyBox.skyname + suf[i] + ".tga";

      // gl.log("loadSky:" + pathname);

      SkyBox.sky_images[i] = Images.findTexture(pathname, QuakeImage.it_sky);

      if (SkyBox.sky_images[i] == null)
        SkyBox.sky_images[i] = GlState.r_notexture;

      if (GlConfig.gl_skymip.value != 0 || SkyBox.skyrotate != 0) { // take less
                                                                    // memory
        GlConfig.gl_picmip.value--;
        sky_min = 1.0f / 256;
        sky_max = 255.0f / 256;
      } else {
        sky_min = 1.0f / 512;
        sky_max = 511.0f / 512;
      }
    }
  }

}

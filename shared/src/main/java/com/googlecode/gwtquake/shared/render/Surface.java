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
import java.util.Arrays;

import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.game.*;
import com.googlecode.gwtquake.shared.util.Math3D;

public class Surface {

  public int visframe; // should be drawn when node is crossed

  public Plane plane;
  public int flags;

  public int firstedge; // look up in model->surfedges[], negative numbers
  public int numedges; // are backwards edges

  public short texturemins[] = { 0, 0 };
  public short extents[] = { 0, 0 };

  public int light_s, light_t; // gl lightmap coordinates
  public int dlight_s, dlight_t;
  // gl lightmap coordinates for dynamic lightmaps

  public Polygon polys; // multiple if warped
  public Surface texturechain;
  public Surface lightmapchain;

  // TODO check this
  public Texture texinfo = new Texture();

  // lighting info
  public int dlightframe;
  public int dlightbits;

  public int lightmaptexturenum;
  public byte styles[] = new byte[Constants.MAXLIGHTMAPS];
  public float cached_light[] = new float[Constants.MAXLIGHTMAPS];
  // values currently used in lightmap
  // public byte samples[]; // [numstyles*surfsize]
  public ByteBuffer samples; // [numstyles*surfsize]

    /**
     * R_BuildLightMap
     *
     * Combine and scale multiple lightmaps into the floating format in
     * blocklights
     */
    public void R_BuildLightMap(IntBuffer dest, int stride) {
      int r, g, b, a, max;
      int i, j;
      int nummaps;
      float[] bl;
      // lightstyle_t style;

      if ((texinfo.flags & (Constants.SURF_SKY | Constants.SURF_TRANS33
          | Constants.SURF_TRANS66 | Constants.SURF_WARP)) != 0)
        Com.Error(Constants.ERR_DROP,
            "R_BuildLightMap called for non-lit surface");

      int smax = (extents[0] >> 4) + 1;
      int tmax = (extents[1] >> 4) + 1;
      int size = smax * tmax;
      if (size > ((Surfaces.s_blocklights.length * Constants.SIZE_OF_FLOAT) >> 4))
        Com.Error(Constants.ERR_DROP, "Bad s_blocklights size");

      // try {
      // set to full bright if no light data
      if (samples == null) {
        // int maps;

        for (i = 0; i < size * 3; i++)
          Surfaces.s_blocklights[i] = 255;

        // TODO useless? hoz
        // for (maps = 0 ; maps < Defines.MAXLIGHTMAPS &&
        // surf.styles[maps] != (byte)255; maps++)
        // {
        // style = r_newrefdef.lightstyles[surf.styles[maps] & 0xFF];
        // }

        // goto store;
        // throw gotoStore;
      } else {
        // count the # of maps
        for (nummaps = 0; nummaps < Constants.MAXLIGHTMAPS
            && styles[nummaps] != (byte) 255; nummaps++)
          ;

        ByteBuffer lightmap = samples;
        int lightmapIndex = 0;

        // add all the lightmaps
        float scale0;
        float scale1;
        float scale2;
        if (nummaps == 1) {
          int maps;

          for (maps = 0; maps < Constants.MAXLIGHTMAPS
              && styles[maps] != (byte) 255; maps++) {
            bl = Surfaces.s_blocklights;
            int blp = 0;

            // for (i = 0; i < 3; i++)
            // scale[i] = gl_modulate.value
            // * r_newrefdef.lightstyles[surf.styles[maps] & 0xFF].rgb[i];
            scale0 = GlConfig.gl_modulate.value
                * GlState.r_newrefdef.lightstyles[styles[maps] & 0xFF].rgb[0];
            scale1 = GlConfig.gl_modulate.value
                * GlState.r_newrefdef.lightstyles[styles[maps] & 0xFF].rgb[1];
            scale2 = GlConfig.gl_modulate.value
                * GlState.r_newrefdef.lightstyles[styles[maps] & 0xFF].rgb[2];

            if (scale0 == 1.0F && scale1 == 1.0F && scale2 == 1.0F) {
              for (i = 0; i < size; i++) {
                bl[blp++] = lightmap.get(lightmapIndex++) & 0xFF;
                bl[blp++] = lightmap.get(lightmapIndex++) & 0xFF;
                bl[blp++] = lightmap.get(lightmapIndex++) & 0xFF;
              }
            } else {
              for (i = 0; i < size; i++) {
                bl[blp++] = (lightmap.get(lightmapIndex++) & 0xFF) * scale0;
                bl[blp++] = (lightmap.get(lightmapIndex++) & 0xFF) * scale1;
                bl[blp++] = (lightmap.get(lightmapIndex++) & 0xFF) * scale2;
              }
            }
            // lightmap += size*3; // skip to next lightmap
          }
        } else {
          int maps;

          // memset( s_blocklights, 0, sizeof( s_blocklights[0] ) * size *
          // 3 );

          Arrays.fill(Surfaces.s_blocklights, 0, size * 3, 0.0f);

          for (maps = 0; maps < Constants.MAXLIGHTMAPS
              && styles[maps] != (byte) 255; maps++) {
            bl = Surfaces.s_blocklights;
            int blp = 0;

            // for (i = 0; i < 3; i++)
            // scale[i] = gl_modulate.value
            // * r_newrefdef.lightstyles[surf.styles[maps] & 0xFF].rgb[i];
            scale0 = GlConfig.gl_modulate.value
                * GlState.r_newrefdef.lightstyles[styles[maps] & 0xFF].rgb[0];
            scale1 = GlConfig.gl_modulate.value
                * GlState.r_newrefdef.lightstyles[styles[maps] & 0xFF].rgb[1];
            scale2 = GlConfig.gl_modulate.value
                * GlState.r_newrefdef.lightstyles[styles[maps] & 0xFF].rgb[2];

            if (scale0 == 1.0F && scale1 == 1.0F && scale2 == 1.0F) {
              for (i = 0; i < size; i++) {
                bl[blp++] += lightmap.get(lightmapIndex++) & 0xFF;
                bl[blp++] += lightmap.get(lightmapIndex++) & 0xFF;
                bl[blp++] += lightmap.get(lightmapIndex++) & 0xFF;
              }
            } else {
              for (i = 0; i < size; i++) {
                bl[blp++] += (lightmap.get(lightmapIndex++) & 0xFF) * scale0;
                bl[blp++] += (lightmap.get(lightmapIndex++) & 0xFF) * scale1;
                bl[blp++] += (lightmap.get(lightmapIndex++) & 0xFF) * scale2;
              }
            }
            // lightmap += size*3; // skip to next lightmap
          }
        }

        // add all the dynamic lights
        if (dlightframe == GlState.r_framecount) {
          R_AddDynamicLights(this);
        }

        // label store:
        // } catch (Throwable store) {
      }

      // put into texture format
      stride -= smax;
      bl = Surfaces.s_blocklights;
      int blp = 0;

      int monolightmap = GlConfig.gl_monolightmap.string.charAt(0);

      int destp = 0;

      if (monolightmap == '0') {
        for (i = 0; i < tmax; i++, destp += stride) {
          // dest.position(destp);

          for (j = 0; j < smax; j++) {

            r = (int) bl[blp++];
            g = (int) bl[blp++];
            b = (int) bl[blp++];

            // catch negative lights
            if (r < 0)
              r = 0;
            if (g < 0)
              g = 0;
            if (b < 0)
              b = 0;

            /*
             * * determine the brightest of the three color components
             */
            if (r > g)
              max = r;
            else
              max = g;
            if (b > max)
              max = b;

            /*
             * * alpha is ONLY used for the mono lightmap case. For this reason *
             * we set it to the brightest of the color components so that * things
             * don't get too dim.
             */
            a = max;

            /*
             * * rescale all the color components if the intensity of the greatest
             * * channel exceeds 1.0
             */
            if (max > 255) {
              float t = 255.0F / max;

              r = (int) (r * t);
              g = (int) (g * t);
              b = (int) (b * t);
              a = (int) (a * t);
            }
            // if (j == 0 || i == 0 || j == smax-1 || i == tmax-1) {
            // r = g = b = a = 0;
            // }
            // r &= 0xFF; g &= 0xFF; b &= 0xFF; a &= 0xFF;
            dest.put(destp++, (a << 24) | (b << 16) | (g << 8) | r);
          }
        }
      } else {
        for (i = 0; i < tmax; i++, destp += stride) {
          // dest.position(destp);

          for (j = 0; j < smax; j++) {

            r = (int) bl[blp++];
            g = (int) bl[blp++];
            b = (int) bl[blp++];

            // catch negative lights
            if (r < 0)
              r = 0;
            if (g < 0)
              g = 0;
            if (b < 0)
              b = 0;

            /*
             * * determine the brightest of the three color components
             */
            if (r > g)
              max = r;
            else
              max = g;
            if (b > max)
              max = b;

            /*
             * * alpha is ONLY used for the mono lightmap case. For this reason *
             * we set it to the brightest of the color components so that * things
             * don't get too dim.
             */
            a = max;

            /*
             * * rescale all the color components if the intensity of the greatest
             * * channel exceeds 1.0
             */
            if (max > 255) {
              float t = 255.0F / max;

              r = (int) (r * t);
              g = (int) (g * t);
              b = (int) (b * t);
              a = (int) (a * t);
            }

            /*
             * * So if we are doing alpha lightmaps we need to set the R, G, and B
             * * components to 0 and we need to set alpha to 1-alpha.
             */
            switch (monolightmap) {
            case 'L':
            case 'I':
              r = a;
              g = b = 0;
              break;
            case 'C':
              // try faking colored lighting
              a = 255 - ((r + g + b) / 3);
              float af = a / 255.0f;
              r *= af;
              g *= af;
              b *= af;
              break;
            case 'A':
            default:
              r = g = b = 0;
              a = 255 - a;
              break;
            }
            // r &= 0xFF; g &= 0xFF; b &= 0xFF; a &= 0xFF;
            dest.put(destp++, (a << 24) | (b << 16) | (g << 8) | r);
          }
        }
      }
    }

    /*
    * ================ CalcSurfaceExtents
    *
    * Fills in s.texturemins[] and s.extents[] ================
    */
    void CalcSurfaceExtents() {
    float[] mins = { 0, 0 };
    float[] maxs = { 0, 0 };
    float val;

    int j, e;
    Vertex v;
    int[] bmins = { 0, 0 };
    int[] bmaxs = { 0, 0 };

    mins[0] = mins[1] = 999999;
    maxs[0] = maxs[1] = -99999;

    Texture tex = texinfo;

    for (int i = 0; i < numedges; i++) {
      e = Models.loadmodel.surfedges[firstedge + i];
      if (e >= 0)
        v = Models.loadmodel.vertexes[Models.loadmodel.edges[e].v[0]];
      else
        v = Models.loadmodel.vertexes[Models.loadmodel.edges[-e].v[1]];

      for (j = 0; j < 2; j++) {
        val = v.position[0] * tex.vecs[j][0] + v.position[1] * tex.vecs[j][1]
            + v.position[2] * tex.vecs[j][2] + tex.vecs[j][3];
        if (val < mins[j])
          mins[j] = val;
        if (val > maxs[j])
          maxs[j] = val;
      }
    }

    for (int i = 0; i < 2; i++) {
      bmins[i] = (int) Math.floor(mins[i] / 16);
      bmaxs[i] = (int) Math.ceil(maxs[i] / 16);

      texturemins[i] = (short) (bmins[i] * 16);
      extents[i] = (short) ((bmaxs[i] - bmins[i]) * 16);

    }
  }

    /**
     * GL_SubdivideSurface Breaks a polygon up along axial 64 unit boundaries so
     * that turbulent and sky warps can be done reasonably.
     */
    void GL_SubdivideSurface() {
      float[][] verts = tmpVerts;
      float[] vec;
      //
      // convert edges back to a normal polygon
      //
      int numverts = 0;
      for (int i = 0; i < numedges; i++) {
        int lindex = Models.loadmodel.surfedges[firstedge + i];

        if (lindex > 0) {
          vec = Models.loadmodel.vertexes[Models.loadmodel.edges[lindex].v[0]].position;
        } else {
          vec = Models.loadmodel.vertexes[Models.loadmodel.edges[-lindex].v[1]].position;
        }
        Math3D.VectorCopy(vec, verts[numverts]);
        numverts++;
      }
      Surfaces.SubdividePolygon(this, numverts, verts);
    }

    public void clear() {
    visframe = 0;
    plane.clear();
    flags = 0;

    firstedge = 0;
    numedges = 0;

    texturemins[0] = texturemins[1] = -1;
    extents[0] = extents[1] = 0;

    light_s = light_t = 0;
    dlight_s = dlight_t = 0;

    polys = null;
    texturechain = null;
    lightmapchain = null;

    // texinfo = new mtexinfo_t();
    texinfo.clear();

    dlightframe = 0;
    dlightbits = 0;

    lightmaptexturenum = 0;

    for (int i = 0; i < styles.length; i++) {
      styles[i] = 0;
    }
    for (int i = 0; i < cached_light.length; i++) {
      cached_light[i] = 0;
    }
    if (samples != null)
      samples.clear();
  }

  static final float[][] tmpVerts = new float[64][3];

    // TODO sync with jogl renderer. hoz

    /**
   * R_SetCacheState
   */
  public static void R_SetCacheState(Surface surf) {
    for (int maps = 0; maps < Constants.MAXLIGHTMAPS
        && surf.styles[maps] != (byte) 255; maps++) {
      surf.cached_light[maps] = GlState.r_newrefdef.lightstyles[surf.styles[maps] & 0xFF].white;
    }
  }

  /**
   * R_AddDynamicLights
   */
  static void R_AddDynamicLights(Surface surf) {
    int sd, td;
    float fdist, frad, fminlight;
    int s, t;
    DynamicLight dl;
    float[] pfBL;
    float fsacc, ftacc;

    int smax = (surf.extents[0] >> 4) + 1;
    int tmax = (surf.extents[1] >> 4) + 1;
    Texture tex = surf.texinfo;

    float local0, local1;
    for (int lnum = 0; lnum < GlState.r_newrefdef.num_dlights; lnum++) {
      if ((surf.dlightbits & (1 << lnum)) == 0)
        continue; // not lit by this light

      dl = GlState.r_newrefdef.dlights[lnum];
      frad = dl.intensity;
      fdist = Math3D.DotProduct(dl.origin, surf.plane.normal) - surf.plane.dist;
      frad -= Math.abs(fdist);
      // rad is now the highest intensity on the plane

      fminlight = GlConstants.DLIGHT_CUTOFF; // FIXME: make configurable?
      if (frad < fminlight)
        continue;
      fminlight = frad - fminlight;

      for (int i = 0; i < 3; i++) {
        DynamicLights.impact[i] = dl.origin[i] - surf.plane.normal[i] * fdist;
      }

      local0 = Math3D.DotProduct(DynamicLights.impact, tex.vecs[0])
          + tex.vecs[0][3] - surf.texturemins[0];
      local1 = Math3D.DotProduct(DynamicLights.impact, tex.vecs[1])
          + tex.vecs[1][3] - surf.texturemins[1];

      pfBL = Surfaces.s_blocklights;
      int pfBLindex = 0;
      for (t = 0, ftacc = 0; t < tmax; t++, ftacc += 16) {
        td = (int) (local1 - ftacc);
        if (td < 0)
          td = -td;

        for (s = 0, fsacc = 0; s < smax; s++, fsacc += 16, pfBLindex += 3) {
          sd = (int) (local0 - fsacc);

          if (sd < 0)
            sd = -sd;

          if (sd > td)
            fdist = sd + (td >> 1);
          else
            fdist = td + (sd >> 1);

          if (fdist < fminlight) {
            pfBL[pfBLindex + 0] += (frad - fdist) * dl.color[0];
            pfBL[pfBLindex + 1] += (frad - fdist) * dl.color[1];
            pfBL[pfBLindex + 2] += (frad - fdist) * dl.color[2];
          }
        }
      }
    }
  }

  /**
   * EmitWaterPolys Does a water warp on the pre-fragmented glpoly_t chain
   */
  public static void EmitWaterPolys(Surface fa) {
    float rdt = GlState.r_newrefdef.time;

    float scroll;
    if ((fa.texinfo.flags & Constants.SURF_FLOWING) != 0)
      scroll = -64
          * ((GlState.r_newrefdef.time * 0.5f) - (int) (GlState.r_newrefdef.time * 0.5f));
    else
      scroll = 0;

    int i;
    float s, t, os, ot;
    Polygon p, bp;
    for (bp = fa.polys; bp != null; bp = bp.next) {
      p = bp;

      GlState.gl.glBegin(Gl1Context.GL_TRIANGLE_FAN);
      for (i = 0; i < p.numverts; i++) {
        os = p.getS1(i);
        ot = p.getT1(i);

        s = os
            + GlConstants.SIN[(int) ((ot * 0.125f + GlState.r_newrefdef.time) * GlConstants.TURBSCALE) & 255];
        s += scroll;
        s *= (1.0f / 64);

        t = ot
            + GlConstants.SIN[(int) ((os * 0.125f + rdt) * GlConstants.TURBSCALE) & 255];
        t *= (1.0f / 64);

        GlState.gl.glTexCoord2f(s, t);
        GlState.gl.glVertex3f(p.getX(i), p.getY(i), p.getZ(i));
      }
      GlState.gl.glEnd();
    }
  }

  /**
   * GL_CreateSurfaceLightmap
   */
  static void GL_CreateSurfaceLightmap(Surface surf) {
    if ((surf.flags & (Constants.SURF_DRAWSKY | Constants.SURF_DRAWTURB)) != 0)
      return;

    int smax = (surf.extents[0] >> 4) + 1;
    int tmax = (surf.extents[1] >> 4) + 1;

    Images.pos_t lightPos = new Images.pos_t(surf.light_s, surf.light_t);

    if (!Surfaces.LM_AllocBlock(smax, tmax, lightPos)) {
      Surfaces.LM_UploadBlock(false);
      Surfaces.LM_InitBlock();
      lightPos = new Images.pos_t(surf.light_s, surf.light_t);
      if (!Surfaces.LM_AllocBlock(smax, tmax, lightPos)) {
        Com.Error(Constants.ERR_FATAL, "Consecutive calls to LM_AllocBlock("
            + smax + "," + tmax + ") failed\n");
      }
    }

    // kopiere die koordinaten zurueck
    surf.light_s = lightPos.x;
    surf.light_t = lightPos.y;

    surf.lightmaptexturenum = Surfaces.gl_lms.current_lightmap_texture;

    IntBuffer base = Surfaces.gl_lms.lightmap_buffer;
    base.position(surf.light_t * Surfaces.BLOCK_WIDTH + surf.light_s);

    Surface.R_SetCacheState(surf);
    surf.R_BuildLightMap(base.slice(), Surfaces.BLOCK_WIDTH);
  }

  /**
   * GL_BuildPolygonFromSurface
   */
  static void GL_BuildPolygonFromSurface(Surface fa) {
    // reconstruct the polygon
    Edge[] pedges = GlState.currentmodel.edges;
    int lnumverts = fa.numedges;
    //
    // draw texture
    //
    // poly = Hunk_Alloc (sizeof(glpoly_t) + (lnumverts-4) *
    // VERTEXSIZE*sizeof(float));
    Polygon poly = Polygons.create(lnumverts);

    poly.next = fa.polys;
    poly.flags = fa.flags;
    fa.polys = poly;

    int lindex;
    float[] vec;
    Edge r_pedge;
    float s, t;
    for (int i = 0; i < lnumverts; i++) {
      lindex = GlState.currentmodel.surfedges[fa.firstedge + i];

      if (lindex > 0) {
        r_pedge = pedges[lindex];
        vec = GlState.currentmodel.vertexes[r_pedge.v[0]].position;
      } else {
        r_pedge = pedges[-lindex];
        vec = GlState.currentmodel.vertexes[r_pedge.v[1]].position;
      }

      // if(!fa.texinfo.image.complete) {
      // gl.log("building surface with bad texture coordinates");
      // }

      s = Math3D.DotProduct(vec, fa.texinfo.vecs[0]) + fa.texinfo.vecs[0][3];
      s /= fa.texinfo.image.width;

      t = Math3D.DotProduct(vec, fa.texinfo.vecs[1]) + fa.texinfo.vecs[1][3];
      t /= fa.texinfo.image.height;

      poly.setX(i, vec[0]);
      poly.setY(i, vec[1]);
      poly.setZ(i, vec[2]);

      poly.setS1(i, s);
      poly.setT1(i, t);

      //
      // lightmap texture coordinates
      //
      s = Math3D.DotProduct(vec, fa.texinfo.vecs[0]) + fa.texinfo.vecs[0][3];
      s -= fa.texturemins[0];
      s += fa.light_s * 16;
      s += 8;
      s /= Surfaces.BLOCK_WIDTH * 16; // fa.texinfo.texture.width;

      t = Math3D.DotProduct(vec, fa.texinfo.vecs[1]) + fa.texinfo.vecs[1][3];
      t -= fa.texturemins[1];
      t += fa.light_t * 16;
      t += 8;
      t /= Surfaces.BLOCK_HEIGHT * 16; // fa.texinfo.texture.height;

      poly.setS2(i, s);
      poly.setT2(i, t);
    }
  }

  /**
   * GL_RenderLightmappedPoly
   * 
   * @param surf
   */
  static void GL_RenderLightmappedPoly(Surface surf) {

    // ersetzt goto
    boolean gotoDynamic = false;
    int map;
    for (map = 0; map < Constants.MAXLIGHTMAPS
        && (surf.styles[map] != (byte) 255); map++) {
      if (GlState.r_newrefdef.lightstyles[surf.styles[map] & 0xFF].white != surf.cached_light[map]) {
        gotoDynamic = true;
        break;
      }
    }

    // this is a hack from cwei
    if (map == 4)
      map--;

    // dynamic this frame or dynamic previously
    boolean is_dynamic = false;
    if (gotoDynamic || (surf.dlightframe == GlState.r_framecount)) {
      // label dynamic:
      if (GlConfig.gl_dynamic.value != 0) {
        if ((surf.texinfo.flags & (Constants.SURF_SKY | Constants.SURF_TRANS33
            | Constants.SURF_TRANS66 | Constants.SURF_WARP)) == 0) {
          is_dynamic = true;
        }
      }
    }

    Polygon p;
    Image image = Texture.R_TextureAnimation(surf.texinfo);
    int lmtex = surf.lightmaptexturenum;

    if (is_dynamic) {
      // ist raus gezogen worden int[] temp = new int[128*128];
      int smax = (surf.extents[0] >> 4) + 1;
      int tmax = (surf.extents[1] >> 4) + 1;

      surf.R_BuildLightMap(Surfaces.temp, smax);
      if (((surf.styles[map] & 0xFF) >= 32 || surf.styles[map] == 0)
          && (surf.dlightframe != GlState.r_framecount)) {
        Surface.R_SetCacheState(surf);
        lmtex = surf.lightmaptexturenum;
      } else {
        lmtex = 0;
      }
      Images.GL_MBind(Gl1Context.GL_TEXTURE1,
          GlConfig.gl_state.lightmap_textures + lmtex);
      GlState.gl.glTexSubImage2D(Gl1Context.GL_TEXTURE_2D, 0, surf.light_s,
          surf.light_t, smax, tmax, Surfaces.GL_LIGHTMAP_FORMAT,
          Gl1Context.GL_UNSIGNED_BYTE, Surfaces.temp);

    }
    GlState.c_brush_polys++;

    Images.GL_MBind(Gl1Context.GL_TEXTURE0, image.texnum);
    Images.GL_MBind(Gl1Context.GL_TEXTURE1, GlConfig.gl_state.lightmap_textures
        + lmtex);

    // ==========
    // PGM
    if ((surf.texinfo.flags & Constants.SURF_FLOWING) != 0) {
      float scroll;

      scroll = -64
          * ((GlState.r_newrefdef.time / 40.0f) - (int) (GlState.r_newrefdef.time / 40.0f));
      if (scroll == 0.0f)
        scroll = -64.0f;

      for (p = surf.polys; p != null; p = p.chain) {
        p.beginScrolling(scroll);
        GlState.gl.glDrawArrays(Gl1Context._GL_POLYGON, p.pos, p.numverts);
        p.endScrolling();
      }
    } else {
      for (p = surf.polys; p != null; p = p.chain) {
        GlState.gl.glDrawArrays(Gl1Context._GL_POLYGON, p.pos, p.numverts);
      }
    }
    // PGM
    // ==========
    // }
    // else
    // {
    // c_brush_polys++;
    //
    // GL_MBind( GL_TEXTURE0, image.texnum );
    // GL_MBind( GL_TEXTURE1, gl_state.lightmap_textures + lmtex);
    //
    // // ==========
    // // PGM
    // if ((surf.texinfo.flags & Defines.SURF_FLOWING) != 0)
    // {
    // float scroll;
    //
    // scroll = -64 * ( (r_newrefdef.time / 40.0f) - (int)(r_newrefdef.time /
    // 40.0f) );
    // if(scroll == 0.0)
    // scroll = -64.0f;
    //
    // for ( p = surf.polys; p != null; p = p.chain )
    // {
    // p.beginScrolling(scroll);
    // gl.glDrawArrays(GLAdapter._GL_POLYGON, p.pos, p.numverts);
    // p.endScrolling();
    // }
    // }
    // else
    // {
    // // PGM
    // // ==========
    // for ( p = surf.polys; p != null; p = p.chain )
    // {
    // gl.glDrawArrays(GLAdapter._GL_POLYGON, p.pos, p.numverts);
    // }
    //
    // // ==========
    // // PGM
    // }
    // // PGM
    // // ==========
    // }
  }

  /**
   * R_RenderBrushPoly
   */
  public static void R_RenderBrushPoly(Surface fa) {
    GlState.c_brush_polys++;

    Image image = Texture.R_TextureAnimation(fa.texinfo);

    if ((fa.flags & Constants.SURF_DRAWTURB) != 0) {
      Images.GL_Bind(image.texnum);

      // warp texture, no lightmaps
      Images.GL_TexEnv(Gl1Context.GL_MODULATE);
      GlState.gl.glColor4f(GlState.inverse_intensity,
          GlState.inverse_intensity, GlState.inverse_intensity, 1.0F);
      Surface.EmitWaterPolys(fa);
      Images.GL_TexEnv(Gl1Context.GL_REPLACE);

      return;
    } else {
      Images.GL_Bind(image.texnum);
      Images.GL_TexEnv(Gl1Context.GL_REPLACE);
    }

    // ======
    // PGM
    if ((fa.texinfo.flags & Constants.SURF_FLOWING) != 0)
      fa.polys.drawFlowing();
    else
      fa.polys.draw();
    // PGM
    // ======

    // ersetzt goto
    boolean gotoDynamic = false;
    /*
     * * check for lightmap modification
     */
    int maps;
    for (maps = 0; maps < Constants.MAXLIGHTMAPS
        && fa.styles[maps] != (byte) 255; maps++) {
      if (GlState.r_newrefdef.lightstyles[fa.styles[maps] & 0xFF].white != fa.cached_light[maps]) {
        gotoDynamic = true;
        break;
      }
    }

    // this is a hack from cwei
    if (maps == 4)
      maps--;

    // dynamic this frame or dynamic previously
    boolean is_dynamic = false;
    if (gotoDynamic || (fa.dlightframe == GlState.r_framecount)) {
      // label dynamic:
      if (GlConfig.gl_dynamic.value != 0) {
        if ((fa.texinfo.flags & (Constants.SURF_SKY | Constants.SURF_TRANS33
            | Constants.SURF_TRANS66 | Constants.SURF_WARP)) == 0) {
          is_dynamic = true;
        }
      }
    }

    if (is_dynamic) {
      if (((fa.styles[maps] & 0xFF) >= 32 || fa.styles[maps] == 0)
          && (fa.dlightframe != GlState.r_framecount)) {
        // ist ersetzt durch temp2: unsigned temp[34*34];
        int smax, tmax;

        smax = (fa.extents[0] >> 4) + 1;
        tmax = (fa.extents[1] >> 4) + 1;

        fa.R_BuildLightMap(Surfaces.temp2, smax);
        Surface.R_SetCacheState(fa);

        Images.GL_Bind(GlState.lightmap_textures + fa.lightmaptexturenum);

        GlState.gl.glTexSubImage2D(Gl1Context.GL_TEXTURE_2D, 0, fa.light_s,
            fa.light_t, smax, tmax, Surfaces.GL_LIGHTMAP_FORMAT,
            Gl1Context.GL_UNSIGNED_BYTE, Surfaces.temp2);

        fa.lightmapchain = Surfaces.gl_lms.lightmap_surfaces[fa.lightmaptexturenum];
        Surfaces.gl_lms.lightmap_surfaces[fa.lightmaptexturenum] = fa;
      } else {
        fa.lightmapchain = Surfaces.gl_lms.lightmap_surfaces[0];
        Surfaces.gl_lms.lightmap_surfaces[0] = fa;
      }
    } else {
      fa.lightmapchain = Surfaces.gl_lms.lightmap_surfaces[fa.lightmaptexturenum];
      Surfaces.gl_lms.lightmap_surfaces[fa.lightmaptexturenum] = fa;
    }
  }
}

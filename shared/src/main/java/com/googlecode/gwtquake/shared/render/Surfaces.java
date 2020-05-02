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

import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import com.googlecode.gwtquake.shared.client.EntityType;
import com.googlecode.gwtquake.shared.client.Lightstyle;
import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.game.Plane;
import com.googlecode.gwtquake.shared.util.Lib;
import com.googlecode.gwtquake.shared.util.Math3D;
import com.googlecode.gwtquake.shared.util.Vec3Cache;
import elemental2.dom.DomGlobal;

/**
 * Surf
 * 
 * @author cwei
 */
public abstract class Surfaces {

  // GL_RSURF.C: surface-related refresh code
  static float[] modelorg = { 0, 0, 0 }; // relative to viewpoint

  static Surface r_alpha_surfaces;

  static final int DYNAMIC_LIGHT_WIDTH = 128;
  static final int DYNAMIC_LIGHT_HEIGHT = 128;

  static final int LIGHTMAP_BYTES = 4;

  static final int BLOCK_WIDTH = 128;
  static final int BLOCK_HEIGHT = 128;

  static final int MAX_LIGHTMAPS = 128;

  static int c_visible_lightmaps;
  static int c_visible_textures;

  static int staticBufferId;

  public static final int GL_LIGHTMAP_FORMAT = Gl1Context.GL_RGBA;

  public static class LightMapState {
    int internal_format;
    int current_lightmap_texture;

    public Surface[] lightmap_surfaces = new Surface[MAX_LIGHTMAPS];
    int[] allocated = new int[BLOCK_WIDTH];

    // the lightmap texture data needs to be kept in
    // main memory so texsubimage can update properly
    // byte[] lightmap_buffer = new byte[4 * BLOCK_WIDTH * BLOCK_HEIGHT];
    IntBuffer lightmap_buffer = 
        Lib.newIntBuffer(BLOCK_WIDTH * BLOCK_HEIGHT, ByteOrder.LITTLE_ENDIAN);

    public LightMapState() {
      for (int i = 0; i < MAX_LIGHTMAPS; i++)
        lightmap_surfaces[i] = new Surface();
    }

    public void clearLightmapSurfaces() {
      for (int i = 0; i < MAX_LIGHTMAPS; i++)
        // TODO lightmap_surfaces[i].clear();
        lightmap_surfaces[i] = new Surface();
    }
  }

  public static LightMapState gl_lms = new LightMapState();

  /*
   * =============================================================
   * 
   * BRUSH MODELS
   * 
   * =============================================================
   */

  /**
   * R_DrawTriangleOutlines
   */
  static void R_DrawTriangleOutlines() {
    if (GlConfig.gl_showtris.value == 0)
      return;

    GlState.gl.glDisable(Gl1Context.GL_TEXTURE_2D);
    GlState.gl.glDisable(Gl1Context.GL_DEPTH_TEST);
    GlState.gl.glColor4f(1, 1, 1, 1);

    Surface surf;
    Polygon p;
    int j;
    for (int i = 0; i < MAX_LIGHTMAPS; i++) {
      for (surf = gl_lms.lightmap_surfaces[i]; surf != null; surf = surf.lightmapchain) {
        for (p = surf.polys; p != null; p = p.chain) {
          for (j = 2; j < p.numverts; j++) {
            GlState.gl.glBegin(Gl1Context.GL_LINE_STRIP);
            GlState.gl.glVertex3f(p.getX(0), p.getY(0), p.getZ(0));
            GlState.gl.glVertex3f(p.getX(j - 1), p.getY(j - 1), p.getZ(j - 1));
            GlState.gl.glVertex3f(p.getX(j), p.getY(j), p.getZ(j));
            GlState.gl.glVertex3f(p.getX(0), p.getY(0), p.getZ(0));
            GlState.gl.glEnd();
          }
        }
      }
    }

    GlState.gl.glEnable(Gl1Context.GL_DEPTH_TEST);
    GlState.gl.glEnable(Gl1Context.GL_TEXTURE_2D);
  }

  public static final IntBuffer temp2 = Lib.newIntBuffer(34 * 34,
      ByteOrder.LITTLE_ENDIAN);

  /**
   * R_DrawAlphaSurfaces Draw water surfaces and windows. The BSP tree is waled
   * front to back, so unwinding the chain of alpha_surfaces will draw back to
   * front, giving proper ordering.
   */
  static void R_DrawAlphaSurfaces() {
    GlState.r_world_matrix.clear();
    //
    // go back to the world matrix
    //
    GlState.gl.glLoadMatrix(GlState.r_world_matrix);

    GlState.gl.glEnable(Gl1Context.GL_BLEND);
    Images.GL_TexEnv(Gl1Context.GL_MODULATE);

    // the textures are prescaled up for a better lighting range,
    // so scale it back down
    float intens = GlConfig.gl_state.inverse_intensity;

    glInterleavedArraysT2F_V3F(Polygons.BYTE_STRIDE,
        globalPolygonInterleavedBuf, staticBufferId);

    for (Surface s = r_alpha_surfaces; s != null; s = s.texturechain) {
      Images.GL_Bind(s.texinfo.image.texnum);
      GlState.c_brush_polys++;
      if ((s.texinfo.flags & Constants.SURF_TRANS33) != 0)
        GlState.gl.glColor4f(intens, intens, intens, 0.33f);
      else if ((s.texinfo.flags & Constants.SURF_TRANS66) != 0)
        GlState.gl.glColor4f(intens, intens, intens, 0.66f);
      else
        GlState.gl.glColor4f(intens, intens, intens, 1);
      if ((s.flags & Constants.SURF_DRAWTURB) != 0)
        Surface.EmitWaterPolys(s);
      else if ((s.texinfo.flags & Constants.SURF_FLOWING) != 0) // PGM 9/16/98
        s.polys.drawFlowing(); // PGM
      else
        s.polys.draw();
    }

    Images.GL_TexEnv(Gl1Context.GL_REPLACE);
    GlState.gl.glColor4f(1, 1, 1, 1);
    GlState.gl.glDisable(Gl1Context.GL_BLEND);

    r_alpha_surfaces = null;
  }

  private static void glInterleavedArraysT2F_V3F(int byteStride, FloatBuffer buf) {
    int pos = buf.position();
    GlState.gl.glTexCoordPointer(2, byteStride, buf);
    GlState.gl.glEnableClientState(Gl1Context.GL_TEXTURE_COORD_ARRAY);

    buf.position(pos + 2);
    GlState.gl.glVertexPointer(3, byteStride, buf);
    GlState.gl.glEnableClientState(Gl1Context.GL_VERTEX_ARRAY);

    buf.position(pos);
  }

  private static void glInterleavedArraysT2F_V3F(int byteStride,
      FloatBuffer buf, int staticDrawIdV) {
    GlState.gl.glEnableClientState(Gl1Context.GL_TEXTURE_COORD_ARRAY);
    GlState.gl.glVertexAttribPointer(Gl1Context.ARRAY_TEXCOORD_0, 2,
        Gl1Context.GL_FLOAT, false, byteStride, 0, buf, staticDrawIdV);

    GlState.gl.glEnableClientState(Gl1Context.GL_VERTEX_ARRAY);
    // gl.glVertexPointer(3, byteStride, buf);
    GlState.gl.glVertexAttribPointer(Gl1Context.ARRAY_POSITION, 3,
        Gl1Context.GL_FLOAT, false, byteStride, 8, buf, staticDrawIdV);
  }

  /**
   * DrawTextureChains
   */
  static void DrawTextureChains() {
    c_visible_textures = 0;

    Surface s;
    Image image;
    int i;
    for (i = 0; i < Images.numgltextures; i++) {
      image = Images.gltextures[i];

      if (image.registration_sequence == 0)
        continue;
      if (image.texturechain == null)
        continue;
      c_visible_textures++;

      for (s = image.texturechain; s != null; s = s.texturechain) {
        if ((s.flags & Constants.SURF_DRAWTURB) == 0)
          Surface.R_RenderBrushPoly(s);
      }
    }

    Images.GL_EnableMultitexture(false);
    for (i = 0; i < Images.numgltextures; i++) {
      image = Images.gltextures[i];

      if (image.registration_sequence == 0)
        continue;
      s = image.texturechain;
      if (s == null)
        continue;

      for (; s != null; s = s.texturechain) {
        if ((s.flags & Constants.SURF_DRAWTURB) != 0)
          Surface.R_RenderBrushPoly(s);
      }

      image.texturechain = null;
    }

    Images.GL_TexEnv(Gl1Context.GL_REPLACE);
  }

  // direct buffer
  static final IntBuffer temp = Lib.newIntBuffer(128 * 128,
      ByteOrder.LITTLE_ENDIAN);

  /**
   * R_DrawInlineBModel
   */
  static void R_DrawInlineBModel() {
    // calculate dynamic lighting for bmodel
    if (GlConfig.gl_flashblend.value == 0) {
      DynamicLight lt;
      for (int k = 0; k < GlState.r_newrefdef.num_dlights; k++) {
        lt = GlState.r_newrefdef.dlights[k];
        lt.mark(1 << k,
            GlState.currentmodel.nodes[GlState.currentmodel.firstnode]);
      }
    }

    // psurf = &currentmodel->surfaces[currentmodel->firstmodelsurface];
    int psurfp = GlState.currentmodel.firstmodelsurface;
    Surface[] surfaces = GlState.currentmodel.surfaces;
    // psurf = surfaces[psurfp];

    if ((GlState.currententity.flags & Constants.RF_TRANSLUCENT) != 0) {
      GlState.gl.glEnable(Gl1Context.GL_BLEND);
      GlState.gl.glColor4f(1, 1, 1, 0.25f);
      Images.GL_TexEnv(Gl1Context.GL_MODULATE);
    }

    //
    // draw texture
    //
    Surface psurf;
    Plane pplane;
    float dot;
    for (int i = 0; i < GlState.currentmodel.nummodelsurfaces; i++) {
      psurf = surfaces[psurfp++];
      // find which side of the node we are on
      pplane = psurf.plane;

      dot = Math3D.DotProduct(modelorg, pplane.normal) - pplane.dist;

      // draw the polygon
      if (((psurf.flags & Constants.SURF_PLANEBACK) != 0 && (dot < -GlConstants.BACKFACE_EPSILON))
          || ((psurf.flags & Constants.SURF_PLANEBACK) == 0 && (dot > GlConstants.BACKFACE_EPSILON))) {
        if ((psurf.texinfo.flags & (Constants.SURF_TRANS33 | Constants.SURF_TRANS66)) != 0) { // add
                                                                                              // to
                                                                                              // the
                                                                                              // translucent
                                                                                              // chain
          psurf.texturechain = r_alpha_surfaces;
          r_alpha_surfaces = psurf;
        } else if ((psurf.flags & Constants.SURF_DRAWTURB) == 0) {
          Surface.GL_RenderLightmappedPoly(psurf);
        } else {
          Images.GL_EnableMultitexture(false);
          Surface.R_RenderBrushPoly(psurf);
          Images.GL_EnableMultitexture(true);
        }
      }
    }

    if ((GlState.currententity.flags & Constants.RF_TRANSLUCENT) != 0) {
      GlState.gl.glDisable(Gl1Context.GL_BLEND);
      GlState.gl.glColor4f(1, 1, 1, 1);
      Images.GL_TexEnv(Gl1Context.GL_REPLACE);
    }
  }

  // stack variable
  private static final float[] mins = { 0, 0, 0 };
  private static final float[] maxs = { 0, 0, 0 };
  private static final float[] org = { 0, 0, 0 };
  private static final float[] forward = { 0, 0, 0 };
  private static final float[] right = { 0, 0, 0 };
  private static final float[] up = { 0, 0, 0 };

  /**
   * R_DrawBrushModel
   */
  static void R_DrawBrushModel(EntityType e) {
    if (GlState.currentmodel.nummodelsurfaces == 0)
      return;

    GlState.currententity = e;
    GlConfig.gl_state.currenttextures[0] = GlConfig.gl_state.currenttextures[1] = -1;

    boolean rotated;
    if (e.angles[0] != 0 || e.angles[1] != 0 || e.angles[2] != 0) {
      rotated = true;
      for (int i = 0; i < 3; i++) {
        mins[i] = e.origin[i] - GlState.currentmodel.radius;
        maxs[i] = e.origin[i] + GlState.currentmodel.radius;
      }
    } else {
      rotated = false;
      Math3D.VectorAdd(e.origin, GlState.currentmodel.mins, mins);
      Math3D.VectorAdd(e.origin, GlState.currentmodel.maxs, maxs);
    }

    if (Entities.R_CullBox(mins, maxs))
      return;

    GlState.gl.glColor3f(1, 1, 1);

    // memset (gl_lms.lightmap_surfaces, 0, sizeof(gl_lms.lightmap_surfaces));

    // TODO wird beim multitexturing nicht gebraucht
    // gl_lms.clearLightmapSurfaces();

    Math3D.VectorSubtract(GlState.r_newrefdef.vieworg, e.origin, modelorg);
    if (rotated) {
      Math3D.VectorCopy(modelorg, org);
      Math3D.AngleVectors(e.angles, forward, right, up);
      modelorg[0] = Math3D.DotProduct(org, forward);
      modelorg[1] = -Math3D.DotProduct(org, right);
      modelorg[2] = Math3D.DotProduct(org, up);
    }

    GlState.gl.glPushMatrix();

    e.angles[0] = -e.angles[0]; // stupid quake bug
    e.angles[2] = -e.angles[2]; // stupid quake bug
    Entities.rotateForEntity(e);
    e.angles[0] = -e.angles[0]; // stupid quake bug
    e.angles[2] = -e.angles[2]; // stupid quake bug

    Images.GL_EnableMultitexture(true);
    Images.GL_SelectTexture(Gl1Context.GL_TEXTURE0);
    Images.GL_TexEnv(Gl1Context.GL_REPLACE);
    glInterleavedArraysT2F_V3F(Polygons.BYTE_STRIDE,
        globalPolygonInterleavedBuf, staticBufferId);
    Images.GL_SelectTexture(Gl1Context.GL_TEXTURE1);
    Images.GL_TexEnv(Gl1Context.GL_MODULATE);
    // gl.glTexCoordPointer(2, Polygon.BYTE_STRIDE, globalPolygonTexCoord1Buf);
    GlState.gl.glEnableClientState(Gl1Context.GL_TEXTURE_COORD_ARRAY);
    GlState.gl.glVertexAttribPointer(Gl1Context.ARRAY_TEXCOORD_1, 2,
        Gl1Context.GL_FLOAT, false, Polygons.BYTE_STRIDE, 20,
        globalPolygonInterleavedBuf, staticBufferId);

    R_DrawInlineBModel();

    GlState.gl.glClientActiveTexture(Gl1Context.GL_TEXTURE1);
    GlState.gl.glDisableClientState(Gl1Context.GL_TEXTURE_COORD_ARRAY);

    Images.GL_EnableMultitexture(false);

    GlState.gl.glPopMatrix();
  }

  /*
   * =============================================================
   * 
   * WORLD MODEL
   * 
   * =============================================================
   */

  private static final EntityType worldEntity = new EntityType();

  /**
   * R_DrawWorld
   */
  static void R_DrawWorld() {
    if (GlConfig.r_drawworld.value == 0)
      return;

    if ((GlState.r_newrefdef.rdflags & Constants.RDF_NOWORLDMODEL) != 0)
      return;

    GlState.currentmodel = GlState.r_worldmodel;

    Math3D.VectorCopy(GlState.r_newrefdef.vieworg, modelorg);

    EntityType ent = worldEntity;
    // auto cycle the world frame for texture animation
    ent.clear();
    ent.frame = (int) (GlState.r_newrefdef.time * 2);
    GlState.currententity = ent;

    GlConfig.gl_state.currenttextures[0] = GlConfig.gl_state.currenttextures[1] = -1;

    GlState.gl.glColor3f(1, 1, 1);
    // memset (gl_lms.lightmap_surfaces, 0, sizeof(gl_lms.lightmap_surfaces));
    // TODO wird bei multitexture nicht gebraucht
    // gl_lms.clearLightmapSurfaces();

    SkyBox.R_ClearSkyBox();

    Images.GL_EnableMultitexture(true);

    Images.GL_SelectTexture(Gl1Context.GL_TEXTURE0);
    Images.GL_TexEnv(Gl1Context.GL_REPLACE);

    // glInterleavedArraysT2F_V3F(Polygon.BYTE_STRIDE,
    // globalPolygonInterleavedBuf);
    glInterleavedArraysT2F_V3F(Polygons.BYTE_STRIDE,
        globalPolygonInterleavedBuf, staticBufferId);

    Images.GL_SelectTexture(Gl1Context.GL_TEXTURE1);
    GlState.gl.glEnableClientState(Gl1Context.GL_TEXTURE_COORD_ARRAY);
    GlState.gl.glVertexAttribPointer(Gl1Context.ARRAY_TEXCOORD_1, 2,
        Gl1Context.GL_FLOAT, false, Polygons.BYTE_STRIDE, 20,
        globalPolygonInterleavedBuf, staticBufferId);
    // gl.glTexCoordPointer(2, Polygon.BYTE_STRIDE, globalPolygonTexCoord1Buf);

    if (GlConfig.gl_lightmap.value != 0)
      Images.GL_TexEnv(Gl1Context.GL_REPLACE);
    else
      Images.GL_TexEnv(Gl1Context.GL_MODULATE);

    Node.R_RecursiveWorldNode(GlState.r_worldmodel.nodes[0]); // root node

    GlState.gl.glClientActiveTexture(Gl1Context.GL_TEXTURE1);
    GlState.gl.glDisableClientState(Gl1Context.GL_TEXTURE_COORD_ARRAY);

    Images.GL_EnableMultitexture(false);

    DrawTextureChains();
    SkyBox.R_DrawSkyBox();
    R_DrawTriangleOutlines();
  }

  final static byte[] fatvis = new byte[Constants.MAX_MAP_LEAFS / 8];

  /**
   * R_MarkLeaves Mark the leaves and nodes that are in the PVS for the current
   * cluster
   */
  static void R_MarkLeaves() {
    if (GlState.r_oldviewcluster == GlState.r_viewcluster
        && GlState.r_oldviewcluster2 == GlState.r_viewcluster2
        && GlConfig.r_novis.value == 0 && GlState.r_viewcluster != -1)
      return;

    // development aid to let you run around and see exactly where
    // the pvs ends
    if (GlConfig.gl_lockpvs.value != 0)
      return;

    GlState.r_visframecount++;
    GlState.r_oldviewcluster = GlState.r_viewcluster;
    GlState.r_oldviewcluster2 = GlState.r_viewcluster2;

    int i;
    if (GlConfig.r_novis.value != 0 || GlState.r_viewcluster == -1
        || GlState.r_worldmodel.vis == null) {
      // mark everything
      for (i = 0; i < GlState.r_worldmodel.numleafs; i++)
        GlState.r_worldmodel.leafs[i].visframe = GlState.r_visframecount;
      for (i = 0; i < GlState.r_worldmodel.numnodes; i++)
        GlState.r_worldmodel.nodes[i].visframe = GlState.r_visframecount;
      return;
    }

    byte[] vis = Models.Mod_ClusterPVS(GlState.r_viewcluster,
        GlState.r_worldmodel);
    int c;
    // may have to combine two clusters because of solid water boundaries
    if (GlState.r_viewcluster2 != GlState.r_viewcluster) {
      // memcpy (fatvis, vis, (r_worldmodel.numleafs+7)/8);
      System.arraycopy(vis, 0, fatvis, 0,
          (GlState.r_worldmodel.numleafs + 7) >> 3);
      vis = Models.Mod_ClusterPVS(GlState.r_viewcluster2, GlState.r_worldmodel);
      c = (GlState.r_worldmodel.numleafs + 31) >> 5;
      c <<= 2;
      for (int k = 0; k < c; k += 4) {
        fatvis[k] |= vis[k];
        fatvis[k + 1] |= vis[k + 1];
        fatvis[k + 2] |= vis[k + 2];
        fatvis[k + 3] |= vis[k + 3];
      }

      vis = fatvis;
    }

    Node node;
    Leaf leaf;
    int cluster;
    for (i = 0; i < GlState.r_worldmodel.numleafs; i++) {
      leaf = GlState.r_worldmodel.leafs[i];
      cluster = leaf.cluster;
      if (cluster == -1)
        continue;
      if (((vis[cluster >> 3] & 0xFF) & (1 << (cluster & 7))) != 0) {
        node = (Node) leaf;
        do {
          if (node.visframe == GlState.r_visframecount)
            break;
          node.visframe = GlState.r_visframecount;
          node = node.parent;
        } while (node != null);
      }
    }
  }

  /*
   * ============================================================================
   * =
   * 
   * LIGHTMAP ALLOCATION
   * 
   * ============================================================================
   * =
   */

  /**
   * LM_InitBlock
   */
  static void LM_InitBlock() {
    Arrays.fill(gl_lms.allocated, 0);
  }

  /**
   * LM_UploadBlock
   * 
   * @param dynamic
   */
  static void LM_UploadBlock(boolean dynamic) {
    int texture = (dynamic) ? 0 : gl_lms.current_lightmap_texture;

    Images.GL_Bind(GlConfig.gl_state.lightmap_textures + texture);
    GlState.gl.glTexParameterf(Gl1Context.GL_TEXTURE_2D,
        Gl1Context.GL_TEXTURE_MIN_FILTER, Gl1Context.GL_LINEAR);
    GlState.gl.glTexParameterf(Gl1Context.GL_TEXTURE_2D,
        Gl1Context.GL_TEXTURE_MAG_FILTER, Gl1Context.GL_LINEAR);

    gl_lms.lightmap_buffer.rewind();
    if (dynamic) {
      int height = 0;
      for (int i = 0; i < BLOCK_WIDTH; i++) {
        if (gl_lms.allocated[i] > height)
          height = gl_lms.allocated[i];
      }

      GlState.gl.glTexSubImage2D(Gl1Context.GL_TEXTURE_2D, 0, 0, 0,
          BLOCK_WIDTH, height, GL_LIGHTMAP_FORMAT, Gl1Context.GL_UNSIGNED_BYTE,
          gl_lms.lightmap_buffer);
    } else {
      GlState.gl.glTexImage2D(Gl1Context.GL_TEXTURE_2D, 0,
          GL_LIGHTMAP_FORMAT/* gl_lms.internal_format */, BLOCK_WIDTH,
          BLOCK_HEIGHT, 0, GL_LIGHTMAP_FORMAT, Gl1Context.GL_UNSIGNED_BYTE,
          gl_lms.lightmap_buffer);
      if (++gl_lms.current_lightmap_texture == MAX_LIGHTMAPS)
        Com.Error(Constants.ERR_DROP,
            "LM_UploadBlock() - MAX_LIGHTMAPS exceeded\n");

      // debugLightmap(gl_lms.lightmap_buffer, 128, 128, 4);
    }
  }

  /**
   * LM_AllocBlock
   * 
   * @param w
   * @param h
   * @param pos
   * @return a texture number and the position inside it
   */
  static boolean LM_AllocBlock(int w, int h, Images.pos_t pos) {
    int best = BLOCK_HEIGHT;
    int x = pos.x;

    int best2;
    int i, j;
    for (i = 0; i < BLOCK_WIDTH - w; i++) {
      best2 = 0;

      for (j = 0; j < w; j++) {
        if (gl_lms.allocated[i + j] >= best)
          break;
        if (gl_lms.allocated[i + j] > best2)
          best2 = gl_lms.allocated[i + j];
      }
      if (j == w) { // this is a valid spot
        pos.x = x = i;
        pos.y = best = best2;
      }
    }

    if (best + h > BLOCK_HEIGHT)
      return false;

    for (i = 0; i < w; i++)
      gl_lms.allocated[x + i] = best + h;

    return true;
  }

  static Lightstyle[] lightstyles;
  static IntBuffer dummy;

  /**
   * GL_EndBuildingLightmaps
   */
  static void GL_EndBuildingLightmaps() {
    LM_UploadBlock(false);
    Images.GL_EnableMultitexture(false);
  }

  /*
   * new buffers for vertex array handling
   */
  static FloatBuffer globalPolygonInterleavedBuf = Polygons.getRewoundBuffer();
  static FloatBuffer globalPolygonTexCoord1Buf = null;

  static {
    globalPolygonInterleavedBuf.position(Polygons.STRIDE - 2);
    globalPolygonTexCoord1Buf = globalPolygonInterleavedBuf.slice();
    globalPolygonInterleavedBuf.position(0);
  };

  // ImageFrame frame;

  // void debugLightmap(byte[] buf, int w, int h, float scale) {
  // IntBuffer pix =
  // ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
  //
  // int[] pixel = new int[w * h];
  //
  // pix.get(pixel);
  //
  // BufferedImage image = new BufferedImage(w, h,
  // BufferedImage.TYPE_4BYTE_ABGR);
  // image.setRGB(0, 0, w, h, pixel, 0, w);
  // AffineTransformOp op = new
  // AffineTransformOp(AffineTransform.getScaleInstance(scale, scale),
  // AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
  // BufferedImage tmp = op.filter(image, null);
  //
  // if (frame == null) {
  // frame = new ImageFrame(null);
  // frame.show();
  // }
  // frame.showImage(tmp);
  //
  // }

  protected static void debugLightmap(IntBuffer lightmapBuffer, int w, int h,
      float scale) {
    GlState.gl.log("debuglightmap");
  }

  static float[] s_blocklights = new float[34 * 34 * 3];

  /**
   * SubdividePolygon
   * 
   * @param numverts
   * @param verts
   */
  static void SubdividePolygon(Surface warp, int numverts, float[][] verts) {
    int i, j, k;
    float m;
    float[][] front = new float[64][3];
    float[][] back = new float[64][3];
  
    int f, b;
    float[] dist = new float[64];
    float frac;
  
    if (numverts > 60)
      Com.Error(Constants.ERR_DROP, "numverts = " + numverts);
  
    float[] mins = Vec3Cache.get();
    float[] maxs = Vec3Cache.get();
  
    SkyBox.BoundPoly(numverts, verts, mins, maxs);
    float[] v;
    // x,y und z
    for (i = 0; i < 3; i++) {
      m = (mins[i] + maxs[i]) * 0.5f;
      m = GlConstants.SUBDIVIDE_SIZE
          * (float) Math.floor(m / GlConstants.SUBDIVIDE_SIZE + 0.5f);
      if (maxs[i] - m < 8)
        continue;
      if (m - mins[i] < 8)
        continue;
  
      // cut it
      for (j = 0; j < numverts; j++) {
        dist[j] = verts[j][i] - m;
      }
  
      // wrap cases
      dist[j] = dist[0];
  
      Math3D.VectorCopy(verts[0], verts[numverts]);
  
      f = b = 0;
      for (j = 0; j < numverts; j++) {
        v = verts[j];
        if (dist[j] >= 0) {
          Math3D.VectorCopy(v, front[f]);
          f++;
        }
        if (dist[j] <= 0) {
          Math3D.VectorCopy(v, back[b]);
          b++;
        }
        if (dist[j] == 0 || dist[j + 1] == 0)
          continue;
  
        if ((dist[j] > 0) != (dist[j + 1] > 0)) {
          // clip point
          frac = dist[j] / (dist[j] - dist[j + 1]);
          for (k = 0; k < 3; k++)
            front[f][k] = back[b][k] = v[k] + frac * (verts[j + 1][k] - v[k]);
  
          f++;
          b++;
        }
      }
  
      SubdividePolygon(warp, f, front);
      SubdividePolygon(warp, b, back);
  
      Vec3Cache.release(2); // mins, maxs
      return;
    }
  
    Vec3Cache.release(2); // mins, maxs
  
    // add a point in the center to help keep warp valid
  
    // wird im Konstruktor erschlagen
    // poly = Hunk_Alloc (sizeof(glpoly_t) + ((numverts-4)+2) *
    // VERTEXSIZE*sizeof(float));
  
    // init polys
    Polygon poly = Polygons.create(numverts + 2);
  
    poly.next = warp.polys;
    warp.polys = poly;
  
    float[] total = Vec3Cache.get();
    Math3D.VectorClear(total);
    float total_s = 0;
    float total_t = 0;
    float s, t;
    for (i = 0; i < numverts; i++) {
      poly.setX(i + 1, verts[i][0]);
      poly.setY(i + 1, verts[i][1]);
      poly.setZ(i + 1, verts[i][2]);
      s = Math3D.DotProduct(verts[i], warp.texinfo.vecs[0]);
      t = Math3D.DotProduct(verts[i], warp.texinfo.vecs[1]);
  
      total_s += s;
      total_t += t;
      Math3D.VectorAdd(total, verts[i], total);
  
      poly.setS1(i + 1, s);
      poly.setT1(i + 1, t);
    }
  
    float scale = 1.0f / numverts;
    poly.setX(0, total[0] * scale);
    poly.setY(0, total[1] * scale);
    poly.setZ(0, total[2] * scale);
    poly.setS1(0, total_s * scale);
    poly.setT1(0, total_t * scale);
  
    poly.setX(i + 1, poly.getX(1));
    poly.setY(i + 1, poly.getY(1));
    poly.setZ(i + 1, poly.getZ(1));
    poly.setS1(i + 1, poly.getS1(1));
    poly.setT1(i + 1, poly.getT1(1));
    poly.setS2(i + 1, poly.getS2(1));
    poly.setT2(i + 1, poly.getT2(1));
  
    Vec3Cache.release(); // total
  }

  //static Surface warpface;

}

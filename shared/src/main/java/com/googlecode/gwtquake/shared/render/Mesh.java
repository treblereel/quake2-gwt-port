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



import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import com.googlecode.gwtquake.shared.client.EntityType;
import com.googlecode.gwtquake.shared.client.Window;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.QuakeFiles;
import com.googlecode.gwtquake.shared.util.Lib;
import com.googlecode.gwtquake.shared.util.Math3D;

/**
 * Mesh
 *  
 * @author cwei
 */
public class Mesh {

  static float[][] r_avertexnormals = GlConstants.VERTEXNORMALS;
  static float[] shadevector = {0, 0, 0};
  static float[] shadelight = {0, 0, 0};


  static float[][]	r_avertexnormal_dots = GlConstants.VERTEXNORMAL_DOTS;

  static float[] shadedots = r_avertexnormal_dots[0];

  /**
   * GL_LerpVerts
   * @param nverts
   * @param ov
   * @param verts
   * @param move
   * @param frontv
   * @param backv
   */
  static void GL_LerpVerts(int nverts, int[] ov, int[] v, float[] move, float[] frontv, float[] backv )
  {
    FloatBuffer lerp = vertexArrayBuf;
    lerp.limit((nverts << 2) - nverts); // nverts * 3

    int ovv, vv;
    //PMM -- added RF_SHELL_DOUBLE, RF_SHELL_HALF_DAM
    if ( (GlState.currententity.flags & ( Constants.RF_SHELL_RED | Constants.RF_SHELL_GREEN | Constants.RF_SHELL_BLUE | Constants.RF_SHELL_DOUBLE | Constants.RF_SHELL_HALF_DAM)) != 0 )
    {
      float[] normal;
      int j = 0;
      for (int i=0 ; i < nverts; i++/* , v++, ov++, lerp+=4 */)
      {
        vv = v[i];
        normal = r_avertexnormals[(vv >>> 24 ) & 0xFF];
        ovv = ov[i];
        lerp.put(j, move[0] + (ovv & 0xFF)* backv[0] + (vv & 0xFF) * frontv[0] + normal[0] * Constants.POWERSUIT_SCALE);
        lerp.put(j + 1, move[1] + ((ovv >>> 8) & 0xFF) * backv[1] + ((vv >>> 8) & 0xFF) * frontv[1] + normal[1] * Constants.POWERSUIT_SCALE);
        lerp.put(j + 2, move[2] + ((ovv >>> 16) & 0xFF) * backv[2] + ((vv >>> 16) & 0xFF) * frontv[2] + normal[2] * Constants.POWERSUIT_SCALE); 
        j += 3;
      }
    }
    else
    {
      int j = 0;
      for (int i=0 ; i < nverts; i++ /* , v++, ov++, lerp+=4 */)
      {
        ovv = ov[i];
        vv = v[i];

        lerp.put(j, move[0] + (ovv & 0xFF)* backv[0] + (vv & 0xFF)*frontv[0]);
        lerp.put(j + 1, move[1] + ((ovv >>> 8) & 0xFF)* backv[1] + ((vv >>> 8) & 0xFF)*frontv[1]);
        lerp.put(j + 2, move[2] + ((ovv >>> 16) & 0xFF)* backv[2] + ((vv >>> 16) & 0xFF)*frontv[2]);
        j += 3;
      }
    }
  }


  static FloatBuffer colorArrayBuf;// = gl.createFloatBuffer(qfiles.MAX_VERTS * 4);
  static FloatBuffer vertexArrayBuf;// = gl.createFloatBuffer(qfiles.MAX_VERTS * 3);
  static FloatBuffer textureArrayBuf;// = gl.createFloatBuffer(qfiles.MAX_VERTS * 2);

  static FloatBuffer colorArrayBuf2;// = gl.createFloatBuffer(qfiles.MAX_VERTS * 4);
  static FloatBuffer vertexArrayBuf2;// = gl.createFloatBuffer(qfiles.MAX_VERTS * 3);
  static FloatBuffer textureArrayBuf2;// = gl.createFloatBuffer(qfiles.MAX_VERTS * 2);


  static public void init() {
    colorArrayBuf = Lib.newFloatBuffer(QuakeFiles.MAX_VERTS * 4);
    vertexArrayBuf = Lib.newFloatBuffer(QuakeFiles.MAX_VERTS * 3);
    textureArrayBuf = Lib.newFloatBuffer(QuakeFiles.MAX_VERTS * 2);


    int FACTOR = 4;

    colorArrayBuf2 = Lib.newFloatBuffer(QuakeFiles.MAX_VERTS * 4 *FACTOR);
    vertexArrayBuf2 = Lib.newFloatBuffer(QuakeFiles.MAX_VERTS * 3 * FACTOR);
    textureArrayBuf2 = Lib.newFloatBuffer(QuakeFiles.MAX_VERTS * 2 * FACTOR);

  }

  static boolean isFilled = false;
  static float[] tmpVec = {0, 0, 0};
  static float[][] vectors = {
      {0, 0, 0}, {0, 0, 0}, {0, 0, 0} // 3 mal vec3_t
  };

  // stack variable
  static float[] move = {0, 0, 0}; // vec3_t		
  static float[] frontv = {0, 0, 0}; // vec3_t
  static float[] backv = {0, 0, 0}; // vec3_t
  /**
   * GL_DrawAliasFrameLerp
   * 
   * interpolates between two frames and origins
   * FIXME: batch lerp all vertexes
   */
  static void GL_DrawAliasFrameLerp(QuakeFiles.dmdl_t paliashdr, float backlerp)
  {
    QuakeFiles.daliasframe_t frame = paliashdr.aliasFrames[GlState.currententity.frame];

    int[] verts = frame.verts;

    QuakeFiles.daliasframe_t oldframe = paliashdr.aliasFrames[GlState.currententity.oldframe];

    int[] ov = oldframe.verts;

    float	alpha;
    if ((GlState.currententity.flags & Constants.RF_TRANSLUCENT) != 0)
      alpha = GlState.currententity.alpha;
    else
      alpha = 1.0f;

    // PMM - added double shell
    if ( (GlState.currententity.flags & ( Constants.RF_SHELL_RED | Constants.RF_SHELL_GREEN | Constants.RF_SHELL_BLUE | Constants.RF_SHELL_DOUBLE | Constants.RF_SHELL_HALF_DAM)) != 0)
      GlState.gl.glDisable( Gl1Context.GL_TEXTURE_2D );

    float frontlerp = 1.0f - backlerp;

    // move should be the delta back to the previous frame * backlerp
    Math3D.VectorSubtract (GlState.currententity.oldorigin, GlState.currententity.origin, frontv);
    Math3D.AngleVectors (GlState.currententity.angles, vectors[0], vectors[1], vectors[2]);

    move[0] = Math3D.DotProduct (frontv, vectors[0]);	// forward
    move[1] = -Math3D.DotProduct (frontv, vectors[1]);	// left
    move[2] = Math3D.DotProduct (frontv, vectors[2]);	// up

    Math3D.VectorAdd (move, oldframe.translate, move);

    for (int i=0 ; i<3 ; i++)
    {
      move[i] = backlerp*move[i] + frontlerp*frame.translate[i];
      frontv[i] = frontlerp*frame.scale[i];
      backv[i] = backlerp*oldframe.scale[i];
    }

    // ab hier wird optimiert

    GL_LerpVerts( paliashdr.num_xyz, ov, verts, move, frontv, backv );

    int num_xyz = paliashdr.num_xyz;
    vertexArrayBuf.limit(num_xyz * 3);

    //gl.glEnableClientState( GLAdapter.GL_VERTEX_ARRAY );
    GlState.gl.glVertexPointer( 3, 0, vertexArrayBuf );

    // PMM - added double damage shell
    if ( (GlState.currententity.flags & ( Constants.RF_SHELL_RED | Constants.RF_SHELL_GREEN | Constants.RF_SHELL_BLUE | Constants.RF_SHELL_DOUBLE | Constants.RF_SHELL_HALF_DAM)) != 0)
    {
      GlState.gl.glColor4f( shadelight[0], shadelight[1], shadelight[2], alpha );
    }
    else
    {
      GlState.gl.glEnableClientState( Gl1Context.GL_COLOR_ARRAY );

      FloatBuffer color = colorArrayBuf;
      color.limit(num_xyz * 4);

      GlState.gl.glColorPointer( 4, 0, color );

      //
      // pre light everything
      //
      float l;
      int j = 0;
      for (int i = 0; i < num_xyz; i++ )
      {
        l = shadedots[(verts[i] >>> 24) & 0xFF];
        color.put(j,  l * shadelight[0]);
        color.put(j + 1, l * shadelight[1]);
        color.put(j + 2, l * shadelight[2]);
        color.put(j + 3, alpha);
        j += 4;
      }
    }

    GlState.gl.glClientActiveTexture(Gl1Context.GL_TEXTURE0);
    FloatBuffer dstTextureCoords = textureArrayBuf;

    GlState.gl.glTexCoordPointer( 2, 0, dstTextureCoords);
    GlState.gl.glEnableClientState( Gl1Context.GL_TEXTURE_COORD_ARRAY);

    int pos = 0;
    int[] counts = paliashdr.counts;

    ShortBuffer srcIndexBuf = null;

    FloatBuffer srcTextureCoords = paliashdr.textureCoordBuf;

    int dstIndex = 0;
    int srcIndex = 0;
    int count;
    int mode;
    int size = counts.length;
    for (int j = 0; j < size; j++) {
      dstTextureCoords.limit(num_xyz * 2);

      // get the vertex count and primitive type
      count = counts[j];
      if (count == 0)
        break;		// done

      srcIndexBuf = paliashdr.indexElements[j];

      mode = Gl1Context.GL_TRIANGLE_STRIP;
      if (count < 0) {
        mode = Gl1Context.GL_TRIANGLE_FAN;
        count = -count;
      }
      srcIndex = pos << 1;
      srcIndex--;

      int minIdx = 99999; 
      int maxIdx = 0;

      for (int k = 0; k < count; k++) {
        dstIndex = srcIndexBuf.get(k) << 1;
        if (dstIndex < minIdx) {
          minIdx = dstIndex;
        } 
        if (dstIndex > maxIdx) {
          maxIdx = dstIndex;
        }
        dstTextureCoords.put(dstIndex, srcTextureCoords.get(++srcIndex));
        dstTextureCoords.put(++dstIndex, srcTextureCoords.get(++srcIndex));
      }

      //gl.updatTCBuffer(dstTextureCoords, minIdx, maxIdx - minIdx + 2);

      dstTextureCoords.limit(maxIdx + 2);
      GlState.gl.glTexCoordPointer( 2, 0, dstTextureCoords);

      GlState.gl.glDrawElements(mode, srcIndexBuf);
      pos += count;
    }

    // PMM - added double damage shell
    if ( (GlState.currententity.flags & ( Constants.RF_SHELL_RED | Constants.RF_SHELL_GREEN | Constants.RF_SHELL_BLUE | Constants.RF_SHELL_DOUBLE | Constants.RF_SHELL_HALF_DAM)) != 0 )
      GlState.gl.glEnable( Gl1Context.GL_TEXTURE_2D );

    GlState.gl.glDisableClientState( Gl1Context.GL_COLOR_ARRAY );
  }


  /**
   * GL_DrawAliasFrameLerp with drawArrays
   */
  static void GL_DrawAliasFrameLerpDA(QuakeFiles.dmdl_t paliashdr, float backlerp)
  {
    QuakeFiles.daliasframe_t frame = paliashdr.aliasFrames[GlState.currententity.frame];

    int[] verts = frame.verts;

    QuakeFiles.daliasframe_t oldframe = paliashdr.aliasFrames[GlState.currententity.oldframe];

    int[] ov = oldframe.verts;

    float	alpha;
    if ((GlState.currententity.flags & Constants.RF_TRANSLUCENT) != 0)
      alpha = GlState.currententity.alpha;
    else
      alpha = 1.0f;

    // PMM - added double shell
    if ( (GlState.currententity.flags & ( Constants.RF_SHELL_RED | Constants.RF_SHELL_GREEN | Constants.RF_SHELL_BLUE | Constants.RF_SHELL_DOUBLE | Constants.RF_SHELL_HALF_DAM)) != 0)
      GlState.gl.glDisable( Gl1Context.GL_TEXTURE_2D );

    float frontlerp = 1.0f - backlerp;

    // move should be the delta back to the previous frame * backlerp
    Math3D.VectorSubtract (GlState.currententity.oldorigin, GlState.currententity.origin, frontv);
    Math3D.AngleVectors (GlState.currententity.angles, vectors[0], vectors[1], vectors[2]);

    move[0] = Math3D.DotProduct (frontv, vectors[0]);	// forward
    move[1] = -Math3D.DotProduct (frontv, vectors[1]);	// left
    move[2] = Math3D.DotProduct (frontv, vectors[2]);	// up

    Math3D.VectorAdd (move, oldframe.translate, move);

    for (int i=0 ; i<3 ; i++)
    {
      move[i] = backlerp*move[i] + frontlerp*frame.translate[i];
      frontv[i] = frontlerp*frame.scale[i];
      backv[i] = backlerp*oldframe.scale[i];
    }

    // ab hier wird optimiert

    GL_LerpVerts( paliashdr.num_xyz, ov, verts, move, frontv, backv );

    int num_xyz = paliashdr.num_xyz;
    FloatBuffer vertices = vertexArrayBuf;

    // PMM - added double damage shell
    boolean hasColorArray;
    FloatBuffer color = colorArrayBuf;
    hasColorArray = (GlState.currententity.flags & ( Constants.RF_SHELL_RED | Constants.RF_SHELL_GREEN | Constants.RF_SHELL_BLUE | Constants.RF_SHELL_DOUBLE | Constants.RF_SHELL_HALF_DAM)) == 0;
    if (hasColorArray) {
      //
      // pre light everything
      //
      float l;
      int j = 0;
      for (int i = 0; i < num_xyz; i++ )
      {
        l = shadedots[(verts[i] >>> 24) & 0xFF];
        color.put(j,  l * shadelight[0]);
        color.put(j + 1, l * shadelight[1]);
        color.put(j + 2, l * shadelight[2]);
        color.put(j + 3, alpha);
        j += 4;
      } 
    } else {
      GlState.gl.glColor4f( shadelight[0], shadelight[1], shadelight[2], alpha );
    }

    int pos = 0;
    int[] counts = paliashdr.counts;

    FloatBuffer dstVertexCoords = vertexArrayBuf2;
    FloatBuffer dstColors = colorArrayBuf2;

    dstVertexCoords.clear();
    dstColors.clear();

    int count;
    int mode;
    int size = counts.length;
    for (int j = 0; j < size; j++) {
      // get the vertex count and primitive type
      count = counts[j];
      if (count == 0)
        break;		// done

      ShortBuffer srcIndexBuf = paliashdr.indexElements[j];

      if (count < 0) {
        count = -count;
      }

      for (int k = 0; k < count; k++) {
        int srcIndex = srcIndexBuf.get(k);
        if (hasColorArray) {
          int cSrcIndex = srcIndex * 4;
          dstColors.put(color.get(cSrcIndex));
          dstColors.put(color.get(cSrcIndex+1));
          dstColors.put(color.get(cSrcIndex+2));
          dstColors.put(color.get(cSrcIndex+3));
        }
        int vSrcIndex = srcIndex * 3;
        dstVertexCoords.put(vertices.get(vSrcIndex));
        dstVertexCoords.put(vertices.get(vSrcIndex+1));
        dstVertexCoords.put(vertices.get(vSrcIndex+2));
      }

      //gl.updatTCBuffer(dstTextureCoords, minIdx, maxIdx - minIdx + 2);
      pos += count;
    }

    if (hasColorArray) {
      GlState.gl.glEnableClientState( Gl1Context.GL_COLOR_ARRAY );
      dstColors.flip();
      GlState.gl.glColorPointer(4, 0, dstColors);
    }

    GlState.gl.glClientActiveTexture(Gl1Context.GL_TEXTURE0);	
    GlState.gl.glEnableClientState( Gl1Context.GL_TEXTURE_COORD_ARRAY);

    FloatBuffer tc0 = paliashdr.textureCoordBuf;
    int limit = tc0.limit();
    tc0.limit(tc0.position() + pos * 2);
    GlState.gl.glVertexAttribPointer(Gl1Context.ARRAY_TEXCOORD_0, 2, Gl1Context.GL_FLOAT, false, 0, 0,
        paliashdr.textureCoordBuf, paliashdr.staticTextureBufId);
    tc0.limit(limit);

    dstVertexCoords.flip();
    GlState.gl.glVertexPointer(3, 0, dstVertexCoords);

    pos = 0;
    for (int j = 0; j < size; j++) {
      // get the vertex count and primitive type
      count = counts[j];
      if (count == 0)
        break;		// done

      mode = Gl1Context.GL_TRIANGLE_STRIP;
      if (count < 0) {
        mode = Gl1Context.GL_TRIANGLE_FAN;
        count = -count;
      }
      GlState.gl.glDrawArrays(mode, pos, count);
      pos += count;
    }


    // PMM - added double damage shell
    if ( (GlState.currententity.flags & ( Constants.RF_SHELL_RED | Constants.RF_SHELL_GREEN | Constants.RF_SHELL_BLUE | Constants.RF_SHELL_DOUBLE | Constants.RF_SHELL_HALF_DAM)) != 0 )
      GlState.gl.glEnable( Gl1Context.GL_TEXTURE_2D );

    GlState.gl.glDisableClientState( Gl1Context.GL_COLOR_ARRAY );
  }


  static private final float[] point = {0, 0, 0};
  /**
   * GL_DrawAliasShadow
   */
  static void GL_DrawAliasShadow(QuakeFiles.dmdl_t paliashdr, int posenum)
  {
    float lheight = GlState.currententity.origin[2] - DynamicLights.lightspot[2];
    //		qfiles.daliasframe_t frame = paliashdr.aliasFrames[currententity.frame];
    int[] order = paliashdr.glCmds;
    float height = -lheight + 1.0f;

    int orderIndex = 0;
    int index = 0;

    // TODO shadow drawing with vertex arrays

    int count;
    while (true)
    {
      // get the vertex count and primitive type
      count = order[orderIndex++];
      if (count == 0)
        break;		// done
      if (count < 0)
      {
        count = -count;
        GlState.gl.glBegin (Gl1Context.GL_TRIANGLE_FAN);
      }
      else
        GlState.gl.glBegin (Gl1Context.GL_TRIANGLE_STRIP);

      do
      {
        index = order[orderIndex + 2] * 3;
        point[0] = vertexArrayBuf.get(index);
        point[1] = vertexArrayBuf.get(index + 1);
        point[2] = vertexArrayBuf.get(index + 2);

        point[0] -= shadevector[0]*(point[2]+lheight);
        point[1] -= shadevector[1]*(point[2]+lheight);
        point[2] = height;
        GlState.gl.glVertex3f(point[0], point[1], point[2]);

        orderIndex += 3;

      } while (--count != 0);

      GlState.gl.glEnd ();
    }	
  }

  //	TODO sync with jogl renderer. hoz
  // stack variable
  private static final float[] mins = { 0, 0, 0 };
  private static final float[] maxs = { 0, 0, 0 };
  /**
   * R_CullAliasModel
   */
  static boolean R_CullAliasModel(EntityType e) {
    QuakeFiles.dmdl_t paliashdr = (QuakeFiles.dmdl_t) GlState.currentmodel.extradata;

    if ((e.frame >= paliashdr.num_frames) || (e.frame < 0)) {
      Window.Printf(Constants.PRINT_ALL, "R_CullAliasModel " + GlState.currentmodel.name + ": no such frame " + e.frame + '\n');
      e.frame = 0;
    }
    if ((e.oldframe >= paliashdr.num_frames) || (e.oldframe < 0)) {
      Window.Printf(Constants.PRINT_ALL, "R_CullAliasModel " + GlState.currentmodel.name + ": no such oldframe " + e.oldframe + '\n');
      e.oldframe = 0;
    }

    QuakeFiles.daliasframe_t pframe = paliashdr.aliasFrames[e.frame];
    QuakeFiles.daliasframe_t poldframe = paliashdr.aliasFrames[e.oldframe];

    /*
     ** compute axially aligned mins and maxs
     */
    if (pframe == poldframe) {
      for (int i = 0; i < 3; i++) {
        mins[i] = pframe.translate[i];
        maxs[i] = mins[i] + pframe.scale[i] * 255;
      }
    } else {
      float thismaxs, oldmaxs;
      for (int i = 0; i < 3; i++) {
        thismaxs = pframe.translate[i] + pframe.scale[i] * 255;

        oldmaxs = poldframe.translate[i] + poldframe.scale[i] * 255;

        if (pframe.translate[i] < poldframe.translate[i])
          mins[i] = pframe.translate[i];
        else
          mins[i] = poldframe.translate[i];

        if (thismaxs > oldmaxs)
          maxs[i] = thismaxs;
        else
          maxs[i] = oldmaxs;
      }
    }

    /*
     ** compute a full bounding box
     */
    float[] tmp;
    for (int i = 0; i < 8; i++) {
      tmp = bbox[i];
      if ((i & 1) != 0)
        tmp[0] = mins[0];
      else
        tmp[0] = maxs[0];

      if ((i & 2) != 0)
        tmp[1] = mins[1];
      else
        tmp[1] = maxs[1];

      if ((i & 4) != 0)
        tmp[2] = mins[2];
      else
        tmp[2] = maxs[2];
    }

    /*
     ** rotate the bounding box
     */
    tmp = mins;
    Math3D.VectorCopy(e.angles, tmp);
    tmp[GlConstants.YAW] = -tmp[GlConstants.YAW];
    Math3D.AngleVectors(tmp, vectors[0], vectors[1], vectors[2]);

    for (int i = 0; i < 8; i++) {
      Math3D.VectorCopy(bbox[i], tmp);

      bbox[i][0] = Math3D.DotProduct(vectors[0], tmp);
      bbox[i][1] = -Math3D.DotProduct(vectors[1], tmp);
      bbox[i][2] = Math3D.DotProduct(vectors[2], tmp);

      Math3D.VectorAdd(e.origin, bbox[i], bbox[i]);
    }

    int f, mask;
    int aggregatemask = ~0; // 0xFFFFFFFF

    for (int p = 0; p < 8; p++) {
      mask = 0;

      for (f = 0; f < 4; f++) {
        float dp = Math3D.DotProduct(GlState.frustum[f].normal, bbox[p]);

        if ((dp - GlState.frustum[f].dist) < 0) {
          mask |= (1 << f);
        }
      }

      aggregatemask &= mask;
    }

    if (aggregatemask != 0) {
      return true;
    }

    return false;
  }


  // bounding box
  static float[][] bbox = {
      {0, 0, 0}, {0, 0, 0}, {0, 0, 0}, {0, 0, 0},
      {0, 0, 0}, {0, 0, 0}, {0, 0, 0}, {0, 0, 0}
  };

  //	TODO sync with jogl renderer. hoz
  /**
   * R_DrawAliasModel
   */
  static void R_DrawAliasModel(EntityType e)
  {
    if ( ( e.flags & Constants.RF_WEAPONMODEL ) == 0)
    {
      if ( R_CullAliasModel(e) )
        return;
    }

    if ( (e.flags & Constants.RF_WEAPONMODEL) != 0 )
    {
      if ( GlConfig.r_lefthand.value == 2.0f )
        return;
    }

    QuakeFiles.dmdl_t paliashdr = (QuakeFiles.dmdl_t)GlState.currentmodel.extradata;

    //
    // get lighting information
    //
    // PMM - rewrote, reordered to handle new shells & mixing
    // PMM - 3.20 code .. replaced with original way of doing it to keep mod authors happy
    //
    int i;
    if ( (GlState.currententity.flags & ( Constants.RF_SHELL_HALF_DAM | Constants.RF_SHELL_GREEN | Constants.RF_SHELL_RED | Constants.RF_SHELL_BLUE | Constants.RF_SHELL_DOUBLE )) != 0 )
    {
      Math3D.VectorClear(shadelight);
      if ((GlState.currententity.flags & Constants.RF_SHELL_HALF_DAM) != 0)
      {
        shadelight[0] = 0.56f;
        shadelight[1] = 0.59f;
        shadelight[2] = 0.45f;
      }
      if ( (GlState.currententity.flags & Constants.RF_SHELL_DOUBLE) != 0 )
      {
        shadelight[0] = 0.9f;
        shadelight[1] = 0.7f;
      }
      if ( (GlState.currententity.flags & Constants.RF_SHELL_RED) != 0 )
        shadelight[0] = 1.0f;
      if ( (GlState.currententity.flags & Constants.RF_SHELL_GREEN) != 0 )
        shadelight[1] = 1.0f;
      if ( (GlState.currententity.flags & Constants.RF_SHELL_BLUE) != 0 )
        shadelight[2] = 1.0f;
    }

    else if ( (GlState.currententity.flags & Constants.RF_FULLBRIGHT) != 0 )
    {
      for (i=0 ; i<3 ; i++)
        shadelight[i] = 1.0f;
    }
    else
    {
      DynamicLights.R_LightPoint (GlState.currententity.origin, shadelight);

      // player lighting hack for communication back to server
      // big hack!
      if ( (GlState.currententity.flags & Constants.RF_WEAPONMODEL) != 0 )
      {
        // pick the greatest component, which should be the same
        // as the mono value returned by software
        if (shadelight[0] > shadelight[1])
        {
          if (shadelight[0] > shadelight[2])
            GlConfig.r_lightlevel.value = 150*shadelight[0];
          else
            GlConfig.r_lightlevel.value = 150*shadelight[2];
        }
        else
        {
          if (shadelight[1] > shadelight[2])
            GlConfig.r_lightlevel.value = 150*shadelight[1];
          else
            GlConfig.r_lightlevel.value = 150*shadelight[2];
        }
      }

      if ( GlConfig.gl_monolightmap.string.charAt(0) != '0' )
      {
        float s = shadelight[0];

        if ( s < shadelight[1] )
          s = shadelight[1];
        if ( s < shadelight[2] )
          s = shadelight[2];

        shadelight[0] = s;
        shadelight[1] = s;
        shadelight[2] = s;
      }
    }

    if ( (GlState.currententity.flags & Constants.RF_MINLIGHT) != 0 )
    {
      for (i=0 ; i<3 ; i++)
        if (shadelight[i] > 0.1f)
          break;
      if (i == 3)
      {
        shadelight[0] = 0.1f;
        shadelight[1] = 0.1f;
        shadelight[2] = 0.1f;
      }
    }

    if ( (GlState.currententity.flags & Constants.RF_GLOW) != 0 )
    {	// bonus items will pulse with time
      float	scale;
      float	min;

      scale = (float)(0.1f * Math.sin(GlState.r_newrefdef.time*7));
      for (i=0 ; i<3 ; i++)
      {
        min = shadelight[i] * 0.8f;
        shadelight[i] += scale;
        if (shadelight[i] < min)
          shadelight[i] = min;
      }
    }

    // =================
    // PGM	ir goggles color override
    if ( (GlState.r_newrefdef.rdflags & Constants.RDF_IRGOGGLES) != 0 && (GlState.currententity.flags & Constants.RF_IR_VISIBLE) != 0)
    {
      shadelight[0] = 1.0f;
      shadelight[1] = 0.0f;
      shadelight[2] = 0.0f;
    }
    // PGM	
    // =================

    shadedots = r_avertexnormal_dots[((int)(GlState.currententity.angles[1] * (GlConstants.SHADEDOT_QUANT / 360.0))) & (GlConstants.SHADEDOT_QUANT - 1)];

    float an = (float)(GlState.currententity.angles[1]/180*Math.PI);
    shadevector[0] = (float)Math.cos(-an);
    shadevector[1] = (float)Math.sin(-an);
    shadevector[2] = 1;
    Math3D.VectorNormalize(shadevector);

    //
    // locate the proper data
    //

    GlState.c_alias_polys += paliashdr.num_tris;

    //
    // draw all the triangles
    //
    if ( (GlState.currententity.flags & Constants.RF_DEPTHHACK) != 0) // hack the depth range to prevent view model from poking into walls
      GlState.gl.glDepthRange(GlState.gldepthmin, (float) (GlState.gldepthmin + 0.3*(GlState.gldepthmax-GlState.gldepthmin)));

    if ( (GlState.currententity.flags & Constants.RF_WEAPONMODEL) != 0 && (GlConfig.r_lefthand.value == 1.0f) )
    {
      GlState.gl.glMatrixMode( Gl1Context.GL_PROJECTION );
      GlState.gl.glPushMatrix();
      GlState.gl.glLoadIdentity();
      GlState.gl.glScalef( -1, 1, 1 );
      Entities.MYgluPerspective( GlState.r_newrefdef.fov_y, ( float ) GlState.r_newrefdef.width / GlState.r_newrefdef.height,  4,  4096);
      GlState.gl.glMatrixMode( Gl1Context.GL_MODELVIEW );

      GlState.gl.glCullFace( Gl1Context.GL_BACK );
    }

    GlState.gl.glPushMatrix ();
    e.angles[GlConstants.PITCH] = -e.angles[GlConstants.PITCH];	// sigh.
    Entities.rotateForEntity (e);
    e.angles[GlConstants.PITCH] = -e.angles[GlConstants.PITCH];	// sigh.



    Image		skin;
    // select skin
    if (GlState.currententity.skin != null)
      skin = GlState.currententity.skin;	// custom player skin
    else
    {
      if (GlState.currententity.skinnum >= QuakeFiles.MAX_MD2SKINS)
        skin = GlState.currentmodel.skins[0];
      else
      {
        skin = GlState.currentmodel.skins[GlState.currententity.skinnum];
        if (skin == null)
          skin = GlState.currentmodel.skins[0];
      }
    }
    if (skin == null)
      skin = GlState.r_notexture;	// fallback...
    Images.GL_Bind(skin.texnum);

    // draw it

    GlState.gl.glShadeModel (Gl1Context.GL_SMOOTH);

    Images.GL_TexEnv( Gl1Context.GL_MODULATE );
    if ( (GlState.currententity.flags & Constants.RF_TRANSLUCENT) != 0 )
    {
      GlState.gl.glEnable (Gl1Context.GL_BLEND);
    }


    if ( (GlState.currententity.frame >= paliashdr.num_frames) 
        || (GlState.currententity.frame < 0) )
    {
      Window.Printf (Constants.PRINT_ALL, "R_DrawAliasModel " + GlState.currentmodel.name +": no such frame " + GlState.currententity.frame + '\n');
      GlState.currententity.frame = 0;
      GlState.currententity.oldframe = 0;
    }

    if ( (GlState.currententity.oldframe >= paliashdr.num_frames)
        || (GlState.currententity.oldframe < 0))
    {
      Window.Printf (Constants.PRINT_ALL, "R_DrawAliasModel " + GlState.currentmodel.name +": no such oldframe " + GlState.currententity.oldframe + '\n');
      GlState.currententity.frame = 0;
      GlState.currententity.oldframe = 0;
    }

    if ( GlConfig.r_lerpmodels.value == 0.0f)
      GlState.currententity.backlerp = 0;

    GL_DrawAliasFrameLerpDA(paliashdr, GlState.currententity.backlerp);

    Images.GL_TexEnv( Gl1Context.GL_REPLACE );
    GlState.gl.glShadeModel (Gl1Context.GL_FLAT);

    GlState.gl.glPopMatrix ();

    if ( ( GlState.currententity.flags & Constants.RF_WEAPONMODEL ) != 0 && ( GlConfig.r_lefthand.value == 1.0F ) )
    {
      GlState.gl.glMatrixMode( Gl1Context.GL_PROJECTION );
      GlState.gl.glPopMatrix();
      GlState.gl.glMatrixMode( Gl1Context.GL_MODELVIEW );
      GlState.gl.glCullFace( Gl1Context.GL_FRONT );
    }

    if ( (GlState.currententity.flags & Constants.RF_TRANSLUCENT) != 0 )
    {
      GlState.gl.glDisable (Gl1Context.GL_BLEND);
    }

    if ( (GlState.currententity.flags & Constants.RF_DEPTHHACK) != 0)
      GlState.gl.glDepthRange (GlState.gldepthmin, GlState.gldepthmax);

    if ( GlConfig.gl_shadows.value != 0.0f && (GlState.currententity.flags & (Constants.RF_TRANSLUCENT | Constants.RF_WEAPONMODEL)) == 0)
    {
      GlState.gl.glPushMatrix ();
      Entities.rotateForEntity (e);
      GlState.gl.glDisable (Gl1Context.GL_TEXTURE_2D);
      GlState.gl.glEnable (Gl1Context.GL_BLEND);
      GlState.gl.glColor4f (0,0,0,0.5f);
      GL_DrawAliasShadow (paliashdr, GlState.currententity.frame );
      GlState.gl.glEnable (Gl1Context.GL_TEXTURE_2D);
      GlState.gl.glDisable (Gl1Context.GL_BLEND);
      GlState.gl.glPopMatrix ();
    }
    GlState.gl.glColor4f (1,1,1,1);
  }
}

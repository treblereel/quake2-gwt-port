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

import com.googlecode.gwtquake.shared.client.Dimension;
import com.googlecode.gwtquake.shared.client.EntityType;
import com.googlecode.gwtquake.shared.client.RendererState;
import com.googlecode.gwtquake.shared.game.Plane;

public class GlState 
{
  public static float inverse_intensity;
  public static boolean fullscreen;

  public static int prev_mode;

  public static int lightmap_textures;

  public static int currenttextures[]= {0,0};
  public static int currenttmu;

  public static float camera_separation;
  public static boolean stereo_enabled;

  public static byte originalRedGammaTable[]= new byte [256];
  public static byte originalGreenGammaTable[]= new byte [256];
  public static byte originalBlueGammaTable[]= new byte [256];
  public static Gl1Context gl;
  // IMPORTED FUNCTIONS
  static protected DisplayMode oldDisplayMode;
  static int window_ypos;
  static protected Dimension vid = new Dimension();
  static int c_visible_lightmaps;
  static	int c_visible_textures;
  static int registration_sequence;
  static boolean qglPointParameterfEXT = false;
  static Model r_worldmodel;
  static float gldepthmax;
  static Plane frustum[] = { new Plane(), new Plane(), new Plane(), new Plane()};
  static Model currentmodel;
  static Image r_notexture; // use for bad textures
  static float gldepthmin;
  static Image r_particletexture; // little dot for particles
  static EntityType currententity;
  static int r_visframecount; // bumped when going to a new PVS
  public static int r_framecount; // used for dlight push checking
  public static int c_brush_polys;
  static int c_alias_polys;
  static float v_blend[] = { 0, 0, 0, 0 }; // final blending color
  //
  //	   view origin
  //
  static float[] vup = { 0, 0, 0 };
  static float[] vpn = { 0, 0, 0 };
  static float[] vright = { 0, 0, 0 };
  static float[] r_origin = { 0, 0, 0 };
  //float r_world_matrix[] = new float[16];
  static FloatBuffer r_world_matrix;
  static float r_base_world_matrix[] = new float[16];
  //
  //	   screen size info
  //
  public static RendererState r_newrefdef = new RendererState();
  static int r_viewcluster;
  static int r_viewcluster2;
  static int r_oldviewcluster;
  static int r_oldviewcluster2;
  // stack variable
  static  float[] light = { 0, 0, 0 };
  // stack variable
  static  float[] point = { 0, 0, 0 };
  // stack variable
  static  float[] shadelight = { 0, 0, 0 };
  // stack variable 
  static  float[] up = { 0, 0, 0 };
  static float[] right = { 0, 0, 0 };
  // stack variable
  static float[] temp = {0, 0, 0};
  static int trickframe = 0;
  static int[] r_rawpalette = new int[256];
  static float[][] start_points = new float[GlConstants.NUM_BEAM_SEGS][3];
  // array of vec3_t
  static float[][] end_points = new float[GlConstants.NUM_BEAM_SEGS][3]; // array of vec3_t
  // stack variable
  static final float[] perpvec = { 0, 0, 0 }; // vec3_t
  static final float[] direction = { 0, 0, 0 }; // vec3_t
  static final float[] normalized_direction = { 0, 0, 0 }; // vec3_t
  static final float[] oldorigin = { 0, 0, 0 }; // vec3_t
  static final float[] origin = { 0, 0, 0 }; // vec3_t

}

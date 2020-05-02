package com.googlecode.gwtquake.shared.render;

import static com.googlecode.gwtquake.shared.common.Constants.CVAR_ARCHIVE;
import static com.googlecode.gwtquake.shared.common.Constants.CVAR_USERINFO;

import com.googlecode.gwtquake.shared.common.ConsoleVariables;
import com.googlecode.gwtquake.shared.game.ConsoleVariable;

public class GlConfig {

  static ConsoleVariable gl_nosubimage;
  static ConsoleVariable gl_allow_software;
  static ConsoleVariable gl_vertex_arrays;
  static ConsoleVariable gl_particle_min_size;
  static ConsoleVariable gl_particle_max_size;
  static ConsoleVariable gl_particle_size;
  static ConsoleVariable gl_particle_att_a;
  static ConsoleVariable gl_particle_att_b;
  static ConsoleVariable gl_particle_att_c;
  static ConsoleVariable gl_ext_swapinterval;
  static ConsoleVariable gl_ext_palettedtexture;
  static ConsoleVariable gl_ext_multitexture;
  static ConsoleVariable gl_ext_pointparameters;
  static ConsoleVariable gl_ext_compiled_vertex_array;
  static ConsoleVariable gl_log;
  static ConsoleVariable gl_bitdepth;
  static ConsoleVariable gl_drawbuffer;
  static ConsoleVariable gl_driver;
  static ConsoleVariable r_norefresh;
  static GlState gl_state = new GlState();
  static ConsoleVariable gl_lightmap;
  static ConsoleVariable gl_shadows;
  static ConsoleVariable gl_mode;
  static ConsoleVariable gl_dynamic;
  static ConsoleVariable gl_monolightmap;
  static ConsoleVariable gl_modulate;
  static ConsoleVariable gl_nobind;
  static ConsoleVariable gl_round_down;
  static ConsoleVariable gl_picmip;
  static ConsoleVariable gl_skymip;
  static ConsoleVariable gl_showtris;
  static ConsoleVariable gl_finish;
  static ConsoleVariable gl_clear;
  static ConsoleVariable gl_cull;
  static ConsoleVariable gl_polyblend;
  static ConsoleVariable gl_flashblend;
  static ConsoleVariable gl_playermip;
  static ConsoleVariable gl_saturatelighting;
  static ConsoleVariable gl_swapinterval;
  static ConsoleVariable gl_lockpvs;
  static ConsoleVariable gl_3dlabs_broken;
  static ConsoleVariable gl_ztrick;
  static ConsoleVariable gl_texturemode;
  static ConsoleVariable gl_texturealphamode;
  static ConsoleVariable gl_texturesolidmode;
  static ConsoleVariable r_drawentities;
  static ConsoleVariable r_drawworld;
  static ConsoleVariable r_speeds;
  static ConsoleVariable r_fullbright;
  static ConsoleVariable r_novis;
  static ConsoleVariable r_lerpmodels;
  static ConsoleVariable r_nocull;
  static ConsoleVariable r_lefthand;
  static ConsoleVariable r_lightlevel;
  // FIXME: This is a HACK to get the client's light level
  static ConsoleVariable vid_gamma;
  static ConsoleVariable vid_ref;

  static void init() {
    GlConfig.r_lefthand = ConsoleVariables.Get("hand", "0", CVAR_USERINFO | CVAR_ARCHIVE);
    GlConfig.r_norefresh = ConsoleVariables.Get("r_norefresh", "0", 0);
    GlConfig.r_fullbright = ConsoleVariables.Get("r_fullbright", "0", 0);
    GlConfig.r_drawentities = ConsoleVariables.Get("r_drawentities", "1", 0);
    GlConfig.r_drawworld = ConsoleVariables.Get("r_drawworld", "1", 0);
    GlConfig.r_novis = ConsoleVariables.Get("r_novis", "0", 0);
    GlConfig.r_nocull = ConsoleVariables.Get("r_nocull", "0", 0);
    GlConfig.r_lerpmodels = ConsoleVariables.Get("r_lerpmodels", "1", 0);
    GlConfig.r_speeds = ConsoleVariables.Get("r_speeds", "0", 0);

    GlConfig.r_lightlevel = ConsoleVariables.Get("r_lightlevel", "1", 0);

    GlConfig.gl_nosubimage = ConsoleVariables.Get("gl_nosubimage", "0", 0);
    GlConfig.gl_allow_software = ConsoleVariables.Get("gl_allow_software", "0", 0);

    GlConfig.gl_particle_min_size = ConsoleVariables.Get("gl_particle_min_size", "2", CVAR_ARCHIVE);
    GlConfig.gl_particle_max_size = ConsoleVariables.Get("gl_particle_max_size", "40", CVAR_ARCHIVE);
    GlConfig.gl_particle_size = ConsoleVariables.Get("gl_particle_size", "40", CVAR_ARCHIVE);
    GlConfig.gl_particle_att_a = ConsoleVariables.Get("gl_particle_att_a", "0.01", CVAR_ARCHIVE);
    GlConfig.gl_particle_att_b = ConsoleVariables.Get("gl_particle_att_b", "0.0", CVAR_ARCHIVE);
    GlConfig.gl_particle_att_c = ConsoleVariables.Get("gl_particle_att_c", "0.01", CVAR_ARCHIVE);

    GlConfig.gl_modulate = ConsoleVariables.Get("gl_modulate", "1.5", CVAR_ARCHIVE);
    GlConfig.gl_log = ConsoleVariables.Get("gl_log", "0", 0);
    GlConfig.gl_bitdepth = ConsoleVariables.Get("gl_bitdepth", "0", 0);
    GlConfig.gl_mode = ConsoleVariables.Get("gl_mode", "3", CVAR_ARCHIVE); // 640x480
    GlConfig.gl_lightmap = ConsoleVariables.Get("gl_lightmap", "0", 0);
    GlConfig.gl_shadows = ConsoleVariables.Get("gl_shadows", "0", CVAR_ARCHIVE);
    GlConfig.gl_dynamic = ConsoleVariables.Get("gl_dynamic", "1", 0);
    GlConfig.gl_nobind = ConsoleVariables.Get("gl_nobind", "0", 0);
    GlConfig.gl_round_down = ConsoleVariables.Get("gl_round_down", "1", 0);
    GlConfig.gl_picmip = ConsoleVariables.Get("gl_picmip", "0", 0);
    GlConfig.gl_skymip = ConsoleVariables.Get("gl_skymip", "0", 0);
    GlConfig.gl_showtris = ConsoleVariables.Get("gl_showtris", "0", 0);
    GlConfig.gl_ztrick = ConsoleVariables.Get("gl_ztrick", "0", 0);
    GlConfig.gl_finish = ConsoleVariables.Get("gl_finish", "0", CVAR_ARCHIVE);
    GlConfig.gl_clear = ConsoleVariables.Get("gl_clear", "0", 0);
    GlConfig.gl_cull = ConsoleVariables.Get("gl_cull", "1", 0);
    GlConfig.gl_polyblend = ConsoleVariables.Get("gl_polyblend", "1", 0);
    GlConfig.gl_flashblend = ConsoleVariables.Get("gl_flashblend", "0", 0);
    GlConfig.gl_playermip = ConsoleVariables.Get("gl_playermip", "0", 0);
    GlConfig.gl_monolightmap = ConsoleVariables.Get("gl_monolightmap", "0", 0);
    GlConfig.gl_driver = ConsoleVariables.Get("gl_driver", "opengl32", CVAR_ARCHIVE);
    GlConfig.gl_texturemode = ConsoleVariables.Get("gl_texturemode", "GL_LINEAR_MIPMAP_NEAREST", CVAR_ARCHIVE);
    GlConfig.gl_texturealphamode = ConsoleVariables.Get("gl_texturealphamode", "default", CVAR_ARCHIVE);
    GlConfig.gl_texturesolidmode = ConsoleVariables.Get("gl_texturesolidmode", "default", CVAR_ARCHIVE);
    GlConfig.gl_lockpvs = ConsoleVariables.Get("gl_lockpvs", "0", 0);

    GlConfig.gl_vertex_arrays = ConsoleVariables.Get("gl_vertex_arrays", "1", CVAR_ARCHIVE);

    GlConfig.gl_ext_swapinterval = ConsoleVariables.Get("gl_ext_swapinterval", "1", CVAR_ARCHIVE);
    GlConfig.gl_ext_palettedtexture = ConsoleVariables.Get("gl_ext_palettedtexture", "0", CVAR_ARCHIVE);
    GlConfig.gl_ext_multitexture = ConsoleVariables.Get("gl_ext_multitexture", "1", CVAR_ARCHIVE);
    GlConfig.gl_ext_pointparameters = ConsoleVariables.Get("gl_ext_pointparameters", "1", CVAR_ARCHIVE);
    GlConfig.gl_ext_compiled_vertex_array = ConsoleVariables.Get("gl_ext_compiled_vertex_array", "1", CVAR_ARCHIVE);

    GlConfig.gl_drawbuffer = ConsoleVariables.Get("gl_drawbuffer", "GL_BACK", 0);
    GlConfig.gl_swapinterval = ConsoleVariables.Get("gl_swapinterval", "0", CVAR_ARCHIVE);

    GlConfig.gl_saturatelighting = ConsoleVariables.Get("gl_saturatelighting", "0", 0);

    GlConfig.gl_3dlabs_broken = ConsoleVariables.Get("gl_3dlabs_broken", "1", CVAR_ARCHIVE);

    GlConfig.vid_fullscreen = ConsoleVariables.Get("vid_fullscreen", "0", CVAR_ARCHIVE);
    GlConfig.vid_gamma = ConsoleVariables.Get("vid_gamma", "1.0", CVAR_ARCHIVE);
    GlConfig.vid_ref = ConsoleVariables.Get("vid_ref", "lwjgl", CVAR_ARCHIVE);

  }

  static protected ConsoleVariable vid_fullscreen;
}

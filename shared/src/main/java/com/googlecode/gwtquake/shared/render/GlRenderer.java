/*
`Copyright (C) 1997-2001 Id Software, Inc.

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

import com.googlecode.gwtquake.shared.client.Dimension;
import com.googlecode.gwtquake.shared.client.Renderer;
import com.googlecode.gwtquake.shared.client.RendererState;
import com.googlecode.gwtquake.shared.client.Window;
import com.googlecode.gwtquake.shared.common.AsyncCallback;
import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.Command;
import com.googlecode.gwtquake.shared.common.ConsoleVariables;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.ExecutableCommand;
import com.googlecode.gwtquake.shared.common.QuakeImage;
import com.googlecode.gwtquake.shared.game.Commands;
import com.googlecode.gwtquake.shared.game.ConsoleVariable;
import com.googlecode.gwtquake.shared.util.Lib;
import com.googlecode.gwtquake.shared.util.Vargs;
import elemental2.dom.DomGlobal;

/**
 * LWJGLRenderer
 * 
 * @author dsanders/cwei
 */
public abstract class GlRenderer implements Renderer {
  int width;
  int height;

  protected GlRenderer(int width, int height) {
    this.width = width;
    this.height = height;
  }

  public DisplayMode[] getAvailableDisplayModes() {
    return new DisplayMode[] { getDisplayMode() };
  }

  public DisplayMode getDisplayMode() {
    return new DisplayMode(width, height);
  }

  // ============================================================================
  // public interface for Renderer implementations
  //
  // refexport_t (ref.h)
  // ============================================================================
  /**
   * @see com.googlecode.gwtquake.shared.client.Renderer#Init()
   */
  public boolean Init(int vid_xpos, int vid_ypos) {
    // pre init
    assert (GlConstants.SIN.length == 256) : "warpsin table bug";

    Window.Printf(Constants.PRINT_ALL, "ref_gl version: " + GlConstants.REF_VERSION + '\n');
    Images.Draw_GetPalette();  
    GlConfig.init();
    
    Commands.addCommand("imagelist", new ExecutableCommand() {
    	public void execute() {
    		Images.GL_ImageList_f();
    	}
    });
    
    Commands.addCommand("screenshot", new ExecutableCommand() {
    	public void execute() {
    		Misc.GL_ScreenShot_f();
    	}
    });
    Commands.addCommand("modellist", new ExecutableCommand() {
    	public void execute() {
    		Models.Mod_Modellist_f();
    	}
    });
    Commands.addCommand("gl_strings", new ExecutableCommand() {
    	public void execute() {
    		Misc.GL_Strings_f();
    	}
    });

    // set our "safe" modes
    GlState.prev_mode = 3;

    // create the window and set up the context
    if (!R_SetMode()) {
      Window.Printf(Constants.PRINT_ALL,
          "ref_gl::R_Init() - could not R_SetMode()\n");
      return false;
    }

    // post init
    GlState.qglPointParameterfEXT = true;
    
    Misc.GL_SetDefaultState();
    
    Images.GL_InitImages();
    Models.Mod_Init();
    Particles.R_InitParticleTexture();
    Drawing.Draw_InitLocal();
    
    int err = GlState.gl.glGetError();
    if (err != Gl1Context.GL_NO_ERROR) {
    	Window.Printf(
    		Constants.PRINT_ALL,
    		"glGetError() = 0x%x\n\t%s\n",
    		new Vargs(2).add(err).add("" + GlState.gl.glGetString(err)));

      //	return false;
    }
    return true;
  }

  protected void init() {
    GlState.r_world_matrix = Lib.newFloatBuffer(16);
    Images.init();
    Mesh.init();
    Models.init();
  }

  /**
   * @see com.googlecode.gwtquake.shared.client.Renderer#Shutdown()
   */
  public void Shutdown() {
    Commands.RemoveCommand("modellist");
    Commands.RemoveCommand("screenshot");
    Commands.RemoveCommand("imagelist");
    Commands.RemoveCommand("gl_strings");
    
    Models.Mod_FreeAll();
    
    Images.GL_ShutdownImages();
    
    /*
     * shut down OS specific OpenGL stuff like contexts, etc.
     */
    GlState.gl.shutdow();
  }

  /**
   * @see com.googlecode.gwtquake.shared.client.Renderer#BeginRegistration(java.lang.String)
   */
  public final void BeginRegistration(String map, Command callback) {
    Models.R_BeginRegistration(map, callback);
  }

  /**
   * @see com.googlecode.gwtquake.shared.client.Renderer#RegisterModel(java.lang.String)
   */
  public final void RegisterModel(String name, AsyncCallback<Model> callback) {
    Models.R_RegisterModel(name, callback);
  }

  /**
   * @see com.googlecode.gwtquake.shared.client.Renderer#RegisterSkin(java.lang.String)
   */
  public final Image RegisterSkin(String name) {
    return Images.R_RegisterSkin(name);
  }

  /**
   * @see com.googlecode.gwtquake.shared.client.Renderer#RegisterPic(java.lang.String)
   */
  public Image RegisterPic(String name) {
    return Images.findPicture(name);
  }

  /**
   * @see com.googlecode.gwtquake.shared.client.Renderer#SetSky(java.lang.String,
   *      float, float[])
   */
  public final void SetSky(String name, float rotate, float[] axis) {
    SkyBox.R_SetSky(name, rotate, axis);
  }

  /**
   * @see com.googlecode.gwtquake.shared.client.Renderer#EndRegistration()
   */
  public final void EndRegistration() {
    Models.R_EndRegistration();
  }

  /**
   * @see com.googlecode.gwtquake.shared.client.Renderer#RenderFrame(com.googlecode.gwtquake.shared.client.RendererState)
   */
  public final void RenderFrame(RendererState fd) {
    Entities.R_RenderFrame(fd);
  }

  public void DrawChar(int x, int y, int ch) {
    Images.GL_Bind(Images.draw_chars.texnum);
    GlState.gl.glBegin(Gl1Context._GL_QUADS);
    DrawChar_(x, y, ch);
    GlState.gl.glEnd();
  }

  public void DrawString(int x, int y, String str) {
    DrawString(x, y, str, 0, str.length());
  }

  public void DrawString(int x, int y, String str, boolean alt) {
    DrawString(x, y, str, 0, str.length(), alt);
  }

  public final void DrawString(int x, int y, String str, int ofs, int len) {
    DrawString(x, y, str, ofs, len, false);
  }

  public void DrawString(int x, int y, String str, int ofs, int len, boolean alt) {
    Images.GL_Bind(Images.draw_chars.texnum);
    GlState.gl.glBegin(Gl1Context._GL_QUADS);
    for (int i = 0; i < len; ++i) {
      DrawChar_(x, y, str.charAt(ofs + i) + (alt ? 128 : 0));
      x += 8;
    }
    GlState.gl.glEnd();
  }

  public void DrawString(int x, int y, byte[] str, int ofs, int len) {
    Images.GL_Bind(Images.draw_chars.texnum);
    GlState.gl.glBegin(Gl1Context._GL_QUADS);
    for (int i = 0; i < len; ++i) {
      DrawChar_(x, y, str[ofs + i]);
      x += 8;
    }
    GlState.gl.glEnd();
  }

  /**
   * @see com.googlecode.gwtquake.shared.client.Renderer#CinematicSetPalette(byte[])
   */
  public void CinematicSetPalette(byte[] palette) {
    // 256 RGB values (768 bytes)
    // or null
    int i;
    int color = 0;
    
    if (palette != null) {
    	int j =0;
    	for (i = 0; i < 256; i++) {
    		color = (palette[j++] & 0xFF) << 0;
    		color |= (palette[j++] & 0xFF) << 8;
    		color |= (palette[j++] & 0xFF) << 16;
    		color |= 0xFF000000;
    		GlState.r_rawpalette[i] = color;
    	}
    }
    else {
    	for (i = 0; i < 256; i++) {
    		GlState.r_rawpalette[i] = QuakeImage.PALETTE_ABGR[i] | 0xff000000;
    	}
    }
    Images.GL_SetTexturePalette(GlState.r_rawpalette);
    
    GlState.gl.glClearColor(0, 0, 0, 0);
    GlState.gl.glClear(Gl1Context.GL_COLOR_BUFFER_BIT);
    GlState.gl.glClearColor(1f, 0f, 0.5f, 0.5f);
  }

  /**
   * @see com.googlecode.gwtquake.shared.client.Renderer#EndFrame()
   */
  public final void EndFrame() {
    GlState.gl.swapBuffers();
    // swap buffers
  }

  /**
   * @see com.googlecode.gwtquake.shared.client.Renderer#AppActivate(boolean)
   */
  public final void AppActivate(boolean activate) {

  }

  public final int apiVersion() {
    return Constants.API_VERSION;
  }

  public boolean showVideo(String name) {
    return false;
  }

  public boolean updateVideo() {
    return false;
  }

  public void DrawStretchPic(int x, int y, int w, int h, String pic) {
    Image image;

    image = Images.findPicture(pic);
    if (image == null) {
      Window.Printf(Constants.PRINT_ALL, "Can't find pic: " + pic + '\n');
      return;
    }

    // if (scrap_dirty)
    // Scrap_Upload();

    // if ( ( ( gl_config.renderer == GL_RENDERER_MCD ) || ( (gl_config.renderer
    // & GL_RENDERER_RENDITION) != 0) ) && !image.has_alpha)
    // gl.glDisable(GLAdapter.GL_ALPHA_TEST);

    Images.GL_Bind(image.texnum);
    GlState.gl.glBegin(Gl1Context.SIMPLE_TEXUTRED_QUAD);
    GlState.gl.glVertex2f(x, y);
    GlState.gl.glVertex2f(x + w, y);
    GlState.gl.glVertex2f(x + w, y + h);
    GlState.gl.glVertex2f(x, y + h);
    GlState.gl.glEnd();

    // if ( ( ( gl_config.renderer == GL_RENDERER_MCD ) || ( (gl_config.renderer
    // & GL_RENDERER_RENDITION) !=0 ) ) && !image.has_alpha)
    // gl.glEnable(GLAdapter.GL_ALPHA_TEST);
  }

  public final void DrawGetPicSize(Dimension dim, String pic) {
    Image image = Images.findPicture(pic);
    dim.width = (image != null) ? image.width : -1;
    dim.height = (image != null) ? image.height : -1;
  }

  protected void DrawChar_(int x, int y, int num) {
    num &= 255;

    if ((num & 127) == 32)
      return; // space

    if (y <= -8)
      return; // totally off screen

    int row = num >> 4;
    int col = num & 15;

    float frow = row * 0.0625f;
    float fcol = col * 0.0625f;
    float size = 0.0625f;

    GlState.gl.glTexCoord2f(fcol, frow);
    GlState.gl.glVertex2f(x, y);
    GlState.gl.glTexCoord2f(fcol + size, frow);
    GlState.gl.glVertex2f(x + 8, y);
    GlState.gl.glTexCoord2f(fcol + size, frow + size);
    GlState.gl.glVertex2f(x + 8, y + 8);
    GlState.gl.glTexCoord2f(fcol, frow + size);
    GlState.gl.glVertex2f(x, y + 8);
  }

  /*
   * ============= Draw_TileClear
   * 
   * This repeats a 64*64 tile graphic to fill the screen around a sized down
   * refresh window. =============
   */
  public final void DrawTileClear(int x, int y, int w, int h, String pic) {
    Image image;
    GlState.gl.log("DrawTileClear " + pic);

    image = Images.findPicture(pic);
    if (image == null) {
      Window.Printf(Constants.PRINT_ALL, "Can't find pic: " + pic + '\n');
      return;
    }

    // if ( ( ( gl_config.renderer == GL_RENDERER_MCD ) || ( (gl_config.renderer
    // & GL_RENDERER_RENDITION) != 0 ) ) && !image.has_alpha)
    // gl.glDisable(GLAdapter.GL_ALPHA_TEST);

    Images.GL_Bind(image.texnum);
    GlState.gl.glBegin(Gl1Context._GL_QUADS);
    GlState.gl.glTexCoord2f(x / 64.0f, y / 64.0f);
    GlState.gl.glVertex2f(x, y);
    GlState.gl.glTexCoord2f((x + w) / 64.0f, y / 64.0f);
    GlState.gl.glVertex2f(x + w, y);
    GlState.gl.glTexCoord2f((x + w) / 64.0f, (y + h) / 64.0f);
    GlState.gl.glVertex2f(x + w, y + h);
    GlState.gl.glTexCoord2f(x / 64.0f, (y + h) / 64.0f);
    GlState.gl.glVertex2f(x, y + h);
    GlState.gl.glEnd();

    // if ( ( ( gl_config.renderer == GL_RENDERER_MCD ) || ( (gl_config.renderer
    // & GL_RENDERER_RENDITION) != 0 ) ) && !image.has_alpha)
    // gl.glEnable(GLAdapter.GL_ALPHA_TEST);
  }

  /*
   * ============= Draw_Fill
   * 
   * Fills a box of pixels with a single color =============
   */
  /**
   * @see com.googlecode.gwtquake.shared.client.Renderer#DrawFill
   */
  public void DrawFill(int x, int y, int w, int h, int colorIndex) {

    if (colorIndex > 255)
      Com.Error(Constants.ERR_FATAL, "Draw_Fill: bad color");

    GlState.gl.glDisable(Gl1Context.GL_TEXTURE_2D);

    int color = QuakeImage.PALETTE_ABGR[colorIndex];

    GlState.gl.glColor3ub((byte) ((color >> 0) & 0xff), // r
        (byte) ((color >> 8) & 0xff), // g
        (byte) ((color >> 16) & 0xff) // b
        );

    GlState.gl.glBegin(Gl1Context._GL_QUADS);

    GlState.gl.glVertex2f(x, y);
    GlState.gl.glVertex2f(x + w, y);
    GlState.gl.glVertex2f(x + w, y + h);
    GlState.gl.glVertex2f(x, y + h);

    GlState.gl.glEnd();
    GlState.gl.glColor3f(1, 1, 1);
    GlState.gl.glEnable(Gl1Context.GL_TEXTURE_2D);
  }

  // =============================================================================

  /*
   * ================ Draw_FadeScreen ================
   */
  /**
   * @see com.googlecode.gwtquake.shared.client.Renderer#DrawFadeScreen()
   */
  public void DrawFadeScreen() {
    GlState.gl.glEnable(Gl1Context.GL_BLEND);
    GlState.gl.glDisable(Gl1Context.GL_TEXTURE_2D);
    GlState.gl.glColor4f(0, 0, 0, 0.8f);
    GlState.gl.glBegin(Gl1Context._GL_QUADS);

    GlState.gl.glVertex2f(0, 0);
    GlState.gl.glVertex2f(GlState.vid.width, 0);
    GlState.gl.glVertex2f(GlState.vid.width, GlState.vid.height);
    GlState.gl.glVertex2f(0, GlState.vid.height);

    GlState.gl.glEnd();
    GlState.gl.glColor4f(1, 1, 1, 1);
    GlState.gl.glEnable(Gl1Context.GL_TEXTURE_2D);
    GlState.gl.glDisable(Gl1Context.GL_BLEND);
  }

  /*
   * ============= Draw_Pic =============
   */
  public void DrawPic(int x, int y, String pic) {
    Image image;

    image = Images.findPicture(pic);
    if (image == null) {
      Window.Printf(Constants.PRINT_ALL, "Can't find pic: " + pic + '\n');
      return;
    }
    // if (scrap_dirty)
    // Scrap_Upload();

    // if ( ( ( gl_config.renderer == GL_RENDERER_MCD ) || ( (gl_config.renderer
    // & GL_RENDERER_RENDITION) != 0 ) ) && !image.has_alpha)
    // gl.glDisable (GLAdapter.GL_ALPHA_TEST);

    Images.GL_Bind(image.texnum);

    GlState.gl.glBegin(Gl1Context.SIMPLE_TEXUTRED_QUAD);
    GlState.gl.glVertex2f(x, y);
    GlState.gl.glVertex2f(x + image.width, y);
    GlState.gl.glVertex2f(x + image.width, y + image.height);
    GlState.gl.glVertex2f(x, y + image.height);
    GlState.gl.glEnd();

    // if ( ( ( gl_config.renderer == GL_RENDERER_MCD ) || ( (gl_config.renderer
    // & GL_RENDERER_RENDITION) != 0 ) ) && !image.has_alpha)
    // gl.glEnable (GLAdapter.GL_ALPHA_TEST);
  }

  /*
   * ============= Draw_StretchRaw =============
   */
  public final void DrawStretchRaw(int x, int y, int w, int h, int cols,
      int rows, byte[] data) {
    int i, j, trows;
    int sourceIndex;
    int frac, fracstep;
    float hscale;
    int row;
    float t;

    Images.GL_Bind(0);

    if (rows <= 256) {
      hscale = 1;
      trows = rows;
    } else {
      hscale = rows / 256.0f;
      trows = 256;
    }
    t = rows * hscale / 256;

    // if ( !qglColorTableEXT )
    // {
    // int[] image32 = new int[256*256];
    Drawing.image32.clear();
    int destIndex = 0;

    for (i = 0; i < trows; i++) {
      row = (int) (i * hscale);
      if (row > rows)
        break;
      sourceIndex = cols * row;
      destIndex = i * 256;
      fracstep = cols * 0x10000 / 256;
      frac = fracstep >> 1;
      for (j = 0; j < 256; j++) {
        Drawing.image32.put(destIndex + j,
            GlState.r_rawpalette[data[sourceIndex + (frac >> 16)] & 0xff]);
        frac += fracstep;
      }
    }
    GlState.gl.glTexImage2D(Gl1Context.GL_TEXTURE_2D, 0,
        Gl1Context.GL_RGBA/* gl_tex_solid_format */, 256, 256, 0,
        Gl1Context.GL_RGBA, Gl1Context.GL_UNSIGNED_BYTE, Drawing.image32);
    // }
    // else
    // {
    // //byte[] image8 = new byte[256*256];
    // image8.clear();
    // int destIndex = 0;;
    //
    // for (i=0 ; i<trows ; i++)
    // {
    // row = (int)(i*hscale);
    // if (row > rows)
    // break;
    // sourceIndex = cols*row;
    // destIndex = i*256;
    // fracstep = cols*0x10000/256;
    // frac = fracstep >> 1;
    // for (j=0 ; j<256 ; j++)
    // {
    // image8.put(destIndex + j, data[sourceIndex + (frac>>16)]);
    // frac += fracstep;
    // }
    // }
    //
    // gl.glTexImage2D( GLAdapter.GL_TEXTURE_2D,
    // 0,
    // GL_COLOR_INDEX8_EXT,
    // 256, 256,
    // 0,
    // GLAdapter._GL_COLOR_INDEX,
    // GLAdapter.GL_UNSIGNED_BYTE,
    // image8 );
    // }
    GlState.gl.glTexParameterf(Gl1Context.GL_TEXTURE_2D,
        Gl1Context.GL_TEXTURE_MIN_FILTER, Gl1Context.GL_LINEAR);
    GlState.gl.glTexParameterf(Gl1Context.GL_TEXTURE_2D,
        Gl1Context.GL_TEXTURE_MAG_FILTER, Gl1Context.GL_LINEAR);

    // if ( ( gl_config.renderer == GL_RENDERER_MCD ) || ( (gl_config.renderer &
    // GL_RENDERER_RENDITION) != 0 ) )
    // gl.glDisable (GLAdapter.GL_ALPHA_TEST);

    GlState.gl.glBegin(Gl1Context._GL_QUADS);
    GlState.gl.glTexCoord2f(0, 0);
    GlState.gl.glVertex2f(x, y);
    GlState.gl.glTexCoord2f(1, 0);
    GlState.gl.glVertex2f(x + w, y);
    GlState.gl.glTexCoord2f(1, t);
    GlState.gl.glVertex2f(x + w, y + h);
    GlState.gl.glTexCoord2f(0, t);
    GlState.gl.glVertex2f(x, y + h);
    GlState.gl.glEnd();

    // if ( ( gl_config.renderer == GL_RENDERER_MCD ) || ( (gl_config.renderer &
    // GL_RENDERER_RENDITION) != 0 ) )
    // gl.glEnable (GLAdapter.GL_ALPHA_TEST);
  }

  /**
   * @param dim
   * @param mode
   * @param fullscreen
   * @return enum rserr_t
   */
  public int GLimp_SetMode(Dimension dim, int mode, boolean fullscreen) {

    // TODO: jgw
    fullscreen = false;

    GlState.gl.log("GLimp_SetMode");

    Dimension newDim = new Dimension(dim.width, dim.height);

    /*
     * fullscreen handling
     */

    GlState.gl.log("determining old display mode");
    if (GlState.oldDisplayMode == null) {
      GlState.oldDisplayMode = getDisplayMode();
    }

    // destroy the existing window
    GlState.gl.shutdow();

    GlState.gl.log("searching new display mode");
    DisplayMode displayMode = DisplayModes.findDisplayMode(newDim);
    GlState.gl.log("copying w/h");
    newDim.width = displayMode.getWidth();
    newDim.height = displayMode.getHeight();

    GlState.gl.log("setting mode: " + displayMode);

    this.width = newDim.width;
    this.height = newDim.height;

    GlState.gl.log("storing mode");
    GlState.vid.width = newDim.width;
    GlState.vid.height = newDim.height;

    // let the sound and input subsystems know about the new window
    GlState.gl.log("newWindow notification");
    Window.NewWindow(GlState.vid.width, GlState.vid.height);
    return GlConstants.rserr_ok;
  }

  /**
   * R_SetMode
   */
  protected boolean R_SetMode() {
    boolean fullscreen = (GlConfig.vid_fullscreen.value > 0.0f);

    GlConfig.vid_fullscreen.modified = false;
    GlConfig.gl_mode.modified = false;

    Dimension dim = new Dimension(GlState.vid.width, GlState.vid.height);

    int err; // enum rserr_t
    if ((err = GLimp_SetMode(dim, (int) GlConfig.gl_mode.value, fullscreen)) == GlConstants.rserr_ok) {
      GlConfig.gl_state.prev_mode = (int) GlConfig.gl_mode.value;
    } else {
      if (err == GlConstants.rserr_invalid_fullscreen) {
        ConsoleVariables.SetValue("vid_fullscreen", 0);
        GlConfig.vid_fullscreen.modified = false;
        Window.Printf(Constants.PRINT_ALL,
            "ref_gl::R_SetMode() - fullscreen unavailable in this mode\n");
        if ((err = GLimp_SetMode(dim, (int) GlConfig.gl_mode.value, false)) == GlConstants.rserr_ok)
          return true;
      } else if (err == GlConstants.rserr_invalid_mode) {
        ConsoleVariables.SetValue("gl_mode", GlConfig.gl_state.prev_mode);
        GlConfig.gl_mode.modified = false;
        Window.Printf(Constants.PRINT_ALL,
            "ref_gl::R_SetMode() - invalid mode\n");
      }

      // try setting it back to something safe
      if ((err = GLimp_SetMode(dim, GlConfig.gl_state.prev_mode, false)) != GlConstants.rserr_ok) {
        Window.Printf(Constants.PRINT_ALL,
            "ref_gl::R_SetMode() - could not revert to safe mode\n");
        return false;
      }
    }
    return true;
  }


 /**
   * this is a hack for jogl renderers.
   * 
   * @param callback
   */
    public final void updateScreen(ExecutableCommand callback) {
        callback.execute();
    }   
  
  
  /**
   * R_BeginFrame
   */
  public final void BeginFrame(float camera_separation) {

    GlConfig.gl_state.camera_separation = camera_separation;

    /*
     * * change modes if necessary
     */
    if (GlConfig.gl_mode.modified || GlConfig.vid_fullscreen.modified) {
      // FIXME: only restart if CDS is required
      ConsoleVariable ref;

      ref = ConsoleVariables.Get("vid_ref", "lwjgl", 0);
      ref.modified = true;
    }

    if (GlConfig.gl_log.modified) {
      // GlBase.GLimp_EnableLogging((GlState.gl_log.value != 0.0f));
      GlConfig.gl_log.modified = false;
    }

    if (GlConfig.gl_log.value != 0.0f) {
      // GlBase.GLimp_LogNewFrame();
    }

    /*
     * * update 3Dfx gamma -- it is expected that a user will do a vid_restart*
     * after tweaking this value
     */
    if (GlConfig.vid_gamma.modified) {
      GlConfig.vid_gamma.modified = false;
    }

    /*
     * * go into 2D mode
     */
    GlState.gl.glViewport(0, 0, GlState.vid.width, GlState.vid.height);
    GlState.gl.glMatrixMode(Gl1Context.GL_PROJECTION);
    GlState.gl.glLoadIdentity();
    GlState.gl.glOrtho(0, GlState.vid.width, GlState.vid.height, 0, -99999,
        99999);
    GlState.gl.glMatrixMode(Gl1Context.GL_MODELVIEW);
    GlState.gl.glLoadIdentity();
    GlState.gl.glDisable(Gl1Context.GL_DEPTH_TEST);
    GlState.gl.glDisable(Gl1Context.GL_CULL_FACE);
    GlState.gl.glDisable(Gl1Context.GL_BLEND);
    // gl.glEnable(GLAdapter.GL_ALPHA_TEST);
    GlState.gl.glColor4f(1, 1, 1, 1);

    /*
     * * draw buffer stuff
     */
    if (GlConfig.gl_drawbuffer.modified) {
      GlConfig.gl_drawbuffer.modified = false;

      if (GlState.camera_separation == 0 || !GlState.stereo_enabled) {
        if (GlConfig.gl_drawbuffer.string.equalsIgnoreCase("GL_FRONT"))
          GlState.gl.glDrawBuffer(Gl1Context.GL_FRONT);
        else
          GlState.gl.glDrawBuffer(Gl1Context.GL_BACK);
      }
    }

    /*
     * * texturemode stuff
     */
    if (GlConfig.gl_texturemode.modified) {
      Images.GL_TextureMode(GlConfig.gl_texturemode.string);
      GlConfig.gl_texturemode.modified = false;
    }

    if (GlConfig.gl_texturealphamode.modified) {
      Images.GL_TextureAlphaMode(GlConfig.gl_texturealphamode.string);
      GlConfig.gl_texturealphamode.modified = false;
    }

    if (GlConfig.gl_texturesolidmode.modified) {
      Images.GL_TextureSolidMode(GlConfig.gl_texturesolidmode.string);
      GlConfig.gl_texturesolidmode.modified = false;
    }

    /*
     * * swapinterval stuff
     */
    Misc.GL_UpdateSwapInterval();

    //
    // clear screen if desired
    //
    Entities.R_Clear();
  }

}

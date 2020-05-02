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
package com.googlecode.gwtquake.shared.client;

import com.googlecode.gwtquake.shared.common.AsyncCallback;
import com.googlecode.gwtquake.shared.common.Command;
import com.googlecode.gwtquake.shared.common.ExecutableCommand;
import com.googlecode.gwtquake.shared.render.DisplayMode;
import com.googlecode.gwtquake.shared.render.Image;
import com.googlecode.gwtquake.shared.render.Model;
import com.googlecode.gwtquake.shared.sys.KBD;


/**
 * refexport_t
 * 
 * @author cwei
 */
public interface Renderer {
	// ============================================================================
	// public interface for Renderer implementations
	//
	// ref.h, refexport_t
	// ============================================================================
	//
	// these are the functions exported by the refresh module
	//
	// called when the library is loaded
	boolean Init(int vid_xpos, int vid_ypos);

	// called before the library is unloaded
	void Shutdown();

	// All data that will be used in a level should be
	// registered before rendering any frames to prevent disk hits,
	// but they can still be registered at a later time
	// if necessary.
	//
	// EndRegistration will free any remaining data that wasn't registered.
	// Any model_s or skin_s pointers from before the BeginRegistration
	// are no longer valid after EndRegistration.
	//
	// Skins and images need to be differentiated, because skins
	// are flood filled to eliminate mip map edge errors, and pics have
	// an implicit "pics/" prepended to the name. (a pic name that starts with a
	// slash will not use the "pics/" prefix or the ".pcx" postfix)
	void BeginRegistration(String map, Command continueCommand);
	void RegisterModel(String name, AsyncCallback<Model> callback);
	Image RegisterSkin(String name);
	Image RegisterPic(String name);
	void SetSky(String name, float rotate, /* vec3_t */
	float[] axis);
	void EndRegistration();

	void RenderFrame(RendererState fd);

	void DrawGetPicSize(Dimension dim /* int *w, *h */, String name);
	// will return 0 0 if not found
	void DrawPic(int x, int y, String name);
	void DrawStretchPic(int x, int y, int w, int h, String name);
  void DrawChar(int x, int y, int ch);
  void DrawString(int x, int y, String str);
  void DrawString(int x, int y, String str, boolean alt);
	void DrawString(int x, int y, String str, int ofs, int len);
  void DrawString(int x, int y, String str, int ofs, int len, boolean alt);
  void DrawString(int x, int y, byte[] str, int ofs, int len);
	void DrawTileClear(int x, int y, int w, int h, String name);
	void DrawFill(int x, int y, int w, int h, int c);
	void DrawFadeScreen();

	// Draw images for cinematic rendering (which can have a different palette). Note that calls
	void DrawStretchRaw(int x,	int y, int w, int h, int cols, int rows, byte[] data);
	
	/*
	** video mode and refresh state management entry points
	*/
	/* 256 r,g,b values;	null = game palette, size = 768 bytes */
	void CinematicSetPalette(final byte[] palette);
	void BeginFrame(float camera_separation);
	void EndFrame();

	void AppActivate(boolean activate);
	
	void updateScreen(ExecutableCommand callback);
	
	int apiVersion();
	
	KBD getKeyboardHandler();

	int GLimp_SetMode(Dimension dimension, int i, boolean b);

	boolean showVideo(String name);
	boolean updateVideo();

  void GL_ResampleTexture(int[] data, int width, int height, int[] scaled,
      int scaled_width, int scaled_height);

  Image GL_LoadNewImage(String name, int type);
  
  public DisplayMode[] getAvailableDisplayModes();
  
  public DisplayMode getDisplayMode();

}

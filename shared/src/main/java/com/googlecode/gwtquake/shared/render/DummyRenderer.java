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

import com.googlecode.gwtquake.shared.client.Dimension;
import com.googlecode.gwtquake.shared.client.Renderer;
import com.googlecode.gwtquake.shared.client.RendererState;
import com.googlecode.gwtquake.shared.common.AsyncCallback;
import com.googlecode.gwtquake.shared.common.Command;
import com.googlecode.gwtquake.shared.common.ExecutableCommand;
import com.googlecode.gwtquake.shared.sys.KBD;


/**
 * DummyRenderer
 * 
 * @author cwei
 */
public class DummyRenderer implements Renderer {

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#Init(int, int)
	 */
	public boolean Init(int vid_xpos, int vid_ypos) {
		return false;
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#Shutdown()
	 */
	public void Shutdown() {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#BeginRegistration(java.lang.String)
	 */
	public void BeginRegistration(String map, Command callback) {
	  callback.execute();
	}

	public void RegisterModel(String name, AsyncCallback<Model> callback) {
	}

  /* (non-Javadoc)
	 * @see jake2.client.refexport_t#RegisterSkin(java.lang.String)
	 */
	public Image RegisterSkin(String name) {
		return null;
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#RegisterPic(java.lang.String)
	 */
	public Image RegisterPic(String name) {
		return null;
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#SetSky(java.lang.String, float, float[])
	 */
	public void SetSky(String name, float rotate, float[] axis) {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#EndRegistration()
	 */
	public void EndRegistration() {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#RenderFrame(jake2.client.refdef_t)
	 */
	public void RenderFrame(RendererState fd) {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#DrawGetPicSize(java.awt.Dimension, java.lang.String)
	 */
	public void DrawGetPicSize(Dimension dim, String name) {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#DrawPic(int, int, java.lang.String)
	 */
	public void DrawPic(int x, int y, String name) {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#DrawStretchPic(int, int, int, int, java.lang.String)
	 */
	public void DrawStretchPic(int x, int y, int w, int h, String name) {
	}

  public void DrawChar(int x, int y, int ch) {
  }

  public void DrawString(int x, int y, byte[] str, int ofs, int len) {
	}

	public void DrawString(int x, int y, String str) {
  }

  public void DrawString(int x, int y, String str, boolean alt) {
  }

  public void DrawString(int x, int y, String str, int ofs, int len) {
  }

  public void DrawString(int x, int y, String str, int ofs, int len, boolean alt) {
  }

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#DrawTileClear(int, int, int, int, java.lang.String)
	 */
	public void DrawTileClear(int x, int y, int w, int h, String name) {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#DrawFill(int, int, int, int, int)
	 */
	public void DrawFill(int x, int y, int w, int h, int c) {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#DrawFadeScreen()
	 */
	public void DrawFadeScreen() {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#DrawStretchRaw(int, int, int, int, int, int, byte[])
	 */
	public void DrawStretchRaw(int x, int y, int w, int h, int cols, int rows, byte[] data) {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#CinematicSetPalette(byte[])
	 */
	public void CinematicSetPalette(byte[] palette) {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#BeginFrame(float)
	 */
	public void BeginFrame(float camera_separation) {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#EndFrame()
	 */
	public void EndFrame() {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#AppActivate(boolean)
	 */
	public void AppActivate(boolean activate) {
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#updateScreen(jake2.qcommon.xcommand_t)
	 */
	public void updateScreen(ExecutableCommand callback) {
	    callback.execute();
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#apiVersion()
	 */
	public int apiVersion() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see jake2.client.refexport_t#getKeyboardHandler()
	 */
	public KBD getKeyboardHandler() {
		return null;
	}

	public int GLimp_SetMode(Dimension dimension, int i, boolean b) {
		// TODO Auto-generated method stub
		return 0;
	}
	

	public boolean showVideo(String name) {
		return false;
	}

	public boolean updateVideo() {
		// TODO Auto-generated method stub
		return false;
	}

  @Override
  public void GL_ResampleTexture(int[] data, int width, int height,
      int[] scaled, int scaled_width, int scaled_height) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Image GL_LoadNewImage(String name, int type) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DisplayMode[] getAvailableDisplayModes() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DisplayMode getDisplayMode() {
    // TODO Auto-generated method stub
    return null;
  }

}

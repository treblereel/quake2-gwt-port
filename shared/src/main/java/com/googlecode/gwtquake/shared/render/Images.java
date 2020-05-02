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
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashMap;

import com.googlecode.gwtquake.shared.client.Window;
import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.ConsoleVariables;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.Globals;
import com.googlecode.gwtquake.shared.common.QuakeImage;
import com.googlecode.gwtquake.shared.game.ConsoleVariable;
import com.googlecode.gwtquake.shared.util.Lib;
import com.googlecode.gwtquake.shared.util.Vargs;
import elemental2.dom.DomGlobal;

/**
 * Image
 * 
 * @author cwei
 */
public abstract class Images {

    static int waitingForImages = 0;
    
    static Image draw_chars;
 
    static Image[] gltextures = new Image[GlConstants.MAX_GLTEXTURES];
    //Map gltextures = new Hashtable(MAX_GLTEXTURES); // image_t
    static int numgltextures;
    static int base_textureid; // gltextures[i] = base_textureid+i

    static byte[] intensitytable = new byte[256];
    static byte[] gammatable = new byte[256];

    static ConsoleVariable intensity;

    //
    //  qboolean GL_Upload8 (byte *data, int width, int height,  qboolean mipmap, qboolean is_sky );
    //  qboolean GL_Upload32 (unsigned *data, int width, int height,  qboolean mipmap);
    //

    static int gl_solid_format = 3;
    static int gl_alpha_format = 4;

    static int gl_tex_solid_format = 3;
    static int gl_tex_alpha_format = 4;

    static int gl_filter_min = Gl1Context.GL_LINEAR;//GLAdapter.GL_LINEAR_MIPMAP_NEAREST;
    static int gl_filter_max = Gl1Context.GL_LINEAR;
    
    static {
        // init the texture cache
        for (int i = 0; i < gltextures.length; i++)
        {
            gltextures[i] = new Image(i);
        }
        numgltextures = 0;
    }
    

    static void GL_SetTexturePalette(int[] palette) {

        assert(palette != null && palette.length == 256) : "int palette[256] bug";

        //int i;
        //byte[] temptable = new byte[768];

// TODO(jgw)
//      if (qglColorTableEXT && gl_ext_palettedtexture.value != 0.0f) 
//      {
//          ByteBuffer temptable=BufferUtils.createByteBuffer(768);
//          for (i = 0; i < 256; i++) {
//              temptable.put(i * 3 + 0, (byte) ((palette[i] >> 0) & 0xff));
//              temptable.put(i * 3 + 1, (byte) ((palette[i] >> 8) & 0xff));
//              temptable.put(i * 3 + 2, (byte) ((palette[i] >> 16) & 0xff));
//          }
//
//          gl.glColorTable(EXTSharedTexturePalette.GL_SHARED_TEXTURE_PALETTE_EXT, GLAdapter.GL_RGB, 256, GLAdapter.GL_RGB, GLAdapter.GL_UNSIGNED_BYTE, temptable);
//      }
    }

    static void GL_EnableMultitexture(boolean enable) {
        if (enable) {
            GL_SelectTexture(Gl1Context.GL_TEXTURE1);
            GlState.gl.glEnable(Gl1Context.GL_TEXTURE_2D);
            GL_TexEnv(Gl1Context.GL_REPLACE);
        }
        else {
            GL_SelectTexture(Gl1Context.GL_TEXTURE1);
            GlState.gl.glDisable(Gl1Context.GL_TEXTURE_2D);
            GL_TexEnv(Gl1Context.GL_REPLACE);
        }
        GL_SelectTexture(Gl1Context.GL_TEXTURE0);
        GL_TexEnv(Gl1Context.GL_REPLACE);
    }

    static void GL_SelectTexture(int texture /* GLenum */) {
        int tmu;

        tmu = (texture == Gl1Context.GL_TEXTURE0) ? 0 : 1;

        if (tmu == GlConfig.gl_state.currenttmu) {
            return;
        }

        GlConfig.gl_state.currenttmu = tmu;

        GlState.gl.glActiveTexture(texture);
        GlState.gl.glClientActiveTexture(texture);
    }

    static int[] lastmodes = { -1, -1 };

    public static void GL_TexEnv(int mode /* GLenum */
    ) {

        if (mode != lastmodes[GlConfig.gl_state.currenttmu]) {
          GlState.gl.glTexEnvi(Gl1Context.GL_TEXTURE_ENV, Gl1Context.GL_TEXTURE_ENV_MODE, mode);
            lastmodes[GlConfig.gl_state.currenttmu] = mode;
        }
    }

    public static void GL_Bind(int texnum) {

        if ((GlConfig.gl_nobind.value != 0) && (draw_chars != null)) {
            // performance evaluation option
            texnum = draw_chars.texnum;
        }
        if (GlConfig.gl_state.currenttextures[GlConfig.gl_state.currenttmu] == texnum)
            return;

        GlConfig.gl_state.currenttextures[GlConfig.gl_state.currenttmu] = texnum;
        GlState.gl.glBindTexture(Gl1Context.GL_TEXTURE_2D, texnum);
    }

    static void GL_MBind(int target /* GLenum */, int texnum) {
        GL_SelectTexture(target);
        if (target == Gl1Context.GL_TEXTURE0) {
            if (GlConfig.gl_state.currenttextures[0] == texnum)
                return;
        }
        else {
            if (GlConfig.gl_state.currenttextures[1] == texnum)
                return;
        }
        GL_Bind(texnum);
    }

    // glmode_t
    static class glmode_t {
        String name;
        int minimize, maximize;

        glmode_t(String name, int minimize, int maximze) {
            this.name = name;
            this.minimize = minimize;
            this.maximize = maximze;
        }
    }

    static final glmode_t modes[] =
        {
            new glmode_t("GL_NEAREST", Gl1Context.GL_NEAREST, Gl1Context.GL_NEAREST),
            new glmode_t("GL_LINEAR", Gl1Context.GL_LINEAR, Gl1Context.GL_LINEAR),
            new glmode_t("GL_NEAREST_MIPMAP_NEAREST", Gl1Context.GL_NEAREST_MIPMAP_NEAREST, Gl1Context.GL_NEAREST),
            new glmode_t("GL_LINEAR_MIPMAP_NEAREST", Gl1Context.GL_LINEAR_MIPMAP_NEAREST, Gl1Context.GL_LINEAR),
            new glmode_t("GL_NEAREST_MIPMAP_LINEAR", Gl1Context.GL_NEAREST_MIPMAP_LINEAR, Gl1Context.GL_NEAREST),
            new glmode_t("GL_LINEAR_MIPMAP_LINEAR", Gl1Context.GL_LINEAR_MIPMAP_LINEAR, Gl1Context.GL_LINEAR)};

    static final int NUM_GL_MODES = modes.length;

    // gltmode_t
    static class gltmode_t {
        String name;
        int mode;

        gltmode_t(String name, int mode) {
            this.name = name;
            this.mode = mode;
        }
    }

    static final gltmode_t[] gl_alpha_modes =
        {
            new gltmode_t("default", 4),
            new gltmode_t("GL_RGBA", Gl1Context.GL_RGBA),
//          new gltmode_t("GL_RGBA8", GL11.GL_RGBA8),
//          new gltmode_t("GL_RGB5_A1", GL11.GL_RGB5_A1),
//          new gltmode_t("GL_RGBA4", GL11.GL_RGBA4),
//          new gltmode_t("GL_RGBA2", GL11.GL_RGBA2),
            };

    static final int NUM_GL_ALPHA_MODES = gl_alpha_modes.length;

    static final gltmode_t[] gl_solid_modes =
        {
            new gltmode_t("default", 3),
            new gltmode_t("GL_RGB", Gl1Context.GL_RGB),
//          new gltmode_t("GL_RGB8", GL11.GL_RGB8),
//          new gltmode_t("GL_RGB5", GL11.GL_RGB5),
//          new gltmode_t("GL_RGB4", GL11.GL_RGB4),
//          new gltmode_t("GL_R3_G3_B2", GL11.GL_R3_G3_B2),
        //  #ifdef GL_RGB2_EXT
        //new gltmode_t("GL_RGB2", GL11.GL_RGB2_EXT)
        //  #endif
    };

    static final int NUM_GL_SOLID_MODES = gl_solid_modes.length;

    /*
    ===============
    GL_TextureMode
    ===============
    */
    static void GL_TextureMode(String string) {

        int i;
        for (i = 0; i < NUM_GL_MODES; i++) {
            if (modes[i].name.equalsIgnoreCase(string))
                break;
        }

        if (i == NUM_GL_MODES) {
            Window.Printf(Constants.PRINT_ALL, "bad filter name: [" + string + "]\n");
            return;
        }

        gl_filter_min = modes[i].minimize;
        gl_filter_max = modes[i].maximize;

        Image glt;
        // change all the existing mipmap texture objects
        for (i = 0; i < numgltextures; i++) {
            glt = gltextures[i];

            if (glt.type != QuakeImage.it_pic && glt.type != QuakeImage.it_sky) {
                GL_Bind(glt.texnum);
                GlState.gl.glTexParameterf(Gl1Context.GL_TEXTURE_2D, Gl1Context.GL_TEXTURE_MIN_FILTER, gl_filter_min);
                GlState.gl.glTexParameterf(Gl1Context.GL_TEXTURE_2D, Gl1Context.GL_TEXTURE_MAG_FILTER, gl_filter_max);
            }
        }
    }

    /*
    ===============
    GL_TextureAlphaMode
    ===============
    */
    static void GL_TextureAlphaMode(String string) {

        int i;
        for (i = 0; i < NUM_GL_ALPHA_MODES; i++) {
            if (gl_alpha_modes[i].name.equalsIgnoreCase(string))
                break;
        }

        if (i == NUM_GL_ALPHA_MODES) {
            Window.Printf(Constants.PRINT_ALL, "bad alpha texture mode name: [" + string + "]\n");
            return;
        }

        gl_tex_alpha_format = gl_alpha_modes[i].mode;
    }

    /*
    ===============
    GL_TextureSolidMode
    ===============
    */
    static void GL_TextureSolidMode(String string) {
        int i;
        for (i = 0; i < NUM_GL_SOLID_MODES; i++) {
            if (gl_solid_modes[i].name.equalsIgnoreCase(string))
                break;
        }

        if (i == NUM_GL_SOLID_MODES) {
            Window.Printf(Constants.PRINT_ALL, "bad solid texture mode name: [" + string + "]\n");
            return;
        }

        gl_tex_solid_format = gl_solid_modes[i].mode;
    }

    /*
    ===============
    GL_ImageList_f
    ===============
    */
    static void GL_ImageList_f() {

        Image image;
        int texels;
        final String[] palstrings = { "RGB", "PAL" };

        Window.Printf(Constants.PRINT_ALL, "------------------\n");
        texels = 0;

        for (int i = 0; i < numgltextures; i++) {
            image = gltextures[i];
            if (image.texnum <= 0)
                continue;

            texels += image.upload_width * image.upload_height;
            switch (image.type) {
                case QuakeImage.it_skin :
                    Window.Printf(Constants.PRINT_ALL, "M");
                    break;
                case QuakeImage.it_sprite :
                    Window.Printf(Constants.PRINT_ALL, "S");
                    break;
                case QuakeImage.it_wall :
                    Window.Printf(Constants.PRINT_ALL, "W");
                    break;
                case QuakeImage.it_pic :
                    Window.Printf(Constants.PRINT_ALL, "P");
                    break;
                default :
                    Window.Printf(Constants.PRINT_ALL, " ");
                    break;
            }

            Window.Printf(
                Constants.PRINT_ALL,
                " %3i %3i %s: %s\n",
                new Vargs(4).add(image.upload_width).add(image.upload_height).add(palstrings[(image.paletted) ? 1 : 0]).add(
                    image.name));
        }
        Window.Printf(Constants.PRINT_ALL, "Total texel count (not counting mipmaps): " + texels + '\n');
    }

    static class pos_t {
        int x, y;

        pos_t(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }



    

    /*
    =================================================================
    
    PCX LOADING
    
    =================================================================
    */

   // private Throwable gotoBreakOut = new Throwable();
   // private Throwable gotoDone = gotoBreakOut;

    static class floodfill_t {
        short x, y;
    }

    //  void FLOODFILL_STEP( int off, int dx, int dy )
    //  {
    //      if (pos[off] == fillcolor)
    //      {
    //          pos[off] = 255;
    //          fifo[inpt].x = x + dx; fifo[inpt].y = y + dy;
    //          inpt = (inpt + 1) & FLOODFILL_FIFO_MASK;
    //      }
    //      else if (pos[off] != 255) fdc = pos[off];
    //  }
    static floodfill_t[] fifo = new floodfill_t[GlConstants.FLOODFILL_FIFO_SIZE];
    static {
        for (int j = 0; j < fifo.length; j++) {
            fifo[j] = new floodfill_t();
        }       
    }
    // TODO check this: R_FloodFillSkin( byte[] skin, int skinwidth, int skinheight)
    static void R_FloodFillSkin(byte[] skin, int skinwidth, int skinheight) {
        //      byte                fillcolor = *skin; // assume this is the pixel to fill
        int fillcolor = skin[0] & 0xff;
//      floodfill_t[] fifo = new floodfill_t[FLOODFILL_FIFO_SIZE];
        int inpt = 0, outpt = 0;
        int filledcolor = -1;
        int i;

//      for (int j = 0; j < fifo.length; j++) {
//          fifo[j] = new floodfill_t();
//      }

        if (filledcolor == -1) {
            filledcolor = 0;
            // attempt to find opaque black
            for (i = 0; i < 256; ++i)
                // TODO check this
                if (QuakeImage.PALETTE_ABGR[i]  == 0xFF000000) { // alpha 1.0
                //if (d_8to24table[i] == (255 << 0)) // alpha 1.0
                    filledcolor = i;
                    break;
                }
        }

        // can't fill to filled color or to transparent color (used as visited marker)
        if ((fillcolor == filledcolor) || (fillcolor == 255)) {
            return;
        }

        fifo[inpt].x = 0;
        fifo[inpt].y = 0;
        inpt = (inpt + 1) & GlConstants.FLOODFILL_FIFO_MASK;

        while (outpt != inpt) {
            int x = fifo[outpt].x;
            int y = fifo[outpt].y;
            int fdc = filledcolor;
            //          byte        *pos = &skin[x + skinwidth * y];
            int pos = x + skinwidth * y;
            //
            outpt = (outpt + 1) & GlConstants.FLOODFILL_FIFO_MASK;

            int off, dx, dy;

            if (x > 0) {
                // FLOODFILL_STEP( -1, -1, 0 );
                off = -1;
                dx = -1;
                dy = 0;
                if (skin[pos + off] == (byte) fillcolor) {
                    skin[pos + off] = (byte) 255;
                    fifo[inpt].x = (short) (x + dx);
                    fifo[inpt].y = (short) (y + dy);
                    inpt = (inpt + 1) & GlConstants.FLOODFILL_FIFO_MASK;
                }
                else if (skin[pos + off] != (byte) 255)
                    fdc = skin[pos + off] & 0xff;
            }

            if (x < skinwidth - 1) {
                // FLOODFILL_STEP( 1, 1, 0 );
                off = 1;
                dx = 1;
                dy = 0;
                if (skin[pos + off] == (byte) fillcolor) {
                    skin[pos + off] = (byte) 255;
                    fifo[inpt].x = (short) (x + dx);
                    fifo[inpt].y = (short) (y + dy);
                    inpt = (inpt + 1) & GlConstants.FLOODFILL_FIFO_MASK;
                }
                else if (skin[pos + off] != (byte) 255)
                    fdc = skin[pos + off] & 0xff;
            }

            if (y > 0) {
                // FLOODFILL_STEP( -skinwidth, 0, -1 );
                off = -skinwidth;
                dx = 0;
                dy = -1;
                if (skin[pos + off] == (byte) fillcolor) {
                    skin[pos + off] = (byte) 255;
                    fifo[inpt].x = (short) (x + dx);
                    fifo[inpt].y = (short) (y + dy);
                    inpt = (inpt + 1) & GlConstants.FLOODFILL_FIFO_MASK;
                }
                else if (skin[pos + off] != (byte) 255)
                    fdc = skin[pos + off] & 0xff;
            }

            if (y < skinheight - 1) {
                // FLOODFILL_STEP( skinwidth, 0, 1 );
                off = skinwidth;
                dx = 0;
                dy = 1;
                if (skin[pos + off] == (byte) fillcolor) {
                    skin[pos + off] = (byte) 255;
                    fifo[inpt].x = (short) (x + dx);
                    fifo[inpt].y = (short) (y + dy);
                    inpt = (inpt + 1) & GlConstants.FLOODFILL_FIFO_MASK;
                }
                else if (skin[pos + off] != (byte) 255)
                    fdc = skin[pos + off] & 0xff;

            }

            skin[x + skinwidth * y] = (byte) fdc;
        }
    }

    public static final int MAX_NAME_SIZE = Constants.MAX_QPATH;

    //    =======================================================

    /*
    ================
    GL_ResampleTexture
    ================
    */
    // cwei :-)
    abstract protected void GL_ResampleTexture(int[] in, int inwidth, int inheight, int[] out, int outwidth, int outheight); 

    /*
    ================
    GL_LightScaleTexture
    
    Scale up the pixel values in a texture to increase the
    lighting range
    ================
    */
    void GL_LightScaleTexture(int[] in, int inwidth, int inheight, boolean only_gamma) {
        if (only_gamma) {
            int i, c;
            int r, g, b, color;

            c = inwidth * inheight;
            for (i = 0; i < c; i++) {
                color = in[i];
                r = (color >> 0) & 0xFF;
                g = (color >> 8) & 0xFF;
                b = (color >> 16) & 0xFF;

                r = gammatable[r] & 0xFF;
                g = gammatable[g] & 0xFF;
                b = gammatable[b] & 0xFF;

                in[i] = (r << 0) | (g << 8) | (b << 16) | (color & 0xFF000000);
            }
        }
        else {
            int i, c;
            int r, g, b, color;

            c = inwidth * inheight;
            for (i = 0; i < c; i++) {
                color = in[i];
                r = (color >> 0) & 0xFF;
                g = (color >> 8) & 0xFF;
                b = (color >> 16) & 0xFF;

                r = gammatable[intensitytable[r] & 0xFF] & 0xFF;
                g = gammatable[intensitytable[g] & 0xFF] & 0xFF;
                b = gammatable[intensitytable[b] & 0xFF] & 0xFF;

                in[i] = (r << 0) | (g << 8) | (b << 16) | (color & 0xFF000000);
            }

        }
    }

    /*
    =============
    Draw_FindPic
    =============
    */
    protected static Image findPicture(String name) {
    	Image image = null;
    	String fullname;
    
    	if (!name.startsWith("/") && !name.startsWith("\\")) {
    		fullname = "pics/" + name + ".pcx";
    	} else {
    		fullname = name.substring(1);
    	}
    	image = findTexture(fullname, QuakeImage.it_pic);
    	return image;
    }

    /*
    ================
    GL_MipMap
    
    Operates in place, quartering the size of the texture
    ================
    */
    static void GL_MipMap(int[] in, int width, int height) {
        int i, j;
        int[] out;

        out = in;

        int inIndex = 0;
        int outIndex = 0;

        int r, g, b, a;
        int p1, p2, p3, p4;

        for (i = 0; i < height; i += 2, inIndex += width) {
            for (j = 0; j < width; j += 2, outIndex += 1, inIndex += 2) {

                p1 = in[inIndex + 0];
                p2 = in[inIndex + 1];
                p3 = in[inIndex + width + 0];
                p4 = in[inIndex + width + 1];

                r = (((p1 >> 0) & 0xFF) + ((p2 >> 0) & 0xFF) + ((p3 >> 0) & 0xFF) + ((p4 >> 0) & 0xFF)) >> 2;
                g = (((p1 >> 8) & 0xFF) + ((p2 >> 8) & 0xFF) + ((p3 >> 8) & 0xFF) + ((p4 >> 8) & 0xFF)) >> 2;
                b = (((p1 >> 16) & 0xFF) + ((p2 >> 16) & 0xFF) + ((p3 >> 16) & 0xFF) + ((p4 >> 16) & 0xFF)) >> 2;
                a = (((p1 >> 24) & 0xFF) + ((p2 >> 24) & 0xFF) + ((p3 >> 24) & 0xFF) + ((p4 >> 24) & 0xFF)) >> 2;

                out[outIndex] = (r << 0) | (g << 8) | (b << 16) | (a << 24);
            }
        }
    }

    static int upload_width; 
    static int upload_height;

    /*
    ===============
    GL_Upload32
    
    Returns has_alpha
    ===============
    */
    private static int[] scaled = new int[256 * 256];
    //byte[] paletted_texture = new byte[256 * 256];
//  ByteBuffer paletted_texture;
    private static IntBuffer tex = Lib.newIntBuffer(512 * 256, ByteOrder.LITTLE_ENDIAN);
    static HashMap<String,Image> imageMap = new HashMap<String,Image>();

    
    
    static boolean GL_Upload32(int[] data, int width, int height, boolean mipmap) {
        int samples;
        int scaled_width, scaled_height;
        int i, c;
        int comp;

        Arrays.fill(scaled, 0);
        // Arrays.fill(paletted_texture, (byte)0);
//      paletted_texture.clear();
//      for (int j=0; j<256*256; j++) paletted_texture.put(j,(byte)0);

        for (scaled_width = 1; scaled_width < width; scaled_width <<= 1);
        if (GlConfig.gl_round_down.value > 0.0f && scaled_width > width && mipmap)
            scaled_width >>= 1;
        for (scaled_height = 1; scaled_height < height; scaled_height <<= 1);
        if (GlConfig.gl_round_down.value > 0.0f && scaled_height > height && mipmap)
            scaled_height >>= 1;

        // let people sample down the world textures for speed
        if (mipmap) {
            scaled_width >>= (int) GlConfig.gl_picmip.value;
            scaled_height >>= (int) GlConfig.gl_picmip.value;
        }

        // don't ever bother with >256 textures
        if (scaled_width > 256)
            scaled_width = 256;
        if (scaled_height > 256)
            scaled_height = 256;

        if (scaled_width < 1)
            scaled_width = 1;
        if (scaled_height < 1)
            scaled_height = 1;

        upload_width = scaled_width;
        upload_height = scaled_height;

        if (scaled_width * scaled_height > 256 * 256)
            Com.Error(Constants.ERR_DROP, "GL_Upload32: too big");

        // scan the texture for any non-255 alpha
        c = width * height;
        samples = gl_solid_format;

        for (i = 0; i < c; i++) {
            if ((data[i] & 0xff000000) != 0xff000000) {
                samples = gl_alpha_format;
                break;
            }
        }

        if (samples == gl_solid_format)
            comp = gl_tex_solid_format;
        else if (samples == gl_alpha_format)
            comp = gl_tex_alpha_format;
        else {
            Window.Printf(Constants.PRINT_ALL, "Unknown number of texture components " + samples + '\n');
            comp = samples;
        }

        // simulates a goto
        try {
            if (scaled_width == width && scaled_height == height) {
                if (!mipmap) {
//                  if (qglColorTableEXT && gl_ext_palettedtexture.value != 0.0f && samples == gl_solid_format) {
//                      uploaded_paletted = true;
//                      GL_BuildPalettedTexture(paletted_texture, data, scaled_width, scaled_height);
//                      gl.glTexImage2D(
//                          GLAdapter.GL_TEXTURE_2D,
//                          0,
//                          GL_COLOR_INDEX8_EXT,
//                          scaled_width,
//                          scaled_height,
//                          0,
//                          GL11.GL_COLOR_INDEX,
//                          GLAdapter.GL_UNSIGNED_BYTE,
//                          paletted_texture);
//                  }
//                  else {
                        tex.rewind(); tex.put(data); tex.rewind();
                        GlState.gl.glTexImage2D(
                            Gl1Context.GL_TEXTURE_2D,
                            0,
                            Gl1Context.GL_RGBA/*comp*/,
                            scaled_width,
                            scaled_height,
                            0,
                            Gl1Context.GL_RGBA,
                            Gl1Context.GL_UNSIGNED_BYTE,
                            tex);
//                  }
                    //goto done;
                    throw new Exception("goto done");
                }
                //memcpy (scaled, data, width*height*4); were bytes
                System.arraycopy(data, 0, scaled, 0, width * height);
            }
            else
                Globals.re.GL_ResampleTexture(data, width, height, scaled, scaled_width, scaled_height);

        //  GL_LightScaleTexture(scaled, scaled_width, scaled_height, !mipmap);

//          if (qglColorTableEXT && gl_ext_palettedtexture.value != 0.0f && (samples == gl_solid_format)) {
//              uploaded_paletted = true;
//              GL_BuildPalettedTexture(paletted_texture, scaled, scaled_width, scaled_height);
//              gl.glTexImage2D(
//                  GLAdapter.GL_TEXTURE_2D,
//                  0,
//                  GL_COLOR_INDEX8_EXT,
//                  scaled_width,
//                  scaled_height,
//                  0,
//                  GL11.GL_COLOR_INDEX,
//                  GLAdapter.GL_UNSIGNED_BYTE,
//                  paletted_texture);
//          }
//          else {
                tex.rewind(); tex.put(scaled); tex.rewind();
                GlState.gl.glTexImage2D(Gl1Context.GL_TEXTURE_2D, 0, Gl1Context.GL_RGBA/*comp*/, scaled_width, scaled_height, 0, Gl1Context.GL_RGBA, Gl1Context.GL_UNSIGNED_BYTE, tex);
//          }

            if (mipmap) {
                int miplevel;
                miplevel = 0;
                while (scaled_width > 1 || scaled_height > 1) {
                    GL_MipMap(scaled, scaled_width, scaled_height);
                    scaled_width >>= 1;
                    scaled_height >>= 1;
                    if (scaled_width < 1)
                        scaled_width = 1;
                    if (scaled_height < 1)
                        scaled_height = 1;

                    miplevel++;
//                  if (qglColorTableEXT && gl_ext_palettedtexture.value != 0.0f && samples == gl_solid_format) {
//                      uploaded_paletted = true;
//                      GL_BuildPalettedTexture(paletted_texture, scaled, scaled_width, scaled_height);
//                      gl.glTexImage2D(
//                          GLAdapter.GL_TEXTURE_2D,
//                          miplevel,
//                          GL_COLOR_INDEX8_EXT,
//                          scaled_width,
//                          scaled_height,
//                          0,
//                          GL11.GL_COLOR_INDEX,
//                          GLAdapter.GL_UNSIGNED_BYTE,
//                          paletted_texture);
//                  }
//                  else {
                        tex.rewind(); tex.put(scaled); tex.rewind();
                        GlState.gl.glTexImage2D(
                            Gl1Context.GL_TEXTURE_2D,
                            miplevel,
                            Gl1Context.GL_RGBA/*comp*/,
                            scaled_width,
                            scaled_height,
                            0,
                            Gl1Context.GL_RGBA,
                            Gl1Context.GL_UNSIGNED_BYTE,
                            tex);
//                  }
                }
            }
            // label done:
        }
        catch (Throwable e) {
            // replaces label done
        }

        if (mipmap) {
          GlState.gl.glTexParameterf(Gl1Context.GL_TEXTURE_2D, Gl1Context.GL_TEXTURE_MIN_FILTER, gl_filter_min);
          GlState.gl.glTexParameterf(Gl1Context.GL_TEXTURE_2D, Gl1Context.GL_TEXTURE_MAG_FILTER, gl_filter_max);
        }
        else {
          GlState.gl.glTexParameterf(Gl1Context.GL_TEXTURE_2D, Gl1Context.GL_TEXTURE_MIN_FILTER, gl_filter_max);
          GlState.gl.glTexParameterf(Gl1Context.GL_TEXTURE_2D, Gl1Context.GL_TEXTURE_MAG_FILTER, gl_filter_max);
        }

        return (samples == gl_alpha_format);
    }

    /*
    ===============
    GL_Upload8
    
    Returns has_alpha
    ===============
    */


    static boolean GL_Upload8(byte[] data, int width, int height, boolean mipmap, boolean is_sky) {
        return GL_Upload32(QuakeImage.applyPalette(data, width, height, QuakeImage.PALETTE_ABGR), 
                width, height, mipmap);
    }

    public final static Image GL_Find_free_image_t(String name, int type) {
        Image image;
        int i;

        // find a free image_t
        for (i = 0; i<numgltextures ; i++)
        {
            image = gltextures[i];
            if (image.texnum == 0)
                break;
        }

        if (i == numgltextures)
        {
            if (numgltextures == GlConstants.MAX_GLTEXTURES)
                Com.Error (Constants.ERR_DROP, "MAX_GLTEXTURES");
            
            numgltextures++;
        }
        image = gltextures[i];

        if (name.length() > Constants.MAX_QPATH)
            Com.Error(Constants.ERR_DROP, "Draw_LoadPic: \"" + name + "\" is too long");

        image.name = name;
        image.type = type;
        image.registration_sequence = GlState.registration_sequence;
        image.width = image.upload_width = 32;
        image.height = image.upload_height = 32;
        image.complete = false;
        image.texnum = GlConstants.TEXNUM_IMAGES + image.getId();
        GL_Bind(image.texnum);
        
        return image;
    }
    static Image GL_LoadPic(String name, byte[] pic, int width, int height, int type, int bits) {
        Image image = GL_Find_free_image_t(name, type);
        image.setData(pic, width, height, bits);
        return image;
    }
        
    /*
    ===============
    GL_FindImage
    
    Finds or loads the given image
    ===============
    */
    static Image findTexture(String name, int type) {
//      // TODO loest das grossschreibungs problem
//      name = name.toLowerCase();
//      // bughack for bad strings (fuck \0)
//      int index = name.indexOf('\0');
//      if (index != -1) 
//          name = name.substring(0, index);

        if (name == null || name.length() < 5)
            return null; // Com.Error (ERR_DROP, "GL_FindImage: NULL name");
        //  Com.Error (ERR_DROP, "GL_FindImage: bad name: %s", name);

        // look for it
        
        Image image = imageMap.get(name);
        
        if (image != null && name.equals(image.name)) {
            image.registration_sequence = GlState.registration_sequence;
            return image;
        }
        image = Globals.re.GL_LoadNewImage(name, type);
        imageMap.put(name, image);
        return image;
    }
        /*
    static ModelImage GL_LoadNewImage(String name, int type) {
        //
        // load the pic from disk
        //
        ModelImage image = null;
        byte[] pic = null;
        Dimension dim = new Dimension();

        //
        // load the file
        //
        byte[] raw = QuakeFileSystem.LoadFile(name);
        if (raw == null) {
            Window.Printf(Constants.PRINT_ALL, "GL_FindImage: can't load " + name + '\n');
            return GlState.r_notexture;
        }
        
        if (name.endsWith(".pcx")) {
            pic = QuakeImage.LoadPCX(raw, null, dim);
            image = GL_LoadPic(name, pic, dim.width, dim.height, type, 8);
        }
        else if (name.endsWith(".wal")) {
            pic = QuakeImage.GL_LoadWal(raw, dim);
            image = GL_LoadPic(name, pic, dim.width, dim.height, type, 8);
        }
        else if (name.endsWith(".tga")) {

            pic = QuakeImage.LoadTGA(raw, dim);

            if (pic == null)
                return null;

            image = GL_LoadPic(name, pic, dim.width, dim.height, type, 32);

        } else throw new RuntimeException("unknow image type!");

        return image;
    }
*/
    /*
    ===============
    R_RegisterSkin
    ===============
    */
    protected static Image R_RegisterSkin(String name) {
        return findTexture(name, QuakeImage.it_skin);
    }

    
    static IntBuffer texnumBuffer;
    
    static void init() {
//      paletted_texture = gl.createByteBuffer(256*256);
        texnumBuffer=Lib.newIntBuffer(1);
    }
    
    /*
    ================
    GL_FreeUnusedImages
    
    Any image that was not touched on this registration sequence
    will be freed.
    ================
    */
    static void GL_FreeUnusedImages() {

        // never free r_notexture or particle texture
        GlState.r_notexture.registration_sequence = GlState.registration_sequence;
        GlState.r_particletexture.registration_sequence = GlState.registration_sequence;

        Image image = null;

        for (int i = 0; i < numgltextures; i++) {
            image = gltextures[i];
            // used this sequence
            if (image.registration_sequence == GlState.registration_sequence)
                continue;
            // free image_t slot
            if (image.registration_sequence == 0)
                continue;
            // don't free pics
            if (image.type == QuakeImage.it_pic)
                continue;

            // free it
            // TODO jogl bug
            texnumBuffer.clear();
            texnumBuffer.put(0,image.texnum);
            GlState.gl.glDeleteTextures(texnumBuffer);
            image.clear();
        }
    }

    /*
    ===============
    Draw_GetPalette
    ===============
    */
    protected static void Draw_GetPalette() {
      // HACK(jgw): This used to load from pics/colormap.pcx, but it was a pain to
      // do this correctly without a sync load, and I see no evidence that this
      // colormap ever changes.
//    d_8to24table = new int[] { ... } (this is now set directly in the static initializer)
    Particles.setColorPalette(QuakeImage.PALETTE_ABGR);
  }

    /*
    ===============
    GL_InitImages
    ===============
    */
    static void GL_InitImages() {
        int i, j;
        float g = GlConfig.vid_gamma.value;

        GlState.registration_sequence = 1;

        // init intensity conversions
        intensity = ConsoleVariables.Get("intensity", "2", 0);

        if (intensity.value <= 1)
            ConsoleVariables.Set("intensity", "1");

        GlConfig.gl_state.inverse_intensity = 1 / intensity.value;

        Draw_GetPalette();

        for (i = 0; i < 256; i++) {
            if (g == 1.0f) {
                gammatable[i] = (byte) i;
            }
            else {

                int inf = (int) (255.0f * Math.pow((i + 0.5) / 255.5, g) + 0.5);
                if (inf < 0)
                    inf = 0;
                if (inf > 255)
                    inf = 255;
                gammatable[i] = (byte) inf;
            }
        }

        for (i = 0; i < 256; i++) {
            j = (int) (i * intensity.value);
            if (j > 255)
                j = 255;
            intensitytable[i] = (byte) j;
        }
    }

    /*
    ===============
    GL_ShutdownImages
    ===============
    */
    static void GL_ShutdownImages() {
        Image image;
        
        for (int i=0; i < numgltextures ; i++)
        {
            image = gltextures[i];
            
            if (image.registration_sequence == 0)
                continue; // free image_t slot
            // free it
            // TODO jogl bug
            texnumBuffer.clear();
            texnumBuffer.put(0,image.texnum);
            GlState.gl.glDeleteTextures(texnumBuffer);
            image.clear();
        }
    }
}

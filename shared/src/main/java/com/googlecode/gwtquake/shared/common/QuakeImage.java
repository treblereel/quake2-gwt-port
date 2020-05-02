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
package com.googlecode.gwtquake.shared.common;


import java.nio.ByteBuffer;

import com.googlecode.gwtquake.shared.client.Dimension;



public class QuakeImage {
	/*
	 * skins will be outline flood filled and mip mapped pics and sprites with
	 * alpha will be outline flood filled pic won't be mip mapped
	 * 
	 * model skin sprite frame wall texture pic
	 */
	// enum imagetype_t
	public static final int it_skin = 0;
	public static final int it_sprite = 1;
	public static final int it_wall = 2;
	public static final int it_pic = 3;
	public static final int it_sky = 4;
	
	public static final int[] PALETTE_ABGR = new int[] {
		0xff000000, 0xff0f0f0f, 0xff1f1f1f, 0xff2f2f2f,
		0xff3f3f3f, 0xff4b4b4b, 0xff5b5b5b, 0xff6b6b6b, 0xff7b7b7b, 0xff8b8b8b,
		0xff9b9b9b, 0xffababab, 0xffbbbbbb, 0xffcbcbcb, 0xffdbdbdb, 0xffebebeb,
		0xff234b63, 0xff1f435b, 0xff1f3f53, 0xff1b3b4f, 0xff1b3747, 0xff172f3f,
		0xff172b3b, 0xff132733, 0xff13232f, 0xff131f2b, 0xff0f1b27, 0xff0f1723,
		0xff0b131b, 0xff0b0f17, 0xff070f13, 0xff070b0f, 0xff6f5f5f, 0xff675b5b,
		0xff5f535b, 0xff5b4f57, 0xff534b53, 0xff4b474f, 0xff433f47, 0xff3b3b3f,
		0xff37373b, 0xff2f2f33, 0xff2b2b2f, 0xff272727, 0xff232323, 0xff1b1b1b,
		0xff171717, 0xff131313, 0xff53778f, 0xff43637b, 0xff3b5b73, 0xff2f4f67,
		0xff4b97cf, 0xff3b7ba7, 0xff2f678b, 0xff27536f, 0xff279feb, 0xff238bcb,
		0xff1f77af, 0xff1b6393, 0xff174f77, 0xff0f3b5b, 0xff0b273f, 0xff071723,
		0xff2b3ba7, 0xff232f9f, 0xff1b2b97, 0xff13278b, 0xff0f1f7f, 0xff0b1773,
		0xff071767, 0xff001357, 0xff000f4b, 0xff000f43, 0xff000f3b, 0xff000b33,
		0xff000b2b, 0xff000b23, 0xff00071b, 0xff000713, 0xff4b5f7b, 0xff435773,
		0xff3f536b, 0xff3b4f67, 0xff37475f, 0xff334357, 0xff2f3f53, 0xff2b374b,
		0xff273343, 0xff232f3f, 0xff1b2737, 0xff17232f, 0xff131b27, 0xff0f171f,
		0xff0b0f17, 0xff070b0f, 0xff173b6f, 0xff17375f, 0xff172f53, 0xff172b43,
		0xff132337, 0xff0f1b27, 0xff0b131b, 0xff070b0f, 0xff4f5bb3, 0xff6f7bbf,
		0xff939bcb, 0xffb7bbd7, 0xffdfd7cb, 0xffd3c7b3, 0xffc3b79f, 0xffb7a787,
		0xffa79773, 0xff9b875b, 0xff8b7747, 0xff7f672f, 0xff6f5317, 0xff674b13,
		0xff5b430f, 0xff533f0b, 0xff4b3707, 0xff3f2f07, 0xff332707, 0xff2b1f00,
		0xff1f1700, 0xff130f00, 0xff0b0700, 0xff000000, 0xff57578b, 0xff4f4f83,
		0xff47477b, 0xff434373, 0xff3b3b6b, 0xff333363, 0xff2f2f5b, 0xff2b2b57,
		0xff23234b, 0xff1f1f3f, 0xff1b1b33, 0xff13132b, 0xff0f0f1f, 0xff0b0b13,
		0xff07070b, 0xff000000, 0xff7b9f97, 0xff73978f, 0xff6b8b87, 0xff63837f,
		0xff5f7b77, 0xff577373, 0xff4f6b6b, 0xff476363, 0xff435b5b, 0xff3b4f4f,
		0xff334343, 0xff2b3737, 0xff232f2f, 0xff1b2323, 0xff131717, 0xff0b0f0f,
		0xff3f4b9f, 0xff374393, 0xff2f3b8b, 0xff27377f, 0xff232f77, 0xff1b2b6b,
		0xff172363, 0xff131f57, 0xff0f1b4f, 0xff0b1743, 0xff0b1337, 0xff070f2b,
		0xff070b1f, 0xff000717, 0xff00000b, 0xff000000, 0xffcf7b77, 0xffc3736f,
		0xffb76b67, 0xffa76363, 0xff9b5b5b, 0xff8f5753, 0xff7f4f4b, 0xff734747,
		0xff673f3f, 0xff573737, 0xff4b2f2f, 0xff3f2727, 0xff2f1f23, 0xff23171b,
		0xff170f13, 0xff07070b, 0xff7bab9b, 0xff6f9f8f, 0xff639787, 0xff578b7b,
		0xff4b8373, 0xff437767, 0xff3b6f5f, 0xff336757, 0xff275b4b, 0xff1b4f3f,
		0xff134337, 0xff0b3b2f, 0xff072f23, 0xff00231b, 0xff001713, 0xff000f0b,
		0xff00ff00, 0xff0fe723, 0xff1bd33f, 0xff27bb53, 0xff2fa75f, 0xff338f5f,
		0xff337b5f, 0xffffffff, 0xffd3ffff, 0xffa7ffff, 0xff7fffff, 0xff53ffff,
		0xff27ffff, 0xff1febff, 0xff17d7ff, 0xff0fbfff, 0xff07abff, 0xff0093ff,
		0xff007fef, 0xff006be3, 0xff0057d3, 0xff0047c7, 0xff003bb7, 0xff002bab,
		0xff001f9b, 0xff00178f, 0xff000f7f, 0xff000773, 0xff00005f, 0xff000047,
		0xff00002f, 0xff00001b, 0xff0000ef, 0xffff3737, 0xff0000ff, 0xffff0000,
		0xff232b2b, 0xff171b1b, 0xff0f1313, 0xff7f97eb, 0xff5373c3, 0xff33579f,
		0xff1b3f7b, 0xffc7d3eb, 0xff9babc7, 0xff778ba7, 0xff576b87, 0xff535b9f,
	/*0x00ffffff*/ };

	public static final int[] PALETTE_ARGB = new int[PALETTE_ABGR.length];
	
	public static final int ALPHA_MASK = 0x0ff000000;

	static {
		int len = PALETTE_ARGB.length;
		for (int i = 0; i < len; i++) {
	      int abgr = QuakeImage.PALETTE_ABGR[i];
	      int argb = (abgr & 0xff00ff00) | ((abgr >> 16) & 255)
	          | ((abgr & 255) << 16);
	      PALETTE_ARGB[i] = argb;
	    }
	}
	
	public final int width;
	public final int height;
	public final int[] data;
	public boolean hasAlpha;

	public QuakeImage(int width, int height, int[] data) {
		this.width = width;
		this.height = height;
		this.data = data;
		for (int i= 0; i < data.length; i++) {
			if ((data[i] & ALPHA_MASK) != ALPHA_MASK) {
				hasAlpha = true;
				break;
			}
		}
	}

	
	public static QuakeImage loadImage(byte[] raw, String ext) {
		byte[] decoded = null;
		Dimension dim = new Dimension();
		int bits;
		
		if (ext.endsWith(".pcx")) {
			decoded = QuakeImage.LoadPCX(raw, null, dim);
			bits = 8;
		}
		else if (ext.endsWith(".wal")) {
			decoded = QuakeImage.GL_LoadWal(raw, dim);
			bits = 8;
		}
		else if (ext.endsWith(".tga")) {
			decoded = QuakeImage.LoadTGA(raw, dim);
			bits = 32;
		} else throw new RuntimeException("unknow image type!");

		return new QuakeImage(dim.width, dim.height, bits == 8 
				? applyPalette(decoded, dim.width, dim.height, PALETTE_ARGB)
				: bytesToIntsArgb(decoded));
	}
	
	
	public static int[] applyPalette(byte[] data, int width, int height, int[] palette) {
		int[] trans = new int[data.length];
		int p;
		//		int rgb;
		int s = width * height;
			for (int i = 0; i < s; i++) {
				p = data[i] & 0xff;
				trans[i] = palette[p];
	
				if (p == 255) { // transparent, so scan around for another color
					// to avoid alpha fringes
					// FIXME: do a full flood fill so mips work...
					if (i > width && (data[i - width] & 0xff) != 255)
						p = data[i - width] & 0xff;
					else if (i < s - width && (data[i + width] & 0xff) != 255)
						p = data[i + width] & 0xff;
					else if (i > 0 && (data[i - 1] & 0xff) != 255)
						p = data[i - 1] & 0xff;
					else if (i < s - 1 && (data[i + 1] & 0xff) != 255)
						p = data[i + 1] & 0xff;
					else
						p = 0;
					// copy rgb components
	
					// ((byte *)&trans[i])[0] = ((byte *)&d_8to24table[p])[0];
					// ((byte *)&trans[i])[1] = ((byte *)&d_8to24table[p])[1];
					// ((byte *)&trans[i])[2] = ((byte *)&d_8to24table[p])[2];
	
					trans[i] = palette[p] & 0x00FFFFFF; // only rgb
				}
			}
			return trans;
		}

	/*
	==============
	LoadPCX
	==============
	*/
	public static byte[] LoadPCX(byte[] raw, byte[][] palette, Dimension dim) {
		QuakeFiles.pcx_t pcx;
	
		
	
		//
		// parse the PCX file
		//
		pcx = new QuakeFiles.pcx_t(raw);
	
		if (pcx.manufacturer != 0x0a
			|| pcx.version != 5
			|| pcx.encoding != 1
			|| pcx.bits_per_pixel != 8
			|| pcx.xmax >= 640
			|| pcx.ymax >= 480) {
	
//			VID.Printf(Defines.PRINT_ALL, "Bad pcx file " +// filename 
//					+ '\n');
			return null;
		}
	
		int width = pcx.xmax - pcx.xmin + 1;
		int height = pcx.ymax - pcx.ymin + 1;
	
		byte[] pix = new byte[width * height];
	
		if (palette != null) {
			palette[0] = new byte[768];
			System.arraycopy(raw, raw.length - 768, palette[0], 0, 768);
		}
	
		if (dim != null) {
			dim.width = width;
			dim.height = height;
		}
	
		//
		// decode pcx
		//
		int count = 0;
		byte dataByte = 0;
		int runLength = 0;
		int x, y;
	
		for (y = 0; y < height; y++) {
			for (x = 0; x < width;) {
	
				dataByte = pcx.data.get();
	
				if ((dataByte & 0xC0) == 0xC0) {
					runLength = dataByte & 0x3F;
					dataByte = pcx.data.get();
					// write runLength pixel
					while (runLength-- > 0) {
						pix[count++] = dataByte;
						x++;
					}
				}
				else {
					// write one pixel
					pix[count++] = dataByte;
					x++;
				}
			}
		}
		return pix;
	}

	//	/*
		//	=========================================================
		//
		//	TARGA LOADING
		//
		//	=========================================================
		//	*/
		/*
		=============
		LoadTGA
		=============
		*/
		public static byte[] LoadTGA(byte[] raw, Dimension dim) {
			int columns, rows, numPixels;
			int pixbuf; // index into pic
			int row, column;
			ByteBuffer buf_p;
	//		int length;
			QuakeFiles.tga_t targa_header;
			byte[] pic = null;
	
			
			targa_header = new QuakeFiles.tga_t(raw);
			
			if (targa_header.image_type != 2 && targa_header.image_type != 10) 
				Com.Error(Constants.ERR_DROP, "LoadTGA: Only type 2 and 10 targa RGB images supported\n");
			
			if (targa_header.colormap_type != 0 || (targa_header.pixel_size != 32 && targa_header.pixel_size != 24))
				Com.Error (Constants.ERR_DROP, "LoadTGA: Only 32 or 24 bit images supported (no colormaps)\n");
			
			columns = targa_header.width;
			rows = targa_header.height;
			numPixels = columns * rows;
			
			if (dim != null) {
				dim.width = columns;
				dim.height = rows;
			}
			
			pic = new byte[numPixels * 4]; // targa_rgba;
			
			if (targa_header.id_length != 0)
				targa_header.data.position(targa_header.id_length);  // skip TARGA image comment
			
			buf_p = targa_header.data;
				
			byte red,green,blue,alphabyte;
			red = green = blue = alphabyte = 0;
			int packetHeader, packetSize, j;
			
			if (targa_header.image_type==2) {  // Uncompressed, RGB images
				for(row=rows-1; row>=0; row--) {
					
					pixbuf = row * columns * 4;
					
					for(column=0; column<columns; column++) {
						switch (targa_header.pixel_size) {
							case 24:
										
								blue = buf_p.get();
								green = buf_p.get();
								red = buf_p.get();
								pic[pixbuf++] = red;
								pic[pixbuf++] = green;
								pic[pixbuf++] = blue;
								pic[pixbuf++] = (byte)255;
								break;
							case 32:
								blue = buf_p.get();
								green = buf_p.get();
								red = buf_p.get();
								alphabyte = buf_p.get();
								pic[pixbuf++] = red;
								pic[pixbuf++] = green;
								pic[pixbuf++] = blue;
								pic[pixbuf++] = alphabyte;
								break;
						}
					}
				}
			}
			else if (targa_header.image_type==10) {   // Runlength encoded RGB images
	
				for(row=rows-1; row>=0; row--) {
					
					pixbuf = row * columns * 4;
					breakOut:
						for(column=0; column<columns; ) {
						
							packetHeader= buf_p.get() & 0xFF;
							packetSize = 1 + (packetHeader & 0x7f);
						
							if ((packetHeader & 0x80) != 0) {        // run-length packet
								switch (targa_header.pixel_size) {
									case 24:
										blue = buf_p.get();
										green = buf_p.get();
										red = buf_p.get();
										alphabyte = (byte)255;
										break;
									case 32:
										blue = buf_p.get();
										green = buf_p.get();
										red = buf_p.get();
										alphabyte = buf_p.get();
										break;
								}
					
								for(j=0;j<packetSize;j++) {
									pic[pixbuf++]=red;
									pic[pixbuf++]=green;
									pic[pixbuf++]=blue;
									pic[pixbuf++]=alphabyte;
									column++;
									if (column==columns) { // run spans across rows
										column=0;
										if (row>0)
											row--;
										else
											// goto label breakOut;
											break breakOut;
				
										pixbuf = row * columns * 4;
									}
								}
							}
							else { // non run-length packet
								for(j=0;j<packetSize;j++) {
									switch (targa_header.pixel_size) {
										case 24:
											blue = buf_p.get();
											green = buf_p.get();
											red = buf_p.get();
											pic[pixbuf++] = red;
											pic[pixbuf++] = green;
											pic[pixbuf++] = blue;
											pic[pixbuf++] = (byte)255;
											break;
										case 32:
											blue = buf_p.get();
											green = buf_p.get();
											red = buf_p.get();
											alphabyte = buf_p.get();
											pic[pixbuf++] = red;
											pic[pixbuf++] = green;
											pic[pixbuf++] = blue;
											pic[pixbuf++] = alphabyte;
											break;
									}
									column++;
									if (column==columns) { // pixel packet run spans across rows
										column=0;
										if (row>0)
											row--;
										else
											// goto label breakOut;
											break breakOut;
				
										pixbuf = row * columns * 4;
									}						
								}
							}
						}
					
				}
			}
			return pic;
		}

		/*
		================
		GL_LoadWal
		================
		*/
		public static byte[] GL_LoadWal(byte[] raw, Dimension dim) {
		
			QuakeFiles.miptex_t mt = new QuakeFiles.miptex_t(raw);
		
			byte[] pix = new byte[mt.width * mt.height];
			System.arraycopy(raw, mt.offsets[0], pix, 0, pix.length);
		
			dim.width = mt.width;
			dim.height = mt.height;
			
			return pix;
		}


		public static int[] bytesToIntsAbgr(byte[] pic) {
			int count = pic.length / 4;
			int[] tmp = new int[count];
		
			for (int i = 0; i < count; i++) {
				tmp[i] = ((pic[4 * i + 0] & 0xFF) << 0) | // & 0x000000FF;
					 ((pic[4 * i + 1] & 0xFF) << 8) | // & 0x0000FF00;
					 ((pic[4 * i + 2] & 0xFF) << 16) | // & 0x00FF0000;
					((pic[4 * i + 3] & 0xFF) << 24); // & 0xFF000000;
			}
			return tmp;
		}
	
		public static int[] bytesToIntsArgb(byte[] pic) {
			int count = pic.length / 4;
			int[] tmp = new int[count];
		
			for (int i = 0; i < count; i++) {
				tmp[i] = ((pic[4 * i + 0] & 0xFF) << 16) | // & 0x000000FF;
					 ((pic[4 * i + 1] & 0xFF) << 8) | // & 0x0000FF00;
					 ((pic[4 * i + 2] & 0xFF) << 0) | // & 0x00FF0000;
					((pic[4 * i + 3] & 0xFF) << 24); // & 0xFF000000;
			}
			return tmp;
		}
	
	
		static class floodfill_t {
			short x, y;
		}

		// must be a power of 2
		static final int FLOODFILL_FIFO_SIZE = 0x1000;
		static final int FLOODFILL_FIFO_MASK = FLOODFILL_FIFO_SIZE - 1;
		//
		//	#define FLOODFILL_STEP( off, dx, dy ) \
		//	{ \
		//		if (pos[off] == fillcolor) \
		//		{ \
		//			pos[off] = 255; \
		//			fifo[inpt].x = x + (dx), fifo[inpt].y = y + (dy); \
		//			inpt = (inpt + 1) & FLOODFILL_FIFO_MASK; \
		//		} \
		//		else if (pos[off] != 255) fdc = pos[off]; \
		//	}

		//	void FLOODFILL_STEP( int off, int dx, int dy )
		//	{
		//		if (pos[off] == fillcolor)
		//		{
		//			pos[off] = 255;
		//			fifo[inpt].x = x + dx; fifo[inpt].y = y + dy;
		//			inpt = (inpt + 1) & FLOODFILL_FIFO_MASK;
		//		}
		//		else if (pos[off] != 255) fdc = pos[off];
		//	}
		// TODO check this: R_FloodFillSkin( byte[] skin, int skinwidth, int skinheight)
		public void floodFill() {
			//		byte				fillcolor = *skin; // assume this is the pixel to fill
			
			floodfill_t[] fifo = new floodfill_t[FLOODFILL_FIFO_SIZE];
			for (int j = 0; j < fifo.length; j++) {
				fifo[j] = new floodfill_t();
			}
			
			int fillcolor = data[0];
//			floodfill_t[] fifo = new floodfill_t[FLOODFILL_FIFO_SIZE];
			int inpt = 0, outpt = 0;

			int filledcolor = 0xff000000;
	
			// can't fill to filled color or to transparent color (used as visited marker)
			if ((fillcolor == filledcolor) || (fillcolor == 0)) {
				return;
			}

			fifo[inpt].x = 0;
			fifo[inpt].y = 0;
			inpt = (inpt + 1) & FLOODFILL_FIFO_MASK;

			while (outpt != inpt) {
				int x = fifo[outpt].x;
				int y = fifo[outpt].y;
				int fdc = filledcolor;
				//			byte		*pos = &skin[x + skinwidth * y];
				int pos = x + width * y;
				//
				outpt = (outpt + 1) & FLOODFILL_FIFO_MASK;

				int off, dx, dy;

				if (x > 0) {
					// FLOODFILL_STEP( -1, -1, 0 );
					off = -1;
					dx = -1;
					dy = 0;
					if (data[pos + off] == fillcolor) {
						data[pos + off] = 0;
						fifo[inpt].x = (short) (x + dx);
						fifo[inpt].y = (short) (y + dy);
						inpt = (inpt + 1) & FLOODFILL_FIFO_MASK;
					}
					else if (data[pos + off] != 0)
						fdc = data[pos + off];
				}

				if (x < width - 1) {
					// FLOODFILL_STEP( 1, 1, 0 );
					off = 1;
					dx = 1;
					dy = 0;
					if (data[pos + off] == fillcolor) {
						data[pos + off] = 0;
						fifo[inpt].x = (short) (x + dx);
						fifo[inpt].y = (short) (y + dy);
						inpt = (inpt + 1) & FLOODFILL_FIFO_MASK;
					}
					else if (data[pos + off] != 0)
						fdc = data[pos + off] & 0xff;
				}

				if (y > 0) {
					// FLOODFILL_STEP( -skinwidth, 0, -1 );
					off = -width;
					dx = 0;
					dy = -1;
					if (data[pos + off] == fillcolor) {
						data[pos + off] = 0;
						fifo[inpt].x = (short) (x + dx);
						fifo[inpt].y = (short) (y + dy);
						inpt = (inpt + 1) & FLOODFILL_FIFO_MASK;
					}
					else if (data[pos + off] != 0)
						fdc = data[pos + off];
				}

				if (y < height - 1) {
					// FLOODFILL_STEP( skinwidth, 0, 1 );
					off = width;
					dx = 0;
					dy = 1;
					if (data[pos + off] == fillcolor) {
						data[pos + off] = 0;
						fifo[inpt].x = (short) (x + dx);
						fifo[inpt].y = (short) (y + dy);
						inpt = (inpt + 1) & FLOODFILL_FIFO_MASK;
					}
					else if (data[pos + off] != 0)
						fdc = data[pos + off];

				}

				data[x + width * y] = fdc;
			}
		}

}

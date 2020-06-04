/*
Copyright (C) 2010 Copyright 2010 Google Inc.

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
package com.googlecode.gwtquake.tools;


import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.QuakeFiles;

public class TGAConverter extends Converter {
  public TGAConverter() {
    super("tga", "png");
  }

  @Override
  public void convert(byte[] raw, File outFile, int[] size) throws IOException {
    RenderedImage ri = makeImage(LoadTGA(raw));
    ImageIO.write(ri, "png", outFile);
    size[0] = ri.getWidth();
    size[1] = ri.getHeight();
  }

  private static image_t LoadTGA(byte[] raw) {
    int columns, rows, numPixels;
    int pixbuf; // index into pic
    int row, column;
    ByteBuffer buf_p;
    // int length;
    QuakeFiles.tga_t targa_header;
    image_t img = new image_t();

    targa_header = new QuakeFiles.tga_t(raw);

    if (targa_header.image_type != 2 && targa_header.image_type != 10)
      Com.Error(Constants.ERR_DROP,
          "LoadTGA: Only type 2 and 10 targa RGB images supported\n");

    if (targa_header.colormap_type != 0
        || (targa_header.pixel_size != 32 && targa_header.pixel_size != 24))
      Com.Error(Constants.ERR_DROP,
          "LoadTGA: Only 32 or 24 bit images supported (no colormaps)\n");

    columns = targa_header.width;
    rows = targa_header.height;
    numPixels = columns * rows;

    img.width = columns;
    img.height = rows;

    img.pix = new byte[numPixels * 4]; // targa_rgba;

    if (targa_header.id_length != 0)
      targa_header.data.position(targa_header.id_length); // skip TARGA image
                                                          // comment

    buf_p = targa_header.data;

    byte red, green, blue, alphabyte;
    red = green = blue = alphabyte = 0;
    int packetHeader, packetSize, j;

    if (targa_header.image_type == 2) { // Uncompressed, RGB images
      for (row = rows - 1; row >= 0; row--) {

        pixbuf = row * columns * 4;

        for (column = 0; column < columns; column++) {
          switch (targa_header.pixel_size) {
            case 24:

              blue = buf_p.get();
              green = buf_p.get();
              red = buf_p.get();
              img.pix[pixbuf++] = red;
              img.pix[pixbuf++] = green;
              img.pix[pixbuf++] = blue;
              img.pix[pixbuf++] = (byte) 255;
              break;
            case 32:
              blue = buf_p.get();
              green = buf_p.get();
              red = buf_p.get();
              alphabyte = buf_p.get();
              img.pix[pixbuf++] = red;
              img.pix[pixbuf++] = green;
              img.pix[pixbuf++] = blue;
              img.pix[pixbuf++] = alphabyte;
              break;
          }
        }
      }
    } else if (targa_header.image_type == 10) { // Runlength encoded RGB images
      for (row = rows - 1; row >= 0; row--) {
        pixbuf = row * columns * 4;
        breakOut: for (column = 0; column < columns;) {

          packetHeader = buf_p.get() & 0xFF;
          packetSize = 1 + (packetHeader & 0x7f);

          if ((packetHeader & 0x80) != 0) { // run-length packet
            switch (targa_header.pixel_size) {
              case 24:
                blue = buf_p.get();
                green = buf_p.get();
                red = buf_p.get();
                alphabyte = (byte) 255;
                break;
              case 32:
                blue = buf_p.get();
                green = buf_p.get();
                red = buf_p.get();
                alphabyte = buf_p.get();
                break;
            }

            for (j = 0; j < packetSize; j++) {
              img.pix[pixbuf++] = red;
              img.pix[pixbuf++] = green;
              img.pix[pixbuf++] = blue;
              img.pix[pixbuf++] = alphabyte;
              column++;
              if (column == columns) { // run spans across rows
                column = 0;
                if (row > 0)
                  row--;
                else
                  // goto label breakOut;
                  break breakOut;

                pixbuf = row * columns * 4;
              }
            }
          } else { // non run-length packet
            for (j = 0; j < packetSize; j++) {
              switch (targa_header.pixel_size) {
                case 24:
                  blue = buf_p.get();
                  green = buf_p.get();
                  red = buf_p.get();
                  img.pix[pixbuf++] = red;
                  img.pix[pixbuf++] = green;
                  img.pix[pixbuf++] = blue;
                  img.pix[pixbuf++] = (byte) 255;
                  break;
                case 32:
                  blue = buf_p.get();
                  green = buf_p.get();
                  red = buf_p.get();
                  alphabyte = buf_p.get();
                  img.pix[pixbuf++] = red;
                  img.pix[pixbuf++] = green;
                  img.pix[pixbuf++] = blue;
                  img.pix[pixbuf++] = alphabyte;
                  break;
              }
              column++;
              if (column == columns) { // pixel packet run spans across rows
                column = 0;
                if (row > 0)
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

    return img;
  }
}

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

import javax.imageio.ImageIO;

import com.googlecode.gwtquake.shared.common.QuakeFiles;

public class PCXConverter extends Converter {
  public PCXConverter() {
    super("pcx", "png");
  }

  @Override
  public void convert(byte[] raw, File outFile, int[] size) throws IOException {
    RenderedImage ri = makePalletizedImage(LoadPCX(raw, null));
    ImageIO.write(ri, "png", outFile);
    size[0] = ri.getWidth();
    size[1] = ri.getHeight();
  }

  private image_t LoadPCX(byte[] raw, byte[][] palette) {
    QuakeFiles.pcx_t pcx;
    image_t img = new image_t();

    //
    // parse the PCX file
    //
    pcx = new QuakeFiles.pcx_t(raw);

    if (pcx.manufacturer != 0x0a || pcx.version != 5 || pcx.encoding != 1
        || pcx.bits_per_pixel != 8 || pcx.xmax >= 640 || pcx.ymax >= 480) {

      // VID.Printf(Defines.PRINT_ALL, "Bad pcx file " +// filename
      // + '\n');
      return null;
    }

    int width = pcx.xmax - pcx.xmin + 1;
    int height = pcx.ymax - pcx.ymin + 1;

    img.pix = new byte[width * height];

    if (palette != null) {
      palette[0] = new byte[768];
      System.arraycopy(raw, raw.length - 768, palette[0], 0, 768);
    }

    img.width = width;
    img.height = height;

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
            img.pix[count++] = dataByte;
            x++;
          }
        } else {
          // write one pixel
          img.pix[count++] = dataByte;
          x++;
        }
      }
    }

    return img;
  }
}

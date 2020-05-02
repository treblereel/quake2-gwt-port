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


import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import com.googlecode.gwtquake.shared.common.QuakeImage;

public abstract class Converter {

  protected static class image_t {
    int width, height;
    byte[] pix;
  }

  private static HashMap<String, Converter> converters = new HashMap<String, Converter>();

  public static Converter get(String name) {
    int idx = name.lastIndexOf('.');
    if (idx != -1) {
      return converters.get(name.substring(idx + 1).toLowerCase());
    }
    return null;
  }

  private final String outExt;

  public Converter(String inExt, String outExt) {
    this.outExt = outExt;
    converters.put(inExt, this);
  }

  public abstract void convert(byte[] raw, File outFile, int[] size) throws IOException;

  public String getOutExt() {
    return outExt;
  }

  protected RenderedImage makeImage(image_t image) {
    BufferedImage bi = new BufferedImage(image.width, image.height,
        BufferedImage.TYPE_4BYTE_ABGR);
    WritableRaster raster = bi.getRaster();
    raster.setDataElements(0, 0, image.width, image.height, image.pix);
    return bi;
  }

  protected RenderedImage makePalletizedImage(image_t image) {
    BufferedImage bi = new BufferedImage(image.width, image.height,
        BufferedImage.TYPE_INT_ARGB);

    int[] data = new int[image.width * image.height];
    int i = 0;
    for (int y = 0; y < image.height; ++y) {
      for (int x = 0; x < image.width; ++x) {
        int ofs = image.pix[y * image.width + x];
        if (ofs < 0) {
          ofs += 256;
        }

        data[i++] = (ofs == 255) ? 0 : QuakeImage.PALETTE_ARGB[ofs];
      }
    }

    WritableRaster raster = bi.getRaster();
    raster.setDataElements(0, 0, image.width, image.height, data);
    return bi;
  }
}

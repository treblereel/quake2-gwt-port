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

import java.io.File;

/**
 * "Umbrella" for the Downloader, Unpacker and Converter
 */
public class Installer {
  public static void main(String args[]) throws Throwable {
    Downloader.main(args);
    if (args.length > 0 &&  args[0].equals("hires") && !new File("raw", "baseq2").exists()) {
        Downloader hirezTextureDl = new Downloader("http://www-personal.umich.edu/~jimw/q2/Quake2_tga_textures/q2_textures.zip");
        hirezTextureDl.setHiresPak(true);
        hirezTextureDl.run();
        Downloader hirezMdlDl = new Downloader("http://www-personal.umich.edu/~jimw/q2/aq2_install/models.zip");
        hirezMdlDl.setHiresPak(true);
        hirezMdlDl.run();
    }
    Unpak.main(new String[] {
        "raw" + File.separator + "baseq2",
        "war" + File.separator + "baseq2"
    });
  }
}

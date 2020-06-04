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
package com.googlecode.gwtquake.shared.util;

public class CanvasHelper {

  public static String getCssColor(int color) {
    int a = (color >> 24) & 255;
    int r = (color >> 16) & 255;
    int g = (color >> 8) & 255;
    int b = color & 255;
    return getCssColor(r, g, b, a);
  }
  
  public static String getCssColor(float red, float green, float blue, float alpha) {
    return "rgba(" + (int) (red * 255) + "," + (int) (green * 255) + "," + (int) (blue * 255) + 
        "," + alpha+")"; 
  }

  public static String getCssColor(int red, int green, int blue, int alpha) {
    return "rgba(" + red + "," + green + "," + blue + "," + alpha / 255.0 + ")"; 
  }
}

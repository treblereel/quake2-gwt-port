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

/**
 * Vec3Cache contains float[3] for temporary usage.
 * The usage can reduce the garbage at runtime.
 *
 * @author cwei
 */
public final class Vec3Cache {
    
    //private static Stack cache = new Stack();
    private static final float[][] cache = new float[64][3];
    private static int index = 0;
    private static int max = 0;
    
    public static final float[] get() {
        //max = Math.max(index, max);
        return cache[index++];
    }
    
    public static final void release() {
        index--;
    }

    public static final void release(int count) {
        index-=count;
    }
    
    public static final void debug() {
        System.err.println("Vec3Cache: max. " + (max + 1) + " vectors used.");
    }
}

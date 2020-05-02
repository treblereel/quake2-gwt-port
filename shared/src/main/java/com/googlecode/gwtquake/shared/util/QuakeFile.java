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


import java.io.*;

import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.game.*;
import com.googlecode.gwtquake.shared.game.adapters.SuperAdapter;

/**
 * RandomAccessFile, but handles readString/WriteString specially and offers
 * other helper functions
 */
public class QuakeFile extends RandomAccessFile {

    /** Standard Constructor. */
    public QuakeFile(String filename, String mode) throws FileNotFoundException {
        super(filename, mode);
    }

    /** Writes a Vector to a RandomAccessFile. */
    public void writeVector(float v[]) throws IOException {
        for (int n = 0; n < 3; n++)
            writeFloat(v[n]);
    }

    /** Writes a Vector to a RandomAccessFile. */
    public float[] readVector() throws IOException {
        float res[] = { 0, 0, 0 };
        for (int n = 0; n < 3; n++)
            res[n] = readFloat();

        return res;
    }

    /** Reads a length specified string from a file. */
    public String readString() throws IOException {
        int len = readInt();

        if (len == -1)
            return null;

        if (len == 0)
            return "";

        StringBuilder sb = new StringBuilder(len);

        for (int i = 0; i < len; i++) {
        	sb.append((char) read());
        }
        return sb.toString();
    }

    /** Writes a length specified string to a file. */
    public void writeString(String s) throws IOException {
        if (s == null) {
            writeInt(-1);
            return;
        }

        writeInt(s.length());
        if (s.length() != 0)
            writeBytes(s);
    }

    /** Writes the edict reference. */
    public void writeEdictRef(Entity ent) throws IOException {
        if (ent == null)
            writeInt(-1);
        else {
            writeInt(ent.s.number);
        }
    }

    /**
     * Reads an edict index from a file and returns the edict.
     */

    public Entity readEdictRef() throws IOException {
        int i = readInt();

        // handle -1
        if (i < 0)
            return null;

        if (i > GameBase.g_edicts.length) {
            Com.DPrintf("jake2: illegal edict num:" + i + "\n");
            return null;
        }

        // valid edict.
        return GameBase.g_edicts[i];
    }

    /** Writes the Adapter-ID to the file. */
    public void writeAdapter(SuperAdapter a) throws IOException {
        writeInt(3988);
        if (a == null)
            writeString(null);
        else {
            String str = a.getID();
            if (a == null) {
                Com.DPrintf("writeAdapter: invalid Adapter id for " + a + "\n");
            }
            writeString(str);
        }
    }

    /** Reads the adapter id and returns the adapter. */
    public SuperAdapter readAdapter() throws IOException {
        if (readInt() != 3988)
            Com.DPrintf("wrong read position: readadapter 3988 \n");

        String id = readString();

        if (id == null) {
            // null adapter. :-)
            return null;
        }

        return SuperAdapter.getFromID(id);
    }

    /** Writes an item reference. */
    public void writeItem(GameItem item) throws IOException {
        if (item == null)
            writeInt(-1);
        else
            writeInt(item.index);
    }

    /** Reads the item index and returns the game item. */
    public GameItem readItem() throws IOException {
        int ndx = readInt();
        if (ndx == -1)
            return null;
        else
            return GameItemList.itemlist[ndx];
    }

    
  
}

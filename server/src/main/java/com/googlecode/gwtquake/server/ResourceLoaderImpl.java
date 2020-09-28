/*
 * Copyright (C) 2010 Copyright 2010 Google Inc.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 *
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package com.googlecode.gwtquake.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.googlecode.gwtquake.shared.client.PlayerModel;
import com.googlecode.gwtquake.shared.common.AsyncCallback;
import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.ResourceLoader;
import elemental2.core.JsArray;
import org.apache.commons.io.FileUtils;

public class ResourceLoaderImpl implements ResourceLoader.Impl {

    protected static final Object LOAD_LOCK = new Object();
    ArrayList<Pending> pending = new ArrayList<>();

    public void loadResourceAsync(String path,
                                  final ResourceLoader.Callback callback) {
        final byte[] bytes = loadResource(path);
        Pending p = new Pending();
        p.bytes = bytes;
        p.callback = callback;
        p.path = path;
        pending.add(p);
    }

    //TODO load from server
    @Override
    public void playerModels(AsyncCallback<JsArray<PlayerModel>> onLoad) {

    }

    public boolean pump() {
        if (pending.size() == 0) {
            return false;
        }

        int i = (int) Math.random() * pending.size();
        if (i < pending.size()) {
            Pending p = pending.get(i);

            pending.remove(i);
            if (p.bytes != null) {
                p.callback.onSuccess(ByteBuffer.wrap(p.bytes));
            } else {
                ResourceLoader.fail(new FileNotFoundException(p.path));
            }
        }
        return true;
    }

    public void reset() {
        // Only needed for deferred stuff
    }

    public byte[] loadResource(String path) {
        RandomAccessFile file;
        byte[] buf = null;
        int len = 0;

        // TODO hack for bad strings (fuck \0)
        int index = path.indexOf('\0');
        if (index != -1) {
            path = path.substring(0, index);
        }

        // look for it in the filesystem
        len = FileLength(path);

        if (len < 1) {
            return null;
        }

        try {
            file = FOpenFile(path);
            // Read(buf = new byte[len], len, h);
            buf = new byte[len];
            file.readFully(buf);
            file.close();
        } catch (IOException e) {
            Com.Error(Constants.ERR_FATAL, e.toString());
        }
        return buf;
    }

    private static int FileLength(String filename) {
        String netpath;

        // check a file in the directory tree
        netpath = Constants.BASEDIRNAME + '/' + filename;

        String serverUrl = "http://127.0.0.1:8080/quake2/";

        File file = new File(netpath);

        try {
            FileUtils.copyURLToFile(new URL(serverUrl + netpath), file, 1000, 1000);
        } catch (IOException e) {
            e.printStackTrace();
            throw new Error(e);
        }

        //File file = new File(netpath);
        if (!file.canRead()) {
            return -1;
        }

        System.out.println("FindFile: " + netpath + '\n');

        Com.DPrintf("FindFile: " + netpath + '\n');

        return (int) file.length();
    }

    private static RandomAccessFile FOpenFile(String filename) throws IOException {
        System.out.println("FOpenFile " + filename + " " + Constants.BASEDIRNAME + '/' + filename);
        String netpath;
        File file = null;

        netpath = Constants.BASEDIRNAME + '/' + filename;

        file = new File(netpath);
        if (!file.canRead()) {
            return null;
        }

        return new RandomAccessFile(file, "r");
    }

    class Pending {

        String path;
        ResourceLoader.Callback callback;
        byte[] bytes;
    }
}

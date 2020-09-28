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
package com.googlecode.gwtquake.shared.common;

import java.nio.ByteBuffer;

import com.googlecode.gwtquake.shared.client.PlayerModel;
import elemental2.core.JsArray;

public class ResourceLoader {

	public interface Callback {
		void onSuccess(ByteBuffer result);
	}
	
	public interface Impl {
	    void loadResourceAsync(String path, Callback callback);
		void playerModels(AsyncCallback<JsArray<PlayerModel>> onLoad);
	    boolean pump();
		void reset();
	  }

	public static boolean Pump() {
	    return ResourceLoader.impl.pump();
	  }

	public static ResourceLoader.Impl impl;

	public static void loadResourceAsync(String path, final Callback callback) {
	    impl.loadResourceAsync(path, callback);
	  }

	public static void fail(Exception e) {
	  System.err.println(e.getMessage());
	}
	
	public static void reset() {
		impl.reset();
	}
}

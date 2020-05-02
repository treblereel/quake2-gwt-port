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

import com.googlecode.gwtquake.shared.common.Compatibility;

public class CompatibilityImpl implements Compatibility.Impl {

  public int floatToIntBits(float f) {
    return Float.floatToIntBits(f);
  }

  public float intBitsToFloat(int i) {
    return intBitsToFloat(i);
  }

  public String createString(byte[] b, int ofs, int length) {

    return new String(b, ofs, length);
  }

  public String getOriginatingServerAddress() {
    return "127.0.0.1";
  }

  public void printStackTrace(Throwable e) {
    e.printStackTrace();
  }

  public String createString(byte[] b, String encoding) {
    return null;
  }

  public void loadClass(String name) throws ClassNotFoundException {
    Class.forName(name);
  }

  public void sleep(int i) {
    try {
      Thread.sleep(i);
    } catch (InterruptedException e) {

    }
  }
}

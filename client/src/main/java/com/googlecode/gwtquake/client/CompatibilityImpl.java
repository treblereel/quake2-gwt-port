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
package com.googlecode.gwtquake.client;



import com.google.gwt.corp.compatibility.ConsolePrintStream;
import com.google.gwt.corp.compatibility.Numbers;
import com.googlecode.gwtquake.shared.common.Compatibility;

public class CompatibilityImpl implements Compatibility.Impl {

	public CompatibilityImpl() {
		ConsolePrintStream cps;
		cps = new ConsolePrintStream();
		System.setOut(cps);
		System.setErr(cps);
		
		System.out.println("Test for System.out.println()");
		new Throwable("Exception test").printStackTrace();
		System.out.println("Did the exception test appear above?");
		
	}
	
	public int floatToIntBits(float f) {
		return Numbers.floatToIntBits(f);
	}

	public float intBitsToFloat(int i) {
		return Numbers.intBitsToFloat(i);
	}

	public String createString(byte[] b, int ofs, int length) {
		StringBuffer sb = new StringBuffer(length);
		for (int i = 0; i < length; i++) {
			sb.append((char) b[ofs + i]);
		}
		return sb.toString();
	}

	
	public String getOriginatingServerAddress() {
		return getServerAddress();
	}

	private static String getServerAddress() {
		return "ws://94.112.128.228:8080/quake2/net";
	}

	public void printStackTrace(Throwable e) {
        for (StackTraceElement ste : e.getStackTrace()) {
          System.out.println(" at " + ste);
        }
	}

	public String createString(byte[] b, String encoding) {
		return createString(b, 0, b.length);
	}

	public void loadClass(String name) throws ClassNotFoundException {
		// TODO Auto-generated method stub
		
	}

	public void sleep(int i) {
		// TODO Auto-generated method stub
		
	}
}

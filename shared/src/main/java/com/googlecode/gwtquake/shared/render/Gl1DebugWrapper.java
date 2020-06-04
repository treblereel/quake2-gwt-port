/*
Copyright (C) 2011 Copyright 2010 Google Inc.

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
package com.googlecode.gwtquake.shared.render;


import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for a Gl1Context that logs most calls. 
 */
public class Gl1DebugWrapper extends Gl1Context {

	final Gl1Context gl;

	public Gl1DebugWrapper(Gl1Context gl) {
	  this.gl = gl;
	}

	public void big(String s) {
		System.out.println("***********************************************************************");
		System.out.println(s);
		System.out.println("***********************************************************************");
	}

	public static String c(int c) {
		switch (c) {
		case GL_BYTE:
			return "GL_BYTE";
		case GL_BLEND:
			return "GL_BLEND";
		case GL_COLOR_ARRAY:
			return "GL_COLOR_ARRAY";
		case GL_DITHER:
			return "GL_DITHER";
		case GL_FIXED:
			return "GL_FIXED";
		case GL_FLOAT:
			return "GL_FLOAT";
		case GL_LINES:
			return "GL_LINES";
		case GL_LINE_LOOP:
			return "GL_LINE_LOOP";
		case GL_LINE_SMOOTH:
			return "GL_LINE_SMOOTH";
		case GL_LINE_SMOOTH_HINT:
			return "GL_LINE_SMOOTH_HINT";
		case GL_LINE_STRIP:
			return "GL_LINE_STRIP";
		case GL_MODELVIEW:
			return "GL_MODELVIWEW";
		case GL_NO_ERROR:
			return "GL_NO_ERROR";
		case GL_PROJECTION:
			return "GL_PROJECTION";
		case GL_SHORT:
			return "GL_SHORT";
		case GL_TRIANGLE_FAN:
			return "GL_TRIANGLE_FAN";
		case GL_TRIANGLE_STRIP:
			return "GL_TRIANGLE_STRIP";
		case GL_TRIANGLES:
			return "GL_TRIANGLES";
		case GL_TEXTURE:
			return "GL_TEXTURE";
		case GL_TEXTURE_COORD_ARRAY:
			return "GL_TEXTURE_COORD_ARRAY";
		case GL_TEXTURE_2D:
			return "GL_TEXTURE_2D";
		case GL_UNSIGNED_BYTE:
			return "GL_UNISGNED_BYTE";
		case GL_UNSIGNED_SHORT:
			return "GL_UNSIGNED_SHORT";

		default:
			return "GL_0x" + Integer.toHexString(c);
		}
	}

	private void print(String label, Object... param) {

		int err = gl.glGetError();
		if (err != gl.GL_NO_ERROR) {
			System.out.println("glGetError(): " + c(err));
		}
		System.out.print(label);
		// printObjectArr(param);
		System.out.println();
	}

	private void printObj(Object o) {
		if (o instanceof float[]) {
			printFloatArr((float[]) o);
		} else if (o instanceof Object[]) {
			printObjectArr((Object[]) o);
		} else if (o instanceof double[]) {
			printDoubleArr((double[]) o);
		} else {
			System.out.print("" + o);
		}
	}

	private void printFloatArr(float[] a) {
		ArrayList<Float> l = new ArrayList<Float>();
		for (float e : a) {
			l.add(e);
		}
		printList(l);
	}

	private void printDoubleArr(double[] a) {
		ArrayList<Double> l = new ArrayList<Double>();
		for (double e : a) {
			l.add(e);
		}
		printList(l);
	}

	private void printObjectArr(Object[] a) {
		ArrayList<Object> l = new ArrayList<Object>();
		for (Object e : a) {
			l.add(e);
		}
		printList(l);
	}

	private void printList(List<?> l) {
		System.out.print("[");
		for (int i = 0; i < l.size(); i++) {
			if (i != 0) {
				System.out.print(", ");
			}
			if (i > 20) {
				System.out.print("...");
				break;
			}
			printObj(l.get(i));
		}
		System.out.print("]");
	}

	public final void glBindTexture(int t, int i) {
		print("glBindTexture", t, i);
		gl.glBindTexture(t, i);
	}

	public final void glBlendFunc(int a, int b) {
		print("glBlendFunc", a, b);
		gl.glBlendFunc(a, b);
	}

	public final void glClear(int mask) {
		print("Clear", mask);
		gl.glClear(mask);
	}

	public final void glColor4f(float red, float green, float blue, float alpha) {
		print("Color4f", red, green, blue, alpha);
		gl.glColor4f(red, green, blue, alpha);
	}

	public final void glColorPointer(int size, int stride, FloatBuffer buf) {
		print("ColorPointer", size, stride, buf);
		gl.glColorPointer(size, stride, buf);
	}

	public final void glDrawElements(int mode, ShortBuffer indices) {
		print("DrawElements", c(mode), indices);
		gl.glDrawElements(mode, indices);
	}

	public final void glEnable(int i) {
		print("Enable", c(i));
		gl.glEnable(i);
	}

	public final int glGetError() {
		print("GetError");
		return gl.glGetError();
	}

	public final void glClearColor(float f, float g, float h, float i) {
		print("ClearColor", f, g, h, i);
		gl.glClearColor(f, g, h, i);
	}

	public void glDrawArrays(int mode, int first, int count) {
		print("DrawArrays", c(mode), first, count);
		// mv-matrix
		gl.glDrawArrays(mode, first, count);
	}

	public final void glLoadIdentity() {
		print("LoadIdentity");
		gl.glLoadIdentity();
	}

	public final void glMatrixMode(int mm) {
		print("MatrixMode", c(mm));
		gl.glMatrixMode(mm);
	}

	public final void glPushMatrix() {
		print("PushMatrix");
		gl.glPushMatrix();
	}

	public final void glPopMatrix() {
		print("PopMatrix");
		gl.glPopMatrix();
	}

	public final void glRotatef(float angle, float x, float y, float z) {
		print("Rotatef", angle, x, y, z);
		gl.glRotatef(angle, x, y, z);
	}

	public final void glScalef(float x, float y, float z) {
		print("Scalef", x, y, z);
		gl.glScalef(x, y, z);
	}

	public final void glScissor(int x, int y, int width, int height) {
		print("Scissor", x, y, width, height);
		gl.glScissor(x, y, width, height);
	}

	public final void glTexEnvi(int target, int pname, int param) {
		print("TexEnvi", target, pname, param);
		gl.glTexEnvi(target, pname, param);
	}

	public final void glTranslatef(float tx, float ty, float tz) {
		print("Translatef", tx, ty, tz);
		gl.glTranslatef(tx, ty, tz);
	}

	public final void glVertexPointer(int size, int stride, FloatBuffer data) {
		print("VertexPointer", size, stride, data);
		gl.glVertexPointer(size, stride, data);
	}

	@Override
	public void glTexParameterf(int target, int pname, float param) {
		print("TexParameter", c(target), c(pname), param);
		gl.glTexParameterf(target, pname, param);
	}

	public final void glDeleteTextures(IntBuffer indices) {
		print("DeleteTextures", indices);
		gl.glDeleteTextures(indices);
	}

	public final void glEnableClientState(int i) {
		print("EnableClientState", c(i));
		gl.glEnableClientState(i);
	}

	public final void glDisableClientState(int i) {
		print("DisableClientState", c(i));
		gl.glDisableClientState(i);
	}

	public final void glDisable(int i) {
		print("Disable", c(i));
		gl.glDisable(i);
	}

	public final void glCullFace(int c) {
		print("CullFace", c(c));
		gl.glCullFace(c);
	}

	public final void glShadeModel(int s) {
		print("ShadeMode", c(s));
		gl.glShadeModel(s);
	}

	public final void glViewport(int x, int y, int w, int h) {
		print("Viewport", x, y, w, h);
		gl.glViewport(x, y, w, h);
	}

	public final void glTexCoordPointer(int size, int stride, FloatBuffer buf) {
		print("TexCoordPointer", size, stride, buf);
		gl.glTexCoordPointer(size, stride, buf);
	}


	@Override
	public void glAlphaFunc(int i, float j) {
		print("glAlphaFunc", i, j);
		gl.glAlphaFunc(i, j);
	}

	@Override
	public void glColorPointer(int i, boolean b, int j,
			ByteBuffer colorAsByteBuffer) {
		print("glColorPointer", i, b, j, colorAsByteBuffer);
		gl.glColorPointer(i, b, j, colorAsByteBuffer);
	}

	@Override
	public void glDepthFunc(int func) {
		print("glDepthFunc", func);
		gl.glDepthFunc(func);
	}

	@Override
	public void glDepthMask(boolean b) {
		print("glDepthMask", b);
		gl.glDepthMask(b);
	}

	@Override
	public void glDepthRange(float gldepthmin, float gldepthmax) {
		print("glDepthRange", gldepthmin, gldepthmax);
		gl.glDepthRange(gldepthmin, gldepthmax);
	}

	@Override
	public void glDrawBuffer(int buf) {
		print("glDrawBuffer", buf);
		gl.glDrawBuffer(buf);
	}

	@Override
	public void glFinish() {
		print("glFinish");
		gl.glFinish();
	}

	@Override
	public void swapBuffers() {
		print("glFlush");
		gl.swapBuffers();
	}

	@Override
	public void glFrustum(double xmin, double xmax, double ymin, double ymax,
			double zNear, double zFar) {
		print("glFrustum", xmin, xmax, ymin, ymax, zNear, zFar);
		gl.glFrustum(xmin, xmax, ymin, ymax, zNear, zFar);
	}

	@Override
	public void glGetFloat(int name, FloatBuffer result) {
		gl.glGetFloat(name, result);
		print("glGetFloat", name, result);
	}

	@Override
	public String glGetString(int glVendor) {
		print("glGetString", glVendor);
		String result = gl.glGetString(glVendor);
		return result;
	}

	@Override
	public void glLoadMatrix(FloatBuffer m) {
		print("glLoadMatrix", m);
		gl.glLoadMatrix(m);
	}

	@Override
	public void glOrtho(int i, int width, int height, int j, int k, int l) {
		print("glOrtho", i, width, height, j, k, l);
		gl.glOrtho(i, width, height, j, k, l);
	}

	@Override
	public void glPixelStorei(int i, int j) {
		print("glPixelStorei", i, j);
		gl.glPixelStorei(i, j);
	}

	@Override
	public void glPointSize(float value) {
		print("glPointSize", value);
		gl.glPointSize(value);
	}

	@Override
	public void glPolygonMode(int i, int j) {
		print("glPolygonMode", i, j);
		gl.glPolygonMode(i, j);
	}

	@Override
	public void glReadPixels(int x, int y, int width, int height, int glBgr,
			int glUnsignedByte, ByteBuffer image) {
		print("glReadPixels", x, y, width, height, glBgr, glUnsignedByte, image);
		gl.glReadPixels(x, y, width, height, glBgr, glUnsignedByte, image);
	}

	@Override
	public void glTexImage2D(int target, int level, int internalformat,
			int width, int height, int border, int format, int type,
			ByteBuffer pixels) {
		print("glTexImage2D", target, level, internalformat, width, height, border, format, type, pixels);
		gl.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
	}

	@Override
	public void glTexImage2D(int target, int level, int internalformat,
			int width, int height, int border, int format, int type,
			IntBuffer pixels) {
		print("glTexImage2D", target, level, internalformat, width, height, border, format, type, pixels);
		gl.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
	}

	@Override
	public void glTexParameteri(int glTexture2d, int glTextureMinFilter,
			int glFilterMin) {
		print("glTexParameteri", glTexture2d, glTextureMinFilter, glFilterMin);
		gl.glTexParameteri(glTexture2d, glTextureMinFilter, glFilterMin);
	}

	@Override
	public void glTexSubImage2D(int target, int level, int xoffset,
			int yoffset, int width, int height, int format, int type,
			ByteBuffer pixels) {
		print("glTexSubImage2D", target, level, xoffset, yoffset, width, height, format, type, pixels);
		gl.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);
	}

	@Override
	public void glTexSubImage2D(int target, int level, int xoffset,
			int yoffset, int width, int height, int format, int type,
			IntBuffer pixels) {
		print("glTexSubImage2D", target, level, xoffset, yoffset, width, height, format, type, pixels);
		gl.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);
	}

	@Override
	public void shutdow() {
		print("shudown");
		gl.shutdow();
	}

	@Override
	public void glActiveTexture(int texture) {
		print("glActiveTexture", texture);
		gl.glActiveTexture(texture);
	}

	@Override
	public void glClientActiveTexture(int texture) {
		print("glClientActiveTexture");
		gl.glClientActiveTexture(texture);
	}

	@Override
	public void glPointParameterf(int id, float value) {
		print("glPointParameterf", id, value);
	}
	@Override
	public void glGetInteger(int what, IntBuffer params) {
		// TODO Auto-generated method stub
	}

}

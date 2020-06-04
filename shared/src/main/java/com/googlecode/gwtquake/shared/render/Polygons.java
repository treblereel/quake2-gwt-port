package com.googlecode.gwtquake.shared.render;

import java.nio.FloatBuffer;

import com.googlecode.gwtquake.shared.util.Lib;

/**
 * Manages a global buffer for all polygons.
 */
public class Polygons {
  public final static int STRIDE = 7;
  public final static int BYTE_STRIDE = 7 * Lib.SIZEOF_FLOAT;
  public final static int MAX_VERTICES = 64;

  private static final int MAX_BUFFER_VERTICES = 120000;
  private static final int MAX_POLYS = 20000;

  /** 
   * the interleaved buffer has the format:
   * textureCoord0 (index 0, 1)
   * vertex (index 2, 3, 4)
   * textureCoord1 (index 5, 6) 
   */
  static FloatBuffer buffer = Lib.newFloatBuffer(MAX_BUFFER_VERTICES * STRIDE);
  static int bufferIndex = 0;
  static Polygon[] polyCache = new Polygon[MAX_POLYS];
  static int polyCount = 0;
  static float[] s1_old = new float[MAX_VERTICES];

  static {
    for (int i = 0; i < polyCache.length; i++) {
      polyCache[i] = new Polygon();
    }
  }

  static Polygon create(int numverts) {
    Polygon poly = Polygons.polyCache[Polygons.polyCount++];
    poly.clear();
    poly.numverts = numverts;
    poly.pos = Polygons.bufferIndex;
    bufferIndex += numverts;
    return poly;
  }

  static void reset() {
    polyCount = 0;
    bufferIndex = 0;
  }

  static FloatBuffer getRewoundBuffer() {
    return (FloatBuffer) buffer.rewind();
  }
}

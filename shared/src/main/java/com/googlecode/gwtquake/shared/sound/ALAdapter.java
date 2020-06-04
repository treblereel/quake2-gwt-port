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
package com.googlecode.gwtquake.shared.sound;

import java.nio.ByteBuffer;

/**
 * Refactored version of LWJGL AL/AL10 interface that supports multiple
 * implementations.
 *
 * @author cromwellian@google.com (Ray Cromwell)
 */
public abstract class ALAdapter {
  public static ALAdapter impl;
  public static final int AL_INVALID = -1;

  public static final int AL_NONE = 0;

  public static final int AL_FALSE = 0;

  public static final int AL_TRUE = 1;

  public static final int AL_SOURCE_TYPE = 4135;

  public static final int AL_SOURCE_ABSOLUTE = 513;

  public static final int AL_SOURCE_RELATIVE = 514;

  public static final int AL_CONE_INNER_ANGLE = 4097;

  public static final int AL_CONE_OUTER_ANGLE = 4098;

  public static final int AL_PITCH = 4099;

  public static final int AL_POSITION = 4100;

  public static final int AL_DIRECTION = 4101;

  public static final int AL_VELOCITY = 4102;

  public static final int AL_LOOPING = 4103;

  public static final int AL_BUFFER = 4105;

  public static final int AL_GAIN = 4106;

  public static final int AL_MIN_GAIN = 4109;

  public static final int AL_MAX_GAIN = 4110;

  public static final int AL_ORIENTATION = 4111;

  public static final int AL_REFERENCE_DISTANCE = 4128;

  public static final int AL_ROLLOFF_FACTOR = 4129;

  public static final int AL_CONE_OUTER_GAIN = 4130;

  public static final int AL_MAX_DISTANCE = 4131;

  public static final int AL_CHANNEL_MASK = 12288;

  public static final int AL_SOURCE_STATE = 4112;

  public static final int AL_INITIAL = 4113;

  public static final int AL_PLAYING = 4114;

  public static final int AL_PAUSED = 4115;

  public static final int AL_STOPPED = 4116;

  public static final int AL_BUFFERS_QUEUED = 4117;

  public static final int AL_BUFFERS_PROCESSED = 4118;

  public static final int AL_FORMAT_MONO8 = 4352;

  public static final int AL_FORMAT_MONO16 = 4353;

  public static final int AL_FORMAT_STEREO8 = 4354;

  public static final int AL_FORMAT_STEREO16 = 4355;

  public static final int AL_FORMAT_VORBIS_EXT = 65539;

  public static final int AL_FREQUENCY = 8193;

  public static final int AL_BITS = 8194;

  public static final int AL_CHANNELS = 8195;

  public static final int AL_SIZE = 8196;

  public static final int AL_DATA = 8197;

  public static final int AL_UNUSED = 8208;

  public static final int AL_PENDING = 8209;

  public static final int AL_PROCESSED = 8210;

  public static final int AL_NO_ERROR = 0;

  public static final int AL_INVALID_NAME = 40961;

  public static final int AL_INVALID_ENUM = 40962;

  public static final int AL_INVALID_VALUE = 40963;

  public static final int AL_INVALID_OPERATION = 40964;

  public static final int AL_OUT_OF_MEMORY = 40965;

  public static final int AL_VENDOR = 45057;

  public static final int AL_VERSION = 45058;

  public static final int AL_RENDERER = 45059;

  public static final int AL_EXTENSIONS = 45060;

  public static final int AL_DOPPLER_FACTOR = 49152;

  public static final int AL_DOPPLER_VELOCITY = 49153;

  public static final int AL_DISTANCE_MODEL = 53248;

  public static final int AL_INVERSE_DISTANCE = 53249;

  public static final int AL_INVERSE_DISTANCE_CLAMPED = 53250;

  private boolean created;

  public abstract void alBufferData(int buffer, int format,
                                    ByteBuffer data, int freq);

  public abstract void alBufferData(int buffer, int format,
      java.nio.IntBuffer data, int freq);

  public abstract void alBufferData(int buffer, int format,
      java.nio.ShortBuffer data, int freq);

  public abstract void alDeleteBuffers(java.nio.IntBuffer buffers);

  public abstract void alDeleteSources(java.nio.IntBuffer sources);

  public abstract void alDisable(int capability);

  public abstract void alDistanceModel(int value);

  public abstract void alDopplerFactor(float value);

  public abstract void alDopplerVelocity(float value);

  public abstract void alEnable(int capability);

  public abstract void alGenBuffers(java.nio.IntBuffer buffers);

  public abstract void alGenSources(java.nio.IntBuffer sources);

  public abstract boolean alGetBoolean(int pname);

  public abstract float alGetBufferf(int buffer, int pname);

  public abstract int alGetBufferi(int buffer, int pname);

  public abstract double alGetDouble(int pname);

  public abstract void alGetDouble(int pname, java.nio.DoubleBuffer data);

  public abstract int alGetEnumValue(java.lang.String ename);

  public abstract int alGetError();

  public abstract float alGetFloat(int pname);

  public abstract void alGetFloat(int pname, java.nio.FloatBuffer data);

  public abstract int alGetInteger(int pname);

  public abstract void alGetInteger(int pname, java.nio.IntBuffer data);

  public abstract void alGetListener(int pname, java.nio.FloatBuffer floatdata);

  public abstract float alGetListenerf(int pname);

  public abstract int alGetListeneri(int pname);

  public abstract void alGetSource(int source, int pname,
      java.nio.FloatBuffer floatdata);

  public abstract float alGetSourcef(int source, int pname);

  public abstract int alGetSourcei(int source, int pname);

  public abstract String alGetString(int i);

  public abstract boolean alIsBuffer(int buffer);

  public abstract boolean alIsEnabled(int capability);

  public abstract boolean alIsExtensionPresent(java.lang.String fname);

  public abstract boolean alIsSource(int id);

  public abstract void alListener(int pname, java.nio.FloatBuffer value);

  public abstract void alListener3f(int pname, float v1, float v2, float v3);

  public abstract void alListenerf(int pname, float value);

  public abstract void alListeneri(int pname, int value);

  public abstract void alSource(int source, int pname,
      java.nio.FloatBuffer value);

  public abstract void alSource3f(int source, int pname, float v1, float v2,
      float v3);

  public abstract void alSourcef(int source, int pname, float value);

  public abstract void alSourcei(int source, int pname, int value);

  public abstract void alSourcePause(java.nio.IntBuffer sources);

  public abstract void alSourcePause(int source);

  public abstract void alSourcePlay(java.nio.IntBuffer sources);

  public abstract void alSourcePlay(int source);

  public abstract void alSourceQueueBuffers(int source,
      java.nio.IntBuffer buffers);

  public abstract void alSourceRewind(java.nio.IntBuffer sources);

  public abstract void alSourceRewind(int source);

  public abstract void alSourceStop(java.nio.IntBuffer sources);

  public abstract void alSourceStop(int source);

  public abstract void alSourceUnqueueBuffers(int source,
      java.nio.IntBuffer buffers);

  public abstract void create(java.lang.String deviceArguments,
      int contextFrequency, int contextRefresh, boolean contextSynchronized);

  public abstract void create(java.lang.String deviceArguments,
      int contextFrequency, int contextRefresh, boolean contextSynchronized,
      boolean openDevice);

  public void create() {
    created = true;
  }

  public void destroy() {
    created = false;
  }

  public boolean isCreated() {
    return created;
  }

  /**
   * Load buffer from URL
   */
  public void alBufferData(int source, String soundUrl) {
  }
}

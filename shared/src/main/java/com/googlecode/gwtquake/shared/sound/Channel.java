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
package com.googlecode.gwtquake.shared.sound;

import static com.googlecode.gwtquake.shared.common.Constants.ca_active;


import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.googlecode.gwtquake.shared.client.ClientEntities;
import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.Globals;
import com.googlecode.gwtquake.shared.game.EntityState;
import com.googlecode.gwtquake.shared.util.Lib;
import com.googlecode.gwtquake.shared.util.Math3D;


/**
 * Channel
 * 
 * @author dsanders/cwei
 */
public class Channel {

	final static int LISTENER = 0;
	final static int FIXED = 1;
	final static int DYNAMIC = 2;
	final static int MAX_CHANNELS = 32;
	private final static FloatBuffer NULLVECTOR = Lib.newFloatBuffer(3);
	
	private static Channel[] channels = new Channel[MAX_CHANNELS];
	private static IntBuffer sources = Lib.newIntBuffer(MAX_CHANNELS);
	// a reference of L:WJGLSoundImpl.buffers 
	private static IntBuffer buffers;
	private static Map looptable = new HashMap(MAX_CHANNELS);

	private static boolean isInitialized = false;
	private static int numChannels;
    
    // stream handling
    private static boolean streamingEnabled = false;
    private static int streamQueue = 0;
		
	// sound attributes
	private int type;
	private int entnum;
	private int entchannel;
	private int bufferId;
	private int sourceId;
	private float volume;
	private float rolloff;
	private float[] origin = {0, 0, 0};

	// update flags
	private boolean autosound;
	private boolean active;
	private boolean modified;
	private boolean bufferChanged;
	private boolean volumeChanged;

	private Channel(int sourceId) {
		this.sourceId = sourceId;
		clear();
		volumeChanged = false;
		volume = 1.0f;
	}

	private void clear() {
		entnum = entchannel = bufferId = -1;
		bufferChanged = false;
		rolloff = 0;
		autosound = false;
		active = false;
		modified = false;
	}
	
	private static IntBuffer tmp = Lib.newIntBuffer(1);

	public static int init(IntBuffer buffers) {
		Channel.buffers = buffers;
	    // create channels
		int sourceId;
		for (int i = 0; i < MAX_CHANNELS; i++) {
			
			ALAdapter.impl.alGenSources(tmp);
			sourceId = tmp.get(0);
			
            // can't generate more sources 
			if (sourceId <= 0) break;
			
			sources.put(i, sourceId);

			channels[i] = new Channel(sourceId);
			numChannels++;
			
			// set default values for AL sources
			ALAdapter.impl.alSourcef (sourceId, ALAdapter.AL_GAIN, 1.0f);
			ALAdapter.impl.alSourcef (sourceId, ALAdapter.AL_PITCH, 1.0f);
			ALAdapter.impl.alSourcei (sourceId, ALAdapter.AL_SOURCE_ABSOLUTE,  ALAdapter.AL_TRUE);
//			ALAdapter.impl.nalSourcefv(sourceId, ALAdapter.impl.AL_VELOCITY, NULLVECTOR, 0);     // TODO: jgw
			ALAdapter.impl.alSourcei (sourceId, ALAdapter.AL_LOOPING, ALAdapter.AL_FALSE);
			ALAdapter.impl.alSourcef (sourceId, ALAdapter.AL_REFERENCE_DISTANCE, 200.0f);
			ALAdapter.impl.alSourcef (sourceId, ALAdapter.AL_MIN_GAIN, 0.0005f);
			ALAdapter.impl.alSourcef (sourceId, ALAdapter.AL_MAX_GAIN, 1.0f);
		}
		isInitialized = true;
		return numChannels;
	}
	
	public static void reset() {
		for (int i = 0; i < numChannels; i++) {
			ALAdapter.impl.alSourceStop(sources.get(i));
			ALAdapter.impl.alSourcei(sources.get(i), ALAdapter.AL_BUFFER, 0);
			channels[i].clear();
		}
	}
	
	public static void shutdown() {
		ALAdapter.impl.alDeleteSources(sources);
		numChannels = 0;
		isInitialized = false;
	}
	
    static void enableStreaming() {
        if (streamingEnabled) return;
        
        // use the last source
        numChannels--;
        streamingEnabled = true;
        streamQueue = 0;

        int source = channels[numChannels].sourceId;
        ALAdapter.impl.alSourcei (source, ALAdapter.impl.AL_SOURCE_RELATIVE,  ALAdapter.AL_TRUE);
        ALAdapter.impl.alSourcef(source, ALAdapter.AL_GAIN, 1.0f);
        channels[numChannels].volumeChanged = true;

        Com.DPrintf("streaming enabled\n");
    }

    public static void disableStreaming() {
        if (!streamingEnabled) return;
        unqueueStreams();
        int source = channels[numChannels].sourceId;
        ALAdapter.impl.alSourcei (source, ALAdapter.AL_SOURCE_ABSOLUTE,  ALAdapter.AL_TRUE);

        // free the last source
        numChannels++;
        streamingEnabled = false;
        Com.DPrintf("streaming disabled\n");
    }
    
    static void unqueueStreams() {
        if (!streamingEnabled) return;
        int source = channels[numChannels].sourceId;

        // stop streaming
        ALAdapter.impl.alSourceStop(source);
        int count = ALAdapter.impl.alGetSourcei(source, ALAdapter.AL_BUFFERS_QUEUED);
        Com.DPrintf("unqueue " + count + " buffers\n");
        while (count-- > 0) {
            ALAdapter.impl.alSourceUnqueueBuffers(source, tmp);
        }
        streamQueue = 0;
    }

    public static void updateStream(ByteBuffer samples, int count, int format, int rate) {
        enableStreaming();
        int source = channels[numChannels].sourceId;
        int processed = ALAdapter.impl.alGetSourcei(source, ALAdapter.AL_BUFFERS_PROCESSED);
        
        boolean playing = (ALAdapter.impl.alGetSourcei(source, ALAdapter.AL_SOURCE_STATE) == ALAdapter.AL_PLAYING);
        boolean interupted = !playing && streamQueue > 2;
        
        IntBuffer buffer = tmp;
        if (interupted) {
            unqueueStreams();
            buffer.put(0, buffers.get(SoundImpl.MAX_SFX + streamQueue++));
            Com.DPrintf("queue " + (streamQueue - 1) + '\n');
        } else if (processed < 2) {
            // check queue overrun
            if (streamQueue >= SoundImpl.STREAM_QUEUE) return;
            buffer.put(0, buffers.get(SoundImpl.MAX_SFX + streamQueue++));
            Com.DPrintf("queue " + (streamQueue - 1) + '\n');
        } else {
            // reuse the buffer
            ALAdapter.impl.alSourceUnqueueBuffers(source, buffer);
        }

        samples.position(0);
        samples.limit(count);
        ALAdapter.impl.alBufferData(buffer.get(0), format, samples, rate);
        ALAdapter.impl.alSourceQueueBuffers(source, buffer);
        
        if (streamQueue > 1 && !playing) {
            Com.DPrintf("start sound\n");
            ALAdapter.impl.alSourcePlay(source);
        }
    }
    
	public static void addPlaySounds() {
		while (Channel.assign(PlaySound.nextPlayableSound()));
	}
	
	private static boolean assign(PlaySound ps) {
	    if (ps == null) return false;
		Channel ch = null;
		int i;
		for (i = 0; i < numChannels; i++) {
			ch = channels[i];

			if (ps.entchannel != 0 && ch.entnum == ps.entnum && ch.entchannel == ps.entchannel) {
				// always override sound from same entity
				if (ch.bufferId != ps.bufferId) {
				    ALAdapter.impl.alSourceStop(ch.sourceId);
				}
				break;
			}

			// don't let monster sounds override player sounds
			if ((ch.entnum == Globals.cl.playernum+1) && (ps.entnum != Globals.cl.playernum+1) && ch.bufferId != -1)
				continue;

			// looking for a free AL source
			if (!ch.active) {
				break;
			}
		}
		
		if (i == numChannels)
		    return false;
		
		ch.type = ps.type;
		if (ps.type == Channel.FIXED)
		    Math3D.VectorCopy(ps.origin, ch.origin);
		ch.entnum = ps.entnum;
		ch.entchannel = ps.entchannel;
		ch.bufferChanged = (ch.bufferId != ps.bufferId);			
		ch.bufferId = ps.bufferId;
		ch.rolloff = ps.attenuation * 2;
		ch.volumeChanged = (ch.volume != ps.volume);
		ch.volume = ps.volume;
		ch.active = true;
		ch.modified = true;
		return true;
	}
	
	private static Channel pickForLoop(int bufferId, float attenuation) {
        Channel ch;
        for (int i = 0; i < numChannels; i++) {
            ch = channels[i];
            // looking for a free AL source
            if (!ch.active) {
                ch.entnum = 0;
                ch.entchannel = 0;
        		ch.bufferChanged = (ch.bufferId != bufferId);			
                ch.bufferId = bufferId;
           		ch.volumeChanged = (ch.volume != 1.0f);
           		ch.volume = 1.0f;
           	    ch.rolloff = attenuation * 2;
                ch.active = true;
                ch.modified = true;
                return ch;
            }
        }
        return null;
    }
	
	private static FloatBuffer sourceOriginBuffer = Lib.newFloatBuffer(3);

	//stack variable
	private static float[] entityOrigin = {0, 0, 0};
 
	public static void playAllSounds(FloatBuffer listenerOrigin) {
		FloatBuffer sourceOrigin = sourceOriginBuffer;
		Channel ch;
		int sourceId;
		int state;

		for (int i = 0; i < numChannels; i++) {
			ch = channels[i];
			if (ch.active) {
				sourceId = ch.sourceId;
				switch (ch.type) {
					case Channel.LISTENER:
					    sourceOrigin.put(0, listenerOrigin.get(0));
						sourceOrigin.put(1, listenerOrigin.get(1));
						sourceOrigin.put(2, listenerOrigin.get(2));
						break;
					case Channel.DYNAMIC:
						ClientEntities.GetEntitySoundOrigin(ch.entnum, entityOrigin);
						convertVector(entityOrigin, sourceOrigin);
						break;
					case Channel.FIXED:
						convertVector(ch.origin, sourceOrigin);
						break;
				}

				if (ch.modified) {
					if (ch.bufferChanged) {
						ALAdapter.impl.alSourcei(sourceId, ALAdapter.AL_BUFFER, ch.bufferId);
					}
					if (ch.volumeChanged) {
						ALAdapter.impl.alSourcef (sourceId, ALAdapter.AL_GAIN, ch.volume);
					}
					ALAdapter.impl.alSourcef (sourceId, ALAdapter.AL_ROLLOFF_FACTOR, ch.rolloff);
                                        ALAdapter.impl.alSource(sourceId, ALAdapter.AL_POSITION, sourceOrigin);
					ALAdapter.impl.alSourcePlay(sourceId);
					ch.modified = false;
				} else {
					state = ALAdapter.impl.alGetSourcei(sourceId, ALAdapter.AL_SOURCE_STATE);
					if (state == ALAdapter.AL_PLAYING) {
                                          ALAdapter.impl.alSource(sourceId, ALAdapter.AL_POSITION, sourceOrigin);
					} else {
						ch.clear();
					}
				}
				ch.autosound = false;
			}
		}
	}
	
	/*
	 * 	adddLoopSounds
	 * 	Entities with a ->sound field will generated looped sounds
	 * 	that are automatically started, stopped, and merged together
	 * 	as the entities are sent to the client
	 */
	public static void addLoopSounds() {
		
		if ((Globals.cl_paused.value != 0.0f) || (Globals.cls.state != ca_active) || !Globals.cl.sound_prepped) {
			removeUnusedLoopSounds();
			return;
		}
		
		Channel ch;
		Sfx	sfx;
		SfxCache sc;
		int num;
		EntityState ent;
		Object key;
		int sound = 0;

		for (int i=0 ; i < Globals.cl.frame.num_entities ; i++) {
			num = (Globals.cl.frame.parse_entities + i)&(Constants.MAX_PARSE_ENTITIES-1);
			ent = Globals.cl_parse_entities[num];
			sound = ent.sound;

			if (sound == 0) continue;

			key = new Integer(ent.number);
			ch = (Channel)looptable.get(key);

			if (ch != null) {
				// keep on looping
				ch.autosound = true;
				Math3D.VectorCopy(ent.origin, ch.origin);
				continue;
			}

			sfx = Globals.cl.sound_precache[sound];
			if (sfx == null)
				continue;		// bad sound effect

			sc = sfx.cache;
			if (sc == null)
				continue;

			// allocate a channel
			ch = Channel.pickForLoop(buffers.get(sfx.bufferId), 6);
			if (ch == null)
				break;
				
			ch.type = FIXED;
			Math3D.VectorCopy(ent.origin, ch.origin);
			ch.autosound = true;
			
			looptable.put(key, ch);
			ALAdapter.impl.alSourcei(ch.sourceId, ALAdapter.AL_LOOPING, ALAdapter.AL_TRUE);
		}

		removeUnusedLoopSounds();

	}
	
	private static void removeUnusedLoopSounds() {
		Channel ch;
		// stop unused loopsounds
		for (Iterator iter = looptable.values().iterator(); iter.hasNext();) {
			ch = (Channel)iter.next();
			if (!ch.autosound) {
				ALAdapter.impl.alSourceStop(ch.sourceId);
				ALAdapter.impl.alSourcei(ch.sourceId, ALAdapter.AL_LOOPING, ALAdapter.AL_FALSE);
				iter.remove();
				ch.clear();
			}
		}
	}

	public static void convertVector(float[] from, FloatBuffer to) {
		to.put(0, from[0]);
		to.put(1, from[2]);
		to.put(2, -from[1]);
	}

	public static void convertOrientation(float[] forward, float[] up, FloatBuffer orientation) {
		orientation.put(0, forward[0]);
		orientation.put(1, forward[2]);
		orientation.put(2, -forward[1]);
		orientation.put(3, up[0]);
		orientation.put(4, up[2]);
		orientation.put(5, -up[1]);
	}

}

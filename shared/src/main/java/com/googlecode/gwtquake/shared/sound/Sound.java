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



import java.nio.ByteBuffer;

import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.ConsoleVariables;
import com.googlecode.gwtquake.shared.game.ConsoleVariable;

/**
 * S
 */
public class Sound {
	
	public static SoundImpl impl;

	/**
	 * Initializes the sound module.
	 */
	public static void Init() {
		Com.Printf("\n------- sound initialization -------\n");

		ConsoleVariable cv = ConsoleVariables.Get("s_initsound", "1", 0);
		if (cv.value == 0.0f) {
			Com.Printf("not initializing.\n");
			return;			
		}

		Com.Printf("\n------- use sound driver \"" + impl.getName() + "\" -------\n");
		StopAllSounds();
	}
	
	public static void Shutdown() {
		impl.Shutdown();
	}
	
	/**
	 * Called before the sounds are to be loaded and registered.
	 */
	public static void BeginRegistration() {
		impl.BeginRegistration();		
	}
	
	/**
	 * Registers and loads a sound.
	 */
	public static Sfx RegisterSound(String sample) {
		return impl.RegisterSound(sample);
	}
	
	/**
	 * Called after all sounds are registered and loaded.
	 */
	public static void EndRegistration() {
		impl.EndRegistration();
	}
	
	/**
	 * Starts a local sound.
	 */
	public static void StartLocalSound(String sound) {
		impl.StartLocalSound(sound);		
	}
	
	/** 
	 * StartSound - Validates the parms and ques the sound up
	 * if pos is NULL, the sound will be dynamically sourced from the entity
	 * Entchannel 0 will never override a playing sound
	 */
	public static void StartSound(float[] origin, int entnum, int entchannel, Sfx sfx, float fvol, float attenuation, float timeofs) {
		impl.StartSound(origin, entnum, entchannel, sfx, fvol, attenuation, timeofs);
	}

	/**
	 * Updates the sound renderer according to the changes in the environment,
	 * called once each time through the main loop.
	 */
	public static void Update(float[] origin, float[] forward, float[] right, float[] up) {
		impl.Update(origin, forward, right, up);
	}

	/**
	 * Cinematic streaming and voice over network.
	 */
	public static void RawSamples(int samples, int rate, int width, int channels, ByteBuffer data) {
		impl.RawSamples(samples, rate, width, channels, data);
	}
    
	/**
	 * Switches off the sound streaming.
	 */ 
    public static void disableStreaming() {
        impl.disableStreaming();
    }

	/**
	 * Stops all sounds. 
	 */
	public static void StopAllSounds() {
		impl.StopAllSounds();
	}
	
	public static String getDriverName() {
		return impl.getName();
	}

	/**
	 * This is used, when resampling to this default sampling rate is activated 
	 * in the wavloader. It is placed here that sound implementors can override 
	 * this one day.
	 */
	public static int getDefaultSampleRate() {
		return 44100;
	}
}

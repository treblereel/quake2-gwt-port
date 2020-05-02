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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import com.googlecode.gwtquake.client.GwtQuake.BrowserType;
import com.googlecode.gwtquake.shared.client.Console;
import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.ConsoleVariables;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.ExecutableCommand;
import com.googlecode.gwtquake.shared.common.Globals;
import com.googlecode.gwtquake.shared.game.Commands;
import com.googlecode.gwtquake.shared.game.ConsoleVariable;
import com.googlecode.gwtquake.shared.game.EntityState;
import com.googlecode.gwtquake.shared.sound.ALAdapter;
import com.googlecode.gwtquake.shared.sound.Channel;
import com.googlecode.gwtquake.shared.sound.PlaySound;
import com.googlecode.gwtquake.shared.sound.Sfx;
import com.googlecode.gwtquake.shared.sound.SfxCache;
import com.googlecode.gwtquake.shared.sound.SoundImpl;
import com.googlecode.gwtquake.shared.util.Lib;
import com.googlecode.gwtquake.shared.util.Vargs;

import static com.googlecode.gwtquake.shared.common.Constants.CS_PLAYERSKINS;

public class GwtSound implements SoundImpl {

    static Sfx[] known_sfx = new Sfx[MAX_SFX];
    static int num_sfx;

    static {
        ALAdapter.impl = new WebALAdapter();
    }

    static {
        for (int i = 0; i < known_sfx.length; i++) {
            known_sfx[i] = new Sfx();
        }
    }

    int s_registration_sequence;
    boolean s_registering;
    private boolean hasEAX;
    private ConsoleVariable s_volume;
    // the last 4 buffers are used for cinematics streaming
    private IntBuffer buffers = Lib.newIntBuffer(MAX_SFX + STREAM_QUEUE);
    // TODO check the sfx direct buffer size
    // 2MB sfx buffer
    private ByteBuffer sfxDataBuffer = Lib.newByteBuffer(2 * 1024 * 1024);
    private FloatBuffer listenerOrigin = Lib.newFloatBuffer(3);
    private FloatBuffer listenerOrientation = Lib.newFloatBuffer(6);
    private IntBuffer eaxEnv = Lib.newIntBuffer(1);
    private ShortBuffer streamBuffer = sfxDataBuffer.slice()
            .order(ByteOrder.BIG_ENDIAN).asShortBuffer();

    public GwtSound() {
        Init();
    }

    /* (non-Javadoc)
     * @see jake2.sound.Sound#getName()
     */
    public String getName() {
        return "HTML5Audio";
    }

    /* (non-Javadoc)
     * @see jake2.sound.SoundImpl#Init()
     */
    public boolean Init() {
        try {
            initOpenAL();
            checkError();
            initOpenALExtensions();
        } catch (Exception e) {
            Com.DPrintf(e.getMessage() + '\n');
            return false;
        }

        // set the listerner (master) volume
        s_volume = ConsoleVariables.Get("s_volume", "0.7", Constants.CVAR_ARCHIVE);
        ALAdapter.impl.alGenBuffers(buffers);
        int count = Channel.init(buffers);
        Com.Printf("... using " + count + " channels\n");
        ALAdapter.impl.alDistanceModel(ALAdapter.AL_INVERSE_DISTANCE_CLAMPED);
        Commands.addCommand("play", new ExecutableCommand() {
            public void execute() {
                Play();
            }
        });
        Commands.addCommand("stopsound", new ExecutableCommand() {
            public void execute() {
                StopAllSounds();
            }
        });
        Commands.addCommand("soundlist", new ExecutableCommand() {
            public void execute() {
                SoundList();
            }
        });
        Commands.addCommand("soundinfo", new ExecutableCommand() {
            public void execute() {
                SoundInfo_f();
            }
        });

        num_sfx = 0;

        Com.Printf("sound sampling rate: 44100Hz\n");

        StopAllSounds();
        Com.Printf("------------------------------------\n");
        return true;
    }

    /* (non-Javadoc)
     * @see jake2.sound.SoundImpl#Shutdown()
     */
    public void Shutdown() {
        StopAllSounds();
        Channel.shutdown();
        ALAdapter.impl.alDeleteBuffers(buffers);
        exitOpenAL();

        Commands.RemoveCommand("play");
        Commands.RemoveCommand("stopsound");
        Commands.RemoveCommand("soundlist");
        Commands.RemoveCommand("soundinfo");

        // free all sounds
        for (int i = 0; i < num_sfx; i++) {
            if (known_sfx[i].name == null) {
                continue;
            }
            known_sfx[i].clear();
        }
        num_sfx = 0;
    }

    /* (non-Javadoc)
     * @see jake2.sound.Sound#BeginRegistration()
     */
    public void BeginRegistration() {
        s_registration_sequence++;
        s_registering = true;
    }

    /* (non-Javadoc)
     * @see jake2.sound.Sound#RegisterSound(java.lang.String)
     */
    public Sfx RegisterSound(String name) {
//		log("Trying to load "+name);

        Sfx sfx = FindName(name, true);
        sfx.registration_sequence = s_registration_sequence;

        if (!s_registering) {
            LoadSound(sfx);
        }

        return sfx;
    }

    /* (non-Javadoc)
     * @see jake2.sound.Sound#EndRegistration()
     */
    public void EndRegistration() {
        int i;
        Sfx sfx;
        int size;

        // free any sounds not from this registration sequence
        for (i = 0; i < num_sfx; i++) {
            sfx = known_sfx[i];
            if (sfx.name == null) {
                continue;
            }
            if (sfx.registration_sequence != s_registration_sequence) {
                // don't need this sound
                sfx.clear();
            }
        }

        // load everything in
        for (i = 0; i < num_sfx; i++) {
            sfx = known_sfx[i];
            if (sfx.name == null) {
                continue;
            }
            LoadSound(sfx);
        }

        s_registering = false;
    }

    /*
          ==============
          S_LoadSound
          ==============
          */
    public SfxCache LoadSound(Sfx s) {

        if (s.isCached) {
            return s.cache;
        }
        if (s.name.charAt(0) == '*') {
            return null;
        }

        // see if still in memory
        gwtsfxcache_t sc = (gwtsfxcache_t) s.cache;
        if (sc != null) {
            return sc;
        }

        String name;
        // load it in
        if (s.truename != null) {
            name = s.truename;
        } else {
            name = s.name;
        }

        String namebuffer;
        if (name.charAt(0) == '#') {
            namebuffer = name.substring(1);
        } else {
            namebuffer = "sound/" + name;
        }

        sc = new gwtsfxcache_t(1);

        if (sc != null) {
            s.cache = sc;
            if (namebuffer.endsWith(".wav")) {
                namebuffer += GwtQuake.getBrowserType() == BrowserType.SAFARI ? ".mp3" : ".ogg";
            }
            Console.Print("Creating audio element " + namebuffer + "\r");

            sc.soundUrl = "baseq2/" + namebuffer;
            initBuffer(sc.soundUrl, sc.data, s.bufferId, sc.speed);
            s.isCached = true;
            // free samples for GC
            s.cache.data = null;
        }

        return sc;
    }

    /* (non-Javadoc)
     * @see jake2.sound.SoundImpl#RegisterSound(jake2.sound.sfx_t)
     */
    private void initBuffer(String soundUrl, byte[] samples, int bufferId, int freq) {
        ALAdapter.impl.alBufferData(buffers.get(bufferId),
                                    soundUrl);
    }

    /* (non-Javadoc)
     * @see jake2.sound.Sound#StartLocalSound(java.lang.String)
     */
    public void StartLocalSound(String sound) {
        Sfx sfx;

        sfx = RegisterSound(sound);
        if (sfx == null) {
            Com.Printf("S_StartLocalSound: can't cache " + sound + "\n");
            return;
        }
        StartSound(null, Globals.cl.playernum + 1, 0, sfx, 1, 1, 0);
    }

    /* (non-Javadoc)
     * @see jake2.sound.SoundImpl#StartSound(float[], int, int, jake2.sound.sfx_t, float, float, float)
     */
    public void StartSound(float[] origin, int entnum, int entchannel, Sfx sfx,
                           float fvol, float attenuation, float timeofs) {

        if (sfx == null) {
            return;
        }

        if (sfx.name.charAt(0) == '*') {
            sfx = RegisterSexedSound(Globals.cl_entities[entnum].current, sfx.name);
        }

        if (LoadSound(sfx) == null) {
            return;
        } // can't load sound

        if (attenuation != Constants.ATTN_STATIC) {
            attenuation *= 0.5f;
        }

        PlaySound
                .allocate(origin, entnum, entchannel, buffers.get(sfx.bufferId), fvol,
                          attenuation, timeofs);
//		((gwtsfxcache_t)sfx.cache).audioElement.play();
    }

    /* (non-Javadoc)
     * @see jake2.sound.SoundImpl#Update(float[], float[], float[], float[])
     */
    public void Update(float[] origin, float[] forward, float[] right,
                       float[] up) {

        Channel.convertVector(origin, listenerOrigin);
        ALAdapter.impl
                .alListener3f(ALAdapter.impl.AL_POSITION, listenerOrigin.get(0),
                              listenerOrigin.get(1), listenerOrigin.get(2)); // TODO(jgw)

//    Channel.convertOrientation(forward, up, listenerOrientation);
//		ALAdapter.impl.nalListenerfv(ALAdapter.impl.AL_ORIENTATION, listenerOrientation, 0); // TODO(jgw)

        // set the master volume
        ALAdapter.impl.alListenerf(ALAdapter.impl.AL_GAIN, s_volume.value);

// TODO(jgw)
//		if (hasEAX){
//			if ((GameBase.gi.pointcontents.pointcontents(origin)& Defines.MASK_WATER)!= 0) {
//				changeEnvironment(EAX20.EAX_ENVIRONMENT_UNDERWATER);
//			} else {
//				changeEnvironment(EAX20.EAX_ENVIRONMENT_GENERIC);
//			}
//		}

        Channel.addLoopSounds();
        Channel.addPlaySounds();
        Channel.playAllSounds(listenerOrigin);
    }

    /* (non-Javadoc)
     * @see jake2.sound.Sound#RawSamples(int, int, int, int, byte[])
     */
    public void RawSamples(int samples, int rate, int width, int channels,
                           ByteBuffer data) {
        int format;
    }

    public void disableStreaming() {
    }

    /* (non-Javadoc)
     * @see jake2.sound.SoundImpl#StopAllSounds()
     */
    public void StopAllSounds() {
        ALAdapter.impl.alListenerf(ALAdapter.impl.AL_GAIN, 0);
        PlaySound.reset();
        Channel.reset();
    }

    void exitOpenAL() {
        // Release the EAX context.
//		if (hasEAX){
//			EAX.destroy();
//		}
        // Release the context and the device.
        ALAdapter.impl.destroy();
    }

    /*
          ==================
          S_AliasName

          ==================
          */
    Sfx AliasName(String aliasname, String truename) {
        Sfx sfx = null;
        String s;
        int i;

        s = truename;

        // find a free sfx
        for (i = 0; i < num_sfx; i++) {
            if (known_sfx[i].name == null) {
                break;
            }
        }

        if (i == num_sfx) {
            if (num_sfx == MAX_SFX) {
                Com.Error(Constants.ERR_FATAL, "S_FindName: out of sfx_t");
            }
            num_sfx++;
        }

        sfx = known_sfx[i];
        sfx.clear();
        sfx.name = aliasname;
        sfx.registration_sequence = s_registration_sequence;
        sfx.truename = s;
        // set the AL bufferId
        sfx.bufferId = i;

        return sfx;
    }

    Sfx FindName(String name, boolean create) {
        int i;
        Sfx sfx = null;

        if (name == null) {
            Com.Error(Constants.ERR_FATAL, "S_FindName: NULL\n");
        }
        if (name.length() == 0) {
            Com.Error(Constants.ERR_FATAL, "S_FindName: empty name\n");
        }

        if (name.length() >= Constants.MAX_QPATH) {
            Com.Error(Constants.ERR_FATAL, "Sound name too long: " + name);
        }

        // see if already loaded
        for (i = 0; i < num_sfx; i++) {
            if (name.equals(known_sfx[i].name)) {
                return known_sfx[i];
            }
        }

        if (!create) {
            return null;
        }

        // find a free sfx
        for (i = 0; i < num_sfx; i++) {
            if (known_sfx[i].name == null)
            // registration_sequence < s_registration_sequence)
            {
                break;
            }
        }

        if (i == num_sfx) {
            if (num_sfx == MAX_SFX) {
                Com.Error(Constants.ERR_FATAL, "S_FindName: out of sfx_t");
            }
            num_sfx++;
        }

        sfx = known_sfx[i];
        sfx.clear();
        sfx.name = name;
        sfx.registration_sequence = s_registration_sequence;
        sfx.bufferId = i;

        return sfx;
    }
  /*
        ===============================================================================

        console functions

        ===============================================================================
        */

    void Play() {
        int i;
        String name;
        Sfx sfx;

        i = 1;
        while (i < Commands.Argc()) {
            name = new String(Commands.Argv(i));
            if (name.indexOf('.') == -1) {
                name += ".wav";
            }

            sfx = RegisterSound(name);
            StartSound(null, Globals.cl.playernum + 1, 0, sfx, 1.0f, 1.0f, 0.0f);
            i++;
        }
    }

    Sfx RegisterSexedSound(EntityState ent, String base) {

        Sfx sfx = null;

        // determine what model the client is using
        String model = null;
        int n = CS_PLAYERSKINS + ent.number - 1;
        if (Globals.cl.configstrings[n] != null) {
            int p = Globals.cl.configstrings[n].indexOf('\\');
            if (p >= 0) {
                p++;
                model = Globals.cl.configstrings[n].substring(p);
                //strcpy(model, p);
                p = model.indexOf('/');
                if (p > 0) {
                    model = model.substring(0, p);
                }
            }
        }
        // if we can't figure it out, they're male
        if (model == null || model.length() == 0) {
            model = "male";
        }

        // see if we already know of the model specific sound
        String sexedFilename = "#players/" + model + "/" + base.substring(1);
        //Com_sprintf (sexedFilename, sizeof(sexedFilename), "#players/%s/%s", model, base+1);
        sfx = FindName(sexedFilename, false);

        if (sfx != null) {
            return sfx;
        }

        //
        // fall back strategies
        //
        // not found , so see if it exists
//		if (FileSystem.FileLength(sexedFilename.substring(1)) > 0) {
        // yes, register it
//			return RegisterSound(sexedFilename);
//		}
        // try it with the female sound in the pak0.pak
//		if (model.equalsIgnoreCase("female")) {
//			String femaleFilename = "player/female/" + base.substring(1);
//			if (FileSystem.FileLength("sound/" + femaleFilename) > 0)
//			    return AliasName(sexedFilename, femaleFilename);
//		}
        // no chance, revert to the male sound in the pak0.pak
        String maleFilename = "player/male/" + base.substring(1);
        return AliasName(sexedFilename, maleFilename);
    }

    void SoundInfo_f() {

        Com.Printf("%5d stereo\n", new Vargs(1).add(1));
        Com.Printf("%5d samples\n", new Vargs(1).add(22050));
        Com.Printf("%5d samplebits\n", new Vargs(1).add(16));
        Com.Printf("%5d speed\n", new Vargs(1).add(44100));
    }

    void SoundList() {
        int i;
        Sfx sfx;
        SfxCache sc;
        int size, total;

        total = 0;
        for (i = 0; i < num_sfx; i++) {
            sfx = known_sfx[i];
            if (sfx.registration_sequence == 0) {
                continue;
            }
            sc = sfx.cache;
            if (sc != null) {
                size = sc.length * sc.width * (sc.stereo + 1);
                total += size;
                if (sc.loopstart >= 0) {
                    Com.Printf("L");
                } else {
                    Com.Printf(" ");
                }
                Com.Printf("(%2db) %6i : %s\n",
                           new Vargs(3).add(sc.width * 8).add(size).add(sfx.name));
            } else {
                if (sfx.name.charAt(0) == '*') {
                    Com.Printf("  placeholder : " + sfx.name + "\n");
                } else {
                    Com.Printf("  not loaded  : " + sfx.name + "\n");
                }
            }
        }
        Com.Printf("Total resident: " + total + "\n");
    }

    private String alErrorString() {
        int error;
        String message = "";
        if ((error = ALAdapter.impl.alGetError()) != ALAdapter.impl.AL_NO_ERROR) {
            switch (error) {
                case ALAdapter.AL_INVALID_OPERATION:
                    message = "invalid operation";
                    break;
                case ALAdapter.AL_INVALID_VALUE:
                    message = "invalid value";
                    break;
                case ALAdapter.AL_INVALID_ENUM:
                    message = "invalid enum";
                    break;
                case ALAdapter.AL_INVALID_NAME:
                    message = "invalid name";
                    break;
                default:
                    message = "" + error;
            }
        }
        return message;
    }

    private void changeEnvironment(int env) {

    }

    private void checkError() {
        Com.DPrintf("AL Error: " + alErrorString() + '\n');
    }

    private void initOpenAL() throws Exception {
        ALAdapter.impl.create();
        String deviceName = null;

        String os = System.getProperty("os.name");
        if (os != null && os.startsWith("Windows")) {
            deviceName = "DirectSound3D";
        }

// TODO(jgw)
//		String deviceSpecifier = ALC.alcGetString(ALC.ALC_DEVICE_SPECIFIER);
//		String defaultSpecifier = ALC.alcGetString(ALC.ALC_DEFAULT_DEVICE_SPECIFIER);
//
//		Com.Printf(os + " using " + ((deviceName == null) ? defaultSpecifier : deviceName) + '\n');
//
//		// Check for an error.
//		if (ALC.alcGetError() != ALC.ALC_NO_ERROR)
//		{
//			Com.DPrintf("Error with SoundDevice");
//		}
    }

    private void initOpenALExtensions() throws Exception {
//		if (ALAdapter.impl.alIsExtensionPresent("EAX2.0"))
//		{
//			try {
//				EAX.create();
//				Com.Printf("... using EAX2.0\n");
//				hasEAX=true;
//			} catch (LWJGLException e) {
//				Com.Printf("... can't create EAX2.0\n");
//				hasEAX=false;
//			}
//		}
//		else
        {
            Com.Printf("... EAX2.0 not found\n");
            hasEAX = false;
        }
    }

    static class gwtsfxcache_t extends SfxCache {

        public String soundUrl;

        public gwtsfxcache_t(int size) {
            super(size);
            // TODO Auto-generated constructor stub
        }
    }
}

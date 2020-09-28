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
package com.googlecode.gwtquake.shared.client;

import java.io.IOException;
import java.io.RandomAccessFile;

import com.googlecode.gwtquake.shared.common.AsyncCallback;
import com.googlecode.gwtquake.shared.common.Buffers;
import com.googlecode.gwtquake.shared.common.CM;
import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.CommandBuffer;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.ExecutableCommand;
import com.googlecode.gwtquake.shared.common.Globals;
import com.googlecode.gwtquake.shared.common.QuakeFileSystem;
import com.googlecode.gwtquake.shared.game.Commands;
import com.googlecode.gwtquake.shared.game.EntityState;
import com.googlecode.gwtquake.shared.render.Model;
import com.googlecode.gwtquake.shared.sound.Sound;
import com.googlecode.gwtquake.shared.sys.Sys;
import com.googlecode.gwtquake.shared.util.Lib;
import elemental2.dom.DomGlobal;

/**
 * CL_parse
 */
public class ClientParser {

    //// cl_parse.c -- parse a message received from the server

    public static String svc_strings[] = { "svc_bad", "svc_muzzleflash",
            "svc_muzzlflash2", "svc_temp_entity", "svc_layout",
            "svc_inventory", "svc_nop", "svc_disconnect", "svc_reconnect",
            "svc_sound", "svc_print", "svc_stufftext", "svc_serverdata",
            "svc_configstring", "svc_spawnbaseline", "svc_centerprint",
            "svc_download", "svc_playerinfo", "svc_packetentities",
            "svc_deltapacketentities", "svc_frame" };

    //	  =============================================================================

    public static String DownloadFileName(String fn) {
        return QuakeFileSystem.Gamedir() + "/" + fn;
    }

    /*
     * =============== CL_CheckOrDownloadFile
     * 
     * Returns true if the file exists, otherwise it attempts to start a
     * download from the server. ===============
     */
    public static boolean CheckOrDownloadFile(String filename) {
        RandomAccessFile fp;
        String name;

        if (filename.indexOf("..") != -1) {
            Com.Printf("Refusing to download a path with ..\n");
            return true;
        }

        if (QuakeFileSystem.FileLength(filename) > 0) { // it exists, no need to download
            return true;
        }

        Globals.cls.downloadname = filename;

        // download to a temp name, and only rename
        // to the real name when done, so if interrupted
        // a runt file wont be left
        Globals.cls.downloadtempname = Com
                .StripExtension(Globals.cls.downloadname);
        Globals.cls.downloadtempname += ".tmp";

        //	  ZOID
        // check to see if we already have a tmp for this file, if so, try to
        // resume
        // open the file if not opened yet
        name = DownloadFileName(Globals.cls.downloadtempname);

        fp = Lib.fopen(name, "r+b");
        
        if (fp != null) { 
            
            // it exists
            long len = 0;

            try {
                len = fp.length();
            } 
            catch (IOException e) {
            }
            

            Globals.cls.download = fp;

            // give the server an offset to start the download
            Com.Printf("Resuming " + Globals.cls.downloadname + "\n");
            Buffers.writeByte(Globals.cls.netchan.message, Constants.clc_stringcmd);
            Buffers.WriteString(Globals.cls.netchan.message, "download "
                    + Globals.cls.downloadname + " " + len);
        } else {
            Com.Printf("Downloading " + Globals.cls.downloadname + "\n");
            Buffers.writeByte(Globals.cls.netchan.message, Constants.clc_stringcmd);
            Buffers.WriteString(Globals.cls.netchan.message, "download "
                    + Globals.cls.downloadname);
        }

        Globals.cls.downloadnumber++;

        return false;
    }

    /*
     * =============== CL_Download_f
     * 
     * Request a download from the server ===============
     */
    public static ExecutableCommand downloadCommand = new ExecutableCommand() {
        public void execute() {
            String filename;

            if (Commands.Argc() != 2) {
                Com.Printf("Usage: download <filename>\n");
                return;
            }

            filename = Commands.Argv(1);

            if (filename.indexOf("..") != -1) {
                Com.Printf("Refusing to download a path with ..\n");
                return;
            }

            if (QuakeFileSystem.LoadFile(filename) != null) { // it exists, no need to
                // download
                Com.Printf("File already exists.\n");
                return;
            }

            Globals.cls.downloadname = filename;
            Com.Printf("Downloading " + Globals.cls.downloadname + "\n");

            // download to a temp name, and only rename
            // to the real name when done, so if interrupted
            // a runt file wont be left
            Globals.cls.downloadtempname = Com
                    .StripExtension(Globals.cls.downloadname);
            Globals.cls.downloadtempname += ".tmp";

            Buffers.writeByte(Globals.cls.netchan.message, Constants.clc_stringcmd);
            Buffers.WriteString(Globals.cls.netchan.message, "download "
                    + Globals.cls.downloadname);

            Globals.cls.downloadnumber++;
        }
    };

    /*
     * ====================== CL_RegisterSounds ======================
     */
    public static void RegisterSounds() {
        Sound.BeginRegistration();
        ClientTent.RegisterTEntSounds();
        for (int i = 1; i < Constants.MAX_SOUNDS; i++) {
            if (Globals.cl.configstrings[Constants.CS_SOUNDS + i] == null
                    || Globals.cl.configstrings[Constants.CS_SOUNDS + i]
                            .equals("")
                    || Globals.cl.configstrings[Constants.CS_SOUNDS + i]
                            .equals("\0"))
                break;
            Globals.cl.sound_precache[i] = Sound
                    .RegisterSound(Globals.cl.configstrings[Constants.CS_SOUNDS
                            + i]);
            Sys.SendKeyEvents(); // pump message loop
        }
        Sound.EndRegistration();
    }

    /*
     * ===================== CL_ParseDownload
     * 
     * A download message has been received from the server
     * =====================
     */
    public static void ParseDownload() {

        // read the data
        int size = Globals.net_message.getShort();
        int percent = Buffers.readUnsignedByte(Globals.net_message);
        if (size == -1) {
            Com.Printf("Server does not have this file.\n");
            if (Globals.cls.download != null) {
                // if here, we tried to resume a file but the server said no
                try {
                    Globals.cls.download.close();
                } catch (IOException e) {
                }
                Globals.cls.download = null;
            }
            Client.requestNextDownload();
            return;
        }

        // open the file if not opened yet
        if (Globals.cls.download == null) {
            String name = DownloadFileName(Globals.cls.downloadtempname).toLowerCase();

            QuakeFileSystem.CreatePath(name);

            Globals.cls.download = Lib.fopen(name, "rw");
            if (Globals.cls.download == null) {
                Globals.net_message.readcount += size;
                Com.Printf("Failed to open " + Globals.cls.downloadtempname
                        + "\n");
                Client.requestNextDownload();
                return;
            }
        }

        //fwrite(net_message.data[net_message.readcount], 1, size,
        // cls.download);
        try {
            Globals.cls.download.write(Globals.net_message.data,
                    Globals.net_message.readcount, size);
        } catch (Exception e) {
        }
        Globals.net_message.readcount += size;

        if (percent != 100) {
            // request next block
            //	   change display routines by zoid
            Globals.cls.downloadpercent = percent;
            Buffers.writeByte(Globals.cls.netchan.message, Constants.clc_stringcmd);
            Buffers.Print(Globals.cls.netchan.message, "nextdl");
        } else {
            String oldn, newn;
            //char oldn[MAX_OSPATH];
            //char newn[MAX_OSPATH];

            //			Com.Printf ("100%%\n");

            try {
                Globals.cls.download.close();
            } catch (IOException e) {
            }

            // rename the temp file to it's final name
            oldn = DownloadFileName(Globals.cls.downloadtempname);
            newn = DownloadFileName(Globals.cls.downloadname);
            int r = Lib.rename(oldn, newn);
            if (r != 0)
                Com.Printf("failed to rename.\n");

            Globals.cls.download = null;
            Globals.cls.downloadpercent = 0;

            // get another file if needed

            Client.requestNextDownload();
        }
    }

    /*
     * =====================================================================
     * 
     * SERVER CONNECTING MESSAGES
     * 
     * =====================================================================
     */

    /*
     * ================== CL_ParseServerData ==================
     */
    //checked once, was ok.
    public static void ParseServerData() {
        String str;
        int i;

        Com.DPrintf("ParseServerData():Serverdata packet received.\n");
        //
        //	   wipe the client_state_t struct
        //
        Client.clearState();
        Globals.cls.state = Constants.ca_connected;

        //	   parse protocol version number
        i = Globals.net_message.getInt();
        Globals.cls.serverProtocol = i;

        // BIG HACK to let demos from release work with the 3.0x patch!!!
        if (Globals.server_state != 0 && Constants.PROTOCOL_VERSION == 34) {
        } else if (i != Constants.PROTOCOL_VERSION)
            Com.Error(Constants.ERR_DROP, "Server returned version " + i
                    + ", not " + Constants.PROTOCOL_VERSION);

        Globals.cl.servercount = Globals.net_message.getInt();
        Globals.cl.attractloop = Buffers.readUnsignedByte(Globals.net_message) != 0;

        // game directory
        str = Buffers.getString(Globals.net_message);
        Globals.cl.gamedir = str;
        Com.dprintln("gamedir=" + str);

        // set gamedir
// TODO(jgw): I think this is irrelevant now.
//        if (str.length() > 0
//                && (FS.fs_gamedirvar.string == null
//                        || FS.fs_gamedirvar.string.length() == 0 || FS.fs_gamedirvar.string
//                        .equals(str))
//                || (str.length() == 0 && (FS.fs_gamedirvar.string != null || FS.fs_gamedirvar.string
//                        .length() == 0)))
//            Cvar.Set("game", str);

        // parse player entity number
        Globals.cl.playernum = Globals.net_message.getShort();
        Com.dprintln("numplayers=" + Globals.cl.playernum);
        // get the full level name
        str = Buffers.getString(Globals.net_message);
        Com.dprintln("levelname=" + str);

        if (Globals.cl.playernum == -1) { // playing a cinematic or showing a
            // pic, not a level
            Screen.PlayCinematic(str);
        } else {
            // seperate the printfs so the server message can have a color
            //			Com.Printf(
            //				"\n\n\35\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\37\n\n");
            //			Com.Printf('\02' + str + "\n");
            Com.Printf("Levelname:" + str + "\n");
            // need to prep refresh at next oportunity
            Globals.cl.refresh_prepped = false;
        }
    }

    /*
     * ================== CL_ParseBaseline ==================
     */
    public static void ParseBaseline() {
        EntityState es;
        int newnum;

        EntityState nullstate = new EntityState(null);
        //memset(nullstate, 0, sizeof(nullstate));
        int bits[] = { 0 };
        newnum = ClientEntities.ParseEntityBits(bits);
        es = Globals.cl_entities[newnum].baseline;
        ClientEntities.ParseDelta(nullstate, es, newnum, bits[0]);
    }

    /*
     * ================ CL_LoadClientinfo
     * 
     * ================
     */
    public static void LoadClientinfo(final ClientInfo ci, String s) {
        int i;
        int t;

        String model_name, skin_name, model_filename, skin_filename;
        String[] weapon_filenames = new String[Constants.MAX_CLIENTWEAPONMODELS];
        int num_weapon_filenames = 0;

        ci.cinfo = s;

        // isolate the player's name
        ci.name = s;

        t = s.indexOf('\\');

        if (t != -1) {
            ci.name = s.substring(0, t);
            s = s.substring(t + 1, s.length());
            //s = t + 1;
        }

        if (Globals.cl_noskins.value != 0 || s.length() == 0) {
            model_filename = ("players/male/tris.md2");
            weapon_filenames[0] = ("players/male/weapon.md2");
            num_weapon_filenames = 1;

            skin_filename = ("players/male/grunt.pcx");
            ci.iconname = ("/players/male/grunt_i.pcx");

            ci.skin = Globals.re.RegisterSkin(skin_filename);
            ci.icon = Globals.re.RegisterPic(ci.iconname);
        } else {
            // isolate the model name
            int pos = s.indexOf('/');

            if (pos == -1)
                pos = s.indexOf('/');
            if (pos == -1) {
                pos = 0;
                Com.Error(Constants.ERR_FATAL, "Invalid model name:" + s);
            }

            model_name = s.substring(0, pos);

            // isolate the skin name
            skin_name = s.substring(pos + 1, s.length());

            // model file
            model_filename = "players/" + model_name + "/tris.md2";

            if (ci.model == null) {
                //model_name = "male";
                //model_filename = "players/male/tris.md2";
            }

            // skin file
            skin_filename = "players/" + model_name + "/" + skin_name + ".pcx";
            ci.skin = Globals.re.RegisterSkin(skin_filename);

            // if we don't have the skin and the model wasn't male,
            // see if the male has it (this is for CTF's skins)
            if (ci.skin == null && !model_name.equalsIgnoreCase("male")) {
                // change model to male
                model_name = "male";
                model_filename = "players/male/tris.md2";

                // see if the skin exists for the male model
                skin_filename = "players/" + model_name + "/" + skin_name
                        + ".pcx";
                ci.skin = Globals.re.RegisterSkin(skin_filename);
            }

            // if we still don't have a skin, it means that the male model
            // didn't have
            // it, so default to grunt
            if (ci.skin == null) {
                // see if the skin exists for the male model
                skin_filename = "players/" + model_name + "/grunt.pcx";
                ci.skin = Globals.re.RegisterSkin(skin_filename);
            }

            // weapon file
            num_weapon_filenames = ClientView.num_cl_weaponmodels;
            for (i = 0; i < ClientView.num_cl_weaponmodels; i++) {
                weapon_filenames[i] = "players/" + model_name + "/"
                        + ClientView.cl_weaponmodels[i];

// TODO(jgw): move this failover code into loadWeaponModel() somehow.
//                if (null == ci.weaponmodel[i] && model_name.equals("cyborg")) {
//                    // try male
//                    weapon_filename = "players/male/"
//                            + CL_view.cl_weaponmodels[i];
//                    ci.weaponmodel[i] = Globals.re
//                            .RegisterModel(weapon_filename);
//                }

                if (0 == Globals.cl_vwep.value)
                    break; // only one when vwep is off
            }

            // icon file
            ci.iconname = "/players/" + model_name + "/" + skin_name + "_i.pcx";
            ci.icon = Globals.re.RegisterPic(ci.iconname);
        }

        // Load player model and weapon models.
        Globals.re.RegisterModel(model_filename, new AsyncCallback<Model>() {
          public void onSuccess(Model response) {
            ci.model = response;
          }

          public void onFailure(Throwable e) {
            // TODO(jgw): fail?
          }
        });

        ci.weaponmodel = new Model[Constants.MAX_CLIENTWEAPONMODELS];
        for (i = 0; i < num_weapon_filenames; ++i) {
          loadWeaponModel(ci, i, weapon_filenames[i]);
        }

        // must have loaded all data types to be valud
/*        if (ci.skin == null || ci.icon == null || ci.model == null
                || ci.weaponmodel[0] == null) {
            ci.skin = null;
            ci.icon = null;
            ci.model = null;
            ci.weaponmodel[0] = null;
            return;
        }*/
    }

    private static void loadWeaponModel(final ClientInfo ci, final int i, String weapon_filename) {
      Globals.re.RegisterModel(weapon_filename, new AsyncCallback<Model>() {
        public void onSuccess(Model response) {
          ci.weaponmodel[i] = response;
        }
        
        public void onFailure(Throwable e) {
          // TODO(jgw): fail?
        }
      });
    }

    /*
     * ================ CL_ParseClientinfo
     * 
     * Load the skin, icon, and model for a client ================
     */
    public static void ParseClientinfo(int player) {
        String s = Globals.cl.configstrings[player + Constants.CS_PLAYERSKINS];

        ClientInfo ci = Globals.cl.clientinfo[player];

        LoadClientinfo(ci, s);
    }

    /*
     * ================ CL_ParseConfigString ================
     */
    public static void ParseConfigString() {
        int i = Globals.net_message.getShort();

        if (i < 0 || i >= Constants.MAX_CONFIGSTRINGS)
            Com.Error(Constants.ERR_DROP, "configstring > MAX_CONFIGSTRINGS");

        String s = Buffers.getString(Globals.net_message);

        String olds = Globals.cl.configstrings[i];
        Globals.cl.configstrings[i] = s;
        
        //Com.dprintln("ParseConfigString(): configstring[" + i + "]=<"+s+">");

        // do something apropriate

        if (i >= Constants.CS_LIGHTS
                && i < Constants.CS_LIGHTS + Constants.MAX_LIGHTSTYLES) {
            ClientEffects.SetLightstyle(i - Constants.CS_LIGHTS);
        } else if (i >= Constants.CS_MODELS && i < Constants.CS_MODELS + Constants.MAX_MODELS) {
            if (Globals.cl.refresh_prepped) {
                // TODO(jgw): It looks to be oimpossible that an inline bsp model ("*1", etc)
                // could show up here referencing anything except the main bsp, which should
                // always be loaded by this point. If not, we should see a failure in
                // refexport_t.RegisterModel() or CM.InlineModel().
                final int model_draw_index = i;
                Globals.re.RegisterModel(Globals.cl.configstrings[i], new AsyncCallback<Model>() {
                  public void onSuccess(Model response) {
                    Globals.cl.model_draw[model_draw_index - Constants.CS_MODELS] = response;
                  }

                  public void onFailure(Throwable e) {
                    // TODO(jgw): fail?
                  }
                });

                if (Globals.cl.configstrings[i].startsWith("*"))
                    Globals.cl.model_clip[i - Constants.CS_MODELS] = CM.InlineModel(Globals.cl.configstrings[i]);
                else
                    Globals.cl.model_clip[i - Constants.CS_MODELS] = null;
            }
        } else if (i >= Constants.CS_SOUNDS
                && i < Constants.CS_SOUNDS + Constants.MAX_MODELS) {
//            if (Globals.cl.refresh_prepped)
                Globals.cl.sound_precache[i - Constants.CS_SOUNDS] = Sound
                        .RegisterSound(Globals.cl.configstrings[i]);
        } else if (i >= Constants.CS_IMAGES
                && i < Constants.CS_IMAGES + Constants.MAX_MODELS) {
            if (Globals.cl.refresh_prepped)
                Globals.cl.image_precache[i - Constants.CS_IMAGES] = Globals.re
                        .RegisterPic(Globals.cl.configstrings[i]);
        } else if (i >= Constants.CS_PLAYERSKINS
                && i < Constants.CS_PLAYERSKINS + Constants.MAX_CLIENTS) {
            if (Globals.cl.refresh_prepped && !olds.equals(s))
                ParseClientinfo(i - Constants.CS_PLAYERSKINS);
        }
    }

    /*
     * =====================================================================
     * 
     * ACTION MESSAGES
     * 
     * =====================================================================
     */

    private static final float[] pos_v = { 0, 0, 0 };
    /*
     * ================== CL_ParseStartSoundPacket ==================
     */
    public static void ParseStartSoundPacket() {
        int flags = Buffers.readUnsignedByte(Globals.net_message);
        int sound_num = Buffers.readUnsignedByte(Globals.net_message);

        float volume;
        if ((flags & Constants.SND_VOLUME) != 0)
            volume = Buffers.readUnsignedByte(Globals.net_message) / 255.0f;
        else
            volume = Constants.DEFAULT_SOUND_PACKET_VOLUME;

        float attenuation;
        if ((flags & Constants.SND_ATTENUATION) != 0)
            attenuation = Buffers.readUnsignedByte(Globals.net_message) / 64.0f;
        else
            attenuation = Constants.DEFAULT_SOUND_PACKET_ATTENUATION;

        float ofs;
        if ((flags & Constants.SND_OFFSET) != 0)
            ofs = Buffers.readUnsignedByte(Globals.net_message) / 1000.0f;
        else
            ofs = 0;

        int channel;
        int ent;
        if ((flags & Constants.SND_ENT) != 0) { // entity reletive
            channel = Globals.net_message.getShort();
            ent = channel >> 3;
            if (ent > Constants.MAX_EDICTS)
                Com.Error(Constants.ERR_DROP, "CL_ParseStartSoundPacket: ent = "
                        + ent);

            channel &= 7;
        } else {
            ent = 0;
            channel = 0;
        }

        float pos[];
        if ((flags & Constants.SND_POS) != 0) { // positioned in space
            Buffers.getPos(Globals.net_message, pos_v);
            // is ok. sound driver copies
            pos = pos_v;
        } else
            // use entity number
            pos = null;

        if (null == Globals.cl.sound_precache[sound_num])
            return;

        Sound.StartSound(pos, ent, channel, Globals.cl.sound_precache[sound_num],
                volume, attenuation, ofs);
    }

    public static void SHOWNET(String s) {
        if (Globals.cl_shownet.value >= 2)
            Com.Printf(Globals.net_message.readcount - 1 + ":" + s + "\n");
    }

    /*
     * ===================== CL_ParseServerMessage =====================
     */
    public static void ParseServerMessage() {
        //
        //	   if recording demos, copy the message out
        //
        //if (cl_shownet.value == 1)
        //Com.Printf(net_message.cursize + " ");
        //else if (cl_shownet.value >= 2)
        //Com.Printf("------------------\n");

        //
        //	   parse the message
        //
        while (true) {
            if (Globals.net_message.readcount > Globals.net_message.cursize) {
                Com.Error(Constants.ERR_FATAL,
                        "CL_ParseServerMessage: Bad server message:");
                break;
            }

            int cmd = Buffers.readUnsignedByte(Globals.net_message);

            if (cmd == -1) {
                SHOWNET("END OF MESSAGE");
                break;
            }

            if (Globals.cl_shownet.value >= 2) {
                if (null == svc_strings[cmd])
                    Com.Printf(Globals.net_message.readcount - 1 + ":BAD CMD "
                            + cmd + "\n");
                else
                    SHOWNET(svc_strings[cmd]);
            }

            // other commands
            switch (cmd) {
            default:
                Com.Error(Constants.ERR_DROP,
                        "CL_ParseServerMessage: Illegible server message\n");
                break;

            case Constants.svc_nop:
                //				Com.Printf ("svc_nop\n");
                break;

            case Constants.svc_disconnect:
                Com.Error(Constants.ERR_DISCONNECT, "Server disconnected\n");
                break;

            case Constants.svc_reconnect:
                Com.Printf("Server disconnected, reconnecting\n");
                if (Globals.cls.download != null) {
                    //ZOID, close download
                    try {
                        Globals.cls.download.close();
                    } catch (IOException e) {
                    }
                    Globals.cls.download = null;
                }
                Globals.cls.state = Constants.ca_connecting;
                Globals.cls.connect_time = -99999; // CL_CheckForResend() will
                // fire immediately
                break;

            case Constants.svc_print:
                int i = Buffers.readUnsignedByte(Globals.net_message);
                if (i == Constants.PRINT_CHAT) {
                    Sound.StartLocalSound("misc/talk.wav");
                    Globals.con.ormask = 128;
                }
                String msgText = Buffers.getString(Globals.net_message);
                Com.Printf(msgText);
                //WebIntegration.onNetMessage(msgText);
                Globals.con.ormask = 0;
                break;

            case Constants.svc_centerprint:
                Screen.CenterPrint(Buffers.getString(Globals.net_message));
                break;

            case Constants.svc_stufftext:
                String s = Buffers.getString(Globals.net_message);
                Com.DPrintf("stufftext: " + s + "\n");
                CommandBuffer.AddText(s);
                break;

            case Constants.svc_serverdata:
                CommandBuffer.Execute(); // make sure any stuffed commands are done
                ParseServerData();
                break;

            case Constants.svc_configstring:
                ParseConfigString();
                break;

            case Constants.svc_sound:
                ParseStartSoundPacket();
                break;

            case Constants.svc_spawnbaseline:
                ParseBaseline();
                break;

            case Constants.svc_temp_entity:
                ClientTent.ParseTEnt();
                break;

            case Constants.svc_muzzleflash:
                ClientEffects.ParseMuzzleFlash();
                break;

            case Constants.svc_muzzleflash2:
                ClientEffects.ParseMuzzleFlash2();
                break;

            case Constants.svc_download:
                // TODO(jgw): Ditch downloads entirely.
                throw new RuntimeException("SVC_DOWNLOAD");
              

            case Constants.svc_frame:
                ClientEntities.ParseFrame();
                break;

            case Constants.svc_inventory:
                ClientInventory.ParseInventory();
                break;

            case Constants.svc_layout:
        	Globals.cl.layout = Buffers.getString(Globals.net_message);
                break;

            case Constants.svc_playerinfo:
            case Constants.svc_packetentities:
            case Constants.svc_deltapacketentities:
                Com.Error(Constants.ERR_DROP, "Out of place frame data");
                break;
            }
        }

        ClientView.AddNetgraph();

        //
        // we don't know if it is ok to save a demo message until
        // after we have parsed the frame
        //
        if (Globals.cls.demorecording && !Globals.cls.demowaiting)
            Client.writeDemoMessage();
    }
}

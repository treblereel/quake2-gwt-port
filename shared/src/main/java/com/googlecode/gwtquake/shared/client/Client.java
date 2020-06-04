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
import java.nio.ByteOrder;

import com.googlecode.gwtquake.shared.common.*;
import com.googlecode.gwtquake.shared.game.*;
import com.googlecode.gwtquake.shared.server.ServerMain;
import com.googlecode.gwtquake.shared.sound.Sound;
import com.googlecode.gwtquake.shared.sys.*;
import com.googlecode.gwtquake.shared.util.*;
import elemental2.dom.DomGlobal;

/**
 * CL
 */
public final class Client {
  public static final int PLAYER_MULT = 5;
  
  //       ENV_CNT is map load, ENV_CNT+1 is first env map
  public static final int ENV_CNT = 
    (Constants.CS_PLAYERSKINS + Constants.MAX_CLIENTS * Client.PLAYER_MULT);

  public static final int TEXTURE_CNT = (ENV_CNT + 13);

  
  static int precache_check; // for autodownload of precache items
  static int precache_spawncount;
  static int precache_tex;
  static int precache_model_skin;
  static byte precache_model[]; // used for skin checking in alias models

  public static class CheatVar {
    String name;
    String value;
    ConsoleVariable var;
  }

  public static String cheatvarsinfo[][] = { 
    {"timescale", "1" },
    {"timedemo", "0" },
    {"r_drawworld", "1" },
    {"cl_testlights", "0" },
    {"r_fullbright", "0" },
    {"r_drawflat", "0" }, 
    {"paused", "0" }, 
    {"fixedtime", "0" },
    {"sw_draworder", "0" }, 
    {"gl_lightmap", "0" },
    {"gl_saturatelighting", "0" }, 
    {null, null}
  };

  public static CheatVar cheatvars[];
  static int numcheatvars;

  static {
    cheatvars = new CheatVar[cheatvarsinfo.length];
    for (int n = 0; n < cheatvarsinfo.length; n++) {
      cheatvars[n] = new CheatVar();
      cheatvars[n].name = cheatvarsinfo[n][0];
      cheatvars[n].value = cheatvarsinfo[n][1];
    }
  }

  /**
   * Stop_f
   * 
   * Stop recording a demo.
   */
  static ExecutableCommand stopCommand = new ExecutableCommand() {
    public void execute() {
      try {

        int len;

        if (!Globals.cls.demorecording) {
          Com.Printf("Not recording a demo.\n");
          return;
        }

        //	   finish up
        len = -1;
        Globals.cls.demofile.writeInt(EndianHandler.swapInt(len));
        Globals.cls.demofile.close();
        Globals.cls.demofile = null;
        Globals.cls.demorecording = false;
        Com.Printf("Stopped demo.\n");
      } catch (IOException e) {
      }
    }
  };

  static EntityState nullstate = new EntityState(null);

  /**
   * Record_f
   * 
   * record &lt;demoname&gt;
   * Begins recording a demo from the current position.
   */
  static ExecutableCommand recordCommand = new ExecutableCommand() {
    public void execute() {
      try {
        String name;
        int i;
        EntityState ent;

        if (Commands.Argc() != 2) {
          Com.Printf("record <demoname>\n");
          return;
        }

        if (Globals.cls.demorecording) {
          Com.Printf("Already recording.\n");
          return;
        }

        if (Globals.cls.state != Constants.ca_active) {
          Com.Printf("You must be in a level to record.\n");
          return;
        }

        //
        // open the demo file
        //
        name = QuakeFileSystem.Gamedir() + "/demos/" + Commands.Argv(1) + ".dm2";

        Com.Printf("recording to " + name + ".\n");
        QuakeFileSystem.CreatePath(name);
        Globals.cls.demofile = new RandomAccessFile(name, "rw");
        if (Globals.cls.demofile == null) {
          Com.Printf("ERROR: couldn't open.\n");
          return;
        }
        Globals.cls.demorecording = true;

        // don't start saving messages until a non-delta compressed
        // message is received
        Globals.cls.demowaiting = true;

        //
        // write out messages to hold the startup information
        //
        Buffer buf = Buffer.allocate(Constants.MAX_MSGLEN);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        // send the serverdata
        Buffers.writeByte(buf, Constants.svc_serverdata);
        buf.putInt(Constants.PROTOCOL_VERSION);
        buf.putInt(0x10000 + Globals.cl.servercount);
        Buffers.writeByte(buf, 1); // demos are always attract loops
        Buffers.WriteString(buf, Globals.cl.gamedir);
        buf.WriteShort(Globals.cl.playernum);

        Buffers.WriteString(buf, Globals.cl.configstrings[Constants.CS_NAME]);

        // configstrings
        for (i = 0; i < Constants.MAX_CONFIGSTRINGS; i++) {
          if (Globals.cl.configstrings[i].length() > 0) {
            if (buf.cursize + Globals.cl.configstrings[i].length()
                + 32 > buf.maxsize) { 
              // write it out
              Globals.cls.demofile.writeInt(EndianHandler.swapInt(buf.cursize));
              Globals.cls.demofile
              .write(buf.data, 0, buf.cursize);
              buf.cursize = 0;
            }

            Buffers.writeByte(buf, Constants.svc_configstring);
            buf.WriteShort(i);
            Buffers.WriteString(buf, Globals.cl.configstrings[i]);
          }

        }

        // baselines
        nullstate.clear();
        for (i = 0; i < Constants.MAX_EDICTS; i++) {
          ent = Globals.cl_entities[i].baseline;
          if (ent.modelindex == 0)
            continue;

          if (buf.cursize + 64 > buf.maxsize) { // write it out
            Globals.cls.demofile.writeInt(EndianHandler.swapInt(buf.cursize));
            Globals.cls.demofile.write(buf.data, 0, buf.cursize);
            buf.cursize = 0;
          }

          Buffers.writeByte(buf, Constants.svc_spawnbaseline);
          Delta.WriteDeltaEntity(nullstate,
              Globals.cl_entities[i].baseline, buf, true, true);
        }

        Buffers.writeByte(buf, Constants.svc_stufftext);
        Buffers.WriteString(buf, "precache\n");

        // write it to the demo file
        Globals.cls.demofile.writeInt(EndianHandler.swapInt(buf.cursize));
        Globals.cls.demofile.write(buf.data, 0, buf.cursize);
        // the rest of the demo file will be individual frames

      } catch (IOException e) {
      }
    }
  };

  /**
   * ForwardToServer_f
   */
  static ExecutableCommand forwardToServerCommand = new ExecutableCommand() {
    public void execute() {
      if (Globals.cls.state != Constants.ca_connected
          && Globals.cls.state != Constants.ca_active) {
        Com.Printf("Can't \"" + Commands.Argv(0) + "\", not connected\n");
        return;
      }

      // don't forward the first argument
      if (Commands.Argc() > 1) {
        Buffers.writeByte(Globals.cls.netchan.message,
            Constants.clc_stringcmd);
        Buffers.Print(Globals.cls.netchan.message, Commands.Args());
      }
    }
  };

  /**
   * Pause_f
   */
  static ExecutableCommand pauseCommand = new ExecutableCommand() {
    public void execute() {
      // never pause in multiplayer

      if (ConsoleVariables.VariableValue("maxclients") > 1
          || Globals.server_state == 0) {
        ConsoleVariables.SetValue("paused", 0);
        return;
      }

      ConsoleVariables.SetValue("paused", Globals.cl_paused.value);
    }
  };

  /**
   * Quit_f
   */
  static ExecutableCommand quitCommand = new ExecutableCommand() {
    public void execute() {
      disconnect();
      Com.Quit();
    }
  };

  /**
   * Connect_f
   */
  static ExecutableCommand connectCommand = new ExecutableCommand() {
    public void execute() {
      String server;

      if (Commands.Argc() != 2) {
        Com.Printf("usage: connect <server>\n");
        return;
      }

      if (Globals.server_state != 0) {
        // if running a local server, kill it and reissue
        ServerMain.SV_Shutdown("Server quit\n", false);
      } else {
        disconnect();
      }

      server = Commands.Argv(1);

      NET.Config(true); // allow remote

      disconnect();

      Globals.cls.state = Constants.ca_connecting;
      //strncpy (cls.servername, server, sizeof(cls.servername)-1);
      Globals.cls.servername = server;
      Globals.cls.connect_time = -99999;
      // CL_CheckForResend() will fire immediately
    }
  };

  /**
   * Rcon_f
   * 
   * Send the rest of the command line over as an unconnected command.
   */
  static ExecutableCommand rconCommand = new ExecutableCommand() {
    public void execute() {

      if (Globals.rcon_client_password.string.length() == 0) {
        Com.Printf("You must set 'rcon_password' before\nissuing an rcon command.\n");
        return;
      }

      StringBuffer message = new StringBuffer(1024);

      // connection less packet
      message.append('\u00ff');
      message.append('\u00ff');
      message.append('\u00ff');
      message.append('\u00ff');

      // allow remote
      NET.Config(true);

      message.append("rcon ");
      message.append(Globals.rcon_client_password.string);
      message.append(" ");

      for (int i = 1; i < Commands.Argc(); i++) {
        message.append(Commands.Argv(i));
        message.append(" ");
      }

      NetworkAddress to = new NetworkAddress();

      if (Globals.cls.state >= Constants.ca_connected)
        to = Globals.cls.netchan.remote_address;
      else {
        if (Globals.rcon_address.string.length() == 0) {
          Com.Printf("You must either be connected,\nor set the 'rcon_address' cvar\nto issue rcon commands\n");
          return;
        }
        NET.StringToAdr(Globals.rcon_address.string, to);
        if (to.port == 0) to.port = Constants.PORT_SERVER;
      }
      message.append('\0');
      String b = message.toString();
      NET.SendPacket(Constants.NS_CLIENT, b.length(), Lib.stringToBytes(b), to);
    }
  };

  static ExecutableCommand disconnectCommand = new ExecutableCommand() {
    public void execute() {
      Com.Error(Constants.ERR_DROP, "Disconnected from server");
    }
  };

  /**
   * Changing_f
   * 
   * Just sent as a hint to the client that they should drop to full console.
   */
  static ExecutableCommand changingCommand = new ExecutableCommand() {
    public void execute() {
      //ZOID
      //if we are downloading, we don't change!
      // This so we don't suddenly stop downloading a map

      if (Globals.cls.download != null)
        return;

      Screen.BeginLoadingPlaque();
      Globals.cls.state = Constants.ca_connected; // not active anymore, but
      // not disconnected
      Com.Printf("\nChanging map...\n");
    }
  };

  /**
   * Reconnect_f
   * 
   * The server is changing levels.
   */
  static ExecutableCommand reconnectCommand = new ExecutableCommand() {
    public void execute() {
      //ZOID
      //if we are downloading, we don't change! This so we don't suddenly
      // stop downloading a map
      if (Globals.cls.download != null)
        return;

      Sound.StopAllSounds();
      if (Globals.cls.state == Constants.ca_connected) {
        Com.Printf("reconnecting...\n");
        Globals.cls.state = Constants.ca_connected;
        Buffers.writeByte(Globals.cls.netchan.message, Constants.clc_stringcmd);
        Buffers.WriteString(Globals.cls.netchan.message, "new");
        return;
      }

      if (Globals.cls.servername != null) {
        if (Globals.cls.state >= Constants.ca_connected) {
          disconnect();
          Globals.cls.connect_time = Globals.cls.realtime - 1500;
        } else
          Globals.cls.connect_time = -99999; // fire immediately

          Globals.cls.state = Constants.ca_connecting;
          Com.Printf("reconnecting...\n");
      }
    }
  };

  /**
   * PingServers_f
   */
  static ExecutableCommand pingServersCommand = new ExecutableCommand() {
    public void execute() {
      int i;
      NetworkAddress adr = new NetworkAddress();
      //char name[32];
      String name;
      String adrstring;
      ConsoleVariable noudp;
      ConsoleVariable noipx;

      NET.Config(true); // allow remote

      // send a broadcast packet
      Com.Printf("pinging broadcast...\n");

      noudp = ConsoleVariables.Get("noudp", "0", Constants.CVAR_NOSET);
      if (noudp.value == 0.0f) {
        adr.type = Constants.NA_BROADCAST;
        adr.port = Constants.PORT_SERVER;
        //adr.port = BigShort(PORT_SERVER);
        NetworkChannel.OutOfBandPrint(Constants.NS_CLIENT, adr, "info "
            + Constants.PROTOCOL_VERSION);
      }

      // we use no IPX
      noipx = ConsoleVariables.Get("noipx", "1", Constants.CVAR_NOSET);
      if (noipx.value == 0.0f) {
        adr.type = Constants.NA_BROADCAST_IPX;
        //adr.port = BigShort(PORT_SERVER);
        adr.port = Constants.PORT_SERVER;
        NetworkChannel.OutOfBandPrint(Constants.NS_CLIENT, adr, "info "
            + Constants.PROTOCOL_VERSION);
      }

      // send a packet to each address book entry
      for (i = 0; i < 16; i++) {
        //Com_sprintf (name, sizeof(name), "adr%i", i);
        name = "adr" + i;
        adrstring = ConsoleVariables.VariableString(name);
        if (adrstring == null || adrstring.length() == 0)
          continue;

        Com.Printf("pinging " + adrstring + "...\n");
        if (!NET.StringToAdr(adrstring, adr)) {
          Com.Printf("Bad address: " + adrstring + "\n");
          continue;
        }
        if (adr.port == 0)
          //adr.port = BigShort(PORT_SERVER);
          adr.port = Constants.PORT_SERVER;
        NetworkChannel.OutOfBandPrint(Constants.NS_CLIENT, adr, "info "
            + Constants.PROTOCOL_VERSION);
      }
    }
  };

  /**
   * Skins_f
   * 
   * Load or download any custom player skins and models.
   */
  static ExecutableCommand skinsCommand = new ExecutableCommand() {
    public void execute() {
      int i;

      for (i = 0; i < Constants.MAX_CLIENTS; i++) {
        if (Globals.cl.configstrings[Constants.CS_PLAYERSKINS + i] == null)
          continue;
        Com.Printf("client " + i + ": "
            + Globals.cl.configstrings[Constants.CS_PLAYERSKINS + i]
                                       + "\n");
        Screen.UpdateScreen();
        Sys.SendKeyEvents(); // pump message loop
        ClientParser.ParseClientinfo(i);
      }
    }
  };

  /**
   * Userinfo_f
   */
  static ExecutableCommand userinfoCommand = new ExecutableCommand() {
    public void execute() {
      Com.Printf("User info settings:\n");
      Info.Print(ConsoleVariables.Userinfo());
    }
  };

  /**
   * Snd_Restart_f
   * 
   * Restart the sound subsystem so it can pick up new parameters and flush
   * all sounds.
   */
  static ExecutableCommand sndRestartCommand = new ExecutableCommand() {
    public void execute() {
      Sound.Shutdown();
      Sound.Init();
      ClientParser.RegisterSounds();
    }
  };


  static String env_suf[] = { "rt", "bk", "lf", "ft", "up", "dn" };

  /**
   * The server will send this command right before allowing the client into
   * the server.
   */
  static ExecutableCommand precacheCommand = new ExecutableCommand() {
    public void execute() {
      // Yet another hack to let old demos work the old precache sequence.
      if (Commands.Argc() < 2) {

        int iw[] = { 0 }; // for detecting cheater maps

        CM.CM_LoadMap(Globals.cl.configstrings[Constants.CS_MODELS + 1],
            true, iw);

        ClientParser.RegisterSounds();
        ClientView.PrepRefresh();
        return;
      }

      Client.precache_check = Constants.CS_MODELS;
      Client.precache_spawncount = Lib.atoi(Commands.Argv(1));
      Client.precache_model = null;
      Client.precache_model_skin = 0;

      requestNextDownload();
    }
  };

  private static int extratime;

  //	  ============================================================================

  /**
   * Shutdown
   * 
   * FIXME: this is a callback from Sys_Quit and Com_Error. It would be better
   * to run quit through here before the final handoff to the sys code.
   */
  static boolean isdown = false;

  /**
   * WriteDemoMessage
   * 
   * Dumps the current net message, prefixed by the length
   */
  static void writeDemoMessage() {
    int swlen;

    // the first eight bytes are just packet sequencing stuff
    swlen = Globals.net_message.cursize - 8;

    try {
      Globals.cls.demofile.writeInt(EndianHandler.swapInt(swlen));
      Globals.cls.demofile.write(Globals.net_message.data, 8, swlen);
    } catch (IOException e) {
    }

  }

  /**
   * SendConnectPacket
   * 
   * We have gotten a challenge from the server, so try and connect.
   */
  static void sendConnectPacket() {
    NetworkAddress adr = new NetworkAddress();
    int port;

    if (!NET.StringToAdr(Globals.cls.servername, adr)) {
      Com.Printf("Bad server address\n");
      Globals.cls.connect_time = 0;
      return;
    }
    if (adr.port == 0)
      adr.port = Constants.PORT_SERVER;
    //			adr.port = BigShort(PORT_SERVER);

    port = (int) ConsoleVariables.VariableValue("qport");
    Globals.userinfo_modified = false;

    NetworkChannel.OutOfBandPrint(Constants.NS_CLIENT, adr, "connect "
        + Constants.PROTOCOL_VERSION + " " + port + " "
        + Globals.cls.challenge + " \"" + ConsoleVariables.Userinfo() + "\"\n");
  }

  /**
   * CheckForResend
   * 
   * Resend a connect message if the last one has timed out.
   */
  static void checkForResend() {
    NetworkAddress adr = new NetworkAddress();

    // if the local server is running and we aren't
    // then connect
    if (Globals.cls.state == Constants.ca_disconnected
        && Globals.server_state != 0) {
      Globals.cls.state = Constants.ca_connecting;
      Globals.cls.servername = "localhost";
      // we don't need a challenge on the localhost
      sendConnectPacket();
      return;
    }

    // resend if we haven't gotten a reply yet
    if (Globals.cls.state != Constants.ca_connecting)
      return;

    if (Globals.cls.realtime - Globals.cls.connect_time < 3000)
      return;

    if (!NET.StringToAdr(Globals.cls.servername, adr)) {
      Com.Printf("Bad server address\n");
      Globals.cls.state = Constants.ca_disconnected;
      return;
    }
    if (adr.port == 0)
      adr.port = Constants.PORT_SERVER;

    // for retransmit requests
    Globals.cls.connect_time = Globals.cls.realtime;

    Com.Printf("Connecting to " + Globals.cls.servername + "...\n");

    NetworkChannel.OutOfBandPrint(Constants.NS_CLIENT, adr, "getchallenge\n");
  }

  /**
   * ClearState
   * 
   */
  static void clearState() {
    Sound.StopAllSounds();
    ClientEffects.ClearEffects();
    ClientTent.ClearTEnts();

    // wipe the entire cl structure

    Globals.cl = new ClientState();
    for (int i = 0; i < Globals.cl_entities.length; i++) {
      Globals.cl_entities[i] = new ClientEntity();
    }

    Globals.cls.netchan.message.clear();
  }

  /**
   * Disconnect
   * 
   * Goes from a connected state to full screen console state Sends a
   * disconnect message to the server This is also called on Com_Error, so it
   * shouldn't cause any errors.
   */
  static void disconnect() {

    String fin;

    if (Globals.cls.state == Constants.ca_disconnected)
      return;

    if (Globals.cl_timedemo != null && Globals.cl_timedemo.value != 0.0f) {
      int time;

      time = (int) (Timer.Milliseconds() - Globals.cl.timedemo_start);
      if (time > 0)
        Com.Printf("%i frames, %3.1f seconds: %3.1f fps\n",
            new Vargs(3).add(Globals.cl.timedemo_frames).add(
                time / 1000.0).add(
                    Globals.cl.timedemo_frames * 1000.0 / time));
    }

    Math3D.VectorClear(Globals.cl.refdef.blend);

    Globals.re.CinematicSetPalette(null);

    Menu.ForceMenuOff();

    Globals.cls.connect_time = 0;

    Screen.StopCinematic();

    if (Globals.cls.demorecording)
      stopCommand.execute();

    // send a disconnect message to the server
    fin = (char) Constants.clc_stringcmd + "disconnect";
    NetworkChannel.Transmit(Globals.cls.netchan, fin.length(), Lib.stringToBytes(fin));
    NetworkChannel.Transmit(Globals.cls.netchan, fin.length(), Lib.stringToBytes(fin));
    NetworkChannel.Transmit(Globals.cls.netchan, fin.length(), Lib.stringToBytes(fin));

    clearState();

    // stop download
    if (Globals.cls.download != null) {
      Lib.fclose(Globals.cls.download);
      Globals.cls.download = null;
    }

    Globals.cls.state = Constants.ca_disconnected;
  }

  /**
   * ParseStatusMessage
   * 
   * Handle a reply from a ping.
   */
  static void parseStatusMessage() {
    String s;

    s = Buffers.getString(Globals.net_message);

    Com.Printf(s + "\n");
    Menu.AddToServerList(Globals.net_from, s);
  }

  /**
   * ConnectionlessPacket
   * 
   * Responses to broadcasts, etc
   */
  static void connectionlessPacket() {
    String s;
    String c;

    Globals.net_message.reset();
    Globals.net_message.getInt(); // skip the -1

    s = Buffers.getLine(Globals.net_message);

    Commands.TokenizeString(s.toCharArray(), false);

    c = Commands.Argv(0);

    Com.Println(Globals.net_from.toString() + ": " + c);

    // server connection
    if (c.equals("client_connect")) {
      if (Globals.cls.state == Constants.ca_connected) {
        Com.Printf("Dup connect received.  Ignored.\n");
        return;
      }
      NetworkChannel.Setup(Globals.cls.netchan, Constants.NS_CLIENT,
          Globals.net_from, Globals.cls.quakePort);
      Buffers.writeByte(Globals.cls.netchan.message, Constants.clc_stringcmd);
      Buffers.WriteString(Globals.cls.netchan.message, "new");
      Globals.cls.state = Constants.ca_connected;
      return;
    }

    // server responding to a status broadcast
    if (c.equals("info")) {
      parseStatusMessage();
      return;
    }

    // remote command from gui front end
    if (c.equals("cmd")) {
      if (!NET.IsLocalAddress(Globals.net_from)) {
        Com.Printf("Command packet from remote host.  Ignored.\n");
        return;
      }
      s = Buffers.getString(Globals.net_message);
      CommandBuffer.AddText(s);
      CommandBuffer.AddText("\n");
      return;
    }
    // print command from somewhere
    if (c.equals("print")) {
      s = Buffers.getString(Globals.net_message);
      if (s.length() > 0)
        Com.Printf(s);
      return;
    }

    // ping from somewhere
    if (c.equals("ping")) {
      NetworkChannel.OutOfBandPrint(Constants.NS_CLIENT, Globals.net_from, "ack");
      return;
    }

    // challenge from the server we are connecting to
    if (c.equals("challenge")) {
      Globals.cls.challenge = Lib.atoi(Commands.Argv(1));
      sendConnectPacket();
      return;
    }

    // echo request from server
    if (c.equals("echo")) {
      NetworkChannel.OutOfBandPrint(Constants.NS_CLIENT, Globals.net_from, Commands
          .Argv(1));
      return;
    }

    Com.Printf("Unknown command.\n");
  }


  /**
   * ReadPackets
   */
  static void readPackets() {
    while (NET.GetPacket(Constants.NS_CLIENT, Globals.net_from,
        Globals.net_message)) {

      //
      // remote command packet
      //		
      if (Globals.net_message.data[0] == -1
          && Globals.net_message.data[1] == -1
          && Globals.net_message.data[2] == -1
          && Globals.net_message.data[3] == -1) {
        //			if (*(int *)net_message.data == -1)
        connectionlessPacket();
        continue;
      }

      if (Globals.cls.state == Constants.ca_disconnected
          || Globals.cls.state == Constants.ca_connecting)
        continue; // dump it if not connected

      if (Globals.net_message.cursize < 8) {
        Com.Printf(NET.AdrToString(Globals.net_from)
            + ": Runt packet\n");
        continue;
      }

      //
      // packet from server
      //
      if (!NET.CompareAdr(Globals.net_from,
          Globals.cls.netchan.remote_address)) {
        Com.DPrintf(NET.AdrToString(Globals.net_from)
            + ":sequenced packet without connection\n");
        continue;
      }
      if (!NetworkChannel.Process(Globals.cls.netchan, Globals.net_message))
        continue; // wasn't accepted for some reason
      ClientParser.ParseServerMessage();
    }

    //
    // check timeout
    //
    if (Globals.cls.state >= Constants.ca_connected
        && Globals.cls.realtime - Globals.cls.netchan.last_received > Globals.cl_timeout.value * 1000) {
      if (++Globals.cl.timeoutcount > 5) // timeoutcount saves debugger
      {
        Com.Printf("\nServer connection timed out.\n");
        disconnect();
        return;
      }
    } else
      Globals.cl.timeoutcount = 0;
  }

  //	  =============================================================================

  /**
   * FixUpGender_f
   */
  static void fixUpGender() {

    String sk;

    if (Globals.gender_auto.value != 0.0f) {

      if (Globals.gender.modified) {
        // was set directly, don't override the user
        Globals.gender.modified = false;
        return;
      }

      sk = Globals.skin.string;
      if (sk.startsWith("male") || sk.startsWith("cyborg"))
        ConsoleVariables.Set("gender", "male");
      else if (sk.startsWith("female") || sk.startsWith("crackhor"))
        ConsoleVariables.Set("gender", "female");
      else
        ConsoleVariables.Set("gender", "none");
      Globals.gender.modified = false;
    }
  }

  public static void requestNextDownload() {
    int map_checksum = 0; // for detecting cheater maps
    //char fn[MAX_OSPATH];
    String fn;

    QuakeFiles.dmdl_t pheader;

    if (Globals.cls.state != Constants.ca_connected)
      return;

    if (ServerMain.allow_download.value == 0 && Client.precache_check < ENV_CNT)
      Client.precache_check = ENV_CNT;

    //	  ZOID
    if (Client.precache_check == Constants.CS_MODELS) { // confirm map
      Client.precache_check = Constants.CS_MODELS + 2; // 0 isn't used
      //            if (SV_MAIN.allow_download_maps.value != 0)
      //                if (!CL_parse
      //                        .CheckOrDownloadFile(Globals.cl.configstrings[Defines.CS_MODELS + 1]))
      //                    return; // started a download
    }
    if (Client.precache_check >= Constants.CS_MODELS
        && Client.precache_check < Constants.CS_MODELS + Constants.MAX_MODELS) {
      //            if (SV_MAIN.allow_download_models.value != 0) {
      //                while (CL.precache_check < Defines.CS_MODELS
      //                        + Defines.MAX_MODELS
      //                        && Globals.cl.configstrings[CL.precache_check].length() > 0) {
      //                    if (Globals.cl.configstrings[CL.precache_check].charAt(0) == '*'
      //                            || Globals.cl.configstrings[CL.precache_check]
      //                                    .charAt(0) == '#') {
      //                        CL.precache_check++;
      //                        continue;
      //                    }
      //                    if (CL.precache_model_skin == 0) {
      //                        if (!CL_parse
      //                                .CheckOrDownloadFile(Globals.cl.configstrings[CL.precache_check])) {
      //                            CL.precache_model_skin = 1;
      //                            return; // started a download
      //                        }
      //                        CL.precache_model_skin = 1;
      //                    }
      //
      //                    // checking for skins in the model
      //                    if (CL.precache_model == null) {
      //
      //                        CL.precache_model = FS
      //                                .LoadFile(Globals.cl.configstrings[CL.precache_check]);
      //                        if (CL.precache_model == null) {
      //                            CL.precache_model_skin = 0;
      //                            CL.precache_check++;
      //                            continue; // couldn't load it
      //                        }
      //                        ByteBuffer bb = ByteBuffer.wrap(CL.precache_model);
      //                        bb.order(ByteOrder.LITTLE_ENDIAN);
      //
      //                        int header = bb.getInt();
      //
      //                        if (header != qfiles.IDALIASHEADER) {
      //                            // not an alias model
      //                            FS.FreeFile(CL.precache_model);
      //                            CL.precache_model = null;
      //                            CL.precache_model_skin = 0;
      //                            CL.precache_check++;
      //                            continue;
      //                        }
      //                        pheader = new qfiles.dmdl_t(ByteBuffer.wrap(
      //                                CL.precache_model).order(
      //                                ByteOrder.LITTLE_ENDIAN));
      //                        if (pheader.version != Defines.ALIAS_VERSION) {
      //                            CL.precache_check++;
      //                            CL.precache_model_skin = 0;
      //                            continue; // couldn't load it
      //                        }
      //                    }
      //
      //                    pheader = new qfiles.dmdl_t(ByteBuffer.wrap(
      //                            CL.precache_model).order(ByteOrder.LITTLE_ENDIAN));
      //
      //                    int num_skins = pheader.num_skins;
      //
      //                    while (CL.precache_model_skin - 1 < num_skins) {
      //                        //Com.Printf("critical code section because of endian
      //                        // mess!\n");
      //
      //                        String name = Lib.CtoJava(CL.precache_model,
      //                                pheader.ofs_skins
      //                                        + (CL.precache_model_skin - 1)
      //                                        * Defines.MAX_SKINNAME,
      //                                Defines.MAX_SKINNAME * num_skins);
      //
      //                        if (!CL_parse.CheckOrDownloadFile(name)) {
      //                            CL.precache_model_skin++;
      //                            return; // started a download
      //                        }
      //                        CL.precache_model_skin++;
      //                    }
      //                    if (CL.precache_model != null) {
      //                        FS.FreeFile(CL.precache_model);
      //                        CL.precache_model = null;
      //                    }
      //                    CL.precache_model_skin = 0;
      //                    CL.precache_check++;
      //                }
      //            }
      Client.precache_check = Constants.CS_SOUNDS;
    }
    if (Client.precache_check >= Constants.CS_SOUNDS
        && Client.precache_check < Constants.CS_SOUNDS + Constants.MAX_SOUNDS) {
      //            if (SV_MAIN.allow_download_sounds.value != 0) {
      //                if (CL.precache_check == Defines.CS_SOUNDS)
      //                    CL.precache_check++; // zero is blank
      //                while (CL.precache_check < Defines.CS_SOUNDS
      //                        + Defines.MAX_SOUNDS
      //                        && Globals.cl.configstrings[CL.precache_check].length() > 0) {
      //                    if (Globals.cl.configstrings[CL.precache_check].charAt(0) == '*') {
      //                        CL.precache_check++;
      //                        continue;
      //                    }
      //                    fn = "sound/"
      //                            + Globals.cl.configstrings[CL.precache_check++];
      //                    if (!CL_parse.CheckOrDownloadFile(fn))
      //                        return; // started a download
      //                }
      //            }
      Client.precache_check = Constants.CS_IMAGES;
    }
    if (Client.precache_check >= Constants.CS_IMAGES
        && Client.precache_check < Constants.CS_IMAGES + Constants.MAX_IMAGES) {
      if (Client.precache_check == Constants.CS_IMAGES)
        Client.precache_check++; // zero is blank
        //
      //            while (CL.precache_check < Defines.CS_IMAGES + Defines.MAX_IMAGES
      //                    && Globals.cl.configstrings[CL.precache_check].length() > 0) {
      //                fn = "pics/" + Globals.cl.configstrings[CL.precache_check++]
      //                        + ".pcx";
      //                if (!CL_parse.CheckOrDownloadFile(fn))
      //                    return; // started a download
      //            }
      Client.precache_check = Constants.CS_PLAYERSKINS;
    }
    // skins are special, since a player has three things to download:
    // model, weapon model and skin
    // so precache_check is now *3
    if (Client.precache_check >= Constants.CS_PLAYERSKINS
        && Client.precache_check < Constants.CS_PLAYERSKINS
        + Constants.MAX_CLIENTS * Client.PLAYER_MULT) {
      //            if (SV_MAIN.allow_download_players.value != 0) {
      //                while (CL.precache_check < Defines.CS_PLAYERSKINS
      //                        + Defines.MAX_CLIENTS * CL.PLAYER_MULT) {
      //
      //                    int i, n;
      //                    //char model[MAX_QPATH], skin[MAX_QPATH], * p;
      //                    String model, skin;
      //
      //                    i = (CL.precache_check - Defines.CS_PLAYERSKINS)
      //                            / CL.PLAYER_MULT;
      //                    n = (CL.precache_check - Defines.CS_PLAYERSKINS)
      //                            % CL.PLAYER_MULT;
      //
      //                    if (Globals.cl.configstrings[Defines.CS_PLAYERSKINS + i]
      //                            .length() == 0) {
      //                        CL.precache_check = Defines.CS_PLAYERSKINS + (i + 1)
      //                                * CL.PLAYER_MULT;
      //                        continue;
      //                    }
      //
      //                    int pos = Globals.cl.configstrings[Defines.CS_PLAYERSKINS + i].indexOf('\\');
      //                    
      //                    if (pos != -1)
      //                        pos++;
      //                    else
      //                        pos = 0;
      //
      //                    int pos2 = Globals.cl.configstrings[Defines.CS_PLAYERSKINS + i].indexOf('\\', pos);
      //                    
      //                    if (pos2 == -1)
      //                        pos2 = Globals.cl.configstrings[Defines.CS_PLAYERSKINS + i].indexOf('/', pos);
      //                    
      //                    
      //                    model = Globals.cl.configstrings[Defines.CS_PLAYERSKINS + i]
      //                            .substring(pos, pos2);
      //                                        
      //                    skin = Globals.cl.configstrings[Defines.CS_PLAYERSKINS + i].substring(pos2 + 1);
      //                    
      //                    switch (n) {
      //                    case 0: // model
      //                        fn = "players/" + model + "/tris.md2";
      //                        if (!CL_parse.CheckOrDownloadFile(fn)) {
      //                            CL.precache_check = Defines.CS_PLAYERSKINS + i
      //                                    * CL.PLAYER_MULT + 1;
      //                            return; // started a download
      //                        }
      //                        n++;
      //                    /* FALL THROUGH */
      //
      //                    case 1: // weapon model
      //                        fn = "players/" + model + "/weapon.md2";
      //                        if (!CL_parse.CheckOrDownloadFile(fn)) {
      //                            CL.precache_check = Defines.CS_PLAYERSKINS + i
      //                                    * CL.PLAYER_MULT + 2;
      //                            return; // started a download
      //                        }
      //                        n++;
      //                    /* FALL THROUGH */
      //
      //                    case 2: // weapon skin
      //                        fn = "players/" + model + "/weapon.pcx";
      //                        if (!CL_parse.CheckOrDownloadFile(fn)) {
      //                            CL.precache_check = Defines.CS_PLAYERSKINS + i
      //                                    * CL.PLAYER_MULT + 3;
      //                            return; // started a download
      //                        }
      //                        n++;
      //                    /* FALL THROUGH */
      //
      //                    case 3: // skin
      //                        fn = "players/" + model + "/" + skin + ".pcx";
      //                        if (!CL_parse.CheckOrDownloadFile(fn)) {
      //                            CL.precache_check = Defines.CS_PLAYERSKINS + i
      //                                    * CL.PLAYER_MULT + 4;
      //                            return; // started a download
      //                        }
      //                        n++;
      //                    /* FALL THROUGH */
      //
      //                    case 4: // skin_i
      //                        fn = "players/" + model + "/" + skin + "_i.pcx";
      //                        if (!CL_parse.CheckOrDownloadFile(fn)) {
      //                            CL.precache_check = Defines.CS_PLAYERSKINS + i
      //                                    * CL.PLAYER_MULT + 5;
      //                            return; // started a download
      //                        }
      //                        // move on to next model
      //                        CL.precache_check = Defines.CS_PLAYERSKINS + (i + 1)
      //                                * CL.PLAYER_MULT;
      //                    }
      //                }
      //            }
      // precache phase completed
      Client.precache_check = ENV_CNT;
    }

    if (Client.precache_check == ENV_CNT) {
      Client.precache_check = ENV_CNT + 1;

      int iw[] = { map_checksum };

      CM.CM_LoadMap(Globals.cl.configstrings[Constants.CS_MODELS + 1],
          true, iw);
      map_checksum = iw[0];

      //            if ((map_checksum ^ Lib
      //                    .atoi(Globals.cl.configstrings[Defines.CS_MAPCHECKSUM])) != 0) {
      //                Com
      //                        .Error(
      //                                Defines.ERR_DROP,
      //                                "Local map version differs from server: "
      //                                        + map_checksum
      //                                        + " != '"
      //                                        + Globals.cl.configstrings[Defines.CS_MAPCHECKSUM]
      //                                        + "'\n");
      //                return;
      //            }
    }
    //
    //        if (CL.precache_check > ENV_CNT && CL.precache_check < TEXTURE_CNT) {
    //            if (SV_MAIN.allow_download.value != 0
    //                    && SV_MAIN.allow_download_maps.value != 0) {
    //                while (CL.precache_check < TEXTURE_CNT) {
    //                    int n = CL.precache_check++ - ENV_CNT - 1;
    //
    //                    if ((n & 1) != 0)
    //                        fn = "env/" + Globals.cl.configstrings[Defines.CS_SKY]
    //                                + env_suf[n / 2] + ".pcx";
    //                    else
    //                        fn = "env/" + Globals.cl.configstrings[Defines.CS_SKY]
    //                                + env_suf[n / 2] + ".tga";
    //                    if (!CL_parse.CheckOrDownloadFile(fn))
    //                        return; // started a download
    //                }
    //            }
    //            CL.precache_check = TEXTURE_CNT;
    //        }
    //
    if (Client.precache_check == TEXTURE_CNT) {
      Client.precache_check = TEXTURE_CNT + 1;
      Client.precache_tex = 0;
    }
    //
    // confirm existance of textures, download any that don't exist
    if (Client.precache_check == TEXTURE_CNT + 1) {
      //            // from qcommon/cmodel.c
      //            // extern int numtexinfo;
      //            // extern mapsurface_t map_surfaces[];
      //
      //            if (SV_MAIN.allow_download.value != 0
      //                    && SV_MAIN.allow_download_maps.value != 0) {
      //                while (CL.precache_tex < CM.numtexinfo) {
      //                    //char fn[MAX_OSPATH];
      //
      //                    fn = "textures/" + CM.map_surfaces[CL.precache_tex++].rname
      //                            + ".wal";
      //                    if (!CL_parse.CheckOrDownloadFile(fn))
      //                        return; // started a download
      //                }
      //            }
      Client.precache_check = TEXTURE_CNT + 999;
    }

    //	  ZOID
    ClientParser.RegisterSounds();
    ClientView.PrepRefresh();

    Buffers.writeByte(Globals.cls.netchan.message, Constants.clc_stringcmd);
    Buffers.WriteString(Globals.cls.netchan.message, "begin "
        + Client.precache_spawncount + "\n");
  }

  /**
   * InitLocal
   */
  public static void initLocal() {
    Globals.cls.state = Constants.ca_disconnected;
    Globals.cls.realtime = Timer.Milliseconds();

    ClientInput.InitInput();

    ConsoleVariables.Get("adr0", "", Constants.CVAR_ARCHIVE);
    ConsoleVariables.Get("adr1", "", Constants.CVAR_ARCHIVE);
    ConsoleVariables.Get("adr2", "", Constants.CVAR_ARCHIVE);
    ConsoleVariables.Get("adr3", "", Constants.CVAR_ARCHIVE);
    ConsoleVariables.Get("adr4", "", Constants.CVAR_ARCHIVE);
    ConsoleVariables.Get("adr5", "", Constants.CVAR_ARCHIVE);
    ConsoleVariables.Get("adr6", "", Constants.CVAR_ARCHIVE);
    ConsoleVariables.Get("adr7", "", Constants.CVAR_ARCHIVE);
    ConsoleVariables.Get("adr8", "", Constants.CVAR_ARCHIVE);

    //
    // register our variables
    //
    Globals.cl_stereo_separation = ConsoleVariables.Get("cl_stereo_separation", "0.4",
        Constants.CVAR_ARCHIVE);
    Globals.cl_stereo = ConsoleVariables.Get("cl_stereo", "0", 0);

    Globals.cl_add_blend = ConsoleVariables.Get("cl_blend", "1", 0);
    Globals.cl_add_lights = ConsoleVariables.Get("cl_lights", "1", 0);
    Globals.cl_add_particles = ConsoleVariables.Get("cl_particles", "1", 0);
    Globals.cl_add_entities = ConsoleVariables.Get("cl_entities", "1", 0);
    Globals.cl_gun = ConsoleVariables.Get("cl_gun", "1", 0);
    Globals.cl_footsteps = ConsoleVariables.Get("cl_footsteps", "1", 0);
    Globals.cl_noskins = ConsoleVariables.Get("cl_noskins", "0", 0);
    Globals.cl_autoskins = ConsoleVariables.Get("cl_autoskins", "0", 0);
    Globals.cl_predict = ConsoleVariables.Get("cl_predict", "1", 0);

    Globals.cl_maxfps = ConsoleVariables.Get("cl_maxfps", "90", 0);

    Globals.cl_upspeed = ConsoleVariables.Get("cl_upspeed", "200", 0);
    Globals.cl_forwardspeed = ConsoleVariables.Get("cl_forwardspeed", "200", 0);
    Globals.cl_sidespeed = ConsoleVariables.Get("cl_sidespeed", "200", 0);
    Globals.cl_yawspeed = ConsoleVariables.Get("cl_yawspeed", "140", 0);
    Globals.cl_pitchspeed = ConsoleVariables.Get("cl_pitchspeed", "150", 0);
    Globals.cl_anglespeedkey = ConsoleVariables.Get("cl_anglespeedkey", "1.5", 0);

    Globals.cl_run = ConsoleVariables.Get("cl_run", "0", Constants.CVAR_ARCHIVE);
    Globals.lookspring = ConsoleVariables.Get("lookspring", "0", Constants.CVAR_ARCHIVE);
    Globals.lookstrafe = ConsoleVariables.Get("lookstrafe", "0", Constants.CVAR_ARCHIVE);
    Globals.sensitivity = ConsoleVariables
    .Get("sensitivity", "3", Constants.CVAR_ARCHIVE);

    Globals.m_pitch = ConsoleVariables.Get("m_pitch", "0.022", Constants.CVAR_ARCHIVE);
    Globals.m_yaw = ConsoleVariables.Get("m_yaw", "0.022", 0);
    Globals.m_forward = ConsoleVariables.Get("m_forward", "1", 0);
    Globals.m_side = ConsoleVariables.Get("m_side", "1", 0);

    Globals.cl_shownet = ConsoleVariables.Get("cl_shownet", "0", 0);
    Globals.cl_showmiss = ConsoleVariables.Get("cl_showmiss", "0", 0);
    Globals.cl_showclamp = ConsoleVariables.Get("showclamp", "0", 0);
    Globals.cl_timeout = ConsoleVariables.Get("cl_timeout", "120", 0);
    Globals.cl_paused = ConsoleVariables.Get("paused", "0", 0);
    Globals.cl_timedemo = ConsoleVariables.Get("timedemo", "0", 0);

    Globals.rcon_client_password = ConsoleVariables.Get("rcon_password", "", 0);
    Globals.rcon_address = ConsoleVariables.Get("rcon_address", "", 0);

    Globals.cl_lightlevel = ConsoleVariables.Get("r_lightlevel", "0", 0);

    //
    // userinfo
    //
    Globals.info_password = ConsoleVariables.Get("password", "", Constants.CVAR_USERINFO);
    Globals.info_spectator = ConsoleVariables.Get("spectator", "0",
        Constants.CVAR_USERINFO);
    Globals.name = ConsoleVariables.Get("name", "unnamed", Constants.CVAR_USERINFO
        | Constants.CVAR_ARCHIVE);
    Globals.skin = ConsoleVariables.Get("skin", "male/grunt", Constants.CVAR_USERINFO
        | Constants.CVAR_ARCHIVE);
    Globals.rate = ConsoleVariables.Get("rate", "25000", Constants.CVAR_USERINFO
        | Constants.CVAR_ARCHIVE); // FIXME
    Globals.msg = ConsoleVariables.Get("msg", "1", Constants.CVAR_USERINFO
        | Constants.CVAR_ARCHIVE);
    Globals.hand = ConsoleVariables.Get("hand", "0", Constants.CVAR_USERINFO
        | Constants.CVAR_ARCHIVE);
    Globals.fov = ConsoleVariables.Get("fov", "90", Constants.CVAR_USERINFO
        | Constants.CVAR_ARCHIVE);
    Globals.gender = ConsoleVariables.Get("gender", "male", Constants.CVAR_USERINFO
        | Constants.CVAR_ARCHIVE);
    Globals.gender_auto = ConsoleVariables
    .Get("gender_auto", "1", Constants.CVAR_ARCHIVE);
    Globals.gender.modified = false; // clear this so we know when user sets
    // it manually

    Globals.cl_vwep = ConsoleVariables.Get("cl_vwep", "1", Constants.CVAR_ARCHIVE);

    //
    // register our commands
    //
    Commands.addCommand("cmd", forwardToServerCommand);
    Commands.addCommand("pause", pauseCommand);
    Commands.addCommand("pingservers", pingServersCommand);
    Commands.addCommand("skins", skinsCommand);

    Commands.addCommand("userinfo", userinfoCommand);
    Commands.addCommand("snd_restart", sndRestartCommand);

    Commands.addCommand("changing", changingCommand);
    Commands.addCommand("disconnect", disconnectCommand);
    Commands.addCommand("record", recordCommand);
    Commands.addCommand("stop", stopCommand);

    Commands.addCommand("quit", quitCommand);

    Commands.addCommand("connect", connectCommand);
    Commands.addCommand("reconnect", reconnectCommand);

    Commands.addCommand("rcon", rconCommand);

    Commands.addCommand("precache", precacheCommand);

    Commands.addCommand("download", ClientParser.downloadCommand);

    //
    // forward to server commands
    //
    // the only thing this does is allow command completion
    // to work -- all unknown commands are automatically
    // forwarded to the server
    Commands.addCommand("wave", null);
    Commands.addCommand("inven", null);
    Commands.addCommand("kill", null);
    Commands.addCommand("use", null);
    Commands.addCommand("drop", null);
    Commands.addCommand("say", null);
    Commands.addCommand("say_team", null);
    Commands.addCommand("info", null);
    Commands.addCommand("prog", null);
    Commands.addCommand("give", null);
    Commands.addCommand("god", null);
    Commands.addCommand("notarget", null);
    Commands.addCommand("noclip", null);
    Commands.addCommand("invuse", null);
    Commands.addCommand("invprev", null);
    Commands.addCommand("invnext", null);
    Commands.addCommand("invdrop", null);
    Commands.addCommand("weapnext", null);
    Commands.addCommand("weapprev", null);

  }

  /**
   * WriteConfiguration
   * 
   * Writes key bindings and archived cvars to config.cfg.
   */
  public static void writeConfiguration() {
    RandomAccessFile f;
    String path;

    //        if (Globals.cls.state == Defines.ca_uninitialized)
    //            return;

    path = QuakeFileSystem.Gamedir() + "/config.cfg";
    f = Lib.fopen(path, "rw");
    if (f == null) {
      Com.Printf("Couldn't write config.cfg.\n");
      return;
    }
    try {
      f.seek(0);
      f.setLength(0);
    } catch (IOException e1) {
    }
    try {
      f.writeBytes("// generated by quake, do not modify\n");
    } catch (IOException e) {
    }

    Key.WriteBindings(f);
    Lib.fclose(f);
    ConsoleVariables.WriteVariables(path);
  }

  /**
   * FixCvarCheats
   */
  public static void fixCvarCheats() {
    int i;
    Client.CheatVar var;

    if ("1".equals(Globals.cl.configstrings[Constants.CS_MAXCLIENTS])
        || 0 == Globals.cl.configstrings[Constants.CS_MAXCLIENTS]
                                         .length())
      return; // single player can cheat

    // find all the cvars if we haven't done it yet
    if (0 == Client.numcheatvars) {
      while (Client.cheatvars[Client.numcheatvars].name != null) {
        Client.cheatvars[Client.numcheatvars].var = ConsoleVariables.Get(
            Client.cheatvars[Client.numcheatvars].name,
            Client.cheatvars[Client.numcheatvars].value, 0);
        Client.numcheatvars++;
      }
    }

    // make sure they are all set to the proper values
    for (i = 0; i < Client.numcheatvars; i++) {
      var = Client.cheatvars[i];
      if (!var.var.string.equals(var.value)) {
        ConsoleVariables.Set(var.name, var.value);
      }
    }
  }

  //	  =============================================================

  /**
   * SendCommand
   */
  public static void sendCommand() {
    // get new key events
    Sys.SendKeyEvents();

    // allow mice or other external controllers to add commands
    IN.Commands();

    // process console commands
    CommandBuffer.Execute();

    // fix any cheating cvars
    fixCvarCheats();

    // send intentions now
    ClientInput.SendCmd();

    // resend a connection request if necessary
    checkForResend();
  }

  //	private static int lasttimecalled;

  /**
   * Frame
   */
  public static void frame(int msec) {

    if (Globals.dedicated.value != 0)
      return;

    extratime += msec;

    if (Globals.cl_timedemo.value == 0.0f) {
      if (Globals.cls.state == Constants.ca_connected && extratime < 100) {
        return; // don't flood packets out while connecting
      }
      if (extratime < 1000 / Globals.cl_maxfps.value) {
        return; // framerate is too high
      }
    }

    // let the mouse activate or deactivate
    IN.Frame();

    // decide the simulation time
    Globals.cls.frametime = extratime / 1000.0f;
    Globals.cl.time += extratime;
    Globals.cls.realtime = Globals.curtime;

    extratime = 0;

    if (Globals.cls.frametime > (1.0f / 5))
      Globals.cls.frametime = (1.0f / 5);

    // if in the debugger last frame, don't timeout
    if (msec > 5000)
      Globals.cls.netchan.last_received = Timer.Milliseconds();

    // fetch results from server
    readPackets();

    // send a new command message to the server
    sendCommand();

    // predict all unacknowledged movements
    ClientPrediction.PredictMovement();

    // allow rendering DLL change
    Window.CheckChanges();

    if (!Globals.cl.refresh_prepped
        && Globals.cls.state == Constants.ca_active) {

      ClientView.PrepRefresh();

      // HACK: PrepRefresh() can kick off model loads, and UpdateScreen()
      // will try to use them imeediately. So we jump out early here if
      // there are any outstanding file requests.
      if (ResourceLoader.Pump()) {
        return;
      }
    }

    Screen.UpdateScreen();

    // update audio
    Sound.Update(Globals.cl.refdef.vieworg, Globals.cl.v_forward,
        Globals.cl.v_right, Globals.cl.v_up);

    // advance local effects for next frame
    ClientEffects.RunDLights();
    ClientEffects.RunLightStyles();
    Screen.RunCinematic();
    Screen.RunConsole();

    Globals.cls.framecount++;
    if (Globals.cls.state != Constants.ca_active
        || Globals.cls.key_dest != Constants.key_game) {
      Compatibility.sleep(20);
    }
  }

  /**
   * Shutdown
   */
  public static void shutdown() {
    if (isdown) {
      System.out.print("recursive shutdown\n");
      return;
    }
    isdown = true;

    writeConfiguration();

    Sound.Shutdown();
    IN.Shutdown();
    Window.Shutdown();
  }

  /**
   * Initialize client subsystem.
   */
  public static void init() {
    if (Globals.dedicated.value != 0.0f)
      return; // nothing running on the client

    // all archived variables will now be loaded

    Console.Init(); //ok

    Sound.Init(); //empty
    Window.Init();

    Video.Init();

    Globals.net_message.data = Globals.net_message_buffer;
    Globals.net_message.maxsize = Globals.net_message_buffer.length;

    Menu.Init();

    Screen.Init();
    //Globals.cls.disable_screen = 1.0f; // don't draw yet

    initLocal();
    IN.Init();

    //        FS.ExecAutoexec();
    CommandBuffer.Execute();
  }

  /**
   * Called after an ERR_DROP was thrown.
   */
  public static void drop() {
    if (Globals.cls.state == Constants.ca_uninitialized)
      return;
    if (Globals.cls.state == Constants.ca_disconnected)
      return;

    disconnect();

    // drop loading plaque unless this is the initial game start
    if (Globals.cls.disable_servercount != -1)
      Screen.EndLoadingPlaque(); // get rid of loading plaque
  }
}

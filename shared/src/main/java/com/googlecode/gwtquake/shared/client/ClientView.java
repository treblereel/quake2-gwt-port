/*
 * Copyright (C) 1997-2001 Id Software, Inc.
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
 *  
 */
/* Modifications
   Copyright 2003-2004 Bytonic Software
   Copyright 2010 Google Inc.
*/
package com.googlecode.gwtquake.shared.client;


import com.googlecode.gwtquake.shared.common.AsyncCallback;
import com.googlecode.gwtquake.shared.common.CM;
import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.Command;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.Globals;
import com.googlecode.gwtquake.shared.render.Models;
import com.googlecode.gwtquake.shared.render.Model;
import com.googlecode.gwtquake.shared.sys.Sys;


import java.util.StringTokenizer;

public class ClientView {

    static int num_cl_weaponmodels;

    static String[] cl_weaponmodels = new String[Constants.MAX_CLIENTWEAPONMODELS];

    static boolean inPrepRefresh;
    
    /*
     * =================
     * 
     * CL_PrepRefresh
     * 
     * Call before entering a new level, or after changing dlls
     * =================
     */
    static void PrepRefresh() {
        int i;

        if ((i = Globals.cl.configstrings[Constants.CS_MODELS + 1].length()) == 0)
            return; // no map loaded

        if (inPrepRefresh) {
          Com.Printf("already in PrepRefresh(); returning\n");
          return;
        }
        inPrepRefresh = true;
        
        Models.clearModelCache();

        Screen.AddDirtyPoint(0, 0);
        Screen.AddDirtyPoint(Globals.viddef.width - 1, Globals.viddef.height - 1);

        // let the render dll load the map
        String mapname = Globals.cl.configstrings[Constants.CS_MODELS + 1].substring(5,
                i - 4); // skip "maps/"
        // cut off ".bsp"

        // register models, pics, and skins
        Com.Printf("Map: " + mapname + "\r");
        Screen.UpdateScreen();
        Globals.re.BeginRegistration(mapname, new Command() {

          public void execute() {
            Com.Printf("                                     \r");

            // precache status bar pics
            Com.Printf("pics\r");
            Screen.UpdateScreen();
            Screen.TouchPics();
            Com.Printf("                                     \r");

            ClientTent.RegisterTEntModels();

            num_cl_weaponmodels = 1;
            cl_weaponmodels[0] = "weapon.md2";

            for (int i = 1; i < Constants.MAX_MODELS
                    && Globals.cl.configstrings[Constants.CS_MODELS + i].length() != 0; i++) {
                String name = new String(Globals.cl.configstrings[Constants.CS_MODELS + i]);
                if (name.length() > 37)
                    name = name.substring(0, 36);

                if (name.charAt(0) != '*')
                    Com.Printf(name + "\r");

                Screen.UpdateScreen();
                Sys.SendKeyEvents(); // pump message loop
                if (name.charAt(0) == '#') {
                    // special player weapon model
                    if (num_cl_weaponmodels < Constants.MAX_CLIENTWEAPONMODELS) {
                        cl_weaponmodels[num_cl_weaponmodels] = Globals.cl.configstrings[Constants.CS_MODELS
                                + i].substring(1);
                        num_cl_weaponmodels++;
                    }
                } else {
                  final int finalI = i;
                  final String finalName = name;
                  
                  Globals.re.RegisterModel(Globals.cl.configstrings[Constants.CS_MODELS + i], 
                      new AsyncCallback<Model>() {
                        public void onSuccess(Model response) {
                          Globals.cl.model_draw[finalI] = response;
                          if (finalName.charAt(0) == '*')
                            Globals.cl.model_clip[finalI] = CM.InlineModel(
                                Globals.cl.configstrings[Constants.CS_MODELS + finalI]);
                              else
                                Globals.cl.model_clip[finalI] = null;
                            }
                            
                            public void onFailure(Throwable e) {
                              // TODO Auto-generated method stub
                              
                            }
                          });
         
                }
                if (name.charAt(0) != '*')
                    Com.Printf("                                     \r");
            }

            Com.Printf("images\r");
            Screen.UpdateScreen();
            for (int i = 1; i < Constants.MAX_IMAGES
                    && Globals.cl.configstrings[Constants.CS_IMAGES + i].length() > 0; i++) {
                Globals.cl.image_precache[i] = Globals.re
                        .RegisterPic(Globals.cl.configstrings[Constants.CS_IMAGES + i]);
                Sys.SendKeyEvents(); // pump message loop
            }

            Com.Printf("                                     \r");
            for (int i = 0; i < Constants.MAX_CLIENTS; i++) {
                if (Globals.cl.configstrings[Constants.CS_PLAYERSKINS + i].length() == 0)
                    continue;
                Com.Printf("client " + i + '\r');
                Screen.UpdateScreen();
                Sys.SendKeyEvents(); // pump message loop
                ClientParser.ParseClientinfo(i);
                Com.Printf("                                     \r");
            }

            ClientParser.LoadClientinfo(Globals.cl.baseclientinfo,
                    "unnamed\\male/grunt");

            // set sky textures and speed
            Com.Printf("sky\r");
            Screen.UpdateScreen();
            float rotate = Float
                    .parseFloat(Globals.cl.configstrings[Constants.CS_SKYROTATE]);
            StringTokenizer st = new StringTokenizer(
                    Globals.cl.configstrings[Constants.CS_SKYAXIS]);
            float[] axis = new float[3];
            axis[0] = Float.parseFloat(st.nextToken());
            axis[1] = Float.parseFloat(st.nextToken());
            axis[2] = Float.parseFloat(st.nextToken());
            Globals.re.SetSky(Globals.cl.configstrings[Constants.CS_SKY], rotate,
                    axis);
            Com.Printf("                                     \r");

            // the renderer can now free unneeded stuff
            Globals.re.EndRegistration();

            // clear any lines of console text
            Console.ClearNotify();

            Screen.UpdateScreen();
            Globals.cl.refresh_prepped = true;
            Globals.cl.force_refdef = true; // make sure we have a valid refdef
            
            inPrepRefresh = false;
          }});
        
    }

    public static void AddNetgraph() {
        int i;
        int in;
        int ping;

        // if using the debuggraph for something else, don't
        // add the net lines
        if (Screen.scr_debuggraph.value == 0.0f || Screen.scr_timegraph.value == 0.0f)
            return;

        for (i = 0; i < Globals.cls.netchan.dropped; i++)
            Screen.DebugGraph(30, 0x40);

        for (i = 0; i < Globals.cl.surpressCount; i++)
            Screen.DebugGraph(30, 0xdf);

        // see what the latency was on this packet
        in = Globals.cls.netchan.incoming_acknowledged
                & (Constants.CMD_BACKUP - 1);
        ping = (int) (Globals.cls.realtime - Globals.cl.cmd_time[in]);
        ping /= 30;
        if (ping > 30)
            ping = 30;
        Screen.DebugGraph(ping, 0xd0);
    }
}

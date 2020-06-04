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
package com.googlecode.gwtquake.shared.game;

import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.ConsoleVariables;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.Globals;
import com.googlecode.gwtquake.shared.game.adapters.EntityThinkAdapter;
import com.googlecode.gwtquake.shared.game.monsters.*;
import com.googlecode.gwtquake.shared.server.ServerGame;
import com.googlecode.gwtquake.shared.server.ServerInit;
import com.googlecode.gwtquake.shared.util.Lib;


public class GameSpawn {

    static EntityThinkAdapter SP_item_health = new EntityThinkAdapter() {
        public String getID(){ return "SP_item_health"; }
        public boolean think(Entity ent) {
            GameItems.SP_item_health(ent);
            return true;
        }
    };

    static EntityThinkAdapter SP_item_health_small = new EntityThinkAdapter() {
        public String getID(){ return "SP_item_health_small"; }
        public boolean think(Entity ent) {
            GameItems.SP_item_health_small(ent);
            return true;
        }
    };

    static EntityThinkAdapter SP_item_health_large = new EntityThinkAdapter() {
        public String getID(){ return "SP_item_health_large"; }
        public boolean think(Entity ent) {
            GameItems.SP_item_health_large(ent);
            return true;
        }
    };

    static EntityThinkAdapter SP_item_health_mega = new EntityThinkAdapter() {
        public String getID(){ return "SP_item_health_mega"; }
        public boolean think(Entity ent) {
            GameItems.SP_item_health_mega(ent);
            return true;
        }
    };

    static EntityThinkAdapter SP_info_player_start = new EntityThinkAdapter() {
        public String getID(){ return "SP_info_player_start"; }
        public boolean think(Entity ent) {
            PlayerClient.SP_info_player_start(ent);
            return true;
        }
    };

    static EntityThinkAdapter SP_info_player_deathmatch = new EntityThinkAdapter() {
        public String getID(){ return "SP_info_player_deathmatch"; }
        public boolean think(Entity ent) {
            PlayerClient.SP_info_player_deathmatch(ent);
            return true;
        }
    };

    static EntityThinkAdapter SP_info_player_coop = new EntityThinkAdapter() {
        public String getID(){ return "SP_info_player_coop"; }
        public boolean think(Entity ent) {
            PlayerClient.SP_info_player_coop(ent);
            return true;
        }
    };

    static EntityThinkAdapter SP_info_player_intermission = new EntityThinkAdapter() {
        public String getID(){ return "SP_info_player_intermission"; }
        public boolean think(Entity ent) {
            PlayerClient.SP_info_player_intermission();
            return true;
        }
    };

    static EntityThinkAdapter SP_func_plat = new EntityThinkAdapter() {
        public String getID(){ return "SP_func_plat"; }
        public boolean think(Entity ent) {
            GameFunc.SP_func_plat(ent);
            return true;
        }
    };


    static EntityThinkAdapter SP_func_water = new EntityThinkAdapter() {
        public String getID(){ return "SP_func_water"; }
        public boolean think(Entity ent) {
            GameFunc.SP_func_water(ent);
            return true;
        }
    };

    static EntityThinkAdapter SP_func_train = new EntityThinkAdapter() {
        public String getID(){ return "SP_func_train"; }
        public boolean think(Entity ent) {
            GameFunc.SP_func_train(ent);
            return true;
        }
    };

    static EntityThinkAdapter SP_func_clock = new EntityThinkAdapter() {
        public String getID(){ return "SP_func_clock"; }
        public boolean think(Entity ent) {
            GameMisc.SP_func_clock(ent);
            return true;
        }
    };

    /**
     * QUAKED worldspawn (0 0 0) ?
     * 
     * Only used for the world. "sky" environment map name "skyaxis" vector axis
     * for rotating sky "skyrotate" speed of rotation in degrees/second "sounds"
     * music cd track number "gravity" 800 is default gravity "message" text to
     * print at user logon
     */

    static EntityThinkAdapter SP_worldspawn = new EntityThinkAdapter() {
        public String getID(){ return "SP_worldspawn"; }

        public boolean think(Entity ent) {
            ent.movetype = Constants.MOVETYPE_PUSH;
            ent.solid = Constants.SOLID_BSP;
            ent.inuse = true;
            // since the world doesn't use G_Spawn()
            ent.s.modelindex = 1;
            // world model is always index 1
            //---------------
            // reserve some spots for dead player bodies for coop / deathmatch
            PlayerClient.InitBodyQue();
            // set configstrings for items
            GameItems.SetItemNames();
            if (GameBase.st.nextmap != null)
                GameBase.level.nextmap = GameBase.st.nextmap;
            // make some data visible to the server
            if (ent.message != null && ent.message.length() > 0) {
                ServerGame.PF_Configstring(Constants.CS_NAME, ent.message);
                GameBase.level.level_name = ent.message;
            } else
                GameBase.level.level_name = GameBase.level.mapname;
            if (GameBase.st.sky != null && GameBase.st.sky.length() > 0)
              ServerGame.PF_Configstring(Constants.CS_SKY, GameBase.st.sky);
            else
              ServerGame.PF_Configstring(Constants.CS_SKY, "unit1_");
            ServerGame.PF_Configstring(Constants.CS_SKYROTATE, ""
            + GameBase.st.skyrotate);
            ServerGame.PF_Configstring(Constants.CS_SKYAXIS, Lib
            .vtos(GameBase.st.skyaxis));
            ServerGame.PF_Configstring(Constants.CS_CDTRACK, "" + ent.sounds);
            ServerGame.PF_Configstring(Constants.CS_MAXCLIENTS, ""
            + (int) (GameBase.maxclients.value));
            // status bar program
            if (GameBase.deathmatch.value != 0)
              ServerGame.PF_Configstring(Constants.CS_STATUSBAR, "" + dm_statusbar);
            else
              ServerGame.PF_Configstring(Constants.CS_STATUSBAR, "" + single_statusbar);
            //---------------
            // help icon for statusbar
            ServerInit.SV_ImageIndex("i_help");
            GameBase.level.pic_health = ServerInit.SV_ImageIndex("i_health");
            ServerInit.SV_ImageIndex("help");
            ServerInit.SV_ImageIndex("field_3");
            if ("".equals(GameBase.st.gravity))
              ConsoleVariables.Set("sv_gravity", "800");
            else
              ConsoleVariables.Set("sv_gravity", GameBase.st.gravity);
            GameBase.snd_fry = ServerInit.SV_SoundIndex("player/fry.wav");
            // standing in lava / slime
            GameItems.PrecacheItem(GameItems.FindItem("Blaster"));
            ServerInit.SV_SoundIndex("player/lava1.wav");
            ServerInit.SV_SoundIndex("player/lava2.wav");
            ServerInit.SV_SoundIndex("misc/pc_up.wav");
            ServerInit.SV_SoundIndex("misc/talk1.wav");
            ServerInit.SV_SoundIndex("misc/udeath.wav");
            // gibs
            ServerInit.SV_SoundIndex("items/respawn1.wav");
            // sexed sounds
            ServerInit.SV_SoundIndex("*death1.wav");
            ServerInit.SV_SoundIndex("*death2.wav");
            ServerInit.SV_SoundIndex("*death3.wav");
            ServerInit.SV_SoundIndex("*death4.wav");
            ServerInit.SV_SoundIndex("*fall1.wav");
            ServerInit.SV_SoundIndex("*fall2.wav");
            ServerInit.SV_SoundIndex("*gurp1.wav");
            // drowning damage
            ServerInit.SV_SoundIndex("*gurp2.wav");
            ServerInit.SV_SoundIndex("*jump1.wav");
            // player jump
            ServerInit.SV_SoundIndex("*pain25_1.wav");
            ServerInit.SV_SoundIndex("*pain25_2.wav");
            ServerInit.SV_SoundIndex("*pain50_1.wav");
            ServerInit.SV_SoundIndex("*pain50_2.wav");
            ServerInit.SV_SoundIndex("*pain75_1.wav");
            ServerInit.SV_SoundIndex("*pain75_2.wav");
            ServerInit.SV_SoundIndex("*pain100_1.wav");
            ServerInit.SV_SoundIndex("*pain100_2.wav");
            // sexed models
            // THIS ORDER MUST MATCH THE DEFINES IN g_local.h
            // you can add more, max 15
            ServerInit.SV_ModelIndex("#w_blaster.md2");
            ServerInit.SV_ModelIndex("#w_shotgun.md2");
            ServerInit.SV_ModelIndex("#w_sshotgun.md2");
            ServerInit.SV_ModelIndex("#w_machinegun.md2");
            ServerInit.SV_ModelIndex("#w_chaingun.md2");
            ServerInit.SV_ModelIndex("#a_grenades.md2");
            ServerInit.SV_ModelIndex("#w_glauncher.md2");
            ServerInit.SV_ModelIndex("#w_rlauncher.md2");
            ServerInit.SV_ModelIndex("#w_hyperblaster.md2");
            ServerInit.SV_ModelIndex("#w_railgun.md2");
            ServerInit.SV_ModelIndex("#w_bfg.md2");
            //-------------------
            ServerInit.SV_SoundIndex("player/gasp1.wav");
            // gasping for air
            ServerInit.SV_SoundIndex("player/gasp2.wav");
            // head breaking surface, not gasping
            ServerInit.SV_SoundIndex("player/watr_in.wav");
            // feet hitting water
            ServerInit.SV_SoundIndex("player/watr_out.wav");
            // feet leaving water
            ServerInit.SV_SoundIndex("player/watr_un.wav");
            // head going underwater
            ServerInit.SV_SoundIndex("player/u_breath1.wav");
            ServerInit.SV_SoundIndex("player/u_breath2.wav");
            ServerInit.SV_SoundIndex("items/pkup.wav");
            // bonus item pickup
            ServerInit.SV_SoundIndex("world/land.wav");
            // landing thud
            ServerInit.SV_SoundIndex("misc/h2ohit1.wav");
            // landing splash
            ServerInit.SV_SoundIndex("items/damage.wav");
            ServerInit.SV_SoundIndex("items/protect.wav");
            ServerInit.SV_SoundIndex("items/protect4.wav");
            ServerInit.SV_SoundIndex("weapons/noammo.wav");
            ServerInit.SV_SoundIndex("infantry/inflies1.wav");
            GameBase.sm_meat_index = ServerInit.SV_ModelIndex("models/objects/gibs/sm_meat/tris.md2");
            ServerInit.SV_ModelIndex("models/objects/gibs/arm/tris.md2");
            ServerInit.SV_ModelIndex("models/objects/gibs/bone/tris.md2");
            ServerInit.SV_ModelIndex("models/objects/gibs/bone2/tris.md2");
            ServerInit.SV_ModelIndex("models/objects/gibs/chest/tris.md2");
            ServerInit.SV_ModelIndex("models/objects/gibs/skull/tris.md2");
            ServerInit.SV_ModelIndex("models/objects/gibs/head2/tris.md2");
            //
            // Setup light animation tables. 'a' is total darkness, 'z' is
            // doublebright.
            //
            // 0 normal
            ServerGame.PF_Configstring(Constants.CS_LIGHTS + 0, "m");
            // 1 FLICKER (first variety)
            ServerGame.PF_Configstring(Constants.CS_LIGHTS + 1, "mmnmmommommnonmmonqnmmo");
            // 2 SLOW STRONG PULSE
            ServerGame.PF_Configstring(Constants.CS_LIGHTS + 2, "abcdefghijklmnopqrstuvwxyzyxwvutsrqponmlkjihgfedcba");
            // 3 CANDLE (first variety)
            ServerGame.PF_Configstring(Constants.CS_LIGHTS + 3, "mmmmmaaaaammmmmaaaaaabcdefgabcdefg");
            // 4 FAST STROBE
            ServerGame.PF_Configstring(Constants.CS_LIGHTS + 4, "mamamamamama");
            // 5 GENTLE PULSE 1
            ServerGame.PF_Configstring(Constants.CS_LIGHTS + 5, "jklmnopqrstuvwxyzyxwvutsrqponmlkj");
            // 6 FLICKER (second variety)
            ServerGame.PF_Configstring(Constants.CS_LIGHTS + 6, "nmonqnmomnmomomno");
            // 7 CANDLE (second variety)
            ServerGame.PF_Configstring(Constants.CS_LIGHTS + 7, "mmmaaaabcdefgmmmmaaaammmaamm");
            // 8 CANDLE (third variety)
            ServerGame.PF_Configstring(Constants.CS_LIGHTS + 8, "mmmaaammmaaammmabcdefaaaammmmabcdefmmmaaaa");
            // 9 SLOW STROBE (fourth variety)
            ServerGame.PF_Configstring(Constants.CS_LIGHTS + 9, "aaaaaaaazzzzzzzz");
            // 10 FLUORESCENT FLICKER
            ServerGame.PF_Configstring(Constants.CS_LIGHTS + 10, "mmamammmmammamamaaamammma");
            // 11 SLOW PULSE NOT FADE TO BLACK
            ServerGame.PF_Configstring(Constants.CS_LIGHTS + 11, "abcdefghijklmnopqrrqponmlkjihgfedcba");
            // styles 32-62 are assigned by the light program for switchable
            // lights
            // 63 testing
            ServerGame.PF_Configstring(Constants.CS_LIGHTS + 63, "a");
            return true;
        }
    };

    /** 
     * ED_NewString.
     */
    static String ED_NewString(String string) {

        int l = string.length();
        StringBuffer newb = new StringBuffer(l);

        for (int i = 0; i < l; i++) {
            char c = string.charAt(i);
            if (c == '\\' && i < l - 1) {
                c = string.charAt(++i);
                if (c == 'n')
                    newb.append('\n');
                else
                    newb.append('\\');
            } else
                newb.append(c);
        }

        return newb.toString();
    }

    /**
     * ED_ParseField
     * 
     * Takes a key/value pair and sets the binary values in an edict.
     */
    static void ED_ParseField(String key, String value, Entity ent) {

        if (key.equals("nextmap"))
            Com.Println("nextmap: " + value);
        if (!GameBase.st.set(key, value))
            if (!ent.setField(key, value))
              ServerGame.PF_dprintf("??? The key [" + key
              + "] is not a field\n");

    }

    /**
     * ED_ParseEdict
     * 
     * Parses an edict out of the given string, returning the new position ed
     * should be a properly initialized empty edict.
     */

    static void ED_ParseEdict(Com.ParseHelp ph, Entity ent) {

        boolean init;
        String keyname;
        String com_token;
        init = false;

        GameBase.st = new SpawnTemp();
        while (true) {

            // parse key
            com_token = Com.Parse(ph);
            if (com_token.equals("}"))
                break;

            if (ph.isEof())
              Com.Error(Constants.ERR_FATAL, "ED_ParseEntity: EOF without closing brace");

            keyname = com_token;

            // parse value
            com_token = Com.Parse(ph);

            if (ph.isEof())
              Com.Error(Constants.ERR_FATAL, "ED_ParseEntity: EOF without closing brace");

            if (com_token.equals("}"))
              Com.Error(Constants.ERR_FATAL, "ED_ParseEntity: closing brace without data");

            init = true;
            // keynames with a leading underscore are used for utility comments,
            // and are immediately discarded by quake
            if (keyname.charAt(0) == '_')
                continue;

            ED_ParseField(keyname.toLowerCase(), com_token, ent);

        }

        if (!init) {
            GameUtil.G_ClearEdict(ent);
        }

        return;
    }

    /**
     * G_FindTeams
     * 
     * Chain together all entities with a matching team field.
     * 
     * All but the first will have the FL_TEAMSLAVE flag set. All but the last
     * will have the teamchain field set to the next one.
     */

    static void G_FindTeams() {
        Entity e, e2, chain;
        int i, j;
        int c, c2;
        c = 0;
        c2 = 0;
        for (i = 1; i < GameBase.num_edicts; i++) {
            e = GameBase.g_edicts[i];

            if (!e.inuse)
                continue;
            if (e.team == null)
                continue;
            if ((e.flags & Constants.FL_TEAMSLAVE) != 0)
                continue;
            chain = e;
            e.teammaster = e;
            c++;
            c2++;
            
            for (j = i + 1; j < GameBase.num_edicts; j++) {
                e2 = GameBase.g_edicts[j];
                if (!e2.inuse)
                    continue;
                if (null == e2.team)
                    continue;
                if ((e2.flags & Constants.FL_TEAMSLAVE) != 0)
                    continue;
                if (0 == Lib.strcmp(e.team, e2.team)) {
                    c2++;
                    chain.teamchain = e2;
                    e2.teammaster = e;
                    chain = e2;
                    e2.flags |= Constants.FL_TEAMSLAVE;

                }
            }
        }
    }

    /**
     * SpawnEntities
     * 
     * Creates a server's entity / program execution context by parsing textual
     * entity definitions out of an ent file.
     */

    public static void SpawnEntities(String mapname, String entities,
            String spawnpoint) {
        
        Com.dprintln("SpawnEntities(), mapname=" + mapname);
        Entity ent;
        int inhibit;
        String com_token;
        int i;
        float skill_level;
        //skill.value =2.0f;
        skill_level = (float) Math.floor(GameBase.skill.value);

        if (skill_level < 0)
            skill_level = 0;
        if (skill_level > 3)
            skill_level = 3;
        if (GameBase.skill.value != skill_level)
          ConsoleVariables.ForceSet("skill", "" + skill_level);

        PlayerClient.SaveClientData();

        GameBase.level = new LevelLocals();
        for (int n = 0; n < GameBase.game.maxentities; n++) {
            GameBase.g_edicts[n] = new Entity(n);
        }
        
        GameBase.level.mapname = mapname;
        GameBase.game.spawnpoint = spawnpoint;

        // set client fields on player ents
        for (i = 0; i < GameBase.game.maxclients; i++)
            GameBase.g_edicts[i + 1].client = GameBase.game.clients[i];

        ent = null;
        inhibit = 0; 

        Com.ParseHelp ph = new Com.ParseHelp(entities);

        while (true) { // parse the opening brace

            com_token = Com.Parse(ph);
            if (ph.isEof())
                break;
            if (!com_token.startsWith("{"))
              Com.Error(Constants.ERR_FATAL, "ED_LoadFromFile: found " + com_token
              + " when expecting {");

            if (ent == null)
                ent = GameBase.g_edicts[0];
            else
                ent = GameUtil.G_Spawn();

            ED_ParseEdict(ph, ent);
            Com.DPrintf("spawning ent[" + ent.index + "], classname=" + 
                    ent.classname + ", flags= " + Integer.toHexString(ent.spawnflags));
            
            // yet another map hack
            if (0 == Lib.Q_stricmp(GameBase.level.mapname, "command")
                    && 0 == Lib.Q_stricmp(ent.classname, "trigger_once")
                    && 0 == Lib.Q_stricmp(ent.model, "*27"))
                ent.spawnflags &= ~Constants.SPAWNFLAG_NOT_HARD;

            // remove things (except the world) from different skill levels or
            // deathmatch
            if (ent != GameBase.g_edicts[0]) {
                if (GameBase.deathmatch.value != 0) {
                    if ((ent.spawnflags & Constants.SPAWNFLAG_NOT_DEATHMATCH) != 0) {
                        
                        Com.DPrintf("->inhibited.\n");
                        GameUtil.G_FreeEdict(ent);
                        inhibit++;
                        continue;
                    }
                } else {
                    if (/*
                         * ((coop.value) && (ent.spawnflags &
                         * SPAWNFLAG_NOT_COOP)) ||
                         */
                    ((GameBase.skill.value == 0) && (ent.spawnflags & Constants.SPAWNFLAG_NOT_EASY) != 0)
                            || ((GameBase.skill.value == 1) && (ent.spawnflags & Constants.SPAWNFLAG_NOT_MEDIUM) != 0)
                            || (((GameBase.skill.value == 2) || (GameBase.skill.value == 3)) && (ent.spawnflags & Constants.SPAWNFLAG_NOT_HARD) != 0)) {
                        
                        Com.DPrintf("->inhibited.\n");
                        GameUtil.G_FreeEdict(ent);
                        inhibit++;
                        
                        continue;
                    }
                }

                ent.spawnflags &= ~(Constants.SPAWNFLAG_NOT_EASY
                        | Constants.SPAWNFLAG_NOT_MEDIUM
                        | Constants.SPAWNFLAG_NOT_HARD
                        | Constants.SPAWNFLAG_NOT_COOP | Constants.SPAWNFLAG_NOT_DEATHMATCH);
            }
            ED_CallSpawn(ent);
            Com.DPrintf("\n");
        }
        Com.DPrintf("player skill level:" + GameBase.skill.value + "\n");
        Com.DPrintf(inhibit + " entities inhibited.\n");
        i = 1;
        G_FindTeams();
        PlayerTrail.Init();
    }

    static String single_statusbar = "yb	-24 " //	   health
            + "xv	0 " + "hnum " + "xv	50 " + "pic 0 " //	   ammo
            + "if 2 " + "	xv	100 " + "	anum " + "	xv	150 " + "	pic 2 "
            + "endif " //	   armor
            + "if 4 " + "	xv	200 " + "	rnum " + "	xv	250 " + "	pic 4 "
            + "endif " //	   selected item
            + "if 6 " + "	xv	296 " + "	pic 6 " + "endif " + "yb	-50 " //	   picked
            // up
            // item
            + "if 7 " + "	xv	0 " + "	pic 7 " + "	xv	26 " + "	yb	-42 "
            + "	stat_string 8 " + "	yb	-50 " + "endif "
            //	   timer
            + "if 9 " + "	xv	262 " + "	num	2	10 " + "	xv	296 " + "	pic	9 "
            + "endif "
            //		help / weapon icon
            + "if 11 " + "	xv	148 " + "	pic	11 " + "endif ";

    static String dm_statusbar = "yb	-24 " //	   health
            + "xv	0 " + "hnum " + "xv	50 " + "pic 0 " //	   ammo
            + "if 2 " + "	xv	100 " + "	anum " + "	xv	150 " + "	pic 2 "
            + "endif " //	   armor
            + "if 4 " + "	xv	200 " + "	rnum " + "	xv	250 " + "	pic 4 "
            + "endif " //	   selected item
            + "if 6 " + "	xv	296 " + "	pic 6 " + "endif " + "yb	-50 " //	   picked
            // up
            // item
            + "if 7 " + "	xv	0 " + "	pic 7 " + "	xv	26 " + "	yb	-42 "
            + "	stat_string 8 " + "	yb	-50 " + "endif "
            //	   timer
            + "if 9 " + "	xv	246 " + "	num	2	10 " + "	xv	296 " + "	pic	9 "
            + "endif "
            //		help / weapon icon
            + "if 11 " + "	xv	148 " + "	pic	11 " + "endif " //		frags
            + "xr	-50 " + "yt 2 " + "num 3 14 " //	   spectator
            + "if 17 " + "xv 0 " + "yb -58 " + "string2 \"SPECTATOR MODE\" "
            + "endif " //	   chase camera
            + "if 16 " + "xv 0 " + "yb -68 " + "string \"Chasing\" " + "xv 64 "
            + "stat_string 16 " + "endif ";

    static Spawn spawns[] = {
            new Spawn("item_health", SP_item_health),
            new Spawn("item_health_small", SP_item_health_small),
            new Spawn("item_health_large", SP_item_health_large),
            new Spawn("item_health_mega", SP_item_health_mega),
            new Spawn("info_player_start", SP_info_player_start),
            new Spawn("info_player_deathmatch", SP_info_player_deathmatch),
            new Spawn("info_player_coop", SP_info_player_coop),
            new Spawn("info_player_intermission", SP_info_player_intermission),
            new Spawn("func_plat", SP_func_plat),
            new Spawn("func_button", GameFunc.SP_func_button),
            new Spawn("func_door", GameFunc.SP_func_door),
            new Spawn("func_door_secret", GameFunc.SP_func_door_secret),
            new Spawn("func_door_rotating", GameFunc.SP_func_door_rotating),
            new Spawn("func_rotating", GameFunc.SP_func_rotating),
            new Spawn("func_train", SP_func_train),
            new Spawn("func_water", SP_func_water),
            new Spawn("func_conveyor", GameFunc.SP_func_conveyor),
            new Spawn("func_areaportal", GameMisc.SP_func_areaportal),
            new Spawn("func_clock", SP_func_clock),
            new Spawn("func_wall", new EntityThinkAdapter() {
        public String getID(){ return "func_wall"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_func_wall(ent);
                    return true;
                }
            }),
            new Spawn("func_object", new EntityThinkAdapter() {
        public String getID(){ return "SP_func_object"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_func_object(ent);
                    return true;
                }
            }),
            new Spawn("func_timer", new EntityThinkAdapter() {
        public String getID(){ return "SP_func_timer"; }
                public boolean think(Entity ent) {
                    GameFunc.SP_func_timer(ent);
                    return true;
                }
            }),
            new Spawn("func_explosive", new EntityThinkAdapter() {
        public String getID(){ return "SP_func_explosive"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_func_explosive(ent);
                    return true;
                }
            }),
            new Spawn("func_killbox", GameFunc.SP_func_killbox),
            new Spawn("trigger_always", new EntityThinkAdapter() {
        public String getID(){ return "SP_trigger_always"; }
                public boolean think(Entity ent) {
                    GameTrigger.SP_trigger_always(ent);
                    return true;
                }
            }),
            new Spawn("trigger_once", new EntityThinkAdapter() {
        public String getID(){ return "SP_trigger_once"; }
                public boolean think(Entity ent) {
                    GameTrigger.SP_trigger_once(ent);
                    return true;
                }
            }),
            new Spawn("trigger_multiple", new EntityThinkAdapter() {
        public String getID(){ return "SP_trigger_multiple"; }
                public boolean think(Entity ent) {
                    GameTrigger.SP_trigger_multiple(ent);
                    return true;
                }
            }),
            new Spawn("trigger_relay", new EntityThinkAdapter() {
        public String getID(){ return "SP_trigger_relay"; }
                public boolean think(Entity ent) {
                    GameTrigger.SP_trigger_relay(ent);
                    return true;
                }
            }),
            new Spawn("trigger_push", new EntityThinkAdapter() {
        public String getID(){ return "SP_trigger_push"; }
                
                public boolean think(Entity ent) {
                    GameTrigger.SP_trigger_push(ent);
                    return true;
                }
            }),
            new Spawn("trigger_hurt", new EntityThinkAdapter() {
        public String getID(){ return "SP_trigger_hurt"; }
                public boolean think(Entity ent) {
                    GameTrigger.SP_trigger_hurt(ent);
                    return true;
                }
            }),
            new Spawn("trigger_key", new EntityThinkAdapter() {
        public String getID(){ return "SP_trigger_key"; }
                public boolean think(Entity ent) {
                    GameTrigger.SP_trigger_key(ent);
                    return true;
                }
            }),
            new Spawn("trigger_counter", new EntityThinkAdapter() {
        public String getID(){ return "SP_trigger_counter"; }
                public boolean think(Entity ent) {
                    GameTrigger.SP_trigger_counter(ent);
                    return true;
                }
            }),
            new Spawn("trigger_elevator", GameFunc.SP_trigger_elevator),
            new Spawn("trigger_gravity", new EntityThinkAdapter() {
        public String getID(){ return "SP_trigger_gravity"; }
                public boolean think(Entity ent) {
                    GameTrigger.SP_trigger_gravity(ent);
                    return true;
                }
            }),
            new Spawn("trigger_monsterjump", new EntityThinkAdapter() {
        public String getID(){ return "SP_trigger_monsterjump"; }
                public boolean think(Entity ent) {
                    GameTrigger.SP_trigger_monsterjump(ent);
                    return true;
                }
            }),
            new Spawn("target_temp_entity", new EntityThinkAdapter() {
        public String getID(){ return "SP_target_temp_entity"; }
                public boolean think(Entity ent) {
                    GameTarget.SP_target_temp_entity(ent);
                    return true;
                }
            }),
            new Spawn("target_speaker", new EntityThinkAdapter() {
        public String getID(){ return "SP_target_speaker"; }
                public boolean think(Entity ent) {
                    GameTarget.SP_target_speaker(ent);
                    return true;
                }
            }),
            new Spawn("target_explosion", new EntityThinkAdapter() {
        public String getID(){ return "SP_target_explosion"; }
                public boolean think(Entity ent) {
                    GameTarget.SP_target_explosion(ent);
                    return true;
                }
            }),
            new Spawn("target_changelevel", new EntityThinkAdapter() {
        public String getID(){ return "SP_target_changelevel"; }
                public boolean think(Entity ent) {
                    GameTarget.SP_target_changelevel(ent);
                    return true;
                }
            }),
            new Spawn("target_secret", new EntityThinkAdapter() {
        public String getID(){ return "SP_target_secret"; }
                public boolean think(Entity ent) {
                    GameTarget.SP_target_secret(ent);
                    return true;
                }
            }),
            new Spawn("target_goal", new EntityThinkAdapter() {
        public String getID(){ return "SP_target_goal"; }
                public boolean think(Entity ent) {
                    GameTarget.SP_target_goal(ent);
                    return true;
                }
            }),
            new Spawn("target_splash", new EntityThinkAdapter() {
        public String getID(){ return "SP_target_splash"; }
                public boolean think(Entity ent) {
                    GameTarget.SP_target_splash(ent);
                    return true;
                }
            }),
            new Spawn("target_spawner", new EntityThinkAdapter() {
        public String getID(){ return "SP_target_spawner"; }
                public boolean think(Entity ent) {
                    GameTarget.SP_target_spawner(ent);
                    return true;
                }
            }),
            new Spawn("target_blaster", new EntityThinkAdapter() {
        public String getID(){ return "SP_target_blaster"; }
                public boolean think(Entity ent) {
                    GameTarget.SP_target_blaster(ent);
                    return true;
                }
            }),
            new Spawn("target_crosslevel_trigger", new EntityThinkAdapter() {
        public String getID(){ return "SP_target_crosslevel_trigger"; }
                public boolean think(Entity ent) {
                    GameTarget.SP_target_crosslevel_trigger(ent);
                    return true;
                }
            }),
            new Spawn("target_crosslevel_target", new EntityThinkAdapter() {
        public String getID(){ return "SP_target_crosslevel_target"; }
                public boolean think(Entity ent) {
                    GameTarget.SP_target_crosslevel_target(ent);
                    return true;
                }
            }),
            new Spawn("target_laser", new EntityThinkAdapter() {
        public String getID(){ return "SP_target_laser"; }
                public boolean think(Entity ent) {
                    GameTarget.SP_target_laser(ent);
                    return true;
                }
            }),
            new Spawn("target_help", new EntityThinkAdapter() {
        public String getID(){ return "SP_target_help"; }
                public boolean think(Entity ent) {
                    GameTarget.SP_target_help(ent);
                    return true;
                }
            }),
            new Spawn("target_actor", new EntityThinkAdapter() {
        public String getID(){ return "SP_target_actor"; }
                public boolean think(Entity ent) {
                    MonsterActor.SP_target_actor(ent);
                    return true;
                }
            }),
            new Spawn("target_lightramp", new EntityThinkAdapter() {
        public String getID(){ return "SP_target_lightramp"; }
                public boolean think(Entity ent) {
                    GameTarget.SP_target_lightramp(ent);
                    return true;
                }
            }),
            new Spawn("target_earthquake", new EntityThinkAdapter() {
        public String getID(){ return "SP_target_earthquake"; }
                public boolean think(Entity ent) {
                    GameTarget.SP_target_earthquake(ent);
                    return true;
                }
            }),
            new Spawn("target_character", new EntityThinkAdapter() {
        public String getID(){ return "SP_target_character"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_target_character(ent);
                    return true;
                }
            }),
            new Spawn("target_string", new EntityThinkAdapter() {
        public String getID(){ return "SP_target_string"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_target_string(ent);
                    return true;
                }
            }),
            new Spawn("worldspawn", SP_worldspawn),
            new Spawn("viewthing", new EntityThinkAdapter() {
        public String getID(){ return "SP_viewthing"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_viewthing(ent);
                    return true;
                }
            }),
            new Spawn("light", new EntityThinkAdapter() {
        public String getID(){ return "SP_light"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_light(ent);
                    return true;
                }
            }),
            new Spawn("light_mine1", new EntityThinkAdapter() {
        public String getID(){ return "SP_light_mine1"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_light_mine1(ent);
                    return true;
                }
            }),
            new Spawn("light_mine2", new EntityThinkAdapter() {
        public String getID(){ return "SP_light_mine2"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_light_mine2(ent);
                    return true;
                }
            }),
            new Spawn("info_null", new EntityThinkAdapter() {
        public String getID(){ return "SP_info_null"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_info_null(ent);
                    return true;
                }
            }),
            new Spawn("func_group", new EntityThinkAdapter() {
        public String getID(){ return "SP_info_null"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_info_null(ent);
                    return true;
                }
            }),
            new Spawn("info_notnull", new EntityThinkAdapter() {
        public String getID(){ return "info_notnull"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_info_notnull(ent);
                    return true;
                }
            }),
            new Spawn("path_corner", new EntityThinkAdapter() {
        public String getID(){ return "SP_path_corner"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_path_corner(ent);
                    return true;
                }
            }),
            new Spawn("point_combat", new EntityThinkAdapter() {
        public String getID(){ return "SP_point_combat"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_point_combat(ent);
                    return true;
                }
            }),
            new Spawn("misc_explobox", new EntityThinkAdapter() {
        public String getID(){ return "SP_misc_explobox"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_misc_explobox(ent);
                    return true;
                }
            }),
            new Spawn("misc_banner", new EntityThinkAdapter() {
        public String getID(){ return "SP_misc_banner"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_misc_banner(ent);
                    return true;
                }
            }),
            new Spawn("misc_satellite_dish", new EntityThinkAdapter() {
        public String getID(){ return "SP_misc_satellite_dish"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_misc_satellite_dish(ent);
                    return true;
                }
            }),
            new Spawn("misc_actor", new EntityThinkAdapter() {
        public String getID(){ return "SP_misc_actor"; }
                public boolean think(Entity ent) {
                    MonsterActor.SP_misc_actor(ent);
                    return false;
                }
            }),
            new Spawn("misc_gib_arm", new EntityThinkAdapter() {
        public String getID(){ return "SP_misc_gib_arm"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_misc_gib_arm(ent);
                    return true;
                }
            }),
            new Spawn("misc_gib_leg", new EntityThinkAdapter() {
        public String getID(){ return "SP_misc_gib_leg"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_misc_gib_leg(ent);
                    return true;
                }
            }),
            new Spawn("misc_gib_head", new EntityThinkAdapter() {
        public String getID(){ return "SP_misc_gib_head"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_misc_gib_head(ent);
                    return true;
                }
            }),
            new Spawn("misc_insane", new EntityThinkAdapter() {
        public String getID(){ return "SP_misc_insane"; }
                public boolean think(Entity ent) {
                    MonsterInsane.SP_misc_insane(ent);
                    return true;
                }
            }),
            new Spawn("misc_deadsoldier", new EntityThinkAdapter() {
        public String getID(){ return "SP_misc_deadsoldier"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_misc_deadsoldier(ent);
                    return true;
                }
            }),
            new Spawn("misc_viper", new EntityThinkAdapter() {
        public String getID(){ return "SP_misc_viper"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_misc_viper(ent);
                    return true;
                }
            }),
            new Spawn("misc_viper_bomb", new EntityThinkAdapter() {
        public String getID(){ return "SP_misc_viper_bomb"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_misc_viper_bomb(ent);
                    return true;
                }
            }),
            new Spawn("misc_bigviper", new EntityThinkAdapter() {
        public String getID(){ return "SP_misc_bigviper"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_misc_bigviper(ent);
                    return true;
                }
            }),
            new Spawn("misc_strogg_ship", new EntityThinkAdapter() {
        public String getID(){ return "SP_misc_strogg_ship"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_misc_strogg_ship(ent);
                    return true;
                }
            }),
            new Spawn("misc_teleporter", new EntityThinkAdapter() {
        public String getID(){ return "SP_misc_teleporter"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_misc_teleporter(ent);
                    return true;
                }
            }),
            new Spawn("misc_teleporter_dest",
                    GameMisc.SP_misc_teleporter_dest),
            new Spawn("misc_blackhole", new EntityThinkAdapter() {
        public String getID(){ return "SP_misc_blackhole"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_misc_blackhole(ent);
                    return true;
                }
            }),
            new Spawn("misc_eastertank", new EntityThinkAdapter() {
        public String getID(){ return "SP_misc_eastertank"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_misc_eastertank(ent);
                    return true;
                }
            }),
            new Spawn("misc_easterchick", new EntityThinkAdapter() {
        public String getID(){ return "SP_misc_easterchick"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_misc_easterchick(ent);
                    return true;
                }
            }),
            new Spawn("misc_easterchick2", new EntityThinkAdapter() {
        public String getID(){ return "SP_misc_easterchick2"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_misc_easterchick2(ent);
                    return true;
                }
            }),
            new Spawn("monster_berserk", new EntityThinkAdapter() {
        public String getID(){ return "SP_monster_berserk"; }
                public boolean think(Entity ent) {
                    MonsterBerserk.SP_monster_berserk(ent);
                    return true;
                }
            }),
            new Spawn("monster_gladiator", new EntityThinkAdapter() {
        public String getID(){ return "SP_monster_gladiator"; }
                public boolean think(Entity ent) {
                    MonsterGladiator.SP_monster_gladiator(ent);
                    return true;
                }
            }),
            new Spawn("monster_gunner", new EntityThinkAdapter() {
        public String getID(){ return "SP_monster_gunner"; }
                public boolean think(Entity ent) {
                    MonsterGunner.SP_monster_gunner(ent);
                    return true;
                }
            }),
            new Spawn("monster_infantry", new EntityThinkAdapter() {
        public String getID(){ return "SP_monster_infantry"; }
                public boolean think(Entity ent) {
                    MonsterInfantry.SP_monster_infantry(ent);
                    return true;
                }
            }),
            new Spawn("monster_soldier_light",
                    MonsterSoldier.SP_monster_soldier_light),
            new Spawn("monster_soldier", MonsterSoldier.SP_monster_soldier),
            new Spawn("monster_soldier_ss", MonsterSoldier.SP_monster_soldier_ss),
            new Spawn("monster_tank", MonsterTank.SP_monster_tank),
            new Spawn("monster_tank_commander", MonsterTank.SP_monster_tank),
            new Spawn("monster_medic", new EntityThinkAdapter() {
        public String getID(){ return "SP_monster_medic"; }
                public boolean think(Entity ent) {
                    MonsterMedic.SP_monster_medic(ent);
                    return true;
                }
            }), new Spawn("monster_flipper", new EntityThinkAdapter() {
        public String getID(){ return "SP_monster_flipper"; }
                public boolean think(Entity ent) {
                    MonsterFlipper.SP_monster_flipper(ent);
                    return true;
                }
            }), new Spawn("monster_chick", new EntityThinkAdapter() {
        public String getID(){ return "SP_monster_chick"; }
                public boolean think(Entity ent) {
                    MonsterChick.SP_monster_chick(ent);
                    return true;
                }
            }),
            new Spawn("monster_parasite", MonsterParasite.SP_monster_parasite),
            new Spawn("monster_flyer", new EntityThinkAdapter() {
        public String getID(){ return "SP_monster_flyer"; }
                public boolean think(Entity ent) {
                    MonsterFlyer.SP_monster_flyer(ent);
                    return true;
                }
            }), new Spawn("monster_brain", new EntityThinkAdapter() {
        public String getID(){ return "SP_monster_brain"; }
                public boolean think(Entity ent) {
                    MonsterBrain.SP_monster_brain(ent);
                    return true;
                }
            }), new Spawn("monster_floater", new EntityThinkAdapter() {
        public String getID(){ return "SP_monster_floater"; }
                public boolean think(Entity ent) {
                    MonsterFloat.SP_monster_floater(ent);
                    return true;
                }
            }), new Spawn("monster_hover", new EntityThinkAdapter() {
        public String getID(){ return "SP_monster_hover"; }
                public boolean think(Entity ent) {
                    MonsterHover.SP_monster_hover(ent);
                    return true;
                }
            }), new Spawn("monster_mutant", MonsterMutant.SP_monster_mutant),
            new Spawn("monster_supertank", MonsterSupertank.SP_monster_supertank),
            new Spawn("monster_boss2", new EntityThinkAdapter() {
        public String getID(){ return "SP_monster_boss2"; }
                public boolean think(Entity ent) {
                    MonsterBoss2.SP_monster_boss2(ent);
                    return true;
                }
            }), new Spawn("monster_boss3_stand", new EntityThinkAdapter() {
        public String getID(){ return "SP_monster_boss3_stand"; }
                public boolean think(Entity ent) {
                    MonsterBoss3.SP_monster_boss3_stand(ent);
                    return true;
                }
            }), new Spawn("monster_jorg", new EntityThinkAdapter() {
        public String getID(){ return "SP_monster_jorg"; }
                public boolean think(Entity ent) {
                    MonsterBoss31.SP_monster_jorg(ent);
                    return true;
                }
            }), new Spawn("monster_commander_body", new EntityThinkAdapter() {
        public String getID(){ return "SP_monster_commander_body"; }
                public boolean think(Entity ent) {
                    GameMisc.SP_monster_commander_body(ent);
                    return true;
                }
            }), new Spawn("turret_breach", new EntityThinkAdapter() {
        public String getID(){ return "SP_turret_breach"; }
                public boolean think(Entity ent) {
                    GameTurret.SP_turret_breach(ent);
                    return true;
                }
            }), new Spawn("turret_base", new EntityThinkAdapter() {
        public String getID(){ return "SP_turret_base"; }
                public boolean think(Entity ent) {
                    GameTurret.SP_turret_base(ent);
                    return true;
                }
            }), new Spawn("turret_driver", new EntityThinkAdapter() {
        public String getID(){ return "SP_turret_driver"; }
                public boolean think(Entity ent) {
                    GameTurret.SP_turret_driver(ent);
                    return true;
                }
            }), new Spawn(null, null) };

    /**
     * ED_CallSpawn
     * 
     * Finds the spawn function for the entity and calls it.
     */
    public static void ED_CallSpawn(Entity ent) {

        Spawn s;
        GameItem item;
        int i;
        if (null == ent.classname) {
            ServerGame.PF_dprintf("ED_CallSpawn: null classname\n");
            return;
        } // check item spawn functions
        for (i = 1; i < GameBase.game.num_items; i++) {

            item = GameItemList.itemlist[i];

            if (item == null)
              Com.Error(Constants.ERR_FATAL, "ED_CallSpawn: null item in pos " + i);

            if (item.classname == null)
                continue;
            if (item.classname.equalsIgnoreCase(ent.classname)) { // found it
                GameItems.SpawnItem(ent, item);
                return;
            }
        } // check normal spawn functions

        for (i = 0; (s = spawns[i]) != null && s.name != null; i++) {
            if (s.name.equalsIgnoreCase(ent.classname)) { // found it

                if (s.spawn == null)
                  Com.Error(Constants.ERR_FATAL, "ED_CallSpawn: null-spawn on index=" + i);
                s.spawn.think(ent);
                return;
            }
        }
        ServerGame.PF_dprintf(ent.classname + " doesn't have a spawn function\n");
    }
}

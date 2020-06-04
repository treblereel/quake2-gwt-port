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

import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.Globals;
import com.googlecode.gwtquake.shared.game.adapters.EntityThinkAdapter;
import com.googlecode.gwtquake.shared.game.adapters.EntityUseAdapter;
import com.googlecode.gwtquake.shared.server.ServerGame;
import com.googlecode.gwtquake.shared.server.ServerInit;
import com.googlecode.gwtquake.shared.server.ServerSend;
import com.googlecode.gwtquake.shared.server.World;
import com.googlecode.gwtquake.shared.util.Lib;
import com.googlecode.gwtquake.shared.util.Math3D;


public class GameTarget {

    public static void SP_target_temp_entity(Entity ent) {
        ent.use = Use_Target_Tent;
    }

    public static void SP_target_speaker(Entity ent) {
        //char buffer[MAX_QPATH];
        String buffer;

        if (GameBase.st.noise == null) {
            ServerGame.PF_dprintf("target_speaker with no noise set at "
            + Lib.vtos(ent.s.origin) + "\n");
            return;
        }
        if (GameBase.st.noise.indexOf(".wav") < 0)
            buffer = "" + GameBase.st.noise + ".wav";
        else
            buffer = GameBase.st.noise;

        ent.noise_index = ServerInit.SV_SoundIndex(buffer);

        if (ent.volume == 0)
            ent.volume = 1.0f;

        if (ent.attenuation == 0)
            ent.attenuation = 1.0f;
        else if (ent.attenuation == -1) // use -1 so 0 defaults to 1
            ent.attenuation = 0;

        // check for prestarted looping sound
        if ((ent.spawnflags & 1) != 0)
            ent.s.sound = ent.noise_index;

        ent.use = Use_Target_Speaker;

        // must link the entity so we get areas and clusters so
        // the server can determine who to send updates to
        World.SV_LinkEdict(ent);
    }

    /**
     * QUAKED target_help (1 0 1) (-16 -16 -24) (16 16 24) help1 When fired, the
     * "message" key becomes the current personal computer string, and the
     * message light will be set on all clients status bars.
     */
    public static void SP_target_help(Entity ent) {
        if (GameBase.deathmatch.value != 0) { // auto-remove for deathmatch
            GameUtil.G_FreeEdict(ent);
            return;
        }

        if (ent.message == null) {
            ServerGame.PF_dprintf(ent.classname + " with no message at "
            + Lib.vtos(ent.s.origin) + "\n");
            GameUtil.G_FreeEdict(ent);
            return;
        }
        ent.use = Use_Target_Help;
    }

    public static void SP_target_secret(Entity ent) {
        if (GameBase.deathmatch.value != 0) { // auto-remove for deathmatch
            GameUtil.G_FreeEdict(ent);
            return;
        }

        ent.use = use_target_secret;
        if (GameBase.st.noise == null)
            GameBase.st.noise = "misc/secret.wav";
        ent.noise_index = ServerInit.SV_SoundIndex(GameBase.st.noise);
        ent.svflags = Constants.SVF_NOCLIENT;
        GameBase.level.total_secrets++;
        // map bug hack
        if (0 == Lib.Q_stricmp(GameBase.level.mapname, "mine3")
                && ent.s.origin[0] == 280 && ent.s.origin[1] == -2048
                && ent.s.origin[2] == -624)
            ent.message = "You have found a secret area.";
    }

    public static void SP_target_goal(Entity ent) {
        if (GameBase.deathmatch.value != 0) { // auto-remove for deathmatch
            GameUtil.G_FreeEdict(ent);
            return;
        }

        ent.use = use_target_goal;
        if (GameBase.st.noise == null)
            GameBase.st.noise = "misc/secret.wav";
        ent.noise_index = ServerInit.SV_SoundIndex(GameBase.st.noise);
        ent.svflags = Constants.SVF_NOCLIENT;
        GameBase.level.total_goals++;
    }

    public static void SP_target_explosion(Entity ent) {
        ent.use = use_target_explosion;
        ent.svflags = Constants.SVF_NOCLIENT;
    }

    public static void SP_target_changelevel(Entity ent) {
        if (ent.map == null) {
            ServerGame.PF_dprintf("target_changelevel with no map at "
            + Lib.vtos(ent.s.origin) + "\n");
            GameUtil.G_FreeEdict(ent);
            return;
        }

        // ugly hack because *SOMEBODY* screwed up their map
        if ((Lib.Q_stricmp(GameBase.level.mapname, "fact1") == 0)
                && (Lib.Q_stricmp(ent.map, "fact3") == 0))
            ent.map = "fact3$secret1";

        ent.use = use_target_changelevel;
        ent.svflags = Constants.SVF_NOCLIENT;
    }

    public static void SP_target_splash(Entity self) {
        self.use = use_target_splash;
        GameBase.G_SetMovedir(self.s.angles, self.movedir);

        if (0 == self.count)
            self.count = 32;

        self.svflags = Constants.SVF_NOCLIENT;
    }

    public static void SP_target_spawner(Entity self) {
        self.use = use_target_spawner;
        self.svflags = Constants.SVF_NOCLIENT;
        if (self.speed != 0) {
            GameBase.G_SetMovedir(self.s.angles, self.movedir);
            Math3D.VectorScale(self.movedir, self.speed, self.movedir);
        }
    }

    public static void SP_target_blaster(Entity self) {
        self.use = use_target_blaster;
        GameBase.G_SetMovedir(self.s.angles, self.movedir);
        self.noise_index = ServerInit.SV_SoundIndex("weapons/laser2.wav");

        if (0 == self.dmg)
            self.dmg = 15;
        if (0 == self.speed)
            self.speed = 1000;

        self.svflags = Constants.SVF_NOCLIENT;
    }

    public static void SP_target_crosslevel_trigger(Entity self) {
        self.svflags = Constants.SVF_NOCLIENT;
        self.use = trigger_crosslevel_trigger_use;
    }

    public static void SP_target_crosslevel_target(Entity self) {
        if (0 == self.delay)
            self.delay = 1;
        self.svflags = Constants.SVF_NOCLIENT;

        self.think = target_crosslevel_target_think;
        self.nextthink = GameBase.level.time + self.delay;
    }

    public static void target_laser_on(Entity self) {
        if (null == self.activator)
            self.activator = self;
        self.spawnflags |= 0x80000001;
        self.svflags &= ~Constants.SVF_NOCLIENT;
        target_laser_think.think(self);
    }

    public static void target_laser_off(Entity self) {
        self.spawnflags &= ~1;
        self.svflags |= Constants.SVF_NOCLIENT;
        self.nextthink = 0;
    }

    public static void SP_target_laser(Entity self) {
        // let everything else get spawned before we start firing
        self.think = target_laser_start;
        self.nextthink = GameBase.level.time + 1;
    }

    public static void SP_target_lightramp(Entity self) {
        if (self.message == null || self.message.length() != 2
                || self.message.charAt(0) < 'a' || self.message.charAt(0) > 'z'
                || self.message.charAt(1) < 'a' || self.message.charAt(1) > 'z'
                || self.message.charAt(0) == self.message.charAt(1)) {
            ServerGame.PF_dprintf("target_lightramp has bad ramp ("
            + self.message + ") at " + Lib.vtos(self.s.origin) + "\n");
            GameUtil.G_FreeEdict(self);
            return;
        }

        if (GameBase.deathmatch.value != 0) {
            GameUtil.G_FreeEdict(self);
            return;
        }

        if (self.target == null) {
            ServerGame.PF_dprintf(self.classname + " with no target at "
            + Lib.vtos(self.s.origin) + "\n");
            GameUtil.G_FreeEdict(self);
            return;
        }

        self.svflags |= Constants.SVF_NOCLIENT;
        self.use = target_lightramp_use;
        self.think = target_lightramp_think;

        self.movedir[0] = self.message.charAt(0) - 'a';
        self.movedir[1] = self.message.charAt(1) - 'a';
        self.movedir[2] = (self.movedir[1] - self.movedir[0])
                / (self.speed / Constants.FRAMETIME);
    }

    public static void SP_target_earthquake(Entity self) {
        if (null == self.targetname)
          ServerGame.PF_dprintf("untargeted " + self.classname + " at "
          + Lib.vtos(self.s.origin) + "\n");

        if (0 == self.count)
            self.count = 5;

        if (0 == self.speed)
            self.speed = 200;

        self.svflags |= Constants.SVF_NOCLIENT;
        self.think = target_earthquake_think;
        self.use = target_earthquake_use;

        self.noise_index = ServerInit.SV_SoundIndex("world/quake.wav");
    }

    /**
     * QUAKED target_temp_entity (1 0 0) (-8 -8 -8) (8 8 8) Fire an origin based
     * temp entity event to the clients. "style" type byte
     */
    public static EntityUseAdapter Use_Target_Tent = new EntityUseAdapter() {
    	public String getID() { return "Use_Target_Tent"; }
        public void use(Entity ent, Entity other, Entity activator) {
            ServerGame.PF_WriteByte(Constants.svc_temp_entity);
            ServerGame.PF_WriteByte(ent.style);
            ServerGame.PF_WritePos(ent.s.origin);
            ServerSend.SV_Multicast(ent.s.origin, Constants.MULTICAST_PVS);
        }
    };

    /**
     * QUAKED target_speaker (1 0 0) (-8 -8 -8) (8 8 8) looped-on looped-off
     * reliable "noise" wav file to play "attenuation" -1 = none, send to whole
     * level 1 = normal fighting sounds 2 = idle sound level 3 = ambient sound
     * level "volume" 0.0 to 1.0
     * 
     * Normal sounds play each time the target is used. The reliable flag can be
     * set for crucial voiceovers.
     * 
     * Looped sounds are always atten 3 / vol 1, and the use function toggles it
     * on/off. Multiple identical looping sounds will just increase volume
     * without any speed cost.
     */
    public static EntityUseAdapter Use_Target_Speaker = new EntityUseAdapter() {
    	public String getID() { return "Use_Target_Speaker"; }
        public void use(Entity ent, Entity other, Entity activator) {
            int chan;

            if ((ent.spawnflags & 3) != 0) { // looping sound toggles
                if (ent.s.sound != 0)
                    ent.s.sound = 0; // turn it off
                else
                    ent.s.sound = ent.noise_index; // start it
            } else { // normal sound
                if ((ent.spawnflags & 4) != 0)
                    chan = Constants.CHAN_VOICE | Constants.CHAN_RELIABLE;
                else
                    chan = Constants.CHAN_VOICE;
                // use a positioned_sound, because this entity won't normally be
                // sent to any clients because it is invisible
                ServerSend.SV_StartSound(ent.s.origin, ent, chan, ent.noise_index, ent.volume,
                ent.attenuation, (float) 0);
            }

        }
    };


    public static EntityUseAdapter Use_Target_Help = new EntityUseAdapter() {
    	public String getID() { return "Use_Target_Help"; }
        public void use(Entity ent, Entity other, Entity activator) {

            if ((ent.spawnflags & 1) != 0)
                GameBase.game.helpmessage1 = ent.message;
            else
                GameBase.game.helpmessage2 = ent.message;

            GameBase.game.helpchanged++;
        }
    };

    /**
     * QUAKED target_secret (1 0 1) (-8 -8 -8) (8 8 8) Counts a secret found.
     * These are single use targets.
     */
    static EntityUseAdapter use_target_secret = new EntityUseAdapter() {
    	public String getID() { return "use_target_secret"; }
        public void use(Entity ent, Entity other, Entity activator) {
            ServerGame.PF_StartSound(ent, Constants.CHAN_VOICE, ent.noise_index, (float) 1, (float) Constants.ATTN_NORM,
            (float) 0);

            GameBase.level.found_secrets++;

            GameUtil.G_UseTargets(ent, activator);
            GameUtil.G_FreeEdict(ent);
        }
    };
    
    /**
     * QUAKED target_goal (1 0 1) (-8 -8 -8) (8 8 8) Counts a goal completed.
     * These are single use targets.
     */
    static EntityUseAdapter use_target_goal = new EntityUseAdapter() {
    	public String getID() { return "use_target_goal"; }
        public void use(Entity ent, Entity other, Entity activator) {
            ServerGame.PF_StartSound(ent, Constants.CHAN_VOICE, ent.noise_index, (float) 1, (float) Constants.ATTN_NORM,
            (float) 0);

            GameBase.level.found_goals++;

            if (GameBase.level.found_goals == GameBase.level.total_goals)
              ServerGame.PF_Configstring(Constants.CS_CDTRACK, "0");

            GameUtil.G_UseTargets(ent, activator);
            GameUtil.G_FreeEdict(ent);
        }
    };


    /**
     * QUAKED target_explosion (1 0 0) (-8 -8 -8) (8 8 8) Spawns an explosion
     * temporary entity when used.
     * 
     * "delay" wait this long before going off "dmg" how much radius damage
     * should be done, defaults to 0
     */
    static EntityThinkAdapter target_explosion_explode = new EntityThinkAdapter() {
    	public String getID() { return "target_explosion_explode"; }
        public boolean think(Entity self) {

            float save;

            ServerGame.PF_WriteByte(Constants.svc_temp_entity);
            ServerGame.PF_WriteByte(Constants.TE_EXPLOSION1);
            ServerGame.PF_WritePos(self.s.origin);
            ServerSend.SV_Multicast(self.s.origin, Constants.MULTICAST_PHS);

            GameCombat.T_RadiusDamage(self, self.activator, self.dmg, null,
                    self.dmg + 40, Constants.MOD_EXPLOSIVE);

            save = self.delay;
            self.delay = 0;
            GameUtil.G_UseTargets(self, self.activator);
            self.delay = save;
            return true;
        }
    };

    static EntityUseAdapter use_target_explosion = new EntityUseAdapter() {
    	public String getID() { return "use_target_explosion"; }
        public void use(Entity self, Entity other, Entity activator) {
            self.activator = activator;

            if (0 == self.delay) {
                target_explosion_explode.think(self);
                return;
            }

            self.think = target_explosion_explode;
            self.nextthink = GameBase.level.time + self.delay;
        }
    };

    /**
     * QUAKED target_changelevel (1 0 0) (-8 -8 -8) (8 8 8) Changes level to
     * "map" when fired
     */
    static EntityUseAdapter use_target_changelevel = new EntityUseAdapter() {
    	public String getID() { return "use_target_changelevel"; }
        public void use(Entity self, Entity other, Entity activator) {
            if (GameBase.level.intermissiontime != 0)
                return; // already activated

            if (0 == GameBase.deathmatch.value && 0 == GameBase.coop.value) {
                if (GameBase.g_edicts[1].health <= 0)
                    return;
            }

            // if noexit, do a ton of damage to other
            if (GameBase.deathmatch.value != 0
                    && 0 == ((int) GameBase.dmflags.value & Constants.DF_ALLOW_EXIT)
                    && other != GameBase.g_edicts[0] /* world */
            ) {
                GameCombat.T_Damage(other, self, self, Globals.vec3_origin,
                        other.s.origin, Globals.vec3_origin,
                        10 * other.max_health, 1000, 0, Constants.MOD_EXIT);
                return;
            }

            // if multiplayer, let everyone know who hit the exit
            if (GameBase.deathmatch.value != 0) {
                if (activator != null && activator.client != null)
                  ServerSend.SV_BroadcastPrintf(Constants.PRINT_HIGH, activator.client.pers.netname
                  + " exited the level.\n");
            }

            // if going to a new unit, clear cross triggers
            if (self.map.indexOf('*') > -1)
                GameBase.game.serverflags &= ~(Constants.SFL_CROSS_TRIGGER_MASK);

            PlayerHud.BeginIntermission(self);
        }
    };

    /**
     * QUAKED target_splash (1 0 0) (-8 -8 -8) (8 8 8) Creates a particle splash
     * effect when used.
     * 
     * Set "sounds" to one of the following: 1) sparks 2) blue water 3) brown
     * water 4) slime 5) lava 6) blood
     * 
     * "count" how many pixels in the splash "dmg" if set, does a radius damage
     * at this location when it splashes useful for lava/sparks
     */
    static EntityUseAdapter use_target_splash = new EntityUseAdapter() {
    	public String getID() { return "use_target_splash"; }
        public void use(Entity self, Entity other, Entity activator) {
            ServerGame.PF_WriteByte(Constants.svc_temp_entity);
            ServerGame.PF_WriteByte(Constants.TE_SPLASH);
            ServerGame.PF_WriteByte(self.count);
            ServerGame.PF_WritePos(self.s.origin);
            ServerGame.PF_WriteDir(self.movedir);
            ServerGame.PF_WriteByte(self.sounds);
            ServerSend.SV_Multicast(self.s.origin, Constants.MULTICAST_PVS);

            if (self.dmg != 0)
                GameCombat.T_RadiusDamage(self, activator, self.dmg, null,
                        self.dmg + 40, Constants.MOD_SPLASH);
        }
    };

    /**
     * QUAKED target_spawner (1 0 0) (-8 -8 -8) (8 8 8) Set target to the type
     * of entity you want spawned. Useful for spawning monsters and gibs in the
     * factory levels.
     * 
     * For monsters: Set direction to the facing you want it to have.
     * 
     * For gibs: Set direction if you want it moving and speed how fast it
     * should be moving otherwise it will just be dropped
     */

    static EntityUseAdapter use_target_spawner = new EntityUseAdapter() {
    	public String getID() { return "use_target_spawner"; }
        public void use(Entity self, Entity other, Entity activator) {
            Entity ent;

            ent = GameUtil.G_Spawn();
            ent.classname = self.target;
            Math3D.VectorCopy(self.s.origin, ent.s.origin);
            Math3D.VectorCopy(self.s.angles, ent.s.angles);
            GameSpawn.ED_CallSpawn(ent);
            World.SV_UnlinkEdict(ent);
            GameUtil.KillBox(ent);
            World.SV_LinkEdict(ent);
            if (self.speed != 0)
                Math3D.VectorCopy(self.movedir, ent.velocity);
        }
    };

    /**
     * QUAKED target_blaster (1 0 0) (-8 -8 -8) (8 8 8) NOTRAIL NOEFFECTS Fires
     * a blaster bolt in the set direction when triggered.
     * 
     * dmg default is 15 speed default is 1000
     */
    public static EntityUseAdapter use_target_blaster = new EntityUseAdapter() {
    	public String getID() { return "use_target_blaster"; }
        public void use(Entity self, Entity other, Entity activator) {
            int effect;

            if ((self.spawnflags & 2) != 0)
                effect = 0;
            else if ((self.spawnflags & 1) != 0)
                effect = Constants.EF_HYPERBLASTER;
            else
                effect = Constants.EF_BLASTER;

            GameWeapon.fire_blaster(self, self.s.origin, self.movedir, self.dmg,
                    (int) self.speed, Constants.EF_BLASTER,
                    Constants.MOD_TARGET_BLASTER != 0
            /* true */
            );
            ServerGame.PF_StartSound(self, Constants.CHAN_VOICE, self.noise_index, (float) 1, (float) Constants.ATTN_NORM,
            (float) 0);
        }
    };

    /**
     * QUAKED target_crosslevel_trigger (.5 .5 .5) (-8 -8 -8) (8 8 8) trigger1
     * trigger2 trigger3 trigger4 trigger5 trigger6 trigger7 trigger8 Once this
     * trigger is touched/used, any trigger_crosslevel_target with the same
     * trigger number is automatically used when a level is started within the
     * same unit. It is OK to check multiple triggers. Message, delay, target,
     * and killtarget also work.
     */
    public static EntityUseAdapter trigger_crosslevel_trigger_use = new EntityUseAdapter() {
    	public String getID() { return "trigger_crosslevel_trigger_use"; }
        public void use(Entity self, Entity other, Entity activator) {
            GameBase.game.serverflags |= self.spawnflags;
            GameUtil.G_FreeEdict(self);
        }
    };

    /**
     * QUAKED target_crosslevel_target (.5 .5 .5) (-8 -8 -8) (8 8 8) trigger1
     * trigger2 trigger3 trigger4 trigger5 trigger6 trigger7 trigger8 Triggered
     * by a trigger_crosslevel elsewhere within a unit. If multiple triggers are
     * checked, all must be true. Delay, target and killtarget also work.
     * 
     * "delay" delay before using targets if the trigger has been activated
     * (default 1)
     */
    static EntityThinkAdapter target_crosslevel_target_think = new EntityThinkAdapter() {
    	public String getID() { return "target_crosslevel_target_think"; }
        public boolean think(Entity self) {
            if (self.spawnflags == (GameBase.game.serverflags
                    & Constants.SFL_CROSS_TRIGGER_MASK & self.spawnflags)) {
                GameUtil.G_UseTargets(self, self);
                GameUtil.G_FreeEdict(self);
            }
            return true;
        }
    };

    /**
     * QUAKED target_laser (0 .5 .8) (-8 -8 -8) (8 8 8) START_ON RED GREEN BLUE
     * YELLOW ORANGE FAT When triggered, fires a laser. You can either set a
     * target or a direction.
     */
    public static EntityThinkAdapter target_laser_think = new EntityThinkAdapter() {
    	public String getID() { return "target_laser_think"; }
        public boolean think(Entity self) {

            Entity ignore;
            float[] start = { 0, 0, 0 };
            float[] end = { 0, 0, 0 };
            Trace tr;
            float[] point = { 0, 0, 0 };
            float[] last_movedir = { 0, 0, 0 };
            int count;

            if ((self.spawnflags & 0x80000000) != 0)
                count = 8;
            else
                count = 4;

            if (self.enemy != null) {
                Math3D.VectorCopy(self.movedir, last_movedir);
                Math3D.VectorMA(self.enemy.absmin, 0.5f, self.enemy.size, point);
                Math3D.VectorSubtract(point, self.s.origin, self.movedir);
                Math3D.VectorNormalize(self.movedir);
                if (!Math3D.VectorEquals(self.movedir, last_movedir))
                    self.spawnflags |= 0x80000000;
            }

            ignore = self;
            Math3D.VectorCopy(self.s.origin, start);
            Math3D.VectorMA(start, 2048, self.movedir, end);
            while (true) {
                tr = World.SV_Trace(start, null, null, end, ignore, Constants.CONTENTS_SOLID | Constants.CONTENTS_MONSTER
                | Constants.CONTENTS_DEADMONSTER);

                if (tr.ent == null)
                    break;

                // hurt it if we can
                if ((tr.ent.takedamage != 0)
                        && 0 == (tr.ent.flags & Constants.FL_IMMUNE_LASER))
                    GameCombat.T_Damage(tr.ent, self, self.activator,
                            self.movedir, tr.endpos, Globals.vec3_origin,
                            self.dmg, 1, Constants.DAMAGE_ENERGY,
                            Constants.MOD_TARGET_LASER);

                // if we hit something that's not a monster or player or is
                // immune to lasers, we're done
                if (0 == (tr.ent.svflags & Constants.SVF_MONSTER)
                        && (null == tr.ent.client)) {
                    if ((self.spawnflags & 0x80000000) != 0) {
                        self.spawnflags &= ~0x80000000;
                        ServerGame.PF_WriteByte(Constants.svc_temp_entity);
                        ServerGame.PF_WriteByte(Constants.TE_LASER_SPARKS);
                        ServerGame.PF_WriteByte(count);
                        ServerGame.PF_WritePos(tr.endpos);
                        ServerGame.PF_WriteDir(tr.plane.normal);
                        ServerGame.PF_WriteByte(self.s.skinnum);
                        ServerSend.SV_Multicast(tr.endpos, Constants.MULTICAST_PVS);
                    }
                    break;
                }

                ignore = tr.ent;
                Math3D.VectorCopy(tr.endpos, start);
            }

            Math3D.VectorCopy(tr.endpos, self.s.old_origin);

            self.nextthink = GameBase.level.time + Constants.FRAMETIME;
            return true;
        }
    };

    public static EntityUseAdapter target_laser_use = new EntityUseAdapter() {
    	public String getID() { return "target_laser_use"; }

        public void use(Entity self, Entity other, Entity activator) {
            self.activator = activator;
            if ((self.spawnflags & 1) != 0)
                target_laser_off(self);
            else
                target_laser_on(self);
        }
    };

    static EntityThinkAdapter target_laser_start = new EntityThinkAdapter() {
    	public String getID() { return "target_laser_start"; }
        public boolean think(Entity self) {

            self.movetype = Constants.MOVETYPE_NONE;
            self.solid = Constants.SOLID_NOT;
            self.s.renderfx |= Constants.RF_BEAM | Constants.RF_TRANSLUCENT;
            self.s.modelindex = 1; // must be non-zero

            // set the beam diameter
            if ((self.spawnflags & 64) != 0)
                self.s.frame = 16;
            else
                self.s.frame = 4;

            // set the color
            if ((self.spawnflags & 2) != 0)
                self.s.skinnum = 0xf2f2f0f0;
            else if ((self.spawnflags & 4) != 0)
                self.s.skinnum = 0xd0d1d2d3;
            else if ((self.spawnflags & 8) != 0)
                self.s.skinnum = 0xf3f3f1f1;
            else if ((self.spawnflags & 16) != 0)
                self.s.skinnum = 0xdcdddedf;
            else if ((self.spawnflags & 32) != 0)
                self.s.skinnum = 0xe0e1e2e3;

            if (null == self.enemy) {
                if (self.target != null) {
                    EntityIterator edit = GameBase.G_Find(null, GameBase.findByTarget,
                            self.target);
                    if (edit == null)
                      ServerGame.PF_dprintf(self.classname + " at "
                      + Lib.vtos(self.s.origin) + ": " + self.target
                      + " is a bad target\n");
                    self.enemy = edit.o;
                } else {
                    GameBase.G_SetMovedir(self.s.angles, self.movedir);
                }
            }
            self.use = target_laser_use;
            self.think = target_laser_think;

            if (0 == self.dmg)
                self.dmg = 1;

            Math3D.VectorSet(self.mins, -8, -8, -8);
            Math3D.VectorSet(self.maxs, 8, 8, 8);
            World.SV_LinkEdict(self);

            if ((self.spawnflags & 1) != 0)
                target_laser_on(self);
            else
                target_laser_off(self);
            return true;
        }
    };

    /**
     * QUAKED target_lightramp (0 .5 .8) (-8 -8 -8) (8 8 8) TOGGLE speed How
     * many seconds the ramping will take message two letters; starting
     * lightlevel and ending lightlevel
     */

    static EntityThinkAdapter target_lightramp_think = new EntityThinkAdapter() {
    	public String getID() { return "target_lightramp_think"; }
        public boolean think(Entity self) {

            char tmp[] = {(char) ('a' + (int) (self.movedir[0] + (GameBase.level.time - self.timestamp)
                    / Constants.FRAMETIME * self.movedir[2]))};
            
            ServerGame.PF_Configstring(Constants.CS_LIGHTS + self.enemy.style, new String(tmp));

            if ((GameBase.level.time - self.timestamp) < self.speed) {
                self.nextthink = GameBase.level.time + Constants.FRAMETIME;
            } else if ((self.spawnflags & 1) != 0) {
                char temp;

                temp = (char) self.movedir[0];
                self.movedir[0] = self.movedir[1];
                self.movedir[1] = temp;
                self.movedir[2] *= -1;
            }

            return true;
        }
    };

    static EntityUseAdapter target_lightramp_use = new EntityUseAdapter() {
    	public String getID() { return "target_lightramp_use"; }
        public void use(Entity self, Entity other, Entity activator) {
            if (self.enemy == null) {
                Entity e;

                // check all the targets
                e = null;
                EntityIterator es = null;

                while (true) {
                    es = GameBase
                            .G_Find(es, GameBase.findByTarget, self.target);
                    
                    if (es == null)
                        break;
                    
                    e = es.o;

                    if (Lib.strcmp(e.classname, "light") != 0) {
                        ServerGame.PF_dprintf(self.classname + " at "
                        + Lib.vtos(self.s.origin));
                        ServerGame.PF_dprintf("target " + self.target + " ("
                        + e.classname + " at " + Lib.vtos(e.s.origin)
                        + ") is not a light\n");
                    } else {
                        self.enemy = e;
                    }
                }

                if (null == self.enemy) {
                    ServerGame.PF_dprintf(self.classname + " target "
                    + self.target + " not found at "
                    + Lib.vtos(self.s.origin) + "\n");
                    GameUtil.G_FreeEdict(self);
                    return;
                }
            }

            self.timestamp = GameBase.level.time;
            target_lightramp_think.think(self);
        }
    };

    /**
     * QUAKED target_earthquake (1 0 0) (-8 -8 -8) (8 8 8) When triggered, this
     * initiates a level-wide earthquake. All players and monsters are affected.
     * "speed" severity of the quake (default:200) "count" duration of the quake
     * (default:5)
     */

    static EntityThinkAdapter target_earthquake_think = new EntityThinkAdapter() {
    	public String getID() { return "target_earthquake_think"; }
        public boolean think(Entity self) {

            int i;
            Entity e;

            if (self.last_move_time < GameBase.level.time) {
                ServerSend.SV_StartSound(self.s.origin, self, Constants.CHAN_AUTO, self.noise_index, 1.0f,
                (float) Constants.ATTN_NONE, (float) 0);
                self.last_move_time = GameBase.level.time + 0.5f;
            }

            for (i = 1; i < GameBase.num_edicts; i++) {
                e = GameBase.g_edicts[i];

                if (!e.inuse)
                    continue;
                if (null == e.client)
                    continue;
                if (null == e.groundentity)
                    continue;

                e.groundentity = null;
                e.velocity[0] += Lib.crandom() * 150;
                e.velocity[1] += Lib.crandom() * 150;
                e.velocity[2] = self.speed * (100.0f / e.mass);
            }

            if (GameBase.level.time < self.timestamp)
                self.nextthink = GameBase.level.time + Constants.FRAMETIME;

            return true;
        }
    };

    static EntityUseAdapter target_earthquake_use = new EntityUseAdapter() {
    	public String getID() { return "target_earthquake_use"; }
        public void use(Entity self, Entity other, Entity activator) {
            self.timestamp = GameBase.level.time + self.count;
            self.nextthink = GameBase.level.time + Constants.FRAMETIME;
            self.activator = activator;
            self.last_move_time = 0;
        }
    };
}

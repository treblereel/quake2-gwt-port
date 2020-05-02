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


import java.util.*;

import com.googlecode.gwtquake.shared.client.ClientMonsterMethods;
import com.googlecode.gwtquake.shared.common.CM;
import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.game.adapters.EntityThinkAdapter;
import com.googlecode.gwtquake.shared.game.adapters.EntityUseAdapter;
import com.googlecode.gwtquake.shared.server.ServerGame;
import com.googlecode.gwtquake.shared.server.ServerSend;
import com.googlecode.gwtquake.shared.server.World;
import com.googlecode.gwtquake.shared.util.*;

public class Monster {

    // FIXME monsters should call these with a totally accurate direction
    //	and we can mess it up based on skill. Spread should be for normal
    //	and we can tighten or loosen based on skill. We could muck with
    //	the damages too, but I'm not sure that's such a good idea.
    public static void monster_fire_bullet(Entity self, float[] start,
            float[] dir, int damage, int kick, int hspread, int vspread,
            int flashtype) {
        GameWeapon.fire_bullet(self, start, dir, damage, kick, hspread, vspread,
                Constants.MOD_UNKNOWN);

        ServerGame.PF_WriteByte(Constants.svc_muzzleflash2);
        ServerGame.PF_WriteShort(self.index);
        ServerGame.PF_WriteByte(flashtype);
        ServerSend.SV_Multicast(start, Constants.MULTICAST_PVS);
    }

    /** The Moster fires the shotgun. */
    public static void monster_fire_shotgun(Entity self, float[] start,
            float[] aimdir, int damage, int kick, int hspread, int vspread,
            int count, int flashtype) {
        GameWeapon.fire_shotgun(self, start, aimdir, damage, kick, hspread, vspread,
                count, Constants.MOD_UNKNOWN);

        ServerGame.PF_WriteByte(Constants.svc_muzzleflash2);
        ServerGame.PF_WriteShort(self.index);
        ServerGame.PF_WriteByte(flashtype);
        ServerSend.SV_Multicast(start, Constants.MULTICAST_PVS);
    }

    /** The Moster fires the blaster. */
    public static void monster_fire_blaster(Entity self, float[] start,
            float[] dir, int damage, int speed, int flashtype, int effect) {
        GameWeapon.fire_blaster(self, start, dir, damage, speed, effect, false);

        ServerGame.PF_WriteByte(Constants.svc_muzzleflash2);
        ServerGame.PF_WriteShort(self.index);
        ServerGame.PF_WriteByte(flashtype);
        ServerSend.SV_Multicast(start, Constants.MULTICAST_PVS);
    }

    /** The Moster fires the grenade. */
    public static void monster_fire_grenade(Entity self, float[] start,
            float[] aimdir, int damage, int speed, int flashtype) {
        GameWeapon
                .fire_grenade(self, start, aimdir, damage, speed, 2.5f,
                        damage + 40);

        ServerGame.PF_WriteByte(Constants.svc_muzzleflash2);
        ServerGame.PF_WriteShort(self.index);
        ServerGame.PF_WriteByte(flashtype);
        ServerSend.SV_Multicast(start, Constants.MULTICAST_PVS);
    }

    /** The Moster fires the rocket. */
    public static void monster_fire_rocket(Entity self, float[] start,
            float[] dir, int damage, int speed, int flashtype) {
        GameWeapon.fire_rocket(self, start, dir, damage, speed, damage + 20, damage);

        ServerGame.PF_WriteByte(Constants.svc_muzzleflash2);
        ServerGame.PF_WriteShort(self.index);
        ServerGame.PF_WriteByte(flashtype);
        ServerSend.SV_Multicast(start, Constants.MULTICAST_PVS);
    }

    /** The Moster fires the railgun. */
    public static void monster_fire_railgun(Entity self, float[] start,
            float[] aimdir, int damage, int kick, int flashtype) {
        GameWeapon.fire_rail(self, start, aimdir, damage, kick);

        ServerGame.PF_WriteByte(Constants.svc_muzzleflash2);
        ServerGame.PF_WriteShort(self.index);
        ServerGame.PF_WriteByte(flashtype);
        ServerSend.SV_Multicast(start, Constants.MULTICAST_PVS);
    }

    /** The Moster fires the bfg. */
    public static void monster_fire_bfg(Entity self, float[] start,
            float[] aimdir, int damage, int speed, int kick,
            float damage_radius, int flashtype) {
        GameWeapon.fire_bfg(self, start, aimdir, damage, speed, damage_radius);

        ServerGame.PF_WriteByte(Constants.svc_muzzleflash2);
        ServerGame.PF_WriteShort(self.index);
        ServerGame.PF_WriteByte(flashtype);
        ServerSend.SV_Multicast(start, Constants.MULTICAST_PVS);
    }

    /*
     * ================ monster_death_use
     * 
     * When a monster dies, it fires all of its targets with the current enemy
     * as activator. ================
     */
    public static void monster_death_use(Entity self) {
        self.flags &= ~(Constants.FL_FLY | Constants.FL_SWIM);
        self.monsterinfo.aiflags &= Constants.AI_GOOD_GUY;

        if (self.item != null) {
            GameItems.Drop_Item(self, self.item);
            self.item = null;
        }

        if (self.deathtarget != null)
            self.target = self.deathtarget;

        if (self.target == null)
            return;

        GameUtil.G_UseTargets(self, self.enemy);
    }

    // ============================================================================
    public static boolean monster_start(Entity self) {
        if (GameBase.deathmatch.value != 0) {
            GameUtil.G_FreeEdict(self);
            return false;
        }

        if ((self.spawnflags & 4) != 0
                && 0 == (self.monsterinfo.aiflags & Constants.AI_GOOD_GUY)) {
            self.spawnflags &= ~4;
            self.spawnflags |= 1;
            //		 gi.dprintf("fixed spawnflags on %s at %s\n", self.classname,
            // vtos(self.s.origin));
        }

        if (0 == (self.monsterinfo.aiflags & Constants.AI_GOOD_GUY))
            GameBase.level.total_monsters++;

        self.nextthink = GameBase.level.time + Constants.FRAMETIME;
        self.svflags |= Constants.SVF_MONSTER;
        self.s.renderfx |= Constants.RF_FRAMELERP;
        self.takedamage = Constants.DAMAGE_AIM;
        self.air_finished = GameBase.level.time + 12;
        self.use = GameUtil.monster_use;
        self.max_health = self.health;
        self.clipmask = Constants.MASK_MONSTERSOLID;

        self.s.skinnum = 0;
        self.deadflag = Constants.DEAD_NO;
        self.svflags &= ~Constants.SVF_DEADMONSTER;

        if (null == self.monsterinfo.checkattack)
            self.monsterinfo.checkattack = GameUtil.M_CheckAttack;
        Math3D.VectorCopy(self.s.origin, self.s.old_origin);

        if (GameBase.st.item != null && GameBase.st.item.length() > 0) {
            self.item = GameItems.FindItemByClassname(GameBase.st.item);
            if (self.item == null)
              ServerGame.PF_dprintf("monster_start:" + self.classname + " at "
              + Lib.vtos(self.s.origin) + " has bad item: "
              + GameBase.st.item + "\n");
        }

        // randomize what frame they start on
        if (self.monsterinfo.currentmove != null)
            self.s.frame = self.monsterinfo.currentmove.firstframe
                    + (Lib.rand() % (self.monsterinfo.currentmove.lastframe
                            - self.monsterinfo.currentmove.firstframe + 1));

        return true;
    }

    public static void monster_start_go(Entity self) {

        float[] v = { 0, 0, 0 };

        if (self.health <= 0)
            return;

        // check for target to combat_point and change to combattarget
        if (self.target != null) {
            boolean notcombat;
            boolean fixup;
            Entity target = null;
            notcombat = false;
            fixup = false;
            /*
             * if (true) { Com.Printf("all entities:\n");
             * 
             * for (int n = 0; n < Game.globals.num_edicts; n++) { edict_t ent =
             * GameBase.g_edicts[n]; Com.Printf( "|%4i | %25s
             * |%8.2f|%8.2f|%8.2f||%8.2f|%8.2f|%8.2f||%8.2f|%8.2f|%8.2f|\n", new
             * Vargs().add(n).add(ent.classname).
             * add(ent.s.origin[0]).add(ent.s.origin[1]).add(ent.s.origin[2])
             * .add(ent.mins[0]).add(ent.mins[1]).add(ent.mins[2])
             * .add(ent.maxs[0]).add(ent.maxs[1]).add(ent.maxs[2])); }
             * sleep(10); }
             */

            EntityIterator edit = null;

            while ((edit = GameBase.G_Find(edit, GameBase.findByTarget,
                    self.target)) != null) {
                target = edit.o;
                if (Lib.strcmp(target.classname, "point_combat") == 0) {
                    self.combattarget = self.target;
                    fixup = true;
                } else {
                    notcombat = true;
                }
            }
            if (notcombat && self.combattarget != null)
              ServerGame.PF_dprintf(self.classname + " at "
              + Lib.vtos(self.s.origin)
              + " has target with mixed types\n");
            if (fixup)
                self.target = null;
        }

        // validate combattarget
        if (self.combattarget != null) {
            Entity target = null;

            EntityIterator edit = null;
            while ((edit = GameBase.G_Find(edit, GameBase.findByTarget,
                    self.combattarget)) != null) {
                target = edit.o;

                if (Lib.strcmp(target.classname, "point_combat") != 0) {
                    ServerGame.PF_dprintf(self.classname + " at "
                    + Lib.vtos(self.s.origin)
                    + " has bad combattarget " + self.combattarget
                    + " : " + target.classname + " at "
                    + Lib.vtos(target.s.origin));
                }
            }
        }

        if (self.target != null) {
            self.goalentity = self.movetarget = GameBase
                    .G_PickTarget(self.target);
            if (null == self.movetarget) {
                ServerGame.PF_dprintf(self.classname + " can't find target "
                + self.target + " at "
                + Lib.vtos(self.s.origin) + "\n");
                self.target = null;
                self.monsterinfo.pausetime = 100000000;
                self.monsterinfo.stand.think(self);
            } else if (Lib.strcmp(self.movetarget.classname, "path_corner") == 0) {
                Math3D.VectorSubtract(self.goalentity.s.origin, self.s.origin,
                        v);
                self.ideal_yaw = self.s.angles[Constants.YAW] = Math3D
                        .vectoyaw(v);
                self.monsterinfo.walk.think(self);
                self.target = null;
            } else {
                self.goalentity = self.movetarget = null;
                self.monsterinfo.pausetime = 100000000;
                self.monsterinfo.stand.think(self);
            }
        } else {
            self.monsterinfo.pausetime = 100000000;
            self.monsterinfo.stand.think(self);
        }

        self.think = Monster.monster_think;
        self.nextthink = GameBase.level.time + Constants.FRAMETIME;
    }

    public static EntityThinkAdapter monster_think = new EntityThinkAdapter() {
        public String getID() { return "monster_think";}
        public boolean think(Entity self) {

            ClientMonsterMethods.M_MoveFrame(self);
            if (self.linkcount != self.monsterinfo.linkcount) {
                self.monsterinfo.linkcount = self.linkcount;
                ClientMonsterMethods.M_CheckGround(self);
            }
            ClientMonsterMethods.M_CatagorizePosition(self);
            ClientMonsterMethods.M_WorldEffects(self);
            ClientMonsterMethods.M_SetEffects(self);
            return true;
        }
    };

    public static EntityThinkAdapter monster_triggered_spawn = new EntityThinkAdapter() {
        public String getID() { return "monster_trigger_spawn";}
        public boolean think(Entity self) {

            self.s.origin[2] += 1;
            GameUtil.KillBox(self);

            self.solid = Constants.SOLID_BBOX;
            self.movetype = Constants.MOVETYPE_STEP;
            self.svflags &= ~Constants.SVF_NOCLIENT;
            self.air_finished = GameBase.level.time + 12;
            World.SV_LinkEdict(self);

            Monster.monster_start_go(self);

            if (self.enemy != null && 0 == (self.spawnflags & 1)
                    && 0 == (self.enemy.flags & Constants.FL_NOTARGET)) {
                GameUtil.FoundTarget(self);
            } else {
                self.enemy = null;
            }
            return true;
        }
    };

    //	we have a one frame delay here so we don't telefrag the guy who activated
    // us
    public static EntityUseAdapter monster_triggered_spawn_use = new EntityUseAdapter() {
        public String getID() { return "monster_trigger_spawn_use";}
        public void use(Entity self, Entity other, Entity activator) {
            self.think = monster_triggered_spawn;
            self.nextthink = GameBase.level.time + Constants.FRAMETIME;
            if (activator.client != null)
                self.enemy = activator;
            self.use = GameUtil.monster_use;
        }
    };

    public static EntityThinkAdapter monster_triggered_start = new EntityThinkAdapter() {
        public String getID() { return "monster_triggered_start";}
        public boolean think(Entity self) {
            if (self.index == 312)
                Com.Printf("monster_triggered_start\n");
            self.solid = Constants.SOLID_NOT;
            self.movetype = Constants.MOVETYPE_NONE;
            self.svflags |= Constants.SVF_NOCLIENT;
            self.nextthink = 0;
            self.use = monster_triggered_spawn_use;
            return true;
        }
    };
}

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
package com.googlecode.gwtquake.shared.game;

import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.Globals;
import com.googlecode.gwtquake.shared.server.ServerGame;
import com.googlecode.gwtquake.shared.server.ServerInit;
import com.googlecode.gwtquake.shared.server.ServerSend;
import com.googlecode.gwtquake.shared.server.World;
import com.googlecode.gwtquake.shared.util.Math3D;


public class GameCombat {

    /**
     * CanDamage
     * 
     * Returns true if the inflictor can directly damage the target. Used for
     * explosions and melee attacks.
     */
    static boolean CanDamage(Entity targ, Entity inflictor) {
        float[] dest = { 0, 0, 0 };
        Trace trace;
    
        // bmodels need special checking because their origin is 0,0,0
        if (targ.movetype == Constants.MOVETYPE_PUSH) {
            Math3D.VectorAdd(targ.absmin, targ.absmax, dest);
            Math3D.VectorScale(dest, 0.5f, dest);
            trace = World.SV_Trace(inflictor.s.origin, Globals.vec3_origin, Globals.vec3_origin, dest, inflictor, Constants.MASK_SOLID);
            if (trace.fraction == 1.0f)
                return true;
            if (trace.ent == targ)
                return true;
            return false;
        }
    
        trace = World.SV_Trace(inflictor.s.origin, Globals.vec3_origin, Globals.vec3_origin, targ.s.origin, inflictor, Constants.MASK_SOLID);
        if (trace.fraction == 1.0)
            return true;
    
        Math3D.VectorCopy(targ.s.origin, dest);
        dest[0] += 15.0;
        dest[1] += 15.0;
        trace = World.SV_Trace(inflictor.s.origin, Globals.vec3_origin, Globals.vec3_origin, dest, inflictor, Constants.MASK_SOLID);
        if (trace.fraction == 1.0)
            return true;
    
        Math3D.VectorCopy(targ.s.origin, dest);
        dest[0] += 15.0;
        dest[1] -= 15.0;
        trace = World.SV_Trace(inflictor.s.origin, Globals.vec3_origin, Globals.vec3_origin, dest, inflictor, Constants.MASK_SOLID);
        if (trace.fraction == 1.0)
            return true;
    
        Math3D.VectorCopy(targ.s.origin, dest);
        dest[0] -= 15.0;
        dest[1] += 15.0;
        trace = World.SV_Trace(inflictor.s.origin, Globals.vec3_origin, Globals.vec3_origin, dest, inflictor, Constants.MASK_SOLID);
        if (trace.fraction == 1.0)
            return true;
    
        Math3D.VectorCopy(targ.s.origin, dest);
        dest[0] -= 15.0;
        dest[1] -= 15.0;
        trace = World.SV_Trace(inflictor.s.origin, Globals.vec3_origin, Globals.vec3_origin, dest, inflictor, Constants.MASK_SOLID);
        if (trace.fraction == 1.0)
            return true;
    
        return false;
    }

    /**
     * Killed.
     */
    public static void Killed(Entity targ, Entity inflictor,
            Entity attacker, int damage, float[] point) {
        Com.DPrintf("Killing a " + targ.classname + "\n");
        if (targ.health < -999)
            targ.health = -999;
    
        targ.enemy = attacker;
    
        if ((targ.svflags & Constants.SVF_MONSTER) != 0
                && (targ.deadflag != Constants.DEAD_DEAD)) {
            //			targ.svflags |= SVF_DEADMONSTER; // now treat as a different
            // content type
            if (0 == (targ.monsterinfo.aiflags & Constants.AI_GOOD_GUY)) {
                GameBase.level.killed_monsters++;
                if (GameBase.coop.value != 0 && attacker.client != null)
                    attacker.client.resp.score++;
                // medics won't heal monsters that they kill themselves
                if (attacker.classname.equals("monster_medic"))
                    targ.owner = attacker;
            }
        }
    
        if (targ.movetype == Constants.MOVETYPE_PUSH
                || targ.movetype == Constants.MOVETYPE_STOP
                || targ.movetype == Constants.MOVETYPE_NONE) { // doors, triggers,
                                                             // etc
            targ.die.die(targ, inflictor, attacker, damage, point);
            return;
        }
    
        if ((targ.svflags & Constants.SVF_MONSTER) != 0
                && (targ.deadflag != Constants.DEAD_DEAD)) {
            targ.touch = null;
            Monster.monster_death_use(targ);
        }
    
        targ.die.die(targ, inflictor, attacker, damage, point);
    }

    /**
     * SpawnDamage.
     */
    static void SpawnDamage(int type, float[] origin, float[] normal, int damage) {
        if (damage > 255)
            damage = 255;
        ServerGame.PF_WriteByte(Constants.svc_temp_entity);
        ServerGame.PF_WriteByte(type);
        //		gi.WriteByte (damage);
        ServerGame.PF_WritePos(origin);
        ServerGame.PF_WriteDir(normal);
        ServerSend.SV_Multicast(origin, Constants.MULTICAST_PVS);
    }

    static int CheckPowerArmor(Entity ent, float[] point, float[] normal,
            int damage, int dflags) {
        GameClient client;
        int save;
        int power_armor_type;
        int index = 0;
        int damagePerCell;
        int pa_te_type;
        int power = 0;
        int power_used;
    
        if (damage == 0)
            return 0;
    
        client = ent.client;
    
        if ((dflags & Constants.DAMAGE_NO_ARMOR) != 0)
            return 0;
    
        if (client != null) {
            power_armor_type = GameItems.PowerArmorType(ent);
            if (power_armor_type != Constants.POWER_ARMOR_NONE) {
                index = GameItems.ITEM_INDEX(GameItems.FindItem("Cells"));
                power = client.pers.inventory[index];
            }
        } else if ((ent.svflags & Constants.SVF_MONSTER) != 0) {
            power_armor_type = ent.monsterinfo.power_armor_type;
            power = ent.monsterinfo.power_armor_power;
        } else
            return 0;
    
        if (power_armor_type == Constants.POWER_ARMOR_NONE)
            return 0;
        if (power == 0)
            return 0;
    
        if (power_armor_type == Constants.POWER_ARMOR_SCREEN) {
            float[] vec = { 0, 0, 0 };
            float dot;
            float[] forward = { 0, 0, 0 };
    
            // only works if damage point is in front
            Math3D.AngleVectors(ent.s.angles, forward, null, null);
            Math3D.VectorSubtract(point, ent.s.origin, vec);
            Math3D.VectorNormalize(vec);
            dot = Math3D.DotProduct(vec, forward);
            if (dot <= 0.3)
                return 0;
    
            damagePerCell = 1;
            pa_te_type = Constants.TE_SCREEN_SPARKS;
            damage = damage / 3;
        } else {
            damagePerCell = 2;
            pa_te_type = Constants.TE_SHIELD_SPARKS;
            damage = (2 * damage) / 3;
        }
    
        save = power * damagePerCell;
    
        if (save == 0)
            return 0;
        if (save > damage)
            save = damage;
    
        SpawnDamage(pa_te_type, point, normal, save);
        ent.powerarmor_time = GameBase.level.time + 0.2f;
    
        power_used = save / damagePerCell;
    
        if (client != null)
            client.pers.inventory[index] -= power_used;
        else
            ent.monsterinfo.power_armor_power -= power_used;
        return save;
    }

    static int CheckArmor(Entity ent, float[] point, float[] normal,
            int damage, int te_sparks, int dflags) {
        GameClient client;
        int save;
        int index;
        GameItem armor;
    
        if (damage == 0)
            return 0;
    
        client = ent.client;
    
        if (client == null)
            return 0;
    
        if ((dflags & Constants.DAMAGE_NO_ARMOR) != 0)
            return 0;
    
        index = GameItems.ArmorIndex(ent);
    
        if (index == 0)
            return 0;
    
        armor = GameItems.GetItemByIndex(index);
        GameItemArmor garmor = (GameItemArmor) armor.info;
    
        if (0 != (dflags & Constants.DAMAGE_ENERGY))
            save = (int) Math.ceil(garmor.energy_protection * damage);
        else
            save = (int) Math.ceil(garmor.normal_protection * damage);
    
        if (save >= client.pers.inventory[index])
            save = client.pers.inventory[index];
    
        if (save == 0)
            return 0;
    
        client.pers.inventory[index] -= save;
        SpawnDamage(te_sparks, point, normal, save);
    
        return save;
    }

    public static void M_ReactToDamage(Entity targ, Entity attacker) {
        if ((null != attacker.client)
                && 0 != (attacker.svflags & Constants.SVF_MONSTER))
            return;
    
        if (attacker == targ || attacker == targ.enemy)
            return;
    
        // if we are a good guy monster and our attacker is a player
        // or another good guy, do not get mad at them
        if (0 != (targ.monsterinfo.aiflags & Constants.AI_GOOD_GUY)) {
            if (attacker.client != null
                    || (attacker.monsterinfo.aiflags & Constants.AI_GOOD_GUY) != 0)
                return;
        }
    
        // we now know that we are not both good guys
    
        // if attacker is a client, get mad at them because he's good and we're
        // not
        if (attacker.client != null) {
            targ.monsterinfo.aiflags &= ~Constants.AI_SOUND_TARGET;
    
            // this can only happen in coop (both new and old enemies are
            // clients)
            // only switch if can't see the current enemy
            if (targ.enemy != null && targ.enemy.client != null) {
                if (GameUtil.visible(targ, targ.enemy)) {
                    targ.oldenemy = attacker;
                    return;
                }
                targ.oldenemy = targ.enemy;
            }
            targ.enemy = attacker;
            if (0 == (targ.monsterinfo.aiflags & Constants.AI_DUCKED))
                GameUtil.FoundTarget(targ);
            return;
        }
    
        // it's the same base (walk/swim/fly) type and a different classname and
        // it's not a tank
        // (they spray too much), get mad at them
        if (((targ.flags & (Constants.FL_FLY | Constants.FL_SWIM)) == (attacker.flags & (Constants.FL_FLY | Constants.FL_SWIM)))
                && (!(targ.classname.equals(attacker.classname)))
                && (!(attacker.classname.equals("monster_tank")))
                && (!(attacker.classname.equals("monster_supertank")))
                && (!(attacker.classname.equals("monster_makron")))
                && (!(attacker.classname.equals("monster_jorg")))) {
            if (targ.enemy != null && targ.enemy.client != null)
                targ.oldenemy = targ.enemy;
            targ.enemy = attacker;
            if (0 == (targ.monsterinfo.aiflags & Constants.AI_DUCKED))
                GameUtil.FoundTarget(targ);
        }
        // if they *meant* to shoot us, then shoot back
        else if (attacker.enemy == targ) {
            if (targ.enemy != null && targ.enemy.client != null)
                targ.oldenemy = targ.enemy;
            targ.enemy = attacker;
            if (0 == (targ.monsterinfo.aiflags & Constants.AI_DUCKED))
                GameUtil.FoundTarget(targ);
        }
        // otherwise get mad at whoever they are mad at (help our buddy) unless
        // it is us!
        else if (attacker.enemy != null && attacker.enemy != targ) {
            if (targ.enemy != null && targ.enemy.client != null)
                targ.oldenemy = targ.enemy;
            targ.enemy = attacker.enemy;
            if (0 == (targ.monsterinfo.aiflags & Constants.AI_DUCKED))
                GameUtil.FoundTarget(targ);
        }
    }

    static boolean CheckTeamDamage(Entity targ, Entity attacker) {
        //FIXME make the next line real and uncomment this block
        // if ((ability to damage a teammate == OFF) && (targ's team ==
        // attacker's team))
        return false;
    }

    /**
     * T_RadiusDamage.
     */
    static void T_RadiusDamage(Entity inflictor, Entity attacker,
            float damage, Entity ignore, float radius, int mod) {
        float points;
        EntityIterator edictit = null;
    
        float[] v = { 0, 0, 0 };
        float[] dir = { 0, 0, 0 };
    
        while ((edictit = GameBase.findradius(edictit, inflictor.s.origin,
                radius)) != null) {
            Entity ent = edictit.o;
            if (ent == ignore)
                continue;
            if (ent.takedamage == 0)
                continue;
    
            Math3D.VectorAdd(ent.mins, ent.maxs, v);
            Math3D.VectorMA(ent.s.origin, 0.5f, v, v);
            Math3D.VectorSubtract(inflictor.s.origin, v, v);
            points = damage - 0.5f * Math3D.VectorLength(v);
            if (ent == attacker)
                points = points * 0.5f;
            if (points > 0) {
                if (CanDamage(ent, inflictor)) {
                    Math3D.VectorSubtract(ent.s.origin, inflictor.s.origin, dir);
                    T_Damage(ent, inflictor, attacker, dir, inflictor.s.origin,
                            Globals.vec3_origin, (int) points, (int) points,
                            Constants.DAMAGE_RADIUS, mod);
                }
            }
        }
    }

    public static void T_Damage(Entity targ, Entity inflictor,
            Entity attacker, float[] dir, float[] point, float[] normal,
            int damage, int knockback, int dflags, int mod) {
        GameClient client;
        int take;
        int save;
        int asave;
        int psave;
        int te_sparks;
    
        if (targ.takedamage == 0)
            return;
    
        // friendly fire avoidance
        // if enabled you can't hurt teammates (but you can hurt yourself)
        // knockback still occurs
        if ((targ != attacker)
                && ((GameBase.deathmatch.value != 0 && 0 != ((int) (GameBase.dmflags.value) & (Constants.DF_MODELTEAMS | Constants.DF_SKINTEAMS))) || GameBase.coop.value != 0)) {
            if (GameUtil.OnSameTeam(targ, attacker)) {
                if (((int) (GameBase.dmflags.value) & Constants.DF_NO_FRIENDLY_FIRE) != 0)
                    damage = 0;
                else
                    mod |= Constants.MOD_FRIENDLY_FIRE;
            }
        }
        GameBase.meansOfDeath = mod;
    
        // easy mode takes half damage
        if (GameBase.skill.value == 0 && GameBase.deathmatch.value == 0
                && targ.client != null) {
            damage *= 0.5;
            if (damage == 0)
                damage = 1;
        }
    
        client = targ.client;
    
        if ((dflags & Constants.DAMAGE_BULLET) != 0)
            te_sparks = Constants.TE_BULLET_SPARKS;
        else
            te_sparks = Constants.TE_SPARKS;
    
        Math3D.VectorNormalize(dir);
    
        // bonus damage for suprising a monster
        if (0 == (dflags & Constants.DAMAGE_RADIUS)
                && (targ.svflags & Constants.SVF_MONSTER) != 0
                && (attacker.client != null) && (targ.enemy == null)
                && (targ.health > 0))
            damage *= 2;
    
        if ((targ.flags & Constants.FL_NO_KNOCKBACK) != 0)
            knockback = 0;
    
        // figure momentum add
        if (0 == (dflags & Constants.DAMAGE_NO_KNOCKBACK)) {
            if ((knockback != 0) && (targ.movetype != Constants.MOVETYPE_NONE)
                    && (targ.movetype != Constants.MOVETYPE_BOUNCE)
                    && (targ.movetype != Constants.MOVETYPE_PUSH)
                    && (targ.movetype != Constants.MOVETYPE_STOP)) {
                float[] kvel = { 0, 0, 0 };
                float mass;
    
                if (targ.mass < 50)
                    mass = 50;
                else
                    mass = targ.mass;
    
                if (targ.client != null && attacker == targ)
                    Math3D.VectorScale(dir, 1600.0f * (float) knockback / mass,
                            kvel);
                // the rocket jump hack...
                else
                    Math3D.VectorScale(dir, 500.0f * (float) knockback / mass,
                            kvel);
    
                Math3D.VectorAdd(targ.velocity, kvel, targ.velocity);
            }
        }
    
        take = damage;
        save = 0;
    
        // check for godmode
        if ((targ.flags & Constants.FL_GODMODE) != 0
                && 0 == (dflags & Constants.DAMAGE_NO_PROTECTION)) {
            take = 0;
            save = damage;
            SpawnDamage(te_sparks, point, normal, save);
        }
    
        // check for invincibility
        if ((client != null && client.invincible_framenum > GameBase.level.framenum)
                && 0 == (dflags & Constants.DAMAGE_NO_PROTECTION)) {
            if (targ.pain_debounce_time < GameBase.level.time) {
                ServerGame.PF_StartSound(targ, Constants.CHAN_ITEM, ServerInit.SV_SoundIndex("items/protect4.wav"), (float) 1, (float) Constants.ATTN_NORM,
                (float) 0);
                targ.pain_debounce_time = GameBase.level.time + 2;
            }
            take = 0;
            save = damage;
        }
    
        psave = CheckPowerArmor(targ, point, normal, take, dflags);
        take -= psave;
    
        asave = CheckArmor(targ, point, normal, take, te_sparks, dflags);
        take -= asave;
    
        // treat cheat/powerup savings the same as armor
        asave += save;
    
        // team damage avoidance
        if (0 == (dflags & Constants.DAMAGE_NO_PROTECTION)
                && CheckTeamDamage(targ, attacker))
            return;
    
        // do the damage
        if (take != 0) {
            if (0 != (targ.svflags & Constants.SVF_MONSTER) || (client != null))
                SpawnDamage(Constants.TE_BLOOD, point, normal, take);
            else
                SpawnDamage(te_sparks, point, normal, take);
    
            targ.health = targ.health - take;
    
            if (targ.health <= 0) {
                if ((targ.svflags & Constants.SVF_MONSTER) != 0
                        || (client != null))
                    targ.flags |= Constants.FL_NO_KNOCKBACK;
                Killed(targ, inflictor, attacker, take, point);
                return;
            }
        }
    
        if ((targ.svflags & Constants.SVF_MONSTER) != 0) {
            M_ReactToDamage(targ, attacker);
            if (0 == (targ.monsterinfo.aiflags & Constants.AI_DUCKED)
                    && (take != 0)) {
                targ.pain.pain(targ, attacker, knockback, take);
                // nightmare mode monsters don't go into pain frames often
                if (GameBase.skill.value == 3)
                    targ.pain_debounce_time = GameBase.level.time + 5;
            }
        } else if (client != null) {
            if (((targ.flags & Constants.FL_GODMODE) == 0) && (take != 0))
                targ.pain.pain(targ, attacker, knockback, take);
        } else if (take != 0) {
            if (targ.pain != null)
                targ.pain.pain(targ, attacker, knockback, take);
        }
    
        // add to the damage inflicted on a player this frame
        // the total will be turned into screen blends and view angle kicks
        // at the end of the frame
        if (client != null) {
            client.damage_parmor += psave;
            client.damage_armor += asave;
            client.damage_blood += take;
            client.damage_knockback += knockback;
            Math3D.VectorCopy(point, client.damage_from);
        }
    }
}

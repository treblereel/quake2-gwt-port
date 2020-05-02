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

import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.Globals;
import com.googlecode.gwtquake.shared.game.adapters.EntityThinkAdapter;
import com.googlecode.gwtquake.shared.game.adapters.EntityTouchAdapter;
import com.googlecode.gwtquake.shared.server.ServerGame;
import com.googlecode.gwtquake.shared.server.ServerInit;
import com.googlecode.gwtquake.shared.server.ServerSend;
import com.googlecode.gwtquake.shared.server.World;
import com.googlecode.gwtquake.shared.util.Lib;
import com.googlecode.gwtquake.shared.util.Math3D;

public class GameWeapon {

    static EntityTouchAdapter blaster_touch = new EntityTouchAdapter() {
        public String getID() {
            return "blaster_touch";
        }

        public void touch(Entity self, Entity other, Plane plane,
                          Surface surf) {
            int mod;

            if (other == self.owner) {
                return;
            }

            if (surf != null && (surf.flags & Constants.SURF_SKY) != 0) {
                GameUtil.G_FreeEdict(self);
                return;
            }

            if (self.owner.client != null) {
                PlayerWeapon.PlayerNoise(self.owner, self.s.origin,
                                         Constants.PNOISE_IMPACT);
            }

            if (other.takedamage != 0) {
                if ((self.spawnflags & 1) != 0) {
                    mod = Constants.MOD_HYPERBLASTER;
                } else {
                    mod = Constants.MOD_BLASTER;
                }

                // bugfix null plane rst
                float[] normal;
                if (plane == null) {
                    normal = new float[3];
                } else {
                    normal = plane.normal;
                }

                GameCombat.T_Damage(other, self, self.owner, self.velocity,
                                    self.s.origin, normal, self.dmg, 1,
                                    Constants.DAMAGE_ENERGY, mod);
            } else {
                ServerGame.PF_WriteByte(Constants.svc_temp_entity);
                ServerGame.PF_WriteByte(Constants.TE_BLASTER);
                ServerGame.PF_WritePos(self.s.origin);
                if (plane == null) {
                    ServerGame.PF_WriteDir(Globals.vec3_origin);
                } else {
                    ServerGame.PF_WriteDir(plane.normal);
                }
                ServerSend.SV_Multicast(self.s.origin, Constants.MULTICAST_PVS);
            }

            GameUtil.G_FreeEdict(self);
        }
    };

    static EntityThinkAdapter Grenade_Explode = new EntityThinkAdapter() {
        public String getID() {
            return "Grenade_Explode";
        }

        public boolean think(Entity ent) {
            float[] origin = {0, 0, 0};
            int mod;

            if (ent.owner.client != null) {
                PlayerWeapon.PlayerNoise(ent.owner, ent.s.origin,
                                         Constants.PNOISE_IMPACT);
            }

            //FIXME: if we are onground then raise our Z just a bit since we
            // are a point?
            if (ent.enemy != null) {
                float points = 0;
                float[] v = {0, 0, 0};
                float[] dir = {0, 0, 0};

                Math3D.VectorAdd(ent.enemy.mins, ent.enemy.maxs, v);
                Math3D.VectorMA(ent.enemy.s.origin, 0.5f, v, v);
                Math3D.VectorSubtract(ent.s.origin, v, v);
                points = ent.dmg - 0.5f * Math3D.VectorLength(v);
                Math3D.VectorSubtract(ent.enemy.s.origin, ent.s.origin, dir);
                if ((ent.spawnflags & 1) != 0) {
                    mod = Constants.MOD_HANDGRENADE;
                } else {
                    mod = Constants.MOD_GRENADE;
                }
                GameCombat.T_Damage(ent.enemy, ent, ent.owner, dir, ent.s.origin,
                                    Globals.vec3_origin, (int) points, (int) points,
                                    Constants.DAMAGE_RADIUS, mod);
            }

            if ((ent.spawnflags & 2) != 0) {
                mod = Constants.MOD_HELD_GRENADE;
            } else if ((ent.spawnflags & 1) != 0) {
                mod = Constants.MOD_HG_SPLASH;
            } else {
                mod = Constants.MOD_G_SPLASH;
            }
            GameCombat.T_RadiusDamage(ent, ent.owner, ent.dmg, ent.enemy,
                                      ent.dmg_radius, mod);

            Math3D.VectorMA(ent.s.origin, -0.02f, ent.velocity, origin);
            ServerGame.PF_WriteByte(Constants.svc_temp_entity);
            if (ent.waterlevel != 0) {
                if (ent.groundentity != null) {
                    ServerGame.PF_WriteByte(Constants.TE_GRENADE_EXPLOSION_WATER);
                } else {
                    ServerGame.PF_WriteByte(Constants.TE_ROCKET_EXPLOSION_WATER);
                }
            } else {
                if (ent.groundentity != null) {
                    ServerGame.PF_WriteByte(Constants.TE_GRENADE_EXPLOSION);
                } else {
                    ServerGame.PF_WriteByte(Constants.TE_ROCKET_EXPLOSION);
                }
            }
            ServerGame.PF_WritePos(origin);
            ServerSend.SV_Multicast(ent.s.origin, Constants.MULTICAST_PHS);

            GameUtil.G_FreeEdict(ent);
            return true;
        }
    };
    static EntityTouchAdapter Grenade_Touch = new EntityTouchAdapter() {
        public String getID() {
            return "Grenade_Touch";
        }

        public void touch(Entity ent, Entity other, Plane plane,
                          Surface surf) {
            if (other == ent.owner) {
                return;
            }

            if (surf != null && 0 != (surf.flags & Constants.SURF_SKY)) {
                GameUtil.G_FreeEdict(ent);
                return;
            }

            if (other.takedamage == 0) {
                if ((ent.spawnflags & 1) != 0) {
                    if (Lib.random() > 0.5f) {
                        ServerGame.PF_StartSound(ent, Constants.CHAN_VOICE, ServerInit.SV_SoundIndex("weapons/hgrenb1a.wav"), (float) 1, (float) Constants.ATTN_NORM,
                                                 (float) 0);
                    } else {
                        ServerGame.PF_StartSound(ent, Constants.CHAN_VOICE, ServerInit.SV_SoundIndex("weapons/hgrenb2a.wav"), (float) 1, (float) Constants.ATTN_NORM,
                                                 (float) 0);
                    }
                } else {
                    ServerGame.PF_StartSound(ent, Constants.CHAN_VOICE, ServerInit.SV_SoundIndex("weapons/grenlb1b.wav"), (float) 1, (float) Constants.ATTN_NORM,
                                             (float) 0);
                }
                return;
            }

            ent.enemy = other;
            Grenade_Explode.think(ent);
        }
    };

    /*
     * =================
     * fire_rocket
     * =================
     */
    static EntityTouchAdapter rocket_touch = new EntityTouchAdapter() {
        public String getID() {
            return "rocket_touch";
        }

        public void touch(Entity ent, Entity other, Plane plane,
                          Surface surf) {
            float[] origin = {0, 0, 0};
            int n;

            if (other == ent.owner) {
                return;
            }

            if (surf != null && (surf.flags & Constants.SURF_SKY) != 0) {
                GameUtil.G_FreeEdict(ent);
                return;
            }

            if (ent.owner.client != null) {
                PlayerWeapon.PlayerNoise(ent.owner, ent.s.origin,
                                         Constants.PNOISE_IMPACT);
            }

            // calculate position for the explosion entity
            Math3D.VectorMA(ent.s.origin, -0.02f, ent.velocity, origin);

            if (other.takedamage != 0) {
                GameCombat.T_Damage(other, ent, ent.owner, ent.velocity,
                                    ent.s.origin, plane.normal, ent.dmg, 0, 0,
                                    Constants.MOD_ROCKET);
            } else {
                // don't throw any debris in net games
                if (GameBase.deathmatch.value == 0 && 0 == GameBase.coop.value) {
                    if ((surf != null)
                            && 0 == (surf.flags & (Constants.SURF_WARP
                            | Constants.SURF_TRANS33
                            | Constants.SURF_TRANS66 | Constants.SURF_FLOWING))) {
                        n = Lib.rand() % 5;
                        while (n-- > 0) {
                            GameMisc.ThrowDebris(ent,
                                                 "models/objects/debris2/tris.md2", 2,
                                                 ent.s.origin);
                        }
                    }
                }
            }

            GameCombat.T_RadiusDamage(ent, ent.owner, ent.radius_dmg, other,
                                      ent.dmg_radius, Constants.MOD_R_SPLASH);

            ServerGame.PF_WriteByte(Constants.svc_temp_entity);
            if (ent.waterlevel != 0) {
                ServerGame.PF_WriteByte(Constants.TE_ROCKET_EXPLOSION_WATER);
            } else {
                ServerGame.PF_WriteByte(Constants.TE_ROCKET_EXPLOSION);
            }
            ServerGame.PF_WritePos(origin);
            ServerSend.SV_Multicast(ent.s.origin, Constants.MULTICAST_PHS);

            GameUtil.G_FreeEdict(ent);
        }
    };
    /*
     * =================
     * fire_bfg
     * =================
     */
    static EntityThinkAdapter bfg_explode = new EntityThinkAdapter() {
        public String getID() {
            return "bfg_explode";
        }

        public boolean think(Entity self) {
            Entity ent;
            float points;
            float[] v = {0, 0, 0};
            float dist;

            EntityIterator edit = null;

            if (self.s.frame == 0) {
                // the BFG effect
                ent = null;
                while ((edit = GameBase.findradius(edit, self.s.origin,
                                                   self.dmg_radius)) != null) {
                    ent = edit.o;
                    if (ent.takedamage == 0) {
                        continue;
                    }
                    if (ent == self.owner) {
                        continue;
                    }
                    if (!GameCombat.CanDamage(ent, self)) {
                        continue;
                    }
                    if (!GameCombat.CanDamage(ent, self.owner)) {
                        continue;
                    }

                    Math3D.VectorAdd(ent.mins, ent.maxs, v);
                    Math3D.VectorMA(ent.s.origin, 0.5f, v, v);
                    Math3D.VectorSubtract(self.s.origin, v, v);
                    dist = Math3D.VectorLength(v);
                    points = (float) (self.radius_dmg * (1.0 - Math.sqrt(dist
                                                                                 / self.dmg_radius)));
                    if (ent == self.owner) {
                        points = points * 0.5f;
                    }

                    ServerGame.PF_WriteByte(Constants.svc_temp_entity);
                    ServerGame.PF_WriteByte(Constants.TE_BFG_EXPLOSION);
                    ServerGame.PF_WritePos(ent.s.origin);
                    ServerSend.SV_Multicast(ent.s.origin, Constants.MULTICAST_PHS);
                    GameCombat.T_Damage(ent, self, self.owner, self.velocity,
                                        ent.s.origin, Globals.vec3_origin, (int) points, 0,
                                        Constants.DAMAGE_ENERGY, Constants.MOD_BFG_EFFECT);
                }
            }

            self.nextthink = GameBase.level.time + Constants.FRAMETIME;
            self.s.frame++;
            if (self.s.frame == 5) {
                self.think = GameUtil.G_FreeEdictA;
            }
            return true;
        }
    };

    static EntityTouchAdapter bfg_touch = new EntityTouchAdapter() {
        public String getID() {
            return "bfg_touch";
        }

        public void touch(Entity self, Entity other, Plane plane,
                          Surface surf) {
            if (other == self.owner) {
                return;
            }

            if (surf != null && (surf.flags & Constants.SURF_SKY) != 0) {
                GameUtil.G_FreeEdict(self);
                return;
            }

            if (self.owner.client != null) {
                PlayerWeapon.PlayerNoise(self.owner, self.s.origin,
                                         Constants.PNOISE_IMPACT);
            }

            // core explosion - prevents firing it into the wall/floor
            if (other.takedamage != 0) {
                GameCombat.T_Damage(other, self, self.owner, self.velocity,
                                    self.s.origin, plane.normal, 200, 0, 0,
                                    Constants.MOD_BFG_BLAST);
            }
            GameCombat.T_RadiusDamage(self, self.owner, 200, other, 100,
                                      Constants.MOD_BFG_BLAST);

            ServerGame.PF_StartSound(self, Constants.CHAN_VOICE, ServerInit.SV_SoundIndex("weapons/bfg__x1b.wav"), (float) 1, (float) Constants.ATTN_NORM,
                                     (float) 0);
            self.solid = Constants.SOLID_NOT;
            self.touch = null;
            Math3D.VectorMA(self.s.origin, -1 * Constants.FRAMETIME,
                            self.velocity, self.s.origin);
            Math3D.VectorClear(self.velocity);
            self.s.modelindex = ServerInit.SV_ModelIndex("sprites/s_bfg3.sp2");
            self.s.frame = 0;
            self.s.sound = 0;
            self.s.effects &= ~Constants.EF_ANIM_ALLFAST;
            self.think = bfg_explode;
            self.nextthink = GameBase.level.time + Constants.FRAMETIME;
            self.enemy = other;

            ServerGame.PF_WriteByte(Constants.svc_temp_entity);
            ServerGame.PF_WriteByte(Constants.TE_BFG_BIGEXPLOSION);
            ServerGame.PF_WritePos(self.s.origin);
            ServerSend.SV_Multicast(self.s.origin, Constants.MULTICAST_PVS);
        }
    };

    static EntityThinkAdapter bfg_think = new EntityThinkAdapter() {
        public String getID() {
            return "bfg_think";
        }

        public boolean think(Entity self) {
            Entity ent;
            Entity ignore;
            float[] point = {0, 0, 0};
            float[] dir = {0, 0, 0};
            float[] start = {0, 0, 0};
            float[] end = {0, 0, 0};
            int dmg;
            Trace tr;

            if (GameBase.deathmatch.value != 0) {
                dmg = 5;
            } else {
                dmg = 10;
            }

            EntityIterator edit = null;
            while ((edit = GameBase.findradius(edit, self.s.origin, 256)) != null) {
                ent = edit.o;

                if (ent == self) {
                    continue;
                }

                if (ent == self.owner) {
                    continue;
                }

                if (ent.takedamage == 0) {
                    continue;
                }

                if (0 == (ent.svflags & Constants.SVF_MONSTER)
                        && (null == ent.client)
                        && (Lib.strcmp(ent.classname, "misc_explobox") != 0)) {
                    continue;
                }

                Math3D.VectorMA(ent.absmin, 0.5f, ent.size, point);

                Math3D.VectorSubtract(point, self.s.origin, dir);
                Math3D.VectorNormalize(dir);

                ignore = self;
                Math3D.VectorCopy(self.s.origin, start);
                Math3D.VectorMA(start, 2048, dir, end);
                while (true) {
                    tr = World.SV_Trace(start, null, null, end, ignore, Constants.CONTENTS_SOLID | Constants.CONTENTS_MONSTER
                            | Constants.CONTENTS_DEADMONSTER);

                    if (null == tr.ent) {
                        break;
                    }

                    // hurt it if we can
                    if ((tr.ent.takedamage != 0)
                            && 0 == (tr.ent.flags & Constants.FL_IMMUNE_LASER)
                            && (tr.ent != self.owner)) {
                        GameCombat.T_Damage(tr.ent, self, self.owner, dir,
                                            tr.endpos, Globals.vec3_origin, dmg, 1,
                                            Constants.DAMAGE_ENERGY, Constants.MOD_BFG_LASER);
                    }

                    // if we hit something that's not a monster or player we're
                    // done
                    if (0 == (tr.ent.svflags & Constants.SVF_MONSTER)
                            && (null == tr.ent.client)) {
                        ServerGame.PF_WriteByte(Constants.svc_temp_entity);
                        ServerGame.PF_WriteByte(Constants.TE_LASER_SPARKS);
                        ServerGame.PF_WriteByte(4);
                        ServerGame.PF_WritePos(tr.endpos);
                        ServerGame.PF_WriteDir(tr.plane.normal);
                        ServerGame.PF_WriteByte(self.s.skinnum);
                        ServerSend.SV_Multicast(tr.endpos, Constants.MULTICAST_PVS);
                        break;
                    }

                    ignore = tr.ent;
                    Math3D.VectorCopy(tr.endpos, start);
                }

                ServerGame.PF_WriteByte(Constants.svc_temp_entity);
                ServerGame.PF_WriteByte(Constants.TE_BFG_LASER);
                ServerGame.PF_WritePos(self.s.origin);
                ServerGame.PF_WritePos(tr.endpos);
                ServerSend.SV_Multicast(self.s.origin, Constants.MULTICAST_PHS);
            }

            self.nextthink = GameBase.level.time + Constants.FRAMETIME;
            return true;
        }
    };

    /*
     * =================
     * fire_hit
     *
     * Used for all impact (hit/punch/slash) attacks
     * =================
     */
    public static boolean fire_hit(Entity self, float[] aim, int damage,
                                   int kick) {
        Trace tr;
        float[] forward = {0, 0, 0}, right = {0, 0, 0}, up = {0, 0, 0};
        float[] v = {0, 0, 0};
        float[] point = {0, 0, 0};
        float range;
        float[] dir = {0, 0, 0};

        //see if enemy is in range
        Math3D.VectorSubtract(self.enemy.s.origin, self.s.origin, dir);
        range = Math3D.VectorLength(dir);
        if (range > aim[0]) {
            return false;
        }

        if (aim[1] > self.mins[0] && aim[1] < self.maxs[0]) {
            // the hit is straight on so back the range up to the edge of their
            // bbox
            range -= self.enemy.maxs[0];
        } else {
            // this is a side hit so adjust the "right" value out to the edge of
            // their bbox
            if (aim[1] < 0) {
                aim[1] = self.enemy.mins[0];
            } else {
                aim[1] = self.enemy.maxs[0];
            }
        }

        Math3D.VectorMA(self.s.origin, range, dir, point);

        tr = World.SV_Trace(self.s.origin, null, null, point, self, Constants.MASK_SHOT);
        if (tr.fraction < 1) {
            if (0 == tr.ent.takedamage) {
                return false;
            }
            // if it will hit any client/monster then hit the one we wanted to
            // hit
            if ((tr.ent.svflags & Constants.SVF_MONSTER) != 0
                    || (tr.ent.client != null)) {
                tr.ent = self.enemy;
            }
        }

        Math3D.AngleVectors(self.s.angles, forward, right, up);
        Math3D.VectorMA(self.s.origin, range, forward, point);
        Math3D.VectorMA(point, aim[1], right, point);
        Math3D.VectorMA(point, aim[2], up, point);
        Math3D.VectorSubtract(point, self.enemy.s.origin, dir);

        // do the damage
        GameCombat.T_Damage(tr.ent, self, self, dir, point, Globals.vec3_origin,
                            damage, kick / 2, Constants.DAMAGE_NO_KNOCKBACK, Constants.MOD_HIT);

        if (0 == (tr.ent.svflags & Constants.SVF_MONSTER)
                && (null == tr.ent.client)) {
            return false;
        }

        // do our special form of knockback here
        Math3D.VectorMA(self.enemy.absmin, 0.5f, self.enemy.size, v);
        Math3D.VectorSubtract(v, point, v);
        Math3D.VectorNormalize(v);
        Math3D.VectorMA(self.enemy.velocity, kick, v, self.enemy.velocity);
        if (self.enemy.velocity[2] > 0) {
            self.enemy.groundentity = null;
        }
        return true;
    }

    /*
     * ================= fire_bullet
     *
     * Fires a single round. Used for machinegun and chaingun. Would be fine for
     * pistols, rifles, etc.... =================
     */
    public static void fire_bullet(Entity self, float[] start, float[] aimdir,
                                   int damage, int kick, int hspread, int vspread, int mod) {
        fire_lead(self, start, aimdir, damage, kick, Constants.TE_GUNSHOT,
                  hspread, vspread, mod);
    }

    /*
     * =================
     * fire_lead
     *
     * This is an internal support routine used for bullet/pellet based weapons.
     * =================
     */
    public static void fire_lead(Entity self, float[] start, float[] aimdir,
                                 int damage, int kick, int te_impact, int hspread, int vspread,
                                 int mod) {
        Trace tr;
        float[] dir = {0, 0, 0};
        float[] forward = {0, 0, 0}, right = {0, 0, 0}, up = {0, 0, 0};
        float[] end = {0, 0, 0};
        float r;
        float u;
        float[] water_start = {0, 0, 0};
        boolean water = false;
        int content_mask = Constants.MASK_SHOT | Constants.MASK_WATER;

        tr = World.SV_Trace(self.s.origin, null, null, start, self, Constants.MASK_SHOT);
        if (!(tr.fraction < 1.0)) {
            Math3D.vectoangles(aimdir, dir);
            Math3D.AngleVectors(dir, forward, right, up);

            r = Lib.crandom() * hspread;
            u = Lib.crandom() * vspread;
            Math3D.VectorMA(start, 8192, forward, end);
            Math3D.VectorMA(end, r, right, end);
            Math3D.VectorMA(end, u, up, end);

            if ((GameBase.pointcontents.pointcontents(start) & Constants.MASK_WATER) != 0) {
                water = true;
                Math3D.VectorCopy(start, water_start);
                content_mask &= ~Constants.MASK_WATER;
            }

            tr = World.SV_Trace(start, null, null, end, self, content_mask);

            // see if we hit water
            if ((tr.contents & Constants.MASK_WATER) != 0) {
                int color;

                water = true;
                Math3D.VectorCopy(tr.endpos, water_start);

                if (!Math3D.VectorEquals(start, tr.endpos)) {
                    if ((tr.contents & Constants.CONTENTS_WATER) != 0) {
                        if (Lib.strcmp(tr.surface.name, "*brwater") == 0) {
                            color = Constants.SPLASH_BROWN_WATER;
                        } else {
                            color = Constants.SPLASH_BLUE_WATER;
                        }
                    } else if ((tr.contents & Constants.CONTENTS_SLIME) != 0) {
                        color = Constants.SPLASH_SLIME;
                    } else if ((tr.contents & Constants.CONTENTS_LAVA) != 0) {
                        color = Constants.SPLASH_LAVA;
                    } else {
                        color = Constants.SPLASH_UNKNOWN;
                    }

                    if (color != Constants.SPLASH_UNKNOWN) {
                        ServerGame.PF_WriteByte(Constants.svc_temp_entity);
                        ServerGame.PF_WriteByte(Constants.TE_SPLASH);
                        ServerGame.PF_WriteByte(8);
                        ServerGame.PF_WritePos(tr.endpos);
                        ServerGame.PF_WriteDir(tr.plane.normal);
                        ServerGame.PF_WriteByte(color);
                        ServerSend.SV_Multicast(tr.endpos, Constants.MULTICAST_PVS);
                    }

                    // change bullet's course when it enters water
                    Math3D.VectorSubtract(end, start, dir);
                    Math3D.vectoangles(dir, dir);
                    Math3D.AngleVectors(dir, forward, right, up);
                    r = Lib.crandom() * hspread * 2;
                    u = Lib.crandom() * vspread * 2;
                    Math3D.VectorMA(water_start, 8192, forward, end);
                    Math3D.VectorMA(end, r, right, end);
                    Math3D.VectorMA(end, u, up, end);
                }

                // re-trace ignoring water this time
                tr = World.SV_Trace(water_start, null, null, end, self, Constants.MASK_SHOT);
            }
        }

        // send gun puff / flash
        if (!((tr.surface != null) && 0 != (tr.surface.flags & Constants.SURF_SKY))) {
            if (tr.fraction < 1.0) {
                if (tr.ent.takedamage != 0) {
                    GameCombat.T_Damage(tr.ent, self, self, aimdir, tr.endpos,
                                        tr.plane.normal, damage, kick,
                                        Constants.DAMAGE_BULLET, mod);
                } else {
                    if (!"sky".equals(tr.surface.name)) {
                        ServerGame.PF_WriteByte(Constants.svc_temp_entity);
                        ServerGame.PF_WriteByte(te_impact);
                        ServerGame.PF_WritePos(tr.endpos);
                        ServerGame.PF_WriteDir(tr.plane.normal);
                        ServerSend.SV_Multicast(tr.endpos, Constants.MULTICAST_PVS);

                        if (self.client != null) {
                            PlayerWeapon.PlayerNoise(self, tr.endpos,
                                                     Constants.PNOISE_IMPACT);
                        }
                    }
                }
            }
        }

        // if went through water, determine where the end and make a bubble
        // trail
        if (water) {
            float[] pos = {0, 0, 0};

            Math3D.VectorSubtract(tr.endpos, water_start, dir);
            Math3D.VectorNormalize(dir);
            Math3D.VectorMA(tr.endpos, -2, dir, pos);
            if ((GameBase.pointcontents.pointcontents(pos) & Constants.MASK_WATER) != 0) {
                Math3D.VectorCopy(pos, tr.endpos);
            } else {
                tr = World.SV_Trace(pos, null, null, water_start, tr.ent, Constants.MASK_WATER);
            }

            Math3D.VectorAdd(water_start, tr.endpos, pos);
            Math3D.VectorScale(pos, 0.5f, pos);

            ServerGame.PF_WriteByte(Constants.svc_temp_entity);
            ServerGame.PF_WriteByte(Constants.TE_BUBBLETRAIL);
            ServerGame.PF_WritePos(water_start);
            ServerGame.PF_WritePos(tr.endpos);
            ServerSend.SV_Multicast(pos, Constants.MULTICAST_PVS);
        }
    }

    /*
     * =================
     * fire_shotgun
     *
     * Shoots shotgun pellets. Used by shotgun and super shotgun.
     * =================
     */
    public static void fire_shotgun(Entity self, float[] start,
                                    float[] aimdir, int damage, int kick, int hspread, int vspread,
                                    int count, int mod) {
        int i;

        for (i = 0; i < count; i++) {
            fire_lead(self, start, aimdir, damage, kick, Constants.TE_SHOTGUN,
                      hspread, vspread, mod);
        }
    }

    public static void fire_blaster(Entity self, float[] start, float[] dir,
                                    int damage, int speed, int effect, boolean hyper) {
        Entity bolt;
        Trace tr;

        Math3D.VectorNormalize(dir);

        bolt = GameUtil.G_Spawn();
        bolt.svflags = Constants.SVF_DEADMONSTER;
        // yes, I know it looks weird that projectiles are deadmonsters
        // what this means is that when prediction is used against the object
        // (blaster/hyperblaster shots), the player won't be solid clipped
        // against
        // the object. Right now trying to run into a firing hyperblaster
        // is very jerky since you are predicted 'against' the shots.
        Math3D.VectorCopy(start, bolt.s.origin);
        Math3D.VectorCopy(start, bolt.s.old_origin);
        Math3D.vectoangles(dir, bolt.s.angles);
        Math3D.VectorScale(dir, speed, bolt.velocity);
        bolt.movetype = Constants.MOVETYPE_FLYMISSILE;
        bolt.clipmask = Constants.MASK_SHOT;
        bolt.solid = Constants.SOLID_BBOX;
        bolt.s.effects |= effect;
        Math3D.VectorClear(bolt.mins);
        Math3D.VectorClear(bolt.maxs);
        bolt.s.modelindex = ServerInit.SV_ModelIndex("models/objects/laser/tris.md2");
        bolt.s.sound = ServerInit.SV_SoundIndex("misc/lasfly.wav");
        bolt.owner = self;
        bolt.touch = blaster_touch;
        bolt.nextthink = GameBase.level.time + 2;
        bolt.think = GameUtil.G_FreeEdictA;
        bolt.dmg = damage;
        bolt.classname = "bolt";
        if (hyper) {
            bolt.spawnflags = 1;
        }
        World.SV_LinkEdict(bolt);

        if (self.client != null) {
            check_dodge(self, bolt.s.origin, dir, speed);
        }

        tr = World.SV_Trace(self.s.origin, null, null, bolt.s.origin, bolt, Constants.MASK_SHOT);
        if (tr.fraction < 1.0) {
            Math3D.VectorMA(bolt.s.origin, -10, dir, bolt.s.origin);
            bolt.touch.touch(bolt, tr.ent, GameBase.dummyplane, null);
        }
    }

    /*
     * =================
     * fire_blaster
     *
     * Fires a single blaster bolt. Used by the blaster and hyper blaster.
     * =================
     */

    /*
     * =================
     * check_dodge
     *
     * This is a support routine used when a client is firing a non-instant
     * attack weapon. It checks to see if a monster's dodge function should be
     * called.
     * =================
     */
    static void check_dodge(Entity self, float[] start, float[] dir, int speed) {
        float[] end = {0, 0, 0};
        float[] v = {0, 0, 0};
        Trace tr;
        float eta;

        // easy mode only ducks one quarter the time
        if (GameBase.skill.value == 0) {
            if (Lib.random() > 0.25) {
                return;
            }
        }
        Math3D.VectorMA(start, 8192, dir, end);
        tr = World.SV_Trace(start, null, null, end, self, Constants.MASK_SHOT);
        if ((tr.ent != null) && (tr.ent.svflags & Constants.SVF_MONSTER) != 0
                && (tr.ent.health > 0) && (null != tr.ent.monsterinfo.dodge)
                && GameUtil.infront(tr.ent, self)) {
            Math3D.VectorSubtract(tr.endpos, start, v);
            eta = (Math3D.VectorLength(v) - tr.ent.maxs[0]) / speed;
            tr.ent.monsterinfo.dodge.dodge(tr.ent, self, eta);
        }
    }

    public static void fire_grenade(Entity self, float[] start,
                                    float[] aimdir, int damage, int speed, float timer,
                                    float damage_radius) {
        Entity grenade;
        float[] dir = {0, 0, 0};
        float[] forward = {0, 0, 0}, right = {0, 0, 0}, up = {0, 0, 0};

        Math3D.vectoangles(aimdir, dir);
        Math3D.AngleVectors(dir, forward, right, up);

        grenade = GameUtil.G_Spawn();
        Math3D.VectorCopy(start, grenade.s.origin);
        Math3D.VectorScale(aimdir, speed, grenade.velocity);
        Math3D.VectorMA(grenade.velocity, 200f + Lib.crandom() * 10.0f, up,
                        grenade.velocity);
        Math3D.VectorMA(grenade.velocity, Lib.crandom() * 10.0f, right,
                        grenade.velocity);
        Math3D.VectorSet(grenade.avelocity, 300, 300, 300);
        grenade.movetype = Constants.MOVETYPE_BOUNCE;
        grenade.clipmask = Constants.MASK_SHOT;
        grenade.solid = Constants.SOLID_BBOX;
        grenade.s.effects |= Constants.EF_GRENADE;
        Math3D.VectorClear(grenade.mins);
        Math3D.VectorClear(grenade.maxs);
        grenade.s.modelindex = ServerInit.SV_ModelIndex("models/objects/grenade/tris.md2");
        grenade.owner = self;
        grenade.touch = Grenade_Touch;
        grenade.nextthink = GameBase.level.time + timer;
        grenade.think = Grenade_Explode;
        grenade.dmg = damage;
        grenade.dmg_radius = damage_radius;
        grenade.classname = "grenade";

        World.SV_LinkEdict(grenade);
    }

    public static void fire_grenade2(Entity self, float[] start,
                                     float[] aimdir, int damage, int speed, float timer,
                                     float damage_radius, boolean held) {
        Entity grenade;
        float[] dir = {0, 0, 0};
        float[] forward = {0, 0, 0}, right = {0, 0, 0}, up = {0, 0, 0};

        Math3D.vectoangles(aimdir, dir);
        Math3D.AngleVectors(dir, forward, right, up);

        grenade = GameUtil.G_Spawn();
        Math3D.VectorCopy(start, grenade.s.origin);
        Math3D.VectorScale(aimdir, speed, grenade.velocity);
        Math3D.VectorMA(grenade.velocity, 200f + Lib.crandom() * 10.0f, up,
                        grenade.velocity);
        Math3D.VectorMA(grenade.velocity, Lib.crandom() * 10.0f, right,
                        grenade.velocity);
        Math3D.VectorSet(grenade.avelocity, 300f, 300f, 300f);
        grenade.movetype = Constants.MOVETYPE_BOUNCE;
        grenade.clipmask = Constants.MASK_SHOT;
        grenade.solid = Constants.SOLID_BBOX;
        grenade.s.effects |= Constants.EF_GRENADE;
        Math3D.VectorClear(grenade.mins);
        Math3D.VectorClear(grenade.maxs);
        grenade.s.modelindex = ServerInit.SV_ModelIndex("models/objects/grenade2/tris.md2");
        grenade.owner = self;
        grenade.touch = Grenade_Touch;
        grenade.nextthink = GameBase.level.time + timer;
        grenade.think = Grenade_Explode;
        grenade.dmg = damage;
        grenade.dmg_radius = damage_radius;
        grenade.classname = "hgrenade";
        if (held) {
            grenade.spawnflags = 3;
        } else {
            grenade.spawnflags = 1;
        }
        grenade.s.sound = ServerInit.SV_SoundIndex("weapons/hgrenc1b.wav");

        if (timer <= 0.0) {
            Grenade_Explode.think(grenade);
        } else {
            ServerGame.PF_StartSound(self, Constants.CHAN_WEAPON, ServerInit.SV_SoundIndex("weapons/hgrent1a.wav"), (float) 1, (float) Constants.ATTN_NORM,
                                     (float) 0);
            World.SV_LinkEdict(grenade);
        }
    }

    public static void fire_rocket(Entity self, float[] start, float[] dir,
                                   int damage, int speed, float damage_radius, int radius_damage) {
        Entity rocket;

        rocket = GameUtil.G_Spawn();
        Math3D.VectorCopy(start, rocket.s.origin);
        Math3D.VectorCopy(dir, rocket.movedir);
        Math3D.vectoangles(dir, rocket.s.angles);
        Math3D.VectorScale(dir, speed, rocket.velocity);
        rocket.movetype = Constants.MOVETYPE_FLYMISSILE;
        rocket.clipmask = Constants.MASK_SHOT;
        rocket.solid = Constants.SOLID_BBOX;
        rocket.s.effects |= Constants.EF_ROCKET;
        Math3D.VectorClear(rocket.mins);
        Math3D.VectorClear(rocket.maxs);
        rocket.s.modelindex = ServerInit.SV_ModelIndex("models/objects/rocket/tris.md2");
        rocket.owner = self;
        rocket.touch = rocket_touch;
        rocket.nextthink = GameBase.level.time + 8000 / speed;
        rocket.think = GameUtil.G_FreeEdictA;
        rocket.dmg = damage;
        rocket.radius_dmg = radius_damage;
        rocket.dmg_radius = damage_radius;
        rocket.s.sound = ServerInit.SV_SoundIndex("weapons/rockfly.wav");
        rocket.classname = "rocket";

        if (self.client != null) {
            check_dodge(self, rocket.s.origin, dir, speed);
        }

        World.SV_LinkEdict(rocket);
    }

    /*
     * =================
     * fire_rail
     * =================
     */
    public static void fire_rail(Entity self, float[] start, float[] aimdir,
                                 int damage, int kick) {
        float[] from = {0, 0, 0};
        float[] end = {0, 0, 0};
        Trace tr = null;
        Entity ignore;
        int mask;
        boolean water;

        Math3D.VectorMA(start, 8192f, aimdir, end);
        Math3D.VectorCopy(start, from);
        ignore = self;
        water = false;
        mask = Constants.MASK_SHOT | Constants.CONTENTS_SLIME
                | Constants.CONTENTS_LAVA;
        while (ignore != null) {
            tr = World.SV_Trace(from, null, null, end, ignore, mask);

            if ((tr.contents & (Constants.CONTENTS_SLIME | Constants.CONTENTS_LAVA)) != 0) {
                mask &= ~(Constants.CONTENTS_SLIME | Constants.CONTENTS_LAVA);
                water = true;
            } else {
                //ZOID--added so rail goes through SOLID_BBOX entities (gibs,
                // etc)
                if ((tr.ent.svflags & Constants.SVF_MONSTER) != 0
                        || (tr.ent.client != null)
                        || (tr.ent.solid == Constants.SOLID_BBOX)) {
                    ignore = tr.ent;
                } else {
                    ignore = null;
                }

                if ((tr.ent != self) && (tr.ent.takedamage != 0)) {
                    GameCombat.T_Damage(tr.ent, self, self, aimdir, tr.endpos,
                                        tr.plane.normal, damage, kick, 0,
                                        Constants.MOD_RAILGUN);
                }
            }

            Math3D.VectorCopy(tr.endpos, from);
        }

        // send gun puff / flash
        ServerGame.PF_WriteByte(Constants.svc_temp_entity);
        ServerGame.PF_WriteByte(Constants.TE_RAILTRAIL);
        ServerGame.PF_WritePos(start);
        ServerGame.PF_WritePos(tr.endpos);
        ServerSend.SV_Multicast(self.s.origin, Constants.MULTICAST_PHS);
        // gi.multicast (start, MULTICAST_PHS);
        if (water) {
            ServerGame.PF_WriteByte(Constants.svc_temp_entity);
            ServerGame.PF_WriteByte(Constants.TE_RAILTRAIL);
            ServerGame.PF_WritePos(start);
            ServerGame.PF_WritePos(tr.endpos);
            ServerSend.SV_Multicast(tr.endpos, Constants.MULTICAST_PHS);
        }

        if (self.client != null) {
            PlayerWeapon.PlayerNoise(self, tr.endpos, Constants.PNOISE_IMPACT);
        }
    }

    public static void fire_bfg(Entity self, float[] start, float[] dir,
                                int damage, int speed, float damage_radius) {
        Entity bfg;

        bfg = GameUtil.G_Spawn();
        Math3D.VectorCopy(start, bfg.s.origin);
        Math3D.VectorCopy(dir, bfg.movedir);
        Math3D.vectoangles(dir, bfg.s.angles);
        Math3D.VectorScale(dir, speed, bfg.velocity);
        bfg.movetype = Constants.MOVETYPE_FLYMISSILE;
        bfg.clipmask = Constants.MASK_SHOT;
        bfg.solid = Constants.SOLID_BBOX;
        bfg.s.effects |= Constants.EF_BFG | Constants.EF_ANIM_ALLFAST;
        Math3D.VectorClear(bfg.mins);
        Math3D.VectorClear(bfg.maxs);
        bfg.s.modelindex = ServerInit.SV_ModelIndex("sprites/s_bfg1.sp2");
        bfg.owner = self;
        bfg.touch = bfg_touch;
        bfg.nextthink = GameBase.level.time + 8000 / speed;
        bfg.think = GameUtil.G_FreeEdictA;
        bfg.radius_dmg = damage;
        bfg.dmg_radius = damage_radius;
        bfg.classname = "bfg blast";
        bfg.s.sound = ServerInit.SV_SoundIndex("weapons/bfg__l1a.wav");

        bfg.think = bfg_think;
        bfg.nextthink = GameBase.level.time + Constants.FRAMETIME;
        bfg.teammaster = bfg;
        bfg.teamchain = null;

        if (self.client != null) {
            check_dodge(self, bfg.s.origin, dir, speed);
        }

        World.SV_LinkEdict(bfg);
    }
}

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
import com.googlecode.gwtquake.shared.game.adapters.EntityTouchAdapter;
import com.googlecode.gwtquake.shared.game.adapters.EntityUseAdapter;
import com.googlecode.gwtquake.shared.server.ServerGame;
import com.googlecode.gwtquake.shared.server.ServerInit;
import com.googlecode.gwtquake.shared.server.World;
import com.googlecode.gwtquake.shared.util.Lib;
import com.googlecode.gwtquake.shared.util.Math3D;

public class GameTrigger {

    public static final int PUSH_ONCE = 1;
    // the wait time has passed, so set back up for another activation
    public static EntityThinkAdapter multi_wait = new EntityThinkAdapter() {
        public String getID() {
            return "multi_wait";
        }

        public boolean think(Entity ent) {

            ent.nextthink = 0;
            return true;
        }
    };
    /**
     * QUAKED trigger_relay (.5 .5 .5) (-8 -8 -8) (8 8 8) This fixed size
     * trigger cannot be touched, it can only be fired by other events.
     */
    public static EntityUseAdapter trigger_relay_use = new EntityUseAdapter() {
        public String getID() {
            return "trigger_relay_use";
        }

        public void use(Entity self, Entity other, Entity activator) {
            GameUtil.G_UseTargets(self, activator);
        }
    };
    public static int windsound;
    static EntityUseAdapter Use_Multi = new EntityUseAdapter() {
        public String getID() {
            return "Use_Multi";
        }

        public void use(Entity ent, Entity other, Entity activator) {
            ent.activator = activator;
            multi_trigger(ent);
        }
    };
    static EntityTouchAdapter Touch_Multi = new EntityTouchAdapter() {
        public String getID() {
            return "Touch_Multi";
        }

        public void touch(Entity self, Entity other, Plane plane,
                          Surface surf) {
            if (other.client != null) {
                if ((self.spawnflags & 2) != 0) {
                    return;
                }
            } else if ((other.svflags & Constants.SVF_MONSTER) != 0) {
                if (0 == (self.spawnflags & 1)) {
                    return;
                }
            } else {
                return;
            }

            if (!Math3D.VectorEquals(self.movedir, Globals.vec3_origin)) {
                float[] forward = {0, 0, 0};

                Math3D.AngleVectors(other.s.angles, forward, null, null);
                if (Math3D.DotProduct(forward, self.movedir) < 0) {
                    return;
                }
            }

            self.activator = other;
            multi_trigger(self);
        }
    };
    /**
     * QUAKED trigger_multiple (.5 .5 .5) ? MONSTER NOT_PLAYER TRIGGERED
     * Variable sized repeatable trigger. Must be targeted at one or more
     * entities. If "delay" is set, the trigger waits some time after activating
     * before firing. "wait" : Seconds between triggerings. (.2 default) sounds
     * 1) secret 2) beep beep 3) large switch 4) set "message" to text string
     */
    static EntityUseAdapter trigger_enable = new EntityUseAdapter() {
        public String getID() {
            return "trigger_enable";
        }

        public void use(Entity self, Entity other, Entity activator) {
            self.solid = Constants.SOLID_TRIGGER;
            self.use = Use_Multi;
            World.SV_LinkEdict(self);
        }
    };

    /*
     * ==============================================================================
     *
     * trigger_always
     *
     * ==============================================================================
     */
    /**
     * QUAKED trigger_key (.5 .5 .5) (-8 -8 -8) (8 8 8) A relay trigger that
     * only fires it's targets if player has the proper key. Use "item" to
     * specify the required key, for example "key_data_cd"
     */

    static EntityUseAdapter trigger_key_use = new EntityUseAdapter() {
        public String getID() {
            return "trigger_key_use";
        }

        public void use(Entity self, Entity other, Entity activator) {
            int index;

            if (self.item == null) {
                return;
            }
            if (activator.client == null) {
                return;
            }

            index = GameItems.ITEM_INDEX(self.item);
            if (activator.client.pers.inventory[index] == 0) {
                if (GameBase.level.time < self.touch_debounce_time) {
                    return;
                }
                self.touch_debounce_time = GameBase.level.time + 5.0f;
                ServerGame.PF_centerprintf(activator, "You need the "
                        + self.item.pickup_name);
                ServerGame.PF_StartSound(activator, Constants.CHAN_AUTO, ServerInit.SV_SoundIndex("misc/keytry.wav"), (float) 1, (float) Constants.ATTN_NORM,
                                         (float) 0);
                return;
            }

            ServerGame.PF_StartSound(activator, Constants.CHAN_AUTO, ServerInit.SV_SoundIndex("misc/keyuse.wav"), (float) 1, (float) Constants.ATTN_NORM,
                                     (float) 0);
            if (GameBase.coop.value != 0) {
                int player;
                Entity ent;

                if (Lib.strcmp(self.item.classname, "key_power_cube") == 0) {
                    int cube;

                    for (cube = 0; cube < 8; cube++) {
                        if ((activator.client.pers.power_cubes & (1 << cube)) != 0) {
                            break;
                        }
                    }
                    for (player = 1; player <= GameBase.game.maxclients; player++) {
                        ent = GameBase.g_edicts[player];
                        if (!ent.inuse) {
                            continue;
                        }
                        if (null == ent.client) {
                            continue;
                        }
                        if ((ent.client.pers.power_cubes & (1 << cube)) != 0) {
                            ent.client.pers.inventory[index]--;
                            ent.client.pers.power_cubes &= ~(1 << cube);
                        }
                    }
                } else {
                    for (player = 1; player <= GameBase.game.maxclients; player++) {
                        ent = GameBase.g_edicts[player];
                        if (!ent.inuse) {
                            continue;
                        }
                        if (ent.client == null) {
                            continue;
                        }
                        ent.client.pers.inventory[index] = 0;
                    }
                }
            } else {
                activator.client.pers.inventory[index]--;
            }

            GameUtil.G_UseTargets(self, activator);

            self.use = null;
        }
    };
    /**
     * QUAKED trigger_counter (.5 .5 .5) ? nomessage Acts as an intermediary for
     * an action that takes multiple inputs.
     * <p>
     * If nomessage is not set, t will print "1 more.. " etc when triggered and
     * "sequence complete" when finished.
     * <p>
     * After the counter has been triggered "count" times (default 2), it will
     * fire all of it's targets and remove itself.
     */
    static EntityUseAdapter trigger_counter_use = new EntityUseAdapter() {
        public String getID() {
            return "trigger_counter_use";
        }

        public void use(Entity self, Entity other, Entity activator) {
            if (self.count == 0) {
                return;
            }

            self.count--;

            if (self.count != 0) {
                if (0 == (self.spawnflags & 1)) {
                    ServerGame.PF_centerprintf(activator, self.count
                            + " more to go...");
                    ServerGame.PF_StartSound(activator, Constants.CHAN_AUTO, ServerInit.SV_SoundIndex("misc/talk1.wav"), (float) 1, (float) Constants.ATTN_NORM,
                                             (float) 0);
                }
                return;
            }

            if (0 == (self.spawnflags & 1)) {
                ServerGame.PF_centerprintf(activator, "Sequence completed!");
                ServerGame.PF_StartSound(activator, Constants.CHAN_AUTO, ServerInit.SV_SoundIndex("misc/talk1.wav"), (float) 1, (float) Constants.ATTN_NORM,
                                         (float) 0);
            }
            self.activator = activator;
            multi_trigger(self);
        }
    };
    static EntityTouchAdapter trigger_push_touch = new EntityTouchAdapter() {
        public String getID() {
            return "trigger_push_touch";
        }

        public void touch(Entity self, Entity other, Plane plane,
                          Surface surf) {
            if (Lib.strcmp(other.classname, "grenade") == 0) {
                Math3D.VectorScale(self.movedir, self.speed * 10,
                                   other.velocity);
            } else if (other.health > 0) {
                Math3D.VectorScale(self.movedir, self.speed * 10,
                                   other.velocity);

                if (other.client != null) {
                    // don't take falling damage immediately from this
                    Math3D.VectorCopy(other.velocity, other.client.oldvelocity);
                    if (other.fly_sound_debounce_time < GameBase.level.time) {
                        other.fly_sound_debounce_time = GameBase.level.time + 1.5f;
                        ServerGame.PF_StartSound(other, Constants.CHAN_AUTO, windsound, (float) 1, (float) Constants.ATTN_NORM,
                                                 (float) 0);
                    }
                }
            }
            if ((self.spawnflags & PUSH_ONCE) != 0) {
                GameUtil.G_FreeEdict(self);
            }
        }
    };
    /**
     * QUAKED trigger_hurt (.5 .5 .5) ? START_OFF TOGGLE SILENT NO_PROTECTION
     * SLOW Any entity that touches this will be hurt.
     * <p>
     * It does dmg points of damage each server frame
     * <p>
     * SILENT supresses playing the sound SLOW changes the damage rate to once
     * per second NO_PROTECTION *nothing* stops the damage
     * <p>
     * "dmg" default 5 (whole numbers only)
     */
    static EntityUseAdapter hurt_use = new EntityUseAdapter() {
        public String getID() {
            return "hurt_use";
        }

        public void use(Entity self, Entity other, Entity activator) {
            if (self.solid == Constants.SOLID_NOT) {
                self.solid = Constants.SOLID_TRIGGER;
            } else {
                self.solid = Constants.SOLID_NOT;
            }
            World.SV_LinkEdict(self);

            if (0 == (self.spawnflags & 2)) {
                self.use = null;
            }
        }
    };
    static EntityTouchAdapter hurt_touch = new EntityTouchAdapter() {
        public String getID() {
            return "hurt_touch";
        }

        public void touch(Entity self, Entity other, Plane plane,
                          Surface surf) {
            int dflags;

            if (other.takedamage == 0) {
                return;
            }

            if (self.timestamp > GameBase.level.time) {
                return;
            }

            if ((self.spawnflags & 16) != 0) {
                self.timestamp = GameBase.level.time + 1;
            } else {
                self.timestamp = GameBase.level.time + Constants.FRAMETIME;
            }

            if (0 == (self.spawnflags & 4)) {
                if ((GameBase.level.framenum % 10) == 0) {
                    ServerGame.PF_StartSound(other, Constants.CHAN_AUTO, self.noise_index, (float) 1, (float) Constants.ATTN_NORM,
                                             (float) 0);
                }
            }

            if ((self.spawnflags & 8) != 0) {
                dflags = Constants.DAMAGE_NO_PROTECTION;
            } else {
                dflags = 0;
            }
            GameCombat.T_Damage(other, self, self, Globals.vec3_origin,
                                other.s.origin, Globals.vec3_origin, self.dmg, self.dmg,
                                dflags, Constants.MOD_TRIGGER_HURT);
        }
    };
    /**
     * QUAKED trigger_gravity (.5 .5 .5) ? Changes the touching entites gravity
     * to the value of "gravity". 1.0 is standard gravity for the level.
     */

    static EntityTouchAdapter trigger_gravity_touch = new EntityTouchAdapter() {
        public String getID() {
            return "trigger_gravity_touch";
        }

        public void touch(Entity self, Entity other, Plane plane,
                          Surface surf) {
            other.gravity = self.gravity;
        }
    };
    /**
     * QUAKED trigger_monsterjump (.5 .5 .5) ? Walking monsters that touch this
     * will jump in the direction of the trigger's angle "speed" default to 200,
     * the speed thrown forward "height" default to 200, the speed thrown
     * upwards
     */

    static EntityTouchAdapter trigger_monsterjump_touch = new EntityTouchAdapter() {
        public String getID() {
            return "trigger_monsterjump_touch";
        }

        public void touch(Entity self, Entity other, Plane plane,
                          Surface surf) {
            if ((other.flags & (Constants.FL_FLY | Constants.FL_SWIM)) != 0) {
                return;
            }
            if ((other.svflags & Constants.SVF_DEADMONSTER) != 0) {
                return;
            }
            if (0 == (other.svflags & Constants.SVF_MONSTER)) {
                return;
            }

            // set XY even if not on ground, so the jump will clear lips
            other.velocity[0] = self.movedir[0] * self.speed;
            other.velocity[1] = self.movedir[1] * self.speed;

            if (other.groundentity != null) {
                return;
            }

            other.groundentity = null;
            other.velocity[2] = self.movedir[2];
        }
    };

    // the trigger was just activated
    // ent.activator should be set to the activator so it can be held through a
    // delay so wait for the delay time before firing
    public static void multi_trigger(Entity ent) {
        if (ent.nextthink != 0) {
            return; // already been triggered
        }

        GameUtil.G_UseTargets(ent, ent.activator);

        if (ent.wait > 0) {
            ent.think = multi_wait;
            ent.nextthink = GameBase.level.time + ent.wait;
        } else { // we can't just remove (self) here, because this is a touch
            // function
            // called while looping through area links...
            ent.touch = null;
            ent.nextthink = GameBase.level.time + Constants.FRAMETIME;
            ent.think = GameUtil.G_FreeEdictA;
        }
    }

    /**
     * QUAKED trigger_once (.5 .5 .5) ? x x TRIGGERED Triggers once, then
     * removes itself. You must set the key "target" to the name of another
     * object in the level that has a matching "targetname".
     * <p>
     * If TRIGGERED, this trigger must be triggered before it is live.
     * <p>
     * sounds 1) secret 2) beep beep 3) large switch 4)
     * <p>
     * "message" string to be displayed when triggered
     */

    public static void SP_trigger_once(Entity ent) {
        // make old maps work because I messed up on flag assignments here
        // triggered was on bit 1 when it should have been on bit 4
        if ((ent.spawnflags & 1) != 0) {
            float[] v = {0, 0, 0};

            Math3D.VectorMA(ent.mins, 0.5f, ent.size, v);
            ent.spawnflags &= ~1;
            ent.spawnflags |= 4;
            ServerGame.PF_dprintf("fixed TRIGGERED flag on " + ent.classname
                                          + " at " + Lib.vtos(v) + "\n");
        }

        ent.wait = -1;
        SP_trigger_multiple(ent);
    }

    public static void SP_trigger_multiple(Entity ent) {
        if (ent.sounds == 1) {
            ent.noise_index = ServerInit.SV_SoundIndex("misc/secret.wav");
        } else if (ent.sounds == 2) {
            ent.noise_index = ServerInit.SV_SoundIndex("misc/talk.wav");
        } else if (ent.sounds == 3) {
            ent.noise_index = ServerInit.SV_SoundIndex("misc/trigger1.wav");
        }

        if (ent.wait == 0) {
            ent.wait = 0.2f;
        }

        ent.touch = Touch_Multi;
        ent.movetype = Constants.MOVETYPE_NONE;
        ent.svflags |= Constants.SVF_NOCLIENT;

        if ((ent.spawnflags & 4) != 0) {
            ent.solid = Constants.SOLID_NOT;
            ent.use = trigger_enable;
        } else {
            ent.solid = Constants.SOLID_TRIGGER;
            ent.use = Use_Multi;
        }

        if (!Math3D.VectorEquals(ent.s.angles, Globals.vec3_origin)) {
            GameBase.G_SetMovedir(ent.s.angles, ent.movedir);
        }

        ServerGame.PF_setmodel(ent, ent.model);
        World.SV_LinkEdict(ent);
    }

    /*
     * ==============================================================================
     *
     * trigger_key
     *
     * ==============================================================================
     */

    public static void SP_trigger_relay(Entity self) {
        self.use = trigger_relay_use;
    }

    public static void SP_trigger_key(Entity self) {
        if (GameBase.st.item == null) {
            ServerGame.PF_dprintf("no key item for trigger_key at "
                                          + Lib.vtos(self.s.origin) + "\n");
            return;
        }
        self.item = GameItems.FindItemByClassname(GameBase.st.item);

        if (null == self.item) {
            ServerGame.PF_dprintf("item " + GameBase.st.item
                                          + " not found for trigger_key at "
                                          + Lib.vtos(self.s.origin) + "\n");
            return;
        }

        if (self.target == null) {
            ServerGame.PF_dprintf(self.classname + " at "
                                          + Lib.vtos(self.s.origin) + " has no target\n");
            return;
        }

        ServerInit.SV_SoundIndex("misc/keytry.wav");
        ServerInit.SV_SoundIndex("misc/keyuse.wav");

        self.use = trigger_key_use;
    }

    /*
     * ==============================================================================
     *
     * trigger_push
     *
     * ==============================================================================
     */

    public static void SP_trigger_counter(Entity self) {
        self.wait = -1;
        if (0 == self.count) {
            self.count = 2;
        }

        self.use = trigger_counter_use;
    }

    /*
     * QUAKED trigger_always (.5 .5 .5) (-8 -8 -8) (8 8 8) This trigger will
     * always fire. It is activated by the world.
     */
    public static void SP_trigger_always(Entity ent) {
        // we must have some delay to make sure our use targets are present
        if (ent.delay < 0.2f) {
            ent.delay = 0.2f;
        }
        GameUtil.G_UseTargets(ent, ent);
    }

    /*
     * QUAKED trigger_push (.5 .5 .5) ? PUSH_ONCE Pushes the player "speed"
     * defaults to 1000
     */
    public static void SP_trigger_push(Entity self) {
        InitTrigger(self);
        windsound = ServerInit.SV_SoundIndex("misc/windfly.wav");
        self.touch = trigger_push_touch;
        if (0 == self.speed) {
            self.speed = 1000;
        }
        World.SV_LinkEdict(self);
    }

    public static void InitTrigger(Entity self) {
        if (!Math3D.VectorEquals(self.s.angles, Globals.vec3_origin)) {
            GameBase.G_SetMovedir(self.s.angles, self.movedir);
        }

        self.solid = Constants.SOLID_TRIGGER;
        self.movetype = Constants.MOVETYPE_NONE;
        ServerGame.PF_setmodel(self, self.model);
        self.svflags = Constants.SVF_NOCLIENT;
    }

    public static void SP_trigger_hurt(Entity self) {
        InitTrigger(self);

        self.noise_index = ServerInit.SV_SoundIndex("world/electro.wav");
        self.touch = hurt_touch;

        if (0 == self.dmg) {
            self.dmg = 5;
        }

        if ((self.spawnflags & 1) != 0) {
            self.solid = Constants.SOLID_NOT;
        } else {
            self.solid = Constants.SOLID_TRIGGER;
        }

        if ((self.spawnflags & 2) != 0) {
            self.use = hurt_use;
        }

        World.SV_LinkEdict(self);
    }

    /*
     * ==============================================================================
     *
     * trigger_gravity
     *
     * ==============================================================================
     */

    public static void SP_trigger_gravity(Entity self) {
        if (GameBase.st.gravity == null) {
            ServerGame.PF_dprintf("trigger_gravity without gravity set at "
                                          + Lib.vtos(self.s.origin) + "\n");
            GameUtil.G_FreeEdict(self);
            return;
        }

        InitTrigger(self);
        self.gravity = Lib.atoi(GameBase.st.gravity);
        self.touch = trigger_gravity_touch;
    }

    /*
     * ==============================================================================
     *
     * trigger_monsterjump
     *
     * ==============================================================================
     */

    public static void SP_trigger_monsterjump(Entity self) {
        if (0 == self.speed) {
            self.speed = 200;
        }
        if (0 == GameBase.st.height) {
            GameBase.st.height = 200;
        }
        if (self.s.angles[Constants.YAW] == 0) {
            self.s.angles[Constants.YAW] = 360;
        }
        InitTrigger(self);
        self.touch = trigger_monsterjump_touch;
        self.movedir[2] = GameBase.st.height;
    }
}

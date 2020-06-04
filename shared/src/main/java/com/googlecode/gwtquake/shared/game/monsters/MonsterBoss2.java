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
package com.googlecode.gwtquake.shared.game.monsters;

import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.game.Entity;
import com.googlecode.gwtquake.shared.game.Frame;
import com.googlecode.gwtquake.shared.game.GameAI;
import com.googlecode.gwtquake.shared.game.GameBase;
import com.googlecode.gwtquake.shared.game.GameUtil;
import com.googlecode.gwtquake.shared.game.Monster;
import com.googlecode.gwtquake.shared.game.MonsterMove;
import com.googlecode.gwtquake.shared.game.Trace;
import com.googlecode.gwtquake.shared.game.adapters.EntityDieAdapter;
import com.googlecode.gwtquake.shared.game.adapters.EntityThinkAdapter;
import com.googlecode.gwtquake.shared.game.adapters.EntityPainAdapter;
import com.googlecode.gwtquake.shared.server.ServerGame;
import com.googlecode.gwtquake.shared.server.ServerInit;
import com.googlecode.gwtquake.shared.server.World;
import com.googlecode.gwtquake.shared.util.Lib;
import com.googlecode.gwtquake.shared.util.Math3D;


public class MonsterBoss2 {

    public final static int FRAME_stand30 = 0;

    public final static int FRAME_stand31 = 1;

    public final static int FRAME_stand32 = 2;

    public final static int FRAME_stand33 = 3;

    public final static int FRAME_stand34 = 4;

    public final static int FRAME_stand35 = 5;

    public final static int FRAME_stand36 = 6;

    public final static int FRAME_stand37 = 7;

    public final static int FRAME_stand38 = 8;

    public final static int FRAME_stand39 = 9;

    public final static int FRAME_stand40 = 10;

    public final static int FRAME_stand41 = 11;

    public final static int FRAME_stand42 = 12;

    public final static int FRAME_stand43 = 13;

    public final static int FRAME_stand44 = 14;

    public final static int FRAME_stand45 = 15;

    public final static int FRAME_stand46 = 16;

    public final static int FRAME_stand47 = 17;

    public final static int FRAME_stand48 = 18;

    public final static int FRAME_stand49 = 19;

    public final static int FRAME_stand50 = 20;

    public final static int FRAME_stand1 = 21;

    public final static int FRAME_stand2 = 22;

    public final static int FRAME_stand3 = 23;

    public final static int FRAME_stand4 = 24;

    public final static int FRAME_stand5 = 25;

    public final static int FRAME_stand6 = 26;

    public final static int FRAME_stand7 = 27;

    public final static int FRAME_stand8 = 28;

    public final static int FRAME_stand9 = 29;

    public final static int FRAME_stand10 = 30;

    public final static int FRAME_stand11 = 31;

    public final static int FRAME_stand12 = 32;

    public final static int FRAME_stand13 = 33;

    public final static int FRAME_stand14 = 34;

    public final static int FRAME_stand15 = 35;

    public final static int FRAME_stand16 = 36;

    public final static int FRAME_stand17 = 37;

    public final static int FRAME_stand18 = 38;

    public final static int FRAME_stand19 = 39;

    public final static int FRAME_stand20 = 40;

    public final static int FRAME_stand21 = 41;

    public final static int FRAME_stand22 = 42;

    public final static int FRAME_stand23 = 43;

    public final static int FRAME_stand24 = 44;

    public final static int FRAME_stand25 = 45;

    public final static int FRAME_stand26 = 46;

    public final static int FRAME_stand27 = 47;

    public final static int FRAME_stand28 = 48;

    public final static int FRAME_stand29 = 49;

    public final static int FRAME_walk1 = 50;

    public final static int FRAME_walk2 = 51;

    public final static int FRAME_walk3 = 52;

    public final static int FRAME_walk4 = 53;

    public final static int FRAME_walk5 = 54;

    public final static int FRAME_walk6 = 55;

    public final static int FRAME_walk7 = 56;

    public final static int FRAME_walk8 = 57;

    public final static int FRAME_walk9 = 58;

    public final static int FRAME_walk10 = 59;

    public final static int FRAME_walk11 = 60;

    public final static int FRAME_walk12 = 61;

    public final static int FRAME_walk13 = 62;

    public final static int FRAME_walk14 = 63;

    public final static int FRAME_walk15 = 64;

    public final static int FRAME_walk16 = 65;

    public final static int FRAME_walk17 = 66;

    public final static int FRAME_walk18 = 67;

    public final static int FRAME_walk19 = 68;

    public final static int FRAME_walk20 = 69;

    public final static int FRAME_attack1 = 70;

    public final static int FRAME_attack2 = 71;

    public final static int FRAME_attack3 = 72;

    public final static int FRAME_attack4 = 73;

    public final static int FRAME_attack5 = 74;

    public final static int FRAME_attack6 = 75;

    public final static int FRAME_attack7 = 76;

    public final static int FRAME_attack8 = 77;

    public final static int FRAME_attack9 = 78;

    public final static int FRAME_attack10 = 79;

    public final static int FRAME_attack11 = 80;

    public final static int FRAME_attack12 = 81;

    public final static int FRAME_attack13 = 82;

    public final static int FRAME_attack14 = 83;

    public final static int FRAME_attack15 = 84;

    public final static int FRAME_attack16 = 85;

    public final static int FRAME_attack17 = 86;

    public final static int FRAME_attack18 = 87;

    public final static int FRAME_attack19 = 88;

    public final static int FRAME_attack20 = 89;

    public final static int FRAME_attack21 = 90;

    public final static int FRAME_attack22 = 91;

    public final static int FRAME_attack23 = 92;

    public final static int FRAME_attack24 = 93;

    public final static int FRAME_attack25 = 94;

    public final static int FRAME_attack26 = 95;

    public final static int FRAME_attack27 = 96;

    public final static int FRAME_attack28 = 97;

    public final static int FRAME_attack29 = 98;

    public final static int FRAME_attack30 = 99;

    public final static int FRAME_attack31 = 100;

    public final static int FRAME_attack32 = 101;

    public final static int FRAME_attack33 = 102;

    public final static int FRAME_attack34 = 103;

    public final static int FRAME_attack35 = 104;

    public final static int FRAME_attack36 = 105;

    public final static int FRAME_attack37 = 106;

    public final static int FRAME_attack38 = 107;

    public final static int FRAME_attack39 = 108;

    public final static int FRAME_attack40 = 109;

    public final static int FRAME_pain2 = 110;

    public final static int FRAME_pain3 = 111;

    public final static int FRAME_pain4 = 112;

    public final static int FRAME_pain5 = 113;

    public final static int FRAME_pain6 = 114;

    public final static int FRAME_pain7 = 115;

    public final static int FRAME_pain8 = 116;

    public final static int FRAME_pain9 = 117;

    public final static int FRAME_pain10 = 118;

    public final static int FRAME_pain11 = 119;

    public final static int FRAME_pain12 = 120;

    public final static int FRAME_pain13 = 121;

    public final static int FRAME_pain14 = 122;

    public final static int FRAME_pain15 = 123;

    public final static int FRAME_pain16 = 124;

    public final static int FRAME_pain17 = 125;

    public final static int FRAME_pain18 = 126;

    public final static int FRAME_pain19 = 127;

    public final static int FRAME_pain20 = 128;

    public final static int FRAME_pain21 = 129;

    public final static int FRAME_pain22 = 130;

    public final static int FRAME_pain23 = 131;

    public final static int FRAME_death2 = 132;

    public final static int FRAME_death3 = 133;

    public final static int FRAME_death4 = 134;

    public final static int FRAME_death5 = 135;

    public final static int FRAME_death6 = 136;

    public final static int FRAME_death7 = 137;

    public final static int FRAME_death8 = 138;

    public final static int FRAME_death9 = 139;

    public final static int FRAME_death10 = 140;

    public final static int FRAME_death11 = 141;

    public final static int FRAME_death12 = 142;

    public final static int FRAME_death13 = 143;

    public final static int FRAME_death14 = 144;

    public final static int FRAME_death15 = 145;

    public final static int FRAME_death16 = 146;

    public final static int FRAME_death17 = 147;

    public final static int FRAME_death18 = 148;

    public final static int FRAME_death19 = 149;

    public final static int FRAME_death20 = 150;

    public final static int FRAME_death21 = 151;

    public final static int FRAME_death22 = 152;

    public final static int FRAME_death23 = 153;

    public final static int FRAME_death24 = 154;

    public final static int FRAME_death25 = 155;

    public final static int FRAME_death26 = 156;

    public final static int FRAME_death27 = 157;

    public final static int FRAME_death28 = 158;

    public final static int FRAME_death29 = 159;

    public final static int FRAME_death30 = 160;

    public final static int FRAME_death31 = 161;

    public final static int FRAME_death32 = 162;

    public final static int FRAME_death33 = 163;

    public final static int FRAME_death34 = 164;

    public final static int FRAME_death35 = 165;

    public final static int FRAME_death36 = 166;

    public final static int FRAME_death37 = 167;

    public final static int FRAME_death38 = 168;

    public final static int FRAME_death39 = 169;

    public final static int FRAME_death40 = 170;

    public final static int FRAME_death41 = 171;

    public final static int FRAME_death42 = 172;

    public final static int FRAME_death43 = 173;

    public final static int FRAME_death44 = 174;

    public final static int FRAME_death45 = 175;

    public final static int FRAME_death46 = 176;

    public final static int FRAME_death47 = 177;

    public final static int FRAME_death48 = 178;

    public final static int FRAME_death49 = 179;

    public final static int FRAME_death50 = 180;

    public final static float MODEL_SCALE = 1.000000f;

    static int sound_pain1;

    static int sound_pain2;

    static int sound_pain3;

    static int sound_death;

    static int sound_search1;

    static EntityThinkAdapter boss2_stand = new EntityThinkAdapter() {
    	public String getID() { return "boss2_stand"; }
        public boolean think(Entity self) {
            self.monsterinfo.currentmove = boss2_move_stand;
            return true;
        }
    };

    static EntityThinkAdapter boss2_run = new EntityThinkAdapter() {
    	public String getID() { return "boss2_run"; }
        public boolean think(Entity self) {
            if ((self.monsterinfo.aiflags & Constants.AI_STAND_GROUND) != 0)
                self.monsterinfo.currentmove = boss2_move_stand;
            else
                self.monsterinfo.currentmove = boss2_move_run;
            return true;
        }
    };

    static EntityThinkAdapter boss2_walk = new EntityThinkAdapter() {
    	public String getID() { return "boss2_walk"; }
        public boolean think(Entity self) {
            self.monsterinfo.currentmove = boss2_move_stand;

            self.monsterinfo.currentmove = boss2_move_walk;
            return true;
        }
    };

    static EntityThinkAdapter boss2_attack = new EntityThinkAdapter() {
    	public String getID() { return "boss2_attack"; }
        public boolean think(Entity self) {
            float[] vec = { 0, 0, 0 };

            float range;

            Math3D.VectorSubtract(self.enemy.s.origin, self.s.origin, vec);
            range = Math3D.VectorLength(vec);

            if (range <= 125) {
                self.monsterinfo.currentmove = boss2_move_attack_pre_mg;
            } else {
                if (Lib.random() <= 0.6)
                    self.monsterinfo.currentmove = boss2_move_attack_pre_mg;
                else
                    self.monsterinfo.currentmove = boss2_move_attack_rocket;
            }
            return true;
        }
    };

    static EntityThinkAdapter boss2_attack_mg = new EntityThinkAdapter() {
    	public String getID() { return "boss2_attack_mg"; }
        public boolean think(Entity self) {
            self.monsterinfo.currentmove = boss2_move_attack_mg;
            return true;
        }
    };

    static EntityThinkAdapter boss2_reattack_mg = new EntityThinkAdapter() {
    	public String getID() { return "boss2_reattack_mg"; }
        public boolean think(Entity self) {
            if (GameUtil.infront(self, self.enemy))
                if (Lib.random() <= 0.7)
                    self.monsterinfo.currentmove = boss2_move_attack_mg;
                else
                    self.monsterinfo.currentmove = boss2_move_attack_post_mg;
            else
                self.monsterinfo.currentmove = boss2_move_attack_post_mg;
            return true;
        }
    };

    static EntityPainAdapter boss2_pain = new EntityPainAdapter() {
    	public String getID() { return "boss2_pain"; }
        public void pain(Entity self, Entity other, float kick, int damage) {
            if (self.health < (self.max_health / 2))
                self.s.skinnum = 1;

            if (GameBase.level.time < self.pain_debounce_time)
                return;

            self.pain_debounce_time = GameBase.level.time + 3;
            //	   American wanted these at no attenuation
            if (damage < 10) {
                ServerGame.PF_StartSound(self, Constants.CHAN_VOICE, sound_pain3, (float) 1, (float) Constants.ATTN_NONE,
                (float) 0);
                self.monsterinfo.currentmove = boss2_move_pain_light;
            } else if (damage < 30) {
                ServerGame.PF_StartSound(self, Constants.CHAN_VOICE, sound_pain1, (float) 1, (float) Constants.ATTN_NONE,
                (float) 0);
                self.monsterinfo.currentmove = boss2_move_pain_light;
            } else {
                ServerGame.PF_StartSound(self, Constants.CHAN_VOICE, sound_pain2, (float) 1, (float) Constants.ATTN_NONE,
                (float) 0);
                self.monsterinfo.currentmove = boss2_move_pain_heavy;
            }
        }
    };

    static EntityThinkAdapter boss2_dead = new EntityThinkAdapter() {
    	public String getID() { return "boss2_dead"; }
        public boolean think(Entity self) {
            Math3D.VectorSet(self.mins, -56, -56, 0);
            Math3D.VectorSet(self.maxs, 56, 56, 80);
            self.movetype = Constants.MOVETYPE_TOSS;
            self.svflags |= Constants.SVF_DEADMONSTER;
            self.nextthink = 0;
            World.SV_LinkEdict(self);
            return true;
        }
    };

    static EntityDieAdapter boss2_die = new EntityDieAdapter() {
    	public String getID() { return "boss2_die"; }
        public void die(Entity self, Entity inflictor, Entity attacker,
                int damage, float[] point) {
            ServerGame.PF_StartSound(self, Constants.CHAN_VOICE, sound_death, (float) 1, (float) Constants.ATTN_NONE,
            (float) 0);
            self.deadflag = Constants.DEAD_DEAD;
            self.takedamage = Constants.DAMAGE_NO;
            self.count = 0;
            self.monsterinfo.currentmove = boss2_move_death;

        }
    };

    static EntityThinkAdapter Boss2_CheckAttack = new EntityThinkAdapter() {
    	public String getID() { return "Boss2_CheckAttack"; }
        public boolean think(Entity self) {
            float[] spot1 = { 0, 0, 0 }, spot2 = { 0, 0, 0 };
            float[] temp = { 0, 0, 0 };
            float chance;
            Trace tr;
            boolean enemy_infront;
            int enemy_range;
            float enemy_yaw;

            if (self.enemy.health > 0) {
                // see if any entities are in the way of the shot
                Math3D.VectorCopy(self.s.origin, spot1);
                spot1[2] += self.viewheight;
                Math3D.VectorCopy(self.enemy.s.origin, spot2);
                spot2[2] += self.enemy.viewheight;

                tr = World.SV_Trace(spot1, null, null, spot2, self, Constants.CONTENTS_SOLID | Constants.CONTENTS_MONSTER
                | Constants.CONTENTS_SLIME
                | Constants.CONTENTS_LAVA);

                // do we have a clear shot?
                if (tr.ent != self.enemy)
                    return false;
            }

            enemy_infront = GameUtil.infront(self, self.enemy);
            enemy_range = GameUtil.range(self, self.enemy);
            Math3D.VectorSubtract(self.enemy.s.origin, self.s.origin, temp);
            enemy_yaw = Math3D.vectoyaw(temp);

            self.ideal_yaw = enemy_yaw;

            // melee attack
            if (enemy_range == Constants.RANGE_MELEE) {
                if (self.monsterinfo.melee != null)
                    self.monsterinfo.attack_state = Constants.AS_MELEE;
                else
                    self.monsterinfo.attack_state = Constants.AS_MISSILE;
                return true;
            }

            //	   missile attack
            if (self.monsterinfo.attack == null)
                return false;

            if (GameBase.level.time < self.monsterinfo.attack_finished)
                return false;

            if (enemy_range == Constants.RANGE_FAR)
                return false;

            if ((self.monsterinfo.aiflags & Constants.AI_STAND_GROUND) != 0) {
                chance = 0.4f;
            } else if (enemy_range == Constants.RANGE_MELEE) {
                chance = 0.8f;
            } else if (enemy_range == Constants.RANGE_NEAR) {
                chance = 0.8f;
            } else if (enemy_range == Constants.RANGE_MID) {
                chance = 0.8f;
            } else {
                return false;
            }

            if (Lib.random() < chance) {
                self.monsterinfo.attack_state = Constants.AS_MISSILE;
                self.monsterinfo.attack_finished = GameBase.level.time + 2
                        * Lib.random();
                return true;
            }

            if ((self.flags & Constants.FL_FLY) != 0) {
                if (Lib.random() < 0.3)
                    self.monsterinfo.attack_state = Constants.AS_SLIDING;
                else
                    self.monsterinfo.attack_state = Constants.AS_STRAIGHT;
            }

            return false;
        }
    };

    static EntityThinkAdapter boss2_search = new EntityThinkAdapter() {
    	public String getID() { return "boss2_search"; }
        public boolean think(Entity self) {
            if (Lib.random() < 0.5)
              ServerGame.PF_StartSound(self, Constants.CHAN_VOICE, sound_search1, (float) 1, (float) Constants.ATTN_NONE,
              (float) 0);
            return true;
        }
    };

    static EntityThinkAdapter Boss2Rocket = new EntityThinkAdapter() {
    	public String getID() { return "Boss2Rocket"; }
        public boolean think(Entity self) {
            float[] forward = { 0, 0, 0 }, right = { 0, 0, 0 };
            float[] start = { 0, 0, 0 };
            float[] dir = { 0, 0, 0 };
            float[] vec = { 0, 0, 0 };

            Math3D.AngleVectors(self.s.angles, forward, right, null);

            //	  1
            Math3D.G_ProjectSource(self.s.origin,
                    MonsterFlash.monster_flash_offset[Constants.MZ2_BOSS2_ROCKET_1],
                    forward, right, start);
            Math3D.VectorCopy(self.enemy.s.origin, vec);
            vec[2] += self.enemy.viewheight;
            Math3D.VectorSubtract(vec, start, dir);
            Math3D.VectorNormalize(dir);
            Monster.monster_fire_rocket(self, start, dir, 50, 500,
                    Constants.MZ2_BOSS2_ROCKET_1);

            //	  2
            Math3D.G_ProjectSource(self.s.origin,
                    MonsterFlash.monster_flash_offset[Constants.MZ2_BOSS2_ROCKET_2],
                    forward, right, start);
            Math3D.VectorCopy(self.enemy.s.origin, vec);
            vec[2] += self.enemy.viewheight;
            Math3D.VectorSubtract(vec, start, dir);
            Math3D.VectorNormalize(dir);
            Monster.monster_fire_rocket(self, start, dir, 50, 500,
                    Constants.MZ2_BOSS2_ROCKET_2);

            //	  3
            Math3D.G_ProjectSource(self.s.origin,
                    MonsterFlash.monster_flash_offset[Constants.MZ2_BOSS2_ROCKET_3],
                    forward, right, start);
            Math3D.VectorCopy(self.enemy.s.origin, vec);
            vec[2] += self.enemy.viewheight;
            Math3D.VectorSubtract(vec, start, dir);
            Math3D.VectorNormalize(dir);
            Monster.monster_fire_rocket(self, start, dir, 50, 500,
                    Constants.MZ2_BOSS2_ROCKET_3);

            //	  4
            Math3D.G_ProjectSource(self.s.origin,
                    MonsterFlash.monster_flash_offset[Constants.MZ2_BOSS2_ROCKET_4],
                    forward, right, start);
            Math3D.VectorCopy(self.enemy.s.origin, vec);
            vec[2] += self.enemy.viewheight;
            Math3D.VectorSubtract(vec, start, dir);
            Math3D.VectorNormalize(dir);
            Monster.monster_fire_rocket(self, start, dir, 50, 500,
                    Constants.MZ2_BOSS2_ROCKET_4);
            return true;
        }
    };

    static EntityThinkAdapter boss2_firebullet_right = new EntityThinkAdapter() {
    	public String getID() { return "boss2_firebullet_right"; }
        public boolean think(Entity self) {
            float[] forward = { 0, 0, 0 }, right = { 0, 0, 0 }, target = { 0,
                    0, 0 };
            float[] start = { 0, 0, 0 };

            Math3D.AngleVectors(self.s.angles, forward, right, null);
            Math3D
                    .G_ProjectSource(
                            self.s.origin,
                            MonsterFlash.monster_flash_offset[Constants.MZ2_BOSS2_MACHINEGUN_R1],
                            forward, right, start);

            Math3D.VectorMA(self.enemy.s.origin, -0.2f, self.enemy.velocity,
                    target);
            target[2] += self.enemy.viewheight;
            Math3D.VectorSubtract(target, start, forward);
            Math3D.VectorNormalize(forward);

            Monster.monster_fire_bullet(self, start, forward, 6, 4,
                    Constants.DEFAULT_BULLET_HSPREAD,
                    Constants.DEFAULT_BULLET_VSPREAD,
                    Constants.MZ2_BOSS2_MACHINEGUN_R1);

            return true;
        }
    };

    static EntityThinkAdapter boss2_firebullet_left = new EntityThinkAdapter() {
    	public String getID() { return "boss2_firebullet_left"; }
        public boolean think(Entity self) {
            float[] forward = { 0, 0, 0 }, right = { 0, 0, 0 }, target = { 0,
                    0, 0 };
            float[] start = { 0, 0, 0 };

            Math3D.AngleVectors(self.s.angles, forward, right, null);
            Math3D
                    .G_ProjectSource(
                            self.s.origin,
                            MonsterFlash.monster_flash_offset[Constants.MZ2_BOSS2_MACHINEGUN_L1],
                            forward, right, start);

            Math3D.VectorMA(self.enemy.s.origin, -0.2f, self.enemy.velocity,
                    target);

            target[2] += self.enemy.viewheight;
            Math3D.VectorSubtract(target, start, forward);
            Math3D.VectorNormalize(forward);

            Monster.monster_fire_bullet(self, start, forward, 6, 4,
                    Constants.DEFAULT_BULLET_HSPREAD,
                    Constants.DEFAULT_BULLET_VSPREAD,
                    Constants.MZ2_BOSS2_MACHINEGUN_L1);

            return true;
        }
    };

    static EntityThinkAdapter Boss2MachineGun = new EntityThinkAdapter() {
    	public String getID() { return "Boss2MachineGun"; }
        public boolean think(Entity self) {
            /*
             * RST: this was disabled ! float[] forward={0,0,0}, right={0,0,0};
             * float[] start={0,0,0}; float[] dir={0,0,0}; float[] vec={0,0,0};
             * int flash_number;
             * 
             * AngleVectors (self.s.angles, forward, right, null);
             * 
             * flash_number = MZ2_BOSS2_MACHINEGUN_1 + (self.s.frame -
             * FRAME_attack10); G_ProjectSource (self.s.origin,
             * monster_flash_offset[flash_number], forward, right, start);
             * 
             * VectorCopy (self.enemy.s.origin, vec); vec[2] +=
             * self.enemy.viewheight; VectorSubtract (vec, start, dir);
             * VectorNormalize (dir); monster_fire_bullet (self, start, dir, 3,
             * 4, DEFAULT_BULLET_HSPREAD, DEFAULT_BULLET_VSPREAD, flash_number);
             */
            boss2_firebullet_left.think(self);
            boss2_firebullet_right.think(self);
            return true;
        }
    };

    static Frame boss2_frames_stand[] = new Frame[] {
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null) };

    static MonsterMove boss2_move_stand = new MonsterMove(FRAME_stand30, FRAME_stand50,
            boss2_frames_stand, null);

    static Frame boss2_frames_fidget[] = new Frame[] {
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null),
            new Frame(GameAI.ai_stand, 0, null) };

    static MonsterMove boss2_move_fidget = new MonsterMove(FRAME_stand1, FRAME_stand30,
            boss2_frames_fidget, null);

    static Frame boss2_frames_walk[] = new Frame[] {
            new Frame(GameAI.ai_walk, 8, null),
            new Frame(GameAI.ai_walk, 8, null),
            new Frame(GameAI.ai_walk, 8, null),
            new Frame(GameAI.ai_walk, 8, null),
            new Frame(GameAI.ai_walk, 8, null),
            new Frame(GameAI.ai_walk, 8, null),
            new Frame(GameAI.ai_walk, 8, null),
            new Frame(GameAI.ai_walk, 8, null),
            new Frame(GameAI.ai_walk, 8, null),
            new Frame(GameAI.ai_walk, 8, null),
            new Frame(GameAI.ai_walk, 8, null),
            new Frame(GameAI.ai_walk, 8, null),
            new Frame(GameAI.ai_walk, 8, null),
            new Frame(GameAI.ai_walk, 8, null),
            new Frame(GameAI.ai_walk, 8, null),
            new Frame(GameAI.ai_walk, 8, null),
            new Frame(GameAI.ai_walk, 8, null),
            new Frame(GameAI.ai_walk, 8, null),
            new Frame(GameAI.ai_walk, 8, null),
            new Frame(GameAI.ai_walk, 8, null) };

    static MonsterMove boss2_move_walk = new MonsterMove(FRAME_walk1, FRAME_walk20,
            boss2_frames_walk, null);

    static Frame boss2_frames_run[] = new Frame[] {
            new Frame(GameAI.ai_run, 8, null),
            new Frame(GameAI.ai_run, 8, null),
            new Frame(GameAI.ai_run, 8, null),
            new Frame(GameAI.ai_run, 8, null),
            new Frame(GameAI.ai_run, 8, null),
            new Frame(GameAI.ai_run, 8, null),
            new Frame(GameAI.ai_run, 8, null),
            new Frame(GameAI.ai_run, 8, null),
            new Frame(GameAI.ai_run, 8, null),
            new Frame(GameAI.ai_run, 8, null),
            new Frame(GameAI.ai_run, 8, null),
            new Frame(GameAI.ai_run, 8, null),
            new Frame(GameAI.ai_run, 8, null),
            new Frame(GameAI.ai_run, 8, null),
            new Frame(GameAI.ai_run, 8, null),
            new Frame(GameAI.ai_run, 8, null),
            new Frame(GameAI.ai_run, 8, null),
            new Frame(GameAI.ai_run, 8, null),
            new Frame(GameAI.ai_run, 8, null),
            new Frame(GameAI.ai_run, 8, null) };

    static MonsterMove boss2_move_run = new MonsterMove(FRAME_walk1, FRAME_walk20,
            boss2_frames_run, null);

    static Frame boss2_frames_attack_pre_mg[] = new Frame[] {
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, boss2_attack_mg) };

    static MonsterMove boss2_move_attack_pre_mg = new MonsterMove(FRAME_attack1,
            FRAME_attack9, boss2_frames_attack_pre_mg, null);

    //	   Loop this
    static Frame boss2_frames_attack_mg[] = new Frame[] {
            new Frame(GameAI.ai_charge, 1, Boss2MachineGun),
            new Frame(GameAI.ai_charge, 1, Boss2MachineGun),
            new Frame(GameAI.ai_charge, 1, Boss2MachineGun),
            new Frame(GameAI.ai_charge, 1, Boss2MachineGun),
            new Frame(GameAI.ai_charge, 1, Boss2MachineGun),
            new Frame(GameAI.ai_charge, 1, boss2_reattack_mg) };

    static MonsterMove boss2_move_attack_mg = new MonsterMove(FRAME_attack10,
            FRAME_attack15, boss2_frames_attack_mg, null);

    static Frame boss2_frames_attack_post_mg[] = new Frame[] {
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null) };

    static MonsterMove boss2_move_attack_post_mg = new MonsterMove(FRAME_attack16,
            FRAME_attack19, boss2_frames_attack_post_mg, boss2_run);

    static Frame boss2_frames_attack_rocket[] = new Frame[] {
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_move, -20, Boss2Rocket),
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null),
            new Frame(GameAI.ai_charge, 1, null) };

    static MonsterMove boss2_move_attack_rocket = new MonsterMove(FRAME_attack20,
            FRAME_attack40, boss2_frames_attack_rocket, boss2_run);

    static Frame boss2_frames_pain_heavy[] = new Frame[] {
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null) };

    static MonsterMove boss2_move_pain_heavy = new MonsterMove(FRAME_pain2,
            FRAME_pain19, boss2_frames_pain_heavy, boss2_run);

    static Frame boss2_frames_pain_light[] = new Frame[] {
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null) };

    static MonsterMove boss2_move_pain_light = new MonsterMove(FRAME_pain20,
            FRAME_pain23, boss2_frames_pain_light, boss2_run);

    static Frame boss2_frames_death[] = new Frame[] {
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, null),
            new Frame(GameAI.ai_move, 0, MonsterSupertank.BossExplode) };

    /*
     * static EntThinkAdapter xxx = new EntThinkAdapter() { public boolean
     * think(edict_t self) { return true; } };
     */

    static MonsterMove boss2_move_death = new MonsterMove(FRAME_death2, FRAME_death50,
            boss2_frames_death, boss2_dead);

    /*
     * QUAKED monster_boss2 (1 .5 0) (-56 -56 0) (56 56 80) Ambush Trigger_Spawn
     * Sight
     */
    public static void SP_monster_boss2(Entity self) {
        if (GameBase.deathmatch.value != 0) {
            GameUtil.G_FreeEdict(self);
            return;
        }

        sound_pain1 = ServerInit.SV_SoundIndex("bosshovr/bhvpain1.wav");
        sound_pain2 = ServerInit.SV_SoundIndex("bosshovr/bhvpain2.wav");
        sound_pain3 = ServerInit.SV_SoundIndex("bosshovr/bhvpain3.wav");
        sound_death = ServerInit.SV_SoundIndex("bosshovr/bhvdeth1.wav");
        sound_search1 = ServerInit.SV_SoundIndex("bosshovr/bhvunqv1.wav");

        self.s.sound = ServerInit.SV_SoundIndex("bosshovr/bhvengn1.wav");

        self.movetype = Constants.MOVETYPE_STEP;
        self.solid = Constants.SOLID_BBOX;
        self.s.modelindex = ServerInit.SV_ModelIndex("models/monsters/boss2/tris.md2");
        Math3D.VectorSet(self.mins, -56, -56, 0);
        Math3D.VectorSet(self.maxs, 56, 56, 80);

        self.health = 2000;
        self.gib_health = -200;
        self.mass = 1000;

        self.flags |= Constants.FL_IMMUNE_LASER;

        self.pain = boss2_pain;
        self.die = boss2_die;

        self.monsterinfo.stand = boss2_stand;
        self.monsterinfo.walk = boss2_walk;
        self.monsterinfo.run = boss2_run;
        self.monsterinfo.attack = boss2_attack;
        self.monsterinfo.search = boss2_search;
        self.monsterinfo.checkattack = Boss2_CheckAttack;
        World.SV_LinkEdict(self);

        self.monsterinfo.currentmove = boss2_move_stand;
        self.monsterinfo.scale = MODEL_SCALE;

        GameAI.flymonster_start.think(self);
    }
}

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



import java.util.StringTokenizer;

import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.game.adapters.EntInteractAdapter;
import com.googlecode.gwtquake.shared.game.adapters.EntityThinkAdapter;
import com.googlecode.gwtquake.shared.game.adapters.EntityTouchAdapter;
import com.googlecode.gwtquake.shared.game.adapters.EntityUseAdapter;
import com.googlecode.gwtquake.shared.game.adapters.ItemDropAdapter;
import com.googlecode.gwtquake.shared.game.adapters.ItemUseAdapter;
import com.googlecode.gwtquake.shared.server.ServerGame;
import com.googlecode.gwtquake.shared.server.ServerInit;
import com.googlecode.gwtquake.shared.server.World;
import com.googlecode.gwtquake.shared.util.Lib;
import com.googlecode.gwtquake.shared.util.Math3D;


public class GameItems {

    public static GameItemArmor jacketarmor_info = new GameItemArmor(25, 50,
    .30f, .00f, Constants.ARMOR_JACKET);
    public static GameItemArmor combatarmor_info = new GameItemArmor(50, 100,
    .60f, .30f, Constants.ARMOR_COMBAT);
    public static GameItemArmor bodyarmor_info = new GameItemArmor(100, 200,
    .80f, .60f, Constants.ARMOR_BODY);
    static int quad_drop_timeout_hack = 0;
    static int jacket_armor_index;
    static int combat_armor_index;
    static int body_armor_index;
    static int power_screen_index;
    static int power_shield_index;
    
    static EntityThinkAdapter DoRespawn = new EntityThinkAdapter() {
        public String getID() { return "do_respawn";}
        public boolean think(Entity ent) {
            if (ent.team != null) {
                Entity master;
                int count;
                int choice = 0;
    
                master = ent.teammaster;
    
                // count the depth
                for (count = 0, ent = master; ent != null; ent = ent.chain, count++)
                    ;
    
                choice = Lib.rand() % count;
    
                for (count = 0, ent = master; count < choice; ent = ent.chain, count++)
                    ;
            }
    
            ent.svflags &= ~Constants.SVF_NOCLIENT;
            ent.solid = Constants.SOLID_TRIGGER;
            World.SV_LinkEdict(ent);
    
            // send an effect
            ent.s.event = Constants.EV_ITEM_RESPAWN;
    
            return false;
        }
    };
    static EntInteractAdapter Pickup_Pack = new EntInteractAdapter() {
        public String getID() { return "pickup_pack";}
        public boolean interact(Entity ent, Entity other) {
    
            GameItem item;
            int index;
    
            if (other.client.pers.max_bullets < 300)
                other.client.pers.max_bullets = 300;
            if (other.client.pers.max_shells < 200)
                other.client.pers.max_shells = 200;
            if (other.client.pers.max_rockets < 100)
                other.client.pers.max_rockets = 100;
            if (other.client.pers.max_grenades < 100)
                other.client.pers.max_grenades = 100;
            if (other.client.pers.max_cells < 300)
                other.client.pers.max_cells = 300;
            if (other.client.pers.max_slugs < 100)
                other.client.pers.max_slugs = 100;
    
            item = FindItem("Bullets");
            if (item != null) {
                index = ITEM_INDEX(item);
                other.client.pers.inventory[index] += item.quantity;
                if (other.client.pers.inventory[index] > other.client.pers.max_bullets)
                    other.client.pers.inventory[index] = other.client.pers.max_bullets;
            }
    
            item = FindItem("Shells");
            if (item != null) {
                index = ITEM_INDEX(item);
                other.client.pers.inventory[index] += item.quantity;
                if (other.client.pers.inventory[index] > other.client.pers.max_shells)
                    other.client.pers.inventory[index] = other.client.pers.max_shells;
            }
    
            item = FindItem("Cells");
            if (item != null) {
                index = ITEM_INDEX(item);
                other.client.pers.inventory[index] += item.quantity;
                if (other.client.pers.inventory[index] > other.client.pers.max_cells)
                    other.client.pers.inventory[index] = other.client.pers.max_cells;
            }
    
            item = FindItem("Grenades");
            if (item != null) {
                index = ITEM_INDEX(item);
                other.client.pers.inventory[index] += item.quantity;
                if (other.client.pers.inventory[index] > other.client.pers.max_grenades)
                    other.client.pers.inventory[index] = other.client.pers.max_grenades;
            }
    
            item = FindItem("Rockets");
            if (item != null) {
                index = ITEM_INDEX(item);
                other.client.pers.inventory[index] += item.quantity;
                if (other.client.pers.inventory[index] > other.client.pers.max_rockets)
                    other.client.pers.inventory[index] = other.client.pers.max_rockets;
            }
    
            item = FindItem("Slugs");
            if (item != null) {
                index = ITEM_INDEX(item);
                other.client.pers.inventory[index] += item.quantity;
                if (other.client.pers.inventory[index] > other.client.pers.max_slugs)
                    other.client.pers.inventory[index] = other.client.pers.max_slugs;
            }
    
            if (0 == (ent.spawnflags & Constants.DROPPED_ITEM)
                    && (GameBase.deathmatch.value != 0))
                SetRespawn(ent, ent.item.quantity);
    
            return true;
        }
    };
    final static EntInteractAdapter Pickup_Health = new EntInteractAdapter() {
        public String getID() { return "pickup_health";}
        public boolean interact(Entity ent, Entity other) {
    
            if (0 == (ent.style & Constants.HEALTH_IGNORE_MAX))
                if (other.health >= other.max_health)
                    return false;
    
            other.health += ent.count;
    
            if (0 == (ent.style & Constants.HEALTH_IGNORE_MAX)) {
                if (other.health > other.max_health)
                    other.health = other.max_health;
            }
    
            if (0 != (ent.style & Constants.HEALTH_TIMED)) {
                ent.think = GameUtil.MegaHealth_think;
                ent.nextthink = GameBase.level.time + 5f;
                ent.owner = other;
                ent.flags |= Constants.FL_RESPAWN;
                ent.svflags |= Constants.SVF_NOCLIENT;
                ent.solid = Constants.SOLID_NOT;
            } else {
                if (!((ent.spawnflags & Constants.DROPPED_ITEM) != 0)
                        && (GameBase.deathmatch.value != 0))
                    SetRespawn(ent, 30);
            }
    
            return true;
        }
    
    };
    static EntityTouchAdapter Touch_Item = new EntityTouchAdapter() {
        public String getID() { return "touch_item";}
        public void touch(Entity ent, Entity other, Plane plane,
                Surface surf) {
            boolean taken;
    
            if (ent.classname.equals("item_breather"))
                taken = false;
    
            if (other.client == null)
                return;
            if (other.health < 1)
                return; // dead people can't pickup
            if (ent.item.pickup == null)
                return; // not a grabbable item?
    
            taken = ent.item.pickup.interact(ent, other);
    
            if (taken) {
                // flash the screen
                other.client.bonus_alpha = 0.25f;
    
                // show icon and name on status bar
                other.client.ps.stats[Constants.STAT_PICKUP_ICON] = (short) ServerInit.SV_ImageIndex(ent.item.icon);
                other.client.ps.stats[Constants.STAT_PICKUP_STRING] = (short) (Constants.CS_ITEMS + ITEM_INDEX(ent.item));
                other.client.pickup_msg_time = GameBase.level.time + 3.0f;
    
                // change selected item
                if (ent.item.use != null)
                    other.client.pers.selected_item = other.client.ps.stats[Constants.STAT_SELECTED_ITEM] = (short) ITEM_INDEX(ent.item);
    
                if (ent.item.pickup == Pickup_Health) {
                    if (ent.count == 2)
                      ServerGame.PF_StartSound(other, Constants.CHAN_ITEM, ServerInit.SV_SoundIndex("items/s_health.wav"), (float) 1, (float) Constants.ATTN_NORM,
                      (float) 0);
                    else if (ent.count == 10)
                      ServerGame.PF_StartSound(other, Constants.CHAN_ITEM, ServerInit.SV_SoundIndex("items/n_health.wav"), (float) 1, (float) Constants.ATTN_NORM,
                      (float) 0);
                    else if (ent.count == 25)
                      ServerGame.PF_StartSound(other, Constants.CHAN_ITEM, ServerInit.SV_SoundIndex("items/l_health.wav"), (float) 1, (float) Constants.ATTN_NORM,
                      (float) 0);
                    else
                      ServerGame.PF_StartSound(other, Constants.CHAN_ITEM, ServerInit.SV_SoundIndex("items/m_health.wav"), (float) 1, (float) Constants.ATTN_NORM,
                      (float) 0);
                } else if (ent.item.pickup_sound != null) {
                    ServerGame.PF_StartSound(other, Constants.CHAN_ITEM, ServerInit.SV_SoundIndex(ent.item.pickup_sound), (float) 1, (float) Constants.ATTN_NORM,
                    (float) 0);
                }
            }
    
            if (0 == (ent.spawnflags & Constants.ITEM_TARGETS_USED)) {
                GameUtil.G_UseTargets(ent, other);
                ent.spawnflags |= Constants.ITEM_TARGETS_USED;
            }
    
            if (!taken)
                return;
            
            Com.dprintln("Picked up:" + ent.classname);
    
            if (!((GameBase.coop.value != 0) && (ent.item.flags & Constants.IT_STAY_COOP) != 0)
                    || 0 != (ent.spawnflags & (Constants.DROPPED_ITEM | Constants.DROPPED_PLAYER_ITEM))) {
                if ((ent.flags & Constants.FL_RESPAWN) != 0)
                    ent.flags &= ~Constants.FL_RESPAWN;
                else
                    GameUtil.G_FreeEdict(ent);
            }
        }
    };
    static EntityTouchAdapter drop_temp_touch = new EntityTouchAdapter() {
        public String getID() { return "drop_temp_touch";}
        public void touch(Entity ent, Entity other, Plane plane,
                Surface surf) {
            if (other == ent.owner)
                return;
    
            Touch_Item.touch(ent, other, plane, surf);
        }
    };
    static EntityThinkAdapter drop_make_touchable = new EntityThinkAdapter() {
        public String getID() { return "drop_make_touchable";}
        public boolean think(Entity ent) {
            ent.touch = Touch_Item;
            if (GameBase.deathmatch.value != 0) {
                ent.nextthink = GameBase.level.time + 29;
                ent.think = GameUtil.G_FreeEdictA;
            }
            return false;
        }
    };
    static ItemUseAdapter Use_Quad = new ItemUseAdapter() {
        public String getID() { return "use_quad";}    
        public void use(Entity ent, GameItem item) {
            int timeout;
    
            ent.client.pers.inventory[ITEM_INDEX(item)]--;
            GameUtil.ValidateSelectedItem(ent);
    
            if (quad_drop_timeout_hack != 0) {
                timeout = quad_drop_timeout_hack;
                quad_drop_timeout_hack = 0;
            } else {
                timeout = 300;
            }
    
            if (ent.client.quad_framenum > GameBase.level.framenum)
                ent.client.quad_framenum += timeout;
            else
                ent.client.quad_framenum = GameBase.level.framenum + timeout;
    
            ServerGame.PF_StartSound(ent, Constants.CHAN_ITEM, ServerInit.SV_SoundIndex("items/damage.wav"), (float) 1, (float) Constants.ATTN_NORM,
            (float) 0);
        }
    };
    
    static ItemUseAdapter Use_Invulnerability = new ItemUseAdapter() {
        public String getID() { return "use_invulnerability";}
        public void use(Entity ent, GameItem item) {
            ent.client.pers.inventory[ITEM_INDEX(item)]--;
            GameUtil.ValidateSelectedItem(ent);
    
            if (ent.client.invincible_framenum > GameBase.level.framenum)
                ent.client.invincible_framenum += 300;
            else
                ent.client.invincible_framenum = GameBase.level.framenum + 300;
    
            ServerGame.PF_StartSound(ent, Constants.CHAN_ITEM, ServerInit.SV_SoundIndex("items/protect.wav"), (float) 1, (float) Constants.ATTN_NORM,
            (float) 0);
        }
    };
    static ItemUseAdapter Use_Breather = new ItemUseAdapter() {
        public String getID() { return "use_breather";}
        public void use(Entity ent, GameItem item) {
            ent.client.pers.inventory[ITEM_INDEX(item)]--;
    
            GameUtil.ValidateSelectedItem(ent);
    
            if (ent.client.breather_framenum > GameBase.level.framenum)
                ent.client.breather_framenum += 300;
            else
                ent.client.breather_framenum = GameBase.level.framenum + 300;
    
            ServerGame.PF_StartSound(ent, Constants.CHAN_ITEM, ServerInit.SV_SoundIndex("items/damage.wav"), (float) 1, (float) Constants.ATTN_NORM,
            (float) 0);
        }
    };
    static ItemUseAdapter Use_Envirosuit = new ItemUseAdapter() {
        public String getID() { return "use_envirosuit";}
        public void use(Entity ent, GameItem item) {
            ent.client.pers.inventory[ITEM_INDEX(item)]--;
            GameUtil.ValidateSelectedItem(ent);
    
            if (ent.client.enviro_framenum > GameBase.level.framenum)
                ent.client.enviro_framenum += 300;
            else
                ent.client.enviro_framenum = GameBase.level.framenum + 300;
    
            ServerGame.PF_StartSound(ent, Constants.CHAN_ITEM, ServerInit.SV_SoundIndex("items/damage.wav"), (float) 1, (float) Constants.ATTN_NORM,
            (float) 0);
        }
    };
    static ItemUseAdapter Use_Silencer = new ItemUseAdapter() {
        public String getID() { return "use_silencer";}
        public void use(Entity ent, GameItem item) {
    
            ent.client.pers.inventory[ITEM_INDEX(item)]--;
            GameUtil.ValidateSelectedItem(ent);
            ent.client.silencer_shots += 30;
    
            ServerGame.PF_StartSound(ent, Constants.CHAN_ITEM, ServerInit.SV_SoundIndex("items/damage.wav"), (float) 1, (float) Constants.ATTN_NORM,
            (float) 0);
        }
    };
    static EntInteractAdapter Pickup_Key = new EntInteractAdapter() {
        public String getID() { return "pickup_key";}
        public boolean interact(Entity ent, Entity other) {
            if (GameBase.coop.value != 0) {
                if (Lib.strcmp(ent.classname, "key_power_cube") == 0) {
                    if ((other.client.pers.power_cubes & ((ent.spawnflags & 0x0000ff00) >> 8)) != 0)
                        return false;
                    other.client.pers.inventory[ITEM_INDEX(ent.item)]++;
                    other.client.pers.power_cubes |= ((ent.spawnflags & 0x0000ff00) >> 8);
                } else {
                    if (other.client.pers.inventory[ITEM_INDEX(ent.item)] != 0)
                        return false;
                    other.client.pers.inventory[ITEM_INDEX(ent.item)] = 1;
                }
                return true;
            }
            other.client.pers.inventory[ITEM_INDEX(ent.item)]++;
            return true;
        }
    };
    public static EntInteractAdapter Pickup_Ammo = new EntInteractAdapter() {
        public String getID() { return "pickup_ammo";}
        public boolean interact(Entity ent, Entity other) {
            int oldcount;
            int count;
            boolean weapon;
    
            weapon = (ent.item.flags & Constants.IT_WEAPON) != 0;
            if ((weapon)
                    && ((int) GameBase.dmflags.value & Constants.DF_INFINITE_AMMO) != 0)
                count = 1000;
            else if (ent.count != 0)
                count = ent.count;
            else
                count = ent.item.quantity;
    
            oldcount = other.client.pers.inventory[ITEM_INDEX(ent.item)];
    
            if (!Add_Ammo(other, ent.item, count))
                return false;
    
            if (weapon && 0 == oldcount) {
                if (other.client.pers.weapon != ent.item
                        && (0 == GameBase.deathmatch.value || other.client.pers.weapon == FindItem("blaster")))
                    other.client.newweapon = ent.item;
            }
    
            if (0 == (ent.spawnflags & (Constants.DROPPED_ITEM | Constants.DROPPED_PLAYER_ITEM))
                    && (GameBase.deathmatch.value != 0))
                SetRespawn(ent, 30);
            return true;
        }
    };
    public static EntInteractAdapter Pickup_Armor = new EntInteractAdapter() {
        public String getID() { return "pickup_armor";}
        public boolean interact(Entity ent, Entity other) {
            int old_armor_index;
            GameItemArmor oldinfo;
            GameItemArmor newinfo;
            int newcount;
            float salvage;
            int salvagecount;
    
            // get info on new armor
            newinfo = (GameItemArmor) ent.item.info;
    
            old_armor_index = ArmorIndex(other);
    
            // handle armor shards specially
            if (ent.item.tag == Constants.ARMOR_SHARD) {
                if (0 == old_armor_index)
                    other.client.pers.inventory[jacket_armor_index] = 2;
                else
                    other.client.pers.inventory[old_armor_index] += 2;
            }
    
            // if player has no armor, just use it
            else if (0 == old_armor_index) {
                other.client.pers.inventory[ITEM_INDEX(ent.item)] = newinfo.base_count;
            }
    
            // use the better armor
            else {
                // get info on old armor
                if (old_armor_index == jacket_armor_index)
                    oldinfo = jacketarmor_info;
    
                else if (old_armor_index == combat_armor_index)
                    oldinfo = combatarmor_info;
    
                else
                    // (old_armor_index == body_armor_index)
                    oldinfo = bodyarmor_info;
    
                if (newinfo.normal_protection > oldinfo.normal_protection) {
                    // calc new armor values
                    salvage = oldinfo.normal_protection
                            / newinfo.normal_protection;
                    salvagecount = (int) salvage
                            * other.client.pers.inventory[old_armor_index];
                    newcount = newinfo.base_count + salvagecount;
                    if (newcount > newinfo.max_count)
                        newcount = newinfo.max_count;
    
                    // zero count of old armor so it goes away
                    other.client.pers.inventory[old_armor_index] = 0;
    
                    // change armor to new item with computed value
                    other.client.pers.inventory[ITEM_INDEX(ent.item)] = newcount;
                } else {
                    // calc new armor values
                    salvage = newinfo.normal_protection
                            / oldinfo.normal_protection;
                    salvagecount = (int) salvage * newinfo.base_count;
                    newcount = other.client.pers.inventory[old_armor_index]
                            + salvagecount;
                    if (newcount > oldinfo.max_count)
                        newcount = oldinfo.max_count;
    
                    // if we're already maxed out then we don't need the new
                    // armor
                    if (other.client.pers.inventory[old_armor_index] >= newcount)
                        return false;
    
                    // update current armor value
                    other.client.pers.inventory[old_armor_index] = newcount;
                }
            }
    
            if (0 == (ent.spawnflags & Constants.DROPPED_ITEM)
                    && (GameBase.deathmatch.value != 0))
                SetRespawn(ent, 20);
    
            return true;
        }
    };
    public static EntInteractAdapter Pickup_PowerArmor = new EntInteractAdapter() {
        public String getID() { return "pickup_powerarmor";}
        public boolean interact(Entity ent, Entity other) {
    
            int quantity;
    
            quantity = other.client.pers.inventory[ITEM_INDEX(ent.item)];
    
            other.client.pers.inventory[ITEM_INDEX(ent.item)]++;
    
            if (GameBase.deathmatch.value != 0) {
                if (0 == (ent.spawnflags & Constants.DROPPED_ITEM))
                    SetRespawn(ent, ent.item.quantity);
                // auto-use for DM only if we didn't already have one
                if (0 == quantity)
                    ent.item.use.use(other, ent.item);
            }
            return true;
        }
    };
    public static EntInteractAdapter Pickup_Powerup = new EntInteractAdapter() {
        public String getID() { return "pickup_powerup";}
        public boolean interact(Entity ent, Entity other) {
            int quantity;
    
            quantity = other.client.pers.inventory[ITEM_INDEX(ent.item)];
            if ((GameBase.skill.value == 1 && quantity >= 2)
                    || (GameBase.skill.value >= 2 && quantity >= 1))
                return false;
    
            if ((GameBase.coop.value != 0)
                    && (ent.item.flags & Constants.IT_STAY_COOP) != 0
                    && (quantity > 0))
                return false;
    
            other.client.pers.inventory[ITEM_INDEX(ent.item)]++;
    
            if (GameBase.deathmatch.value != 0) {
                if (0 == (ent.spawnflags & Constants.DROPPED_ITEM))
                    SetRespawn(ent, ent.item.quantity);
                if (((int) GameBase.dmflags.value & Constants.DF_INSTANT_ITEMS) != 0
                        || ((ent.item.use == Use_Quad) && 0 != (ent.spawnflags & Constants.DROPPED_PLAYER_ITEM))) {
                    if ((ent.item.use == Use_Quad)
                            && 0 != (ent.spawnflags & Constants.DROPPED_PLAYER_ITEM))
                        quad_drop_timeout_hack = (int) ((ent.nextthink - GameBase.level.time) / Constants.FRAMETIME);
    
                    ent.item.use.use(other, ent.item);
                }
            }
    
            return true;
        }
    };
    public static EntInteractAdapter Pickup_Adrenaline = new EntInteractAdapter() {
        public String getID() { return "pickup_adrenaline";}
        public boolean interact(Entity ent, Entity other) {
            if (GameBase.deathmatch.value == 0)
                other.max_health += 1;
    
            if (other.health < other.max_health)
                other.health = other.max_health;
    
            if (0 == (ent.spawnflags & Constants.DROPPED_ITEM)
                    && (GameBase.deathmatch.value != 0))
                SetRespawn(ent, ent.item.quantity);
    
            return true;
    
        }
    };
    public static EntInteractAdapter Pickup_AncientHead = new EntInteractAdapter() {
        public String getID() { return "pickup_ancienthead";}
        public boolean interact(Entity ent, Entity other) {
            other.max_health += 2;
    
            if (0 == (ent.spawnflags & Constants.DROPPED_ITEM)
                    && (GameBase.deathmatch.value != 0))
                SetRespawn(ent, ent.item.quantity);
    
            return true;
        }
    };
    public static EntInteractAdapter Pickup_Bandolier = new EntInteractAdapter() {
        public String getID() { return "pickup_bandolier";}
        public boolean interact(Entity ent, Entity other) {
            GameItem item;
            int index;
    
            if (other.client.pers.max_bullets < 250)
                other.client.pers.max_bullets = 250;
            if (other.client.pers.max_shells < 150)
                other.client.pers.max_shells = 150;
            if (other.client.pers.max_cells < 250)
                other.client.pers.max_cells = 250;
            if (other.client.pers.max_slugs < 75)
                other.client.pers.max_slugs = 75;
    
            item = FindItem("Bullets");
            if (item != null) {
                index = ITEM_INDEX(item);
                other.client.pers.inventory[index] += item.quantity;
                if (other.client.pers.inventory[index] > other.client.pers.max_bullets)
                    other.client.pers.inventory[index] = other.client.pers.max_bullets;
            }
    
            item = FindItem("Shells");
            if (item != null) {
                index = ITEM_INDEX(item);
                other.client.pers.inventory[index] += item.quantity;
                if (other.client.pers.inventory[index] > other.client.pers.max_shells)
                    other.client.pers.inventory[index] = other.client.pers.max_shells;
            }
    
            if (0 == (ent.spawnflags & Constants.DROPPED_ITEM)
                    && (GameBase.deathmatch.value != 0))
                SetRespawn(ent, ent.item.quantity);
    
            return true;
    
        }
    };
    public static ItemDropAdapter Drop_Ammo = new ItemDropAdapter() {
        public String getID() { return "drop_ammo";}
        public void drop(Entity ent, GameItem item) {
            Entity dropped;
            int index;
    
            index = ITEM_INDEX(item);
            dropped = Drop_Item(ent, item);
            if (ent.client.pers.inventory[index] >= item.quantity)
                dropped.count = item.quantity;
            else
                dropped.count = ent.client.pers.inventory[index];
    
            if (ent.client.pers.weapon != null
                    && ent.client.pers.weapon.tag == Constants.AMMO_GRENADES
                    && item.tag == Constants.AMMO_GRENADES
                    && ent.client.pers.inventory[index] - dropped.count <= 0) {
                ServerGame.PF_cprintf(ent, Constants.PRINT_HIGH, "Can't drop current weapon\n");
                GameUtil.G_FreeEdict(dropped);
                return;
            }
    
            ent.client.pers.inventory[index] -= dropped.count;
            Commands.ValidateSelectedItem(ent);
        }
    };
    public static ItemDropAdapter Drop_General = new ItemDropAdapter() {
        public String getID() { return "drop_general";}
        public void drop(Entity ent, GameItem item) {
            Drop_Item(ent, item);
            ent.client.pers.inventory[ITEM_INDEX(item)]--;
            Commands.ValidateSelectedItem(ent);
        }
    };
    
    public static ItemDropAdapter Drop_PowerArmor = new ItemDropAdapter() {
        public String getID() { return "drop_powerarmor";}
        public void drop(Entity ent, GameItem item) {
            if (0 != (ent.flags & Constants.FL_POWER_ARMOR)
                    && (ent.client.pers.inventory[ITEM_INDEX(item)] == 1))
                Use_PowerArmor.use(ent, item);
            Drop_General.drop(ent, item);
        }
    };
    
    public static EntityThinkAdapter droptofloor = new EntityThinkAdapter() {
        public String getID() { return "drop_to_floor";}
        public boolean think(Entity ent) {
            Trace tr;
            float[] dest = { 0, 0, 0 };
    
            //float v[];
    
            //v = Lib.tv(-15, -15, -15);
            //Math3D.VectorCopy(v, ent.mins);
            ent.mins[0] = ent.mins[1] = ent.mins[2] = -15;
            //v = Lib.tv(15, 15, 15);
            //Math3D.VectorCopy(v, ent.maxs);
            ent.maxs[0] = ent.maxs[1] = ent.maxs[2] = 15;
    
            if (ent.model != null)
              ServerGame.PF_setmodel(ent, ent.model);
            else
              ServerGame.PF_setmodel(ent, ent.item.world_model);
            ent.solid = Constants.SOLID_TRIGGER;
            ent.movetype = Constants.MOVETYPE_TOSS;
            ent.touch = Touch_Item;
    
            float v[] = { 0, 0, -128 };
            Math3D.VectorAdd(ent.s.origin, v, dest);
    
            tr = World.SV_Trace(ent.s.origin, ent.mins, ent.maxs, dest, ent, Constants.MASK_SOLID);
            if (tr.startsolid) {
                ServerGame.PF_dprintf("droptofloor: " + ent.classname
                + " startsolid at " + Lib.vtos(ent.s.origin) + "\n");
                GameUtil.G_FreeEdict(ent);
                return true;
            }
    
            Math3D.VectorCopy(tr.endpos, ent.s.origin);
    
            if (ent.team != null) {
                ent.flags &= ~Constants.FL_TEAMSLAVE;
                ent.chain = ent.teamchain;
                ent.teamchain = null;
    
                ent.svflags |= Constants.SVF_NOCLIENT;
                ent.solid = Constants.SOLID_NOT;
                if (ent == ent.teammaster) {
                    ent.nextthink = GameBase.level.time + Constants.FRAMETIME;
                    ent.think = DoRespawn;
                }
            }
    
            if ((ent.spawnflags & Constants.ITEM_NO_TOUCH) != 0) {
                ent.solid = Constants.SOLID_BBOX;
                ent.touch = null;
                ent.s.effects &= ~Constants.EF_ROTATE;
                ent.s.renderfx &= ~Constants.RF_GLOW;
            }
    
            if ((ent.spawnflags & Constants.ITEM_TRIGGER_SPAWN) != 0) {
                ent.svflags |= Constants.SVF_NOCLIENT;
                ent.solid = Constants.SOLID_NOT;
                ent.use = Use_Item;
            }
    
            World.SV_LinkEdict(ent);
            return true;
        }
    };
    public static ItemUseAdapter Use_PowerArmor = new ItemUseAdapter() {
        public String getID() { return "use_powerarmor";}
        public void use(Entity ent, GameItem item) {
            int index;
    
            if ((ent.flags & Constants.FL_POWER_ARMOR) != 0) {
                ent.flags &= ~Constants.FL_POWER_ARMOR;
                ServerGame.PF_StartSound(ent, Constants.CHAN_AUTO, ServerInit.SV_SoundIndex("misc/power2.wav"), (float) 1, (float) Constants.ATTN_NORM,
                (float) 0);
            } else {
                index = ITEM_INDEX(FindItem("cells"));
                if (0 == ent.client.pers.inventory[index]) {
                    ServerGame.PF_cprintf(ent, Constants.PRINT_HIGH, "No cells for power armor.\n");
                    return;
                }
                ent.flags |= Constants.FL_POWER_ARMOR;
                ServerGame.PF_StartSound(ent, Constants.CHAN_AUTO, ServerInit.SV_SoundIndex("misc/power1.wav"), (float) 1, (float) Constants.ATTN_NORM,
                (float) 0);
            }
        }
    };
    public static EntityUseAdapter Use_Item = new EntityUseAdapter() {
        public String getID() { return "use_item";}
        public void use(Entity ent, Entity other, Entity activator) {
            ent.svflags &= ~Constants.SVF_NOCLIENT;
            ent.use = null;
    
            if ((ent.spawnflags & Constants.ITEM_NO_TOUCH) != 0) {
                ent.solid = Constants.SOLID_BBOX;
                ent.touch = null;
            } else {
                ent.solid = Constants.SOLID_TRIGGER;
                ent.touch = Touch_Item;
            }
    
            World.SV_LinkEdict(ent);
        }
    };

    /*
     * =============== GetItemByIndex ===============
     */
    public static GameItem GetItemByIndex(int index) {
        if (index == 0 || index >= GameBase.game.num_items)
            return null;
    
        return GameItemList.itemlist[index];
    }

    /*
     * =============== FindItemByClassname
     * 
     * ===============
     */
    static GameItem FindItemByClassname(String classname) {
    
        for (int i = 1; i < GameBase.game.num_items; i++) {
            GameItem it = GameItemList.itemlist[i];
    
            if (it.classname == null)
                continue;
            if (it.classname.equalsIgnoreCase(classname))
                return it;
        }
    
        return null;
    }

    /*
     * =============== FindItem ===============
     */
    //geht.
    static GameItem FindItem(String pickup_name) {
        for (int i = 1; i < GameBase.game.num_items; i++) {
            GameItem it = GameItemList.itemlist[i];
    
            if (it.pickup_name == null)
                continue;
            if (it.pickup_name.equalsIgnoreCase(pickup_name))
                return it;
        }
        Com.Println("Item not found:" + pickup_name);
        return null;
    }

    static void SetRespawn(Entity ent, float delay) {
        ent.flags |= Constants.FL_RESPAWN;
        ent.svflags |= Constants.SVF_NOCLIENT;
        ent.solid = Constants.SOLID_NOT;
        ent.nextthink = GameBase.level.time + delay;
        ent.think = DoRespawn;
        World.SV_LinkEdict(ent);
    }

    static int ITEM_INDEX(GameItem item) {
        return item.index;
    }

    static Entity Drop_Item(Entity ent, GameItem item) {
        Entity dropped;
        float[] forward = { 0, 0, 0 };
        float[] right = { 0, 0, 0 };
        float[] offset = { 0, 0, 0 };
    
        dropped = GameUtil.G_Spawn();
    
        dropped.classname = item.classname;
        dropped.item = item;
        dropped.spawnflags = Constants.DROPPED_ITEM;
        dropped.s.effects = item.world_model_flags;
        dropped.s.renderfx = Constants.RF_GLOW;
        Math3D.VectorSet(dropped.mins, -15, -15, -15);
        Math3D.VectorSet(dropped.maxs, 15, 15, 15);
        ServerGame.PF_setmodel(dropped, dropped.item.world_model);
        dropped.solid = Constants.SOLID_TRIGGER;
        dropped.movetype = Constants.MOVETYPE_TOSS;
    
        dropped.touch = drop_temp_touch;
    
        dropped.owner = ent;
    
        if (ent.client != null) {
            Trace trace;
    
            Math3D.AngleVectors(ent.client.v_angle, forward, right, null);
            Math3D.VectorSet(offset, 24, 0, -16);
            Math3D.G_ProjectSource(ent.s.origin, offset, forward, right,
                    dropped.s.origin);
            trace = World.SV_Trace(ent.s.origin, dropped.mins, dropped.maxs, dropped.s.origin, ent, Constants.CONTENTS_SOLID);
            Math3D.VectorCopy(trace.endpos, dropped.s.origin);
        } else {
            Math3D.AngleVectors(ent.s.angles, forward, right, null);
            Math3D.VectorCopy(ent.s.origin, dropped.s.origin);
        }
    
        Math3D.VectorScale(forward, 100, dropped.velocity);
        dropped.velocity[2] = 300;
    
        dropped.think = drop_make_touchable;
        dropped.nextthink = GameBase.level.time + 1;
    
        World.SV_LinkEdict(dropped);
    
        return dropped;
    }

    static void Use_Item(Entity ent, Entity other, Entity activator) {
        ent.svflags &= ~Constants.SVF_NOCLIENT;
        ent.use = null;
    
        if ((ent.spawnflags & Constants.ITEM_NO_TOUCH) != 0) {
            ent.solid = Constants.SOLID_BBOX;
            ent.touch = null;
        } else {
            ent.solid = Constants.SOLID_TRIGGER;
            ent.touch = Touch_Item;
        }
    
        World.SV_LinkEdict(ent);
    }

    static int PowerArmorType(Entity ent) {
        if (ent.client == null)
            return Constants.POWER_ARMOR_NONE;
    
        if (0 == (ent.flags & Constants.FL_POWER_ARMOR))
            return Constants.POWER_ARMOR_NONE;
    
        if (ent.client.pers.inventory[power_shield_index] > 0)
            return Constants.POWER_ARMOR_SHIELD;
    
        if (ent.client.pers.inventory[power_screen_index] > 0)
            return Constants.POWER_ARMOR_SCREEN;
    
        return Constants.POWER_ARMOR_NONE;
    }

    static int ArmorIndex(Entity ent) {
        if (ent.client == null)
            return 0;
    
        if (ent.client.pers.inventory[jacket_armor_index] > 0)
            return jacket_armor_index;
    
        if (ent.client.pers.inventory[combat_armor_index] > 0)
            return combat_armor_index;
    
        if (ent.client.pers.inventory[body_armor_index] > 0)
            return body_armor_index;
    
        return 0;
    }

    public static boolean Pickup_PowerArmor(Entity ent, Entity other) {
        int quantity;
    
        quantity = other.client.pers.inventory[ITEM_INDEX(ent.item)];
    
        other.client.pers.inventory[ITEM_INDEX(ent.item)]++;
    
        if (GameBase.deathmatch.value != 0) {
            if (0 == (ent.spawnflags & Constants.DROPPED_ITEM))
                SetRespawn(ent, ent.item.quantity);
            // auto-use for DM only if we didn't already have one
            if (0 == quantity)
                ent.item.use.use(other, ent.item);
        }
    
        return true;
    }

    public static boolean Add_Ammo(Entity ent, GameItem item, int count) {
        int index;
        int max;
    
        if (null == ent.client)
            return false;
    
        if (item.tag == Constants.AMMO_BULLETS)
            max = ent.client.pers.max_bullets;
        else if (item.tag == Constants.AMMO_SHELLS)
            max = ent.client.pers.max_shells;
        else if (item.tag == Constants.AMMO_ROCKETS)
            max = ent.client.pers.max_rockets;
        else if (item.tag == Constants.AMMO_GRENADES)
            max = ent.client.pers.max_grenades;
        else if (item.tag == Constants.AMMO_CELLS)
            max = ent.client.pers.max_cells;
        else if (item.tag == Constants.AMMO_SLUGS)
            max = ent.client.pers.max_slugs;
        else
            return false;
    
        index = ITEM_INDEX(item);
    
        if (ent.client.pers.inventory[index] == max)
            return false;
    
        ent.client.pers.inventory[index] += count;
    
        if (ent.client.pers.inventory[index] > max)
            ent.client.pers.inventory[index] = max;
    
        return true;
    }

    public static void InitItems() {
        GameBase.game.num_items = GameItemList.itemlist.length - 1;
    }

    /*
     * =============== SetItemNames
     * 
     * Called by worldspawn ===============
     */
    public static void SetItemNames() {
        int i;
        GameItem it;
    
        for (i = 1; i < GameBase.game.num_items; i++) {
            it = GameItemList.itemlist[i];
            ServerGame.PF_Configstring(Constants.CS_ITEMS + i, it.pickup_name);
        }
    
        jacket_armor_index = ITEM_INDEX(FindItem("Jacket Armor"));
        combat_armor_index = ITEM_INDEX(FindItem("Combat Armor"));
        body_armor_index = ITEM_INDEX(FindItem("Body Armor"));
        power_screen_index = ITEM_INDEX(FindItem("Power Screen"));
        power_shield_index = ITEM_INDEX(FindItem("Power Shield"));
    }

    public static void SelectNextItem(Entity ent, int itflags) {
        GameClient cl;
        int i, index;
        GameItem it;
    
        cl = ent.client;
    
        if (cl.chase_target != null) {
            GameChase.ChaseNext(ent);
            return;
        }
    
        // scan for the next valid one
        for (i = 1; i <= Constants.MAX_ITEMS; i++) {
            index = (cl.pers.selected_item + i) % Constants.MAX_ITEMS;
            if (0 == cl.pers.inventory[index])
                continue;
            it = GameItemList.itemlist[index];
            if (it.use == null)
                continue;
            if (0 == (it.flags & itflags))
                continue;
    
            cl.pers.selected_item = index;
            return;
        }
    
        cl.pers.selected_item = -1;
    }

    public static void SelectPrevItem(Entity ent, int itflags) {
        GameClient cl;
        int i, index;
        GameItem it;
    
        cl = ent.client;
    
        if (cl.chase_target != null) {
            GameChase.ChasePrev(ent);
            return;
        }
    
        // scan for the next valid one
        for (i = 1; i <= Constants.MAX_ITEMS; i++) {
            index = (cl.pers.selected_item + Constants.MAX_ITEMS - i)
                    % Constants.MAX_ITEMS;
            if (0 == cl.pers.inventory[index])
                continue;
            it = GameItemList.itemlist[index];
            if (null == it.use)
                continue;
            if (0 == (it.flags & itflags))
                continue;
    
            cl.pers.selected_item = index;
            return;
        }
    
        cl.pers.selected_item = -1;
    }

    /*
     * =============== PrecacheItem
     * 
     * Precaches all data needed for a given item. This will be called for each
     * item spawned in a level, and for each item in each client's inventory.
     * ===============
     */
    public static void PrecacheItem(GameItem it) {
        String s;
        String data;
        int len;
        GameItem ammo;
    
        if (it == null)
            return;
    
        if (it.pickup_sound != null)
          ServerInit.SV_SoundIndex(it.pickup_sound);
    
        if (it.world_model != null)
          ServerInit.SV_ModelIndex(it.world_model);
    
        if (it.view_model != null)
          ServerInit.SV_ModelIndex(it.view_model);
    
        if (it.icon != null)
          ServerInit.SV_ImageIndex(it.icon);
    
        // parse everything for its ammo
        if (it.ammo != null && it.ammo.length() != 0) {
            ammo = FindItem(it.ammo);
            if (ammo != it)
                PrecacheItem(ammo);
        }
    
        // parse the space seperated precache string for other items
        s = it.precaches;
        if (s == null || s.length() != 0)
            return;
    
        StringTokenizer tk = new StringTokenizer(s);
    
        while (tk.hasMoreTokens()) {
            data = tk.nextToken();
    
            len = data.length();
    
            if (len >= Constants.MAX_QPATH || len < 5)
              Com.Error(Constants.ERR_FATAL, "PrecacheItem: it.classname has bad precache string: "
              + s);
    
            // determine type based on extension
            if (data.endsWith("md2"))
              ServerInit.SV_ModelIndex(data);
            else if (data.endsWith("sp2"))
              ServerInit.SV_ModelIndex(data);
            else if (data.endsWith("wav"))
              ServerInit.SV_SoundIndex(data);
            else if (data.endsWith("pcx"))
              ServerInit.SV_ImageIndex(data);
            else
              Com.Error(Constants.ERR_FATAL, "PrecacheItem: bad precache string: " + data);
        }
    }

    /*
     * ============ SpawnItem
     * 
     * Sets the clipping size and plants the object on the floor.
     * 
     * Items can't be immediately dropped to floor, because they might be on an
     * entity that hasn't spawned yet. ============
     */
    public static void SpawnItem(Entity ent, GameItem item) {
        PrecacheItem(item);
    
        if (ent.spawnflags != 0) {
            if (Lib.strcmp(ent.classname, "key_power_cube") != 0) {
                ent.spawnflags = 0;
                ServerGame.PF_dprintf("" + ent.classname + " at "
                + Lib.vtos(ent.s.origin)
                + " has invalid spawnflags set\n");
            }
        }
    
        // some items will be prevented in deathmatch
        if (GameBase.deathmatch.value != 0) {
            if (((int) GameBase.dmflags.value & Constants.DF_NO_ARMOR) != 0) {
                if (item.pickup == Pickup_Armor
                        || item.pickup == Pickup_PowerArmor) {
                    GameUtil.G_FreeEdict(ent);
                    return;
                }
            }
            if (((int) GameBase.dmflags.value & Constants.DF_NO_ITEMS) != 0) {
                if (item.pickup == Pickup_Powerup) {
                    GameUtil.G_FreeEdict(ent);
                    return;
                }
            }
            if (((int) GameBase.dmflags.value & Constants.DF_NO_HEALTH) != 0) {
                if (item.pickup == Pickup_Health
                        || item.pickup == Pickup_Adrenaline
                        || item.pickup == Pickup_AncientHead) {
                    GameUtil.G_FreeEdict(ent);
                    return;
                }
            }
            if (((int) GameBase.dmflags.value & Constants.DF_INFINITE_AMMO) != 0) {
                if ((item.flags == Constants.IT_AMMO)
                        || (Lib.strcmp(ent.classname, "weapon_bfg") == 0)) {
                    GameUtil.G_FreeEdict(ent);
                    return;
                }
            }
        }
    
        if (GameBase.coop.value != 0
                && (Lib.strcmp(ent.classname, "key_power_cube") == 0)) {
            ent.spawnflags |= (1 << (8 + GameBase.level.power_cubes));
            GameBase.level.power_cubes++;
        }
    
        // don't let them drop items that stay in a coop game
        if ((GameBase.coop.value != 0)
                && (item.flags & Constants.IT_STAY_COOP) != 0) {
            item.drop = null;
        }
    
        ent.item = item;
        ent.nextthink = GameBase.level.time + 2 * Constants.FRAMETIME;
        // items start after other solids
        ent.think = droptofloor;
        ent.s.effects = item.world_model_flags;
        ent.s.renderfx = Constants.RF_GLOW;
    
        if (ent.model != null)
          ServerInit.SV_ModelIndex(ent.model);
    }

    /*
     * QUAKED item_health (.3 .3 1) (-16 -16 -16) (16 16 16)
     */
    public static void SP_item_health(Entity self) {
        if (GameBase.deathmatch.value != 0
                && ((int) GameBase.dmflags.value & Constants.DF_NO_HEALTH) != 0) {
            GameUtil.G_FreeEdict(self);
        }
    
        self.model = "models/items/healing/medium/tris.md2";
        self.count = 10;
        SpawnItem(self, FindItem("Health"));
        ServerInit.SV_SoundIndex("items/n_health.wav");
    }

    /*
     * QUAKED item_health_small (.3 .3 1) (-16 -16 -16) (16 16 16)
     */
    static void SP_item_health_small(Entity self) {
        if (GameBase.deathmatch.value != 0
                && ((int) GameBase.dmflags.value & Constants.DF_NO_HEALTH) != 0) {
            GameUtil.G_FreeEdict(self);
            return;
        }
    
        self.model = "models/items/healing/stimpack/tris.md2";
        self.count = 2;
        SpawnItem(self, FindItem("Health"));
        self.style = Constants.HEALTH_IGNORE_MAX;
        ServerInit.SV_SoundIndex("items/s_health.wav");
    }

    /*
     * QUAKED item_health_large (.3 .3 1) (-16 -16 -16) (16 16 16)
     */
    static void SP_item_health_large(Entity self) {
        if (GameBase.deathmatch.value != 0
                && ((int) GameBase.dmflags.value & Constants.DF_NO_HEALTH) != 0) {
            GameUtil.G_FreeEdict(self);
            return;
        }
    
        self.model = "models/items/healing/large/tris.md2";
        self.count = 25;
        SpawnItem(self, FindItem("Health"));
        ServerInit.SV_SoundIndex("items/l_health.wav");
    }

    /*
     * QUAKED item_health_mega (.3 .3 1) (-16 -16 -16) (16 16 16)
     */
    static void SP_item_health_mega(Entity self) {
        if (GameBase.deathmatch.value != 0
                && ((int) GameBase.dmflags.value & Constants.DF_NO_HEALTH) != 0) {
            GameUtil.G_FreeEdict(self);
            return;
        }
    
        self.model = "models/items/mega_h/tris.md2";
        self.count = 100;
        SpawnItem(self, FindItem("Health"));
        ServerInit.SV_SoundIndex("items/m_health.wav");
        self.style = Constants.HEALTH_IGNORE_MAX | Constants.HEALTH_TIMED;
    }

    /*
     * =============== 
     * Touch_Item 
     * ===============
     */
    public static void Touch_Item(Entity ent, Entity other, Plane plane,
            Surface surf) {
        boolean taken;

        // freed edicts have not items.
        if (other.client == null || ent.item == null)
            return;
        if (other.health < 1)
            return; // dead people can't pickup
        if (ent.item.pickup == null)
            return; // not a grabbable item?
    
        taken = ent.item.pickup.interact(ent, other);
    
        if (taken) {
            // flash the screen
            other.client.bonus_alpha = 0.25f;
    
            // show icon and name on status bar
            other.client.ps.stats[Constants.STAT_PICKUP_ICON] = (short) ServerInit.SV_ImageIndex(ent.item.icon);
            other.client.ps.stats[Constants.STAT_PICKUP_STRING] = (short) (Constants.CS_ITEMS + ITEM_INDEX(ent.item));
            other.client.pickup_msg_time = GameBase.level.time + 3.0f;
    
            // change selected item
            if (ent.item.use != null)
                other.client.pers.selected_item = other.client.ps.stats[Constants.STAT_SELECTED_ITEM] = (short) ITEM_INDEX(ent.item);
    
            if (ent.item.pickup == Pickup_Health) {
                if (ent.count == 2)
                  ServerGame.PF_StartSound(other, Constants.CHAN_ITEM, ServerInit.SV_SoundIndex("items/s_health.wav"), (float) 1, (float) Constants.ATTN_NORM,
                  (float) 0);
                else if (ent.count == 10)
                  ServerGame.PF_StartSound(other, Constants.CHAN_ITEM, ServerInit.SV_SoundIndex("items/n_health.wav"), (float) 1, (float) Constants.ATTN_NORM,
                  (float) 0);
                else if (ent.count == 25)
                  ServerGame.PF_StartSound(other, Constants.CHAN_ITEM, ServerInit.SV_SoundIndex("items/l_health.wav"), (float) 1, (float) Constants.ATTN_NORM,
                  (float) 0);
                else
                  ServerGame.PF_StartSound(other, Constants.CHAN_ITEM, ServerInit.SV_SoundIndex("items/m_health.wav"), (float) 1, (float) Constants.ATTN_NORM,
                  (float) 0);
            } else if (ent.item.pickup_sound != null) {
                ServerGame.PF_StartSound(other, Constants.CHAN_ITEM, ServerInit.SV_SoundIndex(ent.item.pickup_sound), (float) 1, (float) Constants.ATTN_NORM,
                (float) 0);
            }
        }
    
        if (0 == (ent.spawnflags & Constants.ITEM_TARGETS_USED)) {
            GameUtil.G_UseTargets(ent, other);
            ent.spawnflags |= Constants.ITEM_TARGETS_USED;
        }
    
        if (!taken)
            return;
    
        if (!((GameBase.coop.value != 0) && (ent.item.flags & Constants.IT_STAY_COOP) != 0)
                || 0 != (ent.spawnflags & (Constants.DROPPED_ITEM | Constants.DROPPED_PLAYER_ITEM))) {
            if ((ent.flags & Constants.FL_RESPAWN) != 0)
                ent.flags &= ~Constants.FL_RESPAWN;
            else
                GameUtil.G_FreeEdict(ent);
        }
    }

}

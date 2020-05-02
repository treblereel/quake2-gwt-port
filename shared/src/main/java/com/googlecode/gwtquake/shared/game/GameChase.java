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
import com.googlecode.gwtquake.shared.server.ServerGame;
import com.googlecode.gwtquake.shared.server.World;
import com.googlecode.gwtquake.shared.util.Math3D;

public class GameChase {

    public static void ChasePrev(Entity ent) {
        int i;
        Entity e;

        if (ent.client.chase_target == null) {
            return;
        }

        i = ent.client.chase_target.index;
        do {
            i--;
            if (i < 1) {
                i = (int) GameBase.maxclients.value;
            }
            e = GameBase.g_edicts[i];
            if (!e.inuse) {
                continue;
            }
            if (!e.client.resp.spectator) {
                break;
            }
        } while (e != ent.client.chase_target);

        ent.client.chase_target = e;
        ent.client.update_chase = true;
    }

    public static void GetChaseTarget(Entity ent) {
        int i;
        Entity other;

        for (i = 1; i <= GameBase.maxclients.value; i++) {
            other = GameBase.g_edicts[i];
            if (other.inuse && !other.client.resp.spectator) {
                ent.client.chase_target = other;
                ent.client.update_chase = true;
                UpdateChaseCam(ent);
                return;
            }
        }
        ServerGame.PF_centerprintf(ent, "No other players to chase.");
    }

    public static void UpdateChaseCam(Entity ent) {
        float[] o = {0, 0, 0}, ownerv = {0, 0, 0}, goal = {0, 0, 0};
        Entity targ;
        float[] forward = {0, 0, 0}, right = {0, 0, 0};
        Trace trace;
        int i;
        float[] oldgoal = {0, 0, 0};
        float[] angles = {0, 0, 0};

        // is our chase target gone?
        if (!ent.client.chase_target.inuse
                || ent.client.chase_target.client.resp.spectator) {
            Entity old = ent.client.chase_target;
            ChaseNext(ent);
            if (ent.client.chase_target == old) {
                ent.client.chase_target = null;
                ent.client.ps.pmove.pm_flags &= ~PlayerMove.PMF_NO_PREDICTION;
                return;
            }
        }

        targ = ent.client.chase_target;

        Math3D.VectorCopy(targ.s.origin, ownerv);
        Math3D.VectorCopy(ent.s.origin, oldgoal);

        ownerv[2] += targ.viewheight;

        Math3D.VectorCopy(targ.client.v_angle, angles);
        if (angles[Constants.PITCH] > 56) {
            angles[Constants.PITCH] = 56;
        }
        Math3D.AngleVectors(angles, forward, right, null);
        Math3D.VectorNormalize(forward);
        Math3D.VectorMA(ownerv, -30, forward, o);

        if (o[2] < targ.s.origin[2] + 20) {
            o[2] = targ.s.origin[2] + 20;
        }

        // jump animation lifts
        if (targ.groundentity == null) {
            o[2] += 16;
        }

        trace = World.SV_Trace(ownerv, Globals.vec3_origin, Globals.vec3_origin, o, targ, Constants.MASK_SOLID);

        Math3D.VectorCopy(trace.endpos, goal);

        Math3D.VectorMA(goal, 2, forward, goal);

        // pad for floors and ceilings
        Math3D.VectorCopy(goal, o);
        o[2] += 6;
        trace = World.SV_Trace(goal, Globals.vec3_origin, Globals.vec3_origin, o, targ, Constants.MASK_SOLID);
        if (trace.fraction < 1) {
            Math3D.VectorCopy(trace.endpos, goal);
            goal[2] -= 6;
        }

        Math3D.VectorCopy(goal, o);
        o[2] -= 6;
        trace = World.SV_Trace(goal, Globals.vec3_origin, Globals.vec3_origin, o, targ, Constants.MASK_SOLID);
        if (trace.fraction < 1) {
            Math3D.VectorCopy(trace.endpos, goal);
            goal[2] += 6;
        }

        if (targ.deadflag != 0) {
            ent.client.ps.pmove.pm_type = Constants.PM_DEAD;
        } else {
            ent.client.ps.pmove.pm_type = Constants.PM_FREEZE;
        }

        Math3D.VectorCopy(goal, ent.s.origin);
        for (i = 0; i < 3; i++) {
            ent.client.ps.pmove.delta_angles[i] = (short) Math3D
                    .ANGLE2SHORT(targ.client.v_angle[i]
                                         - ent.client.resp.cmd_angles[i]);
        }

        if (targ.deadflag != 0) {
            ent.client.ps.viewangles[Constants.ROLL] = 40;
            ent.client.ps.viewangles[Constants.PITCH] = -15;
            ent.client.ps.viewangles[Constants.YAW] = targ.client.killer_yaw;
        } else {
            Math3D.VectorCopy(targ.client.v_angle, ent.client.ps.viewangles);
            Math3D.VectorCopy(targ.client.v_angle, ent.client.v_angle);
        }

        ent.viewheight = 0;
        ent.client.ps.pmove.pm_flags |= PlayerMove.PMF_NO_PREDICTION;
        World.SV_LinkEdict(ent);
    }

    public static void ChaseNext(Entity ent) {
        int i;
        Entity e;

        if (null == ent.client.chase_target) {
            return;
        }

        i = ent.client.chase_target.index;
        do {
            i++;
            if (i > GameBase.maxclients.value) {
                i = 1;
            }
            e = GameBase.g_edicts[i];

            if (!e.inuse) {
                continue;
            }
            if (!e.client.resp.spectator) {
                break;
            }
        } while (e != ent.client.chase_target);

        ent.client.chase_target = e;
        ent.client.update_chase = true;
    }
}

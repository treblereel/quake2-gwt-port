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
package com.googlecode.gwtquake.shared.client;

import com.googlecode.gwtquake.shared.common.*;
import com.googlecode.gwtquake.shared.game.*;
import com.googlecode.gwtquake.shared.util.Math3D;


/**
 * CL_pred
 */
public class ClientPrediction {

    /*
     * =================== CL_CheckPredictionError ===================
     */
    static void CheckPredictionError() {
        int frame;
        int[] delta = new int[3];
        int i;
        int len;

        if (Globals.cl_predict.value == 0.0f
                || (Globals.cl.frame.playerstate.pmove.pm_flags & PlayerMove.PMF_NO_PREDICTION) != 0)
            return;

        // calculate the last usercmd_t we sent that the server has processed
        frame = Globals.cls.netchan.incoming_acknowledged;
        frame &= (Constants.CMD_BACKUP - 1);

        // compare what the server returned with what we had predicted it to be
        Math3D.VectorSubtract(Globals.cl.frame.playerstate.pmove.origin,
                Globals.cl.predicted_origins[frame], delta);

        // save the prediction error for interpolation
        len = Math.abs(delta[0]) + Math.abs(delta[1]) + Math.abs(delta[2]);
        if (len > 640) // 80 world units
        { // a teleport or something
            Math3D.VectorClear(Globals.cl.prediction_error);
        } else {
            if (Globals.cl_showmiss.value != 0.0f
                    && (delta[0] != 0 || delta[1] != 0 || delta[2] != 0))
                Com.Printf("prediction miss on " + Globals.cl.frame.serverframe
                        + ": " + (delta[0] + delta[1] + delta[2]) + "\n");

            Math3D.VectorCopy(Globals.cl.frame.playerstate.pmove.origin,
                    Globals.cl.predicted_origins[frame]);

            // save for error itnerpolation
            for (i = 0; i < 3; i++)
                Globals.cl.prediction_error[i] = delta[i] * 0.125f;
        }
    }

    /*
     * ==================== CL_ClipMoveToEntities
     * 
     * ====================
     */
    static void ClipMoveToEntities(float[] start, float[] mins, float[] maxs,
            float[] end, Trace tr) {
        int i, x, zd, zu;
        Trace trace;
        int headnode;
        float[] angles;
        EntityState ent;
        int num;
        Model cmodel;
        float[] bmins = new float[3];
        float[] bmaxs = new float[3];

        for (i = 0; i < Globals.cl.frame.num_entities; i++) {
            num = (Globals.cl.frame.parse_entities + i)
                    & (Constants.MAX_PARSE_ENTITIES - 1);
            ent = Globals.cl_parse_entities[num];

            if (ent.solid == 0)
                continue;

            if (ent.number == Globals.cl.playernum + 1)
                continue;

            if (ent.solid == 31) { // special value for bmodel
                cmodel = Globals.cl.model_clip[ent.modelindex];
                if (cmodel == null)
                    continue;
                headnode = cmodel.headnode;
                angles = ent.angles;
            } else { // encoded bbox
                x = 8 * (ent.solid & 31);
                zd = 8 * ((ent.solid >>> 5) & 31);
                zu = 8 * ((ent.solid >>> 10) & 63) - 32;

                bmins[0] = bmins[1] = -x;
                bmaxs[0] = bmaxs[1] = x;
                bmins[2] = -zd;
                bmaxs[2] = zu;

                headnode = CM.HeadnodeForBox(bmins, bmaxs);
                angles = Globals.vec3_origin; // boxes don't rotate
            }

            if (tr.allsolid)
                return;

            trace = CM.TransformedBoxTrace(start, end, mins, maxs, headnode,
                    Constants.MASK_PLAYERSOLID, ent.origin, angles);

            if (trace.allsolid || trace.startsolid
                    || trace.fraction < tr.fraction) {
                trace.ent = ent.surrounding_ent;
                if (tr.startsolid) {
                    tr.set(trace); // rst: solved the Z U P P E L - P R O B L E
                                   // M
                    tr.startsolid = true;
                } else
                    tr.set(trace); // rst: solved the Z U P P E L - P R O B L E
                                   // M
            } else if (trace.startsolid)
                tr.startsolid = true;
        }
    }

    /*
     * ================ CL_PMTrace ================
     */

    public static Entity DUMMY_ENT = new Entity(-1);

    static Trace PMTrace(float[] start, float[] mins, float[] maxs,
            float[] end) {
        Trace t;

        // check against world
        t = CM.BoxTrace(start, end, mins, maxs, 0, Constants.MASK_PLAYERSOLID);

        if (t.fraction < 1.0f) {
            t.ent = DUMMY_ENT;
        }

        // check all other solid models
        ClipMoveToEntities(start, mins, maxs, end, t);

        return t;
    }

    /*
     * ================= PMpointcontents
     * 
     * Returns the content identificator of the point. =================
     */
    static int PMpointcontents(float[] point) {
        int i;
        EntityState ent;
        int num;
        Model cmodel;
        int contents;

        contents = CM.PointContents(point, 0);

        for (i = 0; i < Globals.cl.frame.num_entities; i++) {
            num = (Globals.cl.frame.parse_entities + i)
                    & (Constants.MAX_PARSE_ENTITIES - 1);
            ent = Globals.cl_parse_entities[num];

            if (ent.solid != 31) // special value for bmodel
                continue;

            cmodel = Globals.cl.model_clip[ent.modelindex];
            if (cmodel == null)
                continue;

            contents |= CM.TransformedPointContents(point, cmodel.headnode,
                    ent.origin, ent.angles);
        }
        return contents;
    }

    /*
     * ================= CL_PredictMovement
     * 
     * Sets cl.predicted_origin and cl.predicted_angles =================
     */
    static void PredictMovement() {

        if (Globals.cls.state != Constants.ca_active)
            return;

        if (Globals.cl_paused.value != 0.0f)
            return;

        if (Globals.cl_predict.value == 0.0f
                || (Globals.cl.frame.playerstate.pmove.pm_flags & PlayerMove.PMF_NO_PREDICTION) != 0) {
            // just set angles
            for (int i = 0; i < 3; i++) {
                Globals.cl.predicted_angles[i] = Globals.cl.viewangles[i]
                        + Math3D
                                .SHORT2ANGLE(Globals.cl.frame.playerstate.pmove.delta_angles[i]);
            }
            return;
        }

        int ack = Globals.cls.netchan.incoming_acknowledged;
        int current = Globals.cls.netchan.outgoing_sequence;

        // if we are too far out of date, just freeze
        if (current - ack >= Constants.CMD_BACKUP) {
            if (Globals.cl_showmiss.value != 0.0f)
                Com.Printf("exceeded CMD_BACKUP\n");
            return;
        }

        // copy current state to pmove
        //memset (pm, 0, sizeof(pm));
        PlayerMove pm = new PlayerMove();

        pm.trace = new PlayerMove.TraceAdapter() {
            public Trace trace(float[] start, float[] mins, float[] maxs,
                    float[] end) {
                return PMTrace(start, mins, maxs, end);
            }
        };
        pm.pointcontents = new PlayerMove.PointContentsAdapter() {
            public int pointcontents(float[] point) {
                return PMpointcontents(point);
            }
        };

        try {
            PlayerMovements.pm_airaccelerate = Float
                    .parseFloat(Globals.cl.configstrings[Constants.CS_AIRACCEL]);
        } catch (Exception e) {
            PlayerMovements.pm_airaccelerate = 0;
        }

        // bugfix (rst) yeah !!!!!!!! found the solution to the B E W E G U N G
        // S P R O B L E M.
        pm.s.set(Globals.cl.frame.playerstate.pmove);

        // SCR_DebugGraph (current - ack - 1, 0);
        int frame = 0;

        // run frames
        UserCommand cmd;
        while (++ack < current) {
            frame = ack & (Constants.CMD_BACKUP - 1);
            cmd = Globals.cl.cmds[frame];

            pm.cmd.set(cmd);

            PlayerMovements.Pmove(pm);

            // save for debug checking
            Math3D.VectorCopy(pm.s.origin, Globals.cl.predicted_origins[frame]);
        }

        int oldframe = (ack - 2) & (Constants.CMD_BACKUP - 1);
        int oldz = Globals.cl.predicted_origins[oldframe][2];
        int step = pm.s.origin[2] - oldz;
        if (step > 63 && step < 160
                && (pm.s.pm_flags & PlayerMove.PMF_ON_GROUND) != 0) {
            Globals.cl.predicted_step = step * 0.125f;
            Globals.cl.predicted_step_time = (int) (Globals.cls.realtime - Globals.cls.frametime * 500);
        }

        // copy results out for rendering
        Globals.cl.predicted_origin[0] = pm.s.origin[0] * 0.125f;
        Globals.cl.predicted_origin[1] = pm.s.origin[1] * 0.125f;
        Globals.cl.predicted_origin[2] = pm.s.origin[2] * 0.125f;

        Math3D.VectorCopy(pm.viewangles, Globals.cl.predicted_angles);
    }
}

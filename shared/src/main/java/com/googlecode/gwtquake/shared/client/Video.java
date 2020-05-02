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

import static com.googlecode.gwtquake.shared.common.Constants.CVAR_ARCHIVE;
import static com.googlecode.gwtquake.shared.common.Constants.ERR_DROP;
import static com.googlecode.gwtquake.shared.common.Constants.MAX_DLIGHTS;
import static com.googlecode.gwtquake.shared.common.Constants.MAX_ENTITIES;
import static com.googlecode.gwtquake.shared.common.Constants.MAX_LIGHTSTYLES;
import static com.googlecode.gwtquake.shared.common.Constants.MAX_PARTICLES;
import static com.googlecode.gwtquake.shared.common.Constants.YAW;
import static com.googlecode.gwtquake.shared.common.Constants.ca_active;
import static com.googlecode.gwtquake.shared.common.Globals.cl;
import static com.googlecode.gwtquake.shared.common.Globals.cl_add_blend;
import static com.googlecode.gwtquake.shared.common.Globals.cl_add_entities;
import static com.googlecode.gwtquake.shared.common.Globals.cl_add_lights;
import static com.googlecode.gwtquake.shared.common.Globals.cl_add_particles;
import static com.googlecode.gwtquake.shared.common.Globals.cl_paused;
import static com.googlecode.gwtquake.shared.common.Globals.cl_timedemo;
import static com.googlecode.gwtquake.shared.common.Globals.cls;
import static com.googlecode.gwtquake.shared.common.Globals.crosshair;
import static com.googlecode.gwtquake.shared.common.Globals.gun_frame;
import static com.googlecode.gwtquake.shared.common.Globals.gun_model;
import static com.googlecode.gwtquake.shared.common.Globals.re;
import static com.googlecode.gwtquake.shared.common.Globals.scr_vrect;


import java.nio.FloatBuffer;

import com.googlecode.gwtquake.shared.common.AsyncCallback;
import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.ConsoleVariables;
import com.googlecode.gwtquake.shared.common.ExecutableCommand;
import com.googlecode.gwtquake.shared.game.Commands;
import com.googlecode.gwtquake.shared.game.ConsoleVariable;
import com.googlecode.gwtquake.shared.render.DynamicLight;
import com.googlecode.gwtquake.shared.render.Model;
import com.googlecode.gwtquake.shared.render.Particles;
import com.googlecode.gwtquake.shared.sys.Timer;
import com.googlecode.gwtquake.shared.util.Math3D;
import com.googlecode.gwtquake.shared.util.Vargs;
import elemental2.dom.DomGlobal;

/**
 * V
 */
public final class Video {

    static ConsoleVariable cl_testblend;

    static ConsoleVariable cl_testparticles;

    static ConsoleVariable cl_testentities;

    static ConsoleVariable cl_testlights;

    static ConsoleVariable cl_stats;

    static int r_numdlights;

    static DynamicLight[] r_dlights = new DynamicLight[MAX_DLIGHTS];

    static int r_numentities;

    static EntityType[] r_entities = new EntityType[MAX_ENTITIES];

    static int r_numparticles;

    //static particle_t[] r_particles = new particle_t[MAX_PARTICLES];

    static Lightstyle[] r_lightstyles = new Lightstyle[MAX_LIGHTSTYLES];
    static {
        for (int i = 0; i < r_dlights.length; i++)
            r_dlights[i] = new DynamicLight();
        for (int i = 0; i < r_entities.length; i++)
            r_entities[i] = new EntityType();
        for (int i = 0; i < r_lightstyles.length; i++)
            r_lightstyles[i] = new Lightstyle();
    }

    /*
     * ==================== V_ClearScene
     * 
     * Specifies the model that will be used as the world ====================
     */
    static void ClearScene() {
        r_numdlights = 0;
        r_numentities = 0;
        r_numparticles = 0;
    }

    /*
     * ===================== V_AddEntity
     * 
     * =====================
     */
    static void AddEntity(EntityType ent) {
        if (r_numentities >= MAX_ENTITIES)
            return;
        r_entities[r_numentities++].set(ent);
    }

    /*
     * ===================== V_AddParticle
     * 
     * =====================
     */
    static void AddParticle(float[] org, int color, float alpha) {
        if (r_numparticles >= MAX_PARTICLES)
            return;

        int i = r_numparticles++;

        int c = Particles.colorTable[color];
        c |= (int) (alpha * 255) << 24;
        Particles.colorArray.put(i, c);

        i *= 3;
        FloatBuffer vertexBuf = Particles.vertexArray;
        vertexBuf.put(i++, org[0]);
        vertexBuf.put(i++, org[1]);
        vertexBuf.put(i++, org[2]);
    }

    /*
     * ===================== V_AddLight
     * 
     * =====================
     */
    static void AddLight(float[] org, float intensity, float r, float g, float b) {
        DynamicLight dl;

        if (r_numdlights >= MAX_DLIGHTS)
            return;
        dl = r_dlights[r_numdlights++];
        Math3D.VectorCopy(org, dl.origin);
        dl.intensity = intensity;
        dl.color[0] = r;
        dl.color[1] = g;
        dl.color[2] = b;
    }

    /*
     * ===================== V_AddLightStyle
     * 
     * =====================
     */
    static void AddLightStyle(int style, float r, float g, float b) {
        Lightstyle ls;

        if (style < 0 || style > MAX_LIGHTSTYLES)
            Com.Error(ERR_DROP, "Bad light style " + style);
        ls = r_lightstyles[style];

        ls.white = r + g + b;
        ls.rgb[0] = r;
        ls.rgb[1] = g;
        ls.rgb[2] = b;
    }

    // stack variable
    private static final float[] origin = { 0, 0, 0 };
    /*
     * ================ V_TestParticles
     * 
     * If cl_testparticles is set, create 4096 particles in the view
     * ================
     */
    static void TestParticles() {
        int i, j;
        float d, r, u;

        r_numparticles = 0;
        for (i = 0; i < MAX_PARTICLES; i++) {
            d = i * 0.25f;
            r = 4 * ((i & 7) - 3.5f);
            u = 4 * (((i >> 3) & 7) - 3.5f);

            for (j = 0; j < 3; j++)
                origin[j] = cl.refdef.vieworg[j] + cl.v_forward[j] * d
                        + cl.v_right[j] * r + cl.v_up[j] * u;

            AddParticle(origin, 8, cl_testparticles.value);
        }
    }

    /*
     * ================ V_TestEntities
     * 
     * If cl_testentities is set, create 32 player models ================
     */
    static void TestEntities() {
        int i, j;
        float f, r;
        EntityType ent;

        r_numentities = 32;
        //memset (r_entities, 0, sizeof(r_entities));
        for (i = 0; i < r_entities.length; i++)
        	r_entities[i].clear();

        for (i = 0; i < r_numentities; i++) {
            ent = r_entities[i];

            r = 64 * ((i % 4) - 1.5f);
            f = 64 * (i / 4) + 128;

            for (j = 0; j < 3; j++)
                ent.origin[j] = cl.refdef.vieworg[j] + cl.v_forward[j] * f
                        + cl.v_right[j] * r;

            ent.model = cl.baseclientinfo.model;
            ent.skin = cl.baseclientinfo.skin;
        }
    }

    /*
     * ================ V_TestLights
     * 
     * If cl_testlights is set, create 32 lights models ================
     */
    static void TestLights() {
        int i, j;
        float f, r;
        DynamicLight dl;

        r_numdlights = 32;
        //memset (r_dlights, 0, sizeof(r_dlights));
        for (i = 0; i < r_dlights.length; i++)
            r_dlights[i] = new DynamicLight();

        for (i = 0; i < r_numdlights; i++) {
            dl = r_dlights[i];

            r = 64 * ((i % 4) - 1.5f);
            f = 64 * (i / 4) + 128;

            for (j = 0; j < 3; j++)
                dl.origin[j] = cl.refdef.vieworg[j] + cl.v_forward[j] * f
                        + cl.v_right[j] * r;
            dl.color[0] = ((i % 6) + 1) & 1;
            dl.color[1] = (((i % 6) + 1) & 2) >> 1;
            dl.color[2] = (((i % 6) + 1) & 4) >> 2;
            dl.intensity = 200;
        }
    }

    static ExecutableCommand Gun_Next_f = new ExecutableCommand() {
        public void execute() {
            gun_frame++;
            Com.Printf("frame " + gun_frame + "\n");
        }
    };

    static ExecutableCommand Gun_Prev_f = new ExecutableCommand() {
        public void execute() {
            gun_frame--;
            if (gun_frame < 0)
                gun_frame = 0;
            Com.Printf("frame " + gun_frame + "\n");
        }
    };

    static ExecutableCommand Gun_Model_f = new ExecutableCommand() {
        public void execute() {
            if (Commands.Argc() != 2) {
                gun_model = null;
                return;
            }
            String name = "models/" + Commands.Argv(1) + "/tris.md2";
            re.RegisterModel(name, new AsyncCallback<Model>() {
              public void onSuccess(Model response) {
                gun_model = response;
              }

              public void onFailure(Throwable e) {
                // TODO(jgw): doesn't look like anyone cares.
              }
            });
        }
    };

    /*
     * ================== V_RenderView
     * 
     * ==================
     */
    static void RenderView(float stereo_separation) {
        //		extern int entitycmpfnc( const entity_t *, const entity_t * );
        //
        if (cls.state != ca_active)
            return;

        if (!cl.refresh_prepped)
            return; // still loading

        if (cl_timedemo.value != 0.0f) {
            if (cl.timedemo_start == 0)
                cl.timedemo_start = Timer.Milliseconds();
            cl.timedemo_frames++;
        }

        // an invalid frame will just use the exact previous refdef
        // we can't use the old frame if the video mode has changed, though...
        if (cl.frame.valid && (cl.force_refdef || cl_paused.value == 0.0f)) {
            cl.force_refdef = false;

            Video.ClearScene();

            // build a refresh entity list and calc cl.sim*
            // this also calls CL_CalcViewValues which loads
            // v_forward, etc.
            ClientEntities.AddEntities();

            if (cl_testparticles.value != 0.0f)
                TestParticles();
            if (cl_testentities.value != 0.0f)
                TestEntities();
            if (cl_testlights.value != 0.0f)
                TestLights();
            if (cl_testblend.value != 0.0f) {
                cl.refdef.blend[0] = 1.0f;
                cl.refdef.blend[1] = 0.5f;
                cl.refdef.blend[2] = 0.25f;
                cl.refdef.blend[3] = 0.5f;
            }

            // offset vieworg appropriately if we're doing stereo separation
            if (stereo_separation != 0) {
                float[] tmp = new float[3];

                Math3D.VectorScale(cl.v_right, stereo_separation, tmp);
                Math3D.VectorAdd(cl.refdef.vieworg, tmp, cl.refdef.vieworg);
            }

            // never let it sit exactly on a node line, because a water plane
            // can
            // dissapear when viewed with the eye exactly on it.
            // the server protocol only specifies to 1/8 pixel, so add 1/16 in
            // each axis
            cl.refdef.vieworg[0] += 1.0 / 16;
            cl.refdef.vieworg[1] += 1.0 / 16;
            cl.refdef.vieworg[2] += 1.0 / 16;

            cl.refdef.x = scr_vrect.x;
            cl.refdef.y = scr_vrect.y;
            cl.refdef.width = scr_vrect.width;
            cl.refdef.height = scr_vrect.height;
            cl.refdef.fov_y = Math3D.CalcFov(cl.refdef.fov_x, cl.refdef.width,
                    cl.refdef.height);
            cl.refdef.time = cl.time * 0.001f;

            cl.refdef.areabits = cl.frame.areabits;

            if (cl_add_entities.value == 0.0f)
                r_numentities = 0;
            if (cl_add_particles.value == 0.0f)
                r_numparticles = 0;
            if (cl_add_lights.value == 0.0f)
                r_numdlights = 0;
            if (cl_add_blend.value == 0) {
                Math3D.VectorClear(cl.refdef.blend);
            }

            cl.refdef.num_entities = r_numentities;
            cl.refdef.entities = r_entities;
            cl.refdef.num_particles = r_numparticles;
            cl.refdef.num_dlights = r_numdlights;
            cl.refdef.dlights = r_dlights;
            cl.refdef.lightstyles = r_lightstyles;

            cl.refdef.rdflags = cl.frame.playerstate.rdflags;

            // sort entities for better cache locality
            // !!! useless in Java !!!
            //Arrays.sort(cl.refdef.entities, entitycmpfnc);
        }

        re.RenderFrame(cl.refdef);
        if (cl_stats.value != 0.0f)
            Com.Printf("ent:%i  lt:%i  part:%i\n", new Vargs(3).add(
                    r_numentities).add(r_numdlights).add(r_numparticles));
        Screen.AddDirtyPoint(scr_vrect.x, scr_vrect.y);
        Screen.AddDirtyPoint(scr_vrect.x + scr_vrect.width - 1, scr_vrect.y
                + scr_vrect.height - 1);

        Screen.DrawCrosshair();
    }

    /*
     * ============= V_Viewpos_f =============
     */
    static ExecutableCommand Viewpos_f = new ExecutableCommand() {
        public void execute() {
            Com.Printf("(%i %i %i) : %i\n", new Vargs(4).add(
                    (int) cl.refdef.vieworg[0]).add((int) cl.refdef.vieworg[1])
                    .add((int) cl.refdef.vieworg[2]).add(
                            (int) cl.refdef.viewangles[YAW]));
        }
    };

    public static void Init() {
        Commands.addCommand("gun_next", Gun_Next_f);
        Commands.addCommand("gun_prev", Gun_Prev_f);
        Commands.addCommand("gun_model", Gun_Model_f);

        Commands.addCommand("viewpos", Viewpos_f);

        crosshair = ConsoleVariables.Get("crosshair", "0", CVAR_ARCHIVE);

        cl_testblend = ConsoleVariables.Get("cl_testblend", "0", 0);
        cl_testparticles = ConsoleVariables.Get("cl_testparticles", "0", 0);
        cl_testentities = ConsoleVariables.Get("cl_testentities", "0", 0);
        cl_testlights = ConsoleVariables.Get("cl_testlights", "0", 0);

        cl_stats = ConsoleVariables.Get("cl_stats", "0", 0);
    }
}
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
package com.googlecode.gwtquake.shared.render;

import static com.googlecode.gwtquake.shared.common.Constants.CVAR_ARCHIVE;
import static com.googlecode.gwtquake.shared.common.Constants.CVAR_USERINFO;



import com.googlecode.gwtquake.shared.client.Dimension;
import com.googlecode.gwtquake.shared.client.EntityType;
import com.googlecode.gwtquake.shared.client.RendererState;
import com.googlecode.gwtquake.shared.client.Window;
import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.ConsoleVariables;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.ExecutableCommand;
import com.googlecode.gwtquake.shared.common.QuakeFiles;
import com.googlecode.gwtquake.shared.common.QuakeImage;
import com.googlecode.gwtquake.shared.game.Commands;
import com.googlecode.gwtquake.shared.game.ConsoleVariable;
import com.googlecode.gwtquake.shared.game.Plane;
import com.googlecode.gwtquake.shared.util.Math3D;
import com.googlecode.gwtquake.shared.util.Vargs;
import elemental2.dom.DomGlobal;

/**
 * Main
 * 
 * @author cwei
 */
public class Entities {


	/*
	====================================================================
	
	from gl_rmain.c
	
	====================================================================
	*/

	
	
	

	// ============================================================================
	// to port from gl_rmain.c, ...
	// ============================================================================

	/**
	 * R_CullBox
	 * Returns true if the box is completely outside the frustum
	 */
	static final boolean R_CullBox(float[] mins, float[] maxs) {
		assert(mins.length == 3 && maxs.length == 3) : "vec3_t bug";

		if (GlConfig.r_nocull.value != 0)
			return false;

		for (int i = 0; i < 4; i++) {
			if (Math3D.BoxOnPlaneSide(mins, maxs, GlState.frustum[i]) == 2)
				return true;
		}
		return false;
	}

	/**
	 * R_RotateForEntity
	 */
	static final void rotateForEntity(EntityType e) {
	  GlState.gl.glTranslatef(e.origin[0], e.origin[1], e.origin[2]);

	  GlState.gl.glRotatef(e.angles[1], 0, 0, 1);
	  GlState.gl.glRotatef(-e.angles[0], 0, 1, 0);
	  GlState.gl.glRotatef(-e.angles[2], 1, 0, 0);
	}

	/*
	=============================================================
	
	   SPRITE MODELS
	
	=============================================================
	*/

	/**
	 * R_DrawSpriteModel
	 */
	static void drawSpriteModel(EntityType e) {
		float alpha = 1.0F;

		QuakeFiles.dsprframe_t frame;
		QuakeFiles.dsprite_t psprite;

		// don't even bother culling, because it's just a single
		// polygon without a surface cache

		psprite = (QuakeFiles.dsprite_t) GlState.currentmodel.extradata;

		e.frame %= psprite.numframes;

		frame = psprite.frames[e.frame];

		if ((e.flags & Constants.RF_TRANSLUCENT) != 0)
			alpha = e.alpha;

		if (alpha != 1.0F)
		  GlState.gl.glEnable(Gl1Context.GL_BLEND);

		GlState.gl.glColor4f(1, 1, 1, alpha);

		Images.GL_Bind(GlState.currentmodel.skins[e.frame].texnum);

		Images.GL_TexEnv(Gl1Context.GL_MODULATE);

//		if (alpha == 1.0)
//		  gl.glEnable(GLAdapter.GL_ALPHA_TEST);
//		else
//		  gl.glDisable(GLAdapter.GL_ALPHA_TEST);

		GlState.gl.glBegin(Gl1Context._GL_QUADS);

		GlState.gl.glTexCoord2f(0, 1);
		Math3D.VectorMA(e.origin, -frame.origin_y, GlState.vup, GlState.point);
		Math3D.VectorMA(GlState.point, -frame.origin_x, GlState.vright, GlState.point);
		GlState.gl.glVertex3f(GlState.point[0], GlState.point[1], GlState.point[2]);

		GlState.gl.glTexCoord2f(0, 0);
		Math3D.VectorMA(e.origin, frame.height - frame.origin_y, GlState.vup, GlState.point);
		Math3D.VectorMA(GlState.point, -frame.origin_x, GlState.vright, GlState.point);
		GlState.gl.glVertex3f(GlState.point[0], GlState.point[1], GlState.point[2]);

		GlState.gl.glTexCoord2f(1, 0);
		Math3D.VectorMA(e.origin, frame.height - frame.origin_y, GlState.vup, GlState.point);
		Math3D.VectorMA(GlState.point, frame.width - frame.origin_x, GlState.vright, GlState.point);
		GlState.gl.glVertex3f(GlState.point[0], GlState.point[1], GlState.point[2]);

		GlState.gl.glTexCoord2f(1, 1);
		Math3D.VectorMA(e.origin, -frame.origin_y, GlState.vup, GlState.point);
		Math3D.VectorMA(GlState.point, frame.width - frame.origin_x, GlState.vright, GlState.point);
		GlState.gl.glVertex3f(GlState.point[0], GlState.point[1], GlState.point[2]);

		GlState.gl.glEnd();

//		gl.glDisable(GLAdapter.GL_ALPHA_TEST);
		Images.GL_TexEnv(Gl1Context.GL_REPLACE);

		if (alpha != 1.0F)
		  GlState.gl.glDisable(Gl1Context.GL_BLEND);

		GlState.gl.glColor4f(1, 1, 1, 1);
	}

	// ==================================================================================

	/**
	 * R_DrawNullModel
	*/
	static void R_DrawNullModel() {
		if ((GlState.currententity.flags & Constants.RF_FULLBRIGHT) != 0) {
			// cwei wollte blau: shadelight[0] = shadelight[1] = shadelight[2] = 1.0F;
			GlState.shadelight[0] = GlState.shadelight[1] = GlState.shadelight[2] = 0.0F;
			GlState.shadelight[2] = 0.8F;
		}
		else {
			DynamicLights.R_LightPoint(GlState.currententity.origin, GlState.shadelight);
		}

		GlState.gl.glPushMatrix();
		rotateForEntity(GlState.currententity);

		GlState.gl.glDisable(Gl1Context.GL_TEXTURE_2D);
		GlState.gl.glColor3f(GlState.shadelight[0], GlState.shadelight[1], GlState.shadelight[2]);

		// this replaces the TRIANGLE_FAN
		//glut.glutWireCube(gl, 20);

		GlState.gl.glBegin(Gl1Context.GL_TRIANGLE_FAN);
		GlState.gl.glVertex3f(0, 0, -16);
		int i;
		for (i=0 ; i<=4 ; i++) {
		  GlState.gl.glVertex3f((float)(16.0f * Math.cos(i * Math.PI / 2)), (float)(16.0f * Math.sin(i * Math.PI / 2)), 0.0f);
		}
		GlState.gl.glEnd();
		
		GlState.gl.glBegin(Gl1Context.GL_TRIANGLE_FAN);
		GlState.gl.glVertex3f (0, 0, 16);
		for (i=4 ; i>=0 ; i--) {
		  GlState.gl.glVertex3f((float)(16.0f * Math.cos(i * Math.PI / 2)), (float)(16.0f * Math.sin(i * Math.PI / 2)), 0.0f);
		}
		GlState.gl.glEnd();

		
		GlState.gl.glColor3f(1, 1, 1);
		GlState.gl.glPopMatrix();
		GlState.gl.glEnable(Gl1Context.GL_TEXTURE_2D);
	}

	/**
	 * R_DrawEntitiesOnList
	 */
	static void R_DrawEntitiesOnList() {
		if (GlConfig.r_drawentities.value == 0.0f)
			return;

		// draw non-transparent first
		int i;
		for (i = 0; i < GlState.r_newrefdef.num_entities; i++) {
			GlState.currententity = GlState.r_newrefdef.entities[i];
			if ((GlState.currententity.flags & Constants.RF_TRANSLUCENT) != 0)
				continue; // solid

			if ((GlState.currententity.flags & Constants.RF_BEAM) != 0) {
				R_DrawBeam(GlState.currententity);
			}
			else {
				GlState.currentmodel = GlState.currententity.model;
				if (GlState.currentmodel == null) {
					R_DrawNullModel();
					continue;
				}
				switch (GlState.currentmodel.type) {
					case GlConstants.mod_alias :
						Mesh.R_DrawAliasModel(GlState.currententity);
						break;
					case GlConstants.mod_brush :
						Surfaces.R_DrawBrushModel(GlState.currententity);
						break;
					case GlConstants.mod_sprite :
						drawSpriteModel(GlState.currententity);
						break;
					default :
						Com.Error(Constants.ERR_DROP, "Bad modeltype");
						break;
				}
			}
		}
		// draw transparent entities
		// we could sort these if it ever becomes a problem...
		GlState.gl.glDepthMask(false); // no z writes
		for (i = 0; i < GlState.r_newrefdef.num_entities; i++) {
			GlState.currententity = GlState.r_newrefdef.entities[i];
			if ((GlState.currententity.flags & Constants.RF_TRANSLUCENT) == 0)
				continue; // solid

			if ((GlState.currententity.flags & Constants.RF_BEAM) != 0) {
				R_DrawBeam(GlState.currententity);
			}
			else {
				GlState.currentmodel = GlState.currententity.model;

				if (GlState.currentmodel == null) {
					R_DrawNullModel();
					continue;
				}
				switch (GlState.currentmodel.type) {
					case GlConstants.mod_alias :
						Mesh.R_DrawAliasModel(GlState.currententity);
						break;
					case GlConstants.mod_brush :
						Surfaces.R_DrawBrushModel(GlState.currententity);
						break;
					case GlConstants.mod_sprite :
						drawSpriteModel(GlState.currententity);
						break;
					default :
						Com.Error(Constants.ERR_DROP, "Bad modeltype");
						break;
				}
			}
		}
		GlState.gl.glDepthMask(true); // back to writing
	}
	
	/**
	 * R_PolyBlend
	 */
	static void R_PolyBlend() {
		if (GlConfig.gl_polyblend.value == 0.0f)
			return;

		if (GlState.v_blend[3] == 0.0f)
			return;

//		gl.glDisable(GLAdapter.GL_ALPHA_TEST);
		GlState.gl.glEnable(Gl1Context.GL_BLEND);
		GlState.gl.glDisable(Gl1Context.GL_DEPTH_TEST);
		GlState.gl.glDisable(Gl1Context.GL_TEXTURE_2D);

		GlState.gl.glLoadIdentity();

		// FIXME: get rid of these
		GlState.gl.glRotatef(-90, 1, 0, 0); // put Z going up
		GlState.gl.glRotatef(90, 0, 0, 1); // put Z going up

		GlState.gl.glColor4f(GlState.v_blend[0], GlState.v_blend[1], GlState.v_blend[2], GlState.v_blend[3]);

		GlState.gl.glBegin(Gl1Context._GL_QUADS);

		GlState.gl.glVertex3f(10, 100, 100);
		GlState.gl.glVertex3f(10, -100, 100);
		GlState.gl.glVertex3f(10, -100, -100);
		GlState.gl.glVertex3f(10, 100, -100);
		GlState.gl.glEnd();

		GlState.gl.glDisable(Gl1Context.GL_BLEND);
		GlState.gl.glEnable(Gl1Context.GL_TEXTURE_2D);
//		gl.glEnable(GLAdapter.GL_ALPHA_TEST);

		GlState.gl.glColor4f(1, 1, 1, 1);
	}

	// =======================================================================

	/**
	 * R_SetFrustum
	 */
	static void R_SetFrustum() {
		// rotate VPN right by FOV_X/2 degrees
		Math3D.RotatePointAroundVector(GlState.frustum[0].normal, GlState.vup, GlState.vpn, - (90f - GlState.r_newrefdef.fov_x / 2f));
		// rotate VPN left by FOV_X/2 degrees
		Math3D.RotatePointAroundVector(GlState.frustum[1].normal, GlState.vup, GlState.vpn, 90f - GlState.r_newrefdef.fov_x / 2f);
		// rotate VPN up by FOV_X/2 degrees
		Math3D.RotatePointAroundVector(GlState.frustum[2].normal, GlState.vright, GlState.vpn, 90f - GlState.r_newrefdef.fov_y / 2f);
		// rotate VPN down by FOV_X/2 degrees
		Math3D.RotatePointAroundVector(GlState.frustum[3].normal, GlState.vright, GlState.vpn, - (90f - GlState.r_newrefdef.fov_y / 2f));

		for (int i = 0; i < 4; i++) {
			GlState.frustum[i].type = Constants.PLANE_ANYZ;
			GlState.frustum[i].dist = Math3D.DotProduct(GlState.r_origin, GlState.frustum[i].normal);
			GlState.frustum[i].signbits = (byte) Plane.SignbitsForPlane(GlState.frustum[i]);
		}
	}

	// =======================================================================

	/**
	 * R_SetupFrame
	 */
	static void R_SetupFrame() {
		GlState.r_framecount++;

		//	build the transformation matrix for the given view angles
		Math3D.VectorCopy(GlState.r_newrefdef.vieworg, GlState.r_origin);

		Math3D.AngleVectors(GlState.r_newrefdef.viewangles, GlState.vpn, GlState.vright, GlState.vup);

		//	current viewcluster
		Leaf leaf;
		if ((GlState.r_newrefdef.rdflags & Constants.RDF_NOWORLDMODEL) == 0) {
			GlState.r_oldviewcluster = GlState.r_viewcluster;
			GlState.r_oldviewcluster2 = GlState.r_viewcluster2;
			leaf = Models.Mod_PointInLeaf(GlState.r_origin, GlState.r_worldmodel);

			//GlState.r_viewcluster = GlState.r_viewcluster2 = leaf.cluster;
			GlState.r_viewcluster = leaf.cluster;
			GlState.r_viewcluster2 = leaf.cluster;
			// check above and below so crossing solid water doesn't draw wrong
			if (leaf.contents == 0) { // look down a bit
				Math3D.VectorCopy(GlState.r_origin, GlState.temp);
				GlState.temp[2] -= 16;
				leaf = Models.Mod_PointInLeaf(GlState.temp, GlState.r_worldmodel);
				if ((leaf.contents & Constants.CONTENTS_SOLID) == 0 && (leaf.cluster != GlState.r_viewcluster2))
					GlState.r_viewcluster2 = leaf.cluster;
			}
			else { // look up a bit
				Math3D.VectorCopy(GlState.r_origin, GlState.temp);
				GlState.temp[2] += 16;
				leaf = Models.Mod_PointInLeaf(GlState.temp, GlState.r_worldmodel);
				if ((leaf.contents & Constants.CONTENTS_SOLID) == 0 && (leaf.cluster != GlState.r_viewcluster2))
				GlState.r_viewcluster2 = leaf.cluster;
			}
		}

		for (int i = 0; i < 4; i++)
			GlState.v_blend[i] = GlState.r_newrefdef.blend[i];

		GlState.c_brush_polys = 0;
		GlState.c_alias_polys = 0;

		// clear out the portion of the screen that the NOWORLDMODEL defines
		if ((GlState.r_newrefdef.rdflags & Constants.RDF_NOWORLDMODEL) != 0) {
		  GlState.gl.glEnable(Gl1Context.GL_SCISSOR_TEST);
		  GlState.gl.glClearColor(0.3f, 0.3f, 0.3f, 1.0f);
		  GlState.gl.glScissor(
				GlState.r_newrefdef.x,
				GlState.vid.height - GlState.r_newrefdef.height - GlState.r_newrefdef.y,
				GlState.r_newrefdef.width,
				GlState.r_newrefdef.height);
		  GlState.gl.glClear(Gl1Context.GL_COLOR_BUFFER_BIT | Gl1Context.GL_DEPTH_BUFFER_BIT);
		  GlState.gl.glClearColor(1.0f, 0.0f, 0.5f, 0.5f);
		  GlState.gl.glDisable(Gl1Context.GL_SCISSOR_TEST);
		}
	}

	/**
	 * MYgluPerspective
	 * 
	 * @param fovy
	 * @param aspect
	 * @param zNear
	 * @param zFar
	 */
	static void MYgluPerspective(double fovy, double aspect, double zNear, double zFar) {
		double ymax = zNear * Math.tan(fovy * Math.PI / 360.0);
		double ymin = -ymax;

		double xmin = ymin * aspect;
		double xmax = ymax * aspect;

		xmin += - (2 * GlConfig.gl_state.camera_separation) / zNear;
		xmax += - (2 * GlConfig.gl_state.camera_separation) / zNear;

		GlState.gl.glFrustum(xmin, xmax, ymin, ymax, zNear, zFar);
	}

	/**
	 * R_SetupGL
	 */
	static void R_SetupGL() {

		//
		// set up viewport
		//
		//int x = (int) Math.floor(r_newrefdef.x * vid.width / vid.width);
		int x = GlState.r_newrefdef.x;
		//int x2 = (int) Math.ceil((r_newrefdef.x + r_newrefdef.width) * vid.width / vid.width);
		int x2 = GlState.r_newrefdef.x + GlState.r_newrefdef.width;
		//int y = (int) Math.floor(vid.height - r_newrefdef.y * vid.height / vid.height);
		int y = GlState.vid.height - GlState.r_newrefdef.y;
		//int y2 = (int) Math.ceil(vid.height - (r_newrefdef.y + r_newrefdef.height) * vid.height / vid.height);
		int y2 = GlState.vid.height - (GlState.r_newrefdef.y + GlState.r_newrefdef.height);

		int w = x2 - x;
		int h = y - y2;

		GlState.gl.glViewport(x, y2, w, h);

		//
		// set up projection matrix
		//
		float screenaspect = (float) GlState.r_newrefdef.width / GlState.r_newrefdef.height;
		GlState.gl.glMatrixMode(Gl1Context.GL_PROJECTION);
		GlState.gl.glLoadIdentity();
		MYgluPerspective(GlState.r_newrefdef.fov_y, screenaspect, 4, 4096);

		GlState.gl.glCullFace(Gl1Context.GL_FRONT);

		GlState.gl.glMatrixMode(Gl1Context.GL_MODELVIEW);
		GlState.gl.glLoadIdentity();

		GlState.gl.glRotatef(-90, 1, 0, 0); // put Z going up
		GlState.gl.glRotatef(90, 0, 0, 1); // put Z going up
		GlState.gl.glRotatef(-GlState.r_newrefdef.viewangles[2], 1, 0, 0);
		GlState.gl.glRotatef(-GlState.r_newrefdef.viewangles[0], 0, 1, 0);
		GlState.gl.glRotatef(-GlState.r_newrefdef.viewangles[1], 0, 0, 1);
		GlState.gl.glTranslatef(-GlState.r_newrefdef.vieworg[0], -GlState.r_newrefdef.vieworg[1], -GlState.r_newrefdef.vieworg[2]);

		GlState.gl.glGetFloat(Gl1Context._GL_MODELVIEW_MATRIX, GlState.r_world_matrix);
		GlState.r_world_matrix.clear();

		//
		// set drawing parms
		//
		if (GlConfig.gl_cull.value != 0.0f)
		  GlState.gl.glEnable(Gl1Context.GL_CULL_FACE);
		else
		  GlState.gl.glDisable(Gl1Context.GL_CULL_FACE);

		GlState.gl.glDisable(Gl1Context.GL_BLEND);
//		gl.glDisable(GLAdapter.GL_ALPHA_TEST);
		GlState.gl.glEnable(Gl1Context.GL_DEPTH_TEST);
	}

	/**
	 * R_Clear
	 */
	static void R_Clear() {
		if (GlConfig.gl_ztrick.value != 0.0f) {

			if (GlConfig.gl_clear.value != 0.0f) {
			  GlState.gl.glClear(Gl1Context.GL_COLOR_BUFFER_BIT);
			}

			GlState.trickframe++;
			if ((GlState.trickframe & 1) != 0) {
				GlState.gldepthmin = 0;
				GlState.gldepthmax = 0.49999f;
				GlState.gl.glDepthFunc(Gl1Context.GL_LEQUAL);
			}
			else {
				GlState.gldepthmin = 1;
				GlState.gldepthmax = 0.5f;
				GlState.gl.glDepthFunc(Gl1Context.GL_GEQUAL);
			}
		}
		else {
			if (GlConfig.gl_clear.value != 0.0f)
			  GlState.gl.glClear(Gl1Context.GL_COLOR_BUFFER_BIT | Gl1Context.GL_DEPTH_BUFFER_BIT);
			else
			  GlState.gl.glClear(Gl1Context.GL_DEPTH_BUFFER_BIT);

			GlState.gldepthmin = 0;
			GlState.gldepthmax = 1;
			GlState.gl.glDepthFunc(Gl1Context.GL_LEQUAL);
		}
		GlState.gl.glDepthRange(GlState.gldepthmin, GlState.gldepthmax);
	}

	/**
	 * R_Flash
	 */
	static void R_Flash() {
		R_PolyBlend();
	}

	/**
	 * R_RenderView
	 * r_newrefdef must be set before the first call
	 */
	static void R_RenderView(RendererState fd) {
		if (GlConfig.r_norefresh.value != 0.0f)
			return;

		GlState.r_newrefdef = fd;

		// included by cwei
		if (GlState.r_newrefdef == null) {
			Com.Error(Constants.ERR_DROP, "R_RenderView: refdef_t fd is null");
		}

		if (GlState.r_worldmodel == null && (GlState.r_newrefdef.rdflags & Constants.RDF_NOWORLDMODEL) == 0)
			Com.Error(Constants.ERR_DROP, "R_RenderView: NULL worldmodel");

		if (GlConfig.r_speeds.value != 0.0f) {
			GlState.c_brush_polys = 0;
			GlState.c_alias_polys = 0;
		}

		DynamicLights.push();

		if (GlConfig.gl_finish.value != 0.0f)
		  GlState.gl.glFinish();

		R_SetupFrame();

		R_SetFrustum();

		R_SetupGL();

		Surfaces.R_MarkLeaves(); // done here so we know if we're in water

		Surfaces.R_DrawWorld();

		R_DrawEntitiesOnList();

		DynamicLights.render();

		com.googlecode.gwtquake.shared.render.Particles.draw();

		Surfaces.R_DrawAlphaSurfaces();

		R_Flash();

		if (GlConfig.r_speeds.value != 0.0f) {
			Window.Printf(
				Constants.PRINT_ALL,
				"%4i wpoly %4i epoly %i tex %i lmaps\n",
				new Vargs(4).add(GlState.c_brush_polys).add(GlState.c_alias_polys).add(GlState.c_visible_textures).add(GlState.c_visible_lightmaps));
		}
	}

	/**
	 * R_SetGL2D
	 */
	static void R_SetGL2D() {
		// set 2D virtual screen size
	  GlState.gl.glViewport(0, 0, GlState.vid.width, GlState.vid.height);
	  GlState.gl.glMatrixMode(Gl1Context.GL_PROJECTION);
	  GlState.gl.glLoadIdentity();
	  GlState.gl.glOrtho(0, GlState.vid.width, GlState.vid.height, 0, -99999, 99999);
	  GlState.gl.glMatrixMode(Gl1Context.GL_MODELVIEW);
	  GlState.gl.glLoadIdentity();
	  GlState.gl.glDisable(Gl1Context.GL_DEPTH_TEST);
	  GlState.gl.glDisable(Gl1Context.GL_CULL_FACE);
	  GlState.gl.glDisable(Gl1Context.GL_BLEND);
//	  gl.glEnable(GLAdapter.GL_ALPHA_TEST);
	  GlState.gl.glColor4f(1, 1, 1, 1);
	}

	/**
	 *	R_SetLightLevel
	 */
	static void R_SetLightLevel() {
		if ((GlState.r_newrefdef.rdflags & Constants.RDF_NOWORLDMODEL) != 0)
			return;

		// save off light value for server to look at (BIG HACK!)

		DynamicLights.R_LightPoint(GlState.r_newrefdef.vieworg, GlState.light);

		// pick the greatest component, which should be the same
		// as the mono value returned by software
		if (GlState.light[0] > GlState.light[1]) {
			if (GlState.light[0] > GlState.light[2])
				GlConfig.r_lightlevel.value = 150 * GlState.light[0];
			else
				GlConfig.r_lightlevel.value = 150 * GlState.light[2];
		}
		else {
			if (GlState.light[1] > GlState.light[2])
				GlConfig.r_lightlevel.value = 150 * GlState.light[1];
			else
				GlConfig.r_lightlevel.value = 150 * GlState.light[2];
		}
	}

	/**
	 * R_RenderFrame
	 */
	protected static void R_RenderFrame(RendererState fd) {
		R_RenderView(fd);
		R_SetLightLevel();
		R_SetGL2D();
	}

	/**
	 * R_DrawBeam
	 */
	static void R_DrawBeam(EntityType e) {
		GlState.oldorigin[0] = e.oldorigin[0];
		GlState.oldorigin[1] = e.oldorigin[1];
		GlState.oldorigin[2] = e.oldorigin[2];

		GlState.origin[0] = e.origin[0];
		GlState.origin[1] = e.origin[1];
		GlState.origin[2] = e.origin[2];

		GlState.normalized_direction[0] = GlState.direction[0] = GlState.oldorigin[0] - GlState.origin[0];
		GlState.normalized_direction[1] = GlState.direction[1] = GlState.oldorigin[1] - GlState.origin[1];
		GlState.normalized_direction[2] = GlState.direction[2] = GlState.oldorigin[2] - GlState.origin[2];

		if (Math3D.VectorNormalize(GlState.normalized_direction) == 0.0f)
			return;

		Math3D.PerpendicularVector(GlState.perpvec, GlState.normalized_direction);
		Math3D.VectorScale(GlState.perpvec, e.frame / 2, GlState.perpvec);

		for (int i = 0; i < 6; i++) {
			Math3D.RotatePointAroundVector(
				GlState.start_points[i],
				GlState.normalized_direction,
				GlState.perpvec,
				(360.0f / GlConstants.NUM_BEAM_SEGS) * i);

			Math3D.VectorAdd(GlState.start_points[i], GlState.origin, GlState.start_points[i]);
			Math3D.VectorAdd(GlState.start_points[i], GlState.direction, GlState.end_points[i]);
		}

		GlState.gl.glDisable(Gl1Context.GL_TEXTURE_2D);
		GlState.gl.glEnable(Gl1Context.GL_BLEND);
		GlState.gl.glDepthMask(false);

		float r = (QuakeImage.PALETTE_ABGR[e.skinnum & 0xFF]) & 0xFF;
		float g = (QuakeImage.PALETTE_ABGR[e.skinnum & 0xFF] >> 8) & 0xFF;
		float b = (QuakeImage.PALETTE_ABGR[e.skinnum & 0xFF] >> 16) & 0xFF;

		r *= 1 / 255.0f;
		g *= 1 / 255.0f;
		b *= 1 / 255.0f;

		GlState.gl.glColor4f(r, g, b, e.alpha);

		GlState.gl.glBegin(Gl1Context.GL_TRIANGLE_STRIP);
		
		float[] v;
		
		for (int i = 0; i < GlConstants.NUM_BEAM_SEGS; i++) {
			v = GlState.start_points[i];
			GlState.gl.glVertex3f(v[0], v[1], v[2]);
			v = GlState.end_points[i];
			GlState.gl.glVertex3f(v[0], v[1], v[2]);
			v = GlState.start_points[(i + 1) % GlConstants.NUM_BEAM_SEGS];
			GlState.gl.glVertex3f(v[0], v[1], v[2]);
			v = GlState.end_points[(i + 1) % GlConstants.NUM_BEAM_SEGS];
			GlState.gl.glVertex3f(v[0], v[1], v[2]);
		}
		GlState.gl.glEnd();

		GlState.gl.glEnable(Gl1Context.GL_TEXTURE_2D);
		GlState.gl.glDisable(Gl1Context.GL_BLEND);
		GlState.gl.glDepthMask(true);
	}
}

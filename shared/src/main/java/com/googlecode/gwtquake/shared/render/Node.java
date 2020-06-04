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

import java.nio.ByteBuffer;

import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.Globals;
import com.googlecode.gwtquake.shared.game.*;
import com.googlecode.gwtquake.shared.util.Math3D;
import com.googlecode.gwtquake.shared.util.Vec3Cache;

public class Node {
	//	common with leaf
	public int contents; // -1, to differentiate from leafs
	public int visframe; // node needs to be traversed if current

	//public float minmaxs[] = new float[6]; // for bounding box culling
	public float mins[] = new float[3]; // for bounding box culling
	public float maxs[] = new float[3]; // for bounding box culling

	public Node parent;

	//	node specific
	public Plane plane;
	public Node children[] = new Node[2];

	// unsigned short
	public int firstsurface;
	public int numsurfaces;
	/**
	 * R_RecursiveWorldNode
	 */
	static void R_RecursiveWorldNode (Node node)
	{
		if (node.contents == Constants.CONTENTS_SOLID)
			return;		// solid
		
		if (node.visframe != GlState.r_visframecount)
			return;
			
		if (Entities.R_CullBox(node.mins, node.maxs))
			return;
	
		int c;
		Surface mark;
		// if a leaf node, draw stuff
		if (node.contents != -1)
		{
			Leaf pleaf = (Leaf)node;
	
			// check for door connected areas
			if (GlState.r_newrefdef.areabits != null)
			{
				if ( ((GlState.r_newrefdef.areabits[pleaf.area >> 3] & 0xFF) & (1 << (pleaf.area & 7)) ) == 0 )
					return;		// not visible
			}
	
			int markp = 0;
	
			mark = pleaf.getMarkSurface(markp); // first marked surface
			c = pleaf.nummarksurfaces;
	
			if (c != 0)
			{
				do
				{
					mark.visframe = GlState.r_framecount;
					mark = pleaf.getMarkSurface(++markp); // next surface
				} while (--c != 0);
			}
	
			return;
		}
	
		// node is just a decision point, so go down the apropriate sides
	
		// find which side of the node we are on
		Plane plane = node.plane;
		float dot;
		switch (plane.type)
		{
		case Constants.PLANE_X:
			dot = Surfaces.modelorg[0] - plane.dist;
			break;
		case Constants.PLANE_Y:
			dot = Surfaces.modelorg[1] - plane.dist;
			break;
		case Constants.PLANE_Z:
			dot = Surfaces.modelorg[2] - plane.dist;
			break;
		default:
			dot = Math3D.DotProduct(Surfaces.modelorg, plane.normal) - plane.dist;
			break;
		}
	
		int side, sidebit;
		if (dot >= 0.0f)
		{
			side = 0;
			sidebit = 0;
		}
		else
		{
			side = 1;
			sidebit = Constants.SURF_PLANEBACK;
		}
	
		// recurse down the children, front side first
		R_RecursiveWorldNode(node.children[side]);
	
		// draw stuff
		Surface surf;
		Image image;
		//for ( c = node.numsurfaces, surf = r_worldmodel.surfaces[node.firstsurface]; c != 0 ; c--, surf++)
		for ( c = 0; c < node.numsurfaces; c++)
		{
			surf = GlState.r_worldmodel.surfaces[node.firstsurface + c];
			if (surf.visframe != GlState.r_framecount)
				continue;
	
			if ( (surf.flags & Constants.SURF_PLANEBACK) != sidebit )
				continue;		// wrong side
	
			if ((surf.texinfo.flags & Constants.SURF_SKY) != 0)
			{	// just adds to visible sky bounds
				SkyBox.R_AddSkySurface(surf);
			}
			else if ((surf.texinfo.flags & (Constants.SURF_TRANS33 | Constants.SURF_TRANS66)) != 0)
			{	// add to the translucent chain
				surf.texturechain = Surfaces.r_alpha_surfaces;
				Surfaces.r_alpha_surfaces = surf;
			}
			else
			{
				if (  ( surf.flags & Constants.SURF_DRAWTURB) == 0 )
				{
					Surface.GL_RenderLightmappedPoly( surf );
				}
				else
				{
					// the polygon is visible, so add it to the texture
					// sorted chain
					// FIXME: this is a hack for animation
					image = Texture.R_TextureAnimation(surf.texinfo);
					surf.texturechain = image.texturechain;
					image.texturechain = surf;
				}
			}
		}
		// recurse down the back side
		R_RecursiveWorldNode(node.children[1 - side]);
	}
	/**
	 * RecursiveLightPoint
	 * @param node
	 * @param start
	 * @param end
	 * @return
	 */
	static int RecursiveLightPoint (Node node, float[] start, float[] end)
	{
		if (node.contents != -1)
			return -1;		// didn't hit anything
	
		// calculate mid point
	
		// FIXME: optimize for axial
		Plane plane = node.plane;
		float front = Math3D.DotProduct (start, plane.normal) - plane.dist;
		float back = Math3D.DotProduct (end, plane.normal) - plane.dist;
		boolean side = (front < 0);
		int sideIndex = (side) ? 1 : 0;
	
		if ( (back < 0) == side)
			return RecursiveLightPoint (node.children[sideIndex], start, end);
	
		float frac = front / (front-back);
		float[] mid = Vec3Cache.get();
		mid[0] = start[0] + (end[0] - start[0])*frac;
		mid[1] = start[1] + (end[1] - start[1])*frac;
		mid[2] = start[2] + (end[2] - start[2])*frac;
	
		// go down front side	
		int r = RecursiveLightPoint (node.children[sideIndex], start, mid);
		if (r >= 0) {
			Vec3Cache.release(); // mid
			return r;		// hit something
		}
		
		if ( (back < 0) == side ) {
			Vec3Cache.release(); // mid
			return -1; // didn't hit anuthing
		}
		
		// check for impact on this node
		Math3D.VectorCopy (mid, DynamicLights.lightspot);
		DynamicLights.lightplane = plane;
		int surfIndex = node.firstsurface;
	
		Surface surf;
		int s, t, ds, dt;
		Texture tex;
		ByteBuffer lightmap;
		int maps;
		for (int i=0 ; i<node.numsurfaces ; i++, surfIndex++)
		{
			surf = GlState.r_worldmodel.surfaces[surfIndex];
			
			if ((surf.flags & (Constants.SURF_DRAWTURB | Constants.SURF_DRAWSKY)) != 0) 
				continue;	// no lightmaps
	
			tex = surf.texinfo;
		
			s = (int)(Math3D.DotProduct (mid, tex.vecs[0]) + tex.vecs[0][3]);
			t = (int)(Math3D.DotProduct (mid, tex.vecs[1]) + tex.vecs[1][3]);
	
			if (s < surf.texturemins[0] || t < surf.texturemins[1])
				continue;
		
			ds = s - surf.texturemins[0];
			dt = t - surf.texturemins[1];
		
			if ( ds > surf.extents[0] || dt > surf.extents[1] )
				continue;
	
			if (surf.samples == null)
				return 0;
	
			ds >>= 4;
			dt >>= 4;
	
			lightmap = surf.samples;
			int lightmapIndex = 0;
	
			Math3D.VectorCopy (Globals.vec3_origin, DynamicLights.pointcolor);
			if (lightmap != null)
			{
				float[] rgb;
				lightmapIndex += 3 * (dt * ((surf.extents[0] >> 4) + 1) + ds);
	
				float scale0, scale1, scale2;
				for (maps = 0 ; maps < Constants.MAXLIGHTMAPS && surf.styles[maps] != (byte)255; maps++)
				{
					rgb = GlState.r_newrefdef.lightstyles[surf.styles[maps] & 0xFF].rgb;
					scale0 = GlConfig.gl_modulate.value * rgb[0];
					scale1 = GlConfig.gl_modulate.value * rgb[1];
					scale2 = GlConfig.gl_modulate.value * rgb[2];
	
					DynamicLights.pointcolor[0] += (lightmap.get(lightmapIndex + 0) & 0xFF) * scale0 * (1.0f/255);
					DynamicLights.pointcolor[1] += (lightmap.get(lightmapIndex + 1) & 0xFF) * scale1 * (1.0f/255);
					DynamicLights.pointcolor[2] += (lightmap.get(lightmapIndex + 2) & 0xFF) * scale2 * (1.0f/255);
					lightmapIndex += 3 * ((surf.extents[0] >> 4) + 1) * ((surf.extents[1] >> 4) + 1);
				}
			}
			Vec3Cache.release(); // mid
			return 1;
		}
	
		// go down back side
		r = RecursiveLightPoint (node.children[1 - sideIndex], mid, end);
		Vec3Cache.release(); // mid
		return r;
	}
  /*
  =================
  Mod_SetParent
  =================
  */
  static void Mod_SetParent(Node node, Node parent)
  {
  	node.parent = parent;
  	if (node.contents != -1) return;
  	Mod_SetParent(node.children[0], node);
  	Mod_SetParent(node.children[1], node);
  }

}

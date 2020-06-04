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



import java.util.Arrays;

import com.googlecode.gwtquake.shared.client.Lightstyle;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.QuakeFiles;
import com.googlecode.gwtquake.shared.game.Plane;
import com.googlecode.gwtquake.shared.util.Lib;
import com.googlecode.gwtquake.shared.util.Math3D;

public class Model implements Cloneable {
	
	public String name = "";

	public int registration_sequence;

	// was enum modtype_t
	public int type;
	public int numframes;

	public int flags;

	//
	// volume occupied by the model graphics
	//		
	public float[] mins = { 0, 0, 0 }, maxs = { 0, 0, 0 };
	public float radius;

	//
	// solid volume for clipping 
	//
	public boolean clipbox;
	public float clipmins[] = { 0, 0, 0 }, clipmaxs[] = { 0, 0, 0 };

	//
	// brush model
	//
	public int firstmodelsurface, nummodelsurfaces;
	public int lightmap; // only for submodels

	public int numsubmodels;
	public SubModel submodels[];

	public int numplanes;
	public Plane planes[];

	public int numleafs; // number of visible leafs, not counting 0
	public Leaf leafs[];

	public int numvertexes;
	public Vertex vertexes[];

	public int numedges;
	public Edge edges[];

	public int numnodes;
	public int firstnode;
	public Node nodes[];

	public int numtexinfo;
	public Texture texinfo[];

	public int numsurfaces;
	public Surface surfaces[];

	public int numsurfedges;
	public int surfedges[];

	public int nummarksurfaces;
	public Surface marksurfaces[];

	public QuakeFiles.dvis_t vis;

	public byte lightdata[];

	// for alias models and skins
	// was image_t *skins[]; (array of pointers)
	public Image skins[] = new Image[Constants.MAX_MD2SKINS];

	public int extradatasize;

	// or whatever
	public Object extradata;

    /**
	 * GL_BeginBuildingLightmaps
	 */
    void GL_BeginBuildingLightmaps()
	{
		GlState.gl.log("BeginBuildingLightmaps!");

		// static lightstyle_t	lightstyles[MAX_LIGHTSTYLES];

		// init lightstyles
		if ( Surfaces.lightstyles == null ) {
			Surfaces.lightstyles = new Lightstyle[Constants.MAX_LIGHTSTYLES];
			for (int i = 0; i < Surfaces.lightstyles.length; i++)
			{
				Surfaces.lightstyles[i] = new Lightstyle();
			}
		}

		// memset( gl_lms.allocated, 0, sizeof(gl_lms.allocated) );
		Arrays.fill(Surfaces.gl_lms.allocated, 0);

		GlState.r_framecount = 1;		// no dlightcache

		Images.GL_EnableMultitexture( true );
		Images.GL_SelectTexture( Gl1Context.GL_TEXTURE1);

		/*
		** setup the base lightstyles so the lightmaps won't have to be regenerated
		** the first time they're seen
		*/
		for (int i=0 ; i < Constants.MAX_LIGHTSTYLES ; i++)
		{
			Surfaces.lightstyles[i].rgb[0] = 1;
			Surfaces.lightstyles[i].rgb[1] = 1;
			Surfaces.lightstyles[i].rgb[2] = 1;
			Surfaces.lightstyles[i].white = 3;
		}
		GlState.r_newrefdef.lightstyles = Surfaces.lightstyles;

		if (GlState.lightmap_textures == 0)
		{
			GlState.lightmap_textures = GlConstants.TEXNUM_LIGHTMAPS;
		}

		Surfaces.gl_lms.current_lightmap_texture = 1;

		/*
		** if mono lightmaps are enabled and we want to use alpha
		** blending (a,1-a) then we're likely running on a 3DLabs
		** Permedia2.  In a perfect world we'd use a GL_ALPHA lightmap
		** in order to conserve space and maximize bandwidth, however
		** this isn't a perfect world.
		**
		** So we have to use alpha lightmaps, but stored in GL_RGBA format,
		** which means we only get 1/16th the color resolution we should when
		** using alpha lightmaps.  If we find another board that supports
		** only alpha lightmaps but that can at least support the GL_ALPHA
		** format then we should change this code to use real alpha maps.
		*/

		char format = GlConfig.gl_monolightmap.string.toUpperCase().charAt(0);

		if ( format == 'A' )
		{
			Surfaces.gl_lms.internal_format = Images.gl_tex_alpha_format;
		}
		/*
		** try to do hacked colored lighting with a blended texture
		*/
		else if ( format == 'C' )
		{
			Surfaces.gl_lms.internal_format = Images.gl_tex_alpha_format;
		}
		else if ( format == 'I' )
		{
			GlState.gl.log("INTENSITY");
			Surfaces.gl_lms.internal_format = 1;
		}
		else if ( format == 'L' )
		{
			GlState.gl.log("LUMINANCE");
			Surfaces.gl_lms.internal_format = Gl1Context.GL_LUMINANCE;
		}
		else
		{
			Surfaces.gl_lms.internal_format = Images.gl_tex_solid_format;
		}

		if (Surfaces.dummy == null) {
			Surfaces.dummy = Lib.newIntBuffer(128*128);
			for (int p = 0; p < 128 * 128; p++) {
				Surfaces.dummy.put(p, 0x0ffffffff);
			}
		}

		/*
		** initialize the dynamic lightmap texture
		*/
		Images.GL_Bind( GlConfig.gl_state.lightmap_textures + 0 );
		GlState.gl.glTexParameterf(Gl1Context.GL_TEXTURE_2D, Gl1Context.GL_TEXTURE_MIN_FILTER, Gl1Context.GL_LINEAR);
		GlState.gl.glTexParameterf(Gl1Context.GL_TEXTURE_2D, Gl1Context.GL_TEXTURE_MAG_FILTER, Gl1Context.GL_LINEAR);
		GlState.gl.glTexImage2D( Gl1Context.GL_TEXTURE_2D,
					   0,
					   Surfaces.GL_LIGHTMAP_FORMAT,
					   Surfaces.BLOCK_WIDTH, Surfaces.BLOCK_HEIGHT,
					   0,
					   Surfaces.GL_LIGHTMAP_FORMAT,
					   Gl1Context.GL_UNSIGNED_BYTE,
					   Surfaces.dummy );
	}

    /*
================
Mod_Free
================
*/
    void Mod_Free()
{
      clear();
}

    public void clear() {
		name = "";
		registration_sequence = 0;

		// was enum modtype_t
		type = 0;
		numframes = 0;
		flags = 0;

		//
		// volume occupied by the model graphics
		//		
		Math3D.VectorClear(mins);
		Math3D.VectorClear(maxs);
		radius = 0;

		//
		// solid volume for clipping 
		//
		clipbox = false;
		Math3D.VectorClear(clipmins);
		Math3D.VectorClear(clipmaxs);

		//
		// brush model
		//
		firstmodelsurface = nummodelsurfaces = 0;
		lightmap = 0; // only for submodels

		numsubmodels = 0;
		submodels = null;

		numplanes = 0;
		planes = null;

		numleafs = 0; // number of visible leafs, not counting 0
		leafs = null;

		numvertexes = 0;
		vertexes = null;

		numedges = 0;
		edges = null;

		numnodes = 0;
		firstnode = 0;
		nodes = null;

		numtexinfo = 0;
		texinfo = null;

		numsurfaces = 0;
		surfaces = null;

		numsurfedges = 0;
		surfedges = null;

		nummarksurfaces = 0;
		marksurfaces = null;

		vis = null;

		lightdata = null;

		// for alias models and skins
		// was image_t *skins[]; (array of pointers)
		Arrays.fill(skins, null);

		extradatasize = 0;
		// or whatever
		extradata = null;
	}

	// TODO replace with set(model_t from)
	public Model copy() {
		Model theClone;
    theClone = dup();
		theClone.mins = Lib.clone(this.mins);
		theClone.maxs = Lib.clone(this.maxs);
		theClone.clipmins = Lib.clone(this.clipmins);
		theClone.clipmaxs = Lib.clone(this.clipmaxs);
		return theClone;
	}

	public Model dup() {
	  Model clone = new Model();

	  clone.name = name;
	  clone.registration_sequence = registration_sequence;
	  clone.type = type;
	  clone.numframes = numframes;
	  clone.flags = flags;
	  clone.mins = mins;
	  clone.maxs = maxs;
	  clone.radius = radius;
	  clone.clipbox = clipbox;
	  clone.clipmins = clipmins;
	  clone.clipmaxs = clipmaxs;
	  clone.firstmodelsurface = firstmodelsurface;
	  clone.nummodelsurfaces = nummodelsurfaces;
	  clone.lightmap = lightmap;
	  clone.numsubmodels = numsubmodels;
	  clone.submodels = submodels;
	  clone.numplanes = numplanes;
	  clone.planes = planes;
	  clone.numleafs = numleafs;
	  clone.leafs = leafs;
	  clone.numvertexes = numvertexes;
	  clone.vertexes = vertexes;
	  clone.numedges = numedges;
	  clone.edges = edges;
	  clone.numnodes = numnodes;
	  clone.firstnode = firstnode;
	  clone.nodes = nodes;
	  clone.numtexinfo = numtexinfo;
	  clone.texinfo = texinfo;
	  clone.numsurfaces = numsurfaces;
	  clone.surfaces = surfaces;
	  clone.numsurfedges = numsurfedges;
	  clone.surfedges = surfedges;
	  clone.nummarksurfaces = nummarksurfaces;
	  clone.marksurfaces = marksurfaces;
	  clone.vis = vis;
	  clone.lightdata = lightdata;
	  clone.skins = skins;
	  clone.extradatasize = extradatasize;
	  clone.extradata = extradata;

	  return clone;
	}

}

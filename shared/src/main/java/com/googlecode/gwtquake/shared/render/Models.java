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



import com.googlecode.gwtquake.shared.client.Window;
import com.googlecode.gwtquake.shared.common.AsyncCallback;
import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.Command;
import com.googlecode.gwtquake.shared.common.Compatibility;
import com.googlecode.gwtquake.shared.common.ConsoleVariables;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.Lump;
import com.googlecode.gwtquake.shared.common.QuakeFiles;
import com.googlecode.gwtquake.shared.common.QuakeImage;
import com.googlecode.gwtquake.shared.common.ResourceLoader;
import com.googlecode.gwtquake.shared.common.TextureInfo;
import com.googlecode.gwtquake.shared.game.ConsoleVariable;
import com.googlecode.gwtquake.shared.game.Plane;
import com.googlecode.gwtquake.shared.util.Lib;
import com.googlecode.gwtquake.shared.util.Math3D;
import com.googlecode.gwtquake.shared.util.Vargs;
import elemental2.dom.DomGlobal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

//import com.google.gwt.user.client.Timer;


/**
 * Model
 *  
 * @author cwei
 */
public class Models  {
	
	// models.c -- model loading and caching
	static Model	loadmodel;
	static int modfilelen;

	static byte[] mod_novis = new byte[Constants.MAX_MAP_LEAFS/8];

	static final int MAX_MOD_KNOWN = 512;
	static Model world_model;

	// the inline * models from the current map are kept seperate
	static Model[] mod_inline = new Model[MAX_MOD_KNOWN];

//	abstract void GL_SubdivideSurface(ModelSurface surface); // Warp.java

	/*
	===============
	Mod_PointInLeaf
	===============
	*/
	static Leaf Mod_PointInLeaf(float[] p, Model model)
	{
		Node node;
		float	d;
		Plane plane;
	
		if (model == null || model.nodes == null)
			Com.Error (Constants.ERR_DROP, "Mod_PointInLeaf: bad model");

		node = model.nodes[0]; // root node
		while (true)
		{
			if (node.contents != -1)
				return (Leaf) node;
				
			plane = node.plane;
			d = Math3D.DotProduct(p, plane.normal) - plane.dist;
			if (d > 0)
				node = node.children[0];
			else
				node = node.children[1];
		}
		// never reached
	}


	static byte[] decompressed = new byte[Constants.MAX_MAP_LEAFS / 8];
	static byte[] model_visibility = new byte[Constants.MAX_MAP_VISIBILITY]; 

	/*
	===================
	Mod_DecompressVis
	===================
	*/
	static byte[] Mod_DecompressVis(byte[] in, int offset, Model model)
	{
		int c;
		byte[] out;
		int outp, inp;
		int row;

		row = (model.vis.numclusters+7)>>3;
		out = decompressed;
		outp = 0;
		inp = offset;

		if (in == null)
		{	// no vis info, so make all visible
			while (row != 0)
			{
				out[outp++] = (byte)0xFF;
				row--;
			}
			return decompressed;		
		}

		do
		{
			if (in[inp] != 0)
			{
				out[outp++] = in[inp++];
				continue;
			}
			c = in[inp + 1] & 0xFF;
			inp += 2;
			while (c != 0)
			{
				out[outp++] = 0;
				c--;
			}
		} while (outp < row);
		return decompressed;
	}

	/*
	==============
	Mod_ClusterPVS
	==============
	*/
	static byte[] Mod_ClusterPVS(int cluster, Model model)
	{
		if (cluster == -1 || model.vis == null)
			return mod_novis;
		//return Mod_DecompressVis( (byte *)model.vis + model.vis.bitofs[cluster][Defines.DVIS_PVS], model);
		return Mod_DecompressVis(model_visibility, model.vis.bitofs[cluster][Constants.DVIS_PVS], model);
	}


//	  ===============================================================================
  private static class ModelRequest {
    public boolean loaded;
    public ArrayList<AsyncCallback<Model>> callbacks = new ArrayList<AsyncCallback<Model>>();
    public Model model = new Model();
  }

  private static HashMap<String, ModelRequest> modelReqs = new HashMap<String, ModelRequest>();

  public static void clearModelCache() {
    modelReqs.clear();
  }

  /*
	================
	Mod_Modellist_f
	================
	*/
	static void Mod_Modellist_f()
	{
		int i;
		Model	mod;
		int total;

		total = 0;
		Window.Printf(Constants.PRINT_ALL,"Loaded models:\n");

//		for (i=0; i < mod_numknown ; i++) {
//			mod = mod_known[i];
//			if (mod.name.length() == 0)
//				continue;
//
//			VID.Printf (Defines.PRINT_ALL, "%8i : %s\n", new Vargs(2).add(mod.extradatasize).add(mod.name));
//			total += mod.extradatasize;
//		}

		for (String name : modelReqs.keySet()) {
		  ModelRequest req = modelReqs.get(name);
		  Window.Printf (Constants.PRINT_ALL, "%8i : %s\n", new Vargs(2).add(req.model.extradatasize).add(req.model.name));
		  total += req.model.extradatasize;
		}
		Window.Printf (Constants.PRINT_ALL, "Total resident: " + total +'\n');
	}

	/*
	===============
	Mod_Init
	===============
	*/
	static void Mod_Init()
	{
		// init mod_known
//		for (int i=0; i < MAX_MOD_KNOWN; i++) {
//			mod_known[i] = new model_t();
//		}
		Arrays.fill(mod_novis, (byte)0xff);
	}

	static ByteBuffer fileBuffer;

	/*
	==================
	Mod_ForName

	Loads in a model for the given name
	==================
	*/
	static void Mod_ForName(final String name, AsyncCallback<Model> callback)
	{
    int i;
	
		if (name == null || name.length() == 0)
			Com.Error(Constants.ERR_DROP, "Mod_ForName: NULL name");
		
		//
		// inline models are grabbed only from worldmodel
		//
		if (name.charAt(0) == '*')
		{
			i = Integer.parseInt(name.substring(1));
			if (i < 1 || GlState.r_worldmodel == null || i >= GlState.r_worldmodel.numsubmodels) {
	            Compatibility.printStackTrace(new Exception("inline model number issue; world model: " + world_model));
	            Com.Error (Constants.ERR_DROP, "bad inline model number " + i + "; worldmodel: " + GlState.r_worldmodel + " numsubmodels: " + GlState.r_worldmodel.numsubmodels);
			}
			callback.onSuccess(mod_inline[i]);
			return;
		}

		final ModelRequest req = modelReqs.get(name);
		if (req != null) {
		  if (req.loaded) {
		    callback.onSuccess(req.model);
		    return;
		  }
		  req.callbacks.add(callback);
		  return;
		}

		final ModelRequest modelReq = new ModelRequest();
		modelReq.model = new Model();
		modelReq.callbacks.add(callback);
		modelReqs.put(name, modelReq);

		//
		// load the file
		//
		final Model model = modelReq.model;
		ResourceLoader.loadResourceAsync(name, new ResourceLoader.Callback() {
      public void onSuccess(final ByteBuffer bb) {
//    	if (waitingForImages > 0) {
//    		gl.log ("still waiting for textures: "+waitingForImages);
//    		new Timer(){
//
//				@Override
//				public void run() {
//					onSuccess(response);
//				}}.schedule(5000);
//    		return;
//    	}

        model.name = name;
        modelReq.loaded = true;

        fileBuffer = bb;
        modfilelen = fileBuffer.limit();
        loadmodel = model;

        //
        // fill it in
        //
        bb.order(ByteOrder.LITTLE_ENDIAN);

        // call the apropriate loader
      
        bb.mark();
        int ident = bb.getInt();
        
        bb.reset();
        
        switch (ident)
        {
        case QuakeFiles.IDALIASHEADER:
          Mod_LoadAliasModel(model, bb);
          break;
        case QuakeFiles.IDSPRITEHEADER:
          Mod_LoadSpriteModel(model, bb);
          break;
        case QuakeFiles.IDBSPHEADER:
          Mod_LoadBrushModel(model, bb);
          break;
        default:
          Com.Error(Constants.ERR_DROP,"Mod_NumForName: unknown fileid for " + model);
          break;
        }

        Models.fileBuffer = null; // free it for garbage collection
        for (AsyncCallback<Model> callback : modelReq.callbacks) {
          callback.onSuccess(model);
        }
        modelReq.callbacks.clear();
      }
      
     
    });
	}

	/*
	===============================================================================

						BRUSHMODEL LOADING

	===============================================================================
	*/

	static ByteBuffer mod_base;


	/*
	=================
	Mod_LoadLighting
	=================
	*/
	static void Mod_LoadLighting(Lump l)
	{
		if (l.filelen == 0)
		{
			loadmodel.lightdata = null;
			return;
		}
		// memcpy (loadmodel.lightdata, mod_base + l.fileofs, l.filelen);
		loadmodel.lightdata = new byte[l.filelen];
//		System.arraycopy(mod_base, l.fileofs, loadmodel.lightdata, 0, l.filelen);
		mod_base.position(l.fileofs);
		mod_base.get(loadmodel.lightdata, 0, l.filelen);
	}


	/*
	=================
	Mod_LoadVisibility
	=================
	*/
	static void Mod_LoadVisibility(Lump l)
	{
		//int		i;

		if (l.filelen == 0)
		{
			loadmodel.vis = null;
			return;
		}
		
//		System.arraycopy(mod_base, l.fileofs, model_visibility, 0, l.filelen);
    mod_base.position(l.fileofs);
		mod_base.get(model_visibility, 0, l.filelen);
		
		ByteBuffer bb = ByteBuffer.wrap(model_visibility, 0, l.filelen);
		
		loadmodel.vis = new QuakeFiles.dvis_t(bb.order(ByteOrder.LITTLE_ENDIAN));
		
		/* done:
		memcpy (loadmodel.vis, mod_base + l.fileofs, l.filelen);

		loadmodel.vis.numclusters = LittleLong (loadmodel.vis.numclusters);
		for (i=0 ; i<loadmodel.vis.numclusters ; i++)
		{
			loadmodel.vis.bitofs[i][0] = LittleLong (loadmodel.vis.bitofs[i][0]);
			loadmodel.vis.bitofs[i][1] = LittleLong (loadmodel.vis.bitofs[i][1]);
		}
		*/ 
	}


	/*
	=================
	Mod_LoadVertexes
	=================
	*/
	static void Mod_LoadVertexes(Lump l)
	{
		Vertex[] vertexes;
		int i, count;

		if ( (l.filelen % Vertex.DISK_SIZE) != 0)
			Com.Error(Constants.ERR_DROP, "MOD_LoadBmodel: funny lump size in " + loadmodel.name);

		count = l.filelen / Vertex.DISK_SIZE;
		
		vertexes = new Vertex[count];

		loadmodel.vertexes = vertexes;
		loadmodel.numvertexes = count;

		ByteBuffer bb = mod_base;
		mod_base.position(l.fileofs);//ByteBuffer.wrap(mod_base, l.fileofs, l.filelen);
		bb.order(ByteOrder.LITTLE_ENDIAN);

		for ( i=0 ; i<count ; i++)
		{
			vertexes[i] = new Vertex(bb);
		}
	}

	/*
	=================
	RadiusFromBounds
	=================
	*/
	static float RadiusFromBounds(float[] mins, float[] maxs)
	{
		float[] corner = {0, 0, 0};

		for (int i=0 ; i<3 ; i++)
		{
			corner[i] = Math.abs(mins[i]) > Math.abs(maxs[i]) ? Math.abs(mins[i]) : Math.abs(maxs[i]);
		}
		return Math3D.VectorLength(corner);
	}


	/*
	=================
	Mod_LoadSubmodels
	=================
	*/
	static void Mod_LoadSubmodels(Lump l) {
	    
	    if ((l.filelen % QuakeFiles.dmodel_t.SIZE) != 0)
	        Com.Error(Constants.ERR_DROP, "MOD_LoadBmodel: funny lump size in "
	                + loadmodel.name);
	    
	    int i, j;
	    
	    int count = l.filelen / QuakeFiles.dmodel_t.SIZE;
	    // out = Hunk_Alloc ( count*sizeof(*out));
	    SubModel out;
	    SubModel[] outs = new SubModel[count];
	    for (i = 0; i < count; i++) {
	        outs[i] = new SubModel();
	    }
	    
	    loadmodel.submodels = outs;
	    loadmodel.numsubmodels = count;
	    
	    ByteBuffer bb = mod_base;mod_base.position(l.fileofs);//ByteBuffer.wrap(mod_base, l.fileofs, l.filelen);
	    bb.order(ByteOrder.LITTLE_ENDIAN);
	    
	    QuakeFiles.dmodel_t in;
	    
	    for (i = 0; i < count; i++) {
	        in = new QuakeFiles.dmodel_t(bb);
	        out = outs[i];
	        for (j = 0; j < 3; j++) { // spread the mins / maxs by a
	            // pixel
	            out.mins[j] = in.mins[j] - 1;
	            out.maxs[j] = in.maxs[j] + 1;
	            out.origin[j] = in.origin[j];
	        }
	        out.radius = RadiusFromBounds(out.mins, out.maxs);
	        out.headnode = in.headnode;
	        out.firstface = in.firstface;
	        out.numfaces = in.numfaces;
	    }
	}

	/*
	=================
	Mod_LoadEdges
	=================
	*/
	static void Mod_LoadEdges (Lump l)
	{
		Edge[] edges;
		int i, count;

		if ( (l.filelen % Edge.DISK_SIZE) != 0)
			Com.Error(Constants.ERR_DROP, "MOD_LoadBmodel: funny lump size in " + loadmodel.name);

		count = l.filelen / Edge.DISK_SIZE;
		// out = Hunk_Alloc ( (count + 1) * sizeof(*out));	
		edges = new Edge[count + 1];

		loadmodel.edges = edges;
		loadmodel.numedges = count;
		
		ByteBuffer bb = mod_base;mod_base.position(l.fileofs);//ByteBuffer.wrap(mod_base, l.fileofs, l.filelen);
		bb.order(ByteOrder.LITTLE_ENDIAN);

		for ( i=0 ; i<count ; i++)
		{
			edges[i] = new Edge(bb);
		}
	}

	/*
	=================
	Mod_LoadTexinfo
	=================
	*/
	static void Mod_LoadTexinfo(Lump l)
	{
		TextureInfo in;
		Texture[] out;
		Texture step;
		int i;
		int next;
		String name;

		if ((l.filelen % TextureInfo.SIZE) != 0)
			Com.Error (Constants.ERR_DROP, "MOD_LoadBmodel: funny lump size in " + loadmodel.name);

		int count = l.filelen / TextureInfo.SIZE;
		// out = Hunk_Alloc ( count*sizeof(*out));
		out = new Texture[count];
		for ( i=0 ; i<count ; i++) {
			out[i] = new Texture();
		}

		loadmodel.texinfo = out;
		loadmodel.numtexinfo = count;
		
		ByteBuffer bb = mod_base;mod_base.position(l.fileofs);//ByteBuffer.wrap(mod_base, l.fileofs, l.filelen);
		bb.order(ByteOrder.LITTLE_ENDIAN);

		for ( i=0 ; i<count ; i++) {
			
			in = new TextureInfo(bb);			
			out[i].vecs = in.vecs;
			out[i].flags = in.flags;
			next = in.nexttexinfo;
			if (next > 0)
				out[i].next = loadmodel.texinfo[next];
			else
				out[i].next = null;

			name = "textures/" +  in.texture + ".wal";

			out[i].image = Images.findTexture(name, QuakeImage.it_wall);
			if (out[i].image == null) {
				Window.Printf(Constants.PRINT_ALL, "Couldn't load " + name + '\n');
				out[i].image = GlState.r_notexture;
			}
		}

		// count animation frames
		for (i=0 ; i<count ; i++) {
			out[i].numframes = 1;
			for (step = out[i].next ; (step != null) && (step != out[i]) ; step=step.next)
				out[i].numframes++;
		}
	}

	/*
	=================
	Mod_LoadFaces
	=================
	*/
	static void Mod_LoadFaces(Lump l) {
	    
	    int i, surfnum;
	    int planenum, side;
	    int ti;
	    
	    if ((l.filelen % QuakeFiles.dface_t.SIZE) != 0)
	        Com.Error(Constants.ERR_DROP, "MOD_LoadBmodel: funny lump size in "
	                + loadmodel.name);
	    
	    int count = l.filelen / QuakeFiles.dface_t.SIZE;
	    // out = Hunk_Alloc ( count*sizeof(*out));
	    Surface[] outs = new Surface[count];
	    for (i = 0; i < count; i++) {
	        outs[i] = new Surface();
	    }
	    
	    loadmodel.surfaces = outs;
	    loadmodel.numsurfaces = count;
	    
	    ByteBuffer bb = mod_base;mod_base.position(l.fileofs);//ByteBuffer.wrap(mod_base, l.fileofs, l.filelen);
	    bb.order(ByteOrder.LITTLE_ENDIAN);
	    
	    GlState.currentmodel = loadmodel;
	    
	    loadmodel.GL_BeginBuildingLightmaps();
	    
	    QuakeFiles.dface_t in;
	    Surface out;
	    
	    for (surfnum = 0; surfnum < count; surfnum++) {
	        in = new QuakeFiles.dface_t(bb);
	        out = outs[surfnum];
	        out.firstedge = in.firstedge;
	        out.numedges = in.numedges;
	        out.flags = 0;
	        out.polys = null;
	        
	        planenum = in.planenum;
	        side = in.side;
	        if (side != 0)
	            out.flags |= Constants.SURF_PLANEBACK;
	        
	        out.plane = loadmodel.planes[planenum];
	        
	        ti = in.texinfo;
	        if (ti < 0 || ti >= loadmodel.numtexinfo)
	            Com.Error(Constants.ERR_DROP,
	            "MOD_LoadBmodel: bad texinfo number");
	        
	        out.texinfo = loadmodel.texinfo[ti];
	        
	        out.CalcSurfaceExtents();
	        
	        // lighting info
	        
	        for (i = 0; i < Constants.MAXLIGHTMAPS; i++)
	            out.styles[i] = in.styles[i];
	        
	        i = in.lightofs;
	        if (i == -1)
	            out.samples = null;
	        else {
	            ByteBuffer pointer = ByteBuffer.wrap(loadmodel.lightdata);
	            pointer.position(i);
	            pointer = pointer.slice();
	            pointer.mark();
	            out.samples = pointer; // subarray
	        }
	        
	        // set the drawing flags
	        
	        if ((out.texinfo.flags & Constants.SURF_WARP) != 0) {
	            out.flags |= Constants.SURF_DRAWTURB;
	            for (i = 0; i < 2; i++) {
	                out.extents[i] = 16384;
	                out.texturemins[i] = -8192;
	            }
	           out.GL_SubdivideSurface(); // cut up polygon for warps
	        }
	        
	        // create lightmaps and polygons
	        if ((out.texinfo.flags & (Constants.SURF_SKY | Constants.SURF_TRANS33
	                | Constants.SURF_TRANS66 | Constants.SURF_WARP)) == 0)
	            Surface.GL_CreateSurfaceLightmap(out);
	        
	        if ((out.texinfo.flags & Constants.SURF_WARP) == 0)
	            Surface.GL_BuildPolygonFromSurface(out);
	        
	    }
	    Surfaces.GL_EndBuildingLightmaps();
	}


	/*
	=================
	Mod_LoadNodes
	=================
	*/
	static void Mod_LoadNodes(Lump l)
	{
		int i, j, count, p;
		QuakeFiles.dnode_t in;
		Node[] out;

		if ((l.filelen % QuakeFiles.dnode_t.SIZE) != 0)
			Com.Error(Constants.ERR_DROP, "MOD_LoadBmodel: funny lump size in " + loadmodel.name);
		
		count = l.filelen / QuakeFiles.dnode_t.SIZE;
		// out = Hunk_Alloc ( count*sizeof(*out));	
		out = new Node[count];

		loadmodel.nodes = out;
		loadmodel.numnodes = count;
		
		ByteBuffer bb = mod_base;mod_base.position(l.fileofs);//ByteBuffer.wrap(mod_base, l.fileofs, l.filelen);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		
		// initialize the tree array
		for ( i=0 ; i<count ; i++) out[i] = new Node(); // do first before linking 

		// fill and link the nodes
		for ( i=0 ; i<count ; i++)
		{
			in = new QuakeFiles.dnode_t(bb);
			for (j=0 ; j<3 ; j++)
			{
				out[i].mins[j] = in.mins[j];
				out[i].maxs[j] = in.maxs[j];
			}
	
			p = in.planenum;
			out[i].plane = loadmodel.planes[p];

			out[i].firstsurface = in.firstface;
			out[i].numsurfaces = in.numfaces;
			out[i].contents = -1;	// differentiate from leafs

			for (j=0 ; j<2 ; j++)
			{
				p = in.children[j];
				if (p >= 0)
					out[i].children[j] = loadmodel.nodes[p];
				else
					out[i].children[j] = loadmodel.leafs[-1 - p]; // mleaf_t extends mnode_t
			}
		}
	
		Node.Mod_SetParent(loadmodel.nodes[0], null);	// sets nodes and leafs
	}

	/*
	=================
	Mod_LoadLeafs
	=================
	*/
	static void Mod_LoadLeafs(Lump l)
	{
		QuakeFiles.dleaf_t in;
		Leaf[] out;
		int i, j, count;

		if ((l.filelen % QuakeFiles.dleaf_t.SIZE) != 0)
			Com.Error (Constants.ERR_DROP, "MOD_LoadBmodel: funny lump size in " + loadmodel.name);

		count = l.filelen / QuakeFiles.dleaf_t.SIZE;
		// out = Hunk_Alloc ( count*sizeof(*out));
		out = new Leaf[count];	

		loadmodel.leafs = out;
		loadmodel.numleafs = count;
		
		ByteBuffer bb = mod_base;mod_base.position(l.fileofs);//ByteBuffer.wrap(mod_base, l.fileofs, l.filelen);
		bb.order(ByteOrder.LITTLE_ENDIAN);

		for ( i=0 ; i<count ; i++)
		{
			in = new QuakeFiles.dleaf_t(bb);
			out[i] = new Leaf();
			for (j=0 ; j<3 ; j++)
			{
				out[i].mins[j] = in.mins[j];
				out[i].maxs[j] = in.maxs[j];

			}

			out[i].contents = in.contents;
			out[i].cluster = in.cluster;
			out[i].area = in.area;

			out[i].setMarkSurface(in.firstleafface, loadmodel.marksurfaces);
			out[i].nummarksurfaces = in.numleaffaces;
		}	
	}


	/*
	=================
	Mod_LoadMarksurfaces
	=================
	*/
	static void Mod_LoadMarksurfaces(Lump l)
	{	
		int i, j, count;

		Surface[] out; 

		if ((l.filelen % Constants.SIZE_OF_SHORT) != 0)
			Com.Error(Constants.ERR_DROP, "MOD_LoadBmodel: funny lump size in " + loadmodel.name);
		count = l.filelen / Constants.SIZE_OF_SHORT;
		// out = Hunk_Alloc ( count*sizeof(*out));	
		out = new Surface[count];

		loadmodel.marksurfaces = out;
		loadmodel.nummarksurfaces = count;

		ByteBuffer bb = mod_base;mod_base.position(l.fileofs);//ByteBuffer.wrap(mod_base, l.fileofs, l.filelen);
		bb.order(ByteOrder.LITTLE_ENDIAN);

		for ( i=0 ; i<count ; i++)
		{
			j = bb.getShort();
			if (j < 0 ||  j >= loadmodel.numsurfaces)
				Com.Error(Constants.ERR_DROP, "Mod_ParseMarksurfaces: bad surface number");

			out[i] = loadmodel.surfaces[j];
		}
	}


	/*
	=================
	Mod_LoadSurfedges
	=================
	*/
	static void Mod_LoadSurfedges(Lump l)
	{	
		int i, count;
		int[] offsets;
	
		if ( (l.filelen % Constants.SIZE_OF_INT) != 0)
			Com.Error (Constants.ERR_DROP, "MOD_LoadBmodel: funny lump size in " + loadmodel.name);

		count = l.filelen / Constants.SIZE_OF_INT;
		if (count < 1 || count >= Constants.MAX_MAP_SURFEDGES)
			Com.Error (Constants.ERR_DROP, "MOD_LoadBmodel: bad surfedges count in " + loadmodel.name + ": " + count);

		offsets = new int[count];

		loadmodel.surfedges = offsets;
		loadmodel.numsurfedges = count;

		ByteBuffer bb = mod_base;mod_base.position(l.fileofs);//ByteBuffer.wrap(mod_base, l.fileofs, l.filelen);
		bb.order(ByteOrder.LITTLE_ENDIAN);

		for ( i=0 ; i<count ; i++) offsets[i] = bb.getInt();
	}


	/*
	=================
	Mod_LoadPlanes
	=================
	*/
	static void Mod_LoadPlanes(Lump l)
	{
		int i, j;
		Plane[] out;
		QuakeFiles.dplane_t in;
		int count;
		int bits;

		if ((l.filelen % QuakeFiles.dplane_t.SIZE) != 0)
			Com.Error(Constants.ERR_DROP, "MOD_LoadBmodel: funny lump size in " + loadmodel.name);

		count = l.filelen / QuakeFiles.dplane_t.SIZE;
		// out = Hunk_Alloc ( count*2*sizeof(*out));
		out = new Plane[count * 2];
		for (i = 0; i < count; i++) {
		    out[i] = new Plane();
		}
	
		loadmodel.planes = out;
		loadmodel.numplanes = count;
		
		ByteBuffer bb = mod_base;mod_base.position(l.fileofs);//ByteBuffer.wrap(mod_base, l.fileofs, l.filelen);
		bb.order(ByteOrder.LITTLE_ENDIAN);

		for ( i=0 ; i<count ; i++)
		{
			bits = 0;
			in = new QuakeFiles.dplane_t(bb);
			for (j=0 ; j<3 ; j++)
			{
				out[i].normal[j] = in.normal[j];
				if (out[i].normal[j] < 0)
					bits |= (1<<j);
			}

			out[i].dist = in.dist;
			out[i].type = (byte)in.type;
			out[i].signbits = (byte)bits;
		}
	}

	/*
	=================
	Mod_LoadBrushModel
	=================
	*/
	static void Mod_LoadBrushModel(Model mod, ByteBuffer buffer)
	{
		int i;
		QuakeFiles.dheader_t	header;
		SubModel bm;
	
		loadmodel.type = GlConstants.mod_brush;
		world_model = loadmodel;

		header = new QuakeFiles.dheader_t(buffer);

		i = header.version;
		if (i != Constants.BSPVERSION)
			Com.Error (Constants.ERR_DROP, "Mod_LoadBrushModel: " + mod.name + " has wrong version number (" + i + " should be " + Constants.BSPVERSION + ")");

		mod_base = fileBuffer; //(byte *)header;
		mod_base.order(ByteOrder.LITTLE_ENDIAN);

		// load into heap
		Mod_LoadVertexes(header.lumps[Constants.LUMP_VERTEXES]); // ok
		Mod_LoadEdges(header.lumps[Constants.LUMP_EDGES]); // ok
		Mod_LoadSurfedges(header.lumps[Constants.LUMP_SURFEDGES]); // ok
		Mod_LoadLighting(header.lumps[Constants.LUMP_LIGHTING]); // ok
		Mod_LoadPlanes(header.lumps[Constants.LUMP_PLANES]); // ok
		Mod_LoadTexinfo(header.lumps[Constants.LUMP_TEXINFO]); // ok
		Mod_LoadFaces(header.lumps[Constants.LUMP_FACES]); // ok
		Mod_LoadMarksurfaces(header.lumps[Constants.LUMP_LEAFFACES]);
		Mod_LoadVisibility(header.lumps[Constants.LUMP_VISIBILITY]); // ok
		Mod_LoadLeafs(header.lumps[Constants.LUMP_LEAFS]); // ok
		Mod_LoadNodes(header.lumps[Constants.LUMP_NODES]); // ok
		Mod_LoadSubmodels(header.lumps[Constants.LUMP_MODELS]);
		mod.numframes = 2;		// regular and alternate animation
	
		//
		// set up the submodels
		//
		Model	starmod;

		for (i=0 ; i<mod.numsubmodels ; i++)
		{

			bm = mod.submodels[i];
			starmod = mod_inline[i] = loadmodel.copy();

			starmod.firstmodelsurface = bm.firstface;
			starmod.nummodelsurfaces = bm.numfaces;
			starmod.firstnode = bm.headnode;
			if (starmod.firstnode >= loadmodel.numnodes)
				Com.Error(Constants.ERR_DROP, "Inline model " + i + " has bad firstnode");

			Math3D.VectorCopy(bm.maxs, starmod.maxs);
			Math3D.VectorCopy(bm.mins, starmod.mins);
			starmod.radius = bm.radius;
	
			if (i == 0)
				loadmodel = starmod.copy();

			starmod.numleafs = bm.visleafs;
		}
		
        Surfaces.staticBufferId = GlState.gl.generateStaticBufferId();
	}

	/*
	==============================================================================

	ALIAS MODELS

	==============================================================================
	*/

	/*
	=================
	Mod_LoadAliasModel
	=================
	*/
	static void Mod_LoadAliasModel (Model mod, ByteBuffer buffer)
	{
		QuakeFiles.dmdl_t pheader;
		QuakeFiles.dstvert_t[] poutst;
		QuakeFiles.dtriangle_t[] pouttri;
		QuakeFiles.daliasframe_t[] poutframe;
		int[] poutcmd;

		pheader = new QuakeFiles.dmdl_t(buffer);

		if (pheader.version != QuakeFiles.ALIAS_VERSION)
			Com.Error(Constants.ERR_DROP, "%s has wrong version number (%i should be %i)",
					 new Vargs(3).add(mod.name).add(pheader.version).add(QuakeFiles.ALIAS_VERSION));

		if (pheader.skinheight > GlConstants.MAX_LBM_HEIGHT)
			Com.Error(Constants.ERR_DROP, "model "+ mod.name +" has a skin taller than " + GlConstants.MAX_LBM_HEIGHT);

		if (pheader.num_xyz <= 0)
			Com.Error(Constants.ERR_DROP, "model " + mod.name + " has no vertices");

		if (pheader.num_xyz > QuakeFiles.MAX_VERTS)
			Com.Error(Constants.ERR_DROP, "model " + mod.name +" has too many vertices");

		if (pheader.num_st <= 0)
			Com.Error(Constants.ERR_DROP, "model " + mod.name + " has no st vertices");

		if (pheader.num_tris <= 0)
			Com.Error(Constants.ERR_DROP, "model " + mod.name + " has no triangles");

		if (pheader.num_frames <= 0)
			Com.Error(Constants.ERR_DROP, "model " + mod.name + " has no frames");

		//
		// load base s and t vertices (not used in gl version)
		//
		poutst = new QuakeFiles.dstvert_t[pheader.num_st]; 
		buffer.position(pheader.ofs_st);
		for (int i=0 ; i<pheader.num_st ; i++)
		{
			poutst[i] = new QuakeFiles.dstvert_t(buffer);
		} 

		//
		//	   load triangle lists
		//
		pouttri = new QuakeFiles.dtriangle_t[pheader.num_tris];
		buffer.position(pheader.ofs_tris);
		for (int i=0 ; i<pheader.num_tris ; i++)
		{
			pouttri[i] = new QuakeFiles.dtriangle_t(buffer);
		}

		//
		//	   load the frames
		//
		poutframe = new QuakeFiles.daliasframe_t[pheader.num_frames];
		buffer.position(pheader.ofs_frames);
		for (int i=0 ; i<pheader.num_frames ; i++)
		{
			poutframe[i] = new QuakeFiles.daliasframe_t(buffer);
			// verts are all 8 bit, so no swapping needed
			poutframe[i].verts = new int[pheader.num_xyz];
			for (int k=0; k < pheader.num_xyz; k++) {
				poutframe[i].verts[k] = buffer.getInt();	
			}
		}

		mod.type = GlConstants.mod_alias;

		//
		// load the glcmds
		//
		poutcmd = new int[pheader.num_glcmds];
		buffer.position(pheader.ofs_glcmds);
		for (int i=0 ; i<pheader.num_glcmds ; i++)
			poutcmd[i] = buffer.getInt(); // LittleLong (pincmd[i]);

		// register all skins
		String[] skinNames = new String[pheader.num_skins];
		byte[] nameBuf = new byte[QuakeFiles.MAX_SKINNAME];
		buffer.position(pheader.ofs_skins);
		for (int i=0 ; i<pheader.num_skins ; i++)
		{
			buffer.get(nameBuf);
			skinNames[i] = Compatibility.newString(nameBuf);
			int n = skinNames[i].indexOf('\0');
			if (n > -1) {
				skinNames[i] = skinNames[i].substring(0, n);
			}	
			mod.skins[i] = Images.findTexture(skinNames[i], QuakeImage.it_skin);
		}
		
		// set the model arrays
		pheader.skinNames = skinNames; // skin names
		pheader.stVerts = poutst; // textur koordinaten
		pheader.triAngles = pouttri; // dreiecke
		pheader.glCmds = poutcmd; // STRIP or FAN
		pheader.aliasFrames = poutframe; // frames mit vertex array
		
		mod.extradata = pheader;
			
		mod.mins[0] = -32;
		mod.mins[1] = -32;
		mod.mins[2] = -32;
		mod.maxs[0] = 32;
		mod.maxs[1] = 32;
		mod.maxs[2] = 32;
		
		precompileGLCmds(pheader);
	}

	/*
	==============================================================================

	SPRITE MODELS

	==============================================================================
	*/

	/*
	=================
	Mod_LoadSpriteModel
	=================
	*/
	static void Mod_LoadSpriteModel(Model mod, ByteBuffer buffer)
	{
		QuakeFiles.dsprite_t sprout = new QuakeFiles.dsprite_t(buffer);
		
		if (sprout.version != QuakeFiles.SPRITE_VERSION)
			Com.Error(Constants.ERR_DROP, "%s has wrong version number (%i should be %i)",
				new Vargs(3).add(mod.name).add(sprout.version).add(QuakeFiles.SPRITE_VERSION));

		if (sprout.numframes > QuakeFiles.MAX_MD2SKINS)
			Com.Error(Constants.ERR_DROP, "%s has too many frames (%i > %i)",
				new Vargs(3).add(mod.name).add(sprout.numframes).add(QuakeFiles.MAX_MD2SKINS));

		for (int i=0 ; i<sprout.numframes ; i++)
		{
			mod.skins[i] = Images.findTexture(sprout.frames[i].name,	QuakeImage.it_sprite);
		}

		mod.type = GlConstants.mod_sprite;
		mod.extradata = sprout;
	}

//	  =============================================================================

	/*
	@@@@@@@@@@@@@@@@@@@@@
	R_BeginRegistration

	Specifies the model that will be used as the world
	@@@@@@@@@@@@@@@@@@@@@
	*/
	static void R_BeginRegistration(String model, final Command callback)
	{
		resetModelArrays();
		Polygons.reset();

		ConsoleVariable flushmap;

		GlState.registration_sequence++;
		GlState.r_oldviewcluster = -1;		// force markleafs

		final String fullname = "maps/" + model + ".bsp";

		// explicitly free the old map if different
		// this guarantees that mod_known[0] is the world map
		flushmap = ConsoleVariables.Get("flushmap", "0", 0);
		if ((world_model != null) && !world_model.name.equals(fullname) || flushmap.value != 0.0f) {
			world_model.Mod_Free();
			
			Com.Println("setting world_model to null");
			world_model = null;
		}

		Mod_ForName(fullname, new AsyncCallback<Model>() {
      public void onSuccess(Model response) {
        // TODO(jgw): Not sure if this is creating a race condition.
        Com.Println("setting world_model to " + response);
        GlState.r_worldmodel = response;
        callback.execute();
      }

      public void onFailure(Throwable e) {
        Com.Error(Constants.ERR_DROP, "Mod_NumForName: " + fullname + " not found");
      }
    });

		GlState.r_viewcluster = -1;
	}


	/*
	@@@@@@@@@@@@@@@@@@@@@
	R_RegisterModel
	@@@@@@@@@@@@@@@@@@@@@
	*/
	static void R_RegisterModel(String name, final AsyncCallback<Model> callback) {
		Mod_ForName(name, new AsyncCallback<Model>() {
      public void onSuccess(Model mod) {
        int   i;
        QuakeFiles.dmdl_t pheader;
        QuakeFiles.dsprite_t sprout;

        mod.registration_sequence = GlState.registration_sequence;

        // register any images used by the models
        if (mod.type == GlConstants.mod_sprite)
        {
          sprout = (QuakeFiles.dsprite_t)mod.extradata;
          for (i=0 ; i<sprout.numframes ; i++)
            mod.skins[i] = Images.findTexture(sprout.frames[i].name, QuakeImage.it_sprite);
        }
        else if (mod.type == GlConstants.mod_alias)
        {
          pheader = (QuakeFiles.dmdl_t)mod.extradata;
          for (i=0 ; i<pheader.num_skins ; i++)
            mod.skins[i] = Images.findTexture(pheader.skinNames[i], QuakeImage.it_skin);
          // PGM
          mod.numframes = pheader.num_frames;
          // PGM
        }
        else if (mod.type == GlConstants.mod_brush)
        {
          for (i=0 ; i<mod.numtexinfo ; i++)
            mod.texinfo[i].image.registration_sequence = GlState.registration_sequence;
        }

        if (callback != null) {
          callback.onSuccess(mod);
        }
      }

      public void onFailure(Throwable e) {
        if (callback != null) {
          callback.onFailure(e);
        }
      }
    });
	}

	/*
	@@@@@@@@@@@@@@@@@@@@@
	R_EndRegistration
	@@@@@@@@@@@@@@@@@@@@@
	*/
	static void R_EndRegistration()
	{
		Model	mod;

//		for (int i=0; i<mod_numknown ; i++) {
//			mod = mod_known[i];
//			if (mod.name.length() == 0)
//				continue;
//			if (mod.registration_sequence != registration_sequence)
//			{	// don't need this model
//				Mod_Free(mod);
//			} else {
//				// precompile AliasModels
//				if (mod.type == mod_alias)
//					precompileGLCmds((qfiles.dmdl_t)mod.extradata);
//			}
//		}

		for (String name : modelReqs.keySet()) {
		  ModelRequest req = modelReqs.get(name);

		  mod = req.model;
      if (mod.name.length() == 0)
        continue;
      if (mod.registration_sequence != GlState.registration_sequence)
      { // don't need this model
        mod.Mod_Free();
      } else {
        // precompile AliasModels
        if (mod.type == GlConstants.mod_alias)
          precompileGLCmds((QuakeFiles.dmdl_t)mod.extradata);
      }
		}
		Images.GL_FreeUnusedImages();
		//modelMemoryUsage();
	}


//	  =============================================================================


	/*
	================
	Mod_FreeAll
	================
	*/
	static void Mod_FreeAll() {
//		for (int i=0 ; i<mod_numknown ; i++) {
//			if (mod_known[i].extradata != null)
//				Mod_Free(mod_known[i]);
//		}

    for (String name : modelReqs.keySet()) {
      ModelRequest req = modelReqs.get(name);
      if (req.model.extradata != null)
        req.model.Mod_Free();
    }
	}

	/*
	 * new functions for vertex array handling
	 */
	static final int MODEL_BUFFER_SIZE = 50000;
	static FloatBuffer globalModelTextureCoordBuf; 
	static ShortBuffer globalModelVertexIndexBuf; 
	
	static  void init() {
		globalModelTextureCoordBuf = Lib.newFloatBuffer(MODEL_BUFFER_SIZE * 2);
		globalModelVertexIndexBuf = Lib.newShortBuffer(MODEL_BUFFER_SIZE);
	}
	
	
	
	static void precompileGLCmds(QuakeFiles.dmdl_t model) {
		model.textureCoordBuf = globalModelTextureCoordBuf.slice();
		model.vertexIndexBuf = globalModelVertexIndexBuf.slice();
		Vector<Integer> tmp = new Vector<Integer>();
			
		int count = 0;
		int[] order = model.glCmds;
		int orderIndex = 0;
		while (true)
		{
			// get the vertex count and primitive type
			count = order[orderIndex++];
			if (count == 0)
				break;		// done

			tmp.addElement(new Integer(count));
				
			if (count < 0)
			{
				count = -count;
				//gl.glBegin (GL11.GL_TRIANGLE_FAN);
			}
			else
			{
				//gl.glBegin (GL11.GL_TRIANGLE_STRIP);
			}

			do {
				// texture coordinates come from the draw list
				globalModelTextureCoordBuf.put(Compatibility.intBitsToFloat(order[orderIndex + 0]));
				globalModelTextureCoordBuf.put(Compatibility.intBitsToFloat(order[orderIndex + 1]));
				globalModelVertexIndexBuf.put((short) order[orderIndex + 2]);

				orderIndex += 3;
			} while (--count != 0);
		}
			
		int size = tmp.size();
			
		model.counts = new int[size];
		model.indexElements = new ShortBuffer[size];
			
		count = 0;
		int pos = 0;
		for (int i = 0; i < model.counts.length; i++) {
			count = ((Integer)tmp.get(i)).intValue();
			model.counts[i] = count;
				
			count = (count < 0) ? -count : count;
			model.vertexIndexBuf.position(pos);
			model.indexElements[i] = model.vertexIndexBuf.slice();
			model.indexElements[i].limit(count);
			pos += count;
		}
		
		model.staticTextureBufId = GlState.gl.generateStaticBufferId();
	}
		
	static void resetModelArrays() {
		globalModelTextureCoordBuf.rewind();
		globalModelVertexIndexBuf.rewind();
	}
		
	static void modelMemoryUsage() {
		System.out.println("AliasModels: globalVertexBuffer size " + globalModelVertexIndexBuf.position());
	}
}

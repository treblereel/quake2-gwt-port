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

import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.render.*;


public class ClientInfo {
	String	name	="";
	String	cinfo	="";
	Image skin;	// ptr
	Image icon;	// ptr
	String iconname	="";
	Model model;	// ptr
	Model weaponmodel[] = new Model[Constants.MAX_CLIENTWEAPONMODELS]; // arary of references
	
//	public void reset()
//	{
//		set(new clientinfo_t());
//	}
	
	public void set (ClientInfo from)
	{
		name = from.name;
		cinfo = from.cinfo;
		skin = from.skin;
		icon = from.icon;
		iconname = from.iconname;
		model = from.model;
		System.arraycopy(from.weaponmodel,0, weaponmodel, 0 , Constants.MAX_CLIENTWEAPONMODELS);
	}
}

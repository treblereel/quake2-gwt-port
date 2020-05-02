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
package com.googlecode.gwtquake.shared.server;

import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.game.Entity;
import com.googlecode.gwtquake.shared.game.ListNode;

public class AreaNode {
	int axis; // -1 = leaf node
	float dist;
	AreaNode children[] = new AreaNode[2];
	ListNode trigger_edicts = new ListNode(this);
	ListNode solid_edicts = new ListNode(this);
	
	// used for debugging
	float mins_rst[] = {0,0,0};
	float maxs_rst[] = {0,0,0};
  /*
   * ==================== SV_AreaEdicts_r
   * 
   * ====================
   */
  public static void SV_AreaEdicts_r(AreaNode node) {
      ListNode l, next, start;
      Entity check;
      int count;
      count = 0;
      // touch linked edicts
      if (World.area_type == Constants.AREA_SOLID)
          start = node.solid_edicts;
      else
          start = node.trigger_edicts;
      for (l = start.next; l != start; l = next) {
          next = l.next;
          check = (Entity) l.o;
          if (check.solid == Constants.SOLID_NOT)
              continue; // deactivated
          if (check.absmin[0] > World.area_maxs[0]
                  || check.absmin[1] > World.area_maxs[1]
                  || check.absmin[2] > World.area_maxs[2]
                  || check.absmax[0] < World.area_mins[0]
                  || check.absmax[1] < World.area_mins[1]
                  || check.absmax[2] < World.area_mins[2])
              continue; // not touching
          if (World.area_count == World.area_maxcount) {
              Com.Printf("SV_AreaEdicts: MAXCOUNT\n");
              return;
          }
          World.area_list[World.area_count] = check;
          World.area_count++;
      }
      if (node.axis == -1)
          return; // terminal node
      // recurse down both sides
      if (World.area_maxs[node.axis] > node.dist)
          SV_AreaEdicts_r(node.children[0]);
      if (World.area_mins[node.axis] < node.dist)
          SV_AreaEdicts_r(node.children[1]);
  }
}

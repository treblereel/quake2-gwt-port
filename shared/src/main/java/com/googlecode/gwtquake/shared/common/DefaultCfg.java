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
package com.googlecode.gwtquake.shared.common;

public class DefaultCfg {

	/* 
	 * This contains the original default.cfg content. If you prefer a different
	 * config, please hack it into reconfigure, so it is easily trackable. Or
	 * implement saving to a cookie and use the in-game menu :)
	 */
	public static final String DEFAULT_CFG =
    "unbindall\n" +
    "bind ' \"inven_drop\"\n" +
    "bind 1 \"use Blaster\"\n" +
    "bind 2 \"use Shotgun\"\n" +
    "bind 3 \"use Super Shotgun\"\n" +
    "bind 4 \"use Machinegun\"\n" +
    "bind 5 \"use Chaingun\"\n" +
    "bind 6 \"use Grenade Launcher\"\n" +
    "bind 7 \"use Rocket Launcher\"\n" +
    "bind 8 \"use HyperBlaster\"\n" +
    "bind 9 \"use Railgun\"\n" +
    "bind 0 \"use BFG10K\"\n" +
    
    "bind CTRL   +attack\n" +
    "bind ALT    +strafe\n" +
    
    "bind , +moveleft\n" +
    "bind . +moveright\n" +
    "bind DEL +lookdown\n" +
    "bind PGDN +lookup\n" +
    "bind END centerview\n" +
    "bind z +lookdown\n" + 
    "bind a +lookup\n" +
    "bind c +movedown\n" +
    
    "bind TAB inven\n" +
    "bind ENTER  invuse\n" +
    "bind [ invprev\n" +
    "bind ] invnext\n" +
    "bind ' invdrop\n" +
    "bind BACKSPACE   invdrop \n" +
    
    "bind /     weapnext\n" +
    
    "bind g use grenades\n" +
    "bind s use silencer\n" +
    "bind q use quad damage\n" +
    "bind b use rebreather\n" +
    "bind e use environment suit\n" +
    "bind i use invulnerability\n" +
    "bind p use power shield\n" +
    "bind x score\n" +
    
    "bind h \"wave 0\"\n" +
    "bind j \"wave 1\"\n" +
    "bind k \"wave 2\"\n" +
    "bind l \"wave 3\"\n" +
    "bind u \"wave 4\"\n" +
    
    "bind SHIFT +speed\n" +
    
    "bind UPARROW +forward\n" +
    "bind DOWNARROW +back\n" +
    "bind LEFTARROW +left\n" +
    "bind RIGHTARROW +right\n" +
    
    "bind SPACE +moveup \n" +

    "bind \\ +mlook\n" +
    
    "bind PAUSE \"pause\"\n" +
    "bind ESCAPE \"togglemenu\"\n" +
    "bind ~ \"toggleconsole\"\n" +
    "bind ` \"toggleconsole\"\n" +
    
    "bind F1 \"cmd help\"\n" +
    "bind F2 \"menu_savegame\"\n" +
    "bind F3 \"menu_loadgame\"\n" +
    "bind F4 \"menu_keys\"\n" +
    "bind F5 \"menu_startserver\"\n" +
    "bind F6 \"echo Quick Saving...; wait; save quick\"\n" +
    "bind F9 \"echo Quick Loading...; wait; load quick\"\n" +
    "bind F10 \"menu_quit\"\n" +
    "bind F12 \"screenshot\"\n" +
    
    "bind t \"messagemode\"\n" +
    
    "bind + \"sizeup\"\n" +
    "bind = \"sizeup\"\n" +
    "bind - \"sizedown\"\n" +
    
    "bind INS +klook\n" +
    
    "bind MOUSE3 +forward\n" +
    "bind MOUSE1 +attack\n" +
    "bind MOUSE2 +strafe\n" +
    
    "set viewsize 100\n" +
    "set vid_fullscreen 1\n" +
    "set win_noalttab 0\n" +
    "set sensitivity 3\n" +
    "set crosshair 1\n" +
    "set cl_run 0\n" +
    "set hand 0\n" +
    "set m_pitch 0.022\n" +
    "set m_yaw 0.022\n" +
    "set m_forward 1\n" +
    "set m_side 0.8\n" +
    "set lookspring 1\n" +
    "set lookstrafe 0\n" +
    
    "set name Player\n" +
    "set skin male/grunt\n" +
    "alias d1 \"killserver; demomap q2demo1.dm2 ; set nextserver d1\"\n" +
//    "alias newgame \" wait ; killserver ; maxclients 1 ; deathmatch 0 ; map demo1\"\n" +
    "alias newgame \" killserver ; maxclients 1 ; deathmatch 0 ; map *ntro.cin+demo1\"\n" +
    "alias dedicated_start \"map demo1\"\n";

}

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
package com.googlecode.gwtquake.shared.sys;

import com.googlecode.gwtquake.shared.client.ClientInput;
import com.googlecode.gwtquake.shared.client.Keys;
import com.googlecode.gwtquake.shared.common.ConsoleVariables;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.ExecutableCommand;
import com.googlecode.gwtquake.shared.common.Globals;
import com.googlecode.gwtquake.shared.game.Commands;
import com.googlecode.gwtquake.shared.game.UserCommand;
import com.googlecode.gwtquake.shared.util.Math3D;


/**
 * IN
 */
public final class IN {

    static boolean mouse_avail = true;
    public static boolean mouse_active = false;
    static boolean ignorefirst = false;
    static int mouse_buttonstate;
    static int mouse_oldbuttonstate;
    static int old_mouse_x;
    static int old_mouse_y;
    static boolean mlooking;

    public static void ActivateMouse() {
        if (!mouse_avail)
            return;
        if (!mouse_active) {
            KBD.mx = KBD.my = 0; // don't spazz
            install_grabs();
            mouse_active = true;
        }
    }

    public static void DeactivateMouse() {
        // if (!mouse_avail || c == null) return;
        if (mouse_active) {
            uninstall_grabs();
            mouse_active = false;
        }
    }

    private static void install_grabs() {
		Globals.re.getKeyboardHandler().installGrabs();
		ignorefirst = true;
    }

    private static void uninstall_grabs() {
		Globals.re.getKeyboardHandler().uninstallGrabs();
    }

    public static void toggleMouse() {
        if (mouse_avail) {
            mouse_avail = false;
            DeactivateMouse();
        } else {
            mouse_avail = true;
            ActivateMouse();
        }
    }

    public static void Init() {
        Globals.in_mouse = ConsoleVariables.Get("in_mouse", "1", Constants.CVAR_ARCHIVE);
        Globals.in_joystick = ConsoleVariables.Get("in_joystick", "0", Constants.CVAR_ARCHIVE);
    }

    public static void Shutdown() {
        mouse_avail = false;
    }

    public static void Real_IN_Init() {
        // mouse variables
        Globals.m_filter = ConsoleVariables.Get("m_filter", "0", 0);
        Globals.in_mouse = ConsoleVariables.Get("in_mouse", "1", Constants.CVAR_ARCHIVE);
        Globals.freelook = ConsoleVariables.Get("freelook", "1", 0);
        Globals.lookstrafe = ConsoleVariables.Get("lookstrafe", "0", 0);
        Globals.sensitivity = ConsoleVariables.Get("sensitivity", "3", 0);
        Globals.m_pitch = ConsoleVariables.Get("m_pitch", "0.022", 0);
        Globals.m_yaw = ConsoleVariables.Get("m_yaw", "0.022", 0);
        Globals.m_forward = ConsoleVariables.Get("m_forward", "1", 0);
        Globals.m_side = ConsoleVariables.Get("m_side", "0.8", 0);

        Commands.addCommand("+mlook", new ExecutableCommand() {
            public void execute() {
                MLookDown();
            }
        });
        Commands.addCommand("-mlook", new ExecutableCommand() {
            public void execute() {
                MLookUp();
            }
        });

        Commands.addCommand("force_centerview", new ExecutableCommand() {
            public void execute() {
                Force_CenterView_f();
            }
        });

        Commands.addCommand("togglemouse", new ExecutableCommand() {
            public void execute() {
                toggleMouse();
            }
        });

        IN.mouse_avail = true;
    }

    public static void Commands() {
		int i;
	
		if (!IN.mouse_avail) 
			return;
	
		KBD kbd=Globals.re.getKeyboardHandler();
		for (i=0 ; i<3 ; i++) {
			if ( (IN.mouse_buttonstate & (1<<i)) != 0 && (IN.mouse_oldbuttonstate & (1<<i)) == 0 )
				kbd.Do_Key_Event(Keys.K_MOUSE1 + i, true);
	
			if ( (IN.mouse_buttonstate & (1<<i)) == 0 && (IN.mouse_oldbuttonstate & (1<<i)) != 0 )
				kbd.Do_Key_Event(Keys.K_MOUSE1 + i, false);
		}
		IN.mouse_oldbuttonstate = IN.mouse_buttonstate;		
    }

    public static void Frame() {

        if (!Globals.cl.refresh_prepped || Globals.cls.key_dest == Constants.key_console
                || Globals.cls.key_dest == Constants.key_menu)
            DeactivateMouse();
        else
            ActivateMouse();
    }

    public static void CenterView() {
        Globals.cl.viewangles[Constants.PITCH] = -Math3D
                .SHORT2ANGLE(Globals.cl.frame.playerstate.pmove.delta_angles[Constants.PITCH]);
    }

    public static void Move(UserCommand cmd) {
        if (!IN.mouse_avail)
            return;

        if (Globals.m_filter.value != 0.0f) {
            KBD.mx = (KBD.mx + IN.old_mouse_x) / 2;
            KBD.my = (KBD.my + IN.old_mouse_y) / 2;
        }

        IN.old_mouse_x = KBD.mx;
        IN.old_mouse_y = KBD.my;

        KBD.mx = (int) (KBD.mx * Globals.sensitivity.value);
        KBD.my = (int) (KBD.my * Globals.sensitivity.value);

        // add mouse X/Y movement to cmd
        if ((ClientInput.in_strafe.state & 1) != 0
                || ((Globals.lookstrafe.value != 0) && IN.mlooking)) {
            cmd.sidemove += Globals.m_side.value * KBD.mx;
        } else {
            Globals.cl.viewangles[Constants.YAW] -= Globals.m_yaw.value * KBD.mx;
        }

        if ((IN.mlooking || Globals.freelook.value != 0.0f)
                && (ClientInput.in_strafe.state & 1) == 0) {
            Globals.cl.viewangles[Constants.PITCH] += Globals.m_pitch.value * KBD.my;
        } else {
            cmd.forwardmove -= Globals.m_forward.value * KBD.my;
        }
        //KBD.mx = KBD.my = 0; //TODO BUG
        KBD.mx = 0;
        KBD.my = 0;
    }

    static void MLookDown() {
        mlooking = true;
    }

    static void MLookUp() {
        mlooking = false;
        CenterView();
    }

    static void Force_CenterView_f() {
        Globals.cl.viewangles[Constants.PITCH] = 0;
    }
}

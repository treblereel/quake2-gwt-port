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


import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Vector;

import com.googlecode.gwtquake.shared.game.Commands;
import com.googlecode.gwtquake.shared.game.ConsoleVariable;
import com.googlecode.gwtquake.shared.game.Info;
import com.googlecode.gwtquake.shared.util.Lib;
import elemental2.dom.DomGlobal;

/**
 * Cvar implements console variables. The original code is located in cvar.c
 */
public class ConsoleVariables {

    /**
     * @param var_name
     * @param var_value
     * @param flags
     * @return
     */
    public static ConsoleVariable Get(String var_name, String var_value, int flags) {
        ConsoleVariable var;

        if ((flags & (Constants.CVAR_USERINFO | Constants.CVAR_SERVERINFO)) != 0) {
            if (!InfoValidate(var_name)) {
                Com.Printf("invalid info cvar name\n");
                return null;
            }
        }

        var = ConsoleVariables.FindVar(var_name);
        if (var != null) {
            var.flags |= flags;
            return var;
        }

        if (var_value == null)
            return null;

        if ((flags & (Constants.CVAR_USERINFO | Constants.CVAR_SERVERINFO)) != 0) {
            if (!InfoValidate(var_value)) {
                Com.Printf("invalid info cvar value\n");
                return null;
            }
        }
        var = new ConsoleVariable();
        var.name = new String(var_name);
        var.string = new String(var_value);
        var.modified = true;
        // handles atof(var.string)
        try {
            var.value = Float.parseFloat(var.string);
        } catch (NumberFormatException e) {
            var.value = 0.0f;
        }
        // link the variable in
        var.next = Globals.cvar_vars;
        Globals.cvar_vars = var;

        var.flags = flags;

        return var;
    }

    static void Init() {
        Commands.addCommand("set", Set_f);
        Commands.addCommand("cvarlist", List_f);
    }

    public static String VariableString(String var_name) {
        ConsoleVariable var;
        var = FindVar(var_name);
        return (var == null) ? "" : var.string;
    }

    static ConsoleVariable FindVar(String var_name) {
        ConsoleVariable var;

        for (var = Globals.cvar_vars; var != null; var = var.next) {
            if (var_name.equals(var.name))
                return var;
        }

        return null;
    }

    /**
     * Creates a variable if not found and sets their value, the parsed float value and their flags.
     */
    public static ConsoleVariable FullSet(String var_name, String value, int flags) {
        ConsoleVariable var;

        var = ConsoleVariables.FindVar(var_name);
        if (null == var) { // create it
            return ConsoleVariables.Get(var_name, value, flags);
        }

        var.modified = true;

        if ((var.flags & Constants.CVAR_USERINFO) != 0)
            Globals.userinfo_modified = true; // transmit at next oportunity

        var.string = value;
        try {
            var.value = Float.parseFloat(var.string);
        } catch (Exception e) {
            var.value = 0.0f;
        }

        var.flags = flags;

        return var;
    }

    /** 
     * Sets the value of the variable without forcing. 
     */
    public static ConsoleVariable Set(String var_name, String value) {
        return Set2(var_name, value, false);
    }

    /** 
     * Sets the value of the variable with forcing. 
     */
    public static ConsoleVariable ForceSet(String var_name, String value) {
        return ConsoleVariables.Set2(var_name, value, true);
    }
    
    /**
     * Gereric set function, sets the value of the variable, with forcing its even possible to 
     * override the variables write protection. 
     */
    static ConsoleVariable Set2(String var_name, String value, boolean force) {

        ConsoleVariable var = ConsoleVariables.FindVar(var_name);
        if (var == null) { 
        	// create it
            return ConsoleVariables.Get(var_name, value, 0);
        }

        if ((var.flags & (Constants.CVAR_USERINFO | Constants.CVAR_SERVERINFO)) != 0) {
            if (!InfoValidate(value)) {
                Com.Printf("invalid info cvar value\n");
                return var;
            }
        }

        if (!force) {
            if ((var.flags & Constants.CVAR_NOSET) != 0) {
                Com.Printf(var_name + " is write protected.\n");
                return var;
            }

            if ((var.flags & Constants.CVAR_LATCH) != 0) {
                if (var.latched_string != null) {
                    if (value.equals(var.latched_string))
                        return var;
                    var.latched_string = null;
                } else {
                    if (value.equals(var.string))
                        return var;
                }

                if (Globals.server_state != 0) {
                    Com.Printf(var_name + " will be changed for next game.\n");
                    var.latched_string = value;
                } else {
                    var.string = value;
                    try {
                        var.value = Float.parseFloat(var.string);
                    } catch (Exception e) {
                        var.value = 0.0f;
                    }
                    if (var.name.equals("game")) {
                        QuakeFileSystem.SetGamedir(var.string);
                        QuakeFileSystem.ExecAutoexec();
                    }
                }
                return var;
            }
        } else {
            if (var.latched_string != null) {
                var.latched_string = null;
            }
        }

        if (value.equals(var.string))
            return var; // not changed

        var.modified = true;

        if ((var.flags & Constants.CVAR_USERINFO) != 0)
            Globals.userinfo_modified = true; // transmit at next oportunity

        var.string = value;
        try {
            var.value = Float.parseFloat(var.string);
        } catch (Exception e) {
            var.value = 0.0f;
        }

        return var;
    }

    /** 
     * Set command, sets variables.
     */
    
    static ExecutableCommand Set_f = new ExecutableCommand() {
        public void execute() {
            int c;
            int flags;

            c = Commands.Argc();
            if (c != 3 && c != 4) {
                Com.Printf("usage: set <variable> <value> [u / s]\n");
                return;
            }

            if (c == 4) {
                if (Commands.Argv(3).equals("u"))
                    flags = Constants.CVAR_USERINFO;
                else if (Commands.Argv(3).equals("s"))
                    flags = Constants.CVAR_SERVERINFO;
                else {
                    Com.Printf("flags can only be 'u' or 's'\n");
                    return;
                }
                ConsoleVariables.FullSet(Commands.Argv(1), Commands.Argv(2), flags);
            } else
                ConsoleVariables.Set(Commands.Argv(1), Commands.Argv(2));

        }

    };

    /**
     * List command, lists all available commands.
     */
    static ExecutableCommand List_f = new ExecutableCommand() {
        public void execute() {
            ConsoleVariable var;
            int i;

            i = 0;
            for (var = Globals.cvar_vars; var != null; var = var.next, i++) {
                if ((var.flags & Constants.CVAR_ARCHIVE) != 0)
                    Com.Printf("*");
                else
                    Com.Printf(" ");
                if ((var.flags & Constants.CVAR_USERINFO) != 0)
                    Com.Printf("U");
                else
                    Com.Printf(" ");
                if ((var.flags & Constants.CVAR_SERVERINFO) != 0)
                    Com.Printf("S");
                else
                    Com.Printf(" ");
                if ((var.flags & Constants.CVAR_NOSET) != 0)
                    Com.Printf("-");
                else if ((var.flags & Constants.CVAR_LATCH) != 0)
                    Com.Printf("L");
                else
                    Com.Printf(" ");
                Com.Printf(" " + var.name + " \"" + var.string + "\"\n");
            }
            Com.Printf(i + " cvars\n");
        }
    };



    /** 
     * Sets a float value of a variable.
     * 
     * The overloading is very important, there was a problem with 
     * networt "rate" string --> 10000 became "10000.0" and that wasn't right.
     */
    public static void SetValue(String var_name, int value) {
        ConsoleVariables.Set(var_name, "" + value);
    }

    public static void SetValue(String var_name, float value) {
        if (value == (int)value) {
            ConsoleVariables.Set(var_name, "" + (int)value);
        } else {
            ConsoleVariables.Set(var_name, "" + value);
        }
    }

    /**
     * Returns the float value of a variable.
     */
    public static float VariableValue(String var_name) {
        ConsoleVariable var = ConsoleVariables.FindVar(var_name);
        if (var == null)
            return 0;
        float val = 0.0f;
        try {
            val = Float.parseFloat(var.string);
        } catch (Exception e) {
        }
        return val;
    }

    /**
     * Handles variable inspection and changing from the console.
     */
    public static boolean Command() {
        ConsoleVariable v;

        // check variables
        v = ConsoleVariables.FindVar(Commands.Argv(0));
        if (v == null)
            return false;

        // perform a variable print or set
        if (Commands.Argc() == 1) {
            Com.Printf("\"" + v.name + "\" is \"" + v.string + "\"\n");
            return true;
        }

        ConsoleVariables.Set(v.name, Commands.Argv(1));
        return true;
    }

    public static String BitInfo(int bit) {
        String info;
        ConsoleVariable var;

        info = "";

        for (var = Globals.cvar_vars; var != null; var = var.next) {
            if ((var.flags & bit) != 0)
                info = Info.Info_SetValueForKey(info, var.name, var.string);
        }
        return info;
    }

    /**
     * Returns an info string containing all the CVAR_SERVERINFO cvars. 
     */
    public static String Serverinfo() {
        return BitInfo(Constants.CVAR_SERVERINFO);
    }

    
    /**
     * Any variables with latched values will be updated.
     */
    public static void GetLatchedVars() {
        ConsoleVariable var;

        for (var = Globals.cvar_vars; var != null; var = var.next) {
            if (var.latched_string == null || var.latched_string.length() == 0)
                continue;
            var.string = var.latched_string;
            var.latched_string = null;
            try {
                var.value = Float.parseFloat(var.string);
            } catch (NumberFormatException e) {
                var.value = 0.0f;
            }
            if (var.name.equals("game")) {
                QuakeFileSystem.SetGamedir(var.string);
                QuakeFileSystem.ExecAutoexec();
            }
        }
    }

    /**
     * Returns an info string containing all the CVAR_USERINFO cvars.
     */
    public static String Userinfo() {
        return BitInfo(Constants.CVAR_USERINFO);
    }
    
    /**
     * Appends lines containing \"set vaqriable value\" for all variables
     * with the archive flag set true. 
     */

    public static void WriteVariables(String path) {
        ConsoleVariable var;
        RandomAccessFile f;
        String buffer;

        f = Lib.fopen(path, "rw");
        if (f == null)
            return;

        try {
            f.seek(f.length());
        } catch (IOException e1) {
            Lib.fclose(f);
            return;
        }
        for (var = Globals.cvar_vars; var != null; var = var.next) {
            if ((var.flags & Constants.CVAR_ARCHIVE) != 0) {
                buffer = "set " + var.name + " \"" + var.string + "\"\n";
                try {
                    f.writeBytes(buffer);
                } catch (IOException e) {
                }
            }
        }
        Lib.fclose(f);
    }

    /**
     * Variable typing auto completition.
     */
    public static Vector CompleteVariable(String partial) {

        Vector vars = new Vector();

        // check match
        for (ConsoleVariable cvar = Globals.cvar_vars; cvar != null; cvar = cvar.next)
            if (cvar.name.startsWith(partial))
                vars.add(cvar.name);

        return vars;
    }

    /**
     * Some characters are invalid for info strings.
     */
    static boolean InfoValidate(String s) {
        if (s.indexOf("\\") != -1)
            return false;
        if (s.indexOf("\"") != -1)
            return false;
        if (s.indexOf(";") != -1)
            return false;
        return true;
    }
}

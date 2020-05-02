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

import java.nio.ByteOrder;

import com.googlecode.gwtquake.shared.common.*;
import com.googlecode.gwtquake.shared.game.Commands;
import com.googlecode.gwtquake.shared.game.ConsoleVariable;
import com.googlecode.gwtquake.shared.game.UserCommand;
import com.googlecode.gwtquake.shared.sys.IN;
import com.googlecode.gwtquake.shared.util.Lib;
import com.googlecode.gwtquake.shared.util.Math3D;


/**
 * Original name: CL_input
 */
public class ClientInput {

	static long frameMsec;
	static long oldSysFrameTime;
	static ConsoleVariable cl_nodelta;

	/*
	 * ===============================================================================
	 * 
	 * KEY BUTTONS
	 * 
	 * Continuous button event tracking is complicated by the fact that two
	 * different input sources (say, mouse button 1 and the control key) can
	 * both press the same button, but the button should only be released when
	 * both of the pressing key have been released.
	 * 
	 * When a key event issues a button command (+forward, +attack, etc), it
	 * appends its key number as a parameter to the command so it can be matched
	 * up with the release.
	 * 
	 * state bit 0 is the current state of the key state bit 1 is edge triggered
	 * on the up to down transition state bit 2 is edge triggered on the down to
	 * up transition
	 * 
	 * 
	 * Key_Event (int key, qboolean down, unsigned time);
	 * 
	 * +mlook src time
	 * 
	 * ===============================================================================
	 */

	static ButtonState in_klook = new ButtonState();

	static ButtonState in_left = new ButtonState();

	static ButtonState in_right = new ButtonState();

	static ButtonState in_forward = new ButtonState();

	static ButtonState in_back = new ButtonState();

	static ButtonState in_lookup = new ButtonState();

	static ButtonState in_lookdown = new ButtonState();

	static ButtonState in_moveleft = new ButtonState();

	static ButtonState in_moveright = new ButtonState();

	public static ButtonState in_strafe = new ButtonState();

	static ButtonState in_speed = new ButtonState();

	static ButtonState in_use = new ButtonState();

	static ButtonState in_attack = new ButtonState();

	static ButtonState in_up = new ButtonState();

	static ButtonState in_down = new ButtonState();

	static int in_impulse;

	static void KeyDown(ButtonState b) {
		int k;
		String c;

		c = Commands.Argv(1);
		if (c.length() > 0)
			k = Lib.atoi(c);
		else
			k = -1; // typed manually at the console for continuous down

		if (k == b.down[0] || k == b.down[1])
			return; // repeating key

		if (b.down[0] == 0)
			b.down[0] = k;
		else if (b.down[1] == 0)
			b.down[1] = k;
		else {
			Com.Printf("Three keys down for a button!\n");
			return;
		}

		if ((b.state & 1) != 0)
			return; // still down

		// save timestamp
		c = Commands.Argv(2);
		b.downtime = Lib.atoi(c);
		if (b.downtime == 0)
			b.downtime = Globals.sys_frame_time - 100;

		b.state |= 3; // down + impulse down
	}

	static void KeyUp(ButtonState b) {
		int k;
		String c;
		int uptime;

		c = Commands.Argv(1);
		if (c.length() > 0)
			k = Lib.atoi(c);
		else {
			// typed manually at the console, assume for unsticking, so clear
			// all
			b.down[0] = b.down[1] = 0;
			b.state = 4; // impulse up
			return;
		}

		if (b.down[0] == k)
			b.down[0] = 0;
		else if (b.down[1] == k)
			b.down[1] = 0;
		else
			return; // key up without coresponding down (menu pass through)
		if (b.down[0] != 0 || b.down[1] != 0)
			return; // some other key is still holding it down

		if ((b.state & 1) == 0)
			return; // still up (this should not happen)

		// save timestamp
		c = Commands.Argv(2);
		uptime = Lib.atoi(c);
		if (uptime != 0)
			b.msec += uptime - b.downtime;
		else
			b.msec += 10;

		b.state &= ~1; // now up
		b.state |= 4; // impulse up
	}

	static void IN_KLookDown() {
		KeyDown(in_klook);
	}

	static void IN_KLookUp() {
		KeyUp(in_klook);
	}

	static void IN_UpDown() {
		KeyDown(in_up);
	}

	static void IN_UpUp() {
		KeyUp(in_up);
	}

	static void IN_DownDown() {
		KeyDown(in_down);
	}

	static void IN_DownUp() {
		KeyUp(in_down);
	}

	static void IN_LeftDown() {
		KeyDown(in_left);
	}

	static void IN_LeftUp() {
		KeyUp(in_left);
	}

	static void IN_RightDown() {
		KeyDown(in_right);
	}

	static void IN_RightUp() {
		KeyUp(in_right);
	}

	static void IN_ForwardDown() {
		KeyDown(in_forward);
	}

	static void IN_ForwardUp() {
		KeyUp(in_forward);
	}

	static void IN_BackDown() {
		KeyDown(in_back);
	}

	static void IN_BackUp() {
		KeyUp(in_back);
	}

	static void IN_LookupDown() {
		KeyDown(in_lookup);
	}

	static void IN_LookupUp() {
		KeyUp(in_lookup);
	}

	static void IN_LookdownDown() {
		KeyDown(in_lookdown);
	}

	static void IN_LookdownUp() {
		KeyUp(in_lookdown);
	}

	static void IN_MoveleftDown() {
		KeyDown(in_moveleft);
	}

	static void IN_MoveleftUp() {
		KeyUp(in_moveleft);
	}

	static void IN_MoverightDown() {
		KeyDown(in_moveright);
	}

	static void IN_MoverightUp() {
		KeyUp(in_moveright);
	}

	static void IN_SpeedDown() {
		KeyDown(in_speed);
	}

	static void IN_SpeedUp() {
		KeyUp(in_speed);
	}

	static void IN_StrafeDown() {
		KeyDown(in_strafe);
	}

	static void IN_StrafeUp() {
		KeyUp(in_strafe);
	}

	static void IN_AttackDown() {
		KeyDown(in_attack);
	}

	static void IN_AttackUp() {
		KeyUp(in_attack);
	}

	static void IN_UseDown() {
		KeyDown(in_use);
	}

	static void IN_UseUp() {
		KeyUp(in_use);
	}

	static void IN_Impulse() {
		in_impulse = Lib.atoi(Commands.Argv(1));
	}

	/*
	 * =============== CL_KeyState
	 * 
	 * Returns the fraction of the frame that the key was down ===============
	 */
	static float KeyState(ButtonState key) {
		float val;
		long msec;

		key.state &= 1; // clear impulses

		msec = key.msec;
		key.msec = 0;

		if (key.state != 0) {
			// still down
			msec += Globals.sys_frame_time - key.downtime;
			key.downtime = Globals.sys_frame_time;
		}

		val = (float) msec / frameMsec;
		if (val < 0)
			val = 0;
		if (val > 1)
			val = 1;

		return val;
	}

	//	  ==========================================================================

	/*
	 * ================ CL_AdjustAngles
	 * 
	 * Moves the local angle positions ================
	 */
	static void AdjustAngles() {
		float speed;
		float up, down;

		if ((in_speed.state & 1) != 0)
			speed = Globals.cls.frametime * Globals.cl_anglespeedkey.value;
		else
			speed = Globals.cls.frametime;

		if ((in_strafe.state & 1) == 0) {
			Globals.cl.viewangles[Constants.YAW] -= speed * Globals.cl_yawspeed.value * KeyState(in_right);
			Globals.cl.viewangles[Constants.YAW] += speed * Globals.cl_yawspeed.value * KeyState(in_left);
		}
		if ((in_klook.state & 1) != 0) {
			Globals.cl.viewangles[Constants.PITCH] -= speed * Globals.cl_pitchspeed.value * KeyState(in_forward);
			Globals.cl.viewangles[Constants.PITCH] += speed * Globals.cl_pitchspeed.value * KeyState(in_back);
		}

		up = KeyState(in_lookup);
		down = KeyState(in_lookdown);

		Globals.cl.viewangles[Constants.PITCH] -= speed * Globals.cl_pitchspeed.value * up;
		Globals.cl.viewangles[Constants.PITCH] += speed * Globals.cl_pitchspeed.value * down;
	}

	/*
	 * ================ CL_BaseMove
	 * 
	 * Send the intended movement message to the server ================
	 */
	static void BaseMove(UserCommand cmd) {
		AdjustAngles();

		//memset (cmd, 0, sizeof(*cmd));
		cmd.clear();

		Math3D.VectorCopy(Globals.cl.viewangles, cmd.angles);
		if ((in_strafe.state & 1) != 0) {
			cmd.sidemove += Globals.cl_sidespeed.value * KeyState(in_right);
			cmd.sidemove -= Globals.cl_sidespeed.value * KeyState(in_left);
		}

		cmd.sidemove += Globals.cl_sidespeed.value * KeyState(in_moveright);
		cmd.sidemove -= Globals.cl_sidespeed.value * KeyState(in_moveleft);

		cmd.upmove += Globals.cl_upspeed.value * KeyState(in_up);
		cmd.upmove -= Globals.cl_upspeed.value * KeyState(in_down);

		if ((in_klook.state & 1) == 0) {
			cmd.forwardmove += Globals.cl_forwardspeed.value * KeyState(in_forward);
			cmd.forwardmove -= Globals.cl_forwardspeed.value * KeyState(in_back);
		}

		//
		//	   adjust for speed key / running
		//
		if (((in_speed.state & 1) ^ (int) (Globals.cl_run.value)) != 0) {
			cmd.forwardmove *= 2;
			cmd.sidemove *= 2;
			cmd.upmove *= 2;
		}

	}

	static void ClampPitch() {

		float pitch;

		pitch = Math3D.SHORT2ANGLE(Globals.cl.frame.playerstate.pmove.delta_angles[Constants.PITCH]);
		if (pitch > 180)
			pitch -= 360;

		if (Globals.cl.viewangles[Constants.PITCH] + pitch < -360)
			Globals.cl.viewangles[Constants.PITCH] += 360; // wrapped
		if (Globals.cl.viewangles[Constants.PITCH] + pitch > 360)
			Globals.cl.viewangles[Constants.PITCH] -= 360; // wrapped

		if (Globals.cl.viewangles[Constants.PITCH] + pitch > 89)
			Globals.cl.viewangles[Constants.PITCH] = 89 - pitch;
		if (Globals.cl.viewangles[Constants.PITCH] + pitch < -89)
			Globals.cl.viewangles[Constants.PITCH] = -89 - pitch;
	}

	/*
	 * ============== CL_FinishMove ==============
	 */
	static void FinishMove(UserCommand cmd) {
		int ms;
		int i;

		//
		//	   figure button bits
		//	
		if ((in_attack.state & 3) != 0)
			cmd.buttons |= Constants.BUTTON_ATTACK;
		in_attack.state &= ~2;

		if ((in_use.state & 3) != 0)
			cmd.buttons |= Constants.BUTTON_USE;
		in_use.state &= ~2;

		if (Key.anykeydown != 0 && Globals.cls.key_dest == Constants.key_game)
			cmd.buttons |= Constants.BUTTON_ANY;

		// send milliseconds of time to apply the move
		ms = (int) (Globals.cls.frametime * 1000);
		if (ms > 250)
			ms = 100; // time was unreasonable
		cmd.msec = (byte) ms;

		ClampPitch();
		for (i = 0; i < 3; i++)
			cmd.angles[i] = (short) Math3D.ANGLE2SHORT(Globals.cl.viewangles[i]);

		cmd.impulse = (byte) in_impulse;
		in_impulse = 0;

		// send the ambient light level at the player's current position
		cmd.lightlevel = (byte) Globals.cl_lightlevel.value;
	}

	/*
	 * ================= CL_CreateCmd =================
	 */
	static void CreateCmd(UserCommand cmd) {
		//usercmd_t cmd = new usercmd_t();

		frameMsec = Globals.sys_frame_time - oldSysFrameTime;
		if (frameMsec < 1)
			frameMsec = 1;
		if (frameMsec > 200)
			frameMsec = 200;

		// get basic movement from keyboard
		BaseMove(cmd);

		// allow mice or other external controllers to add to the move
		IN.Move(cmd);

		FinishMove(cmd);

		oldSysFrameTime = Globals.sys_frame_time;

		//return cmd;
	}

	/*
	 * ============ CL_InitInput ============
	 */
	static void InitInput() {
		Commands.addCommand("centerview", new ExecutableCommand() {
			public void execute() {
				IN.CenterView();
			}
		});

		Commands.addCommand("+moveup", new ExecutableCommand() {
			public void execute() {
				IN_UpDown();
			}
		});
		Commands.addCommand("-moveup", new ExecutableCommand() {
			public void execute() {
				IN_UpUp();
			}
		});
		Commands.addCommand("+movedown", new ExecutableCommand() {
			public void execute() {
				IN_DownDown();
			}
		});
		Commands.addCommand("-movedown", new ExecutableCommand() {
			public void execute() {
				IN_DownUp();
			}
		});
		Commands.addCommand("+left", new ExecutableCommand() {
			public void execute() {
				IN_LeftDown();
			}
		});
		Commands.addCommand("-left", new ExecutableCommand() {
			public void execute() {
				IN_LeftUp();
			}
		});
		Commands.addCommand("+right", new ExecutableCommand() {
			public void execute() {
				IN_RightDown();
			}
		});
		Commands.addCommand("-right", new ExecutableCommand() {
			public void execute() {
				IN_RightUp();
			}
		});
		Commands.addCommand("+forward", new ExecutableCommand() {
			public void execute() {
				IN_ForwardDown();
			}
		});
		Commands.addCommand("-forward", new ExecutableCommand() {
			public void execute() {
				IN_ForwardUp();
			}
		});
		Commands.addCommand("+back", new ExecutableCommand() {
			public void execute() {
				IN_BackDown();
			}
		});
		Commands.addCommand("-back", new ExecutableCommand() {
			public void execute() {
				IN_BackUp();
			}
		});
		Commands.addCommand("+lookup", new ExecutableCommand() {
			public void execute() {
				IN_LookupDown();
			}
		});
		Commands.addCommand("-lookup", new ExecutableCommand() {
			public void execute() {
				IN_LookupUp();
			}
		});
		Commands.addCommand("+lookdown", new ExecutableCommand() {
			public void execute() {
				IN_LookdownDown();
			}
		});
		Commands.addCommand("-lookdown", new ExecutableCommand() {
			public void execute() {
				IN_LookdownUp();
			}
		});
		Commands.addCommand("+strafe", new ExecutableCommand() {
			public void execute() {
				IN_StrafeDown();
			}
		});
		Commands.addCommand("-strafe", new ExecutableCommand() {
			public void execute() {
				IN_StrafeUp();
			}
		});
		Commands.addCommand("+moveleft", new ExecutableCommand() {
			public void execute() {
				IN_MoveleftDown();
			}
		});
		Commands.addCommand("-moveleft", new ExecutableCommand() {
			public void execute() {
				IN_MoveleftUp();
			}
		});
		Commands.addCommand("+moveright", new ExecutableCommand() {
			public void execute() {
				IN_MoverightDown();
			}
		});
		Commands.addCommand("-moveright", new ExecutableCommand() {
			public void execute() {
				IN_MoverightUp();
			}
		});
		Commands.addCommand("+speed", new ExecutableCommand() {
			public void execute() {
				IN_SpeedDown();
			}
		});
		Commands.addCommand("-speed", new ExecutableCommand() {
			public void execute() {
				IN_SpeedUp();
			}
		});
		Commands.addCommand("+attack", new ExecutableCommand() {
			public void execute() {
				IN_AttackDown();
			}
		});
		Commands.addCommand("-attack", new ExecutableCommand() {
			public void execute() {
				IN_AttackUp();
			}
		});
		Commands.addCommand("+use", new ExecutableCommand() {
			public void execute() {
				IN_UseDown();
			}
		});
		Commands.addCommand("-use", new ExecutableCommand() {
			public void execute() {
				IN_UseUp();
			}
		});
		Commands.addCommand("impulse", new ExecutableCommand() {
			public void execute() {
				IN_Impulse();
			}
		});
		Commands.addCommand("+klook", new ExecutableCommand() {
			public void execute() {
				IN_KLookDown();
			}
		});
		Commands.addCommand("-klook", new ExecutableCommand() {
			public void execute() {
				IN_KLookUp();
			}
		});

		cl_nodelta = ConsoleVariables.Get("cl_nodelta", "0", 0);
	}

	private static final Buffer buf = Buffer.allocate(128);
	private static final UserCommand nullcmd = new UserCommand();
	/*
	 * ================= CL_SendCmd =================
	 */
	static void SendCmd() {
		int i;
		UserCommand cmd, oldcmd;
		int checksumIndex;

		// build a command even if not connected

		// save this command off for prediction
		i = Globals.cls.netchan.outgoing_sequence & (Constants.CMD_BACKUP - 1);
		cmd = Globals.cl.cmds[i];
		Globals.cl.cmd_time[i] = (int) Globals.cls.realtime; // for netgraph
															 // ping calculation

		// fill the cmd
		CreateCmd(cmd);

		Globals.cl.cmd.set(cmd);

		if (Globals.cls.state == Constants.ca_disconnected || Globals.cls.state == Constants.ca_connecting)
			return;

		if (Globals.cls.state == Constants.ca_connected) {
			if (Globals.cls.netchan.message.cursize != 0 || Globals.curtime - Globals.cls.netchan.last_sent > 1000)
				NetworkChannel.Transmit(Globals.cls.netchan, 0, new byte[0]);
			return;
		}

		// send a userinfo update if needed
		if (Globals.userinfo_modified) {
			Client.fixUpGender();
			Globals.userinfo_modified = false;
			Buffers.writeByte(Globals.cls.netchan.message, Constants.clc_userinfo);
			Buffers.WriteString(Globals.cls.netchan.message, ConsoleVariables.Userinfo());
		}

		buf.clear();
		buf.order(ByteOrder.LITTLE_ENDIAN);
//		Buffer.Init(buf, data, data.length);

		if (cmd.buttons != 0 && Globals.cl.cinematictime > 0 && !Globals.cl.attractloop
				&& Globals.cls.realtime - Globals.cl.cinematictime > 1000) { // skip
																			 // the
																			 // rest
																			 // of
																			 // the
																			 // cinematic
			Screen.FinishCinematic();
		}

		// begin a client move command
		Buffers.writeByte(buf, Constants.clc_move);

		// save the position for a checksum byte
		checksumIndex = buf.cursize;
		Buffers.writeByte(buf, 0);

		// let the server know what the last frame we
		// got was, so the next message can be delta compressed
		if (cl_nodelta.value != 0.0f || !Globals.cl.frame.valid || Globals.cls.demowaiting)
      buf.putInt(-1);
    else
      buf.putInt(Globals.cl.frame.serverframe);

		// send this and the previous cmds in the message, so
		// if the last packet was dropped, it can be recovered
		i = (Globals.cls.netchan.outgoing_sequence - 2) & (Constants.CMD_BACKUP - 1);
		cmd = Globals.cl.cmds[i];
		//memset (nullcmd, 0, sizeof(nullcmd));
		nullcmd.clear();

		Delta.WriteDeltaUsercmd(buf, nullcmd, cmd);
		oldcmd = cmd;

		i = (Globals.cls.netchan.outgoing_sequence - 1) & (Constants.CMD_BACKUP - 1);
		cmd = Globals.cl.cmds[i];

		Delta.WriteDeltaUsercmd(buf, oldcmd, cmd);
		oldcmd = cmd;

		i = (Globals.cls.netchan.outgoing_sequence) & (Constants.CMD_BACKUP - 1);
		cmd = Globals.cl.cmds[i];

		Delta.WriteDeltaUsercmd(buf, oldcmd, cmd);

		// calculate a checksum over the move commands
		buf.data[checksumIndex] = Com.BlockSequenceCRCByte(buf.data, checksumIndex + 1, buf.cursize - checksumIndex - 1,
				Globals.cls.netchan.outgoing_sequence);

		//
		// deliver the message
		//
		NetworkChannel.Transmit(Globals.cls.netchan, buf.cursize, buf.data);
	}
}

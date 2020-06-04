/*
 * Copyright (C) 1997-2001 Id Software, Inc.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *  
 */
/* Modifications
 Copyright 2003-2004 Bytonic Software
 Copyright 2010 Google Inc.
 */
package com.googlecode.gwtquake.shared.common;

import java.nio.ByteOrder;

import com.googlecode.gwtquake.shared.game.ConsoleVariable;
import com.googlecode.gwtquake.shared.sys.NET;
import com.googlecode.gwtquake.shared.sys.Timer;
import com.googlecode.gwtquake.shared.util.Lib;

/**
 * 
 * packet header ------------- 31 sequence 1 does this message contains a
 * reliable payload 31 acknowledge sequence 1 acknowledge receipt of even/odd
 * message 16 qport
 * 
 * The remote connection never knows if it missed a reliable message, the local
 * side detects that it has been dropped by seeing a sequence acknowledge higher
 * thatn the last reliable sequence, but without the correct evon/odd bit for
 * the reliable set.
 * 
 * If the sender notices that a reliable message has been dropped, it will be
 * retransmitted. It will not be retransmitted again until a message after the
 * retransmit has been acknowledged and the reliable still failed to get there.
 * 
 * if the sequence number is -1, the packet should be handled without a netcon
 * 
 * The reliable message can be added to at any time by doing MSG_Write*
 * (&netchan.message, <data>).
 * 
 * If the message buffer is overflowed, either by a single message, or by
 * multiple frames worth piling up while the last reliable transmit goes
 * unacknowledged, the netchan signals a fatal error.
 * 
 * Reliable messages are always placed first in a packet, then the unreliable
 * message is included if there is sufficient room.
 * 
 * To the receiver, there is no distinction between the reliable and unreliable
 * parts of the message, they are just processed out as a single larger message.
 * 
 * Illogical packet sequence numbers cause the packet to be dropped, but do not
 * kill the connection. This, combined with the tight window of valid reliable
 * acknowledgement numbers provides protection against malicious address
 * spoofing.
 * 
 * 
 * The qport field is a workaround for bad address translating routers that
 * sometimes remap the client's source port on a packet during gameplay.
 * 
 * If the base part of the net address matches and the qport matches, then the
 * channel matches even if the IP port differs. The IP port should be updated to
 * the new value before sending out any replies.
 * 
 * 
 * If there is no information that needs to be transfered on a given frame, such
 * as during the connection stage while waiting for the client to load, then a
 * packet only needs to be delivered if there is something in the unacknowledged
 * reliable
 */

public class NetworkChannel {

 // static final Buffer send = new Buffer();
  static final byte send_buf[] = new byte[Constants.MAX_MSGLEN];
  public static byte net_message_buffer[] = new byte[Constants.MAX_MSGLEN];

  // public static netadr_t net_from = new netadr_t();
  public static Buffer net_message = new Buffer();
  public static ConsoleVariable consoleQport;
  public static ConsoleVariable showdrop;
  public static ConsoleVariable showpackets;

  public boolean fatal_error;

  // was enum {NS_CLIENT, NS_SERVER}
  public int sock;
  public int dropped; // between last packet and previous
  public int last_received; // for timeouts
  public int last_sent; // for retransmits
  public NetworkAddress remote_address = new NetworkAddress();
  public int qport; // qport value to write when transmitting
  // sequencing variables
  public int incoming_sequence;
  public int incoming_acknowledged;
  public int incoming_reliable_acknowledged; // single bit
  public int incoming_reliable_sequence; // single bit, maintained local
  public int outgoing_sequence;
  public int reliable_sequence; // single bit
  public int last_reliable_sequence; // sequence number of last send
  // reliable staging and holding areas
  public Buffer message; // writing buffer to send to server
  // leave space for header
  public byte message_buf[] = new byte[Constants.MAX_MSGLEN - 16];
  // message is copied to this buffer when it is first transfered
  public int reliable_length;
  // unpcked reliable message
  public byte reliable_buf[] = new byte[Constants.MAX_MSGLEN - 16];

  // ok.
  public void clear() {
    sock = dropped = last_received = last_sent = 0;
    remote_address = new NetworkAddress();
    qport = incoming_sequence = incoming_acknowledged = incoming_reliable_acknowledged = incoming_reliable_sequence = outgoing_sequence = reliable_sequence = last_reliable_sequence = 0;
    message = new Buffer();

    message_buf = new byte[Constants.MAX_MSGLEN - 16];

    reliable_length = 0;
    reliable_buf = new byte[Constants.MAX_MSGLEN - 16];
  }

  public boolean Netchan_CanReliable() {
    if (reliable_length != 0)
      return false; // waiting for ack
    return true;
  }

  public boolean Netchan_NeedReliable() {
    boolean send_reliable;

    // if the remote side dropped the last reliable message, resend it
    send_reliable = false;

    if (incoming_acknowledged > last_reliable_sequence
        && incoming_reliable_acknowledged != reliable_sequence)
      send_reliable = true;

    // if the reliable transmit buffer is empty, copy the current message
    // out
    if (0 == reliable_length && message.cursize != 0) {
      send_reliable = true;
    }

    return send_reliable;
  }

  /**
   * called when the current net_message is from remote_address modifies
   * net_message so that it points to the packet payload =================
   */
  public static boolean Process(NetworkChannel chan, Buffer msg) {
    int sequence, sequence_ack;
    int reliable_ack, reliable_message;
    int qport;

    // get sequence numbers
    msg.reset();
    sequence = msg.getInt();
    sequence_ack = msg.getInt();

    // read the qport if we are a server
    if (chan.sock == Constants.NS_SERVER)
      qport = msg.getShort();

    // achtung unsigned int
    reliable_message = sequence >>> 31;
    reliable_ack = sequence_ack >>> 31;

    sequence &= ~(1 << 31);
    sequence_ack &= ~(1 << 31);

    if (showpackets.value != 0) {
      if (reliable_message != 0)
        Com.Printf(
        // "recv %4i : s=%i reliable=%i ack=%i rack=%i\n"
        "recv " + msg.cursize + " : s=" + sequence + " reliable="
            + (chan.incoming_reliable_sequence ^ 1) + " ack=" + sequence_ack
            + " rack=" + reliable_ack + "\n");
      else
        Com.Printf(
        // "recv %4i : s=%i ack=%i rack=%i\n"
        "recv " + msg.cursize + " : s=" + sequence + " ack=" + sequence_ack
            + " rack=" + reliable_ack + "\n");
    }

    //
    // discard stale or duplicated packets
    //
    if (sequence <= chan.incoming_sequence) {
      if (showdrop.value != 0)
        Com.Printf(NET.AdrToString(chan.remote_address)
            + ":Out of order packet " + sequence + " at "
            + chan.incoming_sequence + "\n");
      return false;
    }

    //
    // dropped packets don't keep the message from being used
    //
    chan.dropped = sequence - (chan.incoming_sequence + 1);
    if (chan.dropped > 0) {
      if (showdrop.value != 0)
        Com.Printf(NET.AdrToString(chan.remote_address) + ":Dropped "
            + chan.dropped + " packets at " + sequence + "\n");
    }

    //
    // if the current outgoing reliable message has been acknowledged
    // clear the buffer to make way for the next
    //
    if (reliable_ack == chan.reliable_sequence)
      chan.reliable_length = 0; // it has been received

    //
    // if this message contains a reliable message, bump
    // incoming_reliable_sequence
    //
    chan.incoming_sequence = sequence;
    chan.incoming_acknowledged = sequence_ack;
    chan.incoming_reliable_acknowledged = reliable_ack;
    if (reliable_message != 0) {
      chan.incoming_reliable_sequence ^= 1;
    }

    //
    // the message can now be read from the current message pointer
    //
    chan.last_received = (int) Globals.curtime;

    return true;
  }

  // private static final byte send_buf[] = new byte[Defines.MAX_MSGLEN];
  // private static final sizebuf_t send = new sizebuf_t();
  /**
   * tries to send an unreliable message to a connection, and handles the
   * transmition / retransmition of the reliable messages.
   * 
   * A 0 length will still generate a packet and deal with the reliable
   * messages.
   */
  public static void Transmit(NetworkChannel chan, int length, byte data[]) {
    int send_reliable;
    int w1, w2;

    // check for message overflow
    if (chan.message.overflowed) {
      chan.fatal_error = true;
      Com.Printf(NET.AdrToString(chan.remote_address)
          + ":Outgoing message overflow\n");
      return;
    }

    send_reliable = chan.Netchan_NeedReliable() ? 1 : 0;

    if (chan.reliable_length == 0 && chan.message.cursize != 0) {
      System.arraycopy(chan.message_buf, 0, chan.reliable_buf, 0,
          chan.message.cursize);
      chan.reliable_length = chan.message.cursize;
      chan.message.cursize = 0;
      chan.reliable_sequence ^= 1;
    }

    // write the packet header
    Buffer send = Buffer.wrap(send_buf).order(ByteOrder.LITTLE_ENDIAN);

    w1 = (chan.outgoing_sequence & ~(1 << 31)) | (send_reliable << 31);
    w2 = (chan.incoming_sequence & ~(1 << 31))
        | (chan.incoming_reliable_sequence << 31);

    chan.outgoing_sequence++;
    chan.last_sent = (int) Globals.curtime;

    send.putInt(w1);
    send.putInt(w2);

    // send the qport if we are a client
    if (chan.sock == Constants.NS_CLIENT)
      send.WriteShort((int) consoleQport.value);

    // copy the reliable message to the packet first
    if (send_reliable != 0) {
      Buffers.Write(send, chan.reliable_buf, chan.reliable_length);
      chan.last_reliable_sequence = chan.outgoing_sequence;
    }

    // add the unreliable part if space is available
    if (send.maxsize - send.cursize >= length)
      Buffers.Write(send, data, length);
    else
      Com.Printf("Netchan_Transmit: dumped unreliable\n");

    // send the datagram
    NET.SendPacket(chan.sock, send.cursize, send.data, chan.remote_address);

    if (showpackets.value != 0) {
      if (send_reliable != 0)
        Com.Printf(
        // "send %4i : s=%i reliable=%i ack=%i rack=%i\n"
        "send " + send.cursize + " : s=" + (chan.outgoing_sequence - 1)
            + " reliable=" + chan.reliable_sequence + " ack="
            + chan.incoming_sequence + " rack="
            + chan.incoming_reliable_sequence + "\n");
      else
        Com.Printf(
        // "send %4i : s=%i ack=%i rack=%i\n"
        "send " + send.cursize + " : s=" + (chan.outgoing_sequence - 1)
            + " ack=" + chan.incoming_sequence + " rack="
            + chan.incoming_reliable_sequence + "\n");
    }
  }

  /**
   * called to open a channel to a remote system
   */
  public static void Setup(NetworkChannel chan, int sock, NetworkAddress adr,
      int qport) {
    // memset (chan, 0, sizeof(*chan));

    chan.clear();
    chan.sock = sock;
    chan.remote_address.set(adr);
    chan.qport = qport;
    chan.last_received = Globals.curtime;
    chan.incoming_sequence = 0;
    chan.outgoing_sequence = 1;

    chan.message = Buffer.wrap(chan.message_buf).order(ByteOrder.LITTLE_ENDIAN);
    chan.message.allowoverflow = true;
  }

  public static void OutOfBandPrint(int net_socket, NetworkAddress adr, String s) {
    Netchan_OutOfBand(net_socket, adr, s.length(), Lib.stringToBytes(s));
  }

  /**
   * Sends an out-of-band datagram
   */
  public static void Netchan_OutOfBand(int net_socket, NetworkAddress adr,
      int length, byte data[]) {

    // write the packet header
    Buffer send = Buffer.allocate(Constants.MAX_MSGLEN);

    send.putInt(-1); // -1 sequence means out of band
    Buffers.Write(send, data, length);

    // send the datagram
    NET.SendPacket(net_socket, send.cursize, send.data, adr);
  }

  /*
   * =============== Netchan_Init
   * 
   * ===============
   */
  // ok.
  public static void Netchan_Init() {
    long port;

    // pick a port value that should be nice and random
    port = Timer.Milliseconds() & 0xffff;

    showpackets = ConsoleVariables.Get("showpackets", "0", 0);
    showdrop = ConsoleVariables.Get("showdrop", "0", 0);
    NetworkChannel.consoleQport = ConsoleVariables.Get("qport", "" + port,
        Constants.CVAR_NOSET);
  }

}

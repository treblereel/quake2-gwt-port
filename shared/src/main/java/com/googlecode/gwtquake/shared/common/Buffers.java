package com.googlecode.gwtquake.shared.common;

import com.googlecode.gwtquake.shared.util.Lib;
import com.googlecode.gwtquake.shared.util.Math3D;
import elemental2.dom.DomGlobal;

public class Buffers {

  // returns -1 if no more characters are available, but also [-128 , 127]
  public static int readSignedByte(Buffer msg_read) {
      int c;
  
      if (msg_read.readcount + 1 > msg_read.cursize)
          c = -1;
      else
          c = msg_read.data[msg_read.readcount];
      msg_read.readcount++;
      // kickangles bugfix (rst)
      return c;
  }

  public static int readUnsignedByte(Buffer msg_read) {
      int c;
  
      if (msg_read.readcount + 1 > msg_read.cursize)
          c = -1;
      else
          c = msg_read.data[msg_read.readcount] & 0xff;
      
      msg_read.readcount++;
  
      return c;
  }

  public static String getString(Buffer msg_read) {
  
      byte c;
      int l = 0;
      do {
          c = (byte) readUnsignedByte(msg_read);
          if (c == -1 || c == 0)
              break;
  
          Buffer.readbuf[l] = c;
          l++;
      } while (l < 2047);
      
      String ret = Compatibility.newString(Buffer.readbuf, 0, l);
      // Com.dprintln("MSG.ReadString:[" + ret + "]");
      return ret;
  }

  public static String getLine(Buffer msg_read) {
  
      int l;
      byte c;
  
      l = 0;
      do {
          c = (byte) readSignedByte(msg_read);
          if (c == -1 || c == 0 || c == 0x0a)
              break;
          Buffer.readbuf[l] = c;
          l++;
      } while (l < 2047);
      
      String ret = Compatibility.newString(Buffer.readbuf, 0, l).trim();
      Com.dprintln("MSG.ReadStringLine:[" + ret.replace('\0', '@') + "]");
      return ret;
  }

  public static float getCoord(Buffer msg_read) {
      return msg_read.getShort() * (1.0f / 8);
  }

  public static void getPos(Buffer msg_read, float pos[]) {
      assert (pos.length == 3) : "vec3_t bug";
      pos[0] = msg_read.getShort() * (1.0f / 8);
      pos[1] = msg_read.getShort() * (1.0f / 8);
      pos[2] = msg_read.getShort() * (1.0f / 8);
  }

  public static float getAngle(Buffer msg_read) {
      return readSignedByte(msg_read) * (360.0f / 256);
  }

  public static float ReadAngle16(Buffer msg_read) {
      return Math3D.SHORT2ANGLE(msg_read.getShort());
  }

  public static void WriteAngle16(Buffer sb, float f) {
      sb.WriteShort(Math3D.ANGLE2SHORT(f));
  }

  public static void ReadData(Buffer msg_read, byte data[], int len) {
      for (int i = 0; i < len; i++)
          data[i] = (byte) readUnsignedByte(msg_read);
  }

  //ok.
  public static void WriteStringTrimmed(Buffer sb, byte s[]) {
      Buffers.WriteString(sb, Compatibility.newString(s).trim());
  }

  public static void WritePos(Buffer sb, float[] pos) {
      assert (pos.length == 3) : "vec3_t bug";
      sb.WriteShort((int) (pos[0] * 8));
      sb.WriteShort((int) (pos[1] * 8));
      sb.WriteShort((int) (pos[2] * 8));
  }

  public static void WriteCoord(Buffer sb, float f) {
      sb.WriteShort((int) (f * 8));
  }

  public static void WriteAngle(Buffer sb, float f) {
      Buffers.writeByte(sb, (int) (f * 256 / 360) & 255);
  }

  // had a bug, now its ok.
  public static void WriteString(Buffer sb, String s) {
      String x = s;
  
      if (s == null)
          x = "";
  
      Buffers.Write(sb, Lib.stringToBytes(x));
      Buffers.writeByte(sb, 0);
      //Com.dprintln("MSG.WriteString:" + s.replace('\0', '@'));
  }

  public static void Write(Buffer buf, byte data[]) {
  	int length = data.length;
  	//memcpy(SZ_GetSpace(buf, length), data, length);
  	System.arraycopy(data, 0, buf.data, Buffer.GetSpace(buf, length), length);
  }

  public static void Write(Buffer buf, byte data[], int offset, int length) {
  	System.arraycopy(data, offset, buf.data, Buffer.GetSpace(buf, length), length);
  }

  public static void Write(Buffer buf, byte data[], int length) {
  	//memcpy(SZ_GetSpace(buf, length), data, length);
  	System.arraycopy(data, 0, buf.data, Buffer.GetSpace(buf, length), length);
  }

  // 
  public static void Print(Buffer buf, String data) {
      Com.dprintln("SZ.print():<" + data + ">" );
  	int length = data.length();
  	byte str[] = Lib.stringToBytes(data);
  
  	if (buf.cursize != 0) {
  
  		if (buf.data[buf.cursize - 1] != 0) {
  			//memcpy( SZ_GetSpace(buf, len), data, len); // no trailing 0
  			System.arraycopy(str, 0, buf.data, Buffer.GetSpace(buf, length+1), length);
  		} else {
  			System.arraycopy(str, 0, buf.data, Buffer.GetSpace(buf, length)-1, length);
  			//memcpy(SZ_GetSpace(buf, len - 1) - 1, data, len); // write over trailing 0
  		}
  	} else
  		// first print.
  		System.arraycopy(str, 0, buf.data, Buffer.GetSpace(buf, length), length);
  	//memcpy(SZ_GetSpace(buf, len), data, len);
  	
  	buf.data[buf.cursize - 1]=0;
  }

  //ok.
  public static void writeByte(Buffer sb, int c) {
      sb.data[Buffer.GetSpace(sb, 1)] = (byte) (c & 0xFF);
  }

}

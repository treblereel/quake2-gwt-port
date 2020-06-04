package com.googlecode.gwtquake.shared.render;

import java.util.LinkedList;

import com.googlecode.gwtquake.shared.client.Dimension;
import com.googlecode.gwtquake.shared.common.Globals;

public class DisplayModes {

	static DisplayMode[] getModeList() {
			DisplayMode[] modes = Globals.re.getAvailableDisplayModes();
			
			LinkedList<DisplayMode> l = new LinkedList<DisplayMode>();
			l.add(GlState.oldDisplayMode);
			
			for (int i = 0; i < modes.length; i++) {
				DisplayMode m = modes[i];
				
	//			if (m.getBitsPerPixel() != oldDisplayMode.getBitsPerPixel()) continue;
	////			if (m.getFrequency() > oldDisplayMode.getFrequency()) continue;
	////			if (m.getHeight() < 240 || m.getWidth() < 320) continue;
				
				if (m.height != GlState.oldDisplayMode.height || 
						m.width != GlState.oldDisplayMode.width) {
					l.add(m);
				}
			}
			DisplayMode[] ma = new DisplayMode[l.size()];
			l.toArray(ma);
			return ma;
		}

	static DisplayMode findDisplayMode(Dimension dim) {
		DisplayMode mode = null;
		DisplayMode m = null;
		DisplayMode[] modes = getModeList();
		int w = dim.width;
		int h = dim.height;
		
		for (int i = 0; i < modes.length; i++) {
			m = modes[i];
			if (m.getWidth() == w && m.getHeight() == h) {
				mode = m;
				break;
			}
		}
		if (mode == null) mode = GlState.oldDisplayMode;
		return mode;		
	}

}

package org.treblereel.gwt.quake2;

import com.google.gwt.core.client.EntryPoint;
import com.googlecode.gwtquake.client.GwtQuake;

public class App implements EntryPoint {

    public void onModuleLoad() {
        new GwtQuake().onModuleLoad();
    }
}

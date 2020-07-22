package org.dominokit.samples;

import com.google.gwt.core.client.EntryPoint;
import com.googlecode.gwtquake.client.GwtQuake;
import elemental2.dom.DomGlobal;

public class App implements EntryPoint {

    public void onModuleLoad() {
        new GwtQuake().onModuleLoad();
    }


}

/*
Copyright (C) 2010 Copyright 2010 Google Inc.

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
package com.googlecode.gwtquake.client;

import com.google.gwt.core.client.EntryPoint;
import com.googlecode.gwtquake.shared.client.Dimension;
import com.googlecode.gwtquake.shared.client.Menu;
import com.googlecode.gwtquake.shared.client.PlayerModel;
import com.googlecode.gwtquake.shared.client.Renderer;
import com.googlecode.gwtquake.shared.client.Screen;
import com.googlecode.gwtquake.shared.client.WebSocketFactoryImpl;
import com.googlecode.gwtquake.shared.common.AsyncCallback;
import com.googlecode.gwtquake.shared.common.Compatibility;
import com.googlecode.gwtquake.shared.common.ConsoleVariables;
import com.googlecode.gwtquake.shared.common.Globals;
import com.googlecode.gwtquake.shared.common.QuakeCommon;
import com.googlecode.gwtquake.shared.common.ResourceLoader;
import com.googlecode.gwtquake.shared.sound.Sound;
import com.googlecode.gwtquake.shared.sys.NET;
import elemental2.core.JsArray;
import elemental2.core.JsDate;
import elemental2.dom.CSSProperties;
import elemental2.dom.CSSStyleDeclaration;
import elemental2.dom.DomGlobal;
import elemental2.dom.HTMLBodyElement;
import elemental2.dom.HTMLCanvasElement;
import elemental2.dom.HTMLDivElement;
import elemental2.dom.HTMLDocument;
import elemental2.dom.HTMLElement;
import elemental2.dom.HTMLVideoElement;
import jsinterop.annotations.JsType;
import jsinterop.base.Js;

public class GwtQuake implements EntryPoint {

    private static final int INTER_FRAME_DELAY = 1;

    private static final int LOADING_DELAY = 500;
    private static final java.lang.String NO_WEBGL_MESSAGE =
            "<div style='padding:20px;font-family: sans-serif;'>" +
                    "<h2>WebGL Support Required</h2>" +
                    "<p>For a list of compatible browsers and installation instructions, please refer to" +
                    "<ul><li>" +
                    "<a href='http://code.google.com/p/quake2-gwt-port/wiki/CompatibleBrowsers' " +
                    "style='color:#888'>http://code.google.com/p/quake2-gwt-port/wiki/CompatibleBrowsers</a>" +
                    "</li></ul>" +
                    "<p>For a detailed error log, please refer to the JS console.<p>" +
                    "</div>";
    public static boolean isPointerLockActivated = false;
    static HTMLCanvasElement canvas;
    static HTMLVideoElement video;
    int w;
    int h;
    private HTMLBodyElement body;
    private HTMLDocument doc;
    private Renderer renderer;
    private double startTime = -1;

    static BrowserType getBrowserType() {
        return BrowserType.CHROME;
    }

    public void onModuleLoad() {
        try {
            initCanvas();
            initializeDrivers();
            onResize();
            requestPointerLock();
            loadPlayerModels();
        } catch (Exception e) {
            DomGlobal.console.error(e);

            HTMLDivElement div = (HTMLDivElement) doc.createElement("div");
            div.innerHTML = NO_WEBGL_MESSAGE;
            body.appendChild(div);
        }
    }

    private void requestPointerLock() {
        if (Js.asPropertyMap(canvas).has("requestPointerLock")) {
            canvas.onclick = p0 -> {
                Js.<WithRequestPointerLock>uncheckedCast(canvas).requestPointerLock();
                isPointerLockActivated = true;
                return null;
            };
        }
    }

    private void loadPlayerModels() {
        ResourceLoader.impl.playerModels(new AsyncCallback<JsArray<PlayerModel>>() {
            @Override
            public void onSuccess(JsArray<PlayerModel> response) {
                Menu.s_pmi = new Menu.playermodelinfo_s[response.length];
                for (int i = 0; i < response.length; i++) {
                    PlayerModel playerModel = response.getAt(i);
                    Menu.s_pmi[i] = new Menu.playermodelinfo_s(playerModel.name, playerModel.folder, playerModel.skins);
                }
                startAnimation();
            }

            @Override
            public void onFailure(Throwable e) {
                throw new Error(e);
            }
        });
    }

    private void onResize() {
        DomGlobal.window.onresize = p0 -> {
            if (DomGlobal.window.innerWidth == w &&
                    DomGlobal.window.innerHeight == h) {
                return null;
            }
            w = DomGlobal.window.innerWidth;
            h = DomGlobal.window.innerHeight;

            renderer.GLimp_SetMode(new Dimension(w, h), 0, false);
            return null;
        };
    }

    private void initializeDrivers() {
        Globals.autojoin.value =
                DomGlobal.location.hash.indexOf("autojoin") != -1 ? 1.0f : 0.0f;
        renderer = new GwtWebGLRenderer(canvas, video);
        Globals.re = renderer;

        ResourceLoader.impl = new GwtResourceLoaderImpl();
        Compatibility.impl = new CompatibilityImpl();

        ((GwtKBD) Globals.re.getKeyboardHandler()).Init(canvas);

        Sound.impl = new GwtSound();
        DomGlobal.console.log("GwtSound done");
        NET.socketFactory = new WebSocketFactoryImpl();
        DomGlobal.console.log("NET done");

        // Flags.
        QuakeCommon.Init(new String[]{"GQuake"});
        DomGlobal.console.log("Init done");

        // Enable stdout.
        Globals.nostdout = ConsoleVariables.Get("nostdout", "0", 0);
    }

    private void animate() {
        DomGlobal.requestAnimationFrame(timestamp -> {
            render();
            animate();
        });
    }

    private void startAnimation() {
        startTime = JsDate.now();
        animate();
    }

    private void render() {
        try {
            double curTime = JsDate.now();
            boolean pumping = ResourceLoader.Pump();
            if (pumping) {
                Screen.UpdateScreen2();
            } else {
                int dt = (int) (curTime - startTime);
                GwtKBD.Frame(dt);
                QuakeCommon.Frame(dt);
            }
            startTime = curTime;
        } catch (Exception e) {
            DomGlobal.console.error(e);
        }
    }

    private void initCanvas() {
        doc = DomGlobal.document;
        doc.title = "GWT Quake II";
        body = doc.body;
        CSSStyleDeclaration style = body.style;
        style.padding = CSSProperties.PaddingUnionType.of(0);
        style.margin = CSSProperties.MarginUnionType.of(0);
        style.borderWidth = CSSProperties.BorderWidthUnionType.of(0);
        //style.setProperty("height", "100%");
        style.backgroundColor = "#000";
        style.color = "#888";

        canvas = (HTMLCanvasElement) doc.createElement("canvas");
        video = (HTMLVideoElement) doc.createElement("video");
        video.muted = true;

        w = DomGlobal.window.innerWidth;
        h = DomGlobal.window.innerHeight;

        canvas.width = w;
        canvas.height = h;
        style = canvas.style;
        style.setProperty("height", "100%");
        //style.setProperty("height", String.valueOf(h));
        style.setProperty("width", "100%");

        style.cursor = "none";
        //style.setProperty("width", String.valueOf(w));

        style = video.style;
        style.setProperty("height", "100%");
        style.setProperty("width", "100%");
        style.setProperty("display", "none");

        body.appendChild(canvas);
        body.appendChild(video);
    }

    enum BrowserType {
        FIREFOX,
        CHROME,
        SAFARI,
        OTHER
    }

    @JsType(isNative = true)
    public static class WithRequestPointerLock extends HTMLElement {

        native void requestPointerLock();
    }
}

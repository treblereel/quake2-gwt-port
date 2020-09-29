/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.googlecode.gwtquake.client;

import com.googlecode.gwtquake.shared.client.Key;
import com.googlecode.gwtquake.shared.client.Keys;
import com.googlecode.gwtquake.shared.common.Globals;
import com.googlecode.gwtquake.shared.sys.IN;
import com.googlecode.gwtquake.shared.sys.KBD;
import com.googlecode.gwtquake.shared.sys.Timer;
import elemental2.core.JsDate;
import elemental2.dom.DOMRect;
import elemental2.dom.DomGlobal;
import elemental2.dom.Event;
import elemental2.dom.HTMLCanvasElement;
import elemental2.dom.KeyboardEvent;
import elemental2.dom.MouseEvent;
import elemental2.dom.WheelEvent;
import jsinterop.base.Js;

public class GwtKBD extends KBD {

    public static final int BUTTON_LEFT = 0;
    public static final int BUTTON_MIDDLE = 1;
    public static final int BUTTON_RIGHT = 2;

    /**
     * Show a brighter cursor for the outer quarters of the screen,
     * to serve as a reminder to recenter.
     */
    private static final boolean RECENTER_REMINDER = true;

    /**
     * Mouse position sets a rotation speed instead.
     */
    private static final boolean AUTOROTATE = false;

    /**
     * Use the left mouse key to drag the view, instead of capturing
     * automatically.
     */
    private static final boolean LEFT_MOUSE_DRAG = false;

    private static final double SCALE = 10.0;
    private static final double AUTOROTATE_SCALE = SCALE;
    private static final int MAX_CLICK_TIME = 333;

    private static double normalX;
    private static double lastCmx;
    private static double lastCmy;
    private static boolean captureMove;
    public double mouseDownTime;
    public double mouseUpTime;

    public static void Frame(int ms) {
        if (AUTOROTATE && captureMove) {
            mx += (int) ms * normalX * AUTOROTATE_SCALE;
        }
    }

    @Override
    public void Init() {
        DomGlobal.window.addEventListener("contextmenu", evt -> onPreviewNativeEvent(evt, "contextmenu"));
        windowHalfX = getWidth() / 2;
        X = 0;
        windowHalfY = getHeight() / 2;
        Y = 0;
    }

    HTMLCanvasElement canvasElement;

    public void Init(HTMLCanvasElement canvasElement) {
        this.canvasElement = canvasElement;
        DomGlobal.window.addEventListener("keydown", evt -> onPreviewNativeEvent(evt, "keydown"));
        DomGlobal.window.addEventListener("keyup", evt -> onPreviewNativeEvent(evt, "keyup"));

        canvasElement.addEventListener("mousemove", evt -> onPreviewNativeEvent(evt, "mousemove"), false);
        canvasElement.addEventListener("mousedown", evt -> onPreviewNativeEvent(evt, "mousedown"));
        canvasElement.addEventListener("mouseup", evt -> onPreviewNativeEvent(evt, "mouseup"));
        canvasElement.addEventListener("mousewheel", evt -> onPreviewNativeEvent(evt, "mousewheel"));
        canvasElement.addEventListener("contextmenu", evt -> onPreviewNativeEvent(evt, "contextmenu"));
    }

    double X,Y;

    public void onPreviewNativeEvent(Event event, String type) {
        if (event instanceof KeyboardEvent) {
            KeyboardEvent keyboardEvent = (KeyboardEvent) event;

            if ("keydown".equals(type)) {
                Do_Key_Event(XLateKey(getKeyCode(keyboardEvent)), true);
                if (!hasMeta(keyboardEvent)) {
                    event.preventDefault();
                }
            } else if ("keyup".equals(type)) {
                Do_Key_Event(XLateKey(getKeyCode(keyboardEvent)), false);
                if (!hasMeta(keyboardEvent)) {
                    event.preventDefault();
                }
            }
        } else if (!IN.mouse_active) {
            stopCapturingMouse();
        } else if (event instanceof MouseEvent) {
            MouseEvent mouseEvent = (MouseEvent) event;
            if ("mousemove".equals(type)) {
                double cmx = mouseEvent.clientX;
                double cmy;

                double cx = Globals.viddef.width / 2;
                normalX = (cmx - cx) / cx;

                if (captureMove) {
                    if (!AUTOROTATE) {
                        if(GwtQuake.isPointerLockActivated) {
                            mx += getMovement(event, "movementX") * SCALE;
                            my += getMovement(event, "movementY") * SCALE;
                        } else {
                            cmx = mouseEvent.clientX;
                            cmy = mouseEvent.clientY;

                            mx += (cmx - lastCmx) * SCALE;
                            my += (cmy - lastCmy) * SCALE;

                            lastCmx = cmx;
                            lastCmy = cmy;
                        }
                    }
                }
                mouseEvent.preventDefault();
            } else if ("mousedown".equals(type)) {
                boolean ignoreClick = false;
                //if (mouseEvent.button == BUTTON_RIGHT) {
                    //stopCapturingMouse();
                //} else
                if (mouseEvent.button == BUTTON_LEFT) {
                    mouseDownTime = JsDate.now();
                    ignoreClick = startCapturingMouse(mouseEvent);
                }
                int button = translateMouseButton(mouseEvent);
                if (!ignoreClick && (!LEFT_MOUSE_DRAG || mouseDownTime - mouseUpTime < MAX_CLICK_TIME)) {
                    Do_Key_Event(button, true);
                }
            } else if ("mouseup".equals(type)) {
                int button = translateMouseButton(mouseEvent);
                if (LEFT_MOUSE_DRAG && mouseEvent.button == BUTTON_LEFT) {
                    stopCapturingMouse();
                    if (JsDate.now() - mouseDownTime < MAX_CLICK_TIME) {
                        mouseUpTime = JsDate.now();
                        Do_Key_Event(button, true);
                    }
                } else {
                    startCapturingMouse(mouseEvent);
                }
                Do_Key_Event(button, false);
            }
        } else if (event instanceof WheelEvent) {
            WheelEvent wheelEvent = (WheelEvent) event;
            Do_Key_Event(wheelEvent.deltaY < 0 ? Keys.K_MWHEELUP : Keys.K_MWHEELDOWN, true);
            Do_Key_Event(wheelEvent.deltaY < 0 ? Keys.K_MWHEELUP : Keys.K_MWHEELDOWN, false);
            wheelEvent.preventDefault();
            wheelEvent.stopPropagation();
        } else if ("contextmenu".equals(type)) {
            // try to stop that pesky menu on right button, for some reason, intercepting it on mousedown/up doesn't work
            event.preventDefault();
            event.stopPropagation();
        }
    }

    private double getMovement(Event event, String movementX) {
        return Js.asDouble(Js.asPropertyMap(event).get(movementX));
    }

    double windowHalfX, windowHalfY;

    public double getWidth() {
        return DomGlobal.window.innerWidth * 0.8;
    }

    public double getHeight() {
        return DomGlobal.window.innerHeight;
    }

    private int XLateKey(int key) {
        switch (key) {
            case KeyCodes.KEY_PAGEUP:
                key = Keys.K_PGUP;
                break;
            case KeyCodes.KEY_PAGEDOWN:
                key = Keys.K_PGDN;
                break;
            case KeyCodes.KEY_HOME:
                key = Keys.K_HOME;
                break;
            case KeyCodes.KEY_END:
                key = Keys.K_END;
                break;
            case KeyCodes.KEY_LEFT:
                key = Keys.K_LEFTARROW;
                break;
            case KeyCodes.KEY_RIGHT:
                key = Keys.K_RIGHTARROW;
                break;
            case KeyCodes.KEY_DOWN:
                key = Keys.K_DOWNARROW;
                break;
            case KeyCodes.KEY_UP:
                key = Keys.K_UPARROW;
                break;
            case KeyCodes.KEY_ESCAPE:
                key = Keys.K_ESCAPE;
                break;
            case KeyCodes.KEY_ENTER:
                key = Keys.K_ENTER;
                break;
            case KeyCodes.KEY_TAB:
                key = Keys.K_TAB;
                break;
            case KeyCodes.KEY_BACKSPACE:
                key = Keys.K_BACKSPACE;
                break;
            case KeyCodes.KEY_DELETE:
                key = Keys.K_DEL;
                break;
            case KeyCodes.KEY_SHIFT:
                key = Keys.K_SHIFT;
                break;
            case KeyCodes.KEY_CTRL:
                key = Keys.K_CTRL;
                break;

            // Safari on MAC (TODO(jgw): other browsers may need tweaking):
            case 112:
                key = Keys.K_F1;
                break;
            case 113:
                key = Keys.K_F2;
                break;
            case 114:
                key = Keys.K_F3;
                break;
            case 115:
                key = Keys.K_F4;
                break;
            case 116:
                key = Keys.K_F5;
                break;
            case 117:
                key = Keys.K_F6;
                break;
            case 118:
                key = Keys.K_F7;
                break;
            case 119:
                key = Keys.K_F8;
                break;
            case 120:
                key = Keys.K_F9;
                break;
            case 121:
                key = Keys.K_F10;
                break;
            case 122:
                key = Keys.K_F11;
                break;
            case 123:
                key = Keys.K_F12;
                break;

            case 186:
                key = ';';
                break;
            case 187:
                key = '=';
                break;
            case 188:
                key = ',';
                break;
            case 189:
                key = '-';
                break;
            case 190:
                key = '.';
                break;
            case 191:
                key = '/';
                break;
            case 192:
                key = '`';
                break;
            case 222:
                key = '\'';
                break;
            case 219:
                key = '[';
                break;
            case 220:
                key = '\\';
                break;
            case 221:
                key = ']';
                break;

            default:
                if (key < '0' || key > '9') {
                    if (key >= 'A' && key <= 'Z') {
                        key = Character.toLowerCase((char) key);
                    }
                }

// TODO(jgw): We probably need keycodes for these.
//      case KeyCodes.KEY_PAUSE: key = Key.K_PAUSE; break;
//      case KeyCodes.KEY_MENU: key = Key.K_ALT; break;
//      case KeyCodes.KEY_INSERT: key = Key.K_INS; break;
        }

        return key;
    }

    private int getKeyCode(KeyboardEvent event) {
        return Js.asInt(Js.asPropertyMap(event).get("keyCode"));
    }

    private boolean hasMeta(KeyboardEvent event) {
        return event.altKey || event.metaKey || event.ctrlKey;
    }

    private void stopCapturingMouse() {
        captureMove = false;
        GwtQuake.canvas.style.cursor = "default";
    }

    private boolean startCapturingMouse(MouseEvent nevt) {
        if (captureMove) {
            return false;
        }
        captureMove = true;
        GwtQuake.canvas.style.setProperty("cursor", "none");
        lastCmx = (int) nevt.clientX;
        lastCmy = (int) nevt.clientY;
        return (nevt.buttons & 1) != 1;
    }

    private static int translateMouseButton(MouseEvent evt) {
        switch (evt.button) {
            case BUTTON_LEFT:
                return Keys.K_MOUSE1;
            case BUTTON_RIGHT:
                return Keys.K_MOUSE2;
            default:
                return Keys.K_MOUSE3;
        }
    }

    @Override
    public void Update() {
    }

    @Override
    public void Close() {
    }

    @Override
    public void Do_Key_Event(int key, boolean down) {
        Key.Event(key, down, Timer.Milliseconds());
    }

    @Override
    public void installGrabs() {

    }

    @Override
    public void uninstallGrabs() {

    }
}

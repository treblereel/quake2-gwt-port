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

import java.io.IOException;
import java.util.ArrayList;

import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.ResourceLoader;
import elemental2.core.JsDate;
import elemental2.dom.DomGlobal;
import org.gwtproject.nio.TypedArrayHelper;
import org.gwtproject.timer.client.Timer;
import org.gwtproject.xhr.client.ReadyStateChangeHandler;
import org.gwtproject.xhr.client.XMLHttpRequest;

public class GwtResourceLoaderImpl implements ResourceLoader.Impl {

    private static final int RECEIVED_WAIT_TIME = 1;

    private int freeSequenceNumber;
    private int ignoreSequenceNumbersBelow;
    private int currentSequenceNumber;
    private ArrayList<ResponseHandler> readyList = new ArrayList<>();

    public void loadResourceAsync(final String path, final ResourceLoader.Callback callback) {
        XMLHttpRequest req = XMLHttpRequest.create();

        final Exception e = new Exception();
        final int mySequenceNumber = freeSequenceNumber++;

        req.setOnReadyStateChange(new ReadyStateChangeHandler() {
            boolean receivingMsg;

            public void onReadyStateChange(final XMLHttpRequest xhr) {
                if (xhr.getReadyState() == 3 && !receivingMsg) {
                    Com.Printf("Receiving #" + mySequenceNumber + ": " + path + "\n");
                    receivingMsg = true;
                } else if (xhr.getReadyState() == 4) {
                    if (mySequenceNumber < ignoreSequenceNumbersBelow) {
                        Com.Printf("Ignoring outdated response #" + mySequenceNumber + ": " + path + "\n");
                    } else {
                        String response;
                        if (xhr.getStatus() != 200) {
                            Com.Printf("Failed to load file #" + mySequenceNumber + ": " + path + " status: " +
                                               xhr.getStatus() + "/" + xhr.getStatusText() + "\n");
                            ResourceLoader.fail(new IOException("status = " + xhr.getStatus()));
                            response = null;
                        } else {
                            response = xhr.getResponseText();
                            Com.Printf("Received response #" + mySequenceNumber + ": " + path + "\r");
                        }
                        readyList.add(0, new ResponseHandler(mySequenceNumber, callback, response));
                        if (mySequenceNumber == currentSequenceNumber) {
                            processReadyList();
                        }
                    }
                }
            }
        });

        String href = DomGlobal.location.href;
        DomGlobal.console.log("href " + href);

        Com.Printf("Requesting: " + path + "\n");
        req.overrideMimeType("text/plain; charset=x-user-defined");
        req.open("GET", href + "baseq2/" + path);
        req.send();
    }

    public boolean pump() {
        return currentSequenceNumber != freeSequenceNumber;
    }

    public void reset() {
        ignoreSequenceNumbersBelow = freeSequenceNumber;
        currentSequenceNumber = freeSequenceNumber;
    }

    private void processReadyList() {
        new Timer() {
            @Override
            public void run() {

                try {

                    for (int i = readyList.size() - 1; i >= 0; i--) {
                        ResponseHandler handler = readyList.get(i);
                        if (handler.sequenceNumber == currentSequenceNumber) {
                            if (handler.response != null) {
                                double t0 = JsDate.now();
                                handler.callback.onSuccess(TypedArrayHelper.stringToByteBuffer(handler.response));

                                Com.Printf("Processed #" + currentSequenceNumber + " in " + (JsDate.now() - t0) / 1000.0 + "s\r");
                            }
                            readyList.remove(i);
                            currentSequenceNumber++;
                            processReadyList();
                            return;
                        }
                    }
                } catch (Exception e) {
                    DomGlobal.console.error(e);
                }
            }
        }.schedule(RECEIVED_WAIT_TIME);
    }

    static class ResponseHandler {

        int sequenceNumber;
        ResourceLoader.Callback callback;
        String response;

        ResponseHandler(int sequenceNumber, ResourceLoader.Callback callback, String response) {
            this.sequenceNumber = sequenceNumber;
            this.callback = callback;
            this.response = response;
        }
    }
}

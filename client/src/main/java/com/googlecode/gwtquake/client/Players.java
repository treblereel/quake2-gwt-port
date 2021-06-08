package com.googlecode.gwtquake.client;

import java.io.IOException;

import com.googlecode.gwtquake.shared.client.PlayerModel;
import com.googlecode.gwtquake.shared.common.AsyncCallback;
import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.ResourceLoader;
import elemental2.core.Global;
import elemental2.core.JsArray;
import elemental2.dom.Event;
import elemental2.dom.XMLHttpRequest;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;

/**
 * @author Dmitrii Tikhomirov
 * Created by treblereel 9/26/20
 */
public class Players {

    private final String url = "resource/models";
    private JsArray<PlayerModel> models;

    public Players() {

    }

    public void playerModels(AsyncCallback<JsArray<PlayerModel>> onLoad) {
        if (models != null) {
            onLoad.onSuccess(models);
        }
        XMLHttpRequest req = new XMLHttpRequest();
        req.onreadystatechange = new XMLHttpRequest.OnreadystatechangeFn() {
            boolean receivingMsg;

            @Override
            public Object onInvoke(Event p0) {
                XMLHttpRequest xhr = req;
                if (xhr.readyState == 3 && !receivingMsg) {
                    Com.Printf("Receiving player models list");
                    receivingMsg = true;
                } else if (xhr.readyState == 4) {
                    if (xhr.status != 200) {
                        ResourceLoader.fail(new IOException("status = " + xhr.status));
                        onLoad.onFailure(new IOException("status = " + xhr.status));
                    } else {
                        String response = xhr.responseText;
                        Object temp = Global.JSON.parse(response);
                        models = new JsArray<>();
                        Js.asPropertyMap(temp).forEach(key -> {
                            JsPropertyMap model = Js.uncheckedCast(Js.asPropertyMap(temp).get(key));
                            models.push(new PlayerModel(Js.uncheckedCast(model.get("name")),
                                                        Js.uncheckedCast(model.get("folder")),
                                                        Js.uncheckedCast(model.get("skins"))));
                        });

                        Com.Printf("models " + response);
                        Com.Printf("Received response player models list");
                        onLoad.onSuccess(models);
                    }
                }
                return null;
            }
        };

        Com.Printf("Requesting: " + url + "\n");
        req.overrideMimeType("text/plain; charset=x-user-defined");
        req.open("GET", url, true);
        req.send();
    }

}

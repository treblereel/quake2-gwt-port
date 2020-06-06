package com.googlecode.gwtquake.server;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.websocket.Session;

import com.googlecode.gwtquake.shared.common.Compatibility;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.ResourceLoader;
import com.googlecode.gwtquake.shared.server.QuakeServer;
import com.googlecode.gwtquake.shared.sys.NET;

/**
 * @author Dmitrii Tikhomirov
 * Created by treblereel 5/24/20
 */
@ApplicationScoped
public class GwtQuakeServer {

    private final Map<Session, MyWebSocket> handler = new HashMap<>();

    private ServerWebSocketFactoryImpl serverWebSocketFactory = new ServerWebSocketFactoryImpl();

    @Inject
    private QuakeServerWrapper server;

    @Inject
    private ServletContext servletContext;

    @PostConstruct
    public void init() {
        System.out.println("GwtQuakeServer init");

        Compatibility.impl = new CompatibilityImpl();
        ResourceLoader.impl = new ResourceLoaderImpl(servletContext);

        NET.socketFactory = serverWebSocketFactory;
        NET.Config(true);
        NET.Init();

        System.out.println("startServer");
        server.startServer();
        System.out.println("startServer done");

    }

    public Map<Session, MyWebSocket> getHandler() {
        return handler;
    }

    @PreDestroy
    private void onDestroy() {
        System.out.println("onDestroy");
        NET.isRunning = false;
        server.setRun(false);
    }
}

package com.googlecode.gwtquake.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.websocket.Session;

import com.googlecode.gwtquake.shared.common.Compatibility;
import com.googlecode.gwtquake.shared.common.ResourceLoader;
import com.googlecode.gwtquake.shared.game.Commands;
import com.googlecode.gwtquake.shared.sys.NET;
import org.apache.commons.io.IOUtils;

/**
 * @author Dmitrii Tikhomirov
 * Created by treblereel 5/24/20
 */
@ApplicationScoped
public class GwtQuakeServer {

    private final Map<Session, MyWebSocket> handler = new HashMap<>();
    @Inject
    QuakeServerWrapper server;
    @Inject
    ServletContext servletContext;
    private ServerWebSocketFactoryImpl serverWebSocketFactory = new ServerWebSocketFactoryImpl();

    @PostConstruct
    public void init() {
        System.out.println("GwtQuakeServer init");

        Compatibility.impl = new CompatibilityImpl();
        ResourceLoader.impl = new ResourceLoaderImpl();

        Commands.serverFileLoader = new Function<String, byte[]>() {
            @Override
            public byte[] apply(String s) {
                try {
                    try (InputStream inputStream = getClass().getResourceAsStream("/META-INF/resources/baseq2/" + s)) {
                        return IOUtils.toByteArray(inputStream);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new Error("Unable to read file " + s);
                }
            }
        };

        NET.socketFactory = serverWebSocketFactory;
        NET.Config(true);
        //quake2Socket.setServerWebSocket(serverWebSocketFactory);
        NET.Init();
        //System.out.println("NET " + NET.ip_sockets[Constants.NS_SERVER]);

        System.out.println("startServer");
        server.startServer();
        System.out.println("startServer done");
    }

    public Map<Session, MyWebSocket> getHandler() {
        return handler;
    }

    @PreDestroy
    void onDestroy() {
        System.out.println("onDestroy");
        NET.isRunning = false;
        server.setRun(false);
    }
}

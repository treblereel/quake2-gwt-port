package com.googlecode.gwtquake.server;

import javax.enterprise.context.ApplicationScoped;

import com.googlecode.gwtquake.shared.server.QuakeServer;

/**
 * @author Dmitrii Tikhomirov
 * Created by treblereel 6/4/20
 */
@ApplicationScoped
public class QuakeServerWrapper {

    private QuakeServer server = new QuakeServer();

    public void startServer() {
        Thread thread = new Thread(() -> server.run(new String[]{}));
        thread.start();
    }

    public void setRun(boolean b) {
        server.setRun(b);
    }
}

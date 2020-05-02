package com.googlecode.gwtquake.server;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.enterprise.context.Dependent;

import com.googlecode.gwtquake.shared.server.QuakeServer;

/**
 * @author Dmitrii Tikhomirov
 * Created by treblereel 6/4/20
 */
@Asynchronous
@Stateless
public class QuakeServerWrapper {

    private QuakeServer server = new QuakeServer();

    @Asynchronous
    public void startServer() {
        server.run(new String[]{});
    }

    public void setRun(boolean b) {
        server.setRun(b);
    }
}

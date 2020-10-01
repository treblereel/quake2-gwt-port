package com.googlecode.gwtquake.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * @author Dmitrii Tikhomirov
 * Created by treblereel 9/26/20
 */
@Path("/resource")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class Resources {

    public Resources() {
    }

    @GET
    @Path("/models")
    public Response models() {
        List<Model> models = new ArrayList<>();

        models.add(new Model("Female", "female", Arrays.asList("athena", "brianna", "cobalt", "doomgal",
                                                               "ensign", "jezebel", "jungle", "lotus",
                                                               "stiletto", "venus", "voodoo")));

        models.add(new Model("Male", "male", Arrays.asList("cipher", "claymore", "flak", "grunt",
                                                           "howitzer", "major", "nightops", "pointman",
                                                           "psycho", "rampage", "razor",
                                                           "recon", "scout", "sniper", "viper"
        )));

        return Response.ok(models).build();
    }

    @RegisterForReflection
    static class Model {

        private String name;
        private String folder;
        private List<String> skins;

        Model() {

        }

        Model(String name, String folder, List<String> skins) {
            this.name = name;
            this.folder = folder;
            this.skins = skins;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFolder() {
            return folder;
        }

        public void setFolder(String folder) {
            this.folder = folder;
        }

        public List<String> getSkins() {
            return skins;
        }

        public void setSkins(List<String> skins) {
            this.skins = skins;
        }
    }
}

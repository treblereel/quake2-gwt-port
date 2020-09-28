package com.googlecode.gwtquake.shared.client;

/**
 * @author Dmitrii Tikhomirov
 * Created by treblereel 9/28/20
 */
public class PlayerModel {

    public String name;
    public String folder;
    public String[] skins;

    public PlayerModel(String name, String folder, String[] skins) {
        this.name = name;
        this.folder = folder;
        this.skins = skins;
    }
}

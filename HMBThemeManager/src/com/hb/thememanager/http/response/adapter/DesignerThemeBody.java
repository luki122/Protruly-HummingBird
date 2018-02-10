package com.hb.thememanager.http.response.adapter;

import java.util.ArrayList;

/**
 * Created by alexluo on 17-8-8.
 */

public class DesignerThemeBody extends ResponseBody{

    public ArrayList<ThemeResource> resource;

    public ArrayList<ThemeResource> getResource() {
        return resource;
    }

    public void setResource(ArrayList<ThemeResource> resource) {
        this.resource = resource;
    }
}

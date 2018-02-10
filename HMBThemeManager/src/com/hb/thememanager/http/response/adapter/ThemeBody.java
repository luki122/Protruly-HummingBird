package com.hb.thememanager.http.response.adapter;

import com.alibaba.fastjson.JSON;
import com.hb.thememanager.model.Advertising;
import com.hb.thememanager.model.Fonts;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.Wallpaper;

import java.util.ArrayList;

/**
 * Created by alexluo on 17-8-3.
 */

public class ThemeBody extends ResponseBody{
    public ArrayList<ThemeResource> resource;

    public ArrayList<Theme> getThemes(int themeType){
        ArrayList<Theme> themes = new ArrayList<Theme>();
        if(resource != null) {
            for (int i = 0; i < resource.size(); i++) {
                Theme theme = createTheme(themeType);
                ThemeResource tr = resource.get(i);
                theme.id = tr.id;
                theme.name = tr.name;
                theme.coverUrl = tr.icon;
                theme.hasComment = Theme.HAS_COMMENT;
                theme.downloadUrl = tr.downloadUrl;
                theme.type = themeType;
                theme.price = (tr.price == 0 || tr.isCharge == 0) ? null : String.valueOf(tr.price / 100.0f);
                theme.isCharge = tr.isCharge;
                themes.add(theme);

            }
        }
        return themes;
    }

    private Theme createTheme(int themeType){
        switch (themeType){
            case Theme.THEME_PKG:
                return new Theme();
            case Theme.WALLPAPER:
                return new Wallpaper();
            case Theme.FONTS:
                return new Fonts();
            default:
                return new Theme();
        }
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}

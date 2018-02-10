package com.hb.thememanager.http.response.adapter;

import com.hb.thememanager.model.Fonts;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.Wallpaper;

import java.util.ArrayList;

/**
 * 主题列表推荐数据接受类
 */

public class Hotrecommend {

    public String name;
    public int recommendId;
    public ArrayList<ThemeResource> resource;
    @Override
    public String toString() {
        return "Hotrecommend{" +
                "name='" + name + '\'' +
                ", recommendId=" + recommendId +
                ", resource=" + resource +
                '}';
    }

    public ArrayList<Theme> getThemes(int themeType){
        ArrayList<Theme> themes = new ArrayList<Theme>();
        if(resource == null || resource.size() == 0){
            return themes;
        }
        for(int i = 0;i<resource.size();i++){
            Theme theme = createTheme(themeType);
            ThemeResource tr = resource.get(i);
            theme.id = tr.id;
            theme.name = tr.name;
            theme.coverUrl = tr.icon;
            theme.downloadUrl = tr.downloadUrl;
            theme.hasComment = Theme.HAS_COMMENT;
            theme.isCharge = tr.isCharge;
            theme.price = tr.isCharge == 0?null:String.valueOf(tr.price/100.0f);
            themes.add(theme);

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

}


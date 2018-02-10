package com.hb.thememanager.http.response.adapter;

import com.hb.thememanager.model.Advertising;

import java.util.ArrayList;

/**
 * Created by alexluo on 17-8-3.
 */

public class AdvertisinBody extends ResponseBody{
    public ArrayList<Advertising> banner;

    @Override
    public String toString() {
        return "Body{" +
                "banner=" + banner +
                '}';
    }
}

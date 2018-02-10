package com.android.settings.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TabHost;

import com.android.settings.R;

import java.lang.reflect.Field;

/**
 * Created by liuqin on 17-4-26.
 *
 * @date Liuqin on 2017-04-26
 */
public class SettingTabHost extends TabHost{
    public SettingTabHost(Context context) {
        super(context);
        init();
    }

    public SettingTabHost(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        changeTabLayouToHb(this);
    }

    private void changeTabLayouToHb(TabHost tabHost) {
        try {
            Field field = TabHost.class.getDeclaredField("mTabLayoutId");
            field.setAccessible(true);
            field.set(tabHost, R.layout.tab_indicator_material);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}

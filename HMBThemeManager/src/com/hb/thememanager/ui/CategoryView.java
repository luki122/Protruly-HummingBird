package com.hb.thememanager.ui;

import com.hb.thememanager.model.Tab;

import java.util.List;

/**
 * Created by caizhongting on 17-6-13.
 */

public interface CategoryView extends BaseView {

    public void updateList(Object response);

    public void networkError(int statusCode, String error_msg);
}

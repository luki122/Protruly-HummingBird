package com.hb.thememanager.ui;

import com.hb.thememanager.MvpView;

/**
 * Created by caizhongting on 17-6-13.
 */

public interface BaseView extends MvpView {

    void showToast(String msg);

    void showMyDialog(int dialogId);

    void showEmptyView(boolean show);

    void showNetworkErrorView(boolean show);

}

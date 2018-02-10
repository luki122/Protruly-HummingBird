package com.hb.thememanager.ui;

import com.hb.thememanager.MvpView;
import com.hb.thememanager.http.response.Response;

/**
 * Created by alexluo on 17-8-26.
 */

public interface SimpleRequestView extends MvpView {



    public void showToast(String msg);

    public void showDialog(int dialogId);

    public void update(Response result);

    public void showRequestFailView(boolean show);


}

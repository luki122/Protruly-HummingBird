package com.hb.thememanager.ui.fragment.themedetail;

import android.os.Bundle;
import android.view.View;

/**
 * Created by alexluo on 17-8-8.
 */

public interface IFragment {


    /**
     * 获取传递给该Fragment的参数
     * @return
     */
    public Bundle getBundle();

    /**
     * 获取该Fragment的布局ID
     * @return
     */
    public int getLayoutRes();

    /**
     * 通过ID查找View
     * @param id
     * @return
     */
    public View findViewById(int id);

}

package com.hb.thememanager.ui.fragment.themelist;

import com.hb.thememanager.model.Theme;
import com.hb.thememanager.R;
import com.hb.thememanager.ui.adapter.LocalThemeListAdapter;
import com.hb.thememanager.ui.adapter.LocalThemePkgListAdapter;

/**
 * Created by alexluo on 17-8-21.
 */

public class LocalThemePkgListFragment extends LocalThemeListFragment {




    @Override
    public int getLayoutRes() {
        return R.layout.local_theme_pkg_layout;
    }

    @Override
    protected int getThemeType() {
        return Theme.THEME_PKG;
    }


    @Override
    protected LocalThemeListAdapter getAdapter() {
        return new LocalThemePkgListAdapter(getContext());
    }
}

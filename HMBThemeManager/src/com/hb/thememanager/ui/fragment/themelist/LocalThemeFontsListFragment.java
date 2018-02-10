package com.hb.thememanager.ui.fragment.themelist;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import com.hb.thememanager.database.SharePreferenceManager;
import com.hb.thememanager.model.Fonts;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.R;
import com.hb.thememanager.state.ThemeState;
import com.hb.thememanager.ui.adapter.LocalThemeFontsListAdapter;
import com.hb.thememanager.ui.adapter.LocalThemeListAdapter;
import com.hb.thememanager.ui.fragment.themedetail.LocalFontDetailFragment;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.FileUtils;

import android.content.res.HbFontsManager;
import android.content.res.HbConfiguration;

/**
 * Created by alexluo on 17-8-21.
 */

public class LocalThemeFontsListFragment extends LocalThemeListFragment {

    @Override
    protected int getThemeType() {
        return Theme.FONTS;
    }


    @Override
    public int getLayoutRes() {
        return R.layout.local_theme_fonts_layout;
    }


    @Override
    protected LocalThemeListAdapter getAdapter() {
        return new LocalThemeFontsListAdapter(getContext());
    }

    @Override
    protected void handleItemClick(AdapterView parent, View view, int position, long id) {
        if(position != 0) {
            Theme theme = mAdapter.getTheme(position);
            if (isEditMode()) {
                super.handleItemClick(parent, view, position, id);
            } else {

                if (theme != null) {
                    Bundle args = new Bundle();
                    args.putParcelable(Config.ActionKey.KEY_THEME_PKG_DETAIL, theme);
                    startFragment(this, LocalFontDetailFragment.class.getName(), true, theme.name, 0, args);
                }
            }
        }else{
            HbFontsManager.getInstance().setFonts(null, new HbFontsManager.OnFontsSetListener() {
                public void onSetStart() {
                }

                public void onSetFinish(boolean success) {
                    SharePreferenceManager.setStringPreference(getContext(),
                            SharePreferenceManager.KEY_APPLIED_FONT_ID, "");
                    HbConfiguration.updateThemeConfiguration(1);
                }
            });
        }
    }
}

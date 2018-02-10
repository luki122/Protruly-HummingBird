package com.hb.thememanager.ui;

import com.hb.thememanager.model.Theme;
import com.hb.thememanager.ui.fragment.AbsLocalThemeFragment;
import com.hb.thememanager.ui.fragment.themedetail.AbsThemeDetailFragment;
import com.hb.thememanager.ui.fragment.themedetail.FontPkgDetailFragment;
import com.hb.thememanager.ui.fragment.themedetail.ThemePkgDetailFragment;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.FragmentUtils;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.ViewGroup;


public class DetailActivity extends SecondActivity {
	
	private String mDetailFragment;
	@Override
	protected void onCreate(Bundle savedState) {
		// TODO Auto-generated method stub
		super.onCreate(savedState);
		handleIntent(getIntent());
		
	}
	private void handleIntent(Intent intent){
		Theme theme = (Theme) intent.getExtra(Config.ActionKey.KEY_HOME_THEME_ITEM);

		if(theme != null){
			if(theme.type == Theme.THEME_PKG){
				mDetailFragment = ThemePkgDetailFragment.class.getName();
			}else if(theme.type == Theme.WALLPAPER) {
			}else if(theme.type == Theme.FONTS){
				mDetailFragment = FontPkgDetailFragment.class.getName();
			}

			Bundle args = new Bundle();
			args.putParcelable(Config.ActionKey.KEY_THEME_PKG_DETAIL, theme);
			if(mDetailFragment != null){
				switchToFragment(mDetailFragment, com.hb.R.id.content, args,  false, 0, theme.name, true);
			}
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		handleIntent(intent);
	}



	private void switchToFragment(String fragment, int contentViewId, Bundle args,
								  boolean addToBackStack, int titleResId, CharSequence title, boolean withTransition) {
    	Fragment f = Fragment.instantiate(this, fragment, args);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(contentViewId, f);
        ViewGroup content = (ViewGroup)findViewById(contentViewId);
        if (withTransition) {
            TransitionManager.beginDelayedTransition(content);
        }
        if (addToBackStack) {
            transaction.addToBackStack(":theme_manager:prefs");
        }
        if (titleResId > 0) {
        	setTitle(titleResId);
        } else if (title != null) {
        	setTitle(title);
        }
        transaction.commitAllowingStateLoss();
        getFragmentManager().executePendingTransactions();
    }
	

}

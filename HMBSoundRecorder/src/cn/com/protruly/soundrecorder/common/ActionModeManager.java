package cn.com.protruly.soundrecorder.common;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import cn.com.protruly.soundrecorder.R;
import hb.app.HbActivity;
import hb.view.menu.bottomnavigation.BottomNavigationView;
import hb.view.menu.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener;
import hb.widget.ActionMode;
import hb.widget.ActionModeListener;

/**
 * Created by sqf on 17-5-11.
 */

public class ActionModeManager {

    private HbActivity mActivity;

    private ActionModeListener mActionModeListener;

    //for bottom menus click event
    private OnNavigationItemSelectedListener mOnNavigationItemSelectedListener;

    private BottomNavigationView mBottomBar;
    private ActionMode mActionMode;

    private Boolean mCurrentActionMode = false;

    public ActionModeManager(HbActivity activity) {
        mActivity = activity;
        mActionMode = mActivity.getActionMode();
    }

    public void setActionModeListener(ActionModeListener listener) {
        mActionModeListener = listener;
    }

    public void setOnNavigationItemSelectedListener(OnNavigationItemSelectedListener listener) {
        mOnNavigationItemSelectedListener = listener;
    }

    public Boolean getCurrentActionMode() {
        return mCurrentActionMode;
    }

    public void setCurrentActionMode(Boolean nowInActionMode) {
        mCurrentActionMode = nowInActionMode;
    }



    public void startActionMode() {
        mActivity.setActionModeListener(mActionModeListener);
        mActivity.showActionMode(true);
        mBottomBar = (BottomNavigationView)mActivity.findViewById(R.id.bottom_menu);
        mBottomBar.setVisibility(View.VISIBLE);
        Log.d("qad","startActionMode mBottomBar.setVisibility:"+mBottomBar.getVisibility());
        mBottomBar.setNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    public void startSelectMode(){
        mBottomBar = (BottomNavigationView)mActivity.findViewById(R.id.bottom_menu);
        mBottomBar.setVisibility(View.VISIBLE);
        mBottomBar.setNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    public void finishActionMode() {
        if(null == mBottomBar) {
            mBottomBar = (BottomNavigationView) mActivity.findViewById(R.id.bottom_menu);
        }
        mBottomBar.setVisibility(View.GONE);
        mActivity.showActionMode(false);
        mActivity.setActionModeListener(null);
    }


    public void showBottomNavigationMenuItem(int menuItemId,boolean enabled){
        if(null == mBottomBar) {
            mBottomBar = (BottomNavigationView) mActivity.findViewById(R.id.bottom_menu);
        }
        mBottomBar.setItemEnable(menuItemId,enabled);
    }

 /*   public void removeBottomNavigationMenuItem(int menuItemId){
        if(null == mBottomBar) {
            mBottomBar = (BottomNavigationView) mActivity.findViewById(R.id.bottom_menu1);
        }
        mBottomBar.removeItem(menuItemId);
    }
    //added
    public void startSelectPathActionMode() {
        //mActivity.setActionModeListener(mActionModeListener);
        //mActivity.showActionMode(true);
        mBottomBar = (BottomNavigationView)mActivity.findViewById(R.id.bottom_menu1);
        if(null == mBottomBar) return;
        mBottomBar.setVisibility(View.VISIBLE);
        mBottomBar.setNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }*/

    public void setBottomBarItemEnabled(int menuItemId, boolean enabled) {
        if(null == mBottomBar) return;
        mBottomBar.setItemEnable(menuItemId, enabled);
    }

    public void setAllBottomBarItemEnable(boolean enabled) {
        if(null == mBottomBar) return;
        Menu menu = mBottomBar.getMenu();
        for(int i = 0; i< menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            mBottomBar.setItemEnable(item.getItemId(), enabled);
        }
    }

  /*  public void finishSelectPathActionMode() {
        if(null == mBottomBar) {
            mBottomBar = (BottomNavigationView) mActivity.findViewById(R.id.bottom_menu1);
        }
        mBottomBar.setVisibility(View.GONE);
        mActivity.showActionMode(false);
        mActivity.setActionModeListener(null);
    }

    public void showBottomNavigationSelectPathMenuItem(int menuItemId,boolean enabled){
        if(null == mBottomBar) {
            mBottomBar = (BottomNavigationView) mActivity.findViewById(R.id.bottom_menu1);
        }
        mBottomBar.setItemEnable(menuItemId,enabled);
    }*/

    //added
    public void setPositiveText(int textResId) {
        String text = mActivity.getString(textResId);
        setPostiveText(text);
    }

    public void setPostiveText(String text) {
        mActionMode.setPositiveText(text);
    }

    public void setNegativeText(int textResId) {
        String text = mActivity.getString(textResId);
        setNegativeText(text);
    }

    public void setNegativeText(String text) {
        mActionMode.setNagativeText(text);
    }

    public void setActionModeTitle(int textResId) {
        String text = mActivity.getString(textResId);
        setActionModeTitle(text);
    }

    public void setActionModeTitle(String text) {
        mActivity.updateActionModeTitle(text);
    }
}

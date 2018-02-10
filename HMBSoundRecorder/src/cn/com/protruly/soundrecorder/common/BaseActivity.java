package cn.com.protruly.soundrecorder.common;


import android.os.Bundle;
import android.view.MenuItem;

import cn.com.protruly.soundrecorder.R;
import hb.app.HbActivity;
import hb.view.menu.bottomnavigation.BottomNavigationView;
import hb.widget.ActionMode;
import hb.widget.ActionModeListener;
import hb.widget.toolbar.Toolbar;

/**
 * Created by sqf on 17-8-14.
 */

public abstract  class BaseActivity extends HbActivity implements BottomNavigationView.OnNavigationItemSelectedListener,
        ActionModeListener, ToolbarManager.OnToolbarNavigationIconClickListener, Toolbar.OnMenuItemClickListener {
    protected ToolbarManager mToolbarManager;
    protected ActionModeManager mActionModeManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mToolbarManager = new ToolbarManager(this);
        mActionModeManager = new ActionModeManager(this);
        initView();
        initData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mToolbarManager = getToolbarManager();
        mActionModeManager = getActionModeManager();
        mToolbarManager.setOnNavigationIconClickListener(this);
        mToolbarManager.setOnToolbarMenuItemClickListener(this);
        mActionModeManager.setActionModeListener(this);
        mActionModeManager.setOnNavigationItemSelectedListener(this);
    }

    public ToolbarManager getToolbarManager() {
        return mToolbarManager;
    }

    public ActionModeManager getActionModeManager() {
        return mActionModeManager;
    }


    //call back for BottomNavigationBar Begin
    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        return false;
    }
    //call back for BottomNavigationBar End

    //callbacks for ActionMode Begin
    @Override
    public void onActionItemClicked(ActionMode.Item item) {

    }

    @Override
    public void onActionModeShow(ActionMode actionMode) {
        mActionModeManager.setCurrentActionMode(true);
    }

    @Override
    public void onActionModeDismiss(ActionMode actionMode) {
        mActionModeManager.setCurrentActionMode(false);
    }
    //callbacks for ActionMode End

    //Tool bar Navigation icon begin
    @Override
    public void onToolbarNavigationIconClicked() {

    }
    //Tool bar Navigation icon end

    //Tool bar menu items Begin
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return false;
    }
    //Tool bar menu items End


    protected abstract void initData();
    protected abstract void initView();

}

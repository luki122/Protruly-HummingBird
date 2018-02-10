package cn.com.protruly.filemanager.ui;

import android.view.MenuItem;
import android.view.View;

public interface HbToolBarListener {
    //tool bar back
    public void onHbToolbarNavigationClicked(View view);
    public boolean onHbToolbarMenuItemClicked(MenuItem item);
}

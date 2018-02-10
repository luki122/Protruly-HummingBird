package com.android.settings;

import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import hb.app.HbActivity;
import hb.widget.toolbar.Toolbar;
/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class FullActivityBase extends HbActivity{
	private boolean mDisplayHomeAsUpEnabled = false;
	private OnToolbarListener mOnToolbarListener;
	private boolean mIsBackKeyDown = false;

    @Override
    public void setContentView(int layoutResID) {
    	super.setHbContentView(layoutResID);
    	initToolbar();
    }
    
    @Override
    public void setContentView(View view) {
    	super.setHbContentView(view);
    	initToolbar();
    }
    
    public void initToolbar() {
    	try{
    		Toolbar toolbar = getToolbar();
			if (toolbar != null) {
				toolbar.setTitle(getTitle());
				showBackIcon(this instanceof SubSettings || mDisplayHomeAsUpEnabled);
				setActionBar(toolbar);
				toolbar.setNavigationOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						onNavigationClicked(view);
					}
				});
			}

    	}catch(Exception e){
    		e.printStackTrace();
    	}        
	}

	public void onNavigationClicked(View view){
		if (mOnToolbarListener != null) {
			mOnToolbarListener.onNavigationBackClick();
			return;
		}

		if (this instanceof SubSettings || mDisplayHomeAsUpEnabled) {
			finish();
		}
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		return getWindow().getCallback().onMenuItemSelected(0,item);
	}

	public void setDisplayHomeAsUpEnabled(boolean mDisplayHomeAsUpEnabled) {
		this.mDisplayHomeAsUpEnabled = mDisplayHomeAsUpEnabled;
	}

	@Override
	protected void onResume() {
		LocalSettings.disableScreenRotate(this);
		super.onResume();
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (mOnToolbarListener != null &&
				event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			if (!event.isCanceled()) {
				if (event.getAction() == KeyEvent.ACTION_UP && mIsBackKeyDown) {
					mIsBackKeyDown = false;
					mOnToolbarListener.onNavigationBackClick();
				} else if (event.getAction() == KeyEvent.ACTION_DOWN
						&& event.getRepeatCount() == 0) {
					mIsBackKeyDown = true;
				}
			}
			return true;
		}
		return super.dispatchKeyEvent(event);
	}

	@Override
	public boolean onCreatePanelMenu(int featureId, Menu menu) {
		if (isChangingConfigurations() || isFinishing()) {
			return false;
		}
		return super.onCreatePanelMenu(featureId, menu);
	}

	public void setOnToolbarListener(OnToolbarListener mOnToolbarListener) {
		this.mOnToolbarListener = mOnToolbarListener;
	}

	public interface OnToolbarListener {
		void onNavigationBackClick();
	}
}

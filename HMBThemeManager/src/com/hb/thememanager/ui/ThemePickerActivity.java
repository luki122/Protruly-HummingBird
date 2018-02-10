package com.hb.thememanager.ui;

import java.io.File;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;

import com.hb.thememanager.ui.fragment.DirectoryFragment;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.FileUtils;

import hb.app.HbActivity;

import com.hb.thememanager.R;
public class ThemePickerActivity extends HbActivity implements DirectoryFragment.FileClickListener{
	private static final String ARG_CURRENT_PATH = "arg_title_state";
	private static final String START_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
	private static final int HANDLE_CLICK_DELAY = 150;
	private String mCurrentPath = START_PATH;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setHbContentView(R.layout.activity_file_picker);

        if (savedInstanceState != null) {
            mCurrentPath = savedInstanceState.getString(ARG_CURRENT_PATH);
        } else {
            initFragment();
        }

        updateTitle();
	}
	
	private void updateTitle(){
		getToolbar().setTitle(mCurrentPath);
	}
	
	@Override
	public void onNavigationClicked(View view) {
		// TODO Auto-generated method stub
		onBackPressed();
	}

	 private void initFragment() {
	        getFragmentManager().beginTransaction()
	                .add(R.id.container, DirectoryFragment.getInstance(START_PATH))
	                .commitAllowingStateLoss();
	    }
	 
	 
	 private void addFragmentToBackStack(String path) {
	        getFragmentManager().beginTransaction()
	                .replace(R.id.container, DirectoryFragment.getInstance(path))
	                .addToBackStack(null)
	                .commitAllowingStateLoss();
	    }

	 @Override
	    public void onSaveInstanceState(Bundle outState) {
	        super.onSaveInstanceState(outState);
	        outState.putString(ARG_CURRENT_PATH, mCurrentPath);
	    }

	    @Override
	    public void onFileClicked(final File clickedFile) {
	        new Handler().postDelayed(new Runnable() {
	            @Override
	            public void run() {
	                handleFileClicked(clickedFile);
	            }
	        }, HANDLE_CLICK_DELAY);
	    }

	    private void handleFileClicked(final File clickedFile) {
			if(isDestroyed()){
				return;
			}
	        if (clickedFile.isDirectory()) {
	            addFragmentToBackStack(clickedFile.getPath());
	            mCurrentPath = clickedFile.getPath();
	            updateTitle();
	        } else {
	            setResultAndFinish(clickedFile.getPath());
	        }
	    }

	    private void setResultAndFinish(String filePath) {
	        Intent data = new Intent();
	        data.putExtra(Config.KEY_PICK_THEME_FILE_PATH, filePath);
	        setResult(RESULT_OK, data);
	        finish();
	    }
	 
    @Override
    public void onBackPressed(){
        FragmentManager fm = getFragmentManager();

        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
            mCurrentPath = FileUtils.cutLastSegmentOfPath(mCurrentPath);
            updateTitle();
        } else {
            setResult(RESULT_CANCELED);
            super.onBackPressed();
        }
    }

}

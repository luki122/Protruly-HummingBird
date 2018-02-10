package com.hb.thememanager.ui.fragment;

import hb.utils.DisplayUtils;
import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;

import com.hb.imageloader.HbImageLoader;
import com.hb.imageloader.ImageLoaderConfig;
import com.hb.thememanager.listener.FragmentKeyListener;
import com.hb.thememanager.listener.OnKeyPressListener;
import com.hb.thememanager.ui.MainActivity;
import com.hb.thememanager.utils.TLog;
import com.hb.thememanager.R;
public abstract class AbsThemeFragment extends Fragment implements OnItemLongClickListener,FragmentKeyListener{
	
	
    private static final String TAG = "ThemeFragment";
    
    private Bundle mStartArgs;
    
    private volatile boolean mEditMode = false;

	private HbImageLoader mImageLoader;
	private ImageLoaderConfig mImageLoaderConfig;
	private int mImageWidth,mImageHeight;
	protected boolean mSelectAllState = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	// TODO Auto-generated method stub
    	super.onCreate(savedInstanceState);
    	mStartArgs = getActivity().getIntent().getBundleExtra(MainActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
		mImageWidth = getContext().getResources().getDimensionPixelSize(R.dimen.size_image_item_default_width);
		mImageHeight = getContext().getResources().getDimensionPixelSize(R.dimen.size_image_item_default_height);
		configImageLoader();
    }

	private void configImageLoader(){
		mImageLoader = HbImageLoader.getInstance(getContext());
		mImageLoaderConfig = new ImageLoaderConfig();
		ImageLoaderConfig.Size size = new ImageLoaderConfig.Size(mImageWidth,mImageHeight);
		mImageLoaderConfig.setDecodeFormat(Bitmap.Config.ARGB_8888);//Default is ARGB_8888
		initialImageLoader(size,mImageLoaderConfig);
		mImageLoaderConfig.setSize(size);
		mImageLoader.setConfig(mImageLoaderConfig);

	}


	protected void initialImageLoader(ImageLoaderConfig.Size size,ImageLoaderConfig config){

	}

	public HbImageLoader getImageLoader(){
		return mImageLoader;
	}
    
    public Bundle getBundle(){
    	return mStartArgs;
    }
    
    
    @Override
    public boolean isEditMode(){
    	return mEditMode;
    }
    
    protected void enterEditMode(int position){
    	mEditMode = true;
		mSelectAllState = false;
    	handleEditMode(mEditMode,position);
    }
    
    protected void exitEditMode(){
		mSelectAllState = false;
    	mEditMode = false;
    	handleEditMode(mEditMode,-1);
    }
    
    protected  void handleEditMode(boolean editMode,int position){
    	//for subclass handler edit mode
    }
    protected abstract void initView();

	public boolean startFragment(Fragment caller, String fragmentClass,boolean addToBackStack, int titleRes,
            int requestCode, Bundle extras) {
       return startFragment(caller, fragmentClass, addToBackStack, getResources().getString(titleRes), requestCode, extras);
    }
	
	public boolean startFragment(Fragment caller, String fragmentClass,boolean addToBackStack, CharSequence title,
            int requestCode, Bundle extras) {
        final Activity activity = getActivity();
        if (activity instanceof MainActivity) {
        	MainActivity ma = (MainActivity) activity;
            ma.startThemePanel(fragmentClass, extras,addToBackStack, title,  caller, requestCode);
            return true;
        } else {
            Log.w(TAG,
                    "Parent isn't MainActivity , thus there's no way to "
                    + "launch the given Fragment (name: " + fragmentClass
                    + ", requestCode: " + requestCode + ")");
            return false;
        }
    }
	
	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position,
			long id) {
		if(!isEditMode()){
			enterEditMode(position);
		}
		return isEditMode();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return false;
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK && isEditMode()){
			exitEditMode();
			return true;
		}
		return false;
	}
	
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		if(isEditMode()){
			exitEditMode();
		}
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		if(getActivity() instanceof  MainActivity){
			((MainActivity)getActivity()).setFragmentKeyListener(null);
		}
	}
}

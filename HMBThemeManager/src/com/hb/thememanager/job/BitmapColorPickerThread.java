package com.hb.thememanager.job;

import java.lang.ref.WeakReference;

import com.hb.thememanager.utils.BitmapUtils;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

/**
 * Extract the main color from the target image.but just used for the situation
 * that the widget above bitmap needs update color.
 *
 */
public class BitmapColorPickerThread extends AsyncTask<Void, Integer, Integer>{

	private WeakReference<ImageView> mTargetImageView;
	private OnColorPickerListener mListener;
	private Bitmap mDrawable;
	private volatile int mPosition = -1;
	
	/**
	 *Callback for pick color from bitmap
	 */
	public interface OnColorPickerListener{
		/**
		 * @param color  color picked from bitmap
		 */
		public void onColorPicked(int color,int position);
	}
	
	public void setOnColorPickerListener(OnColorPickerListener listener){
		mListener = listener;
	}
	public  BitmapColorPickerThread(ImageView imageView) {
		// TODO Auto-generated constructor stub
		this(imageView,-1);
	}
	
	public  BitmapColorPickerThread(ImageView imageView,int position) {
		// TODO Auto-generated constructor stub
		mTargetImageView = new WeakReference<ImageView>(imageView);
		mPosition = position;
	}
	
	@Override
	protected void onPreExecute() {
		// TODO Auto-generated method stub
		super.onPreExecute();
		ImageView imageView = mTargetImageView.get();
		if(imageView != null){
			final Drawable imageDrawable = imageView.getDrawable();
			if(imageDrawable != null){
				mDrawable = BitmapUtils.drawable2bitmap(imageDrawable);
			}
		}
	}
	
	@Override
	protected Integer doInBackground(Void... params) {
		// TODO Auto-generated method stub
		if(mDrawable == null){
			return Color.WHITE;
		}
		final int color = BitmapUtils.calcTextColor(mDrawable);
		mDrawable.recycle();
		mDrawable = null;
		return color;
	}

	
	@Override
	protected void onPostExecute(Integer result) {
		// TODO Auto-generated method stub
		super.onPostExecute(result);
		if(mListener != null){
			mListener.onColorPicked(result.intValue(),mPosition);
		}
	}
	
	
	
}

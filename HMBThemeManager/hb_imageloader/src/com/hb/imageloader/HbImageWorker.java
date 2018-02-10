/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hb.imageloader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import com.android.gallery3d.exif.ExifInterface;
import com.hb.thememanager.security.MD5Utils;

public abstract class HbImageWorker {
    private static final String TAG = "HbImageWorker";
    private static final int FADE_IN_TIME = 200;

    private ImageCache mImageCache;
    private static ImageCache.ImageCacheParams sImageCacheParams;
    private boolean mFadeInBitmap = false;
    private boolean mExitTasksEarly = false;
    protected boolean mPauseWork = false;
    private boolean mCropNail = false;
    private final Object mPauseWorkLock = new Object();


    private static final int MESSAGE_CLEAR = 0;
    private static final int MESSAGE_INIT_DISK_CACHE = 1;
    private static final int MESSAGE_FLUSH = 2;
    private static final int MESSAGE_CLOSE = 3;
    
    private static final int MESSAGE_MEMORY_CLEAR = 4;
    public ImageLoaderCallback mImageLoaderCallback;
    private String updateCache = null;

    private Resources mResources;
    private ImageLoaderConfig mConfig;

    protected HbImageWorker(Context context) {
        mResources = context.getResources();
        addImageCache();
    }

    public void loadImage(String url, ImageView imageView) {

        loadImage(url, imageView, -1);
    }
    

    public void loadImage(String url, ImageView imageView, int position) {

       // Log.d("image","has cache->"+(mImageCache != null));

        if (url == null || imageView == null) {
            if (mImageLoaderCallback != null && position > -1) {
                mImageLoaderCallback.onImageLoadFailed(position);
            }
            return;
        }
        
        BitmapDrawable value = null;
        
        if (mImageCache != null) {
            value = mImageCache.getBitmapFromMemCache(getCacheName(url));
        }
        
        if (value != null) {
            imageView.setImageDrawable(value);
            if (mImageLoaderCallback != null && position > -1) {
                mImageLoaderCallback.onImageLoad(true, position);
            }
        } else if (cancelPotentialWork(url, imageView)) {
            final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
            if (position > -1) {
                task.setPosition(position);
            }
            final AsyncDrawable asyncDrawable =
                    new AsyncDrawable(imageView.getResources(), getLoadingBitmap(), task);
            imageView.setImageDrawable(asyncDrawable);
            task.executeOnExecutor(AsyncTask.DUAL_THREAD_EXECUTOR, url);
        }
    }


    public void setConfig(ImageLoaderConfig config){
        mConfig = config;
    }

    public ImageLoaderConfig getConfig(){
        return mConfig;
    }


    public static void setupImageCacheParam(ImageCache.ImageCacheParams params){
        sImageCacheParams = params;
    }
    



    public void addImageCache() {
        mImageCache = ImageCache.getInstance(sImageCacheParams);
        new CacheAsyncTask().execute(MESSAGE_INIT_DISK_CACHE);
    }
    
    public void addImageCache(String updateName) {
    	updateCache = updateName;
      mImageCache = ImageCache.getInstance(sImageCacheParams);
      new CacheAsyncTask().execute(MESSAGE_INIT_DISK_CACHE);
    }
    
    
	public void removeImageCache(Context context, String path) {
		if (mImageCache != null) {
			if (mImageCache.hasMemKey(path)) {
				mImageCache.removeMemCache(path);
			}
		}
	}
	

	protected Bitmap.Config getDecodeFormat(){
        if(mConfig == null){
            return Bitmap.Config.ARGB_8888;
        }
		return mConfig.getDecodeFormat();
	}

	protected ImageLoaderConfig.Size getSize(){
        return mConfig.getSize();
    }
	
	public void setCropNail(boolean cropNail) {
        mCropNail = cropNail;
    }
	
    public void setImageFadeIn(boolean fadeIn) {
        mFadeInBitmap = fadeIn;
    }

    public void setExitTasksEarly(boolean exitTasksEarly) {
        mExitTasksEarly = exitTasksEarly;
        setPauseWork(false);
    }

    protected abstract Bitmap processBitmap(String url);



    public int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    private  Bitmap decodeSampledBitmapFromDescriptor(FileDescriptor fileDescriptor) {
        if(fileDescriptor == null){
            return null;
        }
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);

        options.inSampleSize = calculateInSampleSize(options, getSize().width, getSize().height);
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inPremultiplied = false;
        options.inDither = false;
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);	//bug-5577 java.lang.OutOfMemoryError
    }


    protected ImageCache getImageCache() {
        return mImageCache;
    }

    public static void cancelWork(ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
        if (bitmapWorkerTask != null) {
            bitmapWorkerTask.cancel(true);
        }
    }

    public static boolean cancelPotentialWork(String url, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final String bitmapData = bitmapWorkerTask.url;
            if (bitmapData == null || !bitmapData.equals(url)) {
                bitmapWorkerTask.cancel(true);
            } else {
                return false;
            }
        }
        return true;
    }

    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }
    
    protected int getReplaceHoldDrawable(){
    	
    	return mConfig.getReplaceHold();
    }

    protected Bitmap getLoadingBitmap(){

        return mConfig.getLoadingDrawable();
    }



    protected int getErrorResId(){
    	
    	return mConfig.getErrorDrawable();
    }



    private Bitmap getBitmapFromDiskCache(String fileName){

        return decodeSampledBitmapFromDescriptor(mImageCache.getBitmapFromDiskCache(fileName));
    }

    private class BitmapWorkerTask extends AsyncTask<String, Void, BitmapDrawable> {
        private String url;
        private final WeakReference<ImageView> imageViewReference;
        int mPosition = -1;
        public BitmapWorkerTask(ImageView imageView) {
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        @Override
        protected void onPreExecute() {
        	// TODO Auto-generated method stub
        	super.onPreExecute();
        	if(getReplaceHoldDrawable() != 0){
        		final ImageView imageView = imageViewReference.get();
        		if(imageView != null){
        			imageView.setImageResource(getReplaceHoldDrawable());
        		}
        	}
        }
        
        
        @Override
        protected BitmapDrawable doInBackground(String... params) {

            url = params[0];
            final String dataString = url;
            Bitmap bitmap = null;
            RecyclingBitmapDrawable drawable = null;

            // Wait here if work is paused and the task is not cancelled
            synchronized (mPauseWorkLock) {
            	Log.d(TAG, mPauseWork+"==BitmapWorkerTask: "+isCancelled());
                while (mPauseWork && !isCancelled()) {
                    try {
                        mPauseWorkLock.wait();
                    } catch (InterruptedException e) {}
                }
            }
            String cacheDataString = getCacheName(dataString);
            if (mImageCache != null && !isCancelled() && getAttachedImageView() != null
                    && !mExitTasksEarly) {
                bitmap = getBitmapFromDiskCache(cacheDataString);
            }
            if (bitmap == null && !isCancelled() && getAttachedImageView() != null
                    && !mExitTasksEarly) {
            	try{
                bitmap = processBitmap(url);
            	}catch(Exception e){
            		
            	}
            }
            if (bitmap != null) {
                    drawable = new RecyclingBitmapDrawable(mResources, bitmap);

                if (mImageCache != null) {
                    mImageCache.addBitmapToCache(cacheDataString, drawable);
                }
            }else {
            	bitmap = processBitmap(url);
            	drawable = new RecyclingBitmapDrawable(mResources, bitmap);
            	if (mImageCache != null) {
                    mImageCache.addBitmapToCache(cacheDataString, drawable);
                }
			}
            return drawable;
        }

        @Override
        protected void onPostExecute(BitmapDrawable value) {
            if (isCancelled() || mExitTasksEarly) {
                value = null;
            }
            
            if(value instanceof RecyclingBitmapDrawable){
            	final RecyclingBitmapDrawable verifyBitmap = (RecyclingBitmapDrawable)value;
            	if(!verifyBitmap.decodeSuccess()){
            		 if (mImageLoaderCallback != null && mPosition > -1) {
                         mImageLoaderCallback.onImageLoadFailed(mPosition);
                     }
            	}
            }

            final ImageView imageView = getAttachedImageView();
            if (value != null && imageView != null) {
                setImageDrawable(imageView, value);
                if (mImageLoaderCallback != null && mPosition > -1) {
                    mImageLoaderCallback.onImageLoad(true, mPosition);
                }
            } else if (value == null && imageView != null) {
            	Drawable drawable = null;
            		if(getErrorResId() != 0){
            			drawable = imageView.getResources().getDrawable(getErrorResId());
            		}
                setImageDrawable(imageView, drawable);
                if (mImageLoaderCallback != null && mPosition > -1) {
                    mImageLoaderCallback.onImageLoad(false,mPosition);
                }
            }
        }

        @Override
        protected void onCancelled(BitmapDrawable value) {
            super.onCancelled(value);
            synchronized (mPauseWorkLock) {
                mPauseWorkLock.notifyAll();
            }
        }

        private ImageView getAttachedImageView() {
            final ImageView imageView = imageViewReference.get();
            final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

            if (this == bitmapWorkerTask) {
                return imageView;
            }

            return null;
        }

        public void setPosition(int position) {
            mPosition = position;
        }
    }

    private static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference =
                new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    private void setImageDrawable(ImageView imageView, Drawable drawable) {
        if (mFadeInBitmap) {
            final TransitionDrawable td =
                    new TransitionDrawable(new Drawable[] {
                            new ColorDrawable(android.R.color.transparent),
                            drawable
                    });
            imageView.setBackgroundResource(getReplaceHoldDrawable());

            imageView.setImageDrawable(td);
            td.startTransition(FADE_IN_TIME);
        } else {
            imageView.setImageDrawable(drawable);
        }
    }

    public void setPauseWork(boolean pauseWork) {
        synchronized (mPauseWorkLock) {
            mPauseWork = pauseWork;
            if (!mPauseWork) {
                mPauseWorkLock.notifyAll();
            }
        }
    }

    protected class CacheAsyncTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            switch ((Integer)params[0]) {
                case MESSAGE_CLEAR:
                    clearCacheInternal();
                    break;
                case MESSAGE_INIT_DISK_CACHE:
                    initDiskCacheInternal();
                    break;
                case MESSAGE_FLUSH:
                    flushCacheInternal();
                    break;
                case MESSAGE_CLOSE:
                    closeCacheInternal();
                    break;
                    
                case MESSAGE_MEMORY_CLEAR:
                	clearMemoryCacheInternal();
                	break;
            }
            return null;
        }
    }

    protected void initDiskCacheInternal() {
        if(mImageCache == null){
            addImageCache();
        }
        if (mImageCache != null) {
            mImageCache.initDiskCache(updateCache);
        }
    }

    protected void clearCacheInternal() {
        if(mImageCache == null){
            addImageCache();
        }
        if (mImageCache != null) {
            mImageCache.clearCache();
        }
    }
    
    protected void clearMemoryCacheInternal() {
        if (mImageCache != null) {
            mImageCache.clearMemoryCache();
        }
    }

    protected void flushCacheInternal() {
        if(mImageCache == null){
            addImageCache();
        }
        if (mImageCache != null) {
            mImageCache.flush();
        }
    }

    protected void closeCacheInternal() {
        if(mImageCache == null){
            addImageCache();
        }
        if (mImageCache != null) {
            mImageCache.close();
            mImageCache = null;
        }
    }
    
    public void clearMemoryCache() {
        new CacheAsyncTask().execute(MESSAGE_MEMORY_CLEAR);
    }

    public void clearCache() {
        new CacheAsyncTask().execute(MESSAGE_CLEAR);
    }

    public void flushCache() {
        new CacheAsyncTask().execute(MESSAGE_FLUSH);
    }

    public void closeCache() {
        new CacheAsyncTask().execute(MESSAGE_CLOSE);
    }

    public void setImageLoaderCallback(ImageLoaderCallback callback) {
        mImageLoaderCallback = callback;
    }


    public interface ImageLoaderCallback {
        void onImageLoad(boolean success, int position);
        void onImageLoadFailed(int position);
    }

    private static int getRotationFromExif(Context context, String path) {
    	try{
        return getRotationFromExifHelper(path, null, 0, context, null);
    	}catch(Exception e){
    		return 0;
    	}
    }

    private static int getRotationFromExifHelper(
            String path, Resources res, int resId, Context context, Uri uri) throws IOException{
        ExifInterface ei = new ExifInterface();
            if (path != null) {
                ei.readExif(path);
            } else if (uri != null) {
                InputStream is = context.getContentResolver().openInputStream(uri);
                BufferedInputStream bis = new BufferedInputStream(is);
                ei.readExif(bis);
            } else {
                InputStream is = res.openRawResource(resId);
                BufferedInputStream bis = new BufferedInputStream(is);
                ei.readExif(bis);
            }
            Integer ori = ei.getTagIntValue(ExifInterface.TAG_ORIENTATION);
            if (ori != null) {
                return ExifInterface.getRotationForOrientationValue(ori.shortValue());
            }
      
        return 0;
    }
    
    private String getCacheName(String filename) {
    	StringBuffer sb = new StringBuffer();
            sb.append(MD5Utils.encryptString(filename));
            if(mConfig != null){
                sb.append(mConfig.getSize().width);
                sb.append(mConfig.getSize().height);
            }
    	return sb.toString();
    }
}

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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Thumbnails;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class HbImageLoader extends HbImageWorker {
    private static final String TAG = "HbImageLoader";
    private static final String CONTENT = "content://";
    private static final String ASSERT = "file:///android_asset/";
    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    protected int mImageWidth;
    protected int mImageHeight;
    public static int sCacheWidth;
    public static int sCacheHeight;
    public boolean mIsThumb;
    public static boolean sIsThumb;

    private Context mContext;

    private static HbImageLoader sInstance;
    public static HbImageLoader getInstance(Context context){
//        synchronized (HbImageLoader.class){
//            if(sInstance == null){
//                sInstance = new HbImageLoader(context);
//            }
//            return sInstance;
//        }
        return new HbImageLoader(context);
    }


    
    private HbImageLoader(Context context){
    	super(context);
    	  mContext = context;
    }
    





    @Override
    protected Bitmap processBitmap(String url) {
    	if(isContent(url)){
    		return decodeSampledBitmapFromUrl(url,  mContext);
    	}else if(isAsset(url)){
            return decodeSampledBitmapFromAsset(mContext,url);
        }else if(isHttpOrHttps(url)){
            return decodeSampledBitmapFromHttp(url,getImageCache());
        }else{
    		return decodeSampledBitmapFromDescriptor(url);
    	}
    }

    private Bitmap decodeSampledBitmapFromHttp(String url, ImageCache imageCache) {

        return null;
    }

    private boolean isContent(String url){
        return url.startsWith(CONTENT);
    }

    private boolean isHttpOrHttps(String url){

        return url.startsWith(HTTP) || url.startsWith(HTTPS);
    }

    private boolean isAsset(String url){
        return url.startsWith(ASSERT);
    }

    private Bitmap decodeSampledBitmapFromAsset(Context context, String fileName) {
        AssetManager asset = mContext.getAssets();
        final String realName = fileName.replace(ASSERT,"");
        InputStream input = null;
        try{
            input = asset.open(realName);
            if(input != null){
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = calculateInSampleSize(options, getSize().width, getSize().height);
                options.inDither = false;
                options.inPurgeable = true;
                options.inInputShareable = true;
                options.inPreferredConfig = getDecodeFormat();
                options.inJustDecodeBounds = false;
                Rect outRect = new Rect();
                outRect.top = 0;
                outRect.bottom = getSize().height;
                outRect.left = 0;
                outRect.right = getSize().width ;

                return BitmapFactory.decodeStream(input,outRect,options);
            }
        }catch (Exception e){

        }finally {
            if(input != null){
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;

    }

    public  Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth,
            int reqHeight, ImageCache cache) {

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inDither = false;
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inPreferredConfig = getDecodeFormat();
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public  Bitmap decodeSampledBitmapFromFile(String filename, int reqWidth, int reqHeight,
            ImageCache cache) {

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inDither = false;
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inPreferredConfig = getDecodeFormat();


        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filename, options);
    }
    
    
    
    public  Bitmap decodeSampledBitmapFromUrl(String filename,Context context) {

    	Uri uri = Uri.parse(filename);
    	InputStream inputStream = null;
    	InputStream realStream = null;
    	if(uri != null){
    		try{
    			inputStream = context.getContentResolver().openInputStream(uri);
    			realStream = context.getContentResolver().openInputStream(uri);
    		}catch(Exception e){
    			
    		}
    	}
    	if(inputStream == null){
    		return null;
    	}
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, options);
        options.inSampleSize = calculateInSampleSize(options, getSize().width, getSize().getHeight());
        options.inDither = false;
        options.inPreferredConfig = getDecodeFormat();

        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeStream(realStream, null, options);
    }
    
    
    public  Bitmap decodeSampledBitmapFromDescriptor(String filename) {

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        File file = new File(filename);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if(fis!=null){
        	try {
				BitmapFactory.decodeFileDescriptor(fis.getFD(), null, options);
			} catch (IOException e) {
				e.printStackTrace();
			}
        }

        options.inSampleSize = calculateInSampleSize(options, getSize().width, getSize().getHeight());
        options.inPremultiplied = false;
        options.inDither = false;
        options.inPreferredConfig = getDecodeFormat();


        options.inJustDecodeBounds = false;
        Bitmap bm = null;
        try {
            if(fis!=null) bm = BitmapFactory.decodeFileDescriptor(fis.getFD(), null, options);
        } catch (IOException e) {
            e.printStackTrace();
        } finally{ 
            if(fis!=null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bm;
    }



	
}

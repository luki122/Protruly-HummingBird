package com.dui.systemui.statusbar.image.factory;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by chenheliang on 17-6-8.
 */

public class HbBitmapRegionDecoder {

    private  BitmapRegionDecoder mDecoder;
    private  Bitmap mBitmap;

    private static HbBitmapRegionDecoder mHbBitmapRegionDecoder;

    public static HbBitmapRegionDecoder newInstance(InputStream is,
                                                  boolean isShareable) throws IOException {
        if(mHbBitmapRegionDecoder==null) {
            mHbBitmapRegionDecoder = new HbBitmapRegionDecoder();
        }
        if(mHbBitmapRegionDecoder.mBitmap!=null&&!mHbBitmapRegionDecoder.mBitmap.isRecycled()){
            mHbBitmapRegionDecoder.mBitmap.recycle();
            mHbBitmapRegionDecoder.mBitmap=null;
        }
        mHbBitmapRegionDecoder.mDecoder = BitmapRegionDecoder.newInstance(is,isShareable);
        return mHbBitmapRegionDecoder;
    }

    public static HbBitmapRegionDecoder newInstance(String pathName,
                                                  boolean isShareable) throws IOException {

        if(mHbBitmapRegionDecoder==null) {
            mHbBitmapRegionDecoder = new HbBitmapRegionDecoder();
        }
        if(mHbBitmapRegionDecoder.mBitmap!=null&&!mHbBitmapRegionDecoder.mBitmap.isRecycled()){
            mHbBitmapRegionDecoder.mBitmap.recycle();
            mHbBitmapRegionDecoder.mBitmap=null;
        }
        mHbBitmapRegionDecoder.mDecoder = BitmapRegionDecoder.newInstance(pathName,isShareable);
        return mHbBitmapRegionDecoder;
    }

    public static HbBitmapRegionDecoder newInstance(Bitmap bitmap) {
        if(mHbBitmapRegionDecoder==null) {
            mHbBitmapRegionDecoder = new HbBitmapRegionDecoder();
        }
        mHbBitmapRegionDecoder.mDecoder=null;
        if(mHbBitmapRegionDecoder.mBitmap!=null&&!mHbBitmapRegionDecoder.mBitmap.isRecycled()){
            mHbBitmapRegionDecoder.mBitmap.recycle();
            mHbBitmapRegionDecoder.mBitmap=null;
        }
        mHbBitmapRegionDecoder.mBitmap = bitmap;
        return mHbBitmapRegionDecoder;
    }

    public Bitmap decodeRegion(Rect rect, BitmapFactory.Options options) {

        if(mDecoder!=null){
            return mDecoder.decodeRegion(rect,options);
        }else {
            return Bitmap.createBitmap(mBitmap,rect.left,rect.top,rect.width(),rect.height());
        }
    }

    public int getWidth() {
        if(mDecoder!=null){
            return mDecoder.getWidth();
        }else {
            return mBitmap.getWidth();
        }
    }

    public int getHeight() {
        if(mDecoder!=null){
            return mDecoder.getHeight();
        }else {
            return mBitmap.getHeight();
        }
    }
}

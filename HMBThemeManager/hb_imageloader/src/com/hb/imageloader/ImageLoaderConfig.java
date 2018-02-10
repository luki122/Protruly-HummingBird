package com.hb.imageloader;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.InputStream;


public class ImageLoaderConfig {



    private Bitmap.Config mDecodeFormat = Bitmap.Config.ARGB_8888;

    private Size mSize;


    private int mErrorDrawableResId;

    private Bitmap mLoadingDrawable;

    private int mReplaceHoldResId;

    private boolean mIsThumb;




    public ImageLoaderConfig setDecodeFormat(Bitmap.Config format){
        this.mDecodeFormat = format;
        return this;
    }


    public ImageLoaderConfig setSize(Size size){
        this.mSize = size;
        return this;
    }



    public ImageLoaderConfig setErrorDrawable(int resId){
        this.mErrorDrawableResId = resId;
        return this;
    }

    public ImageLoaderConfig setLoadingDrawable(Resources res,int resId){
        InputStream in = res.openRawResource(resId);
        try{
            mLoadingDrawable = BitmapFactory.decodeStream(in);
        }catch (Exception e){
            //do nothing
        }finally {
            if(in != null){
                try{
                    in.close();
                }catch (Exception e){

                }
            }
        }
        return this;
    }

    public ImageLoaderConfig setIsThumb(boolean isThumb){
        mIsThumb = isThumb;
        return this;
    }

    public ImageLoaderConfig setReplaceHoldDrawable(int resId){
        mReplaceHoldResId = resId;
        return this;
    }

    public int getReplaceHold(){
        return mReplaceHoldResId;
    }






    public Size getSize(){
        return mSize;
    }

    Bitmap.Config getDecodeFormat(){
        return mDecodeFormat;
    }




    int getErrorDrawable(){
        return mErrorDrawableResId;
    }

    Bitmap getLoadingDrawable(){
        return mLoadingDrawable;
    }


    boolean isThumb(){
        return mIsThumb;
    }




    /**
     * Size for loaded bitmap
     */
    public static class Size{
        public int width,height;

        public Size(int width,int height){
            this.width = width;
            this.height = height;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        @Override
        public String toString() {
            return "Size{" +
                    "width=" + width +
                    ", height=" + height +
                    '}';
        }
    }



}

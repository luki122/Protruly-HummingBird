package com.dui.systemui.statusbar.image;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;

import com.dui.systemui.statusbar.image.factory.BitmapDecoderFactory;


/**
 * Created by chenheliang on 17-6-7.
 */

public interface ILargeImageView {
    int getImageWidth();

    int getImageHeight();

    boolean hasLoad();

    void setOnImageLoadListener(BlockImageLoader.OnImageLoadListener onImageLoadListener);

    void setImage(BitmapDecoderFactory factory);

    void setImage(BitmapDecoderFactory factory, Drawable defaultDrawable);

    void setImage(Bitmap bm);

    void setImage(Drawable drawable);

    void setImage(@DrawableRes int resId);

    void setImageDrawable(Drawable drawable);

    void setScale(float scale);

    float getScale();

    BlockImageLoader.OnImageLoadListener getOnImageLoadListener();
}

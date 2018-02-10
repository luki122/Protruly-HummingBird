package com.dui.systemui.statusbar.image.factory;

import android.graphics.Bitmap;

import java.io.IOException;

/**
 * Created by chenheliang on 17-6-8.
 */

public class HbBitmapDecoderFactory implements BitmapDecoderFactory{

    private Bitmap mBitmap;


    public HbBitmapDecoderFactory(Bitmap bitmap){
        mBitmap = bitmap;
    }

    @Override
    public HbBitmapRegionDecoder made() throws IOException {
        return HbBitmapRegionDecoder.newInstance(mBitmap);
    }
}

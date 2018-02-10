package com.dui.systemui.statusbar.image.factory;

import android.graphics.BitmapRegionDecoder;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by chenheliang on 17-6-7.
 */

public class InputStreamBitmapDecoderFactory implements BitmapDecoderFactory{
    private InputStream inputStream;

    public InputStreamBitmapDecoderFactory(InputStream inputStream) {
        super();
        this.inputStream = inputStream;
    }

    @Override
    public HbBitmapRegionDecoder made() throws IOException {
        return HbBitmapRegionDecoder.newInstance(inputStream, false);
    }
}

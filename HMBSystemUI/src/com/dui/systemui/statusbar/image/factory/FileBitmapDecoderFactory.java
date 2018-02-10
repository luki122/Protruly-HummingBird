package com.dui.systemui.statusbar.image.factory;

import java.io.File;
import java.io.IOException;

/**
 * Created by chenheliang on 17-6-7.
 */

public class FileBitmapDecoderFactory implements BitmapDecoderFactory{
    private String path;

    public FileBitmapDecoderFactory(String filePath) {
        super();
        this.path = filePath;
    }

    public FileBitmapDecoderFactory(File file) {
        super();
        this.path = file.getAbsolutePath();
    }

    @Override
    public HbBitmapRegionDecoder made() throws IOException {
        return HbBitmapRegionDecoder.newInstance(path, false);
    }
}

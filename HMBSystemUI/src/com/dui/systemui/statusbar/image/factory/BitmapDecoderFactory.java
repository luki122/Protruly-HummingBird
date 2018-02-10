package com.dui.systemui.statusbar.image.factory;

import java.io.IOException;

/**
 * Created by chenheliang on 17-6-7.
 */

public interface BitmapDecoderFactory {
    HbBitmapRegionDecoder made() throws IOException;
}

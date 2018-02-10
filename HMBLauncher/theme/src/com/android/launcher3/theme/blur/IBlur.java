package com.android.launcher3.theme.blur;

import android.content.Context;
import android.graphics.Bitmap;

/**
 * Created by antino on 16-8-17.
 */
public interface IBlur {
     Bitmap blur(Context context, Bitmap source, float scaleFactor, int radius);
}

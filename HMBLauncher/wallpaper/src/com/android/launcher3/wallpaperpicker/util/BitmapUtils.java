/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.launcher3.wallpaperpicker.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;

import com.android.launcher3.wallpaperpicker.exif.ExifInterface;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BitmapUtils {

    private static final String TAG = "BitmapUtils";

    public static Bitmap drawableToBitmap(Drawable drawable) {
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ?
                Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;

        Bitmap bitmap = Bitmap.createBitmap(w, h, config);
        Canvas canvas = new Canvas(bitmap);

        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);

        return bitmap;
    }

    public static Bitmap roundCorners(Bitmap source, float radius) {
        int width = source.getWidth();
        int height = source.getHeight();

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(android.graphics.Color.WHITE);

        Bitmap clipped = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(clipped);
        canvas.drawRoundRect(new RectF(0, 0, width, height), radius, radius, paint);
        // 遮住下圆角
        // canvas.drawRect(new Rect(0, height/2, width, height), paint);

        paint.setXfermode(new PorterDuffXfermode(
                android.graphics.PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(source, 0, 0, paint);

        source.recycle();

        return clipped;
    }

    // Find the min x that 1 / x >= scale
    public static int computeSampleSizeLarger(float scale) {
        int initialSize = (int) Math.floor(1f / scale);
        if (initialSize <= 1) return 1;

        return initialSize <= 8
                ? Utils.prevPowerOf2(initialSize)
                : initialSize / 8 * 8;
    }

    public static int getRotationFromExif(Context context, Uri uri) {
        return getRotationFromExifHelper(null, 0, context, uri);
    }

    public static int getRotationFromExif(Resources res, int resId) {
        return getRotationFromExifHelper(res, resId, null, null);
    }

    private static int getRotationFromExifHelper(Resources res, int resId, Context context, Uri uri) {
        ExifInterface ei = new ExifInterface();
        InputStream is = null;
        BufferedInputStream bis = null;
        try {
            if (uri != null) {
                is = context.getContentResolver().openInputStream(uri);
                bis = new BufferedInputStream(is);
                ei.readExif(bis);
            } else {
                is = res.openRawResource(resId);
                bis = new BufferedInputStream(is);
                ei.readExif(bis);
            }
            Integer ori = ei.getTagIntValue(ExifInterface.TAG_ORIENTATION);
            if (ori != null) {
                return ExifInterface.getRotationForOrientationValue(ori.shortValue());
            }
        } catch (IOException e) {
            Log.w(TAG, "Getting exif data failed", e);
        } catch (NullPointerException e) {
            // Sometimes the ExifInterface has an internal NPE if Exif data isn't valid
            Log.w(TAG, "Getting exif data failed", e);
        } finally {
            Utils.closeSilently(bis);
            Utils.closeSilently(is);
        }
        return 0;
    }

    ///M. ALPS02278410, Add this for mapping GL renderer.
    public static Bitmap resizeBitmapByScale(
            Bitmap bitmap, float scale, boolean recycle) {
        int width = Math.round(bitmap.getWidth() * scale);
        int height = Math.round(bitmap.getHeight() * scale);
        if (width == bitmap.getWidth()
                && height == bitmap.getHeight()) return bitmap;
        Bitmap target = Bitmap.createBitmap(width, height, getConfig(bitmap));
        Canvas canvas = new Canvas(target);
        canvas.scale(scale, scale);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        if (recycle) bitmap.recycle();
        return target;
    }

    private static Bitmap.Config getConfig(Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            config = Bitmap.Config.ARGB_8888;
        }
        return config;
    }
    ///M.
}

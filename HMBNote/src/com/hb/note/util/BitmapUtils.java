package com.hb.note.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.util.Log;

import java.io.IOException;

public class BitmapUtils {

    private static final String TAG = "BitmapUtils";

    public static int getImageDegrees(String imagePath) {
        int degrees = 0;
        try {
            if (imagePath.startsWith(Globals.FILE_PROTOCOL)) {
                imagePath = imagePath.substring(Globals.FILE_PROTOCOL_LENGTH);
            }
            ExifInterface exifInterface = new ExifInterface(imagePath);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degrees = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degrees = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degrees = 270;
                    break;
            }
        } catch (IOException e) {
            Log.e(TAG, "Can't get image's degrees!");
        }
        return degrees;
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        Bitmap rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (!bitmap.isRecycled()) {
            bitmap.recycle();
        }
        return rotatedBitmap;
    }

    public static Bitmap scaleBitmap(Bitmap bitmap, float scale) {
        if (scale == 1) {
            return bitmap;
        }
        if (scale > 4) {
            Log.w(TAG, "Image need zoom in over 4 times!");
            return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        Bitmap scaledBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (!bitmap.isRecycled()) {
            bitmap.recycle();
        }
        return scaledBitmap;
    }

    public static Bitmap scaleBitmap(Bitmap bitmap, int width) {
        int w = bitmap.getWidth();
        float scale = (float) width / w;
        Log.d(TAG, "w=" + w + ", h=" + bitmap.getHeight() + ", scale=" + scale);
        return scaleBitmap(bitmap, scale);
    }

    public static Bitmap scaleBitmap(Bitmap bitmap, int width, int height) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        float scale = Math.max((float) width / w, (float) height / h);
        return scaleBitmap(bitmap, scale);
    }

    public static Bitmap cropBitmap(Bitmap bitmap, int width, int height, boolean cropFromTop) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w > width || h > height) {
            Rect src = new Rect();
            if (w > width) {
                src.left = (w - width) / 2;
                src.right = w - src.left;
            } else {
                src.left = 0;
                src.right = w;
            }
            if (h > height) {
                if (cropFromTop) {
                    src.top = 0;
                    src.bottom = height;
                } else {
                    src.top = (h - height) / 2;
                    src.bottom = h - src.top;
                }
            } else {
                src.top = 0;
                src.bottom = h;
            }

            Rect dst = new Rect(0, 0, width, height);
            Bitmap targetBitmap = Bitmap.createBitmap(width, height, bitmap.getConfig());
            Canvas canvas = new Canvas(targetBitmap);
            canvas.drawBitmap(bitmap, src, dst, null);
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }
            return targetBitmap;
        }
        return bitmap;
    }

    public static Bitmap getBitmap(String imagePath, int width) {
        Options opts = new Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, opts);

        Log.d(TAG, "width=" + width + ", opts.outWidth=" +
                opts.outWidth + ", opts.outHeight=" + opts.outHeight);
        int scale = opts.outWidth / width;
        int degrees = getImageDegrees(imagePath);
        boolean rotated = degrees == 90 || degrees == 270;
        if (rotated) {
            scale = opts.outHeight / width;
        }
        if (scale < 1) {
            scale = 1;
        }
        Log.d(TAG, "degrees=" + degrees + ", scale=" + scale);

        opts.inJustDecodeBounds = false;
        opts.inSampleSize = scale;
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, opts);
        if (rotated) {
            bitmap = rotateBitmap(bitmap, degrees);
        }
        return scaleBitmap(bitmap, width);
    }

    public static Bitmap getBitmap(String imagePath, int width, int height) {
        Options opts = new Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, opts);

        float scale = Math.min(opts.outWidth / width, opts.outHeight / height);
        int degrees = getImageDegrees(imagePath);
        boolean rotated = degrees == 90 || degrees == 270;
        if (rotated) {
            scale = Math.min(opts.outHeight / width, opts.outWidth / height);
        }
        if (scale < 1) {
            scale = 1;
        }

        opts.inJustDecodeBounds = false;
        opts.inSampleSize = (int) scale;
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, opts);
        if (rotated) {
            bitmap = rotateBitmap(bitmap, degrees);
        }
        bitmap = scaleBitmap(bitmap, width, height);
        return cropBitmap(bitmap, width, height, false);
    }
}

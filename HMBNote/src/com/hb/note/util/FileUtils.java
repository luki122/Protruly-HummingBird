package com.hb.note.util;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileUtils {

    private static final String TAG = "FileUtils";

    private static final String DIR_IMAGE = "/Note/images";
    private static final String DIR_SHARE = "/Note/share";

    private static final String NAME_IMAGE = "IMG";
    private static final String NAME_SHARE = "Note_Share";

    private static final String SUFFIX = ".jpg";

    private static String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("_yyyyMMdd_HHmmss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private static String getImageName() {
        return NAME_IMAGE + getTime();
    }

    public static String getImagePath() {
        File dir = new File(Environment.getExternalStorageDirectory() + DIR_IMAGE);
        if (!dir.mkdirs() && !dir.exists()) {
            return null;
        }

        return dir.getAbsolutePath() + "/" + getImageName() + SUFFIX;
    }

    public static String getShareImagePath() {
        File dir = new File(Environment.getExternalStorageDirectory() + DIR_SHARE);
        if (!dir.mkdirs() && !dir.exists()) {
            return null;
        }

        return dir.getAbsolutePath() + "/" + NAME_SHARE + SUFFIX;
    }

    public static boolean writeToFile(Bitmap bitmap, File file) {
        if (file.exists()) {
            file.delete();
        }

        boolean result = false;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            result = true;
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
        } finally {
            bitmap.recycle();
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                Log.d(TAG, e.getMessage());
            }
        }
        return result;
    }

    // copy a file from srcFile to destFile.
    // Return true if succeed, return false if failed.
    public static boolean copyFile(File srcFile, File destFile) {
        try {
            InputStream in = new FileInputStream(srcFile);
            return copyToFile(in, destFile);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Copy data from a source stream to destFile.
     * Return true if succeed, return false if failed.
     */
    public static boolean copyToFile(InputStream inputStream, File destFile) {
        if (inputStream == null) {
            return false;
        }

        try {
            if (destFile.exists()) {
                destFile.delete();
            }
            FileOutputStream out = new FileOutputStream(destFile);
            try {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) >= 0) {
                    out.write(buffer, 0, bytesRead);
                }
            } finally {
                out.flush();
                try {
                    out.getFD().sync();
                } catch (IOException e) {
                }
                out.close();
                inputStream.close();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}

package cn.com.protruly.soundrecorder.util;

import android.content.Context;
import android.util.Log;

import java.io.File;
import cn.com.protruly.soundrecorder.R;

/**
 * Created by sqf on 17-9-20.
 */

public class FilePathUtil {

    public static File checkAndReture(Context context) {
        String text = context.getString(R.string.new_record);
        File recordDir = new File(GlobalUtil.getRecordDirPath());
        File file = new File(recordDir, text + ".mp3");
        int dirId = 1;
        while ((null != file) && file.exists() && !file.isDirectory()) {
            file = new File(recordDir, text + dirId + ".mp3");
            dirId++;
        }
        return file;
    }

    public static final String getName(String nameAndExtension) {
        int indexOfDot = nameAndExtension.indexOf(".");
        if(-1 == indexOfDot) return nameAndExtension;
        return nameAndExtension.substring(0, indexOfDot);
    }
}

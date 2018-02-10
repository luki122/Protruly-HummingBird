package cn.com.protruly.filemanager.utils;

import java.util.HashSet;

import cn.com.protruly.filemanager.enums.FileInfo;

/**
 * Created by sqf on 17-6-2.
 */

public class DebugUtil {

    private static final String TAG = "DebugUtil";

    public static void debugFileInfos(HashSet<FileInfo> fileInfos) {
        if(GlobalConstants.DEBUG) {
            if(fileInfos == null || fileInfos.isEmpty()) {
                LogUtil.e(TAG, "file ..............missing.........or not exists");
            }
            for (FileInfo fileInfo : fileInfos) {
                LogUtil.i(TAG, fileInfo.toString());
            }
        }
    }
}

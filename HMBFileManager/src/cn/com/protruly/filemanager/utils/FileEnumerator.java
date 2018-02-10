package cn.com.protruly.filemanager.utils;

/**
 * Created by sqf on 17-7-17.
 */

import java.io.File;

public class FileEnumerator implements FileEnumerationListener {

    private int mTotalFileNum;
    private int mEnumeratedFileNum;

    public FileEnumerator(int totalFileNum) {
        mTotalFileNum = totalFileNum;
    }

    @Override
    public void onFileEnumerated(File file) {
        mEnumeratedFileNum ++;
    }

    public boolean isEnumeationFinished() {
        return mEnumeratedFileNum == mTotalFileNum;
    }
}
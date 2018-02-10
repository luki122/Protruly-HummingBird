package cn.com.protruly.filemanager.categorylist;

import android.os.FileObserver;

/**
 * Created by sqf on 17-4-26.
 */

public class HmbFileObserver extends FileObserver {

    public HmbFileObserver(String path) {
        super(path);
    }

    public HmbFileObserver(String path, int mask) {
        super(path, mask);
    }

    @Override
    public void onEvent(int event, String path) {

    }
}

package cn.com.protruly.filemanager.utils;

import android.media.MediaScannerConnection;

/**
 * Created by sqf on 17-5-26.
 */

public interface MediaScannerFullyCompleteListener extends MediaScannerConnection.OnScanCompletedListener {
    public void onScanFullyCompleted(int totalNum);
}
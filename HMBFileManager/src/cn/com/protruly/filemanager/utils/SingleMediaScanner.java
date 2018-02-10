package cn.com.protruly.filemanager.utils;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

/**
 * Created by sqf on 17-5-26.
 */

public class SingleMediaScanner implements MediaScannerConnection.MediaScannerConnectionClient {

    private Context mContext;
    private MediaScannerConnection mConnection;
    private boolean mScanOnConnected;
    private Object mLock = new Object();
    private static final String TAG = "SingleMediaScanner";

    private HashSet<String> mFilePaths = new HashSet<String>();

    public SingleMediaScanner(Context context) {
        mContext = context;
        mConnection = new MediaScannerConnection(context, this);
    }

    public boolean isConnected() {
        return mConnection.isConnected();
    }

    public void connect() {
        mConnection.connect();
    }

    public void disconnect() {
        mConnection.disconnect();
        mScanOnConnected = false;
    }


    private void connect(boolean scanOnConnected) {
        mScanOnConnected = scanOnConnected;
        connect();
    }

    private void addScanInfo(String filePath) {
        synchronized (mFilePaths) {
            mFilePaths.add(filePath);
            if (GlobalConstants.DEBUG) {
                LogUtil.i(TAG, "SingleMediaScanner::addScanInfo file path added to scan info:" + filePath);
            }
        }
    }

    private void removeScanInfo(String filePath) {
        synchronized (mFilePaths) {
            mFilePaths.remove(filePath);
        }
    }

    public void scanSingleFile(String filePath) {
        if(!isConnected()) {
            addScanInfo(filePath);
            connect(true);
        } else {
            scan(filePath);
        }
    }

    private void scan(String filePath) {
        String mime = MediaFileUtil.getMimeTypeForFile(filePath);
        mConnection.scanFile(filePath, mime);
    }

    @Override
    public void onMediaScannerConnected() {
        if(!mScanOnConnected) return;
        synchronized (mFilePaths) {
            for (String filePath : mFilePaths) {
                scan(filePath);
                removeScanInfo(filePath);

                if (GlobalConstants.DEBUG) {
                    LogUtil.i(TAG, "SingleMediaScanner::onMediaScannerConnected file path added to scan info ");
                }
            }
        }
    }

    @Override
    public void onScanCompleted(String path, Uri uri) {
        if(uri != null) {
            LogUtil.i(TAG, "SingleMediaScanner::onScanCompleted SUCCEEDED path:" + path);
        } else {
            LogUtil.i(TAG, "SingleMediaScanner::onScanCompleted FAILED path:" + path);
        }
    }
}
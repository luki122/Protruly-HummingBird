package cn.com.protruly.filemanager.utils;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.com.protruly.filemanager.enums.FileInfo;

/**
 * Created by sqf on 17-5-31.
 */

public class MultiMediaScanner implements MediaScannerConnection.MediaScannerConnectionClient {

    private static final String TAG = "MultiMediaScanner";

    private Context mContext;
    private MediaScannerConnection mConnection;

    private int mTotalFileCount;
    private int mScannedCount;

    private ArrayList<String> mPathsToScan = new ArrayList<>();

    private MediaScannerFullyCompleteListener mListener;


    public MultiMediaScanner(Context context) {
        mContext = context;
    }

    /**
     * Please don't call it in for loop
     * if you need to call it in loop, call scanFiles instead.
     * scan only one file.
     * this function calls MediaScannerConnection.scanFile inside.
     * @param filePath
     * @param mime
     */
    public void scanFile(String filePath, String mime) {
        String [] filePaths = new String [] { filePath };
        String [] mimes = new String [] { mime };
        scanFiles(filePaths, mimes);
    }

    public void scanFiles(String [] filePaths, String [] mimes) {
        //if(filePaths.length != mimes.length) throw new IllegalArgumentException("unequal length");
        /*
        mFilePaths = filePaths;
        mMimes = mimes;
        */
        mScannedCount = 0;
        mTotalFileCount = filePaths.length;
        MediaScannerConnection.scanFile(mContext, filePaths, mimes, this);
    }

    @Override
    public void onMediaScannerConnected() {

    }

    @Override
    public void onScanCompleted(String path, Uri uri) {
        ++ mScannedCount;
        if(mScannedCount == mTotalFileCount) {
            mListener.onScanFullyCompleted(mTotalFileCount);
        }
    }

    private static final int SCAN_THRESHOLD = 500;
    /**
     * this function may takes a long time. use it in Thread or AsyncTask
     * ERROR: This function does not work well under MTP file viewing.
     *        Use FileDbManager instead.
     * @param fileInfo
     */
    public void scanFileInfo(FileInfo fileInfo) {
        if (fileInfo == null || fileInfo.getFile() == null) return;
        final ArrayList<String> paths = new ArrayList<String>();
        int totalNum = FilePathUtil.getAllFileAndDirectoryNum(fileInfo.getFile());
        FileEnumerator enumerator = new FileEnumerator(totalNum) {
            @Override
            public void onFileEnumerated(File file) {
                LogUtil.i(TAG, "onFileEnumerated file:" + file.getPath());
                super.onFileEnumerated(file);
                paths.add(file.getPath());
                LogUtil.i(TAG, "String path added:" + file.getPath());
                boolean reachThreshold = paths.size() >= SCAN_THRESHOLD;
                boolean finished = isEnumeationFinished();
                if(reachThreshold || finished) {
                    int len = paths.size();
                    String [] pathArray = new String [len];
                    paths.toArray(pathArray);
                    LogUtil.i(TAG, "reachThreshold:" + reachThreshold + " finished:" + finished + " scan files ..." + " pathArray is null:" + (pathArray == null));
                    LogUtil.i(TAG, "pathArray length:" + pathArray.length + " paths length:" + paths.size());
                    for(int i = 0; i < pathArray.length; i++) {
                        LogUtil.i(TAG, "" + paths.get(i));
                    }
                    scanFiles(pathArray, null);
                    paths.clear();
                }
            }
        };
        FilePathUtil.enumerateFile(fileInfo.getFile(), enumerator);
    }

}

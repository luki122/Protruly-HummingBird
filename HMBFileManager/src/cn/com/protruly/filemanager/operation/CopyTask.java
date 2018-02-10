package cn.com.protruly.filemanager.operation;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashSet;
import java.util.Iterator;

import cn.com.protruly.filemanager.enums.FileInfo;
import cn.com.protruly.filemanager.utils.FilePathUtil;
import cn.com.protruly.filemanager.utils.GlobalConstants;
import cn.com.protruly.filemanager.utils.LogUtil;
import cn.com.protruly.filemanager.utils.Util;

/**
 * Created by sqf on 17-5-13.
 */

public class CopyTask extends BaseOperationTask implements CopyHelper.CopyListener {

    private static final String TAG = "CopyTask";

    private HashSet<FileInfo> mFileInfos;
    private String mDestinationDirectory;
    private boolean mHasJustOneFile;

    //private static final int BUFFER_SIZE = 8192;
    private String mDiskPath;

    private int mCopiedCount;
    private int mTotalFileCount;
    /**
     * @param context Context
     * @param listener FileOperationTaskListener passed in
     * @param files FileInfos to be copied
     * @param destinationDirectory MUST BE somehting like this : "/sdcard/DCIM",
     *                             MUST NOT BE ended with "/"
     */
    public CopyTask(Context context, FileDbManager fileDbManager, FileOperationTaskListener listener,
                    HashSet<FileInfo> files, String destinationDirectory) {
        super(context, fileDbManager, OperationType.COPY, listener);
        mFileInfos = files;
        mDestinationDirectory = destinationDirectory;
        mOperationTaskListener = listener;
        mDiskPath = FilePathUtil.getCurrentDisk(context, destinationDirectory);
        mCopiedCount = 0;
        mHasJustOneFile = hasJustOneFile();
    }

    private boolean hasJustOneFile() {
        if(mFileInfos.size() != 1) return false;
        Iterator<FileInfo> it = mFileInfos.iterator();
        while (it.hasNext()) {
            FileInfo fileInfo = it.next();
            return fileInfo.isFile();
        }
        return false;
    }

    @Override
    protected FileOperationResult doOperation() {
        FileOperationResult result = new FileOperationResult();

        //judge whether has enough space for copying
        long totalFileSize = FilePathUtil.getFileSize(mFileInfos);
        long currentDiskFreeSize = Util.getFreeSize(mDiskPath);
        if(totalFileSize > currentDiskFreeSize) {
            return result.set(FileOperationResult.FORC_INSUFFICIENT_SPACE, null);
        }

        mTotalFileCount = FilePathUtil.getAllFileAndDirectoryNum(mFileInfos);
        if(0 == mTotalFileCount) {
            //if all FileInfo does not exist
            return result.set(FileOperationResult.FORC_FILE_NOT_EXISTS, null);
        }

        for(FileInfo fileInfo : mFileInfos) {
            if(isCancelled()) {
                return result.set(FileOperationResult.FORC_USER_CANCELLED, mDestinationDirectory);
            }
            if (fileInfo.isFile()) {
                result = copySingleFile(fileInfo.getPath(), mDestinationDirectory, result);
            } else if (fileInfo.isDirectory()) {
                result = copyDirectory(fileInfo.getPath(), mDestinationDirectory, result);
            }
        }
        mFileDbManager.insertFromContentValueList();
        return result;
    }

    private FileOperationResult copySingleFile(String srcFilePath, String destinationDir, FileOperationResult result) {
        File srcFile = new File(srcFilePath);
        return copySingleFile(srcFile, destinationDir, result);
    }

    private FileOperationResult copySingleFile(File srcFile, String destinationDir, FileOperationResult result) {
        String fileName = srcFile.getName();
        String destinationFilePath = destinationDir + FilePathUtil.PATH_SEPARATOR + fileName;
        destinationFilePath = FilePathUtil.generateFilePathWhenExists(destinationFilePath);
        FilePathUtil.createNewFile(destinationFilePath);

        long freeSize = Util.getFreeSize(mDiskPath);
        if(srcFile.length() > freeSize) {
            return result.set(FileOperationResult.FORC_INSUFFICIENT_SPACE, null);
        }
        //LogUtil.i(TAG, "copySingleFile srcFile:" + srcFile.getPath() + " to dir: " + destinationDir);
        int copyResult = CopyHelper.copySingleFile(srcFile, destinationFilePath, mFileDbManager, this);
        if(copyResult != FileOperationResult.FORC_SUCCEEDED) {
            //LogUtil.i(TAG, "copySingleFile NOT SUCCEEDED copyResult:" + copyResult);
            return result.set(copyResult, destinationFilePath);
        }
        mCopiedCount ++;
        //LogUtil.i(TAG, "copySingleFile SUCCEEDED mCopiedCount:" + mCopiedCount + " mTotalFileCount:" + mTotalFileCount);
        if(!mHasJustOneFile) {
            publishProgress(mTotalFileCount, mCopiedCount, destinationFilePath, -1 , -1);
        }
        //LogUtil.i(TAG, "copySingleFile SUCCEEDED -----:");
        return result.set(FileOperationResult.FORC_SUCCEEDED, destinationFilePath);
    }

    private FileOperationResult copyDirectory(String srcDirPath, String destinationDir, FileOperationResult result) {
        File srcDirFile = new File(srcDirPath);
        return copyDirectory(srcDirFile, destinationDir, result);
    }

    private FileOperationResult copyDirectory(File srcDirFile, String destinationDir, FileOperationResult result) {
        if(mDestinationDirectory.startsWith(srcDirFile.getPath())) {
            //NOT ALLOWED
            return result.set(FileOperationResult.FORC_COPY_OR_MOVE_INTO_SUBDIRECTORY_NOT_ALLOWED, null);
        }

        String name = srcDirFile.getName();
        String destinationPath = destinationDir + FilePathUtil.PATH_SEPARATOR + name;
        destinationPath = FilePathUtil.generateFilePathWhenExists(destinationPath);
        FilePathUtil.mkdirs(destinationPath);
        //mFileDbManager.insert(srcDirFile.getAbsolutePath(), destinationPath, true);
        mFileDbManager.addToInsertList(srcDirFile.getAbsolutePath(), destinationPath, true);
        mCopiedCount ++;

        publishProgress(mTotalFileCount, mCopiedCount, destinationPath, -1 , -1);

        File[] files = srcDirFile.listFiles();
        if(null == files || 0 == files.length) return result.set(FileOperationResult.FORC_SUCCEEDED, destinationPath);
        for (int i = 0; i < files.length; i++) {
            if(isCancelled()) {
                return result.set(FileOperationResult.FORC_USER_CANCELLED, mDestinationDirectory);
            }
            File file = files[i];
            if (file.isFile()) {
                result = copySingleFile(files[i], destinationPath, result);
            } else if (file.isDirectory()) {
                result = copyDirectory(files[i], destinationPath, result);
            }
            if(!result.isSucceeded()) {
                result.set(result.resultCode, null);
                return result;
            }
        }
        return result.set(FileOperationResult.FORC_SUCCEEDED, destinationPath);
    }

    @Override
    public boolean isCancelledByUser() {
        boolean cancelled = isCancelled();
        if(cancelled) {
            LogUtil.i(TAG, "CopyTask :: isCancelledByUser:" + cancelled);
        }
        return cancelled;
    }

    @Override
    public void onPartialCopied(long alreadyRead, long total, String destinationFilePath) {
        try {
            if (mHasJustOneFile) {
                publishProgress(total, alreadyRead, destinationFilePath, alreadyRead, total);
                if (total < SINGLE_FILE_SLEEP_SIZE_THRESHOLD) {
                    //progress bar will not perform normally, if we don't sleep(0) here
                    Thread.sleep(1);
                }
            } /*else {
                publishProgress(mCopiedCount, mTotalFileCount, destinationFilePath, alreadyRead, total);
            }*/
        } catch (Exception e) {

        }
    }
}
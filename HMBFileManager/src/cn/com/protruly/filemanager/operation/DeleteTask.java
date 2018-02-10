package cn.com.protruly.filemanager.operation;

import android.content.Context;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;

import cn.com.protruly.filemanager.enums.FileInfo;
import cn.com.protruly.filemanager.utils.FilePathUtil;
import cn.com.protruly.filemanager.utils.LogUtil;

/**
 * Created by sqf on 17-5-26.
 */

public class DeleteTask extends BaseOperationTask {

    private static final String TAG = "DeleteTask";

    private HashSet<FileInfo> mFileInfos;
    private int mTotalFileCount;
    private int mDeletedCount;

    public DeleteTask(Context context, FileDbManager fileDbManager, FileOperationTaskListener listener, HashSet<FileInfo> files) {
        super(context, fileDbManager, OperationType.DELETE, listener);
        mFileInfos = files;
        mDeletedCount = 0;
    }

    @Override
    protected FileOperationResult doOperation() {
        mTotalFileCount = FilePathUtil.getAllFileAndDirectoryNum(mFileInfos);
        LogUtil.i(TAG, "mTotalFileCount:" + mTotalFileCount);
        Iterator<FileInfo> it = mFileInfos.iterator();
        FileOperationResult result = new FileOperationResult();
        while(it.hasNext() && !isCancelled()) {
            FileInfo fileInfo = it.next();

            if(!fileInfo.getFile().exists()) {
                //mFileDbManager.delete(mContext, fileInfo.getPath());
                //LogUtil.i(TAG, "Delete file not exists: " + fileInfo.getPath());
                mFileDbManager.addToDeleteList(fileInfo.getPath());
                continue;
            }

            if(fileInfo.isFile()) {
                //LogUtil.i(TAG, "DeleteTask::doOperation while isFile");
                deleteSingleFile(fileInfo.getFile(), result);
            } else if(fileInfo.isDirectory()) {
                //LogUtil.i(TAG, "DeleteTask::doOperation while isDirectory");
                deleteDirectory(fileInfo.getFile(), result);
            } else {
                //LogUtil.i(TAG, "DeleteTask::doOperation something not ordinary");
            }
        }
        mFileDbManager.deleteFromDeleteList();
        return result;
    }

    private FileOperationResult deleteSingleFile(String filePathToDelete, FileOperationResult result) {
        File file = new File(filePathToDelete);
        deleteSingleFile(file, result);
        return result;
    }

    private FileOperationResult deleteSingleFile(File fileToDelete, FileOperationResult result) {
        try {
            //delete from db
            //mFileDbManager.delete(mContext, fileToDelete.getAbsolutePath(), false);
            mFileDbManager.addToDeleteList(fileToDelete.getAbsolutePath());
            //delete from disk
            if (fileToDelete.exists()) {
                fileToDelete.delete();
            }
            //LogUtil.i(TAG, "------File Deleted:" + fileToDelete.getAbsolutePath());
            mDeletedCount++;
            publishProgress(mTotalFileCount, mDeletedCount, fileToDelete.getAbsolutePath(), -1, -1);
            return result.set(FileOperationResult.FORC_SUCCEEDED, fileToDelete.getAbsolutePath());
        } catch (Exception e) {
            if(e != null) {
                LogUtil.e(TAG, "deleteSingleFile:" + e.getMessage());
            }
        }
        return result.set(FileOperationResult.FORC_UNKNOWN_ERROR, fileToDelete.getAbsolutePath());
    }

    private FileOperationResult deleteDirectory(File directoryToDelete, FileOperationResult result) {
        File [] children = directoryToDelete.listFiles();
        for(File child : children) {
            if(isCancelled()) {
                return result.set(FileOperationResult.FORC_USER_CANCELLED, null);
            }
            if(child.isFile()) {
                result = deleteSingleFile(child, result);
            } else if(child.isDirectory()) {
                result = deleteDirectory(child, result);
            }
            if(!result.isSucceeded()) {
                return result.set(result.resultCode, child.getAbsolutePath());
            }
        }
        if(!directoryToDelete.delete()) {
            return result.set(FileOperationResult.FORC_UNKNOWN_ERROR, directoryToDelete.getAbsolutePath());
        }
        mDeletedCount ++;
        //mFileDbManager.delete(mContext, directoryToDelete.getAbsolutePath(), true);
        mFileDbManager.addToDeleteList(directoryToDelete.getAbsolutePath());
        //mFileDbManager.batchDeleteFileStartWithPathPrefix(mContext, directoryToDelete.getAbsolutePath());
        //LogUtil.i(TAG, "------Directory Deleted:" + directoryToDelete.getAbsolutePath());
        publishProgress(mTotalFileCount, mDeletedCount, directoryToDelete.getAbsolutePath(), -1 , -1);
        return result.set(FileOperationResult.FORC_SUCCEEDED, directoryToDelete.getAbsolutePath());
    }
}

package cn.com.protruly.filemanager.operation;

import android.content.Context;
import android.os.AsyncTask;

import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cn.com.protruly.filemanager.enums.FileInfo;
import cn.com.protruly.filemanager.imageloader.PriorityThreadFactory;
import cn.com.protruly.filemanager.utils.DebugUtil;
import cn.com.protruly.filemanager.utils.LogUtil;
import cn.com.protruly.filemanager.utils.SingleMediaScanner;

/**
 * Created by sqf on 17-6-12.
 */

public class FileOperationTaskManager {

    private static final String TAG = "FileOperationTaskManager";

    private Context mContext;
    private AsyncTask mCurrentTask;
    private FileDbManager mFileDbManager;

    public FileOperationTaskManager(Context context) {
        mContext = context;
        mFileDbManager = new FileDbManager(mContext);
    }

    /* we allow only one task to be performed at one time.
     * this task will block ui.
     */
    private void setTask(AsyncTask task) {
        if(isTaskInProgress()) return;
        mCurrentTask = task;
    }

    public AsyncTask getTask() {
        return mCurrentTask;
    }

    public boolean isTaskInProgress() {
        if(null == mCurrentTask) return false;
        return mCurrentTask.getStatus() != AsyncTask.Status.FINISHED;
    }

    public void executeDeleteTask(HashSet<FileInfo> fileInfos, ProgressiveFileOperationTaskNotifier listener) {
        if(isTaskInProgress()) return;
        listener.setFileOperationTaskManager(this);
        //DebugUtil.debugFileInfos(fileInfos);
        DeleteTask task = new DeleteTask(mContext, mFileDbManager, listener, fileInfos);
        setTask(task);
        task.execute();
    }

    public void executeCopyTask(HashSet<FileInfo> fileInfos, String destinationDir, ProgressiveFileOperationTaskNotifier listener) {
        if(isTaskInProgress()) {
            LogUtil.i(TAG, "executeCopyTask task in progress , return");
            return;
        }
        listener.setFileOperationTaskManager(this);
        CopyTask task = new CopyTask(mContext, mFileDbManager, listener, fileInfos, destinationDir);
        setTask(task);
        task.execute();
    }

    public void executeMoveTask(HashSet<FileInfo> fileInfos, String destinationDir, ProgressiveFileOperationTaskNotifier listener) {
        if(isTaskInProgress()) return;
        listener.setFileOperationTaskManager(this);
        MoveTask task = new MoveTask(mContext, mFileDbManager, listener, fileInfos, destinationDir);
        setTask(task);
        task.execute();
    }

    public void executeZipTask(HashSet<FileInfo> fileInfos, String destinationDir, String zippedFileName, ProgressiveFileOperationTaskNotifier listener) {
        if(isTaskInProgress()) return;
        listener.setFileOperationTaskManager(this);
        ZipTask task = new ZipTask(mContext, mFileDbManager, listener, fileInfos, destinationDir, zippedFileName);
        setTask(task);
        task.execute();
    }

    /**
     * unzip an zip entry inside a .zip file, a zip entry is something like below:
     * entry: META-INF/MANIFEST.MF
     * entry: META-INF/IFLYTEKI.SF
     * entry: META-INF/IFLYTEKI.RSA
     * entry: lib/
     * entry: res/
     * entry: res/240/
     * entry: res/240/abc/
     * entry: res/240/checkboxchecked.png
     * entry: res/320/checkboxchecked.png
     * @param zipFilePath thie file path of .zip file
     * @param zipEntryName zip entry
     * @param destinationDir the destination path to unzip
     * @param listener ProgressiveFileOperationTaskNotifier
     */
    public void executeUnzipEntryTask(String zipFilePath, String zipEntryName, String destinationDir, ProgressiveFileOperationTaskNotifier listener) {
        if(isTaskInProgress()) return;
        listener.setFileOperationTaskManager(this);
        UnzipTask task = new UnzipTask(mContext, mFileDbManager, listener, zipFilePath, zipEntryName, destinationDir);
        setTask(task);
        task.execute();
    }

    public void executeUnzipWholeTask(String zipFilePath, String destinationDir, ProgressiveFileOperationTaskNotifier listener) {
        if(isTaskInProgress()) return;
        listener.setFileOperationTaskManager(this);
        UnzipTask task = new UnzipTask(mContext, mFileDbManager, listener, zipFilePath, destinationDir);
        setTask(task);
        task.execute();
    }
}

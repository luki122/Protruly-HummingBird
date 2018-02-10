package cn.com.protruly.filemanager.operation;

import android.content.Context;
import android.os.AsyncTask;

import cn.com.protruly.filemanager.utils.LogUtil;
import cn.com.protruly.filemanager.utils.SingleMediaScanner;
import cn.com.protruly.filemanager.utils.Util;

/**
 * Created by sqf on 17-5-13.
 */

public abstract class BaseOperationTask extends AsyncTask<Object, BaseOperationTask.OperationProgress, FileOperationResult> {

    private static final String TAG = "BaseOperationTask";

    protected Context mContext;
    protected FileOperationTaskListener mOperationTaskListener;
    protected int mOperationType;
    protected FileDbManager mFileDbManager;

    private int mPrevProgress;

    public static final int BUFF_SIZE = 100 * 1024;//250KB

    //when copying a file which size is less than 700KB. we sleep 1ms in writing loop,
    //in order to show progress normally.
    protected static final int SINGLE_FILE_SLEEP_SIZE_THRESHOLD = 700 * 1024;

    public class OperationProgress {
        public String filePath;
        public int progress;
        public long copiedSize;
        public long totalSizeToCopy;
    }

    public BaseOperationTask(Context context, FileDbManager fileDbManager, int operationType, FileOperationTaskListener listener) {
        mContext = context;
        mOperationTaskListener = listener;
        mFileDbManager = fileDbManager;
        setOperationType(operationType);
    }

    protected abstract FileOperationResult doOperation();
    private void setOperationType(int operationType) {
        mOperationType = operationType;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if(isCancelled()) {
            mOperationTaskListener.onOperationCancelled(mOperationType, null);
            return;
        }
        mOperationTaskListener.onOperationStart(mOperationType);
    }

    protected void publishProgress(long totalFileNum, long operatedFileNum, String operatingFilePath,
                                   long copiedSize, long totalSizeToCopy) {
        int progress =   Util.getProgress(totalFileNum, operatedFileNum);
        if(progress == mPrevProgress) return;
        OperationProgress operationProgress = new OperationProgress();
        operationProgress.progress = progress;
        operationProgress.filePath = operatingFilePath;
        operationProgress.copiedSize = copiedSize;
        operationProgress.totalSizeToCopy = totalSizeToCopy;
        //LogUtil.i(TAG, "publishProgress---- progress:" + operationProgress.progress);
        publishProgress(operationProgress);
        mPrevProgress = progress;
    }

    @Override
    protected void onPostExecute(FileOperationResult result) {
        super.onPostExecute(result);
        if(isCancelled()) {
            mOperationTaskListener.onOperationCancelled(mOperationType, result);
            return;
        }
        if(result != null && result.isSucceeded()) {
            mOperationTaskListener.onOperationSucceeded(mOperationType, result);
        } else {
            mOperationTaskListener.onOperationFailed(mOperationType, result);
        }
    }

    @Override
    protected FileOperationResult doInBackground(Object... params) {
        return doOperation();
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        LogUtil.i(TAG, "onCancelled -- ");
        FileOperationResult result = new FileOperationResult();
        result.set(FileOperationResult.FORC_USER_CANCELLED, null);
        mOperationTaskListener.onOperationCancelled(mOperationType, result);
    }

    @Override
    protected void onProgressUpdate(OperationProgress... values) {
        super.onProgressUpdate(values);
        OperationProgress p = values[0];
        mOperationTaskListener.onOperationProgress(mOperationType, p.progress, p.filePath, p.copiedSize, p.totalSizeToCopy);
    }

}

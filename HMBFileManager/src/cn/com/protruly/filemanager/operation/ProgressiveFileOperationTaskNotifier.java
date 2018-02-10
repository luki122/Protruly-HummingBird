package cn.com.protruly.filemanager.operation;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;

import cn.com.protruly.filemanager.ui.ProgressDialogManager;
import cn.com.protruly.filemanager.utils.LogUtil;
import cn.com.protruly.filemanager.R;
import cn.com.protruly.filemanager.utils.Util;

/**
 * Created by sqf on 17-6-19.
 */

public class ProgressiveFileOperationTaskNotifier implements FileOperationTaskListener,
        DialogInterface.OnCancelListener{

    private static final String TAG = "ProgressiveFileOperationTaskListener";

    private Context mContext;
    private FileOperationTaskManager mFileOperationTaskManager;
    private ProgressDialogManager mProgressDialogManager;


    public ProgressiveFileOperationTaskNotifier(Context context) {
        mContext = context;
        mProgressDialogManager = new ProgressDialogManager(context);
        mProgressDialogManager.setCancelListener(this);
    }

    public void setTitle(int titleResId) {
        if(null == mProgressDialogManager) return;
        mProgressDialogManager.changeTitle(titleResId);
    }

    public void setMessage(String message) {
        if(null == mProgressDialogManager) return;
        mProgressDialogManager.setMessage(message);
    }

    public void setFileOperationTaskManager(FileOperationTaskManager fileOperationTaskManager) {
        this.mFileOperationTaskManager = fileOperationTaskManager;
    }

    @Override
    public void onOperationStart(int operationType) {
        if(null == mProgressDialogManager) return;
        int titleResId = getTitleResId(operationType);
        mProgressDialogManager.showProgressDialog(titleResId);
        switch (operationType) {
            case OperationType.COPY:
                //LogUtil.i(TAG, "onOperationStart COPY : ");
                break;
            case OperationType.CUT:
                //LogUtil.i(TAG, "onOperationStart CUT");
                break;
            case OperationType.DELETE:
                //LogUtil.i(TAG, "onOperationStart DELETE : ");
                break;
            default:
                break;
        }
    }

    private int getTitleResId(int operationType) {
        switch (operationType) {
            case OperationType.DELETE:
                return R.string.deleting;
            case OperationType.COPY:
                return R.string.copying;
            case OperationType.CUT:
                return R.string.moving;
            case OperationType.ZIP:
                return R.string.compressing;
            case OperationType.UNZIP_ENTRY:
            case OperationType.UNZIP_WHOLE:
                return R.string.uncompressing;
            default:
                return R.string.deleting;
        }
    }

    @Override
    public void onOperationProgress(int operationType, int progress, String filePath, long copiedSize, long totalFileSize) {
        if(null == mProgressDialogManager) return;
        mProgressDialogManager.setProgress(progress);
        //LogUtil.i(TAG, "ProgressiveFileOperationTaskNotifier onOperationProgress : " + progress);
        switch (operationType) {
            case OperationType.COPY:
            case OperationType.CUT:
            case OperationType.DELETE:
            default:
                break;
        }
    }

    @Override
    public void onOperationCancelled(int operationType, FileOperationResult result) {
        if(null == mProgressDialogManager) return;
        mProgressDialogManager.dismissProgressDialog();
        if(result!=null) {
            Util.showToast(mContext, result.getDescription());
        }
        switch (operationType) {
            case OperationType.COPY:
                //LogUtil.i(TAG, "onOperationCancelled COPY : ");
                break;
            case OperationType.CUT:
                //LogUtil.i(TAG, "onOperationCancelled CUT : ");
                break;
            case OperationType.DELETE:
                //LogUtil.i(TAG, "onOperationCancelled DELETE ");
                break;
            default:
                break;
        }
    }

    @Override
    public void onOperationSucceeded(int operationType, FileOperationResult result) {
        if(null == mProgressDialogManager) return;
        mProgressDialogManager.dismissProgressDialog();
        switch (operationType) {
            case OperationType.COPY:
                //LogUtil.i(TAG, "onOperationSucceeded COPY ");
                break;
            case OperationType.CUT:
                //LogUtil.i(TAG, "onOperationSucceeded CUT ");
                break;
            case OperationType.DELETE:
                //LogUtil.i(TAG, "onOperationSucceeded DELETE ");
                break;
            default:
                break;
        }
    }

    @Override
    public void onOperationFailed(int operationType, FileOperationResult result) {
        if(null == mProgressDialogManager) return;
        mProgressDialogManager.dismissProgressDialog();
        if(result != null) {
            Util.showToast(mContext, result.getDescription());
        }
        switch (operationType) {
            case OperationType.COPY:
                //LogUtil.i(TAG, "onOperationFailed COPY");
                break;
            case OperationType.CUT:
                //LogUtil.i(TAG, "onOperationFailed CUT");
                break;
            case OperationType.DELETE:
                //LogUtil.i(TAG, "onOperationFailed DELETE");
                break;
            default:
                break;
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if(null == mFileOperationTaskManager) {
            //throw new IllegalStateException("FileOperationTaskManager not set in ProgressiveFileOperationTaskNotifier");
            return;
        }
        AsyncTask currentTask = mFileOperationTaskManager.getTask();
        boolean cancelled = currentTask.cancel(false);
        if(cancelled) {
            LogUtil.i(TAG, "cancelled succeeded");
        } else {
            LogUtil.i(TAG, "cancelled failed");
        }
    }
}

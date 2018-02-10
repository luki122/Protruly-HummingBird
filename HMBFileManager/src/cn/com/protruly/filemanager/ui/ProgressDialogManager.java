package cn.com.protruly.filemanager.ui;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.TextView;

import cn.com.protruly.filemanager.R;
import cn.com.protruly.filemanager.utils.LogUtil;
import hb.app.dialog.ProgressDialog;
/**
 * Created by sqf on 17-5-31.
 */

public class ProgressDialogManager {

    private static final String TAG = "ProgressDialogManager";

    private Context mContext;
    private ProgressDialog mProgressDialog;
    private DialogInterface.OnCancelListener mOnCancelListener;

    /**
     * MUST be called before showProgressDialog
     * @param listener
     */
    public void setCancelListener(DialogInterface.OnCancelListener listener) {
        mOnCancelListener = listener;
    }

    public ProgressDialogManager(Context context) {
        mContext = context;
    }

    public void showProgressDialog(int titleResId) {
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setTitle(titleResId);
        mProgressDialog.setMax(100);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setProgress(0);
        mProgressDialog.setOnCancelListener(mOnCancelListener);
        //mProgressDialog.setCustomView(R.layout.custom_progress_dialog_view);
        //if (!mActivity.isFinishing() && !mProgressDialog.isShowing()) {
            mProgressDialog.show();
            LogUtil.i(TAG, "ProgressDialogManager::showProgressDialog ");
        //}
    }

    public void setMessage(String message) {
        if(null == mProgressDialog) return;
        mProgressDialog.setMessage(message);
    }

    public void changeTitle(int titleResId) {
        if(null == mProgressDialog) return;
        mProgressDialog.setTitle(titleResId);
    }

    public void dismissProgressDialog() {
        if(null == mProgressDialog) return;
        mProgressDialog.dismiss();
        mProgressDialog = null;
        LogUtil.i(TAG, "ProgressDialogManager::dismissProgressDialog ");
    }

    public void setProgress(int progress) {
        if(null == mProgressDialog) {
            LogUtil.i(TAG, "ProgressDialogManager::setProgress return due to mProgressDialog is null  ");
            return;
        }
        mProgressDialog.setProgress(progress);
    }
}

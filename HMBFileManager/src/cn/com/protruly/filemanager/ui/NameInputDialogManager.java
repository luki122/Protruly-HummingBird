package cn.com.protruly.filemanager.ui;

import hb.app.dialog.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import cn.com.protruly.filemanager.pathFileList.FileNameInputDialog;
import cn.com.protruly.filemanager.enums.FileInfo;
import cn.com.protruly.filemanager.operation.OperationType;
import cn.com.protruly.filemanager.utils.Util;
import cn.com.protruly.filemanager.R;

/**
 * Created by sqf on 17-5-31.
 */

public class NameInputDialogManager {

    private Context mContext;
    private int mOperationType;
    private NameInputDialogListener mNameInputDialogListener;
    private FileNameInputDialog mFileNameInputDialog;

    public interface NameInputDialogListener {
        public void onNameInputConfirmed(int operationType, FileInfo old, String newName);
        public void onNameInputCancelled(int operationType, FileInfo old);
    }

    public NameInputDialogManager(Context context, int operationType, NameInputDialogListener listener) {
        mContext = context;
        mOperationType = operationType;
        mNameInputDialogListener = listener;
    }

    public boolean isShowingDialog(){
        return mFileNameInputDialog!=null && mFileNameInputDialog.isShowing();
    }

    public void createNameInputDialog(String title, String name, final FileInfo fileInfo){
        FileNameInputDialog.OnFinishFileInputListener finishFileInputListener = new FileNameInputDialog.OnFinishFileInputListener() {
            @Override
            public void onFinishFileNameInput(String newName,String prefix) {
                if(Util.startWithDot(newName)) {
                    showConfirmStartWithDotDialog(mOperationType, newName+prefix,fileInfo);
                    return;
                }
                mFileNameInputDialog.dismiss();
                mFileNameInputDialog = null;
                //doAfterNewNameConfirmed(operationType, fileInfo, newName);
                mNameInputDialogListener.onNameInputConfirmed(mOperationType, fileInfo, newName+prefix);
            }
        };
        if(mOperationType == OperationType.RENAME){
            mFileNameInputDialog = new FileNameInputDialog(mContext,title,name,fileInfo,finishFileInputListener);
        }else{
            mFileNameInputDialog = new FileNameInputDialog(mContext,title,name,finishFileInputListener);
        }
        if(fileInfo != null && fileInfo instanceof FileInfo){
            mFileNameInputDialog.setIsFile(((FileInfo)fileInfo).isFile);
        }
        mFileNameInputDialog.show();
    }

    private void showConfirmStartWithDotDialog(final int operationType, final String newname,final FileInfo fileInfo){
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(mContext.getResources().getString(R.string.confirm_hiden_file_create));
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mNameInputDialogListener.onNameInputConfirmed(operationType, fileInfo, newname);
            }
        });
        builder.setNegativeButton(android.R.string.cancel,null);
        builder.show();
    }
}

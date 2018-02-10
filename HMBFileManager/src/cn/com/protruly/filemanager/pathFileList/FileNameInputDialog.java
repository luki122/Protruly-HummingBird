package cn.com.protruly.filemanager.pathFileList;

import cn.com.protruly.filemanager.enums.FileInfo;
import cn.com.protruly.filemanager.operation.OperationType;
import hb.app.dialog.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;

import cn.com.protruly.filemanager.R;

/**
 * Created by liushitao on 17-5-17.
 */

public class FileNameInputDialog extends AlertDialog implements TextWatcher,DialogInterface.OnClickListener {
    private static final int MAX_LENGTH = 85;
    private String mDialogTitle;
    private boolean mDismiss;
    private EditText mFileNameEdit;
    private String[] mInValideChars = {"*","/","\\","\"",":","?","|","<",">"};
    private String newFileName;
    private OnFinishFileInputListener mInputListener;
    private boolean mIsFile;
    private FileInfo mfileInfo;
    private int operationType;
    private String lastFix = "";

    public FileNameInputDialog(Context context, String newName, OnFinishFileInputListener inputListener) {
        super(context);
        newFileName = newName;
        mInputListener = inputListener;
    }

    public FileNameInputDialog(Context context, String title,String newName, OnFinishFileInputListener inputListener) {
        super(context);
        mDialogTitle = title;
        newFileName = newName;
        mInputListener = inputListener;
    }

    public FileNameInputDialog(Context context, String title, String newName, FileInfo fileInfo, OnFinishFileInputListener inputListener) {
        super(context);
        mDialogTitle = title;
        newFileName = newName;
        mfileInfo = fileInfo;
        mInputListener = inputListener;
    }

    public FileNameInputDialog(Context context, String title, String newName, int operation,FileInfo fileInfo, OnFinishFileInputListener inputListener) {
        super(context);
        mDialogTitle = title;
        newFileName = newName;
        mfileInfo = fileInfo;
        operationType = operation;
        mInputListener = inputListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.file_name_dialog_layout,null);
        setTitle(mDialogTitle);
        mFileNameEdit = (EditText)view.findViewById(R.id.text);
        int lastPotPos = newFileName.lastIndexOf(".");
        /*if(!TextUtils.isEmpty(newFileName) && newFileName.length()>100 && lastPotPos!=-1){
            newFileName = newFileName.substring(newFileName.length()-100-1,lastPotPos);
        }*/
        if(mfileInfo!=null && mfileInfo.isFile() && !TextUtils.isEmpty(newFileName) && lastPotPos!=-1){
            lastFix = newFileName.substring(lastPotPos, newFileName.length());
            newFileName = newFileName.substring(0, lastPotPos);
        }
        if(mfileInfo == null && !TextUtils.isEmpty(newFileName) && newFileName.length() > MAX_LENGTH){
            newFileName = newFileName.substring(newFileName.length() - MAX_LENGTH - 1, newFileName.length());
        }
        mFileNameEdit.setText(newFileName);
        mFileNameEdit.addTextChangedListener(this);
        setView(view);
        setButton(DialogInterface.BUTTON_POSITIVE,mContext.getResources().getString(android.R.string.ok),this);
        setButton(DialogInterface.BUTTON_NEGATIVE,mContext.getResources().getString(android.R.string.cancel),this);
        setCanceledOnTouchOutside(false);
        super.onCreate(savedInstanceState);
    }

    public void setIsFile(boolean isFile){
        mIsFile = isFile;
    }

    @Override
    public void show() {
        super.show();
        mFileNameEdit.requestFocus();
        getWindow().setSoftInputMode(5);
        /*if(mIsFile){
            int i = newFileName.lastIndexOf(".");
            if(i!=-1){
                mFileNameEdit.setSelection(0,i);
            }
        }*/
    }

    @Override
    protected void onStop() {
        super.onStop();
        mFileNameEdit.removeTextChangedListener(this);
    }

    @Override
    public void dismiss() {
        if(mDismiss){
            super.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        mDismiss = true;
        super.onBackPressed();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if(s.length() >= MAX_LENGTH){
            Toast.makeText(getContext(),R.string.is_too_long,Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void afterTextChanged(Editable s){

    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if(which==DialogInterface.BUTTON_POSITIVE){
            newFileName = mFileNameEdit.getText().toString().trim();
            /*if(mfileInfo!=null && mfileInfo.isFile()){
                newFileName = mFileNameEdit.getText().toString().trim()+lastFix;
            }else{
                newFileName = mFileNameEdit.getText().toString().trim();
            }*/
            if(checkNewName(newFileName)){
                mDismiss = true;
                if(mfileInfo!=null && mfileInfo.isDirectory()){
                    lastFix = "";
                }
                mInputListener.onFinishFileNameInput(newFileName,lastFix);
            }
        }
        if(which==DialogInterface.BUTTON_NEGATIVE){
            mDismiss = true;
        }
    }

    private boolean checkNewName(String name){
        if (nameEmpty(name)) {
            Toast.makeText(getContext(),R.string.is_empty,Toast.LENGTH_SHORT).show();
            return false;
        }
        if (nameInValide(name)) {
            Toast.makeText(getContext(),R.string.is_invide,Toast.LENGTH_SHORT).show();
            return false;
        }
        if (nameEqualsDot(name)) {
            Toast.makeText(getContext(),R.string.is_dot,Toast.LENGTH_SHORT).show();
            return false;
        }
        if(operationType != OperationType.CREATE_FOLDER && mfileInfo!=null && new File(mfileInfo.getParent(),name+lastFix).exists()){
            Toast.makeText(getContext(),R.string.is_same_name,Toast.LENGTH_SHORT).show();
            return false;
        }
        if(operationType == OperationType.CREATE_FOLDER && mfileInfo!=null && new File(mfileInfo.getPath(),name+lastFix).exists()){
            Toast.makeText(getContext(),R.string.is_same_name,Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean nameEmpty(String str){
        return TextUtils.isEmpty(str);
    }

    private boolean nameEqualsDot(String str){
        return str.equals(".");
    }

    private boolean nameInValide(String str){
        for(String str1:mInValideChars){
            if(str.toLowerCase().contains(str1.toLowerCase())){
                return true;
            }
        }
        return false;
    }

    public interface OnFinishFileInputListener{
        void onFinishFileNameInput(String str,String str1);
    }
}

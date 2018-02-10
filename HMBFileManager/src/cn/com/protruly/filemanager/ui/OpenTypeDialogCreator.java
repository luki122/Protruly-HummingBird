package cn.com.protruly.filemanager.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;

import java.io.File;

import cn.com.protruly.filemanager.utils.IntentBuilderUtil;
import cn.com.protruly.filemanager.utils.Util;
import hb.app.dialog.AlertDialog;
import cn.com.protruly.filemanager.R;

/**
 * Created by sqf on 17-5-24.
 */

public class OpenTypeDialogCreator implements View.OnClickListener{

    private Context mContext;
    private String mFilePath;
    private Intent mIntent;
    private AlertDialog mAlertDialog;

    public OpenTypeDialogCreator(Context context, Intent intent, String filePath) {
        mContext = context;
        mIntent = intent;
        mFilePath = filePath;
    }

    public View initDialogView(final Intent intent, final String path) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.file_open_layout,null);
        view.findViewById(R.id.file_document).setOnClickListener(this);
        view.findViewById(R.id.file_music).setOnClickListener(this);
        view.findViewById(R.id.file_video).setOnClickListener(this);
        view.findViewById(R.id.file_picture).setOnClickListener(this);
        return view;
    }

    public final void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.open_type);
        builder.setView(initDialogView(mIntent, mFilePath));
        builder.setNegativeButton(android.R.string.cancel,null);
        mAlertDialog = builder.create();
        mAlertDialog.show();
    }

    @Override
    public void onClick(View v) {
        File file = new File(mFilePath);
        final Uri fileUri = Util.getUriForFile(mContext,file);
        int id = v.getId();
        switch(id) {
            case R.id.file_document:
                mIntent.setDataAndType(fileUri, IntentBuilderUtil.buildType(0));
                break;
            case R.id.file_music:
                mIntent.setDataAndType(fileUri, IntentBuilderUtil.buildType(1));
                break;
            case R.id.file_video:
                mIntent.setDataAndType(fileUri, IntentBuilderUtil.buildType(2));
                break;
            case R.id.file_picture:
                mIntent.setDataAndType(fileUri, IntentBuilderUtil.buildType(3));
                break;
        }
        mContext.startActivity(mIntent);
        mAlertDialog.dismiss();
    }
}

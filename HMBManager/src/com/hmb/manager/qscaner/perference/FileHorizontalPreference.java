package com.hmb.manager.qscaner.perference;


import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hmb.manager.R;
import com.hmb.manager.qscaner.QScannerDetailActivity;
import com.hmb.manager.utils.FileUtils;

import java.io.File;

import hb.preference.Preference;

public class FileHorizontalPreference extends Preference {
    private Drawable mDrawable;
    private String mAppName;
    private String mFileSize;
    public String mFilePath;
    private QScannerDetailActivity mActivity;

    public FileHorizontalPreference(QScannerDetailActivity activity, Drawable drawable,
                                    String appName, String pkgName, String path, String size) {
        super(activity);
        mActivity = activity;
        mDrawable = drawable;
        mAppName = appName;
        mFilePath = path;
        mFileSize = size;
        setLayoutResource(R.layout.file_horizontal_preference);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        ImageView fileIcon = (ImageView)view.findViewById(R.id.fileIcon);
        TextView fileName = (TextView)view.findViewById(R.id.fileName);
        TextView fileSize = (TextView)view.findViewById(R.id.fileSize);
        final TextView filePath = (TextView)view.findViewById(R.id.filePath);
        TextView fileDelete = (TextView)view.findViewById(R.id.text_delete);
        fileDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (FileUtils.deleteFile(new File(mFilePath))) {
                            mActivity.removeFileHorizontalPreference(FileHorizontalPreference.this);
                        } else {
                            Toast.makeText(mActivity,
                                    mActivity.getString(R.string.safety_toast_delete_file_error),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }).start();
            }
        });
        fileIcon.setImageDrawable(mDrawable);
        fileName.setText(mAppName);
        fileSize.setText(mFileSize);
        filePath.setText(mFilePath);
    }
}
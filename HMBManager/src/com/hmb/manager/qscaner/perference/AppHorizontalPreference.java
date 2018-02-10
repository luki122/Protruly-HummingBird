package com.hmb.manager.qscaner.perference;


import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.hmb.manager.R;
import com.hmb.manager.qscaner.QScannerDetailActivity;

import hb.preference.Preference;

public class AppHorizontalPreference extends Preference {

    private String mAppName;
    private Drawable mDrawable;
    public String mPackageName;
    private QScannerDetailActivity mActivity;

    public AppHorizontalPreference(QScannerDetailActivity activity, Drawable drawable,
                                   String packageName, String softName) {
        super(activity);
        mDrawable = drawable;
        mAppName = softName;
        mPackageName = packageName;
        mActivity = activity;
        setLayoutResource(R.layout.app_horizontal_preference);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        ImageView appIcon = (ImageView)view.findViewById(R.id.appIcon);
        TextView appName = (TextView)view.findViewById(R.id.appName);
        TextView appUninstall = (TextView)view.findViewById(R.id.appUninstall);
        appUninstall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uninstallPkg(mPackageName);
            }
        });
        appIcon.setImageDrawable(mDrawable);
        appName.setText(mAppName);
    }

    private void uninstallPkg(String packageName) {
        // Create new intent to launch Uninstaller activity
        Uri packageURI = Uri.parse("package:" + packageName);
        Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageURI);
        uninstallIntent.putExtra(Intent.EXTRA_UNINSTALL_ALL_USERS, true);
        mActivity.startActivity(uninstallIntent);
    }
}

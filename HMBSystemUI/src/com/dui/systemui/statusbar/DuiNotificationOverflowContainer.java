package com.dui.systemui.statusbar;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.service.notification.StatusBarNotification;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.ViewInvertHelper;
import com.android.systemui.statusbar.ActivatableNotificationView;
import com.android.systemui.statusbar.phone.NotificationPanelView;

/**
 * Created by chenheliang on 17-3-8.
 */
public class DuiNotificationOverflowContainer extends ActivatableNotificationView {

    private ViewInvertHelper mViewInvertHelper;
    private boolean mDark;
    private View mContent;
    private int mCount;
    private StringBuilder mStringBuilder;
    private TextView mTitleView,mMessageBox;

    public DuiNotificationOverflowContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        mCount=0;
        mStringBuilder = new StringBuilder();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mTitleView = (TextView)findViewById(R.id.id_tile);
        mMessageBox= (TextView)findViewById(R.id.dui_id_box);
        mContent = findViewById(R.id.content);
        mViewInvertHelper = new ViewInvertHelper(mContent,
                NotificationPanelView.DOZE_ANIMATION_DURATION);
        findViewById(R.id.backgroundNormal).setVisibility(View.GONE);
        findViewById(R.id.backgroundDimmed).setVisibility(View.GONE);
    }

    @Override
    protected void updateBackground() {

    }

    @Override
    public void setDark(boolean dark, boolean fade, long delay) {
        //super.setDark(dark, fade, delay);
        if (mDark == dark) return;
        mDark = dark;
        if (fade) {
            mViewInvertHelper.fade(dark, delay);
        } else {
            mViewInvertHelper.update(dark);
        }
    }

    @Override
    public void setDimmed(boolean dimmed, boolean fade) {
        //super.setDimmed(dimmed, fade);
    }

    @Override
    protected View getContentView() {
        return mContent;
    }

    public void addCount(StatusBarNotification sbn){
        String apn=getAplicationName(sbn.getPackageName());
        if(!mStringBuilder.toString().contains(apn)){
            if(mCount!=0){
                mStringBuilder.append("、");
            }
            mStringBuilder.append(apn);
        }
        mCount++;

    }
    public int getCount()
    {
        return mCount;
    }
    public void removeCount()
    {
        mStringBuilder.delete(0,mStringBuilder.length());
        mCount=0;
    }

    public void updateString(){
        if(mCount<=0){
            return;
        }
        String tile = getContext().getString(R.string.dui_notify_title,mStringBuilder.toString());
        String number = getContext().getString(R.string.dui_notify_number,mCount);
        mTitleView.setText(tile+number);
        mMessageBox.setText(R.string.dui_notify_over_name);
        //mNumberView.setText(number);
    }

    private String getAplicationName(String pkg){
        PackageManager packageManager ;
        ApplicationInfo applicationInfo;
        String applicationName ;
        synchronized (this) {
            try {
                packageManager = getContext().getPackageManager();
                applicationInfo = packageManager.getApplicationInfo(pkg, 0);
                applicationName =
                        (String) packageManager.getApplicationLabel(applicationInfo);
            } catch (PackageManager.NameNotFoundException e) {
                applicationName = pkg;
            }
        }
        return applicationName;
    }
    protected int getContentHeightFromActualHeight(int actualHeight) {
        int realActualHeight = actualHeight;
        if (hasBottomDecor()) {
            realActualHeight -= getBottomDecorHeight();
        }
        realActualHeight = Math.max(getMinHeight(), realActualHeight);
        return realActualHeight;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        FontSizeUtils.updateFontSize(mTitleView, R.dimen.hb_search_tips);
        FontSizeUtils.updateFontSize(mMessageBox, R.dimen.hb_message_box_size);
    }
}

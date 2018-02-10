package com.protruly.music.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.FrameLayout;

import com.protruly.music.MusicUtils;
import com.protruly.music.MusicUtils.DbChangeListener;
import com.protruly.music.model.HBLoadingListener;
import com.protruly.music.util.LogUtil;
/**
 * Created by hujianwei on 17-8-29.
 */

abstract public class AbstractBaseActivity extends BasicActivity implements DbChangeListener{

    private static final String TAG = "AbstractBaseActivity";
    private static final int HB_MSG_UPDATE_DB = 150;
    private static final int HB_MSG_NEEDUPDATE_DB = 151;

    abstract public void onMediaDbChange(boolean selfChange);

    private FrameLayout mContentContainer;
    private View mFloatView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean flag = MusicUtils.registerDbObserver(getApplicationContext(), this);
        if (MusicUtils.mSongDb != null) {
            MusicUtils.mSongDb.setLoadingListener(mLoadingListener);
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (MusicUtils.mSongDb != null) {
            MusicUtils.mSongDb.removesetLoadingListener(mLoadingListener);
        }
        mUiHandler.removeMessages(HB_MSG_UPDATE_DB);
    }
    @Override
    public void finish() {
        super.finish();
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
    }

    private final HBLoadingListener mLoadingListener = new HBLoadingListener() {

        @Override
        public void onNotNeedLoading() {
        }

        @Override
        public void onNeedLoading() {
            LogUtil.d(TAG, "onNeedLoading mLoadingListener");
            onMediaDbChange(false);
        }
    };

    @Override
    public void onDbChanged(boolean selfChange) {
        if (mUiHandler != null) {
            mUiHandler.removeMessages(HB_MSG_UPDATE_DB);
            mUiHandler.sendMessage(mUiHandler.obtainMessage(HB_MSG_UPDATE_DB, selfChange));
        }
    }

    private final Handler mUiHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HB_MSG_UPDATE_DB:
                    boolean selfChange = ((Boolean) msg.obj).booleanValue();
                    if (MusicUtils.mSongDb != null) {
                        MusicUtils.mSongDb.onContentDirty();
                    }

                    break;

                case HB_MSG_NEEDUPDATE_DB:
                    break;

                default:
                    break;
            }
        }

    };

}

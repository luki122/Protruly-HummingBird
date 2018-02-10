package com.protruly.music.online;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.protruly.music.Application;
import com.protruly.music.MediaPlaybackService;
import com.protruly.music.MusicUtils;
import com.protruly.music.MusicUtils.ServiceToken;
import com.protruly.music.R;
import com.protruly.music.ui.AbstractBaseActivity;
import com.protruly.music.ui.HBPlayerActivity;
import com.protruly.music.util.Globals;
import com.protruly.music.util.LogUtil;
import com.protruly.music.util.HBMusicUtil;
import com.protruly.music.widget.BackView;

/**
 * Created by hujianwei on 17-9-1.
 */

public class HBNetTrackDetailActivity extends AbstractBaseActivity implements MusicUtils.Defs, ServiceConnection {
    private static final String TAG = "HBNetTrackDetailActivity";
    private TextView tv_title;
    
    public static final String ID = "Id";
    private ImageView actionBar_play;
    private HBNetTrackDetailFragment mHBNetTrackDetailFragment = null;
    private LinearLayout mPlayHeader;
    private LinearLayout mActionBar;

    // 动画是否在运行
    private boolean isPlaying = false;

    // 播放按钮动画
    private Animation operatingAnim;
    private View actionbar_divider;
    private BackView backView;
    private View backView_parent;
    private View play_parent;
    private View viewHeader;
    public static final String ALBUM_ID = "albumId";
    int mPageNo = 1;
    int mType;
    String id;
    private View mDescView;
    private int[] flystartPoint = new int[2];
    private int[] flyendPoint = new int[2];
    private boolean isStop;
    private ServiceToken mToken;
    private Bitmap bitmap;
    private View view_actionBar;
    private int marginTop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hb_nettrackdetail_activity);
        mToken = MusicUtils.bindToService(this, this);
        initView();
        operatingAnim = AnimationUtils.loadAnimation(this, R.anim.rotate_anim);
        LinearInterpolator lin = new LinearInterpolator();
        operatingAnim.setInterpolator(lin);
        registerStateChangeReceiver();
        mHBNetTrackDetailFragment = new HBNetTrackDetailFragment();
        getFragmentManager().beginTransaction().replace(R.id.activity_base_content, mHBNetTrackDetailFragment).commitAllowingStateLoss();
        if (!HBMusicUtil.isNetWorkActive(this)) {
            Toast.makeText(this, R.string.hb_network_error, Toast.LENGTH_SHORT).show();
        }
        initNotify();

    }

    // 透明通知栏
    private void initNotify() {
        if (Build.VERSION.SDK_INT >= 19 && Globals.SWITCH_FOR_TRANSPARENT_STATUS_BAR) {
            findViewById(R.id.hb_status_bg).setVisibility(View.VISIBLE);
            if (Build.VERSION.SDK_INT < 21) {
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                Notification.Builder builder = new Notification.Builder(this);
                builder.setSmallIcon(com.hb.R.drawable.switch_track_material);
                String tag = "hbwhiteBG653";
                notificationManager.notify(tag, 0, builder.build());
                view_actionBar.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onMediaDbChange(boolean selfChange) {
        // TODO Auto-generated method stub

    }

    public void initView() {
        Intent intent = getIntent();
        String title = intent.getExtras().getString("title");
        tv_title = (TextView) findViewById(R.id.hb_title_text);
        
        viewHeader = findViewById(R.id.hb_header_bg);
        mType = getIntent().getExtras().getInt("tag");
        id = getIntent().getExtras().getString(ID);
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 2;
            ImageView imageView;
            if (mType == 2) {
                LogUtil.d(TAG, "id:" + id);
                if (Integer.valueOf(id) == 0) {
                    bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.xiamirank, opts);
                    ((ImageView) viewHeader.findViewById(R.id.hb_recommend_topbar)).setImageBitmap(bitmap);
                } else if (Integer.valueOf(id) == 1) {
                    bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.xiami_newrank, opts);
                    ((ImageView) viewHeader.findViewById(R.id.hb_recommend_topbar)).setImageBitmap(bitmap);
                } else if (Integer.valueOf(id) == 2) {
                    bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.xiami_hito_p, opts);
                    ((ImageView) viewHeader.findViewById(R.id.hb_recommend_topbar)).setImageBitmap(bitmap);
                } else if (Integer.valueOf(id) == 8) {
                    bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.xiami_original, opts);
                    ((ImageView) viewHeader.findViewById(R.id.hb_recommend_topbar)).setImageBitmap(bitmap);
                } else if (Integer.valueOf(id) == 9) {
                    bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.xiami_demo_p, opts);
                    ((ImageView) viewHeader.findViewById(R.id.hb_recommend_topbar)).setImageBitmap(bitmap);
                } else {
                    bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.hb_billboard_big, opts);
                    ((ImageView) viewHeader.findViewById(R.id.hb_recommend_topbar)).setImageBitmap(bitmap);
                }
            } else {
                bitmap = BitmapFactory.decodeResource(getResources(), R.id.hb_recommend_topbar, opts);
                ((ImageView) viewHeader.findViewById(R.id.hb_recommend_topbar)).setImageBitmap(bitmap);
            }
        } catch (OutOfMemoryError error) {
            error.printStackTrace();
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        // tv_title.setTypeface(mFace);
        tv_title.setText(title);
        actionBar_play = (ImageView) findViewById(R.id.img_bt_play);
        mPlayHeader = (LinearLayout) findViewById(R.id.hb_play_header);
        mPlayHeader.findViewById(R.id.hb_recommend_play).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                mHBNetTrackDetailFragment.playAll(0);
            }
        });
        mPlayHeader.findViewById(R.id.hb_recommand_songnumber).setOnClickListener(null);
        mActionBar = (LinearLayout) findViewById(R.id.title_layout);
        actionbar_divider = findViewById(R.id.hb_action_vdivider);
        backView = (BackView) findViewById(R.id.hb_backview);
        backView_parent = findViewById(R.id.hb_backview_parent);
        backView_parent.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub
                backView.onTouchEvent(event);
                return false;
            }
        });
        play_parent = findViewById(R.id.hb_play_parent);
        play_parent.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // TODO Auto-generated method stub
                Intent intent = new Intent(HBNetTrackDetailActivity.this, HBPlayerActivity.class);
                startActivity(intent);

                Application.setRadio(-1, -1);
            }
        });
        mDescView = findViewById(R.id.hb_desc_view);
        view_actionBar = findViewById(R.id.hb_actionbar);
    }

    public void changeHeaderStatus(int mode, int increase) {

        if (marginTop == 0) {
            View layout = findViewById(R.id.title_layout);
            marginTop = layout.getHeight();
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mPlayHeader.getLayoutParams();
            params.topMargin = marginTop;
            mPlayHeader.setLayoutParams(params);
        }
        
        if (mode == 0) {
            mPlayHeader.setVisibility(View.GONE);
            mActionBar.setBackgroundColor(getResources().getColor(R.color.hb_actionbar_bg));
            mActionBar.getBackground().setAlpha(increase);
            String strColr = "#";
            String strBackViewColor = "#";
            if (increase < 160) {
                for (int i = 0; i < 3; i++) {
                    strColr += Integer.toHexString((HBNetTrackDetail.alaphCount) - increase);
                }
            } else {
                for (int i = 0; i < 3; i++) {
                    strColr += Integer.toHexString((int) (HBNetTrackDetail.alaphCount - ((HBNetTrackDetail.alaphCount - 37) / (HBNetTrackDetail.alaphCount * 1.0)) * increase));
                }
            }
            for (int i = 0; i < 3; i++) {
                if (Integer.toHexString((HBNetTrackDetail.alaphCount - increase)).length() == 1) {
                    strBackViewColor += "0" + Integer.toHexString((HBNetTrackDetail.alaphCount - increase));
                } else
                    strBackViewColor += Integer.toHexString((HBNetTrackDetail.alaphCount - increase));
            }
            strColr = changeBackColor(strColr);
            strBackViewColor = changeBackColor(strBackViewColor);
            tv_title.setTextColor(Color.parseColor(strColr));
            backView.changeColor(Color.parseColor(strBackViewColor));
            actionbar_divider.setVisibility(View.INVISIBLE);
        } else if (mode == 1) {
            mPlayHeader.setVisibility(View.VISIBLE);
            mActionBar.setBackgroundColor(getResources().getColor(R.color.hb_actionbar_bg));
            actionbar_divider.setVisibility(View.VISIBLE);
            backView.changeColor(Color.parseColor("#585858"));
        } else if (mode == 2) {
            String strColr = "#";
            if (increase < 160) {
                for (int i = 0; i < 3; i++) {
                    strColr += Integer.toHexString((HBNetTrackDetail.alaphCount) - increase);
                }
            } else {
                for (int i = 0; i < 3; i++) {
                    strColr += Integer.toHexString((int) (HBNetTrackDetail.alaphCount - ((HBNetTrackDetail.alaphCount - 37) / (HBNetTrackDetail.alaphCount * 1.0)) * increase));
                }
            }
            strColr = changeBackColor(strColr);
            tv_title.setTextColor(Color.parseColor(strColr));
        } else if (mode == 3) {
            mPlayHeader.setVisibility(View.GONE);
        }
    }

    public void showTrackNumber(int number) {
        ((TextView) mPlayHeader.findViewById(R.id.hb_recommand_songnumber)).setText(getString(R.string.number_track, number));
    }

    public void hideHeader() {
        viewHeader.setVisibility(View.GONE);
    }

    public void showDes() {
        if (mDescView.getVisibility() == View.VISIBLE) {
            mDescView.setVisibility(View.GONE);
        } else {
            mDescView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 设置播放动画
     */
    /**
     * 设置播放动画
     */
    private void setPlayAnimation() {
        if (actionBar_play == null || operatingAnim == null) {
            return;
        }
        try {
            if (MusicUtils.sService != null) {
                if (MusicUtils.sService.isPlaying()) {
                    if (!isPlaying) {
                        actionBar_play.startAnimation(operatingAnim);
                        play_parent.setBackgroundResource(android.R.color.transparent);
                        isPlaying = true;
                    }
                } else {
                    actionBar_play.clearAnimation();
                    play_parent.setBackgroundResource(R.drawable.hb_left_bar_clicked2);
                    isPlaying = false;
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            if (isPlaying) {
                actionBar_play.clearAnimation();
                play_parent.setBackgroundResource(R.drawable.hb_left_bar_clicked2);
                isPlaying = false;
            }
        }
    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
        setPlayAnimation();
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
        if (isPlaying && actionBar_play != null) {
            actionBar_play.clearAnimation();
            isPlaying = false;
        }
    }

    // 监听播放状态的变化
    private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MediaPlaybackService.PLAYSTATE_CHANGED)) {
                setPlayAnimation();
            } else if (action.equals(MediaPlaybackService.META_CHANGED)) {
                mHBNetTrackDetailFragment.showAnimation();
            }

        }
    };

    // 注册监听播放器状态更改的广播
    private void registerStateChangeReceiver() {
        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.PLAYSTATE_CHANGED);
        f.addAction(MediaPlaybackService.META_CHANGED);
        registerReceiver(mStatusListener, f);
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        unregisterReceiverSafe(mStatusListener);
        if (mHBNetTrackDetailFragment != null) {
            mHBNetTrackDetailFragment.Destroy();
        }
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        MusicUtils.unbindFromService(mToken);
    }

    private void unregisterReceiverSafe(BroadcastReceiver receiver) {
        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            // ignore
        }
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        isStop = true;
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        isStop = false;
    }

    public void startFly(HBMusicUtil.AnimationEndListener listener) {
        if (isStop)
            return;
        actionBar_play.getLocationInWindow(flyendPoint);
        HBMusicUtil.startFly(this, flystartPoint[0], flyendPoint[0] - (int) getResources().getDimension(R.dimen.hb_actionbar_paddingleft), flystartPoint[1], (int) flyendPoint[1]
                - (int) getResources().getDimension(R.dimen.hb_actionbar_paddingtop), listener, true);
    }

    public void setStartPoint(int x, int y) {
        flystartPoint[0] = x;
        flystartPoint[1] = y;
    }

    public View getPlaySelect() {
        if (mHBNetTrackDetailFragment != null) {
            return mHBNetTrackDetailFragment.getPlaySelect();
        }
        return null;
    }

    @Override
    public void onServiceConnected(ComponentName arg0, IBinder arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
        // TODO Auto-generated method stub

    }

    public void setTitle(String title) {
        tv_title.setText(title);
    }

    public Bitmap getRecommendBitmap() {
        return bitmap;
    }

    private String changeBackColor(String old) {
        if (old.equals("#252525")) {
            old = "#585858";
        }
        return old;
    }
}

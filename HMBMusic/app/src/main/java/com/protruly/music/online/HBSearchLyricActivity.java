package com.protruly.music.online;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.protruly.music.MusicUtils;
import com.protruly.music.R;
import com.protruly.music.adapter.HBPlayerPagerAdapter;
import com.protruly.music.model.XiaMiSdkUtils;
import com.protruly.music.ui.BasicActivity;
import com.protruly.music.util.DisplayUtil;
import com.protruly.music.util.Globals;
import com.protruly.music.util.LogUtil;
import com.protruly.music.util.HBMusicUtil;
import com.protruly.music.util.ThreadPoolExecutorUtils;
import com.protruly.music.widget.HBSearchLyricView;
import com.xiami.sdk.entities.OnlineSong;
import com.xiami.sdk.utils.ImageUtil;
import hb.app.dialog.ProgressDialog;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hujianwei on 17-9-1.
 */

public class HBSearchLyricActivity extends BasicActivity {

    private static final String TAG = "HBSearchLyricActivity";
    //private HbActionBar mHbActionBar;
    //private HbDotView mHbDotView, mImgDotView;
    private TextView showLyricSize, mShowImgSize;
    //private HbViewPager mHbViewPager, mShowImgViewPage;
    private List<View> mViews = new ArrayList<View>();
    private List<View> mImgViews = new ArrayList<View>();
    private View mPlayerLrcLayout;
    public static final String EXTR_ID = "extr_songid";
    public static final String EXTR_ARTIST = "extr_artist";
    public static final String EXTR_NAME = "extr_name";
    public static final String EXTR_PATH = "extr_path";
    public static final String EXTR_ALBUM = "extr_album";
    public static final String EXTR_SONGPATH = "extr_song_path";
    public static final String EXTR_IMG_PATH = "extr_imgpath";
    private int viewCount = 0, imgCount = 0;
    private int downloadCount = 0;
    private ProgressBar mProgressBar;
    private static final int SEARCH_COUNT = 5;
    private String songName, artistName, albumName, songid, mImgPath;

    private TextView mSongView, mArtistView;
    private View mImgViewLayout1, mImgViewLayout2;
    private SparseArray<CheckBox> mLyricChecks = new SparseArray<CheckBox>();
    private SparseArray<CheckBox> mImgcChecks = new SparseArray<CheckBox>();
    private List<String> mImgUrl = new ArrayList<String>();
    private boolean isLoadFinish = false;
    private boolean isSelected = false;
    public static final String DEFUALT_IMG_URL = "defualt";

    private int mWidth = 0;
    private int mHight = 0;
    private HandlerThread mHandlerThread = null;
    private Handler mHandler = null;
    private boolean mRunning;
    private static final int HB_IMG_REFEASH = 1;
    public static final String HB_ORIGINAL_IMG_URL = "Original";
    private Bitmap mOriginalBm = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHbContentView(R.layout.hb_show_search_lyric);
        try {
            Intent intent = getIntent();
            if (intent == null) {
                finish();
                return;
            }

            mRunning = false;
            mWidth = mHight = DisplayUtil.dip2px(this, 106.7f);
            viewCount = 0;
            songid = intent.getStringExtra(EXTR_ID);
            songName = intent.getStringExtra(EXTR_NAME);
            artistName = intent.getStringExtra(EXTR_ARTIST);
            albumName = intent.getStringExtra(EXTR_ALBUM);
            albumName = HBMusicUtil.doAlbumName(intent.getStringExtra(EXTR_SONGPATH), albumName);
            mImgPath = intent.getStringExtra(EXTR_IMG_PATH);
            initActionBar();
            initView();
            isLoadFinish = false;
            mOriginalBm = null;


            new SearchSongTask().executeOnExecutor(ThreadPoolExecutorUtils.getThreadPoolExecutor().getExecutor());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initData(String id) {
        mHandlerThread = new HandlerThread("Hb_Search_Thread");
        mHandlerThread.start();

        mHandler = new Handler(mHandlerThread.getLooper());
        mHandler.post(mRunnable);
        return;
    }

    private final Handler mHandler2 = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            mOriginalBm = null;
            if (msg.what == HB_IMG_REFEASH) {
                if (msg.obj != null) {
                    mOriginalBm = (Bitmap) msg.obj;
                }

                if (mOriginalBm != null) {
                    if (imgCount == 0) {
                        mImgViews.add(mImgViewLayout1);
                    } else if (imgCount == 3) {
                        mImgViews.add(mImgViewLayout2);
                    }
                    initSearchImgView(imgCount, HB_ORIGINAL_IMG_URL);
                    imgCount++;
                }
            }

            return;
        }

    };

    private final Runnable mRunnable = new Runnable() {

        @Override
        public void run() {
            LogUtil.d(TAG, " mRunnable 1 currentThread name:" + Thread.currentThread().getName() + ",mRunning:" + mRunning);
            if (!mRunning) {
                long tid = -1;
                try {
                    tid = Long.parseLong(songid);
                } catch (Exception e) {
                    tid = -1;
                }
                if (tid < 0) {
                    mHandler2.obtainMessage(HB_IMG_REFEASH, null);
                    return;
                }

                Bitmap bm = null;
                Bitmap bm2 = null;
                if (bm != null) {
                    bm2 = bm.createScaledBitmap(bm, mWidth, mHight, true);
                    mHandler2.obtainMessage(HB_IMG_REFEASH, bm2).sendToTarget();
                } else {
                    mHandler2.obtainMessage(HB_IMG_REFEASH, null).sendToTarget();
                }

                if (bm != null && !bm.isRecycled()) {
                    bm.recycle();
                    bm = null;
                }
            } else {
                mHandler2.obtainMessage(HB_IMG_REFEASH, null).sendToTarget();
            }

        }
    };

    private void initActionBar() {
//        mHbActionBar = getHbActionBar();
//        mHbActionBar.setTitle(R.string.hb_now_playing);
//        mHbActionBar.addItem(R.drawable.hb_select_lyric_check, 0, null);
//        mHbActionBar.setOnHbActionBarListener(mOnHbActionBarItemClickListener);
    }

    private void initView() {

//        mImgDotView = (HbDotView) findViewById(R.id.hb_img_dot_layout);
//        mHbDotView = (HbDotView) findViewById(R.id.hb_dot_layout);
        showLyricSize = (TextView) findViewById(R.id.hb_search_lyric_size);
//        mHbViewPager = (HbViewPager) findViewById(R.id.id_container);
        mProgressBar = (ProgressBar) findViewById(R.id.hb_progress);
        mSongView = (TextView) findViewById(R.id.hb_song_name);
        mArtistView = (TextView) findViewById(R.id.hb_artist_name);
        mShowImgSize = (TextView) findViewById(R.id.hb_search_img_size);
//        mShowImgViewPage = (HbViewPager) findViewById(R.id.id_img_container);
//        mHbDotView.setSelectDrawble(R.drawable.hb_search_dot_select);
//        mHbDotView.setUnselectDrawble(R.drawable.hb_search_dot_unselect);
//        mHbDotView.setDotBesize(R.dimen.hb_search_dot_besize);
//        mHbDotView.setSelectDot(0);

//        mImgDotView.setSelectDrawble(R.drawable.hb_search_dot_select);
//        mImgDotView.setUnselectDrawble(R.drawable.hb_search_dot_unselect);
//        mImgDotView.setDotBesize(R.dimen.hb_search_dot_besize);
//        mImgDotView.setSelectDot(0);
        changeViewState(false);

        // 初始化标题
        mSongView.setText(songName);
		/*
		 * if (albumName.contains(artistName)) { mArtistView.setText(albumName);
		 * } else { mArtistView.setText(artistName); }
		 */
        mArtistView.setText(artistName + "·" + albumName);
        mImgViewLayout1 = LayoutInflater.from(HBSearchLyricActivity.this).inflate(R.layout.hb_search_img_item, null);
        mImgViewLayout2 = LayoutInflater.from(HBSearchLyricActivity.this).inflate(R.layout.hb_search_img_item, null);
    }

    private class SearchSongTask extends AsyncTask<Void, Void, List<OnlineSong>> {

        @Override
        protected List<OnlineSong> doInBackground(Void... params) {

            return XiaMiSdkUtils.findSongByNameSync(getApplicationContext(), songName, artistName, null);
        }

        @Override
        protected void onPostExecute(List<OnlineSong> onlineSongs) {
            super.onPostExecute(onlineSongs);
            if (onlineSongs == null || onlineSongs.isEmpty()) {
                showSearchResult(0);
                return;
            }
            mViews = new ArrayList<View>();
            // 添加默认词图
//			mImgViews.add(mImgViewLayout1);
//			initSearchImgView(0, DEFUALT_IMG_URL);
//			imgCount++;
            for (OnlineSong onlineSong : onlineSongs) {
//				HBMusicUtil.ShowOnlineSong(onlineSong);
                String imgUrl = ImageUtil.transferImgUrl(onlineSong.getImageUrl(), 330);
                String lrcUrl = onlineSong.getLyric();
                if (imgUrl != null && !imgUrl.isEmpty()) {
                    if (imgCount < SEARCH_COUNT) {

                        if (!mImgUrl.contains(imgUrl)) {

                            if (imgCount == 0) {
                                mImgViews.add(mImgViewLayout1);
                            } else if (imgCount == 3) {
                                mImgViews.add(mImgViewLayout2);
                            }
                            initSearchImgView(imgCount, imgUrl);
                            imgCount++;
                        }
                    }
                }

                if (lrcUrl == null || lrcUrl.isEmpty()) {
                    continue;
                }

                if (mViews.size() == SEARCH_COUNT) {
                    continue;
                }
                mPlayerLrcLayout = LayoutInflater.from(HBSearchLyricActivity.this).inflate(R.layout.hb_search_lyric_show_info, null);
                mViews.add(mPlayerLrcLayout);
                viewCount++;
                new SearchLyricTask(mPlayerLrcLayout, viewCount).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, lrcUrl);
            }
            showSearchResult(mViews.size());
        }

    }


	/*
	 * private LrcPicSearchListener mLrcPicSearchListener = new
	 * LrcPicSearchListener() {
	 * 
	 * @Override public void onGetLrcPicList(LrcPicList arg0) { //
	 * LogUtil.d(TAG, "onGetLrcPicList:" + arg0); mViews = new
	 * ArrayList<View>(); // 添加默认词图 mImgViews.add(mImgViewLayout1);
	 * initSearchImgView(0, DEFUALT_IMG_URL); imgCount++;
	 * 
	 * if (arg0 == null) { showSearchResult(0); return; } List<LrcPic> list =
	 * arg0.getItems(); // LogUtil.d(TAG, "list:" + list.size()); if (list ==
	 * null) { showSearchResult(0); return; }
	 * 
	 * for (int i = 0; i < list.size(); i++) { String lrc =
	 * list.get(i).getLrclink(); String imgUrl = list.get(i).getPicBig(); //
	 * LogUtil.d(TAG, "lrc " + i + ":" + lrc);
	 * 
	 * if (imgUrl == null || imgUrl.isEmpty()) { imgUrl =
	 * list.get(i).getPicHuge(); }
	 * 
	 * if (imgUrl == null || imgUrl.isEmpty()) { imgUrl =
	 * list.get(i).getPicSmall(); } if (imgUrl != null && !imgUrl.isEmpty()) {
	 * if (imgCount < SEARCH_COUNT) {
	 * 
	 * if (!mImgUrl.contains(imgUrl)) {
	 * 
	 * if (imgCount == 0) { mImgViews.add(mImgViewLayout1); } else if (imgCount
	 * == 3) { mImgViews.add(mImgViewLayout2); } initSearchImgView(imgCount,
	 * imgUrl); imgCount++; } } } if (lrc == null || lrc.isEmpty()) { continue;
	 * } String houzhui = lrc.substring(lrc.lastIndexOf(".") + 1); //
	 * LogUtil.d(TAG, "houzhui:" + houzhui); if (!houzhui.equals("lrc")) {
	 * continue; }
	 * 
	 * if (mViews.size() == SEARCH_COUNT) { continue; } mPlayerLrcLayout =
	 * LayoutInflater.from( HBSearchLyricActivity.this).inflate(
	 * R.layout.hb_search_lyric_show_info, null);
	 * mViews.add(mPlayerLrcLayout); viewCount++; new
	 * SearchLyricTask(mPlayerLrcLayout, viewCount)
	 * .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, lrc); } // viewCount =
	 * mViews.size(); if (viewCount == 0) { showSearchResult(0); } } };
	 */

    private void initSearchImgView(final int postion, String url) {
        // LogUtil.d(TAG, "url "+postion+" :" + url);
        if (url == null || url.isEmpty()) {
            return;
        }
        CheckBox checkView = null;
        ImageView showImge = null;
        View imgViewLayout = null;
        switch (postion) {
            case 0:
                imgViewLayout = mImgViewLayout1.findViewById(R.id.id_img_1);
                checkView = (CheckBox) mImgViewLayout1.findViewById(R.id.id_img_checked_1);
                showImge = (ImageView) mImgViewLayout1.findViewById(R.id.hb_show_img_1);
                break;
            case 1:
                imgViewLayout = mImgViewLayout1.findViewById(R.id.id_img_2);
                checkView = (CheckBox) mImgViewLayout1.findViewById(R.id.id_img_checked_2);
                showImge = (ImageView) mImgViewLayout1.findViewById(R.id.hb_show_img_2);
                break;
            case 2:
                imgViewLayout = mImgViewLayout1.findViewById(R.id.id_img_3);
                checkView = (CheckBox) mImgViewLayout1.findViewById(R.id.id_img_checked_3);
                showImge = (ImageView) mImgViewLayout1.findViewById(R.id.hb_show_img_3);
                break;
            case 3:
                imgViewLayout = mImgViewLayout2.findViewById(R.id.id_img_1);
                checkView = (CheckBox) mImgViewLayout2.findViewById(R.id.id_img_checked_1);
                showImge = (ImageView) mImgViewLayout2.findViewById(R.id.hb_show_img_1);
                break;
            case 4:
                imgViewLayout = mImgViewLayout2.findViewById(R.id.id_img_2);
                checkView = (CheckBox) mImgViewLayout2.findViewById(R.id.id_img_checked_2);
                showImge = (ImageView) mImgViewLayout2.findViewById(R.id.hb_show_img_2);
                break;
            case 5:
                imgViewLayout = mImgViewLayout2.findViewById(R.id.id_img_3);
                checkView = (CheckBox) mImgViewLayout2.findViewById(R.id.id_img_checked_3);
                showImge = (ImageView) mImgViewLayout2.findViewById(R.id.hb_show_img_3);
                break;
        }
        if (checkView == null || showImge == null || imgViewLayout == null) {
            return;
        }
        imgViewLayout.setVisibility(View.VISIBLE);

        if (postion == 0) {
            checkView.setChecked(true);
        } else {
            checkView.setChecked(false);
        }
        mImgcChecks.put(postion, checkView);
        mImgUrl.add(url);
        checkView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                for (int i = 0; i < mImgcChecks.size(); i++) {
                    if (i == postion) {
                        mImgcChecks.get(i).setChecked(true);
                    } else {
                        mImgcChecks.get(i).setChecked(false);
                    }
                }
            }
        });
        imgViewLayout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                for (int i = 0; i < mImgcChecks.size(); i++) {
                    if (i == postion) {
                        mImgcChecks.get(i).setChecked(true);
                    } else {
                        mImgcChecks.get(i).setChecked(false);
                    }
                }
            }
        });
        if (url.equals(DEFUALT_IMG_URL)) {
            showImge.setImageResource(R.drawable.hb_defualt_search);
        } else if (url.equals(HB_ORIGINAL_IMG_URL)) {
            showImge.setImageBitmap(mOriginalBm);
        } else {

        }
    }
//
//    private OnHbActionBarItemClickListener mOnHbActionBarItemClickListener = new OnHbActionBarItemClickListener() {
//        @Override
//        public void onHbActionBarItemClicked(int itemId) {
//            switch (itemId) {
//                case 0:
//                    LogUtil.d(TAG, "mViews.size():" + mViews.size() + " imgCount:" + imgCount + " isLoadFinish:" + isLoadFinish);
//                    if ((mViews.size() == 0 && imgCount == 0) || !isLoadFinish || isSelected) {
//                        break;
//                    }
//                    isSelected = true;
//                    String selectUrl = getSelectImgUrl();
//                    new DonloadImgTask().execute(selectUrl);
//
//                    break;
//            }
//        }
//
//    };

    private ViewPager.OnPageChangeListener mOnPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageScrollStateChanged(int arg0) {

        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        @Override
        public void onPageSelected(int arg0) {
 //           mHbDotView.setSelectDot(arg0);

        }

    };

    private ViewPager.OnPageChangeListener mOnPageChangeListener1 = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageScrollStateChanged(int arg0) {

        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        @Override
        public void onPageSelected(int arg0) {
//            mImgDotView.setSelectDot(arg0);
        }

    };

    class SearchLyricTask extends AsyncTask<String, Void, Boolean> {

        private HBSearchLyricView mLyricView;
        private View layoutView;
        private CheckBox checkedView;

        private int mPostion;

        public SearchLyricTask(View view, int pos) {
            layoutView = view;
            mPostion = pos;
            initLyricView(view);
        }

        @Override
        protected Boolean doInBackground(String... arg0) {
            String lyricUrl = arg0[0];
            HttpGet get = new HttpGet(lyricUrl);
            HttpClient client = new DefaultHttpClient();
            client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 30 * 1000);
            client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 30 * 1000);
            try {
                HttpResponse response = client.execute(get);
                int code = response.getStatusLine().getStatusCode();
                // LogUtil.d(TAG, "code:" + code);
                if (code == HttpStatus.SC_OK) {
                    HttpEntity entity = response.getEntity();
                    long length = entity.getContentLength();
                    // LogUtil.d(TAG, "length:" + length);
                    if (mLyricView != null && length > 0) {
                        InputStream is = entity.getContent();
                        String ext = HBMusicUtil.getExtFromFilename(lyricUrl);
                        if (TextUtils.isEmpty(ext)) {
                            return false;
                        }
                        mLyricView.setFileName(songName + "-" + artistName + mPostion + "." + ext);
                        return mLyricView.read(is);
                    } else {
                        // mViews.remove(layoutView);
                        return false;
                    }
                } else {
                    // mViews.remove(layoutView);
                    return false;
                }
            } catch (Exception e) {
                // mViews.remove(layoutView);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result != null && !result) {
                mViews.remove(layoutView);
            } else {
                mLyricView.setTextsEx();
            }
            downloadCount++;

            if (downloadCount == viewCount) {
                showSearchResult(mViews.size());
                if (mViews.size() == 0) {
                    return;
                }
                View view = mViews.get(0);
                if (view != null) {
                    ((CheckBox) view.findViewById(R.id.hb_checked)).setChecked(true);
                }
            }
        }

        @Override
        protected void onPreExecute() {

        }

        private void initLyricView(View lrcView) {
            if (lrcView == null) {
                return;
            }
            mLyricView = (HBSearchLyricView) (lrcView.findViewById(R.id.lyric_view));
            checkedView = (CheckBox) lrcView.findViewById(R.id.hb_checked);

            if (mPostion == 1) {
                checkedView.setChecked(true);
            } else {
                checkedView.setChecked(false);
            }

            mLyricChecks.put(mPostion, checkedView);
            checkedView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    // LogUtil.d(TAG, "onClick :" + mPostion);
                    for (int i = 1; i <= mLyricChecks.size(); i++) {
                        if (i != mPostion) {
                            mLyricChecks.get(i).setChecked(false);
                        } else {
                            mLyricChecks.get(i).setChecked(true);
                        }
                    }
                }
            });
        }
    }

    private void changeViewState(boolean is) {
        mProgressBar.setVisibility(is ? View.GONE : View.VISIBLE);
        findViewById(R.id.hb_search_main).setVisibility(is ? View.VISIBLE : View.GONE);
    }

    private void showSearchResult(final int size) {

        mPlayerLrcLayout = LayoutInflater.from(HBSearchLyricActivity.this).inflate(R.layout.hb_search_lyric_show_info, null);
        CheckBox checkedView = (CheckBox) mPlayerLrcLayout.findViewById(R.id.hb_checked);
        mPlayerLrcLayout.findViewById(R.id.lyric_scrollview).setVisibility(View.GONE);
        mPlayerLrcLayout.findViewById(R.id.id_defualt_lyric).setVisibility(View.VISIBLE);
        mViews.add(mPlayerLrcLayout);
        if (size == 0) {
            checkedView.setChecked(true);
        }
        mLyricChecks.put(size + 1, checkedView);
        checkedView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // LogUtil.d(TAG, "onClick :" + mPostion);
                for (int i = 1; i <= mLyricChecks.size(); i++) {
                    if (i != size + 1) {
                        mLyricChecks.get(i).setChecked(false);
                    } else {
                        mLyricChecks.get(i).setChecked(true);
                    }
                }
            }
        });
        //mHbDotView.setDotCount(size + 1);
        showLyricSize.setText(getString(R.string.hb_lyricsize, size));
        HBPlayerPagerAdapter mPagerAdapter = new HBPlayerPagerAdapter(mViews);
        //mHbViewPager.setAdapter(mPagerAdapter);
        //mHbViewPager.setCurrentItem(0);
        //mHbViewPager.setOnPageChangeListener(mOnPageChangeListener);

        // 图片选择

        if (imgCount == 0) {
            mImgViews.add(mImgViewLayout1);
        } else if (imgCount == 3) {
            mImgViews.add(mImgViewLayout2);
        }

        initSearchImgView(imgCount, DEFUALT_IMG_URL);
        imgCount++;
        //mImgDotView.setDotCount(imgCount > 3 ? 2 : 1);
        mShowImgSize.setText(getString(R.string.hb_search_img_size, imgCount - 1));
        HBPlayerPagerAdapter mImgAdapter = new HBPlayerPagerAdapter(mImgViews);
        //mShowImgViewPage.setAdapter(mImgAdapter);
        //mShowImgViewPage.setCurrentItem(0);
        //mShowImgViewPage.setOnPageChangeListener(mOnPageChangeListener1);
        isLoadFinish = true;
        changeViewState(true);
    }

    @Override
    protected void onDestroy() {
        mRunning = true;
        if (mHandlerThread != null) {
            mHandlerThread.getLooper().quit();
        }


        if (mOriginalBm != null && !mOriginalBm.isRecycled()) {
            mOriginalBm.recycle();
            mOriginalBm = null;
        }

        deleteAll(new File(Globals.mLycPath_temp));
        super.onDestroy();
    }

    // 递归删除文件夹下所有
    public static void deleteAll(File file) {
        if (file.isFile()) {
            file.delete();
            return;
        }

        if (file.isDirectory()) {
            File[] childFiles = file.listFiles();
            if (childFiles == null || childFiles.length == 0) {
                file.delete();
                return;
            }

            for (int i = 0; i < childFiles.length; i++) {
                deleteAll(childFiles[i]);
            }
        }
    }

    /**
     * 复制单个文件
     * @param oldPath
     *            String 原文件路径 如：c:/fqf.txt
     * @param newPath
     *            String 复制后路径 如：f:/fqf.txt
     * @return boolean
     */
    public void copyFile(String newPath, String oldPath) {
        // LogUtil.d(TAG, "oldPath:" + oldPath + " newPath:" + newPath);
        InputStream inStream = null;
        FileOutputStream fs = null;
        try {

            File newFold = new File(Globals.mLycPath);
            if (!newFold.exists()) {
                newFold.mkdirs();
            }

            int byteread = 0;
            File oldfile = new File(oldPath);
            if (oldfile.exists()) { // 文件存在时
                inStream = new FileInputStream(oldPath); // 读入原文件
                fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[2048];
                while ((byteread = inStream.read(buffer)) != -1) {
                    fs.write(buffer, 0, byteread);
                }
            }
        } catch (Exception e) {

            e.printStackTrace();
        } finally {
            if (fs != null) {
                try {
                    fs.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void saveDefualtLyric(String path) {
        FileOutputStream fs = null;
        try {
            File newFold = new File(Globals.mLycPath);
            if (!newFold.exists()) {
                newFold.mkdirs();
            }
            fs = new FileOutputStream(path);
            fs.write("defualt".getBytes());
        } catch (Exception e) {

        } finally {
            if (fs != null) {
                try {
                    fs.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private int getSelectLyricPostion() {

        for (int i = 1; i <= mLyricChecks.size(); i++) {
            if (mLyricChecks.get(i).isChecked()) {
                return i;
            }
        }
        return 1;
    }

    private String getSelectImgUrl() {

        for (int i = 0; i < mImgcChecks.size(); i++) {
            if (mImgcChecks.get(i).isChecked()) {

                return mImgUrl.get(i);
            }
        }
        return null;
    }

    class DonloadImgTask extends AsyncTask<String, Void, Void> {

        private String imgfilepath = null;
        private String lyricPathString = null;
        private String titleString = null;

        @Override
        protected Void doInBackground(String... arg0) {
            String imgUrl = arg0[0];
            String artstiString = null;
            String albumString = null;
            long tid = Long.parseLong(songid);
            if (MusicUtils.sService != null) {
                try {
                    titleString = MusicUtils.sService.getTrackName();
                    artstiString = MusicUtils.sService.getArtistName();
                    albumString = MusicUtils.sService.getAlbumName();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            if (titleString == null) {
                titleString = songName;
            }

            if (artstiString == null) {
                artstiString = artistName;
            }

            if (albumString == null) {
                albumString = albumName;
            }
            if (imgUrl != null) {
                File file = new File(Globals.mSongImagePath);
                if (!file.exists()) {
                    file.mkdirs();
                }

                String musicbite = "128";
                if (MusicUtils.sService != null) {
                    try {
                        musicbite = MusicUtils.sService.getMusicbitrate();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    if (musicbite == null || musicbite.isEmpty()) {
                        musicbite = "128";
                    }
                }

                if (mImgPath != null) {
                    imgfilepath = mImgPath;
                }

                if (imgUrl.equals(DEFUALT_IMG_URL)) {
                    LogUtil.d(TAG, "1addToAudioInfoLrcEx");
                    MusicUtils.addToAudioInfoLrcEx(HBSearchLyricActivity.this, tid, titleString, artstiString, DEFUALT_IMG_URL, null, 0);
                    imgfilepath = DEFUALT_IMG_URL;
                } else if (imgUrl.equals(HB_ORIGINAL_IMG_URL)) {
                    LogUtil.d(TAG, "2addToAudioInfoLrcEx");
                    MusicUtils.addToAudioInfoLrcEx(HBSearchLyricActivity.this, tid, titleString, artstiString, HB_ORIGINAL_IMG_URL, null, 0);
                    imgfilepath = HB_ORIGINAL_IMG_URL;
                } else {
                    HttpGet get = new HttpGet(imgUrl);
                    HttpClient client = new DefaultHttpClient();
                    client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 30 * 1000);
                    client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 30 * 1000);
                    InputStream input = null;
                    FileOutputStream out = null;
                    try {
                        HttpResponse response = client.execute(get);
                        int code = response.getStatusLine().getStatusCode();
                        if (code == HttpStatus.SC_OK) {
                            HttpEntity entity = response.getEntity();

                            input = entity.getContent();
                            out = new FileOutputStream(imgfilepath);
                            byte[] buffer = new byte[1024];
                            int size = 0;
                            while ((size = input.read(buffer)) != -1) {
                                out.write(buffer, 0, size);
                            }

                            MusicUtils.addToAudioInfoLrcEx(HBSearchLyricActivity.this, tid, titleString, artstiString, imgfilepath, null, 0);

                        }
                    } catch (Exception e) {

                        if (imgfilepath != null) {
                            File file1 = new File(imgfilepath);
                            if (file1.exists()) {
                                file1.delete();
                            }
                        }
                        e.printStackTrace();
                    } finally {
                        if (input != null) {
                            try {
                                input.close();
                                input = null;
                            } catch (IOException e) {

                                e.printStackTrace();
                            }
                        }
                        if (out != null) {
                            try {
                                out.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            lyricPathString = HBMusicUtil.getLrcPathInfo(titleString, artstiString);

            int lyricPostion = getSelectLyricPostion();
            if (lyricPostion == mViews.size()) {
                saveDefualtLyric(lyricPathString);
            } else {
                String ext = HBMusicUtil.getExtFromFilename(lyricPathString);
                copyFile(lyricPathString, Globals.mLycPath_temp + File.separator + songName + "-" + artistName + lyricPostion + "." + ext);

                if (tid < 0) {
                    tid = 0;
                }
            }
            MusicUtils.addToAudioInfoLrc(HBSearchLyricActivity.this, tid, titleString, artstiString, lyricPathString);

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // add by tangjie 2014/09/01
            HBMusicUtil.removeBitmap(songid, titleString);
            // add end
            // mProgressDialog.dismiss();
            isSelected = false;
            Intent intent = new Intent();
            intent.putExtra(EXTR_NAME, songName);
            intent.putExtra(EXTR_PATH, lyricPathString);
            intent.putExtra(EXTR_IMG_PATH, imgfilepath);
            intent.putExtra(EXTR_ID, songid);
            setResult(RESULT_OK, intent);
            finish();
        }

        @Override
        protected void onPreExecute() {
            // showProgress();
            changeViewState(false);
        }

    }

    private ProgressDialog mProgressDialog;

    private void showProgress() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setTitle("正在选择词图");
        }
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();
    }
}

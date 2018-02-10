package com.protruly.music.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.protruly.music.R;
import com.protruly.music.downloadex.DownloadInfo;
import com.protruly.music.downloadex.DownloadManager;
import com.protruly.music.downloadex.DownloadStatusListener;
import com.protruly.music.online.HBMusicDownloadManager;
import com.protruly.music.util.FlowTips;
import com.protruly.music.util.FlowTips.OndialogClickListener;
import com.protruly.music.util.Globals;
import com.protruly.music.util.HBMusicUtil;
import com.protruly.music.util.LogUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import hb.widget.HbListView;

/**
 * Created by hujianwei on 17-9-4.
 */

public class HBMusicDownloadAdapter extends BaseAdapter {
    private static final String TAG = "HBMusicDownloadAdapter";
    private Context mContext;
    private List<DownloadInfo> datas = new ArrayList<DownloadInfo>();
    private static String mArtistName;
    private static String mAlbumName;
    public final static String ACTION_DIR_SCAN = "android.intent.action.HB_DIRECTORY_SCAN";
    private Handler mHandler = new Handler();
    private boolean isLeftDelete = false;
    private boolean isEditMode = false;
    private boolean mNeedin = false, mNeedout = false;
    private HashMap<DownloadInfo, Boolean> mCheckedMap = new HashMap<DownloadInfo, Boolean>();
    //private HashMap<Long, HbRoundProgressBar> storeViews = new HashMap<Long, HbRoundProgressBar>();
    private ListView mListView;
    private BitmapFactory.Options options;
    private DisplayImageOptions disoptions;

    public HBMusicDownloadAdapter(Context context, ListView view) {
        this.mContext = context;
        mArtistName = context.getString(R.string.unknown);
        mAlbumName = context.getString(R.string.unknown_album_name);
        mListView = view;
        options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(), R.drawable.default_music_icon, options);
        initImageCacheParams();
    }

    public HBMusicDownloadAdapter(Context context, List<DownloadInfo> list) {
        this.mContext = context;
        this.datas = list;
        mArtistName = context.getString(R.string.unknown);
        mAlbumName = context.getString(R.string.unknown_album_name);
        initImageCacheParams();
    }

    public void setDatas(List<DownloadInfo> list) {
        if (list == null) {
            return;
        }
        datas = list;
    }

    @Override
    public int getCount() {

        return datas.size();
    }

    @Override
    public Object getItem(int arg0) {

        return datas.get(arg0);
    }

    @Override
    public long getItemId(int arg0) {

        return arg0;
    }

    public void setNeedIn() {
        mNeedin = true;
        isEditMode = true;
        mCheckedMap.clear();
        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                mNeedin = false;
            }
        }, 300);
    }

    public void setNeedIn(int position) {
        mNeedin = true;
        isEditMode = true;
        mCheckedMap.clear();
        mCheckedMap.put(datas.get(position), true);
        notifyDataSetChanged();
        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                mNeedin = false;
            }
        }, 300);
    }

    public void setNeedOut() {
        mNeedout = true;
        isEditMode = false;
        mCheckedMap.clear();
        notifyDataSetChanged();
        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                mNeedout = false;
            }
        }, 300);
    }

    /**
     * 全选
     */
    public void selectAll() {
        LogUtil.d(TAG, "selectAll");
        for (int i = 0; i < datas.size(); i++) {
            mCheckedMap.put(datas.get(i), true);
        }
        notifyDataSetChanged();
    }

    /**
     * 反选
     */
    public void selectAllNot() {
        mCheckedMap.clear();
        notifyDataSetChanged();
    }

    /**
     * 选择个数
     * @return
     */
    public int getCheckedCount() {
        return mCheckedMap.size();
    }

    @Override
    public View getView(int arg0, View arg1, ViewGroup arg2) {

        HoldView holdView;
        if (arg1 == null) {
            holdView = new HoldView();
//            arg1 = LayoutInflater.from(mContext).inflate(com.hb.R.layout.HB_slid_listview, null);
//            holdView.front = (RelativeLayout) arg1.findViewById(com.hb.R.id.hb_listview_front);
//            LayoutInflater.from(mContext).inflate(R.layout.HB_songsingle_item, holdView.front);
//            RelativeLayout rl_control_padding = (RelativeLayout) arg1.findViewById(com.hb.R.id.control_padding);
//            rl_control_padding.setPadding(0, 0, 0, 0);
//            holdView.icon = (ImageView) arg1.findViewById(R.id.song_playicon);
//            holdView.songName = (TextView) arg1.findViewById(R.id.song_title);
//            holdView.songAlbum = (TextView) arg1.findViewById(R.id.song_artist);
//            holdView.songPlay = (ImageView) arg1.findViewById(R.id.iv_song_selected);
//            holdView.progress = (HbRoundProgressBar) arg1.findViewById(R.id.hb_btn_recommend_download);
//            holdView.cb = (CheckBox) arg1.findViewById(com.hb.R.id.hb_list_left_checkbox);

            arg1.setTag(holdView);
        } else {
            holdView = (HoldView) arg1.getTag();
        }

        ViewGroup.LayoutParams vl = arg1.getLayoutParams();
        if (null != vl) {
            vl.height = mContext.getResources().getDimensionPixelSize(R.dimen.hb_song_item_height);
        }
        //arg1.findViewById(com.hb.R.id.content).setAlpha(255);
        //arg1.setAlpha(255);

        // 操作复选框
//        if (mNeedin) {
//            HbListView.hbStartCheckBoxAppearingAnim(holdView.front, holdView.cb);
//        } else if (!mNeedin && isEditMode) {
//            HbListView.hbSetCheckBoxVisible(holdView.front, holdView.cb, true);
//        }

//        if (mNeedout) {
//            HbListView.hbStartCheckBoxDisappearingAnim(holdView.front, holdView.cb);
//        } else if (!isEditMode && !mNeedout) {
//            HbListView.hbSetCheckBoxVisible(holdView.front, holdView.cb, false);
//        }

        final DownloadInfo info = datas.get(arg0);
        if (isEditMode) {
            final int position = arg0;
            final CheckBox checkbox = (CheckBox) holdView.cb;
            arg1.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    LogUtil.d(TAG, "onClick..." + arg0);
                    if (!checkbox.isChecked()) {
                       // checkbox.hbSetChecked(true, true);
                        mCheckedMap.put(datas.get(position), true);
                    } else {
                        //checkbox.hbSetChecked(false, true);
                        mCheckedMap.remove(datas.get(position));
                    }
                    ((HBMusicDownloadManager) mContext).changeMenuState();
                }
            });
            Boolean checked = mCheckedMap.get(info);
            if (checked != null && checked) {
                ((CheckBox) holdView.cb).setChecked(true);
            } else {
                ((CheckBox) holdView.cb).setChecked(false);
            }
            //holdView.progress.setVisibility(View.GONE);
        } else {
            //holdView.progress.setVisibility(View.VISIBLE);
            arg1.setClickable(false);
        }

        if (info != null) {

            holdView.songPlay.setVisibility(View.GONE);

            holdView.songName.setText(info.getTitle());

            StringBuffer tBuffer = new StringBuffer();
            String artiststr = info.getArtist();
            if (artiststr == null || MediaStore.UNKNOWN_STRING.equals(artiststr)) {
                artiststr = mArtistName;
            }

            String albumstr = info.getAlbum();
            if (albumstr == null || MediaStore.UNKNOWN_STRING.equals(albumstr)) {
                albumstr = mAlbumName;
            }
            tBuffer.append(artiststr);
            tBuffer.append("·");
            tBuffer.append(albumstr);
            holdView.songAlbum.setText(tBuffer.toString());

            ImageLoader.getInstance().displayImage(info.getImgUrl(), holdView.icon, disoptions);

            //addStoreViews(holdView.progress, info.getId());
            //changeDownloadButtonState(info.getState().value(), holdView.progress);
            //holdView.progress.setMax(info.getFileLength());
            //holdView.progress.setProgress(info.getProgress());
            final int postion = arg0;
            DownloadManager.getInstance(mContext.getApplicationContext()).setDownloadListener(info.getId(), new MusicDownloadStatusListener(info, postion));
//            holdView.progress.setOnClickListener(new View.OnClickListener() {
//
//                @Override
//                public void onClick(View arg0) {
//                    try {
//                        boolean result = false;
//                        switch (info.getState().value()) {
//                            case 0:
//                            case 1:
//                            case 2:
//                                DownloadManager.getInstance(mContext.getApplicationContext()).pauseDownloadById(info.getId());
//                                result = isDownloading();
//                                break;
//                            case 3:
//                            case 4:
//                            case 6:
//                                if (!HBMusicUtil.isNetWorkActive(mContext)) {
//                                    Toast.makeText(mContext, R.string.hb_network_error, Toast.LENGTH_SHORT).show();
//                                    return;
//                                }
//                                if (FlowTips.showDownloadFlowTips(mContext, new OndialogClickListener() {
//
//                                    @Override
//                                    public void OndialogClick() {
//                                        DownloadManager.getInstance(mContext.getApplicationContext()).resumeDownloadById(info.getId());
//                                        DownloadManager.getInstance(mContext.getApplicationContext()).setDownloadListener(info.getId(), new MusicDownloadStatusListener(info, postion));
//                                        notifyDataSetChanged();
//                                        ((HBMusicDownloadManager) mContext).notifySongsizeChange(true);
//                                    }
//                                })) {
//                                    return;
//                                }
//                                DownloadManager.getInstance(mContext.getApplicationContext()).resumeDownloadById(info.getId());
//                                DownloadManager.getInstance(mContext.getApplicationContext()).setDownloadListener(info.getId(), new MusicDownloadStatusListener(info, postion));
//                                result = true;
//                                break;
//                            default:
//                                break;
//                        }
//                        notifyDataSetChanged();
//                        ((HBMusicDownloadManager) mContext).notifySongsizeChange(result);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            });
        }
        return arg1;
    }

    class HoldView {
        ImageView icon;
        TextView songName;
        TextView songAlbum;
        ImageView songPlay;
        //HbRoundProgressBar progress;
        CheckBox cb;
        RelativeLayout front;
    }

    public boolean onItemclick(final DownloadInfo info, final int postion) {
        if (info == null) {
            return false;
        }
        boolean result = false;
        try {
            // LogUtil.d(TAG, "onClick info.getState:" + info.getState() +
            // " postion:" + postion);
            switch (info.getState().value()) {
                case 0:
                case 1:
                case 2:
                    DownloadManager.getInstance(mContext.getApplicationContext()).pauseDownloadById(info.getId());
                    result = isDownloading();
                    break;
                case 3:
                case 4:
                case 6:
                    if (!HBMusicUtil.isNetWorkActive(mContext)) {
                        Toast.makeText(mContext, R.string.hb_network_error, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    if (FlowTips.showDownloadFlowTips(mContext, new OndialogClickListener() {

                        @Override
                        public void OndialogClick() {
                            DownloadManager.getInstance(mContext.getApplicationContext()).resumeDownloadById(info.getId());
                            DownloadManager.getInstance(mContext.getApplicationContext()).setDownloadListener(info.getId(), new MusicDownloadStatusListener(info, postion));
                            notifyDataSetChanged();
                            ((HBMusicDownloadManager) mContext).notifySongsizeChange(true);
                        }
                    })) {
                        return isDownloading();
                    }
                    DownloadManager.getInstance(mContext.getApplicationContext()).resumeDownloadById(info.getId());
                    DownloadManager.getInstance(mContext.getApplicationContext()).setDownloadListener(info.getId(), new MusicDownloadStatusListener(info, postion));
                    result = true;
                    break;
                default:
                    break;
            }
            notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

//    public void changeDownloadButtonState(int state, HbRoundProgressBar downloadButton) {
//        switch (state) {
//            case 0:
//                downloadButton.setBackgroundResource(R.drawable.hb_download_wait_bg);
//                break;
//            case 1:
//            case 2:
//                downloadButton.setBackgroundResource(R.drawable.hb_download_pause_bg);
//                break;
//            case 7:
//                downloadButton.setBackgroundResource(R.drawable.hb_download_started_bg);
//                break;
//            case 3:
//            case 6:
//                downloadButton.setBackgroundResource(R.drawable.hb_download_started_bg);
//                break;
//            case 4:
//                downloadButton.setBackgroundResource(R.drawable.hb_download_error_bg);
//                ((HBMusicDownloadManager) mContext).notifySongsizeChange(isDownloading());
//                break;
//            default:
//                downloadButton.setBackgroundResource(R.drawable.hb_download_btn_bg);
//                break;
//        }
//    }

    public void setLeftDelete(boolean is, boolean isfromtouch) {

        if (isLeftDelete == is) {
            return;
        }
        isLeftDelete = is;
        if (!isLeftDelete && !isfromtouch)
            notifyDataSetChanged();
    }

    public boolean canDelete(DownloadInfo info) {
        return datas.contains(info);
    }

    public void deleteItem(DownloadInfo info) {

        if (info == null || !datas.contains(info)) {
            return;
        }
        try {
            DownloadManager.getInstance(mContext.getApplicationContext()).removeDownloadById(info.getId());
            datas.remove(info);
            notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
        setLeftDelete(false, false);
    }

    public int getDatasNum(){
        if(mCheckedMap!=null){
            return mCheckedMap.size();
        }
        return 0;
    }

    public void deleteSelectItem() {
        try {
            Iterator<DownloadInfo> it = datas.iterator();
            while (it.hasNext()) {
                DownloadInfo info = it.next();
                if (mCheckedMap.containsKey(info)) {
                    DownloadManager.getInstance(mContext.getApplicationContext()).removeDownloadById(info.getId());
                    it.remove();
                }
            }
            notifyDataSetChanged();
        } catch (Exception e) {

        }
    }

    private void notifyScane() {
        // 通知扫描数据库
        Intent intent = new Intent();
        intent.setAction(ACTION_DIR_SCAN);
        intent.setData(Uri.fromFile(new File(Globals.mSavePath)));
        mContext.sendBroadcast(intent);
    }

    public boolean isDownloading() {

        return DownloadManager.getInstance(mContext.getApplicationContext()).isDownloading();


    }

    public void pauseOrDownloadAll(final boolean is) {
        try {
            if (is) {
                DownloadManager.getInstance(mContext.getApplicationContext()).pauseAllDownload();
            } else {
                if (FlowTips.showDownloadFlowTips(mContext, new OndialogClickListener() {

                    @Override
                    public void OndialogClick() {
                        resumeAllDownload();
                        mHandler.post(new Runnable() {

                            @Override
                            public void run() {
                                notifyDataSetChanged();
                                ((HBMusicDownloadManager) mContext).notifySongsizeChange(true);
                            }
                        });
                    }
                })) {
                    ((HBMusicDownloadManager) mContext).notifySongsizeChange(isDownloading());
                    return;
                }
                resumeAllDownload();
            }
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void resumeAllDownload() {
        HashMap<Long, DownloadStatusListener> maps = new HashMap<Long, DownloadStatusListener>();

        for (int i = 0; i < datas.size(); i++) {
            DownloadInfo info = datas.get(i);
            maps.put(datas.get(i).getId(), new MusicDownloadStatusListener(info, i));
			/*
			 * DownloadInfo info=datas.get(i);
			 * DownloadManager.getInstance(mContext
			 * .getApplicationContext()).resumeDownloadById(info.getId());
			 * DownloadManager
			 * .getInstance(mContext.getApplicationContext()).setDownloadListener
			 * (info.getId(), new MusicDownloadStatusListener(info,i));
			 */
        }

        DownloadManager.getInstance(mContext.getApplicationContext()).resumeAllDownloadAndListener(datas, maps);
    }

//    private void addStoreViews(HbRoundProgressBar view, long id) {
//        HbRoundProgressBar oldView = storeViews.get(id);
//        if (oldView != null) {
//            if (!oldView.equals(view)) {
//                storeViews.remove(id);
//                storeViews.put(id, view);
//            }
//        } else {
//            storeViews.put(id, view);
//        }
//    }

    class MusicDownloadStatusListener implements DownloadStatusListener {

        private int postion;
        private DownloadInfo mDownloadInfo;

        public MusicDownloadStatusListener(DownloadInfo info, int position) {
            postion = position;
            mDownloadInfo = info;
        }

        @Override
        public void onDownload(String url, final long id, final int status, final long downloadSize, final long fileSize) {

//            final HbRoundProgressBar progressBar = storeViews.get(id);
//            if (progressBar == null) {
//                return;
//            }
//            progressBar.post(new Runnable() {
//
//                @Override
//                public void run() {
//                    int current = postion + 1;
//                    if (current >= mListView.getFirstVisiblePosition() && current <= mListView.getLastVisiblePosition()) {
//                        progressBar.setMax(fileSize);
//                        progressBar.setProgress(downloadSize);
//                    }
//                    if (mDownloadInfo.isDownloadOver()) {
//                        LogUtil.d(TAG, mDownloadInfo.getFileName() + " removed!!");
//                        storeViews.remove(id);
//                        if (mCheckedMap.containsKey(mDownloadInfo)) {
//                            mCheckedMap.remove(mDownloadInfo);
//                        }
//                        datas.remove(mDownloadInfo);
//                        notifyDataSetChanged();
//                        ((HBMusicDownloadManager) mContext).notifySongsizeChange();
//                    }
//                }
//            });

        }
    }

    private void initImageCacheParams() {
        disoptions = new DisplayImageOptions.Builder().showImageOnLoading(R.drawable.hb_new_album_mask).showImageForEmptyUri(R.drawable.hb_new_album_mask)
                .showImageOnFail(R.drawable.hb_new_album_mask).cacheInMemory(true).cacheOnDisk(true).considerExifParams(true).displayer(new SimpleBitmapDisplayer()).build();
    }
}

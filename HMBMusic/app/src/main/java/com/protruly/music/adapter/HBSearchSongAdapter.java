package com.protruly.music.adapter;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.protruly.music.MusicUtils;
import com.protruly.music.R;
import com.protruly.music.downloadex.DownloadInfo;
import com.protruly.music.downloadex.DownloadManager;
import com.protruly.music.downloadex.DownloadStatusListener;
import com.protruly.music.util.FlowTips;
import com.protruly.music.util.Globals;
import com.protruly.music.util.HBMusicUtil;
import com.protruly.music.util.LogUtil;
import com.xiami.sdk.entities.OnlineSong;

import java.io.File;
import java.util.HashMap;
import java.util.List;

/**
 * Created by hujianwei on 17-9-4.
 */

public class HBSearchSongAdapter extends BaseAdapter {
    private List<OnlineSong> mSongList;
    private Context mContext;
    private DownloadManager mDownloadManager;
    public static final int MSG_NOTIFY = 10000;
    public static final int MSG_UPDOWNLOAD = 10001;
    private static final String TAG="HBSearchSongAdapter";
    Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            if (msg.what == MSG_NOTIFY) {
                notifyDataSetChanged();
            } else if (msg.what == MSG_UPDOWNLOAD) {
                try {
                    OnlineSong onlineSong = (OnlineSong) msg.obj;
                    DownloadInfo downloadInfo = mDownloadManager.buildDownloadInfoByOnlineSong(onlineSong);
                    mDownloadManager.getDownloadManagerDb().savaOrUpdate(downloadInfo);
                    HashMap<Long, DownloadInfo> hashMap = mDownloadManager.getDownloadingMapData();
                    if (hashMap.get(onlineSong.getSongId()) == null) {
                        hashMap.put(onlineSong.getSongId(), downloadInfo);
                    }
                } catch (Exception e) {
                    LogUtil.e(TAG, "handleMessage save downloadInfo error ", e);
                }
                removeMessages(MSG_NOTIFY);
                sendEmptyMessageDelayed(MSG_NOTIFY, 1000);
            }
        };
    };
    DownloadStatusListener mDownloadStatusListener = new DownloadStatusListener() {

        @Override
        public void onDownload(String url, long id, final int status, final long downloadSize, final long fileSize) {
            mHandler.sendEmptyMessage(MSG_NOTIFY);
        };
    };


    class ViewHolder {
        ImageView image_album;
        //HbRoundProgressBar image_download;
        View download_parent;
        TextView tv_title;
        TextView tv_artist;
        TextView tv_top;
        ImageView iv_song_selected;
        ImageView iv_download_ok;
    }

    public HBSearchSongAdapter(Context context, List<OnlineSong> list) {
        mSongList = list;
        mContext = context;
        mDownloadManager = DownloadManager.getInstance(context);
    }

    @Override
    public int getCount() {
        if (mSongList == null) {
            return 0;
        }
        return mSongList.size();
    }

    @Override
    public Object getItem(int position) {
        if (mSongList != null) {
            return mSongList.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup arg2) {
        // TODO Auto-generated method stub
        ViewHolder viewHolder;
//        if (null == convertView) {
//            convertView = LayoutInflater.from(mContext).inflate(R.layout.hb_netablum_listitem, null);
//            viewHolder = new ViewHolder();
//            viewHolder.iv_download_ok = (ImageView) convertView.findViewById(R.id.hb_download_ok);
//            viewHolder.iv_song_selected = (ImageView) convertView.findViewById(R.id.iv_song_selected);
//            viewHolder.download_parent = convertView.findViewById(R.id.hb_btn_recommend_download_parent);
//            viewHolder.image_download = (HbRoundProgressBar) convertView.findViewById(R.id.hb_btn_recommend_download);
//            viewHolder.tv_title = (TextView) convertView.findViewById(R.id.song_title);
//            viewHolder.tv_artist = (TextView) convertView.findViewById(R.id.song_artist);
//            convertView.setTag(viewHolder);
//        } else {
//            viewHolder = (ViewHolder) convertView.getTag();
//        }
//        final OnlineSong item;
//        try {
//            item = (OnlineSong) mSongList.get(position);
//        } catch (Exception e) {
//            // TODO: handle exception
//            return convertView;
//        }
//        String trackTitle = item.getSongName();
//        String artistName = item.getArtistName();
//        if (artistName != null && artistName.length() > 12) {
//            artistName = artistName.substring(0, 12) + "…";
//        }
//        String albumName = item.getAlbumName();
//        if (trackTitle == null || TextUtils.isEmpty(trackTitle))
//            viewHolder.tv_title.setText(mContext.getResources().getString(R.string.unknown_track));
//        else
//            viewHolder.tv_title.setText(trackTitle);
//        if (artistName == null || TextUtils.isEmpty(artistName)) {
//            viewHolder.tv_artist.setText(mContext.getResources().getString(R.string.unknown_artist) + " · ");
//        } else {
//            viewHolder.tv_artist.setText(artistName + " · ");
//        }
//        if (albumName == null || TextUtils.isEmpty(albumName)) {
//            viewHolder.tv_artist.append(mContext.getResources().getString(R.string.unknown_album_name));
//        } else {
//            viewHolder.tv_artist.append(albumName);
//        }
//        long id = MusicUtils.getCurrentAudioId();
//        if (item.getSongId() == id) {
//            viewHolder.iv_song_selected.setVisibility(View.VISIBLE);
//        } else {
//            viewHolder.iv_song_selected.setVisibility(View.GONE);
//        }
//        final DownloadInfo downloadInfo = mDownloadManager.getDownloadingMapData().get(item.getSongId());
//        if (downloadInfo != null) {
//            viewHolder.image_download.setMax(downloadInfo.getFileLength());
//            viewHolder.image_download.setProgress(downloadInfo.getProgress());
//            changeDownloadButtonState(downloadInfo.getState().value(), viewHolder.image_download);
//            if (downloadInfo.getState().value() != 7) {
//                mDownloadManager.setDownloadListener(item.getSongId(), mDownloadStatusListener);
//                viewHolder.iv_download_ok.setVisibility(View.GONE);
//            } else {
//                viewHolder.iv_download_ok.setVisibility(View.VISIBLE);
//                if (downloadInfo.getFileSavePath() != null) {
//                    File file = new File(downloadInfo.getFileSavePath());
//                    if (!file.exists()) {
//                        viewHolder.image_download.setBackgroundResource(R.drawable.hb_download_btn_bg);
//                        viewHolder.iv_download_ok.setVisibility(View.GONE);
//                    }
//                }
//            }
//        } else {
//
//            if (isExsistDownlaod(item)) {
//                changeDownloadButtonState(7, viewHolder.image_download);
//                viewHolder.iv_download_ok.setVisibility(View.VISIBLE);
//                mHandler.obtainMessage(MSG_UPDOWNLOAD, item).sendToTarget();
//            } else {
//                viewHolder.image_download.setProgress(0);
//                viewHolder.image_download.setBackgroundResource(R.drawable.hb_download_btn_bg);
//                viewHolder.iv_download_ok.setVisibility(View.GONE);
//            }
//        }
//        viewHolder.download_parent.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                //ReportUtils.getInstance(mContext.getApplicationContext()).reportMessage(104);
//                if(HBMusicUtil.isNoPermission(item)){
//                    Toast.makeText(mContext, R.string.hb_download_permission, Toast.LENGTH_SHORT).show();
//                    return;
//                }
//                if (!HBMusicUtil.isNetWorkActive(mContext)) {
//                    Toast.makeText(mContext, R.string.hb_network_error, Toast.LENGTH_SHORT).show();
//                    return;
//                }
//                if (FlowTips.showDownloadFlowTips(mContext, new OndialogClickListener() {
//
//                    @Override
//                    public void OndialogClick() {
//                        addDownload(item, downloadInfo);
//                    }
//                })) {
//                    return;
//                }
//                addDownload(item, downloadInfo);
//
//            }
//        });
//        if(HBMusicUtil.isNoPermission(item)){
//            viewHolder.tv_title.setTextColor(Color.parseColor("#b3b3b3"));
//            viewHolder.tv_artist.setTextColor(Color.parseColor("#b3b3b3"));
//            if(viewHolder.iv_download_ok.getVisibility()==View.GONE){
//                viewHolder.image_download.setBackgroundResource(R.drawable.hb_download_no_btn);
//            }
//        }else {
//            viewHolder.tv_title.setTextColor(Color.parseColor("#ff000000"));
//            viewHolder.tv_artist.setTextColor(mContext.getResources().getColor(R.color.hb_item_song_size));
//        }
        return convertView;
    }


    /**
     * 检测SD指定目录是否存在需要下载到文件
     * @param item
     * @return
     */
    private boolean isExsistDownlaod(OnlineSong item) {
        try {
            String name = "";
            if (item.getArtistName().equalsIgnoreCase(item.getSingers())) {
                name = item.getSongName().replaceAll("/", "-") + "_" + item.getArtistName().replaceAll("/", "-");
            } else {
                name = item.getSongName().replaceAll("/", "-") + "_" + item.getArtistName().replaceAll("/", "-") + "_" + item.getSingers().replaceAll("/", "-");
            }
            String downloadSd = Globals.mSavePath + Globals.SYSTEM_SEPARATOR + name + ".mp3";
            return new File(downloadSd).exists();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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
//                downloadButton.setBackgroundResource(R.drawable.hb_download_started);
//                break;
//            case 3:
//                downloadButton.setBackgroundResource(R.drawable.hb_download_started_bg);
//                break;
//            case 4:
//            case 6:
//                downloadButton.setBackgroundResource(R.drawable.hb_download_error_bg);
//                break;
//            default:
//                downloadButton.setBackgroundResource(R.drawable.hb_download_btn_bg);
//                break;
//        }
//    }

    public void clearCache() {
        mDownloadManager.clearListenerMap();
        mHandler.removeCallbacksAndMessages(null);
    }

    public boolean isFileExists(DownloadInfo downloadInfo) {
        if (downloadInfo.getFileSavePath() != null) {
            File file = new File(downloadInfo.getFileSavePath());
            if (!file.exists()) {
                return false;
            }
        }
        return true;
    }

    public void addDownload(OnlineSong item, final DownloadInfo downloadInfo) {
        try {
            if (downloadInfo != null && (downloadInfo.getState().value() == 7) && isFileExists(downloadInfo)) {
                return;
            } else if (downloadInfo != null && (downloadInfo.getState().value() == 2 || downloadInfo.getState().value() == 0)) {
                mDownloadManager.pauseDownloadById(item.getSongId());
                notifyDataSetChanged();
                return;
            } else if (downloadInfo != null) {
                mDownloadManager.resumeDownloadById(item.getSongId());
                mDownloadManager.setDownloadListener(item.getSongId(), mDownloadStatusListener);
                notifyDataSetChanged();
                return;
            }

            if (item != null) {
                mDownloadManager.addNewDownload(item);
                mDownloadManager.setDownloadListener(item.getSongId(), mDownloadStatusListener);
                notifyDataSetChanged();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

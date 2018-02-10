package com.protruly.music.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.protruly.music.R;
import com.protruly.music.util.LogUtil;
import com.protruly.music.model.HBRankItem;
import com.xiami.music.model.RadioCategory;
import com.xiami.sdk.entities.OnlineAlbum;
import com.xiami.sdk.entities.OnlineCollect;

import java.util.ArrayList;
import java.util.List;

import hb.widget.HbListView;

/**
 * Created by hujianwei on 17-9-4.
 */

public class OnlineMusicMainAdapter extends BaseAdapter  {

    private static final String TAG = "OnlineMusicMainAdapter";
    private LayoutInflater mInflater;
    private Context mContext;
    private List<OnlineItem> datas = new ArrayList<OnlineItem>();
    private OnGridViewClickListener mOnGridViewClickListener;
    private SparseArray<View> storeViews = new SparseArray<View>();
    private DisplayImageOptions options;
    private boolean onGridViewClick=true;

    public OnlineMusicMainAdapter(Context context) {
        this.mInflater = LayoutInflater.from(context);
        this.mContext = context;
        initImageCacheParams();
    }

    public void setOnGridViewClickListener(OnGridViewClickListener l) {
        mOnGridViewClickListener = l;
    }

    public void setDatas(List<OnlineAlbum> albumlist, List<OnlineCollect> playlist, List<HBRankItem> ranklist, List<RadioCategory> radioList) {
        if (albumlist == null || playlist == null || ranklist == null) {
            return;
        }
        datas.clear();
        OnlineItem item = new OnlineItem(mContext.getResources().getString(R.string.hb_online_new_album), R.drawable.hb_online_new_album);
        item.setAlbumlist(albumlist);
        datas.add(item);
        item = new OnlineItem(mContext.getResources().getString(R.string.hb_recommend_playlist), R.drawable.hb_online_recommend_playlist);
        item.setPlaylist(playlist);
        datas.add(item);
        // add radio
        item = new OnlineItem(mContext.getResources().getString(R.string.xiam_radio), R.drawable.hb_online_radio);
        item.setRadioList(radioList);
        datas.add(item);
        item = new OnlineItem(mContext.getResources().getString(R.string.hb_ranking), R.drawable.hb_online_rank);
        item.setRanklist(ranklist);
        datas.add(item);
        notifyDataSetChanged();
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

    @Override
    public View getView(int arg0, View arg1, ViewGroup arg2) {
        View view = storeViews.get(arg0);
//        HoldView holdView;
//        if (view == null) {
//            holdView = new HoldView();
//            arg1 = mInflater.inflate(com.hb.R.layout.hb_slid_listview, null);
//            // 获取添加内容的对象
//            RelativeLayout mainUi = (RelativeLayout) arg1.findViewById(com.hb.R.id.hb_listview_front);
//            View myView = mInflater.inflate(R.layout.hb_online_main_item, null);
//            holdView.mGridView = (GridView) myView.findViewById(R.id.hb_mian_grid_view);
//            holdView.mListView = (HbListView) myView.findViewById(R.id.hb_mian_list_view);
//            holdView.name = (TextView) myView.findViewById(R.id.hb_icon_name);
//            holdView.icon = (ImageView) myView.findViewById(R.id.hb_icon_flag);
//            holdView.more = (TextView) myView.findViewById(R.id.hb_more_icon);
//            if (arg0 == 0) {
//                holdView.mGridView.setVerticalSpacing(mContext.getResources().getDimensionPixelSize(R.dimen.hb_gridview_spading_low));
//                holdView.mGridView.setHorizontalSpacing(mContext.getResources().getDimensionPixelSize(R.dimen.hb_gridview_h_spading));
//                holdView.adapter = new OnlineMusicGridAdapter(mContext, 0);
//                holdView.mGridView.setAdapter(holdView.adapter);
//                holdView.mListView.setVisibility(View.GONE);
//                myView.findViewById(R.id.id_music_listview).setVisibility(View.GONE);
//                myView.findViewById(R.id.id_grid_layout).setVisibility(View.VISIBLE);
//            } else if (arg0 == 1) {
//                holdView.mGridView.setVerticalSpacing(mContext.getResources().getDimensionPixelSize(R.dimen.hb_gridview_spading_large));
//                holdView.mGridView.setHorizontalSpacing(mContext.getResources().getDimensionPixelSize(R.dimen.hb_gridview_h_spading));
//                holdView.mGridView.setColumnWidth(mContext.getResources().getDimensionPixelSize(R.dimen.hb_gridview_width_large));
//                holdView.adapter = new OnlineMusicGridAdapter(mContext, 0);
//                holdView.mGridView.setAdapter(holdView.adapter);
//                holdView.mListView.setVisibility(View.GONE);
//                myView.findViewById(R.id.id_music_listview).setVisibility(View.GONE);
//                myView.findViewById(R.id.id_grid_layout).setVisibility(View.VISIBLE);
//            } else if (arg0 == 2) {
//                myView.findViewById(R.id.id_line1).setVisibility(View.VISIBLE);
//                myView.findViewById(R.id.id_line2).setVisibility(View.VISIBLE);
//                holdView.mGridView.setBackgroundResource(R.drawable.hb_radio_bg);
//                holdView.mGridView.setNumColumns(2);
//                holdView.adapter = new OnlineMusicGridAdapter(mContext, 2);
//                holdView.mGridView.setAdapter(holdView.adapter);
//                holdView.mListView.setVisibility(View.GONE);
//                myView.findViewById(R.id.id_music_listview).setVisibility(View.GONE);
//                myView.findViewById(R.id.id_grid_layout).setVisibility(View.VISIBLE);
//            } else {
//                myView.findViewById(R.id.id_music_listview).setVisibility(View.VISIBLE);
//                holdView.mListView.setVisibility(View.VISIBLE);
//                myView.findViewById(R.id.id_grid_layout).setVisibility(View.GONE);
//                holdView.adapter = new OnlineMusicGridAdapter(mContext, 1);
//                holdView.mListView.setAdapter(holdView.adapter);
//                holdView.mListView.setSelector(R.drawable.hb_playlist_item_clicked);
//            }
//            // 将要显示的内容添加到mainUi中去
//            mainUi.addView(myView, 0, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
//            // 设置间距
//            RelativeLayout rl_control_padding = (RelativeLayout) arg1.findViewById(com.hb.R.id.control_padding);
//            rl_control_padding.setPadding(0, 0, 0, 0);
//            arg1.setClickable(true);
//            arg1.setTag(holdView);
//            storeViews.put(arg0, arg1);
//            view = arg1;
//        } else {
//            holdView = (HoldView) view.getTag();
//        }
//        OnlineItem info = datas.get(arg0);
//        if (info != null) {
//            final int postion = arg0;
//            holdView.name.setText(info.name);
//            holdView.icon.setImageResource(info.resId);
//            if (arg0 == 1) {
//                holdView.adapter.setPlaylist(info.playlist, arg0);
//            } else if (arg0 == 0) {
//                holdView.adapter.setGridData(info.lists, arg0);
//            } else if (arg0 == 2) {
//                holdView.adapter.setRadiolist(info.radioCategories, arg0);
//                holdView.more.setVisibility(View.GONE);
//            } else {
//                holdView.adapter.setRanklist(info.ranklist, arg0);
//                holdView.more.setVisibility(View.GONE);
//            }
//            holdView.more.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View arg0) {
//                    if (mOnGridViewClickListener != null) {
//                        mOnGridViewClickListener.onMoreButtonClick(postion);
//                    }
//                }
//            });
//            holdView.mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                @Override
//                public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
//                    LogUtil.d(TAG, "grid item clicked!!");
//                    if (mOnGridViewClickListener != null) {
//                        mOnGridViewClickListener.onGridItemClick(postion, arg2, arg0.getAdapter().getItem(arg2));
//                    }
//                }
//            });
//            holdView.mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                @Override
//                public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
//                    LogUtil.d(TAG, "list item clicked!!");
//                    if (mOnGridViewClickListener != null) {
//                        mOnGridViewClickListener.onGridItemClick(postion, arg2, arg0.getAdapter().getItem(arg2));
//                    }
//                }
//            });
//        }
        return view;
    }

    class HoldView {
        ImageView icon;
        TextView name;
        GridView mGridView;
        HbListView mListView;
        TextView more;
        OnlineMusicGridAdapter adapter;
    }

    public class OnlineItem {
        private String name;
        private int resId;
        private List<OnlineAlbum> lists;
        private List<OnlineCollect> playlist;
        private List<HBRankItem> ranklist;
        private List<RadioCategory> radioCategories;

        public OnlineItem(String name, int id) {
            this.name = name;
            this.resId = id;
        }

        public void setAlbumlist(List<OnlineAlbum> list) {
            this.lists = list;
        }

        public void setPlaylist(List<OnlineCollect> list) {
            this.playlist = list;
        }

        public void setRanklist(List<HBRankItem> list) {
            this.ranklist = list;
        }

        public void setRadioList(List<RadioCategory> list) {
            this.radioCategories = list;
        }
    }

    class OnlineMusicGridAdapter extends BaseAdapter {
        private Context gridContext;
        private List<OnlineAlbum> items = new ArrayList<OnlineAlbum>();
        private List<OnlineCollect> playlists = new ArrayList<OnlineCollect>();
        private List<HBRankItem> ranklists = new ArrayList<HBRankItem>();
        private List<RadioCategory> radiolists = new ArrayList<RadioCategory>();
        private int type = 0;// 是否显示artist
        private int isList = 0;
        private Bitmap rankBitmap, albumBitmap, playlistBitmap;

        public OnlineMusicGridAdapter(Context context, List<OnlineAlbum> list) {
            gridContext = context;
            if (list != null) {
                this.items = list;
            }
        }

        public OnlineMusicGridAdapter(Context context, int is) {
            gridContext = context;
            this.isList = is;
            rankBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.default_music_icon2);
            albumBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.hb_online_music_defualt_item_bg);
            playlistBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.hb_online_recommend_default);
        }

        public void setGridData(List<OnlineAlbum> list, int is) {
            if (list != null) {
                this.items = list;
            }
            this.type = is;
            // notifyDataSetChanged();
        }

        public void setRadiolist(List<RadioCategory> list, int is) {
            if (list != null) {
                this.radiolists = list;
            }
            this.type = is;
        }

        public void setPlaylist(List<OnlineCollect> list, int is) {
            if (list != null) {
                this.playlists = list;
            }
            this.type = is;
            // notifyDataSetChanged();
        }

        public void setRanklist(List<HBRankItem> list, int is) {
            if (list != null) {
                this.ranklists = list;
            }
            this.type = is;
            // notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            int count = 0;
            switch (type) {
                case 0:
                    count = items.size();
                    break;
                case 1:
                    count = playlists.size();
                    break;
                case 2:
                    count = radiolists.size();
                    break;
                case 3:
                    count = ranklists.size();
                    break;
            }
            return count;
        }

        @Override
        public Object getItem(int arg0) {
            Object item = null;
            switch (type) {
                case 0:
                    item = items.get(arg0);
                    break;
                case 1:
                    item = playlists.get(arg0);
                    break;
                case 2:
                    item = radiolists.get(arg0);
                    break;
                case 3:
                    item = ranklists.get(arg0);
                    break;
            }
            return item;
        }

        @Override
        public long getItemId(int arg0) {
            return arg0;
        }

        @Override
        public View getView(int arg0, View arg1, ViewGroup arg2) {
            HoldView1 holdView;
            // LogUtil.d(TAG, "getView..............................arg0:" + arg0);
            if (arg1 == null) {
                holdView = new HoldView1();
                if (isList == 1) {
                    arg1 = LayoutInflater.from(gridContext).inflate(R.layout.hb_online_ranking_item, null);
                } else if (isList == 2) {
                    arg1 = LayoutInflater.from(gridContext).inflate(R.layout.hb_online_radio_item, null);
                    holdView.artistname = (TextView) arg1.findViewById(R.id.hb_artist_name);
                } else {
                    arg1 = LayoutInflater.from(gridContext).inflate(R.layout.hb_online_music_grid_item, null);
                    holdView.artistname = (TextView) arg1.findViewById(R.id.hb_artist_name);
                    holdView.bgmask = arg1.findViewById(R.id.hb_item_mask);
                }
                holdView.icon = (ImageView) arg1.findViewById(R.id.hb_icon);
                holdView.albumname = (TextView) arg1.findViewById(R.id.hb_album_name);
                arg1.setTag(holdView);
            } else {
                holdView = (HoldView1) arg1.getTag();
            }
            Object info = getItem(arg0);
            if (info != null) {
                ImageView icon = holdView.icon;
                boolean needCrop = true;
                boolean needScale = true;
                int mIconWidth = 0, mIconHeight = 0;
                String url = null;
                int defualtIcon = 0;
                if (type == 0) {
                    OnlineAlbum item = (OnlineAlbum) info;
                    holdView.albumname.setText(item.getAlbumName());
                    holdView.icon.setBackgroundResource(R.drawable.hb_online_music_defualt_item_bg);
                    holdView.artistname.setText(item.getArtistName());
                    holdView.artistname.setVisibility(View.VISIBLE);
                    holdView.bgmask.setBackgroundResource(R.drawable.hb_new_album_mask);
                    mIconWidth = albumBitmap.getWidth();
                    mIconHeight = albumBitmap.getHeight();
                    url = item.getImageUrl(330);
                    defualtIcon = R.drawable.hb_online_music_defualt_item_bg;
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) holdView.icon.getLayoutParams();
                    params.height = mIconHeight;
                    holdView.icon.setLayoutParams(params);
                } else if (type == 1) {
                    OnlineCollect item = (OnlineCollect) info;
                    holdView.albumname.setText(item.getCollectName());
                    holdView.icon.setBackgroundResource(R.drawable.hb_online_recommend_default);
                    holdView.artistname.setVisibility(View.GONE);
                    mIconWidth = playlistBitmap.getWidth();
                    mIconHeight = playlistBitmap.getHeight();
                    url = item.getImageUrl(400);
                    holdView.bgmask.setBackgroundResource(R.drawable.hb_recommend_mask);
                    defualtIcon = R.drawable.hb_online_recommend_default;
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) holdView.icon.getLayoutParams();
                    params.height = mIconHeight;
                    holdView.icon.setLayoutParams(params);
                } else if (type == 2) {
                    RadioCategory item = (RadioCategory) info;
                    holdView.albumname.setText(item.getTypeName() + mContext.getString(R.string.radio));
                    // holdView.icon.setBackgroundResource(R.drawable.hb_online_recommend_default);
                    // holdView.artistname.setVisibility(View.GONE);
                    if (arg0 == 0) {
                        arg1.setBackgroundResource(R.drawable.hb_corner_left_top);
                        holdView.icon.setImageResource(R.drawable.hb_radio_constellation);
                        holdView.artistname.setText(R.string.xing_zuo_info);
                    } else if (arg0 == 1) {
                        arg1.setBackgroundResource(R.drawable.hb_corner_top_right);
                        holdView.icon.setImageResource(R.drawable.hb_radio_age);
                        holdView.artistname.setText(R.string.nian_dai_info);
                    } else if (arg0 == 2) {
                        arg1.setBackgroundResource(R.drawable.hb_corner_bottom_left);
                        holdView.icon.setImageResource(R.drawable.hb_radio_style);
                        holdView.artistname.setText(R.string.style_info);
                    } else {
                        arg1.setBackgroundResource(R.drawable.hb_corner_bottom_right);
                        holdView.icon.setImageResource(R.drawable.hb_radio_mood);
                        holdView.artistname.setText(R.string.mood_info);
                    }
                } else {
                    HBRankItem item = (HBRankItem) info;
                    holdView.albumname.setText(item.getRankname());
                    holdView.icon.setImageResource(item.getImgUri());
                    mIconWidth = rankBitmap.getWidth();
                    mIconHeight = rankBitmap.getHeight();
                    // url=item.mPicBig;
                }
                if (url != null) {
                    // mImageFetcher.loadImageAndSize(url, icon, 100, mIconWidth, mIconHeight);
                    ImageLoader.getInstance().displayImage(url, icon, options);
                }
            }
            return arg1;
        }

        class HoldView1 {
            ImageView icon;
            TextView albumname;
            TextView artistname;
            View bgmask;
        }
    }

    public class GridViewItem {
        private String rankname;
        private String imgUri;

        public GridViewItem(String album, String imguri) {
            this.rankname = album;
            this.imgUri = imguri;
        }
    }

    public interface OnGridViewClickListener {
        public static final int NEW_ALBUM = 0;
        public static final int RECOMMEND_PLAYLIST = 1;
        public static final int RANKING = 2;
        public static final int BANNER = 3;

        // public static final int RADIO =2;
        public void onMoreButtonClick(int type);

        public void onGridItemClick(int type, int postion, Object obj);
    }

    public void clearViews() {
        LogUtil.d(TAG, "storeViews:" + storeViews.size());
        for (int i = 0; i < storeViews.size(); i++) {
            HoldView holdView = (HoldView) storeViews.get(i).getTag();
            holdView.mGridView.setAdapter(null);
            holdView.mListView.setAdapter(null);
            holdView.adapter = null;
        }
        storeViews.clear();
        storeViews = null;
    }

    private void initImageCacheParams() {
        options = new DisplayImageOptions.Builder().showImageOnLoading(R.drawable.hb_online_music_defualt_item_bg)
                .showImageForEmptyUri(R.drawable.hb_online_music_defualt_item_bg).showImageOnFail(R.drawable.hb_online_music_defualt_item_bg)
                .cacheInMemory(true).cacheOnDisk(true).considerExifParams(true).displayer(new SimpleBitmapDisplayer()).build();
    }
}

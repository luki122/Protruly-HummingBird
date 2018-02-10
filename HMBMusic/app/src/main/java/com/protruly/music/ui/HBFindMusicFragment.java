package com.protruly.music.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HbSearchView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.protruly.music.Application;
import com.protruly.music.MusicUtils;
import com.protruly.music.R;
import com.protruly.music.adapter.HBSearchAdapter;
import com.protruly.music.adapter.HBSearchAlbumAdapter;
import com.protruly.music.adapter.HBSearchArtistAdapter;
import com.protruly.music.adapter.HBSearchHistoryAdapter;
import com.protruly.music.adapter.HBSearchPagerAdapter;
import com.protruly.music.adapter.HBSearchSongAdapter;
import com.protruly.music.adapter.ImagePagerAdapter;
import com.protruly.music.adapter.OnlineMusicMainAdapter;
import com.protruly.music.model.HBRankItem;
import com.protruly.music.model.SearchItem;
import com.protruly.music.model.XiaMiSdkUtils;
import com.protruly.music.online.HBNetTrackDetail;
import com.protruly.music.online.HBNetTrackDetailActivity;
import com.protruly.music.util.HBListItem;
import com.protruly.music.util.HBMusicUtil;
import com.protruly.music.util.ThreadPoolExecutorUtils;
import com.protruly.music.widget.BannerViewPager;
import com.protruly.music.widget.HBViewPager;
import com.protruly.music.util.LogUtil;
import com.protruly.music.adapter.ImagePagerAdapter.OnBannerClickListener;
import com.protruly.music.adapter.OnlineMusicMainAdapter.OnGridViewClickListener;
import com.xiami.music.model.RadioCategory;
import com.xiami.sdk.entities.Banner;
import com.xiami.sdk.entities.OnlineAlbum;
import com.xiami.sdk.entities.OnlineArtist;
import com.xiami.sdk.entities.OnlineCollect;
import com.xiami.sdk.entities.OnlineSong;
import com.xiami.sdk.entities.QueryInfo;
import com.xiami.sdk.entities.RankType;
import com.xiami.sdk.entities.SearchSummaryResult;
import com.xiami.sdk.utils.ImageUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import hb.app.HbActivity;
import hb.widget.HbListView;
import android.widget.HbSearchView.OnQueryTextListener;

/**
 * Created by hujianwei on 17-9-4.
 */

public class HBFindMusicFragment  implements View.OnClickListener {
    private static final String TAG = "HBFindMusicFragment";
    private ListView mainListView;
    private HbListView searchListView;
    private HbActivity mContext;
    //private HbDotView mHbDotView;
    private View headView;
    private float yDown;
    private boolean isShowSearch = false;
    private int searchDefaultPadding = -129;
    private BannerViewPager bannerPage;
    private boolean isShowView = false;
    private OnlineMusicMainAdapter mOnlineMusicMainAdapter;
    private int countSize = 0;
    private boolean isFirstClick = true;
    private ProgressBar mProgressBar;
    private LinearLayout.LayoutParams mLayoutParams;
    private HbSearchView searchView;
    private View noNetWork;
    private ImageView networkIcon;
    private TextView networkText;
    private Button networkButton;
    private int networkType = 0;
    private int mPageNo = 1;
    private static final int PAGE_SIZE = 15;
    private ArrayList<SearchItem> seachArrayList = new ArrayList<SearchItem>();
    private HBSearchAdapter searchAdapter = null;
    private Button btn_search;
    private String keyWord;
    private HBViewPager mHBViewPager;
    private Drawable oldDrawable;
    private LinearLayout.LayoutParams oldParams;

    // 动画是否在运行
    private boolean isPlaying = false;

    // 播放按钮动画
    private Animation operatingAnim;
//    private HbClickListener clickListener;
//    private HbSearchClick searchClick;
    private HBSearchHistoryAdapter mHBSearchHistoryAdapter;
    private FrameLayout rootLayoutView;

    public FrameLayout getSearchviewLayout() {
        return rootLayoutView;
    }

    private HbListView historyList;
    private List<String> historyKeywords = new ArrayList<String>();
    private View searchhistory;
    private boolean isClickHistory = false;
    private boolean isEntrySearch = false;
    private boolean isExitSearch = false;

    private ViewPager mViewPager;
    private HBSearchPagerAdapter mSearchPagerAdapter;
    private int mArtistPageNo = 1;
    private int mAlbumPageNo = 1;
    private ListView mSongListView, mAlbumListView, mArtistListView;
    private ArrayList<OnlineSong> mSongList;
    private ArrayList<OnlineAlbum> mAlbumList;
    private ArrayList<OnlineArtist> mArtistList;
    private List mTempList, mSongTempList, mArtistTempList, mAlbumTemptList;
    private HBSearchSongAdapter mHBSearchSongAdapter;
    private HBSearchArtistAdapter mHBSearchArtistAdapter;
    private HBSearchAlbumAdapter mHBSearchAlbumAdapter;
    private int mSongCount, mAlbumCount, mArtistCount;
    private View loadingView;
    private ImagePagerAdapter mPagerAdapter;
    private RadioGroup mRadioGroup;
    private final int MSG_SEARCH_FINISHED = 1;
    private final int MSG_LOAD_MORE_SONG = 2;
    private final int MSG_LOAD_MORE_ALBUM = 3;
    private final int MSG_LOAD_MORE_ARTIST = 4;
    private long mTimeStamp;
    private View mSearchResultView;
    private TextView mTipText;
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            if (loadingView != null) {
                loadingView.setVisibility(View.GONE);
            }
            switch (msg.what) {
                case MSG_SEARCH_FINISHED:
                    refreshUI();
                    break;
                case MSG_LOAD_MORE_SONG:
                    if (mHBSearchSongAdapter != null) {
                        mSongList.addAll(mTempList);
                        mHBSearchSongAdapter.notifyDataSetChanged();
                    }
                    break;
                case MSG_LOAD_MORE_ALBUM:
                    if (mHBSearchAlbumAdapter != null) {
                        mAlbumList.addAll(mTempList);
                        mHBSearchAlbumAdapter.notifyDataSetChanged();
                    }
                    break;
                case MSG_LOAD_MORE_ARTIST:
                    if (mHBSearchArtistAdapter != null) {
                        mArtistList.addAll(mTempList);
                        mHBSearchArtistAdapter.notifyDataSetChanged();
                    }
                    break;
                default:
                    break;
            }
            LogUtil.d(TAG, "mSongList:" + mSongList.size() + " mAlbumList:" + mAlbumList.size() + " mArtistList:" + mArtistList.size());
        };
    };

    @SuppressWarnings("deprecation")
    public void initview(View view, HbActivity context, HBViewPager pager) {
        this.mContext = context;
        mHBViewPager = pager;
        mainListView = (ListView) view.findViewById(R.id.hb_online_music_main);
        mProgressBar = (ProgressBar) view.findViewById(R.id.hb_progress);
        noNetWork = view.findViewById(R.id.hb_no_network);
        networkIcon = (ImageView) view.findViewById(R.id.id_no_network_icon);
        networkText = (TextView) view.findViewById(R.id.id_no_network_text);
        networkButton = (Button) view.findViewById(R.id.id_no_network_button);
        networkButton.setOnClickListener(this);
        headView = LayoutInflater.from(context).inflate(R.layout.hb_online_music_head, null);
        searchDefaultPadding = context.getResources().getDimensionPixelSize(R.dimen.hb_online_search_height);
        //mContext.addSearchviewInwindowLayout();
        initeHead(headView, view);
        mainListView.addHeaderView(headView);
        mOnlineMusicMainAdapter = new OnlineMusicMainAdapter(context);
        //mOnlineMusicMainAdapter.setOnGridViewClickListener(mOnGridViewClickListener);
        mainListView.setAdapter(mOnlineMusicMainAdapter);
        mainListView.setSelector(android.R.color.transparent);
        // mainListView.setOnTouchListener(mOntouchListener);
        mainListView.setOnScrollListener(mOnScrollListener);
        mainListView.setVisibility(View.GONE);
		/*mContext.getSearchView().setOnQueryTextFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				// TODO Auto-generated method stub

				if (!hasFocus) {
					LinearLayout.LayoutParams layoutParams = new LayoutParams((int) mContext.getResources().getDimension(R.dimen.hb_play_width), (int) mContext.getResources().getDimension(
							R.dimen.hb_play_width));
					layoutParams.setMargins(0, 0, (int) mContext.getResources().getDimension(R.dimen.hb_album_info_padding), 0);
					changeButton(0);
				} else {
					changeButton(1);
				}
			}
		});*/
//        mContext.setOnSearchViewQuitListener(new OnSearchViewQuitListener() {
//
//            @Override
//            public boolean quit() {
//                // TODO Auto-generated method stub
//                mainListView.setVisibility(View.VISIBLE);
//                mProgressBar.setVisibility(View.GONE);
//                mContext.setMenuEnable(true);
//                mHBViewPager.setViewPageOnScrolled(false);
//                isPlaying = false;
//                isEntrySearch = false;
//                mViewPager.setCurrentItem(0);
//                LogUtil.d(TAG, "-setOnSearchViewQuitListener-quit");
//                HBMusicUtil.hideInputMethod(mContext, mainListView);
//                return false;
//            }
//        });
        btn_search.setText(mContext.getResources().getString(R.string.songlist_cancel));
        oldDrawable = btn_search.getBackground();
        oldParams = (LinearLayout.LayoutParams) btn_search.getLayoutParams();

        //searchClick = new HbSearchClick();
        //mContext.setOnSearchViewButtonListener(searchClick);
        operatingAnim = AnimationUtils.loadAnimation(mContext, R.anim.rotate_anim);
        LinearInterpolator lin = new LinearInterpolator();
        operatingAnim.setInterpolator(lin);
        //clickListener = new HbClickListener();

        mViewPager = (ViewPager) mSearchResultView.findViewById(R.id.hb_id_viewpager);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int arg0) {
                // TODO Auto-generated method stub
                switch (arg0) {
                    case 0:
                        ((RadioButton) mRadioGroup.findViewById(R.id.hb_rb_song)).setChecked(true);
                        if (mSongList != null && mSongList.size() == 0) {
                            mTipText.setVisibility(View.VISIBLE);
                        } else {
                            mTipText.setVisibility(View.GONE);
                        }
                        break;
                    case 1:
                        ((RadioButton) mRadioGroup.findViewById(R.id.hb_rb_artist)).setChecked(true);
                        if (mArtistList != null && mArtistList.size() == 0) {
                            mTipText.setVisibility(View.VISIBLE);
                        } else {
                            mTipText.setVisibility(View.GONE);
                        }
                        break;
                    case 2:
                        ((RadioButton) mRadioGroup.findViewById(R.id.hb_rb_album)).setChecked(true);
                        if (mAlbumList != null && mAlbumList.size() == 0) {
                            mTipText.setVisibility(View.VISIBLE);
                        } else {
                            mTipText.setVisibility(View.GONE);
                        }
                        break;
                    default:

                        break;
                }
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
                // TODO Auto-generated method stub

            }
        });
        loadingView = mSearchResultView.findViewById(R.id.hb_loading_parent);
        mRadioGroup = (RadioGroup) mSearchResultView.findViewById(R.id.hb_rb_category);
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup radiogroup, int i) {
                // TODO Auto-generated method stub
                switch (i) {
                    case R.id.hb_rb_song:
                        mViewPager.setCurrentItem(0);
                        break;
                    case R.id.hb_rb_artist:
                        mViewPager.setCurrentItem(1);
                        break;
                    case R.id.hb_rb_album:
                        mViewPager.setCurrentItem(2);
                        break;
                    default:
                        break;
                }
            }
        });
        // -add by tangjie 2014/12/26 end//
    }

    private void initeHead(View headView, View rootView) {
        bannerPage = (BannerViewPager) headView.findViewById(R.id.hb_id_banner);
        //mHbDotView = (HbDotView) headView.findViewById(R.id.hb_dot_layout);
        searchView = (HbSearchView) headView.findViewById(R.id.hb_search);
        rootLayoutView = (FrameLayout) rootView.findViewById(R.id.online_music_main);
        
        mHBSearchHistoryAdapter = new HBSearchHistoryAdapter(mContext, true);
        if (rootLayoutView != null) {
            searchhistory = LayoutInflater.from(mContext).inflate(R.layout.hb_searchview_history, null);
            historyList = (HbListView) searchhistory.findViewById(R.id.hb_search_history);
            View bottomview = LayoutInflater.from(mContext).inflate(R.layout.hb_search_history_foot, null);
            bottomview.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    historyKeywords.clear();
                    MusicUtils.mSongDb.clearSearchHistory();
                    mHBSearchHistoryAdapter.notifyDataSetChanged();
                    searchhistory.setVisibility(View.GONE);
                }
            });
            historyList.addFooterView(bottomview);
            historyList.setAdapter(mHBSearchHistoryAdapter);
            historyList.setOnItemClickListener(mOnItemClickListener);
            historyList.setOnTouchListener(mOnHistoryTouchListner);
            mSearchResultView = LayoutInflater.from(mContext).inflate(R.layout.hb_netsearch_layout, null);
            mTipText = (TextView) mSearchResultView.findViewById(R.id.hb_no_tips);
        }
        searchView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                LogUtil.d(TAG, "searchView");
                //mContext.setMenuEnable(false);
                if (historyKeywords.size() == 0) {
                    searchhistory.setVisibility(View.GONE);
                }
                showSearchState(true);
                //mContext.showSearchviewLayout();
                rootLayoutView.addView(mSearchResultView);
                rootLayoutView.addView(searchhistory);
                mHBViewPager.setViewPageOnScrolled(true);
                isEntrySearch = true;
                isExitSearch = false;
                searchView.setVisibility(View.GONE);
                //ReportUtils.getInstance(mContext.getApplicationContext()).reportMessage(ReportUtils.TAG_OL_SEARCH);
                HBMusicUtil.showInputMethod(mContext);
            }
        });
        //mContext.getHbActionBar().getHbActionBarSearchView().setOnQueryTextListener(new SongSearchViewQueryTextChangeListener());
//        mContext.getHbActionBar().getHbActionBarSearchViewBackButton().setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                mContext.hideSearchviewLayout();
//            }
//        });
        // mContext.setOnQueryTextListener(new
        // SongSearchViewQueryTextChangeListener());
    }

    private void changeNetworkUi(int type) {
        networkType = type;
        if (type == 0) {
            networkIcon.setImageResource(R.drawable.hb_no_network);
            networkText.setText(R.string.hb_no_network);
            networkButton.setText(R.string.hb_setting_network);
        } else {
            networkIcon.setImageResource(R.drawable.hb_network_error);
            networkText.setText(R.string.hb_network_error);
            networkButton.setText(R.string.hb_retry);
        }
    }

    private AbsListView.OnScrollListener mOnScrollListener = new AbsListView.OnScrollListener() {

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

        }

        @Override
        public void onScrollStateChanged(AbsListView arg0, int arg1) {

            LogUtil.d(TAG, "scrollstate:" + arg1);
            if (arg1 == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                bannerPage.stopAutoScroll();
            } else if (arg1 == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                bannerPage.startAutoScroll();
            }
        }

    };

    public void isLoadData() {

        if (mainListView.getVisibility() == View.GONE && isFirstClick) {
            LogUtil.d(TAG, "isLoadData.");
            if (HBMusicUtil.isNetWorkActive(mContext)) {
                isFirstClick = false;
                noNetWork.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.VISIBLE);
                mainListView.setVisibility(View.GONE);
                loadXiamiOnlineMusic();// add for xiamimusic
            } else {
                isFirstClick = true;
                changeNetworkUi(0);
                noNetWork.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);
                mainListView.setVisibility(View.GONE);
            }

        }
    }

    private OnBannerClickListener mOnBannerClickListener = new OnBannerClickListener() {

        @Override
        public void onBannerClick(Banner item) {
            if(isEntrySearch&&(searchhistory.getVisibility()==View.VISIBLE)){
                return;
            }
            String source = item.getSourceId();
            LogUtil.d(TAG, "source:" + source);
            String[] split = source.split(":");
            if (split.length < 2) {
                return;
            }
            if (!HBMusicUtil.isNetWorkActive(mContext)) {
                Toast.makeText(mContext, R.string.hb_network_error, Toast.LENGTH_SHORT).show();
                return;
            }
            LogUtil.d(TAG, "id:" + split[1] + " type:" + split[0]);
            // banner 点击响应
            Intent intent = new Intent(mContext, HBNetTrackDetailActivity.class);
            //intent.putExtra("tag", OnGridViewClickListener.BANNER);
            intent.putExtra("type", split[0]);
            //intent.putExtra(HBNetTrackDetail.ID, split[1]);
            // intent.putExtra("title", item.mDescription);
            intent.putExtra("imageUrl", ImageUtil.transferImgUrl(item.getImageUrl(), 330));
            mContext.startActivity(intent);
        }

    };

    private ViewPager.OnPageChangeListener mPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageScrollStateChanged(int arg0) {

        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        @Override
        public void onPageSelected(int arg0) {
            //mHbDotView.setSelectDot(arg0 % countSize);
        }

    };

    private OnGridViewClickListener mOnGridViewClickListener = new OnGridViewClickListener() {

        // 点击更多处理
        @Override
        public void onMoreButtonClick(int type) {
            if (!HBMusicUtil.isNetWorkActive(mContext)) {
                Toast.makeText(mContext, R.string.hb_network_error, Toast.LENGTH_SHORT).show();
                return;
            }
            switch (type) {
                case OnGridViewClickListener.NEW_ALBUM:
//                    Intent mIntent = new Intent(mContext, HBNetTrackActivity.class);
//                    mIntent.putExtra("title", mContext.getString(R.string.hb_online_new_album));
//                    mIntent.putExtra("type", 1);
//                    mContext.startActivity(mIntent);
                    LogUtil.d(TAG, "NEW_ALBUM");
                    break;
                case OnGridViewClickListener.RECOMMEND_PLAYLIST:
//                    mIntent = new Intent(mContext, HBNetTrackActivity.class);
//                    mContext.startActivity(mIntent);
                    LogUtil.d(TAG, "RECOMMEND_PLAYLIST");
                    break;
                case OnGridViewClickListener.RANKING:
//                    LogUtil.d(TAG, "RANKING");
//                    Intent intent = new Intent(mContext, HBRankList.class);
//                    mContext.startActivity(intent);
                    break;
            }
        }

        // 点击每个item响应
        @Override
        public void onGridItemClick(int type, int postion, Object obj) {

            if(isEntrySearch&&(searchhistory.getVisibility()==View.VISIBLE)){
                return;
            }
            if (!HBMusicUtil.isNetWorkActive(mContext)) {
                Toast.makeText(mContext, R.string.hb_network_error, Toast.LENGTH_SHORT).show();
                return;
            }
            switch (type) {
                case OnGridViewClickListener.NEW_ALBUM:
                    OnlineAlbum info = (OnlineAlbum) obj;
                    LogUtil.d(TAG, "NEW_ALBUM:" + postion + info.toString());
                    // 新碟上架处理
                    Intent intent = new Intent(mContext, HBNetTrackDetailActivity.class);
                    intent.putExtra("tag", OnGridViewClickListener.NEW_ALBUM);
                    intent.putExtra("title", info.getAlbumName());
                    intent.putExtra(HBNetTrackDetail.ID, String.valueOf(info.getAlbumId()));
                    intent.putExtra("imageUrl", ImageUtil.transferImgUrl(info.getImageUrl(), 330));
                    intent.putExtra("artist", info.getArtistName());
                    mContext.startActivity(intent);
                    break;
                case OnGridViewClickListener.RECOMMEND_PLAYLIST:
                    OnlineCollect playlist = (OnlineCollect) obj;
                    LogUtil.d(TAG, "RECOMMEND_PLAYLIST:" + postion + playlist.toString());
                    // 推荐歌单处理
                    Intent intent2 = new Intent(mContext, HBNetTrackDetailActivity.class);
                    intent2.putExtra("tag", OnGridViewClickListener.RECOMMEND_PLAYLIST);
                    intent2.putExtra(HBNetTrackDetail.ID, String.valueOf(playlist.getListId()));
                    intent2.putExtra("imageUrl", ImageUtil.transferImgUrl(playlist.getImageUrl(), 330));
                    intent2.putExtra("playlist_tag", playlist.getDescription());
                    mContext.startActivity(intent2);
                    // 推荐歌单处理

                    break;
                case 3:
                    HBRankItem rankitem = (HBRankItem) obj;
                    LogUtil.d(TAG, "RANKING:" + postion);
                    // 排行榜处理
                    Intent intent3 = new Intent(mContext, HBNetTrackDetailActivity.class);
                    intent3.putExtra("tag", OnGridViewClickListener.RANKING);
                    intent3.putExtra(HBNetTrackDetail.ID, String.valueOf(rankitem.getRanktype().ordinal()));
                    intent3.putExtra("title", rankitem.getRankname());
                    mContext.startActivity(intent3);
                    break;
                case 2:
//                    RadioCategory radio = (RadioCategory) obj;
//                    Intent intent4 = new Intent(mContext, HBRadioListActivity.class);
//                    intent4.putExtra("type", postion);
//                    mContext.startActivity(intent4);
                    break;
            }

        }

    };

    public void onResume() {
        if (isShowView)
            bannerPage.startAutoScroll();
        //historyList.hbOnResume();
        setPlayAnimation();
        if (!isEntrySearch) {
            //mContext.getSearchView().getQueryTextView().clearFocus();
        }

    }

    public void onPause() {
        if (isShowView)
            bannerPage.stopAutoScroll();
        //historyList.hbOnPause();
    }

    @Override
    public void onClick(View arg0) {
        switch (arg0.getId()) {
            case R.id.id_no_network_button:
                try {
                    if (networkType == 0) {
                        Intent intent = new Intent();
                        intent.setClassName("com.android.settings", "com.android.settings.Settings");
                        mContext.startActivity(intent);
                    } else {
                        isLoadData();
                    }
                } catch (Exception e) {
                    LogUtil.d(TAG, "start error!!");
                }
                break;
            default:
                break;
        }
    }

    class SongSearchViewQueryTextChangeListener implements OnQueryTextListener {
        @Override
        public boolean onQueryTextSubmit(String query) {
            if (!TextUtils.isEmpty(query)) {
                InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mainListView.getWindowToken(), 0);
                showSearchView(query, mTimeStamp);
            }
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            keyWord = newText;
            mTimeStamp = System.currentTimeMillis();
            LogUtil.d(TAG, "newText:" + newText);
            if (TextUtils.isEmpty(newText.trim())) {
                InputMethodManager manager = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                manager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
//                if (mContext.getSearchView() != null && mContext.getSearchView().getQueryTextView() != null)
//                    mContext.getSearchView().getQueryTextView().requestFocus();
                if (!isExitSearch) {
                    showSearchState(true);
                } else {
                    isExitSearch = false;
                }
            } else {
                showSearchView(newText, mTimeStamp);
                btn_search.setText(mContext.getResources().getString(R.string.songlist_cancel));
            }
            return false;
        }
    }

    public void hideSearch() {
        isEntrySearch = false;
//        mContext.setMenuEnable(true);
//        mContext.hideSearchviewLayout();
        mProgressBar.setVisibility(View.GONE);
        mHBViewPager.setViewPageOnScrolled(false);
    }

/*	public void changeButton(int type) {
		if(true){
			return;
		}
		if (type == 0) {
			LinearLayout.LayoutParams layoutParams = new LayoutParams((int) mContext.getResources().getDimension(R.dimen.hb_play_width), (int) mContext.getResources().getDimension(
					R.dimen.hb_play_width));
			layoutParams.setMargins(0, 0, (int) mContext.getResources().getDimension(R.dimen.hb_album_info_padding), 0);
			btn_search.setLayoutParams(layoutParams);
			btn_search.setBackgroundResource(R.drawable.song_playing);
			btn_search.setText("");
			setPlayAnimation();
			mContext.setOnSearchViewButtonListener(clickListener);
		} else {
			btn_search.setLayoutParams(oldParams);
			btn_search.setBackground(oldDrawable);
			btn_search.setText(mContext.getResources().getString(R.string.songlist_cancel));
			btn_search.clearAnimation();
			mContext.setOnSearchViewButtonListener(searchClick);
		}
	}*/

//    class HbClickListener implements OnSearchViewButtonClickListener {
//
//        @Override
//        public boolean onSearchViewButtonClick() {
//            // TODO Auto-generated method stub
//            Intent intent = new Intent(mContext, HBPlayerActivity.class);
//            mContext.startActivity(intent);
//            return false;
//        }
//
//    }

//    class HbSearchClick implements OnSearchViewButtonClickListener {
//
//        @Override
//        public boolean onSearchViewButtonClick() {
//            hideSearch();
//            return false;
//        }
//
//    }

    /**
     * 设置播放动画
     */
    public void setPlayAnimation() {
        if (!TextUtils.isEmpty(btn_search.getText())) {
            btn_search.clearAnimation();
            //mContext.setOnSearchViewButtonListener(searchClick);
            return;
        }
        try {
            if (MusicUtils.sService != null) {
                if (MusicUtils.sService.isPlaying()) {
                    btn_search.startAnimation(operatingAnim);
                    LogUtil.d(TAG, "btn_search.startAnimation(operatingAnim)");
                    if (!isPlaying) {
                        isPlaying = true;
                    }
                } else {
                    btn_search.clearAnimation();
                    LogUtil.d(TAG, "btn_search.clearAnimation();");
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            if (isPlaying) {
                btn_search.clearAnimation();
                isPlaying = false;
            }
        }
    }

    public void destroy() {
        if (searchAdapter != null) {
            searchAdapter.clearCache();
        }
        if (mOnlineMusicMainAdapter != null) {
            mOnlineMusicMainAdapter.clearViews();
        }
        if (mainListView != null) {
            mainListView.setAdapter(null);
            mOnlineMusicMainAdapter = null;
        }
        if (bannerPage != null) {
            bannerPage.setAdapter(null);
            mPagerAdapter = null;
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        if (mHBSearchSongAdapter != null) {
            mHBSearchSongAdapter.clearCache();
        }

    }


    private void showSearchState(boolean is) {
        historyList.setVisibility(is ? View.VISIBLE : View.GONE);
        mSearchResultView.setVisibility(is ? View.GONE : View.VISIBLE);
        if (historyKeywords.size() == 0 && is) {
            searchhistory.setVisibility(View.GONE);
        } else {
            searchhistory.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 搜索联想和搜索历史点击响应
     */
    private AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
            String keyText = "";
            if (arg0.getId() == R.id.hb_search_history) {
                keyText = (String) arg0.getAdapter().getItem(historyKeywords.size() - arg2 - 1);
            } else {
                keyText = (String) arg0.getAdapter().getItem(arg2);
            }

            LogUtil.d(TAG, "keyText:" + keyText + " arg1:" + arg0);
            isClickHistory = true;
            //mContext.getSearchView().setQuery(keyText, false);
            showSearchView(keyText, mTimeStamp);
        }
    };

    private View.OnTouchListener mOnHistoryTouchListner = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View arg0, MotionEvent arg1) {

            switch (arg1.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    HBMusicUtil.hideInputMethod(mContext, arg0);
                    break;
            }
            return false;
        }
    };

    public void hideSearchviewLayout() {
        rootLayoutView.removeView(mSearchResultView);
        rootLayoutView.removeView(searchhistory);
        searchView.setVisibility(View.VISIBLE);
        rootLayoutView.invalidate();
        mainListView.requestFocus();
        isExitSearch = true;
    }

    private List<OnlineAlbum> mOnlineAlbums = null;
    private List<OnlineCollect> mOnlineCollects = null;
    private List<Banner> mBanners = null;
    private List<HBRankItem> ranklist;
    private List<RadioCategory> mRadioCategories = null;

    private ThreadPoolExecutor executor = ThreadPoolExecutorUtils.getThreadPoolExecutor().getExecutor();

    private void loadXiamiOnlineMusic() {
        mOnlineAlbums = null;
        mOnlineCollects = null;
        mBanners = null;
        mRadioCategories = null;
        // get hot week album
        executor.submit(new Runnable() {

            @Override
            public void run() {
                Pair<QueryInfo, List<OnlineAlbum>> pair = XiaMiSdkUtils.getWeekHotAlbumsSync(mContext, 6, 1);
                if (pair != null && pair.second != null) {
                    mOnlineAlbums = pair.second;
                    if (mOnlineCollects != null && mOnlineAlbums != null && mBanners != null && mRadioCategories != null) {
                        LogUtil.d(TAG, "-getWeekHotAlbumsSync  Success");
                        showRequestSuccess();
                    }
                } else {
                    LogUtil.d(TAG, "-getWeekHotAlbumsSync  Fail");
                    showRequestFail();
                }
            }
        });

        // get Collects

        executor.submit(new Runnable() {

            @Override
            public void run() {
                Pair<QueryInfo, List<OnlineCollect>> pair = XiaMiSdkUtils.getCollectsRecommendSync(mContext, 4, 1);
                if (pair != null && pair.second != null) {
                    mOnlineCollects = pair.second;
                    if (mOnlineCollects != null && mOnlineAlbums != null && mBanners != null && mRadioCategories != null) {
                        LogUtil.d(TAG, "-getCollectsRecommendSync Success");
                        showRequestSuccess();
                    }
                } else {
                    LogUtil.d(TAG, "-getCollectsRecommendSync Fail");
                    showRequestFail();
                }
            }
        });

        // get banner

        executor.submit(new Runnable() {

            @Override
            public void run() {
                historyKeywords = MusicUtils.mSongDb.querySearchHistory();
                mBanners = XiaMiSdkUtils.fetchBannerSync(mContext);
                LogUtil.d(TAG, "-fetchBannerSync");
                if (mBanners != null) {
                    if (mOnlineCollects != null && mOnlineAlbums != null && mBanners != null && mRadioCategories != null) {
                        showRequestSuccess();
                        LogUtil.d(TAG, "-fetchBannerSync Success");
                    }
                } else {
                    showRequestFail();
                    LogUtil.d(TAG, "-fetchBannerSync Fail");
                }
            }
        });

        // get radio
        executor.submit(new Runnable() {

            @Override
            public void run() {
                mRadioCategories = XiaMiSdkUtils.fetchRadioListsSync(mContext);
                Application.mRadioCategories = mRadioCategories;
                if (mRadioCategories != null) {
                    if (mOnlineCollects != null && mOnlineAlbums != null && mBanners != null) {
                        showRequestSuccess();
                        LogUtil.d(TAG, "-fetchRadioListsSync Success");
                    }
                } else {
                    showRequestFail();
                    LogUtil.d(TAG, "-fetchRadioListsSync Fail");
                }
            }
        });

    }

    private void showRequestFail() {
        LogUtil.d(TAG, "show network error!");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                isFirstClick = true;
                changeNetworkUi(1);
                noNetWork.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);
                mainListView.setVisibility(View.GONE);
            }
        });
    }

    private void showRequestSuccess() {
        LogUtil.d(TAG, "showRequestSuccess");
        // 排行榜
        ranklist = new ArrayList<HBRankItem>();
        HBRankItem itme = new HBRankItem("虾米音乐榜", R.drawable.hb_xiami_list, RankType.music_all);
        ranklist.add(itme);
        itme = new HBRankItem("虾米新歌榜", R.drawable.hb_xiami_new_list, RankType.newmusic_all);
        ranklist.add(itme);
        itme = new HBRankItem("虾米原创榜", R.drawable.hb_xiami_local_list, RankType.music_original);
        ranklist.add(itme);
		/*
		 * itme = new HBRankItem("虾米Demo榜", R.drawable.xiami_demo,
		 * RankType.music_demo); ranklist.add(itme);
		 */
        // itme = new HBRankItem("Hito中文榜", R.drawable.xiami_hito,
        // RankType.hito);
        // ranklist.add(itme);
        itme = new HBRankItem("Billboard单曲榜", R.drawable.hb_biboard, RankType.billboard);
        ranklist.add(itme);
        mHandler.post(mUpdateOnlineMusic);
    }

    private Runnable mUpdateOnlineMusic = new Runnable() {

        @SuppressWarnings("deprecation")
        @Override
        public void run() {
            mProgressBar.setVisibility(View.GONE);
            if (mOnlineMusicMainAdapter == null) {

                return;
            }
            mHBSearchHistoryAdapter.addDatas(historyKeywords);
            mainListView.setVisibility(View.VISIBLE);
            mOnlineMusicMainAdapter.setDatas(mOnlineAlbums, mOnlineCollects, ranklist, mRadioCategories);
            if (mBanners.size() > ImagePagerAdapter.MAXNUM) {
                countSize = ImagePagerAdapter.MAXNUM;

            } else {
                countSize = mBanners.size();
            }
            //mHbDotView.setDotCount(countSize);
            mPagerAdapter = new ImagePagerAdapter(mContext, mBanners, mOnBannerClickListener);
            if (countSize > 1) {
                mPagerAdapter.setInfiniteLoop(true);
            } else {
                mPagerAdapter.setInfiniteLoop(false);
            }
            bannerPage.setAdapter(mPagerAdapter);
            bannerPage.setCurrentItem(Integer.MAX_VALUE / 2 - Integer.MAX_VALUE / 2 % countSize);
            bannerPage.setOnPageChangeListener(mPageChangeListener);
            bannerPage.setInterval(3000);
            bannerPage.setScrollDurationFactor(1000);
            isShowView = true;
            bannerPage.startAutoScroll();
        }
    };

    private void addHistory(String key) {
        if (MusicUtils.mSongDb.insertSearchHistory(key)) {
            if (historyKeywords.size() >= 5) {
                historyKeywords.remove(0);
            }
            historyKeywords.add(key);
            mHBSearchHistoryAdapter.notifyDataSetChanged();

        }
    }

    // add by chenhl end

    // -add by tangjie 2014/12/26 start//
    private void showSearchView(final String key, final long time) {
        mSongCount = Integer.MAX_VALUE;
        mAlbumCount = Integer.MAX_VALUE;
        mArtistCount = Integer.MAX_VALUE;
        mPageNo = 1;
        mAlbumPageNo = 1;
        mArtistPageNo = 1;
        // rootLayoutView.setVisibility(View.VISIBLE);
        if (mHBSearchArtistAdapter != null) {
            mHBSearchArtistAdapter.clearCacheList();
        }
        if (mSearchPagerAdapter == null) {
            ArrayList<View> listViews = new ArrayList<View>();
            View songView = View.inflate(mContext, R.layout.hb_viewpager_item, null);
            mSongListView = (ListView) songView.findViewById(R.id.hb_online_music_search);
            mSongListView.setOnScrollListener(new AbsListView.OnScrollListener() {

                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                    // TODO Auto-generated method stub
                    if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                        if (view.getLastVisiblePosition() == view.getCount() - 1 && view.getLastVisiblePosition() < mSongCount - 1 && mSongCount != Integer.MAX_VALUE) {
                            loadMore(keyWord, 0);
                        }
                    }
                }

                @Override
                public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3) {
                    // TODO Auto-generated method stub

                }
            });
            mSongListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                    // TODO Auto-generated method stub
                    OnlineSong item = (OnlineSong) mHBSearchSongAdapter.getItem(arg2);
                    if (item != null) {
                        if (HBMusicUtil.isNoPermission(item)) {
                            Toast.makeText(mContext, R.string.hb_play_permission, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        HBListItem listItem = new HBListItem(item.getSongId(), item.getSongName(), "", item.getAlbumName(), item.getAlbumId(), item.getArtistName(), 1, item.getImageUrl(),
                                null, null, -1);
                        ArrayList<HBListItem> list = new ArrayList<HBListItem>();
                        list.add(listItem);
                        MusicUtils.playAll(mContext, list, 0, 0, true);
                        mSongListView.invalidateViews();
                        addHistory(item.getSongName());
                    }
                }
            });
            mSongListView.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    mSongListView.requestFocus();
                    return false;
                }
            });
            View ablumView = View.inflate(mContext, R.layout.hb_viewpager_item, null);
            mAlbumListView = (ListView) ablumView.findViewById(R.id.hb_online_music_search);
            mAlbumListView.setOnScrollListener(new AbsListView.OnScrollListener() {

                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                    // TODO Auto-generated method stub
                    if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                        if (view.getLastVisiblePosition() == view.getCount() - 1 && view.getLastVisiblePosition() < mAlbumCount - 1 && mAlbumCount != Integer.MAX_VALUE) {
                            loadMore(keyWord, 1);
                        }
                    }
                }

                @Override
                public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3) {
                    // TODO Auto-generated method stub

                }
            });
            mAlbumListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                    // TODO Auto-generated method stub
                    OnlineAlbum album = (OnlineAlbum) mAlbumList.get(arg2);
                    Intent intent = new Intent(mContext, HBNetTrackDetailActivity.class);
                    intent.putExtra("tag", OnGridViewClickListener.NEW_ALBUM);
                    intent.putExtra("title", album.getAlbumName());
                    intent.putExtra(HBNetTrackDetail.ID, String.valueOf(album.getAlbumId()));
                    intent.putExtra("imageUrl", album.getImageUrl((int) mContext.getResources().getDimension(R.dimen.hb_recommend_toplayout_height)));
                    intent.putExtra("artist", album.getArtistName());
                    mContext.startActivity(intent);
                    addHistory(album.getAlbumName());
                }
            });
            mAlbumListView.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    mAlbumListView.requestFocus();
                    return false;
                }
            });
            View artistView = View.inflate(mContext, R.layout.hb_viewpager_item, null);
            mArtistListView = (ListView) artistView.findViewById(R.id.hb_online_music_search);
            mArtistListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                    // TODO Auto-generated method stub
                    OnlineArtist item = mArtistList.get(arg2);
                    if (null != item) {
//                        Intent intent = new Intent(mContext, HBNetSearchActivity.class);
//                        intent.putExtra("title", item.getName());
//                        intent.putExtra("artist_id", item.getId());
//                        intent.putExtra("imageUrl", item.getImageUrl());
//                        intent.putExtra("song_count", item.getAlbumsCount());
//                        intent.putExtra("album_count", item.getAlbumsCount());
//                        mContext.startActivity(intent);
                        addHistory(item.getName());
                    }
                }
            });
            mArtistListView.setOnScrollListener(new AbsListView.OnScrollListener() {

                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                    // TODO Auto-generated method stub
                    if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                        if (view.getLastVisiblePosition() == view.getCount() - 1 && view.getLastVisiblePosition() < mArtistCount - 1 && mArtistCount != Integer.MAX_VALUE) {
                            loadMore(keyWord, 2);
                        }
                    }
                }

                @Override
                public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3) {
                    // TODO Auto-generated method stub

                }
            });
            mArtistListView.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    mArtistListView.requestFocus();
                    return false;
                }
            });
            listViews.add(songView);
            listViews.add(artistView);
            listViews.add(ablumView);
            mSearchPagerAdapter = new HBSearchPagerAdapter(listViews);
            mViewPager.setAdapter(mSearchPagerAdapter);
        }
        if (mSongList == null) {
            mSongList = new ArrayList<OnlineSong>();
        } else {
            mSongList.clear();
            if (mHBSearchSongAdapter != null) {
                mHBSearchSongAdapter.notifyDataSetChanged();
            }
        }
        if (mArtistList == null) {
            mArtistList = new ArrayList<OnlineArtist>();
        } else {
            mArtistList.clear();
            if (mHBSearchArtistAdapter != null) {
                mHBSearchArtistAdapter.notifyDataSetChanged();
            }
        }
        if (mAlbumList == null) {
            mAlbumList = new ArrayList<OnlineAlbum>();
        } else {
            mAlbumList.clear();
            if (mHBSearchAlbumAdapter != null) {
                mHBSearchAlbumAdapter.notifyDataSetChanged();
            }
        }
        executor.execute(new HbRunnale(mHandler, time));
    }

    private void refreshUI() {
        mSearchResultView.setVisibility(View.VISIBLE);
        mSongList.clear();
        mAlbumList.clear();
        mArtistList.clear();
        mSongList.addAll(mSongTempList);
        mAlbumList.addAll(mAlbumTemptList);
        mArtistList.addAll(mArtistTempList);
        if (mHBSearchSongAdapter == null) {
            mHBSearchSongAdapter = new HBSearchSongAdapter(mContext, mSongList);
            mSongListView.setAdapter(mHBSearchSongAdapter);
        } else {
            mHBSearchSongAdapter.notifyDataSetChanged();
            mSongListView.setSelection(0);
        }
        if (mHBSearchArtistAdapter == null) {
            mHBSearchArtistAdapter = new HBSearchArtistAdapter(mContext, mArtistList);
            mArtistListView.setAdapter(mHBSearchArtistAdapter);
        } else {
            mHBSearchArtistAdapter.notifyDataSetChanged();
        }
        if (mHBSearchAlbumAdapter == null) {
            mHBSearchAlbumAdapter = new HBSearchAlbumAdapter(mContext, mAlbumList);
            mAlbumListView.setAdapter(mHBSearchAlbumAdapter);
        } else {
            mHBSearchAlbumAdapter.notifyDataSetChanged();
        }
        if (mSongList != null && mSongList.size() == 0) {
            mTipText.setVisibility(View.VISIBLE);
        } else {
            mTipText.setVisibility(View.GONE);
        }
        searchhistory.setVisibility(View.GONE);
    }

    private void loadMore(final String key, final int type) {
        loadingView.setVisibility(View.VISIBLE);
        executor.execute(new LoadMoreRunnale(key, type));
    }

    private class LoadMoreRunnale implements Runnable {
        String key;
        int type;

        public LoadMoreRunnale(String key, int type) {
            super();
            this.key = key;
            this.type = type;
        }

        @Override
        public void run() {
            LogUtil.d(TAG, "key:" + key + " type:" + type);
            if (type == 0) {
                mPageNo += 1;
                Pair<QueryInfo, List<OnlineSong>> songPair = XiaMiSdkUtils.searchSongSync(mContext, key, PAGE_SIZE, mPageNo);
                if (songPair != null) {
                    if (songPair.second != null) {
                        mTempList = songPair.second;
                    }
                    mHandler.sendEmptyMessage(MSG_LOAD_MORE_SONG);
                }
            } else if (type == 1) {
                mAlbumPageNo += 1;
                Pair<QueryInfo, List<OnlineAlbum>> ablumPair = XiaMiSdkUtils.searchAlbumsSync(mContext, key, PAGE_SIZE, mAlbumPageNo);
                if (ablumPair != null && ablumPair.second != null) {
                    mTempList = ablumPair.second;
                }
                mHandler.sendEmptyMessage(MSG_LOAD_MORE_ALBUM);
            } else if (type == 2) {
                mArtistPageNo += 1;
                Pair<QueryInfo, List<OnlineArtist>> artistPair = XiaMiSdkUtils.searchArtistsSync(mContext, key, PAGE_SIZE, mArtistPageNo);
                if (artistPair != null) {
                    if (artistPair.second != null) {
                        mTempList = artistPair.second;
                    }
                    mHandler.sendEmptyMessage(MSG_LOAD_MORE_ARTIST);
                }
            }

        }

    }

    private class HbRunnale implements Runnable {
        long mSearchTime;
        Handler mHandler;

        public HbRunnale(Handler handler, long time) {
            mSearchTime = time;
            mHandler = handler;
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            if (mTimeStamp == mSearchTime) {
                SearchSummaryResult result = XiaMiSdkUtils.searchSummarySync(mContext, keyWord, 15);
                if (result != null) {
                    mSongCount = result.getSongsCount();
                    mSongTempList = result.getSongs();
                    mArtistCount = result.getArtistsCount();
                    mArtistTempList = result.getArtists();
                    mAlbumCount = result.getAlbumsCount();
                    mAlbumTemptList = result.getAlbums();
                    LogUtil.d(TAG, "mSongCount:" + mSongCount + " mArtistCount:" + mArtistCount + " mAlbumCount:" + mAlbumCount);
                    if (mTimeStamp == mSearchTime) {
                        mHandler.sendEmptyMessage(MSG_SEARCH_FINISHED);
                    }
                }
            }
        }

    }

}

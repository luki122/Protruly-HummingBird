package com.android.launcher3.wallpaperpicker;

import android.animation.TimeInterpolator;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.PagedView;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.colors.ColorManager;
import com.android.launcher3.pageindicators.PageIndicatorUnderline;
import com.android.launcher3.wallpaperpicker.WallpaperPicker.LiveWallpaperInfo;
import com.android.launcher3.wallpaperpicker.WallpaperPicker.PickImageInfo;
import com.android.launcher3.wallpaperpicker.WallpaperPicker.UriWallpaperInfo;
import com.android.launcher3.wallpaperpicker.WallpaperPicker.WallpaperTileInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WallpaperPagedView extends PagedView {

    private static final String KEY_TEMP_WALLPAPER_URI = "TEMP_WALLPAPER_URI";
    private static final int IMAGE_CROP = 1 << 7;

    private static final String TEMP_NAME = "temp_wallpaper.jpg";
    private static final String TEMP_NAME_2 = "temp_wallpaper2.jpg";

    private String mTempName = TEMP_NAME;

    private static final int ROW_SIZE = 4;
    private static final int TEMP_WALLPAPER_LIMIT_COUNT = 2;
    private static final String TEMP_WALLPAPER_TILES = "TEMP_WALLPAPER_TILES";
    private static final String SELECTED_INDEX = "SELECTED_INDEX";

    private List<WallpaperTileInfo> mWallpaperInfoList = new ArrayList<>();
    private ArrayList<Uri> mTempList = new ArrayList<>();

    private View mSelectedTile;
    private View mTempTile;

    private Activity mActivity;
    private LayoutInflater mLayoutInflater;
    private SharedPreferences mSharedPreferences;
    private WallpaperPageviewAdapter mPageviewAdapter;

    private static boolean stateSaved = false;
    private boolean mFinishInitDate;
    private View.OnClickListener mTileOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            WallpaperTileInfo wallpaperInfo = (WallpaperTileInfo) v.getTag();
            if (wallpaperInfo == null) {
                return;
            }

            if (v.isSelected() && !(wallpaperInfo instanceof LiveWallpaperInfo)) {
                return;
            }

            if (wallpaperInfo.isSelectable() && v.getVisibility() == View.VISIBLE) {
                selectTile(v);
            } else if (wallpaperInfo instanceof LiveWallpaperInfo) {
                mTempTile = v;
            }
            wallpaperInfo.onClick(mActivity);
        }
    };

    public WallpaperPagedView(Context context) {
        this(context, null);
    }

    public WallpaperPagedView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WallpaperPagedView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (context instanceof Activity) {
            mActivity = (Activity) context;
        }
        mLayoutInflater = LayoutInflater.from(context);
        mSharedPreferences = Utilities.getPrefs(context);

        initPages();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        final int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
        }

        mViewport.set(0, 0, widthSize, heightSize);
        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void getEdgeVerticalPostion(int[] pos) {
        View view = getChildAt(0);
        if (view != null) {
            pos[0] = view.getTop();
            pos[1] = view.getBottom();
        }
    }

    @Override
    protected void snapToPage(int whichPage, int delta, int duration, boolean immediate, TimeInterpolator interpolator) {
        if (mPageIndicator != null) {
            ((PageIndicatorUnderline) mPageIndicator).animateToAlpha(0f);
        }
        super.snapToPage(whichPage, delta, duration, immediate, interpolator);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (mPageIndicator != null) {
            mPageIndicator.setScroll(l, mMaxScrollX);
        }
    }

    @Override
    protected void determineScrollingStart(MotionEvent ev, float touchSlopScale) {
        super.determineScrollingStart(ev, touchSlopScale);
        if (mPageIndicator != null && mTouchState != TOUCH_STATE_SCROLLING) {
            ((PageIndicatorUnderline) mPageIndicator).animateToAlpha(1.0f);
        }
    }

    @Override
    protected void resetTouchState() {
        super.resetTouchState();
        ((PageIndicatorUnderline) mPageIndicator).animateToAlpha(0.0f);
    }

    private void initPages() {
        initData(getContext());
        initView();
    }

    private void initData(final Context context) {
        mWallpaperInfoList.add(new PickImageInfo());

        String uriString = mSharedPreferences.getString(KEY_TEMP_WALLPAPER_URI, "");
        if (!stateSaved && !TextUtils.isEmpty(uriString)) {
            addTempWallpaper(Uri.parse(uriString));
            updateTempName();
        }

        AsyncTask<Void, Void, Void> as = new AsyncTask<Void, Void, Void>(){
            ArrayList<WallpaperTileInfo> liveWallpapers= null;
            @Override
            protected Void doInBackground(Void... params) {
                 liveWallpapers = WallpaperPicker.findLiveWallpapers(context);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if(liveWallpapers!=null) {
                    mWallpaperInfoList.addAll(liveWallpapers);
                    initView();
                }
                mFinishInitDate = true;
            }
        };
        ArrayList<WallpaperTileInfo> wallpapers =
                WallpaperPicker.findBundledWallpapers(getContext());
        mWallpaperInfoList.addAll(wallpapers);
        as.execute();
        mPageviewAdapter = new WallpaperPageviewAdapter(getContext(), mWallpaperInfoList);
    }

    private void initView() {
        removeAllViews();

        int tileSize = mWallpaperInfoList.size();
        int pageSize = tileSize / ROW_SIZE + ((tileSize % ROW_SIZE > 0) ? 1 : 0);
        for (int i = 0; i < pageSize; i++) {
            LinearLayout pageView = (LinearLayout) mLayoutInflater.inflate(
                    R.layout.wallpaper_pageview, this, false);

            for (int j = 0; j < ROW_SIZE; j++) {
                int index = i * ROW_SIZE + j;
                if (index >= tileSize) break;

                WallpaperTileInfo wallpaperTileInfo = mPageviewAdapter.getItem(index);
                View pageviewItem = mPageviewAdapter.getView(index, null, pageView);
                pageView.addView(pageviewItem);
                bindView(pageviewItem, wallpaperTileInfo);
            }
            addView(pageView);
        }
    }

    private void bindView(View view, WallpaperTileInfo wallpaperTileInfo) {
        view.setTag(wallpaperTileInfo);
        wallpaperTileInfo.setView(view);
        view.setOnClickListener(mTileOnClickListener);
    }

    private void selectTile(View v) {
        if (mSelectedTile != null) {
            mSelectedTile.setSelected(false);
            mSelectedTile = null;
        }
        mSelectedTile = v;
        v.setSelected(true);

        WallpaperTileInfo wallpaperInfo = (WallpaperTileInfo) v.getTag();
        if (wallpaperInfo instanceof UriWallpaperInfo) {
            mSharedPreferences.edit().putString(KEY_TEMP_WALLPAPER_URI,
                    ((UriWallpaperInfo) wallpaperInfo).mUri.toString()).apply();
        }
    }

    private void clearTile() {
        if (mSelectedTile != null) {
            mSelectedTile.setSelected(false);
        }
    }

    private void updateTempName() {
        if (mTempList.isEmpty()) {
            mTempName = TEMP_NAME;
        } else {
            Uri uri = mTempList.get(0);
            if (uri.toString().endsWith(TEMP_NAME)) {
                mTempName = TEMP_NAME_2;
            } else {
                mTempName = TEMP_NAME;
            }
        }
    }

    private Uri getTempUri(String fileName) {
        final File dir = mActivity.getExternalCacheDir();
        dir.mkdirs();
        final File f = new File(dir, fileName);
        return Uri.fromFile(f);
    }

    public static int getCommonDivisor(int m, int n) {
        while (m % n != 0) {
            int temp = m % n;
            m = n;
            n = temp;
        }
        return n;
    }

    private void startCrop(Uri inputUri, Uri outputUri) {
        Point point = WallpaperPicker.getDefaultWallpaperSize(mActivity.getWindowManager());
        int commonDivisor = getCommonDivisor(point.y, point.x);

        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(inputUri, "image/*");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
        intent.putExtra("scale", true);
        intent.putExtra("scaleUpIfNeeded", true);
        intent.putExtra("aspectX", point.x / commonDivisor);
        intent.putExtra("aspectY", point.y / commonDivisor);

        try {
            mActivity.startActivityForResult(intent, IMAGE_CROP);
        } catch (ActivityNotFoundException e) {
            Log.w(WallpaperPicker.TAG, "Can't goto cropping");
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == WallpaperPicker.IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                startCrop(data.getData(), getTempUri(mTempName));
            }
        } else if (requestCode == IMAGE_CROP && resultCode == Activity.RESULT_OK) {
            /*if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                if (mTempList.contains(uri)) {
                    int index = mTempList.indexOf(uri) + 1;
                    WallpaperTileInfo wallpaperInfo = mWallpaperInfoList.get(index);
                    mTileOnClickListener.onClick(wallpaperInfo.mView);
                } else {
                    addTempWallpaperTile(uri);
                }
            }*/
            addTempWallpaperTile(getTempUri(mTempName));
            updateTempName();
        } else if (requestCode == WallpaperPicker.PICK_WALLPAPER_THIRD_PARTY_ACTIVITY
                && resultCode == Activity.RESULT_OK) {
            selectTile(mTempTile);
            ColorManager.getInstance().dealWallpaperForLauncher(LauncherAppState.getInstance().getContext());//liuzuo add
        }
    }

    private void addTempWallpaperTile(Uri uri) {
        Drawable thumb = WallpaperPicker.createThumbnail(getContext(), uri);
        UriWallpaperInfo wallpaperInfo = new UriWallpaperInfo(uri, thumb);

        boolean reachLimit = false;
        if (mTempList.size() == TEMP_WALLPAPER_LIMIT_COUNT) {
            reachLimit = true;
            mTempList.remove(TEMP_WALLPAPER_LIMIT_COUNT - 1);
            mWallpaperInfoList.remove(TEMP_WALLPAPER_LIMIT_COUNT);
        }
        mTempList.add(0, uri);
        mWallpaperInfoList.add(1, wallpaperInfo);

        if (reachLimit) {
            ViewGroup pageView = (ViewGroup) getChildAt(0);
            pageView.removeViewAt(TEMP_WALLPAPER_LIMIT_COUNT);

            View pageviewItem = mPageviewAdapter.getView(1, null, pageView);
            pageView.addView(pageviewItem, 1);
            bindView(pageviewItem, wallpaperInfo);
        } else {
            initView();
        }
        mTileOnClickListener.onClick(wallpaperInfo.mView);
    }

    public void refresh(boolean comeIn) {
        boolean wallpapersChanged;
        if (comeIn) {
            wallpapersChanged = refreshLiveWallpapers();
        } else {
            wallpapersChanged = refreshTempWallpapers();
        }

        final boolean initView = wallpapersChanged;
        postDelayed(new Runnable() {
            @Override
            public void run() {
                clearTile();
                if (initView) {
                    updateTempName();
                    initView();
                }
            }
        }, 200);
    }

    private boolean refreshLiveWallpapers() {
        if(!mFinishInitDate){
            return false;
        }
        ArrayList<String> existPackageNames = new ArrayList<>();
        for (WallpaperTileInfo wallpaperTileInfo : mWallpaperInfoList) {
            if (wallpaperTileInfo instanceof LiveWallpaperInfo) {
                String packageName = ((LiveWallpaperInfo) wallpaperTileInfo).mInfo
                        .getPackageName();
                existPackageNames.add(packageName);
            }
        }

        ArrayList<String> freshPackageNames = new ArrayList<>();
        ArrayList<WallpaperTileInfo> liveWallpapers =
                WallpaperPicker.findLiveWallpapers(getContext());
        for (WallpaperTileInfo wallpaperTileInfo : liveWallpapers) {
            String packageName = ((LiveWallpaperInfo) wallpaperTileInfo).mInfo
                    .getPackageName();
            freshPackageNames.add(packageName);
        }

        boolean liveWallpapersChanged = false;
        if (existPackageNames.size() != freshPackageNames.size()) {
            liveWallpapersChanged = true;
        } else {
            for (int i = 0; i < existPackageNames.size(); i++) {
                if (!existPackageNames.get(i).equals(freshPackageNames.get(i))) {
                    liveWallpapersChanged = true;
                    break;
                }
            }
        }
        if (liveWallpapersChanged) {
            mWallpaperInfoList = mWallpaperInfoList.subList(
                    0, mWallpaperInfoList.size() - existPackageNames.size());
            mWallpaperInfoList.addAll(liveWallpapers);
        }

        return liveWallpapersChanged;
    }

    private boolean refreshTempWallpapers() {
        if (mTempList.isEmpty() || mTempList.size() == 1) {
            return false;
        }

        String uriString = mSharedPreferences.getString(KEY_TEMP_WALLPAPER_URI, "");
        int index = 0;
        for (int i = 0; i < mTempList.size(); i++) {
            if (mTempList.get(i).toString().equals(uriString)) {
                index++;
                continue;
            }
            mTempList.remove(index);
            mWallpaperInfoList.remove(index + 1);
        }
        return true;
    }

    public void onSaveInstanceState(Bundle outState) {
        stateSaved = true;

        outState.putParcelableArrayList(TEMP_WALLPAPER_TILES, mTempList);
        int selectIndex = -1;
        if (mSelectedTile != null) {
            WallpaperTileInfo wallpaperTileInfo = (WallpaperTileInfo) mSelectedTile.getTag();
            selectIndex = mWallpaperInfoList.indexOf(wallpaperTileInfo);
        }
        outState.putInt(SELECTED_INDEX, selectIndex);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        stateSaved = false;

        ArrayList<Uri> uris = savedInstanceState.getParcelableArrayList(TEMP_WALLPAPER_TILES);
        if (uris != null && uris.size() > 0) {
            for (int i = uris.size() - 1; i >= 0; i--) {
                addTempWallpaper(uris.get(i));
            }
            updateTempName();
            initView();
        }

        int selectedIndex = savedInstanceState.getInt(SELECTED_INDEX, -1);
        if (selectedIndex == -1 || selectedIndex >= mWallpaperInfoList.size()) {
            return;
        }

        WallpaperTileInfo wallpaperTileInfo = mWallpaperInfoList.get(selectedIndex);
        selectTile(wallpaperTileInfo.mView);
    }

    private void addTempWallpaper(Uri uri) {
        Drawable thumb = WallpaperPicker.createThumbnail(getContext(), uri);
        if (thumb == null) {
            return;
        }
        UriWallpaperInfo wallpaperInfo = new UriWallpaperInfo(uri, thumb);

        mTempList.add(0, uri);
        mWallpaperInfoList.add(1, wallpaperInfo);
    }
}

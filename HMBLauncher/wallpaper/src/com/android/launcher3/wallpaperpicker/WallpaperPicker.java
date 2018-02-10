package com.android.launcher3.wallpaperpicker;

import android.app.Activity;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.service.wallpaper.WallpaperService;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;

import com.android.launcher3.R;
import com.android.launcher3.wallpaperpicker.util.BitmapCropTask;
import com.android.launcher3.wallpaperpicker.util.BitmapUtils;
import com.android.launcher3.wallpaperpicker.util.Partner;
import com.android.launcher3.wallpaperpicker.util.Utils;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class WallpaperPicker extends RecyclerView {

    public static final String TAG = "WallpaperPicker";

    public static final int IMAGE_PICK = 1 << 5;
    public static final int PICK_WALLPAPER_THIRD_PARTY_ACTIVITY = 1 << 6;

    private static final int TEMP_WALLPAPER_LIMIT_COUNT = 2;
    private static final String TEMP_WALLPAPER_TILES = "TEMP_WALLPAPER_TILES";
    private static final String SELECTED_INDEX = "SELECTED_INDEX";

    private List<WallpaperTileInfo> mWallpaperInfoList = new ArrayList<>();
    private ArrayList<Uri> mTempList = new ArrayList<>();

    private Activity mActivity;
    private WallpaperPickerAdapter mWallpaperPickerAdapter;

    private View mSelectedTile;

    private View.OnClickListener mTileOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int position = (int) v.getTag();
            if (position < 0 || position >= mWallpaperInfoList.size()) {
                return;
            }

            WallpaperTileInfo wallpaperInfo = mWallpaperInfoList.get(position);
            if (v.isSelected() && !(wallpaperInfo instanceof LiveWallpaperInfo)) {
                return;
            }

            if (wallpaperInfo.isSelectable() && v.getVisibility() == View.VISIBLE) {
                selectTile(v);
                mWallpaperPickerAdapter.setSelectedPostion(position);
            } else if (wallpaperInfo instanceof LiveWallpaperInfo) {
                selectTile(v);
            }
            wallpaperInfo.onClick(mActivity);
        }
    };

    public static abstract class WallpaperTileInfo {
        View mView;
        public Drawable mThumb;

        void setView(View v) {
            mView = v;
        }

        public void onClick(Activity a) {
        }

        public boolean isSelectable() {
            return true;
        }
    }

    static class PickImageInfo extends WallpaperTileInfo {
        @Override
        public void onClick(Activity a) {
            try {
                Intent intent = new Intent(Intent.ACTION_PICK/*ACTION_GET_CONTENT*/);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.setType("image/*");
                a.startActivityForResult(intent, IMAGE_PICK);
            }catch (ActivityNotFoundException e){
                try {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.setType("image/*");
                    a.startActivityForResult(intent, IMAGE_PICK);
                }catch (ActivityNotFoundException e1){
                    Log.i(TAG,"PickImageInfo : ",e1);
                }
            }
        }

        @Override
        public boolean isSelectable() {
            return false;
        }
    }

    static class ResourceWallpaperInfo extends WallpaperTileInfo {
        Resources mResources;
        int mResId;

        ResourceWallpaperInfo(Resources res, int resId, Drawable thumb) {
            mResources = res;
            mResId = resId;
            mThumb = thumb;
        }

        @Override
        public void onClick(Activity a) {
            setWallpaperNoCrop(a, mResources, mResId);
        }
    }

    static class UriWallpaperInfo extends WallpaperTileInfo {
        Uri mUri;

        UriWallpaperInfo(Uri uri, Drawable thumb) {
            mUri = uri;
            mThumb = thumb;
        }

        @Override
        public void onClick(Activity a) {
            setWallpaper(a, mUri);
        }
    }

    static class FileWallpaperInfo extends WallpaperTileInfo {
        File mFile;

        FileWallpaperInfo(File target, Drawable thumb) {
            mFile = target;
            mThumb = thumb;
        }

        @Override
        public void onClick(Activity a) {
            setWallpaperNoCrop(a, Uri.fromFile(mFile));
        }
    }

    public static class LiveWallpaperInfo extends WallpaperTileInfo {
        public WallpaperInfo mInfo;

        LiveWallpaperInfo(WallpaperInfo info, Drawable thumb) {
            mInfo = info;
            mThumb = thumb;
        }

        @Override
        public void onClick(Activity a) {
            Intent preview = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            preview.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    mInfo.getComponent());
            a.startActivityForResult(preview, PICK_WALLPAPER_THIRD_PARTY_ACTIVITY);
        }

        @Override
        public boolean isSelectable() {
            return false;
        }
    }

    public WallpaperPicker(Context context) {
        this(context, null);
    }

    public WallpaperPicker(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WallpaperPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (context instanceof Activity) {
            mActivity = (Activity) context;
        }
        init();
    }

    private void init() {
        initData();
        initView();
    }

    private void initData() {
        mWallpaperInfoList.add(new PickImageInfo());

        ArrayList<WallpaperTileInfo> wallpapers = findBundledWallpapers(getContext());
        mWallpaperInfoList.addAll(wallpapers);

        ArrayList<WallpaperTileInfo> liveWallpapers = findLiveWallpapers(getContext());
        mWallpaperInfoList.addAll(liveWallpapers);
    }

    private void initView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        setLayoutManager(linearLayoutManager);

        mWallpaperPickerAdapter = new WallpaperPickerAdapter(getContext(), mWallpaperInfoList);
        mWallpaperPickerAdapter.setOnItemClickListener(mTileOnClickListener);
        setAdapter(mWallpaperPickerAdapter);
    }

    private void selectTile(View v) {
        if (mSelectedTile != null) {
            mSelectedTile.setSelected(false);
            mSelectedTile = null;
        }
        mSelectedTile = v;
        v.setSelected(true);
    }

    private void clearTile() {
        if (mSelectedTile != null) {
            mSelectedTile.setSelected(false);
            mSelectedTile = null;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                if (mTempList.contains(uri)) {
                    int index = mTempList.indexOf(uri) + 1;
                    WallpaperTileInfo wallpaperInfo = mWallpaperInfoList.get(index);
                    int position = (int) wallpaperInfo.mView.getTag();
                    if (index != position) {
                        mWallpaperPickerAdapter.setSelectedPostion(index, true);
                    } else {
                        mTileOnClickListener.onClick(wallpaperInfo.mView);
                    }
                } else {
                    Drawable thumb = createThumbnail(getContext(), uri);
                    UriWallpaperInfo wallpaperInfo = new UriWallpaperInfo(uri, thumb);

                    if (mTempList.size() == TEMP_WALLPAPER_LIMIT_COUNT) {
                        mTempList.remove(TEMP_WALLPAPER_LIMIT_COUNT - 1);
                        mWallpaperInfoList.remove(TEMP_WALLPAPER_LIMIT_COUNT);
                    }
                    mTempList.add(0, uri);
                    mWallpaperInfoList.add(1, wallpaperInfo);
                    mWallpaperPickerAdapter.setSelectedPostion(1, true);
                    mWallpaperPickerAdapter.notifyDataSetChanged();
                }
                scrollToPosition(0);
            }
        } else if (requestCode == PICK_WALLPAPER_THIRD_PARTY_ACTIVITY
                && resultCode == Activity.RESULT_OK) {
            if (mSelectedTile != null) {
                int position = (int) mSelectedTile.getTag();
                mWallpaperPickerAdapter.setSelectedPostion(position);
            }
        } else if (requestCode == PICK_WALLPAPER_THIRD_PARTY_ACTIVITY) {
            int index = mWallpaperPickerAdapter.getSelectPosition();
            if (index >= 0 && index < mWallpaperInfoList.size()) {
                WallpaperTileInfo wallpaperInfo = mWallpaperInfoList.get(index);
                int position = (int) wallpaperInfo.mView.getTag();
                if (index == position) {
                    selectTile(wallpaperInfo.mView);
                } else {
                    clearTile();
                }
            } else {
                clearTile();
            }
        }
    }

    public void refresh() {
        for (int i = 0; i < mTempList.size(); i++) {
            mWallpaperInfoList.remove(1);
        }
        mTempList.clear();
        mWallpaperPickerAdapter.setSelectedPostion(-1);
        mWallpaperPickerAdapter.notifyDataSetChanged();
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(TEMP_WALLPAPER_TILES, mTempList);
        outState.putInt(SELECTED_INDEX, mWallpaperPickerAdapter.getSelectPosition());
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        ArrayList<Uri> uris = savedInstanceState.getParcelableArrayList(TEMP_WALLPAPER_TILES);
        if (uris != null) {
            for (int i = uris.size() - 1; i >= 0; i--) {
                addTemporaryWallpaperTile(uris.get(i));
            }
        }

        int selectedIndex = savedInstanceState.getInt(SELECTED_INDEX, -1);
        mWallpaperPickerAdapter.setSelectedPostion(selectedIndex);
        mWallpaperPickerAdapter.notifyDataSetChanged();
    }

    private void addTemporaryWallpaperTile(Uri uri) {
        Drawable thumb = createThumbnail(getContext(), uri);
        UriWallpaperInfo wallpaperInfo = new UriWallpaperInfo(uri, thumb);

        mTempList.add(0, uri);
        mWallpaperInfoList.add(1, wallpaperInfo);
    }

    public static ArrayList<WallpaperTileInfo> findLiveWallpapers(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentServices(
                new Intent(WallpaperService.SERVICE_INTERFACE),
                PackageManager.GET_META_DATA);

        Collections.sort(list, new Comparator<ResolveInfo>() {
            final Collator mCollator;

            {
                mCollator = Collator.getInstance();
            }

            public int compare(ResolveInfo info1, ResolveInfo info2) {
                return mCollator.compare(info1.loadLabel(packageManager),
                        info2.loadLabel(packageManager));
            }
        });

        ArrayList<WallpaperTileInfo> wallpaperInfos = new ArrayList<>();
        for (ResolveInfo resolveInfo : list) {
            WallpaperInfo info;
            try {
                info = new WallpaperInfo(context, resolveInfo);
            } catch (XmlPullParserException e) {
                Log.w(TAG, "Skipping wallpaper " + resolveInfo.serviceInfo, e);
                continue;
            } catch (IOException e) {
                Log.w(TAG, "Skipping wallpaper " + resolveInfo.serviceInfo, e);
                continue;
            }

            Drawable thumb = info.loadThumbnail(packageManager);
            if (thumb != null) {
                thumb = createThumbnail(context, getBytes(thumb));
            }
            LiveWallpaperInfo wallpaperInfo = new LiveWallpaperInfo(info, thumb);
            wallpaperInfos.add(wallpaperInfo);
        }

        return wallpaperInfos;
    }

    static ArrayList<WallpaperTileInfo> findBundledWallpapers(Context context) {
        final PackageManager pm = context.getPackageManager();
        final ArrayList<WallpaperTileInfo> bundled = new ArrayList<>(24);

        Partner partner = Partner.get(pm);
        if (partner != null) {
            final Resources partnerRes = partner.getResources();
            final int resId = partnerRes.getIdentifier(Partner.RES_WALLPAPERS, "array",
                    partner.getPackageName());
            if (resId != 0) {
                addWallpapers(bundled, partnerRes, partner.getPackageName(), resId);
            }

            // Add system wallpapers
            File systemDir = partner.getWallpaperDirectory();
            if (systemDir != null && systemDir.isDirectory()) {
                for (File file : systemDir.listFiles()) {
                    if (!file.isFile()) {
                        continue;
                    }
                    String name = file.getName();
                    int dotPos = name.lastIndexOf('.');
                    String extension = "";
                    if (dotPos >= -1) {
                        extension = name.substring(dotPos);
                        name = name.substring(0, dotPos);
                    }

                    if (name.endsWith("_small")) {
                        // it is a thumbnail
                        continue;
                    }

                    File thumbnail = new File(systemDir, name + "_small" + extension);
                    Bitmap thumb = BitmapFactory.decodeFile(thumbnail.getAbsolutePath());
                    if (thumb != null) {
                        bundled.add(new FileWallpaperInfo(file,
                                new BitmapDrawable(context.getResources(), thumb)));
                    }
                }
            }
        }

        Pair<ApplicationInfo, Integer> r = getWallpaperArrayResourceId(context);
        if (r != null) {
            try {
                Resources wallpaperRes = context.getPackageManager()
                        .getResourcesForApplication(r.first);
                addWallpapers(bundled, wallpaperRes, r.first.packageName, r.second);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "Could't find wallpaper resource");
            }
        }

        return bundled;
    }

    static void addWallpapers(
            ArrayList<WallpaperTileInfo> known, Resources res, String packageName, int listResId) {
        final String[] extras = res.getStringArray(listResId);
        for (String extra : extras) {
            int resId = res.getIdentifier(extra, "drawable", packageName);
            if (resId != 0) {
                final int thumbRes = res.getIdentifier(extra + "_small", "drawable", packageName);

                if (thumbRes != 0) {
                    ResourceWallpaperInfo wallpaperInfo =
                            new ResourceWallpaperInfo(res, resId, res.getDrawable(thumbRes, null));
                    known.add(wallpaperInfo);
                    // Log.d(TAG, "add: [" + packageName + "]: " + extra + " (" + res + ")");
                }
            } else {
                Log.e(TAG, "Couldn't find wallpaper " + extra);
            }
        }
    }

    static Pair<ApplicationInfo, Integer> getWallpaperArrayResourceId(Context context) {
        // Context.getPackageName() may return the "original" package name,
        // com.android.launcher3; Resources needs the real package name,
        // com.android.launcher3. So we ask Resources for what it thinks the
        // package name should be.
        final String packageName = context.getResources()
                .getResourcePackageName(R.array.wallpapers);
        try {
            ApplicationInfo info = context.getPackageManager()
                    .getApplicationInfo(packageName, 0);
            return new Pair<>(info, R.array.wallpapers);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    static Point getDefaultThumbnailSize(Resources res) {
        return new Point(res.getDimensionPixelSize(R.dimen.wallpaper_thumbnail_width),
                res.getDimensionPixelSize(R.dimen.wallpaper_thumbnail_height));
    }

    static Point getDefaultWallpaperSize(WindowManager windowManager) {
        Point realSize = new Point();
        windowManager.getDefaultDisplay().getRealSize(realSize);
        int maxDim = Math.max(realSize.x, realSize.y);
        int minDim = Math.min(realSize.x, realSize.y);
        realSize.set(minDim, maxDim);
        return realSize;
    }

    static int getRoundCornerRadius(Resources res) {
        return res.getDimensionPixelSize(R.dimen.wallpapar_round_conner_radius);
    }

    static byte[] getBytes(Drawable drawable) {
        Bitmap bitmap = BitmapUtils.drawableToBitmap(drawable);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);

        bitmap.recycle();

        return out.toByteArray();
    }

    static Drawable createThumbnail(Context context, byte[] imageBytes) {
        Point defaultSize = getDefaultThumbnailSize(context.getResources());

        Bitmap bitmap = createThumbnail(defaultSize, context, null, imageBytes, null, 0, 0, false);
        if (bitmap != null) {
            bitmap = BitmapUtils.roundCorners(bitmap, getRoundCornerRadius(context.getResources()));
            return new BitmapDrawable(context.getResources(), bitmap);
        }
        return null;
    }

    static Drawable createThumbnail(Context context, Uri uri) {
        Point defaultSize = getDefaultThumbnailSize(context.getResources());
        int rotation = BitmapUtils.getRotationFromExif(context, uri);

        Bitmap bitmap = createThumbnail(defaultSize, context, uri, null, null, 0, rotation, false);
        if (bitmap != null) {
            bitmap = BitmapUtils.roundCorners(bitmap, getRoundCornerRadius(context.getResources()));
            return new BitmapDrawable(context.getResources(), bitmap);
        }
        return null;
    }

    static Bitmap createThumbnail(Point size, Context context, Uri uri, byte[] imageBytes,
                                  Resources res, int resId, int rotation, boolean leftAligned) {
        int width = size.x;
        int height = size.y;

        BitmapCropTask cropTask;
        if (uri != null) {
            cropTask = new BitmapCropTask(
                    context, uri, null, rotation, width, height, false, true, null);
        } else if (imageBytes != null) {
            cropTask = new BitmapCropTask(
                    imageBytes, null, rotation, width, height, false, true, null);
        } else {
            cropTask = new BitmapCropTask(
                    context, res, resId, null, rotation, width, height, false, true, null);
        }
        Point bounds = cropTask.getImageBounds();
        if (bounds == null || bounds.x == 0 || bounds.y == 0) {
            return null;
        }

        Matrix rotateMatrix = new Matrix();
        rotateMatrix.setRotate(rotation);
        float[] rotatedBounds = new float[]{bounds.x, bounds.y};
        rotateMatrix.mapPoints(rotatedBounds);
        rotatedBounds[0] = Math.abs(rotatedBounds[0]);
        rotatedBounds[1] = Math.abs(rotatedBounds[1]);

        RectF cropRect = Utils.getMaxCropRect(
                (int) rotatedBounds[0], (int) rotatedBounds[1], width, height, leftAligned);
        cropTask.setCropBounds(cropRect);

        if (cropTask.cropBitmap()) {
            return cropTask.getCroppedBitmap();
        } else {
            return null;
        }
    }

    static void setWallpaperNoCrop(Context context, Resources res, int resId) {
        setWallpaperNoCrop(context, res, resId, null);
    }

    static void setWallpaperNoCrop(Context context, Resources res, int resId,
                                   BitmapCropTask.OnBitmapCroppedHandler onBitmapCroppedHandler) {
        setWallpaperNoCrop(context, null, null, res, resId, onBitmapCroppedHandler);
    }

    static void setWallpaperNoCrop(Context context, Uri uri) {
        setWallpaperNoCrop(context, uri, null);
    }

    static void setWallpaperNoCrop(Context context, Uri uri,
                                   BitmapCropTask.OnBitmapCroppedHandler onBitmapCroppedHandler) {
        setWallpaperNoCrop(context, uri, null, null, 0, onBitmapCroppedHandler);
    }

    static void setWallpaperNoCrop(Context context, Uri uri,
                                   byte[] imageBytes, Resources res, int resId,
                                   BitmapCropTask.OnBitmapCroppedHandler onBitmapCroppedHandler) {
        BitmapCropTask cropTask;
        if (uri != null) {
            cropTask = new BitmapCropTask(
                    context, uri, null, 0, 0, 0, true, false, null);
        } else if (imageBytes != null) {
            cropTask = new BitmapCropTask(
                    imageBytes, null, 0, 0, 0, true, false, null);
        } else {
            cropTask = new BitmapCropTask(
                    context, res, resId, null, 0, 0, 0, true, false, null);
        }
        if (onBitmapCroppedHandler != null) {
            cropTask.setOnBitmapCropped(onBitmapCroppedHandler);
        }
        cropTask.setNoCrop(true);
        cropTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);//liuzuo change execute to  executeOnExecutor
    }

    static void setWallpaper(Context context, Resources res, int resId) {
        setWallpaper(context, res, resId, null);
    }

    static void setWallpaper(Context context, Resources res, int resId,
                             BitmapCropTask.OnBitmapCroppedHandler onBitmapCroppedHandler) {
        int rotation = BitmapUtils.getRotationFromExif(res, resId);
        setWallpaper(context, null, null, res, resId, rotation, false, onBitmapCroppedHandler);
    }

    static void setWallpaper(Context context, Uri uri) {
        setWallpaper(context, uri, null);
    }

    static void setWallpaper(Context context, Uri uri,
                             BitmapCropTask.OnBitmapCroppedHandler onBitmapCroppedHandler) {
        int rotation = BitmapUtils.getRotationFromExif(context, uri);
        setWallpaper(context, uri, null, null, 0, rotation, false, onBitmapCroppedHandler);
    }

    static void setWallpaper(Context context, Uri uri, byte[] imageBytes,
                             Resources res, int resId, int rotation, boolean leftAligned,
                             BitmapCropTask.OnBitmapCroppedHandler onBitmapCroppedHandler) {

        Point size = getDefaultWallpaperSize(((Activity) context).getWindowManager());

        int width = size.x;
        int height = size.y;
        Log.i(TAG, "width = " + width + " height = " + height + " rotation = " + rotation);

        BitmapCropTask cropTask;
        if (uri != null) {
            cropTask = new BitmapCropTask(
                    context, uri, null, rotation, width, height, true, false, null);
        } else if (imageBytes != null) {
            cropTask = new BitmapCropTask(
                    imageBytes, null, rotation, width, height, true, false, null);
        } else {
            cropTask = new BitmapCropTask(
                    context, res, resId, null, rotation, width, height, true, false, null);
        }
        Point bounds = cropTask.getImageBounds();
        if (bounds == null || bounds.x == 0 || bounds.y == 0) {
            return;
        }

        Matrix rotateMatrix = new Matrix();
        rotateMatrix.setRotate(rotation);
        float[] rotatedBounds = new float[]{bounds.x, bounds.y};
        rotateMatrix.mapPoints(rotatedBounds);
        rotatedBounds[0] = Math.abs(rotatedBounds[0]);
        rotatedBounds[1] = Math.abs(rotatedBounds[1]);

        RectF cropRect = Utils.getMaxCropRect(
                (int) rotatedBounds[0], (int) rotatedBounds[1], width, height, leftAligned);
        cropTask.setCropBounds(cropRect);
        if (onBitmapCroppedHandler != null) {
            cropTask.setOnBitmapCropped(onBitmapCroppedHandler);
        }
        cropTask.execute();
    }

}

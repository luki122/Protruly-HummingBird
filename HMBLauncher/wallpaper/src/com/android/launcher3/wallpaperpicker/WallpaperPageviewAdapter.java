package com.android.launcher3.wallpaperpicker;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.launcher3.R;
import com.android.launcher3.wallpaperpicker.WallpaperPicker.LiveWallpaperInfo;
import com.android.launcher3.wallpaperpicker.WallpaperPicker.PickImageInfo;
import com.android.launcher3.wallpaperpicker.WallpaperPicker.WallpaperTileInfo;

import java.util.List;

class WallpaperPageviewAdapter extends ArrayAdapter<WallpaperTileInfo> {

    private final LayoutInflater mLayoutInflater;
    private final PackageManager mPackageManager;

    WallpaperPageviewAdapter(Context context, List<WallpaperTileInfo> wallpaperTileInfos) {
        super(context, R.layout.wallpaper_pageview_item, wallpaperTileInfos);
        mLayoutInflater = LayoutInflater.from(context);
        mPackageManager = context.getPackageManager();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        WallpaperTileInfo wallpaperTileInfo = getItem(position);

        View view;
        if (convertView == null) {
            int layoutResId = R.layout.wallpaper_pageview_item;
            if (wallpaperTileInfo instanceof LiveWallpaperInfo) {
                layoutResId = R.layout.wallpaper_pageview_live_wallpaper_item;
            } else if (wallpaperTileInfo instanceof PickImageInfo) {
                layoutResId = R.layout.wallpaper_pageview_pick_image_item;
            }
            view = mLayoutInflater.inflate(layoutResId, parent, false);
        } else {
            view = convertView;
        }

        if (wallpaperTileInfo == null) {
            return view;
        }

        ImageView tileImage = (ImageView) view.findViewById(R.id.wallpaper_image);
        if (wallpaperTileInfo.mThumb != null) {
            tileImage.setImageDrawable(wallpaperTileInfo.mThumb);
        }

        if (wallpaperTileInfo instanceof LiveWallpaperInfo) {
            ImageView tileIcon = (ImageView) view.findViewById(R.id.wallpaper_icon);
            TextView tileLabel = (TextView) view.findViewById(R.id.wallpaper_label);
            LiveWallpaperInfo liveWallpaperInfo = (LiveWallpaperInfo) wallpaperTileInfo;
            if (liveWallpaperInfo.mThumb == null && tileIcon != null) {
                tileIcon.setVisibility(View.VISIBLE);
                tileIcon.setImageDrawable(liveWallpaperInfo.mInfo.loadIcon(mPackageManager));
            }
            tileLabel.setText(liveWallpaperInfo.mInfo.loadLabel(mPackageManager));
        }

        return view;
    }
}

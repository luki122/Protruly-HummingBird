package com.android.launcher3.wallpaperpicker;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.launcher3.R;
import com.android.launcher3.wallpaperpicker.WallpaperPicker.LiveWallpaperInfo;
import com.android.launcher3.wallpaperpicker.WallpaperPicker.PickImageInfo;
import com.android.launcher3.wallpaperpicker.WallpaperPicker.WallpaperTileInfo;

import java.util.List;

class WallpaperPickerAdapter extends RecyclerView.Adapter<WallpaperPickerAdapter.TileHolder> {

    private static final int VIEW_TYPE_DEFAULT = 0;
    private static final int VIEW_TYPE_LIVE = 1;
    private static final int VIEW_TYPE_PICK_IMAGE = 2;

    private LayoutInflater mLayoutInflater;
    private PackageManager mPackageManager;

    private List<WallpaperTileInfo> mWallpaperInfoList;

    private boolean mClickTile = false;
    private int mSelectedPosition = -1;
    private View.OnClickListener mOnItemClickListener;

    WallpaperPickerAdapter(Context context, List<WallpaperTileInfo> wallpaperInfoList) {
        mLayoutInflater = LayoutInflater.from(context);
        mPackageManager = context.getPackageManager();
        mWallpaperInfoList = wallpaperInfoList;
    }

    int getSelectPosition() {
        return mSelectedPosition;
    }

    void setSelectedPostion(int selectedPosition, boolean clickTile) {
        mSelectedPosition = selectedPosition;
        mClickTile = clickTile;
    }

    void setSelectedPostion(int selectedPosition) {
        mSelectedPosition = selectedPosition;
        mClickTile = false;
    }

    void setOnItemClickListener(View.OnClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    @Override
    public TileHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layoutResId = R.layout.wallpaper_picker_item;
        if (viewType == VIEW_TYPE_LIVE) {
            layoutResId = R.layout.wallpaper_picker_live_wallpaper_item;
        } else if (viewType == VIEW_TYPE_PICK_IMAGE) {
            layoutResId = R.layout.wallpaper_picker_pick_image_item;
        }
        View view = mLayoutInflater.inflate(layoutResId, parent, false);
        return new TileHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(final TileHolder holder, final int position) {
        WallpaperTileInfo wallpaperInfo = mWallpaperInfoList.get(position);
        holder.itemView.setTag(position);
        wallpaperInfo.setView(holder.itemView);
        holder.itemView.setOnClickListener(mOnItemClickListener);

        holder.itemView.setSelected(false);
        if (mSelectedPosition >= 0 && mSelectedPosition == position) {
            if (mClickTile) {
                mClickTile = false;
                mOnItemClickListener.onClick(holder.itemView);
            } else {
                holder.itemView.setSelected(true);
            }
        }

        if (wallpaperInfo.mThumb != null) {
            holder.tileImage.setImageDrawable(wallpaperInfo.mThumb);
            if (holder.tileIcon != null) {
                holder.tileIcon.setVisibility(View.GONE);
            }
        } else {
            holder.tileImage.setImageDrawable(null);
            if (holder.tileIcon != null) {
                holder.tileIcon.setVisibility(View.VISIBLE);
            }
        }

        if (wallpaperInfo instanceof LiveWallpaperInfo) {
            LiveWallpaperInfo wallpaper = (LiveWallpaperInfo) wallpaperInfo;
            if (wallpaper.mThumb == null) {
                holder.tileIcon.setImageDrawable(wallpaper.mInfo.loadIcon(mPackageManager));
            }
            holder.tileLabel.setText(wallpaper.mInfo.loadLabel(mPackageManager));
        }
    }

    @Override
    public int getItemCount() {
        return mWallpaperInfoList.size();
    }

    @Override
    public int getItemViewType(int position) {
        int viewType = VIEW_TYPE_DEFAULT;
        WallpaperTileInfo wallpaperTileInfo = mWallpaperInfoList.get(position);
        if (wallpaperTileInfo instanceof LiveWallpaperInfo) {
            viewType = VIEW_TYPE_LIVE;
        } else if (wallpaperTileInfo instanceof PickImageInfo) {
            viewType = VIEW_TYPE_PICK_IMAGE;
        }
        return viewType;
    }

    static class TileHolder extends RecyclerView.ViewHolder {
        ImageView tileImage;
        ImageView tileIcon;
        TextView tileLabel;

        TileHolder(View v) {
            super(v);
            tileImage = (ImageView) v.findViewById(R.id.wallpaper_image);
        }

        TileHolder(View v, int viewType) {
            this(v);
            if (viewType == VIEW_TYPE_LIVE) {
                tileIcon = (ImageView) v.findViewById(R.id.wallpaper_icon);
                tileLabel = (TextView) v.findViewById(R.id.wallpaper_label);
            }
        }
    }
}

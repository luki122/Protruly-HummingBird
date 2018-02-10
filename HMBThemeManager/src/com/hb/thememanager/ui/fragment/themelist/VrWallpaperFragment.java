package com.hb.thememanager.ui.fragment.themelist;

import hb.widget.FloatingActionButton;
import hb.widget.FloatingActionButton.OnFloatActionButtonClickListener;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Fragment;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.TextView;

import com.hb.thememanager.ThemeManagerApplication;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.VrWallpaperInfo;
import com.hb.thememanager.model.Wallpaper;
import com.hb.thememanager.ui.SetDesktopWallpaperActivity;
import com.hb.thememanager.ui.SetLockScreenWallpaperActivity;
import com.hb.thememanager.ui.adapter.AbsBaseAdapter;
import com.hb.thememanager.ui.adapter.LocalWallpaperAdapter;
import com.hb.thememanager.ui.fragment.AbsThemeFragment;
import com.hb.thememanager.ui.fragment.themedetail.LockScreenWallpaperPreviewActivity;
import com.hb.thememanager.ui.fragment.themedetail.WallpaperPreviewActivity;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.IntentUtils;
import com.hb.thememanager.utils.WallpaperUtils;
import com.hb.thememanager.R;

public class VrWallpaperFragment extends AbsThemeFragment implements OnItemClickListener {

	private GridView mWallpaperList;
	private FloatingActionButton mAddWallpaperBtn;
	private VrAdapter mAdapter;
	private View mContentView;
	private static final int REQUEST_SET_VR_WALLPAPER = 100;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mContentView = inflater.inflate(R.layout.activity_local_desktop_wallpaper_list, container, false);
		initView();
		return mContentView;
	}

	@Override
	protected void initView() {
		mWallpaperList = (GridView) mContentView.findViewById(R.id.wallpaper_list_grid);
		mAddWallpaperBtn = (FloatingActionButton) mContentView.findViewById(R.id.btn_add_wallpaper);
		
		mWallpaperList.setVerticalSpacing(getContext().getResources().getDimensionPixelSize(R.dimen.vr_wallpaper_list_item_space_h));
		mAddWallpaperBtn.setVisibility(View.GONE);
		
		mAdapter = new VrAdapter(getContext());
		mWallpaperList.setAdapter(mAdapter);
		mWallpaperList.setOnItemClickListener(this);
	}
	
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mWallpaperList != null) {
			mWallpaperList.setAdapter(null);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		  Intent preview = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
		  preview.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
				  mAdapter.getItem(position).getmComponent());
		  ((Activity) getContext()).startActivityForResult(preview, REQUEST_SET_VR_WALLPAPER);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
	}


	class VrAdapter extends BaseAdapter {
		private List<VrWallpaperInfo> mVrWallpaperInfos;
		private Context mContext;
		
		public VrAdapter(Context context) {
			mContext = context;
			mVrWallpaperInfos = WallpaperUtils.findVrWallpapers(context);
		}

		@Override
		public int getCount() {
			return mVrWallpaperInfos == null ? 0 : mVrWallpaperInfos.size();
		}

		@Override
		public VrWallpaperInfo getItem(int position) {
			return mVrWallpaperInfos.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			VrHolder holder = null;
			if(convertView == null){
				convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item_vr_wallpaper, null);
				holder = new VrHolder(getContext());
				holder.holdConvertView(convertView);
				convertView.setTag(holder);
			}else{
				holder = (VrHolder) convertView.getTag();
			}
			holder.bindDatas(position, mVrWallpaperInfos);
			return convertView;
		}
		
		class VrHolder {
			private Context mContext;
			private ImageView mVrWallpaperImg;
			private TextView mVrWallpaperTitle;

			public VrHolder(Context context) {
				mContext = context;
			}
			
			public void holdConvertView(View convertView) {
				mVrWallpaperImg = (ImageView)convertView.findViewById(R.id.vr_wallpaper_item);
				mVrWallpaperTitle = (TextView)convertView.findViewById(R.id.vr_wallpaper_title);
			}
			public void bindDatas(int position, List<VrWallpaperInfo> infos) {
				VrWallpaperInfo info = infos.get(position);
//				mImageLoader.loadImage(info.mThumb, mVrWallpaperImg);
				mVrWallpaperImg.setImageBitmap(info.getmThumb());
				mVrWallpaperTitle.setText(info.getmTitle());
			}
		}
	}
}

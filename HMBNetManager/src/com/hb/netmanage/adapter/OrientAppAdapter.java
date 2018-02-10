package com.hb.netmanage.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;

import com.hb.themeicon.theme.IconManager;
import android.os.UserHandle;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hb.netmanage.R;
import com.hb.netmanage.entity.AppItem;
import com.hb.netmanage.utils.LogUtil;
import com.hb.netmanage.utils.PreferenceUtil;

import java.util.ArrayList;
import java.util.Arrays;

import hb.app.dialog.AlertDialog.Builder;
import hb.widget.CycleImageView;
import hb.widget.recycleview.RecyclerView;
import hb.widget.recycleview.RecyclerView.ViewHolder;

/**
 * 添加定向应用
 * 
 * @author zhaolaichao
 *
 */
public class OrientAppAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	public final static int UPDATE_UI_TAG = 1000;
	/**
     * 定向应用集合
     */
    private ArrayList<AppItem> mAddedAppList = new ArrayList<AppItem>();
    /**
     * 要添加的应用集合
     */
    private ArrayList<AppItem> mAddAppList = new ArrayList<AppItem>();
    
	private Context mContext;
	private static PackageManager mPManager;
	private String mCurrectImsi;
	private Handler mHandler;
	private IconManager mIconManager;
	
	public OrientAppAdapter(Context context, Handler handler, String imsi) {
		super();
		this.mContext = context;
		this.mCurrectImsi = imsi;
		this.mHandler = handler;
		mPManager = mContext.getPackageManager();
		mIconManager = IconManager.getInstance(mContext, true, false);
	}

	/**
	 * 设置数据
	 * @param addAppList
	 */
	public void setAddAppList(ArrayList<AppItem> addAppList) {
//		mAddedAppList.clear();
//		mAddedAppList.addAll(addAppList);
		mAddedAppList = addAppList;
		LogUtil.e("mAddedAppList", "mAddedAppList>>" + mAddedAppList.size());
	}
	
    /**
     * 移除task
     */
    public void clear() {
    	if (mAddedAppList != null) {
    		mAddedAppList.clear();
    	}
    	if (mAddAppList != null) {
    		mAddAppList.clear();
    	}
    }
    
	@Override
	public int getItemCount() {
		return mAddedAppList.size();
	}

	@Override
	public void onBindViewHolder(ViewHolder viewHolder, int position) {
		AppViewHolder appHolder = (AppViewHolder) viewHolder;
		appHolder.initData(mContext, mAddedAppList, position);
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
		View view = LayoutInflater.from(mContext).inflate(R.layout.orient_app_item, viewGroup, false);
		AppViewHolder viewHolder = new AppViewHolder(view, position);
		return viewHolder;
	}
	
   class AppViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
	    RelativeLayout layItem;
    	CycleImageView civLogo;
    	TextView tvAppName;
    	Button btnRemove;
	    View lay_line;

		public AppViewHolder(View itemView, int position) {
			super(itemView);
			layItem = (RelativeLayout) itemView.findViewById(R.id.lay_item);
			civLogo = (hb.widget.CycleImageView) itemView.findViewById(R.id.imv_logo);
			tvAppName = (TextView) itemView.findViewById(R.id.tv_orient_app_name);
			btnRemove = (Button) itemView.findViewById(R.id.btn_remove);
			lay_line = itemView.findViewById(R.id.lay_line);
			if (position == 0) {
				lay_line.setVisibility(View.GONE);
			}
			civLogo.setVisibility(View.VISIBLE);
			btnRemove.setVisibility(View.VISIBLE);
		}
    	
		/**
		 * 初始化数据
		 * @param context
		 * @param addedList
		 * @param position
		 */
		public void initData(Context context, ArrayList<AppItem> addedList, int position) {
			//获取要添加的应用
			AppItem appItem = addedList.get(position);
			ResolveInfo resolveInfo = appItem.getResolveInfo();
			ApplicationInfo applicationInfo = resolveInfo.activityInfo.applicationInfo;
			String appName  = (String) applicationInfo.loadLabel(mPManager);
			layItem.setTag(position);
			//tvUseData.setText(Formatter.formatFileSize(context, appItem.getAppData()));
			//long maxData = addedList.get(0).getAppData();
			//final int percentTotal = maxData != 0 ? (int) (appItem.getAppData() * 100 / maxData) : 0;
			//pbData.setProgress(percentTotal);
			//获得应用的logo
			Drawable logo = mIconManager.getIconDrawable(resolveInfo, UserHandle.CURRENT);
			civLogo.setImageDrawable(logo);
			tvAppName.setText(appName);
			btnRemove.setTag(position);
			btnRemove.setOnClickListener(this);
			layItem.setOnClickListener(this);
		}

		@Override
		public void onClick(View v) {
			int index = 0;
			switch(v.getId()) {
			case R.id.btn_remove:
				Button btnRemove = (Button) v;
				index = (Integer) btnRemove.getTag();
				showCleanDialog(index);
				break;
			case R.id.lay_item:
				RelativeLayout layItem = (RelativeLayout) v;
				index = (Integer) layItem.getTag();
				showCleanDialog(index);
				break;
			}
		}
    }

   /**
	 * 移除提示
	 */
	private void showCleanDialog(final int position) {
		ResolveInfo resolveInfo = mAddedAppList.get(position).getResolveInfo();
		ApplicationInfo applicationInfo = resolveInfo.activityInfo.applicationInfo;
		String appName  = (String) applicationInfo.loadLabel(mPManager);
		hb.app.dialog.AlertDialog.Builder builder = new Builder(mContext);
		builder.setTitle(mContext.getString(R.string.remove_orient_app_info));
		String message = String.format(mContext.getString(R.string.remove_orient_app_content), appName);
		builder.setMessage(message);
		builder.setPositiveButton(com.hb.R.string.ok, new hb.app.dialog.AlertDialog.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//提示用户
				removeApp(position);
			}
		});
		builder.setNegativeButton(com.hb.R.string.cancel, new hb.app.dialog.AlertDialog.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
			
		});
		builder.create().show();
	}
	
    /**
	 * 移除应用
	 * @param position
	 */
	public void removeApp(int position) {
		String addedAppUids = PreferenceUtil.getString(mContext, mCurrectImsi, PreferenceUtil.ORIENT_APP_ADDED_KEY, "");
		if (TextUtils.isEmpty(addedAppUids)){
			return;
		}
		if (addedAppUids.contains(",")) {
			String[] uidsArray = addedAppUids.split(",");
			ResolveInfo resolveInfo = mAddedAppList.get(position).getResolveInfo();
			int removeUid = resolveInfo.activityInfo.applicationInfo.uid;
			ArrayList<String> uidList = new ArrayList<String>( Arrays.asList(uidsArray));
			if(uidList.contains("" + removeUid)) {
				uidList.remove("" + removeUid);
			}
			StringBuffer addedBf = new StringBuffer();
			for (int i = 0; i < uidList.size(); i++) {
				addedBf.append(uidList.get(i)).append(",");
			}
			//更新增加定向应用的UID
			PreferenceUtil.putString(mContext, mCurrectImsi, PreferenceUtil.ORIENT_APP_ADDED_KEY, addedBf.toString());
		}
		mAddedAppList.remove(position);
		notifyItemRemoved(position);
		notifyItemRangeChanged(position, mAddedAppList.size() - position);
		if(mAddedAppList.size() == 0) {
			mHandler.sendEmptyMessage(UPDATE_UI_TAG);
		}
	}
}

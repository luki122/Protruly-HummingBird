package com.hb.netmanage.adapter;

import java.util.ArrayList;

import com.hb.netmanage.R;
import com.hb.netmanage.entity.AppItem;
import com.hb.netmanage.utils.PreferenceUtil;

import android.content.Context;

import com.hb.themeicon.theme.IconManager;
import android.os.UserHandle;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import com.hb.netmanage.utils.LogUtil;

import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import hb.widget.CycleImageView;
import hb.widget.recycleview.RecyclerView;

/**
 * 添加定向应用
 * 
 * @author zhaolaichao
 *
 */
public class AddOrientAppAdapter  extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
	public static final int SAVE_TAG = 1000;
	private Handler mHandler;
	/**
	 * 要添加的应用集合
	 */
    private ArrayList<AppItem> mAppList = new ArrayList<AppItem>();
    private ArrayList<AppItem> mAddAppList = new ArrayList<AppItem>();
    private Context mContext;
    private PackageManager mPManager;
    private String mCurrectImsi;
	private IconManager mIconManager;
    
	public AddOrientAppAdapter(Context context, String imsi, Handler handler) {
		super();
		this.mContext = context;
		mCurrectImsi = imsi;
		mHandler = handler;
		mPManager = mContext.getPackageManager();
		mIconManager = IconManager.getInstance(mContext, true, false);
	}
	
	public void setAppList(ArrayList<AppItem> appList) {
		mAppList.clear();
		this.mAppList.addAll(appList);
	}

	/**
	 * 获得要添加的集合
	 * @return
     */
	public ArrayList<AppItem> getmAppList() {
		return mAppList;
	}

	public ArrayList<AppItem> getAddAppList() {
		return mAddAppList;
	}

	public void setmAddAppList(ArrayList<AppItem> mAddAppList) {
		this.mAddAppList = mAddAppList;
	}

	/**
	 * 清除缓存
	 */
	public void clean() {
		if (null != mAddAppList) {
			mAddAppList.clear();
		}
	}
	@Override
	public int getItemCount() {
		return mAppList.size();
	}

	@Override
	public void onBindViewHolder(hb.widget.recycleview.RecyclerView.ViewHolder viewHolder, int position) {
		 AppViewHolder appHolder = (AppViewHolder) viewHolder;
		 appHolder.initData(position);
	}

	@Override
	public hb.widget.recycleview.RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int position) {
		 View view = LayoutInflater.from(mContext).inflate(com.hb.R.layout.list_item_1_line_with_icon_large_multiple_choice, parent, false);
		 AppViewHolder appHolder = new AppViewHolder(view);
		return appHolder;
	}
	
	class AppViewHolder extends RecyclerView.ViewHolder implements CompoundButton.OnCheckedChangeListener{
		RelativeLayout layItem;
    	CycleImageView civLogo;
    	TextView tvAppName;
		View cbAdd;
		View lay_line;
    	
		public AppViewHolder(View itemView) {
			super(itemView);
			//layItem = (RelativeLayout) itemView.findViewById(R.id.lay_item);
			civLogo = (hb.widget.CycleImageView) itemView.findViewById(android.R.id.icon);
			tvAppName = (TextView) itemView.findViewById(android.R.id.text1);
			cbAdd = itemView.findViewById(android.R.id.button1);
			tvAppName.setVisibility(View.VISIBLE);
			civLogo.setVisibility(View.VISIBLE);
			cbAdd.setVisibility(View.VISIBLE);
//			layItem.setOnClickListener(this);
//			cbAdd.setOnCheckedChangeListener(this);
		}
    	
		/**
		 * 初始化数据
		 * @param context
		 * @param addList
		 * @param position
		 */
		private void initData(int position) {
			AppItem appItem = mAppList.get(position);
			//获取要添加的应用
			ResolveInfo resolveInfo = appItem.getResolveInfo();
			String appName  = (String) resolveInfo.activityInfo.applicationInfo.loadLabel(mPManager);
			//获得应用的logo
			Drawable logo = mIconManager.getIconDrawable(resolveInfo, UserHandle.CURRENT);
			civLogo.setImageDrawable(logo);
			cbAdd.setTag(position);
			tvAppName.setText(appName);
			if(cbAdd != null){
				if(cbAdd instanceof CompoundButton){
					final CompoundButton checkbox = (CompoundButton) cbAdd;
					checkbox.setOnCheckedChangeListener(null);
					if (TextUtils.equals(appItem.getTag(), mContext.getString(R.string.all_select))) {
						checkbox.setChecked(true);
						LogUtil.v("setChecked", "setChecked>>" + checkbox.isChecked());
					} else {
						checkbox.setChecked(false);
					}
					checkbox.setOnCheckedChangeListener(this);
					itemView.setOnClickListener(new View.OnClickListener(){
						@Override
						public void onClick(View v){
							checkbox.setChecked(!checkbox.isChecked());
						}
					});
				}
			}
		}

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			CheckBox ckAdd = (CheckBox) buttonView;
			int position = (int)ckAdd.getTag();
			AppItem appItem = mAppList.get(position);
			if (ckAdd.isChecked()) {
				appItem.setTag(mContext.getString(R.string.all_select));
				mAddAppList.add(appItem);
			} else {
				appItem.setTag(mContext.getString(R.string.all_unselect));
				mAddAppList.remove(appItem);
			}
			LogUtil.v("mAddAppList", "mAddAppList>>" + mAddAppList.size());
			mHandler.sendEmptyMessage(SAVE_TAG);
		}
	}
	
	/**
	 * 添加应用
	 * @param addAppList
     */
     public void saveOrientAdd(ArrayList<AppItem> addAppList) {
		 if (addAppList.size() == 0) {
			 return;
		 }
		 //取出已添加过的UID
		 StringBuffer addBf = new StringBuffer();
		 String addedAppUids = PreferenceUtil.getString(mContext, mCurrectImsi, PreferenceUtil.ORIENT_APP_ADDED_KEY, "");
		 addBf.append(addedAppUids);
		 //添加新的UID
		 for (int i = 0; i < addAppList.size(); i++) {
			 addBf.append(addAppList.get(i).getResolveInfo().activityInfo.applicationInfo.uid).append(",");
		 }
		 PreferenceUtil.putString(mContext, mCurrectImsi, PreferenceUtil.ORIENT_APP_ADDED_KEY, addBf.toString());
		 LogUtil.v("addApp", "addBf>>" + addBf.toString());

	 }
}

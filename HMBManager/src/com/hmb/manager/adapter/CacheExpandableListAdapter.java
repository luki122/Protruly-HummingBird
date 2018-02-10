package com.hmb.manager.adapter;

import java.util.List;
import java.util.Map;

import com.hmb.manager.R;
import com.hmb.manager.bean.AppInfo;
import com.hmb.manager.bean.RubblishInfo;
import com.hmb.manager.utils.ManagerUtils;
import com.hmb.manager.utils.TransUtils;

import android.content.Context;
import android.content.pm.PackageStats;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import hb.widget.ActionMode;
public class CacheExpandableListAdapter extends BaseExpandableListAdapter {

	private int child_groupId = -1;
	private int child_childId = -1;
	private Context mContext = null;
	private boolean[] group_checked = new boolean[] {};
	private String[][] child_text_array = new String[][] {};

	private String[][] child_text_size = new String[][] {};

	private String[] group_title_arry = new String[] {};

	private boolean[][] child_checkbox = new boolean[][] {};

	private long cleanSize = 0;
	private Button cacheCleanBtn;
	private ActionMode actionModeView;
	public List<RubblishInfo> mlistAppInfo;
	LayoutInflater infater = null;

	public CacheExpandableListAdapter(Context context) {
		this.mContext = context;
	}

	@Override
	public int getGroupCount() {
		return mlistAppInfo.size();
	}

	@Override
	public Object getGroup(int groupPosition) {
		return group_title_arry[groupPosition];
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		if (child_text_array[groupPosition] != null)
			return child_text_array[groupPosition].length;
		return 0;
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return child_text_array[groupPosition][childPosition];
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	public List<RubblishInfo> getMlistAppInfo() {
		return mlistAppInfo;
	}

	public void setMlistAppInfo(List<RubblishInfo> mlistAppInfo) {
		this.mlistAppInfo = mlistAppInfo;
	}


	public CacheExpandableListAdapter(Context context, List<RubblishInfo> list) {
		mlistAppInfo = list;
		mContext = context;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		AppInfo appInfo = null;
		// if (convertView == null) {
		holder = new ViewHolder();
		convertView = LayoutInflater.from(mContext).inflate(R.layout.listview_app, null);

		holder.app_icon = (ImageView) convertView.findViewById(R.id.app_icon);
		holder.app_name = (TextView) convertView.findViewById(R.id.app_name);
		holder.app_size = (TextView) convertView.findViewById(R.id.app_size);
		holder.checkBox = (CheckBox) convertView.findViewById(R.id.app_check_box);
		convertView.setTag(holder);
		// } else {
		// holder = (ViewHolder) convertView.getTag();
		// }
		RubblishInfo info = mlistAppInfo.get(groupPosition);
		if (info != null) {
			appInfo = ManagerUtils.getAppInfoByPackageName(mContext, info.getmPackageName());
			if (appInfo != null && appInfo.getAppIcon() != null) {
				holder.app_icon.setBackground(appInfo.getAppIcon());
				holder.app_name.setText(appInfo.getAppLabel());
				holder.app_size.setText(TransUtils.transformShortType(info.getmSize(), true));
				holder.checkBox.setChecked(group_checked[groupPosition]);
			}
		}
		if (group_checked[groupPosition]) {
			holder.checkBox.setChecked(true);
		} else {
			holder.checkBox.setChecked(false);
		}
		holder.app_size.setText(
				TransUtils.transformShortType(handlerGroupSize(groupPosition, child_text_size, child_checkbox), true));
		cleanBtnHandler();
		final int positiom = groupPosition;
		groupChildHandler(group_checked,child_checkbox);
		holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					group_checked[positiom] = true;
					checkBoxHandler(child_checkbox[positiom], true);
				} else {
					group_checked[positiom] = false;
					checkBoxHandler(child_checkbox[positiom], false);
				}
				actionModeHandler();
			}

		});
		return convertView;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
			ViewGroup parent) {

		convertView = (RelativeLayout) RelativeLayout.inflate(mContext, R.layout.child_layout, null);

		TextView child_text = (TextView) convertView.findViewById(R.id.child_text);

		TextView child_size = (TextView) convertView.findViewById(R.id.child_size);

		CheckBox child_checkboxView = (CheckBox) convertView.findViewById(R.id.child_checkbox);

		ImageView child_iconView = (ImageView) convertView.findViewById(R.id.child_icon);

		child_text.setText(child_text_array[groupPosition][childPosition]);
		if (child_text_size[groupPosition][childPosition] != null) {
			child_size.setText(child_text_size[groupPosition][childPosition]);
		}
		if (child_checkbox[groupPosition][childPosition]) {
			child_checkboxView.setChecked(true);
		} else {
			child_checkboxView.setChecked(false);
		}

		final int gPosition = groupPosition;
		final int cPosition = childPosition;
		child_checkboxView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					child_checkbox[gPosition][cPosition] = true;
					if(cPosition==1){
						child_checkbox[gPosition][cPosition-1] = true;
					}
				} else {
					child_checkbox[gPosition][cPosition] = false;
					group_checked[gPosition] = false;
				}
				cleanBtnHandler();
				notifyDataSetChanged();
				actionModeHandler();
			}

		});

		return convertView;
	}
	
   private void actionModeHandler(){
	   Log.d("CacheCleanActivity", "----actionModeHandler-----"+ManagerUtils.selectAll(child_checkbox));
		   if(ManagerUtils.selectAll(child_checkbox)==1){
			   actionModeView.setPositiveText(mContext.getString(R.string.un_select_all));
		   }else if(ManagerUtils.selectAll(child_checkbox)==0){
			   actionModeView.setPositiveText(mContext.getString(R.string.select_all));
		   }
   }

	private long handlerGroupSize(int groupPosition, String[][] child_text_size, boolean[][] child_checkbox) {
		long mSize = 0;
		String childText = null;
		if (groupPosition >= 0 && child_text_size[groupPosition] != null && child_checkbox[groupPosition] != null) {
			if (child_text_size[groupPosition].length > 0 && child_checkbox[groupPosition].length > 0) {
				for (int i = 0; i < child_text_size[groupPosition].length; ++i) {
					if (child_checkbox[groupPosition][i]) {
						childText = child_text_size[groupPosition][i];
						if (childText != null && childText.trim().length() > 0) {
							mSize = mSize + TransUtils.unTransformShortType(childText.trim().trim());
						}
					}
				}
			}
		}
		return mSize;
	}

	private void checkBoxHandler(boolean[] checkBoxList, boolean flag) {
		if (checkBoxList.length > 0) {
			for (int i = 0; i < checkBoxList.length; ++i) {
				checkBoxList[i] = flag;
			}
		}
		cleanBtnHandler();
		notifyDataSetChanged();
	}

	private void groupChildHandler(boolean[] groupBoxList, boolean[][] child_checkbox) {
		boolean selectAll = false;
		if(groupBoxList!=null&&groupBoxList.length>0){
		for (int i = 0; i < groupBoxList.length; ++i) {
			if (child_checkbox[i] != null && child_checkbox[i].length > 0) {
				for (int j = 0; j < child_checkbox[i].length; ++j) {
					if (!child_checkbox[i][j]){
						groupBoxList[i] = false;
						break;
					}
					groupBoxList[i] = true;
				}
			}

		}
		}
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

	public boolean[] getGroup_checked() {
		return group_checked;
	}

	public void setGroup_checked(boolean[] group_checked) {
		this.group_checked = group_checked;
	}

	public int getChild_groupId() {
		return child_groupId;
	}

	public void setChild_groupId(int child_groupId) {
		this.child_groupId = child_groupId;
	}

	public int getChild_childId() {
		return child_childId;
	}

	public void setChild_childId(int child_childId) {
		this.child_childId = child_childId;
	}

	public String[][] getChild_text_array() {
		return child_text_array;
	}

	public void setChild_text_array(String[][] child_text_array) {
		this.child_text_array = child_text_array;
	}

	public String[][] getChild_text_size() {
		return child_text_size;
	}

	public void setChild_text_size(String[][] child_text_size) {
		this.child_text_size = child_text_size;
	}

	public boolean[][] getChild_checkbox() {
		return child_checkbox;
	}

	public void setChild_checkbox(boolean[][] child_checkbox) {
		this.child_checkbox = child_checkbox;
	}

	public long getCountCleanSize() {
		cleanSize = 0;
		if (mlistAppInfo != null && mlistAppInfo.size() > 0) {
			for (int i = 0; i < mlistAppInfo.size(); ++i) {
				cleanSize = cleanSize + handlerGroupSize(i, child_text_size, child_checkbox);
			}
		}
		return cleanSize;
	}

	public long getCleanSize() {
		return cleanSize;
	}
	
	public void setCleanSize(long cleanSize) {
		this.cleanSize = cleanSize;
	}

	public Button getCacheCleanBtn() {
		return cacheCleanBtn;
	}

	public void setCacheCleanBtn(Button cacheCleanBtn) {
		this.cacheCleanBtn = cacheCleanBtn;
	}

	public String[] getGroup_title_arry() {
		return group_title_arry;
	}

	public void setGroup_title_arry(String[] group_title_arry) {
		this.group_title_arry = group_title_arry;
	}

	private void cleanBtnHandler() {
		long size = getCountCleanSize();
		if (size > 0) {
			cacheCleanBtn.setEnabled(true);
			cacheCleanBtn.setText(
					mContext.getString(R.string.clean_btn_size, TransUtils.transformShortType(size, true)));
		} else {
			cacheCleanBtn.setEnabled(false);
			cacheCleanBtn.setText(R.string.clean_btn_message);
		}
	}

	public final class ViewHolder {
		public ImageView app_icon;
		public TextView app_name;
		public TextView app_size;
		public CheckBox checkBox;
	}

	public ActionMode getActionModeView() {
		return actionModeView;
	}

	public void setActionModeView(ActionMode actionModeView) {
		this.actionModeView = actionModeView;
	}

}

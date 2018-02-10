package com.hmb.manager.adapter;

import com.hmb.manager.R;
import com.hmb.manager.utils.TransUtils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
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

public class RubblishExpandableListAdapter extends BaseExpandableListAdapter {

	private boolean[] group_checked = new boolean[] { false, false, false, false };
	private int child_groupId = -1;
	private int child_childId = -1;
	private Context mContext = null;

	private int[] group_title_arry = new int[] { R.string.rubblish_clean_title, R.string.uninstall_file_title,
			R.string.unuseful_apk, R.string.uninstalled_apk };

	private String[][] child_text_array = new String[4][];

	private String[][] child_text_size = new String[4][];

	private boolean[][] child_checkbox = new boolean[4][];

	private Drawable[][] child_icon = new Drawable[4][];
	private long cleanSize = 0;
    private Button rubblishCleanBtn;
	public RubblishExpandableListAdapter(Context context) {
		this.mContext = context;
	}

	@Override
	public int getGroupCount() {
		return group_title_arry.length;
	}

	@Override
	public Object getGroup(int groupPosition) {
		return getResourcesString(group_title_arry[groupPosition]);
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

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		convertView = (LinearLayout) LinearLayout.inflate(mContext, R.layout.clean_group_layout, null);

		RelativeLayout myLayout = (RelativeLayout) convertView.findViewById(R.id.clean_group_layout);

		TextView group_title = (TextView) convertView.findViewById(R.id.clean_group_title);
		TextView group_title_child = (TextView) convertView.findViewById(R.id.clean_group_title_child);
		CheckBox group_state = (CheckBox) convertView.findViewById(R.id.clean_group_state);

		TextView group_sizeView = (TextView) convertView.findViewById(R.id.clean_group_size);

		ImageView clean_point = (ImageView) convertView.findViewById(R.id.clean_point);

		group_title.setText(getResourcesString(group_title_arry[groupPosition]));

		group_sizeView.setText(
				TransUtils.transformShortType(handlerGroupSize(groupPosition, child_text_size, child_checkbox), true));
		cleanBtnHandler();
		if (groupPosition == 0 || groupPosition == 1) {
			group_title_child.setText(R.string.clean_title_child);
		} else if (groupPosition == 2) {
			group_title_child.setText(R.string.unuseful_apk_title_child);
		} else if (groupPosition == 3) {
			group_title_child.setText(R.string.uninstall_apk_title_child);
		}
		final int positiom = groupPosition;
		if (isExpanded) {
			clean_point.setBackgroundResource(R.drawable.down_arrow);
		} else {
			clean_point.setBackgroundResource(R.drawable.right_arrow);
		}
		if (group_checked[groupPosition]) {
			group_state.setChecked(true);
		} else {
			group_state.setChecked(false);
		}
		groupChildHandler(group_checked,child_checkbox);
		group_state.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					group_checked[positiom] = true;
					checkBoxHandler(child_checkbox[positiom], true);
				} else {
					group_checked[positiom] = false;
					checkBoxHandler(child_checkbox[positiom], false);
				}
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
		if (groupPosition == 2 || groupPosition == 3) {

			child_iconView.setBackground(child_icon[groupPosition][childPosition]);
		}
		final int gPosition = groupPosition;
		final int cPosition = childPosition;
		child_checkboxView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					child_checkbox[gPosition][cPosition] = true;
				} else {
					child_checkbox[gPosition][cPosition] = false;
					group_checked[gPosition] = false;
				}
				cleanBtnHandler();
				notifyDataSetChanged();
			}

		});

		return convertView;
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
		if (checkBoxList!=null&&checkBoxList.length > 0) {
			for (int i = 0; i < checkBoxList.length; ++i) {
				checkBoxList[i] = flag;
			}
		}
		cleanBtnHandler();
		notifyDataSetChanged();
	}

	private void groupChildHandler(boolean[] groupBoxList, boolean[][] child_checkbox) {
		boolean selectAll = false;
		for (int i = 0; i < 4; ++i) {
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

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

	private String getResourcesString(int id) {

		return mContext.getResources().getString(id);
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

	public Drawable[][] getChild_icon() {
		return child_icon;
	}

	public void setChild_icon(Drawable[][] child_icon) {
		this.child_icon = child_icon;
	}

	public long getCleanSize() {
		cleanSize=0;
		for (int i = 0; i < 4; ++i) {
			cleanSize = cleanSize + handlerGroupSize(i, child_text_size, child_checkbox);
		}
		return cleanSize;
	}

	public void setCleanSize(long cleanSize) {
		this.cleanSize = cleanSize;
	}

	public Button getRubblishCleanBtn() {
		return rubblishCleanBtn;
	}

	public void setRubblishCleanBtn(Button rubblishCleanBtn) {
		this.rubblishCleanBtn = rubblishCleanBtn;
	}

	private void cleanBtnHandler(){
		long size=getCleanSize();
		if(size>0){
			rubblishCleanBtn.setEnabled(true);
			rubblishCleanBtn.setText(mContext.getString(R.string.clean_btn_size,TransUtils.transformShortType(getCleanSize(), true)));
		}else {
			rubblishCleanBtn.setEnabled(false);
			rubblishCleanBtn.setText(R.string.clean_btn_message);
		}
	}
	
}

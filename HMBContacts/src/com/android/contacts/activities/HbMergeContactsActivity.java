//add by liyang 2017-4-7


package com.android.contacts.activities;

import hb.widget.ActionMode;
import hb.widget.ActionModeListener;
import hb.widget.ActionMode.Item;
import com.android.contacts.common.activity.TransactionSafeActivity;
import hb.widget.toolbar.Toolbar;
import hb.view.menu.bottomnavigation.BottomNavigationView;
import hb.view.menu.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener;
import hb.app.dialog.ProgressDialog;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.TreeSet;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import com.android.contacts.ContactSaveService;
import com.android.contacts.R;



public class HbMergeContactsActivity extends TransactionSafeActivity {

	protected static final String TAG = "HbMergeContactsActivity";


	private ListView listview;
	private MyAdapter mAdapter;

	private int checkCount;
	private BottomNavigationView bottomBar;

	private boolean[] checkeds;
	private boolean is_all_checked = false;
	private int count;
	public void createCheckedArray(int count) {
		this.count=count;
		if (checkeds == null || count != checkeds.length) checkeds = new boolean[count];
		for (int i = 0; i < count; i++)
			checkeds[i] = true;
	}


	public void setCheckedArrayValue(int position, boolean value) {
		Log.d(TAG,"setCheckedArrayValue,po:"+position+" value:"+value);
		checkeds[position] = value;
		sortedList.get(position).setChecked(value);
	}

	public void setAllSelect(boolean isAllselect){
		for(int i=0;i<checkeds.length;i++){
			checkeds[i]=isAllselect;
			sortedList.get(i).setChecked(isAllselect);
		}
	}

	public void updateActionMode(){
		if(actionMode==null) return;
		boolean isAllChecked=isAllSelect();
		if(isAllChecked){
			actionMode.setPositiveText(getString(R.string.hb_actionmode_selectnone));
		}else{
			actionMode.setPositiveText(getString(R.string.hb_actionmode_selectall));
		}

		if(checkCount==0){
			actionMode.setTitle(getString(R.string.hb_select_contacts));	
		}else{
			actionMode.setTitle(String.format(
					getString(R.string.hb_menu_actionbar_selected_items),
					checkCount));
		}
	}

	public void updateSelectedItemsView() {
		if(checkeds==null) {
			actionMode.setPositiveText(getString(R.string.hb_actionmode_selectall));
			actionMode.enableItem(ActionMode.POSITIVE_BUTTON,false);
			actionMode.setTitle(getString(R.string.hb_select_contacts));
			return;
		}
		checkCount = getCheckedCount();
		Log.d(TAG,"checkCount:"+checkCount);
		if(bottomBar!=null){
			if(checkCount>0){
				if(bottomBar.getVisibility()!=View.VISIBLE){
					bottomBar.setVisibility(View.VISIBLE);
				}
			}else{
				if(bottomBar.getVisibility()!=View.GONE){
					bottomBar.setVisibility(View.GONE);
				}
			}
		}

		updateActionMode();
	}

	public boolean getCheckedArrayValue(int position) {
		return checkeds[position];
	}

	public boolean isAllSelect() {
		for (int i = 0; i < checkeds.length; i++) {
			if (!checkeds[i])
				return false;
		}
		return true;
	}

	public int getCheckedCount() {
		int mChecked = 0;
		for (int i = 0; i < checkeds.length; i++) {
			if (checkeds[i]) {
				mChecked++;
			}
		}
		return mChecked;
	}

	public void clearAllcheckes() {
		for (int i = 0; i < checkeds.length; i++)
			checkeds[i] = false;
	}


	private Toolbar toolbar;
	protected TextView mEmptyView = null;
	protected View emptyLayout;
	private ActionMode actionMode;
	private Handler mHandler = new Handler();
	private Runnable mShowActionModeRunnable = new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			showActionMode(true);
		}
	};
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		Log.d(TAG,"onCreate");
		setHbContentView(R.layout.hb_mergecontacts_activity_layout);

		toolbar = getToolbar();
//		toolbar.setTitle(getString(R.string.hb_merge_contacts));

		toolbar.setNavigationOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				finish();
			}
		});

		listview=(ListView) findViewById(R.id.mlistview);

		Log.d(TAG,"listview:"+listview);

		listview.setOnItemClickListener(new OnItemClickListener(){

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {

				Log.d(TAG,"arg0:"+arg0+" arg1:"+arg1+" arg2:"+arg2+" arg3:"+arg3);
				CheckBox checkBox=(CheckBox)arg1.findViewById(android.R.id.button1);
				//				Log.d(TAG,"checkBox:"+checkBox);
				checkBox.toggle();
				//				setCheckedArrayValue(arg2, checkBox.isChecked());
				sortedList.get(arg2).setChecked(checkBox.isChecked());
				setCheckedArrayValue(arg2, checkBox.isChecked());
				updateSelectedItemsView();
			}});

		mEmptyView = (TextView) findViewById(R.id.contact_list_empty);
		emptyLayout=findViewById(R.id.empty_layout);
		if (mEmptyView != null) {
			mEmptyView.setText(getString(R.string.hb_merge_not_found));
		}

		actionMode=getActionMode();
		actionMode.setNagativeText(getString(R.string.hb_cancel));
		actionMode.setPositiveText(getString(R.string.hb_actionmode_selectnone));
		actionMode.bindActionModeListener(new ActionModeListener(){
			/**
			 * ActionMode上面的操作按钮点击时触发，在这个回调中，默认提供两个ID使用，
			 * 确定按钮的ID是ActionMode.POSITIVE_BUTTON,取消按钮的ID是ActionMode.NAGATIVE_BUTTON
			 * @param view
			 */
			public void onActionItemClicked(Item item){
				Log.d(TAG,"onActionItemClicked,itemid:"+item.getItemId());
				switch (item.getItemId()) {
				case ActionMode.POSITIVE_BUTTON:	{
					is_all_checked = isAllSelect();
					Log.d(TAG,"is_all_checked:"+is_all_checked);
					setAllSelect(!is_all_checked);
					updateSelectedItemsView();
					mAdapter.notifyDataSetChanged();
					break;
				}

				case ActionMode.NAGATIVE_BUTTON:
					finish();
					break;
				default:
					break;
				}
			}

			/**
			 * ActionMode显示的时候触发
			 * @param actionMode
			 */
			public void onActionModeShow(ActionMode actionMode){

			}

			/**
			 * ActionMode消失的时候触发
			 * @param actionMode
			 */
			public void onActionModeDismiss(ActionMode actionMode){

			}
		});		
		mHandler.postDelayed(mShowActionModeRunnable,300);

		bottomBar = (BottomNavigationView)findViewById(R.id.bottom_navigation_view);
		bottomBar.setNavigationItemSelectedListener(new OnNavigationItemSelectedListener() {

			@Override
			public boolean onNavigationItemSelected(MenuItem arg0) {
				// TODO Auto-generated method stub
				Log.d(TAG,"onNavigationItemSelected,arg0.getItemId():"+arg0.getItemId());
				switch (arg0.getItemId()) {
				case R.id.hb_merge:
					new MergeTask().execute();
					break;

				default:
					break;
				} 
				return false;
			}
		});


		IntentFilter intentFilter=new IntentFilter("com.android.contacts.activities.HbMergeContactsActivity");
		registerReceiver(broadcastReceiver, intentFilter);


		mAdapter=new MyAdapter();
		listview.setAdapter(mAdapter);
		mTask =new QueryMergeTask();
		mTask.execute();

	}


	private BroadcastReceiver broadcastReceiver=new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			Log.d(TAG,"onreceive,intent:"+intent);
			if(intent.getAction().equals("com.android.contacts.activities.HbMergeContactsActivity")){
				if(mProgressDialog!=null) mProgressDialog.dismiss();				
				Toast.makeText(HbMergeContactsActivity.this, getString(R.string.hb_merge_finished), Toast.LENGTH_LONG).show();
				releaseWakeLock();
				HbMergeContactsActivity.this.finish();
			}
		}

	};


	private class MyAdapter extends BaseAdapter{

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return sortedList.size();
		}

		@Override
		public ContactEntity getItem(int position) {
			// TODO Auto-generated method stub
			return sortedList.get(position);
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}
		protected ViewGroup newOutView(){
			return (ViewGroup)LayoutInflater.from(HbMergeContactsActivity.this).inflate(R.layout.hb_contacts_listview_item,null);
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			ViewGroup view = null;
			ViewHolder viewHolder;
			if (convertView == null) {
				view =newOutView();
				view.removeViewAt(0);
				View inner =LayoutInflater.from(HbMergeContactsActivity.this).inflate(com.hb.R.layout.list_item_2_line_multiple_choice,null);

				view.addView(inner,0,new LayoutParams(LayoutParams.MATCH_PARENT,
						LayoutParams.WRAP_CONTENT));				

				viewHolder = new ViewHolder();
				viewHolder.name=(TextView)inner.findViewById(android.R.id.text1);
				viewHolder.name.setSingleLine(true);
				viewHolder.name.setEllipsize(TextUtils.TruncateAt.valueOf("END"));
				viewHolder.number=(TextView)inner.findViewById(android.R.id.text2);
				viewHolder.number.setSingleLine(true);
				viewHolder.number.setEllipsize(TextUtils.TruncateAt.valueOf("END"));
				viewHolder.checkBox=(CheckBox) inner.findViewById(android.R.id.button1);
				viewHolder.devider=view.findViewById(R.id.devider);
				view.setTag(viewHolder);
			}else{
				view = (ViewGroup) convertView;
				viewHolder = (ViewHolder) view.getTag();
			}

			ContactEntity contactEntity=sortedList.get(position);
			String name=contactEntity.getName();
			String nums=contactEntity==null?"":(contactEntity.getNums()==null?"":contactEntity.getNums().toString());
			viewHolder.name.setText(name);
			viewHolder.number.setText(nums);
			viewHolder.checkBox.setChecked(sortedList.get(position).isChecked);

			if(position<sortedList.size()-1){
				String nextName=sortedList.get(position+1).getName();
				if(!TextUtils.equals(name, nextName)){
					viewHolder.devider.setVisibility(View.VISIBLE);
				}else{
					viewHolder.devider.setVisibility(View.GONE);
				}
			}else{
				viewHolder.devider.setVisibility(View.GONE);
			}
			return view;
		}
	}

	public static class ViewHolder {
		//		public QuickContactBadge quickContactBadge;
		public TextView name;
		public View devider;
		public TextView number;
		public CheckBox checkBox;
	}

	private ProgressDialog mProgressDialog;
	private void prepareProgressDialogSpinner(String title, String message) {
		if(mProgressDialog!=null) {
			mProgressDialog.dismiss();
		}

		mProgressDialog = new ProgressDialog(this);
		if(title!=null) mProgressDialog.setTitle(title);
		mProgressDialog.setMessage(message);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mProgressDialog.setCanceledOnTouchOutside(false);

		mProgressDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {

			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				Log.d(TAG, "onKey:"+keyCode);
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					return true;
				}
				return false;
			}
		});
		mProgressDialog.show();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unregisterReceiver(broadcastReceiver);
	}

	private QueryMergeTask mTask;
	private static final String[] PROJECTIONS=new String[]{
			"_id",
			"display_name"
	};
	private static final String[] PROJECTIONS2=new String[]{
			"_id",
			"contact_id",
			"data1"
	};
	public class ContactEntity{
		private long _id;
		private String name;
		private boolean isValid=false;
		private ArrayList<String> nums;
		private boolean isChecked=true;
		public ContactEntity(long _id,String name){
			this._id=_id;
			this.name=name;
		}
		public long get_id() {
			return _id;
		}
		public void set_id(long _id) {
			this._id = _id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public ArrayList<String> getNums() {
			return nums;
		}
		public void setNums(ArrayList<String> nums) {
			this.nums = nums;
		}

		public boolean isValid() {
			return isValid;
		}
		public void setValid(boolean isValid) {
			this.isValid = isValid;
		}

		public boolean isChecked() {
			return isChecked;
		}
		public void setChecked(boolean isChecked) {
			this.isChecked = isChecked;
		}
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return "_id:"+_id+" name:"+name+" nums:"+nums+" isChecked:"+isChecked;
		}


	}

	
	WakeLock wakeLock = null;
	private void acquireWakeLock(){
		if (null == wakeLock)  
		{  
			Log.d(TAG,"acquireWakeLock");
			final PowerManager powerManager = (PowerManager) getApplicationContext()
					.getSystemService(Context.POWER_SERVICE);
			wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
					| PowerManager.ON_AFTER_RELEASE, TAG);
			if (null != wakeLock)  wakeLock.acquire();  
		}
	}

	//释放设备电源锁
	private void releaseWakeLock()  {
		if (null != wakeLock)  
		{  
			Log.d(TAG,"releaseWakeLock");
			wakeLock.release();  
			wakeLock = null;  
		}  
	}
	
	@Override
	protected void onStop() {
		Log.d(TAG,"onStop");
		super.onStop();
		releaseWakeLock();
	}
	private class MergeTask extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... params) {
			// TODO Auto-generated method stub
			Log.d(TAG,"sortedList:"+sortedList);
			int count=sortedList.size();
			if(count==0) return false;

			ArrayList<ArrayList> list=new ArrayList<>(); 
			ArrayList<Long> tempList=new ArrayList<>(); 
			for(int i=0;i<count;i++){
				ContactEntity contactEntity=sortedList.get(i);
				if(contactEntity.isChecked) tempList.add(contactEntity.get_id());
				else {
					if(i==count-1) list.add(tempList);
					else{
						if(!TextUtils.equals(contactEntity.getName().toUpperCase().trim(), sortedList.get(i+1).getName().toUpperCase().trim())){
							list.add(tempList);
							tempList=new ArrayList<>();
						}
					}
					continue;
				}
				if(i<count-1){
					if(!TextUtils.equals(contactEntity.getName().toUpperCase().trim(), sortedList.get(i+1).getName().toUpperCase().trim())){
						list.add(tempList);
						tempList=new ArrayList<>();
					}
				}else{
					list.add(tempList);
				}
			}


			Log.d(TAG,"MergeTask list1:"+list);

			if(list.size()==0) return false;

			ArrayList<long[]> terminalList=new ArrayList<>();
			for(ArrayList<Long> mList:list){
				if(mList.size()<2) continue;
				long[] contactIds=new long[mList.size()];
				int i=0;
				for(Long id:mList){
					contactIds[i++]=id;
				}
				terminalList.add(contactIds);
			}
			Log.d(TAG,"terminalList:"+terminalList);
			int terminalCount=terminalList.size();
			if(terminalCount==0) return false;			
			ContactSaveService.shoudJoinCount=terminalCount;

			//开始逐个合并
			for(long[] contactIds:terminalList){
				Log.d(TAG,"contactIds:"+Arrays.toString(contactIds));				
				final Intent intent = ContactSaveService.createJoinSeveralContactsIntent(getApplicationContext(),
						contactIds);
				HbMergeContactsActivity.this.startService(intent);
			}

			tempList=null;
			list=null;
			sortedList=null;
			terminalList=null;
			return true;
		}

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated metExceptionhod stub
			super.onPreExecute();
			acquireWakeLock();
			prepareProgressDialogSpinner(null,getString(R.string.hb_merging));
		}

		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);			
			if(!result) {
				if(mProgressDialog!=null) mProgressDialog.dismiss();
				releaseWakeLock();
				Toast.makeText(HbMergeContactsActivity.this, getString(R.string.hb_merge_failed), Toast.LENGTH_LONG).show();
			}
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			// TODO Auto-generated method stub
			super.onProgressUpdate(values);
		}

		@Override
		protected void onCancelled(Boolean result) {
			// TODO Auto-generated method stub
			super.onCancelled(result);
		}

		@Override
		protected void onCancelled() {
			// TODO Auto-generated method stub
			super.onCancelled();
		}


	}
	long beginTime=0L;
	private boolean waitUtilOneSecond(boolean result){
		while(System.currentTimeMillis()-beginTime<1000L){
			try{
				Thread.sleep(10);
			}catch (Exception e) {
				// TODO: handle exception
				Log.d(TAG,"e:"+e);
				break;
			}
		}
		return result;
	}
	private LinkedList<ContactEntity> list=new LinkedList<>();
	private LinkedList<ContactEntity> sortedList=new LinkedList<>();
	private class QueryMergeTask extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... params) {
			// TODO Auto-generated method stub
			Log.d(TAG,"doInBackground");			
			long time0=System.currentTimeMillis();
			Cursor cursor=null;
			try{
				cursor=getContentResolver().query(android.provider.ContactsContract.Contacts.CONTENT_URI, 
						PROJECTIONS, 
						"display_name is not null", 
						null, 
						"phonebook_bucket ASC,sort_key");
				int count=cursor.getCount();
				if(cursor==null || count==0 ||!cursor.moveToFirst()) return waitUtilOneSecond(false);

				do{
					list.add(new ContactEntity(cursor.getLong(0),cursor.getString(1)));
				}while(cursor.moveToNext());	

				Log.d(TAG,"spend time1:"+(System.currentTimeMillis()-time0));
			} catch(Exception e){
				Log.d(TAG,"e:"+e);
				return waitUtilOneSecond(false);
			} finally{
				if(cursor!=null){
					cursor.close();
					cursor=null;
				}
			}
			Log.d(TAG,"list:"+list);
			int count=list.size();
			if(count==0) return waitUtilOneSecond(false);
			boolean isAdded;
			for(int i=0;i<count;i++){
				String name1=list.get(i).getName();
				isAdded=false;
				for(int j=i+1;j<count;j++){
					String name2=list.get(j).getName();
					if(TextUtils.equals(name1.toUpperCase().trim(), name2.toUpperCase().trim())){
						if(!isAdded){
							list.get(i).setValid(true);
							isAdded=true;
						}
						list.get(j).setValid(true);
					} else break;
				}
			}

			for(ContactEntity contactEntity:list){
				if(contactEntity.isValid){
					sortedList.add(contactEntity);
				}
			}

			list=null;

			Log.d(TAG,"spend time2:"+(System.currentTimeMillis()-time0));


			Cursor cursor2=null;
			StringBuilder sb;

			for(ContactEntity contactEntity:sortedList){
				sb=new StringBuilder("contact_id=");
				sb.append(contactEntity.get_id()+" AND mimetype='vnd.android.cursor.item/phone_v2'");
				try{
					cursor2=getContentResolver().query(android.provider.ContactsContract.Data.CONTENT_URI, PROJECTIONS2, sb.toString(), null, null);
					if(cursor2==null ||cursor2.getCount()==0 ||!cursor2.moveToFirst()) continue;
					ArrayList<String> arrayList=new ArrayList<>();
					do{						
						arrayList.add(cursor2.getString(2));
					}while(cursor2.moveToNext());
					contactEntity.setNums(arrayList);
				}catch(Exception e){
					Log.d(TAG,"e:"+e);
				}finally{
					if(cursor2!=null){
						cursor2.close();
						cursor2=null;
					}
				}
			}

			sb=null;
			Log.d(TAG,"sortedList:"+sortedList);
			Log.d(TAG,"spend time3:"+(System.currentTimeMillis()-time0));
			return sortedList.size()>0?waitUtilOneSecond(true):waitUtilOneSecond(false);
		}


		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			Log.d(TAG,"onPreExecute");
			super.onPreExecute();
			beginTime=System.currentTimeMillis();
			prepareProgressDialogSpinner(null,getString(R.string.hb_querying));
		}

		
		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			Log.d(TAG,"onPostExecute result:"+result);
			
			super.onPostExecute(result);
			if(mProgressDialog!=null) mProgressDialog.dismiss();

			if(!result) {
				emptyLayout.setVisibility(View.VISIBLE);
				listview.setVisibility(View.GONE);
			} else{
				emptyLayout.setVisibility(View.GONE);
				listview.setVisibility(View.VISIBLE);
				createCheckedArray(sortedList.size());				
			}
			updateSelectedItemsView();
			mAdapter.notifyDataSetChanged();
			
			
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			// TODO Auto-generated method stub
			super.onProgressUpdate(values);
		}

		@Override
		protected void onCancelled(Boolean result) {
			// TODO Auto-generated method stub
			super.onCancelled(result);
		}

		@Override
		protected void onCancelled() {
			// TODO Auto-generated method stub
			super.onCancelled();
		}	


	}

}

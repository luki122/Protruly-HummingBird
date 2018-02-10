package com.android.downloadui;

import hb.app.HbActivity;
import hb.app.dialog.AlertDialog;
import hb.view.menu.BottomWidePopupMenu;
import hb.widget.ActionMode;
import hb.widget.ActionModeListener;
import hb.widget.HbListView;
import hb.widget.ActionMode.Item;
import android.app.DownloadManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.Downloads;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseLongArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AbsListView.RecyclerListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.android.providers.downloads.HbDownManager;
import com.android.providers.downloads.OpenHelper;
import com.android.providers.downloads.R;

/**
 * @author wxue
 */
public class HbDownloadList extends HbActivity implements OnItemClickListener, OnItemLongClickListener, OnClickListener{
	private static final String TAG = "HbDownloadList";
	private View mLoadingView;
	private View mEmptyView;
	private HbListView mListView;
	private DownloadListAdapter mAdapter;
	private boolean mEditMode = false;
	private boolean mSelectAll = false;
	private View mBtnDelete;
	private TextView mTxtDelete;
	//HB. Comments : remove sort function , Engerineer : wxue , Date : 2017年6月28日 ,begin
	//private BottomWidePopupMenu mBottomPopMenu;
	//HB. end
	private final int mLoaderId = 101;
	private int mOrder = HbCommonUtil.HB_SORT_ORDER_TIME;
	private LoaderCallbacks<DownloadResult> mCallbacks;
	private DownloadManager mDownloadManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setHbContentView(R.layout.hmb_download_activity);
		initView();
		initialUI(savedInstanceState);
		startDownloadInfoLoader();
	}

	private void initView() {
		mDownloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
		//HB. Comments : remove sort function , Engerineer : wxue , Date : 2017年6月28日 ,begin
		/*mBottomPopMenu = new BottomWidePopupMenu(this);
		mBottomPopMenu.inflateMenu(R.menu.hmb_sort_menu);
		mBottomPopMenu.setOnMenuItemClickedListener(mBottomMenuListener);*/
		//HB. end
		mListView = (HbListView) findViewById(android.R.id.list);
		mBtnDelete = findViewById(R.id.btn_del);
		mTxtDelete = (TextView)findViewById(R.id.txt_del);
		mBtnDelete.setOnClickListener(this);
		mListView.setOnItemClickListener(this);
		mListView.setOnItemLongClickListener(this);
		mListView.setRecyclerListener(mRecycleListener);
		mEmptyView = findViewById(android.R.id.empty);

        /// M: add a loading view to notify user we are loading item background. @{
        mLoadingView = findViewById(R.id.loading);
        TextView loadingTextView = (TextView) mLoadingView.findViewById(R.id.loading_text);
        loadingTextView.setText(com.mediatek.internal.R.string.contact_widget_loading);
	}

	@Override
	protected void initialUI(Bundle savedInstanceState) {
		mAdapter = new DownloadListAdapter(this);
		mListView.setAdapter(mAdapter);
		inflateToolbarMenu(R.menu.hmb_download_toolbar_menu);
      
		getToolbar().setNavigationIcon(null);
		setTitle(R.string.hmb_download_title);
		setupActionModeWithDecor(getToolbar());
		setActionModeListener(new ActionModeListener() {

			@Override
			public void onActionModeShow(ActionMode actionMode) {
				LogUtil.i(TAG, "onActionModeShow");
			}

			@Override
			public void onActionModeDismiss(ActionMode actionMode) {
				LogUtil.i(TAG, "onActionModeDismiss");
			}

			@Override
			public void onActionItemClicked(Item item) {
				switch (item.getItemId()) {
				case ActionMode.NAGATIVE_BUTTON:
					enterEditMode(false);
					break;
				case ActionMode.POSITIVE_BUTTON:
					updateToolbarTitle(true);
					break;

				default:
					break;
				}
			}
		});
	}

	private void startDownloadInfoLoader() {
		mCallbacks = new LoaderCallbacks<DownloadResult>() {

			@Override
			public void onLoaderReset(Loader<DownloadResult> loader) {
				mAdapter.swapResult(null);
			}

			@Override
			public void onLoadFinished(Loader<DownloadResult> loader, DownloadResult result) {
				LogUtil.i(TAG,"---onLoadFinished()--result = " + result);
				if (result == null || result.exception != null) {
					Toast.makeText(HbDownloadList.this, "downloadinfo loaded fail ...", Toast.LENGTH_SHORT).show();
					return;
				}
				mLoadingView.setVisibility(View.GONE);
				mAdapter.swapResult(result);
				if(mAdapter.getCount() == 0){
					mEmptyView.setVisibility(View.VISIBLE);
				}else{
					mEmptyView.setVisibility(View.GONE);
				}
			}

			@Override
			public Loader<DownloadResult> onCreateLoader(int id, Bundle args) {
				// TODO Auto-generated method stub
				return new DownloadListLoader(HbDownloadList.this, Downloads.Impl.CONTENT_URI, mOrder);
			}
		};
		getLoaderManager().restartLoader(mLoaderId, null, mCallbacks);
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		if (mEditMode) {
			enterEditMode(false);
			return;
		}
		super.onBackPressed();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		IntentFilter filter = new IntentFilter(HbCommonUtil.HB_ACTION_SPEEDS);
		registerReceiver(mReceiver, filter);
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		if(mReceiver != null){
			unregisterReceiver(mReceiver);
		}
	}
	
	@Override
	public boolean onMenuItemClick(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_download_all:
			startDownloadOrPauseAll(true);
			break;
		case R.id.menu_download_pause_all:
			startDownloadOrPauseAll(false);
			break;
		//HB. Comments : remove sort function , Engerineer : wxue , Date : 2017年6月28日 ,begin
		/*case R.id.menu_download_sort:
			mBottomPopMenu.show();
			break;
		case R.id.menu_documentui:
			final Intent intent = new Intent(DocumentsContract.ACTION_MANAGE_ROOT);
			intent.setData(DocumentsContract.buildRootUri("com.android.providers.downloads.documents", "downloads"));
			startActivity(intent);
			break;*/
        //HB. end      
		default:
			break;
		}
		return super.onMenuItemClick(item);
	}
	
	private void startDownloadOrPauseAll(boolean allStartDown){
		long[] ids = mAdapter.getDownOrPauseIds(allStartDown);
		if(ids ==null){
			return;
		}
		if(allStartDown){
			HbDownManager.getInstance(this).resumeDownload(ids);
		}else{
			HbDownManager.getInstance(this).pauseDownload(ids);
		}
	}
	
	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.btn_del){
			showDeleteDocDialog();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		int type = mAdapter.getItemViewType(position);
		if(type == DownloadListAdapter.TYPE_GROUP ){
			return ;
		}
		if(mEditMode){
			mAdapter.setSelectedItem(position);
			updateToolbarTitle(false);
		}else{
			if(type == DownloadListAdapter.TYPE_ITEM_DOWNLOADING){
				return;
			}
			Cursor cursor = mAdapter.getItem(position);
			if (cursor != null) {
				final int docId = HbDbUtil.getCursorInt(cursor, Downloads.Impl._ID);
				final String filePath = HbDbUtil.getCursorString(cursor, Downloads.Impl._DATA);
				final String url = HbDbUtil.getCursorString(cursor, Downloads.Impl.COLUMN_URI);
				if(!new File(filePath).exists()){
					showRedownloadDialog(docId, url);
					return;
				}
				openDownload(this, docId);
			}
		}
	}

	private void openDownload(Context context, long id) {
        if (!OpenHelper.startViewIntent(context, id, Intent.FLAG_ACTIVITY_NEW_TASK)) {
            Toast.makeText(context, R.string.download_no_application_title, Toast.LENGTH_SHORT)
                    .show();
        }
    }
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		// TODO Auto-generated method stub
		if(mAdapter.getItemViewType(position) == DownloadListAdapter.TYPE_GROUP){
			return true;
		}
		if (mEditMode) {
			return true;
		}
		mAdapter.setSelectedItem(position);
		updateToolbarTitle(false);
		enterEditMode(true);
		return true;
	}

	private void enterEditMode(boolean editMode) {
		mEditMode = editMode;
		mAdapter.setEditEnable(editMode);
		showActionMode(editMode);
		mBtnDelete.setVisibility(editMode ? View.VISIBLE : View.GONE);
		mAdapter.notifyDataSetChanged();
	}
	
	private void updateToolbarTitle(boolean actionItemClick) {
		if (actionItemClick) {
			mSelectAll = !mSelectAll;
			mAdapter.setSelectAll(mSelectAll);
		} else {
			int selectedSize = mAdapter.getSelectedItems().size();
			if (selectedSize == mAdapter.getValidItemCount()) {
				mSelectAll = true;
			} else {
				mSelectAll = false;
			}
		}
		if (mSelectAll) {
			getActionMode().setPositiveText(getResources().getString(R.string.hmb_unselect_all));
		} else {
			getActionMode().setPositiveText(getResources().getString(R.string.hmb_select_all));
		}
		int selectedCount = mAdapter.getSelectedItems().size();
		updateActionModeTitle(getResources().getString(R.string.hmb_selected_file_count, selectedCount));
		if(selectedCount > 0){
			mBtnDelete.setEnabled(true);
			mTxtDelete.setTextColor(Color.parseColor("#FFF45454"));
		}else{
			mBtnDelete.setEnabled(false);
			mTxtDelete.setTextColor(Color.parseColor("#FFC2C2C2"));
		}
	}

	private void setUserSortOrder(int order){
		if(mOrder != order){
			mOrder = order;
			getLoaderManager().restartLoader(mLoaderId, null, mCallbacks);
		}
	}
	
	private void deleteRecordAndFile(List<Integer> downloadingList, List<Integer> downloadedList){
		int size = 0;
		if(downloadingList != null){
			size += downloadingList.size();
		}
		if(downloadedList != null){
			size += downloadedList.size();
		}
		long[] ids = new long[size];
		int i = 0;
		if(downloadingList != null && downloadingList.size() > 0){
			for (int id : downloadingList) {
				ids[i++] = id;
			}
		}
		if(downloadedList != null && downloadedList.size() > 0){
			for (int id : downloadedList) {
				ids[i++] = id;
			}
		}
		if(ids.length > 0){
			DownloadManager mDm = (DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE);
			mDm.remove(ids);
		}
	}
	
	private void deleteDownloadDocs(boolean delDoc){
		Map<Integer,Boolean> map = mAdapter.getSelectedItems();
		List<Integer> downloadingList = new ArrayList<Integer>();
		List<Integer> downloadedList = new ArrayList<Integer>();
		for (Map.Entry<Integer,Boolean> entry : map.entrySet()) {
			int type = mAdapter.getItemViewType(entry.getKey());
			Cursor cursor = mAdapter.getItem(entry.getKey());
			int docId = HbDbUtil.getCursorInt(cursor, Downloads.Impl._ID);
			if(type == DownloadListAdapter.TYPE_ITEM_DOWNLOADING){
				downloadingList.add(docId);
			}else if(type == DownloadListAdapter.TYPE_ITEM_DOWNLOADED){
				downloadedList.add(docId);
			}
	    }
		if(delDoc){
			deleteRecordAndFile(downloadingList, downloadedList);
		}else{
			if(downloadingList.size() > 0){
				deleteRecordAndFile(downloadingList, null);
			}
			// only delete database record
			if(downloadedList.size() > 0){
				long[] ids = new long[downloadedList.size()];
				int i = 0;
				for (int id : downloadedList) {
					ids[i++] = id;
				}
				ContentResolver resolver = getContentResolver();
				resolver.delete(Downloads.Impl.CONTENT_URI, HbCommonUtil.getWhereClauseForIds(ids), HbCommonUtil.getWhereArgsForIds(ids));
			}
		}
		enterEditMode(false);
	}
	
	private void showDeleteDocDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this); 
		builder.setTitle(getResources().getString(R.string.hmb_delete_doc_msg));
        View contentView = LayoutInflater.from(this).inflate(R.layout.hmb_delete_doc_dialog, null);
        final CheckBox checkBoc = (CheckBox)contentView.findViewById(R.id.checkbox_del);
        builder.setView(contentView);
        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(which == DialogInterface.BUTTON_POSITIVE){
					deleteDownloadDocs(checkBoc.isChecked());
				}
				dialog.cancel();
			}
		};
		builder.setPositiveButton(getResources().getString(R.string.hmb_ok), clickListener);
		builder.setNegativeButton(getResources().getString(R.string.hmb_cancel), clickListener);
		builder.show();
	}
	
	private void showRedownloadDialog(final int id, final String url){
		AlertDialog.Builder builder = new AlertDialog.Builder(this); 
		builder.setTitle(getResources().getString(R.string.hmb_dialog_title));
		builder.setMessage(getResources().getString(R.string.hmb_file_not_exist));
        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(which == DialogInterface.BUTTON_POSITIVE){
					if(HbCommonUtil.isValidUrl(url)){
						mDownloadManager.restartDownload(id);
					}else{
						Toast.makeText(HbDownloadList.this, R.string.hmb_down_url_error, Toast.LENGTH_SHORT).show();
						List<Integer> list = new ArrayList<Integer>();
						list.add(id);
						deleteRecordAndFile(null, list);
					}
				}
				dialog.cancel();
			}
		};
		builder.setPositiveButton(getResources().getString(R.string.hmb_ok), clickListener);
		builder.setNegativeButton(getResources().getString(R.string.hmb_cancel), clickListener);
		builder.show();
	}
	
	private RecyclerListener mRecycleListener = new RecyclerListener() {
		@Override
		public void onMovedToScrapHeap(View view) {
			final ImageView iconThumb = (ImageView) view.findViewById(R.id.file_icon);
			if (iconThumb != null) {
				final HbThumbnailAsyncTask oldTask = (HbThumbnailAsyncTask) iconThumb.getTag();
				if (oldTask != null) {
					oldTask.preempt();
					iconThumb.setTag(null);
				}
			}
		}
	};
    
	//HB. Comments : remove sort function , Engerineer : wxue , Date : 2017年6月28日 ,begin
	/* private BottomWidePopupMenu.OnMenuItemClickListener mBottomMenuListener = new BottomWidePopupMenu.OnMenuItemClickListener() {

		@Override
		public boolean onItemClicked(MenuItem item) {
			switch (item.getItemId()) {
			case R.id.sort_name:
                setUserSortOrder(HbCommonUtil.HB_SORT_ORDER_NAME);
				break;
			case R.id.sort_time:
				setUserSortOrder(HbCommonUtil.HB_SORT_ORDER_TIME);
				break;
			case R.id.sort_size_big_small:
				setUserSortOrder(HbCommonUtil.HB_SORT_ORDER_SIZE_BIG_SMALL);
				break;
			case R.id.sort_size_small_big:
				setUserSortOrder(HbCommonUtil.HB_SORT_ORDER_SIZE_SMALL_BIG);
				break;
			default:
				break;
			}
			return true;
		}
	};*/
	//HB. end
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(action.equals(HbCommonUtil.HB_ACTION_SPEEDS)){
				long[] ids = (long[])intent.getLongArrayExtra(HbCommonUtil.HB_IDS);
				long[] speeds = (long[])intent.getLongArrayExtra(HbCommonUtil.HB_SPEEDS);
				LongSparseLongArray speedLongArray = new LongSparseLongArray();
				for (int i=0;i<ids.length;i++) {
					speedLongArray.put(ids[i], speeds[i]);
				}
				mAdapter.setDownloadSpeed(speedLongArray);
			}
		}
	};
}

package com.android.downloadui;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.android.downloadui.HbCommonUtil.NetState;
import com.android.providers.downloads.DownloadApplication;
import com.android.providers.downloads.HbDownManager;
import com.android.providers.downloads.R;
import com.google.android.collect.Lists;
import android.app.DownloadManager;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.provider.Downloads;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.LongSparseLongArray;
import android.util.SparseLongArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
/**
 * @author wxue
 */
public class DownloadListAdapter extends BaseAdapter {
	private static final String TAG = "DownloadListAdapter";
	public static final int MIN_CLICK_DELAY_TIME = 500;
	private long lastClickTime = 0;
	public static final int TYPE_GROUP = 0;
	public static final int TYPE_ITEM_DOWNLOADING = 1;
	public static final int TYPE_ITEM_DOWNLOADED = 2;
	private Cursor mDownloadingCursor;
	private Cursor mDownloadedCursor;
	private int mDownloadingSize;
	private int mDownloadedSize;
	private Point mThumbSize;
	private Context mContext;
	private boolean mEditMode = false;
	private ArrayMap<Integer,Boolean> mSelectedItems = new ArrayMap<Integer,Boolean>();
	private DownloadManager mDownloadManager;
	private LongSparseLongArray mDownloadSpeed = new LongSparseLongArray();

	public DownloadListAdapter(Context context) {
		mContext = context;
		int thumbSize = context.getResources().getDimensionPixelSize(R.dimen.hb_grid_width);
		mThumbSize = new Point(thumbSize, thumbSize);
		mDownloadManager = (DownloadManager)mContext.getSystemService(Context.DOWNLOAD_SERVICE);
	}
	
	public void setSelectedItem(int position){
		Boolean selected = mSelectedItems.get(position);
		if(selected == null){
			mSelectedItems.put(position, true);
		}else{
			mSelectedItems.remove(position);
		}
	    notifyDataSetChanged();
	}
	
	public void setEditEnable(boolean enable){
		if(!enable){
			mSelectedItems.clear();
		}
		mEditMode = enable;
	}
	
	public Map<Integer,Boolean> getSelectedItems(){
		return mSelectedItems;
	}
	
	public void setSelectAll(boolean selected){
		if(!selected){
			mSelectedItems.clear();
		}else{
			for(int i=0;i<getCount();i++){
				if(getItemViewType(i) == TYPE_GROUP){
					continue;
				}
				mSelectedItems.put(i, selected);
		    }
		}
		notifyDataSetChanged();
	}
	
	public int getValidItemCount(){
		return mDownloadingSize + mDownloadedSize;
	}
	
	public void setDownloadSpeed(LongSparseLongArray speedArray){
		mDownloadSpeed.clear();
		mDownloadSpeed = speedArray;
		notifyDataSetChanged();
	}
	
	public long[] getDownOrPauseIds(boolean allStartDown) {
		if (mDownloadingCursor == null || mDownloadingCursor.getCount() == 0) {
			return null;
		}
		List<Long> list = new ArrayList<Long>();
		for (int i = 0; i < mDownloadingSize; i++) {
			mDownloadingCursor.moveToPosition(i);
			if(allStartDown){
				if (HbDbUtil.getCursorInt(mDownloadingCursor, Downloads.Impl.COLUMN_STATUS) == Downloads.Impl.STATUS_PAUSED_BY_APP) {
					list.add(HbDbUtil.getCursorLong(mDownloadingCursor, Downloads.Impl._ID));
				}
			}else{
				if (HbDbUtil.getCursorInt(mDownloadingCursor, Downloads.Impl.COLUMN_STATUS) != Downloads.Impl.STATUS_PAUSED_BY_APP) {
					list.add(HbDbUtil.getCursorLong(mDownloadingCursor, Downloads.Impl._ID));
				}
			}
		}
		if (list.size() > 0) {
			long[] ids = new long[list.size()];
			int count = 0;
			for (Long id : list) {
				ids[count++] = id;
			}
			return ids;
		}
		return null;
	}
	
	public void swapResult(DownloadResult result) {
		mDownloadingCursor = result != null ? result.mDowningCursor : null;
		mDownloadedCursor = result != null ? result.mDownloadedCursor : null;
		mDownloadingSize = mDownloadingCursor != null ? mDownloadingCursor.getCount() : 0;
		mDownloadedSize = mDownloadedCursor != null ? mDownloadedCursor.getCount() : 0;
		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		int itemType = getItemViewType(position);
		if (itemType == TYPE_GROUP) {
			return getDocGroupView(position, convertView, parent);
		} else if(itemType == TYPE_ITEM_DOWNLOADING){
			return getDownloadingView(position, convertView, parent);
		}else{
			return getDownloadedView(position, convertView, parent);
		}
	}

	private View getDocGroupView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			final LayoutInflater inflater = LayoutInflater.from(mContext);
			convertView = inflater.inflate(R.layout.hmb_item_down_group_list, parent, false);
		}
		String groupTitle = "";
		if (position == 0 && position < mDownloadingSize) {
			groupTitle = mContext.getResources().getString(R.string.hmb_downloading, mDownloadingSize);
		} else {
			groupTitle = mContext.getResources().getString(R.string.hmb_downloaded, mDownloadedSize);
		}

		final TextView title = (TextView) convertView.findViewById(R.id.group_title);
		title.setText(groupTitle);
		title.setEnabled(false);
		return convertView;
	}

	private View getDownloadingView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			final LayoutInflater inflater = LayoutInflater.from(mContext);
			convertView = inflater.inflate(R.layout.hmb_item_downloading_list, parent, false);
		}
		final Cursor cursor = getItem(position);
		
		final int docId = HbDbUtil.getCursorInt(cursor, Downloads.Impl._ID);
		String uri = HbDbUtil.getCursorString(cursor, Downloads.Impl.COLUMN_URI);
		long docTotalSize = HbDbUtil.getCursorLong(cursor, Downloads.Impl.COLUMN_TOTAL_BYTES);
		long docCurSize = HbDbUtil.getCursorLong(cursor, Downloads.Impl.COLUMN_CURRENT_BYTES);
		String displayName = HbDbUtil.getCursorString(cursor, Downloads.Impl.COLUMN_TITLE);
		final int status = HbDbUtil.getCursorInt(cursor, Downloads.Impl.COLUMN_STATUS);
		final int netType = HbDbUtil.getCursorInt(cursor, Downloads.Impl.COLUMN_ALLOWED_NETWORK_TYPES);
		String mimeType = HbDbUtil.getCursorString(cursor, Downloads.Impl.COLUMN_MIME_TYPE);
        if (mimeType == null) {
            mimeType = "vnd.android.document/file";
        }

        final ImageView imgDoc = (ImageView) convertView.findViewById(R.id.file_icon);
		final TextView txtName = (TextView) convertView.findViewById(R.id.file_name);
		final ProgressBar progressBar = (ProgressBar) convertView.findViewById(R.id.progressBar);
		final TextView txtCurrentSize = (TextView) convertView.findViewById(R.id.current_size);
		final TextView txtSpeed = (TextView) convertView.findViewById(R.id.down_speed);
		final View viewDown = convertView.findViewById(R.id.layout_download);
		final ImageView imgDown = (ImageView) convertView.findViewById(R.id.img_download);
		final CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.downloading_checkbox);
		final View line = convertView.findViewById(R.id.downloading_line);
		// set filename
		if(TextUtils.isEmpty(displayName)){
			displayName = HbCommonUtil.getDownloadingFileName(uri);
		}
		txtName.setText(displayName);
		// set checkbox
		setCheckBoxVisible(position, checkBox, viewDown);
		// set downloading image
		imgDoc.setImageDrawable(HbIconUtil.loadMimeIcon(mContext, mimeType));
		progressBar.setProgress((int)(docCurSize * 100 / docTotalSize));
		// set current download size
		if(docTotalSize < 0){
			txtCurrentSize.setText(HbCommonUtil.formatFileSize(docCurSize) + "/" + HbCommonUtil.formatFileSize(0));
		}else{
			txtCurrentSize.setText(HbCommonUtil.formatFileSize(docCurSize) + "/" + HbCommonUtil.formatFileSize(docTotalSize));
		}
		// set download speed
		setDownloadSpeed(docId, status, netType, txtSpeed);
		// set download icon and clicklistener
		setDownloadImage(docId, status, imgDown, viewDown);
		// set line
		if (position == mDownloadingSize) {
			line.setVisibility(View.GONE);
		} else {
			line.setVisibility(View.VISIBLE);
		}
		return convertView;
	}
	
	private void setDownloadSpeed(final int id, final int status, final int type, TextView txtSpeed){
		long downBytes= mDownloadSpeed.get(id);
		if(status == Downloads.Impl.STATUS_RUNNING){
			if(downBytes > 0){
				txtSpeed.setText(HbCommonUtil.formatFileSize(downBytes) + "/s");
			}else{
				txtSpeed.setText("0MB/s");
			}
		}else if(status == Downloads.Impl.STATUS_WAITING_DOWNLOAD){
			txtSpeed.setText(R.string.hmb_waiting_download);
		}else if(status == Downloads.Impl.STATUS_QUEUED_FOR_WIFI){
			txtSpeed.setText(R.string.hmb_waiting_for_wifi);
		}else if(status == Downloads.Impl.STATUS_WAITING_FOR_NETWORK){
			txtSpeed.setText(R.string.hmb_waiting_for_network);
		}else{
			txtSpeed.setText("");
		}
	}
	
	private void setDownloadImage(final long id, final int status, ImageView downImage, View downView){
		int resId = R.drawable.hmb_download;
		if(status == Downloads.Impl.STATUS_RUNNING){
			resId = R.drawable.hmb_pause;
		}else if(status == Downloads.Impl.STATUS_PAUSED_BY_APP || status == Downloads.Impl.STATUS_PENDING){
			resId = R.drawable.hmb_download;
		}else{
			resId = R.drawable.hmb_waiting_download;
		}
		downImage.setImageResource(resId);
		downView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				long currentTime = System.currentTimeMillis();
				if(currentTime - lastClickTime > MIN_CLICK_DELAY_TIME){
					if(status == Downloads.Impl.STATUS_PAUSED_BY_APP){
						HbDownManager.getInstance(mContext).resumeDownload(id);
					}else{
						HbDownManager.getInstance(mContext).pauseDownload(id);
					}
				}
				lastClickTime = currentTime;
			}
		});
	}
	
	private View getDownloadedView(int position, View convertView, ViewGroup parent) {
		final ThumbnailCache thumbs = DownloadApplication.getThumbnailsCache(mContext, mThumbSize);
		if (convertView == null) {
			final LayoutInflater inflater = LayoutInflater.from(mContext);
			convertView = inflater.inflate(R.layout.hmb_item_downloaded_list, parent, false);
		}

		final Cursor cursor = getItem(position);
		final int docId = HbDbUtil.getCursorInt(cursor, Downloads.Impl._ID);
		String uri = HbDbUtil.getCursorString(cursor, Downloads.Impl.COLUMN_URI);
		long docLastModified = HbDbUtil.getCursorLong(cursor, Downloads.Impl.COLUMN_LAST_MODIFICATION);
		String displayName = HbDbUtil.getCursorString(cursor, Downloads.Impl.COLUMN_TITLE);
		String mimeType = HbDbUtil.getCursorString(cursor, Downloads.Impl.COLUMN_MIME_TYPE);
		final int destination = HbDbUtil.getCursorInt(cursor,Downloads.Impl.COLUMN_DESTINATION);
		final String path = HbDbUtil.getCursorString(cursor, Downloads.Impl._DATA);
		 
        if (mimeType == null) {
            // Provide fake MIME type so it's openable
            mimeType = "vnd.android.document/file";
        }
        final Long docSize = HbDbUtil.getCursorLong(cursor, Downloads.Impl.COLUMN_TOTAL_BYTES);
        final Long docCurSize = HbDbUtil.getCursorLong(cursor, Downloads.Impl.COLUMN_CURRENT_BYTES);
        final ImageView imgDoc = (ImageView) convertView.findViewById(R.id.file_icon);
		final TextView txtName = (TextView) convertView.findViewById(R.id.file_name);
		final TextView txtDownloadTime = (TextView) convertView.findViewById(R.id.file_download_time);
		final TextView txtSzie = (TextView) convertView.findViewById(R.id.file_size);
		final CheckBox checkBox = (CheckBox)convertView.findViewById(R.id.downloaded_checkbox);
		final TextView txtDel = (TextView)convertView.findViewById(R.id.txt_deleted);
		final View line = convertView.findViewById(R.id.downloaded_line);
		setFileNameAndDeletedFlag(displayName, txtName, path, txtDel);
		// set iamge
		final HbThumbnailAsyncTask oldTask = (HbThumbnailAsyncTask) imgDoc.getTag();
        if (oldTask != null) {
            oldTask.preempt();
            imgDoc.setTag(null);
        }
		boolean showThumbnail = isSupportThumbnail(mimeType, docCurSize, docSize, destination) && isAllowThumbnail(mimeType);
		boolean cacheHit = false;
        if (showThumbnail) {
            final Uri docUri = HbCommonUtil.getDocumentUri(docId);
            final Bitmap cachedResult = thumbs.get(docUri);
            if (cachedResult != null) {
                imgDoc.setImageBitmap(cachedResult);
                cacheHit = true;
            } else {
            	imgDoc.setImageDrawable(null);
                final HbThumbnailAsyncTask task = new HbThumbnailAsyncTask(docUri, imgDoc, mThumbSize);
                imgDoc.setTag(task);
                ProviderExecutor.forAuthority("downloads").execute(task);
            }
        }
        // Always throw MIME icon into place, even when a thumbnail is being
        // loaded in background.
        if (!cacheHit) {
        	imgDoc.setImageDrawable(HbIconUtil.loadMimeIcon(mContext, mimeType));
        }
		
		// set download time
		 if (docLastModified == -1) {
			 txtDownloadTime.setText(null);
         } else {
        	 txtDownloadTime.setText(HbDateUtil.getFormatTime(mContext, docLastModified));
         }
		 // set file size
		if(docSize == -1){
			txtSzie.setText(null);
		}else{
            txtSzie.setText(HbCommonUtil.formatFileSize(docSize));
		}
		// set checkbox
		setCheckBoxVisible(position, checkBox, null);
		// set line
		if(position == getCount()-1){
			line.setVisibility(View.GONE);
		}else{
			line.setVisibility(View.VISIBLE);
		}
		return convertView;
	}
	
	private void setFileNameAndDeletedFlag(String name, TextView txtView, String path, TextView delView){
		if(!new File(path).exists()){
			delView.setVisibility(View.VISIBLE);
			txtView.setMaxWidth(mContext.getResources().getDimensionPixelSize(R.dimen.hb_textview_filename_max_width));
		}else{
			delView.setVisibility(View.GONE);
			LayoutParams params = txtView.getLayoutParams();
			params.width = LayoutParams.WRAP_CONTENT;
			txtView.setLayoutParams(params);
		}
		txtView.setText(name);
	}
	
	private void setCheckBoxVisible(int position, CheckBox checkBox, View viewDown){
		Boolean selected = mSelectedItems.get(position);
		if(mEditMode){
			checkBox.setVisibility(View.VISIBLE);
			if(selected != null){
				checkBox.setChecked(selected);
			}else{
				checkBox.setChecked(false);
			}
			if(viewDown != null){
				viewDown.setVisibility(View.GONE);
			} 
		}else{
			checkBox.setChecked(false);
			checkBox.setVisibility(View.GONE);
			if(viewDown != null){
				viewDown.setVisibility(View.VISIBLE);
			} 
		}
	}
	
	private boolean isSupportThumbnail(String mimeType, long curSize, long totalSize, int destination){
		if (mimeType != null && mimeType.startsWith("image/")) {
            if ((totalSize >0 && curSize == totalSize) || destination == Downloads.Impl.DESTINATION_NON_DOWNLOADMANAGER_DOWNLOAD) {
                return true;
            }
        }
		return false;
	}
	
	private boolean isAllowThumbnail(String docMimeType){
		return MimePredicate.mimeMatches(MimePredicate.VISUAL_MIMES, docMimeType);
	}

	@Override
	public int getCount() {
		int count = 0;
		if (mDownloadingSize > 0) {
			count += (mDownloadingSize + 1);
		}
		if (mDownloadedSize > 0) {
			count += (mDownloadedSize + 1);
		}
		return count;
	}

	@Override
	public int getItemViewType(int position) {
		if(position == 0){
			return TYPE_GROUP;
		}
		if(mDownloadingSize > 0 && position <= mDownloadingSize){
			return TYPE_ITEM_DOWNLOADING;
		}
		if(mDownloadingSize > 0 && position == mDownloadingSize + 1){
			return TYPE_GROUP;
		}
		if(position >= mDownloadingSize + 1){
			return TYPE_ITEM_DOWNLOADED;
		}
		return -1;
	}

	@Override
	public int getViewTypeCount() {
		return 3;
	}

	@Override
	public Cursor getItem(int position) {
		int type = getItemViewType(position);
		if (type == TYPE_GROUP) {
			return null;
		}
		if (type == TYPE_ITEM_DOWNLOADING) {
			mDownloadingCursor.moveToPosition(position - 1);
			return mDownloadingCursor;
		}
		if (type == TYPE_ITEM_DOWNLOADED) {
			int offset = mDownloadingSize;
			if (mDownloadingSize > 0) {
				offset += 1;
			}
			mDownloadedCursor.moveToPosition(position - offset - 1);
			return mDownloadedCursor;
		}
		return null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
}

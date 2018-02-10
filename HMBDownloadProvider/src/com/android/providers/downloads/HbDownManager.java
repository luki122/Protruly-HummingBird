package com.android.providers.downloads;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.android.downloadui.HbCommonUtil;
import com.google.android.collect.Maps;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.provider.Downloads;

public class HbDownManager {
	private static final String TAG = "HbDownManager";
	private static HbDownManager mInstance;
	private final Map<Long, DownloadInfo> mDownloads = Maps.newHashMap();
	private List<DownloadInfo> mWaitingTasks = new ArrayList<DownloadInfo>();
	private Context mContext;
	private ContentResolver mResolver;
	
	private HbDownManager(Context context){
		mContext = context;
		mResolver = mContext.getContentResolver();
	}
	
	public static HbDownManager getInstance(Context context) {
		if (mInstance == null) {
			synchronized (HbDownManager.class) {
				if (mInstance == null) {
					mInstance = new HbDownManager(context);
				}
			}
		}
		return mInstance;
	}
	
	public Map<Long, DownloadInfo> getDownloadInfos(){
		return mDownloads;
	}

	public void addDownloadInfo(DownloadInfo info){
		//Log.i(TAG,"---addDownloadInfo()--info = " + info);
		mDownloads.put(info.mId, info);
	}
	
	public void removeDownloadInfo(long id){
		mDownloads.remove(id);
	}
	
	public void clearAllDowninfo(){
		//Log.i(TAG,"---clearAllDowninfo()");
		mDownloads.clear();
	}
	
	public void addWaitConfirmedTask(DownloadInfo info){
		mWaitingTasks.add(info);
	}
	
	public boolean isWaitingConfirm(DownloadInfo info){
		return mWaitingTasks.contains(info);
	}
	
	public List<DownloadInfo> getWaitConfirmedTasks(){
		return mWaitingTasks;
	}
	
	 /**
     * Pause downloads from the download manager.  Each download will be stopped if
     * it was running.
     * @param ids the IDs of the downloads to pause
     * @return the number of downloads actually paused
     */
	public int pauseDownload(long... ids) {
		//Log.i(TAG,"---pauseDownload()");
		if (ids == null || ids.length == 0) {
    		// called with nothing to remove!
    		throw new IllegalArgumentException("input param 'ids' can't be null");
    	}
		for (long id : ids) {
    		DownloadInfo info = mDownloads.get(id);
    		//Log.i(TAG,"---pauseDownload() -- id = " + id);
			if(info != null){
				//Log.i(TAG,"---pauseDownload() -- id = " + id + " info.mControl = " + info.mControl + " status = " + info.mStatus + " info = " + info);
				if(!info.isRunning()){
					info.cancelTask();
				}else{
					synchronized (info) {
						info.mControl = Downloads.Impl.CONTROL_PAUSED;
					}
				}
			}
    	}
    	ContentValues values = new ContentValues();
    	values.put(Downloads.Impl.COLUMN_CONTROL, Downloads.Impl.CONTROL_PAUSED);
    	values.put(Downloads.Impl.COLUMN_STATUS, Downloads.Impl.STATUS_PAUSED_BY_APP);
    	if (ids.length == 1) {
    		return mResolver.update(ContentUris.withAppendedId(Downloads.Impl.CONTENT_URI, ids[0]), values,
    				null, null);
    	} 
    	return mResolver.update(Downloads.Impl.CONTENT_URI, values, HbCommonUtil.getWhereClauseForIds(ids),
    			HbCommonUtil.getWhereArgsForIds(ids));
	}
	
	 /**
     * Resume downloads from the download manager. 
     * @param ids the IDs of the downloads to resume
     * @return the number of downloads actually resumed
     */
    public int resumeDownload(long... ids) {
    	//Log.i(TAG,"---resumeDownload()");
    	if (ids == null || ids.length == 0) {
    		// called with nothing to remove!
    		throw new IllegalArgumentException("input param 'ids' can't be null");
    	}
    	for (long id : ids) {
    		DownloadInfo info = mDownloads.get(id);
    		//Log.i(TAG,"---resumeDownload()---id = " + id);
			if(info != null){
				//Log.i(TAG,"---resumeDownload() -- id = " + id + " info.mControl = " + info.mControl + " status = " + info.mStatus + " info = " + info);
				synchronized (info) {
					info.mControl = Downloads.Impl.CONTROL_RUN;
				}
			}
    	}
    	ContentValues values = new ContentValues();
    	values.put(Downloads.Impl.COLUMN_CONTROL, Downloads.Impl.CONTROL_RUN);
    	values.put(Downloads.Impl.COLUMN_STATUS, Downloads.Impl.STATUS_PENDING);
    	if (ids.length == 1) {
    		return mResolver.update(ContentUris.withAppendedId(Downloads.Impl.CONTENT_URI, ids[0]), values,
    				null, null);
    	} 
    	return mResolver.update(Downloads.Impl.CONTENT_URI, values, HbCommonUtil.getWhereClauseForIds(ids),
    			HbCommonUtil.getWhereArgsForIds(ids));
    }
}

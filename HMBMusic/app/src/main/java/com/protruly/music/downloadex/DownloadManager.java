package com.protruly.music.downloadex;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.widget.Toast;

import com.protruly.music.downloadex.DownloadTask.State;
import com.protruly.music.downloadex.db.Dao;

import com.protruly.music.util.HBMusicUtil;
import com.protruly.music.util.Globals;
import com.protruly.music.util.LogUtil;
import com.xiami.sdk.entities.OnlineSong;

import com.protruly.music.R;



public class DownloadManager {

	private static final String TAG = "DownloadManager";
	// private LinkedHashMap<String, DownloadInfo> mHashMap = null;
	private LinkedHashMap<Long, DownloadInfo> mHashMap = null;
	private int maxDownloadThread = 3;
	private static Context mContext = null;
	private Dao mdb = null;
	private Map map = null;
	private static DownloadManager mInstance = null;
	public static final int MSG_TOAST=100;

	// private Handler mHandler = null;

	public static synchronized DownloadManager getInstance(Context context) {
		if (mInstance == null){
			mInstance = new DownloadManager(context);
		}

		return mInstance;
	}

	public Dao getDownloadManagerDb() {
		return mdb;
	}
	
	private Handler handler;

	protected DownloadManager(Context appContext) {
		mContext = appContext.getApplicationContext();

		if (mdb == null) {
			mdb = Dao.getInstance(mContext);
		}
		if(handler==null){
			handler = new Handler(mContext.getMainLooper()){
				
				@Override
				public void handleMessage(Message msg) {
					super.handleMessage(msg);
					if(msg.what==MSG_TOAST){
						Toast.makeText(mContext, mContext.getResources().getString(R.string.no_memory), Toast.LENGTH_LONG).show();
					}
				}
				
			};
		}
		initData();
	}

	private void initHashMap() {
		if (map == null) {
			map = Collections.synchronizedMap(new HashMap());
		}

		map.clear();
		return;
	}

	private void initData() {
		mHashMap = mdb.getAllDownLoadData();
		if (mHashMap == null) {
			LogUtil.iv(TAG, "initData 1 fail.");

			mHashMap = new LinkedHashMap<Long, DownloadInfo>();
		}

		File file = new File(Globals.mSongImagePath);
		if (!file.exists()) {
			file.mkdirs();
		}

		File file2 = new File(Globals.mCachePath);
		if (!file2.exists()) {
			file2.mkdirs();
		}

		initHashMap();
		// mHandler = new Handler();
		LogUtil.iv(TAG, "initData ok mHashMap len:" + mHashMap.size());
		return;
	}

	public boolean isFinished(long id) {
		boolean doing = false;

		if (mHashMap != null) {
			DownloadInfo info = mHashMap.get(id);
			if(!isExDBAndSd(info)){
				return false;
			}
			if (info != null && info.isDownloadFinished()) {
				doing = true;
			}
		}

		return doing;
	}

	public boolean isInDownloading(long id) {
		boolean doing = false;

		if (mHashMap != null) {
			DownloadInfo info = mHashMap.get(id);
			if (info != null && info.isDownloading()) {
				doing = true;
			}
		}

		return doing;
	}

	/**
	 * 存在db和SD
	 * @param info
	 * @return
	 */
	private boolean isExDBAndSd(DownloadInfo info) {
		if (info != null) {
			if (!TextUtils.isEmpty(info.getFileSavePath()) && new File(info.getFileSavePath()).exists()) {
				return true;
			}else {
				try {
					mdb.delete(info.getDownloadUrl(), info.getId());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}

	public boolean isAddDownload(long id) {
		boolean doing = false;

		if (mHashMap != null) {
			DownloadInfo info = mHashMap.get(id);
			if(!isExDBAndSd(info)){
				return false;
			}
			if (info != null) {
				LogUtil.d(TAG, "\n\ninfo::" + info.getState());
			}
			if (info != null && (info.isDownloading() || info.isWaiting() || info.isDownloadPause())) {
				doing = true;
			}
		}
		LogUtil.d(TAG, "doing::" + doing + " \n\n");
		return doing;
	}

	/**
	 * @ClassName: addNewDownload下载歌曲
	 * @Description: 下载歌曲
	 * @param id
	 *            下载歌曲的id
	 * @param title
	 *            歌曲标题
	 * @param artist
	 *            歌曲艺术家
	 * @param album
	 *            歌曲专辑名
	 * @param imgurl
	 *            歌曲图片URL
	 * @param savedir
	 *            可以为null,下载保存路径，现在是默认的 music/song/
	 */
	public void addNewDownload(long id, String title, String artist, String album, String imgurl, String savedir, String lrcUrl) throws Exception {
		if (id < 0) {
			return;
		}

		if (isInDownloading(id)) {
			return;
		}

		final DownloadInfo downloadInfo = buildDownloadInfo(id, title, artist, album, imgurl, savedir, lrcUrl, null);
		DownloadTask task = new DownloadTask(downloadInfo, mdb, mListener, mContext,handler);
		downloadInfo.setTask(task);
		downloadInfo.setState(task.getState());

		task.executeOnExecutor(DownloadAsyncTask.DUAL_THREAD_EXECUTOR);
		mHashMap.put(id, downloadInfo);

		if (mdb != null) {
			// mdb.insert(downloadInfo);
			mdb.savaOrUpdate(downloadInfo);
		}

		LogUtil.d(TAG, "addNewDownload music   mHashMap:" + mHashMap.size() + ",downloadInfo:" + downloadInfo.toString());
	}

	public void addNewDownload(OnlineSong song) throws Exception {
		if (song == null) {
			return;
		}

		if (isInDownloading(song.getSongId())) {
			return;
		}

		final DownloadInfo downloadInfo = buildDownloadInfoByOnlineSong(song);
		DownloadTask task = new DownloadTask(downloadInfo, mdb, mListener, mContext,handler);
		downloadInfo.setTask(task);
		downloadInfo.setState(task.getState());

		task.executeOnExecutor(DownloadAsyncTask.DUAL_THREAD_EXECUTOR);
		mHashMap.put(song.getSongId(), downloadInfo);

		if (mdb != null) {
			// mdb.insert(downloadInfo);
			mdb.savaOrUpdate(downloadInfo);
		}

		LogUtil.d(TAG, "addNewDownload music   mHashMap:" + mHashMap.size() + ",downloadInfo:" + downloadInfo.toString());
	}

	public DownloadInfo buildDownloadInfoByOnlineSong(OnlineSong item) {
		if (item == null) {
			return null;
		}

		DownloadInfo downloadInfo = buildDownloadInfo(item.getSongId(), item.getSongName(), item.getArtistName(), item.getAlbumName(), item.getImageUrl(), null, item.getLyric(), item.getSingers());
		downloadInfo.setState(DownloadTask.State.SUCCESS);
		downloadInfo.setLrcUrl(item.getLyric());
		downloadInfo.setDownloadUrl(item.getListenFile());
		downloadInfo.setCreatetime(System.currentTimeMillis());
		downloadInfo.setFileLength(new File(downloadInfo.getFileSavePath()).length());
		return downloadInfo;
	}



	private DownloadInfo buildDownloadInfo(long id, String title, String artist, String album, String imgurl, String savedir, String lrcUrl, String singers) {
		if (id < 0) {
			return null;
		}
		DownloadInfo downloadInfo = new DownloadInfo();
		downloadInfo.setId(id);
		try {
			if (TextUtils.isEmpty(singers) || singers.trim().equalsIgnoreCase(artist.trim())) {
				downloadInfo.setFileName(title.replaceAll("/", "-") + "_" + artist.replaceAll("/", "-") + ".mp3");
			} else {
				downloadInfo.setFileName(title.replaceAll("/", "-") + "_" + artist.replaceAll("/", "-") + "_" + singers.replaceAll("/", "-") + ".mp3");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		downloadInfo.setAlbum(album);
		downloadInfo.setImgUrl(imgurl);
		downloadInfo.setArtist(artist);
		downloadInfo.setLrcUrl(lrcUrl);
		if (savedir == null) {
			downloadInfo.setFileSavePath(Globals.mSavePath + File.separator + downloadInfo.getFileName());
		} else {
			downloadInfo.setFileSavePath(savedir);
		}
		downloadInfo.setBitrate("128");
		downloadInfo.setTitle(title);
		LogUtil.d(TAG, "downloadInfo:"+downloadInfo.toString());
		return downloadInfo;
	}

	public HashMap<Long, DownloadInfo> getDownloadingMapData() {
		synchronized (this) {
			return mHashMap;
		}
	}

	public DownloadInfo getDownloadInfoById(long id) {
		if (mdb != null) {
			try {
				return mdb.queryItem(null, id);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

	public int getDownloadStatusById(long id) {
		int status = 0;

		if (id < 0) {
			status = mdb.getDownloadStatusById(id);
			if (status < 0) {
				status = 0;
			}
			return status;
		}

		return status;
	}

	public static boolean hasNetwork() {
		return HBMusicUtil.isNetWorkActive(mContext);
	}

	public int getDownloadInfoDataCount() {
		if (mHashMap != null) {
			return mHashMap.size();
		}

		return 0;
	}

	public void setDownloadListener(Long id, final DownloadStatusListener callback) {
		if (map != null) {
			map.put(id, callback);
		}

		return;
	}

	public void pauseDownloadById(long id) throws Exception {
		if (mHashMap == null) {
			return;
		}

		DownloadTask task = null;
		DownloadInfo info = null;
		info = mHashMap.get(id);
		if (info != null) {
			task = info.getTask();
		}

		if (task != null && !task.isCancelled()) {
			// LogUtil.iv(TAG, "pauseDownloadById 1   id:"+id);
			task.pauseTask();
			info.setTask(null);
		} else {
			// LogUtil.iv(TAG, "pauseDownloadById 2   id:"+id);
			if (info != null && info.isWaiting()) {
				// LogUtil.iv(TAG, "pauseDownloadById 3   id:"+id);
				info.setState(DownloadTask.State.PAUSE);
			}
		}

		if (mdb != null) {
			mdb.savaOrUpdate(info);
		}

		if (map != null && info != null && id >= 0) {
			map.remove(id);
		}
	}

	public void pauseAllDownload() throws Exception {
		if (mHashMap == null) {
			return;
		}

		Iterator iter = mHashMap.entrySet().iterator();
		List<DownloadInfo> mList = null;
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			DownloadInfo data = (DownloadInfo) entry.getValue();
			if (data != null) {

				DownloadTask task = data.getTask();
				if (task != null && !task.isCancelled()) {
					task.pauseTask();
					data.setTask(null);
				} else {
					if (data.isWaiting()) {
						data.setState(DownloadTask.State.PAUSE);
					}
				}

				if (mList == null) {
					mList = new ArrayList<DownloadInfo>();
				}
				// LogUtil.iv(TAG, "tttt 7.2 data:"+data.getState());
				mList.add(data);
				// mdb.savaOrUpdate(data);

				if (map != null && data != null && data.getId() >= 0) {
					map.remove(data.getId());
				}
			}

		}

		updateAllDb(mList);
	}

	private byte[] mblock = new byte[0];

	private void updateAllDb(List<DownloadInfo> list) {
		if (list == null) {
			return;
		}

		synchronized (mblock) {
			if (mdb != null) {
				mdb.updateAll(list);
			}
		}

		return;
	}

	public void resumeDownloadById(long id) {
		if (mHashMap == null) {
			return;
		}

		LogUtil.iv(TAG, "resumeDownloadById- 1 id:" + id + ",mList.size():" + mHashMap.size());
		if (mdb == null) {
			mdb = Dao.getInstance(mContext);
		}

		DownloadInfo downloadInfo = null;
		downloadInfo = mHashMap.get(id);

		LogUtil.iv(TAG, "resumeDownloadById- 2.2 ok:" + downloadInfo.toString());
		if (downloadInfo != null) {
			DownloadTask task = downloadInfo.getTask();
			if (downloadInfo.getTitle() != null && downloadInfo.getArtist() != null) {
				String name = downloadInfo.getTitle().replaceAll("/", "-");
				String artistName = downloadInfo.getArtist().replaceAll("/", "-");
				downloadInfo.setFileName(name + "_" + artistName + ".mp3");
			}
			if (task != null && !task.isCancelled()) {
				LogUtil.iv(TAG, "resumeDownloadById- 2.3 ok:");
				task.cancelTaskRunOnly();
			}
		}

		DownloadTask task = new DownloadTask(downloadInfo, mdb, mListener, mContext,handler);
		downloadInfo.setTask(task);
		downloadInfo.setState(DownloadTask.State.WAITING);
		task.executeOnExecutor(DownloadAsyncTask.DUAL_THREAD_EXECUTOR);

		mdb.savaOrUpdate(downloadInfo);
		LogUtil.iv(TAG, "resumeDownloadById- 2.5 ok:");

		return;
	}

	// private long time5 = 0;
	public void resumeAllDownloadAndListener(List<DownloadInfo> list, final HashMap<Long, DownloadStatusListener> callbacks) {
		if (list == null || (list != null && list.size() == 0)) {
			return;
		}

		List<DownloadInfo> list2 = new ArrayList<DownloadInfo>();
		for (int i = 0; i < list.size(); i++) {
			long id = list.get(i).getId();
			resumeOneDownload(id, list2);
			if (map != null) {
				map.put(id, callbacks.get(id));
			}
		}

		// time5 = System.currentTimeMillis();
		updateAllDb(list2);
		// LogUtil.iv(TAG,
		// "tttt 4 time5:"+(System.currentTimeMillis()-time5));

		return;
	}

	// private long time4 = 0;
	private void resumeOneDownload(long id, List<DownloadInfo> list) {
		if (mHashMap == null) {
			return;
		}

		// LogUtil.iv(TAG,
		// "resumeOneDownload- 1 id:"+id+",mList.size():"+mHashMap.size());
		if (mdb == null) {
			mdb = Dao.getInstance(mContext);
		}

		DownloadInfo downloadInfo = null;
		downloadInfo = mHashMap.get(id);

		// LogUtil.iv(TAG,
		// "resumeOneDownload- 2.2 ok:"+downloadInfo.toString());
		if (downloadInfo != null) {
			DownloadTask task = downloadInfo.getTask();
			if (task != null && !task.isCancelled()) {
				// LogUtil.iv(TAG, "resumeOneDownload- 2.3 ok:");
				task.cancelTaskRunOnly();
			}
		}

		DownloadTask task = new DownloadTask(downloadInfo, mdb, mListener, mContext,handler);
		downloadInfo.setTask(task);
		downloadInfo.setState(DownloadTask.State.WAITING);
		task.executeOnExecutor(DownloadAsyncTask.DUAL_THREAD_EXECUTOR);
		// mdb.savaOrUpdate(downloadInfo);
		if (list != null) {
			list.add(downloadInfo);
		}
		// LogUtil.iv(TAG, "resumeOneDownload- 2.5 ok:");
		// LogUtil.iv(TAG,
		// "tttt 5 time5:"+(System.currentTimeMillis()-time5));

		return;
	}

	private void resumeAllDownload() throws Exception {
		if (mHashMap == null) {
			return;
		}

		LogUtil.iv(TAG, "resumeAllDownload- 1 mList.size():" + mHashMap.size());

		Iterator iter = mHashMap.entrySet().iterator();
		while (iter.hasNext()) {

			Map.Entry entry = (Map.Entry) iter.next();
			DownloadInfo downloadInfo = (DownloadInfo) entry.getValue();
			if (downloadInfo != null) {
				DownloadTask oldtask = downloadInfo.getTask();
				if (oldtask != null && !oldtask.isCancelled()) {
					LogUtil.iv(TAG, "resumeAllDownload- 2:");
					oldtask.cancelTaskRunOnly();
				}

				DownloadTask task = new DownloadTask(downloadInfo, mdb, mListener, mContext,handler);
				downloadInfo.setTask(task);
				downloadInfo.setState(DownloadTask.State.WAITING);

				task.executeOnExecutor(DownloadAsyncTask.DUAL_THREAD_EXECUTOR);
				if (mdb != null) {
					mdb.savaOrUpdate(downloadInfo);
				}
			}
		}
	}

	public void removeDownloadById(long id) throws Exception {
		if (id < 0) {
			return;
		}

		if (mHashMap == null) {
			return;
		}

		LogUtil.iv(TAG, "removeDownloadById- 1 id:" + id + ",mList.size():" + mHashMap.size());

		DownloadInfo downloadInfo = null;
		downloadInfo = mHashMap.get(id);
		removeDownload(downloadInfo);

		return;

	}


	public void removeDownloadByPath(String path) {
		if (TextUtils.isEmpty(path)) {
			return;
		}
		if (mdb != null) {
			try {
				long id = mdb.deleteItemByFilePath(path);
				if (id > 0) {
					mHashMap.remove(id);
				}
			} catch (Exception e) {
				return;
			}
		}
	}


	private void removeDownload(final DownloadInfo downloadInfo) throws Exception {
		if (downloadInfo == null) {
			return;
		}

		DownloadTask task = downloadInfo.getTask();
		if (task != null && !task.isCancelled()) {
			task.cancelTask();
		}

		if (map != null) {
			map.remove(downloadInfo.getId());
		}

		if (mdb != null) {
			try {
				mdb.delete(null, downloadInfo.getId());
			} catch (Exception e) {
			}
		}
		mHashMap.remove(downloadInfo.getId());

		new Thread() {
			@Override
			public void run() {
				if (downloadInfo != null) {
					String fileName = downloadInfo.getFileName();
					if (!TextUtils.isEmpty(fileName)) {
						File saveFile = new File(Globals.mSavePath, fileName);
						DownloadFileUtil.deleteFile(saveFile);
					}
				}
			}
		}.start();
	}

	public void removeAllDownload() throws Exception {
		Iterator iter = mHashMap.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			DownloadInfo downloadInfo = (DownloadInfo) entry.getValue();
			removeDownload(downloadInfo);
		}
	}

	public void clearListenerMap() {
		if (map != null) {
			map.clear();
		}
		return;
	}

	public void backupDownloadInfoList() throws Exception {
	}

	public int getMaxDownloadThread() {
		return maxDownloadThread;
	}

	public void setMaxDownloadThread(int maxDownloadThread) {
		this.maxDownloadThread = maxDownloadThread;
	}

	private static void notifyToScanDir() {
		if (mContext == null) {
			return;
		}

		LogUtil.iv(TAG, "notifyToScanDir  ");

		Intent intent = new Intent();
		//intent.setAction(HBMediaPlayHome.ACTION_DIR_SCAN);
		intent.setData(Uri.fromFile(new File(Globals.mSavePath)));
		mContext.sendBroadcast(intent);
	}
	
	

	private final DownloadStatusListener mListener = new DownloadStatusListener() {

		@Override
		public void onDownload(String url, long id, int status, long downloadSize, long fileSize) {
			// LogUtil.iv(TAG, "gggg onDownload- 1 id:"+ id
			// +",url:"+url+",status:"+status+",downloadSize:"+downloadSize+",fileSize:"+fileSize);
			if (map != null && id >= 0) {

				final DownloadStatusListener callback = (DownloadStatusListener) map.get(id);
				if (status == State.SUCCESS.value()) {
//					notifyToScanDir();
				}

				if (callback != null) {
					callback.onDownload(url, id, status, downloadSize, fileSize);
				}
			}
		}
	};

	public boolean isDownloading() {
		if (mHashMap == null) {
			return false;
		}
		Iterator<DownloadInfo> iterator = mHashMap.values().iterator();
		while (iterator.hasNext()) {
			DownloadInfo info = iterator.next();
			if (info.isDownloading()) {
				return true;
			}
		}
		return false;
	}
}

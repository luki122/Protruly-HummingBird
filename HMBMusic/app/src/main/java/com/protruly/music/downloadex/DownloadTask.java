package com.protruly.music.downloadex;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.ConnectTimeoutException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.widget.Toast;

import com.protruly.music.downloadex.db.Dao;
import com.protruly.music.model.XiaMiSdkUtils;
//import com.protruly.music.ui.HBMediaPlayHome;
import com.protruly.music.util.HBMusicUtil;
import com.protruly.music.util.Globals;
import com.protruly.music.util.LogUtil;
import com.protruly.music.Application;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.xiami.sdk.entities.OnlineSong;
import com.xiami.sdk.utils.ImageUtil;

public class DownloadTask extends DownloadAsyncTask<Object, Object, DownloadTask.State> {

	private static final String TAG = "DownloadTask";

	private State state = State.WAITING;		                 // 当前状态
	private DownloadInfo mDownloadData = null;					 // 下载数据
	private DownloadStatusListener mListener;					 // 下载状态监听器
	private String mDownloadUrl;								 // 下载路径
	private long mId =  1;
	private long mFileSize = 0;					                 // 原始文件长度
	private long mDownloadSize = 0;					             // 已下载文件长度
	private String mFileName;									// 文件名
	private File mSaveFile;									     // 本地保存文件
	private long mCreateTime = 0;					            // 任务创建时间
	private Dao mDao = null;
	private boolean mCancleFlag = false;				         // 取消标识
	private boolean mNeedRetry = false;
	private boolean mRetred = false;				             // 是否已经连接重试
	private boolean mPreparePause = false;				          // 是否在准备阶段点下的暂停
	private Context mContext;
	private static final int mFrameLen = 4096;

	private File mCaheFile = null;				                  // 本地保存文件

	public enum State {
		WAITING(0), STARTED(1), LOADING(2), PAUSE(3), FAILURE(4), CANCELLED(5), CONNECTOUT(6), SUCCESS(7);

		private int value = 0;

		State(int value) {
			this.value = value;
		}

		public static State valueOf(int value) {
			switch (value) {
			case 0:
				return WAITING;
			case 1:
				return STARTED;
			case 2:
				return LOADING;
			case 3:
				return PAUSE;
			case 4:
				return FAILURE;
			case 5:
				return CANCELLED;
			case 6:
				return CONNECTOUT;
			case 7:
				return SUCCESS;

			default:
				return FAILURE;
			}
		}

		public int value() {
			return this.value;
		}
	}

	public DownloadTask(DownloadInfo downloadData, Dao dao, DownloadStatusListener listener, Context mContext, Handler handler) {
		initData(downloadData, dao, null, listener, mContext, handler);
	}

	private void initData(DownloadInfo downloadData, Dao dao, String fileSaveDir, DownloadStatusListener listener, Context mContext, Handler handler) {
		if (downloadData == null) {
			LogUtil.iv(TAG, "initData  fail 0:");
			return;
		}
		this.mContext = mContext;
		this.mDownloadData = downloadData;
		this.mListener = listener;
		this.mCancleFlag = false;
		mNeedRetry = false;
		mDownloadUrl = null;
		mId = downloadData.getId();
		this.handler = handler;
		this.mDao = dao;

		return;
	}

	private void updateDownloadInfo() {
		if (mDownloadData != null) {
			mDownloadData.setCreatetime(mCreateTime);
			mDownloadData.setProgress(mDownloadSize);
			mDownloadData.setFileLength(mFileSize);
			mDownloadData.setState(state);
			mDownloadData.setFileSavePath(Globals.mSavePath + Globals.SYSTEM_SEPARATOR + mFileName);
		}
		return;
	}

	private void createNewRecord() {

		mCreateTime = System.currentTimeMillis();
		mFileName = mDownloadData.getFileName();
		mSaveFile = null;
		mDownloadSize = 0;
		mFileSize = 0;
		mDownloadData.setFileSavePath(Globals.mSavePath + Globals.SYSTEM_SEPARATOR + mFileName);
		mDownloadData.setCreatetime(mCreateTime);
		mDownloadData.setProgress(0);
		mDownloadData.setFileLength(0);
		mDownloadData.setState(state);
		try {
			if (mDao != null) {
				mDao.savaOrUpdate(mDownloadData);
			}
		} catch (Exception e) {

		}
	}

	public State getState() {
		return state;
	}

	private void onPreData() {

		File savedir = new File(Globals.mSavePath);
		if (!savedir.exists()) {
			savedir.mkdirs();
		}
		if (savedir.getFreeSpace() <= 2 * Globals.HB_LOW_MEMORY) {
			((Application) mContext).setStorage(2);
			savedir = new File(Globals.mSavePath);
			if (!savedir.exists()) {
				savedir.mkdirs();
			}
		}
		if (mDao != null && mDownloadData != null) {
			try {// 记录存在
				if (mDao.isItemDownloaded(mDownloadData.getId())) {
					mFileName = mDownloadData.getFileName();
					if (mFileName != null && !TextUtils.isEmpty(mFileName)) {
						mSaveFile = new File(Globals.mSavePath, mFileName);
					}
					// 任务已存在，但是文件被删除
					if (mSaveFile == null || !mSaveFile.exists()) {
						LogUtil.iv(TAG, "initData  0.1:");
						mDao.delete(null, mId);
						createNewRecord();
					} else {
						DownloadInfo info = mDao.queryItem(null, mDownloadData.getId());
						mDownloadSize = info.getProgress();
						mCreateTime = info.getCreatetime();
						mFileSize = info.getFileLength();
						updateDownloadInfo();
					}
				} else {
					if (mDao.isItemExist(mDownloadData.getId())) {
						mFileName = mDownloadData.getFileName();
						if (mFileName != null && !TextUtils.isEmpty(mFileName)) {
							mSaveFile = new File(Globals.mSavePath, mFileName);
						}
						// 任务已存在，但是文件被删除
						if (mSaveFile == null || !mSaveFile.exists()) {
							mDao.delete(null, mId);
							createNewRecord();
						} else {
							DownloadInfo info = mDao.queryItem(null, mDownloadData.getId());
							mDownloadSize = info.getProgress();
							mCreateTime = info.getCreatetime();
							mFileSize = info.getFileLength();
							updateDownloadInfo();
						}
					} else {
						createNewRecord();
					}

				}
			} catch (Exception e) {
				LogUtil.e(TAG, "initData  fail ", e);

			}

		}
	}

	@Override
	protected State doInBackground(Object... params) {
		onPreData();
		return startDownload();
	}

	@Override
	protected void onPostExecute(State result) {
		if (result == State.SUCCESS) {
			if (mDownloadData != null) {
				mDownloadData.setTask(null);
			}
		}
	}

	private void upDataDownloadStatus() {
		synchronized (this) {
			if (mDownloadData != null) {
				if (mDao != null) {
					mDao.updateStatus(mDownloadData.getId(), state.value);
				}
				mDownloadData.setState(state);
			}
		}

		return;
	}

	/*
	 * private void copyFile(File oldfile, File newfile) {
	 * 
	 * InputStream inStream = null; FileOutputStream outStream = null; try { //
	 * int bytesum = 0; int byteread = 0; if (oldfile.exists()) { inStream = new
	 * FileInputStream(oldfile); outStream = new FileOutputStream(newfile);
	 * byte[] buffer = new byte[1024]; // int length; while ((byteread =
	 * inStream.read(buffer)) !=  1) { if (mCancleFlag) { LogUtil.iv(TAG,
	 * "copyFile cancel  :"); break; } outStream.write(buffer, 0,
	 * byteread); }
	 * 
	 * }
	 * 
	 * if (oldfile != null) { oldfile.delete(); } } catch (Exception e) {
	 * LogUtil.iv(TAG, "copyFile fail error:" + e.getMessage());
	 * e.printStackTrace(); } finally { try { if (inStream != null) {
	 * inStream.close(); } } catch (Exception e2) { // TODO: handle exception }
	 * 
	 * try { if (outStream != null) { outStream.flush(); outStream.close(); } }
	 * catch (Exception e2) { // TODO: handle exception } } }
	 */

	private static long getHeaderFieldLong(URLConnection conn, String field, long defaultValue) {
		try {
			return Long.parseLong(conn.getHeaderField(field));
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private Handler handler;

	private State startDownload() {
		boolean mNeedOnPreData=false;
		mNeedRetry = false;
		if (mCancleFlag)
			return state;

		if (state == State.CANCELLED || state == State.PAUSE)
			return state;

		lastUpdateTime = SystemClock.uptimeMillis();
		publishProgress(mDownloadUrl, mDownloadData.getId(), state.value, mDownloadSize, mFileSize, true);
		OnlineSong song = XiaMiSdkUtils.findSongByIdSync(mContext, mId, HBMusicUtil.getOnlineSongQuality());
		if (new File(Globals.mSavePath).getFreeSpace() <= 2 * Globals.HB_LOW_MEMORY) {
			LogUtil.e(TAG, " LOW MEMORY ");
			state = State.FAILURE;
			upDataDownloadStatus();
			publishProgress(mDownloadUrl, mDownloadData.getId(), state.value, mDownloadSize, mFileSize, true);
			handler.removeMessages(DownloadManager.MSG_TOAST);
			handler.sendEmptyMessageDelayed(DownloadManager.MSG_TOAST, 1000);
			return state;
		}
		if (song == null) {
			LogUtil.e(TAG, "song is null not downloading ");
			state = State.FAILURE;
			upDataDownloadStatus();
			publishProgress(mDownloadUrl, mDownloadData.getId(), state.value, mDownloadSize, mFileSize, true);
			return state;
		}
		mDownloadUrl = song.getListenFile();
		// 如果是在准备阶段点下的暂停, 则不进行下载
		if (mPreparePause) {
			mPreparePause = false;
			state = State.PAUSE;
			upDataDownloadStatus();
			return state;
		}
		state = State.STARTED;
		upDataDownloadStatus();

		if (mSaveFile == null) {
			mSaveFile = new File(Globals.mSavePath, mFileName);
		}

		if (!mSaveFile.exists()) {
			try {
				mSaveFile.createNewFile();
			} catch (IOException e) {
				LogUtil.e(TAG, " mSaveFile is not exists");
			}
		}
		File cacheDir = new File(Globals.mCachePath);
		if (!cacheDir.exists()) {
			cacheDir.mkdirs();
		}
		if (mCaheFile == null) {
			mCaheFile = new File(Globals.mCachePath, HBMusicUtil.MD5(mFileName));
		}

		if (!mCaheFile.exists()) {
			try {
				mCaheFile.createNewFile();
			} catch (IOException e) {
				LogUtil.e(TAG, " mCaheFile is not exists");
			}
		}

		RandomAccessFile accessFile = null;
		URL url = null;
		HttpURLConnection http = null;
		InputStream inputStream = null;
		try {
			url = new URL(mDownloadUrl);
			http = (HttpURLConnection) url.openConnection();
			http.setConnectTimeout(8 * 1000);
			// http.setDoOutput(true);
			// http.setRequestMethod("POST");
			http.setRequestProperty("Range", "bytes=" + mDownloadSize + " ");
			http.connect();
			int code = http.getResponseCode();

			accessFile = new RandomAccessFile(mCaheFile, "rw");
			accessFile.seek(mDownloadSize);
			LogUtil.d(TAG, "HBListItem startDownload code:" + code);
			// 加入断点下载返回码为206
			if (code == HttpStatus.SC_OK || code == HttpStatus.SC_PARTIAL_CONTENT) {
				if (mFileSize <= 0) {
					mFileSize = getHeaderFieldLong(http, "Content Length",  1);
				}
				if (mDao != null) {
					mDao.updateFileSize(mDownloadData.getId(), mFileSize);
				}
				
				if(new File(Globals.mSavePath).getFreeSpace()+mFileSize<=Globals.HB_LOW_MEMORY){
					LogUtil.d(TAG, "HBListItem throw new IOException");
					 throw new IOException("LOW_MEMORY");
				}

				if (mDownloadData != null) {
					mDownloadData.setFileLength(mFileSize);
				}
				LogUtil.d(TAG, "HBListItem startDownload mDownloadSize:" + mDownloadSize + ",mFileSize:" + mFileSize);

				inputStream = http.getInputStream();
				byte[] buffer = new byte[mFrameLen];
				int offset = 0;
				// 计算下载速度
				while ((offset = inputStream.read(buffer)) !=  1) {
					if (mCancleFlag) {
						publishProgress(mDownloadUrl, mDownloadData.getId(), state.value, mDownloadSize, mFileSize, false);
						break;
					}

					if (state == State.CANCELLED || state == State.CONNECTOUT) {
						publishProgress(mDownloadUrl, mDownloadData.getId(), state.value, mDownloadSize, mFileSize, false);
						break;
					}

					if (state != State.LOADING) {
						state = State.LOADING;
						setRetred(false);
						upDataDownloadStatus();
					}

					if (mDownloadSize >= mFileSize) {
						publishProgress(mDownloadUrl, mDownloadData.getId(), state.value, mDownloadSize, mFileSize, false);
						break;
					}

					accessFile.write(buffer, 0, offset);
					mDownloadSize += offset;
					upDownloadingProgress(mDownloadData.getId(), mDownloadSize);
					publishProgress(mDownloadUrl, mDownloadData.getId(), state.value, mDownloadSize, mFileSize, false);
				}

				if (!mCancleFlag && state == State.LOADING) {
					downloadLrcOrImg(song);
				}

			} else {
				if (DownloadManager.hasNetwork()) {
					LogUtil.e(TAG, " downloading FAILURE no network ");
					state = State.FAILURE;
				} else {
					state = State.CONNECTOUT;
				}

				upDataDownloadStatus();
			}
		} catch (ClientProtocolException e) {
			LogUtil.e(TAG, "startDownload  downloading FAILURE ", e);
		} catch (IOException e) {
			e.printStackTrace();
			if (e instanceof ConnectTimeoutException) {
				LogUtil.e(TAG, "ConnectTimeoutException fail   e:", e);
				state = State.CONNECTOUT;
				if (!mRetred) {
					mRetred = true;

					state = State.LOADING;
					mNeedRetry = true;
				} else {
					setRetred(false);
					state = State.FAILURE;
				}
				upDataDownloadStatus();
			} /*else if (e instanceof IOException) {
				if (DownloadManager.hasNetwork()) {
					//内存满 需要重新换存储
					LogUtil.d(TAG, "HBListItemHBListItem delete mSaveFile:"+mSaveFile.getAbsolutePath());
					mSaveFile.delete();
					mCaheFile.delete();
					onPreData();
				}

			}*/ else {
				if (DownloadManager.hasNetwork()) {
					if (!mRetred) {
						mRetred = true;
						mNeedRetry = true;
					} else {
						setRetred(false);
						state = State.FAILURE;
					}

					upDataDownloadStatus();
				} else {
					state = State.CONNECTOUT;
					upDataDownloadStatus();
				}
				if(e instanceof IOException){
					//内存满 需要重新换存储
					LogUtil.d(TAG, "HBListItemHBListItem delete mSaveFile:"+mSaveFile.getAbsolutePath());
					mSaveFile.delete();
					mCaheFile.delete();
//					onPreData();
					mNeedOnPreData= true;
				}
			}
		} finally {
			if (http != null) {
				http.disconnect();
			}

			try {
				if (accessFile != null) {
					accessFile.close();
				}
				if (inputStream != null) {
					inputStream.close();
				}

			} catch (IOException e) {
				LogUtil.e(TAG, "IOException e:", e);
			}
		}

		if (mNeedRetry) {
			LogUtil.d(TAG, "startDownload mNeedRetry:" + mNeedRetry+" state:"+state);
			if(mNeedOnPreData){
				onPreData();
			}
			startDownload();
			return state;
		}

		// 如果已经下载完成, 删除正在下载数据库中数据
		if (mDownloadSize >= mFileSize) {
			Map<String, String> map = new HashMap<String, String>();
			map.put("TITLE", mDownloadData.getTitle());
			map.put("ARTIST", mDownloadData.getArtist());
			map.put("ALBUM", mDownloadData.getAlbum());
			map.put("SINGER", mDownloadData.getArtist());

			LogUtil.d(TAG, "HBListItemHBListItemmDownloadData:" + mDownloadData.toString());
			if (mCaheFile.exists()&&mSaveFile.exists()) {
				if(!mCaheFile.renameTo(mSaveFile)){
					LogUtil.d(TAG, "HBListItemHBListItemrenameTo copyFile error");
				}
				// copyFile(mCaheFile, mSaveFile);
				boolean ret = XiaMiSdkUtils.writeFileTags(mContext, mSaveFile.getAbsolutePath(), map, null, null);
				// boolean ret= DownloadManager.getXiamiSDKInstance().writeFileTags(
				// inFilePath, outFilePath, map, null, null);
				
				LogUtil.d(TAG, "HBListItem writeFileTags " + ret);
				if (song != null) {
					mDao.updateFileInfo(mDownloadData.getId(), mFileName.substring(0, mFileName.lastIndexOf(".")), song.getListenFile(), song.getLyric());
				} else {
					mDao.updateFileName(mDownloadData.getId(), mFileName.substring(0, mFileName.lastIndexOf(".")));
				}
				state = State.SUCCESS;
			}
		}

		upDataDownloadStatus();
		upDownloadingProgress(mDownloadData.getId(), mDownloadSize);
		publishProgress(mDownloadUrl, mDownloadData.getId(), state.value, mDownloadSize, mFileSize, true);
		if (state == State.SUCCESS) {
			notifyToScanFile(mSaveFile);
		}
		return state;
	}

	/**
	 * 文件扫描加快速度
	 * @param file
	 */
	private void notifyToScanFile(File file) {
		if (file == null || mContext == null) {
			return;
		}
		Intent intent = new Intent();
		//intent.setAction(HBMediaPlayHome.ACTION_FILE_SCAN);
		intent.setData(Uri.fromFile(file));
		mContext.sendBroadcast(intent);
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("DefaultLocale")
	private void downloadLrcOrImg(OnlineSong onlineSong) {
		String lrcUrl = "";
		if (onlineSong != null) {
			lrcUrl = onlineSong.getLyric();
			LogUtil.d(TAG, "findSongByIdSynclrcUrl:" + lrcUrl);
		}
		if (TextUtils.isEmpty(mDownloadData.getImgUrl())) {
			mDownloadData.setImgUrl(onlineSong.getImageUrl());
		}
		int count = 0;
		Bitmap bm = null;
		while (bm == null && count < 3) {
			bm = ImageLoader.getInstance().loadImageSync(ImageUtil.transferImgUrl(mDownloadData.getImgUrl(), 330));
			count++;
		}
		saveBitmap(bm);

		if (TextUtils.isEmpty(lrcUrl)) {
			LogUtil.e(TAG, "downloadLrc lrcUrl is null");
			return;
		}

		URL url = null;
		HttpURLConnection http = null;
		InputStream inputStream = null;
		File dirFile = new File(Globals.mLycPath);
		if (!dirFile.exists()) {
			dirFile.mkdirs();
		}
		String ext = HBMusicUtil.getExtFromFilename(lrcUrl).toLowerCase();
		if (TextUtils.isEmpty(ext)) {
			ext = "lrc";
		}

		String name = onlineSong.getSongName() + "_" + onlineSong.getArtistName() + "." + ext;
		try {
			if (!onlineSong.getArtistName().trim().equalsIgnoreCase(onlineSong.getSingers().trim())) {
				name = onlineSong.getSongName() + "_" + onlineSong.getArtistName() + "_" + onlineSong.getSingers() + "." + ext;
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		final String filepath = Globals.mLycPath + File.separator + name;
		LogUtil.d(TAG, " filepath:" + filepath);
		FileOutputStream outputStream = null;
		try {
			outputStream = new FileOutputStream(filepath);
			url = new URL(lrcUrl);
			http = (HttpURLConnection) url.openConnection();
			http.setConnectTimeout(8 * 1000);
			http.connect();
			int code = http.getResponseCode();
			int offset = 0;
			if (code == HttpStatus.SC_OK) {
				inputStream = http.getInputStream();
				byte[] buffer = new byte[4096];
				while ((offset = inputStream.read(buffer)) !=  1) {
					outputStream.write(buffer, 0, offset);
				}
			}
		} catch (MalformedURLException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		} finally {
			if (http != null) {
				http.disconnect();
			}
			try {
				if (outputStream != null)
					outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				if (inputStream != null)
					inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void saveBitmap(Bitmap bm) {
		if (bm != null && mDownloadData != null) {
			File dirFile = new File(Globals.mSongImagePath);
			if (!dirFile.exists()) {
				dirFile.mkdirs();
			}
			File file = new File(Globals.mSongImagePath + File.separator + HBMusicUtil.MD5(mDownloadData.getTitle().trim() + mDownloadData.getArtist().trim() + mDownloadData.getAlbum().trim()));
			if (file.exists()) {
				file.delete();
			}

			FileOutputStream out = null;
			try {
				out = new FileOutputStream(file);
				bm.compress(Bitmap.CompressFormat.PNG, 100, out);
			} catch (Exception e) {
				LogUtil.iv(TAG, "saveBitmap fail ");
				e.printStackTrace();
			} finally {
				if (out != null) {
					try {
						out.flush();
					} catch (Exception e2) {
					}

					try {
						out.close();
					} catch (Exception e2) {
					}

					out = null;
				}
			}
		}

		return;
	}

	private void upDownloadingProgress(long id, long size) {
		try {
			if (mDownloadData != null) {
				if (mDao != null) {
					mDao.updateDownloadSize(id, size);
				}
				mDownloadData.setProgress(mDownloadSize);
			}
		} catch (Exception e) {
			LogUtil.e(TAG, "HBListItemupDownloadingProgress fail ", e);
		}

		return;
	}

	@Override
	protected void onProgressUpdate(Object... values) {
		// super.onProgressUpdate(values); State.SUCCESS
		updateProgress(String.valueOf(values[0]), Long.valueOf(String.valueOf(values[1])), Integer.valueOf(String.valueOf(values[2])), Long.valueOf(String.valueOf(values[3])),
				Long.valueOf(String.valueOf(values[4])), Boolean.parseBoolean(String.valueOf(values[5])));
		if (Integer.valueOf(String.valueOf(values[2])) == 7) {
			if (mCaheFile != null && mCaheFile.exists()) {
				LogUtil.d(TAG, "onProgressUpdate HBListItem  delete cahe file mCaheFile:" + mCaheFile.getTotalSpace() + " path:" + mCaheFile.getAbsolutePath());
				mCaheFile.delete();
			}
		}
	}

	private long lastUpdateTime;
	private static final int DEFAULT_RATE = 1000;

	private void updateProgress(String url, long id, int status, long current, long total, boolean forceUpdateUI) {
		if (mListener != null /* && this.state != State.CANCELLED */) {
			if (forceUpdateUI) {
				mListener.onDownload(url, id, status, current, total);
			} else {
				long currTime = SystemClock.uptimeMillis();
				if (currTime - lastUpdateTime >= DEFAULT_RATE && !mCancleFlag) {
					lastUpdateTime = currTime;
					mListener.onDownload(url, id, status, current, total);
				}
			}
		}

		return;
	}

	/**
	 * 设置是否已经连接重试
	 * @param retred
	 */
	public void setRetred(boolean retred) {
		this.mRetred = retred;
	}

	public void pauseTask() {
		if (state == State.WAITING || state == State.STARTED) {
			mPreparePause = true;
		}

		if (state == State.STARTED || state == State.LOADING || state == State.WAITING) {
			this.state = State.PAUSE;
		}

		this.mCancleFlag = true;

		if (!this.isCancelled()) {
			try {
				this.cancel(true);
			} catch (Throwable e) {
			}
		}
		upDataDownloadStatusEx();

		return;
	}

	private void upDataDownloadStatusEx() {
		synchronized (this) {
			if (mDownloadData != null) {
				mDownloadData.setState(state);
			}
		}

		return;
	}

	public void cancelTaskRunOnly() {
		this.mCancleFlag = true;

		if (!this.isCancelled()) {
			try {
				this.cancel(true);
			} catch (Throwable e) {
			}
		}

		return;
	}

	public void cancelTask() {
		this.state = State.CANCELLED;
		this.mCancleFlag = true;
		upDataDownloadStatus();
		if (!this.isCancelled()) {
			try {
				this.cancel(true);
			} catch (Throwable e) {
			}
		}
		try {
			if (mDao != null) {
				mDao.delete(null, mId);
			}
			DownloadFileUtil.deleteFile(mSaveFile);
		} catch (Exception e) {
			// TODO: handle exception
		}

		return;
	}

}

package com.hb.thememanager.http.downloader;

import android.content.Context;
import android.util.Log;

import com.hb.thememanager.http.downloader.config.DownloadConfig;
import com.hb.thememanager.http.downloader.core.DownloadResponse;
import com.hb.thememanager.http.downloader.core.DownloadResponseImpl;
import com.hb.thememanager.http.downloader.core.DownloadTaskImpl;
import com.hb.thememanager.http.downloader.core.task.DownloadTask;
import com.hb.thememanager.http.downloader.db.DefaultDownloadDBController;
import com.hb.thememanager.http.downloader.db.DownloadDBController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public final class DownloadManagerImpl implements DownloadManager, DownloadTaskImpl.DownloadTaskListener {

  private static final int MIN_EXECUTE_INTERVAL = 500;
  private static DownloadManagerImpl sInstance;
  private final ExecutorService mExecutorService;
  private final ConcurrentHashMap<Integer, DownloadTask> mCacheDownloadTask;
  private final List<DownloadInfo> mDownloadingCaches;
  private final Context mContext;

  private final DownloadResponse mDownloadResponse;
  private final DownloadDBController mDownloadDBController;
  private final DownloadConfig mDownloadConfig;
  private long mLastExecuteTime;

  private DownloadManagerImpl(Context context, DownloadConfig downloadConfig) {
    this.mContext = context.getApplicationContext();
    if (downloadConfig == null) {
      this.mDownloadConfig = new DownloadConfig();
    } else {
      this.mDownloadConfig = downloadConfig;
    }


    if (mDownloadConfig.getDownloadDBController() == null) {
      mDownloadDBController = new DefaultDownloadDBController(context, this.mDownloadConfig);
    } else {
      mDownloadDBController = downloadConfig.getDownloadDBController();
    }

    if (mDownloadDBController.findAllDownloading() == null) {
      mDownloadingCaches = new ArrayList<>();
    } else {
      mDownloadingCaches = mDownloadDBController.findAllDownloading();
    }

    mCacheDownloadTask = new ConcurrentHashMap<>();

    mDownloadDBController.pauseAllDownloading();

    mExecutorService = Executors.newFixedThreadPool(this.mDownloadConfig.getDownloadThread());

    mDownloadResponse = new DownloadResponseImpl(mDownloadDBController);
  }

  public static DownloadManager getInstance(Context context, DownloadConfig downloadConfig) {
    synchronized (DownloadManagerImpl.class) {
      if (sInstance == null) {
        sInstance = new DownloadManagerImpl(context.getApplicationContext(), downloadConfig);
      }
    }
    return sInstance;
  }


  @Override
  public void download(DownloadInfo downloadInfo) {
    mDownloadingCaches.add(downloadInfo);
    prepareDownload(downloadInfo);
  }

  private void prepareDownload(DownloadInfo downloadInfo) {
    if (mCacheDownloadTask.size() >= mDownloadConfig.getDownloadThread()) {
      downloadInfo.setStatus(DownloadInfo.STATUS_WAIT);
      mDownloadResponse.onStatusChanged(downloadInfo);
    } else {
      DownloadTaskImpl downloadTask = new DownloadTaskImpl(mExecutorService, mDownloadResponse,
          downloadInfo, mDownloadConfig, this);
      mCacheDownloadTask.put(downloadInfo.getId(), downloadTask);
      downloadInfo.setStatus(DownloadInfo.STATUS_PREPARE_DOWNLOAD);
      mDownloadResponse.onStatusChanged(downloadInfo);
      downloadTask.start();
    }
  }

  @Override
  public void pause(DownloadInfo downloadInfo) {
    if (isExecute()) {
      downloadInfo.setStatus(DownloadInfo.STATUS_PAUSED);
      mCacheDownloadTask.remove(downloadInfo.getId());
      mDownloadResponse.onStatusChanged(downloadInfo);
      prepareDownloadNextTask();
    }
  }

  private void prepareDownloadNextTask() {
    for (DownloadInfo downloadInfo : mDownloadingCaches) {
      if (downloadInfo.getStatus() == DownloadInfo.STATUS_WAIT) {
        prepareDownload(downloadInfo);
        break;
      }
    }
  }

  @Override
  public void resume(DownloadInfo downloadInfo) {
    if (isExecute()) {
      mCacheDownloadTask.remove(downloadInfo.getId());
      prepareDownload(downloadInfo);
    }
  }

  @Override
  public void remove(DownloadInfo downloadInfo) {
    downloadInfo.setStatus(DownloadInfo.STATUS_REMOVED);
    mCacheDownloadTask.remove(downloadInfo.getId());
    mDownloadingCaches.remove(downloadInfo);
    mDownloadDBController.delete(downloadInfo);
    mDownloadResponse.onStatusChanged(downloadInfo);
  }

  @Override
  public void onDestroy() {

  }

  @Override
  public DownloadInfo getDownloadById(int id) {
    DownloadInfo downloadInfo = null;
    for (DownloadInfo d : mDownloadingCaches) {
      if (d.getId() == id) {
        downloadInfo = d;
        break;
      }
    }

    if (downloadInfo == null) {
      downloadInfo = mDownloadDBController.findDownloadedInfoById(id);
    }
    return downloadInfo;
  }

  @Override
  public DownloadInfo getDownloadByPath(String path) {
	    DownloadInfo downloadInfo = null;
	    for (DownloadInfo d : mDownloadingCaches) {
	      if (d.getPath() == path) {
	        downloadInfo = d;
	        break;
	      }
	    }

	    if (downloadInfo == null) {
	      downloadInfo = mDownloadDBController.findDownloadedInfoByPath(path);
	    }
	    return downloadInfo;
  }

@Override
  public List<DownloadInfo> findAllDownloading() {
    return mDownloadingCaches;
  }

  @Override
  public List<DownloadInfo> findAllDownloaded() {
    return mDownloadDBController.findAllDownloaded();
  }

  public DownloadDBController getmDownloadDBController() {
    return mDownloadDBController;
  }

  @Override
  public void onDownloadSuccess(DownloadInfo downloadInfo) {
    mCacheDownloadTask.remove(downloadInfo.getId());
    mDownloadingCaches.remove(downloadInfo);
    prepareDownloadNextTask();
  }

  public boolean isExecute() {
    if (System.currentTimeMillis() - mLastExecuteTime > MIN_EXECUTE_INTERVAL) {
      mLastExecuteTime = System.currentTimeMillis();
      return true;
    }
    return false;
  }


}

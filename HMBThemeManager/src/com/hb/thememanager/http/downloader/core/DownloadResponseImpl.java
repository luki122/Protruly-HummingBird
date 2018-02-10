package com.hb.thememanager.http.downloader.core;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.hb.thememanager.http.downloader.core.DownloadResponse;
import com.hb.thememanager.http.downloader.db.DownloadDBController;
import com.hb.thememanager.http.downloader.DownloadInfo;
import com.hb.thememanager.http.downloader.DownloadThreadInfo;
import com.hb.thememanager.http.downloader.exception.DownloadException;
import com.hb.thememanager.utils.TLog;


public class DownloadResponseImpl implements DownloadResponse {

  private static final String TAG = "DownloadResponseImpl";
  private final Handler handler;
  private final DownloadDBController downloadDBController;

  public DownloadResponseImpl(DownloadDBController downloadDBController) {
    this.downloadDBController = downloadDBController;

    handler = new Handler(Looper.getMainLooper()) {
      @Override
      public void handleMessage(Message msg) {
        super.handleMessage(msg);
        DownloadInfo downloadInfo = (DownloadInfo) msg.obj;
        switch (downloadInfo.getStatus()) {

          case DownloadInfo.STATUS_DOWNLOADING:
            if (downloadInfo.getDownloadListener() != null) {
              downloadInfo.getDownloadListener()
                  .onDownloading(downloadInfo.getProgress(), downloadInfo.getSize());
            }

            break;
          case DownloadInfo.STATUS_PREPARE_DOWNLOAD:
            if (downloadInfo.getDownloadListener() != null) {
              downloadInfo.getDownloadListener().onStart();
            }
            break;
          case DownloadInfo.STATUS_WAIT:
            if (downloadInfo.getDownloadListener() != null) {
              downloadInfo.getDownloadListener().onWaited();
            }
            break;
          case DownloadInfo.STATUS_PAUSED:
            if (downloadInfo.getDownloadListener() != null) {
              downloadInfo.getDownloadListener().onPaused();
            }
            break;
          case DownloadInfo.STATUS_COMPLETED:
            if (downloadInfo.getDownloadListener() != null) {
              downloadInfo.getDownloadListener()
                      .onDownloading(downloadInfo.getProgress(), downloadInfo.getSize());
              downloadInfo.getDownloadListener().onDownloadSuccess();
            }
            //TODO submit next downloadInfo task

            break;
          case DownloadInfo.STATUS_ERROR:
            if (downloadInfo.getDownloadListener() != null) {
              downloadInfo.getDownloadListener().onDownloadFailed(downloadInfo.getException());
            }
            break;
          case DownloadInfo.STATUS_REMOVED:
            if (downloadInfo.getDownloadListener() != null) {
              downloadInfo.getDownloadListener().onRemoved();
            }
            break;
        }
      }
    };


  }

  @Override
  public void onStatusChanged(DownloadInfo downloadInfo) {
    if (downloadInfo.getStatus() != DownloadInfo.STATUS_REMOVED) {
      downloadDBController.createOrUpdate(downloadInfo);
      if (downloadInfo.getDownloadThreadInfos() != null) {
        for (DownloadThreadInfo threadInfo : downloadInfo.getDownloadThreadInfos()) {
          downloadDBController.createOrUpdate(threadInfo);
        }
      }
    }

    Message message = handler.obtainMessage(downloadInfo.getId());
    message.obj = downloadInfo;
    message.sendToTarget();

    TLog.d(TAG, "progress:" + downloadInfo.getProgress() + ",size:" + downloadInfo.getSize());
  }

  @Override
  public void handleException(DownloadException exception,DownloadInfo info) {
    info.setStatus(DownloadInfo.STATUS_ERROR);
    info.setException(exception);
    onStatusChanged(info);

  }
}

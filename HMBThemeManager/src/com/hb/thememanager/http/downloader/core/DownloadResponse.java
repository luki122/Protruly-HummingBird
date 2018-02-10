package com.hb.thememanager.http.downloader.core;


import com.hb.thememanager.http.downloader.DownloadInfo;
import com.hb.thememanager.http.downloader.exception.DownloadException;

public interface DownloadResponse {

  void onStatusChanged(DownloadInfo downloadInfo);

  void handleException(DownloadException exception,DownloadInfo info);
}

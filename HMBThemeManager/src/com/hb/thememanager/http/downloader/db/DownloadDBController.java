package com.hb.thememanager.http.downloader.db;

import com.hb.thememanager.http.downloader.DownloadInfo;
import com.hb.thememanager.http.downloader.DownloadThreadInfo;

import java.util.List;


public interface DownloadDBController {

  List<DownloadInfo> findAllDownloading();

  List<DownloadInfo> findAllDownloaded();

  DownloadInfo findDownloadedInfoById(int id);
  
  DownloadInfo findDownloadedInfoByPath(String savedFilePath);

  void pauseAllDownloading();

  void createOrUpdate(DownloadInfo downloadInfo);

  void createOrUpdate(DownloadThreadInfo downloadThreadInfo);

  void delete(DownloadInfo downloadInfo);

  void delete(DownloadThreadInfo download);
}

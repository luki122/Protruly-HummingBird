package com.hb.thememanager.http.downloader;

import com.hb.thememanager.http.downloader.db.DownloadDBController;
import com.hb.thememanager.http.downloader.DownloadInfo;

import java.util.List;

/**
 * 下载管理器，使用该类去实现下载，使用方法：
 * <pre>
 *     DownloadManager dm = DownloadService.getDownloadManager(mContext);
 *    DownloadInfo info = new DownloadInfo.Builder()
 *                        .setUrl("http://www.pp.com")
 *                        .setPath("sdcard/xxx/xxx")
 *                        .setId("0").build();
 *    info.setDownloadListener(new DownloadListener() {
 *     @Override
 *     public void onStart() {
 *
 *
 *
 *   }
 *
 *     @Override
 *     public void onWaited() {
 *
 *     }
 *
 *     @Override
 *     public void onPaused() {
 *
 *     }
 *
 *     @Override
 *     public void onDownloading(long progress, long size) {
 *
 *     }
 *
 *     @Override
 *     public void onRemoved() {
 *
 *     }
 *
 *     @Override
 *     public void onDownloadSuccess() {
 *
 *     }
 *
 *     @Override
 *     public void onDownloadFailed(DownloadException e) {
 *
 *     }
 *     });
 *  dm.download(info);
 *
 * </pre>
 */
public interface DownloadManager {

  void download(DownloadInfo downloadInfo);

  void pause(DownloadInfo downloadInfo);

  void resume(DownloadInfo downloadInfo);

  void remove(DownloadInfo downloadInfo);

  void onDestroy();

  DownloadInfo getDownloadById(int id);

  DownloadInfo getDownloadByPath(String path);
  
  List<DownloadInfo> findAllDownloading();

  List<DownloadInfo> findAllDownloaded();

  DownloadDBController getmDownloadDBController();

}

package com.hb.thememanager.http.downloader.core.thread;


import android.os.Process;
import android.util.Log;

import com.hb.thememanager.http.downloader.config.DownloadConfig;
import com.hb.thememanager.http.downloader.core.DownloadResponse;
import com.hb.thememanager.http.downloader.DownloadInfo;
import com.hb.thememanager.http.downloader.DownloadThreadInfo;
import com.hb.thememanager.http.downloader.exception.DownloadException;
import com.hb.thememanager.http.downloader.exception.DownloadPauseException;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.TLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

public class DownloadThread implements Runnable {

  public static final String TAG = "DownloadManager";
  private static final boolean DBG = Config.DEBUG;
  private final DownloadThreadInfo downloadThreadInfo;
  private final DownloadResponse downloadResponse;
  private final DownloadConfig downloadConfig;
  private final DownloadInfo downloadInfo;
  private final DownloadProgressListener downloadProgressListener;
  private long lastProgress;
  private InputStream inputStream;
  private int retryDownloadCount = 0;

  public DownloadThread(DownloadThreadInfo downloadThreadInfo, DownloadResponse downloadResponse,
      DownloadConfig downloadConfig,
      DownloadInfo downloadInfo, DownloadProgressListener downloadProgressListener) {
    this.downloadThreadInfo = downloadThreadInfo;
    this.downloadResponse = downloadResponse;
    this.downloadConfig = downloadConfig;
    this.downloadInfo = downloadInfo;
    this.lastProgress = downloadThreadInfo.getProgress();
    this.downloadProgressListener = downloadProgressListener;
  }

  @Override
  public void run() {
    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
    checkPause();
    try {
      executeDownload();
    } catch (DownloadException e) {

      downloadInfo.setStatus(DownloadInfo.STATUS_ERROR);
      downloadInfo.setException(e);
      downloadResponse.onStatusChanged(downloadInfo);
      downloadResponse.handleException(e,downloadInfo);
    }
  }

  private void executeDownload() {
    HttpURLConnection httpConnection = null;
    try {
      final URL url = new URL(downloadThreadInfo.getUri());
      httpConnection = (HttpURLConnection) url.openConnection();
      httpConnection.setConnectTimeout(downloadConfig.getConnectTimeout());
      httpConnection.setReadTimeout(downloadConfig.getReadTimeout());
      httpConnection.setRequestMethod(downloadConfig.getMethod());
      long lastStart = downloadThreadInfo.getStart() + lastProgress;
      if (downloadInfo.isSupportRanges()) {
        httpConnection.setRequestProperty("Range",
            "bytes=" + lastStart + "-" + downloadThreadInfo.getEnd());
      }
      final int responseCode = httpConnection.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_PARTIAL
          || responseCode == HttpURLConnection.HTTP_OK) {
        inputStream = httpConnection.getInputStream();
        RandomAccessFile raf = new RandomAccessFile(downloadInfo.getPath(), "rwd");

        raf.seek(lastStart);
        final byte[] bf = new byte[1024 * 4];
        int len = -1;
        int offset = 0;
        while (true) {
          checkPause();
          len = inputStream.read(bf);
          if (len == -1) {
            break;
          }
          raf.write(bf, 0, len);
          offset += len;

//          synchronized (downloadProgressListener) {
          downloadThreadInfo.setProgress(lastProgress + offset);
          downloadProgressListener.onProgress();
//          }
          if(DBG)
          Log.d(TAG,
              "downloadInfo:" + downloadInfo.getId() + " thread:" + downloadThreadInfo.getThreadId()
                  + " progress:"
                  + downloadThreadInfo.getProgress()
                  + ",start:" + downloadThreadInfo.getStart() + ",end:" + downloadThreadInfo
                  .getEnd());
        }

        //downloadInfo success
        downloadProgressListener.onDownloadSuccess();
      } else {
        throw new DownloadException(DownloadException.EXCEPTION_SERVER_SUPPORT_CODE,
            "UnSupported response code:" + responseCode);
      }
      checkPause();
    } catch (ProtocolException e) {
      TLog.e(TAG,"download Protocol error ->"+e);
      throw new DownloadException(DownloadException.EXCEPTION_PROTOCOL, "Protocol error", e);
    } catch (IOException e) {
      TLog.e(TAG,"download IO error ->"+e);
      throw new DownloadException(DownloadException.EXCEPTION_IO_EXCEPTION, "IO error", e);
    } catch (DownloadPauseException e) {
      //TODO process pause logic
    } catch (Exception e) {
      TLog.e(TAG,"download error->"+e);
      throw new DownloadException(DownloadException.EXCEPTION_OTHER, "other error", e);

    } finally {
      if (httpConnection != null) {
        httpConnection.disconnect();
      }
    }
  }

  private void checkPause() {
    if (downloadInfo.isPause()) {
      throw new DownloadPauseException(DownloadException.EXCEPTION_PAUSE);
    }
  }

  /**
   * Download thread progress listener.
   */
  public interface DownloadProgressListener {

    void onProgress();

    void onDownloadSuccess();
  }


}

package com.hb.thememanager.http.downloader.core.task;


import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.hb.thememanager.http.downloader.core.DownloadResponse;
import com.hb.thememanager.http.downloader.DownloadInfo;
import com.hb.thememanager.http.downloader.exception.DownloadException;
import com.hb.thememanager.utils.TLog;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;

import static com.hb.thememanager.http.downloader.exception.DownloadException.EXCEPTION_OTHER;

public class GetFileInfoTask implements Runnable {
  private static final String TAG = "DownloadManager";
  private final DownloadResponse downloadResponse;
  private final DownloadInfo downloadInfo;
  private final OnGetFileInfoListener onGetFileInfoListener;

  public GetFileInfoTask(DownloadResponse downloadResponse, DownloadInfo downloadInfo,
      OnGetFileInfoListener onGetFileInfoListener) {
    this.downloadResponse = downloadResponse;
    this.downloadInfo = downloadInfo;
    this.onGetFileInfoListener = onGetFileInfoListener;
  }

  @Override
  public void run() {
    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
    try {
      executeConnection();
    } catch (DownloadException e) {
      downloadResponse.handleException(e,downloadInfo);
    } catch (Exception e) {
      downloadResponse.handleException(new DownloadException(EXCEPTION_OTHER, e),downloadInfo);
    }
  }

  private void executeConnection() throws DownloadException {
    HttpURLConnection httpConnection = null;

    URL url = null;
    try {
      url = new URL(downloadInfo.getUri());
      httpConnection = (HttpURLConnection) url.openConnection();
      httpConnection.setConnectTimeout(10000);
      httpConnection.setReadTimeout(10000);
      httpConnection.setRequestMethod("GET");
      httpConnection.setRequestProperty("Range", "bytes=" + 0 + "-");
      final int responseCode = httpConnection.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK) {
        parseHttpResponse(httpConnection, false);
      } else if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
        parseHttpResponse(httpConnection, true);
      } else {
        throw new DownloadException(DownloadException.EXCEPTION_SERVER_ERROR,
            "UnSupported response code:" + responseCode);
      }
    } catch (MalformedURLException e) {
      throw new DownloadException(DownloadException.EXCEPTION_URL_ERROR, "Bad url->"+(url == null?null:url.toString()), e);
    } catch (ProtocolException e) {
      TLog.e(TAG,"download Protocol error ->"+e);
      throw new DownloadException(DownloadException.EXCEPTION_PROTOCOL, "Protocol error", e);
    } catch (IOException e) {
      TLog.e(TAG,"download IO error ->"+e);
      throw new DownloadException(DownloadException.EXCEPTION_IO_EXCEPTION, "IO error->"+e, e);
    } catch (Exception e) {
      throw new DownloadException(DownloadException.EXCEPTION_IO_EXCEPTION, "Unknown error->"+e, e);
    } finally {
//      if (httpConnection != null) {
//        httpConnection.disconnect();
//      }
    }
  }

  private void parseHttpResponse(HttpURLConnection httpConnection, boolean isAcceptRanges)
      throws DownloadException {

    final long length;
    String contentLength = httpConnection.getHeaderField("Content-Length");
    if (TextUtils.isEmpty(contentLength) || contentLength.equals("0") || contentLength
        .equals("-1")) {
      length = httpConnection.getContentLength();
    } else {
      length = Long.parseLong(contentLength);
    }

    if (length <= 0) {
      throw new DownloadException(DownloadException.EXCEPTION_FILE_SIZE_ZERO, "length <= 0");
    }

    checkIfPause();

    onGetFileInfoListener.onSuccess(length, isAcceptRanges);
  }

  private void checkIfPause() {
    if (downloadInfo.isPause()) {
      throw new DownloadException(DownloadException.EXCEPTION_PAUSE);
    }
  }

  /**
   * Get file info listener.
   */
  public interface OnGetFileInfoListener {

    void onSuccess(long size, boolean isSupportRanges);

    void onFailed(DownloadException exception);
  }
}

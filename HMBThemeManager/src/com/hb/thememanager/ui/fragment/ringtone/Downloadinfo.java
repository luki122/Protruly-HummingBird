package com.hb.thememanager.ui.fragment.ringtone;

import com.hb.thememanager.http.downloader.exception.DownloadException;

public interface Downloadinfo {
	  void onDownloadSuccess();

	  void onDownloadFailed(DownloadException e);
}

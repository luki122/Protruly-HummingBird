package com.protruly.music.downloadex;


public interface DownloadStatusListener {
	public void onDownload(String url, long id, int status, long downloadSize, long fileSize);
}

package com.hb.thememanager.http.downloader.callback;

import java.lang.ref.SoftReference;

public abstract class AbsDownloadListener implements DownloadListener {

  private SoftReference<Object> userTag;

  public AbsDownloadListener() {
  }

  public AbsDownloadListener(SoftReference<Object> userTag) {
    this.userTag = userTag;
  }

  public SoftReference<Object> getUserTag() {
    return userTag;
  }

  public void setUserTag(SoftReference<Object> userTag) {
    this.userTag = userTag;
  }


}

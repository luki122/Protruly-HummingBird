package com.protruly.music.util;

import android.graphics.Bitmap;
import android.os.Parcelable;

/**
 * Created by hujianwei on 17-8-29.
 */

public interface HBIListItem extends Parcelable {
    public int getIsDownLoadType();
    public long getSongId();
    public long getAlbumId();
    public Bitmap getBitmap();
    public String getTitle();
    public String getAlbumName();
    public String getArtistName();
    public String getUri();
    public String getFilePath();
    public int getM_playlist_order();

    public String getAlbumImgUri();
    public String getLrcUri();
    public String getLrcAuthor();

    public boolean isAvailable();
}

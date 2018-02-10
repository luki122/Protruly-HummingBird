package com.protruly.music.model;

import android.graphics.Bitmap;
import com.protruly.music.util.HBListItem;
/**
 * Created by hujianwei on 17-9-4.
 */

public class SearchItem extends HBListItem {
    public String mMimeType;
    public String mPinYin;
    public int mAlbumCount;
    public int mSongCount;
    public boolean mTag = false;
    public float mSimilarity;
    public String mLrcLink;
    public String mLyricist;
    public String mAlbumImageUri;
    public SearchItem(long songId, Bitmap mBitmap, String mTitle, String mUri,
                      String mPath, String mAlbumName, String mArtistName, String mMimeType){
        super(songId, mTitle, mUri, mAlbumName,-1, mArtistName,0,null,null,null,-1);
        this.mMimeType = mMimeType;
        this.mAlbumImageUri = mPath;
    }
}

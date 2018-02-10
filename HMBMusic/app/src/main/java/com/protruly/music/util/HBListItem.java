package com.protruly.music.util;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * Created by hujianwei on 17-8-29.
 */

public class HBListItem implements HBIListItem {


    private Bitmap mBitmap;                            // 不使用
    private String mTitle;                             // 歌曲名
    private String mAlbumName;                         // 专辑名
    private String mUri;                               // 本地文件路径(绝对路径),PS:对虾米来说就是在线文件的URI
    private String mPath;                              // 本地文件路径(绝对路径不使用)
    private String mArtistName;                        // 艺术家名

    private String m_AlbumImgUri;                      // 专辑图片URI(可选-仅网络使用)
    private String m_LrcUri;                           // 歌词URI(可选)
    private String m_LrcAuthor;                        // 歌词作曲名(可选)

    private long m_SongId;                             // 歌曲ID
    private long m_AlbumId;                            // 专辑ID(需要)
    private int m_ImgType = 0;                         // 0:表示local music；1:表示net download music；
    private int m_playlist_order;                      // 排序(可选)

    private int mWidth = 0;
    private int mHight = 0;

    private String singers;                            //演唱者

    private String mPinyin;
    private int mDuration;
    private long m_ArtistId;
    private long size;

    private boolean isAvailable;

    public HBListItem() {
    }

    public HBListItem(long songId, String title, String uri, String albumName, long albumid, String artistName, int isNet, String albumUri, String lrcUri, String lrcAuthor, int order,boolean isAvailable){
        this.m_SongId = songId;
        this.mBitmap = null;
        this.mTitle = title;
        this.mUri = uri;
        this.mPath = uri;
        this.mAlbumName = albumName;
        this.mArtistName = artistName;
        this.m_ImgType = isNet;
        this.m_AlbumImgUri = albumUri;
        this.m_LrcUri = lrcUri;
        this.m_LrcAuthor = lrcAuthor;
        this.m_AlbumId = albumid;
        this.m_playlist_order = order;
        this.mWidth = 0;
        this.mHight = 0;
        this.isAvailable =isAvailable;
    }

    // 必须使用的数据结构字段
    public HBListItem(long songId, String title, String uri, String albumName, long albumid, String artistName, int isNet, String albumUri, String lrcUri, String lrcAuthor, int order) {
        this(songId, title, uri, albumName, albumid, artistName, isNet, albumUri, lrcUri, lrcAuthor, order, true);
    }


    public void setItemPicSize(int width, int high) {
        this.mWidth = width;
        this.mHight = high;
    }

    public int getItemWidthPicSize() {
        return mWidth;
    }

    public int getItemHighPicSize() {
        return mHight;
    }


    @Override
    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    @Override
    public String getUri() {
        return mUri;
    }

    public void setUri(String uri){
        this.mUri = uri;
    }

    @Override
    public String getAlbumImgUri() {
        return m_AlbumImgUri;
    }

    public void setAlbumImgUri(String m_AlbumImgUri) {
        this.m_AlbumImgUri = m_AlbumImgUri;
    }

    @Override
    public String getLrcUri() {
        return m_LrcUri;
    }

    @Override
    public String getLrcAuthor() {
        return m_LrcAuthor;
    }

    @Override
    public String getFilePath() {
        return mPath;
    }

    @Override
    public Bitmap getBitmap() {
        return mBitmap;
    }

    @Override
    public String getAlbumName() {
        return mAlbumName;
    }

    @Override
    public String getArtistName() {
        return mArtistName;
    }

    public void setArtistName(String name) {
        mArtistName = name;
    }

    @Override
    public long getSongId() {
        return m_SongId;
    }

    @Override
    public long getAlbumId() {
        return m_AlbumId;
    }

    @Override
    public int getIsDownLoadType() {
        return m_ImgType;
    }

    @Override
    public int getM_playlist_order() {
        return m_playlist_order;
    }

    public String getPinyin() {
        return mPinyin;
    }

    public void setPinyin(String pinyin) {
        mPinyin = pinyin;
    }

    public int getDuration() {
        return mDuration;
    }

    public void setDuration(int duration) {
        this.mDuration = duration;
    }

    public void setArtistId(long id) {
        m_ArtistId = id;
    }

    public long getArtistId() {
        return m_ArtistId;
    }

    public String getSingers() {
        return singers;
    }

    public void setSingers(String singers) {
        this.singers = singers;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mAlbumName == null) ? 0 : mAlbumName.hashCode());
        result = prime * result + ((mArtistName == null) ? 0 : mArtistName.hashCode());
        result = prime * result + (int) (m_SongId ^ (m_SongId >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HBListItem other = (HBListItem) obj;
        if (mAlbumName == null) {
            if (other.mAlbumName != null)
                return false;
        } else if (!mAlbumName.equals(other.mAlbumName))
            return false;
        if (mArtistName == null) {
            if (other.mArtistName != null)
                return false;
        } else if (!mArtistName.equals(other.mArtistName))
            return false;
        if (m_SongId != other.m_SongId)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "HBListItem [mBitmap=" + mBitmap + ", mTitle=" + mTitle + ", mAlbumName=" + mAlbumName + ", mUri=" + mUri + ", mPath=" + mPath + ", mArtistName=" + mArtistName + ", m_AlbumImgUri="
                + m_AlbumImgUri + ", m_LrcUri=" + m_LrcUri + ", m_LrcAuthor=" + m_LrcAuthor + ", m_SongId=" + m_SongId + ", m_AlbumId=" + m_AlbumId + ", m_ImgType=" + m_ImgType
                + ", m_playlist_order=" + m_playlist_order + ", mWidth=" + mWidth + ", mHight=" + mHight + ", singers=" + singers + ", mPinyin=" + mPinyin + ", mDuration=" + mDuration
                + ", m_ArtistId=" + m_ArtistId + ", isAvailable=" + isAvailable + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(m_SongId);
        dest.writeString(mTitle);
        dest.writeString(mUri);
        dest.writeString(mAlbumName);
        dest.writeString(mArtistName);
        dest.writeInt(m_ImgType);
        dest.writeString(m_AlbumImgUri);
        dest.writeString(m_LrcUri);
        dest.writeLong(m_AlbumId);
        dest.writeString(singers);
        dest.writeInt(isAvailable ? 1 : 0);
    }

    public HBListItem(Parcel pl) {
        m_SongId = pl.readLong();
        mTitle = pl.readString();
        mUri = pl.readString();
        mAlbumName = pl.readString();
        mArtistName = pl.readString();
        m_ImgType = pl.readInt();
        m_AlbumImgUri = pl.readString();
        m_LrcUri = pl.readString();
        m_AlbumId = pl.readLong();
        singers = pl.readString();
        isAvailable = (pl.readInt()==0)?false:true;
    }

    public static final Parcelable.Creator<HBListItem> CREATOR = new Parcelable.Creator<HBListItem>() {

        @Override
        public HBListItem createFromParcel(Parcel source) {
            return new HBListItem(source);
        }

        @Override
        public HBListItem[] newArray(int size) {
            return new HBListItem[size];
        }
    };
}

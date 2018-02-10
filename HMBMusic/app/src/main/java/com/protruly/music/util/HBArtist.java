package com.protruly.music.util;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by xiaobin on 17-9-22.
 */

public class HBArtist implements Parcelable {

    private int artistId;
    private String artistName;
    private int numberOfTrack;

    private String mPinyin;

    public HBArtist(int artistId, String artistName, int numberOfTrack) {
        this.artistId = artistId;
        this.artistName = artistName;
        this.numberOfTrack = numberOfTrack;
    }

    public int getArtistId() {
        return artistId;
    }

    public void setArtistId(int artistId) {
        this.artistId = artistId;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public int getNumberOfTrack() {
        return numberOfTrack;
    }

    public void setNumberOfTrack(int numberOfTrack) {
        this.numberOfTrack = numberOfTrack;
    }

    public String getPinyin() {
        return mPinyin;
    }

    public void setPinyin(String mPinyin) {
        this.mPinyin = mPinyin;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
}

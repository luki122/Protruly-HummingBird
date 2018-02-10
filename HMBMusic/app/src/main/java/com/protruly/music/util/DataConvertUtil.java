package com.protruly.music.util;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import com.protruly.music.MusicUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hujianwei on 17-9-1.
 */

public class DataConvertUtil {

    // for HBSong Item
    public static final String TRACK_ID = MediaStore.Audio.Media._ID;
    public static final String TRACK_ALBUM_ID = MediaStore.Audio.Media.ALBUM_ID;
    public static final String TRACK_TITLE = MediaStore.Audio.Media.TITLE;
    public static final String TRACK_DATA = MediaStore.Audio.Media.DATA;
    public static final String TRACK_ALBUM_NAME = MediaStore.Audio.Media.ALBUM;
    public static final String TRACK_ARTIST_NAME = MediaStore.Audio.Media.ARTIST;
    public static final String TRACK_ARTIST_ID = MediaStore.Audio.Media.ARTIST_ID;
    public static final String TRACK_DURATION = MediaStore.Audio.Media.DURATION;
    public static final String TRACK_YEAR = MediaStore.Audio.Media.YEAR;//歌曲的发行时间
    public static final String TRACK_NO = MediaStore.Audio.Media.TRACK;//歌曲序号

    // for HBAlbum Item
    public static final String ALBUM_ID = MediaStore.Audio.Albums._ID;
    public static final String ALBUM_NAME = MediaStore.Audio.Media.ALBUM;// = TRACK_ALBUM_NAME
    public static final String ALBUM_ART = MediaStore.Audio.Albums.ALBUM_ART;
    public static final String ALBUM_ARTIST_NAME = MediaStore.Audio.Media.ARTIST;// = TRACK_ARTIST_NAME
    public static final String ALBUM_TRACK_NUMBER = MediaStore.Audio.Albums.NUMBER_OF_SONGS_FOR_ARTIST;
    public static final String ALBUM_RELEASE_DATE = MediaStore.Audio.Albums.LAST_YEAR;

    public static String[] albumCols = new String[] { ALBUM_ID,
            ALBUM_ARTIST_NAME, ALBUM_NAME, ALBUM_TRACK_NUMBER,
            ALBUM_RELEASE_DATE, ALBUM_ART };

    public static String[] trackCols = new String[] { TRACK_ID, TRACK_ALBUM_ID,
            TRACK_TITLE, TRACK_DATA, TRACK_ALBUM_NAME, TRACK_ARTIST_NAME,
            TRACK_ARTIST_ID, TRACK_DURATION, TRACK_YEAR, TRACK_NO };


    public static ArrayList<HBAlbum> ConvertToAlbum(Cursor cursor) {
        int resultCounts = cursor.getCount();
        if (resultCounts == 0 || !cursor.moveToFirst()) {
            return null;
        }
        ArrayList<HBAlbum> rentalCar = new ArrayList<HBAlbum>();
        for (int i = 0; i < resultCounts; i++) {
            HBAlbum mAlbum = new HBAlbum();
            mAlbum.setAlbumId(Long.parseLong(cursor.getString(cursor
                    .getColumnIndex(ALBUM_ID))));
            mAlbum.setAlbumName(cursor.getString(cursor
                    .getColumnIndex(ALBUM_NAME)));
//			mAlbum.setAlbumArt(cursor.getString(cursor
//					.getColumnIndex(ALBUM_ART)));
            mAlbum.setAlbumArt("foo");
            mAlbum.setArtistName(cursor.getString(cursor
                    .getColumnIndex(ALBUM_ARTIST_NAME)));
            mAlbum.setTrackNumber(Integer.parseInt(cursor.getString(cursor
                    .getColumnIndex(ALBUM_TRACK_NUMBER))));
            mAlbum.setReleaseDate(cursor.getString(cursor
                    .getColumnIndex(ALBUM_RELEASE_DATE)));
            try {
                mAlbum.setUri(cursor.getString(cursor
                        .getColumnIndex(TRACK_DATA)));
            } catch (Exception e) {
                e.printStackTrace();
            }
            rentalCar.add(mAlbum);
            cursor.moveToNext();
        }
        return rentalCar;
    }

    public static ArrayList<HBListItem> ConvertToTrack(Cursor cursor, List<String> paths, Context mContext) {
        int resultCounts = cursor.getCount();
        if (resultCounts == 0 || !cursor.moveToFirst()) {
            return null;
        }
        ArrayList<HBListItem> rentalCar = new ArrayList<HBListItem>();
        for (int i = 0; i < resultCounts; i++) {
            long mId = cursor.getLong(0);
            String mTitle = cursor.getString(2);
            if (mTitle == null || mTitle.isEmpty()) {
                mTitle = MediaStore.UNKNOWN_STRING;
            }
            String mPath = cursor.getString(3);
            String mAlbumName = cursor.getString(4);
            String mArtistName = cursor.getString(5);
            int mduration = cursor.getInt(7);
            String mUri = mPath;
            long mAlbumId = cursor.getLong(1);
            String imgUri=HBMusicUtil.getImgPath(mContext, HBMusicUtil.MD5(mTitle+mArtistName+mAlbumName));
            String mPinyin = MusicUtils.getSpell(mTitle);
            if (mPinyin.charAt(0) < 65 || mPinyin.charAt(0) > 90) {
                mPinyin = String.valueOf((char) 91);
            }
            HBListItem mTrack = new HBListItem(mId, mTitle, mUri,
                    mAlbumName, mAlbumId, mArtistName, 0, imgUri, null, null, -1);
            mTrack.setPinyin(mPinyin);
            mTrack.setDuration(mduration);
            if(!paths.contains(mPath))
                rentalCar.add(mTrack);
            cursor.moveToNext();
        }
        return rentalCar;
    }
}

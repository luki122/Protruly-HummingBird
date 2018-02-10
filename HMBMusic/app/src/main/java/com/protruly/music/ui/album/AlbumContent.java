package com.protruly.music.ui.album;

import android.content.AsyncQueryHandler;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.protruly.music.MusicUtils;
import com.protruly.music.util.DataConvertUtil;
import com.protruly.music.util.Globals;
import com.protruly.music.util.HBAlbum;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by hujianwei on 17-9-1.
 */

public class AlbumContent {

    static String artistId;
    static Context mContext;
    /**
     * An array of album items.
     */
    public static ArrayList<HBAlbum> ITEMS = new ArrayList<HBAlbum>();

    /**
     * A map of album items, by ID.
     */
    public static HashMap<Long, HBAlbum> ITEM_MAP = new HashMap<Long, HBAlbum>();

    public AlbumContent(Context mContext, String artistId) {
        AlbumContent.mContext = mContext;
        AlbumContent.artistId = artistId;
        initData();
    }

    public static void initData() {
        Cursor cursor = getQueryCursor(null);
        if (cursor == null) {
            ITEMS.clear();
            ITEM_MAP.clear();
            return;
        } else {
            ArrayList<HBAlbum> albums = DataConvertUtil.ConvertToAlbum(cursor);
            setItems(albums);
        }
    }

    private static void setItems(ArrayList<HBAlbum> albums) {
        if (ITEMS != null) {
            ITEMS.clear();
            ITEM_MAP.clear();
        }
        if (albums != null && albums.size() > 0) {
            ITEMS = albums;
        }

        for (int i = 0 ;i < ITEMS.size();i++) {
            addItem(ITEMS.get(i));
        }
    }

    private static void addItem(HBAlbum item) {
        ITEM_MAP.put(item.getAlbumId(), item);
    }

    // 根据删除歌曲的位置
    public static void deleteItem(int position) {
        int index = getAlbumIndex(position);
        long aid = ITEMS.get(index).getAlbumId();
        if (ITEMS.get(index).getTrackNumber() == 1) {
//			ITEMS.remove(index);
//			ITEM_MAP.remove(aid);
        } else {
            updateAlbumInfo(ITEMS.get(index).getTrackNumber() - 1, aid);
        }

    }

    // 单一删除歌曲时专辑信息更新。
    public static void deleteItem(long aid) {
        if (ITEM_MAP.get(aid) != null) {
            updateAlbumInfo(ITEM_MAP.get(aid).getTrackNumber() - 1, aid);
        }

    }

    private static int getAlbumIndex(int position) {
        if (ITEMS.get(0) != null && position < ITEMS.get(0).getTrackNumber()) {
            return 0;
        }
        if (ITEMS.get(0) != null
                && ITEMS.get(1) != null
                && position < ITEMS.get(0).getTrackNumber()
                + ITEMS.get(1).getTrackNumber()) {
            return 1;
        }
        if (ITEMS.get(0) != null
                && ITEMS.get(1) != null
                && ITEMS.get(2) != null
                && position < ITEMS.get(0).getTrackNumber()
                + ITEMS.get(1).getTrackNumber()
                + ITEMS.get(2).getTrackNumber()) {
            return 2;
        }
        return -1;
    }


    public static void updateAlbumInfo(int num, long aid) {
        HBAlbum mAlbum = ITEM_MAP.get(aid);
        mAlbum.setTrackNumber(num);
    }

    public static boolean isEmpty() {
        return ITEMS.isEmpty();
    }

    private static Cursor getQueryCursor(AsyncQueryHandler async) {
        String id_row = DataConvertUtil.TRACK_ALBUM_ID + " AS "
                + DataConvertUtil.ALBUM_ID;
        String albumdate_row = "MAX(" + DataConvertUtil.TRACK_YEAR + ") AS "
                + DataConvertUtil.ALBUM_RELEASE_DATE;
        String numsongs_row = "count(*) AS "
                + DataConvertUtil.ALBUM_TRACK_NUMBER;
        String[] cols = new String[] { id_row,
                DataConvertUtil.TRACK_ALBUM_NAME,
                DataConvertUtil.TRACK_ARTIST_NAME, numsongs_row, albumdate_row,DataConvertUtil.TRACK_DATA };
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        StringBuilder where = new StringBuilder();

        Cursor ret = null;
        if (artistId != null) {
            where.append(Globals.QUERY_SONG_FILTER + " AND "
                    + MediaStore.Audio.Media.ARTIST_ID + "="
                    + artistId + ") GROUP BY ("
                    + DataConvertUtil.TRACK_ALBUM_ID);
            if (async != null) {
                async.startQuery(0, null, uri, cols, where.toString(), null,
                        DataConvertUtil.ALBUM_RELEASE_DATE + " desc, "+ MediaStore.Audio.Media.ALBUM_KEY);
            } else {
                ret = MusicUtils.query(mContext, uri, cols, where.toString(),
                        null, DataConvertUtil.ALBUM_RELEASE_DATE + " desc, "+ MediaStore.Audio.Media.ALBUM_KEY);
            }
        } else {
            where.append(Globals.QUERY_SONG_FILTER + ") GROUP BY ("
                    + DataConvertUtil.TRACK_ALBUM_ID);
            // Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
            if (async != null) {
                async.startQuery(0, null, uri, cols, where.toString(), null,
                        DataConvertUtil.ALBUM_RELEASE_DATE + " desc, "+ MediaStore.Audio.Media.ALBUM_KEY);
            } else {
                ret = MusicUtils.query(mContext, uri, cols, where.toString(),
                        null, DataConvertUtil.ALBUM_RELEASE_DATE + " desc, "+ MediaStore.Audio.Media.ALBUM_KEY);
            }
        }
        return ret;
    }

    //清除所有数据
    public static void reset() {
        artistId = null;
        mContext = null;
        if (ITEMS != null) {
            ITEMS.clear();
        }
        if (ITEM_MAP != null) {
            ITEM_MAP.clear();
        }
    }
}

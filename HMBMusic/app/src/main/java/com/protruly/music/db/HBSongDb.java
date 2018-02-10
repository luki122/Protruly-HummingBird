package com.protruly.music.db;
/**
 * Created by hujianwei on 17-8-29.
 */
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.protruly.music.util.HBListItem;
import com.protruly.music.util.LogUtil;

import java.util.ArrayList;
import java.util.List;


import com.protruly.music.model.HBCollectPlaylist;
import com.protruly.music.model.HBLoadingListener;
import com.protruly.music.util.HBMusicUtil;
import com.protruly.music.util.HBListItem;


public class HBSongDb {

    private static final String TAG = "HBSongDb";
    private static final int hb_MSG_NEED_UPDATE = 1;
    private static final int hb_MSG_NONEED_UPDATE = 2;

    private HBDbHelper mDbHelper;
    private FavoritesDbThread mDbTask;
    private Context mContext;
    private final Handler mMainHandler;
    private ArrayList<HBLoadingListener> mListeners = new ArrayList<HBLoadingListener>();
    private HBLoadingListener mLoadingListener = null;

    private static List<Long> mWhere = new ArrayList<Long>();

    public HBSongDb(Context context) {
        this.mContext = context;
        this.mDbHelper = new HBDbHelper(context);

        mMainHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                switch (msg.what) {
                    case hb_MSG_NEED_UPDATE:
                        for (int i = 0; i < mListeners.size(); i++) {
                            mListeners.get(i).onNeedLoading();
                        }
                        break;

                    case hb_MSG_NONEED_UPDATE:
                        for (int i = 0; i < mListeners.size(); i++) {
                            mListeners.get(i).onNotNeedLoading();
                        }
                        break;

                    default:
                        break;
                }
            }

        };
    }

    public void resume() {
        mDbTask = new FavoritesDbThread();
        mDbTask.start();
        return;
    }

    public void pause() {

        mMainHandler.removeCallbacksAndMessages(null);
        if (mDbTask != null) {
            mDbTask.terminate();
            mDbTask = null;
        }

        return;
    }

    public void onContentDirty() {
        if (mDbTask != null) {
            mDbTask.onNotify();
        }
        return;
    }

    public void setLoadingListener(HBLoadingListener mListener) {


        if (!mListeners.contains(mListener)) {
            mListeners.add(mListener);
        }

        return;
    }

    public void removesetLoadingListener(HBLoadingListener mListener) {
        
        if (mListeners.contains(mListener)) {
            mListeners.remove(mListener);
        }

        return;
    }

    /*
     * 增加歌曲信息到数据库
     */
    public long insertDb(HBListItem song) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        // values.put(DBData.SONG_DISPLAYNAME, song.getDisplayName());
        values.put(HBDbData.SONG_FILEPATH, song.getFilePath());
        // values.put(DBData.SONG_LYRICPATH, song.getLyricPath());
        // values.put(DBData.SONG_MIMETYPE, song.getMimeType());
        values.put(HBDbData.SONG_NAME, song.getTitle());
        // values.put(DBData.SONG_ALBUMID, song.getAlbum().getId());
        // values.put(DBData.SONG_NETURL, song.getNetUrl());
        // values.put(DBData.SONG_DURATIONTIME, song.getDurationTime());
        // values.put(DBData.SONG_SIZE, song.getSize());
        // values.put(DBData.SONG_ARTISTID, song.getArtist().getId());
        // values.put(DBData.SONG_PLAYERLIST, song.getPlayerList());
        // values.put(DBData.SONG_ISDOWNFINISH, song.isDownFinish());
        values.put(HBDbData.SONG_ISLIKE, "0");
        // values.put(DBData.SONG_ISNET, song.isNet());
        values.put(HBDbData.SONG_ALBUMNAME, song.getAlbumName());
        values.put(HBDbData.SONG_ARTISTNAME, song.getArtistName());
        values.put(HBDbData.SONG_AUDIO_ID, song.getSongId());
        // values.put(HBDbData.SONG_DATAADDED, song.getSongId());

        long rs = db.insert(HBDbData.SONG_TABLENAME, HBDbData.SONG_NAME, values);
        db.close();
        return rs;
    }

    private void compareAudioDb(List<Long> where) {

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        Cursor cur = null;
        ArrayList<Long> list = new ArrayList<Long>();
        try {
            cur = db.query(HBDbData.FAVORITES_TABLENAME, null, HBDbData.FAVORITES_ISNET + "=0", null, null, null, null);
            if (cur != null) {
                cur.moveToFirst();
                while (!cur.isAfterLast()) {
                    Long id = cur.getLong(1);
                    if (!where.contains(id)) {
                        list.add(id);
                    }
                    cur.moveToNext();
                }
            }

            for (int i = 0; i < list.size(); i++) {
                String[] whereArgs = { "" + String.valueOf(list.get(i)) };

                db.delete(HBDbData.FAVORITES_TABLENAME, HBDbData.FAVORITES_AUDIO_ID + " = ?", whereArgs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cur != null) {
                cur.close();
                cur = null;
            }

            db.close();
        }

        return;
    }

    public String getLocalLrc(String title, String artist) {
        if (title == null || (title != null && title.isEmpty()) || artist == null || (artist != null && artist.isEmpty())) {
            return null;
        }

        synchronized (this) {
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.query(HBDbData.AUDIOINFO_TABLENAME, null, null, null, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        if (cursor.getString(2).equalsIgnoreCase(title) && cursor.getString(3).equalsIgnoreCase(artist)) {
                            return cursor.getString(4);
                        }
                        cursor.moveToNext();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                }

                db.close();
            }

            return null;
        }
    }

    private String toLowerCase(String s) {
        if (s == null) {
            return null;
        }

        char[] data = s.toCharArray();
        int dist = 'a' - 'A';
        int end = data.length;
        for (int i = 0; i < end; i++) {
            if (data[i] >= 'A' && data[i] <= 'Z') {
                data[i] += dist;
            }
        }

        return String.valueOf(data);
    }

    public HBMusicInfo getHBMusicInfo(long id, String title, String artist, int isnet) {
        synchronized (this) {
            if (id < 0 && isnet == 1) {
                return null;
            }

            if (title == null || (title != null && TextUtils.isEmpty(title)) || artist == null || (artist != null && TextUtils.isEmpty(artist))) {
                return null;
            }

            String t1 = toLowerCase(title);
            String t2 = toLowerCase(artist);

            StringBuilder where = new StringBuilder();
           
            String where1 = HBDbData.AUDIOINFO_SONG_ID + " = '" + String.valueOf(id) + "'";
            String where2 = HBDbData.AUDIOINFO_SONG_TITLE + " = '" + t1 + "'";
            String where3 = HBDbData.AUDIOINFO_SONG_ARTIST + " = '" + t2 + "'";
            where.append(where1 + " AND " + where2 + " AND " + where3);

            LogUtil.d(TAG, "getHBMusicInfo[1]where:" + where.toString() + ",isnet:" + isnet + " title:" + title + " artist:" + artist);

            SQLiteDatabase db = mDbHelper.getReadableDatabase();
            Cursor cursor = null;

            try {
                cursor = db.query(HBDbData.AUDIOINFO_TABLENAME, null, where.toString(), null, null, null, null);
                if (cursor != null) {
                    if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                        HBMusicInfo item = new HBMusicInfo(cursor.getString(4), cursor.getString(5));
                        LogUtil.d(TAG, "getHBMusicInfo item:" + item.toString());
                        return item;
                    }
                }
            } catch (Exception e) {
                Log.i(TAG, "getHBMusicInfo fail!");
            } finally {
                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                }

                db.close();
            }
        }

        return null;
    }

    public boolean isHaveLrc(String title, String artist) {
        if (title == null || title.isEmpty() || artist == null || artist.isEmpty()) {
            return false;
        }

        synchronized (this) {
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.query(HBDbData.AUDIOINFO_TABLENAME, null, null, null, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        if (cursor.getString(2).equalsIgnoreCase(title) && cursor.getString(3).equalsIgnoreCase(artist)) {

                            return true;
                        }
                        cursor.moveToNext();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                }

                db.close();
            }
        }

        return false;
    }

    // 0:表示local music；1:表示net download music；同HBListItem

    public void addToAudioInfo(long id, String title, String artist, String picpath, String lrcpath, int isnet) {
        synchronized (this) {
            if (id < 0 || (picpath == null && lrcpath == null) || ((picpath != null && TextUtils.isEmpty(picpath)) && (lrcpath != null && TextUtils.isEmpty(lrcpath)))) {
                return;
            }

            if (title == null || (title != null && TextUtils.isEmpty(title))) {
                title = "unknow";
            }

            if (artist == null || (artist != null && TextUtils.isEmpty(artist))) {
                artist = "unknow";
            }

            String t1 = toLowerCase(title).replaceAll("'", "");
            String t2 = toLowerCase(artist).replaceAll("'", "");

            String where1 = HBDbData.AUDIOINFO_SONG_ID + " = '" + String.valueOf(id) + "'";
            String where2 = HBDbData.AUDIOINFO_SONG_TITLE + " = '" + t1 + "'";
            String where3 = HBDbData.AUDIOINFO_SONG_ARTIST + " = '" + t2 + "'";
            StringBuilder where = new StringBuilder();
            where.append(where1 + " AND " + where2 + " AND " + where3);

            LogUtil.d(TAG, "addToAudioInfoPics  where:" + where.toString() + " title:" + t1 + " artist:" + t2 + " picpath:" + picpath + ",lrcpath:" + lrcpath);
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            Cursor cur = null;

            try {
                cur = db.query(HBDbData.AUDIOINFO_TABLENAME, null, where.toString(), null, null, null, null);
                if (cur != null) {
                    ContentValues values = new ContentValues();
                    if (cur.getCount() <= 0) {
                        values.put(HBDbData.AUDIOINFO_SONG_ID, id);
                        values.put(HBDbData.AUDIOINFO_SONG_TITLE, t1);
                        values.put(HBDbData.AUDIOINFO_SONG_ARTIST, t2);
                        if (lrcpath != null) {
                            values.put(HBDbData.AUDIOINFO_SONG_LRC, lrcpath);
                        }
                        if (picpath != null) {
                            values.put(HBDbData.AUDIOINFO_SONG_ALBUMPIC, picpath);
                        }
                        values.put(HBDbData.AUDIOINFO_SONG_ISNET, isnet);
                        long index = db.insert(HBDbData.AUDIOINFO_TABLENAME, null, values);
                        LogUtil.e(TAG, "addToAudioInfoPics [2] index:" + index);
                    } else {
                        if (lrcpath != null) {
                            values.put(HBDbData.AUDIOINFO_SONG_LRC, lrcpath);
                        }
                        if (picpath != null) {
                            values.put(HBDbData.AUDIOINFO_SONG_ALBUMPIC, picpath);
                        }
                        values.put(HBDbData.AUDIOINFO_SONG_ISNET, isnet);
                        int index = db.update(HBDbData.AUDIOINFO_TABLENAME, values, where.toString(), null);
                        LogUtil.e(TAG, "addToAudioInfoPics [3] index:" + index);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cur != null) {
                    cur.close();
                    cur = null;
                }

                db.close();
            }
        }

        return;
    }

    public boolean updateFavoritesEx() {

        if (mContext == null) {
            return false;
        }

        Cursor mAudio = null;
        List<Long> where = new ArrayList<Long>();
        try {
            mAudio = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[] { MediaStore.Audio.Media._ID }, null, null, null);
            if (mAudio != null && mAudio.moveToFirst()) {
                do {
                    where.add(mAudio.getLong(0));
                } while (mAudio.moveToNext());
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (mAudio != null) {
                mAudio.close();
                mAudio = null;
            }
        }

        if (where.size() > 0) {
            if (mWhere.size() > 0 && where.equals(mWhere)) {
                return false;
            }

            compareAudioDb(where);
            mWhere = where;
            return true;
        }

        else {
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            try {
                db.delete(HBDbData.FAVORITES_TABLENAME, HBDbData.FAVORITES_ISNET + "=0", null);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.close();
            }
            mWhere.clear();
        }


        return false;
    }

    public void addToFavoritesEx(HBListItem item) {
        if (item == null) {
            return;
        }

        synchronized (this) {
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            HBListItem tmpItem = item;
            Cursor cur = null;
            try {
                cur = db.query(HBDbData.FAVORITES_TABLENAME, null, null, null, null, null, null);
                if (cur != null) {
                    int base = cur.getCount();
                    cur.moveToFirst();
                    while (!cur.isAfterLast()) {

                        if (cur.getLong(1) == tmpItem.getSongId())
                            return;
                        cur.moveToNext();
                    }

                    if (base > 0) {
                        cur.moveToLast();
                        base = cur.getInt(2);
                    }

                    ContentValues values = new ContentValues();
                    values.put(HBDbData.FAVORITES_AUDIO_ID, tmpItem.getSongId());
                    values.put(HBDbData.FAVORITES_PLAY_ORDER, base + 1);
                    values.put(HBDbData.FAVORITES_TITLE, tmpItem.getTitle());
                    values.put(HBDbData.FAVORITES_ALBUMNAME, tmpItem.getAlbumName());
                    values.put(HBDbData.FAVORITES_ARTISTNAME, tmpItem.getArtistName());
                    values.put(HBDbData.FAVORITES_URI, tmpItem.getFilePath());
                    values.put(HBDbData.FAVORITES_ISNET, tmpItem.getIsDownLoadType());
                    long t = db.insert(HBDbData.FAVORITES_TABLENAME, null, values);
                    if (t < 0) {
                        Log.i(TAG, " addToFavoritesEx fail t:" + t + ",id:" + tmpItem.getSongId());
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cur != null) {
                    cur.close();
                    cur = null;
                }

                db.close();
            }
        }

        return;
    }

    public void removeFromFavoritesEx(long id, int isnet) {
        if (id < 0) {
            return;
        }

        synchronized (this) {
            SQLiteDatabase db = mDbHelper.getWritableDatabase();

            StringBuilder where = new StringBuilder();
            where.append(HBDbData.FAVORITES_AUDIO_ID + " = " + id);
            where.append(" AND " + HBDbData.FAVORITES_ISNET + " = " + isnet);
            db.delete(HBDbData.FAVORITES_TABLENAME, where.toString(), null);
            db.close();
        }

        return;
    }

    public boolean isFavoriteEx(long id, int isnet) {

        if (id < 0) {
            return false;
        }

        synchronized (this) {
            SQLiteDatabase db = mDbHelper.getWritableDatabase();

            Cursor cursor = null;
            try {
                cursor = db.query(HBDbData.FAVORITES_TABLENAME, null, null, null, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        if (cursor.getLong(1) == id && cursor.getInt(7) == isnet) {
                            return true;
                        }
                        cursor.moveToNext();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                    cursor = null;

                    db.close();
                }
            }
        }

        return false;
    }

    private class FavoritesDbThread extends Thread {
        private volatile boolean mActive = true;
        private volatile boolean mDirty = false;

        private void updateLoading(boolean loading) {
            mMainHandler.removeMessages(hb_MSG_NEED_UPDATE);
            mMainHandler.removeMessages(hb_MSG_NONEED_UPDATE);
            mMainHandler.sendEmptyMessage(loading ? hb_MSG_NEED_UPDATE : hb_MSG_NONEED_UPDATE);
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            while (mActive) {
                synchronized (this) {
                    if (mActive && !mDirty) {
                        try {
                            this.wait();
                        } catch (InterruptedException e) {
                            Log.w(TAG, " unexpected interrupt: " + this);
                        }
                        continue;
                    }

                    mDirty = false;
                }

                boolean flag = updateFavoritesEx();
                updateLoading(flag);
            }
        }

        public synchronized void onNotify() {
            mDirty = true;
            notifyAll();
            return;
        }

        public synchronized void terminate() {
            mActive = false;
            notifyAll();
            return;
        }
    }

    public ArrayList<HBListItem> querySongIdFromFavorites(List<String> xmlpath) {
        ArrayList<HBListItem> list = new ArrayList<HBListItem>();
        synchronized (this) {
            SQLiteDatabase db = mDbHelper.getReadableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.query(HBDbData.FAVORITES_TABLENAME, new String[] { HBDbData.FAVORITES_AUDIO_ID, HBDbData.FAVORITES_TITLE, HBDbData.FAVORITES_ALBUMNAME,
                        HBDbData.FAVORITES_ARTISTNAME, HBDbData.FAVORITES_ISNET, HBDbData.FAVORITES_URI }, null, null, null, null, HBDbData.FAVORITES_PLAY_ORDER + " desc");
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        long id = cursor.getLong(0);
                        String title = cursor.getString(1);
                        String album = cursor.getString(2);
                        String artist = cursor.getString(3);
                        int isnet = cursor.getInt(4);
                        String uri = cursor.getString(5);
                        String imgUri = HBMusicUtil.getImgPath(mContext, HBMusicUtil.MD5(title + artist + album));
                        HBListItem item = new HBListItem(id, title, uri, album, -1, artist, isnet, imgUri, null, null, -1);
                        if (isnet == 0) {
                            String dir = uri.substring(0, uri.lastIndexOf("/"));
                            if (!xmlpath.contains(dir)) {
                                list.add(item);
                            }
                        } else {
                            list.add(item);
                        }

                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                    cursor = null;

                    db.close();
                }
            }
        }
        return list;
    }

    public void deleteFavoritesById(long[] list) {

        synchronized (this) {
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            StringBuilder where = new StringBuilder();
            where.append(HBDbData.FAVORITES_AUDIO_ID + " IN (");
            for (int i = 0; i < list.length; i++) {
                where.append(list[i]);
                if (i < list.length - 1) {
                    where.append(",");
                }
            }
            where.append(")");

            try {
                db.delete(HBDbData.FAVORITES_TABLENAME, where.toString(), null);
            } catch (Exception e) {
                e.printStackTrace();
            }

            db.close();
        }

    }

    public void insertCollect(HBCollectPlaylist playlist) {

        synchronized (this) {
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(HBDbData.COLLECT_NAME, playlist.getPlaylistname());
            values.put(HBDbData.COLLECT_SONG_SIZE, playlist.getSongSize());
            values.put(HBDbData.COLLECT_IMG, playlist.getImgUrl());
            values.put(HBDbData.COLLECT_SHOU_INFO, playlist.getInfo());
            values.put(HBDbData.COLLECT_PLAYLISTID, playlist.getPlaylistid());
            values.put(HBDbData.COLLECT_TYPE, playlist.getType());
            values.put(HBDbData.COLLECT_LIST_TYPE, playlist.getListType());

            db.insert(HBDbData.COLLECT_TABLENAME, null, values);
            db.close();
        }
    }

    public void deleteCollectById(String playlistid) {
        if (playlistid == null || playlistid.equals("")) {
            return;
        }
        synchronized (this) {
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            try {
                String[] whereArgs = { "" + playlistid };
                db.delete(HBDbData.COLLECT_TABLENAME, HBDbData.COLLECT_PLAYLISTID + " =?", whereArgs);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.close();

            }
        }
    }

    public boolean isCollectById(String id) {

        if (id == null || id.equals("")) {
            return false;
        }
        synchronized (this) {
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            Cursor cursor = null;
            try {
                String[] whereArgs = { "" + id };
                cursor = db.query(HBDbData.COLLECT_TABLENAME, null, HBDbData.COLLECT_PLAYLISTID + " =?", whereArgs, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                }
                db.close();
            }
        }
        return false;
    }

    public List<HBCollectPlaylist> queryCollectInfo() {
        List<HBCollectPlaylist> list = new ArrayList<HBCollectPlaylist>();
        synchronized (this) {
            SQLiteDatabase db = mDbHelper.getReadableDatabase();
            Cursor cursor = null;
            String[] columns = new String[] { HBDbData.COLLECT_NAME, HBDbData.COLLECT_SONG_SIZE, HBDbData.COLLECT_IMG, HBDbData.COLLECT_SHOU_INFO, HBDbData.COLLECT_PLAYLISTID,
                    HBDbData.COLLECT_TYPE, HBDbData.COLLECT_LIST_TYPE };

            try {
                cursor = db.query(HBDbData.COLLECT_TABLENAME, columns, null, null, null, null, HBDbData.COLLECT_ID + " desc");

                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        String name = cursor.getString(0);
                        int size = cursor.getInt(1);
                        String imgurl = cursor.getString(2);
                        String info = cursor.getString(3);
                        String id = cursor.getString(4);
                        String type = cursor.getString(5);
                        int listType = cursor.getInt(6);
                        HBCollectPlaylist playlist = new HBCollectPlaylist(name, id, imgurl, size, info, listType, type);

                        list.add(playlist);
                    } while (cursor.moveToNext());
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {

                    cursor.close();
                    cursor = null;
                }
                if (db != null && db.isOpen()) {
                    db.close();
                }
            }
        }

        return list;
    }

    public boolean insertSearchHistory(String keyword) {
        if (keyword == null || (keyword != null && keyword.isEmpty())) {
            return false;
        }
        if (isSearchHistory(keyword)) {
            return false;
        }
        synchronized (this) {
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(HBDbData.SEARCH_HISTORY_KEY, keyword);
            db.insert(HBDbData.SEARCH_HISTORY_TABLENAME, null, values);
            db.close();
        }
        return true;
    }

    public List<String> querySearchHistory() {

        List<String> list = new ArrayList<String>();
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = null;
        String[] columns = new String[] { HBDbData.SEARCH_HISTORY_KEY };
        try {
            cursor = db.query(HBDbData.SEARCH_HISTORY_TABLENAME, columns, null, null, null, null, HBDbData.SEARCH_HISTORY_ID);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String keyword = cursor.getString(0);
                    list.add(keyword);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
            db.close();
        }
        return list;
    }

    private boolean isSearchHistory(String keyword) {

        boolean result = false;
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        Cursor cursor = null;
        String[] columns = new String[] { HBDbData.SEARCH_HISTORY_KEY };
        try {
            cursor = db.query(HBDbData.SEARCH_HISTORY_TABLENAME, columns, null, null, null, null, HBDbData.SEARCH_HISTORY_ID);
            if (cursor != null && cursor.moveToFirst()) {
                int count = cursor.getCount();
                String str = cursor.getString(0);
                if (count >= 5) {
                    db.delete(HBDbData.SEARCH_HISTORY_TABLENAME, HBDbData.SEARCH_HISTORY_KEY + "=?", new String[] { str });
                }
                do {
                    str = cursor.getString(0);
                    if (str.equals(keyword)) {
                        result = true;
                        break;
                    }
                } while (cursor.moveToNext());

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
            db.close();
        }

        return result;
    }

    public void clearSearchHistory() {
        synchronized (this) {
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            try {

                db.delete(HBDbData.SEARCH_HISTORY_TABLENAME, null, null);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.close();

            }
        }
    }

}

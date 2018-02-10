package com.protruly.music.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by hujianwei on 17-8-29.
 */


public class HBDbHelper extends SQLiteOpenHelper{

    public HBDbHelper(Context context) {
        super(context, HBDbData.DATABASE_NAME, null, HBDbData.VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建歌曲表 (INTEGER PRIMARY KEY AUTOINCREMENT,)

        db.execSQL("CREATE TABLE IF NOT EXISTS " + HBDbData.FAVORITES_TABLENAME + "("
                + HBDbData.FAVORITES_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + HBDbData.FAVORITES_AUDIO_ID + " INTEGER ,"
                + HBDbData.FAVORITES_PLAY_ORDER + " INTEGER,"
                + HBDbData.FAVORITES_TITLE + " TEXT,"
                + HBDbData.FAVORITES_ALBUMNAME + " TEXT,"
                + HBDbData.FAVORITES_ARTISTNAME + " TEXT,"
                + HBDbData.FAVORITES_URI + " TEXT,"
                + HBDbData.FAVORITES_ISNET + " INTEGER "
                + ")");

        //添加共享数据表
        db.execSQL("CREATE TABLE IF NOT EXISTS " + HBDbData.SHARE_TABLENAME + "("
                + HBDbData.SHARE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + HBDbData.SHARE_ISPLAYING + " INTEGER "
                + ")");

        //添加歌词表
        db.execSQL("CREATE TABLE IF NOT EXISTS " + HBDbData.AUDIOINFO_TABLENAME + "("
                + HBDbData.AUDIOINFO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + HBDbData.AUDIOINFO_SONG_ID + " INTEGER ,"
                + HBDbData.AUDIOINFO_SONG_TITLE + " TEXT,"
                + HBDbData.AUDIOINFO_SONG_ARTIST + " TEXT,"
                + HBDbData.AUDIOINFO_SONG_LRC + " TEXT,"
                + HBDbData.AUDIOINFO_SONG_ALBUMPIC + " TEXT,"
                + HBDbData.AUDIOINFO_SONG_ISNET + " INTEGER"
                + ")");

        // 添加默认列表

        // 创建专辑图片表

        // 创建歌手表

        // 创建播放列表

        //add by  20140711 start
        //创建收藏歌单列表
        db.execSQL("CREATE TABLE IF NOT EXISTS "+HBDbData.COLLECT_TABLENAME +"("
                + HBDbData.COLLECT_ID+" INTEGER PRIMARY KEY AUTOINCREMENT,"
                + HBDbData.COLLECT_NAME+" TEXT,"
                + HBDbData.COLLECT_IMG+ " TEXT,"
                + HBDbData.COLLECT_SONG_SIZE+ " INTEGER,"
                + HBDbData.COLLECT_SHOU_INFO+" TEXT ,"
                + HBDbData.COLLECT_PLAYLISTID+" TEXT,"
                + HBDbData.COLLECT_LIST_TYPE+" INTEGER,"
                + HBDbData.COLLECT_TYPE+" TEXT"
                + ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS "
                + HBDbData.SEARCH_HISTORY_TABLENAME + "("
                + HBDbData.SEARCH_HISTORY_ID
                + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + HBDbData.SEARCH_HISTORY_KEY + " TEXT" + ")");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		/*db.execSQL("DROP TABLE IF EXISTS " + HBDbData.ALBUM_TABLENAME);
		db.execSQL("DROP TABLE IF EXISTS " + HBDbData.ARTIST_TABLENAME);
		db.execSQL("DROP TABLE IF EXISTS " + HBDbData.PLAYERLIST_TABLENAME);*/
        db.execSQL("DROP TABLE IF EXISTS " + HBDbData.AUDIOINFO_TABLENAME);
        db.execSQL("DROP TABLE IF EXISTS " + HBDbData.FAVORITES_TABLENAME);
        db.execSQL("DROP TABLE IF EXISTS " + HBDbData.SHARE_TABLENAME);
        db.execSQL("DROP TABLE IF EXISTS " + HBDbData.COLLECT_TABLENAME);
        db.execSQL("DROP TABLE IF EXISTS "+HBDbData.SEARCH_HISTORY_TABLENAME);
        onCreate(db);
    }

}

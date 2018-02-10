package com.protruly.music.util;

import android.os.Environment;
import android.provider.MediaStore;

/**
 * Created by hujianwei on 17-8-29.
 */

public class Globals {

    public static String currentMode;

    public static final int REQUEST_CODE_BASE = 1;
    public static final int REQUEST_CODE_BROWSER = REQUEST_CODE_BASE + 0;
    public static final int REQUEST_CODE_ALBUM_TRACK_BROWSER = REQUEST_CODE_BASE + 1;

    // wifi网络类型标志
    public static final int NETWORK_WIFI = 0;
    // 2G网络类型标志
    public static final int NETWORK_2G = 1;
    // 3G网络类型标志
    public static final int NETWORK_3G = 2;
    // 4G网络类型标志
    public static final int NETWORK_4G = 3;

    // activity result 100
    public static final int RESULT_CODE_BASE = 100;
    public static final int RESULT_CODE_MODIFY = RESULT_CODE_BASE + 0;
    public static final int RESULT_CODE_ALBUM_TRACK_BROWSER = RESULT_CODE_BASE + 1;
    public static final int RESULT_CODE_ALBUM_TRACK_MODIFY = RESULT_CODE_BASE + 2;

    public static final int MSG_CODE_USER = 1000;
    public static final int NOW_PLAYING = MSG_CODE_USER + 0;

    // 常见key名
    public static final String KEY_SONG_ID = "track_id";
    public static final String KEY_ARTIST_ID = "artist_id";
    public static final String KEY_ARTIST_NAME = "artist_name";
    public static final String KEY_ALBUM_ID = "album_id";
    public static final String KEY_ALBUM_NAME = "album_name";
    public static final String KEY_ALBUM_ITEM = "album_item";
    public static final String KEY_ONLINE_ALBUM_TYPE = "type";


    // 搜索歌曲过滤文件大小
    public static final int FILE_SIZE_FILTER = 1024 * 500;
    public static final String FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
    public static final String SIZE_FILTER = MediaStore.Audio.Media.SIZE + ">= " + Globals.FILE_SIZE_FILTER;
    /**
     * {@link HBMusicUtil#getMountedStorage(android.content.Context)}}
     * QUERY_SONG_FILTER 与QUERY_SONG_FILTER_1 赋值
     */

    // 搜索歌曲过滤条件//MediaStore.Audio.Media.DATA + " NOT LIKE '" + FILE_PATH + "/CallRecording%' "
    public static String QUERY_SONG_FILTER = "";

    // 搜索歌曲过滤条件
    public static String QUERY_SONG_FILTER_1 = null;

    // 搜索歌曲过滤条件
    public static String PLAYER_QUERY_SONG_FILTER = null;

    // 通话录音
    public static final String HB_DIAL_RECODE_DIR = "/CallRecording%\"";

    // 歌单头用于区别是自定义歌单
    public static final String HB_PLAYLIST_TIP = "HB";

    // 备忘录录音
    public static final String HB_NOTE_DIR = "/note/sound%\"";

    // 备忘录录音
    public static final String HB_NOTE_DIR2 = "/HMBIRDNote/sound%\"";
    public static final String HB_ANDROID_DATA = "/Android/%data%\"";
    public static final String HB_CALLRECORDING = "/CallRecording%\"";
    public static final boolean SHOW_SEARCH_LRC = false;
    public static final long HB_LOW_MEMORY = 3*1024*1024;//3M

    public final static String CLIENT_ID = "Nz3ne4pC7SiwvVm6hGf5h58j";

    public final static String CLIENT_SECRET = "UWdCGdx0R2ltELZ5B7io7t4xL0jIjRkW";

    public final static String SCOPE = "music_media_basic,music_musicdata_basic,music_search_basic,music_media_premium";

    public static final long LOW_MEMORY = HB_LOW_MEMORY*10;

    public static final String SYSTEM_SEPARATOR = System.getProperty("file.separator");

    public static boolean mTestMode = false;

    public static String storagePath = Environment.getExternalStorageDirectory().getPath();

    public static String BASE_PATH = storagePath + SYSTEM_SEPARATOR + "Music" + SYSTEM_SEPARATOR + "HBmusic";

    public static String mSavePath = BASE_PATH + SYSTEM_SEPARATOR + "song";

    public static String mTestPath = BASE_PATH + SYSTEM_SEPARATOR + "test9527";
    public static String baseLycPath = SYSTEM_SEPARATOR + "Music" + SYSTEM_SEPARATOR + "HBmusic"+SYSTEM_SEPARATOR + "lyric";

    public static String mLycPath = BASE_PATH + SYSTEM_SEPARATOR + "lyric";

    public static String mLycPath_temp = BASE_PATH + SYSTEM_SEPARATOR + "lyricTemp";//暂时不用

    public static String mCachePath = BASE_PATH + SYSTEM_SEPARATOR + "caches";

    public static String baseSongImagePath = SYSTEM_SEPARATOR + "Music" + SYSTEM_SEPARATOR + "HBmusic"+SYSTEM_SEPARATOR + "songImage";

    public static String mSongImagePath = BASE_PATH + SYSTEM_SEPARATOR + "songImage";

    public static void initPath(String path) {
        storagePath = path;
        BASE_PATH = storagePath + SYSTEM_SEPARATOR + "Music" + SYSTEM_SEPARATOR + "HBmusic";
        mSavePath = BASE_PATH + SYSTEM_SEPARATOR + "song";
        mLycPath = BASE_PATH +SYSTEM_SEPARATOR + "lyric";
        mLycPath_temp = BASE_PATH + SYSTEM_SEPARATOR + "lyricTemp";
        mCachePath = BASE_PATH + SYSTEM_SEPARATOR + "caches";
        mSongImagePath = BASE_PATH + SYSTEM_SEPARATOR + "songImage";
    }

    public static final String PREF_RECENTLY = "pref_recently";
    public static final String PREF_EXIT_NORMAL = "exit_normal";

    public static final int STATUS_NO_NORMAL_EXIT = 1000;

    // Settings.System
    public static final String PLAYER_NAVI_KEY_HIDE = "navigation_key_hide";

    //是否显示存储设置
    public static boolean STORAGE_PATH_SETTING = false;

    // 打开在线音乐开关
    public static boolean SWITCH_FOR_ONLINE_MUSIC = true;

    // 打开音效按钮 默认关闭
    public static boolean SWITCH_FOR_SOUND_CONTROL = false;

    // 在HBMusicUtil里面根据机型打开
    //没有菜单键
    public static boolean NO_MEUN_KEY=false;

    // 1 L 2 M 3 H //低中高
    public static int SWITCH_FOR_ONLINE_MUSIC_Quality = 1;
    // 需要登录XIMI才能获取H品质，M品质不一定存在，L一直存在

    // 分享
    public static final String SHARE_WX_APP_ID = "wx34dcceeaca99281e";
    public static final String SHARE_WX_APP_SECRET_ID = "c23eb80dd4b706cef3ae3777f40908cf";
    public static final int SHARE_TIMELINE_SUPPORTED_VERSION = 0x21020001;

    // 新浪微薄
    // public static final String SHARE_XLWB_APP_ID = "1762246712";
    public static final boolean SWITCH_FOR_TRANSPARENT_STATUS_BAR = true;

    // 参展版本开关
    public static final boolean SWITCH_FOR_CANZHAN = false;
}

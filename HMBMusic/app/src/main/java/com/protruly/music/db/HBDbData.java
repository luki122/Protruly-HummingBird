package com.protruly.music.db;

/**
 * Created by hujianwei on 17-8-29.
 */



public class HBDbData {

    //数据库名称
    public static final String DATABASE_NAME="hbmusic.db";

    //数据库版本
    public static final int VERSION=9;

    //共享数据
    public static final String SHARE_TABLENAME="hbshares";
    public static final String SHARE_ID="_id";
    public static final String SHARE_ISPLAYING="isPlaying";

    //歌词+歌曲信息+图片专辑
    public static final String AUDIOINFO_TABLENAME="audioinfo";
    public static final String AUDIOINFO_ID="_id";
    public static final String AUDIOINFO_SONG_ID="audio_id";
    public static final String AUDIOINFO_SONG_TITLE="audio_title";
    public static final String AUDIOINFO_SONG_ARTIST="audio_artist";
    public static final String AUDIOINFO_SONG_LRC="audio_lrc";

    public static final String AUDIOINFO_SONG_ALBUMPIC="albumpic_path";
    public static final String AUDIOINFO_SONG_ISNET="isnet";

    //最喜爱的歌曲
    public static final String FAVORITES_TABLENAME="favorites";
    public static final String FAVORITES_ID="_id";
    public static final String FAVORITES_AUDIO_ID="audio_id";
    public static final String FAVORITES_PLAYLIST_ID="playlist_id";
    public static final String FAVORITES_PLAY_ORDER="play_order";
    public static final String FAVORITES_TITLE="title";
    public static final String FAVORITES_ALBUMNAME="album_name";
    public static final String FAVORITES_ARTISTNAME="artist_name";
    public static final String FAVORITES_ISNET="isnet";
    public static final String FAVORITES_URI="path";

    //歌曲字段
    public static final String SONG_TABLENAME="song";
    public static final String SONG_ID="_id";
    public static final String SONG_AUDIO_ID="audio_id";
    public static final String SONG_ALBUMID="albumid";
    public static final String SONG_ARTISTID="artistid";
    public static final String SONG_NAME="name";
    public static final String SONG_DISPLAYNAME="displayName";
    public static final String SONG_NETURL="netUrl";
    public static final String SONG_DURATIONTIME="durationTime";
    public static final String SONG_SIZE="size";
    public static final String SONG_ISLIKE="isLike";
    public static final String SONG_LYRICPATH="lyricPath";
    public static final String SONG_FILEPATH="filePath";
    public static final String SONG_PLAYERLIST="playerList";
    public static final String SONG_ISNET="isNet";
    public static final String SONG_MIMETYPE="mimeType";
    public static final String SONG_ISDOWNFINISH="isDownFinish";

    public static final String SONG_ALBUMNAME="albumname";
    public static final String SONG_ARTISTNAME="artistname";
    public static final String SONG_DATAADDED="data_added";

    //专辑字段
    public static final String ALBUM_TABLENAME="album";
    public static final String ALBUM_ID="_id";
    public static final String ALBUM_NAME="name";
    public static final String ALBUM_PICPATH="picPath";

    //歌手字段
    public static final String ARTIST_TABLENAME="artist";
    public static final String ARTIST_ID="_id";
    public static final String ARTIST_NAME="name";
    public static final String ARTIST_PICPATH="picPath";

    //播放列表字段
    public static final String PLAYERLIST_TABLENAME="playerList";
    public static final String PLAYERLIST_ID="_id";
    public static final String PLAYERLIST_NAME="name";
    public static final String PLAYERLIST_DATE="date";

    //add by  20140711 start
    //收藏歌单字段
    public static final String COLLECT_TABLENAME="collectlist";
    public static final String COLLECT_ID="_id";
    public static final String COLLECT_NAME="name";
    public static final String COLLECT_PLAYLISTID="collectplaylistid";
    public static final String COLLECT_SONG_SIZE="songsize";
    public static final String COLLECT_IMG="imgurl";
    public static final String COLLECT_LIST_TYPE="code";
    public static final String COLLECT_TYPE="type"; //可选字段
    public static final String COLLECT_SHOU_INFO="show_info";

    //搜索历史字段
    public static final String SEARCH_HISTORY_TABLENAME="search_history";
    public static final String SEARCH_HISTORY_ID="_id";
    public static final String SEARCH_HISTORY_KEY="history_key";

}

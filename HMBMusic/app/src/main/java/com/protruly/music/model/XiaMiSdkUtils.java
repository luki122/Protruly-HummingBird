package com.protruly.music.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Pair;


import com.protruly.music.util.LogUtil;
import com.xiami.music.model.RadioCategory;
import com.xiami.sdk.XiamiSDK;
import com.xiami.sdk.entities.Banner;
import com.xiami.sdk.entities.OnlineAlbum;
import com.xiami.sdk.entities.OnlineArtist;
import com.xiami.sdk.entities.OnlineCollect;
import com.xiami.sdk.entities.OnlineRadio;
import com.xiami.sdk.entities.OnlineSong;
import com.xiami.sdk.entities.QueryInfo;
import com.xiami.sdk.entities.RadioCategoryNew;
import com.xiami.sdk.entities.RankType;
import com.xiami.sdk.entities.SearchSummaryResult;

/**
 * **17、如何显示与过滤已下架歌曲** 答： (1)、我们新添加了一个XiamiSDK的构造函数： ``` java public
 * XiamiSDK(Context context, String key, String secret,boolean showInvalidSongs)
 * ``` 最后一个参数showInvalidSongs表示是否显示无效歌曲，如果设置为true,则表示显示无效歌曲，如果设置为false，接口在返回歌曲时，
 * 会有无效歌曲过滤掉，只返回当前存在且可播放的歌曲。 (2)、在OnLineSong类中，有一个方法：Permission getPermission()
 * 可以获取歌曲的权限相关信息： Permission中有三个函数： ``` java /** 是否可用： (低品质不可试听 && VIP不免费试听) =>
 * SDK端视为下架
 * @return
 */
/*
 * public boolean isAvailable() { return available; }
 */
/**
 * 当前用户可听的最高品质： 前端根据该字段判断是否显示“HQ” 元素类型为string
 * @return
 */
/*
 * public List<String> getQuality() { return quality; }
 */
/**
 * 需要购买VIP的品质列表: 前端根据当前试听品质全局开关与该字段做比对， 若全局开关对应的品质在此列表中，则显示“VIP” 元素类型为string
 * @return
 */
/*
 * public List<String> getNeed_vip() { return need_vip; }
 */
/* @author jiangxh */

public class XiaMiSdkUtils {

    private static final String TAG = "XiaMiSdkUtils";
    private static XiamiSDK instance;
    public static final String KEY = "c72d4ae616a234b2e5f90a3c971deb74";
    public static final String SECRET = "e635f3a8c0f162fb03489e95d01c6aa3";

    private static XiamiSDK getXiamiSDK(Context context) {
        if (instance == null) {
            instance = new XiamiSDK(context, KEY, SECRET, true);
        }
        return instance;
    }

    /**
     * 获取专辑详情和歌曲列表（同步执行） 默认返回全部描述
     * @param context
     * @param albumId
     * @return
     */
    public static OnlineAlbum getAlbumsDetailSync(Context context, long albumId) {
        LogUtil.d(TAG, "getAlbumsDetailSync albumId:" + albumId);
        return getXiamiSDK(context).getAlbumsDetailSync(albumId);
    }

    /**
     * 获取艺人可试听专辑数和歌曲数（同步执行） 该字段已经包括在OnlineArtist中，建议通过艺人详情接口获取
     * @param context
     * @param artistId
     * @return
     */
    public static HashMap<String, Integer> fetchArtistCountInfoSync(Context context, long artistId) {
        LogUtil.d(TAG, "fetchArtistCountInfoSync artistId:" + artistId);
        return getXiamiSDK(context).fetchArtistCountInfoSync(artistId);
    }

    /**
     * 通过ArtistId获取对应艺人的专辑（同步执行）
     * @param context
     * @param artistId
     * @param pageSize
     * @param pageIndex
     * @return
     */
    public static Pair<QueryInfo, List<OnlineAlbum>> fetchAlbumsByArtistIdSync(Context context, long artistId, int pageSize, int pageIndex) {
        LogUtil.d(TAG, "fetchAlbumsByArtistIdSync artistId:" + artistId + " pageSize:" + pageSize + " pageIndex:" + pageIndex);
        return getXiamiSDK(context).fetchAlbumsByArtistIdSync(artistId, pageSize, pageIndex);
    }

    /**
     * 通过artistId获取艺人对用的歌曲（同步执行） 默认获取第一页前20个，如需获取分页数据请调用
     * @param context
     * @param artistId
     * @return
     */
    public static List<OnlineSong> fetchSongsByArtistIdSync(Context context, long artistId) {
        LogUtil.d(TAG, "fetchSongsByArtistIdSync artistId:" + artistId);
        return getXiamiSDK(context).fetchSongsByArtistIdSync(artistId);
    }

    /**
     * 获取艺人详情（同步执行） 默认返回介绍
     * @param context
     * @param artistId
     * @return
     */
    public static OnlineArtist fetchArtistDetailSync(Context context, long artistId) {
        LogUtil.d(TAG, "fetchArtistDetailSync artistId:" + artistId);
        return getXiamiSDK(context).fetchArtistDetailSync(artistId);
    }

    /**
     * 获取本周热门专辑（同步执行） 默认所有类型
     * @param context
     * @param pageSize
     * @param pageIndex
     * @return
     */
    public static Pair<QueryInfo, List<OnlineAlbum>> getWeekHotAlbumsSync(Context context, int pageSize, int pageIndex) {
        LogUtil.d(TAG, "getWeekHotAlbumsSync  pageSize:" + pageSize + " pageIndex:" + pageIndex);
        return getXiamiSDK(context).getWeekHotAlbumsSync(pageSize, pageIndex);
    }

    /**
     * 通过排行榜类型获取相应排行榜单（同步执行）
     * @param context
     * @param type
     * @return
     */
    public static List<OnlineSong> getRankSongsSync(Context context, RankType type) {
        LogUtil.d(TAG, "getRankSongsSync  type:" + type);
        return getXiamiSDK(context).getRankSongsSync(type);
    }

    /**
     * 获取精选集详情和歌曲列表（同步执行） 默认返回介绍
     * @param context
     * @param collectId
     * @return
     */
    public static OnlineCollect getCollectDetailSync(Context context, long collectId) {
        LogUtil.d(TAG, "getCollectDetailSync collectId:" + collectId);
        return getXiamiSDK(context).getCollectDetailSync(collectId);
    }

    /**
     * 根据id查询在线歌曲（同步执行）
     * @param context
     * @param id
     * @param quality
     * @return
     */
    public static OnlineSong findSongByIdSync(Context context, long id, OnlineSong.Quality quality) {
        LogUtil.d(TAG, "findSongByIdSync id:" + id + " Quality:" + quality);
        return getXiamiSDK(context).findSongByIdSync(id, quality);
    }

    /**
     * 搜索歌曲（同步执行）
     * @param context
     * @param keywords
     *            搜索关键字
     * @param pageSize
     * @param pageIndex
     * @return
     */
    public static Pair<QueryInfo, List<OnlineSong>> searchSongSync(Context context, String keywords, int pageSize, int pageIndex) {
        LogUtil.d(TAG, "searchSongSync  pageSize:" + pageSize + " pageIndex:" + pageIndex + " keywords:" + keywords);
        return getXiamiSDK(context).searchSongSync(keywords, pageSize, pageIndex);
    }

    /**
     * 搜索专辑（同步执行）
     * @param context
     * @param keywords
     * @param pageSize
     * @param pageIndex
     * @return
     */
    public static Pair<QueryInfo, List<OnlineAlbum>> searchAlbumsSync(Context context, String keywords, int pageSize, int pageIndex) {
        LogUtil.d(TAG, "searchAlbumsSync  pageSize:" + pageSize + " pageIndex:" + pageIndex + " keywords:" + keywords);
        return getXiamiSDK(context).searchAlbumsSync(keywords, pageSize, pageIndex);
    }

    /**
     * 搜索艺人（同步执行）
     * @param context
     * @param keywords
     * @param pageSize
     * @param pageIndex
     * @return
     */
    public static Pair<QueryInfo, List<OnlineArtist>> searchArtistsSync(Context context, String keywords, int pageSize, int pageIndex) {
        LogUtil.d(TAG, "searchArtistsSync  pageSize:" + pageSize + " pageIndex:" + pageIndex + " keywords:" + keywords);
        return getXiamiSDK(context).searchArtistsSync(keywords, pageSize, pageIndex);
    }

    /**
     * 搜索整合接口（同步执行）只能获取一页
     * @param context
     * @param keyWords
     *            - 关键词
     * @param pageSize
     *            - 每页数据容量
     * @return：返回整合后的结果，包括歌曲，专辑，艺人，精选集
     */
    public static SearchSummaryResult searchSummarySync(Context context, String keyWords, int pageSize) {
        LogUtil.d(TAG, "searchSummarySync  pageSize:" + pageSize + " keywords:" + keyWords);
        return getXiamiSDK(context).searchSummarySync(keyWords, pageSize);
    }

    /**
     * 获取推荐精选集（同步执行）
     * @param context
     * @param pageSize
     * @param pageIndex
     * @return
     */
    public static Pair<QueryInfo, List<OnlineCollect>> getCollectsRecommendSync(Context context, int pageSize, int pageIndex) {
        LogUtil.d(TAG, "getCollectsRecommendSync  pageSize:" + pageSize + " pageIndex:" + pageIndex);
        return getXiamiSDK(context).getCollectsRecommendSync(pageSize, pageIndex);
    }

    /**
     * 获取运营banner（同步执行）
     * @param context
     * @return
     */
    public static List<Banner> fetchBannerSync(Context context) {
        LogUtil.d(TAG, "fetchBannerSync");
        return getXiamiSDK(context).fetchBannerSync();
    }

    /**
     * 4.0获取电台分类列表，该接口为老接口，内容较老，建议用新接口（同步执行）
     * @param context
     * @return
     */
    public static List<RadioCategory> fetchRadioListsSync(Context context) {
        LogUtil.d(TAG, "fetchRadioListsSync");
        return getXiamiSDK(context).fetchRadioListsSync();
    }

    /**
     * 虾米5.0中根据电台分类获取电台列表接口
     * @param context
     * @param radioCategory
     * @param pageSize
     * @param pageIndex
     * @return
     */
    public static Pair<QueryInfo, List<OnlineRadio>> fetchRadioListSync(Context context, RadioCategoryNew radioCategory, int pageSize, int pageIndex) {
        LogUtil.d(TAG, "fetchRadioListSync  pageSize:" + pageSize + " pageIndex:" + pageIndex + " radioCategory:" + radioCategory);
        return getXiamiSDK(context).fetchRadioListSync(radioCategory, pageSize, pageIndex);
    }

    /**
     * 根据歌曲名及艺人名查询在线歌曲（同步执行） 歌曲名和艺人名不能为空，专辑名可以为空
     * @param context
     * @param songName
     * @param artistName
     * @param albumName
     * @return
     */
    public static List<OnlineSong> findSongByNameSync(Context context, String songName, String artistName, String albumName) {
        LogUtil.d(TAG, "findSongByNameSync  songName:" + songName + " artistName:" + artistName + " albumName:" + albumName);
        if (TextUtils.isEmpty(songName) || songName.equals(MediaStore.UNKNOWN_STRING) || TextUtils.isEmpty(artistName) || artistName.equals(MediaStore.UNKNOWN_STRING)) {
            return null;
        }
        if (TextUtils.isEmpty(albumName) || albumName.equals(MediaStore.UNKNOWN_STRING)) {
            albumName = "";
        }
        List<OnlineSong> onlineSongs = getXiamiSDK(context).findSongByNameSync(songName, artistName, albumName);
        LogUtil.d(TAG, "--onlineSongs:" + onlineSongs);
        if (onlineSongs != null) {
            LogUtil.d(TAG, "--onlineSongs:" + onlineSongs.size() + " onlineSongs:" + onlineSongs);
        }
        return onlineSongs;
    }

    /**
     * 是否开启log 支持0.1.1及以上版本
     * @param context
     * @param enable
     */
    public static void enableLog(Context context, boolean enable) {
        getXiamiSDK(context).enableLog(enable);
    }

    /**
     * 5.0获取电台歌曲接口
     * @param context
     * @param radio
     * @param limit
     * @return
     */
    public static List<OnlineSong> fetchRadioDetailSync(Context context, OnlineRadio radio, int limit) {
        LogUtil.d(TAG, "fetchRadioDetailSync  OnlineRadio:" + radio + " limit:" + limit);
        return getXiamiSDK(context).fetchRadioDetailSync(radio, limit);
    }

    /**
     * 4.0获取电台详情，默认50个（同步执行）
     * @param context
     * @param radioType
     * @param radioId
     * @return
     */
    public static List<OnlineSong> fetchRadioDetailSync(Context context, int radioType, long radioId) {
        LogUtil.d(TAG, "fetchRadioDetailSync  radioType:" + radioType + " radioId:" + radioId);
        return getXiamiSDK(context).fetchRadioDetailSync(radioType, radioId);
    }

    /**
     * 向文件写入TAG, 请异步调用只支持无TAG信息的文件，不支持覆盖和擦除 （SDK给出的音乐地址所下载的文件是可以写入的）
     * 输入和输出不能是同一个文件TAG写入完成后不会去删除输入的文件，请自行清理。
     * @param context
     * @param inFilePath
     * @param keyValues
     * @param apic
     * @param mimeType
     * @return
     */
    public static boolean writeFileTags(Context context, String inFilePath, Map<String, String> keyValues, byte[] apic, String mimeType) {
        LogUtil.d(TAG, "writeFileTags  inFilePath:" + inFilePath);
        return getXiamiSDK(context).writeFileTags(inFilePath, keyValues, apic, mimeType);
    }

    /**
     * 根据文件路径读取音频文件Tag
     * @param context
     * @param filePath
     *            - 文件路径
     * @return Tags map TITLE: 歌曲名 SINGER: 演唱艺人名 ALBUM: 专辑名 ARTIST: 专辑艺人名 DISC:
     *         CD号 TRACK: 轨道号
     */
    public static Map<String, String> readFileTags(Context context, String filePath) {
        LogUtil.d(TAG, "readFileTags  filePath:" + filePath);
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }
        return getXiamiSDK(context).readFileTags(filePath);
    }
}

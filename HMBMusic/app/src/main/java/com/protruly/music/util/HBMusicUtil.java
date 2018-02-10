package com.protruly.music.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ServiceManager;
import android.os.StatFs;
import android.os.SystemProperties;
import android.os.storage.IMountService;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Xml;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.protruly.music.MusicUtils;
import com.protruly.music.R;
import com.protruly.music.Application;
import com.xiami.music.model.Permission;
import com.xiami.sdk.entities.OnlineSong;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.android.internal.R.id.find;

/**
 * Created by hujianwei on 17-8-29.
 */

public class HBMusicUtil {
    private static final String TAG = "HBMusicUtil";

    public static int mCicleOffset = 0;

    public static int mCicleWidth = 0;

    private static final String CURRENT_PLAYLIST = "current_playlist";

    public static final String MUSIC_PATH = "music_config.xml";

    public static String[] mMusicPath = new String[] { "/netease/cloudmusic/Music", "/Baidu_music/download", "/DUOMI/down", "/kgmusic/download", "/KuwoMusic/music", "/qqmusic/song", "/ttpod/song",
            "/NubiaMusic/songs", "/诠音/download", "/xiami", };

    private static FrameLayout frameLayout;

    private static WindowManager wm;

    private static ImageView btn_floatView;

    private static boolean isFromHeadset = false;

    public static final String TIMEOFF_CLOSE_ACTION = "com.protruly.music.timeoffclose";

    public static final String PREF_CURRENT_ALARM_TIME = "current_alarm_time";

    public static final String PREF_TIMING_OFF = "timeoff_time";

    private static long current_alarm_time = 0;

    private static int set_timeoff = 0;

    public static final String lrcRegularExpressions = "<[0-9]{1,5}>";

    private static boolean isFromScreen = false;


    public static void showToast(Context context, String msg){
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }



    /**
     * 判断是否没有权限或只有VIP用户才能使用
     * @param item
     * @return
     */
    public static boolean isNoPermission(OnlineSong item){
        try {
            Permission permission= item.getPermission();
            if(permission==null){
                return false;
            }
            if(!permission.isAvailable()|permission.getNeed_vip().contains(HBMusicUtil.getOnlineSongQuality().toString().toLowerCase())){
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }


    /**
     * 格式化大小
     *
     * @param size
     * @return
     */
    public static String convertStorage(long size) {
        long kb = 1024;
        long mb = kb * 1024;
        long gb = mb * 1024;
        if (size >= gb) {
            return String.format("%.2f GB", (float) size / gb);
        } else if (size >= mb) {
            float f = (float) size / mb;
            return String.format(f > 100 ? "%.1f MB" : "%.2f MB", f);
        } else if (size >= kb) {
            float f = (float) size / kb;
            return String.format(f > 100 ? "%.1f KB" : "%.2f KB", f);
        } else
            return String.format("%d B", size);
    }



    public static class SDCardInfo {
        public long total;
        public long free;
        public long inUse;

        @Override
        public String toString() {
            return "SDCardInfo [total=" + total + ", free=" + free + ", inUse=" + inUse + "]";
        }

    }

    @SuppressWarnings("deprecation")
    public static SDCardInfo getSDCardInfo(boolean isMounted, String path) {
        SDCardInfo info = null;
        if (isMounted && path != null) {
            File pathFile = new File(path);
            info = new SDCardInfo();
            try {
                android.os.StatFs statfs = new android.os.StatFs(pathFile.getPath());

                // 获取SDCard上BLOCK总数
                long nTotalBlocks = statfs.getBlockCount();

                // 获取SDCard上每个block的SIZE
                long nBlocSize = statfs.getBlockSize();

                // 获取可供程序使用的Block的数量
                long nAvailaBlock = statfs.getAvailableBlocks();

                // 获取剩下的所有Block的数量(包括预留的一般程序无法使用的块)
                long nFreeBlock = statfs.getFreeBlocks();

                // 计算SDCard 总容量大小MB
                info.total = nTotalBlocks * nBlocSize;

                // 计算 SDCard 剩余大小MB
                // 极端条件
                if (nAvailaBlock == 0) {
                    info.free = nFreeBlock * nBlocSize;
                } else {
                    info.free = nAvailaBlock * nBlocSize;
                }
                BigDecimal total = new BigDecimal(info.total);
                BigDecimal free = new BigDecimal(info.free);
                info.inUse = total.subtract(free).longValue();

            } catch (Exception e) {
                LogUtil.e(TAG, "StatFs error " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
        return info;
    }

    /**
     * OnlineSong toString
     * @param song
     */
    public static void ShowOnlineSong(OnlineSong song) {
        if (song == null) {
            return;
        }
        LogUtil.d(TAG, "Song:" + " getAlbumId:" + song.getAlbumId() + " getAlbumName:" + song.getAlbumName() + " getArtistId:" + song.getArtistId() + " getArtistLogo:" + song.getArtistLogo()
                + " getArtistName:" + song.getArtistName() + " getCdSerial:" + song.getCdSerial() + " getEncodeRate:" + song.getEncodeRate() + " getExpire:" + song.getExpire() + " getImageName:"
                + song.getImageName() + " getImageUrl:" + song.getImageUrl() + " getLength:" + song.getLength() + " getListenFile:" + song.getListenFile() + " getLogo:" + song.getLogo()
                + " getLyric:" + song.getLyric() + " getQuality:" + song.getQuality() + " getReason:" + song.getReason() + " getSingers:" + song.getSingers() + " getSongId:" + song.getSongId()
                + " getSongName:" + song.getSongName() + " getTrack:" + song.getTrack());
    }

    /**
     * 需要登录XIMI才能获取H品质，M品质不一定存在，L一直存在
     * @return
     */
    public static OnlineSong.Quality getOnlineSongQuality() {
        if (Globals.SWITCH_FOR_ONLINE_MUSIC_Quality == 1) {
            return OnlineSong.Quality.L;
        } else if (Globals.SWITCH_FOR_ONLINE_MUSIC_Quality == 2) {
            return OnlineSong.Quality.M;
        } else {
            return OnlineSong.Quality.H;
        }
    }



    /**
     * 处理本地音乐专辑为父文件夹名称
     * @param path
     * @param mAlbumName
     * @return 如果为父文件夹名称 返回@MediaStore.UNKNOWN_STRING
     */
    public static String doAlbumName(String path, String mAlbumName) {
        if (TextUtils.isEmpty(path) || TextUtils.isEmpty(mAlbumName)) {
            return mAlbumName;
        }
        String folder = getPathFromFolder(path);
        if (!TextUtils.isEmpty(folder) && mAlbumName.equals(folder)) {
            return MediaStore.UNKNOWN_STRING;
        }
        return mAlbumName;
    }




    public static void initData(Context context) {
        if (mCicleOffset == 0 || mCicleWidth == 0) {
            mCicleOffset = DisplayUtil.dip2px(context, 0.5f);
            mCicleWidth = DisplayUtil.dip2px(context, 288);
        }

        return;
    }

    public static List<String> onGetMusicConfig(Context context) {
        return null;

    }

    public static String getExternalStoragePath(Context context) {
        return null;
    }


    /**
     * 获取存储器下歌曲封面图片路径
     * @param context
     * @param md5
     * @return
     */
    public static String getImgPath(Context context,String md5){
        try {
            if(TextUtils.isEmpty(md5)||context==null){
                LogUtil.e(TAG, "Error md5:"+md5+" context:"+context);
                return null;
            }
            List<String> storages = ((Application)context).getStoragePath();
            if(storages.size()<=0){
                LogUtil.e(TAG, "getImgPath Error no sd ");
                return null;
            }
            String filepath = storages.get(0)+Globals.baseSongImagePath + File.separator + md5;
            if (new File(filepath).exists()) {
                return filepath;
            }
            if(storages.size()>1){
                filepath = storages.get(1)+Globals.baseSongImagePath + File.separator + md5;
                if (new File(filepath).exists()) {
                    return filepath;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    /**
     * 获取SD卡指定路径下到歌词文件
     * @param title
     *            歌曲名
     * @param artistName
     *            专辑名
     * @return
     */
    public static String getLrcPathInfo(String title, String artistName) {
        String filepath = Globals.mLycPath + File.separator + title + "_" + artistName;
        if (new File(filepath + ".trc").exists()) {
            return filepath + ".trc";
        } else if (new File(filepath + ".lrc").exists()) {
            return filepath + ".lrc";
        } else if (new File(filepath + ".txt").exists()) {
            return filepath + ".txt";
        }
        return null;

    }




    /**
     * 获取SD卡指定路径下到歌词文件
     * @param fileName
     * @return
     */
    public static String getLrcPath(String fileName,Context context) {
        if(TextUtils.isEmpty(fileName)||context==null){
            LogUtil.e(TAG, "error fileName:"+fileName+" context:"+context);
            return null;
        }
        List<String> storages = ((Application)context).getStoragePath();
        if(storages.size()<=0){
            LogUtil.e(TAG, "getLrcPath error no sd ");
            return null;
        }
        String filepath = storages.get(0)+Globals.baseLycPath + File.separator + fileName;
        if (new File(filepath + ".trc").exists()) {
            return filepath + ".trc";
        } else if (new File(filepath + ".lrc").exists()) {
            return filepath + ".lrc";
        } else if (new File(filepath + ".txt").exists()) {
            return filepath + ".txt";
        }

        if(storages.size()>1){
            if(new File(storages.get(1)+Globals.baseLycPath).exists()){
                filepath = storages.get(1)+Globals.baseLycPath + File.separator + fileName;
                if (new File(filepath + ".trc").exists()) {
                    return filepath + ".trc";
                } else if (new File(filepath + ".lrc").exists()) {
                    return filepath + ".lrc";
                } else if (new File(filepath + ".txt").exists()) {
                    return filepath + ".txt";
                }
            }
        }

        return null;

    }

    /**
     * 通过文件路径 获取父文件夹名称
     * @param fileName
     * @return
     */
    public static String getPathFromFolder(String filepath) {

        int pos = filepath.lastIndexOf('/');
        if (pos != -1) {
            String path = filepath.substring(0, pos);
            int poss = path.lastIndexOf('/');
            if (poss != -1) {
                return path.substring(poss + 1, path.length());
            }
        }
        return "";
    }

    /**
     * 通过文件名获取文件后缀
     * @param filename
     * @return
     */
    public static String getExtFromFilename(String filename) {
        if (filename == null) {
            return "";
        }
        int dotPosition = filename.lastIndexOf('.');
        if (dotPosition != -1) {
            return filename.substring(dotPosition + 1, filename.length());
        }
        return "";
    }

    static String regEx = "<(.*?)>";
    static String regExs = "\\D";

    public static String[][] getTrc(String trc) {
        if (TextUtils.isEmpty(trc)) {
            return null;
        }
        try {
            String[][] trcS = new String[2][];
            trc = trc.trim();
            String[] trcString = trc.replaceAll(HBMusicUtil.lrcRegularExpressions, "#").replaceFirst("#", "").split("#");
            if (trcString.length == 0) {
                return null;
            }
            String[] trcInt = new String[trcString.length];
            Pattern p = Pattern.compile(regEx);
            Matcher m = p.matcher(trc);
            int i = 0;
            while (m.find()) {
                String tmp = m.group();
                if (TextUtils.isEmpty(tmp)) {
                    continue;
                }
                trcInt[i] = tmp.replaceAll(regExs, "");
                i++;
            }
            trcS[0] = trcInt;
            trcS[1] = trcString;
            return trcS;
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.e(TAG, "getTrc error", e);
        }
        return null;
    }

    /**
     * 创建配置文件，查找指定目录
     * @param v
     * @param event
     * @return
     */
    public static boolean onCreateConfigFile(Context context) {
        if (context == null) {
            return false;
        }

        String exitfile = context.getFilesDir().getPath() + File.separator + MUSIC_PATH;
        File file = new File(exitfile);
        if (file.exists()) {
            List<String> tList = onGetMusicConfig(context);
            if (tList != null && tList.size() == mMusicPath.length) {
                return true;
            }
        }

        List<String> paths = new ArrayList<String>();
        for (int i = 0; i < mMusicPath.length; i++) {
            paths.add(mMusicPath[i]);
        }

        String tmpStr = writeToString(paths);
        return writeToXml(context, tmpStr, MUSIC_PATH);
    }

    /**
     * 判断是否需要隐藏键盘
     * @param v
     * @param event
     * @return
     */
    public static boolean isShouldHideInput(View v, MotionEvent event) {
        if (v != null && (v instanceof EditText)) {
            int[] leftTop = { 0, 0 };
            v.getLocationInWindow(leftTop);
            int left = leftTop[0], top = leftTop[1], bottom = top + v.getHeight(), right = left + v.getWidth();
            if (event.getX() > left && event.getX() < right && event.getY() > top && event.getY() < bottom) {
                // 保留点击EditText的事件
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * 隐藏键盘
     * @param context
     * @param v
     * @return
     */
    public static Boolean hideInputMethod(Context context, View v) {
        if (v == null) {
            return false;
        }
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            return imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
        return false;
    }

    /**
     * 强制打开键盘
     * @param context
     * @return
     */
    public static boolean showInputMethod(Context context) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.toggleSoftInput(0, InputMethodManager.SHOW_FORCED);
        }
        return false;
    }

    /**
     * 判断给定字符串是否空白串。 空白串是指由空格、制表符、回车符、换行符组成的字符串 若输入字符串为null或空字符串，返回true
     * @param input
     * @return boolean
     */
    public static boolean isBlank(String input) {
        if (input == null || "".equals(input))
            return true;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断重命名是否存在
     * @param name
     * @param context
     * @return
     */
    public static int idForplaylist(String name, Context context) {
        Cursor c = MusicUtils.query(context, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, new String[] { MediaStore.Audio.Playlists._ID }, MediaStore.Audio.Playlists.NAME + "=?",
                new String[] { Globals.HB_PLAYLIST_TIP + name }, MediaStore.Audio.Playlists.NAME);
        int id = -1;
        if (c != null) {
            c.moveToFirst();
            if (!c.isAfterLast()) {
                id = c.getInt(0);
            }
            c.close();
        }
        return id;
    }

    public static String getNewPlaylistData(String name, int id, Context context) {

        Cursor c = MusicUtils.query(context, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, new String[] { MediaStore.Audio.Playlists.DATA }, MediaStore.Audio.Playlists._ID + "=" + id, null,
                MediaStore.Audio.Playlists.NAME);
        String data = null;
        if (c != null) {
            c.moveToFirst();
            if (!c.isAfterLast()) {
                data = c.getString(0);
                data = data.substring(0, data.lastIndexOf("/") + 1);
                LogUtil.d(TAG, "data1:" + data);
                data = data + name;
                LogUtil.d(TAG, "data2:" + data);
            }
            c.close();
        }
        return data;
    }

    /***
     * 图片的缩放方法
     * @param bgimage
     *            ：源图片资源
     * @param newWidth
     *            ：缩放后宽度
     * @param newHeight
     *            ：缩放后高度
     * @return
     */
    public static Bitmap zoomImage(Bitmap bgimage, double newWidth, double newHeight) {
        // 获取这个图片的宽和高
        float width = bgimage.getWidth();
        float height = bgimage.getHeight();
        // 创建操作图片用的matrix对象
        Matrix matrix = new Matrix();
        // 计算宽高缩放率
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 缩放图片动作
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap bitmap = Bitmap.createBitmap(bgimage, 0, 0, (int) width, (int) height, matrix, true);
        return bitmap;
    }

    /**
     * 画圆形图片
     * @param bitmap
     * @return
     */
    public static Bitmap getCircleBitmap(Bitmap bmp) {
        if (bmp == null) {
            return null;
        }

        Bitmap scaledSrcBmp;
        int diameter = mCicleWidth;
        int radius = mCicleWidth / 2;

        // 为了防止宽高不相等，造成圆形图片变形，因此截取长方形中处于中间位置最大的正方形图片
        int bmpWidth = bmp.getWidth();
        int bmpHeight = bmp.getHeight();
        int squareWidth = 0, squareHeight = 0;
        int x = 0, y = 0;
        Bitmap squareBitmap;

        boolean flag1 = false;
        boolean flag2 = false;
        if (bmpHeight > bmpWidth) {// 高大于宽
            squareWidth = squareHeight = bmpWidth;
            x = 0;
            y = (bmpHeight - bmpWidth) / 2;
            // 截取正方形图片
            squareBitmap = Bitmap.createBitmap(bmp, x, y, squareWidth, squareHeight);
            flag1 = true;
        } else if (bmpHeight < bmpWidth) {// 宽大于高
            squareWidth = squareHeight = bmpHeight;
            x = (bmpWidth - bmpHeight) / 2;
            y = 0;
            squareBitmap = Bitmap.createBitmap(bmp, x, y, squareWidth, squareHeight);
            flag1 = true;
        } else {
            squareBitmap = bmp;
        }

        if (squareBitmap.getWidth() != diameter || squareBitmap.getHeight() != diameter) {
            scaledSrcBmp = Bitmap.createScaledBitmap(squareBitmap, diameter, diameter, true);
            flag2 = true;
        } else {
            scaledSrcBmp = squareBitmap;
        }

        if (flag1 && squareBitmap != null && !squareBitmap.isRecycled()) {
            squareBitmap.recycle();
            squareBitmap = null;
        }

        Bitmap output = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, diameter, diameter);
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(radius, radius, radius - mCicleOffset / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(scaledSrcBmp, rect, rect, paint);

        if (flag2 && scaledSrcBmp != null && !scaledSrcBmp.isRecycled()) {
            scaledSrcBmp.recycle();
            scaledSrcBmp = null;
        }

        return output;
    }

    private static Bitmap CircleSmall(Bitmap bitmap) {

        if (bitmap == null) {
            return null;
        }

        Matrix matrix = new Matrix();
        float scaleWidth = ((float) mCicleWidth) / bitmap.getWidth();
        float scaleHeight = ((float) mCicleWidth) / bitmap.getHeight();
        matrix.postScale(scaleWidth, scaleHeight);

        Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return resizeBmp;
    }

    public static Bitmap setAlphaImgEx2(Bitmap sourceImg, int offset, int hight) {
        int w = sourceImg.getWidth();
        int h = sourceImg.getHeight();
        int[] argb = new int[w * h];
        sourceImg.getPixels(argb, 0, sourceImg.getWidth(), 0, 0, sourceImg.getWidth(), sourceImg.getHeight()); // get
        // the
        // ARGB
        // from
        // photo
        int linearAlphaOffset = hight;
        // int number = 0;
        int line = 0;

        int number1 = 0;
        int line1 = 0;
        int number2 = 0;

        for (int i = 0; i < argb.length; i++) {
            line = i / w;
            if (line < h / 2) {
                if (line < offset)
                    continue;

                number1 = (int) ((linearAlphaOffset + offset - line) * 255 / linearAlphaOffset);
                if (number1 <= 255) {
                    if (number1 < 0) {
                        number1 = 0;
                    }
                    argb[i] = (number1 << 24) | (argb[i] & 0x00FFFFFF); // change
                    // the
                    // highest
                    // 2
                    // bits
                } else {
                    continue;
                }
            } else {
                if (line >= h - offset - linearAlphaOffset) {
					/*
					 * if (line >= h-offset) { break; }
					 */

                    number2 = (int) ((line - h + linearAlphaOffset) * 255 / linearAlphaOffset);
                    if (number2 <= 255) {
                        argb[i] = (number2 << 24) | (argb[i] & 0x00FFFFFF); // change
                        // the
                        // highest
                        // 2
                        // bits
                    } else {
                        break;
                    }
                } else {
                    argb[i] = (number2 << 24) | (argb[i] & 0x00FFFFFF);
                }

            }

        }

        sourceImg = Bitmap.createBitmap(argb, sourceImg.getWidth(), sourceImg.getHeight(), Bitmap.Config.ARGB_8888);
        return sourceImg;
    }

    public static Bitmap setAlphaImgEx(Bitmap sourceImg, int offset, int hight) {
        int[] argb = new int[sourceImg.getWidth() * sourceImg.getHeight()];
        sourceImg.getPixels(argb, 0, sourceImg.getWidth(), 0, 0, sourceImg.getWidth(), sourceImg.getHeight()); // get

        int linearAlphaOffset = hight;

        int number = 0;
        for (int i = argb.length - 1; i >= 0; i--) {
            int j = sourceImg.getHeight() - i / sourceImg.getWidth() - offset;
            number = (int) (j * 255 / linearAlphaOffset);
            if (number <= 255) {
                argb[i] = (number << 24) | (argb[i] & 0x00FFFFFF); // change the

            } else {
                break;
            }
        }

        sourceImg = Bitmap.createBitmap(argb, sourceImg.getWidth(), sourceImg.getHeight(), Bitmap.Config.ARGB_8888);
        return sourceImg;
    }

    public static Bitmap setAlphaImg(Bitmap sourceImg, int offset, int hight) {
        int[] argb = new int[sourceImg.getWidth() * sourceImg.getHeight()];
        sourceImg.getPixels(argb, 0, sourceImg.getWidth(), 0, 0, sourceImg.getWidth(), sourceImg.getHeight()); // get

        int linearAlphaOffset = hight;

        int number = 0;
        for (int i = 0; i < argb.length; i++) {
            int j = i / sourceImg.getWidth() + offset;
            number = (int) (j * 255 / linearAlphaOffset);
            if (number <= 255) {
                argb[i] = (number << 24) | (argb[i] & 0x00FFFFFF); // change the

            } else {
                break;
            }
        }

        sourceImg = Bitmap.createBitmap(argb, sourceImg.getWidth(), sourceImg.getHeight(), Bitmap.Config.ARGB_8888);
        return sourceImg;
    }

    /**
     * 判断歌曲在playlist中是否存在
     * @param context
     * @param songId
     * @param playlistid
     * @return
     */
    public static boolean isForPlaylistById(Context context, long songId, long playlistid) {
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistid);
        Cursor c = MusicUtils.query(context, uri, new String[] { MediaStore.Audio.Playlists.Members._ID }, MediaStore.Audio.Playlists.Members.AUDIO_ID + "=?", new String[] { String.valueOf(songId) },
                null);
        int id = -1;
        if (c != null) {
            c.moveToFirst();
            if (!c.isAfterLast()) {
                id = c.getInt(0);
            }
        }
        c.close();
        return id >= 0 ? true : false;
    }

    public static List<String> getPlayListAudioId(Context context, long playlistid) {
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistid);
        List<String> audioIdList = new ArrayList<String>();
        Cursor c = null;
        try {
            c = MusicUtils.query(context, uri, new String[] { MediaStore.Audio.Playlists.Members.AUDIO_ID }, null, null, null);

            if (c != null && c.moveToFirst()) {
                do {
                    audioIdList.add(String.valueOf(c.getLong(0)));
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return audioIdList;
    }

    /**
     * 得到部分改变颜色text
     * @param context
     * @param value
     * @return
     */
    public static Spanned getTransColorText(Context context, int... value) {
        if (value.length < 2) {
            return null;
        }
        return Html.fromHtml(context.getString(R.string.hb_fold_num, "<font color=\"#000000\">" + value[0] + "</font>", "<font color=\"#000000\">" + value[1] + "</font>"));

    }

    /**
     * 渐入动画
     */
    public static void TranslateAnimationIn(View v, int hight, long durationMillis) {
        if (v == null) {
            return;
        }
        v.clearAnimation();

        Animation mInAnimation = new TranslateAnimation(0, 0, -hight, 0);
        mInAnimation.setDuration(durationMillis);
        v.startAnimation(mInAnimation);
        v.setVisibility(View.VISIBLE);
        return;
    }

    /**
     * 渐出动画
     */
    public static void TranslateAnimationOut(View v, int hight, long durationMillis) {
        if (v == null) {
            return;
        }
        v.clearAnimation();

        Animation mOutAnimation = new TranslateAnimation(0, 0, 0, -hight);
        mOutAnimation.setDuration(durationMillis);
        v.startAnimation(mOutAnimation);
        v.setVisibility(View.GONE);
        return;
    }

    /**
     * 渐显动画
     */
    public static void AlphaAnimationIn(View v, long durationMillis) {
        if (v == null) {
            return;
        }
        v.clearAnimation();

        Animation mInAlphaAnimation = new AlphaAnimation(0.0f, 1.0f);
        mInAlphaAnimation.setDuration(durationMillis);
        v.startAnimation(mInAlphaAnimation);
        v.setVisibility(View.VISIBLE);
        return;
    }

    /**
     * 渐隐动画
     */
    public static void AlphaAnimationOut(View v, long durationMillis) {
        if (v == null) {
            return;
        }
        v.clearAnimation();

        Animation mOutAlphaAnimation = new AlphaAnimation(1.0f, 0.0f);
        mOutAlphaAnimation.setDuration(durationMillis);
        v.startAnimation(mOutAlphaAnimation);
        v.setVisibility(View.GONE);
        return;
    }

    /**
     * 删除或移除歌曲提示
     * @param context
     * @param size
     * @param startMode
     */
    public static void showDeleteToast(Context context, int size, int startMode) {

        String message = null;
        if (startMode == 1) {
            message = context.getResources().getQuantityString(R.plurals.NNNtracksdeleted, size, size);
        } else {
            message = context.getResources().getString(R.string.hb_songs_to_remove, size);
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 将数据序列化为xml流
     * @param list
     * @return
     */
    public static String writeToString(List<String> list) {

        // 实现xml信息序列号的一个对象
        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();
        if (list.size() == 0) {
            return null;
        }
        try {
            // xml数据经过序列化后保存到String中，然后将字串通过OutputStream保存为xml文件
            serializer.setOutput(writer);
            // 文档开始
            serializer.startDocument("utf-8", true);
            // 开始一个节点
            serializer.startTag("", "paths");
            for (String path : list) {
                serializer.startTag("", "name");
                serializer.text(path);
                serializer.endTag("", "name");
            }
            serializer.endTag("", "paths");
            // 关闭文档
            serializer.endDocument();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return writer.toString();
    }

    public static void justTest() {
        LogUtil.d(TAG, " dump ",new Throwable());
    }

    public static void justTest(String STAG) {
        LogUtil.d(TAG, STAG+" dump ",new Throwable());
    }

    /**
     * 将数据保存为私有xml文件
     * @param context
     * @param str
     * @return
     */
    public static boolean writeToXml(Context context, String str, String filepath) {
        if (filepath == null) {
            return false;
        }
        if (str == null) {
            str = "";
        }
        try {
            OutputStream out = context.openFileOutput(filepath, Context.MODE_PRIVATE);
            OutputStreamWriter outw = new OutputStreamWriter(out);
            try {
                outw.write(str);
                outw.close();
                out.close();
                return true;
            } catch (IOException e) {
                return false;
            }
        } catch (FileNotFoundException e) {

            return false;
        }
    }

    /**
     * pull 解析xml文件
     * @param context
     * @return
     */
    public static List<String> doParseXml(Context context, String filepath) {

        if (filepath == null) {
            return null;
        }

        List<String> paths = new ArrayList<String>();
        String path = null;

        try {
            XmlPullParserFactory pullParserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = pullParserFactory.newPullParser();
            InputStream in = context.openFileInput(filepath);
            parser.setInput(in, "utf-8");
            // 获取事件类型
            int eventType = parser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                String name = parser.getName();
                switch (eventType) {
                    // 文档开始
                    case XmlPullParser.START_DOCUMENT:

                        break;
                    case XmlPullParser.START_TAG:
                        if ("name".equals(name)) {
                            parser.next();
                            path = parser.getText();

                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if ("name".equals(name)) {

                            paths.add(path);
                        }
                        break;
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {

            // e.printStackTrace();
        } catch (IOException e) {

            // e.printStackTrace();
        }
        return paths;
    }

    public final static String MD5(String s) {
        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

        try {
            byte[] btInput = s.getBytes();
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            mdInst.update(btInput);
            byte[] md = mdInst.digest();
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param context
     * @param value
     */
    public static void setCurrentPlaylist(Context context, int value) {
        MusicUtils.setIntPref(context, CURRENT_PLAYLIST, value);
    }

    /**
     * -1表示不是歌单 －2 表示我喜欢的歌曲 其他id对应歌单id
     * @param context
     * @return
     */
    public static int getCurrentPlaylist(Context context) {

        return MusicUtils.getIntPref(context, CURRENT_PLAYLIST, -1);
    }

    /**
     * @param str1
     * @param str2
     */

    public static float getSimilarity(String str1, String str2) {
        // 计算两个字符串的长度。
        int len1 = str1.length();
        int len2 = str2.length();
        // 建立上面说的数组，比字符长度大一个空间
        int[][] dif = new int[len1 + 1][len2 + 1];
        // 赋初值，步骤B。
        for (int a = 0; a <= len1; a++) {
            dif[a][0] = a;
        }
        for (int a = 0; a <= len2; a++) {
            dif[0][a] = a;
        }
        // 计算两个字符是否一样，计算左上的值
        int temp;
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    temp = 0;
                } else {
                    temp = 1;
                }
                // 取三个值中最小的
                dif[i][j] = min(dif[i - 1][j - 1] + temp, dif[i][j - 1] + 1, dif[i - 1][j] + 1);
            }
        }

        // 计算相似度
        float similarity = 1 - (float) dif[len1][len2] / Math.max(str1.length(), str2.length());
        return similarity;

    }

    private static int min(int... is) {
        int min = Integer.MAX_VALUE;
        for (int i : is) {
            if (min > i) {
                min = i;
            }
        }
        return min;
    }

    public static HashMap<String, Boolean> traverseFolder(String path) {
        File file = new File(path);
        HashMap<String, Boolean> hashMap = new HashMap<String, Boolean>();
        if (file.exists()) {
            File[] files = file.listFiles();
            for (File file2 : files) {
                if (!file2.isDirectory()) {
                    int end = file2.getName().lastIndexOf(".");
                    if (end > 0) {
                        hashMap.put(String.valueOf(file2.getName().substring(0, end).toLowerCase()), true);
                    }
                }
            }
        }
        return hashMap;

    }

    // add end

    private static IMountService mountService = IMountService.Stub.asInterface(ServiceManager.getService("mount"));

    public static boolean sdIsMounted(String mount) {
        try {
            if (mountService.getVolumeState(mount).equals(android.os.Environment.MEDIA_MOUNTED)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    /**
     * 启动调用获取配置信息
     */
    public static void setMusicProp() {
        try {
            String prop = SystemProperties.get("ro.product.model");

            LogUtil.d(TAG, "- setMusicProp 1 prop:" + prop);
            Globals.currentMode = prop;
            if (prop.equalsIgnoreCase("V10") ) {
                Globals.SWITCH_FOR_SOUND_CONTROL = true;
            } else {
                Globals.SWITCH_FOR_SOUND_CONTROL = false;
            }

            if(prop.equalsIgnoreCase("V10")){
                Globals.STORAGE_PATH_SETTING=true;
            }else {
                Globals.STORAGE_PATH_SETTING=false;
            }

            if(prop.equalsIgnoreCase("V10")){
                Globals.NO_MEUN_KEY = true;
            }else {
                Globals.NO_MEUN_KEY = false;
            }
            LogUtil.d(TAG, "SWITCH_FOR_SOUND_CONTROL:" + Globals.SWITCH_FOR_SOUND_CONTROL);
            String value = SystemProperties.get("phone.type.oversea");
            LogUtil.i(TAG, "setMusicProp 2 value:" + value);
            if (!value.equalsIgnoreCase("true") || TextUtils.isEmpty(value)) {
                Globals.SWITCH_FOR_ONLINE_MUSIC = true;
            } else {
                Globals.SWITCH_FOR_ONLINE_MUSIC = false;
            }
            if (Globals.SWITCH_FOR_CANZHAN) {
                Globals.SWITCH_FOR_ONLINE_MUSIC = false;
            }

        } catch (Exception e) {

        }

    }

    /**
     * 判断文件是否存在
     * @param path
     * @return
     */
    public boolean isFileExists(String path){
        if(TextUtils.isEmpty(path)){
            return false;
        }
        File file = new File(path);
        if(file.exists()){
            return true;
        }
        return false;
    }

    public static boolean isMTKPlatforms() {
        try {
            String prop = SystemProperties.get("ro.mtk_audio_profiles");
            if (prop != null) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
        return false;
    }


    public static boolean isIndiaVersion() {
        String india = SystemProperties.get("ro.iuni.country.option");
        LogUtil.d(TAG, "isindia:" + india);
        if (india.equalsIgnoreCase("india")) {// INDIA
            return true;
        }
        return false;
    }


    public static void getMountedStorage(Context context) {

        StorageManager mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        StorageVolume[] storageVolume = getVolumeList(mStorageManager);
        StringBuilder builder = new StringBuilder();
        builder.append(Globals.SIZE_FILTER);
        int k = 0;
        for (int i = 0; i < storageVolume.length; i++) {

            //反射获取getPath
            String temp = getPath(storageVolume[i]);

            LogUtil.d(TAG, "getMountedStorage:" + temp);
            try {
                if (sdIsMounted(temp)) {
                    LogUtil.d(TAG, "mounted path:" + temp);
                    if (k == 0) {
                        builder.append(" AND ( (" + MediaStore.Audio.Media.DATA + " LIKE \"" + temp + "%\"");
                        builder.append(" AND " + MediaStore.Audio.Media.DATA + " NOT LIKE \"" + temp + Globals.HB_DIAL_RECODE_DIR);
                        builder.append(" AND " + MediaStore.Audio.Media.DATA + " NOT LIKE \"" + temp + Globals.HB_NOTE_DIR);
                        builder.append(" AND " + MediaStore.Audio.Media.DATA + " NOT LIKE \"" + temp + Globals.HB_ANDROID_DATA);
                        builder.append( "AND " + MediaStore.Audio.Media.DATA + " NOT LIKE \"" + temp + Globals.HB_CALLRECORDING);
                        builder.append(" AND " + MediaStore.Audio.Media.DATA + " NOT LIKE \"" + temp + Globals.HB_NOTE_DIR2 + " )");
                    } else {
                        builder.append(" OR (" + MediaStore.Audio.Media.DATA + " LIKE \"" + temp + "%\"");
                        builder.append(" AND " + MediaStore.Audio.Media.DATA + " NOT LIKE \"" + temp + Globals.HB_DIAL_RECODE_DIR);
                        builder.append(" AND " + MediaStore.Audio.Media.DATA + " NOT LIKE \"" + temp + Globals.HB_NOTE_DIR);
                        builder.append(" AND " + MediaStore.Audio.Media.DATA + " NOT LIKE \"" + temp + Globals.HB_ANDROID_DATA);
                        builder.append(" AND " + MediaStore.Audio.Media.DATA + " NOT LIKE \"" + temp + Globals.HB_NOTE_DIR2 + " )");
                    }
                    k++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (k > 0) {
            builder.append(")");
        }

        // LogUtil.d(TAG, "builder:" + builder.toString());
        Globals.QUERY_SONG_FILTER = builder.toString();
        Globals.QUERY_SONG_FILTER_1 = builder.toString();
 ;
    }

    private static byte[] mSynch = new byte[0];
    private static List<String> mPathsXml = new ArrayList<String>();
    private static List<String> mIds = new ArrayList<String>();

    public static void refreshQueryFilter(Context context) {
        synchronized (mSynch) {
            String tmpfilter = Globals.QUERY_SONG_FILTER;
            refreshQueryFilterEx(context);

            StringBuilder where = new StringBuilder();
            int num = mIds.size();
            where.append(MediaStore.Audio.Media._ID + " NOT IN (");
            for (int i = 0; i < num; i++) {
                where.append(Long.parseLong(mIds.get(i)));
                if (i < num - 1) {
                    where.append(",");
                }
            }

            where.append(") AND (" + tmpfilter + ")");
            Globals.QUERY_SONG_FILTER = where.toString();
        }

        return;
    }

    public static String getFileString(Context context) {
        synchronized (mSynch) {
            refreshQueryFilterEx(context);
            StringBuilder where = new StringBuilder();
            int num = mIds.size();
            if (num == 0) {
                return where.toString();
            }
            where.append(" AND " + MediaStore.Audio.Media._ID + " NOT IN (");
            for (int i = 0; i < num; i++) {
                where.append(Long.parseLong(mIds.get(i)));
                if (i < num - 1) {
                    where.append(",");
                }
            }
            where.append(")");
            LogUtil.d(TAG, "where:" + where.toString());
            return where.toString();
        }
    }

    private static List<String> refreshQueryFilterEx(Context context) {
        {
            StringBuilder builder = new StringBuilder();
            List<String> list = HBMusicUtil.doParseXml(context, "paths.xml");
            int len = list.size();
            if (len < 0) {
                return null;
            }

            boolean changed = false;
            // HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
            for (int i = 0; i < len; i++) {
                String path = list.get(i);
                if (path != null && !TextUtils.isEmpty(path)) {
                    int bkt = path.toLowerCase().hashCode();
                    if (i == 0) {
                        builder.append(MediaStore.Audio.Media.DATA + " LIKE \"" + path + "/%\"");
                    } else {
                        builder.append(" OR " + MediaStore.Audio.Media.DATA + " LIKE \"" + path + "/%\"");
                    }

                    if (!mPathsXml.contains(path)) {
                        changed = true;
                    }
                }
            }

			/*
			 * if (!changed && len != mPathsXml.size()) { changed = true; }
			 * 
			 * if (!changed) { return mIds; }
			 */

            Cursor cursor = null;
            try {

                cursor = MusicUtils.query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[] { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA }, builder.toString(), null,
                        null);
                if (cursor != null) {
                    int count = cursor.getCount();
                    if (count <= 0) {
                        return null;
                    }

                    cursor.moveToFirst();
                    mIds.clear();
                    if (!cursor.isAfterLast()) {
                        do {
                            String path = cursor.getString(1);
                            if (path != null) {
                                String dir = path.substring(0, path.lastIndexOf("/"));
                                if (list.contains(dir)) {
                                    mIds.add(String.valueOf(cursor.getLong(0)));
                                }
                            }
                        } while (cursor.moveToNext());
                    }

                    mPathsXml.clear();
                    mPathsXml.addAll(list);
                    return mIds;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                }
            }

            return null;
        }
    }

    public static boolean isNavigationBarHidden(Context context) {
        int hide = Settings.System.getInt(context.getContentResolver(), Globals.PLAYER_NAVI_KEY_HIDE, 0);
        return (hide == 1);
    }

    /**
     * 通过改写 NAVI_KEY_HIDE 的值，可以控制虚拟键的显示/隐藏。 hide = true, 写入1，代表隐藏虚拟键 hide =
     * false, 写入0，代表显示虚拟键
     */
    public static void hideNaviBar(Context context, boolean hide) {
		/*
		 * if (hide == isNavigationBarHidden(context)) { return; }
		 */

        ContentValues values = new ContentValues();
        values.put("name", Globals.PLAYER_NAVI_KEY_HIDE);
        values.put("value", (hide ? 1 : 0));
        ContentResolver cr = context.getContentResolver();
        cr.insert(Settings.System.CONTENT_URI, values);
    }

    public static int getNavigationBarPixelHeight(Context context) {
        int height = 0;
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            height = resources.getDimensionPixelSize(resourceId);
        }
        return height;
    }

    @SuppressLint("NewApi")
    public static void hideSystemUINavi(View view, boolean hide) {
        if (hide) {
            view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
			/* | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY */);
        } else {
            view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    public static boolean isNetWorkActive(Context context) {

        boolean gprsNetDialog = MusicUtils.getIntPref(context, "network_dialog_gprs", 0)==0;
        if(gprsNetDialog){
            return false;
        }
        boolean success = false;
        // 获得网络连接服务
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        // 获取WIFI网络连接状态
        NetworkInfo.State state = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
        // 判断是否正在使用WIFI网络
        if (NetworkInfo.State.CONNECTED == state) {
            success = true;
        }
        // 获取GPRS网络连接状态
        state = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();
        // 判断是否正在使用GPRS网络
        if (NetworkInfo.State.CONNECTED == state) {
            success = true;
        }
        LogUtil.d(TAG, "network state:" + success);
        return success;
    }

    public static boolean isGprsNetActive(Context context) {
        boolean success = false;
        // 获得网络连接服务
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        // 获取GPRS网络连接状态
        NetworkInfo.State state = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();
        // 判断是否正在使用GPRS网络
        if (NetworkInfo.State.CONNECTED == state) {
            success = true;
        }
        LogUtil.d(TAG, "isGprsNetActive state:" + success);
        return success;
    }

    public static boolean isWifiNetActvie(Context context) {
        boolean success = false;
        // 获得网络连接服务
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        // 获取WIFI网络连接状态
        NetworkInfo.State state = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
        // 判断是否正在使用WIFI网络
        if (NetworkInfo.State.CONNECTED == state) {
            success = true;
        }
        return success;
    }

    public static String formatCurrentTime(Context context, String time) {

        try {
            SimpleDateFormat format = new SimpleDateFormat(context.getString(R.string.hb_collect_time));
            time = context.getString(R.string.hb_collect_show_time) + format.format(new Date(Long.parseLong(time)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return time;
    }


    public static DisplayMetrics getDisplay(Activity activity) {
        DisplayMetrics mDisplayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
        return mDisplayMetrics;
    }

    public static void addflyWindow(Context context) {
        if (frameLayout != null)
            return;
        wm = (WindowManager) context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        frameLayout = new FrameLayout(context.getApplicationContext());
        btn_floatView = new ImageView(context.getApplicationContext());
        btn_floatView.setImageResource(R.drawable.hb_note3);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        WindowManager.LayoutParams params2 = new WindowManager.LayoutParams();
        params2.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params2.height = WindowManager.LayoutParams.WRAP_CONTENT;
        frameLayout.addView(btn_floatView, params2);
        params.token = btn_floatView.getWindowToken();

        // 设置window type
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        ;

        params.gravity = Gravity.LEFT | Gravity.TOP;
		/*
		 * 如果设置为params.type = WindowManager.LayoutParams.TYPE_PHONE; 那么优先级会降低一些,
		 * 即拉下通知栏不可见
		 */

        params.format = PixelFormat.RGBA_8888; // 设置图片格式，效果为背景透明

        // 设置Window flag
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
		/*
		 * 下面的flags属性的效果形同“锁定”。 悬浮窗不可触摸，不接受任何事件,同时不影响后面的事件响应。
		 * wmParams.flags=LayoutParams.FLAG_NOT_TOUCH_MODAL |
		 * LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_NOT_TOUCHABLE;
		 */

        // 设置悬浮窗的长得宽
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        wm.addView(frameLayout, params);
    }

    public static void startFly(Context context, int flystartPointX, int flyendPointX, int flystartPointY, int flyendPointY, boolean flag) {
        startFly(context, flystartPointX, flyendPointX, flystartPointY, flyendPointY, null, flag);
    }

    public static void startFly(Context context, int flystartPointX, int flyendPointX, int flystartPointY, int flyendPointY, final AnimationEndListener listener, boolean flag) {
        addflyWindow(context);
        if (flag) {
            flyendPointX += context.getResources().getDimension(R.dimen.hb_bezier_endx);
            flyendPointY -= context.getResources().getDimension(R.dimen.hb_bezier_endy);
            flystartPointX += context.getResources().getDimension(R.dimen.hb_bezier_startx);
            flystartPointY -= context.getResources().getDimension(R.dimen.hb_bezier_starty);
        } else {
            flyendPointX += context.getResources().getDimension(R.dimen.hb_bezier_endx);
            flyendPointY -= context.getResources().getDimension(R.dimen.hb_bezier_endy);
            flystartPointX += context.getResources().getDimension(R.dimen.hb_bezier_startx);
            flystartPointY -= context.getResources().getDimension(R.dimen.hb_bezier_starty2);
        }
        AnimationSet set = new AnimationSet(true);

        TranslateAnimation animation = new TranslateAnimation(flystartPointX, flyendPointX, flystartPointY, flyendPointY);
        AlphaAnimation animation2 = new AlphaAnimation(1, 0.5f);
        set.addAnimation(animation);
        set.addAnimation(animation2);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.setDuration(800);
        set.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                // TODO Auto-generated method stub
                btn_floatView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // TODO Auto-generated method stub
                btn_floatView.setVisibility(View.GONE);
                if (listener != null) {
                    listener.onEnd();
                }
            }
        });

        btn_floatView.startAnimation(set);
    }

    public static void clearflyWindown() {
        if (btn_floatView != null) {
            btn_floatView.clearAnimation();
            btn_floatView.setVisibility(View.GONE);
        }
        if (frameLayout != null) {
            try {
                wm.removeView(frameLayout);
                frameLayout = null;
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }

    public static interface AnimationEndListener {
        public void onEnd();
    }

    public static boolean readSDCard() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File sdcardDir = Environment.getExternalStorageDirectory();
            StatFs sf = new StatFs(sdcardDir.getPath());
            long blockSize = sf.getBlockSize();
            long blockCount = sf.getBlockCount();
            long availCount = sf.getAvailableBlocks();
			/*
			 * LogUtil.d("", "block大小:" + blockSize + ",block数目:" + blockCount +
			 * ",总大小:" + blockSize * blockCount / 1024 + "KB"); LogUtil.d("",
			 * "可用的block数目：:" + availCount + ",剩余空间:" + availCount blockSize /
			 * 1024 + "KB");
			 */
            long availsize = availCount * blockSize;
            if (availsize < 50 * 1024 * 1024) {
                return false;
            }
            return true;
        }
        return false;
    }

    public static void removeBitmap(String id, String name) {
    }

    public static void setScreenFlag(boolean is) {
        isFromScreen = is;
    }

    public static boolean getScreenFlag() {
        return isFromScreen;
    }

    public static void setHeadsetFlag(boolean is) {
        isFromHeadset = is;
    }

    public static boolean isFromHeadset() {
        return isFromHeadset;
    }

    public static void startAlarmToClose(Context context, int time) {
        current_alarm_time = System.currentTimeMillis();
        set_timeoff = time;
        startAlarmToClose(context);
    }

    public static void startAlarmToClose(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(TIMEOFF_CLOSE_ACTION);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        am.cancel(sender);
        if (set_timeoff > 0) {
            am.set(AlarmManager.RTC_WAKEUP, current_alarm_time + set_timeoff * (60 * 1000), sender);
			/*
			 * if(!Build.MODEL.equals("IUNI N1")){
			 * am.set(AlarmManager.RTC_WAKEUP,
			 * current_alarm_time+set_timeoff*(60*1000), sender); return; }
			 * if(getLiveAlarmTime()<=getTimeoff()/3){
			 * am.set(AlarmManager.RTC_WAKEUP,
			 * current_alarm_time+set_timeoff*(60*1000), sender); }else
			 * if(getLiveAlarmTime()<=getTimeoff()*2/3){
			 * am.set(AlarmManager.RTC_WAKEUP,
			 * current_alarm_time+set_timeoff*(60*1000)*2/3, sender); }else {
			 * am.set(AlarmManager.RTC_WAKEUP,
			 * current_alarm_time+set_timeoff*(60*1000)/3, sender); }
			 */
        } else {
            resetAlarmTime(context);
        }
    }

    public static boolean isStartTimingClose() {
        LogUtil.d(TAG, "set_timeoff:" + set_timeoff);
        return set_timeoff > 0 ? true : false;
    }

    public static long getTimeoff() {
        return set_timeoff * 1000 * 60;
    }

    public static void resetAlarmTime(Context context) {
        LogUtil.d(TAG, "resetAlarmTime:" + set_timeoff);
        current_alarm_time = 0;
        set_timeoff = 0;
        MusicUtils.setIntPref(context, "time_select", 0);
    }

    public static long getLiveAlarmTime() {
        if (set_timeoff == 0) {
            return 0;
        }
        long time = (set_timeoff * 1000 * 60 - (System.currentTimeMillis() - current_alarm_time));
        LogUtil.d(TAG, "livetime:" + time);
        if (time < 0) {
            time = 0;
        }
        return time;
    }

    public static String getTimeString(Context context, long time) {
        SimpleDateFormat format = new SimpleDateFormat("mm:ss");
        String strTime = context.getString(R.string.hb_close_on_time) + "（" + format.format(new Date(time)) + "）";
        LogUtil.d(TAG, "strlivetime:" + strTime);
        return strTime;
    }

    /**
     * 4.3及以下版本
     * @return
     */
    public static boolean isLowVersion() {
        int version = android.os.Build.VERSION.SDK_INT;
        if (version <= 18) {
            return true;
        }
        return false;
    }


    public static StorageVolume[] getVolumeList(StorageManager storageManager){
        try {
            Class clz = StorageManager.class;
            Method getVolumeList = clz.getMethod("getVolumeList", null);
            StorageVolume[] result = (StorageVolume[]) getVolumeList.invoke(storageManager, null);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getPath(StorageVolume sv){
        try {
            Method getPath = StorageVolume.class.getMethod("getPath");
            String result = (String) getPath.invoke(sv, null);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }




    public static boolean getVolumeState(StorageManager storageManager,final String path){
        try {
            Method getVolumeState = StorageManager.class.getMethod("getVolumeState", String.class);
            String state = (String) getVolumeState.invoke( storageManager,path);
            LogUtil.d(TAG, "getVolumeState path:" + path + " state:" + state);
            return state.equals(android.os.Environment.MEDIA_MOUNTED);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}

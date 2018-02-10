package com.protruly.music.widget;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.protruly.music.MusicUtils;
import com.protruly.music.R;
import com.protruly.music.util.DisplayUtil;
import com.protruly.music.util.Globals;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.protruly.music.util.HBMusicUtil;
import com.protruly.music.widget.HBScrollView.ScrollViewListener;
import com.protruly.music.util.LogUtil;



/**
 * Created by hujianwei on 17-8-30.
 */

public class HBLyricView extends View implements View.OnTouchListener, ScrollViewListener{

    private final static String TAG = "HBLyricView";
    /** 每行歌词的长度 */
    private int textLength;
    /** 当前行位置 */
    private int currentLine = -1;
    /** y轴值，取控件中间位置 */
    private float middleY;
    /** y轴值，第一句歌词起始位置处于中间 */
    private float startCenterY = -1;
    /** y轴值，当前歌词Y位置 */
    private float curLyricY;
    /** x轴值，取控件中间位置 */
    private float middleX;
    /*** 每一行的间隔 */
    private int lineGap;
    /** 字体大小 */
    private int mLyricSize;
    /** 歌词paint */
    private Paint lycPaint;
    /** 当前歌词paint */
    private Paint currentPaint;
    /*** 上下位置偏移 */
    private int positionOffset;
    /*** 是否有歌词 */
    private Boolean isHasLyric = true;
    /*** 歌词view的高度 ***/
    private static int lyricViewHeight = -1;
    /*** 当前歌词的第几个字 */
    private int lyricsWordIndex = -1;
    /*** 当前歌词第几个字 已经播放的时间 */
    private int lyricsWordHLEDTime = 0;
    /*** 当前歌词第几个字 已经播放的长度 */
    private float lineLyricsHLWidth = 0;

    private Rect rect = new Rect();

    private int textColorFocusRead;
    private int textColorFocus;
    private LinearGradient shader;
    private float readPosition = 0;
    private static final float readPositionOffset = 0.0001f;
    private int[] textColors;

    private int moffset = 0;
    private Paint noLycPaint;
    private int noLyricSize;

    private boolean isFullScreen = false;
    private DisplayMetrics outMetrics;
    private HBScrollView player_lyric_scroll = null;
    private int flushIsSmoothFlag = -1;
    private boolean isTouchScrollView = false;
    private Runnable modifyScrollFlagRun = null;
    private int scrollDelayTime = 1;
    private Context mContext = null;

    private boolean mHideText = false;
    private boolean mbLrcValid = false;

    private TreeMap<Integer, LyricInfo> lrc_map = null;
    public static final byte hb_TEXT_ENCODING_GBK = 0;
    public static final byte hb_TEXT_ENCODING_UTF_16 = 1;
    public static final byte hb_TEXT_ENCODING_UTF_16BE = 2;
    public static final byte hb_TEXT_ENCODING_UTF_8 = 3;
    public static final byte hb_TEXT_ENCODING_UTF_16LE = 4;


    private boolean isLyricScroll = true;

    private class LyricInfo {
        public String strlrc;					// 歌词
        public int starttime;					// 开始时间
        public int endtime;					// 结束时间
        public int timelrc;					// 每句歌词时时间
        public boolean isTrcLrc;					// 判断是否是TRC歌词
        // 只有为true时lyricsWords和wordsDisInterval 有效
        public ArrayList<String> textArray; // strlrc 分行
        public ArrayList<Float> floats;  // textArray中每行长短
        public String[] lyricsWords = null;	// 歌词数组，用来分隔每个歌词
        public String[] wordsDisInterval = null;	// 数组，用来存放每个歌词的时间

        @Override
        public String toString() {
            return "LyricInfo [strlrc=" + strlrc + ", starttime=" + starttime + ", endtime=" + endtime + ", timelrc=" + timelrc + ", isTrcLrc=" + isTrcLrc + ", lyricsWords="
                    + Arrays.toString(lyricsWords) + ", wordsDisInterval=" + Arrays.toString(wordsDisInterval) + "]";
        }

    }

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_SMOOTHSCROLL) {
                issmoothScrolling = false;
            } else if (msg.what == MSG_INVALIDATE) {
                invalidate();
            } else if (msg.what == MSG_SCROLLVIEW) {
                isTouchScrollView = false;
            }
        }

    };

    public HBLyricView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        mHideText = false;
        mbLrcValid = false;

        if (lrc_map == null) {
            lrc_map = new TreeMap<Integer, LyricInfo>();
        }
        if (outMetrics == null) {
            outMetrics = new DisplayMetrics();
        }

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(outMetrics);

        noLyricSize = DisplayUtil.dip2px(context, 16);
        moffset = DisplayUtil.dip2px(context, 2);

        TypedArray a = mContext.obtainStyledAttributes(attrs, R.styleable.custom_lyricView);
        lineGap = a.getDimensionPixelSize(R.styleable.custom_lyricView_lyric_lineGap, 111); // 第一行字与第二行字开头的间隔
        mLyricSize = a.getDimensionPixelSize(R.styleable.custom_lyricView_lyric_textSize, 51);

        textLength = outMetrics.widthPixels - 100;
        positionOffset = a.getInt(R.styleable.custom_lyricView_lyric_positionOffset, 0);
        int textColor = a.getColor(R.styleable.custom_lyricView_lyric_textColor, Color.BLACK);
        textColorFocus = a.getColor(R.styleable.custom_lyricView_lyric_textColor_focus, Color.RED);
        textColorFocusRead = a.getColor(R.styleable.custom_lyricView_lyric_textColor_focus_read, Color.RED);
        if (Globals.mTestMode) {
            textColorFocusRead = Color.parseColor("#FFFFFF");
        }
        a.recycle();

        // 初始化paint
        noLycPaint = new Paint();
        noLycPaint.setAntiAlias(true);
        noLycPaint.setTextSize(noLyricSize);
        noLycPaint.setColor(textColor);
        noLycPaint.setAlpha(50);
        noLycPaint.setTextAlign(Paint.Align.CENTER);

        lycPaint = new Paint();
        lycPaint.setAntiAlias(true);
        lycPaint.setTextSize(mLyricSize);
        lycPaint.setColor(textColor);
        lycPaint.setAlpha(70);
        lycPaint.setTextAlign(Paint.Align.CENTER);

        currentPaint = new Paint();
        currentPaint.setAntiAlias(true);
        currentPaint.setColor(textColorFocusRead);
        currentPaint.setTextSize(mLyricSize + 4); // 如果使用该像素，整体高度需要拉高点，因为两行字会撑开
        currentPaint.setTextAlign(Paint.Align.CENTER);

        textColors = new int[] { textColorFocusRead, textColorFocus };

        modifyScrollFlagRun = new Runnable() {
            @Override
            public void run() {
                if (!isLyricScroll) {
                    return;
                }
                setIsTouchScrollView(false);
                if (player_lyric_scroll != null) {
                    player_lyric_scroll.smoothScrollTo((int) middleX, (int) (curLyricY - startCenterY - positionOffset)); // 如果移动curLyricY，当前歌词就会跑到第一句，所以只需移动this.curLyricY-lyricCenterY
                }
            }
        };
    }

    public void setHideText(boolean bhide) {
        this.mHideText = bhide;
        return;
    }

    public boolean isFullScreen() {
        return isFullScreen;
    }

    public void setFullScreen(boolean isFullScreen) {
        this.isFullScreen = isFullScreen;
    }

    /******
     * 是否触摸了歌词滚动界面(控制是否滚动),true 触摸了 false 没有
     * @param isTouchScrollView
     */
    public void setIsTouchScrollView(boolean isTouchScrollView) {
        this.isTouchScrollView = isTouchScrollView;
    }

    /**
     * 获取是否有歌词
     * @return null 加载中，true有歌词，false无歌词
     */
    public Boolean getHasLyric() {
        return isHasLyric;
    }

    /**
     * 设置是否有歌词 null 加载中，true有歌词，false无歌词
     */
    public void setHasLyric(Boolean flag) {
        this.isHasLyric = flag;

        invalidate();
    }

    public  void clearLyricMap(){
        if (lrc_map != null){
            lrc_map.clear();
        }
    }

    @Override
    public void invalidate() {
        // shader = new LinearGradient(textX, 0, textX + lineLyricsWidth, 0,
        // textColors, new float[] { readPosition, readPosition +
        // readPositionOffset }, TileMode.CLAMP);
        super.invalidate();
    }

    private boolean isTextLrc;
    private boolean isTrcLrc;

    /**
     * 读取歌词文件
     * @param file歌词的路径
     */
    public void read(String file) {
        //LogUtil.d(TAG, "read");
        isTextLrc = true;
        isTrcLrc = false;
        TreeMap<Integer, LyricInfo> lrc_read = new TreeMap<Integer, LyricInfo>();
        TreeMap<Integer, LyricInfo> txt_read = new TreeMap<Integer, LyricInfo>();
        String data = "";
        FileInputStream stream = null;
        BufferedInputStream in = null;
        try {
            if (HBMusicUtil.getExtFromFilename(file).equalsIgnoreCase("trc")) {
                isTrcLrc = true;
            }
            File saveFile = new File(file);
            //LogUtil.d(TAG, "read Lyric" + file + " exists:" + saveFile.exists() + " isTrcLrc:" + isTrcLrc);
            if (!saveFile.exists() || !saveFile.isFile()) {
                LogUtil.e(TAG, "-saveFile null");
                lrc_map.clear();
                isHasLyric = false;
                return;
            }

            stream = new FileInputStream(saveFile);// context.openFileInput(file);
            BufferedReader br = null;
            {
                in = new BufferedInputStream(stream);
                in.mark(4);
                byte[] bytes = new byte[3];
                in.read(bytes);
                in.reset();

                int type = hb_TEXT_ENCODING_UTF_8;// getEncodedType(bytes);
                switch (type) {
                    case hb_TEXT_ENCODING_GBK:
                        br = new BufferedReader(new InputStreamReader(in, "GBK"));
                        break;

                    case hb_TEXT_ENCODING_UTF_16:
                        br = new BufferedReader(new InputStreamReader(in, "unicode"));
                        break;

                    case hb_TEXT_ENCODING_UTF_16BE:
                        br = new BufferedReader(new InputStreamReader(in, "utf-16be"));
                        break;

                    case hb_TEXT_ENCODING_UTF_16LE:
                        br = new BufferedReader(new InputStreamReader(in, "utf-16le"));
                        break;

                    case hb_TEXT_ENCODING_UTF_8:
                        br = new BufferedReader(new InputStreamReader(in, "utf-8"));
                        break;

                    default:
                        break;
                }
            }

            mbLrcValid = false;
            int i = 0;
            Pattern pattern = Pattern.compile("\\d{2}");
            while ((data = br.readLine()) != null) {
                data = data.replace("[", "");// 将前面的替换成后面的
                data = data.replace("]", "@");
                if (!isTrcLrc) {
                    data = data.replaceAll(HBMusicUtil.lrcRegularExpressions, "");
                }
                String splitdata[] = data.split("@");// 分隔
                if (splitdata.length >= 2) {
                    isTextLrc = false;
                }
                if (data.endsWith("@")) {
                    for (int k = 0; k < splitdata.length; k++) {
                        String str = splitdata[k];
                        str = str.replace(":", ".");
                        str = str.replace(".", "@");
                        String timedata[] = str.split("@");
                        Matcher matcher = pattern.matcher(timedata[0]);
                        if (timedata.length == 3 && matcher.matches()) {
                            int m = Integer.parseInt(timedata[0]);  // 分
                            int s = Integer.parseInt(timedata[1]);  // 秒
                            int ms = Integer.parseInt(timedata[2]); // 毫秒
                            int currTime = (m * 60 + s) * 1000 + ms * 10;
                            LyricInfo item1 = new LyricInfo();
                            item1.starttime = currTime;
                            item1.strlrc = "";
                            lrc_read.put(currTime, item1);

                            if (!mbLrcValid) {
                                mbLrcValid = true;
                            }
                        } else if (timedata.length == 2 && matcher.matches()) {
                            int m = Integer.parseInt(timedata[0]);  // 分
                            int s = Integer.parseInt(timedata[1]); // 秒
                            int currTime = (m * 60 + s) * 1000;
                            LyricInfo item1 = new LyricInfo();
                            item1.starttime = currTime;
                            item1.strlrc = "";
                            lrc_read.put(currTime, item1);

                            if (!mbLrcValid) {
                                mbLrcValid = true;
                            }
                        }
                    }

                } else {
                    String lrcContenet = splitdata[splitdata.length - 1];
                    for (int j = 0; j < splitdata.length - 1; j++) {
                        String tmpstr = splitdata[j];
                        tmpstr = tmpstr.replace(":", ".");
                        tmpstr = tmpstr.replace(".", "@");
                        String timedata[] = tmpstr.split("@");
                        Matcher matcher = pattern.matcher(timedata[0]);
                        if (timedata.length == 3 && matcher.matches()) {// 00：00：00
                            int m = Integer.parseInt(timedata[0]);  // 分
                            int s = Integer.parseInt(timedata[1]);  // 秒
                            int ms = Integer.parseInt(timedata[2]); // 毫秒
                            int currTime = (m * 60 + s) * 1000 + ms;
                            LyricInfo item1 = new LyricInfo();
                            item1.starttime = currTime;
                            if (isTrcLrc) {
                                String[][] trc = HBMusicUtil.getTrc(lrcContenet);
                                if (trc != null) {
                                    item1.lyricsWords = trc[1];
                                    item1.wordsDisInterval = trc[0];
                                }
                                item1.strlrc = lrcContenet.replaceAll(HBMusicUtil.lrcRegularExpressions, "");
                            } else {
                                item1.strlrc = lrcContenet;
                            }
                            lrc_read.put(currTime, item1);// 将currTime当标签item1当数据插入TreeMap里
                            if (!mbLrcValid) {
                                mbLrcValid = true;
                            }
                        }
                        else if (timedata.length == 2 && matcher.matches()) {// 00:00
                            int m = Integer.parseInt(timedata[0]);  // 分
                            int s = Integer.parseInt(timedata[1]); // 秒
                            int currTime = (m * 60 + s) * 1000;
                            LyricInfo item1 = new LyricInfo();
                            item1.starttime = currTime;
                            if (isTrcLrc) {
                                String[][] trc = HBMusicUtil.getTrc(lrcContenet);
                                if (trc != null) {
                                    item1.lyricsWords = trc[1];
                                    item1.wordsDisInterval = trc[0];
                                }
                                item1.strlrc = lrcContenet.replaceAll(HBMusicUtil.lrcRegularExpressions, "");
                            } else {
                                item1.strlrc = lrcContenet;
                            }
                            lrc_read.put(currTime, item1);

                            if (!mbLrcValid) {
                                mbLrcValid = true;
                            }
                        }
                    }
                    // 歌词格式支持 无时间 TXT格式
                    if (splitdata.length - 1 == 0) {
                        i++;
                        LyricInfo lyricInfo = new LyricInfo();
                        lyricInfo.strlrc = data;
                        lyricInfo.starttime = 0;
                        txt_read.put(i, lyricInfo);
                        if (!mbLrcValid) {
                            mbLrcValid = true;
                        }
                    }
                }
            }
            i = 0;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }

            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        if (isTextLrc) {
            lrc_read.putAll(txt_read);
            isLyricScroll = false;
        } else {
            isLyricScroll = true;
        }
        lrc_map.clear();
        data = "";
        Iterator<Integer> iterator = lrc_read.keySet().iterator();
        LyricInfo oldval = null;
        int i = 0;
        try {
            while (iterator.hasNext()) {
                Object ob = iterator.next();
                LyricInfo val = (LyricInfo) lrc_read.get(ob);
				/*
				 * if (TextUtils.isEmpty(val.strlrc)) {//去掉空行 continue; }
				 */
                int endtime = 0;
                val.textArray = getTextArray(val.strlrc, lycPaint, textLength);
                // if(isTrcLrc){
                // val.floats=getTextArrayLength(val.textArray);
                // }
                if (oldval == null) {
                    oldval = val;
                } else {
                    LyricInfo item1 = new LyricInfo();
                    item1 = oldval;
                    item1.timelrc = val.starttime - oldval.starttime;
                    endtime = val.starttime + item1.timelrc;
                    lrc_map.put(i, item1);
                    i++;
                    oldval = val;
                }
                if (!iterator.hasNext()) {
                    lrc_map.put(i, val);
                    LyricInfo item = new LyricInfo();
                    item.starttime = endtime + 100;
                    item.strlrc = "END";
                    item.textArray = getTextArray(item.strlrc, lycPaint, textLength);
                    if (isTrcLrc) {
                        item.floats = getTextArrayLength(item.textArray);
                    }
                    lrc_map.put(i + 1, item);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.e(TAG, "lrc_read fail --", e);
        }

    }

    public void setTextsEx(HBScrollView lycView) {


        if (this.player_lyric_scroll == null) {
            this.player_lyric_scroll = lycView;
        }
        if (lrc_map != null && ! lrc_map.isEmpty()) {
            setTexts();
        }

        return;
    }

    /**
     * 设置歌词列表
     * @param texts
     *            歌词列表
     */
    private void setTexts() {

        android.view.ViewGroup.LayoutParams laoutPara = this.getLayoutParams();
        laoutPara.height = (int) (calLyricHeight());
        this.setLayoutParams(laoutPara);
        if (this.player_lyric_scroll == null) {
            this.player_lyric_scroll = (HBScrollView) ((Activity) mContext).findViewById(R.id.lyric_scrollview);
        }
        invalidate();
    }

    public int setCurrentIndex(int time) {
        if (lrc_map == null || lrc_map.isEmpty() || !isLyricScroll) {
            setLyricCurrent(0);
            return 500;
        }
        int index = 0;
        for (int i = 0; i < lrc_map.size(); i++) {
            LyricInfo temp = lrc_map.get(i);
            if(temp==null){
                continue;
            }
            if (temp.starttime < time) {
                ++index;
            }
        }
        index = index - 1;
        setLyricCurrent(index);
        if (isTrcLrc) {
            LyricInfo lyricInfo = lrc_map.get(currentLine);
            getLenFromCurPlayingTime(index, time, lyricInfo);
            if (lyricsWordIndex != -1) {
                String lyricsWords[] = lyricInfo.lyricsWords;
                String wordsDisInterval[] = lyricInfo.wordsDisInterval;
                String lyricsBeforeWord = "";// 当前歌词之前的歌词
                for (int k = 0; k < lyricsWordIndex; k++) {
                    lyricsBeforeWord += lyricsWords[k];
                }
                String lyricsNowWord = lyricsWords[lyricsWordIndex];// 当前歌词
                float lyricsBeforeWordWidth = currentPaint.measureText(lyricsBeforeWord);// 当前歌词之前的歌词长度
                float lyricsNowWordWidth = currentPaint.measureText(lyricsNowWord);// 当前歌词长度
                float len = lyricsNowWordWidth / Float.valueOf(wordsDisInterval[lyricsWordIndex]) * lyricsWordHLEDTime;
                lineLyricsHLWidth = lyricsBeforeWordWidth + len;
            }
            return 30;
        }
        return 500;
    }

    /**
     * 获取当前歌词的第几个歌词的播放进度 获取当前时间正在唱的歌词的第几个字
     * @param lyricsLineNum
     * @param msec
     * @return
     */
    public void getLenFromCurPlayingTime(int lyricsLineNum, int msec, LyricInfo lyrLine) {
        if (lyricsLineNum == -1 || lyrLine == null) {
            lyricsWordIndex = -1;
            lyricsWordHLEDTime = 0;
            return;
        }
        int elapseTime = lyrLine.starttime;
        if (lyrLine.wordsDisInterval != null) {
            for (int i = 0; i < lyrLine.wordsDisInterval.length; i++) {
                int dis = Integer.valueOf(lyrLine.wordsDisInterval[i]);
                elapseTime += dis;
                if (msec < elapseTime) {
                    lyricsWordIndex = i;
                    lyricsWordHLEDTime = dis - (elapseTime - msec);
                    return;
                }
            }
        }
        lyricsWordIndex = -1;
        lyricsWordHLEDTime = 0;
        return;
    }

    private boolean refreshNow = false;

    public void setRefreshNow(boolean refreshNow) {
        this.refreshNow = refreshNow;
    }

    /**
     * 设置当前歌词播放时间
     * @param currentLine
     *            当前播放行数
     */
    public void setLyricCurrent(int currentLine) {
        if (currentLine < 0) {
            currentLine = 0;
        }
        if (this.currentLine == currentLine && !isTrcLrc && !refreshNow) {
            return;
        }
        if (getHasLyric() != null && getHasLyric()) {
            this.currentLine = currentLine;
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        });
    }

    /**
     * 获取中间位置
     */
    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);

        if (this.getHeight() > 1 && lyricViewHeight < 0)
            lyricViewHeight = this.getHeight();

        if (lyricViewHeight > 1) {
            this.startCenterY = lyricViewHeight / 2;
        } else {
            this.startCenterY = outMetrics.heightPixels * 0.3f;
        }
        this.middleX = w * 0.5f;
        positionOffset = lyricViewHeight % lineGap - ((lineGap - mLyricSize - moffset));
        if (positionOffset < 0) {
            positionOffset = mLyricSize / 2;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isHasLyric == null||!isHasLyric) {// 加载歌词中 没有歌词
            if (mHideText) {

                drawOtherText(canvas, "", middleX, this.startCenterY);
            } else {

                drawOtherText(canvas, getResources().getString(R.string.no_lyric), middleX, this.startCenterY);
            }

        }else {
            if (!mbLrcValid) {
                drawFailText(canvas, getResources().getString(R.string.no_lyric), middleX, this.startCenterY);
                return;
            }
            try {
                drawText(canvas, currentLine, this.startCenterY);// 画歌词
            } catch (Exception e) {
                LogUtil.e(TAG, "-onDraw fail", e);
            }
        }
    }

    private void drawFailText(Canvas mCanvas, String text, float middleX, float startCenterY) {
//        android.view.ViewGroup.LayoutParams layoutPara = this.getLayoutParams();
//        if (layoutPara != null) {
//            layoutPara.height = lyricViewHeight;
//            this.setLayoutParams(layoutPara);
//            invalidate();
//        }

        mCanvas.drawText(text, middleX, startCenterY, noLycPaint);// othersPaint
    }

    private void drawOtherText(Canvas mCanvas, String text, float middleX, float startCenterY) {


        mCanvas.drawText(text, middleX, startCenterY, noLycPaint);// othersPaint
    }

    /**
     * 画歌词
     * @param canvas
     *            画布
     * @param strs
     *            歌词队列
     * @param currentLine
     *            当前行
     * @param lyricCenterY
     *            第一句歌词起始位置（中间），向下画所有歌词
     */
    private void drawText(Canvas canvas, int currentLine, float lyricCenterY) {
        if (lrc_map == null || lrc_map.isEmpty() || currentLine < 0) {
            return;
        }
        if (!refreshNow) {
            getLocalVisibleRect(rect);
            canvas.clipRect(rect);// 刷新局部区域
            refreshNow = false;
        }

        int size = lrc_map.size();
        try {
            if (currentLine < size) {
                int tmpY = 0;
                for (int i = 0; i < size; i++) {
                    LyricInfo lyricInfo = lrc_map.get(i);
                    if (lyricInfo == null) {
                        continue;
                    }
                    ArrayList<String> arrays = lyricInfo.textArray;
                    if (arrays == null) {
                        continue;
                    }
                    int lineNum = arrays.size();
                    for (int j = 0; j < lineNum; j++) {
                        float tmp = lyricCenterY + lineGap * i + tmpY + lineGap * j;
                        String lineLyrics = arrays.get(j).trim();
                        if (i == currentLine) {
                            if (isTrcLrc) {
                                // lineLyricsWidth = lyricInfo.floats.get(j);
                                float lineLyricsWidth = currentPaint.measureText(lineLyrics);
                                if (lyricsWordIndex == -1) {
                                    lineLyricsHLWidth = lineLyricsWidth;
                                }
                                float textX = middleX - lineLyricsWidth / 2;
                                canvas.save();// save和restore是为了剪切操作不影响画布的其它元素
                                if (lineNum > 1 && lyricsWordIndex != -1) {
                                    float oneWidtht = getNumLineWidthString(arrays, j);
                                    if (oneWidtht <= lineLyricsHLWidth) {
                                        readPosition = (lineLyricsHLWidth - oneWidtht) / lineLyricsWidth;
                                        shader = new LinearGradient(textX, 0, textX + lineLyricsWidth, 0, textColors, new float[] { readPosition, readPosition + readPositionOffset }, Shader.TileMode.CLAMP);
                                        currentPaint.setShader(shader);
                                    }
                                } else {
                                    readPosition = lineLyricsHLWidth / lineLyricsWidth;
                                    shader = new LinearGradient(textX, 0, textX + lineLyricsWidth, 0, textColors, new float[] { readPosition, readPosition + readPositionOffset }, Shader.TileMode.CLAMP);
                                    currentPaint.setShader(shader);
                                }
                                canvas.drawText(lineLyrics, middleX, tmp, currentPaint);
                                currentPaint.setShader(null);
                                canvas.restore();
                            } else {
                                canvas.drawText(lineLyrics, middleX, tmp, currentPaint);
                            }
                            this.curLyricY = tmp;
                        } else {
                            canvas.drawText(lineLyrics, middleX, tmp, lycPaint);
                        }

                        if (j != 0) // 如果有换行的，则累加起来
                            tmpY = tmpY + lineGap * j;
                    }
                }
                if (player_lyric_scroll != null && isNeedToSmooth(currentLine)) {
                    int offset = (int) (this.curLyricY - lyricCenterY - positionOffset);
                    issmoothScrolling = true;
                    player_lyric_scroll.smoothScrollTo((int) middleX, offset); // 如果移动curLyricY，当前歌词就会跑到第一句，所以只需移动this.curLyricY-lyricCenterY
                    handler.sendEmptyMessageDelayed(MSG_SMOOTHSCROLL, 1000);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean issmoothScrolling = false;
    private static final int MSG_SMOOTHSCROLL = 1002;
    private static final int MSG_INVALIDATE = 1003;
    private static final int MSG_SCROLLVIEW = 1004;

    /**
     * 获取每行宽度
     * @param arrays
     * @param line
     * @return
     */
    private float getNumLineWidth(ArrayList<Float> arrays, int line) {
        float lineWidth = 0;
        for (int i = 0; i < line; i++) {
            lineWidth += arrays.get(i);
        }
        return lineWidth;
    }

    private float getNumLineWidthString(ArrayList<String> arrays, int line) {
        float lineWidth = 0;
        for (int i = 0; i < line; i++) {
            lineWidth += currentPaint.measureText(arrays.get(i));
        }
        return lineWidth;
    }

    /******
     * 是否需要去滚动，只能被调用一次
     * @param currentLine
     * @return true 滚动 false 不滚动
     */
    private boolean isNeedToSmooth(int position) {
        if (isTouchScrollView) {
            return false;
        }

        if (position != this.flushIsSmoothFlag) {
            this.flushIsSmoothFlag = position;
            return true;
        }
        return false;
    }

    /******
     * 计算文字所需的高度
     * @return
     */
    private int calLyricHeight() {
        int height = 0;
        int tmpY = 0;

        if (lrc_map != null) {
            for (int i = 0; i < lrc_map.size(); i++) {
                LyricInfo lyricInfo = lrc_map.get(i);
                if (lyricInfo == null) {
                    continue;
                }
                ArrayList<String> arrays = lyricInfo.textArray;
                if (arrays == null) {
                    continue;
                }

                int lineNum = arrays.size();
                for (int j = 0; j < lineNum; j++) {

                    // lineGap第一句与第二句开头的距离
                    height = (int) (this.startCenterY + lineGap * i + lineGap * j + tmpY);

                    // 如果有换行的，则累加起来
                    if (j != 0)
                        tmpY += lineGap;
                }
            }
        }

        int tp = 0;
        if (lyricViewHeight > 1) {
            tp = (int) ((lyricViewHeight - this.startCenterY) + height);
        } else {
            tp = (int) (outMetrics.heightPixels * 0.5 + height);
        }
        return tp;
    }

    public void setFirstScroll() {
        handler.postDelayed(modifyScrollFlagRun, scrollDelayTime * 500);
        return;
    }

    public void setTouchMode(View v, MotionEvent event, boolean bplay) {

        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            handler.removeCallbacks(modifyScrollFlagRun);
            this.isTouchScrollView = true;
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (bplay) {
                handler.postDelayed(modifyScrollFlagRun, scrollDelayTime * 1000);
            } else {
                this.isTouchScrollView = false;
            }
        }

        return;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            handler.removeCallbacks(modifyScrollFlagRun);
            this.isTouchScrollView = true;
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            handler.postDelayed(modifyScrollFlagRun, scrollDelayTime * 1000);
        }

        return false;
    }

    /**
     * @param textArray
     * @return
     */
    private ArrayList<Float> getTextArrayLength(ArrayList<String> textArray) {
        ArrayList<Float> floats = new ArrayList<Float>();
        if (textArray != null) {
            for (int i = 0; i < textArray.size(); i++) {
                floats.add(currentPaint.measureText(textArray.get(i)));
            }
        }
        return floats;
    }

    private ArrayList<String> getTextArray(String text, Paint p, int maxLength) {
        if (text == null) {
            return null;
        }

        if (p.measureText(text) > maxLength) { // 如果歌词超过最大宽度，分隔
            if (p.measureText(text.substring(text.length() / 2)) > maxLength) { // 分隔成三行
                return devide3line(text);
            } else {// 分隔成二行
                return devide2line(text);
            }

        } else {
            ArrayList<String> arrays = new ArrayList<String>();
            arrays.add(text);
            return arrays;
        }

    }

    private static ArrayList<String> devide3line(String text) {
        ArrayList<String> arrays = new ArrayList<String>();
        int textCount = text.length();
        if (text.charAt(text.length() / 3) > 128) { // 中文
            arrays.add(text.substring(0, textCount / 3));
            arrays.add(text.substring(textCount / 3, textCount / 3 * 2));
            arrays.add(text.substring(textCount / 3 * 2));
        } else {
            int first = text.substring(0, text.length() / 3).lastIndexOf(" ");
            if (first < 10) {
                first = text.length() / 3;
            }
            arrays.add(text.substring(0, first));
            int second = text.substring(0, text.length() / 3 * 2).lastIndexOf(" ");
            if (second < 20) {
                second = text.length() / 3 * 2;
            }
            arrays.add(text.substring(first + 1, second));
            arrays.add(text.substring(second + 1));
        }
        return arrays;
    }

    private static ArrayList<String> devide2line(String text) {
        ArrayList<String> arrays = new ArrayList<String>();
        int textCount = text.length();
        if (text.charAt(textCount / 2) > 128) { // 中文
            arrays.add(text.substring(0, textCount / 2));
            arrays.add(text.substring(textCount / 2));
        } else {
            int first = text.substring(0, text.length() / 2).lastIndexOf(" ");
            if (first < 10) {
                first = text.length() / 2;
            }
            arrays.add(text.substring(0, first));
            arrays.add(text.substring(first + 1));
        }
        return arrays;
    }

    @Override
    public void onScrollChanged(HBScrollView scrollView, int x, int y, int oldx, int oldy) {
        try {
            if (isTrcLrc && !MusicUtils.sService.isPlaying()) {
                invalidate();
            } else if (!isTextLrc && !issmoothScrolling) {
                invalidate();
                isTouchScrollView = true;
                handler.removeMessages(MSG_SCROLLVIEW);
                handler.sendEmptyMessageDelayed(MSG_SCROLLVIEW, 1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

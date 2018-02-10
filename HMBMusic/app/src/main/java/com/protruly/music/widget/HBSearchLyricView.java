package com.protruly.music.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.protruly.music.R;
import com.protruly.music.util.DisplayUtil;
import com.protruly.music.util.Globals;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by hujianwei on 17-9-1.
 */

public class HBSearchLyricView extends View implements View.OnTouchListener {

    private static final String TAG = "HBSearchLyricView";
    private List<String> lyric_text = new ArrayList<String>();// 歌词队列
    private TreeMap<Integer, LyricInfo> lrc_map = null;
    private List<String> mListLines = null;
    private Paint behindPaint;
    /** x轴值，取控件中间位置 */
    private float middleX;
    /*** 每一行的间隔 */
    private int lineGap;
    private Context mContext = null;
    /** 每行歌词的长度 */
    private int textLength;
    /** 字体大小 */
    private int mLyricSize;
    private DisplayMetrics outMetrics;
    /*** 上下位置偏移 */
    private int positionOffset;

    /** 当前行位置 */
    private int currentLine = 0;
    /** y轴值，第一句歌词起始位置处于中间 */
    private float startCenterY = -1;
    /*** 歌词view的高度 ***/
    private static int lyricViewHeight = -1;
    private int moffset = 0;
    private String filename =null;

    public HBSearchLyricView(Context context) {
        this(context, null);
    }

    public HBSearchLyricView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HBSearchLyricView(Context context, AttributeSet attrs,
                                 int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        if (lrc_map == null) {
            lrc_map = new TreeMap<Integer, LyricInfo>();
        }

        if (mListLines == null) {
            mListLines = new ArrayList<String>();
        }

        if (outMetrics == null) {
            outMetrics = new DisplayMetrics();
        }
        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(outMetrics);

        moffset = DisplayUtil.dip2px(context, 2);
        startCenterY = DisplayUtil.dip2px(context, 30);

        currentLine = 0;

        TypedArray a = mContext.obtainStyledAttributes(attrs,
                R.styleable.custom_lyricView);

        lineGap = a.getDimensionPixelSize(
                R.styleable.custom_lyricView_lyric_lineGap, 111); // 第一行字与第二行字开头的间隔
        mLyricSize = a.getDimensionPixelSize(
                R.styleable.custom_lyricView_lyric_textSize, 51);

        textLength = outMetrics.widthPixels - 100;
        positionOffset = a.getInt(
                R.styleable.custom_lyricView_lyric_positionOffset, 0);
        int textColor = a.getColor(
                R.styleable.custom_lyricView_lyric_textColor, Color.BLACK);
        a.recycle();

        behindPaint = new Paint();
        behindPaint.setAntiAlias(true);
        behindPaint.setColor(textColor);
        behindPaint.setTextSize(mLyricSize);
        behindPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    public boolean onTouch(View arg0, MotionEvent arg1) {

        return false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (this.getHeight() > 1 && lyricViewHeight < 0)
            lyricViewHeight = this.getHeight();

        this.middleX = w * 0.5f;

        positionOffset = lyricViewHeight % lineGap
                - ((lineGap - mLyricSize - moffset));

        if (positionOffset < 0) {
            positionOffset = mLyricSize / 2;
        }
    }

    /**
     * 设置歌词列表
     *
     * @param texts
     *            歌词列表
     */
    public void setTexts(List<String> texts) {

        if (texts != null && texts.size() > 0
                && !"END".equalsIgnoreCase(texts.get(texts.size() - 1))) {
            texts.add("END");
        }

        this.lyric_text = texts;
        android.view.ViewGroup.LayoutParams laoutPara = this.getLayoutParams();

        laoutPara.height = (int) (calLyricHeight());
        this.setLayoutParams(laoutPara);

        invalidate();
    }

    /******
     * 计算文字所需的高度
     *
     * @return
     */
    private int calLyricHeight() {
        int height = 0;
        int tmpY = 0;

        if (lyric_text != null) {
            for (int i = 0; i < lyric_text.size(); i++) {
                ArrayList<String> arrays = getTextArray(lyric_text.get(i),
                        behindPaint, textLength);
                if (arrays == null) {
                    continue;
                }

                int lineNum = arrays.size();
                for (int j = 0; j < lineNum; j++) {
                    height = (int) (this.startCenterY*2 + lineGap * i + lineGap
                            * j + tmpY); // lineGap第一句与第二句开头的距离
                    if (j != 0) // 如果有换行的，则累加起来
                        tmpY = tmpY + lineGap;
                }
            }
        }
        return height;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        drawText(canvas, lyric_text, currentLine, this.startCenterY);// 画歌词
    }

    /**
     * 画歌词
     *
     * @param canvas
     *            画布
     * @param strs
     *            歌词队列
     * @param currentLine
     *            当前行
     * @param lyricCenterY
     *            第一句歌词起始位置（中间），向下画所有歌词
     */
    private void drawText(Canvas canvas, List<String> strs, int currentLine,
                          float lyricCenterY) {


        if (strs == null || strs.size() <= 0 || currentLine >= strs.size()
                || currentLine < 0) {

            return;
        }

        String text = strs.get(currentLine);// 获取当前歌词

        if (text != null && currentLine >= 0 && currentLine < strs.size()) {
            int tmpY = 0;
            for (int i = 0; i < strs.size(); i++) {
                ArrayList<String> arrays = getTextArray(strs.get(i),
                        behindPaint, textLength);
                int lineNum = arrays.size();
                for (int j = 0; j < lineNum; j++) {

                    float tmp = lyricCenterY + lineGap * i + tmpY + lineGap * j;

                    canvas.drawText(arrays.get(j).trim(), middleX, tmp,
                            behindPaint);
                    if (j != 0) // 如果有换行的，则累加起来
                        tmpY = tmpY + lineGap * j;
                }

            }
			/*if(player_lyric_scroll!=null&&isNeedToSmooth(currentLine)) {
				player_lyric_scroll.smoothScrollTo((int)middleX, (int)(this.curLyricY-lyricCenterY-positionOffset)); //如果移动curLyricY，当前歌词就会跑到第一句，所以只需移动this.curLyricY-lyricCenterY
			}*/
        }
    }

    private static ArrayList<String> getTextArray(String text, Paint p,
                                                  int maxLength) {
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
            int second = text.substring(0, text.length() / 3 * 2).lastIndexOf(
                    " ");
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

    /**
     * 读取歌词文件
     *
     * @param stream
     *            文件流
     *
     */
    public boolean read(InputStream stream) {
        TreeMap<Integer, LyricInfo> lrc_read = new TreeMap<Integer, LyricInfo>();

        String data = "";

        BufferedInputStream in = new BufferedInputStream(stream);
        FileOutputStream out=null;
        try {
            File file = new File(Globals.mLycPath_temp);
            if(!file.exists()){
                file.mkdirs();
            }
            //LogUtil.d(TAG, "filename:"+filename);
            file = new File(Globals.mLycPath_temp+"/"+filename);
            out =new FileOutputStream(file);

            BufferedReader br = new BufferedReader(new InputStreamReader(in,
                    "utf-8"));

            int i = 0;
            Pattern pattern = Pattern.compile("\\d{2}");
            while ((data = br.readLine()) != null) {

                String datatmp=data+"\n";
                out.write(datatmp.getBytes("utf-8"));

                data = data.replace("[", "");// 将前面的替换成后面的
                data = data.replace("]", "@");
                String splitdata[] = data.split("@");// 分隔
                if (data.endsWith("@")) {
                    for (int k = 0; k < splitdata.length; k++) {
                        String str = splitdata[k];

                        str = str.replace(":", ".");
                        str = str.replace(".", "@");
                        String timedata[] = str.split("@");
                        Matcher matcher = pattern.matcher(timedata[0]);
                        if (timedata.length == 3 && matcher.matches()) {
                            int m = Integer.parseInt(timedata[0]); // 分
                            int s = Integer.parseInt(timedata[1]); // 秒
                            int ms = Integer.parseInt(timedata[2]); // 毫秒
                            int currTime = (m * 60 + s) * 1000 + ms * 10;
                            LyricInfo item1 = new LyricInfo();
                            item1.starttime = currTime;
                            item1.strlrc = "";
                            lrc_read.put(currTime, item1);
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
                        if (timedata.length == 3 && matcher.matches()) {
                            int m = Integer.parseInt(timedata[0]); // 分
                            int s = Integer.parseInt(timedata[1]); // 秒
                            int ms = Integer.parseInt(timedata[2]); // 毫秒
                            int currTime = (m * 60 + s) * 1000 + ms * 10;
                            LyricInfo item1 = new LyricInfo();
                            item1.starttime = currTime;
                            item1.strlrc = lrcContenet;
                            lrc_read.put(currTime, item1);// 将currTime当标签
                            // item1当数据
                            // 插入TreeMap里
                            i++;
                        }
                    }
                }
            }

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

            if(out!=null){
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

		/*
		 * 遍历hashmap 计算每句歌词所需要的时间
		 */
        lrc_map.clear();
        mListLines.clear();
        data = "";
        Iterator<Integer> iterator = lrc_read.keySet().iterator();
        LyricInfo oldval = null;
        int i = 0;
        try {
            while (iterator.hasNext()) {
                Object ob = iterator.next();

                LyricInfo val = (LyricInfo) lrc_read.get(ob);

                if (oldval == null)
                    oldval = val;
                else {
                    LyricInfo item1 = new LyricInfo();
                    item1 = oldval;
                    item1.timelrc = val.starttime - oldval.starttime;
                    lrc_map.put(new Integer(i), item1);
                    i++;
                    oldval = val;
                }
                mListLines.add(val.strlrc);
                if (!iterator.hasNext()) {
                    lrc_map.put(new Integer(i), val);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(mListLines.size()==0){
            return false;
        }else{
            return true;
        }
    }

    public void setFileName(String name){
        filename=name;

    }

    public void setTextsEx() {

        if (mListLines != null) {
            setTexts(mListLines);
        }

        return;
    }

    private class LyricInfo {
        public String strlrc; // 歌词
        public int starttime; // 开始时间
        public int endtime; // 结束时间
        public int timelrc; // 每句歌词时时间
    }
    
}

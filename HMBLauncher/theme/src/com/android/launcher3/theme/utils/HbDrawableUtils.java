package com.android.launcher3.theme.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.NinePatch;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.util.Log;
import android.util.TypedValue;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

/**
 * 该类主要的功能是从主题包中解析.9图片
 * @author alexluo
 */
public class HbDrawableUtils {
	private static final int NO_COLOR = 0x00000001;
    private static final String TAG = "NinePatchTool";
 
    
    public static Drawable decodeDrawableFromStream(Resources res, TypedValue value,
                                                    InputStream is, String srcName, BitmapFactory.Options opts) throws Exception {
        Bitmap bm = decodeFromStream(is);
        if (null == bm.getNinePatchChunk()) {
        	return Drawable.createFromResourceStream(res, value, is, srcName, null);
        } else {
            Rect padding = new Rect();
            readPaddingFromChunk(bm.getNinePatchChunk(), padding);
            NinePatchDrawable d = new NinePatchDrawable(res,
                    bm, bm.getNinePatchChunk(), padding, null);
            return d;
        }
    }
    
    
    public static Drawable decodeDrawableFromStream(InputStream in, Resources res) throws Exception {
        Bitmap bm = decodeFromStream(in);
        if (null == bm.getNinePatchChunk()) {
            BitmapDrawable bitmapDrawable = new BitmapDrawable(res,bm);
            return bitmapDrawable;
        } else {
            Rect padding = new Rect();
            readPaddingFromChunk(bm.getNinePatchChunk(), padding);
            NinePatchDrawable d = new NinePatchDrawable(res,
                    bm, bm.getNinePatchChunk(), padding, null);
            return d;
        }
    }
 
    public static Bitmap decodeFromStream(InputStream in) throws Exception {
        Bitmap srcBm = BitmapFactory.decodeStream(in);
        byte[] chunk = readChunk(srcBm);
        boolean isNinePatchChunk = NinePatch.isNinePatchChunk(chunk);
        Log.e(TAG, "isNinePatchChunk = " + isNinePatchChunk);
        if (isNinePatchChunk) {
            Bitmap tgtBm = Bitmap.createBitmap(srcBm, 1, 1,
                    srcBm.getWidth() - 2, srcBm.getHeight() - 2);
            srcBm.recycle();
            try {
                Class clazz = Class.forName("android.graphics.Bitmap");
                Method getter = clazz.getDeclaredMethod("setNinePatchChunk", byte [].class );
                getter.invoke(tgtBm, chunk);
            }catch (Exception e){
                Log.e(TAG,e.getMessage());
            }finally {

                return tgtBm;
            }
             //tgtBm.setNinePatchChunk(chunk);
        } else {
            return srcBm;
        }
    }
 
    public static Bitmap decodeFromFile(String path) throws Exception {
        InputStream in = new FileInputStream(path);
        Bitmap bm = decodeFromStream(in);
        in.close();
        return bm;
    }
 
    public static Bitmap decodeFromAsset(Context context,
            String ninePatchPngPath) throws Exception {
        InputStream is = context.getAssets().open(ninePatchPngPath);
        Bitmap bm = decodeFromStream(is);
        is.close();
        return bm;
    }
 
    /**
     * 读取图片上下左右的信息
     * @param chunk
     * @param paddingRect
     */
    public static void readPaddingFromChunk(byte[] chunk, Rect paddingRect) {
        paddingRect.left = getInt(chunk, 12);
        paddingRect.right = getInt(chunk, 16);
        paddingRect.top = getInt(chunk, 20);
        paddingRect.bottom = getInt(chunk, 24);
    }
 
    /**
     * 读取bitmap的数据，并对其进行处理，使其具有.9图片的特性
     * @param srcBmp
     * @return
     * @throws IOException
     */
    public static byte[] readChunk(Bitmap srcBmp) throws IOException {
        final int BM_W = srcBmp.getWidth();
        final int BM_H = srcBmp.getHeight();
 
        int xPointCount = 0;
        int yPointCount = 0;
 
        int xBlockCount = 0;
        int yBlockCount = 0;
 
        ByteArrayOutputStream ooo = new ByteArrayOutputStream();
        for (int i = 0; i < 32; i++) {
            ooo.write(0);
        }
 
        /*
         *  根据.9图片（x）水平方向上的黑点来对拉伸区域进行处理
         */
        { 
            int[] pixelsTop = new int[BM_W - 2];
            srcBmp.getPixels(pixelsTop, 0, BM_W, 1, 0, BM_W - 2, 1);
            boolean topFirstPixelIsBlack = pixelsTop[0] == Color.BLACK;
            boolean topLastPixelIsBlack = pixelsTop[pixelsTop.length - 1] == Color.BLACK;
            int tmpLastColor = Color.TRANSPARENT;
            for (int i = 0, len = pixelsTop.length; i < len; i++) {
                if(pixelsTop[i] != tmpLastColor && pixelsTop[i] != Color.BLACK){
             	   return null;
                }
             }
            for (int i = 0, len = pixelsTop.length; i < len; i++) {
                if (tmpLastColor != pixelsTop[i]) {
                    xPointCount++;
                    writeInt(ooo, i);
                    tmpLastColor = pixelsTop[i];
                }
            }
            if (topLastPixelIsBlack) {
                xPointCount++;
                writeInt(ooo, pixelsTop.length);
            }
            xBlockCount = xPointCount + 1;
            if (topFirstPixelIsBlack) {
                xBlockCount--;
            }
            if (topLastPixelIsBlack) {
                xBlockCount--;
            }
        }
 
        /*
         *  根据.9图片（y）垂直方向上的黑点来对拉伸区域进行处理
         */
        { 
            int[] pixelsLeft = new int[BM_H - 2];
            srcBmp.getPixels(pixelsLeft, 0, 1, 0, 1, 1, BM_H - 2);
            boolean firstPixelIsBlack = pixelsLeft[0] == Color.BLACK;
            boolean lastPixelIsBlack = pixelsLeft[pixelsLeft.length - 1] == Color.BLACK;
            int tmpLastColor = Color.TRANSPARENT;
            for (int i = 0, len = pixelsLeft.length; i < len; i++) {
                if (tmpLastColor != pixelsLeft[i]) {
                    yPointCount++;
                    writeInt(ooo, i);
                    tmpLastColor = pixelsLeft[i];
                }
            }
            if (lastPixelIsBlack) {
                yPointCount++;
                writeInt(ooo, pixelsLeft.length);
            }
            yBlockCount = yPointCount + 1;
            if (firstPixelIsBlack) {
                yBlockCount--;
            }
            if (lastPixelIsBlack) {
                yBlockCount--;
            }
        }
 
        {// color
            for (int i = 0; i < xBlockCount * yBlockCount; i++) {
                writeInt(ooo, NO_COLOR);
            }
        }
 
        byte[] data = ooo.toByteArray();
        data[0] = 1;
        data[1] = (byte) xPointCount;
        data[2] = (byte) yPointCount;
        data[3] = (byte) (xBlockCount * yBlockCount);
        dealPaddingInfo(srcBmp, data);
        return data;
    }
 
    /**
     * 根据.9图片中标志的各个拉伸点进行图片拉伸区域和内容区域的处理
     * @param bm
     * @param data
     */
    private static void dealPaddingInfo(Bitmap bm, byte[] data) {
        { // padding left & padding right
            int[] bottomPixels = new int[bm.getWidth() - 2];
            bm.getPixels(bottomPixels, 0, bottomPixels.length, 1,
                    bm.getHeight() - 1, bottomPixels.length, 1);
            for (int i = 0; i < bottomPixels.length; i++) {
                if (Color.BLACK == bottomPixels[i]) { // padding left
                    writeInt(data, 12, i);
                    break;
                }
            }
            for (int i = bottomPixels.length - 1; i >= 0; i--) {
                if (Color.BLACK == bottomPixels[i]) { // padding right
                    writeInt(data, 16, bottomPixels.length - i - 2);
                    break;
                }
            }
        }
        { // padding top & padding bottom
            int[] rightPixels = new int[bm.getHeight() - 2];
            bm.getPixels(rightPixels, 0, 1, bm.getWidth() - 1, 0, 1,
                    rightPixels.length);
            for (int i = 0; i < rightPixels.length; i++) {
                if (Color.BLACK == rightPixels[i]) { // padding top
                    writeInt(data, 20, i);
                    break;
                }
            }
            for (int i = rightPixels.length - 1; i >= 0; i--) {
                if (Color.BLACK == rightPixels[i]) { // padding bottom
                    writeInt(data, 24, rightPixels.length - i - 2);
                    break;
                }
            }
        }
    }
 
    private static void writeInt(OutputStream out, int v) throws IOException {
        out.write((v >> 0) & 0xFF);
        out.write((v >> 8)& 0xFF);
        out.write((v >> 16) & 0xFF);
        out.write((v >> 24) & 0xFF);
    }
 
    private static void writeInt(byte[] b, int offset, int v) {
        b[offset + 0] = (byte) (v >> 0);
        b[offset + 1] = (byte) (v >> 8);
        b[offset + 2] = (byte) (v >> 16);
        b[offset + 3] = (byte) (v >> 24);
    }
 
    private static int getInt(byte[] bs, int from) {
        int b1 = bs[from + 0];
        int b2 = bs[from + 1];
        int b3 = bs[from + 2];
        int b4 = bs[from + 3];
        int i = b1 | (b2 << 8) | (b3 << 16) | b4 << 24;
        return i;
    }
 
    /** print chunk info from bitmap */
    public static void printChunkInfo(Bitmap bm) {
        byte[] chunk = bm.getNinePatchChunk();
        if (null == chunk) {
            System.out.println("can't find chunk info from this bitmap(" + bm
                    + ")");
            return;
        }
        int xLen = chunk[1];
        int yLen = chunk[2];
        int cLen = chunk[3];
 
        StringBuilder sb = new StringBuilder();
        int peddingLeft = getInt(chunk, 12);
        int paddingRight = getInt(chunk, 16);
        int paddingTop = getInt(chunk, 20);
        int paddingBottom = getInt(chunk, 24);
        sb.append("peddingLeft=" + peddingLeft);
        sb.append("\r\n");
        sb.append("paddingRight=" + paddingRight);
        sb.append("\r\n");
        sb.append("paddingTop=" + paddingTop);
        sb.append("\r\n");
        sb.append("paddingBottom=" + paddingBottom);
        sb.append("\r\n");
 
        sb.append("x info=");
        for (int i = 0; i < xLen; i++) {
            int vv = getInt(chunk, 32 + i * 4);
            sb.append("," + vv);
        }
        sb.append("\r\n");
        sb.append("y info=");
        for (int i = 0; i < yLen; i++) {
            int vv = getInt(chunk, xLen * 4 + 32 + i * 4);
            sb.append("," + vv);
        }
        sb.append("\r\n");
        sb.append("color info=");
        for (int i = 0; i < cLen; i++) {
            int vv = getInt(chunk, xLen * 4 + yLen * 4 + 32 + i * 4);
            sb.append("," + vv);
        }
        System.err.println("" + sb);
    }

}

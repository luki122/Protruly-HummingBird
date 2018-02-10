package cn.com.protruly.filemanager.globalsearch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.graphics.Matrix;


public class RoundAngleImageView extends ImageView {

    private Paint paint;
    private boolean isCenterImgShow = false;
    private Bitmap centerbitmap = null;

    public RoundAngleImageView(Context context) {
        this(context,null);
    }

    public RoundAngleImageView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public RoundAngleImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        paint  = new Paint();
    }

    public void setCenterImgShow(int id,boolean centerImgShow) {
        isCenterImgShow = centerImgShow;
        if (isCenterImgShow) {
            centerbitmap = BitmapFactory.decodeResource(getResources(), id);
            //invalidate();
        }else{
            centerbitmap =null;
        }
    }

    /**
     * 绘制圆角矩形图片
     * @author caizhiming
     */
    @Override
    protected void onDraw(Canvas canvas) {

        Drawable drawable = getDrawable();
        if (null != drawable) {

            Bitmap bitmap = drawableToBitmap(drawable);
         /*   Bitmap b = getRoundBitmap(bitmap, 20);
            final Rect rectSrc = new Rect(0, 0, b.getWidth(), b.getHeight());
            final Rect rectDest = new Rect(0,0,getWidth(),getHeight());
            paint.reset();
            canvas.drawBitmap(b, rectSrc, rectDest, paint);*/

            Bitmap reSizeImage = reSizeImage(bitmap, getWidth(), getHeight());//等比缩放
           /* canvas.drawBitmap(createRoundImage(reSizeImage, getWidth(), getHeight()),
                    getPaddingLeft(), getPaddingTop(), null);*/
            canvas.drawBitmap(createRoundImage(reSizeImage, getWidth(), getHeight()),
                    0, 0, null);


            if (isCenterImgShow && centerbitmap != null) {
                canvas.drawBitmap(centerbitmap, getMeasuredWidth() / 2 - centerbitmap.getWidth() / 2, getMeasuredHeight() / 2 - centerbitmap.getHeight() / 2, paint);
            }else{
                centerbitmap=null;
            }

            } else {
            super.onDraw(canvas);
        }
    }


    /**
           * drawable转bitmap
           *
           * @paramdrawable
           * @return
           */
     private Bitmap drawableToBitmap(Drawable drawable) {
                if (drawable instanceof BitmapDrawable) {
                         BitmapDrawable bitmapDrawable =(BitmapDrawable) drawable;
                         return bitmapDrawable.getBitmap();
                    }
                 int w =drawable.getIntrinsicWidth();
                int h =drawable.getIntrinsicHeight();
                Bitmap bitmap = Bitmap.createBitmap(w,h, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                 drawable.setBounds(0, 0, w, h);
                 drawable.draw(canvas);
                 return bitmap;
             }



    private Bitmap createRoundImage(Bitmap source, int width, int height) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Bitmap target = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(target);
        RectF rect = new RectF(0, 0, width, height);
        canvas.drawRoundRect(rect, 20, 20, paint);
        // 核心代码取两个图片的交集部分
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(source, 0, 0, paint);
        return target;
    }



    /**
     * 获取圆角矩形图片方法
     * @param bitmap
     * @param roundPx,一般设置成14
     * @return Bitmap
     * @author caizhiming
     */
    private Bitmap getRoundBitmap(Bitmap bitmap, int roundPx) {

       // Bitmap bitmap = reSizeImage(obitmap,getWidth(),getHeight());
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;

        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        int x = bitmap.getWidth();

        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;


    }


    private Bitmap reSizeImage(Bitmap bitmap, int newWidth, int newHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        // 计算出缩放比
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 矩阵缩放bitmap
        Matrix matrix = new Matrix();

        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }


    private Bitmap reSizeImageC(Bitmap bitmap, int newWidth, int newHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int x = (newWidth - width) / 2;
        int y = (newHeight - height) / 2;
        if (x > 0 && y > 0) {
            return Bitmap.createBitmap(bitmap, 0, 0, width, height, null, true);
        }

        float scale = 1;

        if (width > height) {
            // 按照宽度进行等比缩放
            scale = ((float) newWidth) / width;

        } else {
            // 按照高度进行等比缩放
            // 计算出缩放比
            scale = ((float) newHeight) / height;
        }
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }
}
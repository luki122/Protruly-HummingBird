package com.protruly.music.downloadex;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import junit.framework.Assert;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.graphics.PorterDuff.Mode;

import com.protruly.music.util.HBMusicUtil;
import com.protruly.music.util.LogUtil;


public class BitmapUtil {

	private static final String TAG = "BitmapUtil";
	private static final int MAX_DECODE_PICTURE_SIZE = 864*864;//;1920 * 1440;
	
	 
	public static Bitmap getCircleImage(Bitmap source, int min, boolean recycle) {
		if (source == null) {
			return null;
		}
		
		final Paint paint = new Paint();  
		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);
		paint.setDither(true);
		Bitmap output = Bitmap.createBitmap(min, min, Config.ARGB_8888);
		
		Rect rect = new Rect(0, 0, min, min);
		Canvas canvas = new Canvas(output);
		
		canvas.drawARGB(0, 0, 0, 0);
		canvas.drawCircle(min / 2, min / 2, (min / 2)-(HBMusicUtil.mCicleOffset/2), paint);  
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));  
		canvas.drawBitmap(source, rect, rect, paint);  
		if (recycle && 
			source != null && 
			!source.isRecycled()) {
			source.recycle();
			source = null;
		}
		
		return output;  
	}
	
	public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, float roundPx, boolean recycle) {
		try {
			Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
			Canvas canvas = new Canvas(output);

			final int color = 0xffff0000;
			final Paint paint = new Paint();
			final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
			final RectF rectF = new RectF(rect);

			paint.setAntiAlias(true);
			canvas.drawARGB(0, 0, 0, 0);
			paint.setColor(color);
			canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

			paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
			canvas.drawBitmap(bitmap, rect, rect, paint);

			if (recycle && 
				bitmap != null && 
				!bitmap.isRecycled()) {
				bitmap.recycle();
			}
			return output;
		} catch (final OutOfMemoryError e) {
			LogUtil.i(TAG, "getRoundedCornerBitmap fail");
			return null;
		}
	}
	
	public static byte[] bmpToByteArray(final Bitmap bmp, final boolean needRecycle) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		bmp.compress(CompressFormat.PNG, 100, output);
		if (needRecycle) {
			bmp.recycle();
		}
		
		byte[] result = output.toByteArray();
		try {
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public static int bmpToByteArrayEx(final Bitmap bmp, final boolean needRecycle) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		bmp.compress(CompressFormat.PNG, 100, output);
		if (needRecycle) {
			bmp.recycle();
		}
		
		byte[] result = output.toByteArray();
		try {
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		int len = result.length;
		if (result != null) {
			result = null;
		}
		
		return len;
	}
	
	public static byte[] getHtmlByteArray(final String url) {
		 URL htmlUrl = null;     
		 InputStream inStream = null;     
		 try {         
			 htmlUrl = new URL(url);         
			 URLConnection connection = htmlUrl.openConnection();         
			 HttpURLConnection httpConnection = (HttpURLConnection)connection;         
			 int responseCode = httpConnection.getResponseCode();         
			 if(responseCode == HttpURLConnection.HTTP_OK){             
				 inStream = httpConnection.getInputStream();         
			  }     
			 } catch (MalformedURLException e) {               
				 e.printStackTrace();     
			 } catch (IOException e) {              
				e.printStackTrace();    
		  } 
		byte[] data = inputStreamToByte(inStream);

		return data;
	}
	
	public static int getBmpLength(final Bitmap bitmap) {
		int len = 0;
		
		if (bitmap != null) {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			bitmap.compress(CompressFormat.PNG, 100, output);
			len = output.toByteArray().length;
			output.reset();
		}
		
		return len;
	}
	
	public static byte[] inputStreamToByte(InputStream is) {
		try{
			ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
			int ch;
			while ((ch = is.read()) != -1) {
				bytestream.write(ch);
			}
			byte imgdata[] = bytestream.toByteArray();
			bytestream.close();
			return imgdata;
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static byte[] readFromFile(String fileName, int offset, int len) {
		if (fileName == null) {
			return null;
		}

		File file = new File(fileName);
		if (!file.exists()) {
			LogUtil.i(TAG, "readFromFile: file not found");
			return null;
		}

		if (len == -1) {
			len = (int) file.length();
		}

		Log.d(TAG, "readFromFile : offset = " + offset + " len = " + len + " offset + len = " + (offset + len));

		if(offset <0){
			LogUtil.i(TAG, "readFromFile invalid offset:" + offset);
			return null;
		}
		if(len <=0 ){
			LogUtil.i(TAG, "readFromFile invalid len:" + len);
			return null;
		}
		if(offset + len > (int) file.length()){
			LogUtil.i(TAG, "readFromFile invalid file len:" + file.length());
			return null;
		}

		byte[] b = null;
		try {
			RandomAccessFile in = new RandomAccessFile(fileName, "r");//创建合适文件大小的数组
			b = new byte[len];
			in.seek(offset);
			in.readFully(b);
			in.close();

		} catch (Exception e) {
			LogUtil.i(TAG, "readFromFile : errMsg = " + e.getMessage());
			e.printStackTrace();
		}
		return b;
	}
	
	public static Bitmap extractThumbNailFromDescriptor(final FileDescriptor fileDescriptor, final int height, final int width, final boolean circle) {
		Assert.assertTrue(fileDescriptor != null && height > 0 && width > 0);

		BitmapFactory.Options options = new BitmapFactory.Options();

		try {
			options.inJustDecodeBounds = true;
			Bitmap tmp = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
			if (tmp != null) {
				tmp.recycle();
				tmp = null;
			}

			boolean crop = true; 
			LogUtil.iv(TAG, "----- 1 extractThumbNailFromDescriptor: round=" + width + "x" + height + ", crop=" + crop+",circle:"+circle+",outHeight:"+options.outHeight+",:"+options.outWidth);
			final double beY = options.outHeight * 1.0 / height;
			final double beX = options.outWidth * 1.0 / width;
			Log.d(TAG, "2 extractThumbNailFromDescriptor: extract beX = " + beX + ", beY = " + beY);
			options.inSampleSize = (int) (crop ? (beY > beX ? beX : beY) : (beY < beX ? beX : beY));
			if (options.inSampleSize <= 1) {
				options.inSampleSize = 1;
			}
			
			// NOTE: out of memory error
			while (options.outHeight * options.outWidth / options.inSampleSize > MAX_DECODE_PICTURE_SIZE) {
				options.inSampleSize++;
			}

			int newHeight = height;
			int newWidth = width;
			if (crop) {
				if (beY > beX) {
					newHeight = (int) (newWidth * 1.0 * options.outHeight / options.outWidth);
				} else {
					newWidth = (int) (newHeight * 1.0 * options.outWidth / options.outHeight);
				}
			} else {
				if (beY < beX) {
					newHeight = (int) (newWidth * 1.0 * options.outHeight / options.outWidth);
				} else {
					newWidth = (int) (newHeight * 1.0 * options.outWidth / options.outHeight);
				}
			}

			options.inJustDecodeBounds = false;

			LogUtil.iv(TAG, "3 extractThumbNailFromDescriptor bitmap required size=" + newWidth + "x" + newHeight + ", orig=" + options.outWidth + "x" + options.outHeight + ", sample=" + options.inSampleSize);
			Bitmap bm = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
			if (bm == null) {
				LogUtil.iv(TAG, "4 extractThumbNailFromDescriptor bitmap decode failed");
				return null;
			}

			LogUtil.iv(TAG, "5 extractThumbNailFromDescriptor bitmap decoded size=" + bm.getWidth() + "x" + bm.getHeight());
			final Bitmap scale = Bitmap.createScaledBitmap(bm, newWidth, newHeight, true);
			if (scale != null) {
				bm.recycle();
				//bm = scale;
				
				if (circle) {
					bm = getCircleImage(scale, height, true);
				} else {
					bm = scale;
				}
				LogUtil.iv(TAG, "6 extractThumbNailFromDescriptorbitmap croped size=" + bm.getWidth() + "x" + bm.getHeight());
			}
			
			/*LogUtil.iv(TAG, "6 extractThumbNailFromDescriptor bitmap croped size=" + bm.getWidth() + "x" + bm.getHeight());
			if (crop) {
				final Bitmap cropped = Bitmap.createBitmap(bm, (bm.getWidth() - width) >> 1, (bm.getHeight() - height) >> 1, width, height);
				if (cropped == null) {
					return bm;
				}

				bm.recycle();
				
				if (circle) {
					bm = getCircleImage(cropped, height/2, true);
				} else {
					bm = cropped;
				}
				//LogUtil.iv(TAG, "6 extractThumbNailFromDescriptorbitmap croped size=" + bm.getWidth() + "x" + bm.getHeight());
				
				//bm = cropped;
				
				LogUtil.iv(TAG, "7 extractThumbNailFromDescriptor bitmap croped size=" + bm.getWidth() + "x" + bm.getHeight());
			}*/
			return bm;

		} catch (final OutOfMemoryError e) {
			LogUtil.iv(TAG, "8 extractThumbNailFromDescriptorbitmap decode bitmap failed: " + e.getMessage());
			options = null;
		}

		return null;
	}
	
	public static Bitmap extractThumbNail(final String path, final int height, final int width, final boolean crop) {
		Assert.assertTrue(path != null && !path.equals("") && height > 0 && width > 0);

		BitmapFactory.Options options = new BitmapFactory.Options();

		try {
			options.inJustDecodeBounds = true;
			Bitmap tmp = BitmapFactory.decodeFile(path, options);
			if (tmp != null) {
				tmp.recycle();
				tmp = null;
			}

			LogUtil.iv(TAG, "----- extractThumbNail: round=" + width + "x" + height + ", crop=" + crop);
			final double beY = options.outHeight * 1.0 / height;
			final double beX = options.outWidth * 1.0 / width;
			LogUtil.iv(TAG, "extractThumbNail: extract beX = " + beX + ", beY = " + beY);
			options.inSampleSize = (int) (crop ? (beY > beX ? beX : beY) : (beY < beX ? beX : beY));
			if (options.inSampleSize <= 1) {
				options.inSampleSize = 1;
			}

			// NOTE: out of memory error
			while (options.outHeight * options.outWidth / options.inSampleSize > MAX_DECODE_PICTURE_SIZE) {
				options.inSampleSize++;
			}

			int newHeight = height;
			int newWidth = width;
			if (crop) {
				if (beY > beX) {
					newHeight = (int) (newWidth * 1.0 * options.outHeight / options.outWidth);
				} else {
					newWidth = (int) (newHeight * 1.0 * options.outWidth / options.outHeight);
				}
			} else {
				if (beY < beX) {
					newHeight = (int) (newWidth * 1.0 * options.outHeight / options.outWidth);
				} else {
					newWidth = (int) (newHeight * 1.0 * options.outWidth / options.outHeight);
				}
			}

			options.inJustDecodeBounds = false;

			LogUtil.iv(TAG, "extractThumbNail bitmap required size=" + newWidth + "x" + newHeight + ", orig=" + options.outWidth + "x" + options.outHeight + ", sample=" + options.inSampleSize);
			Bitmap bm = BitmapFactory.decodeFile(path, options);
			if (bm == null) {
				LogUtil.iv(TAG, "extractThumbNail bitmap decode failed");
				return null;
			}

			LogUtil.iv(TAG, "extractThumbNail bitmap decoded size=" + bm.getWidth() + "x" + bm.getHeight());
			final Bitmap scale = Bitmap.createScaledBitmap(bm, newWidth, newHeight, true);
			if (scale != null) {
				bm.recycle();
				bm = scale;
			}

			if (crop) {
				final Bitmap cropped = Bitmap.createBitmap(bm, (bm.getWidth() - width) >> 1, (bm.getHeight() - height) >> 1, width, height);
				if (cropped == null) {
					return bm;
				}

				bm.recycle();
				bm = cropped;
				LogUtil.iv(TAG, "extractThumbNail bitmap croped size=" + bm.getWidth() + "x" + bm.getHeight());
			}
			return bm;

		} catch (final OutOfMemoryError e) {
			LogUtil.iv(TAG, "extractThumbNail decode bitmap failed: " + e.getMessage());
			options = null;
		}

		return null;
	}
	
	public static Bitmap decodeSampledBitmapFromFileForSmall(String filename, int reqWidth, int reqHeight) {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filename, options);

		options.inSampleSize = calculateInSampleSize(options, reqWidth,reqHeight);
		options.inJustDecodeBounds = false;
		options.inDither = false;
		options.inPreferredConfig = Bitmap.Config.RGB_565;
		
		return BitmapFactory.decodeFile(filename, options);
	}
	
	public static Bitmap decodeSampledBitmapFromFile(String filename, int reqWidth, int reqHeight) {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filename, options);

		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
		
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeFile(filename, options);
	}
	
	public static Bitmap decodeSampledBitmapFromDescriptor(FileDescriptor fileDescriptor, int reqWidth, int reqHeight) {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);

		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
				
		options.inJustDecodeBounds = false;
		options.inDither = false;
		options.inPreferredConfig = Bitmap.Config.RGB_565;

		Bitmap tBitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
		return tBitmap;
	}
	
	public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, resId, options);
		
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeResource(res, resId, options);
	}
	
	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			final int heightRatio = Math.round((float) height
					/ (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;

			final float totalPixels = width * height;
			final float totalReqPixelsCap = reqWidth * reqHeight * 2;

			while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
				inSampleSize++;
			}
		}
		return inSampleSize;
	}
}

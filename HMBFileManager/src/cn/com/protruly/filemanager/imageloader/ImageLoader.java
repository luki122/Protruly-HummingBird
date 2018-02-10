package cn.com.protruly.filemanager.imageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cn.com.protruly.filemanager.utils.BitmapUtils;
import cn.com.protruly.filemanager.utils.LogUtil;

/*
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.inusdflyer.util.LogUtil;
*/

public class ImageLoader {
	private static final String TAG = "ImageLoader";
	
	private static ImageLoader sImageLoader;
	private static final int MAX_BITMAP_SIDE = 400;

	public interface ImageProcessingCallback {
		void onImageProcessing(WeakReference<Bitmap> weak, String tag);
	}

	private LruMemoryCache mLruMemoryCache;
	private FileCache mFileCache;
	private ContentProviderThumbnailUtil contentProviderThumbnailUtils;
	private ThreadPoolExecutor executor;
	private Context mContext;
	private Object mPauseLock = new Object();
	private boolean mShouldPause;
	private boolean mPaused;
	private HashSet<Long> mPausedThreadIds = new HashSet<Long>();



	public static ImageLoader getInstance(Context context) {
		if (sImageLoader == null) {
			sImageLoader = new ImageLoader(context);
		}
		return sImageLoader;
	}

	public void notifyPause() {
		mShouldPause = true;
	}

	public void notifyResume() {
		mShouldPause = false;
		resumeLoadingIfNecessary();
	}

	private void pauseLoadingIfNecessary() {
		synchronized (mPauseLock) {
			Long currentThreadId = Thread.currentThread().getId();
			while (mShouldPause) {
				try {
					if(mPausedThreadIds.contains(currentThreadId)) {
						LogUtil.i(TAG, "thread id :" + currentThreadId + " has already been waiting");
						continue;
					}
					LogUtil.i(TAG, "Wait in current thread, thread id is: " + currentThreadId);
					mPausedThreadIds.add(currentThreadId);
					mPauseLock.wait();
					mPausedThreadIds.remove(currentThreadId);
					LogUtil.i(TAG, "remove from wait thread, thread id is: " + currentThreadId);
				} catch (InterruptedException e) {

				}
			}
		}
	}

	private void resumeLoadingIfNecessary() {
		synchronized (mPauseLock) {
			mPauseLock.notifyAll();
		}
	}

	public Bitmap getFromCache(String md5) {
		if (TextUtils.isEmpty(md5)) {
			return null;
		}
		Bitmap bitmap = mLruMemoryCache.getBitmapFromMemCache(md5);
		if (bitmap != null) {
			return bitmap;
		} else {
			bitmap = mFileCache.getFileCache(md5);
			if (bitmap != null) {
				return bitmap;
			}
		}
		return null;
	}

	private BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(15);

	private ImageLoader(Context context) {
		this.mContext = context;
		mLruMemoryCache = LruMemoryCache.getInstance();
		mFileCache = FileCache.getInstance(context);
		int processors = Runtime.getRuntime().availableProcessors();
		LogUtil.i(TAG, "processors: " + processors);

		/*
		executor = new ThreadPoolExecutor(1, processors, 60L,
				TimeUnit.MILLISECONDS, queue, new PriorityThreadFactory(),
				new ThreadPoolExecutor.DiscardOldestPolicy());
				*/
		executor = new ThreadPoolExecutor(1, Math.max(1, processors / 2), 60L,
				TimeUnit.MILLISECONDS, queue, new PriorityThreadFactory(),
				new ThreadPoolExecutor.DiscardOldestPolicy());

		contentProviderThumbnailUtils = ContentProviderThumbnailUtil.getInstance();
		contentProviderThumbnailUtils.setFileCache(mFileCache);
		contentProviderThumbnailUtils.setLruMemoryCache(mLruMemoryCache);

	}

	public void clearCache() {
		mFileCache.clearFileCache();
		mLruMemoryCache.clear();
	}

	private final Handler mHandler = new Handler();

    /**
	 * Not used temporarily, annotated by ShenQianfeng.
	public static void displayRemoteImageIntoImageView(final String url, final String md5, final ImageView imageView) {
		if(TextUtils.isEmpty(url)) return;
		if(null == imageView) return;
		ImageLoader imageLoader = getInstance(imageView.getContext());
		imageLoader.displayRemoteImage(url, md5, new ImageLoader.ImageProcessingCallback() {

			@Override
			public void onImageProcessing(WeakReference<Bitmap> weak, String tag) {
				Bitmap bitmap = weak.get();
				if(null != bitmap) {
					imageView.setImageBitmap(bitmap);
				}
			}
		});
	}
	 */

	public void displayLocalImageResourceIntoImageView(Context context, final int resId, final ImageView imageView) {
		if(null == imageView) return;
		displayLocalImageResource(context, resId, new ImageLoader.ImageProcessingCallback() {
			@Override
			public void onImageProcessing(WeakReference<Bitmap> weak, String tag) {
				Bitmap bitmap = weak.get();
                if(null == bitmap) return;
				if(null != bitmap) {
					imageView.setImageBitmap(bitmap);
				}
			}
		});
	}

	public void displayLocalThumbnail(Context context, final String filePath, String md5, final ImageView imageView) {
        if(null == imageView) return;
		displayLocalThumbnail(context,filePath,md5, new ImageLoader.ImageProcessingCallback() {
			@Override
			public void onImageProcessing(WeakReference<Bitmap> weak, String tag) {
				if( ! ((String)imageView.getTag()).equals(filePath)) {
					return;
				}
				Bitmap bitmap = weak.get();
                if(null == bitmap) return;
				if(null != bitmap) {
					imageView.setImageBitmap(bitmap);
				}
			}
		});
	}

	public void displayLocalThumbnail2(Context context, String filePath, String md5, ImageProcessingCallback imageProcessingCallback) {
		if (TextUtils.isEmpty(filePath)) {
			return;
		}
		Bitmap bitmap = mLruMemoryCache.getBitmapFromMemCache(md5);
		if (bitmap != null) {
			imageProcessingCallback.onImageProcessing(new WeakReference<Bitmap>(bitmap), md5);
			bitmap = null;
		} else {
			ThumbnailDisplayerThread task = new ThumbnailDisplayerThread(context, filePath, md5,
					imageProcessingCallback);
			executor.execute(task);
		}
	}



	public void displayVideoThumbnail(Context context, final String filePath, String md5, final ImageView imageView) {
		if(null == imageView) return;
		displayVideoThumbnail(context,filePath,md5, new ImageLoader.ImageProcessingCallback() {
			@Override
			public void onImageProcessing(WeakReference<Bitmap> weak, String tag) {
				if( ! ((String)imageView.getTag()).equals(filePath)) {
					return;
				}
				Bitmap bitmap = weak.get();
				if(null == bitmap) return;
				if(null != bitmap) {
					imageView.setImageBitmap(bitmap);
				}
			}
		});
	}

	/*=========================== PRIVATE FUNCTIONS BELOW =====================================*/
	/**
	 * not uesed temporarily, annotated it by ShenQianfeng
	private Bitmap getRemoteImage(String url) {
		Bitmap bitmap = null;
		InputStream is = null;
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
		    options.inJustDecodeBounds = false;
		    options.inSampleSize = 2;
			
			HttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet(url);
			HttpResponse response = client.execute(get);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				is = response.getEntity().getContent();
				bitmap = BitmapFactory.decodeStream(is, null, options);
				//int width = bitmap.getWidth();
				//int height = bitmap.getHeight();
				//LogUtil.e("kkkk", "lll");
			}
		} catch (Exception e) {
			String msg = (e == null) ? "" : e.getMessage();
			Log.e(TAG, "get remote image failed" + msg);
		} finally {
			if(null != is) {
				try {
					is.close();
					is = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		}
		return bitmap;
	}

	public void displayRemoteImage(String url, String md5, 
			ImageProcessingCallback imageProcessingCallback) {
		if (TextUtils.isEmpty(url)) {
			return;
		}
		Bitmap bitmap = mLruMemoryCache.getBitmapFromMemCache(md5);
		if (bitmap != null) {
			imageProcessingCallback.onImageProcessing(
					new WeakReference<Bitmap>(bitmap), md5);
			bitmap = null;
		} else {
			BitmapDisplayerThread task = new BitmapDisplayerThread(url, md5,
					imageProcessingCallback);
			executor.execute(task);
		}
	}*/


	public void displayVideoThumbnail(Context context, String filePath, String md5, ImageProcessingCallback imageProcessingCallback) {
		if (TextUtils.isEmpty(filePath)) {
			return;
		}
		Bitmap bitmap = mLruMemoryCache.getBitmapFromMemCache(md5);
		if (bitmap != null) {
			imageProcessingCallback.onImageProcessing(new WeakReference<Bitmap>(bitmap), md5);
			bitmap = null;
		} else {
			VideoThumbnailDisplayerThread task = new VideoThumbnailDisplayerThread(context, filePath, md5, imageProcessingCallback);
			executor.execute(task);
		}
	}

	public static Bitmap createVideoThumbnail(String filePath) {
		// MediaMetadataRetriever is available on API Level 8
		// but is hidden until API Level 10
		Class<?> clazz = null;
		Object instance = null;
		try {
			clazz = Class.forName("android.media.MediaMetadataRetriever");
			instance = clazz.newInstance();

			Method method = clazz.getMethod("setDataSource", String.class);
			method.invoke(instance, filePath);

			// The method name changes between API Level 9 and 10.
			if (Build.VERSION.SDK_INT <= 9) {
				return (Bitmap) clazz.getMethod("captureFrame").invoke(instance);
			} else {
				byte[] data = (byte[]) clazz.getMethod("getEmbeddedPicture").invoke(instance);
				if (data != null) {
					Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
					if (bitmap != null) return bitmap;
				}
				return (Bitmap) clazz.getMethod("getFrameAtTime").invoke(instance);
			}
		} catch (IllegalArgumentException e) {
			// Assume this is a corrupt video file
			Log.e(TAG, "createVideoThumbnail", e);
		} catch (RuntimeException e) {
			// Assume this is a corrupt video file.
			Log.e(TAG, "createVideoThumbnail", e);
		} catch (InstantiationException e) {
			Log.e(TAG, "createVideoThumbnail", e);
		} catch (InvocationTargetException e) {
			Log.e(TAG, "createVideoThumbnail", e);
		} catch (ClassNotFoundException e) {
			Log.e(TAG, "createVideoThumbnail", e);
		} catch (NoSuchMethodException e) {
			Log.e(TAG, "createVideoThumbnail", e);
		} catch (IllegalAccessException e) {
			Log.e(TAG, "createVideoThumbnail", e);
		} finally {
			try {
				if (instance != null) {
					clazz.getMethod("release").invoke(instance);
				}
			} catch (Exception ignored) {
			}
		}
		return null;
	}

	private void displayLocalImageResource(Context context, int imageResId, ImageProcessingCallback imageProcessingCallback) {
		final String md5 = "IMAGE_RESOUCE_CACHE_" + String.valueOf(imageResId);
		Bitmap bitmap = mLruMemoryCache.getBitmapFromMemCache(md5);
		if (bitmap != null) {
			imageProcessingCallback.onImageProcessing(new WeakReference<Bitmap>(bitmap), md5);
			bitmap = null;
		} else {
			ImageResourceDisplayerThread task = new ImageResourceDisplayerThread(context, imageResId, md5,
					imageProcessingCallback);
			executor.execute(task);
		}
	}

	/**
	 *
	 * @param context
	 * @param filePath should be image file path
	 * @param md5 maybe the same with filePath param
	 * @param imageProcessingCallback
	 */
	private void displayLocalThumbnail(Context context, String filePath, String md5, ImageProcessingCallback imageProcessingCallback) {
		if (TextUtils.isEmpty(filePath)) {
			return;
		}
		Bitmap bitmap = mLruMemoryCache.getBitmapFromMemCache(md5);
		if (bitmap != null) {
			imageProcessingCallback.onImageProcessing(new WeakReference<Bitmap>(bitmap), md5);
			bitmap = null;
		} else {
			ThumbnailDisplayerThread task = new ThumbnailDisplayerThread(context, filePath, md5,
					imageProcessingCallback);
			executor.execute(task);
		}
	}

	private class VideoThumbnailDisplayerThread implements Runnable {
		private Context context;
		private Bitmap bitmap;
		private String filePath;
		private String md5;
		private ImageProcessingCallback imageProcessingCallback;

		public VideoThumbnailDisplayerThread(Context context, String filePath,String md5,
										ImageProcessingCallback imageProcessingCallback) {
			super();
			this.context = context;
			this.filePath = filePath;
			this.md5 = md5;
			this.imageProcessingCallback = imageProcessingCallback;
		}

		@Override
		public void run() {

			pauseLoadingIfNecessary();

			bitmap = getFromCache(md5);
			if (bitmap == null) {
				bitmap = createVideoThumbnail(this.filePath);
				if(null != bitmap) {
					//LogUtil.i(TAG, "video bitmap size 111 : [" + bitmap.getWidth() + "x" + bitmap.getHeight() + "]");
					bitmap = BitmapUtils.resizeDownBySideLength(bitmap, MAX_BITMAP_SIDE, true);
					//LogUtil.i(TAG, "video bitmap size 222 : [" + bitmap.getWidth() + "x" + bitmap.getHeight() + "]");
				}

			}
			if (bitmap != null) {
				mLruMemoryCache.addBitmapToMemoryCache(md5, bitmap);
				mFileCache.saveBitmapByLru(md5, bitmap);
			}
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					imageProcessingCallback.onImageProcessing(new WeakReference<Bitmap>(bitmap), md5);
					bitmap = null;
				}
			});
		}

	}

	private class ThumbnailDisplayerThread implements Runnable {
		private Context context;
		private Bitmap bitmap;
		private String filePath;
		private String md5;
		private ImageProcessingCallback imageProcessingCallback;

		public ThumbnailDisplayerThread(Context context, String filePath,String md5,
				ImageProcessingCallback imageProcessingCallback) {
			super();
			this.context = context;
			this.filePath = filePath;
			this.md5 = md5;
			this.imageProcessingCallback = imageProcessingCallback;
		}

		@Override
		public void run() {
			pauseLoadingIfNecessary();
			bitmap = getFromCache(md5);
			if (bitmap == null) {
				bitmap = contentProviderThumbnailUtils.getThumbnail(context, filePath, imageProcessingCallback);
				if(null != bitmap) {
					bitmap = BitmapUtils.resizeDownBySideLength(bitmap, MAX_BITMAP_SIDE, true);
				}
			}
			if (bitmap != null) {
				mLruMemoryCache.addBitmapToMemoryCache(md5, bitmap);
				mFileCache.saveBitmapByLru(md5, bitmap);
			}
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					imageProcessingCallback.onImageProcessing(
							new WeakReference<Bitmap>(bitmap), md5);
					bitmap = null;
				}
			});
		}

	}

	
	private class ImageResourceDisplayerThread implements Runnable {
		private Context context;
		private int imageResourceId;
		private Bitmap bitmap;
		private String md5;
		private ImageProcessingCallback imageProcessingCallback;

		public ImageResourceDisplayerThread(Context context, int imageResId, String md5,
				ImageProcessingCallback imageProcessingCallback) {
			super();
			this.context = context;
			this.imageResourceId = imageResId;
			this.md5 = md5;
			this.imageProcessingCallback = imageProcessingCallback;
		}

		@Override
		public void run() {
			pauseLoadingIfNecessary();
			bitmap = getFromCache(md5);
			if (bitmap == null) {
				BitmapFactory.Options options = new BitmapFactory.Options();
			    options.inJustDecodeBounds = false;
			    options.inSampleSize = 2;
				bitmap = BitmapFactory.decodeResource(context.getResources(), imageResourceId, options);
			}
			if (bitmap != null) {
				mLruMemoryCache.addBitmapToMemoryCache(md5, bitmap);
				//no need to save Bitmap to disk.
				//mFileCache.saveBitmapByLru(md5, bitmap);
			}
			mHandler.post(new Runnable() {

				@Override
				public void run() {
					imageProcessingCallback.onImageProcessing(
							new WeakReference<Bitmap>(bitmap), md5);
					bitmap = null;
				}
			});
		}

	}

	/**
	 * Not used temporarily, annotated by ShenQianfeng
	private class BitmapDisplayerThread implements Runnable {
		private Bitmap bitmap;
		private String url;
		private String md5;
		private ImageProcessingCallback imageProcessingCallback;

		public BitmapDisplayerThread(String url, String md5,
				ImageProcessingCallback imageProcessingCallback) {
			super();
			this.url = url;
			this.md5 = md5;
			this.imageProcessingCallback = imageProcessingCallback;
		}

		@Override
		public void run() {
			bitmap = getFromCache(md5);
			if (bitmap == null) {
				bitmap = getRemoteImage(url);
			}
			if (bitmap != null) {
				mLruMemoryCache.addBitmapToMemoryCache(md5, bitmap);
				mFileCache.saveBitmapByLru(md5, bitmap);
			}
			mHandler.post(new Runnable() {

				@Override
				public void run() {
					imageProcessingCallback.onImageProcessing(
							new WeakReference<Bitmap>(bitmap), md5);
					bitmap = null;
				}
			});
		}

	}
	 *
	 */

}
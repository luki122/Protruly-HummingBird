package cn.com.protruly.filemanager.imageloader;

import android.graphics.Bitmap;
import android.util.LruCache;

public class LruMemoryCache {

	private static LruMemoryCache mInstance;

	public static LruMemoryCache getInstance() {
		if (mInstance == null) {
			mInstance = new LruMemoryCache();
		}
		return mInstance;
	}

	public LruMemoryCache() {
		super();
	}

	private final int mMaxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
	private final int cacheSize = mMaxMemory / 8;
	private LruCache<String, Bitmap> mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {

		@Override
		protected int sizeOf(String key, Bitmap bitmap) {
			return bitmap.getByteCount() / 1024;
		}

		@Override
		protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
			// TODO Auto-generated method stub
			super.entryRemoved(evicted, key, oldValue, newValue);
		}
	};

	public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
		if (key != null && bitmap != null && !bitmap.isRecycled() &&
				getBitmapFromMemCache(key) == null) {
			mMemoryCache.put(key, bitmap);
		}
	}

	public Bitmap getBitmapFromMemCache(String key) {
		return mMemoryCache.get(key);
	}

	public void removeBitmapFromMemCache(String key) {
		mMemoryCache.remove(key);
	}

	public void clear() {
		mMemoryCache.evictAll();
	}
}

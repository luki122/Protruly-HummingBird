package cn.com.protruly.filemanager.imageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.File;

import cn.com.protruly.filemanager.utils.LogUtil;
import cn.com.protruly.filemanager.utils.MD5Util;

public class FileCache {
	private final String TAG = "FileCache";
	private DiskLruCache diskLruCache;
	private File cacheDir;
	private static final int DISK_MAX_SIZE = 20 * 1024 * 1024;// SD 20MB

	private static FileCache instance;

	public static FileCache getInstance(Context context) {
		if (instance == null) {
			instance = new FileCache(context);
		}
		return instance;
	}

	public FileCache(Context context) {
		super();
		try {
			cacheDir = DiskLruCache.getDiskCacheDir(context, "imageCache");
			diskLruCache = DiskLruCache.openCache(cacheDir, DISK_MAX_SIZE);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Bitmap getFileCache(String url) {
		//LogUtil.i(TAG, "getFileCache url 111:" + url);
		String md5 = MD5Util.stringToMD5(url);
		//LogUtil.i(TAG, "getFileCache url 222:" + md5);
		if (diskLruCache == null) {
			Log.d(TAG, "diskLruCache is null");
			return null;
		}
		return diskLruCache.get(md5);
	}

	public void saveBitmapByLru(String key, Bitmap bitmap) {
		if (diskLruCache == null) {
			Log.d(TAG, "diskLruCache is null");
			return;
		}
		//LogUtil.i(TAG, "saveBitmapByLru key 111:" + key);
		key = MD5Util.stringToMD5(key);
		//LogUtil.i(TAG, "saveBitmapByLru key 222:" + key);
		if (key != null && bitmap != null) {
			synchronized (diskLruCache) {
				if (!diskLruCache.containsKey(key)) {
					diskLruCache.put(key, bitmap);
				}
			}
		}
	}

	public void clearFileCache() {
		if (diskLruCache == null) {
			Log.d(TAG, "diskLruCache is null");
			return;
		}
		diskLruCache.clearCache();
	}


}

package cn.com.protruly.filemanager.imageloader;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.Thumbnails;
import android.util.Log;

import java.lang.ref.WeakReference;

import cn.com.protruly.filemanager.imageloader.ImageLoader.ImageProcessingCallback;

public class ContentProviderThumbnailUtil {
	
	private static ContentProviderThumbnailUtil instance;
	
	private ContentProviderThumbnailUtil () {}
	
	public static ContentProviderThumbnailUtil getInstance() {
		if(instance == null) {
			instance = new ContentProviderThumbnailUtil();
		}
		return instance;
	}
	
	private LruMemoryCache lruMemoryCache;
	private FileCache mFileCache;

	public void setLruMemoryCache(LruMemoryCache lruMemoryCache) {
		this.lruMemoryCache = lruMemoryCache;
	}

	public void setFileCache(FileCache fileCache) {
		this.mFileCache = fileCache;
	}
	
	public Bitmap getThumbnail(Context context, String filePath, ImageProcessingCallback imageProcessingCallback) {

		filePath = filePath.replaceAll("'", "''");

		Uri uri = Images.Media.EXTERNAL_CONTENT_URI;
		ContentResolver cr = context.getContentResolver();
		String[] projection = {Thumbnails._ID };
		String selection = Thumbnails.DATA + "='" + filePath + "'";
		int id = -1;
		Cursor cursor = cr.query(uri, projection, selection, null, null);
		BitmapFactory.Options options = new BitmapFactory.Options();    
	    options.inDither = false;    
	    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		//options.outWidth = 200;
	    //options.outHeight = 200;
	    String md5 = filePath;
		try {
			while (cursor.moveToNext()) {
				id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
				if (id != -1) {
					Bitmap bmp = Thumbnails.getThumbnail(cr, id, Thumbnails.MICRO_KIND, options);
					return bmp;
				}
			}
		} finally {
			if(cursor != null && ! cursor.isClosed()) {
				cursor.close();
			}
		}
		return null;
	}
}

package com.android.providers.downloads;

import com.android.downloadui.ThumbnailCache;

import android.app.ActivityManager;
import android.app.Application;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Point;
import android.os.RemoteException;
import android.text.format.DateUtils;

public class DownloadApplication extends Application{
	private static final long PROVIDER_ANR_TIMEOUT = 20 * DateUtils.SECOND_IN_MILLIS;
	private ThumbnailCache mThumbnails;
	private Point mThumbnailsSize;
	/// M: Store whether device is low ram device
    private static boolean sIsLowRamDevice = false;
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		final ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final int memoryClassBytes = am.getMemoryClass() * 1024 * 1024;
        /// M: init right value from ActivityManager at onCreate
        sIsLowRamDevice = am.isLowRamDevice();
        mThumbnails = new ThumbnailCache(memoryClassBytes / 4);
	}
	
	public static ThumbnailCache getThumbnailsCache(Context context, Point size) {
        final DownloadApplication app = (DownloadApplication) context.getApplicationContext();
        final ThumbnailCache thumbnails = app.mThumbnails;
        if (!size.equals(app.mThumbnailsSize)) {
            thumbnails.evictAll();
            app.mThumbnailsSize = size;
        }
        return thumbnails;
    }

    public static ContentProviderClient acquireUnstableProviderOrThrow(
            ContentResolver resolver, String authority) throws RemoteException {
        final ContentProviderClient client = resolver.acquireUnstableContentProviderClient(
                authority);
        if (client == null) {
            throw new RemoteException("Failed to acquire provider for " + authority);
        }
        /// M: Ignore follow case:
        /// 1. Low ram device, in these device system and IO performance is not good, need not acquire provider
        /// respond in 20s.
        /// 2. External storage provider, because it query from file system, if there are too many(>10000)
        /// files in one folder, it will spend long time(may 1 min) to finish create cursor. {@
        if (!sIsLowRamDevice && !"com.android.externalstorage.documents".equals(authority)) {
            client.setDetectNotResponding(PROVIDER_ANR_TIMEOUT);
        }
        /// @}
        return client;
    }
    
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (level >= TRIM_MEMORY_MODERATE) {
            mThumbnails.evictAll();
        } else if (level >= TRIM_MEMORY_BACKGROUND) {
            mThumbnails.trimToSize(mThumbnails.size() / 2);
        }
    }
    
}

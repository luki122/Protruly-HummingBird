package com.android.downloadui;

import com.android.providers.downloads.DownloadApplication;
import android.content.AsyncTaskLoader;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.DeadObjectException;
import android.os.OperationCanceledException;
import android.os.RemoteException;
import android.provider.Downloads;
import android.util.Log;
import libcore.io.IoUtils;

/**
 * @author wxue
 */
class DownloadResult implements AutoCloseable {
    ContentProviderClient client;
    Cursor mDowningCursor;
    Cursor mDownloadedCursor;
    Exception exception;

    @Override
    public void close() {
        IoUtils.closeQuietly(mDowningCursor);
        IoUtils.closeQuietly(mDownloadedCursor);
        ContentProviderClient.releaseQuietly(client);
        mDowningCursor = null;
        mDownloadedCursor = null;
        client = null;
    }
}

public class DownloadListLoader extends AsyncTaskLoader<DownloadResult>{
	private static final String TAG = "DownloadListLoader";
	private final ForceLoadContentObserver mObserver = new ForceLoadContentObserver();
	private final Uri mUri;
	private CancellationSignal mSignal;
	private DownloadResult mResult;
	private int mUserOrder;
	
	// show previous loader's result
    private boolean mIsLoading = false;
	
	public DownloadListLoader(Context context, Uri uri, int order) {
		super(context);
		this.mUri = uri;
		this.mUserOrder = order;
	}
	
	@Override
	public DownloadResult loadInBackground() {
		mIsLoading = true;
        synchronized (this) {
            if (isLoadInBackgroundCanceled()) {
                throw new OperationCanceledException();
            }
            mSignal = new CancellationSignal();
        }
        final ContentResolver resolver = getContext().getContentResolver();
        final DownloadResult result = new DownloadResult();
        Cursor downingCursor = null;
        Cursor downloadedCursor = null;
        ContentProviderClient client = null;
        String where = null;
        String[] whereArgs = null;
        try {
        	where = Downloads.Impl.COLUMN_STATUS + "!=?";
        	whereArgs = new String[]{ String.valueOf(Downloads.Impl.STATUS_SUCCESS) };
            client = DownloadApplication.acquireUnstableProviderOrThrow(resolver, mUri.getAuthority());
            downingCursor = client.query(mUri, null, where, whereArgs, getQuerySortOrder(true), mSignal);
            HbDbUtil.debugCursor(getContext(), downingCursor);
            
            where = Downloads.Impl.COLUMN_STATUS + "=?";
            downloadedCursor = client.query(mUri, null, where, whereArgs, getQuerySortOrder(false), mSignal);
            HbDbUtil.debugCursor(getContext(), downloadedCursor);
            if (downingCursor == null || downloadedCursor == null) {
                throw new RemoteException("Provider returned null");
            }
            
            downingCursor.registerContentObserver(mObserver);
            if(mUserOrder == HbCommonUtil.HB_SORT_ORDER_NAME){
            	downingCursor = new SortByNameCursorWrapper(downingCursor);
            	downloadedCursor = new SortByNameCursorWrapper(downloadedCursor);
            }
            result.client = client;
            result.mDowningCursor = downingCursor;
            result.mDownloadedCursor = downloadedCursor;
        }catch (Exception e) {
            Log.w(TAG, "Failed to query download information", e);
            result.exception = e;
            ContentProviderClient.releaseQuietly(client);
        } finally {
            synchronized (this) {
                mSignal = null;
            }
        }
        mIsLoading = false;
		return result;
	}
	
	@Override
	public void cancelLoadInBackground() {
		super.cancelLoadInBackground();
		synchronized (this) {
	        if (mSignal != null) {
	            mSignal.cancel();
	        }
	    }
	}
	
	@Override
	public void deliverResult(DownloadResult result) {
		 if (isReset()) {
	            IoUtils.closeQuietly(result);
	            return;
	        }
	        /// M: If the given result has exception with DeadObjectException type, it means
	        /// client has died, we need load directory it again.
	        if (isStarted() && result != null && result.exception != null
	                && (result.exception instanceof DeadObjectException)) {
	            Log.d(TAG, "deliverResult with client has dead, reload downloadinfo again");
	            IoUtils.closeQuietly(result);
	            forceLoad();
	            return;
	        }

	        DownloadResult oldResult = mResult;
	        mResult = result;

	        if (isStarted()) {
	            super.deliverResult(result);
	        }

	        if (oldResult != null && oldResult != result) {
	            IoUtils.closeQuietly(oldResult);
	        }
	}
	
	@Override
	protected void onStartLoading() {
        boolean contentChanged = takeContentChanged();
        if (mResult != null) {
            /// Check current contentprovider client, if the server has died, we need reload
            /// to register observer. @{
            try {
                mResult.client.canonicalize(mUri);
                deliverResult(mResult);
            } catch (Exception e) {
                contentChanged = true;
                Log.d(TAG, "onStartLoading with client has dead, reload to register obsever. " + e);
            }
            /// @}
        }
        Log.d(TAG, "onStartLoading contentChanged: " + contentChanged + ", mIsLoading: " + mIsLoading + ", mResult: " + mResult);
        if (!contentChanged && mIsLoading) {
            return;
        }
        if (contentChanged || mResult == null) {
            forceLoad();
        }
	}
	
	@Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    public void onCanceled(DownloadResult result) {
        /// M: show previous loader's result @{
        if (result == null) {
            return;
        }
        if (result.exception != null && (result.exception instanceof OperationCanceledException)) {
            IoUtils.closeQuietly(result);
            Log.d(TAG, "DirectoryLoader: loading has been canceled, no deliver result");
            return;
        }
        if (!isReset() && (mResult == null)) {
            deliverResult(result);
            Log.d(TAG, "DownloadListLoader show result when onCanceled");
        } else {
            IoUtils.closeQuietly(result);
        }
        /// @}
    }

    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        IoUtils.closeQuietly(mResult);
        mResult = null;
        getContext().getContentResolver().unregisterContentObserver(mObserver);
    }
    
	private String getQuerySortOrder(boolean isDownloading) {
		String orderBy = "";
		switch (mUserOrder) {
		case HbCommonUtil.HB_SORT_ORDER_NAME:
			orderBy = Downloads.Impl.COLUMN_LAST_MODIFICATION + " DESC";
			break;
		case HbCommonUtil.HB_SORT_ORDER_TIME:
			if(isDownloading){
				orderBy = Downloads.Impl._ID + " ASC";
			}else{
				orderBy = Downloads.Impl.COLUMN_LAST_MODIFICATION + " DESC";
			}
			break;
		case HbCommonUtil.HB_SORT_ORDER_SIZE_BIG_SMALL:
			orderBy = Downloads.Impl.COLUMN_TOTAL_BYTES + " DESC";
			break;
		case HbCommonUtil.HB_SORT_ORDER_SIZE_SMALL_BIG:
			orderBy = Downloads.Impl.COLUMN_TOTAL_BYTES + " ASC";
			break;
		default:
			break;
		}
		return orderBy;
	}
}

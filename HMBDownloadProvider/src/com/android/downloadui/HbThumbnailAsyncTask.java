package com.android.downloadui;

import com.android.downloadui.ProviderExecutor.Preemptable;
import com.android.providers.downloads.DownloadApplication;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.ImageView;

/**
 * @author wxue
 */
public class HbThumbnailAsyncTask extends AsyncTask<Uri, Void, Bitmap> implements Preemptable {
	private static final String TAG = "ThumbnailAsyncTask";
	private final Uri mUri;
	private final ImageView mIconThumb;
	private final Point mThumbSize;
	private final CancellationSignal mSignal;

	public HbThumbnailAsyncTask(Uri uri, ImageView iconThumb, Point thumbSize) {
		mUri = uri;
		mIconThumb = iconThumb;
		mThumbSize = thumbSize;
		mSignal = new CancellationSignal();
	}

	@Override
	public void preempt() {
		cancel(false);
		mSignal.cancel();
	}

	@Override
	protected Bitmap doInBackground(Uri... params) {
		if (isCancelled())
			return null;
		LogUtil.i(TAG, "Loading thumbnail for " + mUri);
		final Context context = mIconThumb.getContext();
		final ContentResolver resolver = context.getContentResolver();

		ContentProviderClient client = null;
		Bitmap result = null;
		try {
			client = DownloadApplication.acquireUnstableProviderOrThrow(resolver, mUri.getAuthority());
			result = DocumentsContract.getDocumentThumbnail(client, mUri, mThumbSize, mSignal);
			if (result != null) {
				final ThumbnailCache thumbs = DownloadApplication.getThumbnailsCache(context, mThumbSize);
				thumbs.put(mUri, result);
			}
		} catch (Exception e) {
			if (!(e instanceof OperationCanceledException)) {
				LogUtil.i(TAG, "Failed to load thumbnail for " + mUri + ": " + e);
			}
		} finally {
			ContentProviderClient.releaseQuietly(client);
		}
		return result;
	}

	@Override
	protected void onPostExecute(Bitmap result) {
		if (mIconThumb.getTag() == this && result != null) {
			mIconThumb.setTag(null);
			mIconThumb.setImageBitmap(result);
		}
	}
}

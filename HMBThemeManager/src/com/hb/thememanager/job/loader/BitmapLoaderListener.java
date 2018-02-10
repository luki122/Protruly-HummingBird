package com.hb.thememanager.job.loader;

import android.graphics.Bitmap;

public interface BitmapLoaderListener {
	void onSuccess(Bitmap b);

    void onError();
}

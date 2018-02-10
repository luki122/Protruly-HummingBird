package com.hb.thememanager.job;

import android.content.Context;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.module.GlideModule;

public class ImageLoaderModule  implements GlideModule{

	private static final String TAG = "Glide.Module";
	@Override
	public void applyOptions(Context context, GlideBuilder builder) {
		// TODO Auto-generated method stub
		
		builder.setDecodeFormat(DecodeFormat.PREFER_ARGB_8888);
		MemorySizeCalculator calculator = new MemorySizeCalculator(context);
	    int defaultMemoryCacheSize = calculator.getMemoryCacheSize();
	    int defaultBitmapPoolSize = calculator.getBitmapPoolSize();

	    int customMemoryCacheSize = (int) (1.2 * defaultMemoryCacheSize);
	    int customBitmapPoolSize = (int) (1.2 * defaultBitmapPoolSize);

	    builder.setMemoryCache(new LruResourceCache(customMemoryCacheSize));
	    builder.setBitmapPool(new LruBitmapPool(customBitmapPoolSize));
	    Log.d("theme", "customBitmapPoolSize->"+customBitmapPoolSize);
	}

	@Override
	public void registerComponents(Context context, Glide glide) {
		// TODO Auto-generated method stub
		
	}




}

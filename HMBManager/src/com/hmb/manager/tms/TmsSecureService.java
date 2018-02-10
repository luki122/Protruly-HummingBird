package com.hmb.manager.tms;

import tmsdk.common.TMSService;
import android.content.Intent;

/**
 * 常驻内存的后台服务
 */
public final class TmsSecureService extends TMSService {

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
	}
}
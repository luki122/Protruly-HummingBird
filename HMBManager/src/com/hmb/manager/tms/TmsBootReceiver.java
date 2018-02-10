package com.hmb.manager.tms;

import tmsdk.common.TMSBootReceiver;

import com.hmb.manager.autocheck.AutoCheckService;

import android.content.Context;
import android.content.Intent;

/**
 * 开机事件监听
 */
public final class TmsBootReceiver extends TMSBootReceiver {

	@Override
	public void doOnRecv(final Context context, Intent intent) {
		super.doOnRecv(context, intent);
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			Intent i = new Intent(context, AutoCheckService.class);
			context.startService(i);
		}
	}
}
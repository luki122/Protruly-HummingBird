package com.hmb.manager.mark;


import java.util.ArrayList;
import java.util.List;

import com.hmb.manager.aidl.MarkResult;

import tmsdk.common.creator.ManagerCreatorC;
import tmsdk.common.module.numbermarker.NumMarkerManager;
import tmsdk.common.module.numbermarker.NumQueryRet;
import tmsdk.common.module.numbermarker.NumberMarkEntity;

import tmsdk.common.module.numbermarker.OnNumMarkReportFinish;
import android.text.TextUtils;
import android.util.Log;

public class MarkManager {
	
	public static final String TAG = "MarkManager";
	
	private static NumMarkerManager mNumMarkerManager;
	
	public static synchronized  NumMarkerManager  getInstance() {
		if(mNumMarkerManager == null) {
			mNumMarkerManager = ManagerCreatorC.getManager(NumMarkerManager.class);
		}
		return mNumMarkerManager;
	}	
	
	
	public static final int TYPE_CHECK_COMM = 16; //通用
	public static final int TYPE_CHECK_CALLING = 17; //主叫
	public static final int TYPE_CHECK_CALLED = 18; //被叫
	
	private static NumQueryRet getMarkInternal(final int type, String number) {
		if(TextUtils.isEmpty(number)) {
			return null;
		}
		final String numberF = number;
		// 本地查
		Log.v(TAG, "localFetchNumberInfo--inputNumber:[" + numberF + "]");
		NumQueryRet item = getInstance().localFetchNumberInfo(numberF);
		
		//放弃云查，改为定时或者不定时更新数据库
		return item;
	}
	
    public static MarkResult getMark(int type, String number) {
    	NumQueryRet item = getMarkInternal(type, number);
    	if(item != null) {
    		MarkResult result = new MarkResult(item.property, item.number, item.name, item.tagType, item.tagCount, item.warning, item.usedFor, item.location, item.eOperator);
    		return result;
    	}
    	return null;    	    
    }
    
    public static String getTagName(int tagType) {
    	return getInstance().getTagName(tagType);
    }

	public static void UpdateMark(String name, String number) {

		final List<NumberMarkEntity> numberMarkEntityList = new ArrayList<NumberMarkEntity>(1);
		NumberMarkEntity entity = new NumberMarkEntity();
		entity.phonenum = number;//电话号码
		entity.tagtype = NumberMarkEntity.TAG_TYPE_SELF_TAG;//用户自定义标签，详情见javadoc
		entity.userDefineName = name/*"自定义标签"*/;//这个应该是展示给用户标签中的一个自定义标签
		
		numberMarkEntityList.add(entity);
		new Thread() {
			@Override
			public void run() {
				boolean error = getInstance().cloudReportPhoneNum(numberMarkEntityList, new OnNumMarkReportFinish() {
					@Override
					public void onReportFinish(int result) {
						Log.v(TAG, "UpdateMark Success");
					}
				});
				if(!error) {//接口调用失败
					Log.v(TAG, "UpdateMark error");
				}
			}
		}.start();
		
	}
	
}

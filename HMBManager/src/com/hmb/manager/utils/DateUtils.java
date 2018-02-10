package com.hmb.manager.utils;


import java.text.SimpleDateFormat;

import android.content.Context;

import com.hmb.manager.R;

public class DateUtils {
    
    /**
     * HMBManager日期格式
     *
     * 五分钟内 - 刚刚
     * 超过五分钟则显示具体时间日期：2017年5月2日 18：19：57
     *
     * @return
     */
    public static String HMBManagerDate(Context context, long timeStamp) {
        SimpleDateFormat sdf = new SimpleDateFormat(context.getResources().getString(R.string.app_setting_update_time_format));
        long currentTime = System.currentTimeMillis();
        if (currentTime - timeStamp <= 5 * 60 * 1000) {
            return context.getResources().getString(R.string.app_setting_update_just);
        }
        return sdf.format(timeStamp);
    }
}
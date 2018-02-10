package com.hmb.manager.sms;

import com.hmb.manager.aidl.RejectSmsResult;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;
import tmsdk.common.SmsEntity;
import tmsdk.common.creator.ManagerCreatorC;
import tmsdk.common.module.intelli_sms.IntelliSmsCheckResult;
import tmsdk.common.module.intelli_sms.IntelliSmsManager;
import tmsdk.common.tcc.SmsCheckerContentTypes;

public class SmartSmsManager {
    
    public static final String TAG = "SmartSmsManager";
    
    private static IntelliSmsManager mIntelliSmsManager;
    
    private static synchronized  IntelliSmsManager  getInstance() {        
        if(mIntelliSmsManager == null) {
            mIntelliSmsManager = ManagerCreatorC.getManager(IntelliSmsManager.class);
            //必须初始化,与destroy（）一一对应
            mIntelliSmsManager.init();
        }
        return mIntelliSmsManager;
    }
    
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        mIntelliSmsManager.destroy();//必须销毁
//    }
    //tangyisen modify from boolean to RejectSmsResult
    public static RejectSmsResult canRejectSms(String number, String smscontent){
        Log.d(TAG, "canRejectSms number " +number + ", smscontent = " + smscontent);
        if(TextUtils.isEmpty(number) || TextUtils.isEmpty(smscontent)) {
            return null;
        }
        SmsEntity sms = new SmsEntity();
        sms.phonenum = number;
        sms.body = smscontent;
        // 智能拦截调用接口
        IntelliSmsCheckResult checkresult = null;
        checkresult = getInstance().checkSms(sms, false);//本地查
//        ConnectivityManager manager = (ConnectivityManager) TmsApp.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);  
//        NetworkInfo activeInfo = manager.getActiveNetworkInfo();  
//        if(activeInfo != null && activeInfo.isConnected()) {
//              checkresult = getInstance().checkSms(sms, true);//支持云查,最好是在wifi情况下云查。
//        }
        if(checkresult != null) {  
            Log.d(TAG, "checkresult.suggestion =  " + checkresult.suggestion);
            boolean reject = IntelliSmsCheckResult.shouldBeBlockedOrNot(checkresult);
            String rejectTag = null;
            if(reject) {
                rejectTag = getContentType(checkresult.contentType());
            }
            RejectSmsResult result = new RejectSmsResult( reject ? 1 : 0, rejectTag );
            return result;//IntelliSmsCheckResult.shouldBeBlockedOrNot(checkresult);
        }
//        检测结果还有一个不太重要的数据，是IntelliSmsCheckResult. contentType()返回的值，表示检测到的短信的类型，取值范围来自SmsCheckerContentTypes定义的常量。
//        这个类型并不是非常准确的数据。腾讯手机管家一般只判断前四种类型。
        return null;
    }
    
    public static boolean canRejectSmsByKeyword(String smscontent) {
        Log.d(TAG, "canRejectSmsByKeyword  smscontent = " + smscontent);
        if(TextUtils.isEmpty(smscontent)) {
            return false;
        }
        return KeyWordFilter.doFilter(smscontent);
    }

    //begin tangyisen
    private static final String[] ARRAY_ContentType1 = {//0-10
        "未初始化",
        "未知类型",
        "正常类型",
        "广告类型",
        "诈骗类型",
        "12590付费电话",
        "色情类型",
        "合法机构类型，如运营商、银行的短信等",
        "MO扣费类型",
        "MT扣费类型",
        "恶意软件"
    };
    private static final String[] ARRAY_ContentType2 = {//40-49
        "电话广告",
        "电话诈骗",
        "银行电话",
        "信用卡电话推销",
        "保险",
        "房地产",
        "培训电话",
        "中小企业会议邀请电话",
        "网络电话",
        "联通的隐藏号码增值服务"
    };

    private static String getContentType(int pos) {
        if(0 <= pos && pos <= 10) {
            return ARRAY_ContentType1[pos]+pos;
        } else if(40 <= pos && pos <= 49 ) {
            return ARRAY_ContentType2[pos-40]+pos;
        }
        return null;
    }
    //end tangyisen
}

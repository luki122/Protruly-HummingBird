package com.hb.thememanager.wxapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.hb.thememanager.job.pay.Pay;
import com.hb.thememanager.job.pay.Pay.PayResulteListener;
import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;

public class WXPayEntryActivity extends Activity implements IWXAPIEventHandler {
	private PayResulteListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	mListener = Pay.getPay(this).getmListener();				//wxApi.handleIntent(getIntent(), this)之前
        Pay.wxApi.handleIntent(getIntent(), this);					//会马上处理结果，走onResp！
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Pay.wxApi.handleIntent(intent, this);
    }

    @Override
    public void onReq(BaseReq baseReq) {
    }

    @Override
    public void onResp(BaseResp baseResp) {
        if (baseResp.getType() == ConstantsAPI.COMMAND_PAY_BY_WX) {
        	Log.d("pay_info", "---> wechatpay resp code :: " + baseResp.errCode + "  infoStr :: " + baseResp.errStr);
            switch (baseResp.errCode) {
                case BaseResp.ErrCode.ERR_OK:
                	if(mListener != null)
                		mListener.onSuccess();
                    break;
                default:
                	if(mListener != null)
                		mListener.onFailure();
                    break;
            }
        }
        finish();
    }
}


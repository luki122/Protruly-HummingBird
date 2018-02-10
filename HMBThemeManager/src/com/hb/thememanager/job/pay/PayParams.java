package com.hb.thememanager.job.pay;

import android.app.Activity;
import android.content.Context;
import com.hb.thememanager.job.pay.request.PayRequest;
import com.hb.thememanager.job.pay.request.PayRequestBody;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.Config;

public class PayParams {
    public static final int PAY_WAY_ALIPAY = 2;
    public static final int PAY_WAY_WECHAT = 1;
    private Activity mActivity;
    private String mWechatAppID;
    private int mPayType;
    private PayRequest mPayRequest;

    public Activity getActivity() {
        return mActivity;
    }
    public void setActivity(Activity activity) {
        mActivity = activity;
    }
    public String getWechatAppID() {
        return mWechatAppID;
    }
    public void setWechatAppID(String wechatAppID) {
        mWechatAppID = Config.HttpUrl.WECHATPAY_APPID;
    }
    public int getPayType() {
        return mPayType;
    }
    public void setPayType(int payType) {
        mPayType = payType;
    }
    public PayRequest getPayRequest() {
        return mPayRequest;
    }
    public void setPayRequest(PayRequest payRequest) {
        mPayRequest = payRequest;
    }

    public static class Builder {
        private Activity mActivity;
        private String mWechatAppID;
        private long mQlcId;
        private int mDesigner;
        private int mWaresType;
        private String mWaresId;
        private int mCount;
        private int mPrice;
        private int mPayType;
        private String mTitle;
        private String mDescription;
        private String mMobile;
        private Context mContext;

        public Builder(Context context) {
            mContext = context;
        }
        public PayParams.Builder activity(Activity activity) {
            mActivity = activity;
            return this;
        }
        public PayParams.Builder wechatAppID(String appid) {
            mWechatAppID = appid;
            return this;
        }
        public PayParams.Builder qlcId(long qlcId) {
            mQlcId = qlcId;
            return this;
        }
        public PayParams.Builder designer(int designer) {
            mDesigner = designer;
            return this;
        }
        public PayParams.Builder waresType(int waresType) {
            mWaresType = waresType;
            return this;
        }
        public PayParams.Builder waresId(String waresId) {
            mWaresId = waresId;
            return this;
        }
        public PayParams.Builder count(int count) {
            mCount = count;
            return this;
        }
        public PayParams.Builder price(int price) {
            mPrice = price;
            return this;
        }
        public PayParams.Builder payType(int payType) {
            mPayType = payType;
            return this;
        }
        public PayParams.Builder title(String title) {
            mTitle = title;
            return this;
        }
        public PayParams.Builder description(String description) {
            mDescription = description;
            return this;
        }
        public PayParams.Builder mobile(String mobile) {
            mMobile = mobile;
            return this;
        }

        public PayParams build() {
            PayParams payParams = new PayParams();
            payParams.setActivity(mActivity);
            payParams.setWechatAppID(mWechatAppID);
            payParams.setPayType(mPayType);

            PayRequest payRequest = new PayRequest();
            payRequest.setVersion(CommonUtil.getThemeAppVersion(mContext));
    		payRequest.setDeviceId(CommonUtil.getIMEI(mContext));
    		payRequest.setModel(CommonUtil.getModel(mContext));
    		payRequest.setRomVersion(CommonUtil.getRomVersion());

            PayRequestBody body = new PayRequestBody();
            body.qlcId = mQlcId;
            body.designer = mDesigner;
            body.waresType = mWaresType;
            body.waresId = mWaresId;
            body.count = mCount;
            body.price = mPrice;
            body.payType = mPayType;
            body.title = mTitle;
            body.description = mDescription;
            body.mobile = mMobile;
            payRequest.setBody(body);

            payParams.setPayRequest(payRequest);

            return payParams;
        }
    }
}


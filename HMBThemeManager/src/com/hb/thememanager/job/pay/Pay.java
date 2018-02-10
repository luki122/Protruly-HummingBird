package com.hb.thememanager.job.pay;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import com.hb.thememanager.R;
import com.alipay.sdk.app.PayTask;
import com.hb.thememanager.database.DatabaseFactory;
import com.hb.thememanager.database.ThemeDatabaseController;
import com.hb.thememanager.http.Http;
import com.hb.thememanager.http.response.FastJsonResponseHandler;
import com.hb.thememanager.job.MultiTaskDealer;
import com.hb.thememanager.job.pay.alipay.PayResult;
import com.hb.thememanager.job.pay.request.PayRequest;
import com.hb.thememanager.job.pay.request.PayRequestBody;
import com.hb.thememanager.job.pay.respose.PayResponse;
import com.hb.thememanager.job.pay.respose.PayResponseBody;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.FileUtils;
import com.hb.thememanager.utils.ToastUtils;
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Random;

public class Pay {
	private static final String TAG = "pay_info";
    public static IWXAPI wxApi;
    private PayResulteListener mListener;
    private static Pay mPay;
	private Http mHttp;
	private Handler mHandler;
	private Context mContext;

    private Pay(Context context) {
    	mContext = context;
		mHttp = Http.getHttp(context);
    }

    public static Pay getPay(Context context) {
        if (mPay == null) {
            synchronized (Pay.class) {
                if (mPay == null) {
                    mPay = new Pay(context);
                }
            }
        }
        return mPay;
    }

    public PayResulteListener getmListener() {
		return mListener;
	}

	public void startPay(final PayParams payParams) {
        int payWay = payParams.getPayType();
        switch (payWay) {
            case PayParams.PAY_WAY_ALIPAY:
            	mHttp.get(Config.HttpUrl.ALIPAY_URL, payParams.getPayRequest().createJsonRequest(), new FastJsonResponseHandler<PayResponse>() {
					@Override
					public void onFailure(int statusCode, String error_msg) {
		                Log.d(TAG, "--->  error pay(alipay)  " + "  == statusCode :: " + statusCode + "  == error_msg :: " + error_msg);
						if (mListener != null)
							mListener.onFailure();
					}
					@Override
					public void onSuccess(int statusCode, PayResponse response) {
		                String orderInfo = response.body.orderInfo;
		                Log.d(TAG, "--->  start pay(alipay)  " + "  == orderInfo :: " + orderInfo);
		                payAlipay(payParams.getActivity(), orderInfo);
					}
				});
                break;
            case PayParams.PAY_WAY_WECHAT:
            	PayRequestBody body = payParams.getPayRequest().getBody();
            	body.ip = getIp(mContext);
                if (wxApi == null) {
                    wxApi = WXAPIFactory.createWXAPI(mContext, payParams.getWechatAppID());
                    wxApi.registerApp(payParams.getWechatAppID());
                }
            	if(checkWeixinApp()) {
            		break;
            	}
                //TODO 网络获取map
            	mHttp.get(Config.HttpUrl.WECHATPAY_URL, payParams.getPayRequest().createJsonRequest(), new FastJsonResponseHandler<PayResponse>() {
					@Override
					public void onFailure(int statusCode, String error_msg) {
						Log.d(TAG, "--->  error pay(wechatPay)  " + "  == statusCode :: " + statusCode + "  == error_msg :: " + error_msg);
						if (mListener != null)
							mListener.onFailure();
					}
					@Override
					public void onSuccess(int statusCode, PayResponse response) {
		                Log.d(TAG, "--->  start pay(wechatPay)  " + "  == params :: " + response.body.toString());
		                payWechat(response.body);
					}
				});
                break;
//            case 银联:          后续
        }
    }

    private void payAlipay(final Activity activity,final String orderInfo) {
    	if(mHandler == null)
    		mHandler = new Handler();
		MultiTaskDealer dealer = MultiTaskDealer.startDealer("alipay_start", 1);
		dealer.addTask(new Runnable() {
			@Override
			public void run() {
				PayTask alipay = new PayTask(activity);
                Map<String, String> result = alipay.payV2(orderInfo, true);

                PayResult payResult = new PayResult(result);
                // 对于支付结果，请商户依赖服务端的异步通知结果。同步通知结果，仅作为支付结束的通知。
                String resultInfo = payResult.getResult();// 同步返回需要验证的信息
                String resultStatus = payResult.getResultStatus();
                Log.d(TAG, "---> resultStatus :: " + resultStatus + "  resultInfo :: " + resultInfo);
                if (TextUtils.equals(resultStatus, "9000")) {
                    // 该笔订单是否真实支付成功，需要依赖服务端的异步通知。
                    if (mListener != null) {
                    	mHandler.post(new Runnable() {
							@Override
							public void run() {
								mListener.onSuccess();
							}
						});
                    }
                } else {
                    // 该笔订单真实的支付结果，需要依赖服务端的异步通知。
                    if (mListener != null) {
                    	mHandler.post(new Runnable() {
							@Override
							public void run() {
								mListener.onFailure();
							}
						});
                    }
                }
			}
		});
    }

    private void payWechat(PayResponseBody body) {
        PayReq wxPayReq = new PayReq();
        wxPayReq.appId = body.appid;
        wxPayReq.partnerId = body.partnerid;
        wxPayReq.prepayId = body.prepayid;
        wxPayReq.packageValue = body.packageName;
        wxPayReq.nonceStr = body.noncestr;
        wxPayReq.timeStamp = body.timestamp;
        wxPayReq.sign = body.sign;
        wxApi.sendReq(wxPayReq);
    }
    
    private boolean checkWeixinApp() {
        if (!wxApi.isWXAppInstalled()) {
		ToastUtils.showShortToast(mContext, R.string.pay_msg_no_wechat_client);
            return true;
        }
        if (!wxApi.isWXAppSupportAPI()) {
		ToastUtils.showShortToast(mContext, R.string.pay_msg_wechat_client_low);
            return true;
        }
        return false;
    }
    
    private String getIp(Context context) {
        NetworkInfo info = ((ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                try {
                    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                        NetworkInterface intf = en.nextElement();
                        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                            InetAddress inetAddress = enumIpAddr.nextElement();
                            if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                                return inetAddress.getHostAddress();
                            }
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            } else if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String ipAddress = intToIp(wifiInfo.getIpAddress());
                return ipAddress;
            }
        } else {
            //当前无网络连接,请在设置中打开网络
        }
        return null;
    }
    private String intToIp(int i) {
        return (i & 0xFF ) + "." +
        ((i >> 8 ) & 0xFF) + "." +
        ((i >> 16 ) & 0xFF) + "." +
        ( i >> 24 & 0xFF) ;
    }
    
    public void setPayResulteListener(PayResulteListener listener) {
        mListener = listener;
    }
    public interface PayResulteListener {
        void onSuccess();
        void onFailure();
    }
}



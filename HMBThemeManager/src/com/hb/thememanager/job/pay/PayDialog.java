package com.hb.thememanager.job.pay;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import com.hb.thememanager.R;
import com.hb.thememanager.utils.Config;

public class PayDialog extends Dialog implements View.OnClickListener {
    private Context mContext;
    private ImageView mClose;
    private TextView mTitle;
    private TextView mPrice;
    private TextView mWechatUninstall;
    private boolean mWechatClickable = true;
    private LinearLayout mAlipay;
    private LinearLayout mWechatpay;
    private ImageView mSelectedAlipay;
    private ImageView mSelectedWechat;
    private Button mPayButton;
    private PayParams mPayParams;
    private static final int PAY_MODE_ALIPAY = 2;
    private static final int PAY_MODE_WECHAT = 1;
    private int mPayMode = PAY_MODE_ALIPAY;
    private Pay mPay;
    private Pay.PayResulteListener mResultListener;
    public PayDialog(Context context, PayParams payParams) {
        super(context, R.style.WallpaperSetDialog);
        mPayParams = payParams;
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        Resources resources = context.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();

        View dialogView = View.inflate(context, R.layout.item_pay_dialog_layout, null);
        initView(dialogView);
        setContentView(dialogView);
        setCanceledOnTouchOutside(false);

        Window dialogWindow = this.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = dm.widthPixels;
        dialogWindow.setGravity(Gravity.BOTTOM);
        dialogWindow.setAttributes(lp);
    }
    
    private void initView(View content) {
        mClose = (ImageView)content.findViewById(R.id.pay_close);
        mTitle = (TextView)content.findViewById(R.id.goods_title);
        mPrice = (TextView)content.findViewById(R.id.goods_price);
        mAlipay = (LinearLayout)content.findViewById(R.id.pay_alipay);
        mWechatpay = (LinearLayout)content.findViewById(R.id.pay_wechat);
        mSelectedAlipay = (ImageView) content.findViewById(R.id.selected_alipay);
        mSelectedWechat = (ImageView) content.findViewById(R.id.selected_wechat);
        mPayButton = (Button)content.findViewById(R.id.pay_button);
        mWechatUninstall = (TextView)content.findViewById(R.id.wechat_not_installed);

        IWXAPI wxApi = WXAPIFactory.createWXAPI(mContext, Config.HttpUrl.WECHATPAY_APPID);
        if (!wxApi.isWXAppInstalled()) {
        	mWechatUninstall.setVisibility(View.VISIBLE);
        	mSelectedWechat.setVisibility(View.GONE);
        	mWechatClickable = false;
        }
        
        mTitle.setText(mContext.getString(R.string.pay_dialog_pay_title, mPayParams.getPayRequest().getBody().title));
        mPrice.setText(mContext.getString(R.string.pay_dialog_pay_price, (float)mPayParams.getPayRequest().getBody().price / 100));
        mSelectedAlipay.setImageResource(R.drawable.ic_radio_button_on);
        mPayParams.setPayType(mPayMode);
        mPayParams.getPayRequest().getBody().payType = mPayMode;
        mClose.setOnClickListener(this);
        mAlipay.setOnClickListener(this);
        mWechatpay.setOnClickListener(this);
        mPayButton.setOnClickListener(this);
    }

    public void setPayResultListener(Pay.PayResulteListener listener){
        mResultListener = listener;
    }

    public void setPayParams(PayParams payParams) {
        mPayParams = payParams;
        mTitle.setText(mContext.getString(R.string.pay_dialog_pay_title, mPayParams.getPayRequest().getBody().title));
        mPrice.setText(mContext.getString(R.string.pay_dialog_pay_price, (float)mPayParams.getPayRequest().getBody().price / 100));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.pay_close:
                dismiss();
                break;
            case R.id.pay_alipay:
                if (mPayMode != PAY_MODE_ALIPAY) {
                    mPayMode = PAY_MODE_ALIPAY;
                    mPayParams.setPayType(mPayMode);
                    mPayParams.getPayRequest().getBody().payType = mPayMode;
                    mSelectedAlipay.setImageResource(R.drawable.ic_radio_button_on);
                    mSelectedWechat.setImageResource(R.drawable.ic_radio_button_off);
                }
                break;
            case R.id.pay_wechat:
				if (!mWechatClickable) {
	                break;
				}
                if (mPayMode != PAY_MODE_WECHAT) {
                    mPayMode = PAY_MODE_WECHAT;
                    mPayParams.setPayType(mPayMode);
                    mPayParams.getPayRequest().getBody().payType = mPayMode;
                    mSelectedAlipay.setImageResource(R.drawable.ic_radio_button_off);
                    mSelectedWechat.setImageResource(R.drawable.ic_radio_button_on);
                }
                break;
            case R.id.pay_button:
                if (mPay == null) {
                    mPay = Pay.getPay(mContext);
                }
                mPay.setPayResulteListener(mResultListener);
                mPay.startPay(mPayParams);

                dismiss();;
                break;
        }
    }
}



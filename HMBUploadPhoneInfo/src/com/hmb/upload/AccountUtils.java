package com.hmb.upload;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

/**
 * 账号信息
 * Created by zhaolaichao on 17-7-28.
 */

public class AccountUtils {
    private final static String TAG = "AccoutUtils";
    public static final String A_PKG_NAME = "com.protruly.android.account";
    public static final String LOGIN_FILE_NAME = "login_filename";
    public static final String LOGIN_KEY = "login_key";
    public IAccountUIDCallBack mAccountUIDCallBack;
    private static AccountUtils mAcountUtils = new AccountUtils();
    public static AccountUtils getIntance() {
        return mAcountUtils;
    }

    /**
     * 登录状态
     * @param context
     * @return
     */
    public boolean getLoginState(Context context) {
        boolean isLogin = false;
        try {
            Context context1 = context.createPackageContext(A_PKG_NAME, Context.CONTEXT_IGNORE_SECURITY);
            SharedPreferences share = context1.getSharedPreferences(LOGIN_FILE_NAME,
                    Context.MODE_WORLD_READABLE | Context.MODE_MULTI_PROCESS);
            isLogin = share.getBoolean(LOGIN_KEY, isLogin);  //系统账号登录状态
            Log.v(TAG, "isLogin= " + isLogin);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return isLogin;
    }

    /**
     *
     * 系统获取登录账号id
     * @param context
     */
    public void getAccountID(final Context context) {
        //type = 4 获取登录账号id
        int type = 4;
        AccountManager accountManager = AccountManager.get(context);
        Bundle bundle = new Bundle();
        bundle.putInt("fromSettingType", type);
        accountManager.addAccount("com.protruly.AccountType", null, null, bundle, null, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> amfuture) {
                try {
                    Bundle bundle = amfuture.getResult();
                    String uid = bundle.getString("GET_UID");
                    Log.e(TAG, ">>>>uid>>>" + uid);
                    if (mAccountUIDCallBack != null) {
                        mAccountUIDCallBack.getAccountInfo(bundle);
                    }
                } catch (Exception e) {

                }

            }
        }, null);
    }

    public interface IAccountUIDCallBack {
        void getAccountInfo(Bundle bundle);
    };

    public void setAccountCallBack(IAccountUIDCallBack accountCallBack) {
        mAccountUIDCallBack = accountCallBack;
    }
}

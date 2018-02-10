package com.hb.thememanager.model;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class User {

	private Context mContext;
	
	private static User mInstance;  
	/**
	 * 用户ID
	 */
	private String id;
	/**
	 * 用户昵称
	 */
	private String nickName;
	/**
	 * 用户头像URL
	 */
	private String iconUrl;
	
	private String phone;
	
	private String pwd;
	
	private static String mUri = "http://avatardownload.uc.bqlnv.com.cn/sso/DownloadAvatar";

	private  User(Context context){
		mContext = context;
	}

	public static synchronized User getInstance(Context context) {
		if (null == mInstance) {
			mInstance = new User(context);
		}
		return mInstance;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public String getIconUrl() {
		return iconUrl;
	}

	public void setIconUrl(String iconUrl) {
		this.iconUrl = iconUrl;
	}
	
	public void setPhone(String phone) {
		this.phone = phone.substring(0, 3) + "****" + phone.substring(phone.length()-4, phone.length()) ;
	}

	public String getPhone() {
		return phone;
	}
	
	public void setPwd(String pwd) {
		this.pwd = pwd;
	}
	
	public String getPwd() {
		return pwd;
	}
	
	public static final String A_PKG_NAME = "com.protruly.android.account";
	public static final String LOGIN_FILE_NAME = "login_filename";
	public static final String LOGIN_KEY = "login_key";
    private static final String MEDIA_ID = "MEDIA_ID";
    private static final String USER_NAME = "USER_NAME";

	public void jumpLogin(final getUserInfoCallBack callBack, Activity activity) {
		// type = 0,系统账号正常登录 type = 1, 三方应用调用登录 type = 2, 手机首次开机设置账号 type = 3
		// 调用系统账号退出登录
		int type = 1;
		AccountManager accountManager = AccountManager.get(mContext);
		Bundle bundle = new Bundle();
		bundle.putInt("fromSettingType", type);
		accountManager.addAccount("com.protruly.AccountType", null, null,
				bundle, activity, new AccountManagerCallback<Bundle>() {
					@Override
					public void run(AccountManagerFuture<Bundle> amfuture) {
						try {
							Bundle bundle = amfuture.getResult();

							setPhone(bundle.getString(AccountManager.KEY_ACCOUNT_NAME));
							setPwd(bundle.getString(AccountManager.KEY_PASSWORD));
							setId(bundle.getString(MEDIA_ID));
							setNickName(bundle.getString(USER_NAME));
							setIconUrl(mUri + "?member_id=" + getId() + "&ver=1.1&channel=1&pixel=64x64");
							// call back
							if(callBack != null) {
								callBack.getUserInfoSuccess();
							}
							
						} catch (Exception e) {
							Log.e("huliang", "", e);
						}
					}
				}, null);
	}

	public boolean isLogin() {
	    return isSaveLogin() && !(TextUtils.isEmpty(id)) ;
	}

	public boolean isSaveLogin(){
		boolean isLogin = false;
		try {
			Context context1 = mContext.createPackageContext(A_PKG_NAME, Context.CONTEXT_IGNORE_SECURITY);
			SharedPreferences share = context1.getSharedPreferences(LOGIN_FILE_NAME,
					Context.MODE_WORLD_READABLE | Context.MODE_MULTI_PROCESS);
			isLogin = share.getBoolean(LOGIN_KEY, true);  //系统账号登录状态

		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return isLogin;
	}
	
	@Override
	public String toString() {
		return "User{" +
				"id='" + id + '\'' +
				", nickName='" + nickName + '\'' +
				", iconUrl='" + iconUrl + '\'' +
				", phone='" + phone + '\'' +
				'}';
	}
	
	public void LogOut() {
		id = null;
		nickName = null;
		iconUrl = null;
		phone = null;
	}
}

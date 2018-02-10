package com.hmb.manager.update;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.hmb.manager.Constant;
import com.hmb.manager.SettingActivity;
import com.hmb.manager.utils.NetUtils;
import com.hmb.manager.utils.SPUtils;

import tmsdk.common.creator.ManagerCreatorC;
import tmsdk.common.module.update.UpdateManager;

public class HMBUpdateManager {
	private static final String TAG = "HMBUpdateManager";

	/**
	 * Result Code
	 */
	public static final int RESULT_CODE_ERROR = -1;
	public static final int RESULT_CODE_OK = 0;
	public static final int RESULT_CODE_NO_NETWORK_CONNECTED = 2;
	public static final int RESULT_CODE_CHECK_MOBILE_DATA_CONNECTED = 3;
	public static final int RESULT_CODE_UPDATE_MOBILE_DATA_CONNECTED = 4;
	public static final int RESULT_CODE_NETWORK_ERROR = 5;
	public static final int RESULT_CODE_GET_UPDATE = 6;
	public static final int RESULT_CODE_NO_UPDATE = 7;
	public static final int RESULT_CODE_UPDATE_FINISHED = 8;

	/**
	 * Indicates action type.
	 */
	public static final int UPDATE_ACTION_AUTO = 1;
	public static final int UPDATE_ACTION_CHECK = 2;
	public static final int UPDATE_ACTION_UPDATE = 3;

	/**
	 * Pending result code.
	 */
	private int mPendingResultCode = RESULT_CODE_OK;

	private Context mContext;
	private SettingActivity mActivity;
	private UpdateManager mUpdateManager;

	private static HMBUpdateManager sInstance;

	public static synchronized HMBUpdateManager getInstance(Context context) {
		if (sInstance == null) {
			sInstance = new HMBUpdateManager(context);
		}
		return sInstance;
	}

	private HMBUpdateManager(Context context) {
		mContext = context.getApplicationContext();
		mUpdateManager = ManagerCreatorC.getManager(UpdateManager.class);
	}

	public UpdateManager getUpdateManager() {
		return mUpdateManager;
	}

	public void attachActivity(SettingActivity activity) {
		Log.d(TAG, "attachActivity -> activity = " + activity);
		mActivity = activity;
		if (mActivity != null) {
			Log.d(TAG, "attachActivity -> mPendingWarningType = " + mPendingResultCode);
			if (mPendingResultCode != RESULT_CODE_OK) {
				mActivity.updateResult(mPendingResultCode);
				mPendingResultCode = RESULT_CODE_OK;
			}
		}
	}

	public void notifyActivity(int type) {
		if (mActivity != null) {
			mActivity.updateResult(type);
		} else {
			mPendingResultCode = type;
		}
	}

      public void notifyActivityShowDialog(int type) {
          if (mActivity != null) {
              mActivity.showStatusProgressDialog(type);
          } 
      }

	public void scheduleUpdate(int action, boolean useMobileNetwork) {
		Log.d(TAG, "scheduleUpdate() -> action = " + action);

        if (useMobileNetwork) {
            if (action == UPDATE_ACTION_CHECK) {
                notifyActivityShowDialog(UPDATE_ACTION_CHECK);
            } else if (action == UPDATE_ACTION_UPDATE) {
                notifyActivityShowDialog(UPDATE_ACTION_UPDATE);
            }
        }

		boolean wlanAutoUpdate = SPUtils.instance(mContext)
				.getBooleanValue(Constant.SHARED_PREFERENCES_KEY_AUTO_UPDATE_DATABASE, true);
		if (action == UPDATE_ACTION_AUTO && !wlanAutoUpdate) {
			return;
		}
		int resultCode = RESULT_CODE_OK;
		if (!NetUtils.isOnline(mContext)) {
			resultCode = RESULT_CODE_NO_NETWORK_CONNECTED;
		}
		if (NetUtils.isMobileOnline(mContext) && !useMobileNetwork) {
			if (action == UPDATE_ACTION_CHECK) {
				resultCode = RESULT_CODE_CHECK_MOBILE_DATA_CONNECTED;
			} else if (action == UPDATE_ACTION_UPDATE) {
				resultCode = RESULT_CODE_UPDATE_MOBILE_DATA_CONNECTED;
			} else {
				resultCode = RESULT_CODE_ERROR;
			}
		}
		Log.d(TAG, "scheduleUpdate() -> resultCode = " + resultCode);
		if (resultCode != RESULT_CODE_OK) {
			if (action != UPDATE_ACTION_AUTO) {
				notifyActivity(resultCode);
			}
			return;
		}

		doUpdateAction(action);
	}

	private void doUpdateAction(int action) {
		Intent checkIntent = new Intent(mContext, UpdateService.class);
		checkIntent.setAction(UpdateService.ACTION_UPDATE);
		checkIntent.putExtra(UpdateService.EXTRA_UPDATE_ACTION, action);
		mContext.startService(checkIntent);
		Log.d(TAG, "doUpdateAction() -> action = " + action);
	}
}
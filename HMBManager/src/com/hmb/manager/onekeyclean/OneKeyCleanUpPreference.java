package com.hmb.manager.onekeyclean;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import hb.preference.Preference;
import com.hmb.manager.R;
import com.hmb.manager.utils.ManagerUtils;
import com.hmb.manager.widget.HMBProgressBar;

public class OneKeyCleanUpPreference extends Preference {

	private HMBProgressBar mProgressBar;

	private Context mContext;

	public static final int INITIAL_STATUS = 0;
	public static final int SCANNING_STATUS = 1;
	public static final int SCAN_DONE_STATUS = 2;

	private  int mScanStatus = INITIAL_STATUS;
	private int mScore = 100;
	private static final String TAG = "OneKeyCleanUpPreference";

	public OneKeyCleanUpPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		setLayoutResource(R.layout.preference_onkeycleanup);
	}

	@Override
	protected void onBindView(View view) {

		super.onBindView(view);
		mProgressBar = (HMBProgressBar) view.findViewById(R.id.onekey_progressBar);
		mProgressBar.setProgressPaintColor(mContext.getResources().getColor(R.color.progressbar_blue));
		switch (mScanStatus) {
		case INITIAL_STATUS:
			mProgressBar.setTextPaintMsg("");
			mProgressBar.setShowNumberText(false);
			mProgressBar.setTitleText(mContext.getString(R.string.onekey_cleanspeed_ing));
			mProgressBar.setProgressValue(100, true);
			break;
		case SCANNING_STATUS:
			mProgressBar.setTextPaintMsg(mContext.getString(R.string.onekey_rubblish_cleaning));
			mProgressBar.setProgressValue(100, true);
			break;
		case SCAN_DONE_STATUS:
			mScore = ManagerUtils.getPhoneScore(mContext);
			String barTitle = null;
			Log.d(TAG, "------SCAN_DONE_STATUS------"+mScore);
			if (mScore >= 80) {
				mProgressBar.setProgressPaintColor(mContext.getResources().getColor(R.color.progressbar_blue));
				mProgressBar.setTitleColor(Color.parseColor("#B2000000"));
				barTitle = mContext.getString(R.string.phone_status_1);
			} else if (mScore < 80 && mScore >= 60) {
				mProgressBar.setProgressPaintColor(mContext.getResources().getColor(R.color.progressbar_yellow));
				mProgressBar.setTitleColor(Color.parseColor("#B2000000"));
				barTitle = mContext.getString(R.string.phone_status_2);
			} else if (mScore < 60) {
				mProgressBar.setProgressPaintColor(mContext.getResources().getColor(R.color.progressbar_red));
				mProgressBar.setTitleColor(mContext.getResources().getColor(R.color.progressbar_red));
				barTitle = mContext.getString(R.string.phone_status_3);
			}
			mProgressBar.setShowNumberText(true);
			mProgressBar.setTitleSize(150);
			mProgressBar.setProgressValue(mScore, false);
			mProgressBar.setTextPaintMsg(barTitle);
			break;
		}
	}

	public int getmScanStatus() {
		return mScanStatus;
	}

	public void setmScanStatus(int mScanStatus) {
		this.mScanStatus = mScanStatus;
		Log.d(TAG, "------setmScanStatus------"+mScanStatus);
		notifyChanged();
	}

}

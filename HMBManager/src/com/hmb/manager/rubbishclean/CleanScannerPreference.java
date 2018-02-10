package com.hmb.manager.rubbishclean;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.BaseExpandableListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hmb.manager.R;
import com.hmb.manager.bean.StorageSize;
import com.hmb.manager.percent.support.PercentLayoutHelper;
import com.hmb.manager.percent.support.PercentRelativeLayout;
import com.hmb.manager.utils.ManagerUtils;
import com.hmb.manager.utils.TransUtils;
import com.hmb.manager.widget.textconter.CounterView;
import com.hmb.manager.widget.textcounter.formatters.DecimalFormatter;

import hb.preference.Preference;

public class CleanScannerPreference extends Preference {

	private String mScanDetail = "";
	private boolean isScanning = false;
	private CounterView textCounter;
	private TextView mUnitTextview = null;
	private TextView mTotalText = null;
	private TextView mUsedM = null;
	private TextView mCleanupM = null;
	private TextView mSpaceM = null;
	private long mRubblishTotalSize = -1;
	private long mAppCahceSize = -1;
	private Context mContext = null;
	private long usedMem = 0;
	private long totalMem = 0;
	private static final String TAG = "CleanScannerPreference";
	private final int MSG_REFRESH_CONTER = 0x01;
	private ProgressBar progressBar = null;
	public CleanScannerPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		setLayoutResource(R.layout.preference_cleanscan);
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		TextView tvScanDetail = (TextView) view.findViewById(R.id.scanTextConter);
		textCounter = (CounterView) view.findViewById(R.id.textCounter);
		mUnitTextview = (TextView) view.findViewById(R.id.sufix);
		progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
		mTotalText = (TextView) view.findViewById(R.id.total_text);
		usedMem = ManagerUtils.getUsedInternalMemorySize(mContext);
		totalMem = ManagerUtils.getFlashSize(true);
		mTotalText.setText(totalTextHandler());
		if (mRubblishTotalSize != -1 && mAppCahceSize != -1) {
			mUIHandler.removeMessages(MSG_REFRESH_CONTER);
			Message msg = mUIHandler.obtainMessage(MSG_REFRESH_CONTER);
			mUIHandler.sendMessage(msg);
			setCleanUpViewPercent();
		}
		if (isScanning) {
			tvScanDetail.setVisibility(View.VISIBLE);
			tvScanDetail.setText(mScanDetail);
		} else {
			tvScanDetail.setText(mScanDetail);
			//tvScanDetail.setVisibility(View.GONE);
		}
	}
	
	private Handler mUIHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_REFRESH_CONTER:
				startConter();
				break;
		}

	}
    };

	public void updateScanDetail(String detail) {
		mScanDetail = detail;
		isScanning = true;
		notifyChanged();
	}

	public void updataScanResult(String detail) {
		mScanDetail = detail;
		isScanning = false;
		notifyChanged();
	}

	private void startConter() {

		StorageSize mStorageSize = TransUtils.convertStorageSize(mRubblishTotalSize + mAppCahceSize);
		Log.d(TAG, mStorageSize.suffix + "=============================" + mStorageSize.value);
		//textCounter.setText(String.valueOf(TransUtils.getFloatValue(mStorageSize.value, 2)));
		if(mStorageSize.value>0){
		textCounter.setAutoFormat(false);
		textCounter.setFormatter(new DecimalFormatter());
		textCounter.setAutoStart(false);
		textCounter.setStartValue(0.00f);
		textCounter.setEndValue(mStorageSize.value);
		textCounter.setIncrement(mStorageSize.value/8); 
		textCounter.setTimeInterval(50); 
		textCounter.start();
		mUnitTextview.setText(mStorageSize.suffix);
		}
	}
	

	public void setmRubblishTotalSize(long mRubblishTotalSize) {
		Log.d(TAG, "setmRubblishTotalSize = "+mRubblishTotalSize);
		this.mRubblishTotalSize = mRubblishTotalSize;
		if(mRubblishTotalSize>=0)
		notifyChanged();
	}

	private String totalTextHandler() {
		StringBuffer sb = new StringBuffer();
		sb.append(TransUtils.transformShortType(usedMem, true));
		sb.append("/");
		sb.append(TransUtils.transformShortType(totalMem, true));
		return sb.toString();
	}

	private void setMainViewPercent() {
		PercentRelativeLayout.LayoutParams params = null;
		PercentLayoutHelper.PercentLayoutInfo info = null;
		double usedD = 0;
		int secondPro=0;
		usedD = (double) usedMem / totalMem;
		Log.d(TAG, usedD + "-----setView---" + usedMem + "   Percent-------" + TransUtils.getFloatValue(usedD, 3));
		secondPro=(int)(Float.valueOf(TransUtils.getFloatValue(usedD, 3))*100);
        progressBar.setProgress(0);
        progressBar.setSecondaryProgress(0);
		Thread t=new Thread(new ProgressBarThread(0,secondPro));
		t.start();
	}

	private void setCleanUpViewPercent() {
		PercentRelativeLayout.LayoutParams params = null;
		PercentLayoutHelper.PercentLayoutInfo info = null;
		double usedD = 0;
		int progress=0;
		int secondPro=0;
		usedD = (double) usedMem / totalMem;
		Log.d(TAG, usedD + "-----setView---" + usedMem + "   Percent-------" + TransUtils.getFloatValue(usedD, 3));
		secondPro=(int)(Float.valueOf(TransUtils.getFloatValue(usedD, 3))*100);
		double cleanUpM = 0;
		cleanUpM = (double) (mRubblishTotalSize + mAppCahceSize) / totalMem;
		//float cleanUpPercent = 0f;
		if (cleanUpM < 0.01&&cleanUpM>0) {
			progress = 1+secondPro;
		}else{
			progress=(int)(cleanUpM*100)+secondPro;
		}
		Log.d(TAG, progress+"=progress------------secondPro="+secondPro);
        progressBar.setProgress(0);
        progressBar.setSecondaryProgress(0);
		Thread t=new Thread(new ProgressBarThread(progress*2,secondPro*2));
		t.start();
	}

	public long getmAppCahceSize() {
		return mAppCahceSize;
	}

	public void setmAppCahceSize(long mAppCahceSize) {
		this.mAppCahceSize = mAppCahceSize;
		Log.d(TAG, "setmAppCahceSize="+mAppCahceSize);
		if(mAppCahceSize>=0)
		notifyChanged();
	}
	
    class ProgressBarThread implements Runnable{
		int progress=0;
		int secondPro=0;
    	
    	public ProgressBarThread(int p,int s){
    		this.progress=p;
    		this.secondPro=s;
    	}
    	
		@Override
		public void run() {
			int sum = 0;
			int secsum=0;
//			if(secondPro+2>progress){
//				progress=progress+1;
//			}
            while (sum<=progress||secsum<=secondPro) {
            	if(sum<=progress)
            	progressBar.setProgress(sum);
            	if(secsum<=secondPro)
            	progressBar.setSecondaryProgress(secsum);
                sum += 2;
                secsum+=1;
                try {
                    Thread.sleep(8);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
		}
    	
    };

}

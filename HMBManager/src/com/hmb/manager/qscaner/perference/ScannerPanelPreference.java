package com.hmb.manager.qscaner.perference;


import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.hmb.manager.R;
import com.hmb.manager.qscaner.QScannerSettingActivity;
import com.hmb.manager.widget.HMBProgressBar;

import hb.preference.Preference;

/**
 * Scanner Panel
 */
public class ScannerPanelPreference extends Preference {

    public static final int INITIAL_STATUS = 0;
    public static final int DONE_STATUS = 1;

    private static final int SAFE_COLOR = Color.parseColor("#71B7C8");
    private static final int RISK_COLOR = Color.parseColor("#FFF5515F");

    private Context mContext;
    private TextView tvScanDetail;

    private int riskNum = 0;
    private int mScanStatus = INITIAL_STATUS;
    private int mScanMode = QScannerSettingActivity.QUICK_SCAN_MODE;

    public ScannerPanelPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setEnabled(false);
        setLayoutResource(R.layout.preference_scanner_panel);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        HMBProgressBar progressBar = (HMBProgressBar)view.findViewById(R.id.progressBar);
        tvScanDetail = (TextView) view.findViewById(R.id.tv_scanDetail);
        switch (mScanStatus) {
            case INITIAL_STATUS:
                if (mScanMode == QScannerSettingActivity.QUICK_SCAN_MODE) {
                    progressBar.setTitleText(mContext.getString(R.string.safety_quick_scanning));
                } else {
                    progressBar.setTitleText(mContext.getString(R.string.safety_full_scanning));
                }
                progressBar.setTextPaintMsg("");
                progressBar.setProgressValue(100, true);
                break;
            case DONE_STATUS:
                if (riskNum > 0) {
                    progressBar.setProgressPaintColor(RISK_COLOR);
                    progressBar.setTitleText(mContext.getResources().
                            getString(R.string.safety_result_risk, riskNum));
                    progressBar.setTextPaintMsg(mContext.getString(R.string.safety_risk_tips));
                } else {
                    progressBar.setProgressPaintColor(SAFE_COLOR);
                    progressBar.setTitleText(mContext.getString(R.string.safety_result_safe));
                    progressBar.setTextPaintMsg("");
                }
                progressBar.setProgressValue(100, false);
                break;
        }
    }

    public void updateScannerDetail(String detail) {
        if (tvScanDetail != null) {
            tvScanDetail.setText(detail);
        }
    }

    public void setScanMode(int mode) {
        mScanMode = mode;
    }

    public void updateScannerView(int status, int num) {
        mScanStatus = status;
        riskNum = num;
        notifyChanged();
    }
}
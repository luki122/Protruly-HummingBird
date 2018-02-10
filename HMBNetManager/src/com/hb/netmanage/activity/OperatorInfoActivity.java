package com.hb.netmanage.activity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;

import com.hb.netmanage.DataCorrect;
import com.hb.netmanage.receiver.SimStateReceiver;
import com.hb.netmanage.utils.PreferenceUtil;
import com.hb.netmanage.utils.ToolsUtil;
import com.hb.netmanage.view.DataplanPreference;
import com.hb.netmanage.R;

import android.app.hb.TMSManager;
import android.app.hb.CodeNameInfo;
import android.app.hb.TMSManager;
import android.view.View;
import android.widget.SpinnerPopupDialog;

import java.util.HashMap;
import java.util.List;

import hb.preference.Preference;
import hb.widget.toolbar.Toolbar;

/**
 * 运营商信息设置
 */
public class OperatorInfoActivity extends BasePreferenceActivity implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private DataplanPreference mPreProvince;
    private DataplanPreference mPreCity;
    private DataplanPreference mPreOperator;
    private DataplanPreference mPreDataplanType;
    /**
     * 点击view的item
     */
    private Preference mClickPre;
    /**
     * 当前选择的sim的IMS号
     */
    private String mSelectedSimIMSI;
    private List<CodeNameInfo> mAllProvinces;
    private List<CodeNameInfo> mCities;
    private List<CodeNameInfo> mBrands;
    private String[] mTempArray = null;
    private String[] mAllProvincesArray = null;
    private String[] mCitysArray = null;
    private String[] mBrandsArray = null;
    private HashMap< String, String> mProvincesNameCodeMap = new HashMap<String, String>();
    private HashMap< String, String> mNameCodeMap = new HashMap<String, String>();

    /**
     * 当前卡索引
     */
    private int mSelectedIndex;
    private int mClickIndex;
    private int mCurrentState;
    private TMSManager mTmsManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.activity_operator_info);
        try {
            mTmsManager = DataCorrect.getInstance().getmTmsManager();
            if (null != mTmsManager) {
                mAllProvinces = mTmsManager.getAllProvinces();
            }
            if (mAllProvinces != null) {
                mAllProvincesArray = new String[mAllProvinces.size()];
                for (int i = 0; i < mAllProvinces.size(); i++) {
                    CodeNameInfo codeNameInfo = mAllProvinces.get(i);
                    mAllProvincesArray[i] = codeNameInfo.mName;
                    mProvincesNameCodeMap.put(codeNameInfo.mName, codeNameInfo.mCode);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        initSimInfo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //sim基本信息设置完整检查
        PreferenceUtil.putBoolean(this, mSelectedSimIMSI, PreferenceUtil.SIM_BASEINFO_KEY, isSetDataPlan());
    }

    @Override
    public void setSimStateChangeListener(int simState) {
        if (simState == SimStateReceiver.SIM_INVALID) {
            setPreferenceState(false);
        } else {
            setPreferenceState(true);
        }
        initSimInfo();
    }

    private void setPreferenceState(boolean state) {
        if (mPreProvince == null || mPreCity == null || mPreOperator == null || mPreDataplanType == null) {
            return;
        }
        mPreProvince.setEnabled(state);
        mPreCity.setEnabled(state);
        mPreOperator.setEnabled(state);
        mPreDataplanType.setEnabled(state);
    }

    /**
     * 初始化数据
     */
    private void initSimInfo() {
        mSelectedIndex = getIntent().getIntExtra("CURRENT_INDEX", 0);
        if (mSelectedIndex == 0) {
            //卡1
            mSelectedSimIMSI = PreferenceUtil.getString(this, PreferenceUtil.SIM_1, PreferenceUtil.IMSI_KEY, null);
        } else if (mSelectedIndex == 1) {
            //卡2
            mSelectedSimIMSI = PreferenceUtil.getString(this, PreferenceUtil.SIM_2, PreferenceUtil.IMSI_KEY, null);
        }
        mCurrentState = ToolsUtil.getCurrentNetSimSubInfo(this);
        if (mCurrentState == -1) {
            mSelectedIndex = mCurrentState;
            mSelectedSimIMSI = null;
        }
        initView();
    }

    private void initView() {
        Toolbar toolbar = getToolbar();
        toolbar.setTitle(getString(R.string.operator_set));
        toolbar.setElevation(1);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                OperatorInfoActivity.this.finish();
            }
        });
        //省份
        String province = PreferenceUtil.getString(this, mSelectedSimIMSI, PreferenceUtil.PROVINCE_KEY, getString(R.string.un_set));
        mPreProvince = (DataplanPreference) findPreference("sim_province");
        mPreProvince.setItemTitle(getString(R.string.province));
        mPreProvince.setSubContent(province);
        mPreProvince.setOnPreferenceClickListener(this);
        //所在城市
        String city = PreferenceUtil.getString(this, mSelectedSimIMSI, PreferenceUtil.CITY_KEY, getString(R.string.un_set));
        mPreCity = (DataplanPreference) findPreference("sim_city");
        mPreCity.setItemTitle(getString(R.string.city));
        mPreCity.setSubContent(city);
        mPreCity.setOnPreferenceClickListener(this);

        //运营商
        mPreOperator = (DataplanPreference) findPreference("sim_operator");
        mPreOperator.setItemTitle(getString(R.string.operators));
        mPreOperator.dismissNextIcon(true);
        String simOperator = ToolsUtil.getSimOperator(this, mSelectedSimIMSI);
        mPreOperator.setSubContent(simOperator);
        String simCode = "";
        if (TextUtils.equals(simOperator, getString(R.string.china_mobile))) {
            simCode = "CMCC";
        } else if (TextUtils.equals(simOperator, getString(R.string.china_unicom))) {
            simCode = "UNICOM";
        } else if (TextUtils.equals(simOperator, getString(R.string.china_telecom))) {
            simCode = "TELECOM";
        }
        try {
            //初始化时用户运营商默认获得,用户可以不用选择,这时要做个匹配来获得套餐类别
            List<CodeNameInfo> carries = null;
            if (null != mTmsManager) {
                carries = mTmsManager.getCarries();
            }
            if (null != carries) {
                for (int i = 0; i < carries.size(); i++) {
                    CodeNameInfo codeNameInfo = carries.get(i);
                    if (codeNameInfo.mCode.equalsIgnoreCase(simCode)) {
                        PreferenceUtil.putString(OperatorInfoActivity.this, mSelectedSimIMSI, PreferenceUtil.OPERATOR_KEY, codeNameInfo.mName);
                        PreferenceUtil.putString(OperatorInfoActivity.this, mSelectedSimIMSI, PreferenceUtil.OPERATOR_CODE_KEY, codeNameInfo.mCode);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //套餐类型
        String dataPlayType = PreferenceUtil.getString(this, mSelectedSimIMSI, PreferenceUtil.DATAPLAN_TYPE_KEY, getString(R.string.un_set));
        mPreDataplanType = (DataplanPreference) findPreference("sim_dataplan_type");
        mPreDataplanType.setItemTitle(getString(R.string.dataplan_type));
        mPreDataplanType.setSubContent(dataPlayType);
        mPreDataplanType.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        mClickPre = preference;
        if (mAllProvincesArray != null) {
            if (preference == mPreProvince) {
                mClickIndex = 0;
                String province = PreferenceUtil.getString(OperatorInfoActivity.this, mSelectedSimIMSI, PreferenceUtil.PROVINCE_KEY, null);
                for (int i = 0; i < mAllProvincesArray.length; i++) {
                    if (mAllProvincesArray[i].equals(province)) {
                        mClickIndex = i;
                        break;
                    }
                }
                showDataType(getString(R.string.select) + getString(R.string.province), mAllProvincesArray);
            } else if (preference == mPreCity) {
                showSelectDialog(mPreCity);
            } else if (preference == mPreDataplanType) {
                showSelectDialog(mPreDataplanType);
            }
        }
        return true;
    }

    /**
     * 显示数据类型
     */
    private void showDataType(String title, String[] valuesArray) {
        mTempArray = valuesArray;
        SpinnerPopupDialog spinnerPopupDialog = new SpinnerPopupDialog(this);
        spinnerPopupDialog.setNegativeButton(new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        spinnerPopupDialog.setSingleChoiceItems(valuesArray, mClickIndex,	 new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                mClickIndex = which;
                String value = mTempArray[which];
                String code = null;
                if (mClickPre == mPreProvince) {
                    code = mProvincesNameCodeMap.get(value);
                    PreferenceUtil.putString(OperatorInfoActivity.this, mSelectedSimIMSI, PreferenceUtil.PROVINCE_KEY, value);
                    PreferenceUtil.putString(OperatorInfoActivity.this, mSelectedSimIMSI, PreferenceUtil.PROVINCE_CODE_KEY, code);
                    mPreProvince.setSubContent(value);
                    if (null != mTmsManager) {
                        mCities = mTmsManager.getCities(code);
                    }
                    if (null != mCities && mCities.size() > 0) {
                        mPreCity.setSubContent(mCities.get(0).mName);
                        PreferenceUtil.putString(OperatorInfoActivity.this, mSelectedSimIMSI, PreferenceUtil.CITY_KEY, mCities.get(0).mName);
                        PreferenceUtil.putString(OperatorInfoActivity.this, mSelectedSimIMSI, PreferenceUtil.CITY_CODE_KEY, mCities.get(0).mCode);
                        mClickPre = mPreCity;
                        showSelectDialog(mPreCity);
                    }
                } else if (mClickPre == mPreCity) {
                    code = mNameCodeMap.get(value);
                    PreferenceUtil.putString(OperatorInfoActivity.this, mSelectedSimIMSI, PreferenceUtil.CITY_KEY, value);
                    PreferenceUtil.putString(OperatorInfoActivity.this, mSelectedSimIMSI, PreferenceUtil.CITY_CODE_KEY, code);
                    mPreCity.setSubContent(value);
                    String operatorCode = PreferenceUtil.getString(OperatorInfoActivity.this, mSelectedSimIMSI, PreferenceUtil.OPERATOR_CODE_KEY, null);
                    if (null != mTmsManager) {
                        mBrands = mTmsManager.getBrands(operatorCode);
                    }
                    if (null != mBrands && mBrands.size() > 0) {
                        mPreDataplanType.setSubContent(mBrands.get(0).mName);
                        PreferenceUtil.putString(OperatorInfoActivity.this, mSelectedSimIMSI, PreferenceUtil.DATAPLAN_TYPE_KEY, mBrands.get(0).mName);
                        PreferenceUtil.putString(OperatorInfoActivity.this, mSelectedSimIMSI, PreferenceUtil.DATAPLAN_TYPE_CODE_KEY, mBrands.get(0).mCode);
                        mClickPre = mPreDataplanType;
                        if (mBrands.size() > 1) {
                            showSelectDialog(mPreDataplanType);
                        }
                    }
                } else if (mClickPre == mPreDataplanType) {
                    code = mNameCodeMap.get(value);
                    PreferenceUtil.putString(OperatorInfoActivity.this, mSelectedSimIMSI, PreferenceUtil.DATAPLAN_TYPE_KEY, value);
                    PreferenceUtil.putString(OperatorInfoActivity.this, mSelectedSimIMSI, PreferenceUtil.DATAPLAN_TYPE_CODE_KEY, code);
                    mPreDataplanType.setSubContent(value);

                }
                dialog.dismiss();
            }
        });
        spinnerPopupDialog.setCanceledOnTouchOutside(true);
        spinnerPopupDialog.show();
        spinnerPopupDialog.setTitle(title);
    }

    private void showSelectDialog(Preference preference) {
        if (preference == mPreCity) {
            try {
                String code = PreferenceUtil.getString(OperatorInfoActivity.this, mSelectedSimIMSI, PreferenceUtil.PROVINCE_CODE_KEY, null);
                if (TextUtils.isEmpty(code)) {
                    return ;
                }
                mNameCodeMap.clear();
                if (null == mCities || mCities.size() == 0) {
                    if (null != mTmsManager) {
                        mCities = mTmsManager.getCities(code);
                    }
                }
                if (mCities == null) {
                    return;
                }
                mCitysArray = new String[mCities.size()];
                String cityCode = PreferenceUtil.getString(OperatorInfoActivity.this, mSelectedSimIMSI, PreferenceUtil.CITY_CODE_KEY, null);
                mClickIndex = 0;
                for (int i = 0; i < mCities.size(); i++) {
                    CodeNameInfo codeNameInfo = mCities.get(i);
                    mCitysArray[i] = codeNameInfo.mName;
                    mNameCodeMap.put(codeNameInfo.mName, codeNameInfo.mCode);
                    if (codeNameInfo.mCode.equals(cityCode)) {
                        mClickIndex = i;
                    }
                }
                showDataType(getString(R.string.select) + getString(R.string.city), mCitysArray);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (preference == mPreDataplanType) {
            mNameCodeMap.clear();
            try {
                //获得套餐类别
                if (null == mBrands) {
                    String code = PreferenceUtil.getString(OperatorInfoActivity.this, mSelectedSimIMSI, PreferenceUtil.OPERATOR_CODE_KEY, null);
                    if (null != mTmsManager) {
                        mBrands = mTmsManager.getBrands(code);
                    }
                }
                if (mBrands == null) {
                    return;
                }
                mClickIndex = 0;
                String dataPlanCode = PreferenceUtil.getString(OperatorInfoActivity.this, mSelectedSimIMSI, PreferenceUtil.DATAPLAN_TYPE_CODE_KEY, null);
                mBrandsArray = new String[mBrands.size()];
                for (int i = 0; i < mBrands.size(); i++) {
                    CodeNameInfo codeNameInfo = mBrands.get(i);
                    mBrandsArray[i] = codeNameInfo.mName;
                    mNameCodeMap.put(codeNameInfo.mName, codeNameInfo.mCode);
                    if (codeNameInfo.mCode.equals(dataPlanCode)) {
                        mClickIndex = i;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            showDataType(getString(R.string.select) + getString(R.string.dataplan_type), mBrandsArray);
        }
    }

    private boolean isSetDataPlan() {
        String province = PreferenceUtil.getString(this, mSelectedSimIMSI, PreferenceUtil.PROVINCE_KEY, getString(R.string.un_set));
        String city = PreferenceUtil.getString(this, mSelectedSimIMSI, PreferenceUtil.CITY_KEY, getString(R.string.un_set));
        String dataPlanType = PreferenceUtil.getString(this, mSelectedSimIMSI, PreferenceUtil.DATAPLAN_TYPE_KEY, getString(R.string.un_set));
        String operator = PreferenceUtil.getString(this, mSelectedSimIMSI, PreferenceUtil.OPERATOR_KEY, getString(R.string.un_set));
        //sim卡基本信息设置
        if (province.equals(getString(R.string.un_set)) || city.equals(getString(R.string.un_set))
                ||dataPlanType. equals(getString(R.string.un_set)) || TextUtils.equals(operator, getString(R.string.un_set))) {
            return false;
        }
        return true;
    }
}

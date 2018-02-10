package com.mediatek.phone.ext;

import android.content.Context;
import hb.preference.Preference;
import hb.preference.PreferenceActivity;
import hb.preference.PreferenceFragment;

import android.util.Log;

public class DefaultCallFeaturesSettingExt implements ICallFeaturesSettingExt {

    public static final int RESUME = 0;
    public static final int PAUSE = 1;
    public static final int DESTROY = 2;

    /**
     * called when init the preference (onCreate) single card
     * plugin can customize the activity, like add/remove preference screen
     * plugin should check the activiyt class name, to distinct the caller, avoid do wrong work.
     * if (TextUtils.equals(getClass().getSimpleName(), "CallFeaturesSetting") {}
     *
     * @param activity the PreferenceActivity instance
     * @return
     */
    @Override
    public void initOtherCallFeaturesSetting(PreferenceActivity activity) {
    }

    /**
     * called when init the preference (onCreate)
     * plugin can customize the fragment, like add/remove preference screen
     * plugin should check the fragment class name, to distinct the caller, avoid do wrong work.
     * if (TextUtils.equals(getClass().getSimpleName(), "CallFeaturesSetting") {}
     *
     * @param fragment the PreferenceFragment instance
     */
    @Override
    public void initOtherCallFeaturesSetting(PreferenceFragment fragment) {
    }

    /**
     * Init the call forward option item for C2K.
     * @param activity the activity of the setting preference.
     * @param subId the subId of the setting item.
     */
    @Override
    public void initCdmaCallForwardOptionsActivity(PreferenceActivity activity, int subId) {
    }

    /**
     * Need to fire intent to reset IMS PDN connection.
     * @param context the context of the setting preference.
     * @param msg the message to be sent when SS completed.
     * @return
     */
    @Override
    public void resetImsPdnOverSSComplete(Context context, int msg) {
        Log.d("DefaultCallFeaturesSettingExt","resetImsPdnOverSSComplete");
    }

    /**
     * For WWWOP, Whether need to show open mobile data dialog or not.
     * @param context the context of the setting preference.
     * @param subId the sudId of the setting item.
     * @return true if need to show it.
     */
    @Override
    public boolean needShowOpenMobileDataDialog(Context context, int subId) {
        return true;
    }

    /**
     * handle preference status when error happens
     * @param preference the preference which error happens on.
     */
    @Override
    public void onError(Preference preference) {
        Log.d("DefaultCallFeaturesSettingExt", "default onError");
    }

    @Override
    /** Initializes  various parameters required.
     * Used in CallFeatureSettings
     * @param pa PreferenceActivity
     * @param wfcPreference wfc preference
     * @return
     */
    public void initPlugin(PreferenceActivity pa, Preference wfcPreference) {
    }

    @Override
    /** Called on events like onResume/onPause etc from WfcSettings.
     * @param event resume/puase etc.
     * @return
     */
    public void onCallFeatureSettingsEvent(int event) {
    }

    /** get operator specific customized summary for WFC button.
     * Used in CallFeatureSettings
     * @param context context
     * @param defaultSummaryResId default summary res id
     * @return summary string to be displayed
     */
    @Override
    public String getWfcSummary(Context context, int defaultSummaryResId) {
        return context.getResources().getString(defaultSummaryResId);
    }

    /** The switch of VT CF.
     */
    @Override
    public boolean openVtCf() {
        Log.d("DefaultCallFeaturesSettingExt", "openVtCf false");
        return false;
    }

    /** The switch of VT CB.
     */
    @Override
    public boolean openVtCb() {
        Log.d("DefaultCallFeaturesSettingExt", "openVtCB false");
        return false;
    }

}

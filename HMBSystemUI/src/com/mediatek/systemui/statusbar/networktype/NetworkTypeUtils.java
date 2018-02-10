
package com.mediatek.systemui.statusbar.networktype;

import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkControllerImpl.Config;

import java.util.HashMap;
import java.util.Map;

/**
 * An utility class to access network type.
 */
public class NetworkTypeUtils {
    private static final String TAG = "NetworkTypeUtils";

    public static final int[] VOLTEICON = {R.drawable.stat_sys_volte1,
                                           R.drawable.stat_sys_volte2};

    //ShenQianfeng modify begin
    //Original:
    /*
    static final Map<Integer, Integer> sNetworkTypeIcons = new HashMap<Integer, Integer>() {
        {
            // For CDMA 3G
            put(TelephonyManager.NETWORK_TYPE_EVDO_0, R.drawable.stat_sys_network_type_3g);
            put(TelephonyManager.NETWORK_TYPE_EVDO_A, R.drawable.stat_sys_network_type_3g);
            put(TelephonyManager.NETWORK_TYPE_EVDO_B, R.drawable.stat_sys_network_type_3g);
            put(TelephonyManager.NETWORK_TYPE_EHRPD, R.drawable.stat_sys_network_type_3g);
            // For CDMA 1x
            put(TelephonyManager.NETWORK_TYPE_CDMA, R.drawable.stat_sys_network_type_1x);
            put(TelephonyManager.NETWORK_TYPE_1xRTT, R.drawable.stat_sys_network_type_1x);
            // Edge
            put(TelephonyManager.NETWORK_TYPE_EDGE, R.drawable.stat_sys_network_type_e);
            // 3G
            put(TelephonyManager.NETWORK_TYPE_UMTS, R.drawable.stat_sys_network_type_3g);
            // For 4G
            put(TelephonyManager.NETWORK_TYPE_LTE, R.drawable.stat_sys_network_type_4g);
            // 3G
            put(TelephonyManager.NETWORK_TYPE_HSDPA, R.drawable.stat_sys_network_type_3g);
            put(TelephonyManager.NETWORK_TYPE_HSUPA, R.drawable.stat_sys_network_type_3g);
            put(TelephonyManager.NETWORK_TYPE_HSPA, R.drawable.stat_sys_network_type_3g);
            put(TelephonyManager.NETWORK_TYPE_HSPAP, R.drawable.stat_sys_network_type_3g);
        }
    };
    */
    //Modify to:
    static final Map<Integer, Integer> sNetworkTypeIcons = new HashMap<Integer, Integer>() {
        {
            // For CDMA 3G
            put(TelephonyManager.NETWORK_TYPE_EVDO_0, R.drawable.hmb_stat_sys_data_fully_connected_3g);
            put(TelephonyManager.NETWORK_TYPE_EVDO_A, R.drawable.hmb_stat_sys_data_fully_connected_3g);
            put(TelephonyManager.NETWORK_TYPE_EVDO_B, R.drawable.hmb_stat_sys_data_fully_connected_3g);
            put(TelephonyManager.NETWORK_TYPE_EHRPD, R.drawable.hmb_stat_sys_data_fully_connected_3g);
            // For CDMA 1x
            put(TelephonyManager.NETWORK_TYPE_CDMA, R.drawable.hmb_stat_sys_data_fully_connected_1x);
            put(TelephonyManager.NETWORK_TYPE_1xRTT, R.drawable.hmb_stat_sys_data_fully_connected_1x);
            // Edge
            put(TelephonyManager.NETWORK_TYPE_EDGE, R.drawable.hmb_stat_sys_data_fully_connected_e);
            // 3G
            put(TelephonyManager.NETWORK_TYPE_UMTS, R.drawable.hmb_stat_sys_data_fully_connected_3g);
            // For 4G
            put(TelephonyManager.NETWORK_TYPE_LTE, R.drawable.hmb_stat_sys_data_fully_connected_4g);
            put(TelephonyManager.NETWORK_TYPE_LTEA, R.drawable.hmb_stat_sys_data_fully_connected_4ga);
            // 3G
            put(TelephonyManager.NETWORK_TYPE_HSDPA, R.drawable.hmb_stat_sys_data_fully_connected_3g);
            put(TelephonyManager.NETWORK_TYPE_HSUPA, R.drawable.hmb_stat_sys_data_fully_connected_3g);
            put(TelephonyManager.NETWORK_TYPE_HSPA, R.drawable.hmb_stat_sys_data_fully_connected_3g);
            put(TelephonyManager.NETWORK_TYPE_HSPAP, R.drawable.hmb_stat_sys_data_fully_connected_3g);
        }
    };
    //ShenQianfeng modify end
    


    /**
     * Map the network type into the related icons.
     * @param serviceState ServiceState to get current network type.
     * @param config Config passed in.
     * @param hasService true for in service.
     * @return Network type's icon.
     */
    public static int getNetworkTypeIcon(ServiceState serviceState, Config config,
            boolean hasService) {
        if (!hasService) {
            // Not in service, no network type.
            return 0;
        }
        int tempNetworkType = getNetworkType(serviceState);

        Integer iconId = sNetworkTypeIcons.get(tempNetworkType);
        if (iconId == null) {
            //ShenQianfeng modify begin
            //Original:
            /*
            iconId = tempNetworkType == TelephonyManager.NETWORK_TYPE_UNKNOWN ? 0 :
                config.showAtLeast3G ? R.drawable.stat_sys_network_type_3g :
                                       R.drawable.stat_sys_network_type_g;
                                       */
            //Modify to:
            iconId = tempNetworkType == TelephonyManager.NETWORK_TYPE_UNKNOWN ? 0 :
                config.showAtLeast3G ? R.drawable.hmb_stat_sys_data_fully_connected_3g :
                                       R.drawable.hmb_stat_sys_data_fully_connected_g;
            //ShenQianfeng modify end

        }
        Log.d(TAG, "getNetworkTypeIcon iconId = " + iconId);
        return iconId.intValue();
    }

    private static int getNetworkType(ServiceState serviceState) {
        int type = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        if (serviceState != null) {
            type = serviceState.getDataNetworkType() != TelephonyManager.NETWORK_TYPE_UNKNOWN ?
                    serviceState.getDataNetworkType() : serviceState.getVoiceNetworkType();
            //add by chenhl start
            type= getDataNetTypeFromServiceState(type,serviceState);
            //add by chnhl end
        }
        Log.d(TAG, "getNetworkType: type=" + type);
        return type;
    }

    /// M: Support 4G+ icon" @{
    public static int getDataNetTypeFromServiceState(int srcDataNetType, ServiceState sState){
        int destDataNetType = srcDataNetType;
        if (destDataNetType == TelephonyManager.NETWORK_TYPE_LTE
               || destDataNetType == TelephonyManager.NETWORK_TYPE_LTEA) {
            if (sState != null){
                destDataNetType = (sState.getProprietaryDataRadioTechnology() == 0 ?
                    TelephonyManager.NETWORK_TYPE_LTE : TelephonyManager.NETWORK_TYPE_LTEA);
            }
        }

        Log.d(TAG, "getDataNetTypeFromServiceState:srcDataNetType = "
            + srcDataNetType + ", destDataNetType " + destDataNetType);

        return destDataNetType;
    }
    ///@}
}

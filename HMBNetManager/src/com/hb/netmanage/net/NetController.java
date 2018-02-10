package com.hb.netmanage.net;

import android.content.Context;
import android.net.NetworkPolicyManager;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.ServiceManager;

import com.hb.netmanage.utils.LogUtil;

/**
 * 网络控制
 *
 * Created by zhaolaichao on 17-4-6.
 */

public class NetController {

    private static final NetController netController = new NetController();
    public final static int WIFI = 1;
    public final static int MOBILE = 0;
    public final static String CHAIN_MOBILE = "mobile";
    public final static String CHAIN_WIFI = "wifi";

    private static INetworkManagementService mNetworkService;

    private NetController() {
    }

    public static NetController getInstance() {
        if (mNetworkService == null) {
            mNetworkService = INetworkManagementService.Stub.asInterface(
                    ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE));
        }
        return netController;
    }

    /**
     * @Configure firewall rule by uid and chain
     * @param uid
     * @param networkType
     * @param allow
     *
     *
     *  final String MOBILE = "mobile";
        final String WIFI = "wifi";

        final String rule = allow ? "allow" : "deny";
        final String chain = (networkType == 1) ? WIFI : MOBILE;

        try {
           mConnector.execute("firewall", "set_uid_fw_rule", uid, chain, rule);
        } catch (NativeDaemonConnectorException e) {
           throw e.rethrowAsParcelableException();
        }
     */
    public void setFirewallUidChainRule(Integer uid, int networkType, boolean allow) {
        synchronized(uid) {
            try {
                LogUtil.e("setFirewallUidChainRule", "uid>>" + uid + ">allow>>" + allow);
                mNetworkService.setFirewallUidChainRule(uid, networkType, allow);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * @Configure firewall rule by uid and chain
     * @param chain CHAIN_MOBILE or CHAIN_WIFI
     */
    public void clearFirewallChain(String chain) {
        try {
            mNetworkService.clearFirewallChain(chain);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }
}

package com.protruly.music.util;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;

import com.protruly.music.MusicUtils;
import com.protruly.music.R;
import hb.app.dialog.AlertDialog;
import hb.widget.Switch;
/**
 * Created by hujianwei on 17-8-30.
 */

public class FlowTips {

    private static final String WIFI_STATE_ETR = "wifi_state";

    public interface OndialogClickListener {
        public void OndialogClick();
    }

    public static boolean showPlayFlowTips(final Context context, final OndialogClickListener l) {
        if (!isWifiOpen(context) || !HBMusicUtil.isGprsNetActive(context) || !Globals.SWITCH_FOR_ONLINE_MUSIC) {
            return false;
        }
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (l != null) {
                            l.OndialogClick();
                        }
                        closeWifiState(context);
                        dialog.dismiss();
                    }
                }).setTitle(R.string.hb_flow_tips)
                .setMessage(R.string.hb_continue_play_tips).create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
        return true;
    }

    public static boolean showDownloadFlowTips(final Context context, final OndialogClickListener l) {
        if (!isWifiOpen(context) || !HBMusicUtil.isGprsNetActive(context)) {
            return false;
        }
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {}
                }).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (l != null) {
                            l.OndialogClick();
                        }
                        closeWifiState(context);
                        dialog.dismiss();
                    }
                }).setTitle(R.string.hb_flow_tips)
                .setMessage(R.string.hb_continue_download_tips).create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        return true;
    }

    public static void showWifiSwitch(final Context context) {
        View layout = LayoutInflater.from(context).inflate(R.layout.hb_net_setting, null);
        Switch switch1 = (Switch) layout.findViewById(R.id.hb_close_wifi_switch);
        switch1.setChecked(isWifiOpen(context));
        switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundbutton, boolean flag) {
                if (flag) {
                    openWifiState(context);
                } else {
                    closeWifiState(context);
                }
            }
        });
        AlertDialog dialog = new AlertDialog.Builder(context).setView(layout)
                .create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    private static void closeWifiState(Context context) {
        MusicUtils.setIntPref(context, WIFI_STATE_ETR, 0);
    }

    private static void openWifiState(Context context) {
        MusicUtils.setIntPref(context, WIFI_STATE_ETR, 1);
    }

    private static boolean isWifiOpen(Context context) {
        return MusicUtils.getIntPref(context, WIFI_STATE_ETR, 1) == 1 ? true : false;
    }
}

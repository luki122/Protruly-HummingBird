package com.hb.thememanager.utils;

import hb.app.dialog.AlertDialog;
import hb.app.dialog.ProgressDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.TextView;

import com.hb.thememanager.R;
public class DialogUtils {
	public static final int DIALOG_ID_APPLY_PROGRESS = 0;
	public static final int DIALOG_ID_MOBILE_NETWORK_CONFIM = 1;
	private Dialog mDialog;
	
	public synchronized void showDialog(Context context,int dialogId){
		showDialog(context,dialogId,null);
	}


	public synchronized void showDialog(Context context,int dialogId,DialogInterface.OnClickListener listener){
		dismissDialog();
		switch (dialogId) {
			case DIALOG_ID_APPLY_PROGRESS:
				ProgressDialog applyThemeProgressDialog = new ProgressDialog(context);
				applyThemeProgressDialog.setMessage(context.getResources().getString(R.string.msg_apply_theme));
				mDialog = applyThemeProgressDialog;
				break;
			case DIALOG_ID_MOBILE_NETWORK_CONFIM:
				TextView wizardView = new TextView(context);
				wizardView.setText(R.string.msg_open_mobile_wizard);
				AlertDialog ad = new AlertDialog.Builder(context)
						.setMessage(R.string.msg_open_mobile_network_download)
						.setPositiveButton(R.string.open_network_download,listener)
						.setNegativeButton(R.string.confirm_cancel,listener)
						.setView(wizardView)
						.create();
				mDialog = ad;
				break;

			default:
				break;
		}
		if(mDialog != null){
			if(mDialog.isShowing()){
				return;
			}
			mDialog.show();
			mDialog.setCancelable(false);
			mDialog.setCanceledOnTouchOutside(false);
		}
	}


	public synchronized void dismissDialog(){
		if(mDialog != null){
			if(mDialog.isShowing()){
				mDialog.dismiss();
			}
		}
		mDialog = null;
	}
	
}

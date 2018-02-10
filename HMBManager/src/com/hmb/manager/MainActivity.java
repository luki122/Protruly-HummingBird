package com.hmb.manager;


import hb.app.HbActivity;

import com.hmb.manager.qscaner.QScannerActivity;
import com.hmb.manager.rubbishclean.CleanSpeedActivity;
import com.hmb.manager.utils.ManagerUtils;
import com.hmb.manager.utils.SPUtils;
import com.hmb.manager.utils.TransUtils;
import com.hmb.manager.R;
import com.hmb.manager.bean.AppInfo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import hb.widget.toolbar.Toolbar.OnMenuItemClickListener;
import com.hmb.manager.onekeyclean.OneKeyCleanUpActivity;
import com.hmb.manager.widget.HMBProgressBar;

/** 
 * @author damon
 *
 */
public class MainActivity extends HbActivity  implements OnMenuItemClickListener,View.OnClickListener{

	private HMBProgressBar progressBar=null;

	private Context  context=null;

	private Button oneKeyOpt=null;
	
	private static final int PHONE_CHECKUP_START = 0x01;
	private static final int PHONE_CHECKUP_END = 0x02;
	private int mScore=100;
	private String barTitle=null;
	private static final long DELAYMILLIS=0;


	@Override
	protected void onCreate(Bundle savedInstanceState) {   
		super.onCreate(savedInstanceState);
		setHbContentView(R.layout.activity_main);
		context=this.getApplicationContext();
		progressBar=(HMBProgressBar)this.findViewById(R.id.progressBar);
		oneKeyOpt=(Button)this.findViewById(R.id.oneKeyOpt);
        initLayoutParams();
	}
 
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
//		Message msg = mUIHandler.obtainMessage(PHONE_CHECKUP_START);
//		mUIHandler.sendMessage(msg);
		initProgressBar();
		Message msgEnd = mUIHandler.obtainMessage(PHONE_CHECKUP_END);
		mUIHandler.sendMessageDelayed(msgEnd, DELAYMILLIS);
	}

	private void initProgressBar(){
		mScore=ManagerUtils.getPhoneScore(context);
		if(mScore>=80){
			progressBar.setProgressPaintColor(context.getResources().getColor(R.color.progressbar_blue));
			progressBar.setTitleColor(Color.parseColor("#B2000000"));
			barTitle=context.getString(R.string.phone_status_1);
		}else if(mScore<80&&mScore>=60){
			progressBar.setProgressPaintColor(context.getResources().getColor(R.color.progressbar_yellow));
			progressBar.setTitleColor(Color.parseColor("#B2000000"));
			barTitle=context.getString(R.string.phone_status_2);
		}else if(mScore<60){
			progressBar.setProgressPaintColor(context.getResources().getColor(R.color.progressbar_red));
			progressBar.setTitleColor(context.getResources().getColor(R.color.progressbar_red));
			barTitle=context.getString(R.string.phone_status_3);
		}
	}
	
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.menu_settings) {
			startActivity(new Intent(this, SettingActivity.class));
        }
        return false;
    }

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
		int id =v.getId();
		
		Intent intent=null;
		
		switch (id) {

		case R.id.tab_1_layout:
			intent= new Intent(this,CleanSpeedActivity.class);
			startActivity(intent); 
			break;
		case R.id.tab_2_layout:
			intent=new Intent();
			intent.setAction("com.hb.reject.main");  
			startActivity(intent);
			break;
		case R.id.tab_3_layout:
			intent = new Intent();
			intent.setAction("hmb.powermanager.powersave");
			startActivity(intent);
			break;
		case R.id.tab_4_layout:
			intent = new Intent();
			intent.setAction("com.hb.netmanage.main.action");
			startActivity(intent);
			break;
		case R.id.tab_5_layout:
			intent= new Intent(this,QScannerActivity.class);
			startActivity(intent);
			break;
		case R.id.tab_6_layout:
			intent = new Intent();
			intent.setAction("hmb.powermanager.appmanager");
			startActivity(intent);
			break;
		case R.id.oneKeyOpt:
			intent= new Intent(this,OneKeyCleanUpActivity.class);
			startActivity(intent);
			break;
		}

	}

	private void initLayoutParams(){
		getToolbar().inflateMenu(R.menu.main_toolbar_menu);
		oneKeyOpt.setOnClickListener(this);
		this.findViewById(R.id.tab_1_layout).setOnClickListener(this);
		this.findViewById(R.id.tab_2_layout).setOnClickListener(this);
		this.findViewById(R.id.tab_3_layout).setOnClickListener(this);
		this.findViewById(R.id.tab_4_layout).setOnClickListener(this);
		this.findViewById(R.id.tab_5_layout).setOnClickListener(this);
		this.findViewById(R.id.tab_6_layout).setOnClickListener(this);

	}

	
	private Handler mUIHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case PHONE_CHECKUP_START:
				progressBar.setTextPaintMsg(context.getString(R.string.safety_scanning));
				oneKeyOpt.setText(context.getString(R.string.bar_scanning));
				oneKeyOpt.setEnabled(false);
				progressBar.setProgressValue(100, true);
				break;
			case PHONE_CHECKUP_END:
				oneKeyOpt.setText(context.getString(R.string.onekey_optimize_title));
				oneKeyOpt.setEnabled(true);
				progressBar.setTitleText(context.getString(R.string.onekey_optimize_title));
				progressBar.setTextPaintMsg(barTitle);
				progressBar.setProgressValue(mScore, false);
				break;
			}
		}
	};

}

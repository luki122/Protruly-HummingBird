package com.hb.lockscreenapp;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
//import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class MainActivity extends Activity implements View.OnClickListener {

    DevicePolicyManager policyManager;
    private ComponentName componentName;
    private TextView mTV;
    private int mJudgeCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTV = (TextView) findViewById(R.id.tv);
        mTV.setOnClickListener(this);
        policyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        componentName = new ComponentName(this, MyDeviceAdminReceiver.class);
        if (!policyManager.isAdminActive(componentName)) {
            goSetActivity();
        } else {
        	Log.d("tangjun222", "--222systemLock");
            systemLock();
        }
    }

    private void goSetActivity() { 
    	if(!isSystemPackage(this.getPackageName()) || mJudgeCount >=2) {
    		Log.d("tangjun222", "--this.getPackageName() = " + this.getPackageName());
	        // 启动设备管理(隐式Intent) - 在AndroidManifest.xml中设定相应过滤器
	        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
	        // 权限列表
	        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
	        // 描述(additional explanation) 在申请权限时出现的提示语句
	        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
	                this.getResources().getString(R.string.explanation));
	
	        startActivityForResult(intent, 1);
    	} else {
    		try {
    			policyManager.setActiveAdmin(componentName, false);
            } catch (RuntimeException e) {
                // Something bad happened...  could be that it was
                // already set, though.
            }
            if (!policyManager.isAdminActive(componentName)) {
            	Log.d("tangjun222", "--111goSetActivity");
            	mJudgeCount ++;
                goSetActivity();
            } else {
            	Log.d("tangjun222", "--111systemLock");
                systemLock();
            }
    	}
    }
    
    private boolean isSystemPackage(String packageName) {
        try {
			PackageInfo packageInfo = this.getPackageManager().getPackageInfo(packageName,
					PackageManager.GET_DISABLED_COMPONENTS
					|PackageManager.GET_UNINSTALLED_PACKAGES
                    |PackageManager.GET_SIGNATURES);	
			ApplicationInfo appInfo = packageInfo.applicationInfo;
			if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0){
				return true;
			}else{
				return false;
			}
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
	}

    /**
     * 锁屏并关闭屏幕
     */
    private void systemLock() {
        if (policyManager.isAdminActive(componentName)) {
//            Window localWindow = getWindow();
//            WindowManager.LayoutParams localLayoutParams = localWindow.getAttributes();
//            localLayoutParams.screenBrightness = 0.01f;
//            localWindow.setAttributes(localLayoutParams);
            policyManager.lockNow();
        }
        finish();
    }

    @Override
    public void onClick(View view) {
        if (view == mTV) {
            goSetActivity();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (1 == requestCode) {
            if (RESULT_OK == resultCode) {
                systemLock();
            } else if (RESULT_CANCELED == resultCode) {
            }
        }
    }

}

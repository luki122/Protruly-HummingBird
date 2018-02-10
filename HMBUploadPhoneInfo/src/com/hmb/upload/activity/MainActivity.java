package com.hmb.upload.activity;

import android.app.Activity;
import android.os.Bundle;

import com.hmb.upload.R;

/**
 * only test
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        AccountUtils.getIntance().getAccountID(this);
//        Intent intent = new Intent(this, UploadService.class);
//        startService(intent);
    }
}

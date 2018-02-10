package com.android.provision;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.android.provision.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Created by xiaobin on 17-5-16.
 */

public class InfoActivity extends BaseActivity {

    private TextView btn1;
    private TextView btn2;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setHbContentView(R.layout.activity_info);

        getToolbar().setTitle(getString(R.string.back));

        btn1 = (TextView) findViewById(R.id.btn1);
        btn2 = (TextView) findViewById(R.id.btn2);

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.goNext(InfoActivity.this, TransferActivity.INFO);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onNavigationClicked(View view) {
        finish();
    }

    public String getFromAssets(String fileName) {
        try {
            InputStreamReader inputReader = new InputStreamReader(getResources().getAssets().open(fileName));
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line = "";
            StringBuffer sb = new StringBuffer();
            while ((line = bufReader.readLine()) != null)
                sb.append(line);
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

}

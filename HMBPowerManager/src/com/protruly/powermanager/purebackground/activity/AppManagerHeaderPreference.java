
package com.protruly.powermanager.purebackground.activity;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.protruly.powermanager.R;
import com.protruly.powermanager.powersave.view.WaveLoadingView;

import hb.preference.Preference;

public class AppManagerHeaderPreference extends Preference {



    public AppManagerHeaderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_app_manager_header);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        view.setEnabled(false);
    }
}
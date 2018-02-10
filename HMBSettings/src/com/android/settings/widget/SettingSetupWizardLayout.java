package com.android.settings.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.settings.R;

/**
 * Created by liuqin on 17-5-25.
 *
 * @date Liuqin on 2017-05-25
 */
public class SettingSetupWizardLayout extends LinearLayout {
    public SettingSetupWizardLayout(Context context) {
        super(context);
    }

    public SettingSetupWizardLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setHeaderText(CharSequence title) {
        TextView layoutTitle = (TextView) findViewById(
                R.id.suw_layout_title);
        if (layoutTitle != null) {
            layoutTitle.setText(title);
        }
    }
}

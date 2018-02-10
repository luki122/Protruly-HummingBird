
package com.protruly.powermanager.powersave.fuelgauge;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.protruly.powermanager.R;

import hb.preference.Preference;

public class AppProgressPreference extends Preference {

    private String title;
    private String summary;
    private Drawable drawable;
    private Context mContext;

    public AppProgressPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setLayoutResource(R.layout.preference_app_progress);
    }

    public void setAppProgress(Drawable drawable, String title, String summary) {
        this.title = title;
        this.summary = summary;
        this.drawable = drawable;

        notifyChanged();
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        view.setEnabled(false);
        ImageView icon = (ImageView) view.findViewById(R.id.icon);
        TextView tvTitle = (TextView) view.findViewById(R.id.title);
        TextView tvSummary = (TextView) view.findViewById(R.id.summary);

        icon.setImageDrawable(drawable);
        tvTitle.setText(title);
        tvSummary.setText(mContext.getString(R.string.power_consumption_percent) + summary);
    }
}
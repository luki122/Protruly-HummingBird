package com.android.settings.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.android.settings.R;

import hb.preference.CheckBoxPreference;

/**
 * Created by liuqin on 17-7-3.
 *
 * @date Liuqin on 2017-07-03
 */
public class NavigationbarPositionPerference extends CheckBoxPreference{
    private ImageView mNavPositionImage;
    private OnClickListener mListener = null;
    private int mNavPositionImageResId = 0;

    public NavigationbarPositionPerference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.navigationbar_position);
        setWidgetLayoutResource(R.layout.preference_widget_radiobutton);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mNavPositionImage = (ImageView) view.findViewById(R.id.navigation_position_image);
        setNavigationPostionImage(mNavPositionImageResId);
    }

    @Override
    public void onClick() {
        if (mListener != null) {
            mListener.onRadioButtonClicked(this);
        }
    }

    public void setOnClickListener(OnClickListener listener) {
        mListener = listener;
    }

    public void setNavigationPostionImage(int resId) {
        mNavPositionImageResId = resId;
        if (resId > 0 && mNavPositionImage != null) {
            mNavPositionImage.setImageResource(resId);
        }
    }

    public interface OnClickListener {
        void onRadioButtonClicked(NavigationbarPositionPerference emiter);
    }
}

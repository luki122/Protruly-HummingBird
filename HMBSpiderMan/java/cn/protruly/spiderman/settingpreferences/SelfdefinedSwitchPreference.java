package cn.protruly.spiderman.settingpreferences;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import cn.protruly.spiderman.R;


/**
 * Created by lijia on 17-4-25.
 */

public class SelfdefinedSwitchPreference extends Preference {

    private Switch mSwitch;
    private boolean mChecked = false;
    private View.OnClickListener mOnClickListener;
    private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener;

    public SelfdefinedSwitchPreference(Context context, AttributeSet attrs) {

        super(context, attrs);
        //通过调用setWidgetLayoutResource方法来更新preference的widgetLayout,即更新控件区域
        setWidgetLayoutResource(R.layout.switch_button);

    }

    @Override
    protected void onBindView(View view) {

        super.onBindView(view);
        mSwitch = (Switch) view.findViewById(R.id.switch_button);

        /**
         * view即是代表的preference整个区域,可以对该view进行事件监听,也就是实现了preference整个区域的点击事件
         * 此处调用自定义的监听器A方法,该监听器A接口应由使用SelfdefinedSwitchPreference的类来实现,从而实现
         * preference整个区域的点击事件。注:监听器A的定义可以参考OnRadioButtonCheckedListener接口的定义
         */

        view.setOnClickListener(mOnClickListener);

        /**
         * switch开关的点击事件
         * 此处调用自定义的监听器B方法,该监听器B接口应由使用SelfdefinedSwitchPreference的类来实现,从而实现
         * preference的switch点击事件.注:监听器B的定义可以参考OnRadioButtonCheckedListener接口的定义
         */

        if (mSwitch != null) {
            mSwitch.setOnCheckedChangeListener(mOnCheckedChangeListener);
            mSwitch.setChecked(mChecked);
        }

    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        this.mOnClickListener = onClickListener;
    }

    public void setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener onCheckedChangeListener) {
        this.mOnCheckedChangeListener = onCheckedChangeListener;
    }

    public void setChecked(boolean checked) {
        this.mChecked = checked;
        notifyChanged();
    }

    public boolean isChecked() {
        return this.mChecked;
    }

    public void callOnStateChangeListener(boolean paramBoolean)
    {
        setChecked(paramBoolean);
        callChangeListener(Boolean.valueOf(paramBoolean));
    }

}

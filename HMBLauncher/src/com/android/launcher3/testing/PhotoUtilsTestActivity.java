package com.android.launcher3.testing;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.launcher3.R;
import com.android.launcher3.colors.ColorManager;
import com.android.launcher3.theme.IconManager;
import com.android.launcher3.theme.utils.PhotoUtils;

public class PhotoUtilsTestActivity extends Activity {
    ImageView mSmallIcon;
    ImageView mMatchIcon;
    ImageView mBigIcon;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_utils_test);
        mSmallIcon = (ImageView) findViewById(R.id.smallicon);
        mMatchIcon = (ImageView) findViewById(R.id.matchicon);
        mBigIcon = (ImageView) findViewById(R.id.bigicon);
        Bitmap small  = PhotoUtils.composite(getDrawable(R.drawable.res_smallicon),getDrawable(R.drawable.ic_mask),getDrawable(R.drawable.ic_bg),getDrawable(R.drawable.ic_zoom_template));
        Bitmap match  = PhotoUtils.composite(getDrawable(R.drawable.res_matchicon),getDrawable(R.drawable.ic_mask),getDrawable(R.drawable.ic_bg),getDrawable(R.drawable.ic_zoom_template));
        Bitmap big  = PhotoUtils.composite(getDrawable(R.drawable.res_bigicon),getDrawable(R.drawable.ic_mask),getDrawable(R.drawable.ic_bg),getDrawable(R.drawable.ic_zoom_template));
        //mSmallIcon.setImageBitmap(small);
        mMatchIcon.setImageBitmap(match);
        mBigIcon.setImageBitmap(big);
        mSmallIcon.setBackgroundDrawable(new BitmapDrawable(small));

        TextView textView = (TextView) findViewById(R.id.test_text);
        textView.setTextColor(IconManager.getInstance(this).getColor("com.android.launcher3$dym_calendar_day_text"));
    }
}

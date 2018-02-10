package com.hb.thememanager.ui.fragment.themedetail;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hb.thememanager.R;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.job.loader.ImageLoader;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.IntentUtils;
import com.hb.thememanager.views.ThemePreviewDonwloadButton;

import hb.widget.toolbar.Toolbar;

/**
 * Created by alexluo on 17-8-10.
 */

public class LocalFontDetailFragment extends LocalThemeDetailFragment {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        if(mCurrentTheme.type == Theme.FONTS){
            mPreviewImageWidth = getContext().getResources().getDisplayMetrics().widthPixels;
            mPreviewImageHeight = getResources().getDimensionPixelOffset(R.dimen.font_detail_preview_img_height);
            mImageLeftMargin = 0;
        }
    }

}

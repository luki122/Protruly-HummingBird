package com.hb.thememanager.ui;

import android.os.Bundle;

public class FragmentSwitchActivity extends MainActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        clearImageCacheIfNeeded(false);
    }
}

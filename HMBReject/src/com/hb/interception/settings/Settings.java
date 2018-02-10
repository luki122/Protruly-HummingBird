package com.hb.interception.settings;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import hb.preference.*;
import hb.preference.Preference.OnPreferenceChangeListener;
import com.hb.interception.R;
import com.hb.interception.util.BlackUtils;
import hb.app.HbActivity;

public class Settings extends HbActivity {
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        Fragment f = Fragment.instantiate(this, SettingsFragment.class.getName(), null);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(com.hb.R.id.content, f);
        ViewGroup content = (ViewGroup)findViewById(com.hb.R.id.content);
        transaction.commitAllowingStateLoss();
        getFragmentManager().executePendingTransactions();
        setTitle(R.string.action_settings);
    }

    @Override
    public void onNavigationClicked(View view) {
        finish();
    }
}

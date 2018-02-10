package com.hb.interception.settings;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import hb.preference.CheckBoxPreference;
import hb.preference.Preference;
import hb.preference.Preference.OnPreferenceChangeListener;
import hb.preference.PreferenceScreen;
import hb.preference.PreferenceFragment;
import hb.preference.SwitchPreference;

import com.hb.interception.activity.BlackList;
import com.hb.interception.activity.WhiteList;
import com.hb.interception.util.BlackUtils;
import com.hb.interception.R;

public class SettingsFragment extends PreferenceFragment  implements OnPreferenceChangeListener{
    private CountAndNarrowPreference mBlack, mWhite;
    private SwitchPreference mNotificationSwitch;
    private int mBlackCount = 0;
    private int mWhiteCount = 0;
    private ContentResolver mContentResolver;
    private AsyncQueryHandler mQueryHandler;
    private Activity mActivity;
    private SharedPreferences mPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.set);

        mBlack = (CountAndNarrowPreference) findPreference("black_list");
        mWhite = (CountAndNarrowPreference) findPreference("white_list");
        mActivity = getActivity();

        mContentResolver = mActivity.getContentResolver();
        mQueryHandler = new QueryHandler(mContentResolver, mActivity);
        mPreferences = mActivity.getSharedPreferences("settings",
                Context.MODE_PRIVATE);
        initNotificationSwitch();
    }

    //zhangcj add
	private void initNotificationSwitch() {
		mNotificationSwitch = (SwitchPreference) findPreference("notification");
		mNotificationSwitch.setOnPreferenceChangeListener(this);

		Cursor c = mContentResolver.query(BlackUtils.SETTING_URI, null,
				"name = 'notification'", null, null);
		if (c != null) {
			while (c.moveToNext()) {
				boolean value = c.getInt(c.getColumnIndex("value")) > 0;
				mNotificationSwitch.setChecked(value);
			}
			c.close();
		}
	}
    
    @Override
    public void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        mQueryHandler.startQuery(0, null, BlackUtils.BLACK_URI, null,
                "isblack=1", null, null);
        mQueryHandler.startQuery(1, null, BlackUtils.WHITE_URI, null, null, null, null);
    }

    @Override
    public boolean onPreferenceChange(Preference arg0, Object arg1) {
        // TODO Auto-generated method stub
        boolean b = (Boolean) arg1;
        String s = arg0.getKey();
        if(s.equals("notification")) {
        	BlackUtils.updateValue(mActivity, "notification", b);
        }
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        // TODO Auto-generated method stub
        String key = preference.getKey();
        if (key.equals("black_list")) {
            Intent intent = new Intent(mActivity.getApplicationContext(), BlackList.class);
            mActivity.startActivity(intent);
        } else if (key.equals("white_list")) {
            Intent intent = new Intent(mActivity.getApplicationContext(), WhiteList.class);
            mActivity.startActivity(intent);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private class QueryHandler extends AsyncQueryHandler {
        private final Context mContext;

        public QueryHandler(ContentResolver cr, Context context) {
            super(cr);
            this.mContext = context;
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            // TODO Auto-generated method stub
            super.onQueryComplete(token, cookie, cursor);
            if (cursor != null) {
                if (token == 0) {
                    mBlackCount = cursor.getCount();
                    mBlack.setCount(mBlackCount +mActivity.getResources().getString(R.string.item));
                } else {
                	mWhiteCount = cursor.getCount();
                    mWhite.setCount(mWhiteCount + mActivity.getResources().getString(R.string.item));
                }
            }
            if (cursor != null) {
                cursor.close();
            }
        }

    }
}

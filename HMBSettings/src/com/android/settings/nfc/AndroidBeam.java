/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.nfc;

import android.app.ActionBar;
import android.content.Context;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.UserManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import hb.widget.Switch;
import hb.app.HbActivity;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.HelpUtils;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.widget.SwitchBar;

import com.mediatek.beam.BeamShareHistory;
import com.mediatek.settings.FeatureOption;

public class AndroidBeam extends InstrumentedFragment
        implements SwitchBar.OnSwitchChangeListener {

    /// M: Add beam plus history menu
    private final static int MENU_SHOW_RECEIVED_FILES = 0;

    private View mView;
    private NfcAdapter mNfcAdapter;
    private SwitchBar mSwitchBar;
    private CharSequence mOldActivityTitle;
    private boolean mBeamDisallowed;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getActivity().getActionBar();

        mOldActivityTitle = actionBar.getTitle();
        actionBar.setTitle(R.string.android_beam_settings_title);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
        mBeamDisallowed = ((UserManager) getActivity().getSystemService(Context.USER_SERVICE))
                .hasUserRestriction(UserManager.DISALLOW_OUTGOING_BEAM);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        HelpUtils.prepareHelpMenuItem(getActivity(), menu, R.string.help_uri_beam,
                getClass().getName());
        /// M: Add beam plus history menu item
        if (FeatureOption.MTK_BEAM_PLUS_SUPPORT) {
            menu.add(Menu.NONE, MENU_SHOW_RECEIVED_FILES, 0,
                    R.string.beam_share_history_title).setShowAsAction(
                    MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /// M: Show beam plus shared history when menu selected @{
        if (item.getItemId() == MENU_SHOW_RECEIVED_FILES) {
            ((SettingsActivity) getActivity()).startPreferencePanel(
                    BeamShareHistory.class.getName(), null, 0, null, null, 0);
        }
        /// @}
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        /// M: When beam plus support, show beam plus UI @{
        if (FeatureOption.MTK_BEAM_PLUS_SUPPORT) {
            mView = inflater.inflate(R.layout.android_beam_plus, container, false);
            Utils.prepareCustomPreferencesList(container, mView, mView, false);
        } else {
            mView = inflater.inflate(R.layout.android_beam, container, false);
        }
        /// @}
        return mView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        SettingsActivity activity = (SettingsActivity) getActivity();

        mSwitchBar = activity.getSwitchBar();
        mSwitchBar.setChecked(!mBeamDisallowed && mNfcAdapter.isNdefPushEnabled());
        mSwitchBar.addOnSwitchChangeListener(this);
        mSwitchBar.setEnabled(!mBeamDisallowed);
        mSwitchBar.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mOldActivityTitle != null) {
            //getActivity().getActionBar().setTitle(mOldActivityTitle);
        	((HbActivity)getActivity()).getToolbar().setTitle(mOldActivityTitle);
        }
        mSwitchBar.removeOnSwitchChangeListener(this);
        mSwitchBar.hide();
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean desiredState) {
        boolean success = false;
        mSwitchBar.setEnabled(false);
        if (desiredState) {
            success = mNfcAdapter.enableNdefPush();
        } else {
            success = mNfcAdapter.disableNdefPush();
        }
        if (success) {
            mSwitchBar.setChecked(desiredState);
        }
        mSwitchBar.setEnabled(true);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.NFC_BEAM;
    }
}

package com.protruly.powermanager.powersave.fuelgauge;


import android.app.Fragment;
import android.content.Context;
import android.os.BatteryStats;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.protruly.powermanager.R;
import com.protruly.powermanager.utils.LogUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import hb.app.HbActivity;

/**
 * Displays a list of software and hardware that consume power, ordered by how much power was
 * consumed since the last time it was charged.
 */
public class PowerUsageSummaryFragment extends Fragment implements AdapterView.OnItemClickListener {
    private static final String TAG = "PowerUsageSummaryFragment";

    private static final int MAX_ITEMS_TO_LIST = 10;

    private UserManager mUm;
    private BatteryStatsHelper mStatsHelper;

    private ListView mListView;
    private SummaryAppListAdapter mAdapter;

    private double mTotalPower;
    private double mSoftTotalPower;
    private double mHardTotalPower;
    private List<BatteryEntry> mHardwareList = new ArrayList<>();
    private List<BatteryEntry> mSoftwareList = new ArrayList<>();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mStatsHelper = new BatteryStatsHelper(context, true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mStatsHelper.create(savedInstanceState);
        mAdapter = new SummaryAppListAdapter(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.power_usage_summary_fragment, container, false);
        mListView = (ListView) view.findViewById(R.id.usage_summary_list);
        mListView.setItemsCanFocus(true);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mStatsHelper.clearStats();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStats();
    }

    @Override
    public void onPause() {
        super.onPause();
        BatteryEntry.stopRequestQueue();
        mHandler.removeMessages(BatteryEntry.MSG_UPDATE_NAME_ICON);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity().isChangingConfigurations()) {
            mStatsHelper.storeState();
            BatteryEntry.clearUidCache();
        }
    }

    private void refreshStats() {
        mTotalPower = 0;
        mSoftTotalPower = 0;
        mHardTotalPower = 0;
        mHardwareList.clear();
        mSoftwareList.clear();

        mStatsHelper.refreshStats(BatteryStats.STATS_SINCE_CHARGED, mUm.getUserProfiles());
        final List<BatterySipper> usageList = getCoalescedUsageList(mStatsHelper.getUsageList());
        final int numSippers = usageList.size();
        for (int i = 0; i < numSippers; i++) {
            final BatterySipper sipper = usageList.get(i);
            if (sipper.totalPowerMah < 0) {
                LogUtils.d(TAG, "refreshStats() Math.abs -> sipper.drainType = " + sipper.drainType
                        + ", uid = " + sipper.getUid() + ", PowerMah = " + sipper.totalPowerMah);
                sipper.totalPowerMah = Math.abs(sipper.totalPowerMah);
            }
            final BatteryEntry entry = new BatteryEntry(getActivity(), mHandler, mUm, sipper);
            LogUtils.d(TAG, "refreshStats() -> sipper.drainType = " + sipper.drainType
                    + ", label = " + entry.getLabel() + ", PowerMah = " + sipper.totalPowerMah);
            if (sipper.drainType == BatterySipper.DrainType.SCREEN ||
                    sipper.drainType == BatterySipper.DrainType.CELL ||
                    sipper.drainType == BatterySipper.DrainType.IDLE ||
                    sipper.drainType == BatterySipper.DrainType.WIFI ||
                    sipper.drainType == BatterySipper.DrainType.BLUETOOTH ||
                    sipper.drainType == BatterySipper.DrainType.PHONE ) {
                mHardwareList.add(entry);
                mHardTotalPower += sipper.totalPowerMah;
            } else if (sipper.drainType == BatterySipper.DrainType.UNACCOUNTED
                    || sipper.drainType == BatterySipper.DrainType.OVERCOUNTED ){
                LogUtils.d(TAG, "refreshStats() -> ignore " + sipper.drainType
                        + ", PowerMah = " + sipper.totalPowerMah);
            } else {
                mSoftwareList.add(entry);
                mSoftTotalPower += sipper.totalPowerMah;
            }
        }
        mTotalPower = mHardTotalPower + mSoftTotalPower;
        LogUtils.d(TAG, "refreshStats() -> mHardTotalPower = " + mHardTotalPower
                + ", mSoftTotalPower = " + mSoftTotalPower + ", mTotalPower = " + mTotalPower);
        Bundle bundle = getArguments();
        int arg = bundle.getInt("fragment_key");
        BatteryEntry.startRequestQueue();
        if (arg == 0) {
            refreshList(mSoftwareList);
        } else {
            refreshList(mHardwareList);
        }
    }

    /**
     * We want to coalesce some UIDs. For example, dex2oat runs under a shared gid that
     * exists for all users of the same app. We detect this case and merge the power use
     * for dex2oat to the device OWNER's use of the app.
     * @return A sorted list of apps using power.
     */
    private static List<BatterySipper> getCoalescedUsageList(final List<BatterySipper> sippers) {
        final SparseArray<BatterySipper> uidList = new SparseArray<>();

        final ArrayList<BatterySipper> results = new ArrayList<>();
        final int numSippers = sippers.size();
        for (int i = 0; i < numSippers; i++) {
            BatterySipper sipper = sippers.get(i);
            if (sipper.getUid() > 0) {
                int realUid = sipper.getUid();

                // Check if this UID is a shared GID. If so, we combine it with the OWNER's
                // actual app UID.
                if (isSharedGid(sipper.getUid())) {
                    realUid = UserHandle.getUid(UserHandle.USER_OWNER,
                            UserHandle.getAppIdFromSharedAppGid(sipper.getUid()));
                }

                // Check if this UID is a system UID (mediaserver, logd, nfc, drm, etc).
                if (isSystemUid(realUid)
                        && !"mediaserver".equals(sipper.packageWithHighestDrain)) {
                    // Use the system UID for all UIDs running in their own sandbox that
                    // are not apps. We exclude mediaserver because we already are expected to
                    // report that as a separate item.
                    realUid = Process.SYSTEM_UID;
                }

                if (realUid != sipper.getUid()) {
                    // Replace the BatterySipper with a new one with the real UID set.
                    BatterySipper newSipper =
                            new BatterySipper(sipper.drainType, new FakeUid(realUid), 0.0);
                    newSipper.add(sipper);
                    newSipper.packageWithHighestDrain = sipper.packageWithHighestDrain;
                    newSipper.mPackages = sipper.mPackages;
                    sipper = newSipper;
                }

                int index = uidList.indexOfKey(realUid);
                if (index < 0) {
                    // New entry.
                    uidList.put(realUid, sipper);
                } else {
                    // Combine BatterySippers if we already have one with this UID.
                    final BatterySipper existingSipper = uidList.valueAt(index);
                    existingSipper.add(sipper);
                    if (existingSipper.packageWithHighestDrain == null
                            && sipper.packageWithHighestDrain != null) {
                        existingSipper.packageWithHighestDrain = sipper.packageWithHighestDrain;
                    }

                    final int existingPackageLen = existingSipper.mPackages != null ?
                            existingSipper.mPackages.length : 0;
                    final int newPackageLen = sipper.mPackages != null ?
                            sipper.mPackages.length : 0;
                    if (newPackageLen > 0) {
                        String[] newPackages = new String[existingPackageLen + newPackageLen];
                        if (existingPackageLen > 0) {
                            System.arraycopy(existingSipper.mPackages, 0, newPackages, 0,
                                    existingPackageLen);
                        }
                        System.arraycopy(sipper.mPackages, 0, newPackages, existingPackageLen,
                                newPackageLen);
                        existingSipper.mPackages = newPackages;
                    }
                }
            } else {
                results.add(sipper);
            }
        }

        final int numUidSippers = uidList.size();
        for (int i = 0; i < numUidSippers; i++) {
            results.add(uidList.valueAt(i));
        }

        // The sort order must have changed, so re-sort based on total power use.
        Collections.sort(results, new Comparator<BatterySipper>() {
            @Override
            public int compare(BatterySipper a, BatterySipper b) {
                return Double.compare(b.totalPowerMah, a.totalPowerMah);
            }
        });
        return results;
    }

    private static boolean isSharedGid(int uid) {
        return UserHandle.getAppIdFromSharedAppGid(uid) > 0;
    }

    private static boolean isSystemUid(int uid) {
        return uid >= Process.SYSTEM_UID && uid < Process.FIRST_APPLICATION_UID;
    }

    private void refreshList(List<BatteryEntry> list) {
        mAdapter.setItemData(list);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        LogUtils.d(TAG, "onItemClick() -> position = " + position);
        if (mAdapter == null) {
            return;
        }
        BatteryEntry entry = mAdapter.getItem(position);
        if (entry == null) {
            return;
        }
        PowerUsageDetailActivity.startBatteryDetailPage(getActivity(), mStatsHelper, entry);
    }

    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BatteryEntry.MSG_UPDATE_NAME_ICON:
                    if (mAdapter != null) {
                        mAdapter.notifyDataSetChanged();
                    }
                    break;
                case BatteryEntry.MSG_REPORT_FULLY_DRAWN:
                    HbActivity activity = (HbActivity) getActivity();
                    if (activity != null) {
                        activity.reportFullyDrawn();
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    public class SummaryAppListAdapter extends BaseAdapter {
        private Context mContext;
        private CharSequence mProgressText;
        private List<BatteryEntry> mListItemData = new ArrayList<>();

        SummaryAppListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getCount() {
            if (mListItemData != null) {
                return mListItemData.size();
            }
            return 0;
        }

        @Override
        public BatteryEntry getItem(int paramInt) {
            if (mListItemData == null) {
                return null;
            }
            return mListItemData.get(paramInt);
        }

        @Override
        public long getItemId(int paramInt) {
            return 0;
        }

        private void setPercent(double percentOfTotal) {
            mProgressText = mContext.getResources().getString(R.string.percentage, percentOfTotal);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            final ViewCache viewCache;

            if (view == null) {
                view = LayoutInflater.from(mContext)
                        .inflate(R.layout.power_usage_app_percentage_item, parent, false);
                viewCache = new ViewCache();
                view.setTag(viewCache);
            } else {
                viewCache = (ViewCache) view.getTag();
            }

            BatteryEntry entry = mListItemData.get(position);
            setPercent(entry.sipper.percent);

            viewCache.icon = (ImageView) view.findViewById(android.R.id.icon);
            viewCache.icon.setImageDrawable(entry.getIcon());
            String pkgLabel = entry.getLabel();
            viewCache.text = (TextView) view.findViewById(R.id.title);
            viewCache.text.setText(pkgLabel);
            viewCache.text1 = (TextView) view.findViewById(R.id.summary);
            viewCache.text1.setText(mProgressText);

            return view;
        }

        void setItemData(List<BatteryEntry> list) {
            mListItemData.clear();
            double totalPower = 0;
            if (list == mHardwareList) {
                totalPower = mHardTotalPower;
            } else {
                totalPower = mSoftTotalPower;
            }
            if (totalPower == 0) {
                notifyDataSetChanged();
                return;
            }
            for (BatteryEntry entry : list) {
                final double percentOfTotal = ((entry.sipper.sumPower() / mTotalPower) * 100);
                LogUtils.d(TAG, "setItemData() -> entry = " + entry.name
                        + ", sumPower = " + entry.sipper.sumPower()
                        + ", percentOfTotal = " + percentOfTotal);
                entry.sipper.percent = Math.round(percentOfTotal);
                if (entry.sipper.percent < 1f) {
                    continue;
                }
                mListItemData.add(entry);

                if (mListItemData.size() > MAX_ITEMS_TO_LIST) {
                    break;
                }
            }

            notifyDataSetChanged();
        }

        class ViewCache {
            ImageView icon;
            TextView text;
            TextView text1;
        }
    }
}
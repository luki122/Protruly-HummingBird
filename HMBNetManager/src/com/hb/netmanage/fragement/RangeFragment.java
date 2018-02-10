package com.hb.netmanage.fragement;

import android.app.Fragment;
import android.content.Context;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkPolicyManager;
import android.net.NetworkStats;
import android.net.NetworkTemplate;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hb.netmanage.DataCorrect;
import com.hb.netmanage.DataManagerApplication;
import com.hb.netmanage.R;
import com.hb.netmanage.activity.MainActivity;
import com.hb.netmanage.adapter.RangeAppAdapter;
import com.hb.netmanage.entity.AppItem;
import com.hb.netmanage.utils.LogUtil;
import com.hb.netmanage.utils.PreferenceUtil;
import com.hb.netmanage.utils.StringUtil;
import com.hb.netmanage.utils.ToolsUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import hb.utils.AsyncTask;
import hb.widget.recycleview.LinearLayoutManager;
import hb.widget.recycleview.RecyclerView;

import static android.net.NetworkStats.SET_DEFAULT;
import static android.net.NetworkTemplate.buildTemplateMobileAll;
import static android.net.NetworkTemplate.buildTemplateWifiWildcard;
import static android.util.Config.LOGD;

/**
 *
 *
 */
public class RangeFragment extends Fragment implements View.OnClickListener{
    private static final String TAG = "RangeFragment";
    private static final String ARG_DATETYPE = "datetype";
    private static final String ARG_NETTYPE = "nettype";
    private static final String ARG_STATSTOTAL = "statstotal";
    private static final String ARG_APPS = "apps";
    private static final String APPS_POLICY = "apps_policy";
    private static final String APPS_NOPOLICY = "apps_nopolicy";
    private static final String TAB_MOBILE = "mobile";
    private static final String TAB_WIFI = "wifi";

    /**
     * 更新所有
     */
    private static final int UPDATE_ALL = 10000;
    /**
     * 统计当日使用闲时流量
     */
    private static final int DATA_FREE_STATUS_DAY = 1000;
    /**
     * 统计月使用闲时流量
     */
    private static final int DATA_FREE_STATUS_MONTH = 1001;
    /**
     * 更新排行界面
     */
    private static final int DATA_FREE_STATUS_UPDATE = 2000;
    private static final int DIALOG_STATUS_UPDATE = 2001;

    private View mView;
    private RecyclerView mRlView;
    private TextView mTvReject;
    private LinearLayout mLayEmpty;
    private RelativeLayout mLayTab;
    private ProgressBar mFoldProgressBar;
    private RangeAppAdapter mRangeAdapter;
    private Context mContext;
    private String mStatsDateType;
    private String mDataType;
    private boolean mIsStatsTotal;
    /**
     * 闲时流量统计结束
     */
    private boolean mIsFreeTag;

    private NetworkPolicyManager mPolicyManager;
    private NetworkTemplate mTemplate;
    private INetworkStatsSession mStatsSession;
    private INetworkStatsService mStatsService;

    private TelephonyManager mTelephonyManager;
    private OnFragmentInteractionListener mListener;

    private int indexSim = 0;
    /**
     * 统计流量次数
     */
    private int mStatsCount = 0;
    private DataManagerApplication mInstance;

    /**
     * 移动数据网络
     */
    private ArrayList<AppItem> mAppInfos = new ArrayList<AppItem>();
    private ArrayList<AppItem> mAppInfosByPolicy = new ArrayList<AppItem>();
    private ArrayList<AppItem> mAppInfosNoPolicy = new ArrayList<AppItem>();
    /**
     * 存储app使用的流量大小
     */
    private ArrayList<AppItem> mAppDefaultDatas = new ArrayList<AppItem>();
    private ArrayList<AppItem> mAppForegroundDatas = new ArrayList<AppItem>();
    private HashMap<Integer, AppItem> mFreeAppDefault = new HashMap<Integer, AppItem>();
    private HashMap<Integer, AppItem> mFreeAppForeground = new HashMap<Integer, AppItem>();

    private long mFreeTotal;
    //上次操作时间
    private long mLastTime = 0;

    //当前页
    private int mPageIndex = 1;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case UPDATE_ALL:
                    mTvReject.setEnabled(true);
                    break;
                case RangeAppAdapter.ALLOW_ALL:
                    mTvReject.setText(DataManagerApplication.getInstance().getString(R.string.all_allow));
                    break;
                case RangeAppAdapter.REJECT_ALL:
                    mTvReject.setText(DataManagerApplication.getInstance().getString(R.string.all_reject));
                    break;
                case RangeAppAdapter.EMPTY_APP:
                    mLayTab.setVisibility(View.GONE);
                    mLayEmpty.setVisibility(View.VISIBLE);
                    break;
                case RangeAppAdapter.RAGNE_APP:
                    mLayTab.setVisibility(View.VISIBLE);
                    mLayEmpty.setVisibility(View.GONE);
                    break;
                case RangeAppAdapter.UPDATE_ITEM_APP:
                    if (mRlView == null){
                        return;
                    }
                    LogUtil.d(TAG, "mRangeAdapter>>>" + mRlView.getAdapter());
                    if (mRlView.getAdapter() == null) {
                        mRlView.setAdapter(mRangeAdapter);
                    } else {
                        if (mRangeAdapter != null) {
                            mRangeAdapter.notifyDataSetChanged();
                        }
                    }
                    if (mRangeAdapter == null || mTvReject == null) {
                        return;
                    }
                    mAppInfosNoPolicy = mRangeAdapter.getmNoPolicyAppList();
                    if (mAppInfosNoPolicy != null && mAppInfosNoPolicy.size() > 0) {
                        mTvReject.setText(mInstance.getString(R.string.all_reject));
                    }
                    if (mAppInfosNoPolicy != null && mAppInfosNoPolicy.size() == 0) {
                        mTvReject.setText(mInstance.getString(R.string.all_allow));
                    }
                    mTvReject.setEnabled(true);
                    mRlView.setVisibility(View.VISIBLE);
                    break;
                case DATA_FREE_STATUS_UPDATE:
                    //更新排行界面
                    if (mRangeAdapter == null || mInstance == null) {
                        return;
                    }
                    if (TextUtils.equals(mDataType, mInstance.getString(R.string.data_mobile))
                            || TextUtils.equals(mDataType, mInstance.getString(R.string.data_wifi))) {
                        mRangeAdapter.setAppInfosUpdateData(mAppForegroundDatas);
                    } else if (TextUtils.equals(mDataType, mInstance.getString(R.string.net_bg))) {
                        mRangeAdapter.setAppInfosUpdateData(mAppDefaultDatas);
                    }
                    break;
                case DIALOG_STATUS_UPDATE:
                    if (mFoldProgressBar != null) {
                        mFoldProgressBar.setVisibility(View.GONE);
                    }
                    break;
            }
        }
    };

    public RangeFragment() {
        // Required empty public constructor
    }

    public static RangeFragment newInstance(String statsDateType, String netType, boolean isStatsTotal, ArrayList<AppItem> allApps, ArrayList<AppItem> policyApps, ArrayList<AppItem> noPolicyApps) {
        RangeFragment fragment = new RangeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DATETYPE, statsDateType);
        args.putString(ARG_NETTYPE, netType);
        args.putBoolean(ARG_STATSTOTAL, isStatsTotal);
        args.putSerializable(ARG_APPS, allApps);
        args.putSerializable(APPS_POLICY, policyApps);
        args.putSerializable(APPS_NOPOLICY, noPolicyApps);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mStatsDateType = getArguments().getString(ARG_DATETYPE);
            mDataType = getArguments().getString(ARG_NETTYPE);
            mIsStatsTotal = getArguments().getBoolean(ARG_STATSTOTAL);
            mAppInfosByPolicy = (ArrayList<AppItem>) getArguments().getSerializable(APPS_POLICY);
            mAppInfosNoPolicy = (ArrayList<AppItem>)getArguments().getSerializable(APPS_NOPOLICY);
            mAppInfos = (ArrayList<AppItem>)getArguments().getSerializable(ARG_APPS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mInstance = DataManagerApplication.getInstance();
        mContext = getActivity();
        mView = inflater.inflate(R.layout.fragment_range, container, false);
        initView();
        Log.v(TAG, "mView>>>>>" + mView);
        return mView;
    }

    private void initView() {
        mTvReject = (TextView) mView.findViewById(R.id.tv_all_reject);
        mRlView = (RecyclerView) mView.findViewById(R.id.recycler_add_orient_app);
        mLayEmpty = (LinearLayout) mView.findViewById(R.id.lay_empty_info);
        mLayTab = (RelativeLayout) mView.findViewById(R.id.lay_tab);
        mRlView.setHasFixedSize(true);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext);
        mRlView.setLayoutManager(linearLayoutManager);
        mTvReject.setOnClickListener(this);
        mRangeAdapter = new RangeAppAdapter(mContext, mHandler);
//        mRlView.setAdapter(mRangeAdapter);
        LogUtil.e(TAG, "mDataType>>" + mDataType + "<<<<>mAppInfosNoPolicy>>>" + mAppInfosNoPolicy.size());
        if (mAppInfosNoPolicy.size() > 0) {
            mTvReject.setText(mInstance.getString(R.string.all_reject));
        }
        if (mAppInfosNoPolicy.size() == 0) {
            mTvReject.setText(mInstance.getString(R.string.all_allow));
        }
        mLayTab.setVisibility(View.GONE);
        if (!TextUtils.equals(mDataType, mInstance.getString(R.string.data_wifi))){
            mLayEmpty.setVisibility(View.VISIBLE);
        } else {
            mLayEmpty.setVisibility(View.GONE);
        }
        mRlView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int lastVisiblePosition = linearLayoutManager.findLastVisibleItemPosition();
                Log.v(TAG, "lastVisiblePosition>>" + lastVisiblePosition + ">>getItemCount>>>" + mRangeAdapter.getItemCount());
                if (lastVisiblePosition ==  RangeAppAdapter.PAGE_COUNT * mPageIndex - 1) {
                    mPageIndex ++;
                    ArrayList<AppItem> allApps = mRangeAdapter.getmAppList();
                    ArrayList<AppItem> loadApps = mRangeAdapter.getPageAppList();
                    List<AppItem> nextApps = null;
                    int remainCount = allApps.size() - loadApps.size();
                    if (remainCount > RangeAppAdapter.PAGE_COUNT) {
                        nextApps = allApps.subList(loadApps.size(), loadApps.size() + RangeAppAdapter.PAGE_COUNT);
                    } else {
                        nextApps = allApps.subList(loadApps.size(), loadApps.size() + remainCount);
                    }
                    ArrayList<AppItem> addList = new ArrayList<AppItem>();
                    addList.addAll(nextApps);
                    mRangeAdapter.setPageAppList(addList);
                    mRangeAdapter.notifyItemRangeInserted(loadApps.size(), nextApps.size());
                    Log.v(TAG, "mPageIndex>>" + mPageIndex + "nextApps>>" + nextApps.size() + ">>getmPageAppList>>" + mRangeAdapter.getPageAppList().size());
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_all_reject:
                if (System.currentTimeMillis() - mLastTime <= 1000) {
                    mLastTime = System.currentTimeMillis();
                    return;
                }
                v.setEnabled(false);
                mLastTime = System.currentTimeMillis();
                if (mHandler.hasMessages(UPDATE_ALL)) {
                    return;
                }
                if (mRangeAdapter.getItemCount() == 0) {
                    v.setEnabled(true);
                    return;
                }
                if (TextUtils.equals(mTvReject.getText().toString(), mInstance.getString(R.string.all_reject))) {
                    mRangeAdapter.rejectAll();
                    mTvReject.setText(mInstance.getString(R.string.all_allow));
                } else if (TextUtils.equals(mTvReject.getText().toString(), mInstance.getString(R.string.all_allow))) {
                    mRangeAdapter.allowAll();
                    mTvReject.setText(mInstance.getString(R.string.all_reject));
                }
                mHandler.sendEmptyMessage(UPDATE_ALL);
                break;
        }
    }

    /**
     * This interface must be implemented by activities that contain mContext
     * fragment to allow an interaction in mContext fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public RangeAppAdapter getmRangeAdapter() {
        if (mRangeAdapter == null) {
            mRangeAdapter = new RangeAppAdapter(mContext, mHandler);
        }
        return mRangeAdapter;
    }

    public ProgressBar setmFoldProgressBar(ProgressBar progressBar) {
        return mFoldProgressBar = progressBar;
    }

    /**
     *
     * @param currentIndex 当前卡索引
     * @param isStatsTotal 是否插双卡
     * @param statsDateType  查询时间周期
     * @param dataType  查询网络类型
     */
    public void changeStats(int currentIndex, boolean isStatsTotal, String statsDateType, String dataType) {
        try {
            Log.v(TAG, "mView>>changeStats>>>" + mView);
            if (mView == null) {
                if (mHandler != null) {
                    mHandler.sendEmptyMessageDelayed(DIALOG_STATUS_UPDATE, 800);
                }
                return;
            }
            mPageIndex = 1;
            mIsFreeTag = false;
            mIsStatsTotal = isStatsTotal;
            mAppDefaultDatas.clear();
            mAppForegroundDatas.clear();
            if (mInstance == null) {
                mInstance = DataManagerApplication.getInstance();
            }
            mAppInfosNoPolicy = mRangeAdapter.getmNoPolicyAppList();
            mAppInfos = mRangeAdapter.getmAppList();
            if (mTvReject == null) {
                mTvReject = (TextView) mView.findViewById(R.id.tv_all_reject);
            }
            if (mAppInfosNoPolicy != null && mAppInfosNoPolicy.size() > 0) {
                mTvReject.setText(mInstance.getString(R.string.all_reject));
            }
            if (mAppInfosNoPolicy != null && mAppInfosNoPolicy.size() == 0) {
                mTvReject.setText(mInstance.getString(R.string.all_allow));
            }
            if (mRlView != null) {
                mRlView.setVisibility(View.GONE);
            }
            if (mAppInfos != null && mAppInfos.size() == 0 ) {
                mLayTab.setVisibility(View.GONE);
                if (!TextUtils.equals(dataType, mInstance.getString(R.string.data_wifi))){
                    mLayEmpty.setVisibility(View.VISIBLE);
                } else {
                    mLayEmpty.setVisibility(View.GONE);
                }
            }
            mTvReject.setEnabled(false);
            initStats(currentIndex, isStatsTotal, statsDateType, dataType);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获得定向应用
     * @param imsi
     * @return
     */
    private ArrayList<String> getOrientApps(String imsi) {
        ArrayList<String> addUidList = new ArrayList<String>();
        String addUids = PreferenceUtil.getString(mContext, imsi, PreferenceUtil.ORIENT_APP_ADDED_KEY, "");
        if (addUids.contains(",")) {
            String[] addUidsArray = addUids.split(",");
            if (null != addUidsArray &&  addUidsArray.length > 0) {
                Collections.addAll(addUidList, addUidsArray);
            }
        }
        return addUidList;
    }

    /**
     *
     * @param currentIndex 当前卡索引
     * @param isInit 是否要初始化
     * @param statsDateType  查询时间周期
     * @param dataType  查询网络类型
     */
    private void initStats(int currentIndex, boolean isInit, String statsDateType, String dataType) {
        mStatsDateType = statsDateType;
        mDataType = dataType;
        String simImsi = null;
        if (currentIndex == MainActivity.FIRST_SIM_INDEX) {
            //如果卡1为空则直接统计卡2
            simImsi = ToolsUtil.getActiveSubscriberId(mContext, ToolsUtil.getIdInDbBySimId(mContext, currentIndex));
            if (TextUtils.isEmpty(simImsi)) {
                //如果只有一个sim卡只统计一次
                mIsStatsTotal = false;
                simImsi = ToolsUtil.getActiveSubscriberId(mContext, ToolsUtil.getIdInDbBySimId(mContext, MainActivity.SECOND_SIM_INDEX));
            }
        } else if (currentIndex == MainActivity.SECOND_SIM_INDEX){
            //卡2
            simImsi = ToolsUtil.getActiveSubscriberId(mContext, ToolsUtil.getIdInDbBySimId(mContext, currentIndex));
        }
        if (TextUtils.isEmpty(DataManagerApplication.mImsiArray[0]) && TextUtils.isEmpty(DataManagerApplication.mImsiArray[1])
                && !mInstance.getString(R.string.data_wifi).equals(mDataType)) {
            //非wifi情况下，如果没有sim卡刚清空界面显示
            mRangeAdapter.setAppList(null);
            mRangeAdapter.setDataList(null, null);
            mRangeAdapter.setAppInfosUpdateData(mAppForegroundDatas);
            mRangeAdapter.setAppInfosUpdateData(mAppDefaultDatas);
            if (mHandler != null) {
                mHandler.sendEmptyMessageDelayed(DIALOG_STATUS_UPDATE, 800);
            }
            return;
        }
        mTelephonyManager = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
        mStatsService = INetworkStatsService.Stub.asInterface(ServiceManager.getService(Context.NETWORK_STATS_SERVICE));

        if (mInstance.getString(R.string.data_mobile).equals(mDataType) || mInstance.getString(R.string.net_bg).equals(mDataType)) {
            if (LOGD) LogUtil.d(TAG, "updateBody() mobile tab");

            // Match mobile traffic for mContext subscriber, but normalize it to
            // catch any other merged subscribers.
            mTemplate = buildTemplateMobileAll(simImsi);
            mTemplate = NetworkTemplate.normalize(mTemplate, mTelephonyManager.getMergedSubscriberIds());
        } else if (mInstance.getString(R.string.data_wifi).equals(mDataType)) {
            // wifi doesn't have any controls
            if (LOGD) LogUtil.d(TAG, "updateBody() wifi tab");
            mTemplate = buildTemplateWifiWildcard();

        } else {
            if (LOGD) LogUtil.d(TAG, "updateBody() unknown tab");
            throw new IllegalStateException("unknown tab: " + currentIndex);
        }
        long start = 0;
        long end = 0;
        long startTime = 0;
        long endTime = 0;
        int startHour = 0;
        int startMinute = 0;
        int endHour = 0;
        int endMinute = 0;
        String freeStartTime = mInstance.getString(R.string.un_set);
        String freeEndTime = mInstance.getString(R.string.un_set);
        boolean freeStaus = PreferenceUtil.getBoolean(mContext, simImsi, PreferenceUtil.FREE_DATA_STATE_KEY, false);
        boolean isStatsFree = false; //是否计统计闲时流量
        if (freeStaus) {
            //开始时间
            freeStartTime = PreferenceUtil.getString(mContext, simImsi, PreferenceUtil.FREE_DATA_START_TIME_KEY, mInstance.getString(R.string.un_set));
            //结束时间
            freeEndTime = PreferenceUtil.getString(mContext, simImsi, PreferenceUtil.FREE_DATA_END_TIME_KEY, mInstance.getString(R.string.un_set));
            if (!TextUtils.equals(freeStartTime, mInstance.getString(R.string.un_set)) && !TextUtils.equals(freeEndTime, mInstance.getString(R.string.un_set))) {
                startHour = Integer.parseInt(freeStartTime.split(":")[0]);
                startMinute = Integer.parseInt(freeStartTime.split(":")[1]);
                endHour = Integer.parseInt(freeEndTime.split(":")[0]);
                endMinute = Integer.parseInt(freeEndTime.split(":")[1]);
                //闲时时间
                startTime = Integer.parseInt(startHour + "" + (startMinute == 0 ? "00" : startMinute));
                endTime = Integer.parseInt(endHour + "" + (endMinute == 0 ? "00" : endMinute));
                isStatsFree = true;
            }
        }
        boolean isFreeEnd = true;//闲时统计结束
        if (TextUtils.equals(statsDateType, mInstance.getString(R.string.day_data))) {
            start = StringUtil.getStartTime(0, 0, 0);
            end = StringUtil.getEndTime(23, 59, 59);
            if (isStatsFree) {
                if (startTime >= endTime) {
                    start = StringUtil.getStartTime(endHour, endMinute, 0);
                    end = StringUtil.getEndTime(startHour, startMinute, 0);
                } else {
                    isFreeEnd = false;
                    if (!mIsFreeTag) {
                        //是否结束闲时统计
                        StatsFreeDataTask freeTask = new StatsFreeDataTask(DATA_FREE_STATUS_DAY, simImsi, mTemplate, 0);
                        freeTask.execute();
                    } else {
                        //结束闲时统计, 开始全部流量统计
                        isFreeEnd = true;
                    }
                }
            }
            if (isFreeEnd) {
                new StatsTask(simImsi, start, end, mTemplate).execute();
            }

        } else if (TextUtils.equals(statsDateType, mInstance.getString(R.string.month_data))) {
            //当前月结日
            int closeDay = PreferenceUtil.getInt(mContext, simImsi, PreferenceUtil.CLOSEDAY_KEY, 1);
            long totalData  = PreferenceUtil.getLong(mContext, simImsi, PreferenceUtil.DATAPLAN_COMMON_KEY, 0);
            //若用户没设置流量套餐，流量排行从手机开始使用当天起作统计；
            if (totalData > 0) {
                start = StringUtil.getDayByMonth(closeDay);
            }
            end = StringUtil.getDayByNextMonth(closeDay);
            // new StatsTask(simImsi, start, end, mTemplate).execute();

            if (!isStatsFree) {
                //没有设置闲时
                new StatsTask(simImsi, start, end, mTemplate).execute();
            } else {
                isFreeEnd = false;
                if (!mIsFreeTag) {
                    //是否结束闲时统计
                    StatsFreeDataTask freeTask = new StatsFreeDataTask(DATA_FREE_STATUS_MONTH, simImsi, mTemplate, 0);
                    freeTask.execute();
                } else {
                    //结束闲时统计, 开始全部流量统计
                    isFreeEnd = true;
                }
                if (isFreeEnd) {
                    new StatsTask(simImsi, start, end, mTemplate).execute();
                }
            }
        }

    }

    private class StatsTask extends AsyncTask<NetworkStats, Void, NetworkStats> {
        private String imsi;
        private long start;
        private long end;
        private NetworkTemplate template;

        public StatsTask(String imsi, long start, long end, NetworkTemplate template) {
            this.imsi = imsi;
            this.start = start;
            this.end = end;
            this.template = template;
        }

        @Override
        protected NetworkStats doInBackground(NetworkStats... params) {
            NetworkStats networkStats = null;
            try {
                mStatsService.forceUpdate();
                mStatsSession = mStatsService.openSession();
                networkStats = mStatsSession.getSummaryForAllUid(template, start, end, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return networkStats;
        }

        @Override
        protected void onPostExecute(NetworkStats networkStats) {
            super.onPostExecute(networkStats);
            NetworkStats.Entry entry = null;
            NetworkStats stats = null;
            int statusIndex = 0;
            stats = networkStats;
            int uid = 0;
            final int size = stats != null ? stats.size() : 0;
            long usedData = 0;
            if (TextUtils.equals(imsi, DataManagerApplication.mImsiArray[0])) {
                statusIndex = DataCorrect.FIRST_SIM_INDEX;
            } else if (TextUtils.equals(imsi, DataManagerApplication.mImsiArray[1])) {
                statusIndex = DataCorrect.SECOND_SIM_INDEX;
            }
            ArrayList<String> uidOrientList = getOrientApps(imsi);
            String tvDataType = mDataType;
            String mobile = null;
            String wifi = null;
            String bgData = null;
            if (isAdded()) {
                mobile = mInstance.getString(R.string.data_mobile);
                wifi = mInstance.getString(R.string.data_wifi);
                bgData = mInstance.getString(R.string.net_bg);
            }
            for (int i = 0; i < size; i++) {
                entry = stats.getValues(i, entry);
                uid = entry.uid;
                boolean isExit = false;
                if (!TextUtils.equals(tvDataType, wifi)) {
                    if (null != uidOrientList && uidOrientList.size() > 0) {
                        //过滤定向应用流量
                        for (int j = 0; j < uidOrientList.size(); j++) {
                            if (entry.uid == Integer.parseInt(uidOrientList.get(j).trim())) {
                                isExit = true;
                                break;
                            }
                        }
                    }
                }
                AppItem appItem = new AppItem();
                if (tvDataType.equals(mobile) || tvDataType.equals(wifi)) {
                    //统计前台/后台数据
                    if (!isExit) {
                        usedData = entry.rxBytes + entry.txBytes;
                        if (tvDataType.equals(wifi))  {
                            //wifi使用数据
                            appItem.setAppDataBySim1(usedData);
                        } else {
                            if (statusIndex == DataCorrect.FIRST_SIM_INDEX) {
                                appItem.setAppDataBySim1(usedData);
                            } else if (statusIndex == DataCorrect.SECOND_SIM_INDEX) {
                                appItem.setAppDataBySim2(usedData);
                            }
                        }
                        appItem.setAppUid(uid);
                        mAppForegroundDatas.add(appItem);
                    }
                } else if (NetworkStats.SET_DEFAULT == entry.set && tvDataType.equals(bgData)) {
                    //统计后台数据
                    if (!isExit) {
                        long usedDataBg = entry.rxBytes + entry.txBytes;
                        if (statusIndex == DataCorrect.FIRST_SIM_INDEX) {
                            appItem.setAppDataBySim1(usedDataBg);
                        } else if (statusIndex == DataCorrect.SECOND_SIM_INDEX) {
                            appItem.setAppDataBySim2(usedDataBg);
                        }
                        appItem.setAppUid(uid);
                        mAppDefaultDatas.add(appItem);
                    }
                }
            }
            if (tvDataType.equals(wifi)) {
                mRangeAdapter.setAppInfosUpdateData(mAppForegroundDatas);
                if (mHandler != null) {
                    mHandler.sendEmptyMessageDelayed(DIALOG_STATUS_UPDATE, 800);
                }
                return;
            }
            boolean freeStaus = PreferenceUtil.getBoolean(mContext, imsi, PreferenceUtil.FREE_DATA_STATE_KEY, false);
            boolean freeState = false;//闲时是否跨天
            if (freeStaus && start != end) {
                if(start > end) {
                    freeState = false;
                } else {
                    freeState = true;
                }
            }
            if (mIsStatsTotal) {
                //统计双卡流量总和
                mStatsCount++;
                if (mStatsCount >= DataManagerApplication.mImsiArray.length) {
                    mIsFreeTag = true; //初始化
                    mStatsCount = 0;
                    LogUtil.v(TAG, "entry.rxBytes>>>" + Formatter.formatFileSize(mContext, usedData) + ">>mStatsCount>>>" + mStatsCount);
                } else {
                    //统计第二张sim卡流量排行
                    if (TAB_WIFI.equals(mDataType)) {
                        return;
                    }
                    initStats(mStatsCount, false, mStatsDateType, mDataType);
                    return;
                }
            }
            mIsFreeTag = true;
            //统计单张sim卡流量排行
            if (tvDataType.equals(mobile)) {
                if (freeState) {
                    //过滤闲时流量
                    setStats(mAppForegroundDatas, mFreeAppForeground);
                } else {
                    if (mHandler != null) {
                        mHandler.sendEmptyMessage(DATA_FREE_STATUS_UPDATE);
                        mHandler.sendEmptyMessage(DIALOG_STATUS_UPDATE);
                    }
                }
                //过滤闲时流量
            } else if (tvDataType.equals(wifi)) {
                mHandler.sendEmptyMessage(DATA_FREE_STATUS_UPDATE);
            } else if (tvDataType.equals(bgData)) {
                if (freeState) {
                    //过滤后台闲时流量
                    setStats(mAppDefaultDatas, mFreeAppDefault);
                } else {
                    if (mHandler != null) {
                        mHandler.sendEmptyMessage(DATA_FREE_STATUS_UPDATE);
                        mHandler.sendEmptyMessage(DIALOG_STATUS_UPDATE);
                    }
                }
            }
        }

    }

    private void setStats(final ArrayList<AppItem> srcList, final HashMap<Integer, AppItem> desMap) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                if (srcList == null || srcList.size() == 0 || desMap == null || desMap.size() == 0) {
                    return null;
                }
                LogUtil.e(TAG, "desMap>>>>>>" + desMap.size());
                for (int i = 0; i < srcList.size(); i++) {
                    AppItem srcItem = srcList.get(i);
                    int uid = srcItem.getAppUid();
                    AppItem desItem = desMap.get(uid);
                    if (desItem == null) {
                        continue;
                    }
                    long sim1Data = srcItem.getAppDataBySim1();
                    long sim2Data = srcItem.getAppDataBySim2();
                    long desSim1Data = desItem.getAppDataBySim1();
                    long desSim2Data = desItem.getAppDataBySim2();
                    long sim1Total = (sim1Data - desSim1Data) > 0 ? sim1Data - desSim1Data : 0;
                    long sim2Total = (sim2Data - desSim2Data) > 0 ? sim2Data - desSim2Data : 0;
                    srcItem.setAppDataBySim1(sim1Total);
                    srcItem.setAppDataBySim2(sim2Total);
                }
                desMap.clear();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                mHandler.sendEmptyMessage(DATA_FREE_STATUS_UPDATE);
            }
        }.execute();
    }
    /**
     * 统计闲时流量
     * @author zhaolaichao
     *
     */
    private class StatsFreeDataTask extends android.os.AsyncTask<NetworkStats, Void, NetworkStats> {

        private int statsTag;
        private String activeImsi;
        private NetworkTemplate template;
        private int uid;
        private int startTime = 0;
        private int endTime = 0;
        private HashMap<Integer, AppItem> appItemMap = new HashMap<Integer, AppItem>();
        public StatsFreeDataTask(int statsTag, String activeImsi, NetworkTemplate template, int uid) {
            this.statsTag = statsTag;
            this.activeImsi = activeImsi;
            this.template = template;
            this.uid = uid;
        }

        @Override
        protected NetworkStats doInBackground(NetworkStats... params) {
            NetworkStats statsFreeData = null;
            long freeTotalData = PreferenceUtil.getLong(mContext, activeImsi, PreferenceUtil.FREE_DATA_TOTAL_KEY, 0);
            if (freeTotalData <= 0) {
                return null;
            }
            switch (statsTag) {
                case DATA_FREE_STATUS_MONTH:
                    int closeDay = PreferenceUtil.getInt(mContext, activeImsi, PreferenceUtil.CLOSEDAY_KEY, 1);
                    //从月结日到当前一共多少天
                    int count = StringUtil.getDaysByCloseDay(closeDay);
                    LogUtil.v(TAG, "从月结日到当前一共多少天>>" + count);
                    int total = 0;
                    try {
                        //开始时间
                        String freeStartTime = PreferenceUtil.getString(mContext, activeImsi, PreferenceUtil.FREE_DATA_START_TIME_KEY, getString(R.string.un_set));
                        //结束时间
                        String freeEndTime = PreferenceUtil.getString(mContext, activeImsi, PreferenceUtil.FREE_DATA_END_TIME_KEY, getString(R.string.un_set));
                        if (!TextUtils.equals(freeStartTime, mContext.getString(R.string.un_set)) && !TextUtils.equals(freeEndTime, mContext.getString(R.string.un_set))) {

                            int startHour = Integer.parseInt(freeStartTime.split(":")[0]);
                            int startMinute = Integer.parseInt(freeStartTime.split(":")[1]);
                            int endHour = Integer.parseInt(freeEndTime.split(":")[0]);
                            int endMinute = Integer.parseInt(freeEndTime.split(":")[1]);
                            startTime = Integer.parseInt(startHour + "" + (startMinute == 0 ? "00" : startMinute));
                            endTime = Integer.parseInt(endHour + "" + (endMinute == 0 ? "00" : endMinute));
                            if (total <= count) {
                                if (startTime > endTime) {
                                    statsCommonForDay(true, template, activeImsi, uid, closeDay, count, total, startHour, startMinute, endHour, endMinute);
                                } else {
                                    statsCommonForDay(false, template, activeImsi, uid, closeDay, count, total, startHour, startMinute, endHour, endMinute);
                                }
                                LogUtil.v(TAG, "开时统计每天>>" + total);
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case DATA_FREE_STATUS_DAY:
                    //开始时间
                    String freeStartTime = PreferenceUtil.getString(mContext, activeImsi, PreferenceUtil.FREE_DATA_START_TIME_KEY, mContext.getString(R.string.un_set));
                    //结束时间
                    String freeEndTime = PreferenceUtil.getString(mContext, activeImsi, PreferenceUtil.FREE_DATA_END_TIME_KEY, mContext.getString(R.string.un_set));
                    if (!TextUtils.equals(freeStartTime, mContext.getString(R.string.un_set)) && !TextUtils.equals(freeEndTime, mContext.getString(R.string.un_set))) {
                        int startHour = Integer.parseInt(freeStartTime.split(":")[0]);
                        int startMinute = Integer.parseInt(freeStartTime.split(":")[1]);
                        int endHour = Integer.parseInt(freeEndTime.split(":")[0]);
                        int endMinute = Integer.parseInt(freeEndTime.split(":")[1]);
                        long start = 0;
                        long end = 0;
                        start = StringUtil.getStartTime(startHour, startMinute, 0);
                        end = StringUtil.getEndTime(endHour, endMinute, 0);
                        statusFreeData(template, activeImsi, start, end);
                    }
                    break;
            }
            return statsFreeData;
        }

        @Override
        protected void onPostExecute(NetworkStats result) {
            super.onPostExecute(result);

            if (mIsStatsTotal) {
                //统计双卡流量总和
                mStatsCount++;
                if (mStatsCount >= DataManagerApplication.mImsiArray.length) {
                    mStatsCount = 0;
                    mIsFreeTag = true;
                } else {
                    //统计卡2闲时流量
                    initStats(mStatsCount, false, mStatsDateType, mDataType);
                    return;
                }
            }

            mIsFreeTag = true;
            if (statsTag == DATA_FREE_STATUS_MONTH) {
                if (startTime > endTime) {
                    //闲时跨天
                    ArrayList<AppItem> tempList = null;
                    if (TextUtils.equals(mDataType, mInstance.getString(R.string.data_mobile))) {
                        appItemMap = mFreeAppForeground;
                        tempList = mAppForegroundDatas;
                    } else if(TextUtils.equals(mDataType, mInstance.getString(R.string.net_bg))){
                        appItemMap = mFreeAppDefault;
                        tempList = mAppDefaultDatas;
                    }
                    //合并数据
                    for (Integer key : appItemMap.keySet()) {
                        System.out.println("Key = " + key);
                        AppItem item = appItemMap.get(key);
                        long sim1Data = item.getAppDataBySim1();
                        long sim2Data = item.getAppDataBySim2();
                        AppItem appItem = new AppItem();
                        appItem.setAppUid(key);
                        appItem.setAppDataBySim1(sim1Data);
                        appItem.setAppDataBySim2(sim2Data);
                        tempList.add(appItem);
                    }
                    appItemMap.clear();
                    mHandler.sendEmptyMessage(DATA_FREE_STATUS_UPDATE);
                } else {
                    //闲时不跨天 再次统计总流量
                    initStats(mStatsCount, false, mStatsDateType, mDataType);
                }
            } else {
                //再次统计总流量
                initStats(mStatsCount, false, mStatsDateType, mDataType);
            }
        }

    }
    /**
     * 统计闲时流量 闲时时间跨天
     * @param isNextDay  闲时时间是否跨天
     * @param closeDay
     * @param count
     * @param total
     * @param startHour
     * @param startMinute
     * @param endHour
     * @param endMinute
     * @param freeTotalForMonth
     */
    private void statsCommonForDay(boolean isNextDay, NetworkTemplate template, String imsi, int uid, int closeDay, int count, int total, int startHour, int startMinute, int endHour, int endMinute) {
        try {
            if (mStatsSession == null || template == null) {
                return;
            }
            long start = 0;
            long end = 0;
            if (isNextDay) {
                //统计每天的常规流量
                start = StringUtil.getDayByCloseDay(closeDay + total, endHour, endMinute, 59);
                end  = StringUtil.getDayByCloseDay(closeDay + total, startHour, startMinute, 59);
            } else {
                //统计每天的闲时流量
                start = StringUtil.getDayByCloseDay(closeDay + total, startHour, startMinute, 59);
                end  = StringUtil.getDayByCloseDay(closeDay + total, endHour, endMinute, 59);
            }
            statusFreeData(template, imsi, start, end);

            total ++;
            if (total <= count) {
                statsCommonForDay(isNextDay, template, imsi, uid, closeDay, count, total, startHour, startMinute, endHour, endMinute);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void statusFreeData(NetworkTemplate template, String activeImsi, long start, long end) {
        if (mStatsSession == null || mTemplate == null) {
            return;
        }
        try {

            int statusIndex = 0;
            long usedFreeData = 0;
            NetworkStats.Entry entry = null;
            mStatsService.forceUpdate();
            mStatsSession = mStatsService.openSession();
            NetworkStats networkStats = mStatsSession.getSummaryForAllUid(template, start, end, false);
            int size = networkStats != null ? networkStats.size() : 0;
            if (TextUtils.equals(activeImsi, DataManagerApplication.mImsiArray[0])) {
                statusIndex = DataCorrect.FIRST_SIM_INDEX;
            } else if (TextUtils.equals(activeImsi, DataManagerApplication.mImsiArray[1])) {
                statusIndex = DataCorrect.SECOND_SIM_INDEX;
            }
            String mobile = null;
            String bgData = null;
            if (isAdded()) {
                mobile = mInstance.getString(R.string.data_mobile);
                bgData = mInstance.getString(R.string.net_bg);
            }
            LogUtil.e(TAG, "freeData>>>" + size);
            if (null != networkStats) {
                for (int i = 0; i < size; i++) {
                    entry = networkStats.getValues(i, entry);
                    //去除定向流量
                    boolean isExit = false;
                    int appUid = entry.uid;
                    ArrayList<String> orientAppUids = getOrientApps(activeImsi);
                    //统计前台和后台数据
                    if (null != orientAppUids && orientAppUids.size() > 0) {
                        //过滤定向应用流量
                        for (int j = 0; j < orientAppUids.size(); j++) {
                            if (entry.uid == Integer.parseInt(orientAppUids.get(j))) {
                                isExit = true;
                                break;
                            }
                        }
                    }
                    if (!isExit) {
                        usedFreeData = usedFreeData + entry.rxBytes + entry.txBytes;
                    }
                    LogUtil.e(TAG, ">>freeData>SET_DEFAULT>>>FREE>>" + entry);
                    AppItem appItemFree = null;
                    if (TextUtils.equals(mDataType, mobile)) {
                        appItemFree = mFreeAppForeground.get(appUid);
                        if (appItemFree == null) {
                            appItemFree = new AppItem();
                        }
                        if (!isExit) {
                            long usedDataBg = entry.rxBytes + entry.txBytes;
                            if (statusIndex == DataCorrect.FIRST_SIM_INDEX) {
                                long lastSim1 = appItemFree.getAppDataBySim1();
                                appItemFree.setAppDataBySim1(lastSim1 + usedDataBg);
                            } else if (statusIndex == DataCorrect.SECOND_SIM_INDEX) {
                                long lastSim2 = appItemFree.getAppDataBySim2();
                                appItemFree.setAppDataBySim2(lastSim2 + usedDataBg);
                            }
                            appItemFree.setAppUid(appUid);
                            //mFreeAppForeground.add(appItemFree);
                            mFreeAppForeground.put(appUid, appItemFree);
                        }
                    } else if (NetworkStats.SET_DEFAULT == entry.set && TextUtils.equals(mDataType, bgData)) {
                        //统计后台数据
                        appItemFree = mFreeAppDefault.get(appUid);
                        if (appItemFree == null) {
                            appItemFree = new AppItem();
                        }
                        if (!isExit) {
                            long usedDataBg = entry.rxBytes + entry.txBytes;
                            if (statusIndex == DataCorrect.FIRST_SIM_INDEX) {
                                long lastSim1 = appItemFree.getAppDataBySim1();
                                appItemFree.setAppDataBySim1(lastSim1 + usedDataBg);
                            } else if (statusIndex == DataCorrect.SECOND_SIM_INDEX) {
                                long lastSim2 = appItemFree.getAppDataBySim2();
                                appItemFree.setAppDataBySim2(lastSim2 + usedDataBg);
                            }
                            appItemFree.setAppUid(appUid);
                            mFreeAppDefault.put(appUid, appItemFree);
                        }
                    }
                }
            }

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}

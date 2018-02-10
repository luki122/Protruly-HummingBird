package com.protruly.powermanager.powersave;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.protruly.powermanager.R;
import com.protruly.powermanager.purebackground.Info.AppInfo;
import com.protruly.powermanager.purebackground.Info.AppsInfo;
import com.protruly.powermanager.purebackground.Info.ItemInfo;
import com.protruly.powermanager.purebackground.interfaces.Observer;
import com.protruly.powermanager.purebackground.interfaces.Subject;
import com.protruly.powermanager.purebackground.model.ConfigModel;
import com.protruly.powermanager.purebackground.model.ForbitAlarmModel;
import com.protruly.powermanager.purebackground.provider.ForbitAlarmAppProvider;
import com.protruly.powermanager.utils.ApkUtils;
import com.protruly.powermanager.utils.LogUtils;
import com.protruly.powermanager.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import hb.app.HbActivity;
import hb.app.dialog.ProgressDialog;
import hb.widget.HbIndexBar;
import hb.widget.Switch;
import hb.widget.HbIndexBar.Letter;
import hb.widget.HbIndexBar.OnSelectListener;
import hb.widget.HbIndexBar.OnTouchStateChangedListener;
import hb.widget.HbIndexBar.TouchState;
import hb.widget.HbListView;
import hb.widget.toolbar.Toolbar;
public class ForbitAlarmsActivity extends HbActivity implements OnClickListener, OnScrollListener,
        OnItemClickListener, OnSelectListener, OnTouchStateChangedListener, Observer,
        LoaderManager.LoaderCallbacks<Object> {

    private static final String TAG = "ForbitAlarms";

    private static final boolean DEBUG = true;

    private TextView tvMenu;
    private AppAdapter mAdapter;
    private HbListView mListView;
    private HbIndexBar mIndexBar;
    private ProgressBar mProgressBar;
    /** 通过Loader加载完后的数据，转换为ItemInfo，保存在List对象中 */
    private List<ItemInfo> mAppList = new ArrayList<>();

    private boolean isAllOpened = false;

    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHbContentView(R.layout.activity_forbit_alarm);

        initView();
        initData();
    }

    private void initView() {
        initToolbar();

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        mListView = (HbListView) findViewById(R.id.list);
        mListView.setOnScrollListener(this);
        mListView.setOnItemClickListener(this);
        LinearLayout header = (LinearLayout) getLayoutInflater().inflate(R.layout.app_list_header,
                null);
        mListView.addHeaderView(header);

        mIndexBar = (HbIndexBar) findViewById(R.id.index_bar);
        mIndexBar.deleteLetter(0);
        mIndexBar.setOnSelectListener(this);
        mIndexBar.setOnTouchStateChangedListener(this);
    }

    private void initToolbar() {
        View menu = getLayoutInflater().inflate(R.layout.app_menu, null);
        tvMenu = (TextView) menu.findViewById(R.id.tvMenu);
        tvMenu.setOnClickListener(this);
        Toolbar.LayoutParams params = new Toolbar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.TOP | Gravity.RIGHT;
        getToolbar().addView(menu, params);

        getToolbar().setTitle(R.string.power_forbit_alarms);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        checkAll();
    }


    private void initData() {
        mProgressBar.setVisibility(View.VISIBLE);

        ConfigModel.getInstance(this).getAppInfoModel().attach(this); // loadAllAppInfo
        getLoaderManager().initLoader(AppLoader.ID_LOADER_USER_APP, null, this);
    }

    /**
     * set tvMenu status
     */
    private void checkAll() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }

        if (ForbitAlarmModel.getInstance(this).isAllAppOpened(this)) {
            isAllOpened = true;
            tvMenu.setText(R.string.app_menu_title_close);
        } else {
            isAllOpened = false;
            tvMenu.setText(R.string.app_menu_title_open);
        }
    }

    private void setAll() {
        if (mAppList == null) {
            return;
        }
        Task t = new Task();
        t.execute();
    }


    @Override
    public void onSelect(int arg0, int arg1, Letter letter) {
        int listIndex = letter.list_index;

        mListView.setSelection(listIndex + 1);
    }

    @Override
    public void onStateChanged(TouchState state0, TouchState state1) {
    }

    private void setListHeadText() {
        ((TextView) ((mListView.getAdapter().getView(0, null, mListView))
                .findViewById(R.id.list_head_text))).setText(getResources().getString(
                R.string.forbit_alarms_summary,
                ForbitAlarmAppProvider.getForbitAlarmAppList(ForbitAlarmsActivity.this).size()));
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        if (mAdapter != null) {
            AppInfo appInfo = (AppInfo) mAdapter.getItem(firstVisibleItem);
            if (appInfo != null) {
                int index = -1;
                index = mIndexBar.getIndex(appInfo.getAppNamePinYin().substring(0, 1));
                LogUtils.d(TAG, "onScroll() -> index = " + index);
                if (index == -1) {
                    index = mIndexBar.size() - 1;
                }
                mIndexBar.setFocus(index);
            }
        }
    }

    class Task extends AsyncTask<List<ItemInfo>, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(ForbitAlarmsActivity.this);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setTitle(getString(R.string.action_in_progress));
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(List<ItemInfo>... params) {
            for (int i = 0; i < mAppList.size(); i++) {
                AppInfo appInfo = (AppInfo) mAppList.get(i);
                if (isAllOpened) {
                    ForbitAlarmModel.getInstance(ForbitAlarmsActivity.this).tryChangeForbitAlarmState(
                            appInfo.getPackageName(), false);
                } else {
                    ForbitAlarmModel.getInstance(ForbitAlarmsActivity.this).tryChangeForbitAlarmState(
                            appInfo.getPackageName(), true);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mAdapter.notifyDataSetChanged();
            setListHeadText();
            checkAll();
        }
    }

    /**
     * 将mAppList中的ItemInfo对象按拼音字母顺序排序
     * 
     * @param appsList
     */
    private void sortList(List<ItemInfo> appsList) {
        Collections.sort(appsList, new Comparator<ItemInfo>() {
            public int compare(ItemInfo s1, ItemInfo s2) {
                return Utils.compare(((AppInfo) s1).getAppNamePinYin(),
                        ((AppInfo) s2).getAppNamePinYin());
            }
        });
    }

    public void initOrUpdateListData() {
        mProgressBar.setVisibility(View.GONE);
        if (mListView == null) {
            return;
        }
        LogUtils.d(TAG, "initOrUpdateListData()");

        sortList(mAppList);

        if (mAdapter == null) {
            mAdapter = new AppAdapter(this, mAppList);
            mListView.setAdapter(mAdapter);
        } else {
            mAdapter.notifyDataSetChanged();
        }

        initIndexBar();

        TextView header = ((TextView) ((mListView.getAdapter().getView(0, null, mListView))
                .findViewById(R.id.list_head_text)));
        header.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        header.setText(getResources().getString(R.string.forbit_alarms_summary,
                ForbitAlarmAppProvider.getForbitAlarmAppList(this).size()));

        if (mAppList == null || mAppList.size() == 0) {
            findViewById(R.id.NoAppLayout).setVisibility(View.VISIBLE);
            findViewById(R.id.list).setVisibility(View.GONE);
            findViewById(R.id.index_bar).setVisibility(View.GONE);
            tvMenu.setVisibility(View.GONE);
        } else {
            findViewById(R.id.NoAppLayout).setVisibility(View.GONE);
            findViewById(R.id.list).setVisibility(View.VISIBLE);
            findViewById(R.id.index_bar).setVisibility(View.VISIBLE);
            tvMenu.setVisibility(View.VISIBLE);
        }
    }

    private void releaseObject() {
        if (mAppList != null) {
            mAppList.clear();
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tvMenu:
                setAll();
            break;

            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        releaseObject();
        ConfigModel.getInstance(this).getAppInfoModel().detach(this);
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mAdapter == null) {
            return;
        }
    }

    private void initIndexBar() {
        int lastIndex = -1;
        for (int i = 0; i < mAppList.size(); i++) {
            AppInfo appInfo = (AppInfo) mAppList.get(i);
            int index = mIndexBar.getIndex(appInfo.getAppNamePinYin().substring(0, 1));
            LogUtils.d(TAG, "initIndexBar() -> AppName = " + appInfo.getAppName() + ", index = "
                    + index + ", lastIndex = " + lastIndex);

            if (index == -1) {
                index = mIndexBar.size() - 1;
            }
            if (index == lastIndex) {
                continue;
            } else {
                lastIndex = index;
            }
            HbIndexBar.Letter letter = mIndexBar.getLetter(index);
            if (letter != null) {
                letter.list_index = i;
            }

            mIndexBar.setEnables(true, index);
        }
    }

    class AppAdapter extends ArrayAdapter<ItemInfo> implements OnCheckedChangeListener {
        private Activity activity;

        AppAdapter(Activity activity, List<ItemInfo> listData) {
            super(activity, 0, listData);
            this.activity = activity;
        }

        @Override
        public View getView(int position, View convertView, final ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = activity.getLayoutInflater();
                convertView = inflater.inflate(R.layout.app_list_item, parent, false);
                holder = new ViewHolder();
                holder.appIcon = (ImageView) convertView.findViewById(R.id.icon);
                holder.appName = (TextView) convertView.findViewById(R.id.appName);
                holder.appSwitch = (Switch) convertView.findViewById(R.id.appSwitch);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            if (getCount() <= position) {
                return convertView;
            }

            AppInfo curAppInfo = (AppInfo) getItem(position);
            holder.appName.setText(curAppInfo.getAppName());
            holder.appSwitch.setOnCheckedChangeListener(null);
            holder.appIcon.setImageDrawable(curAppInfo.getIconDrawable());
            if (ForbitAlarmAppProvider.isInForbitAlarmAppList(activity, curAppInfo.getPackageName())) {
                holder.appSwitch.setChecked(true);
            } else {
                holder.appSwitch.setChecked(false);
            }
            holder.appSwitch.setTag(curAppInfo);
            holder.appSwitch.setOnCheckedChangeListener(this);

            return convertView;
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            AppInfo curAppInfo = (AppInfo) buttonView.getTag();
            if (curAppInfo == null) {
                return;
            }

            switch (buttonView.getId()) {
            case R.id.appSwitch:
                ForbitAlarmModel.getInstance(activity).tryChangeForbitAlarmState(
                        curAppInfo.getPackageName(), isChecked);

                setListHeadText();

                checkAll();
                break;
            default:
                break;
            }
        }

        final class ViewHolder {
            ImageView appIcon;
            TextView appName;
            Switch appSwitch;
        }
    }

    @Override
    public void updateOfInit(Subject subject) {
    }

    @Override
    public void updateOfInStall(Subject subject, String pkgName) {
        getLoaderManager().restartLoader(AppLoader.ID_LOADER_USER_APP, null, this);
    }

    @Override
    public void updateOfCoverInStall(Subject subject, String pkgName) {
    }

    @Override
    public void updateOfUnInstall(Subject subject, String pkgName) {
        getLoaderManager().restartLoader(AppLoader.ID_LOADER_USER_APP, null, this);
    }

    @Override
    public void updateOfExternalAppAvailable(Subject subject, List<String> pkgList) {
    }

    @Override
    public void updateOfExternalAppUnAvailable(Subject subject, List<String> pkgList) {
    }

    @Override
    public Loader<Object> onCreateLoader(int id, Bundle args) {
        return new AppLoader(ForbitAlarmsActivity.this);
    }

    /**
     * 创建Loader，数据加载完毕后，调用onLoadFinished方法
     */
    @Override
    public void onLoadFinished(Loader<Object> loader, Object data) {
        if (loader.getId() == AppLoader.ID_LOADER_USER_APP) {
            LogUtils.d(TAG, "onLoadFinished() -> ID_LOADER_USER_APP");

            if (data != null && mAppList != null) {
                mAppList.clear();
                mAppList.addAll((List<ItemInfo>) data);
            }

            initOrUpdateListData();
        }
    }

    @Override
    public void onLoaderReset(Loader<Object> loader) {
    }

    static class AppLoader extends AsyncTaskLoader<Object> {
        private Context mContext;
        private List<ItemInfo> AppList = new ArrayList<ItemInfo>();

        public static final int ID_LOADER_USER_APP = 1;

        public AppLoader(Context context) {
            super(context);
            mContext = context;
        }

        /**
         * 在后台加载所有应用
         */
        @Override
        public Object loadInBackground() {
            if (DEBUG) {
                LogUtils.d(TAG, "AsyncTaskLoader, loadInBackground for user apps.");
            }

            Object result = null;

            switch (getId()) {
            case ID_LOADER_USER_APP:
                result = loadUserAppList();
                break;
            default:
                break;
            }

            return result;
        }

        private List<ItemInfo> loadUserAppList() {
            if (AppList != null) {
                AppList.clear();
            }

            AppsInfo userAppsInfo = ConfigModel.getInstance(mContext).getAppInfoModel()
                    .getThirdPartyAppsInfo();
            if (userAppsInfo == null) {
                return null;
            }

            for (int i = 0; i < userAppsInfo.size(); i++) {
                AppInfo appInfo = (AppInfo) userAppsInfo.get(i);
                if (appInfo == null || !appInfo.getIsInstalled()) {
                    continue;
                }

                LogUtils.d(TAG, "loadUserAppList() -> " + i + " : AppName = " + appInfo.getAppName()
                                + ", AppNamePinYin = " + appInfo.getAppNamePinYin());
                ApkUtils.initAppNameInfo(mContext, appInfo);
                AppList.add(appInfo);
            }

            return AppList;
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
            super.onStartLoading();
        }
    }
}

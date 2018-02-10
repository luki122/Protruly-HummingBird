package cn.com.protruly.filemanager.HomePage;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.app.FragmentTransaction;
import android.os.storage.StorageManager;
import android.os.storage.DiskInfo;
import android.os.storage.VolumeInfo;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import cn.com.protruly.filemanager.StorageVolumeManager;
import cn.com.protruly.filemanager.enums.Category;
import cn.com.protruly.filemanager.enums.CategoryInfo;
import cn.com.protruly.filemanager.enums.FileInfo;
import cn.com.protruly.filemanager.format.StorageFormatter;
import cn.com.protruly.filemanager.BaseFragment;
import cn.com.protruly.filemanager.historyList.HistoryFragment;
import cn.com.protruly.filemanager.pathFileList.PathListFragment;
import cn.com.protruly.filemanager.globalsearch.GlobalSearchHistoryFragment;
import cn.com.protruly.filemanager.ui.ProgressDialogManager;
import cn.com.protruly.filemanager.utils.FilePathUtil;
import cn.com.protruly.filemanager.utils.GlobalConstants;
import cn.com.protruly.filemanager.historyList.HistoryListAdapter;
import cn.com.protruly.filemanager.utils.IntentBuilderUtil;
import cn.com.protruly.filemanager.utils.MediaDatabaseDao;
import cn.com.protruly.filemanager.ui.ToolbarManager;
import cn.com.protruly.filemanager.categorylist.CategoryFragment;
import cn.com.protruly.filemanager.utils.LogUtil;
import cn.com.protruly.filemanager.utils.Util;
import android.os.storage.StorageEventListener;
import hb.app.dialog.AlertDialog;
import hb.widget.HbListView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import cn.com.protruly.filemanager.R;
/**
 * Created by liushitao on 17-4-14.
 */

public class HomeFragment extends BaseFragment implements View.OnClickListener,AdapterView.OnItemClickListener, StorageFormatter.StorageFormatListener, StorageVolumeManager.StorageChangeListener{

    private static final String TAG = "HomeFragment";
    private MediaDatabaseDao mediaDatabaseDao;
    private HbListView historyListView;
    private HistoryListAdapter historyListAdapter;
    private boolean mViewHasDestoyed;
    private List<HomeAsyncTask> mAsyncTaskList;
    private List<FileInfo> historyList = new ArrayList<>();
    private List<Integer> mCategoryList;
    private SparseArray<CategoryInfo> mCategorySizeInfoMap;
    private SparseArray<TextView> mCategoryTextViewMap;
    private Button moreButton;
    private TextView historyLable;
    private TextView line;
    private HbListView storageListView;
    private StorageListAdapter mStorageListAdapter;
    private boolean isFirstRefresh;
    //private StorageManager mStorageManager;
    private HomeCategoryContentObserver mContentObserver;
    private ProgressDialogManager mProgressDialogManager;
    private TextView mDocSizeInfo;
    private TextView mMusSizeInfo;
    private TextView mPicSizeInfo;
    private TextView mVidSizeInfo;
    private TextView mZipSizeInfo;
    private TextView mApkSizeInfo;
    private StorageVolumeManager mStorageVolumeManager;
    private AsyncTask<Void, Void, ArrayList<FileInfo>> mCurrentTask;

    protected void initObjOnCreate(Bundle bundle) {
        super.initObjOnCreate(bundle);
        mAsyncTaskList = new ArrayList<>();
        mCategorySizeInfoMap = new SparseArray<>();
        mCategoryTextViewMap = new SparseArray<>();
        initCategoryList();
        initCategorySizeInfoMap();
        mStorageListAdapter = new StorageListAdapter(mContext, Util.getStoragePathListSecond(mContext));
        historyListAdapter = new HistoryListAdapter(mContext, historyList,true);
        mContentObserver = new HomeCategoryContentObserver(new Handler());
        mediaDatabaseDao = new MediaDatabaseDao(mContext);
        isFirstRefresh = true;
        mStorageVolumeManager = StorageVolumeManager.getInstance();
        /*if(mContext!=null) {
            mStorageManager = mContext.getSystemService(StorageManager.class);
        }*/
    }

    /*private final StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            LogUtil.i(TAG, "StorageEventListener----onVolumeStateChanged vol:" + vol + " oldState:" + oldState + " newState:" + newState);
            if(null==vol){
                return;
            }
            /*
            *   public static final int STATE_UNMOUNTED = 0;
                public static final int STATE_CHECKING = 1;
                public static final int STATE_MOUNTED = 2;
                public static final int STATE_MOUNTED_READ_ONLY = 3;
                public static final int STATE_FORMATTING = 4;
                public static final int STATE_EJECTING = 5;
                public static final int STATE_UNMOUNTABLE = 6;
                public static final int STATE_REMOVED = 7;
                public static final int STATE_BAD_REMOVAL = 8;
                public static final int STATE_MEDIA_SHARED = 9;

                当拔出卡槽时，走  STATE_MOUNTED = 2-----》STATE_EJECTING = 5；
                              STATE_EJECTING = 5;----》STATE_UNMOUNTED = 0;
                              STATE_UNMOUNTED = 0;----》STATE_BAD_REMOVAL = 8;
                当插入卡槽时，走 STATE_UNMOUNTED = 0;-----》STATE_CHECKING = 1;
                                STATE_CHECKING = 1;-----》STATE_MOUNTED = 2;
                当点击通知栏和设置的卸载：STATE_MOUNTED = 2-----》STATE_EJECTING = 5；
                              STATE_EJECTING = 5;----》STATE_UNMOUNTED = 0;
                当点击通知栏和设置的安装：STATE_UNMOUNTED = 0;-----》STATE_CHECKING = 1;
                                STATE_CHECKING = 1;-----》STATE_MOUNTED = 2;
             */

            /*if(oldState == VolumeInfo.STATE_MOUNTED && newState == VolumeInfo.STATE_EJECTING ) {
                LogUtil.i(TAG, "qqqqqqqqqqqqqqq");
                refreshStorageList();
            } else if(oldState == VolumeInfo.STATE_CHECKING && newState == VolumeInfo.STATE_MOUNTED) {
                LogUtil.i(TAG, "aaaaaaaaaaaaaaaaa");
                refreshStorageList();
            }
        }
    };*/

    /*private static boolean isInteresting(VolumeInfo vol) {
        switch(vol.getType()) {
            case VolumeInfo.TYPE_PRIVATE:
            case VolumeInfo.TYPE_PUBLIC:
                return true;
            default:
                return false;
        }
    }
    private static boolean isUsbOrSd(DiskInfo diskInfo) {
        if(diskInfo.isUsb()||diskInfo.isSd()){
            return true;
        }
        return false;
    }*/

    private void initCategoryList(){
        mCategoryList = new ArrayList<>();
        mCategoryList.add(Category.Apk);
        mCategoryList.add(Category.Document);
        mCategoryList.add(Category.Music);
        mCategoryList.add(Category.Picture);
        mCategoryList.add(Category.Video);
        mCategoryList.add(Category.Zip);
    }

    private void initCategorySizeInfoMap(){
        for(int i=0;i<mCategoryList.size();i++){
            int category = mCategoryList.get(i);
            CategoryInfo categoryInfo = mCategorySizeInfoMap.get(category);
            if(null==categoryInfo){
                CategoryInfo info = new CategoryInfo();
                mCategorySizeInfoMap.put(category,info);
            }
        }
    }

    protected void initRootView(LayoutInflater inflater,ViewGroup container) {
        if(container == null ||
                (container.getChildCount() < 1 && rootView == null)) {
            rootView = inflater.inflate(R.layout.home_category_fargment_layout, container, false);
        }
    }

    protected void getInitData() {
        if(!mViewHasDestoyed) {
            newAsyncTask();
            gethistoryList();
            return;
        }
        fillCategoryTextView();
    }

    @Override
    public void onStorageFormatStart(String path) {
        LogUtil.i(TAG, "HomeFragment onStorageFormatStart path:" + path);
        if(mProgressDialogManager == null) {
            mProgressDialogManager = new ProgressDialogManager(mContext);
        }
        mProgressDialogManager.setProgress(0);
    }

    @Override
    public void onStorageFormatProgress(String path, int progress) {
        LogUtil.i(TAG, "HomeFragment onStorageFormatProgress path:" + path + " progress:" + progress);
        if(mProgressDialogManager != null) {
            mProgressDialogManager.setProgress(progress);
        }
    }

    @Override
    public void onStorageFormatSucceeded(String path) {
        LogUtil.i(TAG, "HomeFragment onStorageFormatSucceeded path:" + path);
        if(mProgressDialogManager != null) {
            mProgressDialogManager.dismissProgressDialog();
        }
    }

    @Override
    public void onStorageFormatFailed(String path) {
        LogUtil.i(TAG, "HomeFragment onStorageFormatFailed path:" + path);
        if(mProgressDialogManager != null) {
            mProgressDialogManager.dismissProgressDialog();
        }
    }

    @Override
    public void OnStorageMounted() {
        LogUtil.i(TAG, "HomeFragment OnStorageMounted");
        refreshStorageList();
    }

    @Override
    public void OnStorageEjected() {
        LogUtil.i(TAG, "HomeFragment OnStorageEjected");
        refreshStorageList();
    }

    public class GetHistoryListTask extends AsyncTask<Void, Void, ArrayList<FileInfo>> {

        @Override
        protected ArrayList<FileInfo> doInBackground(Void... params) {
            Log.d(TAG,"task:"+ Thread.currentThread()+",name:"+Thread.currentThread().getName());
            Cursor cursor = mediaDatabaseDao.getHomeHistoryInfo(mContext);
            int num = 4 - Util.getStoragePathListSecond(mContext).size();
            if(num <= 0) num = 1;
            if(cursor==null){
                return null;
            }
            ArrayList<FileInfo> pictureList = new ArrayList<FileInfo>();
            while (pictureList.size()<=num-1 && cursor.moveToNext()) {
                String data = cursor.getString(cursor.getColumnIndex("_data"));
                FileInfo info = new FileInfo(data);
                info.filePath = data;
                info.fileName = FilePathUtil.getFileNameAndExtension(info.filePath);
                info.isFile = !"12289".equals(cursor.getString(cursor.getColumnIndex("format")));
                pictureList.add(info);
            }
            cursor.close();
            return pictureList;
        }

        @Override
        protected void onPostExecute(ArrayList<FileInfo> fileInfos) {
            if(null == fileInfos || null == historyListAdapter){
                return;
            }else if(0 == fileInfos.size()){
                Util.hideView(historyLable);
                Util.hideView(moreButton);
                Util.showView(line);
            }else {
                Util.showView(historyLable);
                Util.showView(moreButton);
                Util.hideView(line);
            }
            historyListAdapter.setList(fileInfos);
            historyListAdapter.notifyDataSetChanged();
        }
    };

    public boolean isTaskInProgress() {
        if(null == mCurrentTask) return false;
        return mCurrentTask.getStatus() != AsyncTask.Status.FINISHED;
    }

    private void gethistoryList(){
        if(isTaskInProgress()) return;
        mCurrentTask = new GetHistoryListTask();
        mCurrentTask.execute();
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    private void fillCategoryTextView() {
        Set<Integer> keySet = new HashSet<>();
        keySet.add(Category.Document);
        keySet.add(Category.Apk);
        keySet.add(Category.Music);
        keySet.add(Category.Video);
        keySet.add(Category.Zip);
        keySet.add(Category.Picture);
        for(Integer category : keySet) {
            setViewText(mCategoryTextViewMap.get(category), mCategorySizeInfoMap.get(category).count);
        }
    }

    private void setViewText(TextView view, long count) {
        if(view==null){
            return;
        }
        view.setText(String.valueOf(count));
    }
    @Override
    protected void initViewOnCreateView(){
        initTextViews();
        addTextViewsToMap();
        initStorageListView();
        initHistoryListView();
        setClickListener();
    }

    private void initTextViews(){
        if(rootView==null){
            return;
        }
        mPicSizeInfo = (TextView)rootView.findViewById(R.id.picture_size_info);
        mMusSizeInfo = (TextView)rootView.findViewById(R.id.music_size_info);
        mVidSizeInfo = (TextView)rootView.findViewById(R.id.video_size_info);
        mDocSizeInfo = (TextView)rootView.findViewById(R.id.document_size_info);
        mZipSizeInfo = (TextView)rootView.findViewById(R.id.zip_size_info);
        mApkSizeInfo = (TextView)rootView.findViewById(R.id.apk_size_info);
        moreButton = (Button) rootView.findViewById(R.id.more_category_history);
        historyLable = (TextView) rootView.findViewById(R.id.category_history_lable);
        line = (TextView) rootView.findViewById(R.id.line);
    }

    private void addTextViewsToMap() {
        mCategoryTextViewMap.put(Category.Picture, mPicSizeInfo);
        mCategoryTextViewMap.put(Category.Music, mMusSizeInfo);
        mCategoryTextViewMap.put(Category.Video, mVidSizeInfo);
        mCategoryTextViewMap.put(Category.Document, mDocSizeInfo);
        mCategoryTextViewMap.put(Category.Zip, mZipSizeInfo);
        mCategoryTextViewMap.put(Category.Apk, mApkSizeInfo);
    }

    private void showCannotFormatTip() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.format_tip_title);
        builder.setMessage(R.string.cannot_format_internal_storage);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.create().show();
    }

    private void initStorageListView(){
        if(rootView==null){
            return;
        }
        storageListView = (HbListView) rootView.findViewById(R.id.storage_path_list);
        storageListView.setOnItemClickListener(this);

        storageListView.setLongClickable(true);
        storageListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                if(0 == position) {
                    showCannotFormatTip();
                    return true;
                }

                final FileInfo storage = (FileInfo)mStorageListAdapter.getItem(position);
                LogUtil.i("Format", "storageListView onItemLongClick storagePath:" + storage.filePath);

                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle(R.string.format_tip_title);
                builder.setMessage(R.string.format_storage_tip);
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        StorageFormatter formatter = new StorageFormatter(mContext, HomeFragment.this);
                        //formatter.format(storage.filePath);
                        //formatter.unmount(storage.filePath);
                        formatter.doFormat(storage.filePath);
                    }
                });
                builder.create().show();
                return true;
            }
        });

        storageListView.setAdapter(mStorageListAdapter);
    }

    private void initHistoryListView(){
        if(rootView==null){
            return;
        }
        historyListView = (HbListView) rootView.findViewById(R.id.home_history_list);
        historyListView.setLongClickable(false);
        historyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FileInfo localFileInfo = (FileInfo)parent.getItemAtPosition(position);
                viewFile(localFileInfo.getPath());
            }
        });
        historyListView.setAdapter(historyListAdapter);
    }

    private void viewFile(String file) {
        Intent intent = IntentBuilderUtil.buildIntent(file,mContext);
        String type = intent.getType();
        if(type != null) {
            Util.startActivityForType(mContext,intent);
        }
    }

    private void setClickListener() {
        setCategoryClickListener(R.id.category_apk);
        setCategoryClickListener(R.id.category_document);
        setCategoryClickListener(R.id.category_music);
        setCategoryClickListener(R.id.category_picture);
        setCategoryClickListener(R.id.category_video);
        setCategoryClickListener(R.id.category_zip);
        if(moreButton==null){
            return;
        }
        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HistoryFragment fragment = new HistoryFragment();
                String tag = fragment.getClass().getSimpleName();
                FragmentTransaction transaction = getActivity().getFragmentManager().beginTransaction();
                transaction.addToBackStack(tag);
                transaction.replace(R.id.fragment_container,fragment,fragment.getClass().getSimpleName());
                transaction.commitAllowingStateLoss();
            }
        });
    }

    private void setCategoryClickListener(int categoryId) {
        if(rootView==null){
            return;
        }
        rootView.findViewById(categoryId).setOnClickListener(this);
    }

    public void onResume(){
        super.onResume();
        LogUtil.i(TAG, "onResume");
        mStorageVolumeManager.setStorageChangeListener(this);
        registerObserver();
        /*if(null != mStorageManager) {
            LogUtil.i(TAG, "registerListener");
            mStorageManager.registerListener(mStorageListener);
        }*/
        getToolbarManager().switchToStatus(ToolbarManager.STATUS_MAIN_PAGE);
        if(isFirstRefresh) {
            isFirstRefresh = false;
            return;
        }
        refresh();
    }

    @Override
    public void onPause(){
        super.onPause();
        LogUtil.i(TAG, "onPause");
        /*if(null != mStorageManager) {
            LogUtil.i(TAG, "unregisterListener");
            mStorageManager.unregisterListener(mStorageListener);
        }*/
        unregisterObserver();
    }

    public void refresh(){
        mStorageListAdapter = new StorageListAdapter(mContext, Util.getStoragePathListSecond(mContext));
        if(storageListView==null){
            return;
        }
        storageListView.setAdapter(mStorageListAdapter);
        mStorageListAdapter.notifyDataSetChanged();
        newAsyncTask();
        gethistoryList();
    }

    public void refreshStorageList(){
        mStorageListAdapter = new StorageListAdapter(mContext, Util.getStoragePathListSecond(mContext));
        if(storageListView==null){
            return;
        }
        storageListView.setAdapter(mStorageListAdapter);
        mStorageListAdapter.notifyDataSetChanged();
        gethistoryList();
    }

    public void refreshCategoryNum(){
        newAsyncTask();
    }


    @Override
    public int getFragmentId() {
        return 0;
    }

    public void onDestroyView() {
        super.onDestroyView();
        mViewHasDestoyed = true;
    }

    public void onDestroy() {
        super.onDestroy();
        cancelAsyncTasks();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if(item.getItemId() == R.id.action_main_page_search){
            android.util.Log.d("wenwenchao","main_page_search icon is clicked");
            GlobalSearchHistoryFragment gsfragment  = new GlobalSearchHistoryFragment();
            FragmentTransaction transaction = getActivity().getFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container,gsfragment,gsfragment.getClass().getSimpleName());
            transaction.commitAllowingStateLoss();

        }
        return false;
    }

    @Override
    public void onClick(View v) {
        CategoryFragment fragment = new CategoryFragment();
        Bundle bundle = new Bundle();
        int categoryType = -1;
        switch(v.getId()){
            case R.id.category_picture:
                categoryType = Category.Picture;
                break;
            case R.id.category_music:
                categoryType = Category.Music;
                break;
            case R.id.category_video:
                categoryType = Category.Video;
                break;
            case R.id.category_apk:
                categoryType = Category.Apk;
                break;
            case R.id.category_zip:
                categoryType = Category.Zip;
                break;
            case R.id.category_document:
                categoryType = Category.Document;
                break;
            default:
                //fragment = new CategoryListFragment();
                break;
        }
        bundle.putInt(Category.TAG, categoryType);
        fragment.setArguments(bundle);
        startCategoryFragment(fragment);

        //((CategoryActivity)getActivity()).getToolbar().setTitle(titleResId);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FileInfo fileInfo = (FileInfo)parent.getItemAtPosition(position);
        if(parent.getId()== R.id.storage_path_list){
            onStoragePathClick(fileInfo);
        }

    }

    private void onStoragePathClick(FileInfo paramFileInfo) {
        if (null != paramFileInfo && !sdUnMounted(paramFileInfo)){
            startNewPathListFragment(paramFileInfo);
        }
    }

    private void startNewPathListFragment(FileInfo p1) {
        FragmentTransaction transaction = getActivity().getFragmentManager().beginTransaction();
        PathListFragment fragment = new PathListFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(GlobalConstants.NEW_PATHLIST_FRAGMENT,p1.getFile());
        bundle.putInt(GlobalConstants.MODE_PATH,GlobalConstants.MODE_PATH_VIEW);
        fragment.setArguments(bundle);
        transaction.replace(R.id.fragment_container,fragment,fragment.getClass().getSimpleName());
        transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();
    }

    private boolean sdUnMounted(FileInfo fileInfo){
        return !Util.isStorageMounted(mContext,fileInfo.getPath());
    }

    private void startCategoryFragment(CategoryFragment fragment){
        FragmentTransaction transaction = getActivity().getFragmentManager().beginTransaction();
        String tag = fragment.getClass().getSimpleName();
        transaction.replace(R.id.fragment_container,fragment,fragment.getClass().getSimpleName());
        transaction.addToBackStack(tag);
        transaction.commitAllowingStateLoss();
    }

    private void newAsyncTask() {
        cancelAsyncTasks();
        for(Integer category : mCategoryList) {
            HomeAsyncTask asyncTask = new HomeAsyncTask();
            mAsyncTaskList.add(asyncTask);
            asyncTask.executeProxy(category);
        }
    }

    private void cancelAsyncTasks() {
        for(HomeFragment.HomeAsyncTask task : mAsyncTaskList) {
            task.cancel(true);
        }
        if(null != mAsyncTaskList) {
            mAsyncTaskList.clear();
        }else{
            mAsyncTaskList = new ArrayList<>();
            mAsyncTaskList.clear();
        }
    }

    private class HomeAsyncTask extends AsyncTask<Integer, Void, CategoryInfo>{
        Integer category;

        @Override
        protected CategoryInfo doInBackground(Integer...params) {
            category = params[0];
            Cursor cursor ;
            if(isCancelled()) {
                return null;
            }
            cursor = mediaDatabaseDao.getCategoryInfo(mContext,category);
            if(cursor != null) {
                parseCursor(category,cursor);
            }
            return null;
        }


        protected void onPostExecute(CategoryInfo categoryInfo) {
            fillCategoryTextView();
            if((isCancelled()) || (categoryInfo == null)) {
                return;
            }
        }

        private CategoryInfo parseCursor(Integer category, Cursor cursor) {
            try{
                if(null == mCategorySizeInfoMap){
                    return null;
                }
                CategoryInfo categoryInfo = mCategorySizeInfoMap.get(category);
                categoryInfo.count = 0;
                return parseCategoryCursor(cursor,categoryInfo);
            }
            catch(Exception e){
                e.fillInStackTrace();
            }finally {
                if(cursor!=null){
                    cursor.close();
                }
            }
            return null;
        }

        private CategoryInfo parseCategoryCursor(Cursor cursor, CategoryInfo categoryInfo) throws Exception {
            categoryInfo.count = cursor.getCount();
            return categoryInfo;
        }

        private void executeProxy(Integer params) {
            executeOnExecutor(GlobalConstants.CATEGORY_EXECUTOR,params);
        }
    }


    private class HomeCategoryContentObserver extends ContentObserver {

        private HomeCategoryContentObserver(Handler handler) {
            super(handler);
        }


        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange);
            Log.d(TAG,"HomeCategoryContentObserver onchange uri "+uri);
            refreshCategoryNum();
            gethistoryList();
        }
    }

    private void registerObserver() {
        if(null == mContext || null == mContext.getContentResolver()){
            return;
        }
        mContext.getContentResolver().registerContentObserver(GlobalConstants.FILES_URI, false, mContentObserver);
        mContext.getContentResolver().registerContentObserver(GlobalConstants.PICTURE_URI, false, mContentObserver);
        mContext.getContentResolver().registerContentObserver(GlobalConstants.MUSIC_URI, false, mContentObserver);
        mContext.getContentResolver().registerContentObserver(GlobalConstants.VIDEO_URI, false, mContentObserver);
    }

    private void unregisterObserver() {
        if(null == mContext || null == mContext.getContentResolver()){
            return;
        }
        mContext.getContentResolver().unregisterContentObserver(mContentObserver);
    }

}

package cn.com.protruly.filemanager.pathFileList;

import android.app.Activity;

import cn.com.protruly.filemanager.CategoryActivity;
import cn.com.protruly.filemanager.StorageVolumeManager;
import cn.com.protruly.filemanager.imageloader.ImageLoader;
import cn.com.protruly.filemanager.operation.FileOperationResult;
import cn.com.protruly.filemanager.operation.ProgressiveFileOperationTaskNotifier;
import cn.com.protruly.filemanager.utils.MatchupRecorder;
import cn.com.protruly.filemanager.utils.MediaFileUtil;
import hb.app.dialog.AlertDialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.storage.DiskInfo;
import android.os.storage.VolumeInfo;
import android.os.storage.StorageEventListener;
import android.support.annotation.Nullable;
import android.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import cn.com.protruly.filemanager.enums.FileInfo;
import cn.com.protruly.filemanager.operation.OperationType;
import cn.com.protruly.filemanager.ui.ProgressDialogManager;
import cn.com.protruly.filemanager.ui.ToolbarManager;
import cn.com.protruly.filemanager.utils.GlobalConstants;
import cn.com.protruly.filemanager.operation.OperationHelper;
import cn.com.protruly.filemanager.HomePage.StorageListAdapter;
import cn.com.protruly.filemanager.utils.HiddenFileFilter;
import cn.com.protruly.filemanager.utils.Util;
import cn.com.protruly.filemanager.utils.LogUtil;
import hb.widget.HbListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import cn.com.protruly.filemanager.R;

/**
 * Created by liushitao on 17-4-14.
 */

public class PathListFragment extends BasePathListFragment implements View.OnClickListener,
        OperationHelper.CreateFolderCallback {

    private static final String TAG = "PathListFragment";

    private HbListView storagePathSelectListView;
    private StorageListAdapter mStorageListAdapter;
    private StorageListAdapter mStorageListAdapter1;
    private ArrayList<FileInfo> mPathViewCacheList;
    private View mFilePathArea;
    private HorizontalScrollView mHorizontalScrollView;
    private LinearLayout mDynamicPathViewArea;
    private File mNewPathListFragmentFileInfo;
    private File fromSearch;
    private String mCurrentStorage;
    private int mParentPathChildFileNum;
    private long pastFileTotalSize;

    private boolean isFirstHome = true;
    private static final Executor PATH_LIST_EXECUTOR = Executors.newFixedThreadPool(10);
    private static final Executor CREATE_NEW_FOLDER_EXECUTOR = Executors.newFixedThreadPool(0x1);
    private ProgressDialogManager mProgressDialogManager;
    private int mListViewPosition;
    private MatchupRecorder mMatchupRecorder;
    private static boolean isUnzip = false;
    private String recordedPathJumpBefore;
    private boolean mIsSaveState;

    @Nullable

    @Override
    protected void initObjOnCreate(Bundle bundle) {
        super.initObjOnCreate(bundle);
        Log.d(TAG,"initObjOnCreate");
        mStorageListAdapter = new StorageListAdapter(mContext, Util.getStoragePathListSecond(mContext));
        mPathViewCacheList = new ArrayList<>();
        mProgressDialogManager = new ProgressDialogManager(getActivity());
        getArgumentInfo();
        mIsSaveState = false;
    }

    private void getArgumentInfo(){
        Bundle bundle = getArguments();
        if(bundle!=null){
            mNewPathListFragmentFileInfo = (File)bundle.getSerializable(GlobalConstants.NEW_PATHLIST_FRAGMENT);
            fromSearch = (File)bundle.getSerializable(GlobalConstants.FROM_SEARCH);
            path_mode = bundle.getInt(GlobalConstants.MODE_PATH);
            getOperationType = bundle.getInt(GlobalConstants.OPERATION_TYPE);
            getOperationData = (HashSet<FileInfo>) bundle.getSerializable(GlobalConstants.OPERATION_DATA);
            switchArrayListToSet();
            //isPickMode = getArguments().getBoolean("is_pick_mode",false);
        }
    }

    private void switchArrayListToSet(){
        if(getOperationData == null){
            return;
        }
        selectedFileSet.clear();
        selectedFileSet = getOperationData;
    }

    @Override
    protected void initListAdapter() {
        mFileListAdapter = new FileListAdapter(mContext,filesList,true);
    }

    @Override
    protected void initRootView(LayoutInflater inflater, ViewGroup contaner) {
        rootView = inflater.inflate(R.layout.storage_list_layout,contaner,false);
        super.initRootView(inflater, contaner);
    }

    @Override
    protected void initListChildSelfView() {
        mFilePathArea = rootView.findViewById(R.id.file_path_area);
        mHorizontalScrollView = (HorizontalScrollView)rootView.findViewById(R.id.category_tip_view);
        mDynamicPathViewArea = (LinearLayout) rootView.findViewById(R.id.file_path_container);
        initStoragePathSelectListView();
        mMatchupRecorder = new MatchupRecorder(mFileListView);
    }

    private void initStoragePathSelectListView(){
        storagePathSelectListView = (HbListView) rootView.findViewById(R.id.storage_path__select_list);
        storagePathSelectListView.setLongClickable(false);
        mStorageListAdapter1 = new StorageListAdapter(mContext, Util.getStoragePathListSecond(mContext),storagePathSelectListView);
        storagePathSelectListView.setOnItemClickListener(this);
        storagePathSelectListView.setAdapter(mStorageListAdapter1);
    }

    @Override
    protected void getInitData() {
        if(mNewPathListFragmentFileInfo!=null){
            mFilePathArea.setVisibility(View.VISIBLE);
            FileInfo fileInfo = new FileInfo(mNewPathListFragmentFileInfo.getPath());
            fileInfo.fileName = getStorageName(fileInfo);
            jumpIntoStoragePath(fileInfo);
            return;
        }
        if(fromSearch!=null ){
            mFilePathArea.setVisibility(View.VISIBLE);
            FileInfo fileInfo = new FileInfo(fromSearch.getPath());
            fileInfo.fileName = getStorageName(fileInfo);
            //finish actionmode
            if(mActivity!=null) {
                mActivity.showActionMode(false);
                mActivity.setActionModeListener(null);
            }
            //finish actionmode
            jumpIntoRandomStoragePath(fileInfo);
            return;
        }
        if(path_mode==GlobalConstants.MODE_PATH_SELECT && getOperationData!=null){
            startSelectPath(getOperationType);
            return;
        }
        //if(path_mode==GlobalConstants.MODE_PATH_SELECT && isPickMode){
        if(path_mode==GlobalConstants.MODE_PATH_SELECT){
            startSelectPath(getOperationType);
            return;
        }
    }

    @Override
    public boolean onBackPressed() {
        //modify-start by wenwenchao for bug282697
        //if(null != selectedFileSet && path_mode==GlobalConstants.MODE_PATH_SELECT){
        if(mCurrentPath != null && !(new FileInfo(mCurrentPath).exists())){
            Log.d(TAG,"onBackPressed AAAAAAA");
            return false;
        }
        if(null != selectedFileSet && path_mode==GlobalConstants.MODE_PATH_VIEW){
            selectedFileSet.clear();
        }
       //modify-end by wenwenchao for bug282697
        if(getOperationData != null && isActionModeState() && (new FileInfo(mCurrentPath)).getParent().equals(mCurrentStorage)){
            Log.d(TAG,"onBackPressed BBBBBBBBB");
            return false;
        }
        if(getOperationData ==null && isActionModeState()){
            getActionModeManager().finishActionMode();
            setActionModeState(false);
            Log.d(TAG,"onBackPressed CCCCCCCCCCCC");
            return true;
        }
        if((fromSearch != null)) {
            return false;
        }
        if(mCurrentPath!=null && mCurrentStorage!=null){
            if(mCurrentStorage.equals(mCurrentPath)){
                if(path_mode==GlobalConstants.MODE_PATH_SELECT){
                    getToolbarManager().clearToolbarMenu();
                    getToolbarManager().setNavigationIconAsBack();
                    selectedFileSet.clear();
                }
                if(mNewPathListFragmentFileInfo != null){
                    mCurrentStorage = null;
                    Log.d(TAG,"onBackPressed DDDDD ");
                    //changePath(null);
                    clearFileList();
                    showPath( null, false);
                    return false;
                }
                return false;
            }else{
                String parentPath = new File(mCurrentPath).getParent();
                if(mParentPathChildFileNum >50) {
                    changeView(mLoadingView);
                }
                listFiles(parentPath);
                showPath(null,false);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onPause() {
        Log.d(TAG,"onPause");
        mIsSaveState = false;
        //unRegisterReceiver();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy");
        mIsSaveState = false;
        setSortMode(0);
        super.onDestroy();
    }

    @Override
    public void onStop(){
        super.onStop();
        Log.d(TAG,"onStop");
    }

    private void deStorageInfo(FileInfo fileInfo){
        String storagePath = fileInfo.getPath();
        if(null == storagePath){
            storageType="文件";
            return;
        }
        if(phonePath!=null && storagePath.startsWith(phonePath) || storagePath.startsWith("/storage/emulated/0")) {
            storageType =mContext.getResources().getString(R.string.phone_storage);
            return;
        }
        if(null != otgpathMap && otgpathMap.size()==1 && storagePath.startsWith(String.valueOf(otgpathMap.get(0).get(StorageVolumeManager.PATH)))) {
            storageType = mContext.getResources().getString(R.string.otg_storage);
            return;
        }
        if(null != sdPath && storagePath.startsWith(sdPath)) {
            storageType = mContext.getResources().getString(R.string.sd_storage);
            return;
        }
        storageType="文件";
    }

    private void deRandomStorageInfo(FileInfo fileInfo){
        String storagePath = fileInfo.getPath();
        if(null == storagePath){
            storageType="文件";
            return;
        }
        if(phonePath!=null && storagePath.startsWith(phonePath) || storagePath.startsWith("/storage/emulated/0")) {
            storageType = mContext.getResources().getString(R.string.phone_storage);
            return;
        }
        if(null != otgpathMap && otgpathMap.size()==1 && storagePath.startsWith(String.valueOf(otgpathMap.get(0).get(StorageVolumeManager.PATH)))) {
            storageType = mContext.getResources().getString(R.string.otg_storage);
            return;
        }
        if(null != sdPath && storagePath.startsWith(sdPath)) {
            storageType = mContext.getResources().getString(R.string.sd_storage);
            return;
        }
        storageType="文件";
    }


    private String getStorageName(FileInfo fileInfo){
        String storagePath = fileInfo.getPath();
        if(null != phonePath &&
                (TextUtils.equals(phonePath, storagePath)) || (TextUtils.equals("/storage/emulated/0", storagePath))) {
            return  mContext.getResources().getString(R.string.phone_storage);
        }
        if(null != otgpathMap && otgpathMap.size()==1
                && TextUtils.equals(String.valueOf(otgpathMap.get(0).get(StorageVolumeManager.PATH)), storagePath)) {
            return  mContext.getResources().getString(R.string.otg_storage);
        }
        if(null != sdPath && TextUtils.equals(sdPath, storagePath)) {
            return  mContext.getResources().getString(R.string.sd_storage);
        }
        return  fileInfo.getPath();
    }



    private void jumpIntoStoragePath(FileInfo fileInfo){
        mCurrentStorage = fileInfo.getPath();
        deStorageInfo(fileInfo);
        getToolbarManager().setToolbarTitle(storageType);
        showPath(fileInfo,true);
        changeLoadingViewIfNecessary(fileInfo);
        listFiles(fileInfo.getPath());
    }

    private void jumpIntoRandomStoragePath(FileInfo fileInfo){
        mCurrentStorage = fileInfo.getPath();
        deRandomStorageInfo(fileInfo);
        getToolbarManager().setToolbarTitle(storageType);
        clearPath();
        showPath1(fileInfo);
        listFiles(fileInfo.getPath());
    }

    private void showPath(FileInfo fileInfo,boolean show){
        if(show){
            addPathTextView(fileInfo);
            mPathViewCacheList.add(fileInfo);
            return;
        }
        removePathTextView();
    }

    private void showPath1(FileInfo fileInfo){
        String path = fileInfo.getPath();
        ArrayList<FileInfo> fileList = Util.getFileListFromPathSecond(mContext,path);
        for(int i = fileList.size()-1;i>=0;i--){
            addPathTextView(fileList.get(i));
            mPathViewCacheList.add(fileList.get(i));
        }
    }

    private void clearPath(){
        mPathViewCacheList.clear();
        for(int i = mDynamicPathViewArea.getChildCount()-1;i>=0;i--){
            View childView = mDynamicPathViewArea.getChildAt(i);
            if(childView==null){
                return;
            }
            mDynamicPathViewArea.removeView(childView);
        }
    }

    private void addPathTextView(FileInfo fileInfo){
        if(fileInfo == null){
            return;
        }
        final TextView view = (TextView) View.inflate(mContext,R.layout.path_text_layout,null);
        view.setText(fileInfo.fileName);
        view.setTag(fileInfo);
        view.setVisibility(View.VISIBLE);
        view.setOnClickListener(this);
        mDynamicPathViewArea.addView(view);
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mHorizontalScrollView.smoothScrollTo(mDynamicPathViewArea.getWidth()+view.getWidth(),0);
            }
        });
    }

    private void removePathTextView(){
        View childView = mDynamicPathViewArea.getChildAt(mDynamicPathViewArea.getChildCount()-1);
        if(childView==null){
            return;
        }
        mDynamicPathViewArea.removeView(childView);
        mPathViewCacheList.remove(mPathViewCacheList.size()-1);
        mDynamicPathViewArea.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mDynamicPathViewArea.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mHorizontalScrollView.smoothScrollTo(mDynamicPathViewArea.getWidth(),0);
            }
        });
    }

    private void removePathTextView(View view){
        int position = mDynamicPathViewArea.indexOfChild(view)+1;
        int count = mDynamicPathViewArea.getChildCount();
        removeElementFromCacheList(position);
        mDynamicPathViewArea.removeViews(position,count-position);
    }

    private void removeElementFromCacheList(int position){
        ArrayList<FileInfo> arrayList = new ArrayList<>();
        arrayList.addAll(mPathViewCacheList.subList(0,position));
        mPathViewCacheList.retainAll(arrayList);
    }

    private void listFiles(String p1) {
        mListViewPosition = mMatchupRecorder.computeScrollPosition(p1);
        mParentPathChildFileNum = filesList.size();
        changePath(p1);
        clearFileList();
        newAsyncTask(p1);
    }

    private void newAsyncTask(String path) {
        cancelAsyncTask();
        mAsyncTask = new PathListAsyncTask();
        ((PathListAsyncTask)mAsyncTask).executeProxy(path);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");
        mIsSaveState = false;
        inflateToolBarMenu();
        /*if(!isPickMode) {
            inflateBottomNavigationMenu();
        }*/
        inflateBottomNavigationMenu();
        //registerReceiver();
        if(fileNameInputDialog!=null && fileNameInputDialog.isShowing()){
            inputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
        }
        if(null == StorageVolumeManager.getSDPath() && mContext.getResources().getString(R.string.sd_storage).equals(storageType)){
            if(!mActivity.isFinishing()&& !mIsSaveState) {
                ((CategoryActivity)mActivity).backImmediately();
            }
        }else if(null == StorageVolumeManager.getOTGPathList()&& mContext.getResources().getString(R.string.otg_storage).equals(storageType)){
            if(!mActivity.isFinishing()&& !mIsSaveState) {
                ((CategoryActivity)mActivity).backImmediately();
            }
        }
        if(path_mode==GlobalConstants.MODE_PATH_VIEW && getOperationData!=null){
            startSelectPath(getOperationType);
            return;
        }

        if(path_mode==GlobalConstants.MODE_PATH_VIEW && recordedPathJumpBefore!=null){
            FileInfo fileInfo = new FileInfo(recordedPathJumpBefore);
            if(!fileInfo.exists()) return;
            deRandomStorageInfo(fileInfo);
            getToolbarManager().setToolbarTitle(storageType);
            clearPath();
            showPath1(fileInfo);
            changeLoadingViewIfNecessary(fileInfo);
            listFiles(recordedPathJumpBefore);
            recordedPathJumpBefore = null;
        }
    }

    /*private void registerReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addDataScheme("file");
        getActivity().registerReceiver(scannerReceiver,intentFilter);
    }

    private BroadcastReceiver scannerReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"onReceive");
            if(null == StorageVolumeManager.getSDPath() && mContext.getResources().getString(R.string.sd_storage).equals(storageType)){
                if(!mActivity.isFinishing()&& !mIsSaveState) {
                    ((CategoryActivity)mActivity).backImmediately();
                }
            }else if(null == StorageVolumeManager.getOTGPathList()&& mContext.getResources().getString(R.string.otg_storage).equals(storageType)){
                if(!mActivity.isFinishing()&& !mIsSaveState) {
                    ((CategoryActivity)mActivity).backImmediately();
                }
            }
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mIsSaveState = true;
        Log.d(TAG,"onSaveInstanceState");
    }

    /*private void unRegisterReceiver(){
        getActivity().unregisterReceiver(scannerReceiver);
    }*/

    @Override
    public int getFragmentId() {
        return GlobalConstants.PATH_FRAGMENT;
    }

    @Override
    protected void cancelAsyncTask() {
        super.cancelAsyncTask();
    }

    @Override
    public void changeView(View paramView) {
        super.changeView(paramView);
    }

    @Override
    public void clearFileList() {
        super.clearFileList();
    }

    public void refresh(){

        finishActionMode();
        if(mParentPathChildFileNum >50) {
            changeView(mLoadingView);
        }
        clearFileList();
        //changeView(mListViewArea);
        newAsyncTask(mCurrentPath);
        mFileListAdapter.notifyDataSetChanged();
    }

    public void refreshAfterDeleteCompleted(ArrayList<FileInfo> arrayList){
        finishActionMode();
        filesList.removeAll(arrayList);
        mFileListAdapter.notifyDataSetChanged();
        if(null!=mCurrentPath) {
            deRandomStorageInfo(new FileInfo(mCurrentPath));
        }
        getToolbarManager().setToolbarTitle(storageType);
        inflateToolBarMenu();
        if(filesList.size()==0){
            changeView(mEmptyView);
        }
    }

    private void deleteFileList(ArrayList<FileInfo> arrayList){
        for(FileInfo fileInfo:arrayList){
            arrayList.remove(fileInfo);
        }
    }

    @Override
    protected void refreshAfterRenameCompleted1(FileInfo oldFile, FileInfo newFile) {
        if(null == newFile || null ==newFile.getName()){
            Util.showToast(mContext, R.string.rename_fail);
            return;
        }
        finishRenameActionMode();
        filesList.remove(oldFile);
        if(!getHidenStatus() || !newFile.getName().startsWith(".")){
            filesList.add(newFile);
        }
        sortFileList();
        mFileListAdapter.notifyDataSetChanged();
        mFileListView.setSelection(filesList.indexOf(newFile));
        Util.showToast(mContext, R.string.rename_success);
        if(null!=mCurrentPath) {
            deRandomStorageInfo(new FileInfo(mCurrentPath));
        }
        getToolbarManager().setToolbarTitle(storageType);
        inflateToolBarMenu();
    }

    protected void onPasteCompleted(Message p1) {
        dismissProgress();
        int pasteSize = mPasteFileSet.size();
        cancelPaste();
        refreshAfterPasteCompleted(filesList.size() + pasteSize);
        if(null!=mCurrentPath) {
            deRandomStorageInfo(new FileInfo(mCurrentPath));
        }
        getToolbarManager().setToolbarTitle(storageType);
        mFileListView.setLongClickable(true);
        showNotice();
        clearOperationType();
    }

    protected void onPasteCompletedForCopy() {
        dismissProgress();
        int pasteSize = mPasteFileSet.size();
        cancelPaste();
        refreshAfterPasteCompleted(filesList.size() + pasteSize);
        if(null!=mCurrentPath) {
            deRandomStorageInfo(new FileInfo(mCurrentPath));
        }
        getToolbarManager().switchToStatus(ToolbarManager.STATUS_CATEGORY_PATH_LIST);
        getToolbarManager().setToolbarTitle(storageType);
        mFileListView.setLongClickable(true);
        Util.showToast(this.mContext, R.string.copy_success);
    }

    protected void onPasteCompletedForCut() {
        dismissProgress();
        clearOperationType();
        int pasteSize = mPasteFileSet.size();
        cancelPaste();
        refreshAfterPasteCompleted(filesList.size() + pasteSize);
        if(null!=mCurrentPath) {
            deRandomStorageInfo(new FileInfo(mCurrentPath));
        }
        getToolbarManager().switchToStatus(ToolbarManager.STATUS_CATEGORY_PATH_LIST);
        getToolbarManager().setToolbarTitle(storageType);
        mFileListView.setLongClickable(true);
        Util.showToast(this.mContext,R.string.cut_success);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FileInfo fileInfo = (FileInfo)parent.getItemAtPosition(position);
        if(!fileInfo.exists()){
            Toast.makeText(mContext,mContext.getResources().getString(R.string.forc_file_not_exists),Toast.LENGTH_SHORT).show();
            return;
        }
        if(parent.getId()== R.id.storage_path__select_list){
            mFilePathArea.setVisibility(View.VISIBLE);
            mCurrentStorage = fileInfo.getPath();
            if((TextUtils.equals(phonePath, mCurrentStorage)) || (TextUtils.equals("/storage/emulated/0", mCurrentStorage))){
                if(pastFileTotalSize>=Util.getFreeSizeWithFile(mCurrentStorage)){
                    Util.showToast(mContext,R.string.forc_insufficient_space);
                    return;
                }
            }else if(null != sdPath && TextUtils.equals(sdPath, mCurrentStorage)) {
                if(pastFileTotalSize>=Util.getFreeSizeWithFile(mCurrentStorage)){
                    Util.showToast(mContext,R.string.forc_insufficient_space);
                    return;
                }
            } else if(null != otgpathMap && otgpathMap.size()==1
                    && TextUtils.equals(String.valueOf(otgpathMap.get(0).get(StorageVolumeManager.PATH)), mCurrentStorage)) {
                if(pastFileTotalSize>=Util.getFreeSizeWithFile(mCurrentStorage)){
                    Util.showToast(mContext,R.string.forc_insufficient_space);
                    return;
                }
            }
            storagePathSelectListView.setVisibility(View.GONE);
            //onNormalPathClick(fileInfo);
            //added
            startNewPathListFragment(fileInfo);
            return;
        }
        if (path_mode==GlobalConstants.MODE_PATH_SELECT && (!isActionModeState() && fileInfo.exists()&&!fileInfo.isFile)) {
            onNormalPathClick1(fileInfo);
            return;
        }
        if ((!isActionModeState() && !fileInfo.isFile)) {
            //isSelectPath = false;
            onNormalPathClick(fileInfo);
            return;
        }
        /*if (isPickMode && fileInfo.isFile && Util.isCertinstallerFileType(fileInfo.filePath)) {
            mOperationHelper.pickFile(getActivity(), fileInfo);
            return;
        }*/
        super.onItemClick(parent,view,position,id);
    }


    private void startNewPathListFragment(FileInfo p1) {
        FragmentTransaction transaction = getActivity().getFragmentManager().beginTransaction();
        PathListFragment fragment = new PathListFragment();
        String tag = fragment.getClass().getSimpleName();
        Bundle bundle = new Bundle();
        bundle.putInt(GlobalConstants.OPERATION_TYPE, getOperationType);
        bundle.putSerializable(GlobalConstants.OPERATION_DATA, selectedFileSet);
        bundle.putInt(GlobalConstants.MODE_PATH,GlobalConstants.MODE_PATH_SELECT);
        /*if(isPickMode) {
            bundle.putBoolean("is_pick_mode", isPickMode);
        }*/
        bundle.putSerializable(GlobalConstants.NEW_PATHLIST_FRAGMENT,p1.getFile());
        fragment.setArguments(bundle);
        if(mNewPathListFragmentFileInfo==null) {
            getFragmentManager().popBackStack();
        }
        transaction.replace(R.id.fragment_container,fragment,fragment.getClass().getSimpleName());
        transaction.addToBackStack(tag);
        transaction.commitAllowingStateLoss();
    }


    private void startPaste(String paramString1, String paramString2){
        mPasteFileSet.addAll(selectedFileSet);
        selectedFileSet.clear();
        finishActionMode();
        mCurrentPath = paramString2;
        //paste(paramString1, paramString2);
        if(getOperationType == OperationType.COPY) {
            mFileOperationTaskManager.executeCopyTask(mPasteFileSet, mCurrentPath, new ProgressiveFileOperationTaskNotifier(mContext) {
                @Override
                public void onOperationSucceeded(int operationType, FileOperationResult result) {
                    super.onOperationSucceeded(operationType, result);
                    //Util.showToast(mContext, "Copy succeeded in PathListFragemnt::startPaste");
                    //TODO: refresh list view
                    onPasteCompletedForCopy();
                }

                @Override
                public void onOperationFailed(int operationType, FileOperationResult result) {
                    super.onOperationFailed(operationType, result);
                    refreshAfterPasteFail();
                    if(result.resultCode==FileOperationResult.FORC_INSUFFICIENT_SPACE){
                        Util.showToast(mContext, result.getDescription());
                        return;
                    }
                    //Util.showToast(mContext, R.string.copy_fail);
                    //Util.showToast(mContext, result.getDescription());
                }

                @Override
                public void onOperationCancelled(int operationType, FileOperationResult result) {
                    super.onOperationCancelled(operationType, result);
                    LogUtil.i(TAG, "=====onOperationCancelled=====");
                    refreshAfterPasteFail();
                }
            });
        } else if(getOperationType == OperationType.CUT) {
            mFileOperationTaskManager.executeMoveTask(mPasteFileSet, mCurrentPath, new ProgressiveFileOperationTaskNotifier(mContext) {
                @Override
                public void onOperationSucceeded(int operationType, FileOperationResult result) {
                    super.onOperationSucceeded(operationType, result);
                    //Util.showToast(mContext, "Move succeeded in PathListFragemnt::startPaste");
                    //TODO: refresh list view
                    onPasteCompletedForCut();
                }

                @Override
                public void onOperationFailed(int operationType, FileOperationResult result) {
                    super.onOperationFailed(operationType, result);
                    refreshAfterPasteFail();
                    /*
                    if(result.resultCode==FileOperationResult.FORC_INSUFFICIENT_SPACE){
                        Util.showToast(mContext, result.getDescription());
                        return;
                    }
                    */
                    Util.showToast(mContext, R.string.cut_fail);
                }

                @Override
                public void onOperationCancelled(int operationType, FileOperationResult result) {
                    super.onOperationCancelled(operationType, result);
                    LogUtil.i(TAG, "=====onOperationCancelled=====");
                    refreshAfterPasteFail();
                }
            });
        }

        LogUtil.i(TAG, "selectedFileSet:" + selectedFileSet + " mCurrentPath:" + mCurrentPath);
    }

    protected void refreshAfterPasteCompleted(int paramInt) {
        if (paramInt > 500)
            changeView(mLoadingView);
        clearFileList();
        newAsyncTask(mCurrentPath);
    }

    protected void refreshAfterPasteFail() {
        clearPasteStatus();
        if(null!=mCurrentPath) {
            deRandomStorageInfo(new FileInfo(mCurrentPath));
        }
        getToolbarManager().switchToStatus(ToolbarManager.STATUS_CATEGORY_PATH_LIST);
        getToolbarManager().setToolbarTitle(storageType);
    }

    protected void refreshAfterUnzip() {
        clearOperationType();
        if(null!=mCurrentPath) {
            deRandomStorageInfo(new FileInfo(mCurrentPath));
        }
        getToolbarManager().switchToStatus(ToolbarManager.STATUS_CATEGORY_PATH_LIST);
        getToolbarManager().setToolbarTitle(storageType);
        mFileListView.setLongClickable(true);
    }

    private void onNormalPathClick(FileInfo fileInfo){
        showPath(fileInfo,true);
        onPathClick(fileInfo);
    }

    private void onNormalPathClickForSelectPath(FileInfo fileInfo){
        clearPath();
        showPath(fileInfo,true);
        onPathClick(fileInfo);
    }

    private void onNormalPathClick1(FileInfo fileInfo){
        showPath(fileInfo,true);
        onPathClick(fileInfo);
    }

    private void onPathClick(FileInfo paramFileInfo) {
        changeLoadingViewIfNecessary(paramFileInfo);
        listFiles(paramFileInfo.getPath());
    }

    private void changeLoadingViewIfNecessary(FileInfo p1) {
        changeView(mLoadingView);
        /*if(mParentPathChildFileNum > 50) {
            changeView(mLoadingView);
        }*/
    }

    protected void startSelectPath(int paramOperationType) {
        recordedPathJumpBefore = mCurrentPath;
        getOperationType = paramOperationType;
        new CalculateDataTask().execute();
    }

    private void jumpSelectPath(){
        getToolbarManager().switchToStatus(ToolbarManager.STATUS_HOME_PATH_SELECTION);
        if(Util.getStoragePathListSecond(mContext).size()>1) {
            storagePathSelectListView.setVisibility(View.VISIBLE);
        }else{
            mFilePathArea.setVisibility(View.VISIBLE);
            mCurrentStorage = phonePath;
            //deStorageInfo(new FileInfo(mCurrentStorage));
            //getToolbarManager().setToolbarTitle(storageType);
            FileInfo fileInfo = new FileInfo(mCurrentStorage);
            fileInfo.fileName = mContext.getResources().getString(R.string.phone_storage);
            startNewPathListFragment(fileInfo);
            //onNormalPathClickForSelectPath(fileInfo);
        }
    }

    public class CalculateDataTask extends AsyncTask<Void, Integer, Long> implements DialogInterface.OnCancelListener {

        private static final String TAG = "CalculateDataTask";

        private long pastFileTotalSize;

        public CalculateDataTask() {
            mProgressDialogManager = new ProgressDialogManager(mContext);
            mProgressDialogManager.setCancelListener(this);
        }

        @Override
        protected void onPreExecute() {
            mProgressDialogManager.showProgressDialog(R.string.caculating);
        }

        @Override
        protected Long doInBackground(Void... params) {
            int num = 0;
            int progress = 0;
            final ArrayList<FileInfo> selectList = new ArrayList<>(selectedFileSet);
            final int selectedSize = selectedFileSet.size();
            if(selectedSize==0){
                return null;
            }
            for(FileInfo fileInfo:selectList){
                pastFileTotalSize += getFileSize(mContext,fileInfo.getFile());
                if(isCancelled()) break;
                num++;
                progress = Util.getProgress(selectedSize, num);
                publishProgress(progress);
            }
            return pastFileTotalSize;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if(mProgressDialogManager != null) {
                mProgressDialogManager.setProgress(values[0]);
            }
        }

        @Override
        protected void onPostExecute(Long size) {
            super.onPostExecute(size);
            mProgressDialogManager.dismissProgressDialog();
            if(isCancelled()) return;
            jumpSelectPath();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mToolbarManager.setNavigationIconAsBack();
            selectedFileSet.clear();
            Util.showToast(mContext, android.R.string.cancel);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            cancel(true);
        }

        private long getFileSize(Context context,File file) {
            long size = 0;
            if(file.isDirectory()) {
                File[] files;
                if(((CategoryActivity)context).getHidenStatus()){
                    files = file.listFiles(new HiddenFileFilter());
                }else{
                    files = file.listFiles();
                }
                if((files == null) || (files.length == 0)) {
                    return 0;
                }
                for(File child : files) {
                    if(isCancelled()) break;
                    size += getFileSize(context,child);
                }
                return size;
            }
            size += file.length();
            return size;
        }
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.tv_storage_tip:
                removePathTextView(view);
                if(mNewPathListFragmentFileInfo!=null){
                    changePath(mCurrentStorage);
                }
                break;
            case R.id.tv_path:
                onPathViewClick(view);
                break;
        }
    }

    private void changePath(String str){
        mCurrentPath = str;
        cancelAsyncTask();
    }

    private void onPathViewClick(View view){
        FileInfo fileInfo = (FileInfo) view.getTag();
        if(fileInfo.getPath().equals(mCurrentPath)){
            return;
        }
        onPathClick(fileInfo);
        removePathTextView(view);
    }

    private class PathListAsyncTask extends AsyncTask<String,Void,Object> {

        protected Object doInBackground(String[] str) {
            String path = str[0];
            if((isCancelled()) || (path == null)) {
                return null;
            }
            File file = new File(path);
            file.canExecute();
            File[] files ;
            if(getHidenStatus()){
                files = file.listFiles(new HiddenFileFilter());
            }else{
                files = file.listFiles();
            }
            return parseFiles(files);
        }

        protected void onPostExecute(Object p1) {
            if((isCancelled()) || (p1 == null)) {
                return;
            }
            handleResult(p1);
        }

        private void handleResult(Object obj) {
            ArrayList<FileInfo> arrayList = new ArrayList<>();
            if ((obj instanceof List)) {
                arrayList = (ArrayList)obj;
                if(arrayList.size()==0){
                    changeView(mEmptyView);
                }else{
                    changeView(mListViewArea);
                }
            }
            if(path_mode==GlobalConstants.MODE_PATH_SELECT){
                mFileListView.setLongClickable(false);
            }
            filesList.addAll(arrayList);
            mFileListAdapter.notifyDataSetChanged();
            if(isUnzip) {
                inflateToolBarMenu();
                inflateBottomNavigationMenu();
            }
            mFileListView.setSelection(mListViewPosition);
        }

        private List parseFiles(File[] p1) {
            if(p1 == null) {
                return null;
            }
            ArrayList<FileInfo> fileList = new ArrayList<FileInfo>(p1.length);
            for(int j=0;j<p1.length;j++){
                File file = p1[j];
                if(isCancelled()) {
                    return null;
                }
                FileInfo info = new FileInfo(file);
                fileList.add(info);
            }
            Collections.sort(fileList,getComparator());
            return fileList;
        }

        private void executeProxy(String path) {
            executeOnExecutor(PATH_LIST_EXECUTOR,path);
        }
    }

    private void inflateBottomNavigationMenu(){
        if(path_mode==GlobalConstants.MODE_PATH_SELECT){
            if(null==getActionModeManager()) return;
            getActionModeManager().startSelectPathActionMode();
            if(getOperationType==0 ||getOperationType==1){
                getActionModeManager().removeBottomNavigationMenuItem(R.id.action_menu_unzip);
            }else{
                getActionModeManager().removeBottomNavigationMenuItem(R.id.action_menu_paste);
            }
        }
    }

    @Override
    public void onToolbarNavigationIconClicked() {
        if(!mActivity.isFinishing()&& path_mode!=GlobalConstants.MODE_PATH_SELECT) {
            ((CategoryActivity)mActivity).backImmediately();
        }
    }

    private void createNewFolder(){
        CREATE_NEW_FOLDER_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                String str = getString(R.string.action_menu_create_new_folder);
                final String str2 = Util.reconstructFolderName(mCurrentPath,str);
                if(getActivity()==null){
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        createNameInputDialog(OperationType.CREATE_FOLDER,mContext.getResources().getString(R.string.action_menu_create_new_folder),str2,new FileInfo(str2));
                    }
                });
            }
        });
    }

    protected  <T> void doAfterNewNameConfirmed(int operationType,T t,String str){
        if(operationType ==OperationType.CREATE_FOLDER){
            File file = new File(mCurrentPath,str);
            mOperationHelper.createNewFolder(file,this);
            return;
        }
        super.doAfterNewNameConfirmed(operationType,t,str);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        int id = menuItem.getItemId();
        switch (id) {
            case R.id.create_new_folder:
                createNewFolder();
                break;
            case R.id.category_sort:
                setMenuItemChecked(getSortModeOrder());
                break;
            case R.id.category_sort_by_name:
                setSortMode(0);
                setMenuItemChecked(0);
                break;
            case R.id.category_sort_by_time:
                setSortMode(1);
                setMenuItemChecked(1);
                break;
            case R.id.category_sort_by_size_asc:
                setSortMode(2);
                setMenuItemChecked(2);
                break;
            case R.id.category_sort_by_size_dsc:
                setSortMode(3);
                setMenuItemChecked(3);
                break;
            case R.id.show_hide_file:
                setHidenStatus(false);
                clearFileList();
                inflateToolBarMenuForHiden();
                newAsyncTask(mCurrentPath);
                break;
            case R.id.hide_hide_file:
                setHidenStatus(true);
                clearFileList();
                inflateToolBarMenuForHiden();
                newAsyncTask(mCurrentPath);
                break;
            case R.id.action_cancel:
                if(null != selectedFileSet) {
                    selectedFileSet.clear();
                }
                getToolbarManager().clearToolbarMenu();
                getToolbarManager().setNavigationIconAsBack();
                if(!mActivity.isFinishing()) {
                    ((CategoryActivity)mActivity).backImmediately();
                }
                break;
            default:

                break;
        }
        return false;
    }

    private void setMenuItemChecked(int currentSortOrder) {
        if(null == mToolbarManager) return;
        int [] menuIds = new int[] {R.id.category_sort_by_name, R.id.category_sort_by_time,
                R.id.category_sort_by_size_asc, R.id.category_sort_by_size_dsc};
        for(int i=0; i<menuIds.length; i++) {
            int menuId = menuIds[i];
            boolean checked = currentSortOrder == i;
            mToolbarManager.setMenuItemChecked(menuId, checked);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        if(null == menuItem) return false;
        switch(menuItem.getItemId()) {
            case R.id.category_sort:
                sortDialog();
                break;
            case R.id.create_new_folder:
                createNewFolder();
                break;
            case R.id.action_menu_paste:
                mActionModeManager.finishSelectPathActionMode();
                startPaste(mCurrentStorage,mCurrentPath);
                mFileListView.setLongClickable(true);
                getOperationData = null;
                path_mode=GlobalConstants.MODE_PATH_VIEW;
                break;
            case R.id.action_menu_unzip:
                //ArrayList<FileInfo> list = new ArrayList<>(selectedFileSet);
                mFileListView.setLongClickable(true);
                getOperationData = null;
                path_mode=GlobalConstants.MODE_PATH_VIEW;
                Iterator<FileInfo> it = selectedFileSet.iterator();
                FileInfo fileInfo = null;
                while(it.hasNext()) {
                    fileInfo = it.next();
                    break;
                }
                if(fileInfo == null) return false;
                unzipWhole(fileInfo.getPath(), mCurrentPath);
                break;
        }
        return super.onNavigationItemSelected(menuItem);
    }

    public void sortDialog(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        CharSequence[] items = {mContext.getResources().getString(R.string.category_sort_by_name),
                mContext.getResources().getString(R.string.category_sort_by_time),
                mContext.getResources().getString(R.string.category_sort_by_size_asc),
                mContext.getResources().getString(R.string.category_sort_by_size_dsc)};
        builder.setSingleChoiceItems(items, getSortModeOrder(), new DialogInterface.OnClickListener() {
            boolean dismiss = false;
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case 0:
                        setSortMode(0);
                        dialog.cancel();
                        break;
                    case 1:
                        setSortMode(1);
                        dialog.cancel();
                        break;
                    case 2:
                        setSortMode(2);
                        dialog.cancel();
                        break;
                    case 3:
                        setSortMode(3);
                        dialog.cancel();
                        break;
                }
            }
        });
        builder.create().show();
    }

    private void unzipWhole(String zipFilePath, String destinationDir) {
        mFileOperationTaskManager.executeUnzipWholeTask(zipFilePath, destinationDir, new ProgressiveFileOperationTaskNotifier(mContext) {
            @Override
            public void onOperationSucceeded(int operationType, FileOperationResult result) {
                super.onOperationSucceeded(operationType, result);
                LogUtil.i(TAG, "onUnzipWholeSucceeded ------ 1");
                if(result == null) return;
                LogUtil.i(TAG, "onUnzipWholeSucceeded ------ 2");
                String unzippedFilePath = result.realDestinationPath;
                if(!TextUtils.isEmpty(unzippedFilePath)) {
                    String description = mContext.getString(FileOperationResult.getDescription(result.resultCode));
                    Util.showToast(mContext, description + " " + unzippedFilePath);
                    //FileOperationUtil.viewFile(mContext, unzippedFilePath);
                }else{
                    LogUtil.i(TAG, "onUnzip  not finish");
                }
                refresh();
                refreshAfterUnzip();
                isUnzip = false;
            }

            @Override
            public void onOperationFailed(int operationType, FileOperationResult result) {
                super.onOperationFailed(operationType, result);
                refreshAfterUnzip();
                //if(result == null) return;
                LogUtil.i(TAG, "onUnzipWholeFailed ------ 2");
                //Util.showToast(mContext, FileOperationResult.getDescription(result.resultCode));
            }

            @Override
            public void onOperationCancelled(int operationType, FileOperationResult result) {
                super.onOperationCancelled(operationType, result);
                refreshAfterUnzip();
                LogUtil.i(TAG, "onUnzipWholeCancelled");
                Util.showToast(mContext, android.R.string.cancel);
            }
        });
    }

    protected void showNotice() {
        if (mContext == null) return;
        if (getOperationType == OperationType.COPY){
            Util.showToast(this.mContext, R.string.copy_success);
        }else{
            Util.showToast(this.mContext,R.string.cut_success);
        }
    }

    @Override
    public void onCreateFolderComplete(boolean success,File file) {
        if(success){
            FileInfo fileInfo = new FileInfo(file.getPath());
            changeView(mListViewArea);
            filesList.add(fileInfo);
            sortFileList();
            mFileListAdapter.notifyDataSetChanged();
            mFileListView.setSelection(filesList.indexOf(fileInfo));
            Util.showToast(mContext,R.string.new_folder_success);
            return;
        }
        Util.checkFailToCreateFolderReason(mContext, mCurrentStorage, file);
    }

    public static void openMe(Activity activity, int operationType, HashSet<FileInfo> selected) {
        isUnzip = true;
        PathListFragment fragment = new PathListFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(GlobalConstants.OPERATION_TYPE, operationType);
        bundle.putSerializable(GlobalConstants.OPERATION_DATA, selected);
        bundle.putInt(GlobalConstants.MODE_PATH,GlobalConstants.MODE_PATH_SELECT);
        fragment.setArguments(bundle);
        FragmentTransaction transaction = activity.getFragmentManager().beginTransaction();
        String tag = fragment.getClass().getSimpleName();
        transaction.replace(R.id.fragment_container,fragment,fragment.getClass().getSimpleName());
        transaction.addToBackStack(tag);
        transaction.commitAllowingStateLoss();
    }
}

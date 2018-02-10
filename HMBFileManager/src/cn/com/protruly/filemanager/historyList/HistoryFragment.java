package cn.com.protruly.filemanager.historyList;

import android.app.FragmentTransaction;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.HashSet;

import cn.com.protruly.filemanager.enums.FileInfo;
import cn.com.protruly.filemanager.utils.LogUtil;
import cn.com.protruly.filemanager.utils.MediaDatabaseDao;
import cn.com.protruly.filemanager.operation.FileOperationResult;
import cn.com.protruly.filemanager.operation.OperationType;
import cn.com.protruly.filemanager.operation.OperationHelper;
import cn.com.protruly.filemanager.operation.ProgressiveFileOperationTaskNotifier;
import cn.com.protruly.filemanager.pathFileList.BasePathListFragment;
import cn.com.protruly.filemanager.pathFileList.PathListFragment;
import cn.com.protruly.filemanager.ui.NameInputDialogManager;
import cn.com.protruly.filemanager.ui.ToolbarManager;
import cn.com.protruly.filemanager.utils.FilePathUtil;
import cn.com.protruly.filemanager.utils.GlobalConstants;
import cn.com.protruly.filemanager.utils.Util;
import hb.app.HbActivity;
import hb.widget.HbListView;
import cn.com.protruly.filemanager.R;

/**
 * Created by liushitao on 17-5-10.
 */

public class HistoryFragment extends BasePathListFragment implements View.OnClickListener,
        NameInputDialogManager.NameInputDialogListener {
    private String TAG = "HistoryFragment";
    private MediaDatabaseDao mediaDatabaseDao;
    private OperationHelper mOperationHelper;
    private FileInfo lable_today ;
    private FileInfo lable_treday;
    private FileInfo lable_week;
    private HistoryContentObserver mContentObserver;
    @Nullable

    @Override
    protected void initObjOnCreate(Bundle bundle) {
        super.initObjOnCreate(bundle);
        mediaDatabaseDao = new MediaDatabaseDao(mContext);
        mOperationHelper = new OperationHelper(mContext,new Handler());
        mContentObserver = new HistoryContentObserver(new Handler());
    }

    @Override
    public void onStart() {
        super.onStart();
        registerObserver();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterObserver();
    }

    @Override
    protected void initListAdapter() {
        mFileListAdapter = new HistoryListAdapter(mContext,filesList,false);
    }

    @Override
    protected void initRootView(LayoutInflater inflater, ViewGroup contaner) {
        rootView = inflater.inflate(R.layout.history_list_layout,contaner,false);
        mToolbarManager.switchToStatus(ToolbarManager.STATUS_HISTORY_PATH_LIST);
        super.initRootView(inflater, contaner);
    }

    @Override
    protected void initListChildSelfView() {
        initStorageListView();
    }

    private void initStorageListView(){
        HbListView historyListView = (HbListView) rootView.findViewById(R.id.storage_path_list);
        historyListView.setOnItemClickListener(this);
        historyListView.setOnItemLongClickListener(this);
        historyListView.setAdapter(mFileListAdapter);
    }

    @Override
    protected void getInitData() {
        changeView(mLoadingView);
        getLableData();
        gethistoryList();
    }

    private void getLableData(){
        lable_today = new FileInfo("fileSection");
        lable_today.sectionType = GlobalConstants.SECTION_TYPE;
        lable_today.fileName = mContext.getResources().getString(R.string.today);
        lable_treday = new FileInfo("fileSection");
        lable_treday.sectionType =GlobalConstants.SECTION_TYPE;
        lable_treday.fileName = mContext.getResources().getString(R.string.in_three_day);
        lable_week = new FileInfo("fileSection");
        lable_week.sectionType =GlobalConstants.SECTION_TYPE;
        lable_week.fileName = mContext.getResources().getString(R.string.in_one_week);
    }

    private void gethistoryList(){
        new GetHistoryListTask().execute();
    }

    private class GetHistoryListTask extends AsyncTask<Void, Void, ArrayList<FileInfo>> {

        @Override
        protected ArrayList<FileInfo> doInBackground(Void... params) {
            ArrayList<FileInfo> todayList = new ArrayList<>();
            ArrayList<FileInfo> threeDaysList = new ArrayList<>();
            ArrayList<FileInfo> sevenDaysList = new ArrayList<>();
            todayList.add(lable_today);
            threeDaysList.add(lable_treday);
            sevenDaysList.add(lable_week);
            FileInfo info;
            String data;
            Cursor cursor = mediaDatabaseDao.getHistoryInfo(mContext);
            if(null == cursor) return new ArrayList<>();
            while (cursor.moveToNext()) {
                data = cursor.getString(cursor.getColumnIndex("_data"));
                info = new FileInfo(data,true);
                info.filePath = data;
                //info.fileName =cursor.getString(cursor.getColumnIndex(CategoryFactory.FILES_COLUMN_FILE_NAME));
                info.fileName = FilePathUtil.getFileNameAndExtension(info.filePath);
                info.modifiedTime = Long.parseLong(cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)))*1000;
                info.isFile = !"12289".equals(cursor.getString(cursor.getColumnIndex("format")));
                if(info.isFile){
                    info.fileSize = Long.parseLong(cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)));
                }
                if(info.modifiedTime>GlobalConstants.DAY_TIME*1000){
                    todayList.add(info);
                }else if(info.modifiedTime>GlobalConstants.TREDAY_TIME*1000 && info.modifiedTime<=GlobalConstants.DAY_TIME*1000){
                    threeDaysList.add(info);
                }else{
                    sevenDaysList.add(info);
                }
            }
            filesList.clear();
            filesList.addAll(todayList);
            filesList.addAll(threeDaysList);
            filesList.addAll(sevenDaysList);
            return filesList;
        }

        @Override
        protected void onPostExecute(ArrayList<FileInfo> fileInfos) {
            if(fileInfos.size()==3){
                changeView(mEmptyView);
            }else if(fileInfos.size()>3){
                changeView(null);
                mFileListAdapter.setList(fileInfos);
                if(isActionModeState()){
                    mFileListAdapter.setActionModeState(true);
                }
                mFileListAdapter.notifyDataSetChanged();
            }
        }
    };


    @Override
    public boolean onBackPressed() {
        if(isActionModeState()){
            getActionModeManager().finishActionMode();
            setActionModeState(false);
            selectedFileSet.clear();
            return true;
        }
        return super.onBackPressed();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(fileNameInputDialog!=null && fileNameInputDialog.isShowing()){
            inputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
        }
        if(!isActionModeState()) {
            refresh();
        }
    }

    @Override
    public void onToolbarNavigationIconClicked() {
        if(getActivity()!=null){
            getActivity().onBackPressed();
        }
    }

    @Override
    public int getFragmentId() {
        return 0;
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

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FileInfo fileInfo = (FileInfo)parent.getItemAtPosition(position);
        if ((fileInfo.sectionType==GlobalConstants.SECTION_TYPE)) {
            return;
        }
        super.onItemClick(parent,view,position,id);
    }

    protected void startSelectPath(int paramOperationType) {
        getOperationType = paramOperationType;
        mToolbarManager.setToolbarTitle(mContext.getResources().getString(R.string.select_file_target));
        mToolbarManager.switchToStatus(ToolbarManager.STATUS_HOME_PATH_SELECTION);
    }

    @Override
    protected void onPasteCompleted(Message p1) {
    }


    public void refresh() {
        changeView(mLoadingView);
        clearFileList();
        getLableData();
        gethistoryList();
    }

    protected void finishActionMode(){
        if(getActivity()==null || selectedFileSet==null) return;
        if(((HbActivity)getActivity()).getActionMode()!=null){
            selectedFileSet.clear();
            setActionModeState(false);
            mActionModeManager.finishActionMode();
            mActionModeManager.setActionModeTitle("");
            mToolbarManager.switchToStatus(ToolbarManager.STATUS_HISTORY_PATH_LIST);
        }
    }

    @Override
    protected void refreshAfterPasteCompleted(int p1) {

    }


    @Override
    public void onClick(View view) {

    }


    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        switch(menuItem.getItemId()) {
            case R.id.action_menu_send:
                sendFile();
                break;
            case R.id.action_menu_cut:
                mActionModeManager.finishActionMode();
                setActionModeState(false);
                startPathListFrament(OperationType.CUT, selectedFileSet);
                break;
            case R.id.action_menu_delete:
                delete();
                break;
            case R.id.action_menu_copy:
                mActionModeManager.finishActionMode();
                setActionModeState(false);
                startPathListFrament(OperationType.COPY, selectedFileSet);
                break;
            case R.id.action_menu_rename:
                rename();
                break;
            case R.id.action_menu_compress:
                zipFile();
                break;
            case R.id.action_menu_open_with:
                ArrayList<FileInfo> set = new ArrayList<>(selectedFileSet);
                mOperationHelper.openWithMethod(set.get(0));
                finishActionMode();
                break;
            case R.id.action_menu_detail:
                showFileDetail();
                break;
        }
        return false;
    }

    private void zipFile(){
        ArrayList<FileInfo> arrayList = new ArrayList<>();
        arrayList.addAll(selectedFileSet);
        if (arrayList.size() == 0)
            return;
        FileInfo fileInfo = arrayList.get(0);
        if(arrayList.size()==1){
            createNameInputDialog(OperationType.ZIP, mContext.getResources().getString(R.string.zip_file), fileInfo.getName(), selectedFileSet);
        }else {
            createNameInputDialog(OperationType.ZIP, mContext.getResources().getString(R.string.zip_file),
                    Util.getString(mContext,R.string.and_so_on,fileInfo.getName()), selectedFileSet);
        }
    }

    protected void refreshAfterRenameCompleted1(FileInfo oldFile,FileInfo newFile){
        finishActionMode();
        if(null == filesList){
            return;
        }
        newFile.modifiedTime = oldFile.modifiedTime;
        Util.showToast(mContext,R.string.rename_success);
        /*int index = filesList.indexOf(oldFile);
        if(index!=-1) {
            filesList.add(index, newFile);
            filesList.remove(oldFile);
            mFileListAdapter.notifyDataSetChanged();
            mFileListView.setSelection(filesList.indexOf(newFile));
            Util.showToast(mContext,R.string.rename_success);
        }else{
            Util.showToast(mContext,R.string.rename_fail);
        }
        Log.d("aw","canFreshqqq:"+canFresh);*/
    }

    @Override
    protected void refreshAfterDeleteCompleted(ArrayList<FileInfo> arrayList) {
        finishActionMode();
        /*if(mParentPathChildFileNum >50) {
            changeView(mLoadingView);
        }
        if(null==filesList){
            return;
        }
        filesList.removeAll(arrayList);
        getToolbarManager().setNavigationIconAsBack();
        if(filesList.size()==3){
            changeView(mEmptyView);
        }else {
            mFileListAdapter.notifyDataSetChanged();
        }*/
    }


    private void startPathListFrament(int operationType, HashSet<FileInfo> selected) {
        PathListFragment fragment = new PathListFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(GlobalConstants.OPERATION_TYPE, operationType);
        bundle.putSerializable(GlobalConstants.OPERATION_DATA, selected);
        bundle.putInt(GlobalConstants.MODE_PATH,GlobalConstants.MODE_PATH_SELECT);
        fragment.setArguments(bundle);
        FragmentTransaction transaction = getActivity().getFragmentManager().beginTransaction();
        String tag = fragment.getClass().getSimpleName();
        transaction.replace(R.id.fragment_container,fragment,fragment.getClass().getSimpleName());
        transaction.addToBackStack(tag);
        transaction.commitAllowingStateLoss();
    }


    @Override
    public void onNameInputConfirmed(int operationType, FileInfo old, String newName) {
        switch (operationType) {
            case OperationType.ZIP:
                zip(selectedFileSet,newName);
                break;
        }
    }

    private void zip(HashSet<FileInfo> fileInfos, String newZippedName) {
        String destinationDir = FilePathUtil.getDefaultZipDirectory(mContext);
        String zippedFileName = newZippedName+".zip";
        mFileOperationTaskManager.executeZipTask(fileInfos, destinationDir, zippedFileName, new ProgressiveFileOperationTaskNotifier(mContext) {
            @Override
            public void onOperationSucceeded(int operationType, FileOperationResult result) {
                super.onOperationSucceeded(operationType, result);
                finishActionMode();
                Util.showToast(mContext, R.string.zip_success);
            }

            @Override
            public void onOperationFailed(int operationType, FileOperationResult result) {
                super.onOperationFailed(operationType, result);
                finishActionMode();
                //Util.showToast(mContext, R.string.zip_fail);
            }
        });
    }

    private class HistoryContentObserver extends ContentObserver {

        private HistoryContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange);
            Log.d("bql","HistoryContentObserver onchange uri "+uri);
            refresh();
        }
    }

    private void registerObserver() {
        if(null == mContext || null == mContext.getContentResolver())  return;
        mContext.getContentResolver().registerContentObserver(GlobalConstants.FILES_URI, false, mContentObserver);
        mContext.getContentResolver().registerContentObserver(GlobalConstants.PICTURE_URI, false, mContentObserver);
        mContext.getContentResolver().registerContentObserver(GlobalConstants.MUSIC_URI, false, mContentObserver);
        mContext.getContentResolver().registerContentObserver(GlobalConstants.VIDEO_URI, false, mContentObserver);
    }

    private void unregisterObserver() {
        if(null == mContext || null == mContext.getContentResolver()) return;
        mContext.getContentResolver().unregisterContentObserver(mContentObserver);
    }

    @Override
    public void onNameInputCancelled(int operationType, FileInfo old) {

    }
}

package cn.com.protruly.filemanager.pathFileList;

import android.app.Activity;

import cn.com.protruly.filemanager.BaseFragment;
import cn.com.protruly.filemanager.StorageVolumeManager;
import cn.com.protruly.filemanager.categorylist.ControlledImageLoadingListView;
import cn.com.protruly.filemanager.operation.FileOperationUtil;
import hb.app.dialog.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.Toast;

import cn.com.protruly.filemanager.enums.FileInfo;
import cn.com.protruly.filemanager.operation.FileOperationResult;
import cn.com.protruly.filemanager.operation.OperationType;
import cn.com.protruly.filemanager.operation.OperationHelper;
import cn.com.protruly.filemanager.ui.ActionModeManager;
import cn.com.protruly.filemanager.operation.ProgressiveFileOperationTaskNotifier;
import cn.com.protruly.filemanager.ui.OpenTypeDialogCreator;
import cn.com.protruly.filemanager.ui.ProgressDialogManager;
import cn.com.protruly.filemanager.ui.ToolbarManager;
import cn.com.protruly.filemanager.utils.FilePathUtil;
import cn.com.protruly.filemanager.utils.FileSortHelper;
import cn.com.protruly.filemanager.utils.GlobalConstants;
import cn.com.protruly.filemanager.utils.IntentBuilderUtil;
import cn.com.protruly.filemanager.utils.LogUtil;
import cn.com.protruly.filemanager.utils.Util;
import cn.com.protruly.filemanager.ziplist.ZipListFragment;
import hb.app.HbActivity;
import hb.app.dialog.ProgressDialog;
import hb.widget.ActionMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import cn.com.protruly.filemanager.R;

/**
 * Created by liushitao on 17-4-18.
 */

public abstract class BasePathListFragment extends BaseFragment implements AdapterView.OnItemClickListener,StorageVolumeManager.StorageChangeListener,
        AdapterView.OnItemLongClickListener,Handler.Callback {

    private static final String TAG = "BasePathListFragment";

    protected ArrayList<FileInfo> filesList;
    protected HashSet<FileInfo> selectedFileSet;
    protected View mListViewArea;
    protected View mLoadingView;
    protected View mEmptyView;
    protected View mShowingView;
    protected ControlledImageLoadingListView mFileListView;
    protected BaseListAdapter mFileListAdapter;
    private boolean mIsActionModeState;
    private ActionMode actionMode;
    private FileSortHelper fileSortHelper;
    private SharedPreferences mPref;
    protected OperationHelper mOperationHelper;
    protected HashSet<FileInfo> mPasteFileSet;
    private AlertDialog dialog = null;
    private ProgressDialog mProgressDialog;
    //protected int mOperationType;
    protected String storageType;
    protected String mCurrentPath;
    private ProgressDialogManager mProgressDialogManager;

    protected int getOperationType;
    protected HashSet<FileInfo> getOperationData;
    protected AbsListView mCurrentListView;
    protected int path_mode = GlobalConstants.MODE_PATH_VIEW;

    private int mInAnimRes;
    private int mOutAnimRes;
    private static final Executor SEND_FILE_EXECUTOR = Executors.newFixedThreadPool(1);
    private static final Executor SORT_FILE_EXECUTOR = Executors.newFixedThreadPool(1);

    protected abstract void initListAdapter();
    protected abstract void initListChildSelfView();
    protected FileNameInputDialog fileNameInputDialog;
    protected String phonePath;
    protected String sdPath;
    protected List<Map> otgpathMap;
    private StorageVolumeManager mStorageVolumeManager;
    //protected boolean isPickMode = false;


    @Override
    protected void initObjOnCreate(Bundle bundle) {
        super.initObjOnCreate(bundle);
        filesList = new ArrayList<>();
        selectedFileSet = new HashSet<>();
        mPasteFileSet =  new HashSet<>();
        fileSortHelper = new FileSortHelper();
        mOperationHelper = new OperationHelper(mContext,new Handler(this){
            @Override
            public void handleMessage(Message msg) {
                switch(msg.what){
                    case OperationType.RENAME:
                        if(msg.obj instanceof ArrayList) {
                            ArrayList<FileInfo> fileInfos = (ArrayList<FileInfo>)msg.obj;
                            refreshAfterRenameCompleted1(fileInfos.get(0), fileInfos.get(1));
                        }
                        break;
                    case OperationType.DELETE:
                        if(mOperationHelper!=null&&mOperationHelper.mProgressDialog!=null) {
                            mOperationHelper.mProgressDialog.cancel();
                        }
                        if(msg.obj instanceof ArrayList) {
                            ArrayList<FileInfo> fileInfos = (ArrayList<FileInfo>)msg.obj;
                            refreshAfterDeleteCompleted(fileInfos);
                            //refresh();
                        }
                        Util.showToast(mContext, R.string.delete_success);
                        break;
                    case OperationType.DELETE_UPDATEPROGRESS:
                        if(mOperationHelper!=null&&mOperationHelper.mProgressDialog!=null) {
                            mOperationHelper.mProgressDialog.setProgress((int) msg.obj);
                        }
                        break;
                    case OperationType.DELETE_EXCEPTION:
                        if(mOperationHelper!=null&&mOperationHelper.mProgressDialog!=null) {
                            mOperationHelper.mProgressDialog.cancel();
                        }
                        break;
                }
            }
        });
        mPref = PreferenceManager.getDefaultSharedPreferences(this.mContext.getApplicationContext());
        initAdpater();
        mStorageVolumeManager = StorageVolumeManager.getInstance();
        initStoragePath();
    }

    private void initAdpater(){
        initListAdapter();
        mFileListAdapter.setSelectedFileSet(selectedFileSet);
    }

    private void initStoragePath(){
        phonePath = StorageVolumeManager.getPhoneStoragePath(mContext);
        sdPath = StorageVolumeManager.getSDPath();
        otgpathMap = StorageVolumeManager.getOTGPathList();
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.d(TAG,"onResume");
        mStorageVolumeManager.setStorageChangeListener(this);
        initStoragePath();
    }

    /*@Override
    public void onPause(){
        super.onPause();
        mStorageVolumeManager.setStorageChangeListener(null);
    }*/


    @Override
    protected void initViewOnCreateView() {
        initView();
        setListViewListener();
        setListViewAdapter();
        initListChildSelfView();
    }

    protected void initView() {
        mLoadingView = rootView.findViewById(R.id.loading_view);
        mEmptyView = rootView.findViewById(R.id.empty_view);
        mListViewArea = rootView.findViewById(R.id.file_list_area);
        mFileListView = (ControlledImageLoadingListView)rootView.findViewById(R.id.show_file_list);
    }

    private void setListViewListener() {
        mFileListView.setOnItemClickListener(this);
        mFileListView.setOnItemLongClickListener(this);
    }

    private void setListViewAdapter() {
        mFileListView.setAdapter(mFileListAdapter);
    }


    @Override
    public int getFragmentId() {
        return 0;
    }

    protected abstract void getInitData();


    public void changeView(View paramView) {
        if (mShowingView == paramView) return;
        Util.hideView(mShowingView);
        Util.showView(paramView);
        mShowingView = paramView;
    }

    public void clearFileList() {
        if(filesList.size() > 0) {
            filesList.clear();
            mFileListAdapter.notifyDataSetChanged();
        }
    }

    public Comparator<FileInfo> getComparator(int i){
        return fileSortHelper.getComparator(i);
    }

    public Comparator<FileInfo> getComparator(){
        return fileSortHelper.getComparator(getSortModeOrder());
    }

    public int getSortModeOrder(){
        return mPref.getInt("sort_mode",0);
    }


    public void  setSortMode(final int sortMode){
        SharedPreferences.Editor editor = mPref.edit();
        editor.putInt("sort_mode",sortMode);
        editor.apply();
        sortFileList(sortMode);
    }

    protected void sortFileList(int sortMode){
        if(sortMode<0){
            sortMode = getSortModeOrder();
        }
        Comparator comparator = fileSortHelper.getComparator(sortMode);
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
        Collections.sort(filesList,comparator);
        mFileListAdapter.notifyDataSetChanged();
        changeView(mListViewArea);
    }

    public void sortFileList(){
        sortFileList(-1);
    }

    public boolean isActionModeState() {
        return mIsActionModeState;
    }


    private boolean needMove(String path) {
        ArrayList localArrayList = new ArrayList<>(mPasteFileSet);
        FileInfo fileInfo;
        if (localArrayList.size() > 0){
            fileInfo = (FileInfo)localArrayList.get(0);
        }else{
            return false;
        }
        if(fileInfo!=null && !path.equals(fileInfo.getParent())){
            return true;
        }
        return false;
    }

    protected void clearPasteStatus(){
        cancelPaste();
        clearOperationType();
    }

    public void cancelPaste() {
        mPasteFileSet.clear();
    }

    protected void clearOperationType() {
        getOperationType = 0;
    }

    private void clearMenu(){
        if(null != mCurrentPath) {
            deRandomStorageInfo(new FileInfo(mCurrentPath));
        }
        mToolbarManager.setToolbarTitle(storageType);
        mToolbarManager.clearToolbarMenu();
        mToolbarManager.setNavigationIconAsBack();
        inflateToolBarMenu();
    }

    protected void inflateToolBarMenu(){
        LogUtil.i2("kk","inflateToolBarMenu path_mode:"+getHidenStatus());
        if(!getHidenStatus()){
            getToolbarManager().switchToStatus(ToolbarManager.STATUS_CATEGORY_PATH_HIDE_LIST);
        }else{
            getToolbarManager().switchToStatus(ToolbarManager.STATUS_CATEGORY_PATH_LIST);
        }
        if(path_mode==GlobalConstants.MODE_PATH_SELECT){
        //if(isSelectPath){
            getToolbarManager().switchToStatus(ToolbarManager.STATUS_HOME_PATH_SELECTION);
            return;
        }
    }

    protected void inflateToolBarMenuForHiden(){
        if(!getHidenStatus()){
            getToolbarManager().switchToStatus(ToolbarManager.STATUS_CATEGORY_PATH_HIDE_LIST);
        }else{
            getToolbarManager().switchToStatus(ToolbarManager.STATUS_CATEGORY_PATH_LIST);
        }
    }

    private void deRandomStorageInfo(FileInfo fileInfo){
        String storagePath = fileInfo.getPath();
        if(null == storagePath){
            storageType="文件";
            return;
        }
        if(null != phonePath && storagePath.startsWith(phonePath) || storagePath.startsWith("/storage/emulated/0")) {
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

    private void showHorizontalProgress(int title){
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setMessage(mContext.getResources().getString(title));
        mProgressDialog.setProgressStyle(1);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    private boolean legalPaste(String str){
        Iterator iterator = mPasteFileSet.iterator();
        while(iterator.hasNext()){
            FileInfo fileInfo = (FileInfo) iterator.next();
            if(!fileInfo.isFile && (str+"/").startsWith(fileInfo.getPath()+"/") ){
                return false;
            }
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FileInfo fileInfo = (FileInfo) parent.getItemAtPosition(position);
        CheckBox checkBox = (CheckBox)view.findViewById(R.id.checkbox);
        if(mIsActionModeState){
            selectFileOrNot(fileInfo,checkBox);
            setSendMenuStatus();
            return;
        }
        viewFile(fileInfo.getPath());
        /*if(!isPickMode) {
            viewFile(fileInfo.getPath());
        }*/
    }

    protected void viewFile(String file) {
        Intent intent = IntentBuilderUtil.buildIntent(file,mContext);
        String type = intent.getType();
        Log.d(TAG,"type:"+type);
        if(file.toLowerCase().endsWith(".zip")) {
            startViewZipFile(file);
            return;
        }
        /*if(file.toLowerCase().endsWith(".vcf")) {
            new AlertDialog.Builder(mContext).setTitle(0x7f0b00bd).setPositiveButton(0x7f0b0087, new CategoryFragment.1(this, "intent")).setNegativeButton(0x7f0b000c, 0x0).show();
            return;
        }*/
        if(type != null) {
            Util.startActivityForType(mContext,intent);
            return;
        }
        //showOpenTypeDialog(intent, file);
        OpenTypeDialogCreator creator = new OpenTypeDialogCreator(getActivity(), intent, file);
        creator.show();
    }


    private void startViewZipFile(String filepath){
        Bundle bundle = new Bundle();
        ZipListFragment fragment = new ZipListFragment();
        bundle.putString(ZipListFragment.ARGS_ZIP_FILE_PATH, filepath);
        fragment.setArguments(bundle);
        FragmentTransaction transaction = ((Activity)mContext).getFragmentManager().beginTransaction();
        //transaction.replace(android.R.id.content,fragment,fragment.getClass().getSimpleName());
        String tag = fragment.getClass().getSimpleName();
        transaction.replace(R.id.fragment_container,fragment,fragment.getClass().getSimpleName());
        transaction.addToBackStack(tag);
        transaction.commitAllowingStateLoss();
    }

    /*
    private void showOpenTypeDialog(Intent intent,String path){
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("打开方式");
        builder.setView(initDialogView(intent,path));
        builder.setNegativeButton("取消",null);
        builder.create().show();
    }
    */

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        FileInfo fileInfo = (FileInfo) parent.getItemAtPosition(position);
        if ((!isActionModeState() && fileInfo.sectionType==GlobalConstants.SECTION_TYPE)) {
            return false;
        }
        if(!fileInfo.exists()){
            Toast.makeText(mContext,R.string.file_no_found,Toast.LENGTH_SHORT).show();
            return false;
        }
        CheckBox checkBox = (CheckBox)view.findViewById(R.id.checkbox);
        if(mIsActionModeState){
            selectFileOrNot(fileInfo,checkBox);
        }else{
            selectedFileSet.add(fileInfo);
            setActionModeState(true);
            startActionMode();
        }
        setSendMenuStatus();
        return true;
    }


    private boolean isContainFolder(){
        for(FileInfo fileInfo:selectedFileSet){
            if(!fileInfo.isFile){
                return true;
            }
        }
        return false;
    }

    private void setSendMenuStatus(){
        ActionModeManager actionModeManager = getActionModeManager();
        int selectedCount = selectedFileSet.size();
        boolean noneSelected = selectedCount == 0;
        boolean multipleSelected = selectedCount > 1;
        boolean isOneFile = isOneFile();
        boolean isContainFolder = isContainFolder();
        if(noneSelected) {
            actionModeManager.setAllBottomBarItemEnable(false);
        } else if(multipleSelected) {
            actionModeManager.setAllBottomBarItemEnable(true);
            actionModeManager.setBottomBarItemEnabled(R.id.action_menu_rename, false);
            actionModeManager.setBottomBarItemEnabled(R.id.action_menu_open_with, false);
            actionModeManager.showBottomNavigationMenuItem(R.id.action_menu_send,!isContainFolder);
        } else {
            actionModeManager.setAllBottomBarItemEnable(true);
            actionModeManager.showBottomNavigationMenuItem(R.id.action_menu_send,isOneFile);
            actionModeManager.setBottomBarItemEnabled(R.id.action_menu_open_with,isOneFile);
        }
    }

    private boolean isOneFile(){
        ArrayList<FileInfo> arrayList = new ArrayList<>(selectedFileSet);
        if(selectedFileSet.size()==1 && arrayList.get(0).isFile()){
            return true;
        }
        return false;
    }

    protected void selectFileOrNot(FileInfo fileInfo,CheckBox checkBox){
        if(selectedFileSet.contains(fileInfo)){
            deSelectFile(fileInfo,checkBox);
        }else{
            selectFile(fileInfo,checkBox);
        }
        updateSelectedNum();
    }

    private void deSelectFile(FileInfo fileInfo,CheckBox checkBox){
        selectedFileSet.remove(fileInfo);
        if(null != checkBox) {
            checkBox.setChecked(false);
        }
    }

    private void selectFile(FileInfo fileInfo,CheckBox checkBox){
        selectedFileSet.add(fileInfo);
        if(null != checkBox) {
            checkBox.setChecked(true);
        }
    }

    protected void setActionModeState(boolean actionModeState){
        mIsActionModeState = actionModeState;
        mFileListAdapter.setActionModeState(actionModeState);
        mFileListAdapter.notifyDataSetChanged();
    }

    private void startActionMode(){
        mActionModeManager = getActionModeManager();
        if(getActivity()!=null){
            actionMode = ((HbActivity)getActivity()).getActionMode();
            mActionModeManager.startActionMode();
            mActionModeManager.setNegativeText(com.hb.R.string.cancel);
            mActionModeManager.setPositiveText(R.string.action_mode_select_all_text);
            updateSelectedNum();
        }
        mFileListAdapter.setSelectedFileSet(selectedFileSet);
        mFileListAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        switch(menuItem.getItemId()) {
            case R.id.action_menu_send:
                sendFile();
                break;
            case R.id.action_menu_cut:
                //showSelectionPathFragment(this,OperationType.CUT);
                getActionModeManager().finishActionMode();
                startSelectPath(OperationType.CUT);
                finishSelectActionMode();
                break;
            case R.id.action_menu_delete:
                delete();
                break;
            case R.id.action_menu_copy:
                //showSelectionPathFragment(this,OperationType.COPY);
                finishSelectActionMode();
                startSelectPath(OperationType.COPY);
                break;
            case R.id.action_menu_rename:
                rename();
                break;
            case R.id.action_menu_compress:
                zip();
                break;
            case R.id.action_menu_open_with:
                ArrayList<FileInfo> set = new ArrayList<>(selectedFileSet);
                mOperationHelper.openWithMethod(set.get(0));
                finishActionMode();
                /*ArrayList<FileInfo> set = new ArrayList<>(selectedFileSet);
                if(set.size()!=0){
                    viewFile(set.get(0).filePath);
                }*/
                break;
            case R.id.action_menu_detail:
                showFileDetail();
                break;
        }
        return false;
    }

    protected void startSelectPath(int operationType){
        getOperationType = operationType;
    };


    public void showFileDetail(){
        if(selectedFileSet.size() == 0){
            return;
        }
        FileDetailDialog detailDialog = new FileDetailDialog(mContext,selectedFileSet);
        detailDialog.show();
    }

    public void sendFile(){
        SEND_FILE_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                if(selectedFileSet.size()>20){
                    ((Activity)mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Util.showToast(mContext,R.string.send_not_alown_upper_to_ten);
                        }
                    });
                    return;
                }
                /*
                Intent intent = IntentBuilderUtil.buildShareIntent(mContext,selectedFileSet);
                if(intent!=null){
                    intent =Intent.createChooser(intent,mContext.getResources().getString(R.string.send_file));
                    intent.putExtra("android.intent.extra.TITLE",mContext.getResources().getString(R.string.send_file));
                    Util.startActivityForType(mContext,intent);
                    if(mContext==null){
                        return;
                    }
                    ((Activity)mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            finishActionMode();
                        }
                    });
                }
                */
                FileOperationUtil.sendOneOrMultiple(mContext, selectedFileSet);
            }
        });
    }

    public void delete(){
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(R.string.delete_files);
        builder.setNegativeButton(android.R.string.cancel,null);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                doDelete();
            }
        });
        builder.create().show();
    }

    private void doDelete(){
        //before method
        //mOperationHelper.delete(selectedFileSet,true);
        mFileOperationTaskManager.executeDeleteTask(selectedFileSet, new ProgressiveFileOperationTaskNotifier(mContext){
            @Override
            public void onOperationSucceeded(int operationType, FileOperationResult result) {
                super.onOperationSucceeded(operationType, result);
                ArrayList<FileInfo> fileInfos = new ArrayList<>(selectedFileSet);
                refreshAfterDeleteCompleted(fileInfos);
                Util.showToast(mContext, R.string.delete_success);
            }

            @Override
            public void onOperationFailed(int operationType, FileOperationResult result) {
                super.onOperationFailed(operationType, result);
            }

            @Override
            public void onOperationCancelled(int operationType, FileOperationResult result) {
                super.onOperationCancelled(operationType, result);
            }
        });
    }

    public void rename() {
        ArrayList<FileInfo> arrayList = new ArrayList<>();
        arrayList.addAll(selectedFileSet);
        selectedFileSet.clear();
        if (arrayList.size() == 0) return;
        FileInfo localFileInfo = arrayList.get(0);
        createNameInputDialog(OperationType.RENAME, mContext.getResources().getString(R.string.rename), localFileInfo.getName(), localFileInfo);
        finishRenameActionMode();
    }

    public void zip() {
        ArrayList<FileInfo> arrayList = new ArrayList<>();
        arrayList.addAll(selectedFileSet);
        if (arrayList.size() == 0) return;
        FileInfo localFileInfo = arrayList.get(0);
        if(arrayList.size()==1){
            createNameInputDialog(OperationType.ZIP, mContext.getResources().getString(R.string.zip_file), localFileInfo.getName(), selectedFileSet);
        }else {
            createNameInputDialog(OperationType.ZIP, mContext.getResources().getString(R.string.zip_file),
                    Util.getString(mContext,R.string.and_so_on,localFileInfo.getName()), selectedFileSet);
        }
    }

    protected void finishActionMode(){
        if(getActivity()==null){
            return;
        }
        if(((HbActivity)getActivity()).getActionMode()!=null){
            selectedFileSet.clear();
            setActionModeState(false);
            getActionModeManager().finishActionMode();
            getActionModeManager().setActionModeTitle("");
            getToolbarManager().switchToStatus(ToolbarManager.STATUS_CATEGORY_PATH_LIST);
            //getToolbarManager().hideNavigationIcon();
        }
    }

    public void finishRenameActionMode(){
        if(getActivity()==null){
            return;
        }
        if(((HbActivity)getActivity()).getActionMode()!=null){
            getActionModeManager().finishActionMode();
            setActionModeState(false);
            selectedFileSet.clear();
        }
    }

    public void finishSelectActionMode(){
        if(getActivity()==null){
            return;
        }
        if(((HbActivity)getActivity()).getActionMode()!=null){
            getActionModeManager().finishActionMode();
            //selectedFileSet.clear();
            getActionModeManager().setActionModeTitle("");
            //getToolbarManager().clearToolbarMenu();
            //getToolbarManager().hideNavigationIcon();
            setActionModeState(false);
        }
    }

    @Override
    public void onActionItemClicked(ActionMode.Item item) {
        int id = item.getItemId();
        switch (id) {
            case ActionMode.POSITIVE_BUTTON:
                if(hasSelectedAll()){
                    selectNone();
                }else{
                    selectAll();
                }
                setSendMenuStatus();
                break;
            case ActionMode.NAGATIVE_BUTTON:
                selectedFileSet.clear();
                setActionModeState(false);
                getActionModeManager().finishActionMode();
                getActionModeManager().setActionModeTitle("");
                break;
        }
    }

    /*private boolean hasSelectedAll(){
        int size = filesList.size();
        for(FileInfo fileInfo:filesList){
            if("fileSection".equals(fileInfo.getPath())){
                size--;
            }
        }
        return size== selectedFileSet.size();
    }*/

    private boolean hasSelectedNone(){
        return 0 ==selectedFileSet.size();
    }

    private boolean hasSelectedLeastOne(){
        return selectedFileSet.size()>=0 && !hasSelectedAll() ;
    }

    private void selectNone(){
        selectedFileSet.clear();
        mFileListAdapter.notifyDataSetChanged();
        getActionModeManager().setPositiveText(R.string.action_mode_select_all_text);
        updateSelectedNum();
    }

    private void selectAll(){
        selectedFileSet.clear();
        for(FileInfo fileInfo:filesList){
            if(fileInfo.sectionType!=GlobalConstants.SECTION_TYPE){
                selectedFileSet.add(fileInfo);
            }
        }
        mFileListAdapter.notifyDataSetChanged();
        getActionModeManager().setPositiveText(R.string.action_mode_unselect_all_text);
        updateSelectedNum();
    }

    private boolean hasSelectedAll(){
        int size = filesList.size();
        for(FileInfo fileInfo:filesList){
            if(fileInfo.sectionType==GlobalConstants.SECTION_TYPE){
                size--;
            }
        }
        return size== selectedFileSet.size();
    }


    /*private void selectAll(){
        Log.d("nn","w:"+System.currentTimeMillis());
        selectedFileSet.clear();
        selectedFileSet.addAll(filesList);
        selectedFileSet.remove(new FileInfo("fileSection"));
        Log.d("nn","selectedFileSet.size():"+selectedFileSet.size());
        mFileListAdapter.notifyDataSetChanged();
        getActionModeManager().setPositiveText(R.string.action_mode_unselect_all_text);
        updateSelectedNum();
        Log.d("nn","e:"+System.currentTimeMillis());
    }*/


    protected void updateSelectedNum(){
        String titleFormat;
        if(Locale.getDefault().toString().equals("en_GB")||Locale.getDefault().toString().equals("en_US") &&selectedFileSet.size()==0){
            titleFormat = mContext.getResources().getString(R.string.zero_item,selectedFileSet.size());
        }else{
            titleFormat = mContext.getResources().getQuantityString(R.plurals.selected_item_count_format, selectedFileSet.size());
        }
        String title = String.format(titleFormat, selectedFileSet.size());
        getActionModeManager().setActionModeTitle(title);
        if(hasSelectedAll()){
            getActionModeManager().setPositiveText(R.string.action_mode_unselect_all_text);
        }else{
            getActionModeManager().setPositiveText(R.string.action_mode_select_all_text);
        }
    }

    protected <T> void createNameInputDialog(final int operationType,String title,String name,final T t){
        FileNameInputDialog.OnFinishFileInputListener finishFileInputListener = new FileNameInputDialog.OnFinishFileInputListener() {
            @Override
            public void onFinishFileNameInput(String str,String prefix) {
                if(Util.startWithDot(str)){
                    showConfirmStartWithDotDialog(operationType,t,str+prefix);
                    return;
                }
                doAfterNewNameConfirmed(operationType,t,str+prefix);
            }
        };
        if(null != mCurrentPath && operationType == OperationType.CREATE_FOLDER){
            fileNameInputDialog = new FileNameInputDialog(mContext,title,name,operationType,new FileInfo(mCurrentPath),finishFileInputListener);
        }else if(operationType == OperationType.RENAME){
            fileNameInputDialog = new FileNameInputDialog(mContext,title,name,(FileInfo)t,finishFileInputListener);
        }else{
            fileNameInputDialog = new FileNameInputDialog(mContext,title,name,finishFileInputListener);
        }
        if(t instanceof FileInfo){
            fileNameInputDialog.setIsFile(((FileInfo)t).isFile);
        }
        fileNameInputDialog.show();
    }

    protected  <T> void doAfterNewNameConfirmed(int operationType,T t,String str){
        switch(operationType){
            case OperationType.ZIP:
                if(t instanceof  HashSet){
                    doZip((HashSet)t,str);
                }
                break;
            case OperationType.UNZIP:break;
            case OperationType.RENAME:
                if(t instanceof FileInfo){
                     doRename((FileInfo)t,str);
                }
                break;
        }
    }

    private void ensureProgressDialogManager() {
        if(mProgressDialogManager != null) return;
        mProgressDialogManager = new ProgressDialogManager(getActivity());
    }

    private void doZip(HashSet<FileInfo> arrayList,String name){
        String destinationDir = FilePathUtil.getDefaultZipDirectory(mContext);
        String zippedFileName = name+".zip";
        mFileOperationTaskManager.executeZipTask(arrayList, destinationDir, zippedFileName, new ProgressiveFileOperationTaskNotifier(mContext) {
            @Override
            public void onOperationSucceeded(int operationType, FileOperationResult result) {
                super.onOperationSucceeded(operationType, result);
                finishActionMode();
                refresh();
                Util.showToast(mContext, mContext.getResources().getString(R.string.zip_success)+result.realDestinationPath);
            }

            @Override
            public void onOperationFailed(int operationType, FileOperationResult result) {
                super.onOperationFailed(operationType, result);
                //finishActionMode();
                //if(result != null) {
                    //Util.showToast(mContext, R.string.zip_fail);
                    //Util.showToast(mContext, result.getDescription());
                //}
            }

            @Override
            public void onOperationCancelled(int operationType, FileOperationResult result) {
                super.onOperationCancelled(operationType, result);
            }
        });
    }

    private void doRename(final FileInfo fileInfo, final String str){
        Runnable failRun = new Runnable() {
            @Override
            public void run() {
                checkoutFailureReason(fileInfo,str);
            }
        };
        mOperationHelper.rename(fileInfo,str,failRun);
    }

    private void checkoutFailureReason(FileInfo fileInfo,String string){
        Util.showToast(mContext, R.string.rename_fail);
    }


    private <T> void showConfirmStartWithDotDialog(final int operationType,final T t,final String str){
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(mContext.getResources().getString(R.string.confirm_hiden_file_create));
        builder.setPositiveButton(mContext.getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                doAfterNewNameConfirmed(operationType,t,str);
            }
        });
        builder.setNegativeButton(mContext.getResources().getString(android.R.string.cancel),null);
        builder.show();
    }

    /*protected void onPasteCompleted(Message p1) {
        dismissProgress();
        clearOperationType();
        int pasteSize = mPasteFileSet.size();
        cancelPaste();
        refreshAfterPasteCompleted(filesList.size() + pasteSize);
        showNotice(p1);
    }*/



    protected void showNotice(Message paramMessage) {
        String str = (String)paramMessage.obj;
        if (mContext == null)
            return;
        /*
        if (paramMessage.arg1 == OperationType.COPY){
            Util.showToast(this.mContext, R.string.copy_success);
        }else{
            Util.showToast(this.mContext,R.string.cut_success);
        }
        */
        if (getOperationType == OperationType.COPY){
            Util.showToast(this.mContext, R.string.copy_success);
        }else{
            Util.showToast(this.mContext,R.string.cut_success);
        }
    }

    protected void showFailureNotice(Message p1) {
        if(mContext == null) {
            return;
        }
        if(p1.arg1 == OperationType.COPY) {
            Util.showToast(mContext, R.string.copy_fail);
            return;
        }
        Util.showToast(mContext, R.string.cut_fail);
    }

    protected abstract void refreshAfterPasteCompleted(int p1);

    protected void dismissProgress() {
        if(mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    protected abstract void onPasteCompleted(Message message);

    protected abstract void refresh();

    @Override
    public boolean handleMessage(Message p1) {
        switch(p1.what){
            case OperationType.RENAME:
                if(p1.obj instanceof ArrayList) {
                    ArrayList<FileInfo> fileInfos = (ArrayList<FileInfo>)p1.obj;
                    if(fileInfos.size()<1){
                        return false;
                    }
                    refreshAfterRenameCompleted((FileInfo)fileInfos.get(0), (FileInfo)fileInfos.get(1));
                }
                return true;
            case OperationType.DELETE:
                if(p1.obj instanceof ArrayList) {
                    ArrayList<FileInfo> fileInfos = (ArrayList<FileInfo>)p1.obj;
                    if(fileInfos.size()<1){
                        return false;
                    }
                    refreshAfterDeleteCompleted(fileInfos);
                    //refresh();
                    Util.showToast(mContext, R.string.delete_success);
                }
                break;
            case 20: {
                updateProgress((Long)p1.obj, p1.arg1);
                return true;
            }
            case OperationType.COPY_SUCCEED: {
                onPasteCompleted(p1);
                return true;
            }
            case OperationType.COPY_FAIL: {
                dismissProgress();
                clearPasteStatus();
                showFailureNotice(p1);
                return true;
            }
            case OperationType.CUT_SUCCEED: {
                onPasteCompleted(p1);
                return true;
            }
            case OperationType.CUT_FAIL: {
                dismissProgress();
                clearPasteStatus();
                showFailureNotice(p1);
                return true;
            }
            case OperationType.SET_PROGRESS_MAX: {
                setProgressMaxValue((Long)p1.obj, p1.arg1);
                return true;
            }
        }
        return false;
    }

    private void setProgressMaxValue(long p1, int p2) {
        if(mProgressDialog == null) {
            return;
        }
        int maxValue = (int)p1;
        if(p2 != -1) {
            if(p1 != 0) {
                if(p1 < 100) {
                    maxValue = (int)Math.round((double)p1);
                    mProgressDialog.setProgressNumberFormat("%1d B/%2d B");
                } else {
                    maxValue = (int)Math.round(((double)p1 / 100));
                    mProgressDialog.setProgressNumberFormat("%1d MB/%2d MB");
                }
            }
        }
        mProgressDialog.setMax(maxValue);
        mProgressDialog.setProgress(0);
    }

    private void updateProgress(long progress, int paramInt) {
        if (mProgressDialog == null) return;
        int progressValue = (int)progress;
        if(paramInt==-1) return;
        if(progress==0) return;
        if(progress<100){
            progressValue = (int)Math.round((double) progress);
        }else{
            progressValue = (int)Math.round((double) progress)/100;
        }
        if(progressValue <= mProgressDialog.getMax()){
            mProgressDialog.setProgress(progressValue);
        }
    }

    protected abstract void refreshAfterRenameCompleted1(FileInfo oldFile,FileInfo newFile);

    protected void refreshAfterRenameCompleted(FileInfo oldFile,FileInfo newFile){
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
    }

    protected abstract void refreshAfterDeleteCompleted(ArrayList<FileInfo> fileInfoArrayList);

    @Override
    public void OnStorageMounted() {
        initStoragePath();
    }

    @Override
    public void OnStorageEjected() {
        initStoragePath();
    }
}

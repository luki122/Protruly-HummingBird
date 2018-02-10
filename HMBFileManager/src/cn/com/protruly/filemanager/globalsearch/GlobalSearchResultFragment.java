package cn.com.protruly.filemanager.globalsearch;


import android.app.Activity;

import cn.com.protruly.filemanager.imageloader.ImageLoader;
import cn.com.protruly.filemanager.operation.FileOperationUtil;
import hb.app.dialog.AlertDialog;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.storage.StorageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.HbSearchView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import cn.com.protruly.filemanager.R;
import cn.com.protruly.filemanager.pathFileList.FileDetailDialog;
import cn.com.protruly.filemanager.enums.FileInfo;
import cn.com.protruly.filemanager.operation.FileOperationResult;
import cn.com.protruly.filemanager.operation.OperationType;
import cn.com.protruly.filemanager.BaseFragment;
import cn.com.protruly.filemanager.HomePage.HomeFragment;
import cn.com.protruly.filemanager.pathFileList.PathListFragment;
import cn.com.protruly.filemanager.operation.OperationHelper;
import cn.com.protruly.filemanager.operation.ProgressiveFileOperationTaskNotifier;
import cn.com.protruly.filemanager.ui.ActionModeManager;
import cn.com.protruly.filemanager.ui.NameInputDialogManager;
import cn.com.protruly.filemanager.ui.ProgressDialogManager;
import cn.com.protruly.filemanager.ui.ToolbarManager;
import cn.com.protruly.filemanager.utils.FilePathUtil;
import cn.com.protruly.filemanager.utils.GlobalConstants;
import cn.com.protruly.filemanager.utils.IntentBuilderUtil;
import cn.com.protruly.filemanager.utils.LogUtil;
import cn.com.protruly.filemanager.utils.Util;
import cn.com.protruly.filemanager.ziplist.ZipListFragment;
import hb.widget.ActionMode;
import hb.widget.ActionModeListener;
import hb.widget.HbListView;

import android.os.storage.StorageEventListener;
import android.os.storage.VolumeInfo;
import android.os.storage.DiskInfo;

public class GlobalSearchResultFragment extends BaseFragment implements ActionModeListener,
        AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener, HbSearchView.OnQueryTextListener,
        Handler.Callback, SearchAsyncTask.SearchAsyncTaskListener{

    private static final String TAG = "GlobalSearchResultFragment";

    protected View mSearchAndListArea;
    protected View mLoadingView;
    protected View mEmptyView;
    protected TextView loadingText;
    protected GlobalSearchReaultAdapter globalsearchAdapter;
    protected SearchAsyncTask searchAsyncTask;
    protected HbListView pinned_section_list;
    protected String keyword;
    public ActionModeManager actionModeManager;
    protected ToolbarManager mToolbarManager;
    protected static boolean mIsActionModeState;
    private AlertDialog dialog = null;
    protected int mOperationType;
    private Boolean sortEnableFlag;
    private Boolean hidenFile = true;
    private ImageLoader mImageLoader;
    protected HbSearchView mToolbarSearchView;

    private GlobalSearchHisDbHelper helper;

    protected HashSet<FileInfo> selectedFileSet;
    private SearchSectionManager mSearchSectionManager;
    private static final Executor SEND_FILE_EXECUTOR = Executors.newFixedThreadPool(1);
    protected OperationHelper mOperationHelper;
    private ProgressDialogManager mProgressDialogManager;
    private Boolean enableAutoSearchflog;

    private StorageManager mStorageManager;
    private List StoragePathList;
    private NameInputDialogManager renamemanager;
    private NameInputDialogManager zipmanager;


    @Override
    public void onCreate(Bundle savedInstanceSate) {
        super.onCreate(savedInstanceSate);
        mIsActionModeState = false;
        enableAutoSearchflog=true;
        mToolbarManager=getToolbarManager();
        actionModeManager=getActionModeManager();
        actionModeManager.setActionModeListener(this);
        helper = new GlobalSearchHisDbHelper(mContext);
        mOperationHelper = new OperationHelper(mContext,new Handler(this));


        mSearchSectionManager = new SearchSectionManager(mContext);
        selectedFileSet = mSearchSectionManager.selectedFileSet;

        globalsearchAdapter = new GlobalSearchReaultAdapter(mContext,mSearchSectionManager);

        mStorageManager =  mContext.getSystemService(StorageManager.class);
        //StoragePathList = new ArrayList<String>();
        StoragePathList = getStoragePathList(mContext);
        mImageLoader = ImageLoader.getInstance(mContext);
        globalsearchAdapter.setImageLoader(mImageLoader);

    }



    @Override
    protected void initRootView(LayoutInflater inflater, ViewGroup contaner) {
        rootView = inflater.inflate(R.layout.layout_global_search_result, contaner, false);
        mToolbarManager.switchToStatus(ToolbarManager.STATUS_GLOBALSEARCH_RESULT);
        super.initRootView(inflater, contaner);

    }

    @Override
    protected void initViewOnCreateView() {
        super.initViewOnCreateView();
        mLoadingView = rootView.findViewById(R.id.loading_view);
        mEmptyView = rootView.findViewById(R.id.empty_view);
        mSearchAndListArea = rootView.findViewById(R.id.file_list_area);
        loadingText = (TextView) rootView.findViewById(R.id.tv_loading_tip);
        //实例化拥有悬停头的控件
        pinned_section_list = (PinnedSectionListView) rootView.findViewById(R.id.globalsearch_listview);
       // pinned_section_list.setOnScrollListener(mOnScrollListener);
    }

    @Override
    protected void getInitData() {
        showListView();

        globalsearchAdapter.setActionModeManager(mActionModeManager);
        pinned_section_list.setAdapter(globalsearchAdapter);
        pinned_section_list.setOnItemClickListener(this);
        pinned_section_list.setOnItemLongClickListener(this);

        InitSearchView();

    }

    public void InitSearchView() {
        Menu menu = mActivity.getOptionMenu();
        MenuItem item = menu.findItem(R.id.menu_search);
        if(item==null) return;
        mToolbarSearchView = (HbSearchView) item.getActionView();
        mToolbarSearchView.setMaxWidth(800);
        mToolbarSearchView.setIconifiedByDefault(true);
        mToolbarSearchView.setIconified(false);
        mToolbarSearchView.clearFocus();
        mToolbarSearchView.setOnQueryTextListener(this);
        hidenFile = getHidenStatus();
        mSearchSectionManager.setHidenFileStatus(hidenFile);
        if(enableAutoSearchflog) {
            mToolbarSearchView.setQuery(getArguments().getString("keyword"),false);
        }else{
            mToolbarSearchView.setQueryHint(keyword);
        }
    }



    @Override
    public void onResume() {
        super.onResume();
        StoragePathList = getStoragePathList(mContext);
        mStorageManager.registerListener(mStorageListener);
        if((renamemanager!=null && renamemanager.isShowingDialog())||(zipmanager!=null && zipmanager.isShowingDialog())){
            Log.d("soft","callback:");
            inputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        mStorageManager.unregisterListener(mStorageListener);
    }


    @Override
    public boolean onQueryTextSubmit(String s) {
        String keyword=s.trim();
        if(keyword!=null && !keyword.equals("")){
            goToSearch(keyword);
        }
        return true;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        String keyword=s.trim();
        if(keyword!=null && !keyword.equals("")){
                goToSearch(keyword);
        }else{
            if(searchAsyncTask != null && searchAsyncTask.getStatus() != AsyncTask.Status.FINISHED){
                searchAsyncTask.cancel(true);
                searchAsyncTask = null;
            }
            startSearchHisFragment();
        }
        return true;
    }


    /**
     * 检查数据库中是否已经有该条记录
     */
    private boolean hasData(String tempName) {
        // Cursor cursor = helper.getReadableDatabase().rawQuery(querycmd,new String[]{tempName});
        Cursor cursor = helper.getReadableDatabase().rawQuery(
                "select _id,name from records where name =?", new String[]{"%"+tempName+"%"});
        //判断是否有下一个
        Boolean hasdata = cursor.moveToNext();
        cursor.close();
        return hasdata;
    }
    /**
     * 插入数据
     */
    private void insertData(String tempName) {
        SQLiteDatabase db = helper.getWritableDatabase();
       // db.execSQL("insert into records(name) values('" + tempName + "')");
        ContentValues values = new ContentValues();
        values.put("name", tempName);
        db.insert("records",null,values);
        db.close();
    }


    private void goToSearch(String keyword){

            this.keyword = keyword;
            if(searchAsyncTask != null && searchAsyncTask.getStatus() != AsyncTask.Status.FINISHED){
                searchAsyncTask.cancel(true);
            }
            //else {
                searchAsyncTask = new SearchAsyncTask(mContext,null,null,globalsearchAdapter,mSearchSectionManager);
                searchAsyncTask.setSelectionListener(this);
                loadingText.setText(mContext.getResources().getString(R.string.loading_tip));
                searchAsyncTask.setHidenStatus(hidenFile);
                searchAsyncTask.setSearchMessageText(loadingText);
                searchAsyncTask.setStoragePathList(StoragePathList);
                searchAsyncTask.setKeyWord(keyword);
                searchAsyncTask.execute();

           // }
    }


    private final StorageEventListener mStorageListener = new StorageEventListener() {

        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            if (isInteresting(vol) &&
                    ((oldState == VolumeInfo.STATE_CHECKING && newState == VolumeInfo.STATE_MOUNTED)
                    ||(oldState == VolumeInfo.STATE_MOUNTED && newState == VolumeInfo.STATE_EJECTING))) {
                StoragePathList = getStoragePathList(mContext);
                if(keyword!=null)goToSearch(keyword);
                if(mToolbarSearchView!=null)mToolbarSearchView.clearFocus();
            }
        }

        public void onDiskDestroyed(DiskInfo disk) {
            StoragePathList = getStoragePathList(mContext);
        }


    };

    private  boolean isInteresting(VolumeInfo vol) {
        switch(vol.getType()) {
            case VolumeInfo.TYPE_PRIVATE:
            case VolumeInfo.TYPE_PUBLIC:
                return true;
            default:
                return false;
        }
    }

    public static ArrayList<String> getStoragePathList(final Context context){

        ArrayList<String> pathList = new ArrayList<>();
        String firstPath = Environment.getExternalStorageDirectory().getPath();
        pathList.add(firstPath);
        StorageManager mStorageManager = (StorageManager)context.getSystemService(Context.STORAGE_SERVICE);
        final List<VolumeInfo> vols = mStorageManager.getVolumes();
        for(VolumeInfo vol : vols){
            if(vol.getType()==VolumeInfo.TYPE_PUBLIC)
                if(vol.isMountedReadable()) {
                    pathList.add(vol.getPath().toString());
                }
        }
        return pathList;
    }


    public GlobalSearchReaultAdapter getGlobalsearchAdapter() {
        return globalsearchAdapter;
    }
    public SearchSectionManager SearchSectionManager() {
        return mSearchSectionManager;
    }


    @Override
    public void onToolbarNavigationIconClicked() {

        if(searchAsyncTask != null && searchAsyncTask.getStatus() != AsyncTask.Status.FINISHED){
            searchAsyncTask.cancel(true);
            searchAsyncTask = null;
        }
            if(mToolbarSearchView!=null)mToolbarSearchView.clearFocus();
            startHomeFragment();
    }

    @Override
    public boolean onBackPressed() {
        if(isActionModeState()){
            setActionModeState(false);
            return true;
        }else {

            if(searchAsyncTask != null && searchAsyncTask.getStatus() != AsyncTask.Status.FINISHED){
                searchAsyncTask.cancel(true);
                searchAsyncTask = null;
            }
            startHomeFragment();
            return true;
        }

    }


    @Override
    public int getFragmentId() {
        return 0;
    }


    protected void showLoadingView() {
        Util.showView(mLoadingView);
        Util.hideView(mSearchAndListArea);
        Util.hideView(mEmptyView);
    }

    protected void showEmptyView() {
        Util.hideView(mLoadingView);
        Util.hideView(mSearchAndListArea);
        Util.showView(mEmptyView);
    }

    protected void showListView() {
        Util.hideView(mLoadingView);
        Util.showView(mSearchAndListArea);
        Util.hideView(mEmptyView);
    }

    @Override
    public void onActionItemClicked(ActionMode.Item item) {
        super.onActionItemClicked(item);

        int id = item.getItemId();
        switch (id) {
            case ActionMode.POSITIVE_BUTTON:

                if(hasSelectedAll()){
                    selectNone();
                }else{
                    selectAll();
                }
                setMenuStatus();
                break;
            case ActionMode.NAGATIVE_BUTTON:

                setActionModeState(false);
                break;
        }

    }





    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

        FileInfo fileInfo = (FileInfo)adapterView.getItemAtPosition(i);
        if(fileInfo.sectionType == GlobalConstants.SECTION_TYPE)return;

        String path = fileInfo.getPath();
        if(!isActionModeState()) {
            enableAutoSearchflog=false;
            if (fileInfo.isDirectory()) {
                startNewPathListFragment(fileInfo);
            } else {
                viewFile(fileInfo.getPath());
            }
        }else{
           // CheckBox checkBox = (CheckBox)view.findViewById(R.id.checkbox);
            GlobalSearchReaultAdapter.ViewHolder viewHolder = (GlobalSearchReaultAdapter.ViewHolder) view.getTag();
            selectFileOrNot(fileInfo,viewHolder.checkbox);
            setMenuStatus();
            return;
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {

        FileInfo fileInfo = (FileInfo)adapterView.getItemAtPosition(i);
        if(fileInfo.sectionType ==GlobalConstants.SECTION_TYPE)return false;

        if(!isActionModeState()){
            setActionModeState(true);
        }
        return false;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        switch(menuItem.getItemId()) {
            case R.id.action_menu_send:
                sendFile();
                break;
            case R.id.action_menu_cut:
                this.mIsActionModeState = false;
                getActionModeManager().finishActionMode();
                startPathListFrament(OperationType.CUT);
                break;
            case R.id.action_menu_delete:
                delete();
                break;
            case R.id.action_menu_copy:
                this.mIsActionModeState = false;
                getActionModeManager().finishActionMode();
                startPathListFrament(OperationType.COPY);
                break;
            case R.id.action_menu_rename:
                rename();
                break;
            case R.id.action_menu_compress:
                compress();
                break;
            case R.id.action_menu_open_with:
                ArrayList<FileInfo> set = new ArrayList<>(selectedFileSet);
                mOperationHelper.openWithMethod(set.get(0));
                /*ArrayList<FileInfo> set = new ArrayList<>(selectedFileSet);
                viewFile(set.get(0).filePath);*/
                break;
            case R.id.action_menu_detail:
                showFileDetail();
                break;
        }
        return false;
    }


    private void rename(){

        ArrayList<FileInfo> selectedData = new ArrayList<FileInfo>(selectedFileSet);
        if (selectedData.size() < 1) {
            return ;
        }
        FileInfo fileInfo = (FileInfo) selectedData.get(0);
        //createNameInputDialog(OperationType.RENAME, "重命名", fileInfo.getName(), fileInfo);
        String title = mContext.getString(R.string.rename);
        renamemanager = new NameInputDialogManager(mContext, OperationType.RENAME, new NameInputDialogManager.NameInputDialogListener() {
            @Override
            public void onNameInputConfirmed(int operationType, FileInfo old, String newName) {
                doRename(old, newName);
            }
            @Override
            public void onNameInputCancelled(int operationType, FileInfo old) {
            }
        });

        renamemanager.createNameInputDialog(title, fileInfo.getName(), fileInfo);

    }

    private void compress(){

        ArrayList<FileInfo> arrayList = new ArrayList<>(selectedFileSet);
        if (arrayList.size() == 0) return;
        String FileInfoName;
        if(arrayList.size()==1){
            FileInfoName = arrayList.get(0).fileName;
        }else{
            FileInfoName = Util.getString(mContext,R.string.and_so_on,arrayList.get(0).getName());
        }
        zipmanager = new NameInputDialogManager(mContext, OperationType.ZIP, new NameInputDialogManager.NameInputDialogListener() {
        @Override
        public void onNameInputConfirmed(int operationType,final FileInfo old,final String newName) {
            String destinationDir = FilePathUtil.getDefaultZipDirectory(mContext);
            String zippedFileName = newName + ".zip";

            mFileOperationTaskManager.executeZipTask(selectedFileSet, destinationDir, zippedFileName,new ProgressiveFileOperationTaskNotifier(mContext){
                @Override
                public void onOperationSucceeded(int operationType, FileOperationResult result) {
                    super.onOperationSucceeded(operationType, result);
                    Util.showToast(mContext, R.string.zip_success);
                    setActionModeState(false);
                }
            });
        }
        @Override
        public void onNameInputCancelled(int operationType, FileInfo old) {
        }
        });

        zipmanager.createNameInputDialog(getString(R.string.optiontitle_compress), FileInfoName, null);
    }

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
                            Util.showToast(mContext,getString(R.string.send_not_alown_upper_to_ten));
                        }
                    });
                    return;
                }

                /*
                Intent intent = IntentBuilderUtil.buildShareIntent(mContext,selectedFileSet);
                if(intent!=null){
                    intent =Intent.createChooser(intent, getString(R.string.action_menu_send));
                    intent.putExtra("android.intent.extra.TITLE",getString(R.string.action_menu_send));
                    Util.startActivityForType(mContext,intent);
                }
                */

                FileOperationUtil.sendOneOrMultiple(mContext, selectedFileSet);
            }
        });
    }

    public void delete(){
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(R.string.optiontitle_delete);
        builder.setNegativeButton(R.string.cancel,null);
        builder.setPositiveButton(R.string.confirm_know, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                doDelete();
            }
        });
        builder.create().show();
    }

    private void doDelete(){

        //mOperationHelper.delete(selectedFileSet,true);
        mFileOperationTaskManager.executeDeleteTask(selectedFileSet, new ProgressiveFileOperationTaskNotifier(mContext){
            @Override
            public void onOperationSucceeded(int operationType, FileOperationResult result) {
                super.onOperationSucceeded(operationType, result);
                mSearchSectionManager.deleteFromFileList(mSearchSectionManager.fileinfolist,selectedFileSet);
                setActionModeState(false);
                Util.showToast(mContext, R.string.delete_success);
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
        Util.showToast(mContext,R.string.rename_fail);
    }

    protected void startSelectPath(int operationType){
        mOperationType = operationType;
    };



    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        if(!sortEnableFlag) return false;
        int id = menuItem.getItemId();
        switch (id) {

            case R.id.category_sort_by_name:
                sortFileList(0);
                break;
            case R.id.category_sort_by_time:
                sortFileList(1);
                break;
            case R.id.category_sort_by_size_asc:
                sortFileList(2);
                break;
            case R.id.category_sort_by_size_dsc:
                sortFileList(3);
                break;

            default:

                break;
        }
        return false;
    }



    public void setActionModeState(boolean isActionMode){

          selectedFileSet.clear();
          if(!isActionMode) {
              getActionModeManager().finishActionMode();
          }else{
              actionModeManager.startActionMode();
              actionModeManager.setNegativeText(getString(R.string.cancel));
              actionModeManager.setPositiveText(R.string.action_mode_select_all_text);
              actionModeManager.showBottomNavigationMenuItem(R.id.action_menu_send, true);
              updateSelectedNum();
              enableAutoSearchflog = false;
          }
          this.mIsActionModeState = isActionMode;
          getGlobalsearchAdapter().notifyDataSetChanged(false);
    }


    public boolean isActionModeState(){
        return mIsActionModeState;
    }

    private void startHomeFragment(){

        HomeFragment fragment= new HomeFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container,fragment,fragment.getClass().getSimpleName());
        transaction.commitAllowingStateLoss();
    }
    private void startSearchHisFragment(){


           FragmentTransaction transaction = getActivity().getFragmentManager().beginTransaction();
           GlobalSearchHistoryFragment fragment= new GlobalSearchHistoryFragment();
           //FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container,fragment,fragment.getClass().getSimpleName());
        transaction.commitAllowingStateLoss();

    }

    private void startNewPathListFragment(FileInfo p1) {

        enableAutoSearchflog =false;
        FragmentTransaction transaction = getActivity().getFragmentManager().beginTransaction();
        PathListFragment fragment = new PathListFragment();
        Bundle bundle = new Bundle();
        LogUtil.d(TAG,"fileInfo p1:"+p1.getPath());
        bundle.putSerializable(GlobalConstants.FROM_SEARCH,new File(p1.getPath()));
        fragment.setArguments(bundle);
        String tag = fragment.getClass().getSimpleName();
        transaction.replace(R.id.fragment_container,fragment,fragment.getClass().getSimpleName());
        transaction.addToBackStack(tag);
        transaction.commitAllowingStateLoss();
    }

    private void startPathListFrament(int type){

        enableAutoSearchflog =true;
        PathListFragment fragment = new PathListFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(GlobalConstants.OPERATION_TYPE, type);
       // Log.d("zxd","startPathListFrament mAdapter.getSelectedData():"+mAdapter.getSelectedData());
        bundle.putInt(GlobalConstants.MODE_PATH,GlobalConstants.MODE_PATH_SELECT);
        bundle.putSerializable(GlobalConstants.OPERATION_DATA,selectedFileSet);
        fragment.setArguments(bundle);
        FragmentTransaction transaction = getActivity().getFragmentManager().beginTransaction();
        String tag = fragment.getClass().getSimpleName();
        transaction.replace(R.id.fragment_container,fragment,fragment.getClass().getSimpleName());
        transaction.addToBackStack(tag);
        transaction.commitAllowingStateLoss();
    }

    private void viewFile(String file) {
        Intent intent = IntentBuilderUtil.buildIntent(file,mContext);
        String type = intent.getType();

        if(type != null) {
            if (type.equals("application/zip")) {
                ZipListFragment.openMe((Activity) mContext, file);
                return;
            } else {
                Log.d(TAG, "viewFile type:" + type);
                Util.startActivityForType(mContext,intent);
                return;
            }
        }

        showOpenTypeDialog(intent, file);
    }

    private void showOpenTypeDialog(Intent intent,String path){
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.open_type);
        builder.setView(initDialogView(intent,path));
        builder.setNegativeButton(R.string.cancel,null);
        builder.create().show();
    }

    private View initDialogView(final Intent intent,final String path){
        View view = LayoutInflater.from(mContext).inflate(R.layout.file_open_layout,null);
        final Uri uri = Util.getUriForFile(mContext,new File(path));
        view.findViewById(R.id.file_document).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent.setDataAndType(uri,IntentBuilderUtil.buildType(0));
                startActivity(intent);
                if(dialog!=null){
                    dialog.dismiss();
                }
            }
        });

        view.findViewById(R.id.file_music).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent.setDataAndType(uri,IntentBuilderUtil.buildType(1));
                startActivity(intent);
                if(dialog!=null){
                    dialog.dismiss();
                }
            }
        });

        view.findViewById(R.id.file_video).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent.setDataAndType(uri,IntentBuilderUtil.buildType(2));
                startActivity(intent);
                if(dialog!=null){
                    dialog.dismiss();
                }
            }
        });

        view.findViewById(R.id.file_picture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent.setDataAndType(uri,IntentBuilderUtil.buildType(3));
                startActivity(intent);
                if(dialog!=null){
                    dialog.dismiss();
                }
            }
        });

        return view;
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
        checkBox.setChecked(false);
    }

    private void selectFile(FileInfo fileInfo,CheckBox checkBox){
        selectedFileSet.add(fileInfo);
        checkBox.setChecked(true);
    }

    private void updateSelectedNum(){
        String titleFormat;
        if(Locale.getDefault().toString().equals("en_GB")||Locale.getDefault().toString().equals("en_US") && selectedFileSet.size()==0){
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

    private boolean hasSelectedAll(){
        return mSearchSectionManager.getAllFileListNum()==selectedFileSet.size();
    }

    private boolean hasSelectedNone(){
        return 0 ==selectedFileSet.size();
    }

    private void selectNone(){
        selectedFileSet.clear();
        globalsearchAdapter.notifyDataSetChanged(false);
        getActionModeManager().setPositiveText(R.string.action_mode_select_all_text);
        updateSelectedNum();
    }

    private void selectAll(){
        mSearchSectionManager.selectAllFileList();
        globalsearchAdapter.notifyDataSetChanged(false);
        getActionModeManager().setPositiveText(R.string.action_mode_unselect_all_text);
        updateSelectedNum();
    }

    private void setMenuStatus(){
        if(!mIsActionModeState) return;
        if(selectedFileSet.size()==0){

            getActionModeManager().showBottomNavigationMenuItem(R.id.action_menu_send,false);
            getActionModeManager().showBottomNavigationMenuItem(R.id.action_menu_cut,false);
            getActionModeManager().showBottomNavigationMenuItem(R.id.action_menu_delete,false);
            getActionModeManager().showBottomNavigationMenuItem(R.id.action_menu_copy,false);
            getActionModeManager().showBottomNavigationMenuItem(R.id.action_menu_rename,false);
            getActionModeManager().showBottomNavigationMenuItem(R.id.action_menu_compress,false);
            getActionModeManager().showBottomNavigationMenuItem(R.id.action_menu_open_with,false);
            getActionModeManager().showBottomNavigationMenuItem(R.id.action_menu_detail,false);

            return;

        }else{
            getActionModeManager().showBottomNavigationMenuItem(R.id.action_menu_send,true);
            getActionModeManager().showBottomNavigationMenuItem(R.id.action_menu_cut,true);
            getActionModeManager().showBottomNavigationMenuItem(R.id.action_menu_delete,true);
            getActionModeManager().showBottomNavigationMenuItem(R.id.action_menu_copy,true);
            getActionModeManager().showBottomNavigationMenuItem(R.id.action_menu_rename,true);
            getActionModeManager().showBottomNavigationMenuItem(R.id.action_menu_compress,true);
            getActionModeManager().showBottomNavigationMenuItem(R.id.action_menu_open_with,true);
            getActionModeManager().showBottomNavigationMenuItem(R.id.action_menu_detail,true);
        }

        if(isContainFolder()){
            getActionModeManager().showBottomNavigationMenuItem(R.id.action_menu_send,false);
        }

        if(!isOneFile()){
            getActionModeManager().showBottomNavigationMenuItem(R.id.action_menu_rename,false);
            getActionModeManager().showBottomNavigationMenuItem(R.id.action_menu_open_with,false);
        }
    }


    private boolean isContainFolder(){
        for(FileInfo fileInfo:selectedFileSet){
            if(fileInfo.isDirectory()){
                return true;
            }
        }
        return false;
    }

    private boolean isOneFile(){
        if(selectedFileSet.size()==1){
            return true;
        }
        return false;
    }

    @Override
    public void onStartSearch() {
        Log.d(TAG,"onStartSearch");
        sortEnableFlag = false;
        showLoadingView();
    }

    @Override
    public void doInSearch() {
        Log.d(TAG,"doInSearch");

    }

    @Override
    public void onEndSearch(List<FileInfo> data) {
        Log.d(TAG,"onEndSearch");
        if(data.size()>mSearchSectionManager.sectionPath.length){
            showListView();
            if (!hasData(keyword)) { //满足两个条件才能存入  无历史 有搜索结果
                insertData(keyword);
            }
        }else{
            showEmptyView();
        }
        sortEnableFlag = true;
        if(mToolbarSearchView!=null)mToolbarSearchView.clearFocus();
    }

    @Override
    public boolean handleMessage(Message msg) {

        switch(msg.what){
            case OperationType.RENAME:
                FileInfo oldfileinfo = ((ArrayList<FileInfo>)(msg.obj)).get(0);
                FileInfo newfileinfo = ((ArrayList<FileInfo>)(msg.obj)).get(1);
                mSearchSectionManager.reNameFromFileList(keyword,oldfileinfo,newfileinfo.getName());
                setActionModeState(false);
                //goToSearch(keyword);
                break;
        }
        return false;
    }


    private void setMenuSortEnable(Boolean enable) {
        if(null == mToolbarManager) return;
        int [] menuIds = new int[] {R.id.category_sort_by_name, R.id.category_sort_by_time,
                R.id.category_sort_by_size_asc, R.id.category_sort_by_size_dsc};
        for(int i=0; i<menuIds.length; i++) {
            int menuId = menuIds[i];
           // boolean checked = currentSortOrder == i;
            mToolbarManager.setToolbarMenuItemEnable(menuId, enable);
        }
    }

    private  SortAsyncTask mSortAsyncTask;
    protected void sortFileList(int sortMode){
        if(mSearchSectionManager==null || mSearchSectionManager.fileinfolist.size()<=3)return;
        if(mToolbarSearchView!=null)mToolbarSearchView.clearFocus();
       if(mSortAsyncTask!=null && mSortAsyncTask.getStatus()!=AsyncTask.Status.FINISHED){
           mSortAsyncTask.cancel(true);
       }
        mSortAsyncTask = new SortAsyncTask(sortMode);
        mSortAsyncTask.execute();

    }

    class SortAsyncTask extends AsyncTask{
        int sortmode;

        public SortAsyncTask(int sortmode) {
            super();
            this.sortmode = sortmode;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
           if(loadingText!=null) {
               if(sortmode==2 || sortmode==3) {
                   loadingText.setText(mContext.getResources().getString(R.string.loading_calculator_tip));
               }else{
                   loadingText.setText(mContext.getResources().getString(R.string.loading_tip));
               }
           }
            showLoadingView();
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            showListView();
            mSearchSectionManager.refreshFileList();
            getGlobalsearchAdapter().notifyDataSetChanged(true);
            if(mToolbarSearchView!=null)mToolbarSearchView.clearFocus();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            showListView();
            getGlobalsearchAdapter().notifyDataSetChanged(true);
        }

        @Override
        protected Object doInBackground(Object[] params) {

            mSearchSectionManager.sortFileList(sortmode);
            return null;
        }
    }





  /*  AbsListView.OnScrollListener mOnScrollListener = new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            GlobalSearchReaultAdapter madapter = getGlobalsearchAdapter();
            if(madapter!=null){
                getGlobalsearchAdapter().setScrollStatusFlag(scrollState);
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

        }
    };*/
}

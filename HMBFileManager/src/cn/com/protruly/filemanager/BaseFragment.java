package cn.com.protruly.filemanager;

import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import cn.com.protruly.filemanager.CategoryActivity;
import cn.com.protruly.filemanager.operation.FileDbManager;
import cn.com.protruly.filemanager.operation.FileOperationTaskManager;
import cn.com.protruly.filemanager.ui.ActionModeManager;
import cn.com.protruly.filemanager.ui.ToolbarManager;
import cn.com.protruly.filemanager.utils.LogUtil;
import cn.com.protruly.filemanager.OnBackPressHandlerInterface;
import cn.com.protruly.filemanager.utils.SingleMediaScanner;
import hb.app.HbActivity;
import hb.view.menu.bottomnavigation.BottomNavigationView;
import hb.widget.ActionMode;
import cn.com.protruly.filemanager.R;
import hb.widget.ActionModeListener;
import hb.widget.toolbar.Toolbar;

/**
 * Created by liushitao on 17-4-14.
 */

public abstract class BaseFragment extends Fragment implements BottomNavigationView.OnNavigationItemSelectedListener,
        ActionModeListener,
        ToolbarManager.OnToolbarNavigationIconClickListener,
        Toolbar.OnMenuItemClickListener {

    private static final String TAG = "BaseFragment";
    protected Context mContext;
    protected View rootView;
    protected AsyncTask<String, Void, Object> mAsyncTask;

    protected ToolbarManager mToolbarManager;
    protected ActionModeManager mActionModeManager;

    protected boolean mNeedNavigationIconImmediateBack;

    protected HbActivity mActivity;

    protected FileDbManager mFileDbManager;
    protected FileOperationTaskManager mFileOperationTaskManager;
    protected InputMethodManager inputMethodManager;

    public void onCreate(Bundle savedInstanceSate){
        super.onCreate(savedInstanceSate);
        setHasOptionsMenu(true);
        mContext = getActivity();
        initObjOnCreate(savedInstanceSate);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (HbActivity)context;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        initRootView(inflater,container);
        initViewOnCreateView();
        getInitData();
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        ((OnBackPressHandlerInterface)getActivity()).setCurrentFragment(this);
    }

    public void setFileDbManager(FileDbManager fileDbManager) {
        mFileDbManager = fileDbManager;
    }

    public void setFileOperationTaskManager(FileOperationTaskManager taskManager) {
        mFileOperationTaskManager = taskManager;
    }

    public FileDbManager getFileDbManager() { return mFileDbManager; }

    public FileOperationTaskManager getFileOperationTaskManager() {
        return mFileOperationTaskManager;
    }
    /*
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        LogUtil.i(TAG, "onCreateOptionsMenu................");
        mMenu = menu;
    }
    */

    @Override
    public void onResume() {
        super.onResume();
        mToolbarManager = getToolbarManager();
        mActionModeManager = getActionModeManager();
        mToolbarManager.setOnNavigationIconClickListener(this);
        mToolbarManager.setOnToolbarMenuItemClickListener(this);
        mActionModeManager.setActionModeListener(this);
        mActionModeManager.setOnNavigationItemSelectedListener(this);
        inputMethodManager = getInputMethodManager();
    }

    protected void initObjOnCreate(Bundle bundle){
        LogUtil.i(TAG, "initObjOnCreate");
        HbActivity activity = (HbActivity)getActivity();
        mToolbarManager = new ToolbarManager(activity);
        mActionModeManager = new ActionModeManager(activity);
        inputMethodManager = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        /*
        mToolbarManager.setOnNavigationIconClickListener(this);
        mToolbarManager.setOnToolbarMenuItemClickListener(this);
        mActionModeManager.setActionModeListener(this);
        mActionModeManager.setOnNavigationItemSelectedListener(this);
        */
    }

    public ToolbarManager getToolbarManager() {
        return mToolbarManager;
    }

    public ActionModeManager getActionModeManager() {
        return mActionModeManager;
    }

    public InputMethodManager getInputMethodManager(){
        return inputMethodManager;
    }

    protected void initRootView(LayoutInflater inflater,ViewGroup contaner){}

    protected void initViewOnCreateView(){}

    public abstract int getFragmentId();

    protected abstract void getInitData();

    protected void cancelAsyncTask() {
        if(mAsyncTask != null) {
            mAsyncTask.cancel(true);
            mAsyncTask = null;
        }
    }

    public boolean onBackPressed() {
        LogUtil.i(TAG, "BaseFragment::onBackPressed");
        return false;
    }

    //call back for BottomNavigationBar Begin
    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        return false;
    }
    //call back for BottomNavigationBar End

    //callbacks for ActionMode Begin
    @Override
    public void onActionItemClicked(ActionMode.Item item) {

    }

    @Override
    public void onActionModeShow(ActionMode actionMode) {

    }

    @Override
    public void onActionModeDismiss(ActionMode actionMode) {

    }
    //callbacks for ActionMode End

    //Tool bar Navigation icon begin
    @Override
    public void onToolbarNavigationIconClicked() {

    }
    //Tool bar Navigation icon end

    //Tool bar menu items Begin
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return false;
    }
    //Tool bar menu items End

    public boolean isNeedNavigationIconImmediateBack() {
        return mNeedNavigationIconImmediateBack;
    }

    public void setNeedNavigationIconImmediateBack(boolean immediateBack) {
        mNeedNavigationIconImmediateBack = immediateBack;
    }

    public boolean getHidenStatus(){
        if(getActivity()==null) return true;
        return ((CategoryActivity)getActivity()).getHidenStatus();
    }

    public void setHidenStatus(boolean hiden){
        ((CategoryActivity)getActivity()).setHidenStatus(hiden);
    }
}

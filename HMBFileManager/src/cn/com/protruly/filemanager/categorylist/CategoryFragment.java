package cn.com.protruly.filemanager.categorylist;

import android.animation.ObjectAnimator;
import android.app.Activity;

import cn.com.protruly.filemanager.imageloader.ImageLoader;
import hb.app.dialog.AlertDialog;

import android.app.FragmentTransaction;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

import cn.com.protruly.filemanager.pathFileList.FileDetailDialog;
import cn.com.protruly.filemanager.operation.OperationType;
import cn.com.protruly.filemanager.BaseFragment;
import cn.com.protruly.filemanager.enums.Category;
import cn.com.protruly.filemanager.enums.FileInfo;
import cn.com.protruly.filemanager.pathFileList.PathListFragment;
import cn.com.protruly.filemanager.operation.FileOperationResult;
import cn.com.protruly.filemanager.operation.FileOperationUtil;
import cn.com.protruly.filemanager.operation.OperationHelper;
import cn.com.protruly.filemanager.operation.ProgressiveFileOperationTaskNotifier;
import cn.com.protruly.filemanager.ui.ActionModeManager;
import cn.com.protruly.filemanager.ui.NameInputDialogManager;
import cn.com.protruly.filemanager.ui.ProgressDialogManager;
import cn.com.protruly.filemanager.utils.FilePathUtil;
import cn.com.protruly.filemanager.utils.GlobalConstants;
import cn.com.protruly.filemanager.utils.Util;

import cn.com.protruly.filemanager.ui.ToolbarManager;
import cn.com.protruly.filemanager.utils.LogUtil;
import cn.com.protruly.filemanager.enums.SortOrder;
import cn.com.protruly.filemanager.ziplist.ZipListFragment;
import hb.app.HbActivity;
import hb.widget.ActionMode;

import android.app.LoaderManager;
import android.widget.AdapterView;
import android.widget.HbSearchView;
import android.widget.HbSearchView.OnQueryTextListener;
import android.widget.TextView;

import cn.com.protruly.filemanager.R;
import hb.widget.toolbar.Toolbar;

/**
 * Created by sqf on 17-4-18.
 */

public class CategoryFragment extends BaseFragment implements AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener, Handler.Callback, LoaderManager.LoaderCallbacks<Cursor>,
        OnQueryTextListener, View.OnClickListener,
        SelectionManager.SelectionListener, CategoryListView.SearchViewActionNotifier {

    private static final String TAG = "CategoryFragment";

    protected View mSearchAndListArea;
    protected View mLoadingView;
    protected View mEmptyView;
    protected View mFakeSearchView;
    protected View mFakeSearchViewContainer;

    protected LoaderManager mLoaderManager;

    //category list view
    private static final int LOADER_ID = 1;
    protected CategoryAdapter mAdapter;
    protected CategoryListView mFileListView;
    protected int mCurrentCategorySortOrder;

    //search result list view;
    private static final int LOADER_SEARCH_ID = 2;
    protected CategorySearchResultAdapter mSearchResultAdapter;
    protected ControlledImageLoadingListView mSearchResultListView;
    protected boolean mIsSearchLoaderInitialized = false;

    protected CategoryContentObserver mContentObserver;
    protected int mCategoryType;
    protected Bundle mArguments;
    protected int mSortOrder;
    protected static final int DEFAULT_SORT_ORDER = SortOrder.SORT_ORDER_TIME;
    protected static final String KEY_SORT_ORDER_FOR_LOADER = "SORT_ORDER";
    protected static final String KEY_SEARCH_KEY_TEXT = "search_key_text";
    protected Bundle mLoaderArgs = new Bundle();

    protected Toolbar mToolbar;
    protected View mToolbarSearchLayout;
    protected HbSearchView mToolbarSearchView;
    //protected View mToolbarSearchView;

    protected boolean mInSearchMode;

    protected String mToolbarTitle;
    protected String mOldToolbarTitle;

    protected SelectionManager mSelectionManager;
    private int DURATION_SEARCH_VIEW = 250;
    private ObjectAnimator mSearchViewAnimator;

    private ProgressDialogManager mProgressDialogManager;

    private static final int PROGRESS_TIP_COUNT = 100;
    private static final int PROGRESS_DETAIL_TIP_COUNT = 1000;
    private OperationHelper operationHelper;
    private boolean isPickMode;
    private boolean isSetRingtone;
    private NameInputDialogManager manager;

    private ProgressiveFileOperationTaskNotifier mProgressiveFileOperationTaskNotifier;

    private ImageLoader mImageLoader;

    private static final int MSG_DELETE = 1300;

    private Handler mDeleteHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == MSG_DELETE) {
                HashSet<FileInfo> fileInfos = (HashSet<FileInfo>)msg.obj;
                delete(fileInfos);
                /*
                mFileOperationTaskManager.executeDeleteTask(mAdapter.getSelectedFiles(),
                        mProgressiveFileOperationTaskNotifier);
                        */
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceSate) {
        super.onCreate(savedInstanceSate);
        operationHelper = new OperationHelper(getActivity(),new Handler());
        isPickMode = getArguments().getBoolean("is_pick_mode",false);
        isSetRingtone = getArguments().getBoolean("set_ringtone",false);
        LogUtil.i(TAG, "onCreate isPickMode 222:" + isPickMode);
    }

    protected void getInitData() { }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        LogUtil.i(TAG, "onAttach");
    }

    @Override
    public void onStart() {
        super.onStart();
        LogUtil.i(TAG, "onStart isPickMode:" + isPickMode);
        if(isPickMode){
            getToolbarManager().switchToStatus(ToolbarManager.STATUS_HOME_PATH_SELECTION);
            setFakeSearchEnabled(false);
            mAdapter.notifyDataSetChanged();
        } else {
            getToolbarManager().switchToStatus(ToolbarManager.STATUS_CATEGORY_LIST);
            setToolbarTitleByCategoryType(mCategoryType);
            setFakeSearchEnabled(true);
            mAdapter.notifyDataSetChanged();
        }
        registerObserver();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterObserver();
    }

    @Override
    public void doSearchViewShowAnimation() {
        if(null != mSearchViewAnimator) {
            mSearchViewAnimator.cancel();
        }
        float currentTranslationY = mFakeSearchViewContainer.getTranslationY();
        float from = currentTranslationY;
        float to = 0.0f;
        if(Math.abs(from - to) < 0.00001f) {
            return;
        }
        mSearchViewAnimator = ObjectAnimator.ofFloat(mFakeSearchViewContainer,
                "translationY",
                from,
                to );
        //LogUtil.i(TAG, "SHOW --> doSearchViewShowAnimation: from--> currentTranslationY:" + from + " to --> " + to);
        mSearchViewAnimator.setDuration(DURATION_SEARCH_VIEW);
        mSearchViewAnimator.start();
    }

    @Override
    public void doSearchViewHideAnimation() {
        if(null != mSearchViewAnimator) {
            mSearchViewAnimator.cancel();
        }
        float currentTranslationY = mFakeSearchViewContainer.getTranslationY();
        float from = currentTranslationY;
        float to = -1 * mFileListView.getHeaderViewHeight();
        if(Math.abs(from - to) < 0.00001f) {
            return;
        }
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(mFakeSearchViewContainer,
                "translationY",
                from,
                to);
        //LogUtil.i(TAG, "HIDE --> doSearchViewHideAnimation: from--> currentTranslationY:" + from + " to --> " + to);
        objectAnimator.setDuration(DURATION_SEARCH_VIEW);
        objectAnimator.start();
    }

    @Override
    public void moveSearchViewTop(int moveTop) {
        mFakeSearchViewContainer.setTranslationY(-1 * moveTop);
    }

    private void checkoutFailureReason(FileInfo fileInfo,String string){
        Util.showToast(mContext,R.string.rename_fail);
    }

    private void doRename(final FileInfo fileInfo, final String str){
        mSelectionManager.leaveSelectionMode();
        Runnable failRun = new Runnable() {
            @Override
            public void run() {
                checkoutFailureReason(fileInfo, str);
            }
        };
        OperationHelper operationHelper = new OperationHelper(mContext, new Handler(this));
        operationHelper.rename(fileInfo, str, failRun);
    }

    private void showZippedFileNameInputDialog(final HashSet<FileInfo> fileInfos) {
        if(null == fileInfos || fileInfos.isEmpty()) return;
        Iterator<FileInfo> it = fileInfos.iterator();
        String zippedFileName = "";
        while(it.hasNext()) {
            FileInfo firstFileInfo = it.next();

            if(fileInfos.size()==1){
                zippedFileName = firstFileInfo.getName();
            }else{
                zippedFileName = Util.getString(mContext,R.string.and_so_on,firstFileInfo.getName());
            }
            break;
        }
        String title = mContext.getString(R.string.zip_file_name_tip);
        manager = new NameInputDialogManager(mContext, OperationType.ZIP, new NameInputDialogManager.NameInputDialogListener() {
            @Override
            public void onNameInputConfirmed(int operationType, FileInfo old, String newName) {
                zip(fileInfos, newName);
            }

            @Override
            public void onNameInputCancelled(int operationType, FileInfo old) {

            }
        });
        manager.createNameInputDialog(title, zippedFileName, null);
    }

    private void zip(HashSet<FileInfo> fileInfos, String newZippedName) {
        /*
        if(GlobalConstants.DEBUG) {
            for (FileInfo file : fileInfos) {
                LogUtil.i(TAG, " file to zip: " + file.filePath);
            }
            LogUtil.i(TAG, "zipped to newName:" + newZippedName + " destinationDir:" + param.destinationDir);
        }
        */
        String destinationDir = FilePathUtil.getDefaultZipDirectory(mContext);
        String zippedFileName = newZippedName + ".zip";
        mFileOperationTaskManager.executeZipTask(fileInfos, destinationDir, zippedFileName, mProgressiveFileOperationTaskNotifier);
    }

    private class CategoryContentObserver extends ContentObserver {

        public CategoryContentObserver(CategoryFragment fragment, Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            mLoaderArgs.putInt(KEY_SORT_ORDER_FOR_LOADER, mSortOrder);
            mLoaderManager.restartLoader(LOADER_ID, mLoaderArgs, CategoryFragment.this);
        }
    }

    private void registerObserver() {
        CategoryFactory factory = CategoryFactory.instance();
        Uri uri = factory.getCategoryUri(mCategoryType);
        mContext.getContentResolver().registerContentObserver(uri, false, mContentObserver);
    }

    private void unregisterObserver() {
        mContext.getContentResolver().unregisterContentObserver(mContentObserver);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CategoryFactory factory = CategoryFactory.instance();
        if(LOADER_ID == id) {
            LogUtil.i(TAG, "onCreateLoader LOADER_ID id:" + LOADER_ID + " === normal loader");
            int sortOrder = args.getInt(KEY_SORT_ORDER_FOR_LOADER);
            mCurrentCategorySortOrder = sortOrder;
            Uri uri = factory.getCategoryUri(mCategoryType);
            String[] projections = factory.getProjections(mCategoryType);
            String whereClause = factory.getWhereClause(mContext, mCategoryType);
            String sortOrderDescription = factory.getSortOrderDescription(mCategoryType, sortOrder);
            return new CategoryCursorLoader(mContext, uri, projections, whereClause, null, sortOrderDescription, sortOrder);
        } else if(LOADER_SEARCH_ID == id) {
            LogUtil.i(TAG, "onCreateLoader LOADER_SEARCH_ID id:" + LOADER_SEARCH_ID + " === search loader ");
            String searchKeyText = args.getString(KEY_SEARCH_KEY_TEXT);
            Uri uri = factory.getCategoryUri(mCategoryType);
            String[] projections = factory.getProjections(mCategoryType);
            String whereClause = factory.getSearchWhereClause(mContext, mCategoryType, searchKeyText);
            String sortOrderDescription = factory.getSortOrderDescription(mCategoryType, SortOrder.SORT_ORDER_NAME);
            return new CursorLoader(mContext, uri, projections, whereClause, null, sortOrderDescription);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        int loaderId = loader.getId();
        if(LOADER_ID == loaderId) {
            mAdapter.setSortOrder(mCurrentCategorySortOrder);
            mAdapter.swapCursor(data);
            if(data.getCount() != 0) {
                showListView();
            } else {
                showEmptyView();
            }
            mAdapter.notifyDataSetChanged();
        } else if(LOADER_SEARCH_ID == loaderId) {
            mSearchResultAdapter.swapCursor(data);
            mSearchResultAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        int loaderId = loader.getId();
        if(LOADER_ID == loaderId && mAdapter != null) {
            mAdapter.swapCursor(null);
            mAdapter.notifyDataSetChanged();
        } else if(LOADER_SEARCH_ID == loaderId && mSearchResultAdapter != null) {
            mSearchResultAdapter.swapCursor(null);
            mSearchResultAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void initObjOnCreate(Bundle bundle) {
        super.initObjOnCreate(bundle);
        mArguments = getArguments();
        if(mArguments!=null){
            mCategoryType = (int)mArguments.get(Category.TAG);
        }
        mImageLoader = ImageLoader.getInstance(mContext);
        setToolbarTitleByCategoryType(mCategoryType);

        initProgressiveFileOperationTaskNotifier(mContext);

        mContentObserver = new CategoryContentObserver(this, new Handler());
        mLoaderManager = getLoaderManager();
        mSortOrder = DEFAULT_SORT_ORDER;
        showLoadingView();
        mLoaderArgs.putInt(KEY_SORT_ORDER_FOR_LOADER, DEFAULT_SORT_ORDER);
        mLoaderManager.initLoader(LOADER_ID, mLoaderArgs, this);

        mSelectionManager = new SelectionManager();
        mSelectionManager.setSelectionListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(!isPickMode){
            setMenuItemChecked(mSortOrder);
        }
        if(manager!=null && manager.isShowingDialog()){
            inputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(Category.TAG, mCategoryType);
    }

    protected void setToolbarTitleByCategoryType(int categoryType) {
        int titleResId = Category.TITLE_ID[categoryType];
        getToolbarManager().setToolbarTitle(titleResId);
    }

    protected void initAdpater() {
        CategoryFactory factory = CategoryFactory.instance();
        mAdapter = factory.get(mFileListView.getContext(), mCategoryType);
        mAdapter.setCategoryType(mCategoryType);
        mAdapter.setSelectionManager(mSelectionManager);
        mAdapter.setImageLoader(mImageLoader);

        mSearchResultAdapter = factory.getSearchResultAdapter(mFileListView.getContext(), mCategoryType);
        mSearchResultAdapter.setCategoryType(mCategoryType);
    }

    @Override
    protected void initRootView(LayoutInflater inflater, ViewGroup container) {
        rootView = inflater.inflate(R.layout.category_layout,container,false);
        super.initRootView(inflater, container);
    }

    @Override
    protected void initViewOnCreateView() {
        initView();
        setListViewListener();
        initAdpater();
        setListViewAdapter();
    }

    protected void initView() {
        mToolbar = ((HbActivity)getActivity()).getToolbar();
        mToolbarSearchLayout = LayoutInflater.from(mContext).inflate(R.layout.category_toolbar_search_layout, null);
        Toolbar.LayoutParams lp = new Toolbar.LayoutParams(Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT);
        Resources res = mContext.getResources();
        lp.topMargin = res.getDimensionPixelSize(R.dimen.toolbar_search_layout_top_margin);
        lp.bottomMargin = res.getDimensionPixelSize(R.dimen.toolbar_search_layout_bottom_margin);
        lp.rightMargin = res.getDimensionPixelSize(R.dimen.toolbar_search_layout_right_margin);
        mToolbarSearchLayout.setLayoutParams(lp);
        mToolbarSearchView = (HbSearchView) mToolbarSearchLayout.findViewById(R.id.search_view);
        mToolbarSearchView.setOnQueryTextListener(this);

        mLoadingView = rootView.findViewById(R.id.loading_view);
        mEmptyView = rootView.findViewById(R.id.empty_view);
        mSearchAndListArea = rootView.findViewById(R.id.file_list_area);
        mFileListView = (CategoryListView)rootView.findViewById(R.id.show_file_list);
        mFileListView.setImageLoader(mImageLoader);
        //add list header view
        mFileListView.addHeaderView();
        mFileListView.setSearchViewActionNotifier(this);

        mSearchResultListView = (ControlledImageLoadingListView)rootView.findViewById(R.id.search_result_list);
        mSearchResultListView.setImageLoader(mImageLoader);
        mFakeSearchViewContainer = rootView.findViewById(R.id.category_search_layout);
        mFakeSearchViewContainer.setBackgroundColor(Color.WHITE);
        mFakeSearchView = mFakeSearchViewContainer.findViewById(R.id.search_view);
        mFakeSearchView.setOnClickListener(this);
    }

    private TextView findTextViewInSearchView(HbSearchView searchView) {
        int id = searchView.getContext().getResources().getIdentifier("android:id/search_src_text",null,null);
        TextView textView = (TextView) searchView.findViewById(id);
        return textView;
    }

    private void setSearchViewStyle(HbSearchView searchView) {
        /*
        int id = searchView.getContext().getResources().getIdentifier("android:id/search_src_text",null,null);
        TextView textView = (TextView) searchView.findViewById(id);
        */
        TextView textView = findTextViewInSearchView(searchView);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);//14sp
        //textView.setTextColor(getActivity().getResources().getColor(R.color.searchview_text_color));
        //textView.setHintTextColor(getActivity().getResources().getColor(R.color.searchview_hint_text_color));

        Resources res = getActivity().getResources();
        //searchView.setQueryHintTextColor(res.getColor(R.color.searchview_hint_text_color));
        ColorStateList csl = (ColorStateList)res.getColorStateList(R.color.searchview_hint_text_color);
        textView.setHintTextColor(csl);

        //searchView.setQueryHintTextColor(csl);
        searchView.setIconifiedByDefault(false);
        searchView.setQueryHint(getResources().getString(R.string.category_search_hint));
    }


    private void setListViewListener() {
        mFileListView.setOnItemClickListener(this);
        if(isPickMode) {
            mFileListView.setLongClickable(false);
        }else{
            mFileListView.setOnItemLongClickListener(this);
        }
        mSearchResultListView.setOnItemClickListener(this);
        mSearchResultListView.setOnItemLongClickListener(this);
    }

    private void setListViewAdapter() {
        mFileListView.setAdapter(mAdapter);
        mSearchResultListView.setAdapter(mSearchResultAdapter);
    }

    private void showSortMenu(boolean show) {
        Menu menu = ((HbActivity)getActivity()).getOptionMenu();
        if(null == menu) return;
        MenuItem item = menu.findItem(R.id.action_sort);
        if(null == item) return;
        item.setVisible(show);
    }

    private void showCancelMenu(boolean show) {
        Menu menu = ((HbActivity)getActivity()).getOptionMenu();
        if(null == menu) return;
        MenuItem item = menu.findItem(R.id.action_cancel);
        if(null == item) return;
        item.setVisible(show);
    }

    @Override
    public boolean onBackPressed() {
        if(mSelectionManager.isInSelectionMode()) {
            mSelectionManager.leaveSelectionMode();
            return true;
        }
        if(mInSearchMode) {
            LogUtil.i(TAG, "onBackPressed in search mode: true");
            changeToolbarTitle(mOldToolbarTitle);
            mToolbar.removeView(mToolbarSearchLayout);
            mSearchResultListView.setVisibility(View.GONE);
            mInSearchMode = false;
            showSortMenu(true);
            return true;//back press comsumed
        }
        getToolbarManager().switchToStatus(ToolbarManager.STATUS_MAIN_PAGE);
        return super.onBackPressed();
    }

    @Override
    public int getFragmentId() {
        return 2;
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

    public void changeToolbarTitle(int resId) {
        if(mContext == null) return;
        mToolbarTitle = mContext.getResources().getString(resId);
        changeToolbarTitle(mToolbarTitle);
    }

    public void changeToolbarTitle(String title) {
        mToolbarTitle = title;
        mToolbar.setTitle(mToolbarTitle);
    }


    public String escapeExprSpecialWord(String keyword) {
        String[] fbsArr = { "\\", "$", "(", ")", "*", "+", ".", "[", "]", "?", "^", "{", "}", "|" };
        for (String key : fbsArr) {
            if (keyword.contains(key)) {
                keyword = keyword.replace(key, "\\" + key);
            }
        }
        return keyword;
    }


    public void search(String key) {
        if(TextUtils.isEmpty(key)) {
            mSearchResultAdapter.swapCursor(null);
            mSearchResultAdapter.notifyDataSetChanged();
            return;
        }
        key = escapeExprSpecialWord(key.trim());
        mSearchResultAdapter.setSearchKeyword(key);
        LogUtil.i(TAG, "search key:" + key);
        mLoaderArgs.putInt(KEY_SORT_ORDER_FOR_LOADER, SortOrder.SORT_ORDER_NAME);
        mLoaderArgs.putString(KEY_SEARCH_KEY_TEXT, key);
        if( ! mIsSearchLoaderInitialized) {
            mLoaderManager.initLoader(LOADER_SEARCH_ID, mLoaderArgs, this);
            mIsSearchLoaderInitialized = true;
        } else {
            mLoaderManager.restartLoader(LOADER_SEARCH_ID, mLoaderArgs, this);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        LogUtil.i(TAG, "onQueryTextSubmit --- s:" + s);
        search(s);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        LogUtil.i(TAG, "onQueryTextChange --- s:" + s);
        search(s);
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        boolean handled = true;
        int id = menuItem.getItemId();
        switch (id) {
            case R.id.category_sort_by_name:
                LogUtil.i(TAG, "category_sort_by_name");
                mSortOrder = SortOrder.SORT_ORDER_NAME;
                break;
            case R.id.category_sort_by_time:
                LogUtil.i(TAG, "category_sort_by_time");
                mSortOrder = SortOrder.SORT_ORDER_TIME;
                break;
            case R.id.category_sort_by_size_asc:
                LogUtil.i(TAG, "category_sort_by_size_asc");
                mSortOrder = SortOrder.SORT_ORDER_SIZE_ASC;
                break;
            case R.id.category_sort_by_size_dsc:
                LogUtil.i(TAG, "category_sort_by_size_dsc");
                mSortOrder = SortOrder.SORT_ORDER_SIZE_DSC;
                break;
            case R.id.action_cancel:
                getActivity().finish();
            default:
                handled = false;
                break;
        }
        if(handled) {
            mLoaderArgs.putInt(KEY_SORT_ORDER_FOR_LOADER, mSortOrder);
            mLoaderManager.restartLoader(LOADER_ID, mLoaderArgs, CategoryFragment.this);
            setMenuItemChecked(mSortOrder);
            return true;
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
    public void onClick(View v) {
        if(v == mFakeSearchView) {
            if(mSelectionManager.isInSelectionMode()) {
                return;
            }
            mOldToolbarTitle = String.valueOf(mToolbar.getTitle());
            changeToolbarTitle("");
            mToolbar.addView(mToolbarSearchLayout);
            mToolbarSearchView.setIconified(false);
            mInSearchMode = true;
            mSearchResultListView.setVisibility(View.VISIBLE);
            showSortMenu(false);
        }

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(!mInSearchMode) {
            Cursor cursor = mAdapter.getCursor();
            if(null == cursor) return;
            if(cursor.getCount() == 0) return;
            boolean moved = cursor.moveToPosition(position - 1);
            if(!moved) return;
            final String filePath = cursor.getString(CategoryFactory.COLUMN_INDEX_DISPLAY_DATA);//file path
            if (isPickMode) {
                FileInfo fileInfo = new FileInfo(filePath);
                Log.d("test","isSetRingtone:"+isSetRingtone);
                if(isSetRingtone){
                    operationHelper.setRingtone(getActivity(), fileInfo);
                }else{
                    operationHelper.pickFile(getActivity(), fileInfo);
                }
                return;
            }
            if (mSelectionManager.isInSelectionMode()) {
                mSelectionManager.toggle(filePath);
                mAdapter.notifyDataSetChanged();
            } else {
                if (mAdapter.getCategoryType() != Category.Zip) {
                    FileOperationUtil.viewFile(mContext, filePath);
                } else {
                    ZipListFragment.openMe(getActivity(), filePath);
                }
            }
        } else {
            Cursor cursor = mSearchResultAdapter.getCursor();
            if(null == cursor) return;
            if(cursor.getCount() == 0) return;
            boolean moved = cursor.moveToPosition(position);
            if(!moved) return;
            final String filePath = cursor.getString(CategoryFactory.COLUMN_INDEX_DISPLAY_DATA);//file path
            if(mCategoryType != Category.Zip) {
                FileOperationUtil.viewFile(mContext, filePath);
            } else {
                mToolbar.removeView(mToolbarSearchLayout);
                ZipListFragment.openMe((Activity) mContext, filePath);
            }
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if(!mInSearchMode) {
            if (mSelectionManager.isInSelectionMode()) return false;
            Cursor cursor = mAdapter.getCursor();
            cursor.moveToPosition(position - 1);
            final String filePath = cursor.getString(CategoryFactory.COLUMN_INDEX_DISPLAY_DATA);//file path
            mSelectionManager.toggle(filePath);
            mAdapter.notifyDataSetChanged();
            return true;
        }
        return false;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            /*
            case OperationType.RENAME:
                break;
            case OperationType.ZIP:
                break;
            */
            case OperationType.DELETE:
                /*
                if(operationHelper!=null&&operationHelper.mProgressDialog!=null) {
                    operationHelper.mProgressDialog.cancel();
                }

                mSelectionManager.leaveSelectionMode();
                Util.showToast(mContext, R.string.delete_success);*/
                break;
            /*
            case OperationType.DELETE_UPDATEPROGRESS:
                if(operationHelper!=null&&operationHelper.mProgressDialog!=null) {
                    operationHelper.mProgressDialog.setProgress((int) msg.obj);
                }
                break;
            case OperationType.DELETE_EXCEPTION:
                if(operationHelper!=null&&operationHelper.mProgressDialog!=null) {
                    operationHelper.mProgressDialog.cancel();
                }
                break;
                */
        }
        return false;
    }

    private void setFakeSearchEnabled(boolean enabled) {
        mFakeSearchView.setEnabled(enabled);
        mFakeSearchView.setClickable(enabled);
    }

    @Override
    public void onEnterSelectionMode() {
        mActionModeManager.startActionMode();
        mActionModeManager.setNegativeText(com.hb.R.string.cancel);
        mActionModeManager.setPositiveText(R.string.action_mode_select_all_text);
        setFakeSearchEnabled(false);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLeaveSelectionMode() {
        mActionModeManager.finishActionMode();
        setFakeSearchEnabled(true);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAllSelected() {
        mActionModeManager.setPositiveText(R.string.action_mode_unselect_all_text);
        updateBottomBarMenu();
        setSelectedCountOnActionModeTitle();
    }

    @Override
    public void onAllUnselected() {
        mActionModeManager.setPositiveText(R.string.action_mode_select_all_text);
        updateBottomBarMenu();
        setSelectedCountOnActionModeTitle();
    }

    @Override
    public void onSelectionChange(String filePath, boolean selected) {
        updateBottomBarMenu();
        setSelectedCountOnActionModeTitle();
    }

    private void updateBottomBarMenu() {
        ActionModeManager actionModeManager = getActionModeManager();
        int selectedCount = mSelectionManager.getSelectedCount();
        boolean noneSelected = selectedCount == 0;
        boolean multipleSelected = selectedCount > 1;
        if(noneSelected) {
            actionModeManager.setAllBottomBarItemEnable(false);
        } else if(multipleSelected) {
            actionModeManager.setAllBottomBarItemEnable(true);
            actionModeManager.setBottomBarItemEnabled(R.id.action_menu_rename, !multipleSelected);
            actionModeManager.setBottomBarItemEnabled(R.id.action_menu_open_with, !multipleSelected);
        } else {
            actionModeManager.setAllBottomBarItemEnable(true);
        }
    }

    private void setSelectedCountOnActionModeTitle() {
        int selectedCount = mSelectionManager.getSelectedCount();
        String titleFormat;
        if(Locale.getDefault().toString().equals("en_GB")||Locale.getDefault().toString().equals("en_US")&&selectedCount==0){
            titleFormat = mContext.getResources().getString(R.string.zero_item,selectedCount);
        }else{
            titleFormat = mContext.getResources().getQuantityString(R.plurals.selected_item_count_format, selectedCount);
        }
        String title = String.format(titleFormat, selectedCount);
        getActionModeManager().setActionModeTitle(title);
    }

    //callback for BottomNavigationBar menu items;
    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        LogUtil.i(TAG, "BottomNavigationBar item clicked");
        switch(menuItem.getItemId()) {
            case R.id.action_menu_send:
                if(mSelectionManager.getSelectedCount() > PROGRESS_TIP_COUNT) {
                    GetDataTask task = new GetDataTask(OperationType.SEND);
                    task.execute();
                } else {
                    HashSet<FileInfo> fileInfos = mAdapter.getSelectedFiles();
                    LogUtil.i(TAG, "fssssssssssssssssss:" + fileInfos.size());
                    if(fileInfos != null && fileInfos.size() > GlobalConstants.SEND_LIMIT) {
                        Util.showToast(mContext,getString(R.string.send_not_alown_upper_to_ten));
                        return true;
                    }
                    FileOperationUtil.sendOneOrMultiple(getActivity(), fileInfos);
                    mSelectionManager.leaveSelectionMode();
                }
                return true;
            case R.id.action_menu_cut:
                if(mSelectionManager.getSelectedCount() > PROGRESS_TIP_COUNT) {
                    GetDataTask task = new GetDataTask(OperationType.CUT);
                    task.execute();
                } else {
                    startPathListFrament(OperationType.CUT, mAdapter.getSelectedFiles());
                    getActionModeManager().finishActionMode();
                    mSelectionManager.leaveSelectionMode();
                }
                return true;
            case R.id.action_menu_delete:
                deleteTip();
                return true;
            case R.id.action_menu_copy:
                if(mSelectionManager.getSelectedCount() > PROGRESS_TIP_COUNT) {
                    GetDataTask task = new GetDataTask(OperationType.COPY);
                    task.execute();
                } else {
                    copy(mAdapter.getSelectedFiles());
                }
                return true;
            case R.id.action_menu_rename: {
                ArrayList selectedData = mAdapter.getSelectedData();
                if (selectedData.size() < 1) {
                    return false;
                }
                FileInfo fileInfo = (FileInfo) selectedData.get(0);
                //createNameInputDialog(OperationType.RENAME, "重命名", fileInfo.getName(), fileInfo);
                String title = mContext.getString(R.string.rename);
                manager = new NameInputDialogManager(mContext, OperationType.RENAME, new NameInputDialogManager.NameInputDialogListener() {
                    @Override
                    public void onNameInputConfirmed(int operationType, FileInfo old, String newName) {
                        doRename(old, newName);
                    }
                    @Override
                    public void onNameInputCancelled(int operationType, FileInfo old) {

                    }
                });
                manager.createNameInputDialog(title, fileInfo.getName(), fileInfo);
                return true;
            }
            case R.id.action_menu_compress:
                /*
                String title = mContext.getString(R.string.zip_file_name_tip);
                NameInputDialogManager manager = new NameInputDialogManager(mContext, OperationType.ZIP, this);
                manager.createNameInputDialog(title, "", null);
                */
                if(mSelectionManager.getSelectedCount() > PROGRESS_TIP_COUNT) {
                    GetDataTask task = new GetDataTask(OperationType.ZIP);
                    task.execute();
                } else {
                    //ArrayList<FileInfo> fileInfos = mAdapter.getSelectedData();
                    HashSet<FileInfo> fileInfos = mAdapter.getSelectedFiles();
                    //zip(fileInfos, newName);
                    showZippedFileNameInputDialog(fileInfos);
                }
                return true;
            case R.id.action_menu_open_with: {
                ArrayList selectedData = mAdapter.getSelectedData();
                if (selectedData.size() < 1) {
                    return false;
                }
                FileInfo fileInfo = (FileInfo) selectedData.get(0);
                //FileOperationUtil.openWith(mContext, fileInfo.filePath);
                operationHelper.openWithMethod(fileInfo);
                mSelectionManager.leaveSelectionMode();
                return true;
            }
            case R.id.action_menu_detail:
                if(mSelectionManager.getSelectedCount() > PROGRESS_TIP_COUNT) {
                   GetDataTask task = new GetDataTask(OperationType.DETAIL);
                   task.execute();
                } else {
                    HashSet<FileInfo> fileInfos =  mAdapter.getSelectedFiles();
                    if(fileInfos == null || fileInfos.size() == 0) return false;
                    FileDetailDialog detailDialog = new FileDetailDialog(mContext,fileInfos);
                    detailDialog.show();
                }
                return true;
        }
        return false;
    }

    private <T> void showConfirmStartWithDotDialog(int operationType,T t,String str){
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(mContext.getResources().getString(R.string.confirm_hiden_file_create));
        builder.setPositiveButton(mContext.getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //doAfterNewNameConfirmed();
            }
        });
        builder.setNegativeButton(mContext.getResources().getString(android.R.string.cancel),null);
        builder.show();
    }

    private void startPathListFrament(int operationType, HashSet<FileInfo> selected) {
        PathListFragment fragment = new PathListFragment();
        Bundle bundle = new Bundle();

        //bundle.putParcelableArrayList("operation_data", selected);
        bundle.putInt(GlobalConstants.OPERATION_TYPE, operationType);
        bundle.putInt(GlobalConstants.MODE_PATH,GlobalConstants.MODE_PATH_SELECT);
        bundle.putSerializable(GlobalConstants.OPERATION_DATA, selected);
        fragment.setArguments(bundle);
        FragmentTransaction transaction = getActivity().getFragmentManager().beginTransaction();
        String tag = fragment.getClass().getSimpleName();
        transaction.replace(R.id.fragment_container,fragment,fragment.getClass().getSimpleName());
        transaction.addToBackStack(tag);
        transaction.commitAllowingStateLoss();
    }


    private void paste(String targetPath) {
        Cursor cursor = mAdapter.getCursor();
        /*
        CategoryFileMoveOperationTask task = new CategoryFileMoveOperationTask(getActivity().getBaseContext(), this, cursor, mSelectionManager, targetPath);
        task.execute();
        */
    }

    @Override
    public void onActionItemClicked(ActionMode.Item item) {
        int id = item.getItemId();
        switch (id) {
            case ActionMode.POSITIVE_BUTTON:
                if(mSelectionManager.isAllSelected()) {
                    mSelectionManager.unselectAll();
                } else if(mSelectionManager.isAllUnselected()){
                    mSelectionManager.selectAll();
                } else {
                    mSelectionManager.selectAll();
                }
                mAdapter.notifyDataSetChanged();
                break;
            case ActionMode.NAGATIVE_BUTTON:
                mSelectionManager.leaveSelectionMode();
                break;
        }
    }

    @Override
    public void onActionModeShow(ActionMode actionMode) {

    }

    @Override
    public void onActionModeDismiss(ActionMode actionMode) {

    }

    @Override
    public void onToolbarNavigationIconClicked() {
        getActivity().onBackPressed();
    }

    public void deleteTip(){
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
        if(mSelectionManager.getSelectedCount() > PROGRESS_TIP_COUNT) {
            LogUtil.i("Delete", "> PROGRESS_TIP_COUNT....Will Call GetDataTask delete");
            GetDataTask task = new GetDataTask(OperationType.DELETE);
            task.execute();
        } else {
            LogUtil.i("Delete", "< PROGRESS_TIP_COUNT....No-GetDataTask delete");
            delete(mAdapter.getSelectedFiles());
        }
    }

    private void initProgressiveFileOperationTaskNotifier(Context context) {
        mProgressiveFileOperationTaskNotifier = new ProgressiveFileOperationTaskNotifier(context) {

            @Override
            public void onOperationSucceeded(int operationType, FileOperationResult result) {
                LogUtil.i(TAG, "=====onOperationSucceeded=====");
                super.onOperationSucceeded(operationType, result);
                mSelectionManager.leaveSelectionMode();
                switch (operationType) {
                    case OperationType.DELETE:
                        Util.showToast(mContext, R.string.delete_success);
                        break;
                    case OperationType.ZIP:
                        String zipSuccessTip = mContext.getString(R.string.zip_success);
                        if(result != null && result.isSucceeded() && ! TextUtils.isEmpty(result.realDestinationPath)) {
                            Util.showToast(mContext, zipSuccessTip + " " + result.realDestinationPath);
                        }
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onOperationFailed(int operationType, FileOperationResult result) {
                super.onOperationFailed(operationType, result);
                //LogUtil.i(TAG, "=====onOperationFailed=====");
            }
        };
    }

    private void delete(HashSet<FileInfo> fileInfos) {
        LogUtil.i(TAG, "Delete dddddddddddd");
        mFileOperationTaskManager.executeDeleteTask(fileInfos, mProgressiveFileOperationTaskNotifier);
    }

    private void copy(HashSet<FileInfo> fileInfos) {
        startPathListFrament(OperationType.COPY, fileInfos);
        getActionModeManager().finishActionMode();
        mSelectionManager.leaveSelectionMode();
    }

    public class GetDataTask extends AsyncTask<Void, Integer, HashSet<FileInfo>> implements DialogInterface.OnCancelListener {

        private static final String TAG = "GetDataTask";

        private ProgressDialogManager mProgressDialogManager;
        private int mOperationType;

        public GetDataTask(int operationType) {
            mProgressDialogManager = new ProgressDialogManager(mContext);
            mProgressDialogManager.setCancelListener(this);
            mOperationType = operationType;
        }

        @Override
        protected void onPreExecute() {
            /*
            if(mOperationType==OperationType.DETAIL && mSelectionManager.getSelectedCount() < PROGRESS_DETAIL_TIP_COUNT ) {
                return;
            }
            */
            mProgressDialogManager.showProgressDialog(R.string.preparing);
        }

        @Override
        protected HashSet<FileInfo> doInBackground(Void... params) {
            final Cursor ref = mAdapter.getCursor();
            HashSet<FileInfo> fileInfos = new HashSet<FileInfo>();
            try {
                int addedCount = 0;
                LogUtil.i(TAG, "GetDataTask doInBackground");
                int selectedCount = mSelectionManager.getSelectedCount();

                for (int i = 0; i < ref.getCount(); i++) {
                    if (isCancelled()) {
                        return null;
                    }
                    ref.moveToPosition(i);
                    final String filePath = ref.getString(CategoryFactory.COLUMN_INDEX_DISPLAY_DATA);
                    LogUtil.i(TAG, "GetDataTask moveToPosition:" + i + " filePath:" + filePath);
                    if (mSelectionManager.isSelected(filePath)) {
                        FileInfo fileInfo = new FileInfo(filePath);
                        fileInfos.add(fileInfo);
                        ++addedCount;
                        int progress = Util.getProgress(selectedCount, addedCount);
                        LogUtil.i(TAG, "GetDataTask progress:" + progress);
                        publishProgress(progress);
                        if (addedCount == selectedCount) break;
                    }
                }
                return fileInfos;
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                return fileInfos;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if(mProgressDialogManager != null) {
                mProgressDialogManager.setProgress(values[0]);
            }
        }

        @Override
        protected void onPostExecute(HashSet<FileInfo> fileInfos) {
            super.onPostExecute(fileInfos);
            mProgressDialogManager.dismissProgressDialog();
            Log.i("Delete", "onPostExecute mProgressDialogManager.dismissProgressDialog()");
            if(isCancelled()) return;
            switch(mOperationType) {
                case OperationType.ZIP:
                    showZippedFileNameInputDialog(fileInfos);
                    break;
                case OperationType.DELETE:
                    mDeleteHandler.obtainMessage(MSG_DELETE, fileInfos).sendToTarget();
                    break;
                case OperationType.SEND:
                    //FileOperationUtil.sendOneOrMultiple(getActivity(), mAdapter.getSelectedData());
                    if(fileInfos != null && fileInfos.size() > GlobalConstants.SEND_LIMIT) {
                        Util.showToast(mContext,getString(R.string.send_not_alown_upper_to_ten));
                        return;
                    }
                    FileOperationUtil.sendOneOrMultiple(getActivity(), fileInfos);
                    mSelectionManager.leaveSelectionMode();
                    break;
                case OperationType.CUT:
                    startPathListFrament(OperationType.CUT, fileInfos);
                    getActionModeManager().finishActionMode();
                    mSelectionManager.leaveSelectionMode();
                    break;
                case OperationType.COPY:
                    copy(fileInfos);
                    break;
                case OperationType.DETAIL:
                    FileDetailDialog detailDialog = new FileDetailDialog(mContext,fileInfos);
                    detailDialog.show();
                default:
                    break;
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Util.showToast(mContext, android.R.string.cancel);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            cancel(true);
        }
    }
}

package cn.com.protruly.filemanager.ziplist;

import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashSet;

import java.util.zip.ZipInputStream;
//import java.util.zip.ZipEntry;
import org.apache.tools.zip.ZipEntry;
//import java.util.zip.ZipFile;
import org.apache.tools.zip.ZipFile;

import cn.com.protruly.filemanager.CategoryActivity;
import cn.com.protruly.filemanager.R;
import cn.com.protruly.filemanager.enums.FileInfo;
import cn.com.protruly.filemanager.BaseFragment;
import cn.com.protruly.filemanager.pathFileList.PathListFragment;
import cn.com.protruly.filemanager.operation.FileOperationResult;
import cn.com.protruly.filemanager.operation.FileOperationUtil;
import cn.com.protruly.filemanager.operation.OperationType;
import cn.com.protruly.filemanager.operation.ProgressiveFileOperationTaskNotifier;
import cn.com.protruly.filemanager.ui.ProgressDialogManager;
import cn.com.protruly.filemanager.ui.ToolbarManager;
import cn.com.protruly.filemanager.utils.FilePathUtil;
import cn.com.protruly.filemanager.utils.LogUtil;
import cn.com.protruly.filemanager.utils.Util;
import hb.app.HbActivity;
import hb.app.dialog.AlertDialog;
import hb.widget.HbListView;
import hb.widget.toolbar.Toolbar;

/**
 * Created by sqf on 17-5-17.
 */

public class ZipListFragment extends BaseFragment implements AdapterView.OnItemClickListener, View.OnClickListener, DialogInterface.OnClickListener,
        DialogInterface.OnCancelListener {

    private static final String TAG = "ZipListFragment";
    protected Toolbar mToolbar;
    private View mLoadingView;
    private View mEmptyView;
    private HbListView mListView;
    private ZipListAdapter mAdapter;
    private String mZipFilePath;
    private Bundle mArguments;
    private Button mUncompressButton;
    private Dialog mPathSelectDialog;
    private ProgressDialogManager mProgressDialogManager;
    public static final String ARGS_ZIP_FILE_PATH = "ZIP_FILE_PATH";

    private ZipEntryListAsyncTask mZipEntryListAsyncTask;

    @Override
    public void onCreate(Bundle savedInstanceSate) {
        super.onCreate(savedInstanceSate);
        getToolbarManager().switchToStatus(ToolbarManager.STATUS_ZIP_LIST);
    }

    @Override
    public int getFragmentId() {
        return 3;
    }

    @Override
    protected void getInitData() {
        showListView();
        if(mAdapter.isEmpty()) {
            showEmptyView();
        }
        listZipFiles();
    }

    @Override
    protected void initRootView(LayoutInflater inflater, ViewGroup container) {
        rootView = inflater.inflate(R.layout.zip_layout,container,false);
        super.initRootView(inflater, container);
    }

    @Override
    protected void initViewOnCreateView() {
        initView();
        initAdpater();
        setListViewAdapter();
    }

    protected void initView() {
        mToolbar = ((HbActivity)getActivity()).getToolbar();

        mLoadingView = rootView.findViewById(R.id.loading_view);
        mEmptyView = rootView.findViewById(R.id.empty_view);
        //mListArea = rootView.findViewById(R.id.file_list_area);

        mListView = (HbListView) rootView.findViewById(R.id.show_file_list);
        mListView.setOnItemClickListener(this);

        mUncompressButton = (Button)rootView.findViewById(R.id.uncompress_btn);
        mUncompressButton.setOnClickListener(this);
    }

    @Override
    protected void initObjOnCreate(Bundle bundle) {
        super.initObjOnCreate(bundle);
        mArguments = getArguments();
        mZipFilePath = (String)mArguments.get(ARGS_ZIP_FILE_PATH);
        mZipFilePath = Uri.decode(mZipFilePath);
        if(mZipFilePath.startsWith("file://")) {
            mZipFilePath = mZipFilePath.replaceFirst("file://", "");
        }
        setNeedNavigationIconImmediateBack(true);
        setToolbarTitleByFilePath(mZipFilePath);
    }

    protected void initAdpater() {
        mAdapter = new ZipListAdapter(mListView.getContext());
    }

    private void setListViewAdapter() {
        mListView.setAdapter(mAdapter);
    }

    private void setToolbarTitleByZipPath(ZipPath zipPath) {
        getToolbarManager().setToolbarTitle(zipPath.getName());
    }

    private void setToolbarTitleByFilePath(String zipFilePath) {
        /*
        Activity activity = getActivity();
        if(activity == null) return;
        CategoryActivity categoryActivity = (CategoryActivity)activity;
        if( !(categoryActivity.getCurrentFragment() instanceof ZipListFragment)) return;
        */
        String title = FilePathUtil.getFileNameAndExtension(zipFilePath);
        getToolbarManager().setToolbarTitle(title);
    }

    protected void showLoadingView() {
        Util.showView(mLoadingView);
        Util.hideView(mListView);
        Util.hideView(mEmptyView);
    }

    protected void showEmptyView() {
        Util.hideView(mLoadingView);
        Util.hideView(mListView);
        Util.showView(mEmptyView);
    }

    protected void showListView() {
        Util.hideView(mLoadingView);
        Util.showView(mListView);
        Util.hideView(mEmptyView);
    }

    private void showZipPathInList(ZipPath zipPath) {
        mAdapter.setCurrentZipPath(zipPath);
        mAdapter.notifyDataSetChanged();
        if(zipPath != mAdapter.getRootPath()) {
            setToolbarTitleByZipPath(zipPath);
        } else {
            setToolbarTitleByFilePath(mZipFilePath);
        }
    }

    private void listZipFiles() {
        if(mZipEntryListAsyncTask!=null && mZipEntryListAsyncTask.getStatus() != AsyncTask.Status.FINISHED) {
            mZipEntryListAsyncTask.cancel(true);
            mZipEntryListAsyncTask = null;
        }
        showLoadingView();
        mZipEntryListAsyncTask = new ZipEntryListAsyncTask();
        mZipEntryListAsyncTask.execute(mZipFilePath);
    }

    @Override
    public boolean onBackPressed() {
        //if(isNeedNavigationIconImmediateBack()) return false;
        LogUtil.i(TAG, "onBackPressed");
        ZipPath current = mAdapter.getCurrentPath();
        if(null == current || current == mAdapter.getRootPath()) {
            LogUtil.i(TAG, "onBackPressed 111 return directly");
            return false;
        }
        ZipPath parent = current.getParent();
        if(parent != null) {
            showZipPathInList(parent);
            LogUtil.i(TAG, "onBackPressed 222 show zip path list ---> parent" + parent.getName());
            return true;
        }
        LogUtil.i(TAG, "onBackPressed 333 ");
        return false;
    }

    @Override
    public void onToolbarNavigationIconClicked() {
        if(!mActivity.isFinishing()) {
            ((CategoryActivity)mActivity).backImmediately();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ZipPath zipPath = (ZipPath) mAdapter.getItem(position);
        if(zipPath.isDirectory()) {
            showZipPathInList(zipPath);
        } else {
            //TODO: open directly
            String zipEntry = zipPath.getPath();
            //String tmpDir = FilePathUtil.getDiskCacheDir(getActivity());
            String tmpDir = FilePathUtil.getDefaultZipDirectory(getActivity());
            LogUtil.i(TAG, "onItemClick open zipEntry directly:" + zipEntry + " tmpDir:" + tmpDir);
            unzipZipEntry(mZipFilePath, zipEntry, tmpDir);
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        mProgressDialogManager.dismissProgressDialog();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(R.id.uncompress_btn == id) {
            showUncompressPathSelectDialog();
        }
    }

    private void showUncompressPathSelectDialog() {
        if(mPathSelectDialog == null) {
            mPathSelectDialog = new AlertDialog.Builder(getActivity()).
                    setNegativeButton(R.string.uncompress_to_user_designated_path, this).
                    setPositiveButton(R.string.uncompress_to_file_path, this).create();
        }
        mPathSelectDialog.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        String destinationDir = "";
        switch(which) {
            case DialogInterface.BUTTON_POSITIVE:
                //uncompress to current directory
                LogUtil.i(TAG, "uncompress to current directory");
                destinationDir = FilePathUtil.getFileDirectory(mZipFilePath);
                unzipWhole(mZipFilePath, destinationDir);
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                //uncompress to user designated directory
                LogUtil.i(TAG, "uncompress to user designated directory");
                //Set Destination here --> destinationDir
                HashSet<FileInfo> fileInfos = new HashSet<FileInfo>();
                FileInfo fileInfo = new FileInfo(mZipFilePath);
                fileInfos.add(fileInfo);
                PathListFragment.openMe(getActivity(), OperationType.UNZIP, fileInfos);
                break;
        }
    }

    private void unzipZipEntry(String zipFilePath, String zipEntry, String destinationDir) {
        mFileOperationTaskManager.executeUnzipEntryTask(zipFilePath, zipEntry, destinationDir, new ProgressiveFileOperationTaskNotifier(mContext) {
            @Override
            public void onOperationSucceeded(int operationType, FileOperationResult result) {
                super.onOperationSucceeded(operationType, result);
                if(result == null) return;
                LogUtil.i(TAG, "onUnzipEntrySucceeded ------ 2");
                String unzippedFilePath = result.realDestinationPath;
                boolean isUnzippedFileStillZip = Util.isZipFile(unzippedFilePath);
                if(isUnzippedFileStillZip) {
                    ZipListFragment.openMe(getActivity(), unzippedFilePath);
                }else if(!TextUtils.isEmpty(unzippedFilePath)) {
                    String description = mContext.getString(FileOperationResult.getDescription(result.resultCode));
                    Util.showToast(mContext, description + " " + unzippedFilePath);
                    FileOperationUtil.viewFile(mContext, unzippedFilePath);
                    LogUtil.i(TAG, " unzippedFilePath: " + unzippedFilePath);
                }
            }

            @Override
            public void onOperationFailed(int operationType, FileOperationResult result) {
                super.onOperationFailed(operationType, result);
                if(result == null) return;
                LogUtil.i(TAG, "onUnzipEntryFailed ------ 2");
                //Util.showToast(mContext, FileOperationResult.getDescription(result.resultCode));
            }

            @Override
            public void onOperationCancelled(int operationType, FileOperationResult result) {
                super.onOperationCancelled(operationType, result);
                LogUtil.i(TAG, "onUnzipEntryCancelled");
                Util.showToast(mContext, android.R.string.cancel);
            }
        });
    }

    private void unzipWhole(String zipFilePath, String destinationDir) {
        LogUtil.i(TAG, "unzipWhole zipFilePath:" + zipFilePath + " destinationDir:" + destinationDir);
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
                }
            }
            @Override
            public void onOperationFailed(int operationType, FileOperationResult result) {
                super.onOperationFailed(operationType, result);
                if(result == null) return;
                LogUtil.i(TAG, "onUnzipWholeFailed ------ 2");
                //Util.showToast(mContext, FileOperationResult.getDescription(result.resultCode));
            }

            @Override
            public void onOperationCancelled(int operationType, FileOperationResult result) {
                super.onOperationCancelled(operationType, result);
                LogUtil.i(TAG, "onUnzipWholeCancelled");
                LogUtil.i2(TAG, "onUnzipWholeCancelled");
                Util.showToast(mContext, android.R.string.cancel);
            }
        });
    }

    private void ensureProgressDialogManager() {
        if(null != mProgressDialogManager) return;
        mProgressDialogManager = new ProgressDialogManager(getActivity());
    }

    public static void openMe(Activity activity, String zipFilePath) {
        Bundle bundle = new Bundle();
        ZipListFragment fragment = new ZipListFragment();
        bundle.putString(ZipListFragment.ARGS_ZIP_FILE_PATH, zipFilePath);
        fragment.setArguments(bundle);
        FragmentTransaction transaction = activity.getFragmentManager().beginTransaction();
        //transaction.replace(android.R.id.content,fragment,fragment.getClass().getSimpleName());
        String tag = fragment.getClass().getSimpleName();
        transaction.replace(R.id.fragment_container,fragment,fragment.getClass().getSimpleName());
        transaction.addToBackStack(tag);
        transaction.commitAllowingStateLoss();
    }

    public static void openMeForNoBack(Activity activity, String zipFilePath) {
        LogUtil.i(TAG, "openMeForNoBack --> zipFilePath:" + zipFilePath);
        Bundle bundle = new Bundle();
        ZipListFragment fragment = new ZipListFragment();
        bundle.putString(ZipListFragment.ARGS_ZIP_FILE_PATH, zipFilePath);
        fragment.setArguments(bundle);
        FragmentManager fragmentManager = activity.getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        //transaction.replace(android.R.id.content,fragment,fragment.getClass().getSimpleName());
        String tag = fragment.getClass().getSimpleName();
        transaction.replace(R.id.fragment_container,fragment,fragment.getClass().getSimpleName());
       // transaction.addToBackStack(tag);
        transaction.commitAllowingStateLoss();
    }

    private class ZipEntryListAsyncTask extends AsyncTask<String, Float, ZipPath> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showLoadingView();
        }

        @Override
        protected ZipPath doInBackground(String... params) {
            String zipFilePath = params[0];
            if(TextUtils.isEmpty(zipFilePath)) return null;
            ZipPath root = listZipFiles();
            return root;
        }

        private ZipPath listZipFiles() {

            /*
            //------------------- OS api below, there's encoding problem with this
            //LogUtil.i(TAG, "ZipEntryListAsyncTask listZipFiles");
            ZipPath root = mAdapter.getRootPath();
            ZipFile zipFile = null;
            try {
                zipFile = new ZipFile(mZipFilePath);
                Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zipFile.entries();
                while (entries.hasMoreElements() && !isCancelled()) {
                    ZipEntry entry = entries.nextElement();
                    //LogUtil.i(TAG, "entry: " + entry.getName());
                    ZipPath.parseZipEntry(root, entry);
                }
            } catch (Exception e) {
                LogUtil.e(TAG, "listZipFiles Exception: " + e.getMessage());
                return null;
            } finally {
                FilePathUtil.closeSilently(zipFile);
            }
            return root;
            */
            //------------------- Apache zip api below
            //LogUtil.i(TAG, "ZipEntryListAsyncTask listZipFiles");
            ZipPath root = mAdapter.getRootPath();
            ZipFile zipFile = null;
            try {
                zipFile = new ZipFile(mZipFilePath);
                Enumeration<ZipEntry> entries = zipFile.getEntries();
                while (entries.hasMoreElements() && !isCancelled()) {
                    ZipEntry entry = entries.nextElement();
                    ZipPath.parseZipEntry(root, entry);
                }
            } catch (Exception e) {
                LogUtil.e(TAG, "listZipFiles Exception: " + e.getMessage());
                return null;
            } finally {
                //FilePathUtil.closeSilently(zipFile);
                ZipFile.closeQuietly(zipFile);
            }

            return root;
        }

        @Override
        protected void onCancelled() {
            showEmptyView();
            mUncompressButton.setEnabled(false);
        }

        @Override
        protected void onPostExecute(ZipPath root) {
            if(root == null) {
                Util.showToast(mContext, R.string.open_zip_failed);
                showEmptyView();
                mUncompressButton.setEnabled(false);
                return;
            }
            showZipPathInList(root);
            if(!mAdapter.isEmpty()) {
                showListView();
            }
            if(ZipPath.DEBUG) {
                LogUtil.i(TAG, "================================ ZIP PATH DUMP BEGIN ========================");
                String log = root.dump();
                LogUtil.i(TAG, log);
                LogUtil.i(TAG, "================================ ZIP PATH DUMP END ========================");
            }
        }
    }

    @Override
    public void onDestroy() {
        if(mZipEntryListAsyncTask != null) {
            LogUtil.i(TAG, "ZipListFragment::onDestroy cancel mZipEntryListAsyncTask");
            mZipEntryListAsyncTask.cancel(true);
            mZipEntryListAsyncTask = null;
        }
        super.onDestroy();
    }
}

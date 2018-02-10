package cn.com.protruly.filemanager;

import android.Manifest;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.com.protruly.filemanager.R;
import cn.com.protruly.filemanager.categorylist.CategoryFragment;
import cn.com.protruly.filemanager.enums.Category;
import cn.com.protruly.filemanager.HomePage.HomeFragment;
import cn.com.protruly.filemanager.pathFileList.PathListFragment;
import cn.com.protruly.filemanager.operation.FileOperationTaskManager;
import cn.com.protruly.filemanager.operation.FileOperationUtil;
import cn.com.protruly.filemanager.ui.HbToolBarListener;
import cn.com.protruly.filemanager.utils.CustomUsbManager;
import cn.com.protruly.filemanager.utils.DbUtil;
import cn.com.protruly.filemanager.utils.FilePathUtil;
import cn.com.protruly.filemanager.utils.GlobalConstants;
import cn.com.protruly.filemanager.utils.LogUtil;
import cn.com.protruly.filemanager.utils.Util;
import hb.app.HbActivity;

public class CategoryActivity extends HbActivity implements HbToolBarListener, OnBackPressHandlerInterface {

    private static final String TAG = "CategoryActivity";
    private static final boolean StrictMode_switch = false;
    private BaseFragment mCurrentFragment;
    private StorageVolumeManager mStorageVolumeManager;

    private FileOperationTaskManager mFileOperationTaskManager;
    public boolean hidenFile = true;

    //private CustomUsbManager mCustomUsbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mStorageVolumeManager =  StorageVolumeManager.getInstance();
        if(mStorageVolumeManager != null){
            mStorageVolumeManager.init(this);
        }
        //added permission judge
        /*if (Build.VERSION.SDK_INT>23 && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1);
        }*/
        mFileOperationTaskManager = new FileOperationTaskManager(this);

        /*
        mCustomUsbManager = new CustomUsbManager(this);
        mCustomUsbManager.registerBroadcastReceiver();
        */

        setHbContentView(R.layout.main_activity);
        Intent intent = getIntent();
        LogUtil.i(TAG, "onCreate ");
        if(intent!=null && intent.getAction()!=null && intent.getAction().equals(Intent.ACTION_VIEW)) {
            LogUtil.i(TAG, "onCreate ACTION_VIEW");
            processExtraData(intent);
            return;
        }
        startWitchFragment();
        if (StrictMode_switch) {
            openStrictMode();
        }
        LogUtil.i(TAG,  "SDK_INT:" + Build.VERSION.SDK_INT + " " );
    }

    //added permission callback
    /*@Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case GlobalConstants.STORAGE_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {

                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }*/

    private void openStrictMode(){
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectCustomSlowCalls()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                //.detectAll()
                .penaltyLog()
                .penaltyFlashScreen()
                //.penaltyDeath()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects() //API等级11
                .penaltyLog()
                //.penaltyDeath()
                .build());
    }

    public void setHidenStatus(boolean hiden){
        hidenFile = hiden;
    }

    public boolean getHidenStatus(){
        return hidenFile;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if(intent != null) {
            LogUtil.i(TAG, "onNewIntent:" + intent.getAction());
        }
        if(intent!=null && null != intent.getAction() && intent.getAction().equals(Intent.ACTION_VIEW)) {
            LogUtil.i(TAG, "onNewIntent processExtraData ACTION_VIEW");
            processExtraData(intent);
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        //mCustomUsbManager.unregisterBroadcastReceiver();
        super.onDestroy();
        if(mStorageVolumeManager != null) {
            mStorageVolumeManager.onDestroy();
        }
    }

    private void startWitchFragment(){
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        android.util.Log.d(TAG, "startWitchFragment action:" + action);
        android.util.Log.d(TAG, "startWitchFragment type:" + type);
        if(GlobalConstants.PROTRULY_ACTION_GET_CONTENT.equals(action)||Intent.ACTION_GET_CONTENT.equals(action)){
            startTypeFragment(type,intent);
            return ;
        }
        Log.d("test","action:"+action);
        if("android.intent.action.RINGTONE_PICKER".equals(action)){
            type = "audio/*";
            startTypeFragment(type,intent);
            return ;
        }
        if(Intent.ACTION_PICK.equals(action)){
            startTypeFragment(type,intent);
            return ;
        }
        /*if(Intent.ACTION_OPEN_DOCUMENT.equals(action)){
            startPathFragment();
            return;
        }*/
        startHomeFragment();
    }

    private void startTypeFragment(String type,Intent intent){
        BaseFragment fragment;
        Bundle bundle = new Bundle();
        bundle.putBoolean("is_pick_mode",true);
        int categoryType = -1;
        if(null != type && type.startsWith("image/")){
            categoryType = Category.Picture;
            android.util.Log.d(TAG,"11111111111");
            fragment = new CategoryFragment();
        } else if(null != type && type.startsWith("audio/")){
            categoryType = Category.Music;
            //TYPE_RINGTONE = 1;  TYPE_NOTIFICATION = 2; TYPE_ALARM = 4;
            Log.d("test","getIntExtra:"+intent.getIntExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,-1));
            if(intent.getIntExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,-1) > 0){
                bundle.putBoolean("set_ringtone",true);
            }
            fragment = new CategoryFragment();
        } else if(null != type && type.startsWith("video/")){
            categoryType = Category.Video;
            fragment = new CategoryFragment();
        } else{
            fragment = new HomeFragment();
        }
        bundle.putInt(Category.TAG, categoryType);
        fragment.setArguments(bundle);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container,fragment,fragment.getClass().getSimpleName());
        transaction.commitAllowingStateLoss();
        Log.i("CategoryActivity", "test , startTypeFragment");
    }

    private void startPathFragment(){
        PathListFragment fragment = new PathListFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(GlobalConstants.MODE_PATH,GlobalConstants.MODE_PATH_SELECT);
        bundle.putBoolean("is_pick_mode",true);
        fragment.setArguments(bundle);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        String tag = fragment.getClass().getSimpleName();
        transaction.replace(R.id.fragment_container,fragment,fragment.getClass().getSimpleName());
        transaction.addToBackStack(tag);
        transaction.commitAllowingStateLoss();
    }

    private void startHomeFragment(){
        HomeFragment fragment= new HomeFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container,fragment,fragment.getClass().getSimpleName());
        transaction.commitAllowingStateLoss();
        Log.i("CategoryActivity", "test , startHomeFragment");
    }


    @Override
    public void onHbToolbarNavigationClicked(View view) {

    }

    @Override
    public boolean onHbToolbarMenuItemClicked(MenuItem item) {
        return false;
    }

    @Override
    public void setCurrentFragment(BaseFragment baseFragment) {
        mCurrentFragment = baseFragment;
        mCurrentFragment.setFileOperationTaskManager(mFileOperationTaskManager);
    }

    public BaseFragment getCurrentFragment() {
        return mCurrentFragment;
    }

    @Override
    public void onBackPressed() {
        if(mCurrentFragment != null && mCurrentFragment.onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    public void backImmediately() {
        super.onBackPressed();
    }

    private void processExtraData(Intent intent) {
        Intent mIntent;
        Log.i(TAG, "processExtraData  " + intent);
        if (intent == null) {
            mIntent = getIntent();
        } else {
            mIntent = intent;
        }
        if (mIntent == null) return;
        String action = mIntent.getAction();
        if (action == null) return;
        if (Intent.ACTION_VIEW.equals(action)) {
            Uri pathuri = intent.getData();
            Log.i(TAG, "pathuri is " + pathuri);
            if (pathuri == null || TextUtils.isEmpty(pathuri.toString())) return;
            String path = null;
            Log.d(TAG, "processExtraData>pathuri>" + pathuri);
            String scheme = pathuri.getScheme();

            if(scheme != null && pathuri.getScheme().equals("file")){
                path =pathuri.getPath();
                android.util.Log.d(TAG, "processExtraData>path>" + path);

            } else if(scheme != null && scheme.equals("content")) {
                LogUtil.i(TAG, "processExtraData scheme is content");
                String authority = pathuri.getAuthority();
                LogUtil.i(TAG, "processExtraData authority is " + authority);
                if(authority != null && authority.equals("downloads")) {
                    path = queryDownloadManagerFilePath(pathuri);
                } else if(authority != null && authority.endsWith(".fileprovider")){
                    path = getPathFromFileProviderUri(pathuri);
                } else {
                    path = "" + pathuri;
                    LogUtil.i(TAG, " path: " + path + " throw new RuntimeException Not Implemented");
                }
            } else{
                path = pathuri.toString();
            }

            FragmentManager fragmentManager = getFragmentManager();
            int backStackCount = fragmentManager.getBackStackEntryCount();
            for(int i=0; i<backStackCount; i++) {
                fragmentManager.popBackStackImmediate();
            }
            Log.d(TAG, "the final path for viewFile---" + path);
            FileOperationUtil.viewFileForNoBack(this, path);
            return;
        }
    }


    private String getPathFromFileProviderUri(Uri pathUri){
        if(pathUri==null)return null;
        String authority = pathUri.getAuthority();
        if(authority==null)return null;
        String path=null;
        if("com.android.browser.fileprovider".equals(authority)){
            // content://com.android.browser.fileprovider/%2F/storage/emulated/0/AndroidZip/50ab33ce5dc2f902f98163f2acfd7b61.jpg.zip
            // %2F is "/"
            List<String> pathSegments = pathUri.getPathSegments();
            StringBuilder builder = new StringBuilder();
            for(int i=0; i<pathSegments.size(); i++) {
                Log.d(TAG, "authority: com.android.browser.fileprovider path segment " + i + ":" + pathSegments.get(i));
                String segment = pathSegments.get(i);
                builder.append(segment);
                boolean last = (i == pathSegments.size()-1);
                if(!segment.equals(FilePathUtil.PATH_SEPARATOR) && ! last ) {
                    builder.append(FilePathUtil.PATH_SEPARATOR);
                }
            }
            path = builder.toString();
        }else if("com.android.nfc.fileprovider".equals(authority)
                || "com.google.android.bluetooth.fileprovider".equals(authority)){
             //content://com.android.nfc.fileprovider/beam/beam/homepage.zip
            //content://com.google.android.bluetooth.fileprovider/bluetooth/bluetooth/homepage.zip
            StringBuilder builder = new StringBuilder();
            String phoneRootPath = StorageVolumeManager.getPhoneStoragePath(getApplicationContext());
            String nfcpath = pathUri.getPath();
            if(phoneRootPath==null || nfcpath==null)return null;
            String subPathString = null;
            int dex = nfcpath.indexOf(File.separator);
            if(dex != -1) {
                if (dex == 0) {
                    subPathString = nfcpath.substring(dex+1);
                    dex = subPathString.indexOf(File.separator);
                }
            }
            if(dex != -1){
                subPathString = nfcpath.substring(dex+1);
            }else{
                subPathString = nfcpath;
            }

            if(phoneRootPath != null && subPathString != null){
                builder.append(phoneRootPath);
                builder.append(subPathString);
                path =  builder.toString();
            }

        }else if("cn.com.protruly.filemanager.fileprovider".equals(authority)){
           // content://cn.com.protruly.filemanager.fileprovider/storage_path/storage/78FE-0907/新建文件夹 2/test/Desktop.zip
            String tempPath = pathUri.getPath();
            if(tempPath==null)return null;
            String subPathString = null;
            int dex = tempPath.indexOf(File.separator);
            if(dex != -1) {
                if (dex == 0) {
                    subPathString = tempPath.substring(dex+1);
                    dex = subPathString.indexOf(File.separator);
                }
            }
            if(dex != -1){
                subPathString = tempPath.substring(dex+1);
            }else{
                subPathString = tempPath;
            }

            if(subPathString != null){
                path = subPathString;
            }
        }else{
            path = pathUri.getPath();
        }

        Log.d(TAG, "processExtraData>fileprovider>path>" + path);
        return path;
    }



    private String queryDownloadManagerFilePath(Uri pathUri) {
        Cursor cursor = getContentResolver().query(pathUri, null, null, null ,null);
        boolean moved = cursor.moveToFirst();
        if(!moved) {
            DbUtil.closeSilently(cursor);
            return "";
        }
        int columnIndex = cursor.getColumnIndex("_data");
        if(-1 == columnIndex) {
            DbUtil.closeSilently(cursor);
            return "";
        }
        String filePath = cursor.getString(columnIndex);
        LogUtil.i(TAG, "content://download: " + pathUri + " " + filePath);
        DbUtil.closeSilently(cursor);
        return filePath;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LogUtil.i(TAG, "onActivityResult --->  requesstCode:" + requestCode);
    }



    /*
    public SingleMediaScanner getMediaScanner() {
        return mMediaScanner;
    }

    public void connectMediaScanner() {
        if(mMediaScanner == null) {
            mMediaScanner = new SingleMediaScanner(this);
        }
        if(!mMediaScanner.isConnected()) {
            mMediaScanner.connect();
        }
    }

    public void disconnectMediaScanner() {
        if(mMediaScanner.isConnected()) {
            mMediaScanner.disconnect();
        }
    }
    */
}

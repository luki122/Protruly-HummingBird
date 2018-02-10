package cn.com.protruly.filemanager.operation;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.media.MediaScannerConnection;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;

import cn.com.protruly.filemanager.utils.FileEnumerator;
import cn.com.protruly.filemanager.utils.FilePathUtil;
import cn.com.protruly.filemanager.utils.GlobalConstants;
import cn.com.protruly.filemanager.utils.LogUtil;
import cn.com.protruly.filemanager.utils.MediaDatabaseDao;
import cn.com.protruly.filemanager.enums.FileInfo;
import cn.com.protruly.filemanager.ui.OpenTypeDialogCreator;
import cn.com.protruly.filemanager.utils.IntentBuilderUtil;
import cn.com.protruly.filemanager.utils.MultiMediaScanner;
import cn.com.protruly.filemanager.utils.Util;
import hb.app.dialog.ProgressDialog;
import cn.com.protruly.filemanager.R;

import static cn.com.protruly.filemanager.utils.GlobalConstants.CATEGORY_EXECUTOR;

/**
 * Created by liushitao on 17-4-26.
 */

public class OperationHelper {

    private long mCompletedSize = 0;
    private long mCurrentStepSize = 0;
    private long mStepSize = 0;
    private long mTotalSize = 0x0;
    private long mCountSize = 0;
    private long mTempSize = 0;
    private Context mContext;
    private Handler mHandler;
    private MediaDatabaseDao mediaDatabaseDao;
    private Callable<String> mCallable;
    private Future<String> mFuture;
    public ProgressDialog mProgressDialog;
    private static final ExecutorService OPERATION_EXECUTOR = Executors.newFixedThreadPool(0x1);
    private static final Executor UPDATE_FOLDER_INFO_EXECUTOR = Executors.newFixedThreadPool(0x5);
    public OperationHelper(Context context, Handler handler){
        mContext = context;
        mHandler = handler;
        mediaDatabaseDao = new MediaDatabaseDao(mContext);
        mProgressDialog = new ProgressDialog(mContext);
    }

    private PowerManager.WakeLock acquireWakeLock(){
        PowerManager.WakeLock wakelock = null;
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        wakelock = pm.newWakeLock(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY,getClass().getSimpleName());
        wakelock.acquire();
        return wakelock;
    }

    private void releaseWakeLock(PowerManager.WakeLock paramWakeLock) {
        if (paramWakeLock != null)
            paramWakeLock.release();
    }


    public void createNewFolder(final File newFolderFile,final CreateFolderCallback cb){
        UPDATE_FOLDER_INFO_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                final boolean successCreate = newFolderFile.mkdirs();
                if(successCreate){
                    ContentValues values = new ContentValues();
                    values.put("_data",newFolderFile.getPath());
                    values.put("format","12289");
                    mediaDatabaseDao.insert(values);
                }
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        cb.onCreateFolderComplete(successCreate,newFolderFile);
                    }
                });
            }
        });
    }

    public void rename(final FileInfo oldInfo, final String newName, final Runnable failRunable){
        UPDATE_FOLDER_INFO_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                File newFile = new File(oldInfo.getParent(),newName);
                Log.d("hid","newFile.getname:"+newFile.getName());
                //boolean renameSuccess = oldInfo.renameTo(newFile);
                if(!newFile.exists()&&oldInfo.renameTo(newFile)){
                    FileInfo newInfo = new FileInfo(newFile);
                    /*
                    if(newInfo.isFile()){
                        deleteSingleFileFromDB(oldInfo.getPath());
                        Log.d("hid","newFile.getPath():"+newFile.getPath());
                        scanSingleFile(newFile.getPath());
                    }
                    */
                    ArrayList<FileInfo> fileInfos = new ArrayList<FileInfo>();
                    fileInfos.add(oldInfo);
                    fileInfos.add(newInfo);
                    Util.sendMessage(mHandler, OperationType.RENAME, fileInfos, 0);

                    //delete old
                    FileDbManager fileDbManager = new FileDbManager(mContext);
                    fileDbManager.batchDeleteFileStartWithPathPrefix(mContext, oldInfo.getPath());
                    //scan new
                    //MultiMediaScanner scanner = new MultiMediaScanner(mContext);
                    //scanner.scanFileInfo(newInfo);
                    //scanFileInfo(newInfo);
                    fileDbManager.scanFileInfo(newInfo);
                }
                if(false){
                    mHandler.post(failRunable);
                }
            }
        });
    }


    private static final int SCAN_THRESHOLD = 500;

    /**
     * If fileInfo is a directory, scanFileInfo will scan files under it.
     * @param fileInfo
     */
    public void scanFileInfo(FileInfo fileInfo) {
        if (fileInfo == null || fileInfo.getFile() == null) return;
        final ArrayList<String> paths = new ArrayList<String>();
        int totalNum = FilePathUtil.getAllFileAndDirectoryNum(fileInfo.getFile());
        final FileDbManager fileDbManager = new FileDbManager(mContext);
        FileEnumerator enumerator = new FileEnumerator(totalNum) {
            @Override
            public void onFileEnumerated(File file) {
                //LogUtil.i("Scan", "onFileEnumerated file:" + file.getPath());
                super.onFileEnumerated(file);
                paths.add(file.getPath());
                //LogUtil.i("Scan", "String path added:" + file.getPath());
                boolean reachThreshold = paths.size() >= SCAN_THRESHOLD;
                boolean finished = isEnumeationFinished();
                fileDbManager.addToInsertList(file.getPath(), file.isDirectory());
                if(reachThreshold || finished) {
                    fileDbManager.insertFromContentValueList();
                }
            }
        };
        FilePathUtil.enumerateFile(fileInfo.getFile(), enumerator);
    }

    private void deleteSingleFileFromDB(String deletePath){
        mediaDatabaseDao.deleteSingle(deletePath);
    }

    private void scanSingleFile(String scanPath){
        MediaScannerConnection.scanFile(mContext, new String[]{scanPath}, null, new MediaScannerConnection.OnScanCompletedListener() {
            @Override
            public void onScanCompleted(String path, Uri uri) {
                Log.d("hid","path:"+path+",uri:"+uri);
            }
        });
    }

    public interface CreateFolderCallback{
        void onCreateFolderComplete(boolean b,File file);
    }

    public void pickFile(final Context context, final FileInfo fileInfo){
        CATEGORY_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    Uri uri = mediaDatabaseDao.getContentUriFromPathForMusic(fileInfo.getPath(),Util.getCategoryForMusic(fileInfo.getFile()));
                    Intent intent = null;
                    if(uri!=null){
                        intent = Intent.parseUri(uri.toString(),0);
                    }else{
                        Uri uri0 = Uri.fromFile(fileInfo.getFile());
                        if(null != uri0){
                            intent = Intent.parseUri(uri0.toString(),0);
                        }
                    }
                    Log.d("uri","intent:"+intent);
                    ((Activity)context).setResult(-1,intent);
                    ((Activity)context).finish();
                }catch(URISyntaxException e){
                    e.fillInStackTrace();
                }
            }
        });
    }
    //contact set rngtong  send intent
    /*final Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        // Allow user to pick 'Default'
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        // Show only ringtones
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
        // Allow the user to pick a silent ringtone
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);*/
    public void setRingtone(final Context context, final FileInfo fileInfo){
        CATEGORY_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    Uri uri = mediaDatabaseDao.getContentUriFromPathForMusic(fileInfo.getPath(),Util.getCategoryForMusic(fileInfo.getFile()));
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, uri);
                    Intent intent = new Intent();
                    intent.putExtras(bundle);
                    Log.d("test","uri:"+uri);
                    Log.d("test","intent:"+intent);
                    ((Activity)context).setResult(-1,intent);
                    ((Activity)context).finish();
                }catch(Exception e){
                    e.fillInStackTrace();
                }
            }
        });
    }

    public void openWithMethod(final FileInfo fileInfo){
        CATEGORY_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                Intent intent = IntentBuilderUtil.buildIntent(fileInfo.getPath(),mContext);
                if(intent==null) return;
                String type = intent.getType();
                Log.d("open","type:"+type);
                if(type==null){
                    if(mContext==null){
                        return;
                    }
                    final OpenTypeDialogCreator creator = new OpenTypeDialogCreator(mContext, intent, fileInfo.getPath());
                    ((Activity)mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            creator.show();
                        }
                    });
                    return;
                }
                intent = Intent.createChooser(intent,mContext.getResources().getString(R.string.open_file_method));
                intent.putExtra("android.intent.extra.TITLE",mContext.getResources().getString(R.string.open_file_method));
                Util.startActivityForType(mContext,intent);
            }
        });
    }
}

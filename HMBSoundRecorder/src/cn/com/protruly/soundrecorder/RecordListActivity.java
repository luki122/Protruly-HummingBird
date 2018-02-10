package cn.com.protruly.soundrecorder;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import cn.com.protruly.soundrecorder.common.BaseActivity;
import cn.com.protruly.soundrecorder.common.ToolbarManager;
import cn.com.protruly.soundrecorder.lockscreen.MediaRecordTestActivity;
import cn.com.protruly.soundrecorder.managerUtil.AudioPlayManager;
import cn.com.protruly.soundrecorder.recordlist.CustomView;
import cn.com.protruly.soundrecorder.recordlist.ExpandableAdapter;
import cn.com.protruly.soundrecorder.recordlist.FileNameInputDialog;
import cn.com.protruly.soundrecorder.util.DatabaseUtil;
import cn.com.protruly.soundrecorder.util.GlobalConstant;
import cn.com.protruly.soundrecorder.util.GlobalUtil;
import cn.com.protruly.soundrecorder.util.RecordFileInfo;
import hb.app.dialog.AlertDialog;
import hb.view.menu.BottomWidePopupMenu;
import hb.widget.ActionMode;



/**
 * Created by wenwenchao on 17-8-14.
 */

public class RecordListActivity extends BaseActivity {

    private final String TAG = "RecordListActivity";
    CustomView.MyListView mRecordListView;
    ExpandableAdapter mRecordListViewAdapter;
    AudioPlayManager mAudioPlayManager;
    HashSet<RecordFileInfo> selectedFileSet = new HashSet<>();
    List<RecordFileInfo> RecordFileList;
    AudioManager audioManager;
    private static final Executor UPDATE_FOLDER_INFO_EXECUTOR = Executors.newFixedThreadPool(1);
    private DatabaseUtil databaseUtil;
    private MarkManager mMarkManager;
    private BDContentObserver mBDContentObserver;

    @Override
    protected void initData() {

    }

    @Override
    protected void initView() {

    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSPKIcon();
    }

    AudioPlayManager.playModeChangedCallBack mModeChangedCallBack = new AudioPlayManager.playModeChangedCallBack() {
        @Override
        public void onModeChanged() {
            updateSPKIcon();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHbContentView(R.layout.activity_record_list);
        RecordFileList = new ArrayList<RecordFileInfo>();
        mMarkManager =  new MarkManager(this);
        mAudioPlayManager = AudioPlayManager.getInstance();
        if(mModeChangedCallBack!=null){
            mAudioPlayManager.setPlayModeChangedCallBack(mModeChangedCallBack);
        }
        mAudioPlayManager.onInitAudioPlayManager(this);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        databaseUtil = new DatabaseUtil(getApplicationContext());

        getRecordFileFormDir();

        mRecordListView = (CustomView.MyListView) findViewById(R.id.recordlistview);
        mRecordListViewAdapter = new ExpandableAdapter(this,RecordFileList);
        mRecordListViewAdapter.setActionModeManager(mActionModeManager);
        mRecordListViewAdapter.setAudioPlayManager(mAudioPlayManager);
        mRecordListViewAdapter.setSelectedFileSet(selectedFileSet);
        mRecordListViewAdapter.setmOnMenuClickListenerInAll(mOnMenuClickListenerInAll);
        mRecordListViewAdapter.setItemExpandCollapseListener(mOnItemExpandCollapseListener);
        mRecordListViewAdapter.setParent(mRecordListView);
        mRecordListView.setAdapter(mRecordListViewAdapter);
        mRecordListView.setOnItemClickListener(mItemClickListener);
        mRecordListView.setOnItemLongClickListener(mItemLongClickListener);
        mRecordListView.setOnBlankClickListener(mOnBlankClickListener);

        mBDContentObserver = new BDContentObserver(new Handler());
        //注册短信变化监听
        getContentResolver().registerContentObserver(MediaStore.Files.getContentUri("external"), true, mBDContentObserver);

    }


    public void getRecordFileFormDir(){
        RecordFileList.clear();
        Cursor cursor = databaseUtil.queryRecordFile(GlobalUtil.getRecordDirPath());
        while(cursor.moveToNext())
        {
            RecordFileInfo recordFileInfo = new RecordFileInfo(cursor.getString(cursor.getColumnIndex("_data")));
            if(recordFileInfo.getName().endsWith(".mp3")){
                ArrayList<Long> marklist = mMarkManager.getMarkList(recordFileInfo.getPath());
                if(marklist!=null && marklist.size()>0){
                    recordFileInfo.addMarkList(marklist);
                }
                RecordFileList.add(recordFileInfo);
            }
        }
        cursor.close();
        sortAndUpdateRecordFileList(RecordFileList);//排序
    }





    AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            if(mActionModeManager.getCurrentActionMode()){
                RecordFileInfo fileInfo =(RecordFileInfo)((CustomView.MyListView)adapterView).getItemAtPosition(i);
                ExpandableAdapter.ViewHolder vh = (ExpandableAdapter.ViewHolder) view.getTag();
                selectFileOrNot(fileInfo,vh.checkBox);
            }else {
                mRecordListViewAdapter.firstopen = true;
                stopPlayAndPauseStats();
                mRecordListViewAdapter.getView(i, view, adapterView);
                ExpandableAdapter.ViewHolder vh = (ExpandableAdapter.ViewHolder) view.getTag();
                mRecordListViewAdapter.dealAnimationOnItemClick(view,vh.expandLayout, i);
            }

        }
    };
    AdapterView.OnItemLongClickListener mItemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {

                RecordFileInfo fileInfo =(RecordFileInfo)RecordFileList.get(i);
                mRecordListViewAdapter.collapseLastOpen();
                showLongClickDialog(fileInfo);
                return true;
            }
    };

    CustomView.MyListView.OnBlankClickListener mOnBlankClickListener = new CustomView.MyListView.OnBlankClickListener(){
        @Override
        public void OnBlankClick() {
               mRecordListViewAdapter.collapseLastOpen();
        }
    };


    ExpandableAdapter.OnMenuClickListenerInAll mOnMenuClickListenerInAll = new ExpandableAdapter.OnMenuClickListenerInAll(){
        @Override
        public void onPlayButtomClick(RecordFileInfo fileInfo, ExpandableAdapter.ViewHolder mvh) {
           Log.d("wwc516","onPlayButtomClick");
            if(mRecordListViewAdapter.getLastOpenPosition()==-1)return;
            mAudioPlayManager.onStartPlay(fileInfo.getPath(), 0,mRecordListViewAdapter.mHandler);

        }

        @Override
        public void onMarkButtomClick(RecordFileInfo fileInfo, ExpandableAdapter.ViewHolder mvh) {
            Log.d("wwc516","onMarkButtomClick");
            if(mRecordListViewAdapter.getLastOpenPosition()==-1)return;
            if(fileInfo.getMarkTimeList()!=null && fileInfo.getMarkTimeList().size()>49){
                Toast.makeText(RecordListActivity.this,getString(R.string.marked_size_warn),Toast.LENGTH_SHORT).show();
                return;
            }
            long marktime = mAudioPlayManager.GetNowPosForMark();
            if (marktime != -1) {
                sortAndUpdateMarkList(marktime,fileInfo,mvh);
                //mRecordListViewAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onEditButtomClick(RecordFileInfo fileInfo, ExpandableAdapter.ViewHolder mvh) {
            Log.d("wwc516","onEditButtomClick");
            if(mRecordListViewAdapter.getLastOpenPosition()==-1)return;
            stopPlayAndPauseStats();
            mRecordListViewAdapter.collapseLastOpen();
            Intent intent = new Intent(RecordListActivity.this,ClipActivity.class);
            intent.putExtra(ClipActivity.KEY_CLIP_FILE_PATH,fileInfo.getPath());
            RecordListActivity.this.startActivity(intent);
        }

        @Override
        public void onDeleteButtomClick(RecordFileInfo fileInfo, ExpandableAdapter.ViewHolder mvh,int index) {
            Log.d("wwc516","onDeleteButtomClick");
            if(mRecordListViewAdapter.getLastOpenPosition()==-1)return;
            Set<RecordFileInfo> list = new HashSet<>();
            list.add(fileInfo);
            delete(list);
        }

        @Override
        public void onMarkGridItemClick(RecordFileInfo fileInfo, ExpandableAdapter.ViewHolder mvh,int index) {
            Log.d("wwc516","onMarkGridItemClick->"+(long)(fileInfo.getMarkTimeList().get(index)));
            if(mRecordListViewAdapter.getLastOpenPosition()==-1)return;
            mAudioPlayManager.SeektoPlayAtPos(fileInfo.getPath(),(long)(fileInfo.getMarkTimeList().get(index)),mRecordListViewAdapter.mHandler);
        }

        @Override
        public void onMarkGridItemLongClick(RecordFileInfo fileInfo, ExpandableAdapter.ViewHolder mvh,int index) {
            Log.d("wwc516","onMarkGridItemLongClick");
            if(mRecordListViewAdapter.getLastOpenPosition()==-1)return;
            showDelateMarkWarmDialog(fileInfo, index);
        }
    };

    ExpandableAdapter.OnItemExpandCollapseListener mOnItemExpandCollapseListener = new ExpandableAdapter.OnItemExpandCollapseListener(){

        @Override
       public void onExpand(View itemView, int position) {
                        Log.d("wwc516","onExpand");
                       if(mRecordListView.getBackground()==null)mRecordListView.setBackgroundColor(Color.parseColor("#4d000000"));

                     /*  WindowManager.LayoutParams lp = getWindow().getAttributes();
                       lp.alpha = 0.4f;
                       getWindow().setAttributes(lp);*/
        }

               @Override
       public void onCollapse(View itemView, int position) {
                        Log.d("wwc516","onCollapse");
                        if(mRecordListView.getBackground()!=null)mRecordListView.setBackground(null);
                     /*   WindowManager.LayoutParams lp = getWindow().getAttributes();
                        lp.alpha = 1f;
                        getWindow().setAttributes(lp);*/
               }
    };




    void stopPlayAndPauseStats(){
        Log.d("wwc516","initPlayAndPauseStats>getStatus="+mAudioPlayManager.GetCurrentPlayStats());
        if(mAudioPlayManager.GetCurrentPlayStats()== GlobalConstant.PLAY_START|| mAudioPlayManager.GetCurrentPlayStats()== GlobalConstant.PLAY_GOING || mAudioPlayManager.GetCurrentPlayStats()== GlobalConstant.PLAY_PAUSE){
            mAudioPlayManager.onStopPlay();
            Log.d("wwc516","initPlayAndPauseStats>gotoStoped!  getStatus="+mAudioPlayManager.GetCurrentPlayStats());
        }
    }

    public void setActionModeState(boolean isActionMode){

        if(!isActionMode) {
            getActionModeManager().finishActionMode();
            mActionModeManager.setCurrentActionMode(false);
            selectedFileSet.clear();
        }else{
            mActionModeManager.startActionMode();
            mActionModeManager.setCurrentActionMode(true);
            mActionModeManager.setNegativeText(getString(R.string.cancel));
            mActionModeManager.setPositiveText(R.string.action_mode_select_all_text);
            mActionModeManager.showBottomNavigationMenuItem(R.id.action_menu_delete, true);
            selectedFileSet.clear();
            updateSelectedNum();
        }
        getRecordListViewAdapter().notifyDataSetChanged();
    }

    public ExpandableAdapter getRecordListViewAdapter() {
        return mRecordListViewAdapter;
    }

    @Override
    protected void onDestroy() {
        mAudioPlayManager.onDestroyAudioPlayManager();
        getContentResolver().unregisterContentObserver(mBDContentObserver);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

        if(getActionModeManager().getCurrentActionMode()){
            setActionModeState(false);
            return ;
        }else{
            super.onBackPressed();
        }
    }
    public void onBackPressedImmediately() {
            super.onBackPressed();
    }


    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        Log.d("wwc516","onNavigationItemSelected");
        if(menuItem.getItemId()==R.id.action_menu_delete){
            delete(selectedFileSet);
        }
        return super.onNavigationItemSelected(menuItem);
    }

    @Override
    public void onToolbarNavigationIconClicked() {
        super.onToolbarNavigationIconClicked();
        onBackPressed();
        Log.d("wwc516","onToolbarNavigationIconClicked");
    }


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if(mAudioPlayManager==null)return true;
        switch (item.getItemId()){
            case  R.id.spk_choose_menu_one:
                if(mAudioPlayManager.getAudioPlay_SPKMode()==AudioPlayManager.PLAY_SPK_MODE_SPK){
                    mAudioPlayManager.setAudioPlayMode_TelepReceiver(true);
                }else{
                    mAudioPlayManager.setAudioPlayMode_Speeker();
                }
                break;
            case R.id.spk_outdoor_menu:
                mAudioPlayManager.setAudioPlayMode_Speeker();
                break;
            case R.id.spk_hear_menu:
                mAudioPlayManager.setAudioPlayMode_TelepReceiver(true);
                break;
            case R.id.spk_bluetooth_menu:
                mAudioPlayManager.setAudioPlayMode_BlueTooth();
                break;

        }
        updateSPKIcon();
        return super.onMenuItemClick(item);
    }


    @Override
    public ToolbarManager getToolbarManager() {
        return super.getToolbarManager();
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
                break;
            case ActionMode.NAGATIVE_BUTTON:
                setActionModeState(false);
                break;
        }
    }

    @Override
    public void onActionModeShow(ActionMode actionMode) {
        super.onActionModeShow(actionMode);
    }

    @Override
    public void onActionModeDismiss(ActionMode actionMode) {
        super.onActionModeDismiss(actionMode);
    }


    private void updateSelectedNum(){
        String titleFormat;
        if(Locale.getDefault().toString().equals("en_GB")||Locale.getDefault().toString().equals("en_US") && selectedFileSet.size()==0){
            titleFormat = getResources().getString(R.string.zero_item,selectedFileSet.size());
        }else{
            titleFormat = getResources().getQuantityString(R.plurals.selected_item_count_format, selectedFileSet.size());
        }
        String title = String.format(titleFormat, selectedFileSet.size());
        getActionModeManager().setActionModeTitle(title);
        if(hasSelectedAll()){
            getActionModeManager().setPositiveText(R.string.action_mode_unselect_all_text);
        }else{
            getActionModeManager().setPositiveText(R.string.action_mode_select_all_text);
        }
        getRecordListViewAdapter().notifyDataSetChanged();
    }

    private boolean hasSelectedAll(){
        return RecordFileList.size()==selectedFileSet.size();
    }

    private boolean hasSelectedNone(){
        return 0 ==selectedFileSet.size();
    }

    private void deSelectFile(RecordFileInfo fileInfo,CheckBox checkBox){
        selectedFileSet.remove(fileInfo);
        checkBox.setChecked(false);
    }

    private void selectFile(RecordFileInfo fileInfo,CheckBox checkBox){
        selectedFileSet.add(fileInfo);
        checkBox.setChecked(true);
    }
    protected void selectFileOrNot(RecordFileInfo fileInfo,CheckBox checkBox){
        if(selectedFileSet.contains(fileInfo)){
            deSelectFile(fileInfo,checkBox);
        }else{
            selectFile(fileInfo,checkBox);
        }
        updateSelectedNum();
    }

    private void selectNone(){
        selectedFileSet.clear();
        getActionModeManager().setPositiveText(R.string.action_mode_select_all_text);
        updateSelectedNum();
    }

    private void selectAll(){
        selectedFileSet.clear();
        selectedFileSet.addAll(RecordFileList);
        getActionModeManager().setPositiveText(R.string.action_mode_unselect_all_text);
        updateSelectedNum();
    }



    public void showLongClickDialog(final RecordFileInfo fileInfo){

        final String[] longclicklist = {
                getResources().getString(R.string.rename),
                getResources().getString(R.string.share),
                getResources().getString(R.string.delete),
                getResources().getString(R.string.delete_pl)
        };
        BottomWidePopupMenu mBottomWidePopupMenu =  new BottomWidePopupMenu(this);
        mBottomWidePopupMenu.setTitle(fileInfo.getNameLabel());
        for(int i=0;i<longclicklist.length;i++){
            mBottomWidePopupMenu.addItem(i,longclicklist[i],null);
        }
        mBottomWidePopupMenu.setOnMenuItemClickedListener(new BottomWidePopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onItemClicked(MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case 0: rename(fileInfo);break;
                    case 1: {
                        List<RecordFileInfo> sharelist  = new ArrayList<RecordFileInfo>();
                        sharelist.add(fileInfo);
                        share(sharelist,RecordListActivity.this);
                    } break;
                    case 2: {
                        Set<RecordFileInfo> deleteSet  = new HashSet<RecordFileInfo>();
                        deleteSet.add(fileInfo);
                        delete(deleteSet);
                    }break;
                    case 3: setActionModeState(true);break;
                    default:break;
                }
                return true;
            }
        });
        mBottomWidePopupMenu.show();
    }



    void share(List<RecordFileInfo> files,Context context){
        Intent intent = buildSendFileIntent(files,context);
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)));
    }

    private static Intent buildSendFileIntent(List<RecordFileInfo> files, Context context) {
        ArrayList<Uri> uris = new ArrayList<>();
        Set<String> mimeSet = new HashSet<>();
        boolean isMediaFile = false;
        String mimeType = "audio/*";
        for (RecordFileInfo file : files) {
            if (file.isDirectory()) {
                continue;
            }
            mimeSet.add(mimeType);
            //LogUtil.i(TAG, "mimeType:" + mimeType + " isMediaFile:" + isMediaFile);
            Uri uri;
           // uri = Util.getUriForFile(context,new File(file.getPath()));
            uri = Uri.fromFile(file.getFile());
            //android 7.0 sharefile must use file provider content uri,common content uri invide;
            /*if(isMediaFile){
                uri = mediaDatabaseDao.getContentUriFromPath(file.getPath());
            } else {
                uri = Util.getUriForFile(context,new File(file.getPath()));
            }*/
            //LogUtil.i(TAG, "Send --> uri:" + uri);
            uris.add(uri);
        }
        if (uris.size() == 0) {
            return null;
        }
        boolean multiple = uris.size() > 1;
        Intent intent = new Intent(multiple ? android.content.Intent.ACTION_SEND_MULTIPLE
                : android.content.Intent.ACTION_SEND);
        if (multiple) {
            intent.setType(mimeType);
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        } else {
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
        }
        return intent;
    }


    void showDelateMarkWarmDialog(final RecordFileInfo fileinfo, final int markindex) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.delete_mark_warm) + "?");
        builder.setPositiveButton(getResources().getString(R.string.delete_selected_mark_warm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                fileinfo.getMarkTimeList().remove(markindex);
                //mMarkManager.putMarkList(fileinfo.getPath(),fileinfo.getMarkTimeList());
                mMarkManager.removeSomeMark(fileinfo.getPath(),markindex);
                Toast.makeText(RecordListActivity.this,getString(R.string.delete_success),Toast.LENGTH_SHORT).show();
                mRecordListViewAdapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton(getResources().getString(R.string.delete_all_mark_warm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                fileinfo.getMarkTimeList().clear();
                //mMarkManager.putMarkList(fileinfo.getPath(),fileinfo.getMarkTimeList());
                mMarkManager.clearAllMarkData(fileinfo.getPath());
                Toast.makeText(RecordListActivity.this,getString(R.string.delete_success),Toast.LENGTH_SHORT).show();
                mRecordListViewAdapter.notifyDataSetChanged();
            }
        });
        builder.setNeutralButton(getResources().getString(android.R.string.cancel), null);
        AlertDialog dialog = builder.create();
        dialog.show();

        Button positiveBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        Button negativeBtn = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        if (positiveBtn != null) {
            positiveBtn.setBackgroundResource(com.hb.R.drawable.button_background_hb_delete);
        }
        if (negativeBtn != null) {
            negativeBtn.setBackgroundResource(com.hb.R.drawable.button_background_hb_delete);
        }

    }



    public void delete(final Set<RecordFileInfo> files) {
        if (files == null || files.size()==0) {
            Toast.makeText(this,getString(R.string.select_warn),Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (files.size() == 1) {
            List<RecordFileInfo> list  = new ArrayList<RecordFileInfo>(files);
            builder.setTitle(getString(R.string.delete) + " "+list.get(0).getNameLabel() + "?");
        } else {
            builder.setTitle(getString(R.string.delete_select_warn));
        }
        builder.setPositiveButton(getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int currentExpandPos = mRecordListViewAdapter.getLastOpenPosition();
                RecordFileInfo currentFile = null;
                if(currentExpandPos!=-1) {
                    currentFile = RecordFileList.get(currentExpandPos);
                }
                for(RecordFileInfo file:files){
                    if(file.getFile().exists()){
                        file.getFile().delete();
                    }
                    if(RecordFileList.contains(file)){
                        RecordFileList.remove(file);
                    }
                    databaseUtil.deleteRecordFile(file.getPath());
                }

                Toast.makeText(RecordListActivity.this,getString(R.string.delete_success),Toast.LENGTH_SHORT).show();

                if(RecordFileList.size()==0){
                    onBackPressedImmediately();
                    return;
                }

                if(currentFile!=null && !RecordFileList.contains(currentFile)){
                    mRecordListViewAdapter.collapseLastOpen();
                }
                if(mActionModeManager.getCurrentActionMode()){
                    setActionModeState(false);
                }else{
                    mRecordListViewAdapter.notifyDataSetChanged();
                }

            }
        });

        builder.setNegativeButton(getResources().getString(android.R.string.cancel), null);
        AlertDialog dialog = builder.create();
        dialog.show();
        Button positiveBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (positiveBtn != null) {
            positiveBtn.setBackgroundResource(com.hb.R.drawable.button_background_hb_delete);
        }

    }





    public void rename(RecordFileInfo fileInfo) {
        createNameInputDialog(getResources().getString(R.string.rename), fileInfo.getName(), fileInfo);
    }

    protected <T> void createNameInputDialog(String title,String name,final T t){
        FileNameInputDialog.OnFinishFileInputListener finishFileInputListener = new FileNameInputDialog.OnFinishFileInputListener() {
            @Override
            public void onFinishFileNameInput(String str,String prefix) {
                if(startWithDot(str)){
                    showConfirmStartWithDotDialog(t,str+prefix);
                    return;
                }
                doAfterNewNameConfirmed(t,str+prefix);
            }
        };

        FileNameInputDialog  fileNameInputDialog = new FileNameInputDialog(this,title,name,(RecordFileInfo)t,finishFileInputListener);

        if(t instanceof RecordFileInfo){
            fileNameInputDialog.setIsFile(((RecordFileInfo)t).isFile());
        }
        fileNameInputDialog.show();
    }

    public boolean startWithDot(String name){
        return !TextUtils.isEmpty(name) && !name.equals(".") && name.startsWith(".");
    }

    private <T> void showConfirmStartWithDotDialog(final T t,final String str){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getResources().getString(R.string.confirm_hiden_file_create));
        builder.setPositiveButton(getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                doAfterNewNameConfirmed(t,str);
            }
        });
        builder.setNegativeButton(getResources().getString(android.R.string.cancel),null);
        builder.show();
    }

    protected  <T> void doAfterNewNameConfirmed(T t,String str){

                if(t instanceof RecordFileInfo){
                    doRename((RecordFileInfo)t,str);
                }
        }


    private void doRename(final RecordFileInfo fileInfo, final String str){
        Runnable failRun = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(RecordListActivity.this, str, Toast.LENGTH_SHORT).show();
            }
        };
        toRename(fileInfo,str,failRun);
    }


    public void toRename(final RecordFileInfo oldInfo, final String newName, final Runnable failRunable){
        UPDATE_FOLDER_INFO_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                File newFile = new File(oldInfo.getParent(),newName);
                Log.d("hid","newFile.getname:"+newFile.getName());
                //boolean renameSuccess = oldInfo.renameTo(newFile);
                if(!newFile.exists()&&oldInfo.renameTo(newFile)){
                    RecordFileInfo newInfo = new RecordFileInfo(newFile);
                    newInfo.addMarkList(oldInfo.getMarkTimeList());
                    newInfo.setTimeLong(oldInfo.getTimeLong());
                    newInfo.setTimeLabel(oldInfo.getTimeLabel());
                    if(RecordFileList.contains(oldInfo)){
                        int index = RecordFileList.indexOf(oldInfo);
                        RecordFileList.add(index,newInfo);
                        RecordFileList.remove(oldInfo);
                        databaseUtil.insertRecordFile(newInfo.getPath());
                        databaseUtil.deleteRecordFile(oldInfo.getPath());
                        if(newInfo.getMarkTimeList()!=null && newInfo.getMarkTimeList().size()>0){
                            mMarkManager.putMarkList(newInfo.getPath(),newInfo.getMarkTimeList());
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getRecordListViewAdapter().notifyDataSetChanged();
                            }
                        });
                    }
                    /*
                    if(newInfo.isFile()){
                        deleteSingleFileFromDB(oldInfo.getPath());
                        Log.d("hid","newFile.getPath():"+newFile.getPath());
                        scanSingleFile(newFile.getPath());
                    }

                    ArrayList<RecordFileInfo> fileInfos = new ArrayList<RecordFileInfo>();
                    fileInfos.add(oldInfo);
                    fileInfos.add(newInfo);
                    Util.sendMessage(mHandler, OperationType.RENAME, fileInfos, 0);

                    //delete old
                    FileDbManager fileDbManager = new FileDbManager(mContext);
                    fileDbManager.batchDeleteFileStartWithPathPrefix(mContext, oldInfo.getPath());
                    //scan new
                    MultiMediaScanner scanner = new MultiMediaScanner(mContext);
                    scanner.scanFileInfo(newInfo);*/

                }
                if(false){
                   /* mHandler.post(failRunable);*/
                }
            }
        });
    }





    public void sortAndUpdateRecordFileList(final List<RecordFileInfo> list){
        new AsyncTask(){
            @Override
            protected List<RecordFileInfo>  doInBackground(Object[] objects) {
                GlobalUtil.sortFileListByTime(list);
                return list;
            }
            @Override
            protected void onPostExecute(Object o) {
                mRecordListViewAdapter.notifyDataSetChanged();
            }
        }.execute();
    }



    private long markSucced=0;
    private Boolean isMarkBusy = false;
    public void sortAndUpdateMarkList(final long marktime,final RecordFileInfo fileInfo,final ExpandableAdapter.ViewHolder mvh){
       // if(marktime<markSucced)markSucced=0;
       // if(markSucced!=0 && marktime-markSucced<1500){
       //     Toast.makeText(RecordListActivity.this,getString(R.string.marked_fast_warn),Toast.LENGTH_SHORT).show();
       //     return;
       // }
        if(!isMarkBusy) {
            isMarkBusy = true;
            new AsyncTask() {
                @Override
                protected Object doInBackground(Object[] objects) {
                    if (!fileInfo.getMarkTimeList().contains(marktime)) {
                        fileInfo.addOneMark(marktime);
                        //markSucced = marktime;
                        GlobalUtil.sortLongList(fileInfo.getMarkTimeList());
                        mMarkManager.putMarkList(fileInfo.getPath(), fileInfo.getMarkTimeList());
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Object o) {
                    mvh.markButton.setEnabled(false);
                    mRecordListViewAdapter.notifyDataSetChanged();
                    isMarkBusy = false;
                }
            }.execute();
        }
    }



/*

    AsyncTask mSortAsyncTask = new AsyncTask<List<RecordFileInfo>,Intent,List<RecordFileInfo>>(){

        @Override
        protected List<RecordFileInfo> doInBackground(List<RecordFileInfo>... lists) {
            GlobalUtil.sortFileListByTime(lists[0]);
            return lists[0];
        }

        @Override
        protected void onPostExecute(List<RecordFileInfo> recordFileInfos) {
            super.onPostExecute(recordFileInfos);
            mRecordListViewAdapter.notifyDataSetChanged();
        }
    };
*/


   /* private void initPlayMode(){
        AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if(mAudioManager.getMode()){

        }
    }
*/
    private void updateSPKIcon(){
        if(mAudioPlayManager!=null){
            int SPK_Menu_id = 0;
           if(mAudioPlayManager.isBlueToothConnect()){
               mToolbarManager.switchToStatus(ToolbarManager.RECORD_LIST_SPK_MENU_LIST);
               SPK_Menu_id = R.id.spk_choose_menu_list;
           }else{
               mToolbarManager.switchToStatus(ToolbarManager.RECORD_LIST_SPK_MENU_ONE);
               SPK_Menu_id = R.id.spk_choose_menu_one;
           }
            switch (mAudioPlayManager.getAudioPlay_SPKMode()){
                case AudioPlayManager.PLAY_SPK_MODE_SPK: mToolbarManager.setToolbarMenuItemIcon(SPK_Menu_id,R.drawable.icon_spk_outdoor); break;
                case AudioPlayManager.PLAY_SPK_MODE_REC: mToolbarManager.setToolbarMenuItemIcon(SPK_Menu_id,R.drawable.icon_spk_hear); break;
                case AudioPlayManager.PLAY_SPK_MODE_BLU: mToolbarManager.setToolbarMenuItemIcon(SPK_Menu_id,R.drawable.icon_spk_bluetooth); break;
            }
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            // 音量减小
           case KeyEvent.KEYCODE_VOLUME_DOWN:
               if(mAudioPlayManager.adjustAudioStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,AudioManager.FX_FOCUS_NAVIGATION_UP)){
                   return true;
               }
               break;
       // 音量增大
          case KeyEvent.KEYCODE_VOLUME_UP:
              if(mAudioPlayManager.adjustAudioStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,AudioManager.FX_FOCUS_NAVIGATION_UP)){
                  return true;
              }
              break;
        }
        return super.onKeyDown(keyCode, event);
    }



    class BDContentObserver extends ContentObserver {

        public BDContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            getRecordFileFormDir();
            mRecordListViewAdapter.notifyDataSetChanged();
        }
    }



}









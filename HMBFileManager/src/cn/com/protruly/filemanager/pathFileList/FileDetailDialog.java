package cn.com.protruly.filemanager.pathFileList;

import cn.com.protruly.filemanager.utils.Util;
import hb.app.dialog.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;

import cn.com.protruly.filemanager.R;

import cn.com.protruly.filemanager.enums.FileInfo;

/**
 * Created by liushitao on 17-5-19.
 */

public class FileDetailDialog extends AlertDialog{
    private FileInfo mFileInfo;
    private ArrayList<FileInfo> mfileList;
    private ArrayList<FileInfo> mSubfileList;
    private ArrayList<FileInfo> mSubfolderList;

    private TextView mNameTip;
    private TextView mNameInfo;
    private TextView mLocationInfo;
    private TextView mLocationTip;
    private TextView mMediaAttributeInfo;
    private TextView mMediaAttributeTip;
    private TextView mModifyInfo;
    private TextView mModifyTip;
    private TextView mSizeInfo;
    private TextView mSizeTip;

    private TextView mFileNumInfo;
    private TextView mFolderNumInfo;
    private TextView mTotalSizeInfo;
    private TextView mFileNumTip;
    private TextView mFolderNumTip;
    private TextView mTotalSizeTip;
    private Context mContext;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            long size = msg.getData().getLong("SIZE");
            long total_size = msg.getData().getLong("TOTAL_SIZE");
            if(size!=-1){
                mSizeInfo.setText(Util.formatSize(getContext(),size));
                super.handleMessage(msg);
                return;
            }
            if(total_size!=-1){
                mTotalSizeInfo.setText(Util.formatSize(getContext(),total_size));
                super.handleMessage(msg);
                return;
            }
        }
    };

    private AttibuteObtainerTask mTask;

    public FileDetailDialog(Context context,FileInfo fileInfo) {
        super(context);
        mFileInfo = fileInfo;
    }

    public FileDetailDialog(Context context, HashSet<FileInfo> mSelectSet) {
        super(context);
        mContext = context;
        mfileList = new ArrayList<>();
        mSubfileList = new ArrayList<>();
        mSubfolderList = new ArrayList<>();
        mfileList = new ArrayList<>(mSelectSet);
        if(mfileList!=null){
            mFileInfo = mfileList.get(0);
        }
    }

    private void getFileAndFolderList(ArrayList<FileInfo> mSelectSet){
        for(FileInfo fileInfo:mSelectSet){
            if(fileInfo.isFile){
                mSubfileList.add(fileInfo);
            }else{
                mSubfolderList.add(fileInfo);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.file_detail_dialog_layout,null);
        initView(view);
        setView(view);
        setTitle(mContext.getResources().getString(R.string.action_menu_detail));
        setButton(DialogInterface.BUTTON_POSITIVE,mContext.getResources().getString(android.R.string.ok),(OnClickListener)null);
        mTask = new AttibuteObtainerTask(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mfileList.size()==1){
            showSingelDetailInfo();
        }else{
            showMuiltyDetailInfo();
        }
    }

    private void showMuiltyDetailInfo(){
        hideViewInMultiDetail();
        String filePath = mFileInfo.getParent();
        mNameInfo.setText(Util.getString(mContext,R.string.and_so_on,mFileInfo.getName()));
        mLocationInfo.setText(filePath);
        if(mTask!=null) {
            mTask.execute(mfileList);
        }
    }

    private void hideViewInMultiDetail(){
        hideView(mMediaAttributeInfo);
        hideView(mMediaAttributeTip);
        hideView(mModifyInfo);
        hideView(mModifyTip);
        hideView(mSizeTip);
        hideView(mSizeInfo);
    }

    private void showSingelDetailInfo(){
        hideViewInSingelDetail();
        if(mFileInfo.isFile){
            showFileDetail();
        }else{
            showFolderDetail();
        }
        String filePath = mFileInfo.getPath();
        mNameInfo.setText(mFileInfo.getName());
        mLocationInfo.setText(filePath);
        mModifyInfo.setText(Util.formatDateStringThird(mFileInfo.lastModified()));
    }

    private void hideViewInSingelDetail(){
        hideView(mFileNumInfo);
        hideView(mFileNumTip);
        hideView(mFolderNumInfo);
        hideView(mFolderNumTip);
        hideView(mTotalSizeTip);
        hideView(mTotalSizeInfo);
    }

    private void showFolderDetail(){
        hideView(mMediaAttributeInfo);
        hideView(mMediaAttributeTip);
        mLocationTip.setText(getContext().getResources().getString(R.string.file_info_location));
        mSizeTip.setText(getContext().getResources().getString(R.string.file_info_size));
        mTask.execute(mfileList);
    }

    private void showFileDetail(){
        showView(mMediaAttributeInfo);
        showView(mMediaAttributeTip);
        mLocationTip.setText(getContext().getResources().getString(R.string.file_info_location));
        mSizeTip.setText(getContext().getResources().getString(R.string.file_info_size));
        mSizeInfo.setText(Util.formatSize(getContext(),mFileInfo.fileSize));
        if(Util.isImageFile(mFileInfo.getPath())){
            mMediaAttributeTip.setText(getContext().getResources().getString(R.string.file_info_density));
            mTask.execute(mfileList);
            return;
        }
        if(Util.isAudioFile(mFileInfo.getPath()) || Util.isVideoFile(mFileInfo.getPath())){
            mMediaAttributeInfo.setText(getContext().getResources().getString(R.string.file_info_duration));
            mTask.execute(mfileList);
            return;
        }else{
            hideView(mMediaAttributeInfo);
            hideView(mMediaAttributeTip);
        }
    }

    private void showView(View view){
        if(view.getVisibility() != View.VISIBLE){
            view.setVisibility(View.VISIBLE);
        }
    }

    private void hideView(View view){
        if(view.getVisibility() == View.VISIBLE){
            view.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mTask.cancel(true);
    }

    private void initView(View view){
        mNameTip = ((TextView)view.findViewById(R.id.tv_name_tip));
        mNameInfo = ((TextView)view.findViewById(R.id.tv_name_info));

        mLocationTip = ((TextView)view.findViewById(R.id.tv_location_tip));
        mLocationInfo = ((TextView)view.findViewById(R.id.tv_location_info));
        mSizeTip = ((TextView)view.findViewById(R.id.tv_size_tip));
        mSizeInfo = ((TextView)view.findViewById(R.id.tv_size_info));
        mMediaAttributeTip = ((TextView)view.findViewById(R.id.tv_media_attribute_tip));
        mMediaAttributeInfo = ((TextView)view.findViewById(R.id.tv_media_attribute_info));
        mModifyInfo = ((TextView)view.findViewById(R.id.tv_modify_info));
        mModifyTip = ((TextView)view.findViewById(R.id.file_info_modified));

        mFileNumInfo = ((TextView)view.findViewById(R.id.tv_file_num_info));
        mFolderNumInfo = ((TextView)view.findViewById(R.id.tv_folder_num_info));
        mTotalSizeInfo = ((TextView)view.findViewById(R.id.tv_total_size_info));
        mFileNumTip = ((TextView)view.findViewById(R.id.file_num_info));
        mFolderNumTip = ((TextView)view.findViewById(R.id.folder_num_info));
        mTotalSizeTip = ((TextView)view.findViewById(R.id.total_size_info));
    }

    private class AttibuteObtainerTask extends AsyncTask<ArrayList<FileInfo>,Void,Object>{
        long folderSize;
        long totalSize;
        private AttibuteObtainerTask(FileDetailDialog fileDetailDialog) {
            folderSize = 0;
            totalSize = 0;
        }

        @Override
        protected Object doInBackground(ArrayList<FileInfo>... params) {
            if(params[0].size()>1){
                totalSize = 0;
                getFileAndFolderList(params[0]);
                getTotalSize(params[0]);
                return "total";
            }
            FileInfo info = params[0].get(0);
            if(info.isFile){
                if(Util.isImageFile(info.getPath())){
                    return getDesity(info.getPath());
                }
                return getDuration(info.getPath());
            }
            folderSize = 0;
            folderSize=Util.getFileSize(mContext,info.getFile());
            return null;
        }

        private BitmapFactory.Options getDesity(String path){
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            if(isCancelled()){
                return options;
            }
            BitmapFactory.decodeFile(path,options);
            return options;
        }

        private long getDuration(String path){
            try{
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(path);
                if(isCancelled()){
                    return 0;
                }
                String dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if(dur!=null){
                    return Long.valueOf(dur);
                }
            }catch(Exception e){
                if(e.getMessage()!=null){
                    e.fillInStackTrace();
                }
            }
            return 0;
        }

        private void getTotalSize(ArrayList<FileInfo> filelist){
            for(FileInfo fileInfo:filelist){
                totalSize+=Util.getFileSize(mContext,fileInfo.getFile());
            }
        }

        @Override
        protected void onPostExecute(Object object) {
            if(object instanceof BitmapFactory.Options){
                BitmapFactory.Options  options = (BitmapFactory.Options) object;
                mMediaAttributeInfo.setText(options.outHeight+"x"+options.outWidth);
            }
            if(object==null){
                updateSizeInfo(folderSize);
                return;
            }
            if(object.toString().equals("total")){
                mFileNumInfo.setText(Util.getString(mContext,R.string.file_count,mSubfileList.size()));
                mFolderNumInfo.setText(Util.getString(mContext,R.string.file_count,mSubfolderList.size()));
                updateTotalSizeInfo(totalSize);
                return;
            }
            if(!(object instanceof Long)){
                return;
            }
            long dur = ((Long)object);
            mMediaAttributeInfo.setText(Util.formatTimeString(dur));
            super.onPostExecute(object);
        }
    }

    private void updateSizeInfo(long size){
        Message message = mHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putLong("SIZE",size);
        bundle.putLong("TOTAL_SIZE",-1);
        message.setData(bundle);
        mHandler.sendMessage(message);
    }

    private void updateTotalSizeInfo(long size){
        Message message = mHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putLong("SIZE",-1);
        bundle.putLong("TOTAL_SIZE",size);
        message.setData(bundle);
        mHandler.sendMessage(message);
    }



}

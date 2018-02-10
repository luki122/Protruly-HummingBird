package cn.com.protruly.filemanager.pathFileList;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import cn.com.protruly.filemanager.enums.FileInfo;
import cn.com.protruly.filemanager.globalsearch.RoundAngleImageView;
import cn.com.protruly.filemanager.utils.Util;
import cn.com.protruly.filemanager.R;

/**
 * Created by liushitao on 17-6-22.
 */
public class FileListAdapter extends BaseListAdapter{
    private boolean mShowFolderName = false;
    private static final Executor GET_FOLDE_SIZE_EXECUTOR = Executors.newCachedThreadPool();

    private static final int MSG_UPDATE_FOLDER_INFO = 100;
    private static final int MSG_UPDATE_FILE_INFO = 200;

    public FileListAdapter(Context context, List<FileInfo> fileList) {
        super(context, fileList);
    }

    public FileListAdapter(Context context, List<FileInfo> fileList, boolean show) {
        super(context, fileList);
        mShowFolderName = show;
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_UPDATE_FOLDER_INFO:
                    FInfo folderInfo = (FInfo) msg.obj;
                    FoderViewHolder foderViewHolder = folderInfo.foderViewHolder;
                    String foder_path = folderInfo.path;
                    long foder_size = folderInfo.size;
                    long foder_count = folderInfo.count;
                    if (foder_size != -1 && foderViewHolder.getFoderTag().equals(foder_path)) {
                        setText(foderViewHolder.fileSize, Util.formatSize(mContext, foder_size));
                    }
                    if (foder_count != -1 && foderViewHolder.getFoderTag().equals(foder_path)) {
                        if((Locale.getDefault().toString().equals("en_GB")||Locale.getDefault().toString().equals("en_US"))&&(foder_count==0||foder_count==1)){
                            setText(foderViewHolder.fileCount, getString(R.string.zero_one_file_count, foder_count));
                        }else {
                            setText(foderViewHolder.fileCount, getString(R.string.file_count, foder_count));
                        }
                    }
                    break;
                case MSG_UPDATE_FILE_INFO:
                    FInfo fileInfo = (FInfo) msg.obj;
                    FileViewHolder fileViewHolder = fileInfo.fileViewHolder;
                    String path = fileInfo.path;
                    long size = fileInfo.size;
                    long modifytime = fileInfo.modifyTime;
                    if (size != -1 && fileViewHolder.getFileTag().equals(path)) {
                        setText(fileViewHolder.fileSize, Util.formatSize(mContext, size));
                    }
                    if (modifytime != -1 && fileViewHolder.getFileTag().equals(path)) {
                        setText(fileViewHolder.modifiedTime, Util.formatDateStringThird(modifytime));
                    }
                    break;
            }
        }
    };

    public View getView(int position, View convertView, ViewGroup parent) {
        FileViewHolder fileViewHolder = null;
        FoderViewHolder foderViewHolder = null;
        FileInfo info = mFileInfoList.get(position);
        if(info.isFile){
            if(convertView == null || !(convertView.getTag() instanceof FileViewHolder)) {
                convertView = View.inflate(mContext,R.layout.path_file_list_item,null);
                fileViewHolder = new FileViewHolder(convertView);
                convertView.setTag(fileViewHolder);
            } else {
                fileViewHolder = (FileViewHolder) convertView.getTag();
            }
            showFileInfo(fileViewHolder,info);
            setCheckboxStatus(fileViewHolder,info);
        }else{
            if(convertView == null || !(convertView.getTag() instanceof FoderViewHolder)) {
                convertView = View.inflate(mContext,R.layout.path_foder_list_item,null);
                foderViewHolder = new FoderViewHolder(convertView);
                convertView.setTag(foderViewHolder);
            } else {
                foderViewHolder = (FoderViewHolder) convertView.getTag();
            }
            showPathInfo(foderViewHolder,info);
            setCheckboxStatus(foderViewHolder,info);
        }
        return convertView;
    }

    private void setCheckboxStatus(Object object,FileInfo fileInfo){
        FileViewHolder fileViewHolder;
        FoderViewHolder foderViewHolder;
        if(object instanceof FileViewHolder){
            fileViewHolder = (FileViewHolder)object;
            if(isActionModeState()){
                showView(fileViewHolder.checkbox);
                if(mSelectedFileSet.contains(fileInfo) && !fileViewHolder.checkbox.isChecked()){
                    fileViewHolder.checkbox.setChecked(true);
                } else if(!mSelectedFileSet.contains(fileInfo) && fileViewHolder.checkbox.isChecked()){
                    fileViewHolder.checkbox.setChecked(false);
                }
            }else{
                fileViewHolder.checkbox.setChecked(false);
                hideView(fileViewHolder.checkbox);
            }
        }else{
            foderViewHolder = (FoderViewHolder)object;
            if(isActionModeState()){
                showView(foderViewHolder.checkbox);
                if(mSelectedFileSet.contains(fileInfo) && !foderViewHolder.checkbox.isChecked()){
                    foderViewHolder.checkbox.setChecked(true);
                } else if(!mSelectedFileSet.contains(fileInfo) && foderViewHolder.checkbox.isChecked()){
                    foderViewHolder.checkbox.setChecked(false);
                }
            }else{
                foderViewHolder.checkbox.setChecked(false);
                hideView(foderViewHolder.checkbox);
            }
        }

    }

    private void showFileInfo(FileViewHolder holder, FileInfo info) {
        setFileIcon(info, holder.fileIcon,holder);
        setText(holder.fileName, info.fileName);
        holder.setFileTag(info.filePath);
        if(info.fileSize == -1 || info.modifiedTime == -1){
            setText(holder.fileSize, mContext.getResources().getString(R.string.loading_tip));
            setText(holder.modifiedTime, "");
            getFileSize(holder,info);
        }else{
            setText(holder.fileSize, Util.formatSize(mContext, info.fileSize));
            setText(holder.modifiedTime, Util.formatDateStringThird(info.modifiedTime));
        }
    }

    public void setShowFolderName(boolean show) {
        mShowFolderName = show;
    }

    private void showPathInfo(FoderViewHolder holder, FileInfo info) {
        if(isActionModeState()) {
            hideView(holder.rightArrow);
        } else {
            showView(holder.rightArrow);
        }
        setImageSize(holder.fileIcon, "file");
        holder.fileIcon.setCenterImgShow(R.drawable.vediopreview,false);
        setImageRes(holder.fileIcon, R.drawable.ic_file_icon_folder);
        setText(holder.fileName, info.fileName);
        holder.setFoderTag(info.filePath);
        if(info.fileSize == -1 || info.childFileNum == -1){
            setText(holder.fileCount, mContext.getResources().getString(R.string.loading_tip));
            setText(holder.fileSize, "");
            getFolderSize(holder,info);
        }else{
            if((Locale.getDefault().toString().equals("en_GB")||Locale.getDefault().toString().equals("en_US"))&&(info.childFileNum==0||info.childFileNum==1)){
                setText(holder.fileCount, getString(R.string.zero_one_file_count, info.childFileNum));
            }else {
                setText(holder.fileCount, getString(R.string.file_count, info.childFileNum));
            }
            setText(holder.fileSize, Util.formatSize(mContext, info.fileSize));
        }
        if((mShowFolderName)&& (FolderNote.needToShowFolderName(info.filePath))){
            showView(holder.folderName);
            setText(holder.folderName,FolderNote.getFolderName(info.filePath));
        } else {
            setText(holder.folderName, "");
            hideView(holder.folderName);
        }
    }

    private void getFolderSize(final FoderViewHolder viewHolder,final FileInfo fileInfo){
        GET_FOLDE_SIZE_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                long fileSize = Util.getFileSize(mContext,fileInfo.getFile());
                fileInfo.fileSize = fileSize;
                long fileCount = Util.getChildNum(mContext,fileInfo);
                updateSizeInfo(viewHolder,fileSize,fileCount,fileInfo);
            }
        });
    }

    private void getFileSize(final FileViewHolder viewHolder,final FileInfo fileInfo){
        GET_FOLDE_SIZE_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                fileInfo.fileSize = fileInfo.length();
                fileInfo.modifiedTime = fileInfo.lastModified();
                updateFileSizeInfo(viewHolder,fileInfo.fileSize,fileInfo.modifiedTime,fileInfo);
            }
        });
    }



    private class FInfo {
        public long size;
        public long count;
        public long modifyTime;
        public FoderViewHolder foderViewHolder;
        public FileViewHolder fileViewHolder;
        public String path;
    }

    private void updateSizeInfo(FoderViewHolder viewHolder,long size,long count,FileInfo fileInfo){
        FInfo folderInfo = new FInfo();
        folderInfo.count = count;
        folderInfo.size = size;
        folderInfo.foderViewHolder = viewHolder;
        folderInfo.path = fileInfo.filePath;
        mHandler.obtainMessage(MSG_UPDATE_FOLDER_INFO, folderInfo).sendToTarget();
    }

    private void updateFileSizeInfo(FileViewHolder viewHolder,long size,long time,FileInfo fileInfo){
        FInfo folderInfo = new FInfo();
        folderInfo.size = size;
        folderInfo.modifyTime = time;
        folderInfo.fileViewHolder = viewHolder;
        folderInfo.path = fileInfo.filePath;
        mHandler.obtainMessage(MSG_UPDATE_FILE_INFO, folderInfo).sendToTarget();
    }

    protected class FileViewHolder implements Serializable {
        public RoundAngleImageView fileIcon;
        public TextView fileName;
        public TextView fileSize;
        public TextView modifiedTime;
        public CheckBox checkbox;
        public String tag;

        public void setFileTag(String mTag){
            tag = mTag;
        }

        public String getFileTag(){
            return tag;
        }

        public FileViewHolder(View view) {
            fileIcon = (RoundAngleImageView)view.findViewById(R.id.file_icon);
            fileName = (TextView)view.findViewById(R.id.file_name);
            modifiedTime = (TextView)view.findViewById(R.id.modified_time);
            fileSize = (TextView)view.findViewById(R.id.file_size);
            checkbox = (CheckBox)view.findViewById(R.id.checkbox);
        }
    }

    protected class FoderViewHolder implements Serializable {
        public RoundAngleImageView fileIcon;
        public TextView fileName;
        public TextView folderName;
        public TextView fileCount;
        public TextView fileSize;
        public ImageView rightArrow;
        public CheckBox checkbox;
        public String tag;

        public void setFoderTag(String mTag){
            tag = mTag;
        }

        public String getFoderTag(){
            return tag;
        }

        public FoderViewHolder(View view) {
            fileIcon = (RoundAngleImageView)view.findViewById(R.id.file_icon);
            fileName = (TextView)view.findViewById(R.id.file_name);
            fileCount = (TextView)view.findViewById(R.id.file_count);
            fileSize = (TextView)view.findViewById(R.id.file_size);
            checkbox = (CheckBox)view.findViewById(R.id.checkbox);
            rightArrow = (ImageView)view.findViewById(R.id.rightarrow);
            folderName = (TextView)view.findViewById(R.id.folder_name);
        }
    }

}

package cn.com.protruly.filemanager.historyList;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.List;

import cn.com.protruly.filemanager.enums.FileInfo;
import cn.com.protruly.filemanager.globalsearch.PinnedSectionListView;
import cn.com.protruly.filemanager.globalsearch.RoundAngleImageView;
import cn.com.protruly.filemanager.imageloader.ImageLoader;
import cn.com.protruly.filemanager.pathFileList.BaseListAdapter;
import cn.com.protruly.filemanager.pathFileList.FileListAdapter;
import cn.com.protruly.filemanager.utils.GlobalConstants;
import cn.com.protruly.filemanager.utils.Util;
import cn.com.protruly.filemanager.R;


public class HistoryListAdapter extends BaseListAdapter implements PinnedSectionListView.PinnedSectionListAdapter {
    private boolean isFromHome = false;

    public HistoryListAdapter(Context context, List<FileInfo> fileList) {
        super(context, fileList);
    }

    public HistoryListAdapter(Context context, List<FileInfo> fileList, boolean show) {
        super(context, fileList);
        isFromHome = show;
    }

    public void setList(List<FileInfo> fileInfos) {
        mFileInfoList = fileInfos;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ContentViewHolder contentViewHolder = null;
        FileInfo info = mFileInfoList.get(position);
        if(isFromHome) {
            if (convertView == null) {
                convertView = View.inflate(mContext, R.layout.history_home_item, null);
                contentViewHolder = new ContentViewHolder(convertView);
                convertView.setTag(contentViewHolder);
            } else {
                contentViewHolder = (ContentViewHolder) convertView.getTag();
            }
        }

        if(info.sectionType == GlobalConstants.SECTION_TYPE ){
            if(convertView == null){
                convertView = View.inflate(mContext,R.layout.history_title_item,null);
                contentViewHolder = new ContentViewHolder(convertView);
                convertView.setTag(contentViewHolder);
            }else{
                contentViewHolder = (ContentViewHolder) convertView.getTag();
            }
        }else {
            if(convertView == null){
                convertView =  View.inflate(mContext,R.layout.history_list_item,null);
                contentViewHolder = new ContentViewHolder(convertView);
                convertView.setTag(contentViewHolder);
            }else{
                contentViewHolder = (ContentViewHolder) convertView.getTag();
            }

            if(isActionModeState()){
                showView(contentViewHolder.checkbox);
                if(mSelectedFileSet.contains(info) && !contentViewHolder.checkbox.isChecked()){
                    contentViewHolder.checkbox.setChecked(true);
                } else if(!mSelectedFileSet.contains(info) && contentViewHolder.checkbox.isChecked()){
                    contentViewHolder.checkbox.setChecked(false);
                }
            }else if(null != contentViewHolder.checkbox){
                contentViewHolder.checkbox.setChecked(false);
                hideView(contentViewHolder.checkbox);
            }
        }
        if(info.isFile &&info.sectionType != GlobalConstants.SECTION_TYPE) {
            showFileInfo(contentViewHolder, info);
        }else if(info.sectionType == GlobalConstants.SECTION_TYPE && contentViewHolder.historyTitle!=null){
            contentViewHolder.historyTitle.setText(info.fileName);
        }
        return convertView;
    }

    @Override
    public int getItemViewType(int position){
        if(mFileInfoList.size()!=0 && position!=0 && position<=mFileInfoList.size()-1){
            return ((FileInfo)getItem(position)).sectionType;
        }
        return GlobalConstants.SECTION_TYPE;
    }

    @Override
    public int getViewTypeCount() {
        return 11;
    }

    private void showFileInfo(ContentViewHolder holder, FileInfo info) {
        if(isFromHome){
            setFileIcon(info, holder.fileIcon,holder);
            setText(holder.fileName, info.fileName);
            holder.fileName.setTag(info.filePath);
            return;
        }
        setFileIcon(info, holder.fileIcon,holder);
        setText(holder.fileName, info.fileName);
        holder.fileName.setTag(info.filePath);
        showView(holder.modifiedTime);
        setText(holder.modifiedTime, Util.formatDateStringThird(info.modifiedTime));
        showView(holder.fileSize);
        setText(holder.fileSize, Util.formatSize(mContext, info.fileSize));
    }

    private void setFileIcon(FileInfo fileInfo, RoundAngleImageView imageView, final ContentViewHolder fileViewHolder){
        int defaltIcon = Util.getDefaultIconRes(fileInfo.getPath());
        imageView.setTag(fileInfo.getPath());
        imageView.setCenterImgShow(R.drawable.vediopreview,false);
        imageView.setImageResource(defaltIcon);
        if(Util.isImageFile(fileInfo.getPath())){
            mImageLoader.displayLocalThumbnail(mContext, fileInfo.getPath(), fileInfo.getPath(), imageView);
        }else if(Util.isVideoFile(fileInfo.getPath())){
            final String path = fileInfo.getPath();
            mImageLoader.displayVideoThumbnail(mContext,path, path, new ImageLoader.ImageProcessingCallback() {
                @Override
                public void onImageProcessing(WeakReference<Bitmap> weak, String tag) {
                    RoundAngleImageView imageView = fileViewHolder.fileIcon;
                    if( ! ((String)imageView.getTag()).equals(path)) {
                        return;
                    }
                    Bitmap bitmap = weak.get();
                    if(null == bitmap) return;
                    imageView.setCenterImgShow(R.drawable.vediopreview,true);
                    imageView.setImageBitmap(bitmap);
                }
            });
        }else{
            imageView.setImageResource(defaltIcon);
        }
    }

    @Override
    public boolean isItemViewTypePinned(int viewType) {
        return viewType == GlobalConstants.SECTION_TYPE;
    }

    private class ContentViewHolder {
        public CheckBox checkbox;
        public RoundAngleImageView fileIcon;
        public TextView fileName;
        public TextView fileSize;
        public TextView modifiedTime;
        public TextView historyTitle;

        public ContentViewHolder(View view) {
            fileIcon = (RoundAngleImageView)view.findViewById(R.id.file_icon);
            fileName = (TextView)view.findViewById(R.id.file_name);
            modifiedTime = (TextView)view.findViewById(R.id.modified_time);
            fileSize = (TextView)view.findViewById(R.id.file_size);
            historyTitle = (TextView)view.findViewById(R.id.history_title);
            checkbox = (CheckBox) view.findViewById(R.id.checkbox);
        }
    }
}


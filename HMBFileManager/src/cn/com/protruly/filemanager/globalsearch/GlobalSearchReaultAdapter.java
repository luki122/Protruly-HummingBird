package cn.com.protruly.filemanager.globalsearch;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;

import cn.com.protruly.filemanager.utils.GlobalConstants;

import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import cn.com.protruly.filemanager.R;
import cn.com.protruly.filemanager.enums.Category;
import cn.com.protruly.filemanager.enums.FileInfo;
import cn.com.protruly.filemanager.imageloader.ImageLoader;
import cn.com.protruly.filemanager.ui.ActionModeManager;
import cn.com.protruly.filemanager.utils.Util;


public class GlobalSearchReaultAdapter extends BaseAdapter implements PinnedSectionListView.PinnedSectionListAdapter{
    private SearchSectionManager mSearchSectionManager;
    private List<FileInfo> fileInfoList;//这个才是真正显示的list数据
    protected HashSet<FileInfo> selectedFileSet;
    private Context context;
    protected ImageLoader mImageLoader;
    protected ActionModeManager actionModeManager;
    private int mScrollStatusFlag=0;
    private final Executor GET_FOLDE_SIZE_EXECUTOR = Executors.newFixedThreadPool(2);
    private final int MSG_UPDATE_FOLDER_INFO = 100;
    private final DateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    public GlobalSearchReaultAdapter(Context context,SearchSectionManager searchSectionManager) {
        this.context = context;
        this.mSearchSectionManager = searchSectionManager;
        fileInfoList = searchSectionManager.fileinfolist;
        selectedFileSet = searchSectionManager.selectedFileSet;
    }

    public void setSearchSectionManager(SearchSectionManager searchSectionManager){
        this.mSearchSectionManager = searchSectionManager;
        fileInfoList = searchSectionManager.fileinfolist;
        selectedFileSet = searchSectionManager.selectedFileSet;
    }

    public void setAdapterData(List<FileInfo> fileInfoList){

        fileInfoList = fileInfoList;

    }

    public void setImageLoader(ImageLoader mImageLoader){
        this.mImageLoader = mImageLoader;
    }


    public void setActionModeManager(ActionModeManager actionModeManager){

        this.actionModeManager = actionModeManager;
    }

   /* public void setSelectedFileSet(HashSet<FileInfo> selectedFileSet){

        this.selectedFileSet = selectedFileSet;
    }*/

    //当前view是否属于固定的item
    @Override
    public boolean isItemViewTypePinned(int viewType) {
        return viewType == GlobalConstants.SECTION_TYPE;
    }

    @Override
    public int getViewTypeCount() {
        return 11;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).sectionType;
    }

    @Override
    public int getCount() {
        return fileInfoList.size();
    }

    @Override
    public FileInfo getItem(int position) {
        return fileInfoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    public void notifyDataSetChanged(Boolean flag) {
        super.notifyDataSetChanged();
    }
    public void setScrollStatusFlag(int flag){
        mScrollStatusFlag = flag;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        View v;
        final ViewHolder viewHolder;
        SectionViewHolder sectionviewHolder;
        final  FileInfo fileinfo = fileInfoList.get(position);

       if(fileinfo.sectionType == GlobalConstants.SECTION_TYPE ){

            if(view == null) {
                v = View.inflate(context, R.layout.section_item_layout, null);
                sectionviewHolder = new SectionViewHolder(v);
                v.setTag(sectionviewHolder);
            }else{
                v = view;
                sectionviewHolder = (SectionViewHolder) view.getTag();
            }
            sectionviewHolder.sectionName.setText(fileinfo.fileName);
            updateSectionTitleNum(fileinfo,sectionviewHolder.sectioncount);

            return v;

        }else {

            if (view == null) {
                v = View.inflate(context, R.layout.sticklist_item_layout, null);
                viewHolder = new ViewHolder(v);
                v.setTag(viewHolder);
            } else {
                v = view;
                viewHolder = (ViewHolder) view.getTag();
            }


            final  String path = fileinfo.filePath;

            viewHolder.fileName.setText(fileinfo.spannableName);


            if (!fileinfo.isFile) {
                hideView(viewHolder.fileTime);
                showView(viewHolder.filecount);
                showView(viewHolder.rightArrow);
                showFolderExtraInfo(viewHolder,fileinfo);

            } else {
                hideView(viewHolder.rightArrow);
                hideView(viewHolder.filecount);
                showView(viewHolder.fileTime);
                viewHolder.fileSize.setText(Util.formatSize(context, fileinfo.fileSize));
                viewHolder.fileTime.setText(dataFormat.format(new Date(fileinfo.modifiedTime)));
                setFileIcon(viewHolder,fileinfo);
            }

           if (GlobalSearchResultFragment.mIsActionModeState) {
                hideView(viewHolder.rightArrow);
                showView(viewHolder.checkbox);
               if(selectedFileSet.contains(fileinfo)){
                   viewHolder.checkbox.setChecked(true);
               }else {
                   viewHolder.checkbox.setChecked(false);
               }
            } else {
                viewHolder.checkbox.setChecked(false);
                hideView(viewHolder.checkbox);
                if(!fileinfo.isFile)showView(viewHolder.rightArrow);
            }
            return v;

        }

    }


    private void showFolderExtraInfo(final ViewHolder holder, final FileInfo fileinfo) {
        holder.fileImage.setImageResource(R.drawable.ic_file_icon_folder);

        if(fileinfo.fileSize == -1 || fileinfo.childFileNum == -1) {
            holder.fileSize.setText(Util.formatSize(context, 0));
            holder.filecount.setText(0 + context.getResources().getQuantityString(R.plurals.title_item_count_format, 0));
        }else{
            holder.fileSize.setText(Util.formatSize(context, fileinfo.fileSize));
            holder.filecount.setText(fileinfo.childFileNum + context.getResources().getQuantityString(R.plurals.title_item_count_format, fileinfo.childFileNum));
            return;
        }

        //if(mScrollStatusFlag==0) {
            final String path = fileinfo.filePath;
            holder.fileSize.setTag(path);
            holder.filecount.setTag(path);
            GET_FOLDE_SIZE_EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    long fileSize = Util.getFileSize(context, fileinfo.getFile());
                    fileinfo.fileSize = fileSize;
                    int fileCount = Util.getChildNum(context, fileinfo);
                    fileinfo.childFileNum = fileCount;
                    FolderExtraInfo folderextrainfo = new FolderExtraInfo(holder, path, fileSize, fileCount);
                    mHandler.obtainMessage(MSG_UPDATE_FOLDER_INFO, folderextrainfo).sendToTarget();
                }
            });
       // }
    }
    private class FolderExtraInfo{
        public FolderExtraInfo(ViewHolder viewholder,String path,long size,int count){
            this.viewholder = viewholder;
            this.path = path;
            this.size = size;
            this.count = count;
        }
        public long size;
        public int count;
        // public long modifyTime;
        public ViewHolder viewholder;
        public String path;
    }


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_FOLDER_INFO:
                    FolderExtraInfo folderextrainfo = (FolderExtraInfo)msg.obj;
                    long fileSize = folderextrainfo.size;
                    int  fileCount = folderextrainfo.count;
                    String path = folderextrainfo.path;
                    ViewHolder viewholder = folderextrainfo.viewholder;
                    if (fileSize != -1 && viewholder.fileSize.getTag().equals(path)) {
                        viewholder.fileSize.setText(Util.formatSize(context, fileSize));
                    }
                    if (fileCount != -1 && viewholder.filecount.getTag().equals(path)) {
                        viewholder.filecount.setText(fileCount + context.getResources().getQuantityString(R.plurals.title_item_count_format, fileCount));
                    }

                    break;
            }
        }
    };


    protected void setFileIcon(final ViewHolder viewholder,FileInfo fileInfo){
       // int defaltIcon = Util.getDefaultIconRes(path);
        //viewholder.fileImage.setCenterImgShow(R.drawable.vediopreview,false);
        //viewholder.fileImage.setImageResource(defaltIcon);
        final String path = fileInfo.filePath;
        setFileDefaultIcon(fileInfo,viewholder.fileImage);
        viewholder.fileImage.setTag(path);
        if(fileInfo.sectionType == Category.Picture){
            mImageLoader.displayLocalThumbnail(context, path, path, viewholder.fileImage);
        }else if(fileInfo.sectionType == Category.Video){
            mImageLoader.displayVideoThumbnail(context,path, path, new ImageLoader.ImageProcessingCallback() {
                @Override
                public void onImageProcessing(WeakReference<Bitmap> weak, String tag) {
                    RoundAngleImageView imageView = viewholder.fileImage;
                    if( ! ((String)imageView.getTag()).equals(path)) {
                        return;
                    }
                    Bitmap bitmap = weak.get();
                    if(null == bitmap) return;
                    imageView.setCenterImgShow(R.drawable.vediopreview,true);
                    imageView.setImageBitmap(bitmap);
                }
            });
        }
    }


    private void updateSectionTitleNum(FileInfo fileinfo,View view){
        int count;
        String titleFormat;
        if(fileinfo.getPath().equals("picture_fileSection")) {
            count = mSearchSectionManager.picfilelists.size();
            titleFormat = context.getResources().getQuantityString(R.plurals.title_item_count_format, count);
            ((TextView)view).setText(count+titleFormat);
            return;
        }
        if(fileinfo.getPath().equals("music_fileSection")) {
            count = mSearchSectionManager.musicfilelist.size();
            titleFormat = context.getResources().getQuantityString(R.plurals.title_item_count_format, count);
            ((TextView)view).setText(count+titleFormat);
            return;
        }
        if(fileinfo.getPath().equals("vedio_fileSection")) {
            count = mSearchSectionManager.videofilelists.size();
            titleFormat = context.getResources().getQuantityString(R.plurals.title_item_count_format, count);
            ((TextView)view).setText(count+titleFormat);
            return;
        }
        if(fileinfo.getPath().equals("other_fileSection")) {
            count = mSearchSectionManager.otherfilelists.size();
            titleFormat = context.getResources().getQuantityString(R.plurals.title_item_count_format, count);
            ((TextView)view).setText(count+titleFormat);
            return;
        }

    }



    private void setFileDefaultIcon(FileInfo fileinfo,  ImageView imageView) {
        int defaultIconId = 0;
        String filePath = fileinfo.filePath;
        switch (fileinfo.sectionType) {
            case Category.Picture:
                defaultIconId = R.drawable.category_icon_picture;
                break;
            case Category.Music:
                defaultIconId = R.drawable.category_icon_music;
                break;
            case Category.Video:
                defaultIconId = R.drawable.category_icon_video;
                ((RoundAngleImageView)imageView).setCenterImgShow( R.drawable.vediopreview,false);
                break;
            case Category.Other:
                defaultIconId = Util.getDefaultIconRes(fileinfo.filePath);
                break;
            default:
                defaultIconId = R.drawable.unkown_icon;
        }
        if(imageView!=null)imageView.setImageResource(defaultIconId);
    }


  /*  private void setFileDefaultIcon(FileInfo fileinfo,  ImageView imageView) {
        int defaultIconId = 0;
        String filePath = fileinfo.filePath;
        switch (fileinfo.sectionType) {
            case Category.Picture:
                defaultIconId = R.drawable.category_icon_picture;
                break;
            case Category.Music:
                defaultIconId = R.drawable.category_icon_music;
                break;
            case Category.Video:
                defaultIconId = R.drawable.category_icon_video;
                break;
            case Category.Apk:
                defaultIconId = R.drawable.category_icon_apk;
                break;
            case Category.Zip:
                defaultIconId = R.drawable.category_icon_zip;
                break;
            case Category.Document:
                //defaultIconId = Util.getDefaultIconRes(fileinfo.getPath());
                if(Util.istTxtFile(filePath)) {
                    defaultIconId = R.drawable.text_icon;
                } else if(Util.istWordFile(filePath)) {
                    defaultIconId = R.drawable.word_icon;
                } else if(Util.istExcelFile(filePath)) {
                    defaultIconId = R.drawable.excel_icon;
                } else if(Util.isPPtFile(filePath)) {
                    defaultIconId = R.drawable.ppt_icon;
                } else if(Util.isPdfFile(filePath)) {
                    defaultIconId = R.drawable.pdf_icon;
                }else {
                    defaultIconId = R.drawable.category_icon_document;
                }
                break;
            default:
                defaultIconId = R.drawable.unkown_icon;
                break;
        }
        if(defaultIconId != 0) {
            if(fileinfo.sectionType==Category.Video){
                ((RoundAngleImageView)imageView).setCenterImgShow( R.drawable.vediopreview,false);
            }
            imageView.setImageResource(defaultIconId);
        }
    }*/


    protected void showView(View view){
        if(view==null){
            return;
        }
        if(view.getVisibility()!=View.VISIBLE){}
        view.setVisibility(View.VISIBLE);
    }

    protected void hideView(View v) {
        if(v == null) {
            return;
        }
        if(v.getVisibility() == View.VISIBLE) {
            v.setVisibility(View.GONE);
        }
    }

    protected void setText(TextView tv, String text) {
        if(tv == null) {
            return;
        }
        showView(tv);
        tv.setText(text);
    }


    public static class ViewHolder {
        RoundAngleImageView fileImage;
        TextView fileName;
        TextView fileSize;
        TextView fileTime;
        TextView filecount;
        CheckBox checkbox;
        ImageView rightArrow;
        ImageButton filemore;
        public ViewHolder(View v) {
            fileImage = (RoundAngleImageView) v.findViewById(R.id.file_image);
            fileName = (TextView) v.findViewById(R.id.file_name);
            fileSize = (TextView) v.findViewById(R.id.file_size);
            fileTime = (TextView) v.findViewById(R.id.file_time);
            filecount = (TextView) v.findViewById(R.id.file_count);
            checkbox = (CheckBox) v.findViewById(R.id.checkbox);
            rightArrow = (ImageView)v.findViewById(R.id.rightarrow);
           // filemore = (ImageButton) v.findViewById(R.id.file_more);
        }
    }

    // 存储一行中的控件（缓存作用）---避免多次强转每行的控件
    public static class SectionViewHolder {

        TextView sectionName;
        TextView sectioncount;

        public SectionViewHolder(View v) {
            sectionName = (TextView) v.findViewById(R.id.section_name);
            sectioncount = (TextView) v.findViewById(R.id.section_count);
            // filemore = (ImageButton) v.findViewById(R.id.file_more);
        }
    }


}

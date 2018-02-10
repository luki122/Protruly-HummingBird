package cn.com.protruly.filemanager.pathFileList;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Set;

import cn.com.protruly.filemanager.enums.Category;
import cn.com.protruly.filemanager.enums.FileInfo;
import cn.com.protruly.filemanager.globalsearch.RoundAngleImageView;
import cn.com.protruly.filemanager.utils.Util;
import cn.com.protruly.filemanager.imageloader.ImageLoader;
import cn.com.protruly.filemanager.R;

/**
 * Created by liushitao on 17-4-19.
 */

public abstract class BaseListAdapter extends BaseAdapter {
    protected Context mContext;
    protected List<FileInfo> mFileInfoList;
    protected Set<FileInfo> mSelectedFileSet;
    protected ImageLoader mImageLoader;
    protected boolean mIsActionModeState;

    public BaseListAdapter(Context context,List<FileInfo> fileInfoList) {
        mContext = context;
        mFileInfoList = fileInfoList;
        mImageLoader = ImageLoader.getInstance(context);
    }

    @Override
    public int getCount() {
        return mFileInfoList.size();
    }

    @Override
    public Object getItem(int position) {
        return mFileInfoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public void setSelectedFileSet(Set<FileInfo> selectedFileSet) {
        mSelectedFileSet = selectedFileSet;
    }

    public void setList(List<FileInfo> fileInfos) {
        mFileInfoList = fileInfos;
    }

    protected void setImageRes(ImageView img,int imgRes){
        if(img==null){
            return;
        }
        showView(img);
        img.setImageResource(imgRes);
    }

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

    protected void setFileIcon(FileInfo fileInfo, RoundAngleImageView imageView, final FileListAdapter.FileViewHolder fileViewHolder){
        setImageSize(imageView,"file");
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

    protected void setImageSize(ImageView view,String type){
        if(mContext==null){
            return ;
        }else if(mContext.getResources()==null){
            return ;
        }
        int size;
        size = mContext.getResources().getDimensionPixelSize(R.dimen.file_icon_list_size);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(size,size);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT,-1);
        view.setLayoutParams(layoutParams);
    }

    protected void setText(TextView tv, String text) {
        if(tv == null) {
            return;
        }
        showView(tv);
        tv.setText(text);
    }

    protected String getString(int str, Object formatArgs) {
        if(mContext==null){
            return "";
        }else if(mContext.getResources()==null){
            return "";
        }
        return mContext.getResources().getString(str, formatArgs);
    }

    public boolean setActionModeState(boolean isActionMode){
        return mIsActionModeState = isActionMode;
    }

    public boolean isActionModeState(){
        return mIsActionModeState;
    }
}

package cn.com.protruly.filemanager.categorylist;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;

import cn.com.protruly.filemanager.R;
import cn.com.protruly.filemanager.enums.Category;
import cn.com.protruly.filemanager.enums.FileInfo;
import cn.com.protruly.filemanager.imageloader.ImageLoader;
import cn.com.protruly.filemanager.operation.FileOperationUtil;
//import cn.com.protruly.filemanager.utils.DateTimeUtil;
import cn.com.protruly.filemanager.utils.FilePathUtil;
import cn.com.protruly.filemanager.utils.Util;
import cn.com.protruly.filemanager.ziplist.ZipListFragment;

/**
 * Created by sqf on 17-4-27.
 */

public class CategoryAdapter extends SimpleCursorAdapter {

    protected Context mContext;
    protected int mCategoryType;
    private ImageLoader mImageLoader;
    private int mLayoutId;
    private int mSortOrder;
    //private PinyinSortedCursor mSortedCursor;
    private SelectionManager mSelectionManager;

    private static final String TAG = "CategoryAdapter";

    public CategoryAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        mContext = context;
        mLayoutId = layout;
        //mImageLoader = ImageLoader.getInstance(context);
    }

    public void setImageLoader(ImageLoader imageLoader) {
        mImageLoader = imageLoader;
    }

    public void setSelectionManager(SelectionManager selectionManager) {
        mSelectionManager = selectionManager;
    }

    protected void setSortOrder(int sortOrder) {
        mSortOrder = sortOrder;
    }

    public void setCategoryType(int type) {
        mCategoryType = type;
    }

    public int getCategoryType() {
        return mCategoryType;
    }

    @Override
    public Cursor swapCursor(Cursor c) {
        if(mSelectionManager != null) {
            mSelectionManager.setAllSize(null != c ? c.getCount() : 0);
        }
        return super.swapCursor(c);
    }

    public void notifyScrollingStarted() {

    }

    public void notifyScrollingStopped() {

    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        ViewHolder viewHolder = new ViewHolder();
        View ret = null;
        switch(mCategoryType) {
            case Category.Picture:
                ret = LayoutInflater.from(mContext).inflate(R.layout.category_list_item_slider_view_for_pic, null);
                break;
            case Category.Video:
                ret = LayoutInflater.from(mContext).inflate(R.layout.category_list_item_slider_view_for_video, null);
                break;
            default:
                ret = LayoutInflater.from(mContext).inflate(R.layout.category_list_item_slider_view, null);
                break;
        }
        viewHolder.mCheckBox = (CheckBox) ret.findViewById(android.R.id.checkbox);
        viewHolder.iconImageView = (ImageView)ret.findViewById(android.R.id.icon);
        viewHolder.fileNameTextView = (TextView)ret.findViewById(R.id.file_name);
        viewHolder.dateTextView = (TextView)ret.findViewById(R.id.file_date);
        viewHolder.sizeTextView = (TextView)ret.findViewById(R.id.file_size);
        if(mCategoryType == Category.Video) {
            viewHolder.maskImageView = (ImageView)ret.findViewById(R.id.mask);
        }
        ret.setTag(viewHolder);
        return ret;
    }

    @Override
    public void bindView(View view, Context context, final Cursor cursor) {
        int id = cursor.getInt(CategoryFactory.COLUMN_INDEX_ID);
        String displayName = cursor.getString(CategoryFactory.COLUMN_INDEX_DISPLAY_NAME);
        //String title = ref.getString(CategoryFactory.COLUMN_INDEX_TITLE);
        String mimeType = cursor.getString(CategoryFactory.COLUMN_INDEX_MIME_TYPE);
        long time = cursor.getInt(CategoryFactory.COLUMN_INDEX_DISPLAY_DATETIME);
        String dateTime = Util.formatDateStringThird(time * 1000);
        String fileSize = String.valueOf(Util.getFileSizeAndUnit(cursor.getLong(CategoryFactory.COLUMN_INDEX_DISPLAY_SIZE), 2));
        //LogUtil.i(TAG, "AAA id:" + id + " fileName:" + fileName);
        final String filePath = cursor.getString(CategoryFactory.COLUMN_INDEX_DISPLAY_DATA);//file path
        String fileName = "";
        switch(mCategoryType) {
            case Category.Apk:
            case Category.Zip:
            case Category.Document:
                fileName = FilePathUtil.getFileNameAndExtension(filePath);
                break;
            default:
                fileName = TextUtils.isEmpty(displayName) ? FilePathUtil.getFileNameAndExtension(filePath) : displayName;
                break;
        }
        FilePathUtil.getFileNameAndExtension(filePath);
        final ViewHolder viewHolder = (ViewHolder) view.getTag();
        int checkBoxVisibility = mSelectionManager.isInSelectionMode() ? View.VISIBLE : View.GONE;
        viewHolder.mCheckBox.setVisibility(checkBoxVisibility);
        if(mSelectionManager.isSelected(filePath)) {
            viewHolder.mCheckBox.setChecked(true);
        } else {
            viewHolder.mCheckBox.setChecked(false);
        }
        viewHolder.iconImageView.setTag(filePath);//set tag to avoid setting when tag is changed.
        setDefaultIcon(viewHolder.iconImageView, filePath);
        switch (mCategoryType) {
            case Category.Picture:
                mImageLoader.displayLocalThumbnail(context, filePath, filePath, viewHolder.iconImageView);
                break;
            case Category.Video:
                //mImageLoader.displayVideoThumbnail(context, filePath, filePath, viewHolder.iconImageView);
                viewHolder.maskImageView.setVisibility(View.GONE);
                mImageLoader.displayVideoThumbnail(context,filePath, filePath, new ImageLoader.ImageProcessingCallback() {
                    @Override
                    public void onImageProcessing(WeakReference<Bitmap> weak, String tag) {
                        ImageView imageView = viewHolder.iconImageView;
                        if( ! ((String)imageView.getTag()).equals(filePath)) {
                            return;
                        }
                        Bitmap bitmap = weak.get();
                        if(null == bitmap) return;
                        if(null != bitmap) {
                            imageView.setImageBitmap(bitmap);
                            viewHolder.maskImageView.setVisibility(View.VISIBLE);
                        }
                    }
                });
                break;
            default:
                break;
        }
        viewHolder.fileNameTextView.setText(fileName);//file name
        viewHolder.dateTextView.setText(dateTime);// 2 for date
        viewHolder.sizeTextView.setText(fileSize);// 3 for size
    }

    private void setDefaultIcon(ImageView imageView, String filePath) {
        int defaultIconId = 0;
        switch (mCategoryType) {
            case Category.Picture:
                defaultIconId = R.drawable.ic_default_pic;
                break;
            case Category.Music:
                defaultIconId = R.drawable.category_icon_music;
                break;
            case Category.Video:
                defaultIconId = R.drawable.ic_default_video;
                break;
            case Category.Apk:
                defaultIconId = R.drawable.category_icon_apk;
                break;
            case Category.Zip:
                defaultIconId = R.drawable.category_icon_zip;
                break;
            case Category.Document:
                if(Util.istTxtFile(filePath)) {
                    defaultIconId = R.drawable.text_icon;
                } else if(Util.istWordFile(filePath)) {
                    defaultIconId = R.drawable.word_icon;
                } else if(Util.istExcelFile(filePath)) {
                    defaultIconId = R.drawable.excel_icon;
                } else if(Util.isPPtFile(filePath)) {
                    defaultIconId = R.drawable.ppt_icon;
                }else if(Util.isPdfFile(filePath)) {
                    defaultIconId = R.drawable.pdf_icon;
                }else {
                    defaultIconId = R.drawable.category_icon_document;
                }
                break;
            default:
            break;
        }
        if(defaultIconId != 0) {
            imageView.setImageResource(defaultIconId);
        }
    }

    private class ViewHolder {
        public CheckBox mCheckBox;
        public ImageView iconImageView;
        public TextView fileNameTextView;
        public TextView dateTextView;
        public TextView sizeTextView;
        public ImageView maskImageView;
    }

    public ArrayList<FileInfo> getSelectedData() {
        final Cursor ref = getCursor();
        ArrayList<FileInfo> fileInfos = new ArrayList<FileInfo>();
        int addedCount = 0;
        int selectedCount = mSelectionManager.getSelectedCount();
        for(int i = 0; i < getCount(); i ++) {
            ref.moveToPosition(i);
            final String filePath = ref.getString(CategoryFactory.COLUMN_INDEX_DISPLAY_DATA);
            if(mSelectionManager.isSelected(filePath)) {
                FileInfo fileInfo = new FileInfo(filePath);
                fileInfos.add(fileInfo);
                ++ addedCount;
                if(addedCount == selectedCount) break;
            }
        }
        return fileInfos;
    }

    public HashSet<FileInfo> getSelectedFiles() {
        final Cursor ref = getCursor();
        HashSet<FileInfo> filePaths = new HashSet<FileInfo>();
        int addedCount = 0;
        int selectedCount = mSelectionManager.getSelectedCount();
        for(int i = 0; i < getCount(); i ++) {
            ref.moveToPosition(i);
            final String filePath = ref.getString(CategoryFactory.COLUMN_INDEX_DISPLAY_DATA);
            if(mSelectionManager.isSelected(filePath)) {
                FileInfo fileInfo = new FileInfo(filePath);
                filePaths.add(fileInfo);
                ++ addedCount;
                if(addedCount == selectedCount) break;
            }
        }
        return filePaths;
    }
}

package cn.com.protruly.filemanager.categorylist;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.com.protruly.filemanager.enums.Category;
import cn.com.protruly.filemanager.imageloader.ImageLoader;
import cn.com.protruly.filemanager.operation.FileOperationUtil;

import cn.com.protruly.filemanager.R;
import cn.com.protruly.filemanager.utils.FilePathUtil;
import cn.com.protruly.filemanager.utils.Util;
import cn.com.protruly.filemanager.ziplist.ZipListFragment;

/**
 * Created by sqf on 17-5-8.
 */

public class CategorySearchResultAdapter extends SimpleCursorAdapter {

    protected Context mContext;
    protected int mCategoryType;
    private ImageLoader mImageLoader;
    private int mLayoutId;
    private String mKeyword;

    public CategorySearchResultAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        mContext = context;
        mLayoutId = layout;
        mImageLoader = ImageLoader.getInstance(context);
    }

    public void setCategoryType(int type) {
        mCategoryType = type;
    }

    public void setSearchKeyword(String keyword) {
        mKeyword = keyword;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        ViewHolder viewHolder;
        LayoutInflater inflater = LayoutInflater.from(context);

        View ret = null;
        switch(mCategoryType) {
            case Category.Picture:
                ret = LayoutInflater.from(mContext).inflate(R.layout.category_search_result_list_item_for_pic, null);
                break;
            case Category.Video:
                ret = LayoutInflater.from(mContext).inflate(R.layout.category_search_result_list_item_for_video, null);
                break;
            default:
                ret = LayoutInflater.from(mContext).inflate(R.layout.category_search_result_list_item_normal, null);
                break;
        }
        //View convertView = inflater.inflate(mLayoutId, null);
        viewHolder = new ViewHolder();
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
        final ViewHolder viewHolder = (ViewHolder) view.getTag();
        /*
        String filePath = cursor.getString(4);
        viewHolder.iconImageView.setTag(filePath);//set tag to avoid setting when tag is changed.
        setDefaultIcon(viewHolder.iconImageView);
        mImageLoader.displayLocalThumbnail(context, filePath, filePath, viewHolder.iconImageView);
        viewHolder.fileNameTextView.setText(cursor.getString(1));// 1 for file name
        viewHolder.dateTextView.setText(Util.formatDateStringThird(cursor.getInt(2)));// 2 for date
        viewHolder.sizeTextView.setText(String.valueOf(cursor.getInt(3)));// 3 for size
        */
        final String filePath = cursor.getString(CategoryFactory.COLUMN_INDEX_DISPLAY_DATA);//file path
        /*
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCategoryType != Category.Zip) {
                    FileOperationUtil.viewFile(mContext, filePath);
                } else {
                    ZipListFragment.openMe((Activity) mContext, filePath);
                }
            }
        });
        */

        viewHolder.iconImageView.setTag(filePath);//set tag to avoid setting when tag is changed.
        setDefaultIcon(viewHolder.iconImageView);
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
        int id = cursor.getInt(CategoryFactory.COLUMN_INDEX_ID);
        String displayName = cursor.getString(CategoryFactory.COLUMN_INDEX_DISPLAY_NAME);
        //String title = ref.getString(CategoryFactory.COLUMN_INDEX_TITLE);
        String mimeType = cursor.getString(CategoryFactory.COLUMN_INDEX_MIME_TYPE);
        String dateTime = Util.formatDateStringThird(cursor.getInt(CategoryFactory.COLUMN_INDEX_DISPLAY_DATETIME));
        String fileSize = String.valueOf(Util.getFileSizeAndUnit(cursor.getLong(CategoryFactory.COLUMN_INDEX_DISPLAY_SIZE), 2));
        //LogUtil.i(TAG, "AAA id:" + id + " fileName:" + fileName);
        //String fileName = TextUtils.isEmpty(displayName) ? FilePathUtil.getFileNameAndExtension(filePath) : displayName;
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

        SpannableString spannableString = getSpannable(fileName, mKeyword);
        if(spannableString == null) {
            viewHolder.fileNameTextView.setText(fileName);//file name
        } else {
            viewHolder.fileNameTextView.setText(spannableString);
        }
        viewHolder.dateTextView.setText(dateTime);// 2 for date
        viewHolder.sizeTextView.setText(fileSize);// 3 for size
    }

    private class ViewHolder {
        public ImageView iconImageView;
        public TextView fileNameTextView;
        public TextView dateTextView;
        public TextView sizeTextView;
        public ImageView maskImageView;
    }

    private void setDefaultIcon(ImageView imageView) {
        int defaultIconId = 0;
        switch (mCategoryType) {
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
                defaultIconId = R.drawable.category_icon_document;
                break;
            default:
                break;
        }
        if(defaultIconId != 0) {
            imageView.setImageResource(defaultIconId);
        }
    }


    private SpannableString getSpannable(String name, String keyword) {
        if(TextUtils.isEmpty(name) || TextUtils.isEmpty(keyword)) return null;
        SpannableString s = new SpannableString(name);
        //Pattern p = Pattern.compile(keyword,Pattern.CASE_INSENSITIVE);
        Pattern p = Pattern.compile(keyword);
        if(p == null) return null;
        Matcher m = p.matcher(s);
        if (!m.find()) return null;
        int start = m.start();
        int end = m.end();
        s.setSpan(new ForegroundColorSpan(Color.RED), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return s;
    }

}

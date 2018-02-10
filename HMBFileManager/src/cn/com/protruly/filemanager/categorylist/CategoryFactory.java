package cn.com.protruly.filemanager.categorylist;

import cn.com.protruly.filemanager.CategoryActivity;
import cn.com.protruly.filemanager.enums.Category;

import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;

import cn.com.protruly.filemanager.utils.GlobalConstants;
import cn.com.protruly.filemanager.utils.MediaFileUtil;
import cn.com.protruly.filemanager.R;
import cn.com.protruly.filemanager.enums.SortOrder;

import android.provider.MediaStore.Files.FileColumns;
import android.provider.MediaStore.Video.VideoColumns;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;


/**
 * Created by sqf on 17-4-28.
 */

public class CategoryFactory {

    private static CategoryFactory sFactory;

    public static final int COLUMN_INDEX_ID = 0;
    public static final int COLUMN_INDEX_DISPLAY_NAME = 1;
    public static final int COLUMN_INDEX_DISPLAY_DATETIME = 2;
    public static final int COLUMN_INDEX_DISPLAY_SIZE = 3;
    public static final int COLUMN_INDEX_DISPLAY_DATA = 4;
    public static final int COLUMN_INDEX_TITLE = 5;
    public static final int COLUMN_INDEX_MIME_TYPE = 6;

    public static final String FILES_COLUMN_FILE_NAME = "title";//"file_name";//title

    public CategoryFactory() {
    }

    public static CategoryFactory instance() {
        if(null == sFactory) {
            sFactory = new CategoryFactory();
        }
        return sFactory;
    }

    public CategoryAdapter get(Context context, int type) {
        return new CategoryAdapter(context, getLayout(type),
                null, getProjections(type),
                null, 0);
        /*
        switch (type) {
            case Category.Document:
                break;
            case Category.Video:
                break;
            case Category.Zip:
                break;
            case Category.Apk:
                break;
            case Category.Picture:
                break;
            case Category.Music:
                break;
        }
        return null;
        */
    }

    public CategorySearchResultAdapter getSearchResultAdapter(Context context, int type) {
        return new CategorySearchResultAdapter(context, getLayout(type),
                null, getProjections(type),
                null, 0);
    }

    public Uri getCategoryUri(int type) {
        switch(type) {
            case Category.Document:
            case Category.Zip:
            case Category.Apk:
                return GlobalConstants.FILES_URI;
            case Category.Video:
                return GlobalConstants.VIDEO_URI;
            case Category.Picture:
                return GlobalConstants.PICTURE_URI;
            case Category.Music:
                return GlobalConstants.MUSIC_URI;
            default:
                break;
        }
        return null;
    }

    public String[] getProjections(int type) {
        switch(type) {
            case Category.Zip:
            case Category.Apk:
            case Category.Document:
                return new String[] {
                        MediaStore.Files.FileColumns._ID,
                        //MediaColumns.DISPLAY_NAME,
                        FILES_COLUMN_FILE_NAME,
                        FileColumns.DATE_MODIFIED,
                        FileColumns.SIZE,
                        FileColumns.DATA,
                        MediaColumns.TITLE,
                        MediaColumns.MIME_TYPE,
                };
            case Category.Video:
                return new String[] {
                        MediaStore.Video.Media._ID,
                        MediaColumns.DISPLAY_NAME,
                        MediaStore.Video.Media.DATE_MODIFIED,
                        MediaStore.Video.Media.SIZE,
                        MediaStore.Video.Media.DATA,
                        MediaColumns.TITLE,
                        MediaColumns.MIME_TYPE,
                };

            case Category.Picture:
                return new String[] {
                        MediaStore.Images.Media._ID,
                        MediaColumns.DISPLAY_NAME,
                        MediaStore.Images.Media.DATE_MODIFIED,
                        MediaStore.Images.Media.SIZE,
                        MediaStore.Images.Media.DATA,
                        MediaColumns.TITLE,
                        MediaColumns.MIME_TYPE,
                };
            case Category.Music:
                return new String[] {
                        MediaStore.Audio.Media._ID,
                        MediaColumns.DISPLAY_NAME,
                        MediaStore.Audio.Media.DATE_MODIFIED,
                        MediaStore.Audio.Media.SIZE,
                        MediaStore.Audio.Media.DATA,
                        MediaColumns.TITLE,
                        MediaColumns.MIME_TYPE,
                };
            default:
                break;
        }
        return null;
    }

    public String getSearchWhereClause(Context context, int type, String key) {
        key = key.replaceAll("'", "''");
        String whereClause = getWhereClause(context, type);
        if(type == Category.Apk ||
                type == Category.Document ||
                type == Category.Zip) {
            whereClause += " AND " + FILES_COLUMN_FILE_NAME + " like '%" + key + "%' ";
        } else {
            whereClause += " AND " + MediaStore.MediaColumns.DISPLAY_NAME + " like '%" + key + "%' ";
        }
        return whereClause;
    }

    public String getWhereClause(Context context, int type) {
        String where = MediaFileUtil.getTypeSet(type);
        Log.d("hid","(CategoryActivity)context).getHidenStatus():"+((CategoryActivity)context).getHidenStatus());
        if(((CategoryActivity)context).getHidenStatus()) {
            where += " AND (_data not like \'%/.%\') ";
        }
        Log.d("hid","where"+where);
        return where;
        /*
        switch(type) {
            case Category.Document:
                where = MediaFileUtil.getTypeSet(Category.Document);
            case Category.Video:
                return MediaFileUtil.getTypeSet(Category.Video);
            case Category.Zip:
                return MediaFileUtil.getTypeSet(Category.Zip);
            case Category.Apk:
                return MediaFileUtil.getTypeSet(Category.Apk);
            case Category.Picture:
                return MediaFileUtil.getTypeSet(Category.Picture);
            case Category.Music:
                return MediaFileUtil.getTypeSet(Category.Music);
            default:
                break;
        }
        */
    }

    public String getSortOrderDescription(int categoryType, int sortOrder) {
        switch (sortOrder) {
            case SortOrder.SORT_ORDER_NAME:
                if(categoryType == Category.Zip ||
                        categoryType == Category.Apk ||
                        categoryType == Category.Document) {
                    return MediaColumns.TITLE + " ASC "; //COLLATE LOCALIZED
                } else {
                    return MediaColumns.DISPLAY_NAME + " ASC "; //COLLATE LOCALIZED
                }
            case SortOrder.SORT_ORDER_TIME:
                if(Category.Video == categoryType) {
                    return VideoColumns.DATE_TAKEN + " DESC ";
                } else if(Category.Picture == categoryType) {
                    return ImageColumns.DATE_TAKEN + " DESC ";
                }
                return MediaColumns.DATE_ADDED + " DESC ";
            case SortOrder.SORT_ORDER_SIZE_ASC:
                return MediaColumns.SIZE + " ASC ";
            case SortOrder.SORT_ORDER_SIZE_DSC:
                return MediaColumns.SIZE + " DESC ";
        }
        return null;
    }

    public int getLayout(int type) {
        switch(type) {
            case Category.Document:
            case Category.Video:
            case Category.Zip:
            case Category.Apk:
            case Category.Picture:
            case Category.Music:
                //return R.layout.category_list_item;
                return R.layout.category_search_result_list_item;
            default:
                break;
        }
        return 0;
    }

}

package cn.com.protruly.filemanager.enums;
import cn.com.protruly.filemanager.R;

/**
 * Created by liushitao on 17-4-17.
 */

public class Category {

    public static final String TAG = "Category";

    public static final int Document = 0;
    public static final int Video = 1;
    public static final int Zip = 2;
    public static final int Apk = 3;
    public static final int Picture = 4;
    public static final int Music = 5;
    public static final int History = 6;
    public static final int Other = 7;
    public static final int System = 8;
    public static final int Default = 9;


    public static final int [] TITLE_ID = {
            R.string.category_document,
            R.string.category_video,
            R.string.category_zip,
            R.string.category_apk,
            R.string.category_picture,
            R.string.category_music,
    } ;
}

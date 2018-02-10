package com.android.dlauncher.badge;

import android.net.Uri;

/**
 * Created by lijun on 17-7-27.
 */

public class Badge {

    public static final String TABLE_NAME = "badge";

    /*Data Field*/
    public static final String ID = "id";
    public static final String PACKAGE_NAME = "package_name";
    public static final String SHORTCUT_CUSTOM_ID = "shortcutCustomId";
    public static final String COUNT = "count";
    public static final String APP_SHORTCUT_CREATOR = "app_shortcut_creator";
    public static final String LAST_MODIFY = "last_modify";

    /*Default sort order*/
    public static final String DEFAULT_SORT_ORDER = "id asc";

    /*Call Method*/
    public static final String METHOD_GET_ITEM_COUNT = "METHOD_GET_ITEM_COUNT";
    public static final String KEY_ITEM_COUNT = "KEY_ITEM_COUNT";

    /*Authority*/
    public static final String AUTHORITY = "com.android.dlauncher.badge";

    /*Content URI*/
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/badge");

    public static String DEFAULT_SHORTCUT_CUSTOM_ID = "";
}

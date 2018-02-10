package cn.com.protruly.filemanager.operation;

/**
 * Created by liushitao on 17-5-12.
 */

public class OperationType {

    public static final int COPY = 0;
    public static final int CUT = 1;
    public static final int RENAME = 2;
    public static final int CREATE_FOLDER = 3;
    public static final int ZIP = 4;
    public static final int UNZIP = 5;
    public static final int DELETE = 6;
    public static final int DELETE_UPDATEPROGRESS = 7;
    public static final int DELETE_EXCEPTION = 8;

    public static final int COPY_SUCCEED = 9;
    public static final int COPY_FAIL= 10;
    public static final int CUT_SUCCEED= 11;
    public static final int CUT_FAIL= 12;
    public static final int SET_PROGRESS_MAX= 13;

    public static final int SEND = 14;
    public static final int DETAIL = 15;

    public static final int UNZIP_ENTRY = 16;
    public static final int UNZIP_WHOLE = 17;
}

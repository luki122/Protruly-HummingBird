package cn.com.protruly.filemanager.operation;

import java.util.HashSet;

import cn.com.protruly.filemanager.R;
import cn.com.protruly.filemanager.enums.FileInfo;

/**
 * Created by sqf on 17-5-25.
 */

public class FileOperationResult {

    public static final int FORC_SUCCEEDED = 0;
    public static final int FORC_INSUFFICIENT_SPACE = 1;
    public static final int FORC_USER_CANCELLED = 2;
    public static final int FORC_ILLEGAL_ARGS = 3;
    public static final int FORC_NOT_A_FILE = 4;
    public static final int FORC_FILE_NOT_EXISTS = 5;
    public static final int FORC_CREATE_NEW_FILE_ERROR = 6;
    public static final int FORC_CREATE_NEW_DIRECTORY_ERROR = 7;
    public static final int FORC_EMPTY_ZIP_FILE = 8;
    public static final int FORC_OPEN_ZIP_FILE_FAILED = 9;
    public static final int FORC_COPY_OR_MOVE_INTO_SUBDIRECTORY_NOT_ALLOWED = 10;
    public static final int FORC_RENAME_TO_FAILED_WHEN_MOVING = 11;
    public static final int FORC_FILE_ZERO_BYTE = 12;
    public static final int FORC_UNKNOWN_ERROR = 13;


    //MUST have same sequence as FORC_CONSTANTS.
    private static final int [] RESULT_DESCRIPTION = {
            R.string.forc_succeeded,
            R.string.forc_insufficient_space,
            R.string.forc_user_cancelled,
            R.string.forc_illegal_args,
            R.string.forc_not_a_file,
            R.string.forc_file_not_exists,
            R.string.forc_create_new_file_error,
            R.string.forc_create_new_directory_error,
            R.string.forc_empty_zip_file,
            R.string.forc_open_zip_file_failed,
            R.string.forc_copy_or_move_into_child_direcotry_not_allowed,
            R.string.forc_rename_to_failed_when_moving,
            R.string.forc_file_zero_byte,
            R.string.forc_unknown_error,
    };

    public int resultCode;
    public String realDestinationPath;

    public FileOperationResult set(int resultCode, String realDestinationPath) {
        this.resultCode = resultCode;
        this.realDestinationPath = realDestinationPath;
        return this;
    }

    public boolean isSucceeded() {
        return FORC_SUCCEEDED == resultCode;
    }

    public static int getDescription(int code) {
        return RESULT_DESCRIPTION[code];
    }

    public int getDescription() {
        return RESULT_DESCRIPTION[resultCode];
    }
}

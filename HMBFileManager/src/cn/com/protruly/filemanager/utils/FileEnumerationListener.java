package cn.com.protruly.filemanager.utils;

import java.io.File;

/**
 * Created by sqf on 17-7-17.
 */

public interface FileEnumerationListener {
    void onFileEnumerated(File file);
}

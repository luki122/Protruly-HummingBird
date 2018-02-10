package cn.com.protruly.filemanager.utils;

import android.util.Log;

import java.io.File;
import java.io.FileFilter;

/**
 * Created by liushitao on 17-5-12.
 */

public class HiddenFileFilter implements FileFilter{
    @Override
    public boolean accept(File pathname) {
        /*if(pathname != null && pathname.isDirectory() && pathname.list()==null){
            return false;//////pathname.isHidden()
        }*/
        return pathname!=null && !pathname.isHidden();
    }
}

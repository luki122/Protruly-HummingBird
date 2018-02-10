package cn.com.protruly.filemanager.categorylist;

/**
 * Created by sqf on 17-4-26.
 */

public interface HmbFileObserverListener {
    public void onFileCreated(String path);
    public void onFileDeleted(String path);
    public void onFileModified(String path);
    public void onFileRenamed(String path);
}
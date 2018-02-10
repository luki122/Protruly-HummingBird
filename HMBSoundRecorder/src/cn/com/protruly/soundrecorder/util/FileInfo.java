package cn.com.protruly.soundrecorder.util;

import java.io.File;

/**
 * Created by sqf on 17-9-20.
 */

public class FileInfo {

    protected File file;
    protected String name;
    protected String path;
    protected long modifiedTime;
    protected long size;

    public FileInfo(File file) {
        this.file = file;
        this.name = file.getName();
        this.path = file.getPath();
        this.modifiedTime = file.lastModified();
        if(file.isFile()){
            this.size = file.length();
        }
    }


    public File getFile(){
        if(file==null)return null;
        return file;
    }

    public String getName(){
        if(file==null)return null;
        return this.name;
    }

    public String getPath(){
        if(file==null)return null;
        return this.path;
    }

    public Boolean isFile(){
        if(file==null)return false;
        return file.isFile();
    }

    public Boolean isDirectory(){
        if(file==null)return false;
        return file.isDirectory();
    }

    public String getParent(){
        if(file==null)return null;
        return file.getParent();
    }

    public Boolean renameTo(File dest){
        if(file==null)return false;
        return file.renameTo(dest);
    }

    public long getModifiedTime(){
        if(file==null)return -1;
        return modifiedTime;
    }
}

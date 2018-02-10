/**
  * Generated by smali2java 1.0.0.558
  * Copyright (C) 2013 Hensence.com
  */

package cn.com.protruly.filemanager.utils;


public class MediaFileType {
    public int mFileType=0;
    public String mMimeType=null;
    public Integer mIconRes = 0;
    
    MediaFileType(int fileType, String mimeType) {
        mFileType = fileType;
        mMimeType = mimeType;
    }
    
    MediaFileType(int fileType, String mimeType, Integer iconRes) {
        mFileType = fileType;
        mMimeType = mimeType;
        mIconRes = iconRes;
    }
}
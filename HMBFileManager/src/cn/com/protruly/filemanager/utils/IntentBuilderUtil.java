package cn.com.protruly.filemanager.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import cn.com.protruly.filemanager.enums.FileInfo;

/**
 * Created by liushitao on 17-4-27.
 */

public class IntentBuilderUtil {

    public static Intent buildIntent(String path,Context context) {
        String mimeType = MediaFileUtil.getMimeTypeForFile(path);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (GlobalConstants.isSupportFileProviderForSdkN) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        if(mimeType != null) {
            intent.setDataAndType(Util.getUriForFile(context,new File(path)), mimeType);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }

    public static String buildType(int type){
        switch (type){
            case 0:return "text/plain";
            case 1:return "audio/*";
            case 2:return "video/*";
            case 3:return "image/*";
            default:return "*/*";
        }
    }

    public static Intent buildShareIntent(Context context, HashSet<FileInfo> infoHashSet){
        MediaDatabaseDao mediaDatabaseDao = new MediaDatabaseDao(context);
        HashSet<String> fileTypeSet = new HashSet<>();
        ArrayList<Uri> fileUriList = new ArrayList<>();
        String mimeType = "";
        String fileType = "";
        Uri uri;
        for(FileInfo fileInfo:infoHashSet){
            if(MediaFileUtil.isMimeTypeMedia(MediaFileUtil.getMimeTypeForFile(fileInfo.fileName))){
                uri = mediaDatabaseDao.getContentUriFromPath(fileInfo.filePath,Util.getCategory(fileInfo.getFile()));
                Log.d("bql","uri c:"+uri);
            }else{
                uri = Util.getUriForFile(context,new File(fileInfo.getPath()));
                Log.d("bql","uri f:"+uri);
            }
            fileUriList.add(uri);
            mimeType = MediaFileUtil.getMimeTypeForFile(fileInfo.fileName);
            if(mimeType != null){
                int index = mimeType.indexOf("/");
                if(index != -1){
                    fileType = mimeType.substring(0,index).toLowerCase();
                    fileTypeSet.add(fileType);
                }
            }
            fileType = "*";
            fileTypeSet.add(fileType);
        }
        if (fileUriList.size() == 0){
            return null;
        }
        Intent intent;
        if(fileUriList.size()>1){
            intent = new Intent("android.intent.action.SEND_MULTIPLE");
        }else{
            intent = new Intent("android.intent.action.SEND");
        }
        if(fileUriList.size()>1){
            if(fileTypeSet.size()>1){
                intent.setType("*/*");
            }else{
                intent.setType(fileType+"/*");
            }
            intent.putParcelableArrayListExtra("android.intent.extra.STREAM",fileUriList);
            return intent;
        }

        if(mimeType == null){
            mimeType = "*/*";
        }
        intent.setType(mimeType);
        intent.putExtra("android.intent.extra.STREAM",(Parcelable) fileUriList.get(0));
        //if (GlobalConstants.isSupportFileProviderForSdkN && Build.VERSION.SDK_INT >= 24) {
            //intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        //}
        return intent;
    }
}

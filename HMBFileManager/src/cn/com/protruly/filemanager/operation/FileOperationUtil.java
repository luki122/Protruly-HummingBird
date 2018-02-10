package cn.com.protruly.filemanager.operation;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.com.protruly.filemanager.enums.FileInfo;
import cn.com.protruly.filemanager.pathFileList.PathListFragment;
import cn.com.protruly.filemanager.ui.OpenTypeDialogCreator;
import cn.com.protruly.filemanager.utils.GlobalConstants;
import cn.com.protruly.filemanager.utils.IntentBuilderUtil;
import cn.com.protruly.filemanager.utils.LogUtil;
import cn.com.protruly.filemanager.utils.MediaDatabaseDao;
import cn.com.protruly.filemanager.utils.MediaFileType;
import cn.com.protruly.filemanager.utils.MediaFileUtil;
import cn.com.protruly.filemanager.utils.Util;
import cn.com.protruly.filemanager.R;
import cn.com.protruly.filemanager.ziplist.ZipListFragment;

/**
 * Created by sqf on 17-5-16.
 * Convention in this file:
 * Single file means a file, such as an image file, a video file
 */

public class FileOperationUtil {

    private static final String TAG = "FileOperationUtil";

    public static void viewFile(Context context, String file) {
        try {
            File f = new File(file);
            if(!f.exists()) {
                FileDbManager dbManager = new FileDbManager(context);
                dbManager.addToDeleteList(file);
                dbManager.deleteFromDeleteList();
                Util.showToast(context, R.string.file_no_found);
                return;
            }

            Intent intent = IntentBuilderUtil.buildIntent(file,context);
            String type = intent.getType();
            LogUtil.d(TAG, "type:" + type);
            if (!TextUtils.isEmpty(type)) {
                LogUtil.d("teee", "viewFile  context.startActivity(intent); !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                context.startActivity(intent);
                return;
            }
            OpenTypeDialogCreator creator = new OpenTypeDialogCreator(context, intent, file);
            creator.show();
        } catch (ActivityNotFoundException exception) {
            Util.showToast(context, R.string.no_app_to_open_file);
        }
    }
    public static void viewFileForNoBack(Context context, String file) {
        LogUtil.d("teee", "viewFile  context.viewFileForNoBack(intent); !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        if(file==null)return;
        if(file.startsWith("/mnt/")){
            Toast.makeText(context,context.getString(R.string.unknowfiles),Toast.LENGTH_SHORT).show();
            ((Activity)context).finish();
            return;
        }
        try {
            FileInfo info = new FileInfo(file);
            if(info.isDirectory()){
                startNewPathListFragment(context,info);
            }else {
                Intent intent = IntentBuilderUtil.buildIntent(file,context);
                String type = intent.getType();
                LogUtil.d(TAG, "type:" + type);
                if(type!=null && type.equals("application/zip")){
                    ZipListFragment.openMeForNoBack((Activity)context,file);
                }else {
                    if (!TextUtils.isEmpty(type)) {
                        context.startActivity(intent);
                        ((Activity)context).finish();
                        return;
                    }
                    Toast.makeText(context,context.getString(R.string.unknowfiles),Toast.LENGTH_SHORT).show();
                   // OpenTypeDialogCreator creator = new OpenTypeDialogCreator(context, intent, file);
                   // creator.show();
                    ((Activity)context).finish();
                }
            }
        } catch (ActivityNotFoundException exception) {
            Util.showToast(context, R.string.no_app_to_open_file);
        }
    }

    private static void startNewPathListFragment(Context context,FileInfo p1) {
        if(context==null)return;
        FragmentTransaction transaction = ((Activity)context).getFragmentManager().beginTransaction();
        PathListFragment fragment = new PathListFragment();
        Bundle bundle = new Bundle();
        LogUtil.d(TAG,"fileInfo p1:"+p1.getPath());
        bundle.putSerializable(GlobalConstants.FROM_SEARCH,new File(p1.getPath()));
        fragment.setArguments(bundle);
        transaction.replace(R.id.fragment_container,fragment,fragment.getClass().getSimpleName());
        transaction.commitAllowingStateLoss();
    }

    public static void openWith(Context context, String file) {
        Intent intent = IntentBuilderUtil.buildIntent(file,context);
        OpenTypeDialogCreator creator = new OpenTypeDialogCreator(context, intent, file);
        creator.show();
    }

    private static Intent buildSendFileIntent(List<FileInfo> files,Context context) {
        ArrayList<Uri> uris = new ArrayList<>();
        Set<String> mimeSet = new HashSet<>();
        boolean isMediaFile = false;
        String mimeType = "*/*";
        for (FileInfo file : files) {
            if (file.isDirectory()) {
                continue;
            }
            mimeType = MediaFileUtil.getMimeTypeForFile(file.getName());
            isMediaFile = MediaFileUtil.isMimeTypeMedia(mimeType);
            if(mimeType==null){
                mimeType = "*/*";
            }
            mimeSet.add(mimeType);
            LogUtil.i(TAG, "mimeType:" + mimeType + " isMediaFile:" + isMediaFile);
            Uri uri;
            uri = Util.getUriForFile(context,new File(file.getPath()));
            //android 7.0 sharefile must use file provider content uri,common content uri invide;
            /*if(isMediaFile){
                uri = mediaDatabaseDao.getContentUriFromPath(file.getPath());
            } else {
                uri = Util.getUriForFile(context,new File(file.getPath()));
            }*/
            LogUtil.i(TAG, "Send --> uri:" + uri);
            uris.add(uri);
        }
        if (uris.size() == 0) {
            return null;
        }
        boolean multiple = uris.size() > 1;
        Intent intent = new Intent(multiple ? android.content.Intent.ACTION_SEND_MULTIPLE
                        : android.content.Intent.ACTION_SEND);
        if (multiple) {
            if (mimeSet.size() == 1 && isMediaFile) {
                intent.setType(mimeType);
            } else {
                intent.setType("*/*");
            }
            LogUtil.d(TAG, "mimeSet.size():" + mimeSet.size());
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        } else {
            if (mimeType.equals("application/vnd.android.package-archive")) {// apk
                intent.setType("application/*");
            } else {
                intent.setType(mimeType);
            }
            intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
        }
        if (GlobalConstants.isSupportFileProviderForSdkN) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        LogUtil.d(TAG, "buildSendFileIntent intent::" + intent);
        return intent;
    }

    public static void sendOneOrMultiple(Context context, HashSet<FileInfo> fileInfos) {
        ArrayList<FileInfo> list = new ArrayList<>(fileInfos);
        Intent intent = buildSendFileIntent(list,context);
        //Intent intent = IntentBuilderUtil.buildShareIntent(context, fileInfos);
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_menu_send)));
    }

    /**
     * delete a single file, this file may be an image, a video, or a txt document.
     * @param file
     * @return
     */
    public static boolean deleteSingleFile(File file) {
        if(file.exists()) {
            file.delete();
        }
        return true;
    }

    public static boolean deleteSingleFile(String filePath) {
        File file = new File(filePath);
        return deleteSingleFile(file);
    }

}

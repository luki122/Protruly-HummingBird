package cn.com.protruly.filemanager.utils;

import android.media.RingtoneManager;
import android.provider.Settings;
import android.support.v4.content.FileProvider;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageVolume;
import android.os.storage.VolumeInfo;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import cn.com.protruly.filemanager.CategoryActivity;
import cn.com.protruly.filemanager.StorageVolumeManager;
import cn.com.protruly.filemanager.enums.Category;
import cn.com.protruly.filemanager.enums.FileInfo;
import cn.com.protruly.filemanager.enums.StorageCategory;
import cn.com.protruly.filemanager.utils.MediaFileType;
import cn.com.protruly.filemanager.R;

/**
 * Created by liushitao on 17-4-19.
 */

public class Util {

    public static Uri getUriForFile(Context context, File file){
        MediaDatabaseDao mediaDatabaseDao = new MediaDatabaseDao(context);
        if(null == context || null == file){
            Log.d("bql","getUriForFile nullpoint");
            return null;
        }
        Uri uri = null;
        //String mimeType = MediaFileUtil.getMimeTypeForFile(file.getName());
        //boolean isMediaFile = MediaFileUtil.isMimeTypeMedia(mimeType);
        if(isUsbImageOrTextForN(file,context)){
            uri = FileProvider.getUriForFile(context.getApplicationContext(), GlobalConstants.FILE_PROVIDER_AUTHORITY, file);
            Log.d("bql","uri  >=24::::"+uri);
            Log.d("bql","FileProvider.getUriForFile");
            if(uri != null) return uri;
        }
        Log.d("bql","getCategory(file):"+getCategory(file));
        uri = mediaDatabaseDao.getContentUriFromPath(file.getPath(), getCategory(file));
        Log.d("bql", "getContentUriFromPath uri:" + uri);
        if (uri != null) return uri;
        /*if(GlobalConstants.isSupportFileProviderForSdkN){
            uri = FileProvider.getUriForFile(context.getApplicationContext(), GlobalConstants.FILE_PROVIDER_AUTHORITY, file);
            Log.d("bql","uri  >=24::::"+uri);
            Log.d("bql","FileProvider.getUriForFile");
            if(uri != null) return uri;
        }*/
        Log.d("bql","Uri.fromFile uri:"+Uri.fromFile(file));
        return Uri.fromFile(file);
        //Android N view file another method with content uri
        //MediaDatabaseDao mediaDatabaseDao = new MediaDatabaseDao(context);
        //uri = mediaDatabaseDao.getContentUriFromPath(file.getPath());
    }

    public static boolean isUsbImageOrTextForN(File file,Context context){
        String path = file.getPath();
        if(GlobalConstants.isSupportFileProviderForSdkN
                && path.startsWith(Util.getOtgStoragePath(context))
                && (Util.isImageFile(path)||Util.isPopularFile(path)||Util.isVideoFile(path))){
            return true;
        }
        return false;
    }

    public static int getCategory(File file){
        int category = Category.Default;
        if(Util.isImageFile(file.getPath())) category = Category.Picture;
        if(Util.isVideoFile(file.getPath())) category = Category.Video;
        return category;
    }

    public static int getCategoryForMusic(File file){
        int category = Category.Default;
        if(Util.isImageFile(file.getPath())) category = Category.Picture;
        if(Util.isVideoFile(file.getPath())) category = Category.Video;
        if(Util.isAudioFile(file.getPath())) category = Category.Music;
        return category;
    }

    public static boolean storageHasEnoughCapacity(String storagePath, long size) {
        return (size <= getFreeSize(storagePath));
    }

    public static long getPhoneRomSpace(Context context) {
        long knownTotalSpace = getKnownTotalSpace(context);
        /*long size1G = 0x40000000;
        long phoneRomSpace = size1G << 0x2;
        for(; phoneRomSpace < knownTotalSpace; ) {
            if(phoneRomSpace <= 0x0) {
                return phoneRomSpace;
            }
        }
        return phoneRomSpace;*/
        return knownTotalSpace;

    }

    private static long getKnownTotalSpace(Context context) {
        long systemTotalSize = getTotalSize("/system");
        long cacheTotalSize = getTotalSize("/cache");
        long phoneStorageSize = getTotalSize(getPhoneStoragePath(context));
        return ((systemTotalSize + cacheTotalSize) + phoneStorageSize);
    }

    public static long getTotalSize(String string) {
        if (string == null) return 0L;
        try {
            StatFs localStatFs = new StatFs(string);
            long l = localStatFs.getBlockCountLong();
            long i = localStatFs.getBlockSizeLong();
            return l * i;
        }
        catch (Exception exception) {
            Log.d("bql",exception+"");
        }
        return 0L;
    }

    public static long getFreeSize(String string) {
        if (string == null) return 0L;
        try {
            StatFs localStatFs = new StatFs(string);
            long l = localStatFs.getAvailableBlocksLong();
            long i = localStatFs.getBlockSizeLong();
            return l * i;
        }
        catch (Exception exception) {
            Log.d("bql",exception+"");
        }
        return 0L;
    }

    public static long getFreeSizeWithFile(String string) {
        File file = new File(string);
        return file.getFreeSpace();
    }

    public static String reconstructPath(String srcPath, String destPath) {
        int i = 1;
        String name = new File(srcPath).getName();
        File newFile = new File(destPath, name);
        String postfix = getPostfixWithDot(getFilePostfix(name));
        while(newFile.exists()) {
            String newTitle = MediaFileUtil.getFileTitle(name) + " " + i;
            newFile = new File(destPath, newTitle + postfix);
            i++;
        }
        return newFile.getPath();
    }

    public static String getFilePostfix(String fileName) {
        if(fileName != null) {
            int dotPosition = fileName.lastIndexOf(".");
            if(dotPosition != -1) {
                return fileName.substring((dotPosition + 1), fileName.length());
            }
        }
        return "";
    }

    public static ArrayList<FileInfo> getFileListFromPath(Context context,String fileName){
        ArrayList<FileInfo> fileList = new ArrayList<>();
        if(fileName == null){
            return null;
        }
        if(!fileName.startsWith(getPhoneStoragePath(context))&& !fileName.startsWith("/storage/emulated/0")&&
        !fileName.startsWith(getSdStoragePath(context)) && !fileName.startsWith(getOtgStoragePath(context))){
            FileInfo fileInfo = new FileInfo(fileName);
            fileList.add(fileInfo);
            return fileList;
        }
        String name = fileName;
        fileList.add(new FileInfo(name));
        while(getStorageName(context,name).equals(context.getResources().getString(R.string.other_storage))){
            int seperator = name.lastIndexOf("/");
            if(seperator != -1){
                name = fileName.substring(0,seperator);
                if(!getStorageName(context,name).equals(context.getResources().getString(R.string.phone_storage))&&
                        !getStorageName(context,name).equals(context.getResources().getString(R.string.sd_storage))
                        &&!getStorageName(context,name).equals(context.getResources().getString(R.string.otg_storage))){
                    fileList.add(new FileInfo(name));
                }else{
                    break;
                }
            }
        }
        fileList.remove(new FileInfo(name));
        FileInfo fileInfo = new FileInfo(name);
        fileInfo.fileName = getStorageName(context,name);
        fileList.add(fileInfo);
        return fileList;
    }

    public static ArrayList<FileInfo> getFileListFromPathSecond(Context context,String fileName){
        ArrayList<FileInfo> fileList = new ArrayList<>();
        if(fileName == null){
            return fileList;
        }
        String sdPath = StorageVolumeManager.getSDPath();
        List<Map> otgPathMap = StorageVolumeManager.getOTGPathList();
        if(!fileName.startsWith(StorageVolumeManager.getPhoneStoragePath(context))&& !fileName.startsWith("/storage/emulated/0")&&
                null != sdPath && !fileName.startsWith(sdPath) && null != otgPathMap && otgPathMap.size()==1 &&
                !fileName.startsWith(String.valueOf(otgPathMap.get(0).get(StorageVolumeManager.PATH)))){
            FileInfo fileInfo = new FileInfo(fileName);
            fileList.add(fileInfo);
            return fileList;
        }
        String storageName = getStorageName(context,fileName);
        if(getStorageCategory(context,fileName)!=StorageCategory.CHILD_STORAGE){
            fileList.add(new FileInfo(storageName));
            return fileList;
        }
        String name = fileName;
        fileList.add(new FileInfo(name));
        while(getStorageCategory(context,name)==StorageCategory.CHILD_STORAGE){
            int seperator = name.lastIndexOf("/");
            if(seperator != -1){
                name = fileName.substring(0,seperator);
                int storageCate =  getStorageCategory(context,name);
                if(storageCate!=StorageCategory.PHONE_STORAGE && storageCate!=StorageCategory.SD_STORAGE && storageCate!=StorageCategory.SINGLE_OTG){
                    fileList.add(new FileInfo(name));
                }else{
                    break;
                }
            }
        }
        fileList.remove(new FileInfo(name));
        FileInfo fileInfo = new FileInfo(name);
        fileInfo.fileName = getStorageName(context,name);
        fileList.add(fileInfo);
        return fileList;
    }

    public static String getPostfixWithDot(String postfix) {
        if(!TextUtils.isEmpty(postfix)) {
            return "." + postfix;
        }
        return "";
    }


    public static final long KB = 1024;
    public static final long MB = 1024 * KB;
    public static final long GB = 1024 * MB;
    public static final long TB = 1024 * GB;

    public static String getFileSizeAndUnit(long value, int keepAfterPoint) {
        if(value > TB) {
            BigDecimal fraction = BigDecimal.valueOf(TB);
            BigDecimal result = BigDecimal.valueOf(value).divide(fraction);
            float ret = result.setScale(keepAfterPoint, BigDecimal.ROUND_HALF_UP).floatValue();
            return ret + "TB";
        } else if(value > GB) {
            BigDecimal fraction = BigDecimal.valueOf(GB);
            BigDecimal result = BigDecimal.valueOf(value).divide(fraction);
            float ret = result.setScale(keepAfterPoint, BigDecimal.ROUND_HALF_UP).floatValue();
            return ret + "GB";
        } else if(value > MB) {
            BigDecimal fraction = BigDecimal.valueOf(MB);
            BigDecimal result = BigDecimal.valueOf(value).divide(fraction);
            float ret = result.setScale(keepAfterPoint, BigDecimal.ROUND_HALF_UP).floatValue();
            return ret + "MB";
        } else if(value > KB) {
            BigDecimal fraction = BigDecimal.valueOf(KB);
            BigDecimal result = BigDecimal.valueOf(value).divide(fraction);
            float ret = result.setScale(keepAfterPoint, BigDecimal.ROUND_HALF_UP).floatValue();
            return ret + "KB";
        } else {
            return value + "B";
        }
    }

    public static Map<String, Object> getFileSizeAndUnits(Context paramContext, long paramLong) {
        HashMap localHashMap = new HashMap();
        float f = (float)paramLong;
        int i = R.string.kilobyteShort;
        if (f > 1024.0F) {
            i = R.string.kilobyteShort;
            f /= 1024.0F;
        }
        if (f > 1024.0F) {
            i = R.string.megabyteShort;
            f /= 1024.0F;
        }
        if (f > 1024.0F) {
            i = R.string.gigabyteShort;
            f /= 1024.0F;
        }
        if (f > 1024.0F) {
            i = R.string.terabyteShort;
            f /= 1024.0F;
        }
        if (f > 1024.0F) {
            i = R.string.petabyteShort;
            f /= 1024.0F;
        }
        String str = paramContext.getResources().getString(i);
        localHashMap.put("file.size", Float.valueOf(f));
        localHashMap.put("file.units", str);
        return localHashMap;
    }

    public static Map<String, Object> getTotalFileSizeAndUnits(Context paramContext, long paramLong) {
        HashMap<String,Object> hashMap = new HashMap<>();
        Log.d("long","paramLong:"+paramLong);
        if(paramLong==0L){
            hashMap.put("file.size", 0);
            hashMap.put("file.units", paramContext.getResources().getString(R.string.kilobyteShort));
            return hashMap;
        }
        float f = (float)paramLong;
        int i = R.string.kilobyteShort;
        if (f > 1024.0F) {
            i = R.string.kilobyteShort;
            f /= 1024.0F;
        }
        if (f > 1024.0F) {
            i = R.string.megabyteShort;
            f /= 1024.0F;
        }
        if (f > 1024.0F) {
            i = R.string.gigabyteShort;
            f /= 1024.0F;
        }
        if (f > 1024.0F) {
            i = R.string.terabyteShort;
            f /= 1024.0F;
        }
        if (f > 1024.0F) {
            i = R.string.petabyteShort;
            f /= 1024.0F;
        }
        String str = paramContext.getResources().getString(i);
        hashMap.put("file.size", totalToWeight(f));
        hashMap.put("file.units", str);
        return hashMap;
    }

    public static double totalToWeight(float total){
        for(int i=1;;i++){
            double max = Math.pow(2,i);
            if(total <= max){
                return max;
            }
        }
    }

    //the last０ is not show
    public static String formatFreePhoneSizeSecond(float paramFloat) {
        BigDecimal bigFloat = new BigDecimal(paramFloat);
        int zhengshu = (int)Math.floor(paramFloat);
        if(zhengshu==paramFloat){
            return String.valueOf(zhengshu);
        }else{
            return String.valueOf(bigFloat.setScale(2,BigDecimal.ROUND_HALF_UP).floatValue());
        }
    }
    //always show two point num
    public static String formatSize(float paramFloat){
        String roundFormat = "%.2f";
        Object[] arrayOfObject = new Object[1];
        arrayOfObject[0] = paramFloat;
        return String.format(Locale.getDefault(),roundFormat, arrayOfObject);
    }

    public static String formatFreePhoneSize(float paramFloat)
    {
        if (paramFloat < 100.0F);
        for (String str = "%.1f"; ; str = "%.0f") {
            Object[] arrayOfObject = new Object[1];
            arrayOfObject[0] = paramFloat;
            return String.format(str, arrayOfObject);
        }
    }

    public static String formatFileSizeSecond(Double sizeBytes) {
        String roundFormat = "%.0f";
        Object[] arrayOfObject = new Object[1];
        arrayOfObject[0] = sizeBytes;
        return String.format(Locale.getDefault(),roundFormat, arrayOfObject);
    }

    public static String formatFileSize(Double sizeBytes) {
        String roundFormat;
        if(sizeBytes < 1.0f) {
            roundFormat = "%.1f";
        } else if(sizeBytes < 10.0f) {
            roundFormat = "%.1f";
        } else if(sizeBytes < 100.0f) {
            roundFormat = "%.0f";
        } else {
            roundFormat = "%.0f";
        }
        Object[] arrayOfObject = new Object[1];
        arrayOfObject[0] = sizeBytes;
        return String.format(roundFormat, arrayOfObject);
    }

    public static boolean isStorageMounted(Context paramContext, String path) {
        return "mounted".equals(getStorageState(paramContext, path))
                ||"mounted".equals(getOtgState(paramContext));
    }

    private static Object getStorageState(Context paramContext, String path) {
        if (path != null)
            try {
                StorageManager storageManager = (StorageManager)paramContext.getSystemService(Context.STORAGE_SERVICE);
                return StorageManager.class.getDeclaredMethod("getVolumeState", new Class[] { String.class }).invoke(storageManager, path);
            }
            catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        return "";
    }


    public static String getPhoneStoragePath(Context context) {
        Log.d("sx","phone Environment.isExternalStorageRemovable():"+Environment.isExternalStorageRemovable());
        Log.d("sx","phone getSecondaryStorageDirectory():"+getSecondaryStorageDirectory());
        Log.d("sx","phone Environment.getExternalStorageDirectory().getAbsolutePath():"+Environment.getExternalStorageDirectory().getAbsolutePath());
        if(Environment.isExternalStorageRemovable()) {
            return getSecondaryStorageDirectorySencond(context);
        }
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public static String getSdStoragePath(Context context) {
        Log.d("sx","sd Environment.isExternalStorageRemovable():"+Environment.isExternalStorageRemovable());
        Log.d("sx","sd getSecondaryStorageDirectory():"+getSecondaryStorageDirectory());
        Log.d("sx","sd Environment.getExternalStorageDirectory().getAbsolutePath():"+Environment.getExternalStorageDirectory().getAbsolutePath());
        if(!Environment.isExternalStorageRemovable()) {
            return getSecondaryStorageDirectorySencond(context);
        }
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public static String getSecondaryStorageDirectory() {
        String result = null;
        try {
            Class<?> c = Class.forName("android.os.Environment");
            Method getSecondaryStorageDirectory = c.getMethod("getSecondaryStorageDirectory", new Class[0]);
            if (getSecondaryStorageDirectory != null)
                result = (String)getSecondaryStorageDirectory.invoke(null, new Object[0]);
            if(result!=null) {
                return result;
            }else{
                return getLevelSecondaryStorageDirectory();
            }
        } catch(Exception e) {
            e.fillInStackTrace();
        }
        return getLevelSecondaryStorageDirectory();
    }

    public static String getSecondaryStorageDirectorySencond(Context context) {
        StorageManager mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(mStorageManager);
            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String path = (String) getPath.invoke(storageVolumeElement);
                boolean removable = (Boolean) isRemovable.invoke(storageVolumeElement);
                if (removable) {
                    return path;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return getLevelSecondaryStorageDirectory();
    }

    private static String getLevelSecondaryStorageDirectory() {
        try {
            return System.getenv("SECONDARY_STORAGE");
        } catch(Exception localException1) {
        }
        return "";
    }

    public static String getOtgStoragePath(Context context){
        StorageManager mStorageManager = (StorageManager)context.getSystemService(Context.STORAGE_SERVICE);
        final List<VolumeInfo> vols = mStorageManager.getVolumes();
        VolumeInfo usbotgVolume = null;
        for(VolumeInfo vol : vols){
            if(vol.getDisk() != null && vol.getType()==VolumeInfo.TYPE_PUBLIC
                    && vol.getDisk().isUsb())
                usbotgVolume = vol;
        }
        if(usbotgVolume!= null) {
            String usbOtgPath = usbotgVolume.getPath()==null ? null :usbotgVolume.getPath().toString();
            if(usbotgVolume.isMountedReadable() && usbOtgPath != null){
                return usbOtgPath;
            }
        }
        return "no_otg";
    }

    public static ArrayList<String> getOtherStorageNameList(Context context){
        StorageManager mStorageManager = (StorageManager)context.getSystemService(Context.STORAGE_SERVICE);
        ArrayList<String> usbStorageList = new ArrayList<>();
        final List<VolumeInfo> vols = mStorageManager.getVolumes();
        VolumeInfo usbotgVolume = null;
        for(VolumeInfo vol : vols){
            if(vol.getDisk() != null && vol.getType()==VolumeInfo.TYPE_PUBLIC
                    && vol.getDisk().isUsb()) {
                usbotgVolume = vol;
            }
            if(usbotgVolume!= null) {
                String usbOtgPath = usbotgVolume.getPath()==null ? null :usbotgVolume.getPath().toString();
                if(usbotgVolume.isMountedReadable() && usbOtgPath != null){
                    usbStorageList.add(usbotgVolume.getDescription());
                }
            }
        }
        return usbStorageList;
    }

    public static String getSdStoragePathSecond(Context context){
        StorageManager mStorageManager = (StorageManager)context.getSystemService(Context.STORAGE_SERVICE);
        final List<VolumeInfo> vols = mStorageManager.getVolumes();
        VolumeInfo sdVolume = null;
        for(VolumeInfo vol : vols){
            if(vol.getDisk() != null && vol.getType()==VolumeInfo.TYPE_PUBLIC
                    && vol.getDisk().isSd())
                sdVolume = vol;
        }
        if(sdVolume!= null) {
            String sdPath = sdVolume.getPath()==null ? null :sdVolume.getPath().toString();
            if(sdVolume.isMountedReadable() && sdPath != null){
                return sdPath;
            }
        }
        return "no_sd";
    }

    private static String getOtgState(Context context){
        if(null == context) return null;
        StorageManager mStorageManager = (StorageManager)context.getSystemService(Context.STORAGE_SERVICE);
        final List<VolumeInfo> vols = mStorageManager.getVolumes();
        VolumeInfo usbotgVolume = null;
        for(VolumeInfo vol : vols){
            if(vol.getDisk() != null && vol.getType()==VolumeInfo.TYPE_PUBLIC
                    && vol.getDisk().isUsb())
                usbotgVolume = vol;
        }
        if(usbotgVolume!= null) {
            if(usbotgVolume.isMountedReadable()){
                return "mounted";
            }
        }
        return "unkown";
    }


    public static String getOtgStoragePathSecond() {
        String result = null;
        try {
            Class<?> c = Class.forName("android.os.Environment");
            Method getOtgStorageDirectory = c.getMethod("getOtgStorageDirectory");
            if (getOtgStorageDirectory != null)
                result = (String)getOtgStorageDirectory.invoke(null, new Object[0]);
            return result;
        } catch(Exception e) {
            e.fillInStackTrace();
        }
        return getLevelOtgStoragePath();
    }

    private static String getLevelOtgStoragePath() {
        try {
            return System.getenv("USBOTG_STORAGE");
        } catch(Exception localException1) {
        }
        return "";
    }

    public static ArrayList<String> getStoragePathListSecond(final Context context){
        if(null == context) return null;
        try{
            ArrayList<String> pathList = new ArrayList<>();
            String firstPath = Environment.getExternalStorageDirectory().getPath();
            pathList.add(firstPath);
            StorageManager mStorageManager = (StorageManager)context.getSystemService(Context.STORAGE_SERVICE);
            //added reflect method
            Class<?>  VoInfo = Class.forName("android.os.storage.VolumeInfo");
            Method getVolumes = StorageManager.class.getDeclaredMethod("getVolumes");
            Method getTYpe = VoInfo.getDeclaredMethod("getType");
            Method isMountedReadable = VoInfo.getDeclaredMethod("isMountedReadable");
            Method getPath = VoInfo.getDeclaredMethod("getPath");
            List<Object> vols = (List<Object>)getVolumes.invoke(mStorageManager);
            for(Object vol : vols){
                int type = (int)getTYpe.invoke(vol);
                boolean isRead = (boolean)isMountedReadable.invoke(vol);
                File path = (File)getPath.invoke(vol);
                if(type == 0){
                    if(isRead){
                        pathList.add(path.toString());
                    }
                }
            }
            /*final List<VolumeInfo> vols = mStorageManager.getVolumes();
            for(VolumeInfo vol : vols){
                if(vol.getType()==VolumeInfo.TYPE_PUBLIC)
                    if(vol.isMountedReadable()) {
                        pathList.add(vol.getPath().toString());
                    }
            }*/
            return pathList;
        }catch(Exception e){
            e.fillInStackTrace();
        }
        return new ArrayList<>();
    }

    public static List<String> getStoragePathList(Context context) {
        return getStoragePathListByReflect(context);
    }

    private static List<String> getStoragePathListByReflect(Context context) {
        if(null == context) return null;
        StorageManager sManager = (StorageManager)context.getSystemService(Context.STORAGE_SERVICE);
        try {
            Class<?>[] paramClasses = {};
            Method getVolumePaths = StorageManager.class.getDeclaredMethod("getVolumePaths",paramClasses);
            getVolumePaths.setAccessible(true);
            Object[] params = {};
            String[] storagePaths = (String[])getVolumePaths.invoke(sManager,params);
            return Arrays.asList(storagePaths);
        } catch(Exception e) {
            e.fillInStackTrace();
        }
        return new ArrayList<>();
    }

    public static boolean isMediaFile(String path) {
        MediaFileType type = MediaFileUtil.getFileType(path);
        return false;
    }

    public static boolean isCertinstallerFileType(String path) {
        MediaFileType type = MediaFileUtil.getFileType(path);
        return ((type != null) && (MediaFileUtil.isCertinstallerFileType(type.mFileType)));
    }

    public static boolean isAudioFile(String path) {
        MediaFileType type = MediaFileUtil.getFileType(path);
        return ((type != null) && (MediaFileUtil.isAudioFileType(type.mFileType)));
    }

    public static boolean isImageFile(String path) {
        MediaFileType type = MediaFileUtil.getFileType(path);
        return ((type != null) && (MediaFileUtil.isImageFileType(type.mFileType)));
    }

    public static boolean isPopularFile(String path) {
        MediaFileType type = MediaFileUtil.getFileType(path);
        return ((type != null) && (MediaFileUtil.isPopularFileType(type.mFileType)));
    }

    public static boolean isVideoFile(String path) {
        MediaFileType type = MediaFileUtil.getFileType(path);
        return ((type != null) && (MediaFileUtil.isVideoFileType(type.mFileType)));
    }

    public static boolean isZipFile(String path) {
        MediaFileType type = MediaFileUtil.getFileType(path);
        return ((type != null) && (MediaFileUtil.isZipFileType(type.mFileType)));
    }

    public static boolean istTxtFile(String path) {
        MediaFileType type = MediaFileUtil.getFileType(path);
        return ((type != null) && (MediaFileUtil.isTxtFileType(type.mFileType)));
    }

    public static boolean istWordFile(String path) {
        MediaFileType type = MediaFileUtil.getFileType(path);
        return ((type != null) && (MediaFileUtil.isWordFileType(type.mFileType)));
    }

    public static boolean istExcelFile(String path) {
        MediaFileType type = MediaFileUtil.getFileType(path);
        return ((type != null) && (MediaFileUtil.isExcelFileType(type.mFileType)));
    }

    public static boolean isPPtFile(String path) {
        MediaFileType type = MediaFileUtil.getFileType(path);
        return ((type != null) && (MediaFileUtil.isPPTFileType(type.mFileType)));
    }

    public static boolean isPdfFile(String path) {
        MediaFileType type = MediaFileUtil.getFileType(path);
        return ((type != null) && (MediaFileUtil.isPdfFileType(type.mFileType)));
    }

    public static boolean isApkFile(String path) {
        MediaFileType type = MediaFileUtil.getFileType(path);
        return ((type != null) && (MediaFileUtil.isApkFileType(type.mFileType)));
    }

    public static int getDefaultIconRes(FileInfo file) {
        return getDefaultIconRes(file.getPath());
    }

    public static int getDefaultIconRes(String path) {
        if(path==null){
            return Integer.valueOf(R.drawable.unkown_icon);
        }
        MediaFileType mediaType = MediaFileUtil.getFileType(path);
        Integer iconRes = 0;
        if((mediaType == null) || (mediaType.mIconRes == null)) {
            iconRes = Integer.valueOf(R.drawable.unkown_icon);
        } else {
            iconRes = mediaType.mIconRes;
        }
        return iconRes;
    }

    public static String formatDateString(long time) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd  HH:mm ",Locale.getDefault());
        Date date = new Date(time);
        return format.format(date);
    }

    public static String formatDateStringThird(long time) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd  HH:mm:ss",Locale.getDefault());
        Date date = new Date(time);
        return format.format(date);
    }

    public static String formatTimeString(long time){
        long formate_time = time/1000L;
        long hours = formate_time/3600L;
        long minutes = (formate_time%3600L)/60L;
        long seconds = formate_time%3600L%60L;
        String hour_format;
        String minites_format;
        String second_format;
        if(hours<10 && hours>=0){
            hour_format = "0"+hours;
        }else{
            hour_format = ""+hours;
        }
        if(minutes<10 && minutes>=0){
            minites_format = "0"+minutes;
        }else{
            minites_format = ""+minutes;
        }
        if(seconds<10 && seconds>=0){
            second_format = "0"+seconds;
        }else{
            second_format = ""+seconds;
        }
        return hour_format+":"+minites_format+":"+second_format;
    }

    public static String formatSize(Context context, long size) {
        return Formatter.formatFileSize(context, size);
    }

    public static void hideView(View view) {
        if (view == null) return;
        if (view.getVisibility() != View.VISIBLE) return;
        view.setVisibility(View.GONE);
    }

    public static void showView(View view) {
        if (view == null) return;
        if (view.getVisibility() == View.VISIBLE) return;
        view.setVisibility(View.VISIBLE);
    }


    public static void sendMessage(Handler handler,int what,Object obj,int arg1){
        Message msg = handler.obtainMessage();
        if(null == msg){
            return;
        }
        msg.what = what;
        msg.obj = obj;
        msg.arg1 = arg1;
        handler.sendMessage(msg);
    }

    private static String getStorageName(Context context, String paramString) {
        if(null==context || paramString == null){
            return "";
        }
        String sdPath = StorageVolumeManager.getSDPath();
        List<Map> otgPathMap = StorageVolumeManager.getOTGPathList();
        if ((TextUtils.equals("/storage/emulated/0", paramString)) || (TextUtils.equals(StorageVolumeManager.getPhoneStoragePath(context), paramString)))
            return context.getResources().getString(R.string.phone_storage);
        if (null != sdPath && TextUtils.equals(sdPath, paramString))
            return context.getResources().getString(R.string.sd_storage);
        if (null != otgPathMap && otgPathMap.size()==1 &&
                TextUtils.equals(String.valueOf(otgPathMap.get(0).get(StorageVolumeManager.PATH)), paramString))
            {return context.getResources().getString(R.string.otg_storage);}
        else if(null != otgPathMap && otgPathMap.size()>1) {
            return showOtgListStorageName(context,paramString, otgPathMap);
        }
        return context.getResources().getString(R.string.other_storage);
    }

    private static int getStorageCategory(Context context, String paramString) {
        if(null==context || paramString == null){
            return -1;
        }
        String sdPath = StorageVolumeManager.getSDPath();
        List<Map> otgPathMap = StorageVolumeManager.getOTGPathList();
        if ((TextUtils.equals("/storage/emulated/0", paramString)))
            return StorageCategory.PHONE_STORAGE;
        if (null != sdPath && TextUtils.equals(sdPath, paramString))
            return StorageCategory.SD_STORAGE;
        if (null != otgPathMap && otgPathMap.size()==1 &&
                TextUtils.equals(String.valueOf(otgPathMap.get(0).get(StorageVolumeManager.PATH)), paramString))
           {return StorageCategory.SINGLE_OTG;}
        else if(null != otgPathMap && otgPathMap.size()>1) {
            return StorageCategory.MULT_OTG;
        }
        return StorageCategory.CHILD_STORAGE;
    }

    private static String showOtgListStorageName(Context context,String info,List<Map> mapArrayList) {
        for(int i=0;i<mapArrayList.size();i++){
            String path = String.valueOf(mapArrayList.get(i).get(StorageVolumeManager.PATH));
            if(TextUtils.equals(path, info)) {
                return String.valueOf(mapArrayList.get(i).get(StorageVolumeManager.LABEL));
            }
        }
        return context.getResources().getString(R.string.other_storage);
    }

    public static String getString(Context context,int str, Object formatArgs) {
        if(context==null){
            return "";
        }else if(context.getResources()==null){
            return "";
        }
        return context.getResources().getString(str, formatArgs);
    }


    public static void showToast(Context context, int resId) {
          Toast.makeText(context, resId, Toast.LENGTH_SHORT).show();
    }

    public static void showToast(Context context, String str) {
        if(null == context) return;
        Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
    }


    public static void showToast(Context context, int resId, int duration) {
        if(null == context) return;
        Toast.makeText(context, resId, duration).show();
    }

    public static void showToast(Context context, String text, int duration) {
        if(null == context) return;
        Toast.makeText(context, text, duration).show();
    }

    public static String reconstructFolderName(String targetPath,String name){
        int i=1;
        String initName =name;
        while(new File(targetPath,name).exists()){
            name = initName+" "+i++;
        }
        return name;
    }

    public static boolean startWithDot(String name){
        return !TextUtils.isEmpty(name) && !name.equals(".") && name.startsWith(".");
    }

    public static void checkFailToCreateFolderReason(Context context,String destStotage,File newFile){
        if(getFreeSize(destStotage)==0){
            showToast(context,R.string.no_space);
            return;
        }
        showToast(context,R.string.new_folder_fail);
    }

    public static long getFileSize(Context context,File file) {
        long size = 0;
        if(file.isDirectory()) {
            File[] files;
            if(((CategoryActivity)context).getHidenStatus()){
                files = file.listFiles(new HiddenFileFilter());
            }else{
                files = file.listFiles();
            }
            if((files == null) || (files.length == 0)) {
                return 0;
            }
            for(File child : files) {
                size += getFileSize(context,child);
            }
            return size;
        }
        size +=file.length();
        return size;
    }

    public static long getFileSizeAvoidDigui(Context context,File file) {
        long size = 0;
        if (file.exists()) {
            LinkedList<File> list = new LinkedList<File>();
            File[] files;
            if(((CategoryActivity)context).getHidenStatus()){
                files = file.listFiles(new HiddenFileFilter());
            }else{
                files = file.listFiles();
            }
            for (File file2 : files) {
                if (file2.isDirectory()) {
                    list.add(file2);
                } else {
                    size += file2.length();
                }
            }
            File temp_file;
            while (!list.isEmpty()) {
                temp_file = list.removeFirst();
                File[] files1;
                if(((CategoryActivity)context).getHidenStatus()){
                    files1 = temp_file.listFiles(new HiddenFileFilter());
                }else{
                    files1 = temp_file.listFiles();
                }
                for (File file2 : files1) {
                    if (file2.isDirectory()) {
                        list.add(file2);
                    } else {
                        size += file2.length();
                    }
                }
            }
        }
        return size;
    }

    public static void supplementFileInfo(FileInfo info) {
        info.fileName = info.getName();
        info.filePath = info.getPath();
        info.modifiedTime = info.lastModified();
        info.isFile = info.isFile();
        if(info.isFile) {
            info.fileSize = info.length();
        }
    }

    private static void getChildNum(FileInfo info) {
        File[] files = info.listFiles(new HiddenFileFilter());
        if(files != null) {
            info.childFileNum = files.length;
        } else {
            info.childFileNum = 0;
        }
    }

    public static int getChildNum(Context context,FileInfo info) {
        if(null == context) return 0;
        File[] files;
        if(((CategoryActivity)context).getHidenStatus()){
            files = info.listFiles(new HiddenFileFilter());
        }else{
            files = info.listFiles();
        }
        if(files != null) {
            info.childFileNum = files.length;
        } else {
            info.childFileNum = 0;
        }
        return info.childFileNum;
    }

    /**
     * return 0-100 progress value
     * @param total
     * @param current
     * @return
     */
    public static int getProgress(long total, long current) {
        BigDecimal progress = null;
        if(BigDecimal.valueOf(total) != null){
            progress = BigDecimal.valueOf(current).divide(BigDecimal.valueOf(total), 3, BigDecimal.ROUND_HALF_EVEN);
        }
        if(null == progress) return 0;
        return (int)(progress.floatValue() * 100);
    }

    public static void startActivityForType(Context context,Intent intent){
        try{
            if(null == context) return;
            Log.d("open","intent xxxx:"+intent);
            context.startActivity(intent);
        }
        catch (ActivityNotFoundException exception) {
            Toast.makeText(context,R.string.no_app_to_open_file,Toast.LENGTH_SHORT).show();
        }
    }
}

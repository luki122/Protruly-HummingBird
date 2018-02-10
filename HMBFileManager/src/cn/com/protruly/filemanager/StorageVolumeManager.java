package cn.com.protruly.filemanager;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Environment;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.DiskInfo;
import android.util.Log;
import android.widget.Toast;
import cn.com.protruly.filemanager.R;

/**
 * Created by wenwenchao on 17-7-27.
 */

public class StorageVolumeManager {
    private String TAG = "StorageVolumeManager";

    public final static List<Map> storageMapList = new ArrayList<>();
    public final static int TYPE_PHONE = 0;
    public final static int TYPE_SD = 1;
    public final static int TYPE_OTG = 2;
    public final static int TYPE_OTHER = 3;
    public final static String PATH = "path";
    public final static String TYPE = "type";
    public final static String LABEL = "label";
    private StorageChangeListener mStorageChangeListener;
    private StorageManager mStorageManager;
    private Context mContext;
    private final static StorageVolumeManager mStorageVolumeManager = new StorageVolumeManager();
    private StorageVolumeManager(){}

    public static StorageVolumeManager getInstance(){
        return mStorageVolumeManager;
    }

    public void onDestroy(){
        this.mStorageManager.unregisterListener(mStorageListener);
    }

    public void init(Context context){
        this.mContext = context;
        this.mStorageManager = (StorageManager)context.getSystemService(Context.STORAGE_SERVICE);
        this.mStorageManager.registerListener(mStorageListener);
        getStorageMapList(context);
        if(storageMapList!=null) {
            android.util.Log.d(TAG, "init>storageMapList:  " + storageMapList.toString());
        }
    }


    public void setStorageChangeListener(StorageChangeListener mStorageChangeListener) {
        this.mStorageChangeListener = mStorageChangeListener;
    }

    public interface StorageChangeListener{
         void OnStorageMounted();
         void OnStorageEjected();
    }



    private final StorageEventListener mStorageListener = new StorageEventListener() {


        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            android.util.Log.d(TAG,"oldState is:"+oldState+",newstate:"+newState);
            if (isInteresting(vol)){
                if(oldState == VolumeInfo.STATE_CHECKING && newState == VolumeInfo.STATE_MOUNTED){
                    getStorageMapList(mContext);
                    if(storageMapList!=null) {
                        android.util.Log.d(TAG, "STATE_MOUNTED" + storageMapList.toString());
                    }
                    Log.d(TAG,"mStorageChangeListener SS :"+mStorageChangeListener);
                    if(null != mStorageChangeListener) {
                        mStorageChangeListener.OnStorageMounted();
                    }
                }
                if(oldState == VolumeInfo.STATE_MOUNTED && newState == VolumeInfo.STATE_EJECTING){
                    getStorageMapList(mContext);
                    if(storageMapList!=null) {
                        android.util.Log.d(TAG, "STATE_EJECTING" + storageMapList.toString());
                    }
                    Log.d(TAG,"mStorageChangeListener AA:"+mStorageChangeListener);
                    if(null != mStorageChangeListener) {
                        mStorageChangeListener.OnStorageEjected();
                    }
                }
            }
        }

    };

    private  boolean isInteresting(VolumeInfo vol) {
        switch(vol.getType()) {
            case VolumeInfo.TYPE_PRIVATE:
            case VolumeInfo.TYPE_PUBLIC:
                return true;
            default:
                return false;
        }
    }

    private void getStorageMapList(Context context){
        if(mStorageManager == null)return;
        storageMapList.clear();
        String phonepath = getPhoneStoragePath(context);
        if(phonepath != null){
            Map<String,Object> map = new HashMap<>();
            map.put(PATH, phonepath);
            map.put(TYPE, TYPE_PHONE);
            map.put(LABEL, mContext==null?"internal storage":mContext.getString(R.string.phone_storage));
            storageMapList.add(map);
        }

        final List<VolumeInfo> vols = mStorageManager.getVolumes();
        for(VolumeInfo vol : vols){
            if(vol.getType()==VolumeInfo.TYPE_PUBLIC)
                if(vol.isMountedReadable()) {
                    DiskInfo mDiskInfo = vol.getDisk();
                    String path = vol.getPath().toString();
                    Map<String,Object> map = new HashMap<>();
                    /*if(path.startsWith("/storage/emulated")){
                        map.put(PATH, path);
                        map.put(TYPE, TYPE_PHONE);
                        map.put(LABEL, vol.getDescription());
                    }else*/
                    if(mDiskInfo!=null && mDiskInfo.isSd()){
                        map.put(PATH, path);
                        map.put(TYPE, TYPE_SD);
                        map.put(LABEL, mDiskInfo.getDescription());

                    }else if(mDiskInfo!=null && mDiskInfo.isUsb()){
                        map.put(PATH, path);
                        map.put(TYPE, TYPE_OTG);
                        map.put(LABEL, mDiskInfo.getDescription());
                    }else{
                        map.put(PATH, path);
                        map.put(TYPE, TYPE_OTHER);
                        map.put(LABEL, mContext==null?"other storage":mContext.getString(R.string.other_storage));
                    }
                    storageMapList.add(map);
                }
        }
    }


    private static String getLevelSecondaryStorageDirectory() {
        try {
            return System.getenv("SECONDARY_STORAGE");
        } catch(Exception localException1) {
        }
        return "";
    }

    public static int getStorageTypeByPath(String path){
        if(path == null )return -1;
        if(null==storageMapList || null==storageMapList && storageMapList.size()<1)return -1;
        for(Map map:storageMapList){
             String rootpath = (String) map.get(PATH);
             if(rootpath == null) continue;
             if(path.startsWith(rootpath)){
                 return (int)map.get(TYPE);
             }
        }
        return -1;
    }


    public static String getPhoneStoragePath(Context context) {
        /*if(null==storageMapList || null==storageMapList && storageMapList.size()<1)return null;
        for(Map map:storageMapList){
            int type = (int)map.get(TYPE);
            if(type==TYPE_PHONE){
                return (String) map.get(PATH);
            }
        }
        return null;*/
        if(Environment.isExternalStorageRemovable()) {
            return getSecondaryStorageDirectorySencond(context);
        }
        return Environment.getExternalStorageDirectory().getAbsolutePath();
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

    public static String getSDPath(){
        if(null==storageMapList || null==storageMapList && storageMapList.size()<1)return null;
        for(Map map:storageMapList){
            int type = (int)map.get(TYPE);
            if(TYPE_SD==type){
                return (String) map.get(PATH);
            }
        }
        return null;
    }

    public static ArrayList<Map> getOTGPathList(){
        if(null==storageMapList || null==storageMapList && storageMapList.size()<1)return null;
        ArrayList<Map> otgList = new ArrayList<>();
        for(Map map:storageMapList){
            int type = (int)map.get(TYPE);
            if(TYPE_OTG==type){
                otgList.add(map);
            }
        }
        if(otgList != null && otgList.size()>0){
            return otgList;
        }
        return null;
    }

}

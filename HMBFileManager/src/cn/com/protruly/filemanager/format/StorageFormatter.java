package cn.com.protruly.filemanager.format;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.storage.IMountService;
import android.os.ServiceManager;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.os.storage.DiskInfo;
import android.os.IBinder;
import android.util.Log;
import android.view.WindowManager;

import java.io.File;
import java.util.List;

import cn.com.protruly.filemanager.utils.LogUtil;
import cn.com.protruly.filemanager.utils.Util;

/**
 * Created by sqf on 17-6-13.
 */

public class StorageFormatter {

    private static final String TAG = "StorageFormatter";

    private Context mContext;
    private IMountService mMountService;
    private StorageFormatListener mStorageFormatListener;
    private boolean mFormattingAndMounting;
    private StorageManager mStorageManager;
    private VolumeInfo mVolumeInfoToFormat;
    private FormatTask mFormatTask;
    private MountTask mMountTask;
    private String mStoragePath;

    private int mStatus;

    //not started yet
    private static final int STATUS_UNKNOWN = 0;
    //formatting
    private static final int STATUS_EJECTTING = 1;
    private static final int STATUS_UNMOUNT_AFTER_EJECT = 2;
    private static final int STATUS_FORMATTING = 3;
    private static final int STATUS_UNMOUNT_AFTER_FOMAT = 4;
    //mounting
    private static final int STATUS_CHECKING = 5;
    private static final int STATUS_MOUNTED = 6;

    private static final int [] mProgressMap = {
            0, 20, 40, 60, 80, 90, 100
    };


    public interface StorageFormatListener {
        public void onStorageFormatStart(String path);
        public void onStorageFormatProgress(String path, int progress);
        public void onStorageFormatSucceeded(String path);
        public void onStorageFormatFailed(String path);
    }

    public class FormatTask extends AsyncTask<String, Integer, Boolean> {

        private String mStoragePath;


        public FormatTask(String storagePath) {
            mStoragePath = storagePath;
            LogUtil.i(TAG, "FormatTask mStoragePath:" + mStoragePath);
        }

        @Override
        protected void onPreExecute() {
            mFormattingAndMounting = true;
            mStorageFormatListener.onStorageFormatStart(mStoragePath);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            VolumeInfo currentVolume = null;
            final List<VolumeInfo> vols = mStorageManager.getVolumes();
            for(VolumeInfo vol : vols) {
                String volumePath = vol.getPath().getPath();
                LogUtil.i(TAG, "volumenInfo Id:" + vol.getId() + " volumeInfo path:" + volumePath + " volumeInfo internal path:" + vol.getInternalPath());
                if(volumePath.equals(mStoragePath)) {
                    LogUtil.i(TAG, "compare: " + vol.getPath() + " compared true ");
                    currentVolume = vol;
                    break;
                } else if(mStoragePath.startsWith(volumePath)) {
                    //volume path may be /storage/emulated, but real path is /storage/emulated/0
                    LogUtil.i(TAG, "BEFORE CORRECTION: mStoragePath: " + mStoragePath);
                    mStoragePath = volumePath;
                    LogUtil.i(TAG, "AFTER CORRECTION: mStoragePath: " + mStoragePath);
                    currentVolume = vol;
                    break;
                }
            }
            mVolumeInfoToFormat = currentVolume;
            return format(currentVolume);
        }

        @Override
        protected void onPostExecute(Boolean value) {
            if(!value.booleanValue()) {
                mStorageFormatListener.onStorageFormatFailed(mStoragePath);
            }
        }
    }

    public class MountTask extends AsyncTask<Void, Void, Boolean> {

        private final VolumeInfo mVolumeInfo;
        private final String mDescription;

        public MountTask(VolumeInfo volume) {
            mVolumeInfo = volume;
            mDescription = mStorageManager.getBestVolumeDescription(volume);
            LogUtil.i(TAG, "MountTask description:" + mDescription);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                mStorageManager.mount(mVolumeInfo.getId());
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean value) {
            if(!value.booleanValue()) {
                mStorageFormatListener.onStorageFormatFailed(mStoragePath);
            }
        }
    }

    private int getProgress(int status) {
        return mProgressMap[status];
    }

    public boolean inProgress() {
        return mFormattingAndMounting;
    }

    private StorageEventListener mStorageListener = new StorageEventListener() {

        /**
         * Called when the detection state of a USB Mass Storage host has changed.
         * @param connected true if the USB mass storage is connected.
         */
        @Override
        public void onUsbMassStorageConnectionChanged(boolean connected) {

        }

        /**
         * Called when storage has changed state
         * @param path the filesystem path for the storage
         * @param oldState the old state as returned by {@link android.os.Environment#getExternalStorageState()}.
         * @param newState the old state as returned by {@link android.os.Environment#getExternalStorageState()}.
         */
        @Override
        public void onStorageStateChanged(String path, String oldState, String newState) {
            LogUtil.i(TAG, "StorageEventListener----onStorageStateChanged path:" + path);
        }

        @Override
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            if( !mFormattingAndMounting) return;
            LogUtil.i(TAG, "StorageEventListener----onVolumeStateChanged vol:" + vol + " oldState:" + oldState + " newState:" + newState);
            LogUtil.i(TAG, "mVolumeInfoToFormat.getId():" + mVolumeInfoToFormat.getId() + " vol.getId():" + vol.getId());

            if( ! mVolumeInfoToFormat.getId().equals(vol.getId())) return;
            if(oldState == VolumeInfo.STATE_MOUNTED && newState == VolumeInfo.STATE_EJECTING ) {
                mStatus = STATUS_EJECTTING;
            } else if(oldState == VolumeInfo.STATE_EJECTING && newState == VolumeInfo.STATE_UNMOUNTED) {
                mStatus = STATUS_UNMOUNT_AFTER_EJECT;
            } else if(oldState == VolumeInfo.STATE_UNMOUNTED && newState == VolumeInfo.STATE_FORMATTING) {
                mStatus = STATUS_FORMATTING;
            } else if(oldState == VolumeInfo.STATE_FORMATTING && newState == VolumeInfo.STATE_UNMOUNTED) {
                mStatus = STATUS_UNMOUNT_AFTER_FOMAT;
            } else if(oldState == VolumeInfo.STATE_UNMOUNTED && newState == VolumeInfo.STATE_CHECKING) {
                mStatus = STATUS_CHECKING;
            } else if(oldState == VolumeInfo.STATE_CHECKING && newState == VolumeInfo.STATE_MOUNTED) {
                mStatus = STATUS_MOUNTED;
            }
            mStorageFormatListener.onStorageFormatProgress(mStoragePath, getProgress(mStatus));
            if(mStatus == STATUS_UNMOUNT_AFTER_FOMAT) {
                mMountTask = new MountTask(vol);
                mMountTask.execute();
            } else if(mStatus == STATUS_MOUNTED) {
                mStorageFormatListener.onStorageFormatSucceeded(mStoragePath);
                mFormattingAndMounting = false;
            }
        }

        @Override
        public void onVolumeRecordChanged(VolumeRecord rec) {
            LogUtil.i(TAG, "StorageEventListener----onVolumeRecordChanged rec:" + rec);
        }

        @Override
        public void onVolumeForgotten(String fsUuid) {
            LogUtil.i(TAG, "StorageEventListener----onVolumeForgotten fsUuid:" + fsUuid);
        }

        @Override
        public void onDiskScanned(DiskInfo disk, int volumeCount) {
            LogUtil.i(TAG, "StorageEventListener----onDiskScanned disk:" + disk + " volumeCount:" + volumeCount);
        }

        @Override
        public void onDiskDestroyed(DiskInfo disk) {
            LogUtil.i(TAG, "StorageEventListener----onDiskDestroyed disk:" + disk);
        }

    };

    public StorageFormatter(Context context, StorageFormatListener listener) {
        mContext = context;
        //mMountService = getMountService();
        mStorageManager = (StorageManager)context.getSystemService(Context.STORAGE_SERVICE);
        mStorageManager.registerListener(mStorageListener);
        mStorageFormatListener = listener;
    }

    public void cleanUp() {
        mStorageManager.unregisterListener(mStorageListener);
    }

    public void unmount(VolumeInfo volumeInfo) {
        try {
            mStorageManager.unmount(volumeInfo.getId());
        } catch (Exception e) {
            Log.e(TAG, "unmount:" + e.getMessage());
        }
    }

    public void doFormat(String storagePath) {
        mStoragePath = storagePath;
        mFormatTask = new FormatTask(storagePath);
        mFormatTask.execute();
    }

    public boolean isFormatting() {
        return mFormatTask.getStatus() != AsyncTask.Status.FINISHED;
    }

    private boolean format(VolumeInfo volumeInfo) {
        try {
            mStorageManager.format(volumeInfo.getId());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "format:" + e.getMessage());
            return false;
        }
    }

    private void mount(VolumeInfo volumeInfo) {
        try {
            //mMountService.mount(storagePath);
            mStorageManager.mount(volumeInfo.getId());
        } catch (Exception e) {
            Log.e(TAG, "mount:" + e.getMessage());
        }
    }

    private synchronized IMountService getMountService() {
        if (mMountService == null) {
            IBinder service = ServiceManager.getService("mount");
            if (service != null) {
                mMountService = IMountService.Stub.asInterface(service);
            } else {
                Log.e(TAG, "Can't get mount service");
            }
        }
        return mMountService;
    }

}

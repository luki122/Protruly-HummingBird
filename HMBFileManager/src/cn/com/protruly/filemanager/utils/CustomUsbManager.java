package cn.com.protruly.filemanager.utils;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import java.util.HashMap;
import java.util.Iterator;

import cn.com.protruly.filemanager.CategoryActivity;

/**
 * Created by sqf on 17-7-18.
 */

public class CustomUsbManager extends BroadcastReceiver {

    private static final String TAG = "CustomUsbManager";

    private Context mContext;
    private UsbManager mUsbManager;

    private IntentFilter mIntentFilter = new IntentFilter();
    public static final int USB_REQUEST_CODE = 1001;

    public CustomUsbManager(Context context) {
        mContext = context;
        mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);

        mIntentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        mIntentFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
    }

    public void registerBroadcastReceiver() {
        mContext.registerReceiver(this, mIntentFilter);
    }

    public void unregisterBroadcastReceiver() {
        mContext.unregisterReceiver(this);
    }

    private void listUsbDevices() {
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while(deviceIterator.hasNext()){
            UsbDevice device = deviceIterator.next();
            LogUtil.i(TAG, "Usb Device Name:" + device.getDeviceName() + " Device Id:" + device.getDeviceId());
        }

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action == null) return;
        if(action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
            LogUtil.i(TAG, " UsbBroadcastReceiver onReceive -- ACTION_USB_DEVICE_ATTACHED");
            UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            LogUtil.i(TAG, " usbDevice:" + usbDevice);
            Intent activityIntent = new Intent(mContext, CategoryActivity.class);
            PendingIntent pi = PendingIntent.getActivity(mContext, USB_REQUEST_CODE, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            boolean hasPermission = mUsbManager.hasPermission(usbDevice);
            Util.showToast(mContext, "hasPermission:" + hasPermission);
            LogUtil.i(TAG, "hasPermission:" + hasPermission);
            if(!hasPermission) {
                LogUtil.i(TAG, "no permission will requestPermission:");
                mUsbManager.requestPermission(usbDevice, pi);
                Util.showToast(mContext, "requestPermission");
            }
        } else if(action.equals(UsbManager.ACTION_USB_STATE)) {
            boolean connected = intent.getBooleanExtra(UsbManager.USB_CONNECTED, false);
            boolean massStorage = intent.getBooleanExtra(UsbManager.USB_FUNCTION_MASS_STORAGE, false);
            LogUtil.i(TAG, "connected:" + connected + " massStorage:" + massStorage);
            Util.showToast(mContext, "connected:" + connected + " massStorage:" + massStorage);
        }
    }
}

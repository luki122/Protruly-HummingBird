package com.protruly.powermanager.powersave.lowpowermode.operation;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.provider.Settings;

import com.protruly.powermanager.utils.Utils;

/**
 * Bluetooth Power Save Operation.
 */
public class BluetoothPowerSaveOperation implements PowerSaveOperation {
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mBluetoothOriginalState;

    @Override
    public void init(int state) {
        mBluetoothOriginalState = state > 0;
    }

    @Override
    public int enabled(Context context) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            mBluetoothOriginalState = getCurrentBtState();
            if (mBluetoothOriginalState) {
                mBluetoothAdapter.disable();
            }
        }
        return mBluetoothOriginalState ? 1 : 0;
    }

    @Override
    public void disabled(Context context) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            boolean currentBluetoothState = getCurrentBtState();
            if (mBluetoothOriginalState != currentBluetoothState) {
                setBluetoothEnabled(context, currentBluetoothState || mBluetoothOriginalState);
            }
        }
    }

    private boolean getCurrentBtState() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            int bluetoothState = mBluetoothAdapter.getState();
            return bluetoothState == BluetoothAdapter.STATE_ON;
        }
        return mBluetoothOriginalState;
    }

    private boolean setBluetoothEnabled(Context context, boolean isChecked) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (isChecked &&
                !Utils.isRadioAllowed(context, Settings.Global.RADIO_BLUETOOTH)) {
            return false;
        }
        boolean setState;
        if (isChecked) {
            setState = mBluetoothAdapter.enable();
        } else {
            setState = mBluetoothAdapter.disable();
        }
        return setState;
    }
}
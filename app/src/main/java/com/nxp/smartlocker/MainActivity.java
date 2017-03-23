package com.nxp.smartlocker;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;


public class MainActivity extends Activity {

    // TAG for activity
    private static final String TAG = MainActivity.class.getSimpleName();

    private BluetoothManager mBTManager; // BT manager for getting adapter and open GATT server
    private BluetoothAdapter mBTAdapter; // BT Adapter
    private boolean mStartEnabled; // BT is enabled before us?

    // Smartlocker BLE peripheral instance
    private SmartLockerBLEPeripheral mLockerBLEPerh;
    private SmartLocker mSmartLocker;

    private IntentFilter mBtFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");

        // We have BLE support?
        if (!getApplicationContext().getPackageManager().
                hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e(TAG, "No BLE support on this platform!");
            finish(); // call onDestroy()
        }

        // Get Bluetooth manager
        mBTManager = (BluetoothManager)getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        if (null == mBTManager) {
            Log.e(TAG, "Failed to get BluetoothManager!");
            finish();
        }
        // Get Bluetooth Adapter
        mBTAdapter = mBTManager.getAdapter();
        if (null == mBTAdapter) {
            Log.e(TAG, "BLE Peripheral already started!");
            finish();
        }

        mSmartLocker = new SmartLocker();

        // Instance the SmartLockerBLEPeripheral
        mLockerBLEPerh = new SmartLockerBLEPeripheral(mBTAdapter, mPerhCallback);

        // check if the BT is already enable?
        if (mBTAdapter.isEnabled()) {
            Log.d(TAG, "Bluetooth is already enabled.");
            mLockerBLEPerh.initPeripheral(mBTManager, getApplicationContext());
            mLockerBLEPerh.startAdvertise();
            mStartEnabled = true;
        } else {
            Log.d(TAG, "Bluetooth is disabled, enable it manually");
            // Register for broadcasts on BluetoothAdapter state change
            mBtFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mReceiver, mBtFilter);
            // Enable bt manually
            if (!mBTAdapter.enable()) {
                Log.e(TAG, "Failed to enable BT!");
                finish();
            }
            mStartEnabled = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy");

        if (null != mSmartLocker) {
            // lock it when we quit
            mSmartLocker.lock();
        }

        if (null != mBtFilter) {
            // Unregister the broadcasts receiver
            unregisterReceiver(mReceiver);
        }

        if (null != mLockerBLEPerh) {
            // stop advertise and close connection
            mLockerBLEPerh.stopAdvertise();
            mLockerBLEPerh.closeConnection();
        }

        if (!mStartEnabled) {
            mBTAdapter.disable();
        }
    }

    private BLEPeripheralCallback mPerhCallback = new BLEPeripheralCallback() {
        @Override
        public void ConnectionStateChange(String device, int state, int newState) {

        }

        @Override
        public void AdvertiseChange(boolean isAdvertising) {

        }

        // read the SmartLocker status, and fill the characteristic
        @Override
        public int ReadLockerStatus() {

            if (mSmartLocker.isLocked()) {
                return 0;
            } else {
                return 1;
            }
        }

        // write the locker status
        @Override
        public void WriteLockerStatus(int ctrlStatus) {

            if (0 == ctrlStatus) {
                mSmartLocker.lock();
            } else {
                mSmartLocker.unlock();
            }
        }
    };

    // Bluetooth Adapter ACTION_STATE_CHANGED event receiver
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                                     BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "BT turned off");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "BT turning off");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "BT turned on");
                        // Init the BLE peripheral
                        mLockerBLEPerh.initPeripheral(mBTManager, getApplicationContext());
                        // Start advertise
                        mLockerBLEPerh.startAdvertise();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "BT turning on");
                        break;
                }
            }
        }
    };
}

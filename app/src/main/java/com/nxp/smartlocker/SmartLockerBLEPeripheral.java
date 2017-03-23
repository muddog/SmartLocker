package com.nxp.smartlocker;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.UUID;

/**
 * BLE Peripheral class
 * to handle the BLE GATT server, and it's connection.
 * All of the characteristic changes are handled in the GATTServer callback
 */

class SmartLockerBLEPeripheral {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String SMARTLOCK_SERVICE_UUID128 = "bc846c60-701c-11e6-8a96-00059a3c7a00";
    private static final String SMARTLOCK_CHARACTERISTIC_CONTROL_UUID128 = "bc846c61-701c-11e6-8a96-00059a3c7a00";
    private static final String SMARTLOCK_CHARACTERISTIC_STATUS_UUID128 = "bc846c62-701c-11e6-8a96-00059a3c7a00";

    // BT manager, adapter and GATT service/server

    private BluetoothAdapter mBTAdapter; // BT adapter for advertise and data
    private ArrayList<BluetoothDevice> mBtClients; // Represents a remote Bluetooth device
    private BluetoothGattServer mGattServer;
    private BluetoothGattService mGattService;
    private BLEPeripheralCallback mPeripheralCallback;

    // Advertisers
    private BluetoothLeAdvertiser mBLEAdvertiser;
    private AdvertiseSettings.Builder mAdvSettingsBuilder;
    private AdvertiseData.Builder mAdvDataBuilder;

    // Advertise callback
    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(TAG, "AdvertiseCallback: onStart ok");
            mPeripheralCallback.AdvertiseChange(true);
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.d(TAG, "AdvertiseCallback: onStart fail:" + String.valueOf(errorCode));
            mPeripheralCallback.AdvertiseChange(false);
        }
    };

    // Construction
    SmartLockerBLEPeripheral(BluetoothAdapter btAdapter, BLEPeripheralCallback callback) {

        mBTAdapter = btAdapter;
        mPeripheralCallback = callback;
        mBtClients = new ArrayList<>();
    }

    boolean initPeripheral(BluetoothManager btManager, Context context) {

        if (null == btManager) {
            Log.e(TAG, "No BluetoothManager");
            return false;
        }

        // create a new service for GATT server
        mGattService = new BluetoothGattService(UUID.fromString(SMARTLOCK_SERVICE_UUID128),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // create characteristics for locker control
        BluetoothGattCharacteristic lockCtrlChar = new BluetoothGattCharacteristic(
                UUID.fromString(SMARTLOCK_CHARACTERISTIC_CONTROL_UUID128),
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE | BluetoothGattCharacteristic.FORMAT_UINT8,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        lockCtrlChar.setValue(String.valueOf(0).getBytes());

        // create characteristics for locker status
        BluetoothGattCharacteristic lockStatusChar = new BluetoothGattCharacteristic(
                UUID.fromString(SMARTLOCK_CHARACTERISTIC_STATUS_UUID128),
                BluetoothGattCharacteristic.PROPERTY_NOTIFY |
                        BluetoothGattCharacteristic.PROPERTY_READ |
                        BluetoothGattCharacteristic.FORMAT_UINT8,
                BluetoothGattCharacteristic.PERMISSION_READ);
        lockStatusChar.setValue(String.valueOf(0).getBytes());

        // Add the characteristic into GATT service
        mGattService.addCharacteristic(lockCtrlChar);
        mGattService.addCharacteristic(lockStatusChar);

        // Start the GATT server
        mGattServer = btManager.openGattServer(context, gattServerCallback);
        mGattServer.addService(mGattService);

        // advertise init
        mAdvDataBuilder = new AdvertiseData.Builder();
        mBTAdapter.setName("NXP_SLK");
        mAdvDataBuilder.setIncludeDeviceName(true);
        mAdvDataBuilder.setIncludeTxPowerLevel(false);
        mAdvDataBuilder.addServiceUuid(new ParcelUuid(UUID.fromString(SMARTLOCK_SERVICE_UUID128)));

        mAdvSettingsBuilder = new AdvertiseSettings.Builder();
        mAdvSettingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
        mAdvSettingsBuilder.setConnectable(true);
        mAdvSettingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);

        return true;
    }

    boolean startAdvertise() {

        if (null != mBLEAdvertiser) {
            Log.e(TAG, "Advertise already started");
            return false;
        }
        // Create the GATT service and related characteristics
        // start advertise
        mBLEAdvertiser = mBTAdapter.getBluetoothLeAdvertiser();
        if (null == mBLEAdvertiser) {
            Log.e(TAG, "Failed to get BLE Advertiser");
            return false;
        }

        mBLEAdvertiser.startAdvertising(mAdvSettingsBuilder.build(),
                mAdvDataBuilder.build(), advertiseCallback);

        return true;
    }

    void stopAdvertise() {

        if (null != mBLEAdvertiser) { mBLEAdvertiser.stopAdvertising(advertiseCallback); }

        mBLEAdvertiser = null;
    }

    // Bluetooth Gatt Server callback
    private BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mBtClients.add(device);
                Log.d(TAG, "A new BLE device connected:" + device.getAddress());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mBtClients.remove(device);
                Log.d(TAG, "BLE device disconnected:" + device.getAddress());
            }
            Log.v(TAG, "onConnectionStateChange status=" + status + "->" + newState);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId,
                                                int offset, BluetoothGattCharacteristic characteristic) {

            Log.d(TAG, "onCharacteristicReadRequest " + characteristic.getUuid().toString());

            if (0 == characteristic.getUuid().compareTo(UUID.fromString(SMARTLOCK_CHARACTERISTIC_STATUS_UUID128))) {

                byte[] value = new byte[1];
                value[0] = (byte)mPeripheralCallback.ReadLockerStatus();
                // get current locker status, and update the characteristic
                characteristic.setValue(value);
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                        offset, characteristic.getValue());
                Log.d(TAG, "Send response(read) to client:" + device.getAddress());
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                 BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite, boolean responseNeeded,
                                                 int offset, byte[] value) {
            Log.d(TAG, "onCharacteristicWriteRequest " + characteristic.getUuid().toString());

            if (0 == characteristic.getUuid().compareTo(UUID.fromString(SMARTLOCK_CHARACTERISTIC_CONTROL_UUID128))) {

                Log.d(TAG, "we are here");
                // callback to operate the SmartLocker
                mPeripheralCallback.WriteLockerStatus(value[0]);

                BluetoothGattCharacteristic statusChara =
                        mGattService.getCharacteristic(UUID.fromString(SMARTLOCK_CHARACTERISTIC_STATUS_UUID128));

                // change the status characteristic value
                statusChara.setValue(value);
                // send notification to all clients about the change
                for (BluetoothDevice btClient : mBtClients) {
                    mGattServer.notifyCharacteristicChanged(btClient, statusChara, false);
                    Log.d(TAG, "Send notification to client:" + device.getAddress());
                }
            }
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            super.onExecuteWrite(device, requestId, execute);
        }
    };

    void closeConnection() {

        // Disconnected all the clients
        for (BluetoothDevice btDev : mBtClients) {
            mGattServer.cancelConnection(btDev);
        }
    }
}

package com.nxp.smartlocker;


/**
 * BLEPeripheral Callback to connect the MainActivity
 * to the SmartLockerBLEPeriperhal
 */

public interface BLEPeripheralCallback {

    void ConnectionStateChange(String device, int state, int newState);

    void AdvertiseChange(boolean isAdvertising);

    int ReadLockerStatus();

    void WriteLockerStatus(int ctrlStatus);
}

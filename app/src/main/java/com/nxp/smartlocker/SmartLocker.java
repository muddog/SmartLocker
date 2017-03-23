package com.nxp.smartlocker;

import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

/**
 * SmartLocker class
 * Handle the GPIO to control locker
 */

class SmartLocker {

    private static final String TAG = "SmartLocker";
    // GPIO name used for Locker control
    private static final String LOCKER_CTRL_GPIOA = "GPIO4_IO22";
    private static final String LOCKER_CTRL_GPIOB = "GPIO4_IO23";
    // GPIO pin instance
    private Gpio mLockerGpios[];
    private boolean mLocked;

    public SmartLocker() {

        mLockerGpios = new Gpio[2];
        PeripheralManagerService pioService = new PeripheralManagerService();
        try {
            // Open the GPIO, and set direction
            mLockerGpios[0] = pioService.openGpio(LOCKER_CTRL_GPIOA);
            mLockerGpios[0].setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
            mLockerGpios[0].setActiveType(Gpio.ACTIVE_HIGH);
            mLockerGpios[1] = pioService.openGpio(LOCKER_CTRL_GPIOB);
            mLockerGpios[1].setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mLockerGpios[1].setActiveType(Gpio.ACTIVE_HIGH);
        } catch (IOException e) {
            Log.e(TAG, "Error on opening the GPIO pin", e);
        }

        // lock by default
        lock();

    }

    public void lock() {

        Log.d(TAG, "lock it");

        try {
            mLockerGpios[0].setValue(true);
            mLockerGpios[1].setValue(false);
        } catch (IOException e) {
            Log.e(TAG, "Error on toggle GPIO pin", e);
        }
        mLocked = true;
    }

    public void unlock() {

        Log.d(TAG, "unlock it");

        try {
            mLockerGpios[0].setValue(false);
            mLockerGpios[1].setValue(true);
        } catch (IOException e) {
            Log.e(TAG, "Error on toggle GPIO pin", e);
        }
        mLocked = false;
    }

    public boolean isLocked() {
        Log.d(TAG, "isLocked called");
        return mLocked;
    }

    protected void finalize() {

        try {
            mLockerGpios[0].close();
            mLockerGpios[1].close();
        } catch (IOException e) {
            Log.e(TAG, "Error on closing the GPIO pin", e);
        }

    }
}

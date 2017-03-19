package com.nxp.smartlocker;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

public class MainActivity extends Activity {

    // TAG for activity
    private static final String TAG = MainActivity.class.getSimpleName();
    // GPIO name used for Locker control
    private static final String LOCKER_CTRL_GPIO = "GPIO4_IO20"; // TODO: fix the name
    // GPIO pin instance
    private Gpio mLockerGpio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PeripheralManagerService pioService = new PeripheralManagerService();
        try {
            // Open the GPIO, and set direction
            mLockerGpio = pioService.openGpio(LOCKER_CTRL_GPIO);
            mLockerGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);

        } catch (IOException e) {
            Log.e(TAG, "Error on opening the GPIO pin", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLockerGpio != null) {
            try {
                mLockerGpio.close();
            } catch (IOException e) {
                Log.e(TAG, "Error on closing the GPIO pin", e);
            } finally {
                mLockerGpio = null;
            }
        }
    }
}

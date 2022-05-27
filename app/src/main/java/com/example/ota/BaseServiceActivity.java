/**
 * Copyright 2016 Freescale Semiconductors, Inc.
 */
package com.example.ota;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.ota.service.BLEService;
import com.example.ota.utils.BLEAttributes;
import com.example.ota.utils.BLEConverter;
import com.example.ota.utils.BLEStateEvent;

import de.greenrobot.event.EventBus;

public class BaseServiceActivity extends BaseActivity {

    /**
     * Requested application should pass MAC Address of BLE device here.
     */
    public static final String INTENT_KEY_ADDRESS = "intent.key.address";
    public static final String INTENT_KEY_NAME = "intent.key.name";
    public boolean isShowMenuOption;


    protected String mDeviceAddress;
    protected final Handler mHandler = new Handler();

    /**
     * Preodically check for battery value.
     */
    private final Runnable mBatteryRunner = new Runnable() {

        @Override
        public void run() {
            invokeBatteryCheck();
        }
    };

    public void onEventMainThread(BLEStateEvent.BluetoothStateChanged e) {
    }

    /**
     * Update connection state to Connected in main thread. Subclasses can override this method to update any other UI part.
     *
     * @param e
     */
    public void onEventMainThread(BLEStateEvent.Connected e) {
    }

    /**
     * Update connection state to Connecting in main thread. Subclasses can override this method to update any other UI part.
     *
     * @param e
     */
    public void onEventMainThread(BLEStateEvent.Connecting e) {
    }

    /**
     * Update connection state to Disconnected in main thread. Subclasses can override this method to update any other UI part.
     * Subclasses MUST override this method if they want to clear current values when BLE device is disconnected.
     *
     * @param e
     */
    public void onEventMainThread(BLEStateEvent.Disconnected e) {
    }

    /**
     * Whenever all services inside BLE device are discovered, this event will be fire.
     * Subclasses MUST override this method and can call any required requests to get data from here.
     * Note that this method is called in BLE thread. Invoking UI actions need to be done in main thread.
     *
     * @param e
     */
    public void onEvent(BLEStateEvent.ServiceDiscovered e) {
    }

    /**
     * BLE device has sent us a data package. Parse it and extract assigned number to do action if needed.
     * Subclasses MUST override this method to handle corresponding data.
     *
     * @param e
     */
    public void onEventMainThread(BLEStateEvent.DataAvailable e) {
        updateBatteryInfo(e.characteristic);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
            return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isShowMenuOption = true;
        EventBus.getDefault().register(this);
//        mDeviceAddress = getIntent().getStringExtra(INTENT_KEY_ADDRESS);
//        mDeviceAddress=mac_addr;
//        if (TextUtils.isEmpty(mDeviceAddress)) {
//            throw new NullPointerException("Invalid Bluetooth MAC Address");
//        }
        // automatically perform connection request
        BLEService.INSTANCE.init(getApplicationContext());
//        toggleState(true);
    }
public void setmDeviceAddress(String mac){
        mDeviceAddress=mac;
}
    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * Automatically clear and unregister event handler.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        BLEService.INSTANCE.disconnect();
    }

    /**
     * Connect, or disconnect from BLE device.
     *
     * @param connected
     */
    protected void toggleState(boolean connected) {
        if (connected) {
            if (BLEService.INSTANCE.isBluetoothAvailable()) {
                BLEService.INSTANCE.connect(mDeviceAddress, needUartSupport());
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        } else {
            BLEService.INSTANCE.disconnect();
        }
    }

    /**
     * Update battery info with a piece of data. Will need to check for assigned number first.
     *
     * @param characteristic
     */
    protected void updateBatteryInfo(@NonNull BluetoothGattCharacteristic characteristic) {
        int assignedNumber = BLEConverter.getAssignedNumber(characteristic.getUuid());
    }

    protected void invokeBatteryCheck() {

    }

    protected boolean needUartSupport() {
        return false;
    }
}

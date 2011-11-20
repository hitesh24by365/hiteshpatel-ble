/************************************************************************************
 *
 *  Copyright (C) 2009-2011 Broadcom Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ************************************************************************************/
package com.broadcom.bt.BloodPressure;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;

import com.broadcom.bt.le.api.BleClientProfile;
import com.broadcom.bt.le.api.BleClientService;
import com.broadcom.bt.le.api.BleGattID;

public class BloodPressureProfileClient extends BleClientProfile {
    private static String TAG = "BloodPressureProfileClient";

    public static final String BLOODPRESSURE_REGISTERED = "com.broadcom.action.bloodpressure_registered";
    public static final String BLOODPRESSURE_CONNECTED = "com.broadcom.action.bloodpressure_connected";
    public static final String BLOODPRESSURE_DISCONNECTED = "com.broadcom.action.bloodpressure_disconnected";

    public final static int ALERT_LEVEL_NONE = 0;
    public final static int ALERT_LEVEL_LOW = 1;
    public final static int ALERT_LEVEL_HIGH = 2;

    private static final String PROFILE_UUID = "00001810-1112-2223-8000-00805f9b34fb";
    private static final int BLOOD_PRESSURE_CHARACTERISTIC_UUID = 0x2a35;

    private BloodPressureServiceClient mBloodPressureService = null;
    private Context mContext = null;

    public BloodPressureProfileClient(Context context) {
        super(context, new BleGattID(PROFILE_UUID));
        mContext = context;

        Log.d(TAG, "Constructor...");

        mBloodPressureService = new BloodPressureServiceClient(context);
        
        ArrayList<BleClientService> services = new ArrayList<BleClientService>();
        services.add(mBloodPressureService);

        init(services, null);
    }

    public synchronized void deregister() throws InterruptedException {
        deregisterProfile();
        wait(5000);
    }

    public void onInitialized(boolean success) {
        Log.d(TAG, "onInitialized");
        if (success) {
            registerProfile();
        }
    }

    public void onDeviceConnected(BluetoothDevice device) {
        Log.d(TAG, "onDeviceConnected");

        mBloodPressureService.registerForNotification(device, 0, new BleGattID(BLOOD_PRESSURE_CHARACTERISTIC_UUID));

        refresh(device);
    }

    public void onDeviceDisconnected(BluetoothDevice device) {
        Log.d(TAG, "onDeviceDisconnected");

        mBloodPressureService.unregisterNotification(device, 0, new BleGattID(BLOOD_PRESSURE_CHARACTERISTIC_UUID));
        
        Intent intent = new Intent();
        intent.setAction(BLOODPRESSURE_DISCONNECTED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device.getAddress());
        mContext.sendBroadcast(intent);

        connectBackground(device);
    }

    public void onRefreshed(BluetoothDevice device) {
        Log.d(TAG, "onRefreshed");

        Intent intent = new Intent();
        intent.setAction(BLOODPRESSURE_CONNECTED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device.getAddress());
        mContext.sendBroadcast(intent);
    }

    public void onProfileRegistered() {
        Log.d(TAG, "onProfileRegistered");
        Intent intent = new Intent();
        intent.setAction(BLOODPRESSURE_REGISTERED);
        mContext.sendBroadcast(intent);
    }

    public void onProfileDeregistered() {
        Log.d(TAG, "onProfileDeregistered");
        notifyAll();
    }
}

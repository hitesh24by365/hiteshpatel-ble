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
package com.broadcom.bt.ble.proximitymonitor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

import com.broadcom.bt.le.api.BleCharacteristic;
import com.broadcom.bt.le.api.BleClientProfile;
import com.broadcom.bt.le.api.BleClientService;
import com.broadcom.bt.le.api.BleGattID;

/**
 * @hide
 */
public class FindMeProfileClient extends BleClientProfile {
    private static String TAG = "FindMeProfileClient";

    static BleGattID PROFILE_UUID = new BleGattID("00001802-1111-2222-8000-00805f9b34fb");
    private static final int ALERT_LEVEL_CHARACTERISTIC_UUID = 0x2a06;
    
    ImmediateAlertServiceClient mServiceClient = new ImmediateAlertServiceClient();

    public ArrayList<BleClientService> mServices = new ArrayList<BleClientService>();

    private boolean mInitialized = false;

    public final static byte ALERT_LEVEL_NONE = 0;
    public final static byte ALERT_LEVEL_LOW = 1;
    public final static byte ALERT_LEVEL_HIGH = 2;

    public FindMeProfileClient(Context context) {
        super(context, PROFILE_UUID);
        Log.d(TAG, "FindMeProfileClient");
        mServices.add(mServiceClient);
        init(mServices, null);
    }
    
    public void findme(String bdaddr) {
        Log.d(TAG," findme(" + bdaddr + ")");

        if (mInitialized) {
            BluetoothDevice bd = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(bdaddr );
            connect(bd);
        }
    }

    public void alert(BluetoothDevice device) {
        BleCharacteristic alertLevelCharacteristic = 
                mServiceClient.getCharacteristic(device, new BleGattID(ALERT_LEVEL_CHARACTERISTIC_UUID));
        byte[] value = { FindMeProfileClient.ALERT_LEVEL_HIGH };
        alertLevelCharacteristic.setValue(value);
	alertLevelCharacteristic.setWriteType(BleConstants.GATTC_TYPE_WRITE_NO_RSP);
        mServiceClient.writeCharacteristic(device, 0, alertLevelCharacteristic);
    }

    public void onInitialized(boolean success) {
        Log.d(TAG, "onInitialized");
        mInitialized  = true;
    }

    public void onDeviceConnected(BluetoothDevice device) {
        Log.d(TAG, "onDeviceConnected");
        refresh(device);
    }

    public void onDeviceDisconnected(BluetoothDevice device) {
        Log.d(TAG, "onDeviceDisconnected");
    }

    public void onRefreshed(BluetoothDevice device) {
        Log.d(TAG, "onRefreshed");
        alert(device);
    }

    public void onProfileRegistered() {
        Log.d(TAG, "onProfileRegistered");
    }

    public void onProfileDeregistered() {
        Log.d(TAG, "onProfileDeregistered");
    }
}

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
import android.util.Log;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;

import com.broadcom.bt.le.api.BleCharacteristic;
import com.broadcom.bt.le.api.BleGattID;
import com.broadcom.bt.le.api.BleClientProfile;
import com.broadcom.bt.le.api.BleClientService;

/**
 * @hide
 */
public class ProximityProfileClient extends BleClientProfile {

    public static final String PROXIMITY_CONNECT = "com.broadcom.bt.proximityservice.connect";
    public static final String PROXIMITY_DISCONNECT = "com.broadcom.bt.proximityservice.disconnect";
    
    static String TAG = "ProximityProfileClient";

    static BleGattID PROFILE_UUID = new BleGattID("00001803-1111-2222-8000-00805f9b34fb");

    LinkLossServiceClient mServiceClient = new LinkLossServiceClient();

    public ArrayList<BleClientService> mServices = new ArrayList<BleClientService>();

    private LinkLossNotificationManager mLinkLossNotificationManager;

    private LocationUpdater mLocationUpdater;
    
    private Context mContext;
    
    public ProximityProfileClient(Context context) {
        super(context, PROFILE_UUID);
        mContext = context;
        mLocationUpdater = new LocationUpdater(context);
        mLinkLossNotificationManager = new LinkLossNotificationManager(context);

        Log.d(TAG, "ProximityProfileClient");

        mServices.add(mServiceClient);
        init(mServices, null);
    }

    public void onInitialized(boolean success) {
        Log.d(TAG, "onInitialized");
    }

    public void onDeviceConnected(BluetoothDevice device) {
        Log.d(TAG, "onDeviceConnected");
        ProximityReporter pr = ProximityReporter.get(device.getAddress());
        refresh(device);
        if (pr != null) {
            pr.isConnected = true;
            Intent intent = new Intent();
            intent.setAction(PROXIMITY_CONNECT);
            intent.putExtra(BluetoothDevice.EXTRA_DEVICE, pr.address);
            mContext.sendBroadcast(intent);
        }

        mLinkLossNotificationManager.displayNotification(pr, true);
    }

    public void onDeviceDisconnected(BluetoothDevice device) {
        Log.d(TAG, "onDeviceDisconnected");
        Intent intent = new Intent();
        intent.setAction(PROXIMITY_DISCONNECT);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device.getAddress());
        mContext.sendBroadcast(intent);

        ProximityReporter pr = ProximityReporter.get(device.getAddress());
        if (pr != null) {
            pr.isConnected = false;
            pr.clearLocation();
            // if (pr.localLinkLossAlertVibrate || pr.localLinkLossAlertSound)
            mLinkLossNotificationManager.displayNotification(pr, false);
            mLocationUpdater.requestLocationUpdates();
        }
    }

    public void onRefreshed(BluetoothDevice device) {
        Log.d(TAG, "onRefreshed");
        ProximityReporter pr = ProximityReporter.get(device.getAddress());
        if (pr != null)
            writeLinkLossAlertLevel(device, pr.remoteLinkLossAlertLevel);
    }

    public void setLinkLostAlertLevel(ProximityReporter pr) {
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(pr.address);
        if (device != null)
            writeLinkLossAlertLevel(device, pr.remoteLinkLossAlertLevel);
    }
    
    private void writeLinkLossAlertLevel(BluetoothDevice device, byte level) {
        byte[] value = { level }; // ALERT_LEVEL_HIGH / ALERT_LEVEL_NONE;
        BleCharacteristic characteristic = mServices.get(0).getCharacteristic(device, new BleGattID(LinkLossServiceClient.ALERT_LEVEL_CHARACTERISTIC_UUID));
        characteristic.setValue(value);
        mServices.get(0).writeCharacteristic(device, 0, characteristic);
    }

    public void onProfileRegistered() {
        Log.d(TAG, "onProfileRegistered");
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        
        for (ProximityReporter pr: ProximityReporter.getAll()) {
            pr.updateBluetoothDeviceName();
            if (pr.enabled) {
                BluetoothDevice bd = adapter.getRemoteDevice(pr.address);
                if (bd != null && bd.getBondState() == BluetoothDevice.BOND_BONDED) {
                    connectBackground(bd);
                }
            }
        }
    }

    public void onProfileDeregistered() {
        Log.d(TAG, "onProfileDeregistered");
    }
}

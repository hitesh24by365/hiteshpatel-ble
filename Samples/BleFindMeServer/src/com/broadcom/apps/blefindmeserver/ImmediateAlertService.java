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
package com.broadcom.apps.blefindmeserver;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.broadcom.bt.le.api.BleCharacteristic;
import com.broadcom.bt.le.api.BleConstants;
import com.broadcom.bt.le.api.BleGattID;
import com.broadcom.bt.le.api.BleServerService;

public class ImmediateAlertService extends BleServerService {
    public static String TAG = "ImmediateAlertService";

    public static final String FINDME_ALERT = "com.broadcom.action.findme_immediatealert";
    public static final String ALERT_LEVEL = "alert_level";

    private static final String SERVICE_UUID                    = "00001802-0000-1000-8000-00805f9b34fb";
    private static final String ALERT_LEVEL_CHARACTERISTIC_UUID = "00002a06-0000-1000-8000-00805f9b34fb";

    private static final int NUM_HANDLES = 8;

    private Context mContext;

    public ImmediateAlertService(Context context) {
    	super(new BleGattID(SERVICE_UUID), NUM_HANDLES);
        mContext = context;
        Log.d(TAG, "ImmediateAlertService()");
    }

    void addAlertLevelCharacteristic() {
        Log.d(TAG, "addAlertLevelCharacteristic()");
        BleCharacteristic bleChar= new BleCharacteristic(new BleGattID(ALERT_LEVEL_CHARACTERISTIC_UUID));
        bleChar.setProperty((byte) (BleConstants.GATT_CHAR_PROP_BIT_WRITE));
        bleChar.setPermMask(BleConstants.GATT_PERM_WRITE);
        addCharacteristic(bleChar);
    }

    @Override
    public void onCharacteristicAdded(byte status, BleCharacteristic charObj) {
        Log.d(TAG, "onCharacteristicAdded(" + status + ", " + charObj + ")");
    }

    @Override
    public void onCharacteristicWrite(String address, BleCharacteristic charObj) {
        Log.d(TAG, "onCharacteristicWrite(" + address + ", " + charObj + ")");
        byte alertLevel = charObj.getValue()[0];
        setAlertLevel(address, alertLevel);
    }

    @Override
    public void onIncludedServiceAdded(byte status, BleServerService includedService) {
        Log.d(TAG, "onIncludedServiceAdded(" + status + ", " + includedService + ")");
    }

    @Override
    public void onResponseSendCompleted(byte status, BleCharacteristic charObj) {
        Log.d(TAG, "onResponseSendCompleted(" + status + ", " + charObj + ")");
    }

    private void setAlertLevel(String address, byte alertLevel) {
        if (alertLevel != FindMeProfileServer.ALERT_LEVEL_NONE) {
            Intent intent = new Intent();
            intent.setAction(FINDME_ALERT);
            intent.putExtra(BluetoothDevice.EXTRA_DEVICE, address);
            intent.putExtra(ALERT_LEVEL, alertLevel);
            mContext.sendBroadcast(intent);
        }
    }

}

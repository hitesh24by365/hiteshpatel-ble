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
package com.broadcom.apps.bletimeserver;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.text.format.Time;
import android.util.Log;

import com.broadcom.bt.le.api.BleCharacteristic;
import com.broadcom.bt.le.api.BleConstants;
import com.broadcom.bt.le.api.BleGattID;
import com.broadcom.bt.le.api.BleServerService;

public class CurrentTimeService extends BleServerService {
    public static String TAG = "CurrentTimeService";

    private static final String SERVICE_UUID                     = "00001805-0000-1000-8000-00805f9b34fb";
    private static final String CURRENT_TIME_CHARACTERISTIC_UUID = "00002a2b-0000-1000-8000-00805f9b34fb";

    private static final int NUM_HANDLES = 8;

    private Context mContext;

    public CurrentTimeService(Context context) {
        super(new BleGattID(SERVICE_UUID), NUM_HANDLES);
        mContext = context;
        Log.d(TAG, "Constructor()");
    }

    void addCurrentTimeCharacteristic() {
        Log.d(TAG, "addCurrentTimeCharacteristic()");
        BleCharacteristic bleChar= new BleCharacteristic(new BleGattID(CURRENT_TIME_CHARACTERISTIC_UUID));
        bleChar.setProperty((byte) (BleConstants.GATT_CHAR_PROP_BIT_READ | BleConstants.GATT_CHAR_PROP_BIT_NOTIFY));
        bleChar.setPermMask(BleConstants.GATT_PERM_READ);
        addCharacteristic(bleChar);
    }

    @Override
    public void onCharacteristicAdded(byte status, BleCharacteristic charObj) {
        Log.d(TAG, "onCharacteristicAdded(" + status + ", " + charObj + ")");
    }

    @Override
    public void onCharacteristicWrite(String address, BleCharacteristic charObj) {
        Log.d(TAG, "onCharacteristicWrite(" + address + ", " + charObj + ")");
    }

    @Override
    public void onIncludedServiceAdded(byte status, BleServerService includedService) {
        Log.d(TAG, "onIncludedServiceAdded(" + status + ", " + includedService + ")");
    }

    @Override
    public void onResponseSendCompleted(byte status, BleCharacteristic charObj) {
        Log.d(TAG, "onResponseSendCompleted(" + status + ", " + charObj + ")");
    }

    @Override
    public void onCharacteristicRead(java.lang.String address, int transId, int attrHandle, BleCharacteristic charObj) {
        Log.d(TAG, "onCharacteristicRead(" + address + ", " + transId + ", " + attrHandle + ", " + charObj + ")");
        sendResponse(address, transId, getTimeData(), BleConstants.GATT_SUCCESS);
    }
    
    private byte[] getTimeData() {
    	/*
    	 * org.bluetooth.characteristic.current_time
    	 * - Exact Time 256: org.bluetooth.characteristic.exact_time_256
    	 *   - Day Date Time: org.bluetooth.characteristic.day_date_time
    	 *     - Date Time: org.bluetooth.characteristic.date_time
    	 *       - Year: uint16
    	 *       - Month: uint8
    	 *       - Day: uint8
    	 *       - Hours: uint8
    	 *       - Minutes: uint8
    	 *       - Seconds: uint8
    	 *     - Day of Week: org.bluetooth.characteristic.day_of_week
    	 *       - uint8
    	 *   - Fractions256: uint8
    	 * - Adjust Reason: 8bit
    	 */
    	Time currentTime = new Time();
    	
    	BleTimeServerActivity activity = (BleTimeServerActivity) mContext;
    	if (activity.useTestTime()) {
    		currentTime.year = 2011;
    		currentTime.month = 9; // October
    		currentTime.monthDay = 1;
    		currentTime.hour = 12; // noon
    		currentTime.minute = 0;
    		currentTime.second = 0;
    	} else {
    		currentTime.setToNow();
    	}
    	
    	byte[] timeData = new byte[10];
    	timeData[0] = (byte) (currentTime.year % 256);
    	timeData[1] = (byte) (currentTime.year / 256);
    	timeData[2] = (byte) (currentTime.month + 1);
    	timeData[3] = (byte) (currentTime.monthDay);
    	timeData[4] = (byte) (currentTime.hour);
    	timeData[5] = (byte) (currentTime.minute);
    	timeData[6] = (byte) (currentTime.second);
    	timeData[7] = (byte) (currentTime.weekDay + 1);
    	timeData[8] = 0;
    	timeData[9] = 0;
    	
    	return timeData;
    }

}

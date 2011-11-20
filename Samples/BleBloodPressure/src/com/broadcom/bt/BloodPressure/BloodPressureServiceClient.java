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

import java.util.ArrayList;
import java.util.Iterator;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.broadcom.bt.le.api.BleClientService;
import com.broadcom.bt.le.api.BleCharacteristic;
import com.broadcom.bt.le.api.BleConstants;
import com.broadcom.bt.le.api.BleDescriptor;
import com.broadcom.bt.le.api.BleGattID;

public class BloodPressureServiceClient extends BleClientService {
    public static String TAG = "BloodPressureServiceClient";

    final static int SERVICE_UUID = 0x1810;

    public static final String BLOODPRESSURE_MEASUREMENT = "com.broadcom.action.bloodpressure_measurement";
    public static final String EXTRA_SYSTOLIC = "extra_systolic";
    public static final String EXTRA_DIASTOLIC = "extra_diastolic";
    public static final String EXTRA_MEAN = "extra_map";
    public static final String EXTRA_PULSE = "extra_map";
    
    private Context mContext = null;
    
    public BloodPressureServiceClient(Context context) {
        super(new BleGattID(SERVICE_UUID));
        Log.d(TAG, "ImmediateAlertServiceClient");
        mContext = context;
    }

    public void onWriteCharacteristicComplete(int status, BluetoothDevice d,
            BleCharacteristic characteristic) {
        Log.d(TAG, "onWriteCharacteristicComplete");
    }

    public void characteristicsRetrieved(BluetoothDevice d) {
        Log.d(TAG, "characteristicsRetrieved");
    }

    public void onRefreshComplete(BluetoothDevice d) {
        Log.d(TAG, "onRefreshComplete");
                
        ArrayList<BleCharacteristic> characteristics = getAllCharacteristics(d);
        Log.d(TAG, "Characteristics: " + characteristics.size());
        
        Iterator<BleCharacteristic> it = characteristics.iterator();
        while(it.hasNext()) {
        	BleCharacteristic characteristic = it.next();
        	Log.d(TAG, "Characteristic: " + characteristic.getID().toString());
        		
        	byte[] value = { 0x02, 0x00 };
        	BleDescriptor clientConfig = characteristic.getDescriptor(new BleGattID(BleConstants.GATT_UUID_CHAR_CLIENT_CONFIG16));
        	clientConfig.setValue(value);

        	clientConfig.setWriteType(BleConstants.GATTC_TYPE_WRITE_NO_RSP);
        	writeCharacteristic(d, 0, characteristic);
        }
    }

    public void onSetCharacteristicAuthRequirement(BluetoothDevice d,
            BleCharacteristic characteristic, int instanceID) {
        Log.d(TAG, "onSetCharacteristicAuthRequirement");
    }

    public void onReadCharacteristicComplete(BluetoothDevice d, BleCharacteristic characteristic) {
        Log.d(TAG, "refreshOneCharacteristicComplete");
    }

    public void onCharacteristicChanged(BluetoothDevice d, BleCharacteristic characteristic) {
        byte[] data = characteristic.getValue();
        Log.d(TAG, "onCharacteristicChanged - char = " + characteristic.getID().toString() +	 " + value len = " + data.length + ", 1st byte = " + data[0]);
        
        Intent intent = new Intent();
        intent.setAction(BLOODPRESSURE_MEASUREMENT);
        intent.putExtra(EXTRA_SYSTOLIC, unsignedByteToInt(data[1]));
        intent.putExtra(EXTRA_DIASTOLIC, unsignedByteToInt(data[3]));
        intent.putExtra(EXTRA_MEAN, unsignedByteToInt(data[5]));
        intent.putExtra(EXTRA_PULSE, unsignedByteToInt(data[7]));
        mContext.sendBroadcast(intent);
    }
    
    public static int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
	}
}

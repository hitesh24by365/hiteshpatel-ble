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

import android.util.Log;

import java.util.UUID;
import java.util.ArrayList;

import com.broadcom.bt.le.api.BleClientService;
import com.broadcom.bt.le.api.BleConstants;
import com.broadcom.bt.le.api.BleGattID;

/**
 * @hide
 */
public class ImmediateAlertServiceClient extends BleClientService {

    public static String TAG = "ImmediateAlertServiceClient";
    private static final String SERVICE_UUID = "00001802-0000-1000-8000-00805f9b34fb";
    private static final String ALERT_LEVEL_CHARACTERISTIC_UUID = "00002a06-0000-1000-8000-00805f9b34fb";

    private ArrayList<UUID> mRequiredUuid = new ArrayList<UUID>();
    private int[] mCharInstanceID;
    private byte[] mAuthReq;

    public ImmediateAlertServiceClient() {
        super(new BleGattID(SERVICE_UUID));

        Log.d(TAG, "ImmediateAlertServiceClient");
        /*
         * The following uuids are required
         * 0x2A06 - Alert Level
         */
        mRequiredUuid.add(UUID.fromString(ALERT_LEVEL_CHARACTERISTIC_UUID));
        mCharInstanceID = new int[mRequiredUuid.size()];
        mCharInstanceID[0] = 0;

        mAuthReq = new byte[mRequiredUuid.size()];
        mAuthReq[0] = BleConstants.GATT_AUTH_REQ_NONE;
    }

}

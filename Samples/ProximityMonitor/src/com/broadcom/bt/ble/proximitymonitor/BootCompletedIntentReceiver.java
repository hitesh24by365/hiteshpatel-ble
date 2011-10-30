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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootCompletedIntentReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompletedIntentReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
            int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            Log.d(TAG, "Received state change = " + btState);
            if (btState == BluetoothAdapter.STATE_ON) {
                context.startService(new Intent(context, ProximityService.class));
            }
            if (btState == BluetoothAdapter.STATE_TURNING_OFF) {
                context.stopService(new Intent(context, ProximityService.class));
            }
        }
    }
}

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
import android.util.Log;

import java.util.ArrayList;

import com.broadcom.bt.le.api.BleGattID;
import com.broadcom.bt.le.api.BleServerProfile;
import com.broadcom.bt.le.api.BleServerService;

public class TimeProfileServer extends BleServerProfile {
    private static String TAG = "TimeProfileServer";

    public static final String CLIENT_CONNECTED    = "com.broadcom.action.timeprofile_client_connected";
    public static final String CLIENT_DISCONNECTED = "com.broadcom.action.timeprofile_client_disconnected";

    private static final String myUuid = "00001805-1112-2223-8000-00805f9b34fb";

    private ArrayList<BleServerService> mServices;

    private Context mContext = null;

    public static TimeProfileServer createProfile(Context context) {
        CurrentTimeService service = new CurrentTimeService(context);
        ArrayList<BleServerService> services = new ArrayList<BleServerService>();
        services.add(service);
        return new TimeProfileServer(context, services);
    }

    private TimeProfileServer(Context context, ArrayList<BleServerService> services) {
        super(context, new BleGattID(myUuid), services);
        mContext = context;
        mServices = services;
        Log.d(TAG, "Constructor()");
    }

    @Override
    public void onInitialized(boolean initialized) {
        Log.d(TAG, "onInitialized(" + initialized + ")");

        CurrentTimeService timeService = (CurrentTimeService) mServices.get(0);
        timeService.addCurrentTimeCharacteristic();

        startProfile();
    }

    @Override
    public void onClientConnected(String bdaddr, boolean isConnected) {
        Log.d(TAG, "onClientConnected(" + bdaddr + ", " + isConnected + ")");

        Intent intent = new Intent();
        intent.setAction(isConnected ? CLIENT_CONNECTED : CLIENT_DISCONNECTED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, bdaddr);
        mContext.sendBroadcast(intent);
    }

    @Override
    public void onCloseCompleted(int status) {
        Log.d(TAG, "onCloseCompleted(" + status + ")");
    }

    @Override
    public void onOpenCancelCompleted(int status) {
        Log.d(TAG, "onOpenCancelCompleted(" + status + ")");
    }

    @Override
    public void onOpenCompleted(int status) {
        Log.d(TAG, "onOpenCompleted(" + status + ")");
    }

    @Override
    public void onStarted(boolean started) {
        Log.d(TAG, "onStarted(" + started + ")");
    }

    @Override
    public void onStopped() {
        Log.d(TAG, "onStopped()");
    }
}

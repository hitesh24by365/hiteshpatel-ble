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

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;

import java.util.Set;

import com.broadcom.bt.le.api.BleAdapter;

public class ProximityService extends Service {

    public static final String PROXIMITY_INSERT = "com.broadcom.bt.proximityservice.insert";
    public static final String PROXIMITY_UPDATE = "com.broadcom.bt.proximityservice.update";
    public static final String PROXIMITY_DELETE = "com.broadcom.bt.proximityservice.remove";

    static ProximityService instance = null;
    static final String TAG = "ProximityService";

    ProximityProfileClient mProximityProfile = null;
    FindMeProfileClient mFindMeProfile = null;
    private BluetoothAdapter mDefaultAdapter = BluetoothAdapter.getDefaultAdapter();

    public class LocalBinder extends Binder {
        ProximityService getService() {
            return ProximityService.this;
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return binder;
    }

    private final IBinder binder = new LocalBinder();

    private final BroadcastReceiver uuidReceiver = new BroadcastReceiver() {
        public void onReceive(final Context context, final Intent intent) {
            boolean findMeProfile = false, proximityMonitor = false;

            final Bundle b = intent.getExtras();
            if (intent.getAction().equals(BleAdapter.ACTION_UUID)) {
                Parcelable[] uuids = b.getParcelableArray(BleAdapter.EXTRA_UUID);
                BluetoothDevice device = (BluetoothDevice) intent.getExtras().get(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "Got remote services ACTION_UUID for device " + device.getAddress());
                // Check if we recognize the ID
                if ( uuids != null ) {
                    for (int i = 0; i < uuids.length; i++) {
                        ParcelUuid uuid = (ParcelUuid) uuids[i];

                        String uuidStr = uuid.toString().toUpperCase();
                        if (uuidStr == null) {
                            return;
                        }

                        String uuidPrefixStr = uuidStr.substring(0, 8).toUpperCase();
                        int uuidPrefix = -1;
                        try {
                            uuidPrefix = Integer.parseInt(uuidPrefixStr,16);
                        } catch (Exception e) {
                            Log.v(TAG, "Exception in integer parse - unknown uuid " + uuidStr);
                        }

                        switch (uuidPrefix) {
                        case 0x00001801: // GATT
                            break;
                        case 0x00001802: // Immediate alert
                            // FindMe Profile can be executed.
                            findMeProfile = true;
                            break;
                        case 0x00001803: // Link loss
                            proximityMonitor = true;
                            break;
                        default:
                            break;
                        }
                    }

                }
                Log.d(TAG, "Got remote services ACTION_UUID for device " + device.getAddress() + " / " + findMeProfile + " / " + proximityMonitor);

                if (findMeProfile && proximityMonitor) {
                    if (!ProximityReporter.containsKey(device.getAddress())) {
                        createProximityReporter(device.getAddress());
                    }
                }
            }
        }
    };

    private final BroadcastReceiver mBondStateChangedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Log.d(TAG, "mBondStateChangedReceiver.onReceive() device="+device);
            if (device != null) {
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                Log.d(TAG, "... bonded: " + device.getAddress() + " / " + bondState);
                if (bondState == BluetoothDevice.BOND_NONE) {
                    if (ProximityReporter.containsKey(device.getAddress())) {
                        Log.d(TAG, "Device " + device.getAddress() + " unpaired - cancelling background connection" );
                        remove(device, true);
                    }
                } else if (bondState == BluetoothDevice.BOND_BONDED && BleAdapter.getDeviceType(device) == BleAdapter.DEVICE_TYPE_BLE) {
                    Log.d(TAG, "Getting remote services for device " + device.getAddress());
                    mDefaultAdapter.getRemoteServices(device.getAddress());
                }
            }
        }
    };

    @Override
    public void onCreate() {
        instance = this;

        ProximityReporter.initDatabase(this);

        Log.d(TAG,"Initializing Proximity profile" );
        mProximityProfile = new ProximityProfileClient(ProximityService.this.getBaseContext());

        Log.d(TAG,"Initializing FindMe profile" );
        mFindMeProfile = new FindMeProfileClient(ProximityService.this.getBaseContext());

        Log.d(TAG,"Registering receivers");
        registerReceiver(mBondStateChangedReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        registerReceiver(uuidReceiver, new IntentFilter(BleAdapter.ACTION_UUID));
        Log.d(TAG,"Registered receivers");

        checkNewPairedDevices();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() called");

        ProximityReporter.closeDatabase();
        unregisterReceiver(mBondStateChangedReceiver);
        try {
            unregisterReceiver(uuidReceiver);
        } catch (Exception ignore) {
        }

        if (mFindMeProfile != null) {
            mFindMeProfile.finish();
            mFindMeProfile = null;
        }

        if (mProximityProfile != null) {
            mProximityProfile.finish();
            mProximityProfile = null;
        }

        ProximityReporter.closeDatabase();
        instance = null;

        super.onDestroy();
    }
    
    public void onFindmeButtonClick(String bdaddr) {
        mFindMeProfile.findme(bdaddr);
    }
    
    public void onRemoteLinkLossPlayAlertCheckedChanged(ProximityReporter pr, boolean isChecked) {
        pr.remoteLinkLossAlertLevel = isChecked ? FindMeProfileClient.ALERT_LEVEL_HIGH : FindMeProfileClient.ALERT_LEVEL_NONE;
        setLinkLostAlertLevel(pr);
        update(pr, false, true);
    }

    public void onLocalLinkLossAlertSoundCheckedChanged(ProximityReporter pr, boolean isChecked) {
        pr.localLinkLossAlertSound = isChecked;
        update(pr, false, true);
    }

    public void onLocalLinkLossAlertVibrateCheckedChanged(ProximityReporter pr, boolean isChecked) {
        pr.localLinkLossAlertVibrate = isChecked;
        update(pr, false, true);
    }
    
    public void onDeviceEnabledCheckedChanged(ProximityReporter pr, boolean isChecked) {
        pr.enabled = isChecked;
        update(pr, false, true);
        if (isChecked)
            enable(pr);
        else
            disable(pr);
    }

    public void setLinkLostAlertLevel(ProximityReporter pr) {
        mProximityProfile.setLinkLostAlertLevel(pr);
    }

    public void checkNewPairedDevices() {
        Set<BluetoothDevice> bondedDevices = mDefaultAdapter.getBondedDevices();
        for (BluetoothDevice bondedDevice: bondedDevices) {
            if ( (BleAdapter.getDeviceType(bondedDevice) == BleAdapter.DEVICE_TYPE_BLE) && ! ProximityReporter.containsKey(bondedDevice.getAddress())) {
                mDefaultAdapter.getRemoteServices(bondedDevice.getAddress());
            }
        }
    }

    public ProximityReporter createProximityReporter(String bdaddr) {
        ProximityReporter result = new ProximityReporter(bdaddr);
        result.insert();
        BluetoothDevice bd = mDefaultAdapter.getRemoteDevice(bdaddr);
        mProximityProfile.connectBackground(bd);

        Intent intent = new Intent();
        intent.setAction(PROXIMITY_INSERT);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, bdaddr);
        sendBroadcast(intent);

        return result;
    }

    public void update(ProximityReporter pr, boolean broadcast, boolean writeToDatabase) {
        if (writeToDatabase) {
            pr.update();
        }
        if (broadcast) {
            Intent intent = new Intent();
            intent.setAction(PROXIMITY_UPDATE);
            intent.putExtra(BluetoothDevice.EXTRA_DEVICE, pr.address);
            sendBroadcast(intent);
        }
    }

    public void remove(BluetoothDevice device, boolean broadcast) {
        Log.d(TAG, "Remove device " + device.getAddress() + ", mProximityProfile = " + mProximityProfile );
        if (mProximityProfile == null) Log.e(TAG, "mProximityProfile == null");

        ProximityReporter.delete(device.getAddress());

        try {
            mProximityProfile.cancelBackgroundConnection(device);
        } catch (Exception ex) {
            Log.e(TAG, "cancelbackgroundConnection() - " + ex.toString());
        }

        try {
            mProximityProfile.disconnect(device);
        } catch (Exception ex) {
            Log.e(TAG, "disconnect() - " + ex.toString());
        }

        try {
            device.removeBond();
        } catch (Exception ex) {
            Log.e(TAG, "removeBond() - " + ex.toString());
        }

        if (broadcast) {
            Intent intent = new Intent();
            intent.setAction(PROXIMITY_DELETE);
            intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device.getAddress());
            sendBroadcast(intent);
        }
    }

    public void enable(ProximityReporter pr) {
        BluetoothDevice device = mDefaultAdapter.getRemoteDevice(pr.address);
        if (device != null) {
            mProximityProfile.connectBackground(device);
        }
    }

    public void disable(ProximityReporter pr) {
        BluetoothDevice device = mDefaultAdapter.getRemoteDevice(pr.address);
        if (device != null) {
            try {
                mProximityProfile.cancelBackgroundConnection(device);
            } catch (Exception ex) {
                Log.e(TAG, "cancelbackgroundConnection() - " + ex.toString());
            }

            try {
                mProximityProfile.disconnect(device);
            } catch (Exception ex) {
                Log.e(TAG, "disconnect() - " + ex.toString());
            }
        }
    }

}

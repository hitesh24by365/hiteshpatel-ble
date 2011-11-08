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

import java.util.HashSet;
import java.util.Set;

import com.broadcom.apps.bletimeserver.R;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

public class BleTimeServerActivity extends Activity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_SELECT_DEVICE = 2;

    private TimeProfileServer mTime = null;

    private String selectedDevice = null;
    private Set<String> connectedDevices = new HashSet<String>();
    
    private final BroadcastReceiver mTimeClientConnectedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String bdaddr = intent.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
            final String action = intent.getAction();

            runOnUiThread(new Runnable() {
                public void run() {
                    if (action.equals(TimeProfileServer.CLIENT_CONNECTED))
                        clientConnected(bdaddr);
                    else if (action.equals(TimeProfileServer.CLIENT_DISCONNECTED))
                        clientDisconnected(bdaddr);
                }
            });
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        ((Button)findViewById(R.id.btn_select)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mTime != null &&  selectedDevice != null) {
                    mTime.cancelOpen(selectedDevice, false);
                    mTime.close(selectedDevice);
                    selectedDevice = null;
                    updateStatus();
                }

                Intent newIntent = new Intent(BleTimeServerActivity.this, DeviceListActivity.class);
                startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
            }
        });
        
        /* Ensure Bluetooth is enabled */
        BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available - exiting...", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!mBtAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            init();
        }

        /* Register event receivers */
        IntentFilter filter = new IntentFilter();
        filter.addAction(TimeProfileServer.CLIENT_CONNECTED);
        filter.addAction(TimeProfileServer.CLIENT_DISCONNECTED);
        registerReceiver(mTimeClientConnectedReceiver, filter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mTimeClientConnectedReceiver);

        if (mTime != null) {
            if (selectedDevice != null) {
                mTime.cancelOpen(selectedDevice, false);
                mTime.close(selectedDevice);
                selectedDevice = null;
            }
        	mTime.stopProfile();
            mTime.finishProfile();
        }

        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode != Activity.RESULT_OK) {
                finish();
            } else {
                init();
            }
        } else if (requestCode == REQUEST_SELECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                devicePicked(deviceAddress);
            }
        }

    }

//        		mTime.open(BleTimeServerActivity.CASIO_WATCH_BDADDR, false);
//        		mTime.cancelOpen(BleTimeServerActivity.CASIO_WATCH_BDADDR, false);

    private void init() {
        mTime = TimeProfileServer.createProfile(this);
        mTime.startProfile();
    }

    private void clientConnected(String bdaddr) {
    	connectedDevices.add(bdaddr);
    	updateStatus();
    }

    private void clientDisconnected(String bdaddr) {
    	connectedDevices.remove(bdaddr);
    	updateStatus();
    }
    
    private void devicePicked(String bdaddr) {
    	selectedDevice = bdaddr;
    	mTime.open(bdaddr, false);
    	updateStatus();
    }
    
    private void updateStatus() {
    	TextView deviceName =  (TextView) findViewById(R.id.deviceName);
    	TextView statusValue = (TextView) findViewById(R.id.statusValue);
    	
    	if (selectedDevice == null) {
    		deviceName.setText(null);
    		statusValue.setText(null);
    	} else {
    		deviceName.setText(getDeviceName(selectedDevice));
    		boolean isConnected = connectedDevices.contains(selectedDevice);
    		statusValue.setText(getString(isConnected ? R.string.connected : R.string.disconnected));
    	}
    }

    private String getDeviceName(String bdaddr) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null)
            return bdaddr;

        BluetoothDevice device = adapter.getRemoteDevice(bdaddr);

        if (device == null)
            return bdaddr;

        String name = device.getName();

        if (name == null || name.length() == 0)
            return bdaddr;

        return name;
    }

	public boolean useTestTime() {
		CheckBox cb = (CheckBox) findViewById(R.id.use_test_time);
		return cb.isChecked();
	}

}

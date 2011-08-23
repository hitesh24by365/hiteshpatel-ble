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
package com.broadcom.apps.blefindmeclient;

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
import android.widget.TextView;
import android.widget.Toast;

public class BleFindMeClientActivity extends Activity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_SELECT_DEVICE = 2;
   
    private static final int STATE_BT_OFF = 0;
    private static final int STATE_DISCONNECTED = 1;
    private static final int STATE_CONNECTED = 2;
    
    private int mState = STATE_BT_OFF;
    private BluetoothDevice mDevice = null;
    
    private FindMeProfileClient mFindMe = null;
    
    private final BroadcastReceiver mFindMeConnectedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            runOnUiThread(new Runnable() {
                public void run() {
                	onDeviceConnected();
                }
            } );
        }
    };

    private final BroadcastReceiver mFindMeDisconnectedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            runOnUiThread(new Runnable() {
                public void run() {
                	onDeviceDisconnected();
                }
            } );
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
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
        	mState = STATE_DISCONNECTED;
        	mFindMe = new FindMeProfileClient(this);
        }
        
        /* Assign button click handlers */

        ((Button)findViewById(R.id.btn_select)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
        		if (mFindMe != null && mDevice != null) {
        			mFindMe.cancelBackgroundConnection(mDevice);
        		}

                Intent newIntent = new Intent(BleFindMeClientActivity.this, DeviceListActivity.class);
                startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
            }
        });
        
        ((Button)findViewById(R.id.btn_alert)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                writeAlertLevel();
            }
        });
        
        /* Update UI state */
        
        setUiState();
        
        /* Register FindMe event receivers */
        
        registerReceiver(mFindMeConnectedReceiver, new IntentFilter(FindMeProfileClient.FINDME_CONNECTED));    
        registerReceiver(mFindMeDisconnectedReceiver, new IntentFilter(FindMeProfileClient.FINDME_DISCONNECTED));    
    }
    
    @Override
    public void onDestroy() {
        unregisterReceiver(mFindMeConnectedReceiver);
        unregisterReceiver(mFindMeDisconnectedReceiver);
        
    	if (mFindMe != null) {
    		if (mDevice != null) {
    			mFindMe.cancelBackgroundConnection(mDevice);
    		}
    		
    		try {
				mFindMe.deregister();
			} catch (InterruptedException ignored) {
			}
			
    		mFindMe.finish();
    	}
    	
    	super.onDestroy();
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
        	if (resultCode != Activity.RESULT_OK) {
        		finish();
        	} else {
        		mState = STATE_DISCONNECTED;
        		setUiState();
            	mFindMe = new FindMeProfileClient(this);
        	}
        }
        
        if (requestCode == REQUEST_SELECT_DEVICE) {
        	if (resultCode == Activity.RESULT_OK && data != null) {
        		String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
        		mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
        		setUiState();
        		
        		connectDevice();
        	}
        }
    }
    
    private void connectDevice() {
    	if (mFindMe == null || mDevice == null)
    		return;
    	mFindMe.connect(mDevice);
    }
    
    private void onDeviceConnected() {
    	mState = STATE_CONNECTED;
    	setUiState();
    }
    
    private void onDeviceDisconnected() {
    	mState = STATE_DISCONNECTED;
    	setUiState();
    }
    
    private void writeAlertLevel() {
    	if (mState != STATE_CONNECTED || mDevice == null || mFindMe == null) {
    		return;
    	}
    	
    	mFindMe.alert(mDevice);
    }
    
    private void setUiState() {
		findViewById(R.id.btn_select).setEnabled(mState == STATE_DISCONNECTED);
		findViewById(R.id.btn_alert).setEnabled(mState == STATE_CONNECTED);
    	
    	if (mDevice != null) {
    		((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName());
    	}

    	switch(mState) {
    	case STATE_CONNECTED:
    		((TextView) findViewById(R.id.statusValue)).setText(R.string.connected);
    		break;
    	case STATE_DISCONNECTED:
    		((TextView) findViewById(R.id.statusValue)).setText(R.string.disconnected);
    		break;
    	}
    }
}
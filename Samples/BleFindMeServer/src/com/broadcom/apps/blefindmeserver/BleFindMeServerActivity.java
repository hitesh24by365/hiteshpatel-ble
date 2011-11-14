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

import com.broadcom.apps.blefindmeserver.R;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class BleFindMeServerActivity extends Activity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_SELECT_DEVICE = 2;

    private FindMeProfileServer mFindMe = null;

    private ImageView statusImageView;
    private TextView statusTextView;

    private long[] vibratePattern;
    private AnimationDrawable anim;

    private final BroadcastReceiver mFindMeImmediateAlertReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final byte alertLevel = intent.getByteExtra(ImmediateAlertService.ALERT_LEVEL, (byte)0);

            runOnUiThread(new Runnable() {
                public void run() {
                    alertCharacteristicWritten(alertLevel);
                }
            });
        }
    };

    private final BroadcastReceiver mFindMeClientConnectedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String bdaddr = intent.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
            final String action = intent.getAction();

            runOnUiThread(new Runnable() {
                public void run() {
                    if (action.equals(FindMeProfileServer.CLIENT_CONNECTED))
                        clientConnected(bdaddr);
                    else if (action.equals(FindMeProfileServer.CLIENT_DISCONNECTED))
                        clientDisconnected(bdaddr);
                }
            });
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        statusImageView = (ImageView) findViewById(R.id.status_image);
        statusImageView.setImageResource(R.drawable.status_inactive);
        // AnimationDrawables generated dynamically may not know their proper width/height
        Drawable alertDrawable = getResources().getDrawable(R.drawable.status_alert);
        statusImageView.setMinimumWidth(alertDrawable.getIntrinsicWidth());
        statusImageView.setMinimumHeight(alertDrawable.getIntrinsicHeight());

        statusTextView = (TextView) findViewById(R.id.status_text);
        statusTextView.setText(getString(R.string.status_inactive));

        setupVibratePattern();

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

        /* Register FindMe event receivers */
        registerReceiver(mFindMeImmediateAlertReceiver, new IntentFilter(ImmediateAlertService.FINDME_ALERT));
        IntentFilter filter = new IntentFilter();
        filter.addAction(FindMeProfileServer.CLIENT_CONNECTED);
        filter.addAction(FindMeProfileServer.CLIENT_DISCONNECTED);
        registerReceiver(mFindMeClientConnectedReceiver, filter);

        ((Button)findViewById(R.id.btn_select)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent newIntent = new Intent(BleFindMeServerActivity.this, DeviceListActivity.class);
                startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
            }
        });
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mFindMeImmediateAlertReceiver);
        unregisterReceiver(mFindMeClientConnectedReceiver);

        if (mFindMe != null) {
            mFindMe.stopProfile();
            mFindMe.finishProfile();
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
                connectBackgroundDevice(deviceAddress);
            }
        }
    }

    private void connectBackgroundDevice(String deviceAddress) {
        if (mFindMe == null || deviceAddress == null)
            return;
        mFindMe.open(deviceAddress, false);
    }

    private void init() {
        mFindMe = FindMeProfileServer.createProfile(this);
        mFindMe.startProfile();
    }

    private void setupVibratePattern() {
        int dot = 200;
        int short_gap = 150;
        int long_gap = 750;
        long[] pattern = {
            0,  // Start immediately
            dot, short_gap, dot, short_gap, dot,
            long_gap,
            dot, short_gap, dot, short_gap, dot,
            long_gap,
            dot, short_gap, dot, short_gap, dot,
            1
        };
        vibratePattern = pattern;

        Drawable connectedDrawable = getResources().getDrawable(R.drawable.status_connected);
        Drawable alertDrawable     = getResources().getDrawable(R.drawable.status_alert);

        anim = new AnimationDrawable();
        anim.setOneShot(true);
        for (int i = 1; i < pattern.length; i+= 2) {
            anim.addFrame(alertDrawable, (int) pattern[i]);
            anim.addFrame(connectedDrawable,  (int) pattern[i+1]);
        }
    }

    private void clientConnected(String bdaddr) {
        statusImageView.setImageResource(R.drawable.status_connected);
        statusTextView.setText(getString(R.string.status_connected, getDeviceName(bdaddr)));
    }

    private void clientDisconnected(String bdaddr) {
        statusImageView.setImageResource(R.drawable.status_inactive);
        statusTextView.setText(getString(R.string.status_inactive));
    }

    private void alertCharacteristicWritten(byte alertLevel) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        anim.stop();
        statusImageView.setImageDrawable(anim);

        v.vibrate(vibratePattern, -1); // don't repeat
        anim.start();
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
}

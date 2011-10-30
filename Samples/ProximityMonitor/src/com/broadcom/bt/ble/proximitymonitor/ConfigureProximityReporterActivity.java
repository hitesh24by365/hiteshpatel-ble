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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.broadcom.bt.ble.proximitymonitor.R;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

public class ConfigureProximityReporterActivity extends Activity {

    public final static String TAG = "ConfigureProximityReporterActivity";
    public final static int SET_LOCAL_LINK_LOSS_SOUND = 1;
    private final static int DIALOG_NAME = 1;

    String mAddress;
    ProximityService mService;
    ProximityReporter mProximityReporter = null;
    private ServiceConnection mOnService = null;
    private static final char[] DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7',
                                           '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
                                         };

    public static char[] encodeHex(byte[] data) {
        int l = data.length;
        char[] out = new char[l << 1];
        // Two characters form the hex value.
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = DIGITS[(0xF0 & data[i]) >>> 4];
            out[j++] = DIGITS[0x0F & data[i]];
        }
        return out;
    }

    private final BroadcastReceiver proximityStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String device = intent.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
            if (!mAddress.equals(device))
                return; // Not the same device

            if (action.equals(ProximityProfileClient.PROXIMITY_CONNECT)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        // Connected
                        findViewById(R.id.alert) .setVisibility(View.VISIBLE);
                        findViewById(R.id.locate).setVisibility(View.GONE);
                    }
                } );
            }

            if (action.equals(ProximityProfileClient.PROXIMITY_DISCONNECT)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        // Disconnected
                        findViewById(R.id.alert) .setVisibility(View.GONE);
                        findViewById(R.id.locate).setVisibility(View.VISIBLE);
                    }
                } );
            }

        }
    };

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        public void onReceive(final Context context, final Intent intent) {
            if ( intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                        Log.d(TAG, "Received state change = " + btState);

                        if (btState == BluetoothAdapter.STATE_TURNING_OFF) {
                            finish();
                        }
                    }
                } );
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAddress = getIntent().getExtras().getString("address");

        mOnService = new ServiceConnection() {
            public void onServiceConnected(ComponentName className,
            IBinder rawBinder) {
                mService = ((ProximityService.LocalBinder) rawBinder).getService();
                mProximityReporter = ProximityReporter.get(mAddress);
                if (mProximityReporter != null) {
                    init();
                } else {
                    // We should never get here, unless the device was just unpaired and removed from the database
                    Toast.makeText(ConfigureProximityReporterActivity.this, "Device " + mAddress + " not found.", Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            public void onServiceDisconnected(ComponentName classname) {
                mService = null;
            }
        };

        // Start service, if not already running
        startService(new Intent(this, ProximityService.class));

        Intent bindIntent = new Intent(this, ProximityService.class);
        bindService(bindIntent, mOnService, Context.BIND_AUTO_CREATE);

        IntentFilter f = new IntentFilter();
        f.addAction(ProximityProfileClient.PROXIMITY_CONNECT);
        f.addAction(ProximityProfileClient.PROXIMITY_DISCONNECT);
        this.registerReceiver(proximityStatusChangeReceiver, f);
        this.registerReceiver(bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(proximityStatusChangeReceiver);
        } catch (Exception ignore) {
        }

        try {
            unregisterReceiver(bluetoothStateReceiver);
        } catch (Exception ignore) {
        }

        unbindService(mOnService);
        mService = null;

        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.activity_close_enter, R.anim.activity_close_exit);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        return false;
    }

    private void init() {
        this.setContentView(R.layout.configure);
        if (mProximityReporter.nickname == null || mProximityReporter.nickname.length() == 0) {
            ((TextView) findViewById(R.id.name_1)).setText(mProximityReporter.bluetoothDeviceName);
            ((TextView) findViewById(R.id.name_2)).setText(getString(R.string.set_nickname));
        } else {
            ((TextView) findViewById(R.id.name_1)).setText(mProximityReporter.nickname);
            ((TextView) findViewById(R.id.name_2)).setText(mProximityReporter.bluetoothDeviceName);
        }

        findViewById(R.id.name_1).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showDialog(DIALOG_NAME);
            }
        });

        findViewById(R.id.name_2).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showDialog(DIALOG_NAME);
            }
        });

        if (mProximityReporter.isConnected) {
            findViewById(R.id.alert) .setVisibility(View.VISIBLE);
            findViewById(R.id.locate).setVisibility(View.GONE);
        } else {
            findViewById(R.id.alert) .setVisibility(View.GONE);
            findViewById(R.id.locate).setVisibility(View.VISIBLE);
        }

        findViewById(R.id.locate).setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                Intent newIntent = new Intent(ConfigureProximityReporterActivity.this, ProximityMapActivity.class);
                newIntent.putExtra("address", mAddress);
                startActivity(newIntent);
            }
        });

        findViewById(R.id.alert).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "findme clicked");
                mService.onFindmeButtonClick(mAddress);
            }
        });

        CheckBox checkbox = (CheckBox) findViewById(R.id.remote_link_loss_play_alert);
        checkbox.setChecked(mProximityReporter.remoteLinkLossAlertLevel != FindMeProfileClient.ALERT_LEVEL_NONE);
        checkbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
                mService.onRemoteLinkLossPlayAlertCheckedChanged(mProximityReporter,  isChecked);
            }
        });

        checkbox = (CheckBox) findViewById(R.id.local_link_loss_play_sound);
        checkbox.setChecked(mProximityReporter.localLinkLossAlertSound);
        checkbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
                mService.onLocalLinkLossAlertSoundCheckedChanged(mProximityReporter, isChecked);
            }
        });

        checkbox = (CheckBox) findViewById(R.id.local_link_loss_vibrate);
        checkbox.setChecked(mProximityReporter.localLinkLossAlertVibrate);
        checkbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
                mService.onLocalLinkLossAlertVibrateCheckedChanged(mProximityReporter, isChecked);
            }
        });

        checkbox = (CheckBox) findViewById(R.id.device_enabled);
        checkbox.setChecked(mProximityReporter.enabled);
        checkbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
                mService.onDeviceEnabledCheckedChanged(mProximityReporter, isChecked);
            }
        });

        try {
            Ringtone rt = RingtoneManager.getRingtone(this, Uri.parse(mProximityReporter.localLinkLossAlertSoundUri));
            TextView tv = (TextView) findViewById(R.id.local_link_loss_sound_name);
            tv.setText(rt.getTitle(this));
        } catch (Exception ignore) {
        }

        View v = findViewById(R.id.pick_local_link_loss_sound);
        v.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.select_local_link_loss_alert));
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                mProximityReporter.localLinkLossAlertSoundUri != null ? Uri.parse(mProximityReporter.localLinkLossAlertSoundUri) : (Uri) null);
                startActivityForResult(intent, SET_LOCAL_LINK_LOSS_SOUND);
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SET_LOCAL_LINK_LOSS_SOUND && resultCode == RESULT_OK) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                mProximityReporter.localLinkLossAlertSoundUri = uri.toString();
                mService.update(mProximityReporter, false, true);
                try {
                    Ringtone rt = RingtoneManager.getRingtone(this, Uri.parse(mProximityReporter.localLinkLossAlertSoundUri));
                    TextView tv = (TextView) findViewById(R.id.local_link_loss_sound_name);
                    tv.setText(rt.getTitle(this));
                } catch (Exception ignore) {
                }
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_NAME:
            final EditText input = new EditText(this);
            input.setText(mProximityReporter.nickname);
            return new AlertDialog.Builder(
                       ConfigureProximityReporterActivity.this)
                   .setTitle(getString(R.string.set_nickname_dialog_title))
                   .setView(input)
                   .setPositiveButton(
                       android.R.string.ok,
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,
                int whichButton) {
                    String s = input.getText().toString();
                    if (s == null || s.length() == 0)
                        return; // No new name
                    mProximityReporter.nickname = s;
                    ((TextView) findViewById(R.id.name_1)).setText(mProximityReporter.nickname);
                    ((TextView) findViewById(R.id.name_2)).setText(mProximityReporter.bluetoothDeviceName);
                    mService.update(mProximityReporter, true, true);
                }
            }).setNegativeButton(android.R.string.cancel,
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,
                int whichButton) {
                }
            }).create();
        default:
            return null;
        }
    }

}

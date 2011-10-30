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

import com.broadcom.bt.ble.proximitymonitor.R;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.List;

public class ProximityMonitorActivity extends ListActivity {
    protected static final String TAG = "ProximityMonitorActivity";

    private static final int REQUEST_DISCOVER = 1;
    private static final int REQUEST_CONFIGURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    private static final int CONTEXT_MENU_REMOVE = 1;

    List<ProximityReporter> mProximityReporters;
    ProximityReporterAdapter mAdapter;
    ProximityReporter mContextMenuProximityReporter;

    private final BroadcastReceiver proximityStatusChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            runOnUiThread(new Runnable() {
                public void run() {
                    populateProximityReporters();
                }
            } );
        }
    };

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        public void onReceive(final Context context, final Intent intent) {
            if ( intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                        Log.d(TAG, "Bluetooth state change = " + btState);

                        if (btState == BluetoothAdapter.STATE_TURNING_OFF) {
                            Log.d(TAG, "Bluetooth is shutting down - exiting activity...");
                            finish();
                        }
                    }
                } );
            }
        }
    };

    boolean discovery = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mProximityReporters = new ArrayList<ProximityReporter>();
        mAdapter = new ProximityReporterAdapter(ProximityMonitorActivity.this, mProximityReporters);
        setListAdapter(mAdapter);

        // Get the local Bluetooth adapter
        BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!mBtAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            populateProximityReporters();
        }

        registerForContextMenu(getListView());

        IntentFilter f = new IntentFilter();
        f.addAction(ProximityService.PROXIMITY_INSERT);
        f.addAction(ProximityService.PROXIMITY_DELETE);
        f.addAction(ProximityProfileClient.PROXIMITY_CONNECT);
        f.addAction(ProximityProfileClient.PROXIMITY_DISCONNECT);
        this.registerReceiver(proximityStatusChangeReceiver, f);
        this.registerReceiver(bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @Override
    public void onStart() {
        super.onStart();
        populateProximityReporters();
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

        super.onDestroy();
    }

    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        if (position == 0) {
            Intent newIntent = new Intent(ProximityMonitorActivity.this, DeviceListActivity.class);
            startActivityForResult(newIntent, REQUEST_DISCOVER);
        } else if (position > 1) {
            String deviceAddress = mProximityReporters.get(position-2).address;
            Intent newIntent = new Intent(ProximityMonitorActivity.this, ConfigureProximityReporterActivity.class);
            newIntent.putExtra("address", deviceAddress);
            startActivityForResult(newIntent, REQUEST_CONFIGURE);
        }
    }

    private void populateProximityReporters() {
        Log.d(TAG, "populateBondedDevices");
        mProximityReporters.clear();
        for (ProximityReporter reporter : ProximityReporter.getAll()) {
            mProximityReporters.add(reporter);
        }
        onContentChanged();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult " + data);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode != Activity.RESULT_OK) {
                Log.d(TAG, "BT not enabled");
                finish();
            }
        }

        if (requestCode == REQUEST_DISCOVER && data != null) {
            String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
            if (deviceAddress != null) {
                Log.d(TAG, "onActivityResult - getting default services");
                BluetoothAdapter.getDefaultAdapter().getRemoteServices(deviceAddress);
            }
        }

        populateProximityReporters();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.clear();

        try {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            if (info.position > 1) {
                menu.add(0, CONTEXT_MENU_REMOVE, 0, R.string.context_menu_remove);
                mContextMenuProximityReporter = mProximityReporters.get(info.position - 2);
            } else {
                mContextMenuProximityReporter = null;
            }
        } catch (Exception ex) {
            mContextMenuProximityReporter = null;
        }

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == CONTEXT_MENU_REMOVE) {
            Toast.makeText(this, "Removing device: " + mContextMenuProximityReporter.address, Toast.LENGTH_SHORT).show();

            BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = btAdapter.getRemoteDevice(mContextMenuProximityReporter.address);
            device.removeBond();

            return true;
        }
        return false;
    }

    class ProximityReporterAdapter extends BaseAdapter {
        Context context;
        List<ProximityReporter> proximityReporters;
        LayoutInflater inflater;

        public ProximityReporterAdapter(Context context, List<ProximityReporter> proximityReporters) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            this.proximityReporters = proximityReporters;
        }

        public int getCount() {
            return 2 + proximityReporters.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (position == 0) {
                if (convertView == null || convertView.findViewById(R.id.text) == null) {
                    convertView = inflater.inflate(R.layout.element_generic_1textview, null);
                    ((TextView) convertView.findViewById(R.id.text)).setText(getString(R.string.add_a_device));
                    ((ImageView) convertView.findViewById(R.id.icon)).setImageResource(R.drawable.key_add);
                }
            } else if (position == 1) {
                // "Devices" header
                if (convertView == null || convertView.findViewById(R.id.title) == null) {
                    convertView = inflater.inflate(R.layout.element_header_sm_progressbar, null);
                    ((TextView) convertView.findViewById(R.id.title)).setText(context.getString(R.string.devices));
                    convertView.findViewById(R.id.scanning_progress).setVisibility(View.GONE);
                }
            } else {
                if (convertView == null || convertView.findViewById(R.id.text1) == null) {
                    convertView = inflater.inflate(R.layout.element_generic_2textviews, null);
                    holder = new ViewHolder();
                    holder.text1 = (TextView) convertView.findViewById(R.id.text1);
                    holder.text2 = (TextView) convertView.findViewById(R.id.text2);
                    holder.icon = (ImageView) convertView.findViewById(R.id.icon);
                    convertView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }

                ProximityReporter pr = proximityReporters.get(position - 2);
                holder.text1.setText(pr.getName());
                holder.text2.setText(getString(pr.isConnected ? R.string.status_connected: R.string.status_not_connected));
                ((ImageView) convertView.findViewById(R.id.icon)).setImageResource(
                    pr.isConnected ? R.drawable.key_connected : R.drawable.key_disconnected);
            }

            return convertView;
        }
    }

    static class ViewHolder {
        TextView text1, text2;
        ImageView icon;
    }
}

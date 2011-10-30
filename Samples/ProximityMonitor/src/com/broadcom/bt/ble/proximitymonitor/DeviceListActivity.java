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

import java.util.ArrayList;
import java.util.List;

import com.broadcom.bt.ble.proximitymonitor.R;
import com.broadcom.bt.le.api.BleAdapter;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class DeviceListActivity extends Activity {
    // Debugging
    private static final String TAG = "DeviceListActivity";
    private static final boolean D = true;

    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 1;

    // Member fields
    private BluetoothAdapter mBtAdapter;
    List<BluetoothDevice> mUnbondedDevices;
    TextView mEmptyList;

    private UnbondedDeviceAdapter mUnbondedDeviceAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mEmptyList = (TextView) findViewById(R.id.empty);
        mEmptyList.setText("Discovering...");

        Button cancelButton = (Button) findViewById(R.id.button_cancel);
        cancelButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void populateList() {
        mUnbondedDevices = new ArrayList<BluetoothDevice>();
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        mUnbondedDeviceAdapter = new UnbondedDeviceAdapter(this, mUnbondedDevices);
        newDevicesListView.setAdapter(mUnbondedDeviceAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        mBtAdapter.startDiscovery();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (D)
            Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // populateList() will then be called during onActivityResult
        if (!mBtAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            populateList();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        this.unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        try {
            unregisterReceiver(mReceiver);
        } catch (Exception ex) {

        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (D)
            Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so populate the list now
                populateList();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving,
                               Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    // The onClick listener for all devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Log.d(TAG, "onItemClick - Device selected");
            // Cancel discovery because it's costly and we're about to connect
            mBtAdapter.cancelDiscovery();

            Intent result = new Intent();
            Bundle b = new Bundle();
            b.putString(BluetoothDevice.EXTRA_DEVICE, mUnbondedDevices.get(position).getAddress());
            result.putExtras(b);
            setResult(Activity.RESULT_OK, result);

            finish();
        }
    };

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    if ( BleAdapter.getDeviceType(device) == BleAdapter.DEVICE_TYPE_BLE ) {
                        mEmptyList.setVisibility(View.GONE);
                        mUnbondedDevices.add(device);
                        mUnbondedDeviceAdapter.notifyDataSetChanged();
                    }
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);
                if (mUnbondedDevices.size() == 0) {
                    mEmptyList.setText("No devices found");
                }
            }
        }
    };

    class UnbondedDeviceAdapter extends BaseAdapter {
        Context context;
        List<BluetoothDevice> unbondedDevices;
        LayoutInflater inflater;

        public UnbondedDeviceAdapter(Context context, List<BluetoothDevice> unbondedDevices) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            this.unbondedDevices = unbondedDevices;
        }

        public int getCount() {
            return unbondedDevices.size();
        }

        public Object getItem(int position) {
            return unbondedDevices.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewGroup vg;

            if (convertView != null) {
                vg = (ViewGroup) convertView;
            } else {
                vg = (ViewGroup) inflater.inflate(R.layout.unbonded_device_element, null);
            }

            BluetoothDevice device = unbondedDevices.get(position);
            ((TextView) vg.findViewById(R.id.address)).setText(device.getAddress());
            ((TextView) vg.findViewById(R.id.name)).setText(device.getName());

            return vg;
        }
    }
}

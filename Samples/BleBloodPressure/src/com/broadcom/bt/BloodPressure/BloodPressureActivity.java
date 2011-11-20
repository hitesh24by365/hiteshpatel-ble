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
package com.broadcom.bt.BloodPressure;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class BloodPressureActivity extends Activity {
    Dialog mDialog = null;

    private BloodPressureProfileClient mBloodPressure = null;
    private BluetoothDevice mDevice = null;

    private static String BLOOD_PRESSURE_MONITOR_BD_ADDRESS = "20:73:20:00:11:11";

    private final BroadcastReceiver bpmReceiver = new BroadcastReceiver() {
        public void onReceive(final Context context, final Intent intent) {
            ((TextView) findViewById(R.id.sys)).setText(Integer.toString(
            		intent.getIntExtra(BloodPressureServiceClient.EXTRA_SYSTOLIC, 0)));
            ((TextView) findViewById(R.id.dia)).setText(Integer.toString(
               		intent.getIntExtra(BloodPressureServiceClient.EXTRA_DIASTOLIC, 0)));            
            ((TextView) findViewById(R.id.pul)).setText(Integer.toString(
               		intent.getIntExtra(BloodPressureServiceClient.EXTRA_PULSE, 0)));
        }
    };

    private final BroadcastReceiver regReceiver = new BroadcastReceiver() {
        public void onReceive(final Context context, final Intent intent) {
        	mBloodPressure.connectBackground(mDevice);
        }
    };


    @Override
    protected Dialog onCreateDialog(int id) {
        ProgressDialog dialog = new ProgressDialog(this);
        // dialog.setTitle("AND Blood Pressure Demo");
        dialog.setMessage("Reading blood pressure...");
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        
        mDialog = dialog;
        return dialog;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        
        Typeface lcdTypeface = Typeface.createFromAsset(getAssets(),"LCDMN___.TTF");
        ((TextView) findViewById(R.id.sys)).setTypeface(lcdTypeface);
        ((TextView) findViewById(R.id.dia)).setTypeface(lcdTypeface);
        ((TextView) findViewById(R.id.pul)).setTypeface(lcdTypeface);

        ((TextView) findViewById(R.id.device_branding_line1)).setText(Html.fromHtml(getResources().getString(R.string.device_branding_line1)));
        ((TextView) findViewById(R.id.device_branding_line2)).setText(Html.fromHtml(getResources().getString(R.string.device_branding_line2)));
        
        View v = findViewById(R.id.sync);
        v.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                mBloodPressure.connect(mDevice);
            }
        });
        
        registerReceiver(bpmReceiver, new IntentFilter(BloodPressureServiceClient.BLOODPRESSURE_MEASUREMENT));
        registerReceiver(regReceiver, new IntentFilter(BloodPressureProfileClient.BLOODPRESSURE_REGISTERED));
        
        mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(BLOOD_PRESSURE_MONITOR_BD_ADDRESS);
        mBloodPressure = new BloodPressureProfileClient(this);        
    }

    public void onDestroy() {
        try {
            unregisterReceiver(bpmReceiver);
        } catch (Exception ignore) {
        }
        
        try {
            unregisterReceiver(regReceiver);
        } catch (Exception ignore) {
        }
        
    	if (mBloodPressure != null) {
            if (mBloodPressure != null) {
            	mBloodPressure.cancelBackgroundConnection(mDevice);
            }

            try {
            	mBloodPressure.deregister();
            } catch (InterruptedException ignored) {
            }

            mBloodPressure.finish();
        }

        super.onDestroy();
    }    
} 

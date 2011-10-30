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
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.broadcom.bt.ble.proximitymonitor.R;
//import com.google.android.maps.GeoPoint;
//import com.google.android.maps.MapController;
//import com.google.android.maps.MapView;

import java.util.Date;
import java.text.DateFormat;

import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.net.http.SslError;
import android.graphics.Bitmap;
import android.os.Message;
import android.view.KeyEvent;

public class ProximityMapActivity extends Activity {

    public final static String TAG = "ConfigureProximityReporterActivity";

    private final static int DIALOG_NAME = 1;

    String mAddress;
    ProximityService mService;

    ProximityReporter mProximityReporter = null;

    private ServiceConnection mOnService = null;

    private boolean mNeedToLoadMap = false;

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
                    Toast.makeText(ProximityMapActivity.this, "Device " + mAddress + " not found.", Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            public void onServiceDisconnected(ComponentName classname) {
                mService = null;
            }
        };

        // Start service, if not already running (but it is)
        startService(new Intent(this, ProximityService.class));

        Intent bindIntent = new Intent(this, ProximityService.class);
        bindService(bindIntent, mOnService, Context.BIND_AUTO_CREATE);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(mOnService);
        mService = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.activity_close_enter, R.anim.activity_close_exit);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_NAME:
            final EditText input = new EditText(this);
            input.setText(mProximityReporter.nickname);
            return new AlertDialog.Builder(
                       ProximityMapActivity.this)
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

    private void init() {
        this.setContentView(R.layout.map);

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

        if (mProximityReporter.location != null) {
            TextView timestampText = (TextView) findViewById(R.id.timestamp_text);
            DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);
            timestampText.setText(
                getString(R.string.map_timestamp_prefix)
                + df.format(new Date(mProximityReporter.location.getTime()))
                + getString(R.string.map_timestamp_suffix));
        }

        final WebView webView = (WebView) findViewById(R.id.web);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {

            public void onLoadResource(WebView view, String url) {
                Log.d(TAG, "onLoadResource / " + url);
            }
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "onPageFinished / " + url);
                if (mNeedToLoadMap) {
                    mNeedToLoadMap = false;
                    Log.d(TAG, "loadUrl - lat="+mProximityReporter.location.getLatitude()+"&lon="+mProximityReporter.location.getLongitude());
                    webView.loadUrl("file:///android_asset/map.html?lat="+mProximityReporter.location.getLatitude()+"&lon="+mProximityReporter.location.getLongitude());
                }
            }
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.d(TAG, "onPageStarted / " + url);
            }
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.d(TAG, "onReceivedError / " + errorCode + " / " + description);
            }
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
                Log.d(TAG, "onReceivedHttpAuthRequest / " + realm);
            }
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                Log.d(TAG, "onReceivedSslError");
            }
            public void onScaleChanged(WebView view, float oldScale, float newScale) {
                Log.d(TAG, "onScaleChanged / " + newScale);
            }
            public void onTooManyRedirects(WebView view, Message cancelMsg, Message continueMsg) {
                Log.d(TAG, "onTooManyRedirects");
            }
            public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
                Log.d(TAG, "onUnhandledKeyEvent");
            }

        });
        if (mProximityReporter.location == null) {
            webView.loadUrl("file:///android_asset/no_location.html");
        } else if (isDataConnectionAvailable()) {
            mNeedToLoadMap = true;
            webView.loadUrl("file:///android_asset/loading.html");
        } else {
            webView.loadUrl("file:///android_asset/no_data.html");
        }
    }

    private boolean isDataConnectionAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo[] networks = cm.getAllNetworkInfo();
            for (NetworkInfo network: networks) {
                if (network.getState() == NetworkInfo.State.CONNECTED) {
                    return true;
                }
            }
        }
        return false;
    }
}

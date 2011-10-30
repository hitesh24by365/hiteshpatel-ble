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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.location.LocationManager;
import android.media.RingtoneManager;

import java.util.Collection;
import java.util.HashMap;


public class ProximityReporter {
    private static SQLiteDatabase db;
    private final static String[] PROJ = {
        "address",
        "nickname",
        "immediateAlertSupported",
        "remoteLinkLossAlertLevel",
        "localLinkLossAlertVibrate",
        "localLinkLossAlertSound",
        "localLinkLossAlertSoundName",
        "localLinkLossAlertSoundUri",
        "longitude",
        "latitude",
        "accuracy",
        "timestamp",
        "enabled"
    };

    private static final String TABLE_NAME = "proximity_reporters";

    static HashMap<String,ProximityReporter> map = new HashMap<String,ProximityReporter>();

    // Stored in database
    public String address;
    public String nickname;

    public boolean immediateAlertSupported;

    public byte remoteLinkLossAlertLevel;

    public boolean localLinkLossAlertVibrate;
    public boolean localLinkLossAlertSound;
    public String  localLinkLossAlertSoundName;
    public String  localLinkLossAlertSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString();

    public boolean enabled = true;

    // Transient
    public int currentTxPowerLevel;

    public boolean isConnected = false;

    public Location location;
    public long cachedDisconnectStartTime;

    public String bluetoothDeviceName;

    public ProximityReporter(String address) {
        this.address = address;

        updateBluetoothDeviceName();
        map.put(address, this);
    }

    public ProximityReporter(Cursor cursor) {
        address = cursor.getString(0);
        nickname = cursor.getString(1);

        immediateAlertSupported = (cursor.getInt(2) != 0);
        remoteLinkLossAlertLevel = (byte) cursor.getInt(3);

        localLinkLossAlertVibrate = (cursor.getInt(4) != 0);
        localLinkLossAlertSound = (cursor.getInt(5) != 0);
        localLinkLossAlertSoundName = cursor.getString(6);
        localLinkLossAlertSoundUri = cursor.getString(7);

        if (cursor.getDouble(8) != 0.0 || cursor.getDouble(9) != 0.0) {
            location = new Location(LocationManager.NETWORK_PROVIDER);
            location.setLongitude(cursor.getDouble(8));
            location.setLatitude(cursor.getDouble(9));
            location.setAccuracy(cursor.getFloat(10));
            location.setTime(cursor.getLong(11));
        } else {
            location = null;
        }

        enabled = (cursor.getInt(12) != 0);

        updateBluetoothDeviceName();
        map.put(address, this);
    }

    public void updateBluetoothDeviceName() {
        BluetoothDevice bd = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
        if (bd != null)
            bluetoothDeviceName = bd.getName();
    }

    public String getName() {
        return (nickname != null && nickname.length() > 0) ? nickname : bluetoothDeviceName;
    }

    public void clearLocation() {
        location = null;
        cachedDisconnectStartTime = System.currentTimeMillis();
        update();
    }

    public void updateLocation(Location newLocation) {
        location = newLocation;
        update();
    }

    public ContentValues toContentValues(boolean includeAddress) {
        ContentValues values = new ContentValues();

        if (includeAddress)
            values.put("address"                , address);

        values.put("nickname"                       , nickname);

        values.put("immediateAlertSupported"        , immediateAlertSupported ? 1:0);
        values.put("remoteLinkLossAlertLevel"       , remoteLinkLossAlertLevel);

        values.put("localLinkLossAlertVibrate"      , localLinkLossAlertVibrate ? 1:0);
        values.put("localLinkLossAlertSound"        , localLinkLossAlertSound? 1:0);
        values.put("localLinkLossAlertSoundName"    , localLinkLossAlertSoundName);
        values.put("localLinkLossAlertSoundUri"     , localLinkLossAlertSoundUri);

        values.put("longitude"                      , location != null ? location.getLongitude() : 0.0);
        values.put("latitude"                       , location != null ? location.getLatitude()  : 0.0);
        values.put("accuracy"                       , location != null ? location.getAccuracy()  : 0.0);
        values.put("timestamp"                      , location != null ? location.getTime()      : 0);

        values.put("enabled"                        , enabled? 1:0);

        return values;
    }

    public long insert() {
        return db.insert(TABLE_NAME, null, toContentValues(true));
    }

    public long update() {
        return db.update(TABLE_NAME, toContentValues(false), "address=?", new String[] { address });
    }

    public long delete() {
        map.remove(address);
        return db.delete(TABLE_NAME, "address=?", new String[] { address });
    }

    public static long delete(String address) {
        map.remove(address);
        return db.delete(TABLE_NAME, "address=?", new String[] { address });
    }

    public static long deleteAll() {
        map.clear();
        return db.delete(TABLE_NAME, null, null);
    }

    public static void initDatabase(Context context) {
        OpenHelper openHelper = new OpenHelper(context);
        db = openHelper.getWritableDatabase();
        loadAll();
    }

    public static void closeDatabase() {
        db.close();
    }

    public static void loadAll() {
        map.clear();
        Cursor cursor = db.query(TABLE_NAME, PROJ, null, null, null, null, "nickname desc");
        if (cursor.moveToFirst()) {
            do {
                new ProximityReporter(cursor);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    public static ProximityReporter get (String address) {
        return map.get(address);
    }

    public static boolean containsKey (String address) {
        return map.containsKey(address);
    }

    public static Collection<ProximityReporter> getAll() {
        return map.values();
    }

    private static class OpenHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "ProximityMonitor.db";
        private static final int DATABASE_VERSION = 11;

        OpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                       + "address STRING, nickname STRING, "
                       + "immediateAlertSupported INTEGER, remoteLinkLossAlertLevel INTEGER,"
                       + "localLinkLossAlertVibrate INTEGER, localLinkLossAlertSound INTEGER,"
                       + "localLinkLossAlertSoundName STRING, localLinkLossAlertSoundUri STRING,"
                       + "longitude REAL, latitude REAL, accuracy REAL, timestamp INTEGER, enabled INTEGER)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }

}

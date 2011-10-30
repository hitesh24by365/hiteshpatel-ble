package com.broadcom.bt.ble.proximitymonitor;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class LocationUpdater implements LocationListener {
    private Context mContext;
    private LocationManager mLocationManager;
    private boolean mUpdatingLocation = false;

    public LocationUpdater(Context context) {
        this.mContext = context;
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public void requestLocationUpdates() {
        if (!mUpdatingLocation) {
            try {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 1, this, mContext.getMainLooper());
                mUpdatingLocation = true;
            } catch (IllegalArgumentException ex) {
                // GPS_PROVIDER not available
            }
            try {
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 1, this, mContext.getMainLooper());
                mUpdatingLocation = true;
            } catch (IllegalArgumentException ex) {
                // NETWORK_PROVIDER not available
            }
        }
    }

    public void onLocationChanged(Location newLocation) {
        boolean locationNeeded = false;
        long currentTime = System.currentTimeMillis();

        for (ProximityReporter pr: ProximityReporter.getAll()) {
            if (pr.isConnected == false && (currentTime - pr.cachedDisconnectStartTime < 60*1000)) {
                locationNeeded = true;
                if (isNewLocationBetter(pr.location, newLocation)) {
                    pr.updateLocation(newLocation);
                }
            }

            if (!locationNeeded) {
                mUpdatingLocation = false;
                mLocationManager.removeUpdates(this);
            }
        }
    }

    public void onProviderDisabled(String provider) {
    }

    public void onProviderEnabled(String provider) {
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    private boolean isNewLocationBetter (Location currentLocation, Location newLocation) {
        if (currentLocation == null)
            return true;

        if (currentLocation.getTime() < newLocation.getTime() - 30*1000)
            return true; // CurrentLocation is too old;
        if (newLocation.getTime() < currentLocation.getTime() - 30*1000)
            return false; // NewLocation is too old;

        if (currentLocation.hasAccuracy() && ! newLocation.hasAccuracy())
            return false; // Current location is more accurate
        if (newLocation.hasAccuracy() && ! currentLocation.hasAccuracy())
            return true; // New location is more accurate

        if (newLocation.getAccuracy() < currentLocation.getAccuracy())
            return true; // New location is more accurate
        if (currentLocation.getAccuracy() < newLocation.getAccuracy())
            return false; // Current location is more accurate

        // Same accuracy - return newest result
        return (newLocation.getTime() > currentLocation.getTime());
    }

}

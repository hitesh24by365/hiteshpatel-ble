package com.broadcom.bt.ble.proximitymonitor;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;

public class LinkLossNotificationManager {
    private Context mContext;
    
    public LinkLossNotificationManager(Context context) {
        mContext = context;
    }
    
    public void displayNotification(ProximityReporter pr, boolean connected) {
        cancelNotification(pr); // Cancel any previous notifications
        CharSequence contentTitle = mContext.getString(connected ? R.string.notification_title_connected : R.string.notification_title_disconnected);
        String bodyPrefix = mContext.getString(connected ? R.string.notification_body_prefix_connected : R.string.notification_body_prefix_disconnected);
        String bodySuffix = mContext.getString(connected ? R.string.notification_body_suffix_connected : R.string.notification_body_suffix_disconnected);
        String deviceName = pr.getName();
        String tickerText = bodyPrefix + deviceName + bodySuffix;
        SpannableString body = new SpannableString(tickerText);
        body.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), bodyPrefix.length(), bodyPrefix.length() + deviceName.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

        int icon = R.drawable.notification_icon;
        long when = System.currentTimeMillis();
        Notification notification = new Notification(icon, tickerText, when);

        Intent notificationIntent = new Intent(mContext, ProximityMonitorActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);
        notification.setLatestEventInfo(mContext, contentTitle, body, contentIntent);

        if (pr.localLinkLossAlertVibrate)
            notification.defaults |= Notification.DEFAULT_VIBRATE;

        if (pr.localLinkLossAlertSound)
            notification.sound = Uri.parse(pr.localLinkLossAlertSoundUri);

        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        int notificationID = addressToID(pr.address);
        nm.notify(notificationID, notification);
    }

    public void cancelNotification(ProximityReporter pr) {
        NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        int notificationID = addressToID(pr.address);
        nm.cancel(notificationID);
    }

    private int addressToID(String address) {
        String s = address.replace(":","");
        long l = Long.parseLong(s, 16);
        return (int) l;
    }

}

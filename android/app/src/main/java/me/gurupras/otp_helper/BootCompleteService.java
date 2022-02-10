package me.gurupras.otp_helper;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class BootCompleteService extends Service {
    private static final String TAG = Utils.TAG + ":boot-svc";
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        Log.d(TAG, "Attempting to start foreground service");
        switch (action) {
            case "stop": {
                stopForeground(true);
                stopSelfResult(startId);
                break;
            }
            case "start": {
                Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
                PendingIntent pendingIntent =
                        PendingIntent.getActivity(this, 0, notificationIntent, 0);
                Notification notification =
                        new Notification.Builder(this)
                                .setContentTitle(getText(R.string.notification_title))
                                .setContentText(getText(R.string.notification_message))
                                .setSmallIcon(R.drawable.launch_background)
                                .setContentIntent(pendingIntent)
                                .build();

                startForeground(0, notification);

                Log.d(TAG, "Performing registration");
                initialize(this);
                Utils.doRegister(this);
                Intent stopIntent = new Intent(this, BootCompleteService.class);
                stopIntent.setAction("stop");
                startService(stopIntent);
            }
        }
        return START_STICKY;
    }

    public static void initialize(Context context) {
        State.loadPreferences(context);
        if (State.simpleCrypto == null) {
            State.simpleCrypto = new SimpleCrypto(State.aesKey);
        }
        startServices(context);
    }

    public static void startServices(Context context) {
        Class[] services = new Class[]{
                PushNotificationsService.class
        };
        for (Class cls : services) {
            Intent serviceIntent = new Intent(context, cls);
            context.startService(serviceIntent);
        }
        PushNotificationsService.initialize(context);
    }
}

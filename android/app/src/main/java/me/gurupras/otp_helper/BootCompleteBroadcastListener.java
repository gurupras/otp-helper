package me.gurupras.otp_helper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BootCompleteBroadcastListener extends BroadcastReceiver {
    private static final String TAG = Utils.TAG + ":boot";
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received intent");
            String action = intent.getAction();
            if (action.equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
                Intent serviceIntent = new Intent(context, BootCompleteService.class);
                serviceIntent.setAction("start");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            } else if (action.equalsIgnoreCase("android.provider.Telephony.SMS_RECEIVED")) {
                // TODO: Queue messages until initialized
                JSONArray messages = new JSONArray();
                if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                            String from = smsMessage.getOriginatingAddress();
                            String body = null;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.DONUT) {
                                body = smsMessage.getMessageBody();
                            }
                            JSONObject json = new JSONObject();
                            try {
                                json.put("from", from);
                                json.put("body", body);
                            } catch (Exception e) {
                                // Cannot happen
                            }
                            messages.put(json);
                            // Send a message
                        }
                    }
                }
                Map<String, Object> dataMap = new HashMap<String, Object>();
                dataMap.put("messages", messages.toString());
                Data data = new Data.Builder().putAll(dataMap).build();
                OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(SMSWorker.class)
                        .setInputData(data)
                        .build();
                WorkManager.getInstance(context).beginWith(work).enqueue();
                Log.d(TAG, "Queued up work");
            }
        }
    }

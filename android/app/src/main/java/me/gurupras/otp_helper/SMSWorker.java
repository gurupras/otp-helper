package me.gurupras.otp_helper;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SMSWorker extends Worker {
    private static final String TAG = Utils.TAG + ":sms-worker";

    public SMSWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Performing long running task in scheduled job");
        Context context = getApplicationContext();
        try {
            Utils.checkAndHandlePendingSubscriptions(context);
        } catch (Exception e) {
            Log.e(TAG, "Failed to check and handle pending subscriptions on SMS", e);
        }
        Data data = getInputData();
        String messagesStr = data.getString("messages");
        try {
            JSONArray messages = new JSONArray(messagesStr);
            for (int idx = 0; idx < messages.length(); idx++) {
                JSONObject message = messages.getJSONObject(idx);
                String from = message.getString("from");
                String body = message.getString("body");
                Utils.onSMS(context, from, body);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to broadcast SMS", e);
        }
        return Result.success();
    }
}

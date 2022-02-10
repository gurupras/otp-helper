package me.gurupras.otp_helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.android.volley.RequestQueue;

import java.nio.charset.StandardCharsets;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

public class State {
    public static final String TAG = Utils.TAG + ":state";

    public static FlutterEngine flutterEngine;
    public static MethodChannel methodChannel;
    public static String deviceID = "";
    public static String endpoint = "https://otp.gurupras.me";
    public static String password = "";
    public static String firebaseToken = "";
    public static SecretKey aesKey = null;
    public static RequestQueue requestQueue;
    public static SimpleCrypto simpleCrypto;

    public static void loadPreferences (Context context) {
        Log.d(TAG, "Loading settings from disk");
        final SharedPreferences sharedPrefs = context.getSharedPreferences( "settings", Context.MODE_PRIVATE);
        String deviceID = sharedPrefs.getString("deviceID", "");
        if (deviceID != "") {
            State.deviceID = deviceID;
            Log.d(TAG, "deviceID=" + deviceID);
        }

        String password = sharedPrefs.getString("password", "");
        if (password != "") {
            State.password = password;
            Log.d(TAG, "password=...");
        }

        String endpoint = sharedPrefs.getString("endpoint", "");
        if (endpoint != "") {
            State.endpoint = endpoint;
            Log.d(TAG, "endpoint=" + endpoint);
        }
        initializeAESKey(context);
    }
    public static void initializeAESKey(Context context) {
        final SharedPreferences sharedPrefs = context.getSharedPreferences( "settings", Context.MODE_PRIVATE);
        String base64AESKey = sharedPrefs.getString("aesKey", "");
        if (!base64AESKey.equals("")) {
            try {
                byte[] aesBytes = Base64.decode(base64AESKey, Base64.DEFAULT);
                SecretKey aesKey = new SecretKeySpec(aesBytes, 0, aesBytes.length, "AES");
                State.aesKey = aesKey;
                Log.d(TAG, "AES key loaded from preferences and initialized");
            } catch (Exception e) {
                Log.d(TAG, "Failed to parse existing AES key. Will be generating a new one", e);
            }
        }
        if (State.aesKey == null) {
            // Generate an AES key and store it
            try {
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256); // for example
//                SecretKeySpec keySpec = new SecretKeySpec("testtesttesttesttesttesttesttest".getBytes(StandardCharsets.UTF_8), "AES");
                SecretKey key = keyGen.generateKey();
                State.aesKey = key;
                base64AESKey = Base64.encodeToString(State.aesKey.getEncoded(), Base64.DEFAULT);
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString("aesKey", base64AESKey);
                editor.commit();
                Log.d(TAG, "AES key generated and initialized");
            } catch (Exception e) {
                Log.e(TAG, "Failed to generate AES key", e);
            }
        }
    }
}
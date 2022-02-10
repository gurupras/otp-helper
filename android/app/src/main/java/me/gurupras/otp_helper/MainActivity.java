package me.gurupras.otp_helper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

import me.gurupras.otp_helper.State;
import me.gurupras.otp_helper.Utils;

import org.json.JSONObject;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends FlutterActivity {
  private static final String CHANNEL = "otp.gurupras.me/bridge";
  private static final String TAG = Utils.TAG;

  @Override
  public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
    super.configureFlutterEngine(flutterEngine);
    State.flutterEngine = flutterEngine;
    State.methodChannel = new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL);

    State.methodChannel.setMethodCallHandler((call, result) -> {
      final SharedPreferences sharedPrefs = getSharedPreferences( "settings", Context.MODE_PRIVATE);
      try {
        switch(call.method) {
          case "setDeviceID": {
            State.deviceID = (String) call.arguments;
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString("deviceID", State.deviceID);
            editor.commit();
            Log.d(TAG, "Set deviceID=" + State.deviceID);
            result.success("{}");
            break;
          }
          case "setEndpoint": {
            State.endpoint = (String) call.arguments;
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString("endpoint", State.endpoint);
            editor.commit();
            Log.d(TAG, "Set endpoint=" + State.endpoint);
            result.success("{}");
            break;
          }
          case "setPassword": {
            String rawPassword = (String) call.arguments;
            State.password = rawPassword;
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString("password", State.password);
            editor.commit();
            Log.d(TAG, "Set password=" + State.password);
            result.success("{}");
            break;
          }
          case "getSettings": {
            try {
              JSONObject data = new JSONObject(sharedPrefs.getAll());
              result.success(data.toString());
              break;
            } catch (Exception e) {
              Log.e(TAG, "Failed to run getSettings", e);
            }
            break;
          }
          case "register": {

            Utils.doRegister(this);
            result.success("{}");
            break;
          }
        }
      } catch (Exception e) {
      }
    });
    BootCompleteService.initialize(this);
  }
}
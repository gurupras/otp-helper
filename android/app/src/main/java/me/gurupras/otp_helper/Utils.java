package me.gurupras.otp_helper;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Base64;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.FirebaseApp;

import me.gurupras.otp_helper.State;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

public class Utils {
    public static final String TAG = "otp-helper";


    public static void onSMS(Context context, String from, String body) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("from", State.simpleCrypto.encrypt(from));
            obj.put("body", State.simpleCrypto.encrypt(body));
//                        State.methodChannel.invokeMethod("newSMS", obj.toString());
            // Make POST request
            try {
                JSONObject response = new JSONObject(obj.toString());
                response.put("deviceID", State.deviceID);
                Utils.POSTRequest(context, response, State.endpoint + "/api/sms");
            } catch (Exception e) {
                // TODO: Send back a response to caller
            }
        } catch (Exception e) {
        }
    }

    public static void onSMS(final Context context, final Intent intent) {
        try {
            checkAndHandlePendingSubscriptions(context);
        } catch (Exception e) {
        }
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                    String from = smsMessage.getOriginatingAddress();
                    String messageBody = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.DONUT) {
                        messageBody = smsMessage.getMessageBody();
                    }
                    onSMS(context, from, messageBody);
                    // Send a message
                }
            }
        }
    }

    public static void doRegister(Context context) {
        try {
            JSONObject body = new JSONObject();
            body.put("deviceID", State.deviceID);
            body.put("password", State.password);
            body.put("pushToken", State.firebaseToken);
            Utils.POSTRequest(context, body, State.endpoint + "/api/register");
            Log.d(TAG, "Registered device");
        } catch (Exception e) {
            // TODO: Send back a response to caller
            Log.e(TAG, "Failed to register device", e);
        }
    }

    public static void checkAndHandlePendingSubscriptions(Context context) throws Exception {
        JSONArray pending = getPendingSubscriptions(context);
        for (int idx = 0; idx < pending.length(); idx++) {
            JSONObject pendingObj = pending.getJSONObject(idx);
            String dataStr = pendingObj.getString("data");
            JSONObject data = new JSONObject(dataStr);
            String ackID = data.getString("ackID");
            String publicKeyString = data.getString("publicKey");
            // TODO: Send out encrypted key
            JSONObject response = new JSONObject();
            response.put("deviceID", State.deviceID);
            response.put("ackID", ackID);

            String publicKeyPEM = publicKeyString
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replaceAll(System.lineSeparator(), "")
                    .replace("-----END PUBLIC KEY-----", "");
            byte[] publicKeyEncoded = Base64.decode(publicKeyPEM, Base64.DEFAULT);

            PublicKey publicKey = null;
            try {
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyEncoded);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                publicKey = keyFactory.generatePublic(keySpec);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            }

            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
            OAEPParameterSpec oaepParams = new OAEPParameterSpec("SHA-512", "MGF1", new MGF1ParameterSpec("SHA-512"), PSource.PSpecified.DEFAULT);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParams);
            byte[] aesBytes = State.aesKey.getEncoded();
            byte[] encrypted = cipher.doFinal(aesBytes);
            String encryptedBase64 = Base64.encodeToString(encrypted, Base64.NO_WRAP);
            response.put("key", encryptedBase64);
            Utils.POSTRequest(context, response, State.endpoint + "/api/key-exchange");
        }
    }

    public static JSONArray getPendingSubscriptions(Context context) throws Exception {
        JSONObject response = Utils.GETRequest(context, State.endpoint + "/api/pending-subscriptions/" + State.deviceID);
        return response.getJSONArray("pending");
    }

    public static void POSTRequest(Context context, JSONObject json, String urlStr) {
        final String TAG = Utils.TAG + ":volley";
        final String mRequestBody = json.toString();
        RequestQueue requestQueue = Volley.newRequestQueue(context);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, urlStr, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i(TAG, response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, error.toString());
            }
        }) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    return mRequestBody == null ? null : mRequestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", mRequestBody, "utf-8");
                    return null;
                }
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                String responseString = "";
                if (response != null) {

                    responseString = String.valueOf(response.statusCode);

                }
                return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
            }
        };
        requestQueue.add(stringRequest);
    }

    public static JSONObject GETRequest(Context context, String url) throws Exception {
        final AtomicReference<JSONObject> notifier = new AtomicReference();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        synchronized (notifier) {
                            notifier.set(response);
                            notifier.notify();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error
                        JSONObject obj = new JSONObject();
                        try {
                            obj.put("error", error.toString());
                            synchronized (notifier) {
                                notifier.set(obj);
                                notifier.notify();
                            }
                        } catch (Exception e) {
                            // Can't happen
                        }
                    }
                });
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        requestQueue.add(jsonObjectRequest);
        synchronized (notifier) {
            while (notifier.get() == null)
                notifier.wait();
        }
        return notifier.get();
    }
}
import 'dart:convert';

import 'package:flutter/services.dart';

class SMSMessage {
  final String from;
  final String body;

  SMSMessage({required this.from, required this.body});

  factory SMSMessage.fromJSON(Map<String, dynamic> json) {
    final String from = json['from'] as String;
    final String body = json['body'] as String;
    return SMSMessage(from: from, body: body);
  }

  factory SMSMessage.fromJSONStr(String jsonStr) {
    final json = jsonDecode(jsonStr);
    return SMSMessage.fromJSON(json);
  }
}

class Bridge {
  MethodChannel methodChannel;
  Bridge(this.methodChannel) {
    methodChannel.setMethodCallHandler((call) async {
      final String jsonStr = call.arguments;

      final message = SMSMessage.fromJSONStr(jsonStr);
      switch (call.method) {
        case "newSMS": // this method name needs to be the same from invokeMethod in Android
          print(
              "[otp-helper]: Received SMS: from=${message.from} body=${message.body}");
          break;
        default:
          print('no method handler for method ${call.method}');
      }
    });
  }

  Future<Map<String, dynamic>> getSettings() async {
    final raw = await methodChannel.invokeMethod("getSettings");
    final str = raw as String;
    final json = jsonDecode(str);
    return json;
  }

  Future<void> setDeviceID(final String data) async {
    await methodChannel.invokeMethod("setDeviceID", data);
  }

  Future<void> setPassword(final String data) async {
    await methodChannel.invokeMethod("setPassword", data);
  }

  Future<void> setEndpoint(final String data) async {
    await methodChannel.invokeMethod("setEndpoint", data);
  }

  Future<void> register() async {
    await methodChannel.invokeMethod("register");
  }
}

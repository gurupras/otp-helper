import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:get/get.dart';
import 'package:otp_helper/bridge.dart';
import 'package:otp_helper/state.dart';
import 'package:permission_handler/permission_handler.dart';

void main() {
  Get.put(GlobalState());
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'OTP Helper',
      theme: ThemeData(
        // This is the theme of your application.
        //
        // Try running your application with "flutter run". You'll see the
        // application has a blue toolbar. Then, without quitting the app, try
        // changing the primarySwatch below to Colors.green and then invoke
        // "hot reload" (press "r" in the console where you ran "flutter run",
        // or simply save your changes to "hot reload" in a Flutter IDE).
        // Notice that the counter didn't reset back to zero; the application
        // is not restarted.
        primarySwatch: Colors.blue,
      ),
      home: const MyHomePage(title: 'OTP Helper'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({Key? key, required this.title}) : super(key: key);

  // This widget is the home page of your application. It is stateful, meaning
  // that it has a State object (defined below) that contains fields that affect
  // how it looks.

  // This class is the configuration for the state. It holds the values (in this
  // case the title) provided by the parent (in this case the App widget) and
  // used by the build method of the State. Fields in a Widget subclass are
  // always marked "final".

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  final state = Get.find<GlobalState>();
  static const platform = MethodChannel('otp.gurupras.me/bridge');
  Bridge bridge = Bridge(platform);
  PermissionStatus _permissionStatus = PermissionStatus.denied;

  Future<void> requestPermission(Permission permission) async {
    final status = await permission.request();

    setState(() {
      _permissionStatus = status;
    });
  }

  Future<void> register() async {
    state.registering.value = true;
    await bridge.register();
    state.registering.value = false;
  }

  Future<void> getSettings() async {
    final settings = await bridge.getSettings();
    print('Settings: ');
    print(settings);
    setState(() {
      if (settings.containsKey("deviceID")) {
        state.deviceID.value = settings["deviceID"] as String;
        state.settings["deviceID"] = state.deviceID.value;
      }
      if (settings.containsKey("password")) {
        state.password.value = settings["password"] as String;
        state.settings["password"] = state.password.value;
      }
    });
  }

  Future<void> updateSettings() async {
    await Future.wait([
      bridge.setDeviceID(state.deviceID.value),
      bridge.setPassword(state.password.value),
      bridge.setEndpoint(state.endpoint.value),
    ]);
    await register();
  }

  @override
  void initState() {
    requestPermission(Permission.sms);
    print('Getting settings');
    getSettings().then((value) {
      if (state.deviceID.value != '' &&
          state.password.value != '' &&
          state.endpoint.value != '') {
        updateSettings().then((value) => register());
      }
    });
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    // This method is rerun every time setState is called, for instance as done
    // by the _incrementCounter method above.
    //
    // The Flutter framework has been optimized to make rerunning build methods
    // fast, so that you can just rebuild anything that needs updating rather
    // than having to individually change instances of widgets.
    return Scaffold(
      appBar: AppBar(
        // Here we take the value from the MyHomePage object that was created by
        // the App.build method, and use it to set our appbar title.
        title: Text(widget.title),
      ),
      body: Center(
        // Center is a layout widget. It takes a single child and positions it
        // in the middle of the parent.
        child: Column(
          // Column is also a layout widget. It takes a list of children and
          // arranges them vertically. By default, it sizes itself to fit its
          // children horizontally, and tries to be as tall as its parent.
          //
          // Invoke "debug painting" (press "p" in the console, choose the
          // "Toggle Debug Paint" action from the Flutter Inspector in Android
          // Studio, or the "Toggle Debug Paint" command in Visual Studio Code)
          // to see the wireframe for each widget.
          //
          // Column has various properties to control how it sizes itself and
          // how it positions its children. Here we use mainAxisAlignment to
          // center the children vertically; the main axis here is the vertical
          // axis because Columns are vertical (the cross axis would be
          // horizontal).
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 16),
              child: TextFormField(
                controller: TextEditingController(text: state.deviceID.value),
                decoration: const InputDecoration(
                    border: UnderlineInputBorder(),
                    hintText: 'Enter a name for this device',
                    labelText: 'Device ID'),
                onChanged: (value) {
                  state.deviceID.value = value;
                },
              ),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 16),
              child: TextFormField(
                controller: TextEditingController(text: state.password.value),
                obscureText: !state.passwordVisible.value,
                decoration: InputDecoration(
                  border: const UnderlineInputBorder(),
                  hintText: 'Enter a password for this device',
                  labelText: 'Password',
                  suffixIcon: IconButton(
                    icon: Icon(
                      // Based on passwordVisible state choose the icon
                      state.passwordVisible.value
                          ? Icons.visibility
                          : Icons.visibility_off,
                      color: Theme.of(context).primaryColorDark,
                    ),
                    onPressed: () {
                      // Update the state i.e. toogle the state of passwordVisible variable
                      state.passwordVisible.value =
                          !state.passwordVisible.value;
                    },
                  ),
                ),
                onChanged: (value) {
                  state.password.value = value;
                },
              ),
            ),
            Padding(
                padding:
                    const EdgeInsets.symmetric(horizontal: 8, vertical: 16),
                child: ElevatedButton(
                  child: const Text('Update'),
                  onPressed: state.changed ? updateSettings : null,
                )),
          ],
        ),
      ),
    );
  }
}

import 'package:get/get.dart';

class GlobalState extends GetxController {
  var deviceID = ''.obs;
  var password = ''.obs;
  // var endpoint = 'https://ota.gurupras.me'.obs;
  var endpoint = 'http://67.205.129.123:6687'.obs;

  final settings = <String, String>{}.obs;

  var passwordVisible = false.obs;
  var registering = false.obs;

  bool get changed => (deviceID.value != settings["deviceID"] ||
      password.value != settings["password"] ||
      endpoint.value != settings["endpoint"]);

  bool get updateDisabled => !changed || registering.value;
}

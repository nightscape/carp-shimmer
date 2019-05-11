import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bluetooth_serial/flutter_bluetooth_serial.dart';
import 'package:shimmer/shimmer.dart';

void main() => runApp(new MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => new _MyAppState();
}

class _MyAppState extends State<MyApp> {
  FlutterBluetoothSerial bluetooth = FlutterBluetoothSerial.instance;

  List<BluetoothDevice> _devices = [];
  BluetoothDevice _device;
  Stream<Map<String, dynamic>> _dataStream;
  bool _connected = false;
  bool _pressed = false;

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  Future<void> initPlatformState() async {
    List<BluetoothDevice> devices = [];

    try {
      devices = await bluetooth.getBondedDevices();
    } on PlatformException {
      // TODO - Error
    }

    bluetooth.onStateChanged().listen((state) {
      switch (state) {
        case FlutterBluetoothSerial.CONNECTED:
          setState(() {
            _connected = true;
            _pressed = false;
          });
          break;
        case FlutterBluetoothSerial.DISCONNECTED:
          setState(() {
            _connected = false;
            _pressed = false;
          });
          break;
        default:
          // TODO
          print(state);
          break;
      }
    });

    bluetooth.onRead().listen((msg) {
      setState(() {
        // TODO
      });
    });

    if (!mounted) return;
    setState(() {
      _devices = devices;
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_dataStream == null)
      _dataStream = Stream.empty();
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: Text('Flutter Bluetooth Serial'),
        ),
        body: Container(
          child: ListView(
            children: <Widget>[
              Padding(
                padding: const EdgeInsets.fromLTRB(10.0, 10.0, 10.0, 0.0),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: <Widget>[
                    Text(
                      'Device:',
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    DropdownButton(
                      items: _getDeviceItems(),
                      onChanged: (value) => setState(() => _device = value),
                      value: _device,
                    ),
                    RaisedButton(
                      onPressed:
                          _pressed ? null : _connected ? _disconnect : _connect,
                      child: Text(_connected ? 'Disconnect' : 'Connect'),
                    ),
                  ],
                ),
              ),
              new StreamBuilder(
                  stream: _dataStream,
                  builder: (context, asyncSnapshot) {
                    if (asyncSnapshot.hasError) {
                      return new Text("Error!");
                    } else if (asyncSnapshot.data == null) {
                      return Text("Waiting");
                    } else {
                      return Text(asyncSnapshot.data.toString());
                    }
                  })
            ],
          ),
        ),
      ),
    );
  }

  List<DropdownMenuItem<BluetoothDevice>> _getDeviceItems() {
    List<DropdownMenuItem<BluetoothDevice>> items = [];
    if (_devices.isEmpty) {
      items.add(DropdownMenuItem(
        child: Text('NONE'),
      ));
    } else {
      _devices.forEach((device) {
        items.add(DropdownMenuItem(
          child: Text(device.name),
          value: device,
        ));
      });
    }
    return items;
  }

  void _connect() {
    if (_device == null) {
      show('No device selected.');
    } else {
      bluetooth.isConnected.then((isConnected) {
        if (!isConnected) {
          setState(() {
            _dataStream = Shimmer().connectDevice({"macAddress": _device.address});
          });
          /* bluetooth.connect(_device).catchError((error) {
            setState(() => _pressed = false);
          }); */
          //setState(() => _pressed = true);
        }
      });
    }
  }

  void _disconnect() {
    bluetooth.disconnect();
    setState(() => _pressed = true);
  }

  Future show(
    String message, {
    Duration duration: const Duration(seconds: 3),
  }) async {
    await new Future.delayed(new Duration(milliseconds: 100));
    Scaffold.of(context).showSnackBar(
      new SnackBar(
        content: new Text(
          message,
          style: new TextStyle(
            color: Colors.white,
          ),
        ),
        duration: duration,
      ),
    );
  }
}

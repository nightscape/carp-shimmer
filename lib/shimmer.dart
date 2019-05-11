import 'dart:async';

import 'package:flutter/services.dart';
import 'dart:convert';
import 'dart:io' show Platform;

/// Custom Exception for the plugin,
/// thrown whenever the plugin is used on platforms other than Android
class ShimmerException implements Exception {
  String _cause;

  ShimmerException(this._cause);

  @override
  String toString() {
    return _cause;
  }
}


/// The main plugin class which establishes a [MethodChannel] and an [EventChannel].
class Shimmer {
  MethodChannel _methodChannel = MethodChannel('shimmer.method_channel');
  EventChannel _eventChannel = EventChannel('shimmer.event_channel');

  Stream<Map<String, dynamic>> connectDevice(Map<String, dynamic> args) {
    if (Platform.isAndroid) {
      Stream<Map<String, dynamic>> _shimmerStream = _eventChannel
            .receiveBroadcastStream()
            .map((d) => Map<String, dynamic>.from(d));
      _shimmerStream.first.then((map) => 0);
      Future.delayed(Duration(seconds: 1)).then((_) => _methodChannel.invokeMethod("connectDevice", args));
      return _shimmerStream;
    } else {
      throw ShimmerException('Shimmer API exclusively available on Android!');
    }
  }

  Future<String> get test => Future.sync(() => "Yes");
}

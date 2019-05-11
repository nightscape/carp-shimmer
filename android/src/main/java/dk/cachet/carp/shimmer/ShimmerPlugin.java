package dk.cachet.carp.shimmer;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry.Registrar;


/**
 * ShimmerPlugin
 */
public class ShimmerPlugin implements EventChannel.StreamHandler, MethodChannel.MethodCallHandler
{

    private EventChannel.EventSink eventSink;
    private SensorHandler manager;
    private Registrar registrar;
    static String CONNECT_DEVICE = "connectDevice";

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar)
    {
        // Set up plugin instance
        ShimmerPlugin plugin = new ShimmerPlugin(registrar);

        // Set up method channel
        final MethodChannel methodChannel = new MethodChannel(registrar.messenger(), "shimmer.method_channel");
        methodChannel.setMethodCallHandler(plugin);

        // Set up event channel
        final EventChannel eventChannel = new EventChannel(registrar.messenger(), "shimmer.event_channel");
        eventChannel.setStreamHandler(plugin);
    }

    public ShimmerPlugin(Registrar registrar)
    {
        this.registrar = registrar;
    }

    @Override
    public void onListen(Object o, EventChannel.EventSink eventSink)
    {
        this.eventSink = eventSink;
    }

    @Override
    public void onCancel(Object o)
    {
        manager.stopService();
        this.eventSink = null;
    }

    @Override
    public void onMethodCall(MethodCall methodCall, MethodChannel.Result result)
    {
        if (methodCall.method.equals(CONNECT_DEVICE))
        {
            connectToDevice(methodCall);
        } else
        {
            result.notImplemented();
        }
    }

    private void connectToDevice(MethodCall methodCall)
    {
        String macAddress = methodCall.argument("macAddress");
        manager = new ShimmerSensorHandler(registrar.activity(), this.eventSink, macAddress);
        manager.startService();
    }

}

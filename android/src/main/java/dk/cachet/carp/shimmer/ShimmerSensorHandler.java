package dk.cachet.carp.shimmer;

import android.app.Activity;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.EventChannel;

import android.os.Handler;
import android.os.Message;

import com.shimmerresearch.android.Shimmer;
import com.shimmerresearch.bluetooth.ShimmerBluetooth;
import com.shimmerresearch.driver.FormatCluster;
import com.shimmerresearch.driver.ObjectCluster;

import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid;
import com.shimmerresearch.driver.CallbackObject;
import com.shimmerresearch.driver.ShimmerDevice;
import com.shimmerresearch.managers.bluetoothManager.ShimmerBluetoothManager;

public class ShimmerSensorHandler implements SensorHandler
{
    private final Activity activity;
    private final EventChannel.EventSink eventSink;
    private final String macAddress;
    final static String LOG_TAG = "ShimmerSensorHandler";
    private ShimmerBluetoothManagerAndroid btManager;

    public ShimmerSensorHandler(Activity activity, EventChannel.EventSink eventSink, String macAddress)
    {
        this.activity = activity;
        this.eventSink = eventSink;
        this.macAddress = macAddress;
    }
    private ShimmerDevice shimmerDevice;
    public class ShimmerHandler extends Handler {

        private ShimmerBluetoothManager btManager;
        private final EventChannel.EventSink eventSink;

        public ShimmerHandler(EventChannel.EventSink eventSink)
        {
            this.eventSink = eventSink;
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case ShimmerBluetooth.MSG_IDENTIFIER_DATA_PACKET:
                    if ((msg.obj instanceof ObjectCluster)) {
                        Map<String, Object> m = new HashMap<String, Object>();
                        ObjectCluster obj = (ObjectCluster) msg.obj;
                        if (obj != null)
                        {
                            m.put("timestamp", obj.mSystemTimeStamp);
                            m.put("sensor", obj.getShimmerName());
                            for (Map.Entry<String, FormatCluster> entry : obj.mPropertyCluster.entries())
                            {
                                m.put(entry.getKey(), entry.getValue().mData);
                            }
                        }
                        eventSink.success(m);

                    }
                    break;
                case Shimmer.MESSAGE_TOAST:
                    /** Toast messages sent from {@link Shimmer} are received here. E.g. device xxxx now streaming.
                     *  Note that display of these Toast messages is done automatically in the Handler in {@link com.shimmerresearch.android.shimmerService.ShimmerService} */

                    //Toast.makeText(getApplicationContext(), msg.getData().getString(Shimmer.TOAST), Toast.LENGTH_SHORT).show();
                    break;
                case ShimmerBluetooth.MSG_IDENTIFIER_STATE_CHANGE:
                    ShimmerBluetooth.BT_STATE state = null;
                    String macAddress = "";

                    if (msg.obj instanceof ObjectCluster) {
                        state = ((ObjectCluster) msg.obj).mState;
                        macAddress = ((ObjectCluster) msg.obj).getMacAddress();
                    } else if (msg.obj instanceof CallbackObject) {
                        state = ((CallbackObject) msg.obj).mState;
                        macAddress = ((CallbackObject) msg.obj).mBluetoothAddress;
                    }

                    Log.d(LOG_TAG, "Shimmer state changed! Shimmer = " + macAddress + ", new state = " + state);

                    switch (state) {
                        case CONNECTED:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] is now CONNECTED");
                            shimmerDevice = btManager.getShimmerDeviceBtConnectedFromMac(macAddress); // TODO: Was (shimmerBtAdd);
                            if (shimmerDevice != null) {
                                Log.i(LOG_TAG, "Got the ShimmerDevice!");
                                shimmerDevice.startStreaming();
                            } else {
                                Log.i(LOG_TAG, "ShimmerDevice returned is NULL!");
                            }
                            break;
                        case CONNECTING:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] is CONNECTING");
                            break;
                        case STREAMING:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] is now STREAMING");
                            break;
                        case STREAMING_AND_SDLOGGING:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] is now STREAMING AND LOGGING");
                            break;
                        case SDLOGGING:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] is now SDLOGGING");
                            break;
                        case DISCONNECTED:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] has been DISCONNECTED");
                            break;
                    }
                    break;
            }

            super.handleMessage(msg);
        }

        public void setBtManager(ShimmerBluetoothManager btManager)
        {
            this.btManager = btManager;
        }
    };

    @Override
    public void startService()
    {
        try
        {
            ShimmerHandler handler = new ShimmerHandler(eventSink);
            btManager = new ShimmerBluetoothManagerAndroid(activity, handler);
            handler.setBtManager(btManager);

            btManager.connectShimmerThroughBTAddress(macAddress);
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void stopService()
    {
        //Disconnect the Shimmer device when app is stopped
        if (shimmerDevice != null) {
            if (shimmerDevice.isSDLogging()) {
                shimmerDevice.stopSDLogging();
                Log.d(LOG_TAG, "Stopped Shimmer Logging");
            } else if (shimmerDevice.isStreaming()) {
                shimmerDevice.stopStreaming();
                Log.d(LOG_TAG, "Stopped Shimmer Streaming");
            } else {
                shimmerDevice.stopStreamingAndLogging();
                Log.d(LOG_TAG, "Stopped Shimmer Streaming and Logging");
            }
        }
        btManager.disconnectAllDevices();
        Log.i(LOG_TAG, "Shimmer DISCONNECTED");
    }


}

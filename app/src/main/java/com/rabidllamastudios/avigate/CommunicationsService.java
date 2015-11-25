package com.rabidllamastudios.avigate;

import android.app.IntentService;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.rabidllamastudios.avigate.model.BundleableJSONObject;
import com.rabidllamastudios.avigate.model.ConnectionPacket;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: write javadoc for all methods
 * helper methods.
 */
public class CommunicationsService extends Service {
    private static final String MQTT_BROKER = "test.mosquitto.org";

    private static final int DEFAULT_PORT = 1883;

    private static final String PACKAGE_NAME = AvigateApplication.class.getPackage().getName();
    public static final String ACTION_CONFIGURE = PACKAGE_NAME + ".action.CONFIGURE";

    public static final String SUBSCRIPTIONS_LOCAL = PACKAGE_NAME + ".extra.LOCAL";
    public static final String SUBSCRIPTIONS_REMOTE = PACKAGE_NAME + ".extra.REMOTE";
    public static final String LOCAL_DEVICE_TYPE = PACKAGE_NAME + ".extra.TYPE";

    //TODO finish actions and extras

    public enum DeviceType {
        CRAFT, CONTROLLER;

        public DeviceType getOpposite() {
            if (super.equals(CONTROLLER)) return CRAFT;
            else if (super.equals(CRAFT)) return CONTROLLER;
            return null;
        }
    }

    public static Intent getConfiguredIntent(Context context, List<String> localSubs, List<String> remoteSubs, DeviceType localDeviceType) {
        Intent intent = new Intent(context, CommunicationsService.class);
        intent.setAction(ACTION_CONFIGURE);
        intent.putStringArrayListExtra(SUBSCRIPTIONS_LOCAL, (ArrayList<String>) localSubs);
        intent.putStringArrayListExtra(SUBSCRIPTIONS_REMOTE, (ArrayList<String>) remoteSubs);
        intent.putExtra(LOCAL_DEVICE_TYPE, localDeviceType.name());
        return intent;
    }

    private BroadcastReceiver mBroadcastReceiver;
    private MqttConnectionManager mMqttConnectionManager;

    private List<String> mRemoteSubs;
    private DeviceType mLocalDeviceType;

    public CommunicationsService() {}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_CONFIGURE.equals(action)) {
                List<String> localSubs = new ArrayList<>();
                List<String> remoteSubs = new ArrayList<>();
                if (intent.hasExtra(SUBSCRIPTIONS_LOCAL)) localSubs = intent.getStringArrayListExtra(SUBSCRIPTIONS_LOCAL);
                if (intent.hasExtra(SUBSCRIPTIONS_REMOTE)) remoteSubs = intent.getStringArrayListExtra(SUBSCRIPTIONS_REMOTE);
                mLocalDeviceType = DeviceType.valueOf(intent.getStringExtra(LOCAL_DEVICE_TYPE));

                if (mBroadcastReceiver != null) {
                    unregisterReceiver(mBroadcastReceiver);
                    mBroadcastReceiver = null;
                }
                mBroadcastReceiver = createBroadcastReceiver(mLocalDeviceType.getOpposite());

                IntentFilter intentFilter = new IntentFilter();
                    for (String each : localSubs) {
                        intentFilter.addAction(each);
                    }
                registerReceiver(mBroadcastReceiver, intentFilter);

                if (mMqttConnectionManager == null) {
                    mMqttConnectionManager = new MqttConnectionManager(this, createMqttConnectionManagerCallback(), MQTT_BROKER, DEFAULT_PORT);
                    mMqttConnectionManager.start();
                }

                mRemoteSubs = new ArrayList<>();
                mRemoteSubs = remoteSubs;
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }

        if (mMqttConnectionManager != null) {
            mMqttConnectionManager.stop();
            mMqttConnectionManager = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private BroadcastReceiver createBroadcastReceiver(final DeviceType remoteDeviceType) {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mMqttConnectionManager == null) return;
                Bundle bundle = intent.getExtras();
                if (bundle == null) return;
                String topic = remoteDeviceType.name() + "/" + intent.getAction();
                String message = new BundleableJSONObject(bundle).toString();
                Log.i("CommunicationsService", "Sending broadcast: " + topic + "/" + message);
                mMqttConnectionManager.publish(topic, message);
            }
        };
    }

    private MqttConnectionManagerCallback createMqttConnectionManagerCallback(){
        return new MqttConnectionManagerCallback() {
            @Override
            public void onConnect() {
                mMqttConnectionManager.unsubscribeAll();
                for (String each : mRemoteSubs) {
                    String topic = mLocalDeviceType.name() + "/" + each;
                    Log.i("CommunicationsService","Subscribing to topic: " + topic);
                    mMqttConnectionManager.subscribe(mLocalDeviceType.name() + "/" + each);
                }
                Intent connectionIntent = new ConnectionPacket(true).toIntent();
                sendBroadcast(connectionIntent);
            }

            @Override
            public void connectionLost() {
                Intent connectionIntent = new ConnectionPacket(false).toIntent();
                sendBroadcast(connectionIntent);
            }

            @Override
            public void messageArrived(String topic, String message) {
                Log.i("CommunicationsService","Message arrived: " + message);
                String[] topicSegments = topic.split("/");
                Intent intent = new Intent(topicSegments[topicSegments.length-1]);
                try {
                    intent.putExtras(new BundleableJSONObject(message).toBundle());
                } catch (JSONException jsonException) {
                    //TODO implement exception handling
                }
                sendBroadcast(intent);
            }
        };
    }
}
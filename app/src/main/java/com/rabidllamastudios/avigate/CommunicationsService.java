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

import com.rabidllamastudios.avigate.model.BundleableJsonObject;
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

    public static final String ACTION_CONFIGURE_COMM_SERVICE =
            PACKAGE_NAME + ".action.CONFIGURE_COMM_SERVICE";

    public static final String EXTRA_SUBSCRIPTIONS_LOCAL = PACKAGE_NAME + ".extra.LOCAL";
    public static final String EXTRA_SUBSCRIPTIONS_REMOTE = PACKAGE_NAME + ".extra.REMOTE";
    public static final String EXTRA_LOCAL_DEVICE_TYPE = PACKAGE_NAME + ".extra.TYPE";

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
        intent.setAction(ACTION_CONFIGURE_COMM_SERVICE);
        intent.putStringArrayListExtra(EXTRA_SUBSCRIPTIONS_LOCAL, (ArrayList<String>) localSubs);
        intent.putStringArrayListExtra(EXTRA_SUBSCRIPTIONS_REMOTE, (ArrayList<String>) remoteSubs);
        intent.putExtra(EXTRA_LOCAL_DEVICE_TYPE, localDeviceType.name());
        return intent;
    }

    private BroadcastReceiver mBroadcastReceiver;
    private MqttConnectionManager mMqttConnectionManager;

    private List<String> mRemoteSubs;
    private DeviceType mLocalDeviceType;

    public CommunicationsService() {}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction().equals(ACTION_CONFIGURE_COMM_SERVICE)) {
            List<String> localSubs = new ArrayList<>();
            List<String> remoteSubs = new ArrayList<>();
            if (intent.hasExtra(EXTRA_SUBSCRIPTIONS_LOCAL))
                localSubs = intent.getStringArrayListExtra(EXTRA_SUBSCRIPTIONS_LOCAL);
            if (intent.hasExtra(EXTRA_SUBSCRIPTIONS_REMOTE))
                remoteSubs = intent.getStringArrayListExtra(EXTRA_SUBSCRIPTIONS_REMOTE);
            mLocalDeviceType = DeviceType.valueOf(intent.getStringExtra(EXTRA_LOCAL_DEVICE_TYPE));

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
                mMqttConnectionManager = new MqttConnectionManager(this
                        , mMqttConnectionManagerCallback, MQTT_BROKER, DEFAULT_PORT);
                mMqttConnectionManager.start();
            }

            mRemoteSubs = new ArrayList<>();
            mRemoteSubs = remoteSubs;
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
                String topic = remoteDeviceType.name() + "/" + intent.getAction();
                Bundle bundle = intent.getExtras();
                String message = "";
                if (bundle != null) {
                    message = new BundleableJsonObject(bundle).toString();
                }
                Log.i("CommunicationsService", "Sending broadcast: " + topic + "/" + message);
                mMqttConnectionManager.publish(topic, message);
            }
        };
    }

    private MqttConnectionManager.Callback mMqttConnectionManagerCallback
            = new MqttConnectionManager.Callback() {
        @Override
        public void onConnect() {
            mMqttConnectionManager.unsubscribeAll();
            for (String each : mRemoteSubs) {
                String topic = mLocalDeviceType.name() + "/" + each;
                Log.i("CommunicationsService", "Subscribing to topic: " + topic);
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
            String[] topicSegments = topic.split("/");
            Intent intent = new Intent(topicSegments[topicSegments.length - 1]);
            if (!message.equals("")) {
                try {
                    Log.i("CommunicationsService", "Message arrived: " + message);
                    intent.putExtras(new BundleableJsonObject(message).toBundle());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Log.i("CommunicationsService", "Messageless topic arrived: " + topic);
            }
            sendBroadcast(intent);
        }
    };
}
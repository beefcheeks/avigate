package com.rabidllamastudios.avigate.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.rabidllamastudios.avigate.AvigateApplication;
import com.rabidllamastudios.avigate.helpers.MqttConnectionManager;
import com.rabidllamastudios.avigate.helpers.BundleableJsonObject;
import com.rabidllamastudios.avigate.models.ConnectionPacket;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/** Service responsible for network interactions. Uses MqttConnectionManager to manage connections.
 * Before starting the service, it can be configured to listen for local and/or remote broadcasts
 * Created by Ryan Staatz on 11/14/2015
 */
public class NetworkService extends Service {
    private static final String CLASS_NAME = NetworkService.class.getSimpleName();
    private static final String PACKAGE_NAME = AvigateApplication.class.getPackage().getName();

    //Intent actions associated with the NetworkService class
    public static final String INTENT_ACTION_CONFIGURE_NETWORK_SERVICE =
            PACKAGE_NAME + ".action.CONFIGURE_NETWORK_SERVICE";
    public static final String INTENT_ACTION_REQUEST_CONNECTION_STATUS =
            PACKAGE_NAME + ".action.REQUEST_CONNETION_STATUS";

    //Strings used to store and retrieve IntentExtras
    private static final String EXTRA_SUBSCRIPTIONS_LOCAL = PACKAGE_NAME + ".extra.LOCAL";
    private static final String EXTRA_SUBSCRIPTIONS_REMOTE = PACKAGE_NAME + ".extra.REMOTE";
    private static final String EXTRA_LOCAL_DEVICE_TYPE = PACKAGE_NAME + ".extra.TYPE";

    //MQTT default broker address and port
    private static final String DEFAULT_MQTT_BROKER = "test.mosquitto.org";
    private static final int DEFAULT_MQTT_PORT = 1883;

    private boolean mIsConnected = false;  //Denotes whether connected to the MQTT broker

    private BroadcastReceiver mConnectionRequestReceiver;
    private BroadcastReceiver mLocalBroadcastReceiver;
    private DeviceType mLocalDeviceType;
    private List<String> mRemoteSubs;
    private MqttConnectionManager mMqttConnectionManager;

    //Denotes whether an Android device is attached to the craft or acting as a remote controller
    public enum DeviceType {
        CRAFT, CONTROLLER;

        //Returns the complementary DeviceType of the current DeviceType
        //Returns null if the DeviceType does not match either of the predefined enum categories
        public DeviceType getOpposite() {
            if (super.equals(CONTROLLER)) return CRAFT;
            else if (super.equals(CRAFT)) return CONTROLLER;
            return null;
        }
    }

    public NetworkService() {}

    /** Returns a Configured Intent that can be used to start this service (NetworkService)
     * @param context the application context of the activity invoking this method
     * @param localSubs a list of local subscriptions in the form of Intent actions (Strings)
     * @param remoteSubs a list of remote subscriptions in the form of Intent actions (Strings)
     * @param localDeviceType the type of device starting the service (e.g. craft or controller)
     */
    public static Intent getConfiguredIntent(Context context, List<String> localSubs,
                                             List<String> remoteSubs, DeviceType localDeviceType) {
        Intent intent = new Intent(context, NetworkService.class);
        intent.setAction(INTENT_ACTION_CONFIGURE_NETWORK_SERVICE);
        intent.putStringArrayListExtra(EXTRA_SUBSCRIPTIONS_LOCAL, (ArrayList<String>) localSubs);
        intent.putStringArrayListExtra(EXTRA_SUBSCRIPTIONS_REMOTE, (ArrayList<String>) remoteSubs);
        intent.putExtra(EXTRA_LOCAL_DEVICE_TYPE, localDeviceType.name());
        return intent;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //If the Intent isn't null & is for configuring the network service, configure the service
        if (intent != null && intent.getAction().equals(INTENT_ACTION_CONFIGURE_NETWORK_SERVICE)) {
            //Initialize ArrayLists
            List<String> localSubs = new ArrayList<>();
            mRemoteSubs = new ArrayList<>();
            //Get corresponding local and remote lists of Intent subscriptions from IntentExtras
            if (intent.hasExtra(EXTRA_SUBSCRIPTIONS_LOCAL))
                localSubs = intent.getStringArrayListExtra(EXTRA_SUBSCRIPTIONS_LOCAL);
            if (intent.hasExtra(EXTRA_SUBSCRIPTIONS_REMOTE))
                mRemoteSubs = intent.getStringArrayListExtra(EXTRA_SUBSCRIPTIONS_REMOTE);
            //Get the local DeviceType from IntentExtras
            mLocalDeviceType = DeviceType.valueOf(intent.getStringExtra(EXTRA_LOCAL_DEVICE_TYPE));
            //If the local broadcast receiver is not null, unregister it and set it to null
            if (mLocalBroadcastReceiver != null) {
                unregisterReceiver(mLocalBroadcastReceiver);
                mLocalBroadcastReceiver = null;
            }
            //Create a new local broadcast receiver (based on the remote device type)
            mLocalBroadcastReceiver = createLocalBroadcastReceiver(mLocalDeviceType.getOpposite());
            //For each Intent action in the local subscription list, add it to the IntentFilter
            IntentFilter localSubsIntentFilter = new IntentFilter();
            for (String each : localSubs) {
                localSubsIntentFilter.addAction(each);
            }
            //Register the local broadcast receiver to listen for the list of Intent actions
            registerReceiver(mLocalBroadcastReceiver, localSubsIntentFilter);
            //If the connection request receiver is not null, unregister it and set it to null
            if (mConnectionRequestReceiver != null) {
                unregisterReceiver(mConnectionRequestReceiver);
                mConnectionRequestReceiver = null;
            }
            //Create and register a new connection request receiver
            mConnectionRequestReceiver = createConnectionRequestBroadcastReceiver();
            registerReceiver(mConnectionRequestReceiver,
                    new IntentFilter(INTENT_ACTION_REQUEST_CONNECTION_STATUS));
            //If the MqttConnectionManager is null, create a new one and start it
            if (mMqttConnectionManager == null) {
                mMqttConnectionManager = new MqttConnectionManager(this,
                        mMqttConnectionManagerCallback, DEFAULT_MQTT_BROKER, DEFAULT_MQTT_PORT);
                mMqttConnectionManager.start();
            }
        }
        Log.i(CLASS_NAME, "Service started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        //Unregister all BroadcastReceivers and set them to null
        if (mLocalBroadcastReceiver != null) {
            unregisterReceiver(mLocalBroadcastReceiver);
            mLocalBroadcastReceiver = null;
        }
        if (mConnectionRequestReceiver != null) {
            unregisterReceiver(mConnectionRequestReceiver);
            mConnectionRequestReceiver = null;
        }
        //Stop the MqttConnectionManager and set it to null
        if (mMqttConnectionManager != null) {
            mMqttConnectionManager.stop();
            mMqttConnectionManager = null;
        }
        //Call the super method
        Log.i(CLASS_NAME, "Service stopped");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //Listens for connection status requests and broadcasts a ConnectionPacket
    private BroadcastReceiver createConnectionRequestBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(INTENT_ACTION_REQUEST_CONNECTION_STATUS)) {
                    sendBroadcast(new ConnectionPacket(mIsConnected).toIntent());
                }
            }
        };
    }

    //Listens for pre-defined Intents to send over the network via MQTT
    private BroadcastReceiver createLocalBroadcastReceiver(final DeviceType remoteDeviceType) {
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
                Log.i(CLASS_NAME, "Publishing: " + topic + "/" + message);
                mMqttConnectionManager.publish(topic, message);
            }
        };
    }

    //Receives notifications from MqttConnectionManager when certain events occur
    private MqttConnectionManager.Callback mMqttConnectionManagerCallback
            = new MqttConnectionManager.Callback() {
        @Override
        public void onConnect() {
            mMqttConnectionManager.unsubscribeAll();
            for (String each : mRemoteSubs) {
                String topic = mLocalDeviceType.name() + "/" + each;
                Log.i(CLASS_NAME, "Subscribing to topic: " + topic);
                mMqttConnectionManager.subscribe(mLocalDeviceType.name() + "/" + each);
            }
            mIsConnected = true;
            sendBroadcast(new ConnectionPacket(true).toIntent());
        }

        @Override
        public void connectionLost() {
            mIsConnected = false;
            sendBroadcast(new ConnectionPacket(false).toIntent());
        }

        @Override
        public void messageArrived(String topic, String message) {
            String[] topicSegments = topic.split("/");
            Intent intent = new Intent(topicSegments[topicSegments.length - 1]);
            if (!message.equals("")) {
                try {
                    Log.i(CLASS_NAME, "Message arrived: " + message);
                    intent.putExtras(new BundleableJsonObject(message).toBundle());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Log.i(CLASS_NAME, "Messageless topic arrived: " + topic);
            }
            sendBroadcast(intent);
        }
    };
}
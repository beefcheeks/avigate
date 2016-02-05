package com.rabidllamastudios.avigate.helpers;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.UUID;

/**
 * Manages the MQTT connection to the server. Responsible for sending and receiving messages.
 * Created by Ryan Staatz on 11/11/15.
 */
public class MqttConnectionManager {

    //Reconnect interval in milliseconds (ms)
    private static final int RECONNECT_INTERVAL = 500;
    private MqttAndroidClient mMqttAndroidClient;
    private Callback mMqttConnectionManagerCallback;
    private int mMqttQoS=0;
    private Thread mReconnectThread;

    /** Constructor that takes a context, inner callback class, server Address, and port number
     * @param context the application context from the activity invoking this method
     * @param callback the configured callback of the Callback interface defined in this class
     * @param serverAddress the MQTT server address excluding "http://" (e.g. "fly.craft.com")
     * @param portNumber the port number to connect on for the input server address (URL)
     */
    public MqttConnectionManager(Context context, Callback callback, String serverAddress,
                                 int portNumber) {
        String serverURL= "tcp://" + serverAddress + ":" + Integer.toString(portNumber);
        mMqttAndroidClient = new MqttAndroidClient(context, serverURL,
                UUID.randomUUID().toString(), new MemoryPersistence());
        mMqttConnectionManagerCallback = callback;
    }

    /**
     * Required for instantiating MqttConnectionManager class
     * Notifies instantiating class of certain events via a callback
     */
    public interface Callback {
        void onConnect();
        void connectionLost();
        void messageArrived(String topic, String message);
    }

    /** Publishes a message under a given topic (channel)
     * @param topic the topic (channel) to publish the message on
     * @param message the message to publish
     */
    public void publish(String topic, String message) {
        if (mMqttAndroidClient.isConnected()) {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(mMqttQoS);
            try {
                mMqttAndroidClient.publish(topic, mqttMessage);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    /** Opens a MQTT connection. Will automatically reconnect if the connection is lost hereafter */
    public void start() {
        connect();
    }

    /** Stops the MQTT connection cleanly */
    public void stop() {
        if (mMqttAndroidClient != null) {
            if (mMqttAndroidClient.isConnected()) {
                unsubscribeAll();
                mMqttAndroidClient.close();
            }
            mMqttAndroidClient.unregisterResources();
            mMqttAndroidClient = null;
        }
    }

    /** Subscribes to a topic. Will notify the callback if a message is received on this topic. */
    public void subscribe(String topic) {
        try {
            mMqttAndroidClient.subscribe(topic, mMqttQoS);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /** Unsubscribes from a topic. Will no longer receive messages on this topic. */
    public void unsubscribe(String topic) {
        try {
            mMqttAndroidClient.unsubscribe(topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /** Unsubscribes from all topics */
    public void unsubscribeAll() {
        this.unsubscribe("#");
    }

    private void connect() {
        try {
            mMqttAndroidClient.connect(null, new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken iMqttToken) {
                    mMqttAndroidClient.setCallback(mMqttCallback);
                    mMqttConnectionManagerCallback.onConnect();
                    if (mReconnectThread != null) {
                        mReconnectThread.interrupt();
                        mReconnectThread = null;
                    }
                }

                @Override
                public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                    throwable.printStackTrace();
                    if (mReconnectThread == null) {
                        mReconnectThread = new Thread(new ReconnectRunnable());
                        mReconnectThread.start();
                    }
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private final MqttCallback mMqttCallback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable throwable) {
            mMqttConnectionManagerCallback.connectionLost();
            // Call connect so it will fail if there is no connection
            connect();
            if (throwable != null) {
                throwable.printStackTrace();
            }
        }

        @Override
        public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
            mMqttConnectionManagerCallback.messageArrived(s, new String(mqttMessage.getPayload()));
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

        }
    };

    private class ReconnectRunnable implements Runnable {
        @Override
        public void run() {
            while(mMqttAndroidClient !=null && !mMqttAndroidClient.isConnected()) {
                connect();
                try {
                    Log.i("Avigate", "Reconnecting...");
                    Thread.sleep(RECONNECT_INTERVAL);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
}
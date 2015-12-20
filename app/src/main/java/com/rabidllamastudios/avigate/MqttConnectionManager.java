package com.rabidllamastudios.avigate;

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
 * TODO: write javadoc for class
 * Created by Ryan on 11/11/15.
 */
public class MqttConnectionManager {

    //Reconnect interval in ms
    private static final int RECONNECT_INTERVAL = 500;
    private MqttAndroidClient mMqttAndroidClient;
    private Callback mMqttConnectionManagerCallback;
    private int mMqttQoS=0;
    private Thread mReconnectThread;

    public MqttConnectionManager(Context inputContext, Callback callback, String serverAddress, int portNumber) {
        String serverURL= "tcp://" + serverAddress + ":" + Integer.toString(portNumber);
        mMqttAndroidClient = new MqttAndroidClient(inputContext, serverURL, UUID.randomUUID().toString(), new MemoryPersistence());
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

    public void start() {
        connect();
    }

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

    public void subscribe(String topic) {
        try {
            mMqttAndroidClient.subscribe(topic, mMqttQoS);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void unsubscribe(String topic) {
        try {
            mMqttAndroidClient.unsubscribe(topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void unsubscribeAll() {
        this.unsubscribe("#");
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
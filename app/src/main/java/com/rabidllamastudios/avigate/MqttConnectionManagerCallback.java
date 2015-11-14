package com.rabidllamastudios.avigate;

/**
 * Required for instantiating MqttConnectionManager class
 * Notifies instantiating class of certain events via a callback
 * Created by Ryan on 11/12/15.
 */
public interface MqttConnectionManagerCallback {
    void onConnect();
    void connectionLost();
    void messageArrived(String topic, String message);
}
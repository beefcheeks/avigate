package com.rabidllamastudios.avigate;

import android.Manifest;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.eclipse.paho.android.service.*;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;
import java.util.UUID;

public class ConnectivityTestActivity extends AppCompatActivity {

    public MqttAndroidClient mqttClient = null;
    private TextView messageOutput;
    private ArrayList<String> subscribedTopics;
    private MqttCallback callback;

    private static final String DEFAULT_SERVER = "test.mosquitto.org";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connectivity_test);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initializeLayout();
    }

    public void initializeCallback (){
        callback = new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                messageOutput.append("\n" + "Connection lost: " + cause.getMessage());
                setLayoutBasedOnConnectionStatus(false);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                messageOutput.append("\n" + "Received: " + topic + "/" + new String (message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        };
    }

    public void initializeLayout() {
        subscribedTopics = new ArrayList();

        EditText serverField = (EditText) findViewById(R.id.et_connect_hint_server);
        serverField.setText(DEFAULT_SERVER);
        
        messageOutput = (TextView) findViewById(R.id.tv_connect_value_messages);
        messageOutput.setMovementMethod(new ScrollingMovementMethod());

        final Button subscribeButton = (Button) findViewById(R.id.button_subscribe);
        EditText topicFieldSubscribe = (EditText) findViewById(R.id.et_connect_hint_topic_subscribe);

        topicFieldSubscribe.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (subscribedTopics.contains(s.toString())) {
                    subscribeButton.setText(getString(R.string.button_unsubscribe));
                } else if (subscribeButton.getText().toString() == getString(R.string.button_unsubscribe)) {
                    subscribeButton.setText(getString(R.string.button_subscribe));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    public boolean hasWritePermissions() {
        PermissionsCheck permCheck = new PermissionsCheck();
        return permCheck.hasPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, permCheck.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
    }

    public void connectButtonPressed(View view) {
        Button connectionButton = (Button) findViewById(R.id.button_connect);
        if (connectionButton.getText().equals(getString(R.string.button_connect))) {
            connect();
        } else if (connectionButton.getText().equals(getString(R.string.button_disconnect))) {
            disconnect();
        }
    }

    public void publishButtonPressed(View view) {
        EditText topicFieldPublish = (EditText) findViewById(R.id.et_connect_hint_topic_publish);
        EditText messageField = (EditText) findViewById(R.id.et_connect_hint_message);
        String topicText = topicFieldPublish.getText().toString();
        String messageText = messageField.getText().toString();

        MqttMessage message = new MqttMessage(messageText.getBytes());
        message.setQos(2);
        message.setRetained(false);

        try {
            mqttClient.publish(topicText, message);
            messageField.setText("");
            messageOutput.append("\n" + "Published: " + topicText + "/" + messageText);
        } catch (MqttPersistenceException e) {
            messageOutput.append("\n" + "Failed to publish:: " + topicText + "/" + messageText + ", " + e.getMessage());
            e.printStackTrace();
        } catch (MqttException e) {
            messageOutput.append("\n" + "Failed to publish:: " + topicText + "/" + messageText + ", " + e.getMessage());
            e.printStackTrace();
        }

    }

    public void subscribeButtonPressed(View view) {
        Button subscribeButton = (Button) findViewById(R.id.button_subscribe);
        EditText topicFieldPublish = (EditText) findViewById(R.id.et_connect_hint_topic_publish);
        EditText topicFieldSubscribe = (EditText) findViewById(R.id.et_connect_hint_topic_subscribe);
        String topicText = topicFieldSubscribe.getText().toString();

        if (subscribeButton.getText().toString().equals(getString(R.string.button_unsubscribe))) {
            try {
                mqttClient.unsubscribe(topicText);
                subscribedTopics.remove(topicText);
                messageOutput.append("\n" + "Unsubscribed from: " + topicText);
                topicFieldSubscribe.setText("");
                if (topicFieldPublish.getText().toString().equals(topicText)) {
                    topicFieldPublish.setText("");
                }

            } catch (MqttPersistenceException e) {
                messageOutput.append("\n" + "Failed to unsubscribe from" + topicText + ", " + e.getMessage());
                e.printStackTrace();

            } catch (MqttException e) {
                messageOutput.append("\n" + "Failed to unsubscribe from" + topicText + ", " + e.getMessage());
                e.printStackTrace();
            }

        } else {
            try {
                mqttClient.subscribe(topicFieldSubscribe.getText().toString(), 2);
                subscribedTopics.add(topicText);
                messageOutput.append("\n" + "Subscribed to: " + topicText);
                topicFieldSubscribe.setText("");
                topicFieldPublish.setText(topicText);
            } catch (MqttPersistenceException e) {
                messageOutput.append("\n" + "Failed to subscribe to: " + topicText + ", " + e.getMessage());
                e.printStackTrace();
            } catch (MqttException e) {
                messageOutput.append("\n" + "Failed to subscribe to: " + topicText + ", " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void connect() {
        if (hasWritePermissions()) {
            MemoryPersistence memPersist = new MemoryPersistence();
            String clientID = UUID.randomUUID().toString();
            final EditText serverAddressField = (EditText) findViewById(R.id.et_connect_hint_server);
            String serverAddress = serverAddressField.getText().toString();
            serverAddress = "tcp://" + serverAddress + ":1883";
            Log.i("URL", serverAddress);
            mqttClient = new MqttAndroidClient(this, serverAddress, clientID, memPersist);
            try {
                mqttClient.connect(null, new IMqttActionListener() {

                    @Override
                    public void onSuccess(IMqttToken iMqttToken) {
                        initializeCallback();
                        mqttClient.setCallback(callback);
                        if (!messageOutput.getText().toString().equals("")) {
                            messageOutput.append("\n");
                        }
                        messageOutput.append("Client connected");
                        setLayoutBasedOnConnectionStatus(true);
                    }

                    @Override
                    public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                        if (!messageOutput.getText().toString().equals("")) {
                            messageOutput.append("\n");
                        }
                        messageOutput.append("Client connection failed: " + throwable.getMessage());
                        throwable.printStackTrace();
                    }
                });

            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            if(!messageOutput.getText().toString().equals("")) {
                messageOutput.append("\n");
            }
            messageOutput.append("Grant access to storage permissions to connect");
        }
    }

    public void disconnect() {
        try {
            mqttClient.disconnect();
            messageOutput.append("\n" + "Client disconnected");
        } catch (MqttException e){
            messageOutput.append("\n" + "Client disconnected with error: " + e.getMessage());
            e.printStackTrace();
        }
        setLayoutBasedOnConnectionStatus(false);
    }

    private void setLayoutBasedOnConnectionStatus(boolean justConnected) {
        Button connectionButton = (Button) findViewById(R.id.button_connect);
        Button publishButton = (Button) findViewById(R.id.button_publish);
        Button subscribeButton = (Button) findViewById(R.id.button_subscribe);

        EditText serverAddressField = (EditText) findViewById(R.id.et_connect_hint_server);
        EditText topicFieldPublish = (EditText) findViewById(R.id.et_connect_hint_topic_publish);
        EditText messageField = (EditText) findViewById(R.id.et_connect_hint_message);
        EditText topicFieldSubscribe = (EditText) findViewById(R.id.et_connect_hint_topic_subscribe);

        TextView serverInfo = (TextView) findViewById(R.id.tv_connect_value_server);

        if (justConnected) {
            serverInfo.setText(serverAddressField.getText().toString());
            serverAddressField.setVisibility(View.GONE);
            serverInfo.setVisibility(View.VISIBLE);
            connectionButton.setText(getString(R.string.button_disconnect));

            topicFieldPublish.setClickable(true);
            topicFieldPublish.setCursorVisible(true);
            topicFieldPublish.setFocusable(true);
            topicFieldPublish.setFocusableInTouchMode(true);

            messageField.setClickable(true);
            messageField.setCursorVisible(true);
            messageField.setFocusable(true);
            messageField.setFocusableInTouchMode(true);

            topicFieldSubscribe.setClickable(true);
            topicFieldSubscribe.setCursorVisible(true);
            topicFieldSubscribe.setFocusable(true);
            topicFieldSubscribe.setFocusableInTouchMode(true);

            publishButton.setEnabled(true);
            subscribeButton.setEnabled(true);

        } else {
            serverInfo.setText("");
            serverInfo.setVisibility(View.GONE);
            serverAddressField.setVisibility(View.VISIBLE);
            connectionButton.setText(getString(R.string.button_connect));

            topicFieldPublish.setClickable(false);
            topicFieldPublish.setCursorVisible(false);
            topicFieldPublish.setFocusable(false);
            topicFieldPublish.setFocusableInTouchMode(false);

            messageField.setClickable(false);
            messageField.setCursorVisible(false);
            messageField.setFocusable(false);
            messageField.setFocusableInTouchMode(false);

            topicFieldSubscribe.setClickable(false);
            topicFieldSubscribe.setCursorVisible(false);
            topicFieldSubscribe.setFocusable(false);
            topicFieldSubscribe.setFocusableInTouchMode(false);

            publishButton.setEnabled(false);
            subscribeButton.setEnabled(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mqttClient != null) {
            mqttClient.unregisterResources();
            mqttClient.close();
        }
    }

    public void onStop() {
        super.onStop();
        if(mqttClient != null) {
            mqttClient.unregisterResources();
            mqttClient.close();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mqttClient != null) {
            mqttClient.unregisterResources();
            mqttClient.close();
        }
    }
}
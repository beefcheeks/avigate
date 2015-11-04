package com.rabidllamastudios.avigate;

import android.Manifest;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.eclipse.paho.android.service.*;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.UUID;

public class ConnectivityTestActivity extends AppCompatActivity {

    public MqttAndroidClient mqttClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connectivity_test);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public boolean hasWritePermissions() {
        PermissionsCheck permCheck = new PermissionsCheck();
        return permCheck.hasPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, permCheck.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
    }

    public void connectButtonPressed(View view) {
        Button connectionButton = (Button) findViewById(R.id.button_connect);
        if (connectionButton.getText() == getString(R.string.button_connect)) {
            connect();
        } else if (connectionButton.getText() == getString(R.string.button_disconnect)) {
            disconnect();
        }
    }

    public void sendButtonPressed(View view) {
        TextView sendStatus = (TextView) findViewById(R.id.tv_connect_value_status_send);

        EditText topicField = (EditText) findViewById(R.id.et_connect_hint_topic);
        EditText messageField = (EditText) findViewById(R.id.et_connect_hint_message);
        String messageText = messageField.getText().toString();

        MqttMessage message = new MqttMessage(messageText.getBytes());
        message.setQos(2);
        message.setRetained(false);

        try {
            mqttClient.publish(topicField.getText().toString(), message);
            sendStatus.setText("Message successfully sent!");
            topicField.setText("");
            messageField.setText("");

        } catch (MqttPersistenceException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    public void connect() {
        final TextView connectionStatus = (TextView) findViewById(R.id.tv_connect_value_status_server);
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
                        connectionStatus.setText("Client connected");
                        setLayoutBasedOnConnectionStatus(true);
                    }

                    @Override
                    public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                        connectionStatus.setText("Client connection failed: " + throwable.getMessage());
                        throwable.printStackTrace();
                    }
                });

            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            connectionStatus.setText("Grant access to storage permissions to connect");
        }
    }

    public void disconnect() {
        final TextView connectionStatus = (TextView) findViewById(R.id.tv_connect_value_status_server);
        try {
            mqttClient.disconnect();
            connectionStatus.setText(getString(R.string.tv_connect_value_placeholder));
        } catch (MqttException e){
            connectionStatus.setText("Client disconnected with error: " + e.getMessage());
            e.printStackTrace();
        }
        setLayoutBasedOnConnectionStatus(false);
    }

    private void setLayoutBasedOnConnectionStatus(boolean justConnected) {
        Button connectionButton = (Button) findViewById(R.id.button_connect);
        Button sendButton = (Button) findViewById(R.id.button_send);

        EditText serverAddressField = (EditText) findViewById(R.id.et_connect_hint_server);
        EditText topicField = (EditText) findViewById(R.id.et_connect_hint_topic);
        EditText messageField = (EditText) findViewById(R.id.et_connect_hint_message);

        TextView serverInfo = (TextView) findViewById(R.id.tv_connect_value_server);

        if (justConnected) {
            serverInfo.setText(serverAddressField.getText().toString());
            serverAddressField.setVisibility(View.GONE);
            serverInfo.setVisibility(View.VISIBLE);
            connectionButton.setText(getString(R.string.button_disconnect));

            topicField.setClickable(true);
            topicField.setCursorVisible(true);
            topicField.setFocusable(true);
            topicField.setFocusableInTouchMode(true);

            messageField.setClickable(true);
            messageField.setCursorVisible(true);
            messageField.setFocusable(true);
            messageField.setFocusableInTouchMode(true);

            sendButton.setEnabled(true);

        } else {
            serverInfo.setText("");
            serverInfo.setVisibility(View.GONE);
            serverAddressField.setVisibility(View.VISIBLE);
            connectionButton.setText(getString(R.string.button_connect));

            topicField.setClickable(false);
            topicField.setCursorVisible(false);
            topicField.setFocusable(false);
            topicField.setFocusableInTouchMode(false);

            messageField.setClickable(false);
            messageField.setCursorVisible(false);
            messageField.setFocusable(false);
            messageField.setFocusableInTouchMode(false);

            sendButton.setEnabled(false);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mqttClient != null) {
            mqttClient.unregisterResources();
            mqttClient.close();
        }
    }
}
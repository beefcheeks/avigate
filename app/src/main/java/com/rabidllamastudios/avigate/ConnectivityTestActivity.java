package com.rabidllamastudios.avigate;

import android.Manifest;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

public class ConnectivityTestActivity extends AppCompatActivity {

    private MqttConnectionManager mMqttConnectionManager;
    private TextView mMessageOutput;
    private ArrayList<String> mSubscribedTopics;
    private static final String DEFAULT_SERVER = "test.mosquitto.org";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connectivity_test);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setLayoutBasedOnConnectionStatus(false);
        mSubscribedTopics = new ArrayList<>();

        EditText serverField = (EditText) findViewById(R.id.et_connect_hint_server);
        serverField.setText(DEFAULT_SERVER);

        mMessageOutput = (TextView) findViewById(R.id.tv_connect_value_messages);
        mMessageOutput.setMovementMethod(new ScrollingMovementMethod());

        final Button subscribeButton = (Button) findViewById(R.id.button_subscribe);
        EditText topicFieldSubscribe = (EditText) findViewById(R.id.et_connect_hint_topic_subscribe);

        topicFieldSubscribe.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mSubscribedTopics.contains(s.toString())) {
                    subscribeButton.setText(getString(R.string.button_unsubscribe));
                } else if (subscribeButton.getText().toString().equals(getString(R.string.button_unsubscribe))) {
                    subscribeButton.setText(getString(R.string.button_subscribe));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    public MqttConnectionManagerCallback createMqttConnectionManagerCallback(){
        return new MqttConnectionManagerCallback() {
            @Override
            public void connectionLost() {
                mMessageOutput.append("\n" + "Connection lost");
                mSubscribedTopics.clear();
                setLayoutBasedOnConnectionStatus(false);
            }

            @Override
            public void onConnect() {
                mMessageOutput.append("\n" + "Client connected");
                setLayoutBasedOnConnectionStatus(true);
            }

            @Override
            public void messageArrived(String topic, String message) {
                mMessageOutput.append("\n" + "Received: " + topic + "/" + message);
            }
        };
    }

    public boolean hasWritePermissions() {
        PermissionsChecker permChecker = new PermissionsChecker(this, null);
        return permChecker.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, permChecker.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
    }

    public void connectButtonPressed(View view) {
        if (hasWritePermissions()) {
            Button connectionButton = (Button) findViewById(R.id.button_connect);
            if (connectionButton.getText().equals(getString(R.string.button_connect))) {
                if (mMqttConnectionManager == null) {
                    final EditText serverAddressField = (EditText) findViewById(R.id.et_connect_hint_server);
                    mMqttConnectionManager = new MqttConnectionManager(this, createMqttConnectionManagerCallback(), serverAddressField.getText().toString(), 1883);
                }
                mMqttConnectionManager.start();
            } else if (connectionButton.getText().equals(getString(R.string.button_disconnect))) {
                mMqttConnectionManager.stop();
                mMqttConnectionManager = null;
                mMessageOutput.append("\n" + "Client disconnected");
                setLayoutBasedOnConnectionStatus(false);
            }
        } else {
            mMessageOutput.append("Grant access to storage permissions to connect");
        }
    }

    public void publishButtonPressed(View view) {
        EditText topicFieldPublish = (EditText) findViewById(R.id.et_connect_hint_topic_publish);
        EditText messageField = (EditText) findViewById(R.id.et_connect_hint_message);
        String topicText = topicFieldPublish.getText().toString();
        String messageText = messageField.getText().toString();

        mMqttConnectionManager.publish(topicText, messageText);
        mMessageOutput.append("\n" + "Published: " + topicText + "/" + messageText);
    }

    public void subscribeButtonPressed(View view) {
        Button subscribeButton = (Button) findViewById(R.id.button_subscribe);
        EditText topicFieldPublish = (EditText) findViewById(R.id.et_connect_hint_topic_publish);
        EditText topicFieldSubscribe = (EditText) findViewById(R.id.et_connect_hint_topic_subscribe);
        String topicText = topicFieldSubscribe.getText().toString();

        if (subscribeButton.getText().toString().equals(getString(R.string.button_unsubscribe))) {
            mMqttConnectionManager.unsubscribe(topicText);
            mSubscribedTopics.remove(topicText);
            mMessageOutput.append("\n" + "Unsubscribed from: " + topicText);
            topicFieldSubscribe.setText("");
            if (topicFieldPublish.getText().toString().equals(topicText)) {
                topicFieldPublish.setText("");
            }

        } else {
            mMqttConnectionManager.subscribe(topicText);
            mSubscribedTopics.add(topicText);
            mMessageOutput.append("\n" + "Subscribed to: " + topicText);
            topicFieldSubscribe.setText("");
            topicFieldPublish.setText(topicText);
        }
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

    //TODO correctly implement below methods or remove class entirely

    @Override
    public void onResume() {
        super.onResume();
        setLayoutBasedOnConnectionStatus(false);
        mMessageOutput.append("\n" + "Client disconnected");
    }

    @Override
    public void onPause() {
        if (mMqttConnectionManager !=null) {
            mMqttConnectionManager.stop();
            mMqttConnectionManager = null;
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        if (mMqttConnectionManager !=null) {
            mMqttConnectionManager.stop();
            mMqttConnectionManager = null;
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (mMqttConnectionManager !=null) {
        mMqttConnectionManager.stop();
            mMqttConnectionManager = null;
        }
        super.onDestroy();
    }
}
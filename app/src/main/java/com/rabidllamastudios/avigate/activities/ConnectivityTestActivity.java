package com.rabidllamastudios.avigate.activities;

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

import com.rabidllamastudios.avigate.R;
import com.rabidllamastudios.avigate.helpers.MqttConnectionManager;
import com.rabidllamastudios.avigate.helpers.PermissionsChecker;

import java.util.ArrayList;

/**
 * Used to test connectivity with an MQTT server
 * Created by Ryan Staatz on 11/3/2015
 */
public class ConnectivityTestActivity extends AppCompatActivity {

    private static final String DEFAULT_SERVER = "test.mosquitto.org";

    private boolean mHasWritePermissions = false;
    private ArrayList<String> mSubscribedTopics;
    private MqttConnectionManager mMqttConnectionManager;
    private PermissionsChecker permissionsChecker;
    private TextView mMessageOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connectivity_test);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Initialize the PermissionsChecker
        permissionsChecker = new PermissionsChecker(mPermissionsCheckerCallback);
        //Initialize the List of subscribed topics
        mSubscribedTopics = new ArrayList<>();

        //Set the server text field to the default server URL
        EditText serverField = (EditText) findViewById(R.id.et_connect_hint_server);
        serverField.setText(DEFAULT_SERVER);

        //Initialize the textview used for scrolling output
        mMessageOutput = (TextView) findViewById(R.id.tv_connect_value_messages);
        mMessageOutput.setMovementMethod(new ScrollingMovementMethod());

        //Initialize the subscribe button
        final Button subscribeButton = (Button) findViewById(R.id.button_subscribe);
        EditText topicFieldSubscribe =
                (EditText) findViewById(R.id.et_connect_hint_topic_subscribe);

        topicFieldSubscribe.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mSubscribedTopics.contains(s.toString())) {
                    subscribeButton.setText(getString(R.string.button_unsubscribe));
                } else if (subscribeButton.getText().toString().equals(
                        getString(R.string.button_unsubscribe))) {
                    subscribeButton.setText(getString(R.string.button_subscribe));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mHasWritePermissions = permissionsChecker.hasPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                PermissionsChecker.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        setLayout(false);
        mMessageOutput.append("\n" + "Client disconnected");
    }

    @Override
    public void onPause() {
        if (mMqttConnectionManager != null) {
            mMqttConnectionManager.stop();
            mMqttConnectionManager = null;
        }
        super.onPause();
    }

    // If the user allows write permissions,
    private PermissionsChecker.Callback mPermissionsCheckerCallback =
            new PermissionsChecker.Callback() {
                @Override
                public void permissionGranted(int permissionsConstant) {
                    if (permissionsConstant ==
                            PermissionsChecker.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {
                        mHasWritePermissions = true;
                        mMessageOutput.append("Storage permissions granted");
                    }
                }
            };


    public MqttConnectionManager.Callback mMqttConnectionManagerCallback =
            new MqttConnectionManager.Callback() {
        @Override
        public void connectionLost() {
            mMessageOutput.append("\n" + "Connection lost");
            mSubscribedTopics.clear();
            setLayout(false);
        }

        @Override
        public void onConnect() {
            mMessageOutput.append("\n" + "Client connected");
            setLayout(true);
        }

        @Override
        public void messageArrived(String topic, String message) {
            mMessageOutput.append("\n" + "Received: " + topic + "/" + message);
        }
    };

    public void connectButtonPressed(View view) {
        if (mHasWritePermissions) {
            Button connectionButton = (Button) findViewById(R.id.button_connect);
            if (connectionButton.getText().equals(getString(R.string.button_connect))) {
                if (mMqttConnectionManager == null) {
                    final EditText serverAddressField =
                            (EditText) findViewById(R.id.et_connect_hint_server);
                    mMqttConnectionManager = new MqttConnectionManager(this,
                            mMqttConnectionManagerCallback,
                            serverAddressField.getText().toString(), 1883);
                }
                mMqttConnectionManager.start();
            } else if (connectionButton.getText().equals(getString(R.string.button_disconnect))) {
                mMqttConnectionManager.stop();
                mMqttConnectionManager = null;
                mMessageOutput.append("\n" + "Client disconnected");
                setLayout(false);
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
        EditText topicFieldSubscribe =
                (EditText) findViewById(R.id.et_connect_hint_topic_subscribe);
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

    //Sets the layout based on the connection status
    private void setLayout(boolean justConnected) {
        Button connectionButton = (Button) findViewById(R.id.button_connect);
        Button publishButton = (Button) findViewById(R.id.button_publish);
        Button subscribeButton = (Button) findViewById(R.id.button_subscribe);

        EditText serverAddressField = (EditText) findViewById(R.id.et_connect_hint_server);
        EditText topicFieldPublish = (EditText) findViewById(R.id.et_connect_hint_topic_publish);
        EditText messageField = (EditText) findViewById(R.id.et_connect_hint_message);
        EditText topicFieldSubscribe =
                (EditText) findViewById(R.id.et_connect_hint_topic_subscribe);

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
}
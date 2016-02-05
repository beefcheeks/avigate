package com.rabidllamastudios.avigate.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.rabidllamastudios.avigate.R;
import com.rabidllamastudios.avigate.helpers.SharedPreferencesManager;
import com.rabidllamastudios.avigate.models.ConnectionPacket;
import com.rabidllamastudios.avigate.models.ArduinoPacket;
import com.rabidllamastudios.avigate.services.NetworkService;
import com.rabidllamastudios.avigate.services.FlightControlService;

import java.util.ArrayList;
import java.util.List;

/**
 * Remotely manages a flight over the network. Can be used to start, stop, and command a craft.
 * Displays relevant flight data and debug info.
 * Created by Ryan Staatz on 11/11/2015
 */
public class ControllerActivity extends AppCompatActivity {

    private Intent mNetworkService;
    private ArduinoPacket mConfigArduinoPacket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Load the Arduino configuration for the craft profile sent in the Intent
        loadArduinoConfiguration(getIntent());

        //Register a ConnectionPacket IntentFilter and associated Broadcast Receiver
        IntentFilter connectionIntentFilter = new IntentFilter(ConnectionPacket.INTENT_ACTION);
        registerReceiver(mConnectionReceiver, connectionIntentFilter);

        //Register a ArduinoPacket output IntentFilter and associated Broadcast Receiver
        IntentFilter servoPacketIntentFilter = new IntentFilter(ArduinoPacket.INTENT_ACTION_OUTPUT);
        registerReceiver(mArduinoOutputReceiver, servoPacketIntentFilter);

        //Configure and start NetworkService
        List<String> localSubs = new ArrayList<>();
        List<String> remoteSubs = new ArrayList<>();
        localSubs.add(FlightControlService.INTENT_ACTION_CONFIGURE_FLIGHT_CONTROL_SERVICE);
        localSubs.add(ArduinoPacket.INTENT_ACTION_INPUT);
        remoteSubs.add(ArduinoPacket.INTENT_ACTION_OUTPUT);
        mNetworkService = NetworkService.getConfiguredIntent(this, localSubs, remoteSubs,
                NetworkService.DeviceType.CONTROLLER);
        startService(mNetworkService);
    }

    @Override
    protected void onDestroy() {
        //Unregister receivers and stop NetworkService
        unregisterReceiver(mArduinoOutputReceiver);
        unregisterReceiver(mConnectionReceiver);
        if (mNetworkService != null) stopService(mNetworkService);
        super.onDestroy();
    }

    //Broadcast receiver for output received from the Arduino
    private BroadcastReceiver mArduinoOutputReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ArduinoPacket.INTENT_ACTION_OUTPUT)) {
                ArduinoPacket arduinoPacket = new ArduinoPacket(intent.getExtras());
                if (arduinoPacket.isStatusReady()) {
                    Intent flightControlServiceIntent =
                            FlightControlService.getConfiguredIntent(mConfigArduinoPacket);
                    sendBroadcast(flightControlServiceIntent);
                }
            }
        }
    };

    //Broadcast Receiver for connection state changes
    private BroadcastReceiver mConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //If the intent is type connection packet, if connected, query the Arduino status
            if (intent.getAction().equals(ConnectionPacket.INTENT_ACTION)) {
                ConnectionPacket connectionPacket = new ConnectionPacket(intent.getExtras());
                if (connectionPacket.isConnected()) {
                    //Request the status of the Arduino after a connection is established
                    ArduinoPacket arduinoPacket = new ArduinoPacket();
                    arduinoPacket.addStatusRequest();
                    sendBroadcast(arduinoPacket.toIntent(ArduinoPacket.INTENT_ACTION_INPUT));
                } else {
                    //TODO do stuff when disconnected
                }
            }
        }
    };

    //Loads the Arduino configuration into a ArduinoPacket from SharedPreferences
    private void loadArduinoConfiguration(Intent intent) {
        mConfigArduinoPacket = new ArduinoPacket();
        String craftProfileName = intent.getStringExtra(SharedPreferencesManager.KEY_CRAFT_NAME);
        SharedPreferencesManager sharedPreferencesManager = new SharedPreferencesManager(this);
        if (craftProfileName != null) {
            String config = sharedPreferencesManager.getCraftConfiguration(craftProfileName);
            if (config != null) {
                mConfigArduinoPacket = new ArduinoPacket(config);
            }
        }
    }
}

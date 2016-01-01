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
import com.rabidllamastudios.avigate.models.ServoPacket;
import com.rabidllamastudios.avigate.services.CommunicationsService;
import com.rabidllamastudios.avigate.services.FlightControlService;

import java.util.ArrayList;
import java.util.List;

public class ControllerActivity extends AppCompatActivity {

    private Intent mCommService;
    private ServoPacket mConfigServoPacket;

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

        //Register a ServoPacket output IntentFilter and associated Broadcast Receiver
        IntentFilter servoPacketIntentFilter = new IntentFilter(ServoPacket.INTENT_ACTION_OUTPUT);
        registerReceiver(mArduinoOutputReceiver, servoPacketIntentFilter);

        //Configure and start CommunicationsService
        List<String> localSubs = new ArrayList<>();
        List<String> remoteSubs = new ArrayList<>();
        localSubs.add(FlightControlService.INTENT_ACTION_CONFIGURE_FLIGHT_CONTROL_SERVICE);
        localSubs.add(ServoPacket.INTENT_ACTION_INPUT);
        remoteSubs.add(ServoPacket.INTENT_ACTION_OUTPUT);
        mCommService = CommunicationsService.getConfiguredIntent(this, localSubs, remoteSubs,
                CommunicationsService.DeviceType.CONTROLLER);
        startService(mCommService);
    }

    @Override
    protected void onDestroy() {
        //Unregister receivers and stop CommunicationsService
        unregisterReceiver(mArduinoOutputReceiver);
        unregisterReceiver(mConnectionReceiver);
        if (mCommService != null) stopService(mCommService);
        super.onDestroy();
    }


    //Broadcast receiver for output received from the Arduino
    private BroadcastReceiver mArduinoOutputReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ServoPacket.INTENT_ACTION_OUTPUT)) {
                ServoPacket servoPacket = new ServoPacket(intent.getExtras());
                if (servoPacket.isStatusReady()) {
                    Intent flightControlServiceIntent =
                            FlightControlService.getConfiguredIntent(mConfigServoPacket);
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
                    ServoPacket servoPacket = new ServoPacket();
                    servoPacket.addStatusRequest();
                    sendBroadcast(servoPacket.toIntent(ServoPacket.INTENT_ACTION_INPUT));
                } else {
                    //TODO do stuff when disconnected
                }
            }
        }
    };

    //Loads the Arduino configuration into a ServoPacket from SharedPreferences
    private void loadArduinoConfiguration(Intent intent) {
        mConfigServoPacket = new ServoPacket();
        String craftProfileName = intent.getStringExtra(SharedPreferencesManager.KEY_CRAFT_NAME);
        SharedPreferencesManager sharedPreferencesManager = new SharedPreferencesManager(this);
        if (craftProfileName != null) {
            String config = sharedPreferencesManager.getCraftConfiguration(craftProfileName);
            if (config != null) {
                mConfigServoPacket = new ServoPacket(config);
            }
        }
    }

}

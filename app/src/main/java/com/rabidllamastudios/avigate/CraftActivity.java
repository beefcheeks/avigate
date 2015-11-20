package com.rabidllamastudios.avigate;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.rabidllamastudios.avigate.model.ConnectionPacket;
import com.rabidllamastudios.avigate.model.GPSPacket;
import com.rabidllamastudios.avigate.model.OrientationPacket;
import com.rabidllamastudios.avigate.model.PressurePacket;

import java.util.ArrayList;
import java.util.List;

public class CraftActivity extends AppCompatActivity {

    //TODO adjust sensor rate with latency?
    //Sensor update rate in microseconds
    private static final int SENSOR_UPDATE_RATE = SensorManager.SENSOR_DELAY_UI;

    private BroadcastReceiver mBroadcastReceiver;
    private Intent mCommService;
    private Intent mSensorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_craft);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Create broadcast receiver and listen for specific intents
        mBroadcastReceiver = createBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectionPacket.INTENT_ACTION);
        intentFilter.addAction(OrientationPacket.INTENT_ACTION);
        intentFilter.addAction(GPSPacket.INTENT_ACTION);
        intentFilter.addAction(PressurePacket.INTENT_ACTION);
        registerReceiver(mBroadcastReceiver, intentFilter);

        //Configure sensor service intent
        mSensorService = SensorService.getConfiguredIntent(this, SENSOR_UPDATE_RATE);

        //Check for location permissions before starting the sensor service
        PermissionsChecker permissionsChecker = new PermissionsChecker(this, createPermissionsCheckerCallback());
        if (permissionsChecker.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION, PermissionsChecker.PERMISSIONS_REQUEST_READ_LOCATION_FINE)) {
            startService(mSensorService);
        }

        //Start the communications service.
        List<String> localSubs = new ArrayList<>();
        List<String> remoteSubs = new ArrayList<>();
        localSubs.add(OrientationPacket.INTENT_ACTION);
        mCommService = CommunicationsService.getConfiguredIntent(this, localSubs, remoteSubs, CommunicationsService.DeviceType.CRAFT);
        startService(mCommService);
    }

    // If the user allows location permissions, start the sensor service
    private PermissionsCheckerCallback createPermissionsCheckerCallback() {
        return new PermissionsCheckerCallback() {
            @Override
            public void permissionGranted(int permissionsConstant) {
                if (permissionsConstant == PermissionsChecker.PERMISSIONS_REQUEST_READ_LOCATION_FINE) {
                    startService(mSensorService);
                }
            }
        };
    }

    @Override
    public void onDestroy() {
        stopService(mCommService);
        stopService(mSensorService);
        unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    private BroadcastReceiver createBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //If the intent is type connection packet, update corresponding textview value
                if (intent.getAction().equals(ConnectionPacket.INTENT_ACTION)) {
                    TextView connectionStatusTV = (TextView) findViewById(R.id.tv_craft_value_connect);
                    ConnectionPacket connectionPacket = new ConnectionPacket(intent.getExtras());
                    if (connectionPacket.isConnected()) {
                        connectionStatusTV.setText(getResources().getString(R.string.tv_placeholder_connected));
                    } else {
                        connectionStatusTV.setText(getResources().getString(R.string.tv_placeholder_disconnected));
                    }
                //If the intent is type orientation packet, update corresponding textview values
                } else if (intent.getAction().equals(OrientationPacket.INTENT_ACTION)) {
                    TextView pitchTV = (TextView) findViewById(R.id.tv_craft_value_pitch);
                    TextView yawTV = (TextView) findViewById(R.id.tv_craft_value_yaw);
                    TextView rollTV = (TextView) findViewById(R.id.tv_craft_value_roll);
                    OrientationPacket orientationPacket = new OrientationPacket(intent.getExtras());
                    //yaw and roll switched due to necessary coordinate system transformation
                    pitchTV.setText(String.valueOf(orientationPacket.getOrientation().getPitch()));
                    yawTV.setText(String.valueOf(orientationPacket.getOrientation().getRoll()));
                    rollTV.setText(String.valueOf(orientationPacket.getOrientation().getYaw()));
                //If the intent is type gps packet, update corresponding textview values
                } else if (intent.getAction().equals(GPSPacket.INTENT_ACTION)) {
                    TextView gpsCoordinatesTV = (TextView) findViewById(R.id.tv_craft_value_gps_coordinates);
                    TextView gpsAccuracyTV = (TextView) findViewById(R.id.tv_craft_value_gps_accuracy);
                    TextView gpsBearingTV = (TextView) findViewById(R.id.tv_craft_value_bearing);
                    GPSPacket gpsPacket = new GPSPacket(intent.getExtras());
                    String coordinates = String.valueOf(gpsPacket.getLatitude()) + " ," + String.valueOf(gpsPacket.getLongitude());
                    String accuracy = String.valueOf(gpsPacket.getAccuracy()) + " m";
                    gpsCoordinatesTV.setText(coordinates);
                    gpsAccuracyTV.setText(accuracy);
                    if (gpsPacket.getBearing() == Double.NaN) {
                        gpsBearingTV.setText(getResources().getString(R.string.tv_placeholder_sensor));
                    } else {
                        String bearing = String.valueOf(gpsPacket.getBearing()) + " °";
                        gpsBearingTV.setText(bearing);
                    }
                //If the intent type is pressure packet, update corresponding textview value
                } else if (intent.getAction().equals(PressurePacket.INTENT_ACTION)) {
                    TextView pressureTV = (TextView) findViewById(R.id.tv_craft_value_barometer);
                    PressurePacket pressurePacket = new PressurePacket(intent.getExtras());
                    String altitude = String.valueOf(pressurePacket.getPressure()) + " hPa";
                    pressureTV.setText(altitude);
                }
            }
        };
    }

}

package com.rabidllamastudios.avigate.activities;

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

import com.rabidllamastudios.avigate.R;
import com.rabidllamastudios.avigate.helpers.PermissionsChecker;
import com.rabidllamastudios.avigate.models.AngularVelocityPacket;
import com.rabidllamastudios.avigate.models.GPSPacket;
import com.rabidllamastudios.avigate.models.LinearAccelerationPacket;
import com.rabidllamastudios.avigate.models.MagneticFieldPacket;
import com.rabidllamastudios.avigate.models.OrientationPacket;
import com.rabidllamastudios.avigate.services.SensorService;

public class DisplaySensorActivity extends AppCompatActivity {

    private Intent mSensorService;
    //Sensor update rate in microseconds
    private static final int SENSOR_UPDATE_RATE = SensorManager.SENSOR_DELAY_UI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_sensor);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.title_activity_display_sensor);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Create broadcast receiver and listen for specific intents
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(OrientationPacket.INTENT_ACTION);
        intentFilter.addAction(LinearAccelerationPacket.INTENT_ACTION);
        intentFilter.addAction(AngularVelocityPacket.INTENT_ACTION);
        intentFilter.addAction(MagneticFieldPacket.INTENT_ACTION);
        intentFilter.addAction(GPSPacket.INTENT_ACTION);
        registerReceiver(mSensorDataReceiver, intentFilter);

        //Configure the sensor service intent
        mSensorService = SensorService.getConfiguredIntent(this, SENSOR_UPDATE_RATE);

        //Check permissions before starting the sensor service
        PermissionsChecker permissionsChecker =
                new PermissionsChecker(this, mPermissionsCheckerCallback);
        if (permissionsChecker.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION,
                PermissionsChecker.PERMISSIONS_REQUEST_READ_LOCATION_FINE)) {
            startService(mSensorService);
        } else {
            //TODO warn the user about permissions needed
        }
    }

    // If the user allows location permissions, start the sensor service
    private PermissionsChecker.Callback mPermissionsCheckerCallback = new PermissionsChecker.Callback() {
        @Override
        public void permissionGranted(int permissionsConstant) {
            if (permissionsConstant == PermissionsChecker.PERMISSIONS_REQUEST_READ_LOCATION_FINE) {
                startService(mSensorService);
            }
        }
    };

    private BroadcastReceiver mSensorDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //If the intent is type orientation packet, update TextView values
            if (intent.getAction().equals(OrientationPacket.INTENT_ACTION)) {
                TextView orientationXTV =
                        (TextView) findViewById(R.id.tv_sensor_value_orientation_x);
                TextView orientationYTV =
                        (TextView) findViewById(R.id.tv_sensor_value_orientation_y);
                TextView orientationZTV =
                        (TextView) findViewById(R.id.tv_sensor_value_orientation_z);
                //y and z switched due to necessary coordinate system transformation
                OrientationPacket orientationPacket = new OrientationPacket(intent.getExtras());
                orientationXTV.setText(String.valueOf(orientationPacket.getRawOrientation().x));
                orientationYTV.setText(String.valueOf(orientationPacket.getRawOrientation().z));
                orientationZTV.setText(String.valueOf(orientationPacket.getRawOrientation().y));
            //If the intent is type linear acceleration packet, update TextView values
            } else if (intent.getAction().equals(LinearAccelerationPacket.INTENT_ACTION)) {
                TextView linearXTV =
                        (TextView) findViewById(R.id.tv_sensor_value_linear_acceleration_x);
                TextView linearYTV =
                        (TextView) findViewById(R.id.tv_sensor_value_linear_acceleration_y);
                TextView linearZTV =
                        (TextView) findViewById(R.id.tv_sensor_value_linear_acceleration_z);
                //yaw and roll switched due to necessary coordinate system transformation
                LinearAccelerationPacket linearAccelerationPacket =
                        new LinearAccelerationPacket(intent.getExtras());
                linearXTV.setText(String.valueOf(linearAccelerationPacket.getX()));
                linearYTV.setText(String.valueOf(linearAccelerationPacket.getZ()));
                linearZTV.setText(String.valueOf(linearAccelerationPacket.getY()));
            //If the intent is type angular velocity packet, update TextView values
            } else if (intent.getAction().equals(AngularVelocityPacket.INTENT_ACTION)) {
                TextView angularXTV =
                        (TextView) findViewById(R.id.tv_sensor_value_angular_velocity_x);
                TextView angularYTV =
                        (TextView) findViewById(R.id.tv_sensor_value_angular_velocity_y);
                TextView angularZTV =
                        (TextView) findViewById(R.id.tv_sensor_value_angular_velocity_z);
                //yaw and roll switched due to necessary coordinate system transformation
                AngularVelocityPacket angularAccelerationPacket =
                        new AngularVelocityPacket(intent.getExtras());
                angularXTV.setText(String.valueOf(angularAccelerationPacket.getX()));
                angularYTV.setText(String.valueOf(angularAccelerationPacket.getZ()));
                angularZTV.setText(String.valueOf(angularAccelerationPacket.getY()));
            //If the intent is type magnetic field packet, update TextView values
            } else if (intent.getAction().equals(MagneticFieldPacket.INTENT_ACTION)) {
                TextView magneticXTV =
                        (TextView) findViewById(R.id.tv_sensor_value_magnetic_field_x);
                TextView magneticYTV =
                        (TextView) findViewById(R.id.tv_sensor_value_magnetic_field_y);
                TextView magneticZTV =
                        (TextView) findViewById(R.id.tv_sensor_value_magnetic_field_z);
                //TODO determine if magnetic field data needs a coordinate transformation
                MagneticFieldPacket magneticFieldPacket =
                        new MagneticFieldPacket(intent.getExtras());
                magneticXTV.setText(String.valueOf(magneticFieldPacket.getX()));
                magneticYTV.setText(String.valueOf(magneticFieldPacket.getY()));
                magneticZTV.setText(String.valueOf(magneticFieldPacket.getZ()));
            //If the intent is type gps packet, update corresponding textview values
            } else if (intent.getAction().equals(GPSPacket.INTENT_ACTION)) {
                TextView gpsCoordinatesTV = (TextView) findViewById(R.id.tv_sensor_value_gps);
                TextView gpsBearingTV = (TextView) findViewById(R.id.tv_sensor_value_bearing);
                TextView gpsAltitudeTV = (TextView) findViewById(R.id.tv_sensor_value_altitude);
                GPSPacket gpsPacket = new GPSPacket(intent.getExtras());
                String coordinates = String.valueOf(gpsPacket.getLatitude()) + ", "
                        + String.valueOf(gpsPacket.getLongitude());
                gpsCoordinatesTV.setText(coordinates);
                if (gpsPacket.getBearing() == Double.NaN) {
                    gpsBearingTV.setText(getResources().getString(R.string.tv_placeholder_sensor));
                } else {
                    String bearing = String.valueOf(gpsPacket.getBearing()) + " °";
                    gpsBearingTV.setText(bearing);
                }
                if (gpsPacket.getAltitude() == Double.NaN) {
                    gpsAltitudeTV.setText(getResources().getString(R.string.tv_placeholder_sensor));
                } else {
                    String altitude = String.valueOf(gpsPacket.getAltitude()) + " m";
                    gpsAltitudeTV.setText(altitude);
                }
            }
        }
    };

    @Override
    public void onDestroy() {
        stopService(mSensorService);
        unregisterReceiver(mSensorDataReceiver);
        super.onDestroy();
    }
}
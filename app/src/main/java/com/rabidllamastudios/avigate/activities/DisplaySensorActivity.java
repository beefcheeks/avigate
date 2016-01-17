package com.rabidllamastudios.avigate.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.rabidllamastudios.avigate.R;
import com.rabidllamastudios.avigate.helpers.PermissionsChecker;
import com.rabidllamastudios.avigate.models.CraftStatePacket;
import com.rabidllamastudios.avigate.services.SensorService;

public class DisplaySensorActivity extends AppCompatActivity {

    private Intent mSensorService = null;
    //Sensor broadcast rate in milliseconds (ms)
    private static final int SENSOR_BROADCAST_RATE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_sensor);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.title_activity_display_sensor);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Register broadcast receiver for any CraftStatePacket (sensor-related) Intents
        registerReceiver(mCraftStateReceiver, new IntentFilter(CraftStatePacket.INTENT_ACTION));

        //Configure the SensorService Intent
        mSensorService = SensorService.getConfiguredIntent(this, SENSOR_BROADCAST_RATE);

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

    //Listens for CraftStatePacket Intents and updates various TextViews accordingly
    private BroadcastReceiver mCraftStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //If the Intent is type CraftStatePacket, update correspondingTextView values
            if (intent.getAction().equals(CraftStatePacket.INTENT_ACTION)) {
                CraftStatePacket craftStatePacket = new CraftStatePacket(intent.getExtras());
                //Process orientation data and update corresponding TextViews
                CraftStatePacket.Orientation orientation = craftStatePacket.getOrientation();
                TextView orientationXTV =
                        (TextView) findViewById(R.id.tv_sensor_value_orientation_x);
                TextView orientationYTV =
                        (TextView) findViewById(R.id.tv_sensor_value_orientation_y);
                TextView orientationZTV =
                        (TextView) findViewById(R.id.tv_sensor_value_orientation_z);
                orientationXTV.setText(String.valueOf(orientation.getRawOrientation().x));
                orientationYTV.setText(String.valueOf(orientation.getRawOrientation().y));
                orientationZTV.setText(String.valueOf(orientation.getRawOrientation().z));

                //Process linear acceleration data and update corresponding TextViews
                CraftStatePacket.LinearAcceleration linearAcceleration =
                        craftStatePacket.getLinearAcceleration();
                TextView linearXTV =
                        (TextView) findViewById(R.id.tv_sensor_value_linear_acceleration_x);
                TextView linearYTV =
                        (TextView) findViewById(R.id.tv_sensor_value_linear_acceleration_y);
                TextView linearZTV =
                        (TextView) findViewById(R.id.tv_sensor_value_linear_acceleration_z);
                linearXTV.setText(String.valueOf(linearAcceleration.getX()));
                linearYTV.setText(String.valueOf(linearAcceleration.getY()));
                linearZTV.setText(String.valueOf(linearAcceleration.getZ()));

                //Process angular velocity data and update corresponding TextViews
                CraftStatePacket.AngularVelocity angularVelocity =
                        craftStatePacket.getAngularVelocity();
                TextView angularXTV =
                        (TextView) findViewById(R.id.tv_sensor_value_angular_velocity_x);
                TextView angularYTV =
                        (TextView) findViewById(R.id.tv_sensor_value_angular_velocity_y);
                TextView angularZTV =
                        (TextView) findViewById(R.id.tv_sensor_value_angular_velocity_z);
                angularXTV.setText(String.valueOf(angularVelocity.getX()));
                angularYTV.setText(String.valueOf(angularVelocity.getY()));
                angularZTV.setText(String.valueOf(angularVelocity.getZ()));

                //Process magnetic field values and update corresponding TextViews
                CraftStatePacket.MagneticField magneticField = craftStatePacket.getMagneticField();
                TextView magneticXTV =
                        (TextView) findViewById(R.id.tv_sensor_value_magnetic_field_x);
                TextView magneticYTV =
                        (TextView) findViewById(R.id.tv_sensor_value_magnetic_field_y);
                TextView magneticZTV =
                        (TextView) findViewById(R.id.tv_sensor_value_magnetic_field_z);
                magneticXTV.setText(String.valueOf(magneticField.getX()));
                magneticYTV.setText(String.valueOf(magneticField.getY()));
                magneticZTV.setText(String.valueOf(magneticField.getZ()));

                //Process location data and update corresponding TextViews
                Location location = craftStatePacket.getLocation();
                String coordinates = String.valueOf(location.getLatitude()) + ", "
                        + String.valueOf(location.getLongitude());
                String bearing = String.valueOf(location.getBearing()) + " °";
                String altitude = String.valueOf(location.getAltitude()) + " m";

                TextView gpsCoordinatesTV = (TextView) findViewById(R.id.tv_sensor_value_gps);
                TextView gpsBearingTV = (TextView) findViewById(R.id.tv_sensor_value_bearing);
                TextView gpsAltitudeTV = (TextView) findViewById(R.id.tv_sensor_value_altitude);

                gpsCoordinatesTV.setText(coordinates);
                gpsBearingTV.setText(bearing);
                gpsAltitudeTV.setText(altitude);
            }
        }
    };

    @Override
    public void onDestroy() {
        //Unregister all receivers
        unregisterReceiver(mCraftStateReceiver);
        //Stop all services (if running)
        if (mSensorService != null) stopService(mSensorService);
        //Call super method
        super.onDestroy();
    }
}
package com.rabidllamastudios.avigate;

import android.Manifest;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

public class DisplaySensorActivity extends AppCompatActivity implements SensorEventListener {

    private LocationManager locationManager;
    private LocationListener locationListener;
    private SensorManager mSensorManager;

    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private Sensor mOrientation;
    private Sensor mCompass;

    private float[] linearAccelerationVector;
    private float[] angularAccelerationVector;
    private float[] orientationVector;
    private float[] magneticFieldVector;

    private double[] gpsVector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_sensor);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.title_activity_display_sensor);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        startLocationCheck();
        startSensors();
    }

    public boolean hasLocationPermissions() {
        //check location permissions
        PermissionsCheck permCheck = new PermissionsCheck();
        return permCheck.hasPermission(this, Manifest.permission.ACCESS_FINE_LOCATION, permCheck.PERMISSIONS_REQUEST_READ_LOCATION_FINE);
    }

    public void startLocationCheck() {
        if (hasLocationPermissions()) {
            gpsVector = new double[4];
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    updateLocation(location);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            };
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
    }

    public void startSensors() {

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mCompass = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        linearAccelerationVector = new float[3];
        angularAccelerationVector = new float[3];
        orientationVector = new float[3];
        magneticFieldVector = new float[3];
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            updateLinearAccelerationValues(event);
        } else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            updateAngularAccelerationValues(event);
        } else if (sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            updateOrientationValues(event);
        } else if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            updateMagneticFieldValues(event);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void updateLocation(Location location) {
        gpsVector[0] = location.getLatitude();
        gpsVector[1] = location.getLongitude();

        TextView coordinates = (TextView) findViewById(R.id.tv_sensor_value_gps);
        TextView bearing = (TextView) findViewById(R.id.tv_sensor_value_bearing);
        TextView altitude = (TextView) findViewById(R.id.tv_sensor_value_altitude);

        coordinates.setText(String.valueOf(gpsVector[0]) + ", " + String.valueOf(gpsVector[1]));

        if (location.hasBearing()) {
            gpsVector[2] = location.getBearing();
            bearing.setText(String.valueOf(gpsVector[2]) + " °");
        } else {
            bearing.setText(getString(R.string.tv_sensor_placeholder));
        }

        if (location.hasAltitude()) {
            gpsVector[3] = location.getAltitude();
            altitude.setText(String.valueOf(gpsVector[3]) + " m");
        } else {
            altitude.setText(getString(R.string.tv_sensor_placeholder));
        }
    }

    public void updateLinearAccelerationValues(SensorEvent event) {
        linearAccelerationVector[0] = event.values[0];
        linearAccelerationVector[1] = event.values[1];
        linearAccelerationVector[2] = event.values[2];
        TextView linearAccelerationX = (TextView) findViewById(R.id.tv_sensor_value_linear_acceleration_x);
        TextView linearAccelerationY = (TextView) findViewById(R.id.tv_sensor_value_linear_acceleration_y);
        TextView linearAccelerationZ = (TextView) findViewById(R.id.tv_sensor_value_linear_acceleration_z);
        linearAccelerationX.setText(String.valueOf(linearAccelerationVector[0]));
        linearAccelerationY.setText(String.valueOf(linearAccelerationVector[1]));
        linearAccelerationZ.setText(String.valueOf(linearAccelerationVector[2]));
    }

    public void updateAngularAccelerationValues(SensorEvent event) {
        angularAccelerationVector[0] = event.values[0];
        angularAccelerationVector[1] = event.values[1];
        angularAccelerationVector[2] = event.values[2];
        TextView angularAccelerationX = (TextView) findViewById(R.id.tv_sensor_value_angular_acceleration_x);
        TextView angularAccelerationY = (TextView) findViewById(R.id.tv_sensor_value_angular_acceleration_y);
        TextView angularAccelerationZ = (TextView) findViewById(R.id.tv_sensor_value_angular_acceleration_z);
        angularAccelerationX.setText(String.valueOf(angularAccelerationVector[0]));
        angularAccelerationY.setText(String.valueOf(angularAccelerationVector[1]));
        angularAccelerationZ.setText(String.valueOf(angularAccelerationVector[2]));
    }

    public void updateOrientationValues(SensorEvent event) {
        orientationVector[0] = event.values[0];
        orientationVector[1] = event.values[1];
        orientationVector[2] = event.values[2];
        TextView orientationX = (TextView) findViewById(R.id.tv_sensor_value_orientation_x);
        TextView orientationY = (TextView) findViewById(R.id.tv_sensor_value_orientation_y);
        TextView orientationZ = (TextView) findViewById(R.id.tv_sensor_value_orientation_z);
        orientationX.setText(String.valueOf(orientationVector[0]));
        orientationY.setText(String.valueOf(orientationVector[1]));
        orientationZ.setText(String.valueOf(orientationVector[2]));
    }

    public void updateMagneticFieldValues(SensorEvent event) {
        magneticFieldVector[0] = event.values[0];
        magneticFieldVector[1] = event.values[1];
        magneticFieldVector[2] = event.values[2];
        TextView magneticFieldX = (TextView) findViewById(R.id.tv_sensor_value_magnetic_field_x);
        TextView magneticFieldY = (TextView) findViewById(R.id.tv_sensor_value_magnetic_field_y);
        TextView magneticFieldZ = (TextView) findViewById(R.id.tv_sensor_value_magnetic_field_z);
        magneticFieldX.setText(String.valueOf(magneticFieldVector[0]));
        magneticFieldY.setText(String.valueOf(magneticFieldVector[1]));
        magneticFieldZ.setText(String.valueOf(magneticFieldVector[2]));
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        if (hasLocationPermissions()) {
            locationManager.removeUpdates(locationListener);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mCompass, SensorManager.SENSOR_DELAY_NORMAL);
        if (hasLocationPermissions()) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
    }

}
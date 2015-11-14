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

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private SensorManager mSensorManager;

    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private Sensor mOrientation;
    private Sensor mCompass;

    private float[] mLinearAccelerationVector;
    private float[] mAngularAccelerationVector;
    private float[] mOrientationVector;
    private float[] mMagneticFieldVector;

    private double[] mGpsVector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_sensor);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.title_activity_display_sensor);

        assert getSupportActionBar() != null;
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
            mGpsVector = new double[4];
            mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            mLocationListener = new LocationListener() {
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
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
        }
    }

    public void startSensors() {

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mCompass = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mLinearAccelerationVector = new float[3];
        mAngularAccelerationVector = new float[3];
        mOrientationVector = new float[3];
        mMagneticFieldVector = new float[3];
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
        mGpsVector[0] = location.getLatitude();
        mGpsVector[1] = location.getLongitude();

        TextView coordinates = (TextView) findViewById(R.id.tv_sensor_value_gps);
        TextView bearing = (TextView) findViewById(R.id.tv_sensor_value_bearing);
        TextView altitude = (TextView) findViewById(R.id.tv_sensor_value_altitude);

        String gpsCoordinatesValue = String.valueOf(mGpsVector[0]) + ", " + String.valueOf(mGpsVector[1]);
        coordinates.setText(gpsCoordinatesValue);

        if (location.hasBearing()) {
            mGpsVector[2] = location.getBearing();
            String bearingValue = String.valueOf(mGpsVector[2] + " °");
            bearing.setText(bearingValue);
        } else {
            bearing.setText(getString(R.string.tv_placeholder_sensor));
        }

        if (location.hasAltitude()) {
            mGpsVector[3] = location.getAltitude();
            String altitudeValue = String.valueOf(mGpsVector[3] + " m");
            altitude.setText(altitudeValue);
        } else {
            altitude.setText(getString(R.string.tv_placeholder_sensor));
        }
    }

    public void updateLinearAccelerationValues(SensorEvent event) {
        mLinearAccelerationVector[0] = event.values[0];
        mLinearAccelerationVector[1] = event.values[1];
        mLinearAccelerationVector[2] = event.values[2];
        TextView linearAccelerationX = (TextView) findViewById(R.id.tv_sensor_value_linear_acceleration_x);
        TextView linearAccelerationY = (TextView) findViewById(R.id.tv_sensor_value_linear_acceleration_y);
        TextView linearAccelerationZ = (TextView) findViewById(R.id.tv_sensor_value_linear_acceleration_z);
        linearAccelerationX.setText(String.valueOf(mLinearAccelerationVector[0]));
        linearAccelerationY.setText(String.valueOf(mLinearAccelerationVector[1]));
        linearAccelerationZ.setText(String.valueOf(mLinearAccelerationVector[2]));
    }

    public void updateAngularAccelerationValues(SensorEvent event) {
        mAngularAccelerationVector[0] = event.values[0];
        mAngularAccelerationVector[1] = event.values[1];
        mAngularAccelerationVector[2] = event.values[2];
        TextView angularAccelerationX = (TextView) findViewById(R.id.tv_sensor_value_angular_acceleration_x);
        TextView angularAccelerationY = (TextView) findViewById(R.id.tv_sensor_value_angular_acceleration_y);
        TextView angularAccelerationZ = (TextView) findViewById(R.id.tv_sensor_value_angular_acceleration_z);
        angularAccelerationX.setText(String.valueOf(mAngularAccelerationVector[0]));
        angularAccelerationY.setText(String.valueOf(mAngularAccelerationVector[1]));
        angularAccelerationZ.setText(String.valueOf(mAngularAccelerationVector[2]));
    }

    public void updateOrientationValues(SensorEvent event) {
        mOrientationVector[0] = event.values[0];
        mOrientationVector[1] = event.values[1];
        mOrientationVector[2] = event.values[2];
        TextView orientationX = (TextView) findViewById(R.id.tv_sensor_value_orientation_x);
        TextView orientationY = (TextView) findViewById(R.id.tv_sensor_value_orientation_y);
        TextView orientationZ = (TextView) findViewById(R.id.tv_sensor_value_orientation_z);
        orientationX.setText(String.valueOf(mOrientationVector[0]));
        orientationY.setText(String.valueOf(mOrientationVector[1]));
        orientationZ.setText(String.valueOf(mOrientationVector[2]));
    }

    public void updateMagneticFieldValues(SensorEvent event) {
        mMagneticFieldVector[0] = event.values[0];
        mMagneticFieldVector[1] = event.values[1];
        mMagneticFieldVector[2] = event.values[2];
        TextView magneticFieldX = (TextView) findViewById(R.id.tv_sensor_value_magnetic_field_x);
        TextView magneticFieldY = (TextView) findViewById(R.id.tv_sensor_value_magnetic_field_y);
        TextView magneticFieldZ = (TextView) findViewById(R.id.tv_sensor_value_magnetic_field_z);
        magneticFieldX.setText(String.valueOf(mMagneticFieldVector[0]));
        magneticFieldY.setText(String.valueOf(mMagneticFieldVector[1]));
        magneticFieldZ.setText(String.valueOf(mMagneticFieldVector[2]));
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        if (hasLocationPermissions()) {
            mLocationManager.removeUpdates(mLocationListener);
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
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
        }
    }

}
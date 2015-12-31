package com.rabidllamastudios.avigate;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;

import com.rabidllamastudios.avigate.model.AngularVelocityPacket;
import com.rabidllamastudios.avigate.model.GPSPacket;
import com.rabidllamastudios.avigate.model.LinearAccelerationPacket;
import com.rabidllamastudios.avigate.model.MagneticFieldPacket;
import com.rabidllamastudios.avigate.model.OrientationPacket;
import com.rabidllamastudios.avigate.model.PressurePacket;

import org.rajawali3d.math.Quaternion;

public class SensorService extends Service implements SensorEventListener {

    private static final String PACKAGE_NAME = AvigateApplication.class.getPackage().getName();
    public static final String SENSOR_RATE = PACKAGE_NAME + ".extra.SENSOR_RATE";

    private static final int DEFAULT_SENSOR_RATE = SensorManager.SENSOR_DELAY_UI;

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private SensorManager mSensorManager;

    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private Sensor mOrientation;
    private Sensor mCompass;
    private Sensor mBarometer;

    private Quaternion mPhoneOrientationQuaternion;

    public SensorService() {
    }

    public static Intent getConfiguredIntent(Context context, int sensorRate) {
        Intent intent = new Intent(context, SensorService.class);
        intent.putExtra(SENSOR_RATE, sensorRate);
        return intent;
    }

    @Override
    public void onCreate() {
        //Initialize phone orientation quaternion
        mPhoneOrientationQuaternion = new Quaternion();

        //Initialize mSensorManager and associated sensors
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mCompass = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mBarometer = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

        //Initialize LocationManager and LocationListener
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double[] gpsVector = new double[5];
                gpsVector[0] = location.getLatitude();
                gpsVector[1] = location.getLongitude();
                gpsVector[2] = location.getAccuracy();
                if (location.hasBearing()) {
                    gpsVector[3] = location.getBearing();
                } else {
                    gpsVector[3] = Double.NaN;
                }
                if (location.hasAltitude()) {
                    gpsVector[4] = location.getAltitude();
                } else {
                    gpsVector[4] = Double.NaN;
                }
                Intent gpsIntent = new GPSPacket(gpsVector[0], gpsVector[1], gpsVector[2],
                        gpsVector[3], gpsVector[4]).toIntent();
                sendBroadcast(gpsIntent);
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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            //Set sensor rate as specified in the intent extra, if none, set to default value
            int sensorRate = intent.getIntExtra(SENSOR_RATE, DEFAULT_SENSOR_RATE);

            //Set sensor refresh rate and start sensors
            mSensorManager.registerListener(this, mAccelerometer, sensorRate);
            mSensorManager.registerListener(this, mGyroscope, sensorRate);
            mSensorManager.registerListener(this, mOrientation, sensorRate);
            mSensorManager.registerListener(this, mCompass, sensorRate);
            mSensorManager.registerListener(this, mBarometer, sensorRate);

            //TODO make GPS refresh rate configurable via Intent
            //Start GPS using fastest rate (0)
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
                    mLocationListener);

        }
        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            Intent linearAccelerationIntent = new LinearAccelerationPacket(event.values[0],
                    event.values[1], event.values[2]).toIntent();
            sendBroadcast(linearAccelerationIntent);
        } else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            Intent angularVelocityIntent = new AngularVelocityPacket(event.values[0],
                    event.values[1], event.values[2]).toIntent();
            sendBroadcast(angularVelocityIntent);
        } else if (sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            mPhoneOrientationQuaternion.setAll(event.values[3], event.values[0], event.values[1],
                    event.values[2]);
            Intent orientationIntent =
                    new OrientationPacket(mPhoneOrientationQuaternion).toIntent();
            sendBroadcast(orientationIntent);
        } else if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            Intent magneticFieldIntent = new MagneticFieldPacket(event.values[0], event.values[1],
                    event.values[2]).toIntent();
            sendBroadcast(magneticFieldIntent);
        } else if (sensor.getType() == Sensor.TYPE_PRESSURE) {
            Intent pressureIntent = new PressurePacket(event.values[0]).toIntent();
            sendBroadcast(pressureIntent);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this);
        mLocationManager.removeUpdates(mLocationListener);
        super.onDestroy();
    }
}

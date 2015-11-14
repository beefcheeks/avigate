package com.rabidllamastudios.avigate;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;

import com.rabidllamastudios.avigate.model.OrientationPacket;

import org.rajawali3d.math.Quaternion;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class CraftActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mOrientation;
    private Quaternion mPhoneOrientationQuaternion;

    //Sensor update rate in ms
    //TODO adjust sensor rate with latency?
    private static final int SENSOR_UPDATE_RATE_ORIENTATION = 50;
    private long lastTime = -1;
    private float avgTime;

    private Intent mCommService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_craft);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Initialize sensors
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        //Initialize phone orientation quaternion
        mPhoneOrientationQuaternion = new Quaternion();

        //Start the communications service.
        List<String> localSubs = new ArrayList<>();
        List<String> remoteSubs = new ArrayList<>();
        localSubs.add(OrientationPacket.INTENT_ACTION);
        mCommService = CommunicationsService.getConfiguredIntent(this, localSubs, remoteSubs, CommunicationsService.DeviceType.CRAFT);
        startService(mCommService);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            if (lastTime != -1) {
                avgTime = 0.9f * avgTime + 0.1f * ((System.nanoTime() - lastTime)*0.000001f);
            }
            long dur = System.nanoTime() - lastTime;
            if (dur < SENSOR_UPDATE_RATE_ORIENTATION * 1000000) return;
            lastTime = System.nanoTime();
            Log.i("xmitter", "average sensor interval:" + avgTime);
            //when orientation update is received, set phone quaternion equal to latest values
            mPhoneOrientationQuaternion.setAll(event.values[3], event.values[0], event.values[1], event.values[2]);
            Intent out = new OrientationPacket(mPhoneOrientationQuaternion).toIntent();
            sendBroadcast(out);

            TextView pitchTV = (TextView) findViewById(R.id.tv_craft_value_pitch);
            TextView yawTV = (TextView) findViewById(R.id.tv_craft_value_yaw);
            TextView rollTV = (TextView) findViewById(R.id.tv_craft_value_roll);

            //Display orientation output in realtime
            pitchTV.setText(NumberFormat.getInstance().format(mPhoneOrientationQuaternion.getPitch()));
            //getRoll and getYaw switched due to non-standard phone orientation while in plane
            yawTV.setText(NumberFormat.getInstance().format(mPhoneOrientationQuaternion.getRoll()));
            rollTV.setText(NumberFormat.getInstance().format(mPhoneOrientationQuaternion.getYaw()));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mOrientation, SENSOR_UPDATE_RATE_ORIENTATION*1000);
    }

    @Override
    public void onDestroy() {
        stopService(mCommService);
        super.onDestroy();
    }


}

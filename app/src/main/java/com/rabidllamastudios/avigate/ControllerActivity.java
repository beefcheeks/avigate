package com.rabidllamastudios.avigate;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ViewGroup;

import com.rabidllamastudios.avigate.model.OrientationPacket;

import org.rajawali3d.surface.IRajawaliSurface;
import org.rajawali3d.surface.RajawaliSurfaceView;

import java.util.ArrayList;
import java.util.List;

public class ControllerActivity extends AppCompatActivity {

    private Intent mCommService;
    private BroadcastReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Create 3d scene using rajawali 3d library
        final RajawaliSurfaceView surface = new RajawaliSurfaceView(this);
        surface.setFrameRate(60);
        surface.setRenderMode(IRajawaliSurface.RENDERMODE_WHEN_DIRTY);
        addContentView(surface, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT));

        //Create new flight renderer to handle aircraft and camera orientations
        final FlightRenderer flightRenderer = new FlightRenderer(this);
        surface.setSurfaceRenderer(flightRenderer);

        //register for updates or orientation data.
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i("ControllerActivity", "Received OrientationPacket Intent");
                OrientationPacket packet = new OrientationPacket(intent.getExtras());
                //update FlightRenderer with quaternion values
                flightRenderer.setAircraftOrientationQuaternion(packet.mOrientation);

            }
        };
        registerReceiver(mReceiver, new IntentFilter(OrientationPacket.INTENT_ACTION));

        //start the communications service.
        List<String> localSubs = new ArrayList<>();
        List<String> remoteSubs = new ArrayList<>();
        remoteSubs.add(OrientationPacket.INTENT_ACTION);
        mCommService = CommunicationsService.getConfiguredIntent(this, localSubs, remoteSubs, CommunicationsService.DeviceType.CONTROLLER);
        startService(mCommService);

    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        stopService(mCommService);
        super.onDestroy();
    }

}

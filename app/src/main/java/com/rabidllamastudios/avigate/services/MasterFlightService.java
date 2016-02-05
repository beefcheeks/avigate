package com.rabidllamastudios.avigate.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.rabidllamastudios.avigate.AvigateApplication;
import com.rabidllamastudios.avigate.R;
import com.rabidllamastudios.avigate.activities.CraftActivity;
import com.rabidllamastudios.avigate.models.ArduinoPacket;
import com.rabidllamastudios.avigate.models.CraftStatePacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for managing all other services needed to manage the craft during flight
 * Will eventually be able to remotely receive stop and start commands for specific services
 * Created by Ryan Staatz on 1/18/2016
 */
public class MasterFlightService extends Service {
    private static final String CLASS_NAME = MasterFlightService.class.getSimpleName();
    private static final String PACKAGE_NAME = AvigateApplication.class.getPackage().getName();

    //Intent actions associated with the MasterFlightService class
    private static final String INTENT_ACTION_CONFIGURE_CRAFT_SERVICE =
            PACKAGE_NAME + ".action.CONFIGURE_CRAFT_SERVICE";
    private static final String INTENT_ACTION_FOREGROUND_CRAFT_SERVICE =
            PACKAGE_NAME + ".action.FOREGROUND_CRAFT_SERVICE";
    private static final String INTENT_ACTION_STOP_CRAFT_SERVICE =
            PACKAGE_NAME + ".action.STOP_FOREGROUND_CRAFT_SERVICE";

    //Unique foreground notification id
    private static final int NOTIFICATION_ID = 843;
    //Sensor broadcast rate in milliseconds (ms)
    private static final int SENSOR_BROADCAST_RATE = 100;

    //Intents corresponding to various services
    private Intent mFlightControlService = null;
    private Intent mNetworkService = null;
    private Intent mSensorService = null;
    private Intent mUsbSerialService = null;

    public MasterFlightService() {}

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(INTENT_ACTION_CONFIGURE_CRAFT_SERVICE)) {
            Log.i(CLASS_NAME, "Service started");
            showForegroundNotification();  //Start the MasterFlightService in the foreground
            registerBroadcastReceivers();  //Register all relevant BroadcastReceivers
            startOtherServices();  //Start other services
        } else if (intent.getAction().equals(INTENT_ACTION_STOP_CRAFT_SERVICE)) {
            stopForeground(true);
            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        //Unregister all receivers
        unregisterReceiver(mStartServiceReceiver);
        //Stop all services (if running)
        if (mNetworkService != null) stopService(mNetworkService);
        if (mFlightControlService != null) stopService(mFlightControlService);
        if (mSensorService != null) stopService(mSensorService);
        if (mUsbSerialService != null) stopService(mUsbSerialService);
        //Call super method
        Log.i(CLASS_NAME, "Service stopped");
        super.onDestroy();
    }

    /** Returns a configured Intent that can be used to start this service (MasterFlightService) */
    public static Intent getConfiguredIntent(Context context){
        Intent intent = new Intent(context, MasterFlightService.class);
        intent.setAction(INTENT_ACTION_CONFIGURE_CRAFT_SERVICE);
        return intent;
    }

    //Runs the service in the foreground and displays a foreground service ongoing notification
    private void showForegroundNotification() {
        //Create notification Intent & associated PendingIntent, and configure it accordingly
        Intent notificationIntent = new Intent(this, CraftActivity.class);
        notificationIntent.setAction(INTENT_ACTION_FOREGROUND_CRAFT_SERVICE);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent notificationPendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        //Create the Intent that stops this service and configure it accordingly
        Intent stopServiceIntent = new Intent (this, MasterFlightService.class);
        stopServiceIntent.setAction(INTENT_ACTION_STOP_CRAFT_SERVICE);
        PendingIntent stopServicePendingIntent = PendingIntent.getService(this, 0,
                stopServiceIntent, 0);
        //Create the foreground notification used to manage this service, including a stop button
        Notification foregroundNotification = new NotificationCompat.Builder(this)
                .addAction(R.drawable.ic_stop_black_24dp, "Stop",
                        stopServicePendingIntent)
                .setContentIntent(notificationPendingIntent)
                .setContentText("Service is running")
                .setContentTitle("Avigate Flight Service")
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_flight_takeoff_black_24dp)
                .setTicker("Avigate Flight Service").build();
        //Start the foreground service
        startForeground(NOTIFICATION_ID, foregroundNotification);
    }

    //Registers mStartServiceReceiver BroadcastReceiver to listen for service-starting Intents
    private void registerBroadcastReceivers() {
        IntentFilter startServiceIntentFilter = new IntentFilter();
        startServiceIntentFilter.addAction(
                FlightControlService.INTENT_ACTION_CONFIGURE_FLIGHT_CONTROL_SERVICE);
        registerReceiver(mStartServiceReceiver, startServiceIntentFilter);
    }

    //Starts other relevant services
    private void startOtherServices() {
        //Configure and start NetworkService.
        List<String> localSubs = new ArrayList<>();
        List<String> remoteSubs = new ArrayList<>();
        localSubs.add(CraftStatePacket.INTENT_ACTION);
        localSubs.add(ArduinoPacket.INTENT_ACTION_OUTPUT);
        localSubs.add(UsbSerialService.INTENT_ACTION_USB_READY);
        localSubs.add(UsbSerialService.INTENT_ACTION_USB_PERMISSION_GRANTED);
        localSubs.add(UsbSerialService.INTENT_ACTION_NO_USB);
        localSubs.add(UsbSerialService.INTENT_ACTION_USB_DISCONNECTED);
        localSubs.add(UsbSerialService.INTENT_ACTION_USB_NOT_SUPPORTED);
        localSubs.add(UsbSerialService.INTENT_ACTION_USB_PERMISSION_NOT_GRANTED);
        remoteSubs.add(FlightControlService.INTENT_ACTION_CONFIGURE_FLIGHT_CONTROL_SERVICE);
        remoteSubs.add(ArduinoPacket.INTENT_ACTION_INPUT);
        mNetworkService = NetworkService.getConfiguredIntent(this, localSubs, remoteSubs,
                NetworkService.DeviceType.CRAFT);
        startService(mNetworkService);
        //Configure and start SensorService
        mSensorService = SensorService.getConfiguredIntent(
                getApplicationContext(), SENSOR_BROADCAST_RATE);
        startService(mSensorService);
        //Configure and start UsbSerialService
        mUsbSerialService = UsbSerialService.getConfiguredIntent(this);
        startService(mUsbSerialService);
    }

    //Listens for any Intents that are used to start other services
    private BroadcastReceiver mStartServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(
                    FlightControlService.INTENT_ACTION_CONFIGURE_FLIGHT_CONTROL_SERVICE)) {
                Log.i(CLASS_NAME, "FlightControlService start command received");
                intent.setClass(getApplicationContext(), FlightControlService.class);
                mFlightControlService = intent;
                startService(mFlightControlService);
            }
        }
    };
}
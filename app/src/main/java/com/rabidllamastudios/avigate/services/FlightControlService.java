package com.rabidllamastudios.avigate.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import com.rabidllamastudios.avigate.AvigateApplication;
import com.rabidllamastudios.avigate.models.ArduinoPacket;

/**
 * Service responsible for maintaining craft stability
 */
public class FlightControlService extends Service {

    private static final String CLASS_NAME = FlightControlService.class.getSimpleName();
    private static final String PACKAGE_NAME = AvigateApplication.class.getPackage().getName();

    public static final String INTENT_ACTION_CONFIGURE_FLIGHT_CONTROL_SERVICE =
            PACKAGE_NAME + ".action.CONFIGURE_FLIGHT_CONTROL_SERVICE";
    public static final String EXTRA_CONFIG = PACKAGE_NAME + ".extra.CONFIG";

    //TODO implement receiver control logic
    private boolean mReceiverControl = false;
    private boolean mUsbSerialIsReady = false;

    private BroadcastReceiver mArduinoOutputReceiver = null;
    private ArduinoPacket mConfigArduinoPacket = null;

    public FlightControlService() {}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction().equals(
                INTENT_ACTION_CONFIGURE_FLIGHT_CONTROL_SERVICE)) {
            if (intent.hasExtra(EXTRA_CONFIG))
                mConfigArduinoPacket = new ArduinoPacket(intent.getStringExtra(EXTRA_CONFIG));

            if (mArduinoOutputReceiver != null) {
                unregisterReceiver(mArduinoOutputReceiver);
                mArduinoOutputReceiver = null;
            }
            mArduinoOutputReceiver = createArduinoOutputReceiver();
            IntentFilter intentFilter = new IntentFilter(ArduinoPacket.INTENT_ACTION_OUTPUT);
            registerReceiver(mArduinoOutputReceiver, intentFilter);
        }
        Log.i(CLASS_NAME, "Service started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mArduinoOutputReceiver != null) {
            unregisterReceiver(mArduinoOutputReceiver);
            mArduinoOutputReceiver = null;
        }
        Log.i(CLASS_NAME, "Service stopped");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static Intent getConfiguredIntent(ArduinoPacket configArduinoPacket){
        if (configArduinoPacket != null) {
            //Don't set class/component so that CommunicationsService can handle the intent
            Intent intent = new Intent(INTENT_ACTION_CONFIGURE_FLIGHT_CONTROL_SERVICE);
            intent.putExtra(EXTRA_CONFIG, configArduinoPacket.toJsonString());
            return intent;
        }
        return null;
    }

    //Listens for responses from the connected USB serial device and responds accordingly
    private BroadcastReceiver createArduinoOutputReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ArduinoPacket.INTENT_ACTION_OUTPUT)) {
                    ArduinoPacket arduinoPacket = new ArduinoPacket(intent.getExtras());

                    //If the device sent out a ready status
                    if (arduinoPacket.isStatusReady()) {
                        if (!mUsbSerialIsReady) mUsbSerialIsReady = true;
                        //Send the config for each servo to the Arduino
                        sendServoConfig(ArduinoPacket.ServoType.AILERON);
                        sendServoConfig(ArduinoPacket.ServoType.ELEVATOR);
                        sendServoConfig(ArduinoPacket.ServoType.RUDDER);
                        sendServoConfig(ArduinoPacket.ServoType.THROTTLE);
                        sendServoConfig(ArduinoPacket.ServoType.CUTOVER);
                    }

                    //If the ArduinoPacket contains the receiverControl json key, set mReceiverOnly
                    if (arduinoPacket.hasReceiverControl()) {
                        mReceiverControl = arduinoPacket.isReceiverControl();
                    }

                    //If the ArduinoPacket contains the calibrationMode, log it accordingly
                    if (arduinoPacket.hasCalibrationMode()) {
                        //If the arduino is in calibration mode, inform the user
                        String output = "Calibration Mode: "
                                + String.valueOf(arduinoPacket.isCalibrationMode());
                        Log.i(CLASS_NAME, output);
                    }

                    //If the ArduinoPacket contains receiver calibration ranges, log it accordingly
                    if (arduinoPacket.hasInputRanges()) {
                        Log.i(CLASS_NAME, "Calibration ranges received");
                    }

                    //If the ArduinoPacket contains an error message, log it accordingly
                    if (arduinoPacket.hasErrorMessage()) {
                        String error = "Error: " + arduinoPacket.getErrorMessage();
                        Log.i(CLASS_NAME, error);
                    }
                }
            }
        };
    }

    //Sends the configuration (minus the receiver input min and max) for a given ServoType
    private void sendServoConfig(ArduinoPacket.ServoType servoType) {
        String servoConfigJson = mConfigArduinoPacket.getConfigJson(servoType);
        if (servoConfigJson != null) {
            ArduinoPacket arduinoPacket = new ArduinoPacket(servoConfigJson);
            sendBroadcast(arduinoPacket.toIntent(ArduinoPacket.INTENT_ACTION_INPUT));
        }
        String servoInputRangeJson = mConfigArduinoPacket.getInputRangeJson(servoType);
        if (servoInputRangeJson != null) {
            ArduinoPacket arduinoPacket = new ArduinoPacket(servoInputRangeJson);
            sendBroadcast(arduinoPacket.toIntent(ArduinoPacket.INTENT_ACTION_INPUT));
        }
    }
}

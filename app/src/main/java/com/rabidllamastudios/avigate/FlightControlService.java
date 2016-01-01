package com.rabidllamastudios.avigate;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import com.rabidllamastudios.avigate.model.ServoPacket;

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
    private ServoPacket mConfigServoPacket = null;

    public FlightControlService() {}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction().equals(
                INTENT_ACTION_CONFIGURE_FLIGHT_CONTROL_SERVICE)) {
            if (intent.hasExtra(EXTRA_CONFIG))
                mConfigServoPacket = new ServoPacket(intent.getStringExtra(EXTRA_CONFIG));

            if (mArduinoOutputReceiver != null) {
                unregisterReceiver(mArduinoOutputReceiver);
                mArduinoOutputReceiver = null;
            }
            mArduinoOutputReceiver = createArduinoOutputReceiver();
            IntentFilter intentFilter = new IntentFilter(ServoPacket.INTENT_ACTION_OUTPUT);
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

    public static Intent getConfiguredIntent(ServoPacket configServoPacket){
        if (configServoPacket != null) {
            //Don't set class/component so that CommunicationsService can handle the intent
            Intent intent = new Intent(INTENT_ACTION_CONFIGURE_FLIGHT_CONTROL_SERVICE);
            intent.putExtra(EXTRA_CONFIG, configServoPacket.toJsonString());
            return intent;
        }
        return null;
    }

    //Listens for responses from the connected USB serial device and responds accordingly
    private BroadcastReceiver createArduinoOutputReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ServoPacket.INTENT_ACTION_OUTPUT)) {
                    ServoPacket servoPacket = new ServoPacket(intent.getExtras());

                    //If the device sent out a ready status
                    if (servoPacket.isStatusReady()) {
                        if (!mUsbSerialIsReady) mUsbSerialIsReady = true;
                        //Send the config for each servo to the Arduino
                        sendServoConfig(ServoPacket.ServoType.AILERON);
                        sendServoConfig(ServoPacket.ServoType.ELEVATOR);
                        sendServoConfig(ServoPacket.ServoType.RUDDER);
                        sendServoConfig(ServoPacket.ServoType.THROTTLE);
                        sendServoConfig(ServoPacket.ServoType.CUTOVER);
                    }

                    //If the ServoPacket contains the receiverControl json key, set mReceiverOnly
                    if (servoPacket.hasReceiverControl()) {
                        mReceiverControl = servoPacket.isReceiverControl();
                    }

                    //If the ServoPacket contains the calibrationMode, set text and buttons accordingly
                    if (servoPacket.hasCalibrationMode()) {
                        //If the arduino is in calibration mode, inform the user
                        String output = "Calibration Mode: "
                                + String.valueOf(servoPacket.isCalibrationMode());
                        Log.i(CLASS_NAME, output);
                    }

                    //If the ServoPacket contains receiver input calibration ranges, show them on screen
                    if (servoPacket.hasInputRanges()) {
                        Log.i(CLASS_NAME, "Calibration ranges received");
                    }

                    //If the ServoPacket contains an error message, display the message to the user
                    if (servoPacket.hasErrorMessage()) {
                        String error = "Error: " + servoPacket.getErrorMessage();
                        Log.i(CLASS_NAME, error);
                    }
                }
            }
        };
    }

    //Sends the configuration (minus the receiver input min and max) for a given ServoType
    private void sendServoConfig(ServoPacket.ServoType servoType) {
        String servoConfigJson = mConfigServoPacket.getConfigJson(servoType);
        if (servoConfigJson != null) {
            ServoPacket servoPacket = new ServoPacket(servoConfigJson);
            sendBroadcast(servoPacket.toIntent(ServoPacket.INTENT_ACTION_INPUT));
        }
        String servoInputRangeJson = mConfigServoPacket.getInputRangeJson(servoType);
        if (servoInputRangeJson != null) {
            ServoPacket servoPacket = new ServoPacket(servoInputRangeJson);
            sendBroadcast(servoPacket.toIntent(ServoPacket.INTENT_ACTION_INPUT));
        }
    }
}

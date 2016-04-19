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
import com.rabidllamastudios.avigate.models.CraftStatePacket;

/**
 * Service responsible for maintaining craft control and stability
 * Reads in sensor data via CraftStatePackets and broadcasts craft commands via ArduinoPackets
 * Created by Ryan Staatz on 1/1/2016
 */
public class FlightControlService extends Service {

    private static final String CLASS_NAME = FlightControlService.class.getSimpleName();
    private static final String PACKAGE_NAME = AvigateApplication.class.getPackage().getName();

    public static final String INTENT_ACTION_CONFIGURE_FLIGHT_CONTROL_SERVICE =
            PACKAGE_NAME + ".action.CONFIGURE_FLIGHT_CONTROL_SERVICE";
    public static final String EXTRA_CONFIG = PACKAGE_NAME + ".extra.CONFIG";

    //TODO implement instance boolean variable logic
    private boolean mPhoneFacingNose = false;
    private boolean mReceiverControl = false;
    private boolean mUsbSerialIsReady = false;

    //TODO empirically test differential gain constants
    //Gain constant in degrees correction per error degrees/second
    private static final double DIFFERENTIAL_GAIN = -0.5;
    //Gain constant in degrees correction per error degrees
    private static final int PROPORTIONAL_GAIN = -3;

    private BroadcastReceiver mArduinoOutputReceiver = null;
    private BroadcastReceiver mCraftStateReceiver = null;
    private ArduinoPacket mConfigArduinoPacket = null;

    public FlightControlService() {}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction().equals(
                INTENT_ACTION_CONFIGURE_FLIGHT_CONTROL_SERVICE)) {
            if (intent.hasExtra(EXTRA_CONFIG))
                mConfigArduinoPacket = new ArduinoPacket(intent.getStringExtra(EXTRA_CONFIG));
            //If mArduinoOutputReceiver is already initialized, unregister it and set it to null
            if (mArduinoOutputReceiver != null) {
                unregisterReceiver(mArduinoOutputReceiver);
                mArduinoOutputReceiver = null;
            }
            //Register BroadcastReceiver for ArduinoPacket output Intents
            mArduinoOutputReceiver = createArduinoOutputReceiver();
            IntentFilter arduinoIntentFilter = new IntentFilter(ArduinoPacket.INTENT_ACTION_OUTPUT);
            registerReceiver(mArduinoOutputReceiver, arduinoIntentFilter);
            //Register listener for CraftStatePacket Intents
            if (mCraftStateReceiver != null) {
                unregisterReceiver(mCraftStateReceiver);
                mCraftStateReceiver = null;
            }
            mCraftStateReceiver = createCraftStateReceiver();
            registerReceiver(mCraftStateReceiver, new IntentFilter(CraftStatePacket.INTENT_ACTION));
        }
        Log.i(CLASS_NAME, "Service started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        //Unregister all receivers
        if (mArduinoOutputReceiver != null) {
            unregisterReceiver(mArduinoOutputReceiver);
            mArduinoOutputReceiver = null;
        }
        if (mCraftStateReceiver != null) {
            unregisterReceiver(mCraftStateReceiver);
            mCraftStateReceiver = null;
        }
        Log.i(CLASS_NAME, "Service stopped");
        //Call super method
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /** Returns a configured intent that can be used to start this service (FlightControlService)
     * @param configArduinoPacket contains all the necessary Arduino configuration data
     * @return a configured Intent (minus the class/component) that can start FlightControlService
     */
    public static Intent getConfiguredIntent(ArduinoPacket configArduinoPacket){
        if (configArduinoPacket != null) {
            //Don't set class/component so that NetworkService can handle the intent
            Intent intent = new Intent(INTENT_ACTION_CONFIGURE_FLIGHT_CONTROL_SERVICE);
            intent.putExtra(EXTRA_CONFIG, configArduinoPacket.toJsonString());
            return intent;
        }
        return null;
    }

    //Listens for responses from the connected Arduino and responds accordingly
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

    //Listens for incoming CraftStatePackets (in the form of an Intent)
    private BroadcastReceiver createCraftStateReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(CraftStatePacket.INTENT_ACTION)) {
                    CraftStatePacket craftStatePacket = new CraftStatePacket(intent.getExtras());
                    stabilizeRoll(craftStatePacket);
                }
            }
        };
    }

    //Broadcasts a configured ArduinoPacket (in the form of an Intent) for a given ServoType
    private void sendServoConfig(ArduinoPacket.ServoType servoType) {
        String fullServoConfigJson = mConfigArduinoPacket.getConfigJson(servoType, true);
        if (fullServoConfigJson != null) {
            ArduinoPacket arduinoPacket = new ArduinoPacket(fullServoConfigJson);
            sendBroadcast(arduinoPacket.toIntent(ArduinoPacket.INTENT_ACTION_INPUT));
        }
    }

    //Maintains the craft at a flat (~0 degree) roll angle
    private void stabilizeRoll(CraftStatePacket craftStatePacket) {
        if (!mReceiverControl &&
                !mConfigArduinoPacket.isReceiverOnly(ArduinoPacket.ServoType.AILERON)) {
            //Calculate neutral aileron value
            int aileronMin = mConfigArduinoPacket.getOutputMin(ArduinoPacket.ServoType.AILERON);
            int aileronMax = mConfigArduinoPacket.getOutputMax(ArduinoPacket.ServoType.AILERON);
            int aileronNeutral = (aileronMax - aileronMin)/2 + aileronMin;
            //Get latest orientation and angular velocity data
            CraftStatePacket.Orientation orientation = craftStatePacket.getOrientation();
            CraftStatePacket.AngularVelocity angularVelocity =
                    craftStatePacket.getAngularVelocity();
            //Get roll and roll rate
            double roll = orientation.getCraftRoll(mPhoneFacingNose);
            double rollRate = angularVelocity.getCraftRollRate(mPhoneFacingNose);
            //Calculate new (proposed) aileron value
            int newAileronValue = Math.round(Math.round(PROPORTIONAL_GAIN * roll
                    + DIFFERENTIAL_GAIN * rollRate)) + aileronNeutral;
            //Constrain the new aileron value if it is outside configured output range
            if (newAileronValue < aileronMin) newAileronValue = aileronMin;
            if (newAileronValue > aileronMax) newAileronValue = aileronMax;
            //Set aileron servo to the new value
            ArduinoPacket arduinoPacket = new ArduinoPacket();
            arduinoPacket.setServoValue(ArduinoPacket.ServoType.AILERON, newAileronValue);
            sendBroadcast(arduinoPacket.toIntent(ArduinoPacket.INTENT_ACTION_INPUT));
        }
    }
}
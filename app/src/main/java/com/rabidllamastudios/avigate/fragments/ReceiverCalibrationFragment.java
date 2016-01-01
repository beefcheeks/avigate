package com.rabidllamastudios.avigate.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.rabidllamastudios.avigate.R;
import com.rabidllamastudios.avigate.model.ServoPacket;

/**
 * Created by Ryan on 12/19/15.
 * This Fragment class sets the layout and UI logic for calibrating the receiver inputs
 */
public class ReceiverCalibrationFragment extends Fragment {

    private static final String MICROSECONDS = "μs";

    private Button mCalibrationButton;
    private Callback mCallback;
    private View mRootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_receiver_calibration, container, false);

        if (mCallback != null) {
            //If the callback isn't null, configures the behavior of the calibration button
            mCalibrationButton = (Button) mRootView.findViewById(R.id.button_calibrate);
            mCalibrationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String calibrationText = mCalibrationButton.getText().toString();
                    if (calibrationText.equals(getString(R.string.button_calibrate)) ||
                            calibrationText.equals(getString(R.string.button_recalibrate))) {
                        mCallback.calibrationButtonPressed(true);
                    } else if (calibrationText.equals(
                            getString(R.string.button_stop_calibrating))) {
                        mCallback.calibrationButtonPressed(false);
                    }
                }
            });
        }
        return mRootView;
    }

    //Set the callback for ReceiverCalibrationFragment
    //Enables the Fragment to communicate with the activity
    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    //When calibration is started, change the calibration button text and reset min/max values
    public void calibrationStarted() {
        mCalibrationButton.setText(getString(R.string.button_stop_calibrating));
        resetServoCalibrationTextViews(ServoPacket.ServoType.AILERON);
        resetServoCalibrationTextViews(ServoPacket.ServoType.ELEVATOR);
        resetServoCalibrationTextViews(ServoPacket.ServoType.RUDDER);
        resetServoCalibrationTextViews(ServoPacket.ServoType.THROTTLE);
        resetServoCalibrationTextViews(ServoPacket.ServoType.CUTOVER);
    }

    //When calibration is stopped, change the calibration button text
    public void calibrationStopped(boolean isCalibrated) {
        if (isCalibrated) {
            mCalibrationButton.setText(getString(R.string.button_recalibrate));
        } else {
            mCalibrationButton.setText(getString(R.string.button_calibrate));
        }
    }

    //Displays the receiver input min and max values in the corresponding TextViews
    public void showCalibrationRange(ServoPacket.ServoType servoType, long inputMin,
                                     long inputMax) {
        TextView calibrationMinTextView = getCalibrationMinTextView(servoType);
        TextView calibrationMaxTextView = getCalibrationMaxTextView(servoType);
        if (calibrationMinTextView != null) {
            String minString = String.valueOf(inputMin) + MICROSECONDS;
            calibrationMinTextView.setText(minString);
        }
        if (calibrationMaxTextView != null) {
            String maxString = String.valueOf(inputMax) + MICROSECONDS;
            calibrationMaxTextView.setText(maxString);
        }
    }

    //Shows a warning dialog informing the user that no USB device is connected
    public void showNoUsbDeviceCalibrationWarningDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle("No USB device connected!")
                .setMessage("The Arduino cannot be calibrated until it is connected " +
                        "to an Android phone via USB.")
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {}
                        })
                .show();
    }

    //Resets the calibration min and max values to the default for a given ServoType
    private void resetServoCalibrationTextViews(ServoPacket.ServoType servoType) {
        TextView servoMinTextView = getCalibrationMinTextView(servoType);
        TextView servoMaxTextView = getCalibrationMaxTextView(servoType);
        if (servoMinTextView != null) {
            servoMinTextView.setText(getString(R.string.tv_placeholder_calibration));
        }
        if (servoMaxTextView != null) {
            servoMaxTextView.setText("");
        }
    }

    //Takes a ServoType and returns the corresponding receiver input min value
    private TextView getCalibrationMinTextView(ServoPacket.ServoType servoType) {
        switch(servoType) {
            case AILERON:
                return (TextView) mRootView.findViewById(
                        R.id.tv_arduino_value_calibration_aileron_min);
            case ELEVATOR:
                return (TextView) mRootView.findViewById(
                        R.id.tv_arduino_value_calibration_elevator_min);
            case RUDDER:
                return (TextView) mRootView.findViewById(
                        R.id.tv_arduino_value_calibration_rudder_min);
            case THROTTLE:
                return (TextView) mRootView.findViewById(
                        R.id.tv_arduino_value_calibration_throttle_min);
            case CUTOVER:
                return (TextView) mRootView.findViewById(
                        R.id.tv_arduino_value_calibration_cutover_min);
            default:
                return null;
        }
    }

    //Takes a ServoType and returns the corresponding receiver input max value
    private TextView getCalibrationMaxTextView(ServoPacket.ServoType servoType) {
        switch (servoType) {
            case AILERON:
                return (TextView) mRootView.findViewById(
                        R.id.tv_arduino_value_calibration_aileron_max);
            case ELEVATOR:
                return (TextView) mRootView.findViewById(
                        R.id.tv_arduino_value_calibration_elevator_max);
            case RUDDER:
                return (TextView) mRootView.findViewById(
                        R.id.tv_arduino_value_calibration_rudder_max);
            case THROTTLE:
                return (TextView) mRootView.findViewById(
                        R.id.tv_arduino_value_calibration_throttle_max);
            case CUTOVER:
                return (TextView) mRootView.findViewById(
                        R.id.tv_arduino_value_calibration_cutover_max);
            default:
                return null;
        }
    }

    /**
     * This is the callback class for ReceiverCalibrationFragment
     */
    public interface Callback {
        //Called when the calibration button is pressed
        void calibrationButtonPressed(boolean calibrationMode);
    }

}
package com.rabidllamastudios.avigate.fragments;

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

    private Button mCalibrationButton;
    private Callback mCallback;
    private View mRootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_receiver_calibration, container, false);
        mCalibrationButton = (Button) mRootView.findViewById(R.id.button_calibrate);
        mCalibrationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCalibrationButton.getText().equals(getString(R.string.button_calibrate))) {
                    mCallback.calibrationButtonPressed(true);
                } else if (mCalibrationButton.getText()
                        .equals(getString(R.string.button_stop_calibrating))) {
                    mCallback.calibrationButtonPressed(false);
                }
            }
        });

        return mRootView;
    }

    //Set the callback for ReceiverCalibrationFragment
    //Enables the Fragment to communicate with the activity
    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    //When calibration is started, change the calibration button text
    public void calibrationStarted() {
        mCalibrationButton.setText(getString(R.string.button_stop_calibrating));
    }

    //When calibration is stopped, change the calibration button text
    public void calibrationStopped() {
        mCalibrationButton.setText(getString(R.string.button_calibrate));
    }

    //Displays the receiver input min and max values in the corresponding TextViews
    public void showCalibrationRange(ServoPacket.ServoType servoType, int inputMin, int inputMax) {
        TextView calibrationMinTextView = getCalibrationMinTextView(servoType);
        TextView calibrationMaxTextView = getCalibrationMaxTextView(servoType);
        if (calibrationMinTextView != null) {
            calibrationMinTextView.setText(String.valueOf(inputMin));
        }
        if (calibrationMaxTextView != null) {
            calibrationMaxTextView.setText(String.valueOf(inputMax));
        }
    }

    //Takes a ServoType and returns the corresponding receiver input min value
    private TextView getCalibrationMinTextView(ServoPacket.ServoType servoType) {
        switch(servoType) {
            case AILERON:
                return (TextView) mRootView.findViewById(
                        R.id.tv_arduino_value_calibration_aileron_min);
            case CUTOVER:
                return (TextView) mRootView.findViewById(
                        R.id.tv_arduino_value_calibration_cutover_min);
            case ELEVATOR:
                return (TextView) mRootView.findViewById(
                        R.id.tv_arduino_value_calibration_elevator_min);
            case RUDDER:
                return (TextView) mRootView.findViewById(
                        R.id.tv_arduino_value_calibration_rudder_min);
            case THROTTLE:
                return (TextView) mRootView.findViewById(
                        R.id.tv_arduino_value_calibration_throttle_min);
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
            case CUTOVER:
                return (TextView) mRootView.findViewById(
                        R.id.tv_arduino_value_calibration_cutover_max);
            case ELEVATOR:
                return (TextView) mRootView.findViewById(
                        R.id.tv_arduino_value_calibration_elevator_max);
            case RUDDER:
                return (TextView) mRootView.findViewById(
                        R.id.tv_arduino_value_calibration_rudder_max);
            case THROTTLE:
                return (TextView) mRootView.findViewById(
                        R.id.tv_arduino_value_calibration_throttle_max);
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

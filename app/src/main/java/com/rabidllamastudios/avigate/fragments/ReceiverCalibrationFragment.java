package com.rabidllamastudios.avigate.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
        setHasOptionsMenu(true);

        if (mCallback != null) {
            //If the callback isn't null, configures the behavior of the calibration button
            mCalibrationButton = (Button) mRootView.findViewById(R.id.button_calibrate);
            mCallback.loadCalibrationValues();
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //Adds an 'enable transmitter' icon to the SupportActionBar menu
        inflater.inflate(R.menu.menu_fragment_calibration, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //This menu action is handled by the parent activity and not this fragment
                return false;
            case R.id.item_reset_servos:
                //This menu action is handled by ServoOutputFragment and not this fragment
                return false;
            case R.id.item_enable_transmitter:
                //Notifies the parent activity that the 'enable transmitter' icon was tapped
                mCallback.transmitterIconPressed(false);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //Set the callback for ReceiverCalibrationFragment
    //Enables the Fragment to communicate with the activity
    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    //Loads the receiver input ranges for each servo (if they already exist)
    public void loadCalibrationConfig(ServoPacket masterServoPacket) {
        if (masterServoPacket.hasInputRanges()) {
            loadServoInputRange(masterServoPacket, ServoPacket.ServoType.AILERON);
            loadServoInputRange(masterServoPacket, ServoPacket.ServoType.ELEVATOR);
            loadServoInputRange(masterServoPacket, ServoPacket.ServoType.RUDDER);
            loadServoInputRange(masterServoPacket, ServoPacket.ServoType.THROTTLE);
            loadServoInputRange(masterServoPacket, ServoPacket.ServoType.CUTOVER);
            //Sets the calibrate button text to "recalibrate" since there is an existing calibration
            mCalibrationButton.setText(getString(R.string.button_recalibrate));
        }
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

    //Shows a warning dialog informing the user that the receiver inputs have not been calibrated
    public void showNotCalibratedWarningDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle("Calibration required!")
                .setMessage("The transmitter cannot be enabled " +
                        "until the receiver inputs are calibrated.")
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .show();
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

    //Shows a warning dialog informing the user that no USB device is connected
    public void showNoUsbDeviceTransmitterWarningDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle("No USB device connected!")
                .setMessage("The transmitter cannot be enabled until the Arduino is connected " +
                        "to an Android phone via USB.")
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {}
                        })
                .show();
    }

    //Shows a warning dialog before sending transmitter calibration settings to Arduino
    public void showTransmitterWarningDialog() {
        AlertDialog.Builder alertDialogBuilder =
                new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle("WARNING - DANGER!");
        alertDialogBuilder.setMessage("Are you sure you want to enable the transmitter? " +
                "Incorrect configuration may result in an UNCONTROLLABLE CRAFT! " +
                "Before proceeding, keep away from propeller and firmly hold craft. " +
                "Be prepared to disconnect the craft battery if necessary.");
        alertDialogBuilder.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {}
                });
        alertDialogBuilder.setPositiveButton("Enable", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mCallback.transmitterIconPressed(true);
            }
        });
        alertDialogBuilder.show();
    }

    //Loads the receiver input range from a ServoPacket for a given ServoType
    private void loadServoInputRange(ServoPacket servoPacket, ServoPacket.ServoType servoType) {
        TextView inputMinTV = getCalibrationMinTextView(servoType);
        if (inputMinTV != null && servoPacket.hasInputMin(servoType)) {
            String minString = String.valueOf(servoPacket.getInputMin(servoType)) + MICROSECONDS;
            inputMinTV.setText(minString);
        }
        TextView inputMaxTV = getCalibrationMaxTextView(servoType);
        if (inputMaxTV != null && servoPacket.hasInputMax(servoType)) {
            String maxString = String.valueOf(servoPacket.getInputMax(servoType)) + MICROSECONDS;
            inputMaxTV.setText(maxString);
        }
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

        //Called when transmitter action bar icon is pressed
        void transmitterIconPressed(boolean enableTransmitter);

        //Triggers the loading of the receiver input calibration values
        void loadCalibrationValues();
    }

}
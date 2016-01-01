package com.rabidllamastudios.avigate.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.NumberPicker;

import com.rabidllamastudios.avigate.R;
import com.rabidllamastudios.avigate.models.ServoPacket;

/**
 * Created by Ryan on 12/19/15.
 * This Fragment class sets the layout and UI logic for configuring the servo inputs
 */
public class ServoInputFragment extends Fragment implements NumberPicker.OnValueChangeListener {

    private static final int PIN_MAX = 12;
    private static final int PIN_MIN = 0;

    private Callback mCallback = null;
    private View mRootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_servo_inputs, container, false);

        if (mCallback != null) mCallback.loadInputConfiguration();

        //Configure UI
        configurePinEditText();
        configureCheckBoxes();

        return mRootView;
    }

    //Set the callback for ServoInputFragment
    //Enables the Fragment to communicate with the activity
    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    //Loads the servo input configuration into the fragment UI from the masterServoPacket parameter
    public void loadInputConfiguration(ServoPacket masterServoPacket) {
        loadServoInputConfig(masterServoPacket, ServoPacket.ServoType.AILERON);
        loadServoInputConfig(masterServoPacket, ServoPacket.ServoType.ELEVATOR);
        loadServoInputConfig(masterServoPacket, ServoPacket.ServoType.RUDDER);
        loadServoInputConfig(masterServoPacket, ServoPacket.ServoType.THROTTLE);
        loadServoInputConfig(masterServoPacket, ServoPacket.ServoType.CUTOVER);
    }

    //Loads the input pin and receiverOnly value from a ServoPacket for a given ServoType
    private void loadServoInputConfig(ServoPacket servoPacket, ServoPacket.ServoType servoType) {
        EditText editText = getInputPinEditText(servoType);
        if (editText != null && servoPacket.hasInputPin(servoType)) {
            editText.setText(String.valueOf(servoPacket.getInputPin(servoType)));
        }

        CheckBox checkBox = getCheckBox(servoType);
        if (checkBox != null && servoPacket.hasReceiverOnly(servoType)) {
            checkBox.setChecked(!servoPacket.isReceiverOnly(servoType));
        }
    }

    //Configures all input pin EditTexts
    private void configurePinEditText() {
        configureInputPinEditText(ServoPacket.ServoType.AILERON);
        configureInputPinEditText(ServoPacket.ServoType.ELEVATOR);
        configureInputPinEditText(ServoPacket.ServoType.RUDDER);
        configureInputPinEditText(ServoPacket.ServoType.THROTTLE);
        configureInputPinEditText(ServoPacket.ServoType.CUTOVER);
    }

    //Configures all checkboxes in the layout
    private void configureCheckBoxes() {
        configureCheckBox(ServoPacket.ServoType.AILERON);
        configureCheckBox(ServoPacket.ServoType.ELEVATOR);
        configureCheckBox(ServoPacket.ServoType.RUDDER);
        configureCheckBox(ServoPacket.ServoType.THROTTLE);
    }

    //Configures a servo input pin EditText based on the given ServoType
    private void configureInputPinEditText(final ServoPacket.ServoType servoType) {
        final EditText editText = getInputPinEditText(servoType);
        if (editText != null ) editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Creates a customized AlertDialogBuilder that includes a NumberPicker
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
                final NumberPicker numberPicker = new NumberPicker(getActivity());
                numberPicker.setMaxValue(PIN_MAX);
                numberPicker.setMinValue(PIN_MIN);
                if (editText.getText().toString().equals(getString(
                        R.string.et_arduino_value_pin_default))) {
                    //If there is no output pin yet set, set selector to middle value
                    int middlePinValue = (PIN_MAX - PIN_MIN)/2;
                    numberPicker.setValue(middlePinValue);
                } else {
                    //If there is already an output pin set, set selector to this value
                    numberPicker.setValue(Integer.parseInt(editText.getText().toString()));
                }                numberPicker.setWrapSelectorWheel(false);
                //Create numPickFrameLayout to properly center numberPicker
                final FrameLayout numPickFrameLayout = new FrameLayout(getActivity());
                numPickFrameLayout.addView(numberPicker, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER));
                alertDialogBuilder.setView(numPickFrameLayout);
                alertDialogBuilder.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                alertDialogBuilder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int value = numberPicker.getValue();
                        mCallback.setServoInputPin(servoType, value);
                        editText.setText(String.valueOf(value));
                    }
                });
                //Set the title
                alertDialogBuilder.setTitle("Set " + servoType.getStringValue()
                        + " input pin number");
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        });
    }

    //Configures a checkbox for a given ServoType
    private void configureCheckBox(final ServoPacket.ServoType servoType) {
        CheckBox checkBox = getCheckBox(servoType);
        if (checkBox != null) {
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    //If checked, phone control is allowed, and therefore receiverOnly is false
                    mCallback.setControlType(servoType, !isChecked);
                }
            });
            //In case the control type has not yet been set
            mCallback.setControlType(servoType, !checkBox.isChecked());
        }
    }

    //Takes a ServoType and returns the EditText that contains that ServoType's input pin value
    @SuppressWarnings("ConstantConditions")
    private EditText getInputPinEditText(ServoPacket.ServoType servoType) {
        switch (servoType) {
            case AILERON:
                return (EditText) mRootView.findViewById(R.id.et_arduino_value_pin_input_aileron);
            case ELEVATOR:
                return (EditText) mRootView.findViewById(R.id.et_arduino_value_pin_input_elevator);
            case RUDDER:
                return (EditText) mRootView.findViewById(R.id.et_arduino_value_pin_input_rudder);
            case THROTTLE:
                return (EditText) mRootView.findViewById(R.id.et_arduino_value_pin_input_throttle);
            case CUTOVER:
                return (EditText) mRootView.findViewById(R.id.et_arduino_value_pin_input_cutover);
            default:
                return null;
        }
    }

    //Takes a ServoType and returns the corresponding CheckBox
    @SuppressWarnings("ConstantConditions")
    private CheckBox getCheckBox(ServoPacket.ServoType servoType) {
        switch(servoType) {
            case AILERON:
                return (CheckBox) mRootView.findViewById(R.id.checkbox_arduino_input_phone_aileron);
            case ELEVATOR:
                return (CheckBox) mRootView.findViewById(
                        R.id.checkbox_arduino_input_phone_elevator);
            case RUDDER:
                return (CheckBox) mRootView.findViewById(R.id.checkbox_arduino_input_phone_rudder);
            case THROTTLE:
                return (CheckBox) mRootView.findViewById(
                        R.id.checkbox_arduino_input_phone_throttle);
            default:
                return null;
        }
    }

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
        //Required method for implementing the NumberPicker class
    }

    /**
     * This is the callback class for ServoInputFragment
     */
    public interface Callback {
        //Triggers the loading of the servo input configuration
        void loadInputConfiguration();

        //Sets the control type for a given ServoType (e.g. shared control or receiver only)
        void setControlType(ServoPacket.ServoType servoType, boolean receiverOnly);

        //Sets the receiver input pin number for a given ServoType
        void setServoInputPin(ServoPacket.ServoType servoType, int pinValue);
    }
}
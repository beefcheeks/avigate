package com.rabidllamastudios.avigate.model;

import android.content.Intent;
import android.os.Bundle;

import com.rabidllamastudios.avigate.AvigateApplication;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Created by Ryan on 11/30/15.
 * A data model class to communicate servo data to and from the USB serial device
 * For convenience, this class can convert to and between Bundle and Intent
 * This class stores servo data as a JSON object and can return a JSON string
 **/
public class ServoPacket {
    private static final String PACKAGE_NAME = AvigateApplication.class.getPackage().getName();

    //Intent actions
    public static final String INTENT_ACTION_INPUT = PACKAGE_NAME + ".action.SERVO_INPUT_DATA";
    public static final String INTENT_ACTION_OUTPUT = PACKAGE_NAME + ".action.SERVO_OUTPUT_DATA";

    //JSON keys for key value pairs
    private static final String KEY_CALIBRATION_MODE = "calibrationMode";
    private static final String KEY_ERROR = "error";
    private static final String KEY_INPUT_CONFIG = "inputConfig";
    private static final String KEY_MAX = "max";
    private static final String KEY_MIN = "min";
    private static final String KEY_OUTPUT_CONFIG = "outputConfig";
    private static final String KEY_PIN = "pin";
    private static final String KEY_RECEIVER_CONTROL = "receiverControl";
    private static final String KEY_RECEIVER_ONLY = "receiverOnly";
    private static final String KEY_REQUEST = "request";
    private static final String KEY_STATUS = "status";
    private static final String KEY_VALUE ="value";

    //JSON value(s) for key value pairs
    private static final String VALUE_STATUS_READY = "ready";


    //Denotes the type of servo
    public enum ServoType {
        AILERON, CUTOVER, ELEVATOR, RUDDER, THROTTLE;

        //Returns the corresponding JSON String key
        public String getStringValue() {
            switch (this) {
                case AILERON:
                    return "aileron";
                case CUTOVER:
                    return "cutover";
                case ELEVATOR:
                    return "elevator";
                case RUDDER:
                    return "rudder";
                case THROTTLE:
                    return "throttle";
            }
            return null;
        }
    }

    private JSONObject rootJson;  //The JSON root object where all JSON data is stored

    public ServoPacket() {
        rootJson = new JSONObject();
    }

    public ServoPacket(String jsonString) {
        rootJson = new JSONObject();
        try {
            rootJson = (JSONObject) new JSONParser().parse(jsonString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public ServoPacket(Bundle bundle) {
        rootJson = new JSONObject();
        try {
            rootJson = (JSONObject) new JSONParser().parse(bundle.getString("json"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public Intent toIntent(String intentAction) {
        Intent intent = new Intent(intentAction);
        intent.putExtra("json", rootJson.toJSONString());
        return intent;
    }

    //Returns the stored JSON data as a string
    public String toJsonString() {
        return rootJson.toJSONString();
    }

    //Returns true if the ready status key value pair is present in the stored JSON object
    public boolean isStatusReady() {
        if (rootJson.containsKey(KEY_STATUS) && rootJson.get(KEY_STATUS).equals(VALUE_STATUS_READY)) {
            return true;
        }
        return false;
    }

    //Adds a microcontroller status request to the rootJson object
    @SuppressWarnings("unchecked")
    public void addStatusRequest() {
        rootJson.put(KEY_REQUEST, KEY_STATUS);
    }

    //Retrieves any error message if present
    public String getErrorMessage() {
        if (rootJson.containsKey(KEY_ERROR)) return (String) rootJson.get(KEY_ERROR);
        return null;
    }

    //Returns a JSON object containing the min and max values (in microseconds) for a ServoType
    @SuppressWarnings("unchecked")
    public JSONObject getInputRange(ServoType servoType) {
        if (rootJson.containsKey(servoType.getStringValue())) {
            JSONObject servoJson = (JSONObject) rootJson.get(servoType.getStringValue());
            if (servoJson.containsKey(KEY_INPUT_CONFIG)) {
                JSONObject inputConfigJson = (JSONObject) servoJson.get(KEY_INPUT_CONFIG);
                if (inputConfigJson.containsKey(KEY_MAX) && inputConfigJson.containsKey(KEY_MIN)) {
                    JSONObject inputRangeJson = new JSONObject();
                    inputRangeJson.put(KEY_MAX, inputConfigJson.get(KEY_MAX));
                    inputRangeJson.put(KEY_MIN, inputConfigJson.get(KEY_MIN));
                    JSONObject servoRangeJson = new JSONObject();
                    servoRangeJson.put(KEY_INPUT_CONFIG, inputRangeJson);
                    JSONObject rootRangeJson = new JSONObject();
                    rootRangeJson.put(servoType, servoRangeJson);
                    return rootRangeJson;
                }
            }
        }
        return null;
    }

    //Returns the value of the receiverControl JSON key
    //Use hasReceiverControl method to determine whether to use this method
    public boolean isReceiverControl() {
        return (boolean) rootJson.get(KEY_RECEIVER_CONTROL);
    }

    //Checks whether rootJson contains the receiverControl JSON key
    public boolean hasReceiverControl() {
        return rootJson.containsKey(KEY_RECEIVER_CONTROL);
    }

    //Sets the calibrationMode JSON boolean via boolean parameter calibrationMode
    //If calibrationMode is true, the USB device listens for receiver input to determine input range
    //If calibrationMode is false, the USB device stops listening for input
    @SuppressWarnings("unchecked")
    public void setCalibrationMode(boolean calibrationMode) {
        rootJson.put(KEY_CALIBRATION_MODE, calibrationMode);
    }

    //Sets the control type for the given ServoType based the value of the receiverOnly parameter
    //If receiverOnly is true, only the transmitter/receiver can send commands to the servos
    // If receiveronly is false, the phone and transmitter/receiver share control of the plane
    @SuppressWarnings("unchecked")
    public void setInputControl(ServoType servoType, boolean receiverOnly) {
        JSONObject servoJson = new JSONObject();
        JSONObject inputConfigJson = new JSONObject();
        if (rootJson.containsKey(servoType.getStringValue())) {
            servoJson = (JSONObject) rootJson.get(servoType.getStringValue());
            if (servoJson.containsKey(KEY_INPUT_CONFIG)) {
                inputConfigJson = (JSONObject) servoJson.get(KEY_INPUT_CONFIG);
            }
        }
        inputConfigJson.put(KEY_RECEIVER_ONLY, receiverOnly);
        servoJson.put(KEY_INPUT_CONFIG, inputConfigJson);
        rootJson.put(servoType, servoJson);
    }

    //Sets the receiver input pin for the given ServoType
    @SuppressWarnings("unchecked")
    public void setInputPin(ServoType servoType, int pinNumber) {
        JSONObject servoJson = new JSONObject();
        JSONObject inputConfigJson = new JSONObject();
        if (rootJson.containsKey(servoType.getStringValue())) {
            servoJson = (JSONObject) rootJson.get(servoType.getStringValue());
            if (servoJson.containsKey(servoType.getStringValue())) {
                inputConfigJson = (JSONObject) servoJson.get(KEY_INPUT_CONFIG);
            }
        }
        inputConfigJson.put(KEY_PIN, pinNumber);
        servoJson.put(KEY_INPUT_CONFIG, inputConfigJson);
        rootJson.put(servoType.getStringValue(), servoJson);
    }

    //Sets the min and max receiver input values for the given ServoType (in microseconds)
    @SuppressWarnings("unchecked")
    public void setInputRange(ServoType servoType, int intputMin, int inputMax) {
        JSONObject servoJson = new JSONObject();
        JSONObject inputConfigJson = new JSONObject();
        if (rootJson.containsKey(servoType.getStringValue())) {
            servoJson = (JSONObject) rootJson.get(servoType.getStringValue());
            if (rootJson.containsKey(servoType.getStringValue())) {
                inputConfigJson = (JSONObject) servoJson.get(KEY_INPUT_CONFIG);
            }
        }
        inputConfigJson.put(KEY_MAX, inputMax);
        inputConfigJson.put(KEY_MIN, intputMin);
        servoJson.put(KEY_INPUT_CONFIG, inputConfigJson);
        rootJson.put(servoType.getStringValue(), servoJson);
    }

    //Sets the min and max output values for the given ServoType (in degrees)
    @SuppressWarnings("unchecked")
    public void setOutputRange(ServoType servoType, int outputMin, int outputMax) {
        JSONObject servoJson = new JSONObject();
        JSONObject outputConfigJson = new JSONObject();
        if (rootJson.containsKey(servoType.getStringValue())) {
            servoJson = (JSONObject) rootJson.get(servoType.getStringValue());
            if (servoJson.containsKey(servoType.getStringValue())) {
                outputConfigJson = (JSONObject) servoJson.get(KEY_OUTPUT_CONFIG);
            }
        }
        outputConfigJson.put(KEY_MAX, outputMax);
        outputConfigJson.put(KEY_MIN, outputMin);
        servoJson.put(KEY_OUTPUT_CONFIG, outputConfigJson);
        rootJson.put(servoType.getStringValue(), servoJson);
    }

    //Sets the servo output pin for the given ServoType
    @SuppressWarnings("unchecked")
    public void setOutputPin(ServoType servoType, int pinNumber) {
        JSONObject servoJson = new JSONObject();
        JSONObject outputConfigJson = new JSONObject();
        if (rootJson.containsKey(servoType.getStringValue())) {
            servoJson = (JSONObject) rootJson.get(servoType.getStringValue());
            if (servoJson.containsKey(servoType.getStringValue())) {
                outputConfigJson = (JSONObject) servoJson.get(KEY_OUTPUT_CONFIG);
            }
        }
        outputConfigJson.put(KEY_PIN, pinNumber);
        servoJson.put(KEY_OUTPUT_CONFIG, outputConfigJson);
        rootJson.put(servoType.getStringValue(), servoJson);
    }

    //Sets the output value of the servo (in degrees)
    @SuppressWarnings("unchecked")
    public void setServoValue(ServoType servoType, int value) {
        JSONObject servoJson = new JSONObject();
        if (rootJson.containsKey(servoType.getStringValue())) {
            servoJson = (JSONObject) rootJson.get(servoType.getStringValue());
        }
        servoJson.put(KEY_VALUE, value);
        rootJson.put(servoType.getStringValue(), servoJson);
    }

}
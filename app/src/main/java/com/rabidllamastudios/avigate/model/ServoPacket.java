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

    public static final String INTENT_ACTION_INPUT = PACKAGE_NAME + ".action.SERVO_INPUT_DATA";
    public static final String INTENT_ACTION_OUTPUT = PACKAGE_NAME + ".action.SERVO_OUTPUT_DATA";

    private static final String REQUEST = "request";
    private static final String STATUS_KEY = "status";
    private static final String STATUS_VALUE = "ready";

    //Denotes the type of servo
    public enum ServoType {
        AILERON, ELEVATOR, RUDDER, THROTTLE, AILERON_IN, ELEVATOR_IN, RUDDER_IN, THROTTLE_IN;

        //Returns the corresponding JSON key
        public String getStringValue() {
            switch (this) {
                case AILERON:
                    return "aileron";
                case ELEVATOR:
                    return "elevator";
                case RUDDER:
                    return "rudder";
                case THROTTLE:
                    return "throttle";
                case AILERON_IN:
                    return "aileronIn";
                case ELEVATOR_IN:
                    return "elevatorIn";
                case RUDDER_IN:
                    return "rudderIn";
                case THROTTLE_IN:
                    return "throttleIn";
            }
            return null;
        }
    }

    private JSONObject servoJson;  //The JSON object where all data is stored

    public ServoPacket() {
        servoJson = new JSONObject();
    }

    public ServoPacket(String jsonString) {
        servoJson = new JSONObject();
        try {
            servoJson = (JSONObject) new JSONParser().parse(jsonString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public ServoPacket(Bundle bundle) {
        servoJson = new JSONObject();
        try {
            servoJson = (JSONObject) new JSONParser().parse(bundle.getString("json"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public Intent toIntent(String intentAction) {
        Intent intent = new Intent(intentAction);
        intent.putExtra("json", servoJson.toJSONString());
        return intent;
    }

    //Returns the stored JSON data as a string
    public String toJsonString() {
        return servoJson.toJSONString();
    }

    //Returns true if the ready status key value pair is present in the stored JSON object
    public boolean isStatusReady() {
        if (servoJson.containsKey(STATUS_KEY) && servoJson.get(STATUS_KEY).equals(STATUS_VALUE)) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public void addStatusRequest() {
        servoJson.put(REQUEST, STATUS_KEY);
    }

    @SuppressWarnings("unchecked")
    public void setControlType(boolean isManualControl) {
        servoJson.put("manualControl", isManualControl);
    }

    @SuppressWarnings("unchecked")
    public void setCutover(int cutoverPin, boolean hasInterrupt) {
        JSONObject config = new JSONObject();
        if(servoJson.containsKey("config")) {
            config = (JSONObject) servoJson.get("config");
        }
        config.put("cutoverIn", cutoverPin);
        config.put("cutoverInterrupt", hasInterrupt);
        servoJson.put("config", config);
    }

    @SuppressWarnings("unchecked")
    public void setServoPin(ServoType servoType, int pinNumber) {
        JSONObject config = new JSONObject();
        if(servoJson.containsKey("config")) {
            config = (JSONObject) servoJson.get("config");
        }
        config.put(servoType.getStringValue(), pinNumber);
        servoJson.put("config", config);
    }

    @SuppressWarnings("unchecked")
    public void setServoValue(ServoType servoType, int servoValue) {
        servoJson.put(servoType.getStringValue(), servoValue);
    }

}
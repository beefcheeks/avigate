package com.rabidllamastudios.avigate.services;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import com.felhr.usbserial.CDCSerialDevice;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.rabidllamastudios.avigate.AvigateApplication;
import com.rabidllamastudios.avigate.models.ArduinoPacket;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/**
 * UsbSerialService communicates with the CDC-ACM USB Serial Controller (e.g. Arduino) using USB-OTG
 * In this case, servo and motor commands are sent to this service from other parts of the app
 *
 * File created by Ryan on 11/12/15.
 * Much of this code is taken from: https://github.com/felHR85/SerialPortExample
 */

public class UsbSerialService extends Service {

    private static final String CLASS_NAME = UsbSerialService.class.getSimpleName();
    private static final String PACKAGE_NAME = AvigateApplication.class.getPackage().getName();

    //USB Intent Actions
    public static final String INTENT_ACTION_CDC_DRIVER_NOT_WORKING =
            PACKAGE_NAME + ".action.CDC_DRIVER_NOT_WORKING";
    public static final String INTENT_ACTION_NO_USB = PACKAGE_NAME + ".action.NO_USB";
    public static final String INTENT_ACTION_USB_DEVICE_NOT_WORKING =
            PACKAGE_NAME + ".action.USB_DEVICE_NOT_WORKING";
    public static final String INTENT_ACTION_USB_DISCONNECTED =
            PACKAGE_NAME + ".action.USB_DISCONNECTED";
    public static final String INTENT_ACTION_USB_NOT_SUPPORTED =
            PACKAGE_NAME + ".action.USB_NOT_SUPPORTED";
    public static final String INTENT_ACTION_USB_PERMISSION_GRANTED =
            PACKAGE_NAME + ".action.USB_PERMISSION_GRANTED";
    public static final String INTENT_ACTION_USB_PERMISSION_NOT_GRANTED =
            PACKAGE_NAME + ".action.USB_PERMISSION_NOT_GRANTED";
    public static final String INTENT_ACTION_USB_READY = PACKAGE_NAME + ".action.USB_READY";

    //Configuration intent for UsbSerialService
    private static final String INTENT_ACTION_CONFIGURE_USB_SERIAL_SERVICE =
            PACKAGE_NAME + ".action.CONFIGURE_USB_SERIAL_SERVICE";
    private static final String INTENT_ACTION_USB_PERMISSION =
            PACKAGE_NAME + ".action.USB_PERMISSION";

    //Baud rate extra name
    private static final String EXTRA_BAUD_RATE = PACKAGE_NAME + ".extra.BAUD_RATE";

    //Start and end markers required for CDC device to recognize serial input as valid input
    private static final String SERIAL_START_MARKER = "@";
    private static final String SERIAL_END_MARKER = "#";

    private static int BAUD_RATE = 115200; //Default value for baud rate

    private Context mContext;
    private IntentFilter mUsbIntentFilter;
    private UsbManager mUsbManager;
    private UsbDevice mUsbDevice;
    private UsbDeviceConnection mUsbConnection;
    private UsbSerialDevice mSerialPort;

    private boolean mSerialPortConnected = false;
    private boolean mReceiveInProgress = false;
    private String mReceivedJsonData = "";

    /*
     * onCreate is executed when service is started. It configures an IntentFilter to listen for
     * incoming Intents (USB ATTACHED, USB DETACHED...) and attempts to open a serial port.
     */
    @Override
    public void onCreate() {
        //Initialize non-primitive variables
        mContext = this;
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        //Initialize mUsbIntentFilter
        mUsbIntentFilter = new IntentFilter();
        mUsbIntentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        mUsbIntentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        mUsbIntentFilter.addAction(INTENT_ACTION_USB_PERMISSION);
    }

    //Return null since this service is not designed to bind to an activity
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null
                && INTENT_ACTION_CONFIGURE_USB_SERIAL_SERVICE.equals(intent.getAction())) {
            BAUD_RATE = intent.getIntExtra(EXTRA_BAUD_RATE, BAUD_RATE);
        }
        //Set up USB intent filter for Android system USB intents
        registerReceiver(mUsbReceiver, mUsbIntentFilter);
        findSerialPortDevice();
        Log.i(CLASS_NAME, "Service started");
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mUsbReceiver);
        if (mSerialPortConnected) closeSerialPort();
        Log.i(CLASS_NAME, "Service stopped");
        super.onDestroy();
    }

    //Get a pre-configured intent for starting this service class (UsbSerialService)
    public static Intent getConfiguredIntent(Context context) {
        Intent configuredIntent = new Intent(context, UsbSerialService.class);
        configuredIntent.setAction(INTENT_ACTION_CONFIGURE_USB_SERIAL_SERVICE);
        configuredIntent.putExtra(EXTRA_BAUD_RATE, BAUD_RATE);
        return configuredIntent;
    }

    //Get a pre-configured intent for starting this service class (UsbSerialService)
    public static Intent getConfiguredIntent(Context context, int baudRate) {
        Intent configuredIntent = new Intent(context, UsbSerialService.class);
        configuredIntent.setAction(INTENT_ACTION_CONFIGURE_USB_SERIAL_SERVICE);
        configuredIntent.putExtra(EXTRA_BAUD_RATE, baudRate);
        return configuredIntent;
    }

    //Closes the USB serial connection
    private void closeSerialPort() {
        unregisterReceiver(mServoInputReceiver);
        mSerialPort.close();
        mSerialPortConnected = false;
    }

    private void findSerialPortDevice() {
        // This will try to open the first encountered usb device connected, excluding usb root hubs
        HashMap<String, UsbDevice> usbDevices = mUsbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean deviceChosen = false;
            for(Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                mUsbDevice = entry.getValue();
                if (mUsbDevice.getVendorId() != 0x1d6b) {
                    //If there is a USB device connected, try to open it as a Serial Port.
                    requestUserPermission();
                    deviceChosen = true;
                    break;
                } else {
                    mUsbConnection = null;
                    mUsbDevice = null;
                }
            }

            if (!deviceChosen) {
                //Send out an intent to notify that there are no USB devices connected
                Intent intent = new Intent(INTENT_ACTION_NO_USB);
                sendBroadcast(intent);
            }
        } else {
            //Send out an intent to notify that there are no USB devices connected.
            Intent intent = new Intent(INTENT_ACTION_NO_USB);
            sendBroadcast(intent);
        }
    }

    //Request user permission. The response will be received in the BroadcastReceiver
    private void requestUserPermission() {
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(INTENT_ACTION_USB_PERMISSION), 0);
        mUsbManager.requestPermission(mUsbDevice, mPendingIntent);
    }

    //Data received from serial port is received and processed
    private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] receivedData) {
            try {
                //Attempt to create a String from the incoming data stream and then process it
                String incomingSerialData = new String(receivedData, "UTF-8");
                processSerialInput(incomingSerialData);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    };

    //This method processes the serial input and broadcasts it as a JSON string
    private void processSerialInput(String incomingSerialData) {
        //If there is a receive in progress, find the end marker or append the serial data
        if (mReceiveInProgress) {
            if (incomingSerialData.contains(SERIAL_END_MARKER)) {
                //Since just we hit the end marker, set receive in progress to false
                mReceiveInProgress = false;
                int endMarkerIndex = incomingSerialData.indexOf(SERIAL_END_MARKER);
                //Create the full JSON string based on the end marker position
                String jsonDataFull = mReceivedJsonData.concat(incomingSerialData.substring(0,
                        endMarkerIndex));
                Log.i(CLASS_NAME, jsonDataFull);
                ArduinoPacket arduinoPacket = new ArduinoPacket(jsonDataFull);
                //TODO implement throttling and recording of servo values
                //Ignore incoming data with servo output values until throttling is implemented
                if (!arduinoPacket.hasServoValues()) {
                    //Broadcast the JSON string as a ArduinoPacket output Intent
                    Intent servoOutputIntent = new ArduinoPacket(jsonDataFull).toIntent(
                            ArduinoPacket.INTENT_ACTION_OUTPUT);
                    sendBroadcast(servoOutputIntent);
                }
                //If there is still more data beyond the end marker, process the data
                if (endMarkerIndex < (incomingSerialData.length() - 1)) {
                    //Treat this data as if it is 'new' incoming serial data
                    incomingSerialData = incomingSerialData.substring(endMarkerIndex + 1);
                }
            } else {
                //If there is a receive in progress, but no end marker, concatenate the Strings
                mReceivedJsonData = mReceivedJsonData.concat(incomingSerialData);
            }
        }
        //If the incoming data contains a start marker, process the data
        if (incomingSerialData.contains(SERIAL_START_MARKER)) {
            int startMarkerIndex = incomingSerialData.indexOf(SERIAL_START_MARKER);
            //Create the JSON String start via the substring of everything after the start marker
            String jsonDataStart = incomingSerialData.substring(startMarkerIndex + 1);
            mReceivedJsonData = "";  //Reset mReceivedJsonData
            mReceiveInProgress = true;  //Since this is the start, there is a receive in progress
            //Process the rest of the string beyond the start marker as if it were incoming data
            processSerialInput(jsonDataStart);
        }
    }

    //Send received servo input json data to the connected USB serial device
    private BroadcastReceiver mServoInputReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mSerialPortConnected) {
                Bundle bundle = intent.getExtras();
                if (bundle == null) return;
                String servoInputJson = new ArduinoPacket(bundle).toJsonString();
                servoInputJson = SERIAL_START_MARKER + servoInputJson + SERIAL_END_MARKER;
                Log.i(CLASS_NAME, servoInputJson);
                mSerialPort.write(servoInputJson.getBytes());
            }
        }
    };

    /*
     * Different USB notifications are received here (USB attached, detached, permission responses)
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(INTENT_ACTION_USB_PERMISSION)) {
                boolean permissionGranted = intent.getExtras().getBoolean(
                        UsbManager.EXTRA_PERMISSION_GRANTED);
                if (permissionGranted) {
                    // User accepted our USB connection. Try to open the device as a serial port
                    Intent permissionGrantedIntent =
                            new Intent(INTENT_ACTION_USB_PERMISSION_GRANTED);
                    context.sendBroadcast(permissionGrantedIntent);
                    mUsbConnection = mUsbManager.openDevice(mUsbDevice);
                    mSerialPortConnected = true;
                    new ConnectionThread().run();
                } else {
                    //Send out an intent to notify that the user denies access to the USB connection
                    Intent permissionDeniedIntent =
                            new Intent(INTENT_ACTION_USB_PERMISSION_NOT_GRANTED);
                    context.sendBroadcast(permissionDeniedIntent);
                }

            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                // A USB device has been attached. Try to open it as a Serial port
                if(!mSerialPortConnected) findSerialPortDevice();

            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                // Usb device was disconnected. Stop listening for servo commands.
                Intent usbDisconnectedIntent = new Intent(INTENT_ACTION_USB_DISCONNECTED);
                context.sendBroadcast(usbDisconnectedIntent);
                if (mSerialPortConnected) closeSerialPort();
            }
        }
    };

    /*
     * A simple thread to open a serial port.
     * Although it is a fast operation, moving usb operations off of the UI thread is a good thing
     */
    private class ConnectionThread extends Thread {
        @Override
        public void run() {
            mSerialPort = UsbSerialDevice.createUsbSerialDevice(mUsbDevice, mUsbConnection);
            if (mSerialPort != null) {
                if (mSerialPort.open()) {
                    //Set the appropriate properties for the serial port connection
                    mSerialPort.setBaudRate(BAUD_RATE);
                    mSerialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                    mSerialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                    mSerialPort.setParity(UsbSerialInterface.PARITY_NONE);
                    mSerialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                    mSerialPort.read(mCallback);

                    //Set up servo input intent filter
                    IntentFilter servoInputFilter =
                            new IntentFilter(ArduinoPacket.INTENT_ACTION_INPUT);
                    registerReceiver(mServoInputReceiver, servoInputFilter);

                    //Send out an intent that the USB serial interface is ready
                    Intent intent = new Intent(INTENT_ACTION_USB_READY);
                    mContext.sendBroadcast(intent);

                    //Request status from device in case the device is already running
                    ArduinoPacket statusArduinoPacket = new ArduinoPacket();
                    statusArduinoPacket.addStatusRequest();
                    sendBroadcast(statusArduinoPacket.toIntent(ArduinoPacket.INTENT_ACTION_INPUT));
                    Log.i(CLASS_NAME, "Sending status request to Arduino");

                } else {
                    //Send out an intent to notify that the serial port could not be opened
                    //(e.g. I/O error or no driver)
                    if (mSerialPort instanceof CDCSerialDevice) {
                        Intent intent = new Intent(INTENT_ACTION_CDC_DRIVER_NOT_WORKING);
                        mContext.sendBroadcast(intent);
                    } else {
                        Intent intent = new Intent(INTENT_ACTION_USB_DEVICE_NOT_WORKING);
                        mContext.sendBroadcast(intent);
                    }
                }
            } else {
                // No driver for given device, even generic CDC driver could not be loaded
                Intent intent = new Intent(INTENT_ACTION_USB_NOT_SUPPORTED);
                mContext.sendBroadcast(intent);
            }
        }
    }

}
package com.rabidllamastudios.avigate.services;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
 * In this case, configuration & servo commands are sent to this service from other parts of the app
 *
 * A good chunk of this code was originally taken from: https://github.com/felHR85/SerialPortExample
 * File created by Ryan Staatz on 11/12/15.
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
    private static final int DEFAULT_BAUD_RATE = 115200;  //Default value for baud rate in bytes/sec
    private static final int DEFAULT_THROTTLE_RATE = 100; //Default value for throttle rate in ms

    //Start and end markers required for CDC device to recognize serial input as valid input
    private static final String SERIAL_START_MARKER = "@";
    private static final String SERIAL_END_MARKER = "#";

    //Synchronization lock for making changes to mThrottleServoValues ArduinoPacket instance variabl
    private static final Object mThrottleLock = new Object();

    //Use volatile boolean since 1 thread only writes to it, and the main thread only reads it
    //See status flag volatile pattern #1: http://www.ibm.com/developerworks/library/j-jtp06197/
    private volatile boolean mSerialPortConnected = false;

    //See cheap read-write lock pattern #5: http://www.ibm.com/developerworks/library/j-jtp06197/
    private volatile ArduinoPacket mThrottledServoValues;

    private boolean mReceiveInProgress = false;
    private int mBaudRate = DEFAULT_BAUD_RATE;

    private Executor mIncomingSerialDataExecutor;
    private Executor mSerialPortExecutor;
    private IntentFilter mUsbIntentFilter;
    private ScheduledExecutorService mScheduleBroadcastExecutor;
    private String mReceivedJsonData = "";
    private UsbDevice mUsbDevice;
    private UsbDeviceConnection mUsbConnection;
    private UsbManager mUsbManager;
    private UsbSerialDevice mSerialPort;

    //Configures an IntentFilter that listens for USB intents when the service is first started
    @Override
    public void onCreate() {
        //Initialize executors (using sequential executors to prevent concurrency issues)
        mIncomingSerialDataExecutor = Executors.newSingleThreadExecutor();
        mScheduleBroadcastExecutor = Executors.newSingleThreadScheduledExecutor();
        mSerialPortExecutor = Executors.newSingleThreadExecutor();

        //Initialize other variables
        mThrottledServoValues = new ArduinoPacket();
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
                && intent.getAction().equals(INTENT_ACTION_CONFIGURE_USB_SERIAL_SERVICE)) {
            mBaudRate = intent.getIntExtra(EXTRA_BAUD_RATE, DEFAULT_BAUD_RATE);
        }
        //Register BroadcastReceiver to listen for Android system USB intents
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

    /** Returns a pre-configured intent that can be used to start UsbSerialService */
    public static Intent getConfiguredIntent(Context context) {
        Intent configuredIntent = new Intent(context, UsbSerialService.class);
        configuredIntent.setAction(INTENT_ACTION_CONFIGURE_USB_SERIAL_SERVICE);
        configuredIntent.putExtra(EXTRA_BAUD_RATE, DEFAULT_BAUD_RATE);
        return configuredIntent;
    }

    /** Returns a pre-configured intent for starting UsbSerialService and also set the baud rate
     * @param context the application context from the activity invoking this method
     * @param baudRate the baud rate in bytes per second (e.g. 115200, 9600, etc)
     */
    public static Intent getConfiguredIntent(Context context, int baudRate) {
        Intent configuredIntent = new Intent(context, UsbSerialService.class);
        configuredIntent.setAction(INTENT_ACTION_CONFIGURE_USB_SERIAL_SERVICE);
        configuredIntent.putExtra(EXTRA_BAUD_RATE, baudRate);
        return configuredIntent;
    }

    //Closes the USB serial connection
    private void closeSerialPort() {
        unregisterReceiver(mArduinoInputReceiver);
        //Run all serial port commands (including the one below) on the serial port executor
        mSerialPortExecutor.execute(new Runnable() {
            @Override
            public void run() {
                //These variables are only modified on the same thread
                mSerialPort.close();
                mSerialPortConnected = false;
            }
        });
    }

    //Attempts to open the first encountered usb device connected, excluding usb root hubs
    private void findSerialPortDevice() {
        HashMap<String, UsbDevice> usbDevices = mUsbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean deviceChosen = false;
            for(Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                mUsbDevice = entry.getValue();
                //If there is a USB device connected, try to open it as a Serial port.
                if (mUsbDevice.getVendorId() != 0x1d6b) {
                    requestUserPermission();
                    deviceChosen = true;
                    break;
                } else {
                    mUsbConnection = null;
                    mUsbDevice = null;
                }
            }
            //If there are no USB devices chosen, send out an intent to notify other app components
            if (!deviceChosen) sendBroadcast(new Intent(INTENT_ACTION_NO_USB));
        //If there are no USB devices connected, send out an intent to notify other app components
        } else {
            sendBroadcast(new Intent(INTENT_ACTION_NO_USB));
        }
    }

    //Request user permission. The response will be received in the BroadcastReceiver
    private void requestUserPermission() {
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(INTENT_ACTION_USB_PERMISSION), 0);
        mUsbManager.requestPermission(mUsbDevice, mPendingIntent);
    }

    //Send received Arduino input json data to the Arduino
    private BroadcastReceiver mArduinoInputReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mSerialPortConnected) {
                //Process data on separate thread to prevent potential UI lock
                mSerialPortExecutor.execute(new OutgoingSerialDataProcessor(intent));
            }
        }
    };

    //Different USB notifications are received here (USB attached, detached, permission responses)
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(INTENT_ACTION_USB_PERMISSION)) {
                boolean permissionGranted = intent.getExtras().getBoolean(
                        UsbManager.EXTRA_PERMISSION_GRANTED);
                if (permissionGranted) {
                    // User accepted our USB connection. Try to open the device as a serial port
                    sendBroadcast(new Intent(INTENT_ACTION_USB_PERMISSION_GRANTED));
                    mUsbConnection = mUsbManager.openDevice(mUsbDevice);
                    //Run serial port opening operation on separate thread
                    mSerialPortExecutor.execute(new SerialPortOpener());
                } else {
                    //Send out an intent to notify that the user denies access to the USB connection
                    sendBroadcast(new Intent(INTENT_ACTION_USB_PERMISSION_NOT_GRANTED));
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                // A USB device has been attached. Try to open it as a Serial port
                if(!mSerialPortConnected) findSerialPortDevice();
            // Usb device disconnected. Stop listening for Arduino input & close serial port
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                sendBroadcast(new Intent(INTENT_ACTION_USB_DISCONNECTED));
                if (mSerialPortConnected) closeSerialPort();
            }
        }
    };

    //Data received from serial port is received and processed
    private UsbSerialInterface.UsbReadCallback mCallback =
            new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] receivedData) {
            try {
                //Attempt to create a String from the incoming data stream and then process it
                String incomingData = new String(receivedData, "UTF-8");
                //Process received data on separate thread to prevent potential UI lock
                mIncomingSerialDataExecutor.execute(new IncomingSerialDataProcessor(incomingData));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    };

    //Processes and packages serial input, and broadcasts an intent containing a JSON String
    private class IncomingSerialDataProcessor implements Runnable {
        private String mSerialData;

        //Takes incoming data in the form of a String
        public IncomingSerialDataProcessor(String incomingSerialData){
            mSerialData = incomingSerialData;
        }

        @Override
        public void run() {
            //Separate method used due to use of recursion
            processIncomingSerialData(mSerialData);
        }

        //Processes serial input and broadcasts it as a JSON string
        private void processIncomingSerialData(String incomingSerialData) {
            //If there is a receive in progress, find the end marker or append the serial data
            if (mReceiveInProgress) {
                if (incomingSerialData.contains(SERIAL_END_MARKER)) {
                    //Since just we hit the end marker, set receive in progress to false
                    mReceiveInProgress = false;
                    int endMarkerIndex = incomingSerialData.indexOf(SERIAL_END_MARKER);
                    //Create the full JSON string based on the end marker position
                    String jsonDataFull= mReceivedJsonData.concat(
                                incomingSerialData.substring(0, endMarkerIndex));
                    Log.i("Incoming Arduino data", jsonDataFull);
                    ArduinoPacket arduinoPacket = new ArduinoPacket(jsonDataFull);
                    if (arduinoPacket.hasServoValue()) {
                        storeServoValues(arduinoPacket);
                    } else {
                        //Broadcast the JSON string as a ArduinoPacket output Intent
                        sendBroadcast(new ArduinoPacket(jsonDataFull).toIntent(
                                ArduinoPacket.INTENT_ACTION_OUTPUT));
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
                //The start of the JSON String is the substring of everything after the start marker
                String jsonDataStart = incomingSerialData.substring(startMarkerIndex + 1);
                mReceivedJsonData = "";  //Reset mReceivedJsonData
                mReceiveInProgress = true; //Since this is the start, there is a receive in progress
                //Process the rest of the string beyond the start marker as if it were incoming data
                processIncomingSerialData(jsonDataStart);
            }
        }

        //Stores any servo values contained within the input ArduinoPacket
        private void storeServoValues(ArduinoPacket arduinoPacket) {
            ArduinoPacket.ServoType aileron = ArduinoPacket.ServoType.AILERON;
            ArduinoPacket.ServoType elevator = ArduinoPacket.ServoType.ELEVATOR;
            ArduinoPacket.ServoType rudder = ArduinoPacket.ServoType.RUDDER;
            ArduinoPacket.ServoType throttle = ArduinoPacket.ServoType.THROTTLE;

            if (arduinoPacket.hasServoValue(aileron)) {
                storeServoValue(aileron, arduinoPacket.getServoValue(aileron));
            }
            if (arduinoPacket.hasServoValue(elevator)) {
                storeServoValue(elevator, arduinoPacket.getServoValue(elevator));
            }
            if (arduinoPacket.hasServoValue(rudder)) {
                storeServoValue(rudder, arduinoPacket.getServoValue(rudder));
            }
            if (arduinoPacket.hasServoValue(throttle)) {
                storeServoValue(throttle, arduinoPacket.getServoValue(throttle));
            }
        }

        //Stores a servo value for a given ServoType in mThrottledServoValues (an ArduinoPacket)
        private void storeServoValue(ArduinoPacket.ServoType servoType, int servoValue) {
            //Needs synchronized method since mThrottleServoValues is modified on a different thread
            synchronized (mThrottleLock) {
                //This will overwrite old servo values if still present (intended behavior)
                mThrottledServoValues.setServoValue(servoType, servoValue);
            }
        }
    }

    //Processes the incoming intent and writes serial data out to the USB device (e.g. Arduino)
    private class OutgoingSerialDataProcessor implements Runnable {
        private Intent mReceivedIntent;

        public OutgoingSerialDataProcessor(Intent incomingIntent) {
            mReceivedIntent = incomingIntent;
        }

        @Override
        public void run() {
            Bundle bundle = mReceivedIntent.getExtras();
            if (bundle == null) return;
            String arduinoInputJson = new ArduinoPacket(bundle).toJsonString();
            //Prepend start marker character and append end marker character
            arduinoInputJson = SERIAL_START_MARKER + arduinoInputJson + SERIAL_END_MARKER;
            Log.i("Sending data to Arduino", arduinoInputJson);
            mSerialPort.write(arduinoInputJson.getBytes());
        }
    }

    //A runnable that attempts to open a serial connection to the USB device (e.g. Arduino)
    private class SerialPortOpener implements Runnable {
        @Override
        public void run() {
            mSerialPort = UsbSerialDevice.createUsbSerialDevice(mUsbDevice, mUsbConnection);
            if (mSerialPort != null) {
                if (mSerialPort.open()) {
                    //Set the appropriate properties for the serial port connection
                    mSerialPort.setBaudRate(mBaudRate);
                    mSerialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                    mSerialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                    mSerialPort.setParity(UsbSerialInterface.PARITY_NONE);
                    mSerialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                    mSerialPort.read(mCallback);

                    //Serial port is now connected!
                    mSerialPortConnected = true;

                    //Register a Broadcast Receiver to listen for Arduino input
                    registerReceiver(mArduinoInputReceiver,
                            new IntentFilter(ArduinoPacket.INTENT_ACTION_INPUT));

                    //Send out an intent that the USB serial interface is ready
                    sendBroadcast(new Intent(INTENT_ACTION_USB_READY));

                    //Request status from device in case the device is already running
                    ArduinoPacket statusArduinoPacket = new ArduinoPacket();
                    statusArduinoPacket.addStatusRequest();
                    sendBroadcast(statusArduinoPacket.toIntent(ArduinoPacket.INTENT_ACTION_INPUT));
                    Log.i(CLASS_NAME, "Sending status request to Arduino");

                    mScheduleBroadcastExecutor.scheduleAtFixedRate(new ServoValueBroadcaster(),
                            DEFAULT_THROTTLE_RATE, DEFAULT_THROTTLE_RATE, TimeUnit.MILLISECONDS);
                } else {
                    //Send intent if the serial port could not be opened (e.g. no driver, i/o error)
                    if (mSerialPort instanceof CDCSerialDevice) {
                        sendBroadcast(new Intent(INTENT_ACTION_CDC_DRIVER_NOT_WORKING));
                    } else {
                        sendBroadcast(new Intent(INTENT_ACTION_USB_DEVICE_NOT_WORKING));
                    }
                }
            } else {
                // No driver for given device, even generic CDC driver could not be loaded
                sendBroadcast(new Intent(INTENT_ACTION_USB_NOT_SUPPORTED));
            }
        }
    }

    //Broadcasts servo values contained in mThrottledServoValues (an ArduinoPacket)
    private class ServoValueBroadcaster implements Runnable {
        @Override
        public void run() {
            if (mThrottledServoValues.hasServoValue()) {
                Log.i("Incoming servo values", mThrottledServoValues.toJsonString());
                sendBroadcast(mThrottledServoValues.toIntent(ArduinoPacket.INTENT_ACTION_OUTPUT));
                synchronized (mThrottleLock) {
                    mThrottledServoValues = new ArduinoPacket();
                }
            }
        }
    }

}
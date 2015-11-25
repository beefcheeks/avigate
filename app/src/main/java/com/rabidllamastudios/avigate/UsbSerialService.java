package com.rabidllamastudios.avigate;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import com.felhr.usbserial.CDCSerialDevice;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

/**
 * UsbSerialService communicates with the CDC-ACM USB Serial Controller (e.g. Arduino) using USB-OTG
 * In this case, servo and motor commands are sent to this service from other parts of the app
 *
 * File created by Ryan on 11/12/15.
 * Most of this code is taken from: https://github.com/felHR85/SerialPortExample
 */

public class UsbSerialService extends Service {

    public static final String PACKAGE_NAME = AvigateApplication.class.getPackage().getName();

    public static final String ACTION_NO_USB = PACKAGE_NAME + ".action.NO_USB";
    public static final String ACTION_USB_READY = PACKAGE_NAME + ".action.USB_READY";
    public static final String ACTION_USB_DISCONNECTED = PACKAGE_NAME + ".action.USB_DISCONNECTED";
    public static final String ACTION_USB_NOT_SUPPORTED = PACKAGE_NAME + ".action.USB_NOT_SUPPORTED";
    public static final String ACTION_USB_PERMISSION_GRANTED = PACKAGE_NAME + ".action.USB_PERMISSION_GRANTED";
    public static final String ACTION_USB_PERMISSION_NOT_GRANTED = PACKAGE_NAME + ".action.USB_PERMISSION_NOT_GRANTED";

    public static final String ACTION_CDC_DRIVER_NOT_WORKING = PACKAGE_NAME + ".action.CDC_DRIVER_NOT_WORKING";
    public static final String ACTION_USB_DEVICE_NOT_WORKING = PACKAGE_NAME + ".action.USB_DEVICE_NOT_WORKING";

    private static final String ACTION_USB_PERMISSION = PACKAGE_NAME + ".action.USB_PERMISSION";

    private static int BAUD_RATE = 115200; // Baud rate - change this value if you need
    public static final int MESSAGE_FROM_SERIAL_PORT = 0;

    public static boolean SERVICE_CONNECTED = false;

    private IBinder binder = new UsbBinder();

    private Context context;
    private Handler mHandler;
    private UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;

    private boolean serialPortConnected;

    /*
     * onCreate will be executed when service is started. It configures an IntentFilter to listen for
     * incoming Intents (USB ATTACHED, USB DETACHED...) and it tries to open a serial port.
     */
    @Override
    public void onCreate()
    {
        this.context = this;
        serialPortConnected = false;
        UsbSerialService.SERVICE_CONNECTED = true;
        registerUsbReceiver();
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        findSerialPortDevice();
    }

    /* MUST READ about services
     * http://developer.android.com/guide/components/services.html
     * http://developer.android.com/guide/components/bound-services.html
     */
    @Override
    public IBinder onBind(Intent intent)
    {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        UsbSerialService.SERVICE_CONNECTED = false;
    }

    /*
     * Set the Baud Rate to something besides the default (not a required method)
     */
    public void setBaudRate(int baudRate) {
        BAUD_RATE = baudRate;
    }

    /*
     * This function will be called from MainActivity to write data through Serial Port
     */
    public void write(byte[] data)
    {
        if(serialPort != null)
            serialPort.write(data);
    }

    public void setHandler(Handler mHandler)
    {
        this.mHandler = mHandler;
    }

    private void findSerialPortDevice()
    {
        // This snippet will try to open the first encountered usb device connected, excluding usb root hubs
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if(!usbDevices.isEmpty())
        {
            boolean keep = true;
            for(Map.Entry<String, UsbDevice> entry : usbDevices.entrySet())
            {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                int devicePID = device.getProductId();

                if(deviceVID != 0x1d6b && (devicePID != 0x0001 || devicePID != 0x0002 || devicePID != 0x0003))
                {
                    // There is a device connected to our Android device. Try to open it as a Serial Port.
                    requestUserPermission();
                    keep = false;
                }else
                {
                    connection = null;
                    device = null;
                }

                if(!keep)
                    break;
            }
            if(!keep)
            {
                // There is no USB devices connected (but usb host were listed). Send an intent to MainActivity.
                Intent intent = new Intent(ACTION_NO_USB);
                sendBroadcast(intent);
            }
        }else
        {
            // There is no USB devices connected. Send an intent to MainActivity
            Intent intent = new Intent(ACTION_NO_USB);
            sendBroadcast(intent);
        }
    }

    private void registerUsbReceiver()
    {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        registerReceiver(usbReceiver , filter);
    }

    /*
     * Request user permission. The response will be received in the BroadcastReceiver
     */
    private void requestUserPermission()
    {
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION),0);
        usbManager.requestPermission(device, mPendingIntent);
    }

    /*
     *  Data received from serial port will be received here. Just populate onReceivedData with your code
     *  In this particular example. byte stream is converted to String and send to UI thread to
     *  be treated there.
     */
    private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback()
    {
        @Override
        public void onReceivedData(byte[] arg0)
        {
            try
            {
                String data = new String(arg0, "UTF-8");
                if(mHandler != null)
                    mHandler.obtainMessage(MESSAGE_FROM_SERIAL_PORT,data).sendToTarget();
            } catch (UnsupportedEncodingException e)
            {
                e.printStackTrace();
            }
        }
    };

    public class UsbBinder extends Binder
    {
        public UsbSerialService getService()
        {
            return UsbSerialService.this;
        }
    }

    /*
     * Different notifications from OS will be received here (USB attached, detached, permission responses...)
     * About BroadcastReceiver: http://developer.android.com/reference/android/content/BroadcastReceiver.html
     */
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context arg0, Intent arg1)
        {
            if(arg1.getAction().equals(ACTION_USB_PERMISSION))
            {
                boolean granted = arg1.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if(granted) // User accepted our USB connection. Try to open the device as a serial port
                {
                    Intent intent = new Intent(ACTION_USB_PERMISSION_GRANTED);
                    arg0.sendBroadcast(intent);
                    connection = usbManager.openDevice(device);
                    serialPortConnected = true;
                    new ConnectionThread().run();
                }else // User not accepted our USB connection. Send an Intent to the Main Activity
                {
                    Intent intent = new Intent(ACTION_USB_PERMISSION_NOT_GRANTED);
                    arg0.sendBroadcast(intent);
                }
            }else if(arg1.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED))
            {
                if(!serialPortConnected)
                    findSerialPortDevice(); // A USB device has been attached. Try to open it as a Serial port
            }else if(arg1.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED))
            {
                // Usb device was disconnected. send an intent to the Main Activity
                Intent intent = new Intent(ACTION_USB_DISCONNECTED);
                arg0.sendBroadcast(intent);
                serialPortConnected = false;
                serialPort.close();
            }
        }
    };

    /*
     * A simple thread to open a serial port.
     * Although it should be a fast operation. moving usb operations away from UI thread is a good thing.
     */
    private class ConnectionThread extends Thread
    {
        @Override
        public void run()
        {
            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
            if(serialPort != null)
            {
                if(serialPort.open())
                {
                    serialPort.setBaudRate(BAUD_RATE);
                    serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                    serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                    serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                    serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                    serialPort.read(mCallback);

                    // Everything went as expected. Send an intent to MainActivity
                    Intent intent = new Intent(ACTION_USB_READY);
                    context.sendBroadcast(intent);
                }else
                {
                    // Serial port could not be opened, maybe an I/O error or if CDC driver was chosen, it does not really fit
                    // Send an Intent to Main Activity
                    if(serialPort instanceof CDCSerialDevice)
                    {
                        Intent intent = new Intent(ACTION_CDC_DRIVER_NOT_WORKING);
                        context.sendBroadcast(intent);
                    }else
                    {
                        Intent intent = new Intent(ACTION_USB_DEVICE_NOT_WORKING);
                        context.sendBroadcast(intent);
                    }
                }
            }else
            {
                // No driver for given device, even generic CDC driver could not be loaded
                Intent intent = new Intent(ACTION_USB_NOT_SUPPORTED);
                context.sendBroadcast(intent);
            }
        }
    }

}

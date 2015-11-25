package com.rabidllamastudios.avigate;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Set;

/**
 * Created by Ryan Staatz
 * This activity is intended as a test for USB-OTG connected CDC-ACM devices (e.g. Arduino)
 * Some code adapted from: https://github.com/felHR85/SerialPortExample
 */

public class ArduinoTestActivity extends AppCompatActivity {

    private TextView mPwmValueTV;
    private TextView mStatusTV;
    private SeekBar mSeekbar;
    private UsbSerialService mUsbSerialService;
    private OutputHandler mOutputHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arduino_test);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //initialize handler
        mOutputHandler = new OutputHandler(this);

        //initialize UI
        mPwmValueTV = (TextView) findViewById(R.id.tv_arduino_value_pwm);
        mStatusTV = (TextView) findViewById(R.id.tv_arduino_value_status);
        mSeekbar = (SeekBar) findViewById(R.id.intensitySlider);
        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) sendInt(progress);
                mPwmValueTV.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    /** Called when the user clicks the Stop button */
    public void stopPwm (View view) {
            sendInt(0);
            mPwmValueTV.setText(String.valueOf(0));
            mSeekbar.setProgress(0);
    }

    /** Called when the user clicks the Half Speed button */
    public void halfPwm (View view) {
            sendInt((127));
            mPwmValueTV.setText(String.valueOf(127));
            mSeekbar.setProgress(127);
    }

    /** Called when the user clicks the Full Speed button */
    public void fullPwm (View view) {
            sendInt(255);
            mPwmValueTV.setText(String.valueOf(255));
            mSeekbar.setProgress(255);
    }

    public void sendInt(int value) {
        if (mUsbSerialService != null) {
            String str = "{" + String.valueOf(value) + "}";
            mUsbSerialService.write(str.getBytes());
        }
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras)
    {
        if(!UsbSerialService.SERVICE_CONNECTED)
        {
            Intent startService = new Intent(this, service);
            if(extras != null && !extras.isEmpty())
            {
                Set<String> keys = extras.keySet();
                for(String key: keys)
                {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void registerUsbReceiver()
    {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbSerialService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbSerialService.ACTION_NO_USB);
        filter.addAction(UsbSerialService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbSerialService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbSerialService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    /*
     * This handler will be passed to UsbSerialService. Dara received from serial port is displayed through this handler
     */
    private static class OutputHandler extends Handler
    {
        private final WeakReference<ArduinoTestActivity> mArduinoTestActivity;

        public OutputHandler (ArduinoTestActivity activity)
        {
            mArduinoTestActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message message)
        {
            switch(message.what)
            {
                case UsbSerialService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) message.obj;
                    if (!data.equals("")) {
                        mArduinoTestActivity.get().mStatusTV.setText(data);
                    }
                    break;
            }
        }
    }

    /*
     * Notifications from UsbSerialService will be received here.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context arg0, Intent arg1)
        {
            if(arg1.getAction().equals(UsbSerialService.ACTION_USB_PERMISSION_GRANTED))
            {
                mStatusTV.setText("USB Ready");
            }else if(arg1.getAction().equals(UsbSerialService.ACTION_USB_PERMISSION_NOT_GRANTED))
            {
                mStatusTV.setText("USB Permission not granted");
            }else if(arg1.getAction().equals(UsbSerialService.ACTION_NO_USB))
            {
                mStatusTV.setText("USB not connected");
            }else if(arg1.getAction().equals(UsbSerialService.ACTION_USB_DISCONNECTED))
            {
                mStatusTV.setText("USB disconnected");
            }else if(arg1.getAction().equals(UsbSerialService.ACTION_USB_NOT_SUPPORTED))
            {
                mStatusTV.setText("USB device not supported");
            }
        }
    };

    private final ServiceConnection usbConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1)
        {
            mUsbSerialService = ((UsbSerialService.UsbBinder) arg1).getService();
            mUsbSerialService.setHandler(mOutputHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            mUsbSerialService = null;
        }
    };

    @Override
    public void onPause() {
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
        super.onPause();
    }

    @Override
    public void onResume() {
        registerUsbReceiver();  // Start listening notifications from UsbSerialService
        startService(UsbSerialService.class, usbConnection, null); // Start UsbSerialService(if it was not started before) and Bind it
        super.onResume();
    }

}

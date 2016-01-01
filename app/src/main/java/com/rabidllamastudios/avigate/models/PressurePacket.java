package com.rabidllamastudios.avigate.models;

import android.content.Intent;
import android.os.Bundle;

import com.rabidllamastudios.avigate.AvigateApplication;

/**
 * Created by Ryan on 11/19/15.
 * A data model class to communicate barometer sensor data (pressure)
 * For convenience, this class can convert to and between Bundle and Intent
 */
public class PressurePacket {
    private static final String PACKAGE_NAME = AvigateApplication.class.getPackage().getName();
    public static final String INTENT_ACTION = PACKAGE_NAME + ".action.PRESSURE_DATA";

    private float mhPa;

    public PressurePacket(float hPa) {
        mhPa = hPa;
    }

    public PressurePacket(Bundle bundle) {
        mhPa = bundle.getFloat("hpa");
    }

    public Intent toIntent() {
        Intent intent = new Intent(INTENT_ACTION);
        intent.putExtra("hpa", mhPa);
        return intent;
    }

    public float getPressure() {
        return mhPa;
    }
}

package com.rabidllamastudios.avigate.model;

import android.content.Intent;
import android.os.Bundle;

import com.rabidllamastudios.avigate.AvigateApplication;

/**
 * Created by Ryan on 11/19/15.
 * A data model class to communicate linear acceleration sensor data
 * For convenience, this class can convert to and between Bundle and Intent
 */
public class LinearAccelerationPacket {
    private static final String PACKAGE_NAME = AvigateApplication.class.getPackage().getName();
    public static final String INTENT_ACTION = PACKAGE_NAME + ".action.LINEAR_ACCELERATION_DATA";

    private float mX;
    private float mY;
    private float mZ;

    public LinearAccelerationPacket(float x, float y, float z) {
        mX = x;
        mY = y;
        mZ = z;
    }

    public LinearAccelerationPacket(Bundle bundle) {
        mX = bundle.getFloat("x");
        mY = bundle.getFloat("y");
        mZ = bundle.getFloat("z");
    }

    public Intent toIntent() {
        Intent intent = new Intent(INTENT_ACTION);
        intent.putExtra("x", mX);
        intent.putExtra("y", mY);
        intent.putExtra("z", mZ);
        return intent;
    }

    public float getX() {
        return mX;
    }

    public float getY() {
        return mY;
    }

    public float getZ() {
        return mZ;
    }
}

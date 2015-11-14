package com.rabidllamastudios.avigate.model;

import android.content.Intent;
import android.os.Bundle;

import com.rabidllamastudios.avigate.AvigateApplication;

import org.rajawali3d.math.Quaternion;

/**
 *  TODO write javadoc
 * Created by Ryan on 11/13/15.
 */
public class OrientationPacket {
    public static final String PACKAGE_NAME = AvigateApplication.class.getPackage().getName();
    public static final String INTENT_ACTION = PACKAGE_NAME + ".action.ORIENTATION_DATA";

    public Quaternion mOrientation;

    public OrientationPacket(Quaternion orientation) {
        mOrientation = orientation;
    }

    public OrientationPacket(Bundle bundle) {
        mOrientation = new Quaternion(bundle.getDouble("w"),bundle.getDouble("x"),bundle.getDouble("y"),bundle.getDouble("z"));
    }

    public Intent toIntent() {
        Intent out = new Intent(INTENT_ACTION);
        out.putExtra("w", mOrientation.w);
        out.putExtra("x", mOrientation.x);
        out.putExtra("y", mOrientation.y);
        out.putExtra("z", mOrientation.z);
        return out;
    }
}

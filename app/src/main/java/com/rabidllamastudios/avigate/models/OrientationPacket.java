package com.rabidllamastudios.avigate.models;

import android.content.Intent;
import android.os.Bundle;

import com.rabidllamastudios.avigate.AvigateApplication;

import org.rajawali3d.math.Quaternion;

/**
 *  TODO write javadoc
 * Created by Ryan on 11/13/15.
 */
public class OrientationPacket {
    private static final String PACKAGE_NAME = AvigateApplication.class.getPackage().getName();
    public static final String INTENT_ACTION = PACKAGE_NAME + ".action.ORIENTATION_DATA";

    private Quaternion mOrientation;

    public OrientationPacket(Quaternion orientation) {
        mOrientation = orientation;
    }

    public OrientationPacket(Bundle bundle) {
        mOrientation = new Quaternion(bundle.getDouble("w"),bundle.getDouble("x"),bundle.getDouble("y"),bundle.getDouble("z"));
    }

    public Intent toIntent() {
        Intent intent = new Intent(INTENT_ACTION);
        intent.putExtra("w", mOrientation.w);
        intent.putExtra("x", mOrientation.x);
        intent.putExtra("y", mOrientation.y);
        intent.putExtra("z", mOrientation.z);
        return intent;
    }

    public Quaternion getOrientation() {
        return mOrientation;
    }
}

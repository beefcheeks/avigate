package com.rabidllamastudios.avigate.models;

import android.content.Intent;
import android.os.Bundle;

import com.rabidllamastudios.avigate.AvigateApplication;

/**
 * A data model class to communicate connection data
 * Can be constructed from a Bundle and converted into an Intent
 * Created by Ryan Staatz on 11/19/15.
 */
public class ConnectionPacket {
    private static final String PACKAGE_NAME = AvigateApplication.class.getPackage().getName();
    public static final String INTENT_ACTION = PACKAGE_NAME + ".action.CONNECTION_DATA";

    private boolean mIsConnected = false;

    /** Constructor that takes a boolean denoting the network connection status */
    public ConnectionPacket(boolean isConnected) {
        mIsConnected = isConnected;
    }

    /** Constructor that takes a bundle. Use toIntent to export class data to an Intent */
    public ConnectionPacket(Bundle bundle) {
        mIsConnected = bundle.getBoolean("con");
    }

    /** Returns an Intent containing the ConnectionPacket data as IntentExtras */
    public Intent toIntent() {
        Intent intent = new Intent(INTENT_ACTION);
        intent.putExtra("con", mIsConnected);
        return intent;
    }

    /** Returns a boolean denoting the network connection status */
    public boolean isConnected() {
        return mIsConnected;
    }
}
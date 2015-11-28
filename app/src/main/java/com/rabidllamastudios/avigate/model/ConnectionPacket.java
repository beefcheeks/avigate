package com.rabidllamastudios.avigate.model;

import android.content.Intent;
import android.os.Bundle;

import com.rabidllamastudios.avigate.AvigateApplication;

/**
 * Created by Ryan on 11/19/15.
 */
public class ConnectionPacket {

    public static final String PACKAGE_NAME = AvigateApplication.class.getPackage().getName();
    public static final String INTENT_ACTION = PACKAGE_NAME + ".action.CONNECTION_DATA";

    private boolean mIsConnected = false;

    public ConnectionPacket(boolean isConnected) {
        mIsConnected = isConnected;
    }

    public ConnectionPacket(Bundle bundle) {
        mIsConnected = bundle.getBoolean("con");
    }

    public Intent toIntent() {
        Intent intent = new Intent(INTENT_ACTION);
        intent.putExtra("con", mIsConnected);
        return intent;
    }

    public boolean isConnected() {
        return mIsConnected;
    }
}
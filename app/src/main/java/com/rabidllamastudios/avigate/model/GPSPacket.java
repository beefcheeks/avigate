package com.rabidllamastudios.avigate.model;

import android.content.Intent;
import android.os.Bundle;

import com.rabidllamastudios.avigate.AvigateApplication;

/**
 * Created by Ryan on 11/19/15.
 * A data model class to communicate GPS sensor data
 * For convenience, this class can convert to and between Bundle and Intent
 */
public class GPSPacket {
    private static final String PACKAGE_NAME = AvigateApplication.class.getPackage().getName();
    public static final String INTENT_ACTION = PACKAGE_NAME + ".action.GPS_DATA";

    private double mLatitude;
    private double mLongitude;
    private double mAccuracy;
    private double mBearing;
    private double mAltitude;

    public GPSPacket(double latitude, double longitude, double accuracy, double bearing, double altitude) {
        mLatitude = latitude;
        mLongitude = longitude;
        mAccuracy = accuracy;
        mBearing = bearing;
        mAltitude = altitude;
    }

    public GPSPacket(Bundle bundle) {
        mLatitude = bundle.getDouble("lat");
        mLongitude = bundle.getDouble("long");
        mAccuracy = bundle.getDouble("acc");
        mBearing = bundle.getDouble("bear");
        mAltitude = bundle.getDouble("alt");
    }

    public Intent toIntent() {
        Intent intent = new Intent(INTENT_ACTION);
        intent.putExtra("lat", mLatitude);
        intent.putExtra("long", mLongitude);
        intent.putExtra("acc", mAccuracy);
        intent.putExtra("bear", mBearing);
        intent.putExtra("alt", mAltitude);
        return intent;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public double getAccuracy() {
        return mAccuracy;
    }

    public double getBearing() {
        return mBearing;
    }

    public double getAltitude() {
        return mAltitude;
    }
}

package com.rabidllamastudios.avigate.helpers;

import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

/**
 * The purpose of this class is to handle permissions, especially for the new system in 6.0
 * Created by Ryan on 11/3/15.
 */
public class PermissionsChecker extends AppCompatActivity {

    public static final int PERMISSIONS_REQUEST_READ_LOCATION_FINE = 0;
    public static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    private final AppCompatActivity mContext;
    private final Callback mCallback;

    public PermissionsChecker(AppCompatActivity context, Callback callback) {
        mContext = context;
        mCallback = callback;
    }

    public boolean hasPermission(String permission, int permissionsConstant) {
        //Pre-marshmallow users use different permissions system, return true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        //Check if marshmallow permissions have already been granted, if so, return true
        if (ContextCompat.checkSelfPermission(mContext, permission) == PackageManager.PERMISSION_GRANTED) return true;
        //Check if marshmallow permissions have been rejected, if so, return false
        if (ActivityCompat.shouldShowRequestPermissionRationale(mContext, permission)) return false;
        //If permissions haven't been requested yet, request permissions
        if (ContextCompat.checkSelfPermission(mContext, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mContext, new String[]{permission}, permissionsConstant);
        }
        //Return false for any other result
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_LOCATION_FINE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (mCallback != null) {
                        mCallback.permissionGranted(PERMISSIONS_REQUEST_READ_LOCATION_FINE);
                    }
                }
                return;
            }
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (mCallback != null) {
                        mCallback.permissionGranted(PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                    }
                }
            }
        }
    }

    /**
     * This is the callback class for PermissionsChecker
     */
    public interface Callback {
        void permissionGranted(int permissionsConstant);
    }
}
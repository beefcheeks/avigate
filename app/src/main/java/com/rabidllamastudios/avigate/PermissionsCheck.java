package com.rabidllamastudios.avigate;

import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by Ryan on 11/3/15.
 */
public class PermissionsCheck extends AppCompatActivity {

    public final int PERMISSIONS_REQUEST_READ_LOCATION_FINE = 0;
    public final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    public PermissionsCheck() {}

    public boolean hasPermission(AppCompatActivity context, String permission, int permissionsConstant) {
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            // if the user has previously denied a permissions request
            if (ActivityCompat.shouldShowRequestPermissionRationale(context, permission)) {
                return false;
            } else {
                //If the user hasn't yet set the permission, prompt them with a permissions request
                ActivityCompat.requestPermissions(context, new String[]{permission}, permissionsConstant);
                //If the user accepts the permissions request, return true
                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
                return false;
            }
        } else {
            //If the user has previously accepted the permission, return true
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult (int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_LOCATION_FINE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                }
                return;
            } case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                }
                return;
            }
        }
    }

}
package com.rabidllamastudios.avigate.models;

import android.content.Intent;
import android.os.Bundle;

import com.rabidllamastudios.avigate.AvigateApplication;

import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;

/**
 *  TODO write javadoc
 * Created by Ryan on 11/13/15.
 */
public class OrientationPacket {
    private static final String PACKAGE_NAME = AvigateApplication.class.getPackage().getName();
    public static final String INTENT_ACTION = PACKAGE_NAME + ".action.ORIENTATION_DATA";

    private Quaternion mRawOrientation;

    public OrientationPacket(Quaternion orientation) {
        mRawOrientation = orientation;
    }

    public OrientationPacket(Bundle bundle) {
        mRawOrientation = new Quaternion(bundle.getDouble("w"), bundle.getDouble("x"),
                bundle.getDouble("y"), bundle.getDouble("z"));
    }

    public Intent toIntent() {
        Intent intent = new Intent(INTENT_ACTION);
        intent.putExtra("w", mRawOrientation.w);
        intent.putExtra("x", mRawOrientation.x);
        intent.putExtra("y", mRawOrientation.y);
        intent.putExtra("z", mRawOrientation.z);
        return intent;
    }

    //Returns the raw orientation quaternion provided by the RotationVector Android Sensor API
    public Quaternion getRawOrientation() {
        return mRawOrientation;
    }

    //Returns a quaternion aligned with the orientation of the craft
    //Takes a boolean to determine the direction of the phone along the nose/tail axis of the craft
    public Quaternion getCraftOrientation(boolean phoneFacingNose) {
        Quaternion coordinateTransform = new Quaternion();
        coordinateTransform.multiply(new Quaternion().fromAngleAxis(Vector3.Axis.Y, 180));
        if (phoneFacingNose) {
            coordinateTransform = coordinateTransform.multiply(
                    new Quaternion().fromAngleAxis(Vector3.Axis.Z, 90));
        } else {
            coordinateTransform = coordinateTransform.multiply(
                    new Quaternion().fromAngleAxis(Vector3.Axis.Z, -90));
        }
        //Created the inverted transform (quaternion) needed to apply the coordinate transformation
        Quaternion invertedTransform = coordinateTransform.invertAndCreate();
        return coordinateTransform.multiply(mRawOrientation).multiply(invertedTransform);
    }

    //Returns the craft pitch angle in degrees
    //Takes a boolean to determine the direction of the phone along the nose/tail axis of the craft
    //Quaternion to pitch angle conversion equation: arcsin(2*(w*y - z*x))
    //See: https://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles#Conversion
    //For sign see: https://en.wikipedia.org/wiki/Aircraft_principal_axes#Lateral_axis_.28pitch.29
    public double getCraftPitch(boolean phoneFacingNose) {
        Quaternion craftOrientation = getCraftOrientation(phoneFacingNose);
        return Math.toDegrees(Math.asin(2 * (craftOrientation.w * craftOrientation.y
                - craftOrientation.z * craftOrientation.x)));
    }

    //Returns the craft roll angle in degrees
    //Takes a boolean to determine the direction of the phone along the nose/tail axis of the craft
    //Quaternion to roll angle conversion equation: atan2(2*(w*x + y*z), 1 - 2*(x^2 + y^2))
    //See: https://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles#Conversion
    //For sign see: https://en.wikipedia.org/wiki/Aircraft_principal_axes#Longitudinal_.28roll.29
    public double getCraftRoll(boolean phoneFacingNose) {
        Quaternion craftOrientation = getCraftOrientation(phoneFacingNose);
        return Math.toDegrees(Math.atan2(2 * (craftOrientation.w * craftOrientation.x
                        + craftOrientation.y * craftOrientation.z),
                1 - 2 * (Math.pow(craftOrientation.x, 2) + Math.pow(craftOrientation.y, 2))));
    }

    //Returns the craft yaw angle in degrees
    //Takes a boolean to determine the direction of the phone along the nose/tail axis of the craft
    //Quaternion to roll angle conversion equation: atan2(2*(w*z + x*y), 1 - 2*(y^2 + z^2))
    //See: https://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles#Conversion
    //For sign see: https://en.wikipedia.org/wiki/Aircraft_principal_axes#Vertical_axis_.28yaw.29
    public double getCraftYaw(boolean phoneFacingNose) {
        Quaternion craftOrientation = getCraftOrientation(phoneFacingNose);
        double yaw = Math.toDegrees(Math.atan2(2 * (craftOrientation.w * craftOrientation.z
                        + craftOrientation.x * craftOrientation.y),
                1 - 2 * (Math.pow(craftOrientation.y, 2) + Math.pow(craftOrientation.z, 2))));
        if (phoneFacingNose) {
            //Yaw values from Android are -180 to 180 degrees. Should be converted to 0 to 360.
            if (yaw < 0) return yaw + 360;
            return yaw;
        }
        //If the phone is facing the tail, the yaw values will need to be flipped 180 degrees
        return yaw + 180;
    }
}
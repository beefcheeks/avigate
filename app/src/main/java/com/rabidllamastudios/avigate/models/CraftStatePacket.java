package com.rabidllamastudios.avigate.models;

import android.content.Intent;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;

import com.rabidllamastudios.avigate.AvigateApplication;

import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;

/**
 * Stores information about the state of the craft (e.g. formatted sensor data)
 * Can be constructed from a bundle and converted into an intent
 * Created by Ryan Staatz on 1/17/2016
 */
public class CraftStatePacket {
    private static final String PACKAGE_NAME = AvigateApplication.class.getPackage().getName();
    public static final String INTENT_ACTION = PACKAGE_NAME + ".action.CRAFT_STATE_DATA";
    private static final String KEY_LOCATION = "loc";

    private AngularVelocity mAngularVelocity;
    private BarometricPressure mBarometricPressure;
    private LinearAcceleration mLinearAcceleration;
    private MagneticField mMagneticField;
    private Orientation mOrientation;
    private Location mLocation;

    /** Constructs a CraftStatePacket from its component static inner classes */
    public CraftStatePacket(AngularVelocity angularVelocity, BarometricPressure barometricPressure,
                            LinearAcceleration linearAcceleration, MagneticField magneticField,
                            Orientation orientation, Location location){
        mAngularVelocity = angularVelocity;
        mBarometricPressure = barometricPressure;
        mLinearAcceleration = linearAcceleration;
        mMagneticField = magneticField;
        mOrientation = orientation;
        mLocation = new Location(location);
    }

    /** Constructor that takes a Bundle. Use toIntent method to export data to an Intent */
    public CraftStatePacket(Bundle bundle) {
        mAngularVelocity = new AngularVelocity(bundle);
        mBarometricPressure = new BarometricPressure(bundle);
        mLinearAcceleration = new LinearAcceleration(bundle);
        mMagneticField = new MagneticField(bundle);
        mOrientation = new Orientation(bundle);
        mLocation = bundle.getParcelable(KEY_LOCATION);
    }

    /** Returns an Intent with all CraftStatePacket data as IntentExtras */
    public Intent toIntent() {
        Intent intent = new Intent(INTENT_ACTION);
        intent = mAngularVelocity.addIntentExtras(intent);
        intent = mBarometricPressure.addIntentExtras(intent);
        intent = mLinearAcceleration.addIntentExtras(intent);
        intent = mMagneticField.addIntentExtras(intent);
        intent = mOrientation.addIntentExtras(intent);
        intent.putExtra(KEY_LOCATION, mLocation);
        return intent;
    }

    /** Returns the instance variable of type AngularVelocity (an inner class) */
    public AngularVelocity getAngularVelocity() {
        return mAngularVelocity;
    }

    /** Returns the instance variable of type Barometric pressure (an inner class) */
    public BarometricPressure getBarometricPressure() {
        return mBarometricPressure;
    }

    /** Returns the instance variable of type LinearAcceleration (an inner class) */
    public LinearAcceleration getLinearAcceleration() {
        return mLinearAcceleration;
    }

    /** Returns the instance variable of type Location (an inner class) */
    public Location getLocation() {
        return mLocation;
    }

    /** Returns the instance variable of type MagneticField (an inner class) */
    public MagneticField getMagneticField() {
        return mMagneticField;
    }

    /** Returns the instance variable of type Orientation (an inner class) */
    public Orientation getOrientation() {
        return mOrientation;
    }

    /** Sets the instance variable of type AngularVelocity (an inner class) */
    public void setAngularVelocity(AngularVelocity angularVelocity) {
        mAngularVelocity = angularVelocity;
    }

    /** Sets the instance variable of type BarometricPressure (an inner class) */
    public void setBarometricPressure(BarometricPressure barometricPressure) {
        mBarometricPressure = barometricPressure;
    }

    /** Sets the instance variable of type LinearAcceleration (an inner class) */
    public void setLinearAcceleration(LinearAcceleration linearAcceleration) {
        mLinearAcceleration = linearAcceleration;
    }

    /** Updates the instance variable of type Location */
    public void setLocation(Location location) {
        mLocation = new Location(location);
    }

    /** Sets the instance variable of type MagneticField (an inner class) */
    public void setMagneticField(MagneticField magneticField) {
        mMagneticField = magneticField;
    }

    /** Sets the instance variable of type Orientation (an inner class) */
    public void setOrientation(Orientation orientation) {
        mOrientation = orientation;
    }

/**
 ********************************* STATIC INNER CLASSES BEGIN HERE *********************************
 */

    /** A static inner data model class that contains angular velocity sensor data
     * Intended to contain values from the sensor type, TYPE_GYROSCOPE
     * Can be constructed from a Bundle and added to an existing Intent
     * For phone axes see: http://developer.android.com/reference/android/hardware/SensorEvent.html
     */
    public static class AngularVelocity {
        private float mX;
        private float mY;
        private float mZ;

        /** Constructs AngularVelocity from its component values (angular velocity about each axis)
         * @param x Angular velocity about the phone's X-axis
         * @param y Angular velocity about the phone's Y-axis
         * @param z Angular velocity about the phone's Z axis
         */
        public AngularVelocity(float x, float y, float z) {
            mX = x;
            mY = y;
            mZ = z;
        }

        /** Constructor that takes a Bundle. Use addIntentExtras method to convert to an Intent */
        public AngularVelocity(Bundle bundle) {
            mX = bundle.getFloat("ang-x");
            mY = bundle.getFloat("ang-y");
            mZ = bundle.getFloat("ang-z");
        }

        /** Returns an Intent with the AngularVelocity component values added to the input Intent */
        public Intent addIntentExtras(Intent intent) {
            intent.putExtra("ang-x", mX);
            intent.putExtra("ang-y", mY);
            intent.putExtra("ang-z", mZ);
            return intent;
        }

        /** Returns the rate of rotation around the phone's X-axis in radians per second */
        public float getX() {
            return mX;
        }

        /** Returns the rate of rotation around the phone's Y-axis in radians per second */
        public float getY() {
            return mY;
        }

        /** Returns the rate of rotation around the phone's Z-axis in radians per second */
        public float getZ() {
            return mZ;
        }

        /** Gets the pitch rate of the craft
         * @param phoneFacingNose the direction of the phone along the nose/tail axis of the craft
         * @return pitch rate in degrees per second
         */
        public double getCraftPitchRate(boolean phoneFacingNose) {
            int sign = -1;
            if (phoneFacingNose) sign = 1;
            return sign*Math.toDegrees(mX);
        }

        /** Gets the roll rate of the craft
         * @param phoneFacingNose the direction of the phone along the nose/tail axis of the craft
         * @return roll rate in degrees per second
         */
        public double getCraftRollRate(boolean phoneFacingNose) {
            int sign = -1;
            if (phoneFacingNose) sign = 1;
            return sign*Math.toDegrees(mY);
        }

        /** Gets the yaw rate of the craft
         * @param phoneFacingNose the direction of the phone along the nose/tail axis of the craft
         * @return yaw rate in degrees per second
         */
        public double getCraftYawRate(boolean phoneFacingNose) {
            int sign = -1;
            if (phoneFacingNose) sign = 1;
            return sign*Math.toDegrees(mZ);        }
    }

    /** A static inner data model class that contains barometric pressure sensor data
     * Intended to contain values from the sensor type, TYPE_PRESSURE
     * Can be constructed from a Bundle and added to an existing Intent
     */
    public static class BarometricPressure {
        private float mhPa;

        /** Constructs a BarometricPressure from a pressure value in hectopascals (hPa) */
        public BarometricPressure(float hPa) {
            mhPa = hPa;
        }

        /** Constructor that takes a Bundle. Use addIntentExtras method to convert to an Intent */
        public BarometricPressure(Bundle bundle) {
            mhPa = bundle.getFloat("hpa");
        }

        /** Returns the input Intent with the BarometricPressure component value added to it */
        public Intent addIntentExtras(Intent intent) {
            intent.putExtra("hpa", mhPa);
            return intent;
        }

        /** Returns the approximate altitude in meters based on the barometric pressure */
        public float getAltitude() {
            return SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, mhPa);
        }

        /** Returns the barometric pressure in hectopascals (hPa) */
        public float getPressure() {
            return mhPa;
        }
    }

    /** A static inner data model class that contains linear acceleration sensor data
     * Intended to contain values from the sensor type, TYPE_LINEAR_ACCELERATION
     * Can be constructed from a Bundle and added to an existing Intent
     * For phone axes see: http://developer.android.com/reference/android/hardware/SensorEvent.html
     */
    public static class LinearAcceleration {
        private float mX;
        private float mY;
        private float mZ;

        /** Constructor that requires linear acceleration parameters for three different axes
         * @param x linear acceleration along the phone's x-axis
         * @param y linear acceleration along the phone's y-axis
         * @param z linear acceleration along the phone's z-axis
         */
        public LinearAcceleration(float x, float y, float z) {
            mX = x;
            mY = y;
            mZ = z;
        }

        /** Constructor that takes a Bundle. Use addIntentExtras method to convert to an Intent */
        public LinearAcceleration(Bundle bundle) {
            mX = bundle.getFloat("lin-x");
            mY = bundle.getFloat("lin-y");
            mZ = bundle.getFloat("lin-z");
        }

        /** Returns the input Intent with the LinearAcceleration component values added to it */
        public Intent addIntentExtras(Intent intent) {
            intent.putExtra("lin-x", mX);
            intent.putExtra("lin-y", mY);
            intent.putExtra("lin-z", mZ);
            return intent;
        }

        /** Returns the acceleration along the phone's X axis in radians per second squared */
        public float getX() {
            return mX;
        }

        /** Returns the acceleration along the phone's Y axis in radians per second squared */
        public float getY() {
            return mY;
        }

        /** Returns the acceleration along the phone's Z axis in radians per second squared */
        public float getZ() {
            return mZ;
        }
    }

    /** A static inner data model class that contains magnetic field sensor data
     * Intended to contain values from the sensor type, TYPE_MAGNETIC_FIELD
     * Can be constructed from a Bundle and added to an existing Intent
     * For phone axes see: http://developer.android.com/reference/android/hardware/SensorEvent.html
     */
    public static class MagneticField {
        private float mX;
        private float mY;
        private float mZ;

        /** Constructs a MagneticField from its component values
         * @param x the magnetic field along the phone's x-axis
         * @param y the magnetic field along the phone's y-axis
         * @param z the magnetic field along the phone's z-axis
         */
        public MagneticField(float x, float y, float z) {
            mX = x;
            mY = y;
            mZ = z;
        }

        /** Constructor that takes a Bundle. Use addIntentExtras method to export data to Intent */
        public MagneticField(Bundle bundle) {
            mX = bundle.getFloat("mag-x");
            mY = bundle.getFloat("mag-y");
            mZ = bundle.getFloat("mag-z");
        }

        /** Returns the input Intent with the MagneticField component values added to it */
        public Intent addIntentExtras(Intent intent) {
            intent.putExtra("mag-x", mX);
            intent.putExtra("mag-y", mY);
            intent.putExtra("mag-z", mZ);
            return intent;
        }

        /** Returns the geomagnetic field strength along the phone's X axis in microteslas (µT) */
        public float getX() {
            return mX;
        }

        /** Returns the geomagnetic field strength along the phone's Y axis in microteslas (µT) */
        public float getY() {
            return mY;
        }

        /** Returns the geomagnetic field strength along the phone's Z axis in microteslas (µT) */
        public float getZ() {
            return mZ;
        }
    }

    /** A static inner data model class that contains orientation sensor data
     * Intended to contain values from the sensor type, TYPE_ROTATION_VECTOR, see below for details:
     * http://developer.android.com/guide/topics/sensors/sensors_motion.html#sensors-motion-rotate
     * Can be constructed from a Bundle and added to an existing Intent
     * For phone axes see: http://developer.android.com/reference/android/hardware/SensorEvent.html
     */
    public static class Orientation {
        private Quaternion mRawOrientation;

        /** Constructor that takes four input Quaternion component values (w, x, y, z) */
        public Orientation(double w, double x, double y, double z) {
            mRawOrientation = new Quaternion(w, x, y, z);
        }

        /** Constructor that takes a Bundle. Use addIntentExtras method to convert to an Intent */
        public Orientation(Bundle bundle) {
            mRawOrientation = new Quaternion(bundle.getDouble("ori-w"), bundle.getDouble("ori-x"),
                    bundle.getDouble("ori-y"), bundle.getDouble("ori-z"));
        }

        /** Returns the input Intent with the raw orientation component values added to it */
        public Intent addIntentExtras(Intent intent) {
            intent.putExtra("ori-w", mRawOrientation.w);
            intent.putExtra("ori-x", mRawOrientation.x);
            intent.putExtra("ori-y", mRawOrientation.y);
            intent.putExtra("ori-z", mRawOrientation.z);
            return intent;
        }

        /** Returns the raw orientation quaternion (a RotationVector from the Android Sensor API) */
        public Quaternion getRawOrientation() {
            return mRawOrientation;
        }

        /** Gets the orientation of the craft from transformed RotationVector sensor data
         * @param phoneFacingNose the direction of the phone along the nose/tail axis of the craft
         * @return a quaternion that represents the orientation of the craft
         */
        public Quaternion getCraftOrientation(boolean phoneFacingNose) {
            Quaternion coordinateTransform = new Quaternion();
            coordinateTransform.multiply(new Quaternion().fromAngleAxis(Vector3.Axis.Y, 180));
            //Configure the coordinate transform based on the orientation of the phone to the craft
            if (phoneFacingNose) {
                coordinateTransform = coordinateTransform.multiply(
                        new Quaternion().fromAngleAxis(Vector3.Axis.Z, 90));
            } else {
                coordinateTransform = coordinateTransform.multiply(
                        new Quaternion().fromAngleAxis(Vector3.Axis.Z, -90));
            }
            //Invert the coordinate transform before multiplying by the raw orientation quaternion
            Quaternion invertedTransform = coordinateTransform.invertAndCreate();
            return coordinateTransform.multiply(mRawOrientation).multiply(invertedTransform);
        }

        /** Returns the pitch angle of the craft in degrees
         * @param phoneFacingNose the direction of the phone along the nose/tail axis of the craft
         * @return craft pitch angle in degrees
         * Quaternion to pitch angle conversion equation: arcsin(2*(w*y - z*x))
         * Conversion: https://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles
         * Sign: https://en.wikipedia.org/wiki/Aircraft_principal_axes#Lateral_axis_.28pitch.29
         */
        public double getCraftPitch(boolean phoneFacingNose) {
            Quaternion craftOrientation = getCraftOrientation(phoneFacingNose);
            return Math.toDegrees(Math.asin(2 * (craftOrientation.w * craftOrientation.y
                    - craftOrientation.z * craftOrientation.x)));
        }

        /** Returns the roll angle of the craft in degrees
         * @param phoneFacingNose the direction of the phone along the nose/tail axis of the craft
         * @return craft roll angle in degrees
         * Quaternion to roll angle conversion equation: atan2(2*(w*x + y*z), 1 - 2*(x^2 + y^2))
         * Conversion: https://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles
         * Sign: https://en.wikipedia.org/wiki/Aircraft_principal_axes#Longitudinal_.28roll.29
         */
        public double getCraftRoll(boolean phoneFacingNose) {
            Quaternion craftOrientation = getCraftOrientation(phoneFacingNose);
            //noinspection SuspiciousNameCombination
            return Math.toDegrees(Math.atan2(2 * (craftOrientation.w * craftOrientation.x
                            + craftOrientation.y * craftOrientation.z),
                    1 - (2 * (Math.pow(craftOrientation.x, 2) + Math.pow(craftOrientation.y, 2)))));
        }

        /** Returns the yaw angle of the craft in degrees
         * @param phoneFacingNose the direction of the phone along the nose/tail axis of the craft
         * @return craft yaw angle in degrees
         * Quaternion to yaw angle conversion equation: atan2(2*(w*z + x*y), 1 - 2*(y^2 + z^2))
         * Conversion: https://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles
         * Sign: https://en.wikipedia.org/wiki/Aircraft_principal_axes#Vertical_axis_.28yaw.29
         */
        public double getCraftYaw(boolean phoneFacingNose) {
            Quaternion craftOrientation = getCraftOrientation(phoneFacingNose);
            //noinspection SuspiciousNameCombination
            double yaw = Math.toDegrees(Math.atan2(2 * (craftOrientation.w * craftOrientation.z
                            + craftOrientation.x * craftOrientation.y),
                    1 - 2 * (Math.pow(craftOrientation.y, 2) + Math.pow(craftOrientation.z, 2))));
            if (phoneFacingNose) {
                //Yaw values from Android are -180 to 180 degrees. Should be converted to 0 to 360.
                if (yaw < 0) return yaw + 360;
                return yaw;
            } else {
                //If the phone is facing the tail, the yaw values are flipped 180 degrees
                return yaw + 180;
            }
        }
    }
}
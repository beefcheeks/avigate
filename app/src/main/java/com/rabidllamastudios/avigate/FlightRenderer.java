package com.rabidllamastudios.avigate;

import android.content.Context;
import android.view.MotionEvent;

import org.rajawali3d.Object3D;
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.loader.LoaderOBJ;
import org.rajawali3d.loader.ParsingException;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.renderer.RajawaliRenderer;

/**
 * TODO write javadoc stuff for future class
 * Created by Ryan on 11/5/15.
 */
public class FlightRenderer extends RajawaliRenderer {

    private Matrix4 mCameraOffsetMatrix;
    private Object3D mAircraftObject;
    private Quaternion mNewQuaternion;
    private Quaternion mAircraftOrientation;

    private static final double RADIUS = 40;

    public FlightRenderer(Context context) {
        super(context);
        this.mContext = context;
        setFrameRate(60);

        mAircraftOrientation = new Quaternion();
        mNewQuaternion = new Quaternion();
    }

    public void initScene() {
        //Create position matrix for camera
        mCameraOffsetMatrix = Matrix4.createTranslationMatrix(0, 0, -RADIUS);
        //Create new Quaternion for reference in later method

        //Create directional light source in scene
        DirectionalLight directionalLight = new DirectionalLight(-100f, -50f, -100f);
        directionalLight.setColor(1.0f, 1.0f, 1.0f);
        directionalLight.setPower(2);
        getCurrentScene().addLight(directionalLight);

        //Import custom object (e.g. aircraft) as focus
        mAircraftObject = importCustomObject(R.raw.eurofighter_obj);
        getCurrentScene().addChild(mAircraftObject);

        //Set camera position and orientation
        getCurrentCamera().setPosition(mCameraOffsetMatrix.getTranslation());
        getCurrentCamera().setRotation(Vector3.Axis.Y, 180);

        try {
            getCurrentScene().setSkybox(R.drawable.skybox);
        } catch (ATexture.TextureException e) {
            e.printStackTrace();
        }

    }

    /** Note: the following settings must be checked when exporting an obj file for use in this app:
     * Apply Modifiers
     * Include Normals
     * Include UVs
     * Write Materials (if applicable)
     * Triangulate Faces
     * Objects as OBJ Objects
     *
     * In addition to the correct export settings, you must rename the output files:
     * <original object file name>_obj
     * <original mtl file name>_mtl
     */
    private Object3D importCustomObject(int rawFile) {
        LoaderOBJ objectLoader = new LoaderOBJ(mContext.getResources(), mTextureManager, rawFile);
        try {
            objectLoader.parse();
        } catch (ParsingException e) {
            e.printStackTrace();
        }
        return objectLoader.getParsedObject();
    }

    public void setAircraftOrientationQuaternion(Quaternion inputQuaternion) {
        synchronized (mAircraftOrientation) {
            mAircraftOrientation.setAll(inputQuaternion);
        }
    }

    //TODO separate method for calibrating coordinate system transformation
    private void setAircraftOrientation() {
        mAircraftObject.setOrientation(mNewQuaternion);
        //transform model coordinates to phone coordinates
        mAircraftObject.rotate(Vector3.Y, 180);
        mAircraftObject.rotate(Vector3.X, -90);
        // transform phone coordinates to real world coordinates
        synchronized (mAircraftOrientation) {
            mAircraftObject.rotate(mAircraftOrientation.invertAndCreate());
        }
        //transform real world coordinates to view world coordinates
        mAircraftObject.rotate(Vector3.X, 90);
    }

    // Camera follows aircraft orientation on yaw and pitch axes
    private void followAircraftWithCamera() {
        getCurrentCamera().setPosition(mAircraftObject.getOrientation().toRotationMatrix().multiply(mCameraOffsetMatrix).getTranslation());
        getCurrentCamera().setLookAt(mAircraftObject.getPosition());
    }

    @Override
    public void onRender(final long elapsedTime, final double deltaTime) {
        super.onRender(elapsedTime, deltaTime);
        setAircraftOrientation();
        followAircraftWithCamera();
    }

    public void onOffsetsChanged(float x, float y, float z, float w, int i, int j) {}

    public void onTouchEvent(MotionEvent event) {}

}

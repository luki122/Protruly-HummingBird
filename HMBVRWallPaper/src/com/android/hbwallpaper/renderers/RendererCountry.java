package com.android.hbwallpaper.renderers;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.android.hbwallpaper.utils.LoggerConfig;
import com.android.hbwallpaper.vr.SkySphere;

import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by antino on 17-6-19.
 */
public class RendererCountry implements  GLSurfaceView.Renderer,SensorEventListener {
    private static final String TAG = RendererCountry.class.getSimpleName();
    SkySphere mSkySphere;
    SensorManager mSensorManager;
    private Sensor mRotation;
    private float[] matrix=new float[16];

    public RendererCountry(Context context,String vrDrawable){
        mSensorManager=(SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensors=mSensorManager.getSensorList(Sensor.TYPE_ALL);
        mRotation=mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mSkySphere=new SkySphere(context.getApplicationContext(),vrDrawable);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        if (LoggerConfig.ON) {
            Log.i(TAG, "Wallpaper renderer : onSurfaceCreated");
        }
        mSkySphere.create();
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_FRONT);
        // Position the eye in front of the origin.
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = 0.0f;

        // We are looking toward the distance
        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = -1.0f;

        // Set our up vector. This is where our head would be pointing were we holding the camera.
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        // Set the view matrix. This matrix can be said to represent the camera position.
        // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
        // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
        Matrix.setLookAtM(matrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);
        mSkySphere.setMatrix(matrix);

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (LoggerConfig.ON) {
            Log.i(TAG, "Wallpaper renderer : onSurfaceChanged width = " + width + " , height = " + height);
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT| GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glClearColor(0,0,0,0);
        mSkySphere.setSize(width, height);
        GLES20.glViewport(0,0,width ,height);
        mSkySphere.setMatrix(matrix);
        mSkySphere.draw();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (LoggerConfig.ON) {
            Log.i(TAG, "Wallpaper renderer : onDrawFrame");
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT| GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glClearColor(0,0,0,0);
        mSkySphere.draw();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (LoggerConfig.ON) {
            Log.i(TAG, "Wallpaper renderer : onSensorChanged");
        }
        SensorManager.getRotationMatrixFromVector(matrix,event.values);
        mSkySphere.setMatrix(matrix);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void onResume(){
        if (LoggerConfig.ON) {
            Log.i(TAG, "Wallpaper renderer : onResume");
        }
        mSensorManager.registerListener(this, mRotation, SensorManager.SENSOR_DELAY_GAME);

    }

    public void onPause() {
        if (LoggerConfig.ON) {
            Log.i(TAG, "Wallpaper renderer : onPause");
        }
        mSensorManager.unregisterListener(this);
    }

    public void onDestroy(){
        mSkySphere.clear();
    }
}

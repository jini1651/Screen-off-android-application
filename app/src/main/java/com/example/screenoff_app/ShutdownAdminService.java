package com.example.screenoff_app;

import android.Manifest;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.IOException;

import static androidx.core.app.ActivityCompat.startActivityForResult;

public class ShutdownAdminService extends Service {
    private IBinder mBinder = new LocalBinder();

    CameraSource cameraSource;

    long startTime;
    long onMissingTime = 0;

    public ShutdownAdminService() {
    }

    public class LocalBinder extends Binder {
        public ShutdownAdminService getService(){
            return ShutdownAdminService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    void turnScreenOff(Context context){
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getApplicationContext().getSystemService(Context.DEVICE_POLICY_SERVICE);

        ComponentName componentName = new ComponentName(getApplicationContext(), ShutdownAdminReceiver.class);

        if(!devicePolicyManager.isAdminActive(componentName)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
            startActivityForResult(MainActivity,ShutdownAdminService.this, 0);
            return;
        }
        devicePolicyManager.lockNow();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null) return Service.START_STICKY;
        else {
            boolean signal = intent.getBooleanExtra("signal", false);
            if (signal)    createCameraSource();
            else            return Service.START_STICKY;
        }
        Toast.makeText(ShutdownAdminService.this, "started!", Toast.LENGTH_LONG).show();

        return super.onStartCommand(intent, flags, startId);
    }

    private class EyesTracker extends Tracker<Face> {

        private final float THRESHOLD = 0.75f;

        public EyesTracker() {

        }

        @Override
        public void onUpdate(Detector.Detections<Face> detections, Face face) {
            if (face.getIsLeftEyeOpenProbability() > THRESHOLD || face.getIsRightEyeOpenProbability() > THRESHOLD) {
                startTime = System.currentTimeMillis();
            } else {
                onMissingTime = System.currentTimeMillis() - startTime;
                if (onMissingTime >= 5000) {
                    Intent returnIntent = new Intent(getApplicationContext(), MainActivity.class);
                    returnIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    returnIntent.putExtra("message", true);
                    startActivity(returnIntent);
                }
            }
        }

        @Override
        public void onMissing(Detector.Detections<Face> detections) {
            super.onMissing(detections);
            onMissingTime = System.currentTimeMillis() - startTime;
            //if (onMissingTime >= 5000) { turnScreenOff(getApplicationContext());}
            Toast.makeText(ShutdownAdminService.this, "face not detected!", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onDone() {
            super.onDone();
        }
    }
    public void createCameraSource() {
        FaceDetector detector = new FaceDetector.Builder(this)
                .setTrackingEnabled(true)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setMode(FaceDetector.FAST_MODE)
                .build();
        detector.setProcessor(new MultiProcessor.Builder(new FaceTrackerFactory()).build());

        cameraSource = new CameraSource.Builder(this, detector)
                .setRequestedPreviewSize(1024, 768)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(30.0f)
                .build();

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            cameraSource.start();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    public class FaceTrackerFactory implements MultiProcessor.Factory<Face> {

        private FaceTrackerFactory() {

        }

        @Override
        public Tracker<Face> create(Face face) {
            return new EyesTracker();
        }
    }
}
package com.example.screenoff_app;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class ShutdownAdminService extends Service {
    private IBinder iBinder = new LocalBinder();

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
       return iBinder;
    }
}
package ru.elifantiev.rallyresults.service;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

public class WakeLockHolder {

    private static final String TAG = "WakeLockHolder";
    private static final String wakeLockName = "ru.elifantiev.rallyresults.WAKE_LOCK";
    private static PowerManager.WakeLock wakeLock = null;
    private Context ctx;

    public WakeLockHolder(Context ctx) {
        this.ctx = ctx;
    }

    synchronized private PowerManager.WakeLock getLock() {
        if(wakeLock == null) {
            PowerManager powerManager = (PowerManager)ctx.getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockName);
            wakeLock.setReferenceCounted(true);
        }
        return wakeLock;
    }

    public void acquire() {
        if(!getLock().isHeld()) {
            Log.d(TAG, "Acquiring wake lock");
            getLock().acquire();
        }
    }

    public void release() {
        if(getLock().isHeld()) {
            Log.d(TAG, "Releasing wake lock");
            getLock().release();
        }
    }
}
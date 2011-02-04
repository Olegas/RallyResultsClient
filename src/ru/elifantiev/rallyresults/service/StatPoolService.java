package ru.elifantiev.rallyresults.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import ru.elifantiev.rallyresults.CompetitionActivity;
import ru.elifantiev.rallyresults.R;
import ru.elifantiev.rallyresults.RallyWebService;
import ru.elifantiev.rallyresults.infrastructure.RallySection;
import ru.elifantiev.rallyresults.infrastructure.StatRecord;

import java.io.IOException;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

public class StatPoolService extends Service {

    private final int notifyId = 1;
    private boolean hasClients = true;
    private RallyWebService svc = null;
    private WakeLockHolder lockHolder = null;
    private Stack<StatRecord> statQueue = null;
    private final IBinder binder = new StatPoolBinder();
    private OnStatRefreshListener listener = null;
    private final String lock = "lock", uploadLock = "uploadLock";
    private AtomicReference<Boolean> isUploading = null;
    private Timer timer;
    private final TimerTask uploadTask = new TimerTask() {
        @Override
        public void run() {
            synchronized (uploadLock) {
                if (!isUploading.get()) {
                    StatRecord record = getNextRecord();
                    if (record != null) {
                        new UploadTask().execute(record);
                    }
                }
            }
        }
    };

    public class StatPoolBinder extends Binder {
        public StatPoolService getService() {
            return StatPoolService.this;
        }
    }

    public interface OnStatRefreshListener {
        public void onStatRefresh(RallySection sectionData);
    }

    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        hasClients = false;
        return false;
    }

    private StatRecord getNextRecord() {
        StatRecord retval = null;
        synchronized (lock) {
            if (statQueue.size() > 0) {
                retval = statQueue.pop();
                if (statQueue.size() > 0)
                    startForeground(notifyId, getNotification());
            }
        }
        return retval;
    }

    public void uploadStatRecord(StatRecord record) {
        synchronized (lock) {
            statQueue.push(record);
            startForeground(notifyId, getNotification());
            lockHolder.acquire();
        }
    }

    public int getQueueStatus() {
        synchronized (lock) {
            return statQueue.size();
        }
    }

    public void setOnStatRefreshListener(OnStatRefreshListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate() {
        if (svc == null) {
            SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
            svc = new RallyWebService(
                    getString(R.string.wsRootUrl),
                    prefs.getString("login", ""),
                    prefs.getString("password", ""));
        }

        if (lockHolder == null)
            lockHolder = new WakeLockHolder(this);

        if (timer == null) {
            timer = new Timer();
            timer.scheduleAtFixedRate(uploadTask, 0, 60000);
        }

        if (statQueue == null)
            statQueue = new Stack<StatRecord>();

        if (isUploading == null)
            isUploading = new AtomicReference<Boolean>(false);

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        lockHolder.release();
        timer.cancel();
        statQueue.clear();
        svc = null;
        lockHolder = null;
        timer = null;
        statQueue = null;
        if (listener != null)
            listener = null;
        Log.d("StatPoolService", "Service destroyed");
    }

    private Notification getNotification() {
        Notification retval = new Notification(android.R.drawable.stat_notify_sync, "Syncing...", System.currentTimeMillis());
        retval.flags = Notification.FLAG_FOREGROUND_SERVICE | Notification.FLAG_NO_CLEAR;
        retval.number = statQueue.size();
        retval.defaults = 0;

        Intent intent = new Intent(this, CompetitionActivity.class);
        intent.setAction("Number" + String.valueOf(retval.number));
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                Intent.FLAG_ACTIVITY_NEW_TASK | PendingIntent.FLAG_UPDATE_CURRENT);

        retval.setLatestEventInfo(this, "RallyResults", "Syncing...", pendingIntent);
        return retval;
    }

    private class UploadTask extends AsyncTask<StatRecord, Void, RallySection> {

        @Override
        protected RallySection doInBackground(StatRecord... statRecords) {
            isUploading.set(true);
            try {
                return svc.updateStatRecord(statRecords[0]);
            } catch (IOException e) {
                uploadStatRecord(statRecords[0]);
            }
            return null;
        }

        @Override
        protected void onPostExecute(RallySection rallySection) {
            isUploading.set(false);
            lockHolder.release();
            stopForeground(true);
            if(!hasClients)
                stopSelf();
            if (listener != null && rallySection != null) {
                listener.onStatRefresh(rallySection);
            }
        }
    }
}

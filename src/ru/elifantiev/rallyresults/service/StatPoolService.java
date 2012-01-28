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
import java.util.Queue;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

public class StatPoolService extends Service {

    private final int notifyId = 1;
    private RallyWebService svc = null;
    //private WakeLockHolder lockHolder = null;
    private BlockingQueue<StatRecord> statQueue = null;
    private final IBinder binder = new StatPoolBinder();
    private OnStatRefreshListener listener = null;
    private UploadingWorker worker;
    //private AtomicReference<Boolean> isUploading = null;
    //private Timer timer;
    /*private final TimerTask uploadTask = new TimerTask() {
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
    };*/

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
        return false;
    }

    public void uploadStatRecord(StatRecord record) {
        statQueue.add(record);
        startForeground(notifyId, getNotification());
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

        /*if (lockHolder == null)
            lockHolder = new WakeLockHolder(this);
*/
        if (statQueue == null)
            statQueue = new LinkedBlockingQueue<StatRecord>();

        if(worker == null)
            worker = new UploadingWorker(svc, statQueue);

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        worker.cancel();
        //lockHolder.release();
        statQueue.clear();
        svc = null;
        //lockHolder = null;
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

    /*private class UploadTask extends AsyncTask<StatRecord, Void, RallySection> {

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
            if (statQueue.size() > 0)
                startForeground(notifyId, getNotification());
            else
                stopForeground(true);
            if (listener != null && rallySection != null) {
                listener.onStatRefresh(rallySection);
            }
        }
    }*/
}

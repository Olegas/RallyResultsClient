package ru.elifantiev.rallyresults.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import ru.elifantiev.rallyresults.CompetitionActivity;
import ru.elifantiev.rallyresults.R;
import ru.elifantiev.rallyresults.RallyWebService;
import ru.elifantiev.rallyresults.infrastructure.RallySection;
import ru.elifantiev.rallyresults.infrastructure.StatRecord;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class StatPoolService extends Service implements UploadingWorker.WorkerStatusChangeListener {

    private final int notifyId = 1;
    private RallyWebService svc = null;
    private BlockingQueue<StatRecord> statQueue = null;
    private final IBinder binder = new StatPoolBinder();
    private OnStatRefreshListener listener = null;
    private UploadingWorker worker;
    private Handler uiThreadHandler;

    @Override
    public void onQueueLengthChanged(final int queueLength) {
        uiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (queueLength > 0)
                    startForeground(notifyId, getNotification(queueLength));
                else
                    stopForeground(true);
            }
        });

    }

    @Override
    public void onDataReceived(final RallySection section) {
        if(listener != null)
            uiThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onStatRefresh(section);
                }
            });
    }

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
        int size = statQueue.size();
        statQueue.add(record);
        startForeground(notifyId, getNotification(size + 1));
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

        if (uiThreadHandler == null)
            uiThreadHandler = new Handler();

        if (worker == null) {
            worker = new UploadingWorker(this, svc, statQueue, this);
            worker.start();
        }



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

    private Notification getNotification(int queueSize) {
        Notification retval = new Notification(android.R.drawable.stat_notify_sync, "Syncing...", System.currentTimeMillis());
        retval.flags = Notification.FLAG_FOREGROUND_SERVICE | Notification.FLAG_NO_CLEAR;
        retval.number = queueSize;
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

}

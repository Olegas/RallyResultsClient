package ru.elifantiev.rallyresults.service;

import ru.elifantiev.rallyresults.RallyWebService;
import ru.elifantiev.rallyresults.infrastructure.StatRecord;

import java.util.concurrent.BlockingQueue;

public class UploadingWorker extends Thread {

    private final BlockingQueue<StatRecord> queue;
    private final RallyWebService service;
    private boolean doWork = true;

    UploadingWorker(RallyWebService service, BlockingQueue<StatRecord> recordsQueue) {
        queue = recordsQueue;
        this.service = service;
    }


    @Override
    public void run() {
        StatRecord rec = null;
        while(doWork) {
            if(true) { // check connectivity here
                try {
                    if(rec == null)
                        rec = queue.take();
                    service.updateStatRecord(rec);
                } catch (Exception e) {
                    if(rec != null) {
                        try {
                            queue.put(rec);
                            rec = null; // if successful - clear pointer
                        } catch (InterruptedException e1) {
                            // ignore
                            // if can't put, go to the next cycle and repeat there
                        }
                    }
                }
            }
        }
    }
    
    public void cancel() {
        doWork = false;
    }
}

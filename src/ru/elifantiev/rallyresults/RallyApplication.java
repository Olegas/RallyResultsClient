package ru.elifantiev.rallyresults;

import android.app.Application;
import ru.elifantiev.android.roboerrorreporter.RoboErrorReporter;


public class RallyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        RoboErrorReporter.bindReporter(this);
    }
}

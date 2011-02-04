package ru.elifantiev.rallyresults;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import ru.elifantiev.rallyresults.infrastructure.RallySection;
import ru.elifantiev.rallyresults.infrastructure.StatRecord;
import ru.elifantiev.rallyresults.service.StatPoolService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class InputActivity extends Activity implements ServiceConnection, StatPoolService.OnStatRefreshListener {

    int competitionId, sectionId;
    private Spinner spnStats, spnNumber;
    private ProgressDialog progress;
    protected RallyWebService svc;
    private TextView txtStartHour, txtStartMinute, txtStartSecond,
            txtFinishHour, txtFinishMinute, txtFinishSecond, txtFinishMSecond;
    LinkedHashMap<String, StatRecord> statHash = new LinkedHashMap<String, StatRecord>();
    StatPoolService boundService = null;

    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        boundService = ((StatPoolService.StatPoolBinder) iBinder).getService();
        boundService.setOnStatRefreshListener(this);
    }

    public void onServiceDisconnected(ComponentName componentName) {
        boundService = null;
    }

    @Override
    protected void onStart() {
        bindService(
                new Intent(InputActivity.this, StatPoolService.class),
                InputActivity.this,
                Context.BIND_AUTO_CREATE);
        super.onStart();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected void onPause() {
        unbindService(this);
        super.onPause();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inputscreen);

        Intent callingIntent = getIntent();
        spnStats = (Spinner) findViewById(R.id.spnItemToEdit);

        competitionId = callingIntent.getExtras().getInt("competitionId");
        sectionId = callingIntent.getExtras().getInt("sectionId");

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);

        spnNumber = (Spinner) findViewById(R.id.spnNumber);
        txtStartHour = ((TextView) findViewById(R.id.startHour));
        txtStartMinute = ((TextView) findViewById(R.id.startMinute));
        txtStartSecond = ((TextView) findViewById(R.id.startSecond));
        txtFinishHour = ((TextView) findViewById(R.id.finishHour));
        txtFinishMinute = ((TextView) findViewById(R.id.finishMinute));
        txtFinishSecond = ((TextView) findViewById(R.id.finishSecond));
        txtFinishMSecond = ((TextView) findViewById(R.id.finishMillisecond));


        svc = new RallyWebService(getString(R.string.wsRootUrl),
                prefs.getString("login", ""),
                prefs.getString("password", ""));

        findViewById(R.id.btnEdit).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                HashMap<String, String> statItem = (HashMap<String, String>) spnStats.getSelectedItem();
                StatRecord selected = statHash.get(statItem.get("number"));

                spnNumber.setSelection(spnStats.getSelectedItemPosition());

                txtStartHour.setText(selected.getStartHour());
                txtStartMinute.setText(selected.getStartMinute());
                txtStartSecond.setText(selected.getStartSecond());

                txtFinishHour.setText(selected.getFinishHour());
                txtFinishMinute.setText(selected.getFinishMinute());
                txtFinishSecond.setText(selected.getFinishSecond());
                txtFinishMSecond.setText(selected.getFinishMSecond());
            }
        });

        findViewById(R.id.sendResult).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                StatRecord rec = new StatRecord(
                        (String) spnNumber.getSelectedItem(),
                        competitionId,
                        sectionId);
                try {
                    rec.setStart(
                            txtStartHour.getText().toString(),
                            txtStartMinute.getText().toString(),
                            txtStartSecond.getText().toString());
                    rec.setFinish(
                            txtFinishHour.getText().toString(),
                            txtFinishMinute.getText().toString(),
                            txtFinishSecond.getText().toString(),
                            txtFinishMSecond.getText().toString());

                    boundService.uploadStatRecord(rec);
                } catch (StatRecord.ParseException e) {
                    Log.d("InputActivity", "Format parse: " + e.getMessage());
                }
                clearInputs();
            }
        });

        new AsyncLoadSection().execute();

        startService(new Intent(this, StatPoolService.class));
    }

    private void clearInputs() {
        spnNumber.setSelection(0);
        txtStartHour.setText("");
        txtStartMinute.setText("");
        txtStartSecond.setText("00");
        txtFinishHour.setText("");
        txtFinishMinute.setText("");
        txtFinishSecond.setText("");
        txtFinishMSecond.setText("");
    }

    private void fillNumbers(RallySection rallySection) {
        List<StatRecord> nStats = rallySection.getStats();
        List<String> nValues = new ArrayList<String>(nStats.size());

        for (StatRecord record : nStats) {
            nValues.add(record.getNumber());
            statHash.put(record.getNumber(), null);
        }

        ArrayAdapter<String> na = new ArrayAdapter<String>(
                InputActivity.this,
                android.R.layout.simple_spinner_item,
                nValues);
        na.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnNumber.setAdapter(na);
    }

    public void onStatRefresh(RallySection rallySection) {
        String[] keys = new String[]{"number", "start", "finish"};
        int[] views = new int[]{R.id.statNumber, R.id.statStart, R.id.statFinish};
        List<StatRecord> stats = rallySection.getStats();
        List<HashMap<String, String>> values = new ArrayList<HashMap<String, String>>(stats.size() + 1);

        for (StatRecord record : stats) {
            HashMap<String, String> recItem = new HashMap<String, String>(3);

            recItem.put("number", record.getNumber());
            recItem.put("start", record.getStart());
            recItem.put("finish", record.getFinish());
            values.add(recItem);
            statHash.put(record.getNumber(), record);
        }

        setTitle(rallySection.getCompetitionName() + ", " + rallySection.getSectionName());
        spnStats.setAdapter(
                new SimpleAdapter(InputActivity.this, values, R.layout.statlistitem, keys, views)
        );
    }

    abstract public class AsyncGetStat<T> extends AsyncTask<T, Void, RallySection> {

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(InputActivity.this, "", getString(R.string.loadingSection), true, false);
        }

        @Override
        protected void onPostExecute(RallySection rallySection) {
            progress.dismiss();
            fillNumbers(rallySection);
            onStatRefresh(rallySection);
        }
    }

    public class AsyncLoadSection extends AsyncGetStat<Void> {

        @Override
        protected RallySection doInBackground(Void... strings) {
            return svc.getSectionStats(competitionId, sectionId);
        }
    }
}

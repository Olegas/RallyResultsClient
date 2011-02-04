package ru.elifantiev.rallyresults;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Map;

public class CompetitionActivity extends Activity {

    SharedPreferences prefs;
    protected ProgressDialog progress;
    Integer selectedValue = -1;
    Integer[] keys;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        String login, password;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.competitions);
        prefs = getSharedPreferences("prefs", MODE_PRIVATE);

        login = prefs.getString("login", "");
        password = prefs.getString("password", "");

        findViewById(R.id.next).setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                if(selectedValue > 0) {
                    Intent callingIntent = new Intent(CompetitionActivity.this, SectionActivity.class);
                    callingIntent.putExtra("competitionId", selectedValue);
                    CompetitionActivity.this.startActivity(callingIntent);
                }
            }
        });

        progress = ProgressDialog.show(this, "", getString(R.string.loadingCompetitions), true, false);
        new AsyncLoadCompetitions().execute(getString(R.string.wsRootUrl), login, password);
    }

    public class MyOnItemSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            if(keys != null) {
                selectedValue = keys[pos];
            }
        }

        public void onNothingSelected(AdapterView parent) {
        }
    }

    private class AsyncLoadCompetitions extends AsyncTask<String, Object, Map<Integer, String>> {

        @Override
        protected Map<Integer, String> doInBackground(String... strings) {
            RallyWebService svc = new RallyWebService(strings[0], strings[1], strings[2]);
            return svc.getCompetitions();
        }

        @Override
        protected void onPostExecute(Map<Integer, String> competitions) {
            progress.dismiss();
            keys = competitions.keySet().toArray(new Integer[competitions.size()]);
            Spinner spinner = (Spinner) findViewById(R.id.competitionList);
            ArrayList<String> lst = new ArrayList<String>(competitions.values());

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    getApplicationContext(),
                    android.R.layout.simple_spinner_item,
                    lst
            );

            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            spinner.setOnItemSelectedListener(new MyOnItemSelectedListener());
            spinner.setAdapter(adapter);
        }
    }

}

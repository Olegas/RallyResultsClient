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


public class SectionActivity extends Activity {

    ProgressDialog progress;
    Integer[] keys;
    Integer selectedValue = -1;
    int competitionId = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        String login, password;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.sections);

        competitionId = getIntent().getExtras().getInt("competitionId");

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);

        login = prefs.getString("login", "");
        password = prefs.getString("password", "");

        findViewById(R.id.next).setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                if(selectedValue > 0) {
                    Intent callingIntent = new Intent(SectionActivity.this, InputActivity.class);
                    callingIntent.putExtra("competitionId", competitionId);
                    callingIntent.putExtra("sectionId", selectedValue);
                    SectionActivity.this.startActivity(callingIntent);
                }
            }
        });

        progress = ProgressDialog.show(this, "", getString(R.string.loadingSections), true, false);
        new AsyncLoadSections().execute(getString(R.string.wsRootUrl), login, password);
    }

    public class MyOnItemSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            if(keys != null) {
                selectedValue = keys[pos];
            }
        }

        public void onNothingSelected(AdapterView parent) {
            selectedValue = -1;
        }
    }

    private class AsyncLoadSections extends AsyncTask<String, Object, Map<Integer, String>> {

        @Override
        protected Map<Integer, String> doInBackground(String... strings) {
            RallyWebService svc = new RallyWebService(strings[0], strings[1], strings[2]);
            return svc.getSections(competitionId);
        }

        @Override
        protected void onPostExecute(Map<Integer, String> sections) {
            progress.dismiss();
            keys = sections.keySet().toArray(new Integer[sections.size()]);
            Spinner spinner = (Spinner) findViewById(R.id.sectionList);
            ArrayList<String> lst = new ArrayList<String>(sections.values());
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

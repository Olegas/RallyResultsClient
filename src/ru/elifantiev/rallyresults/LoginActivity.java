package ru.elifantiev.rallyresults;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity {

    protected SharedPreferences prefs;
    protected String login, password;
    protected ProgressDialog progress;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        prefs = getSharedPreferences("prefs", MODE_PRIVATE);

        login = prefs.getString("login", "");
        password = prefs.getString("password", "");

        ((EditText) findViewById(R.id.login)).setText(login);
        ((EditText) findViewById(R.id.password)).setText(password);

        findViewById(R.id.doLogin).setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {

                login = ((EditText) LoginActivity.this.findViewById(R.id.login)).getText().toString();
                password = ((EditText) LoginActivity.this.findViewById(R.id.password)).getText().toString();

                LoginActivity.this.prefs.edit().putString("login", login).putString("password", password).commit();

                progress = ProgressDialog.show(LoginActivity.this, "", getString(R.string.loggingIn), true, false);
                new AsyncLogin().execute(getString(R.string.wsRootUrl), login, password);
            }
        });
    }

    private class AsyncLogin extends AsyncTask<String, Object, Boolean> {

        @Override
        protected Boolean doInBackground(String... strings) {
            return new RallyWebService(strings[0], strings[1], strings[2]).login();
        }

        @Override
        protected void onPostExecute(Boolean loginResult) {
            progress.dismiss();
            if (loginResult)
                LoginActivity.this.startActivity(new Intent(LoginActivity.this, CompetitionActivity.class));
            else
                Toast.makeText(getApplicationContext(), getString(R.string.loginFailed), Toast.LENGTH_LONG).show();
        }
    }
}

package com.example.quintabinf.easybus;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;


public class ActivityAccount extends ActionBarActivity {
    TextView labelNomeCognome;

    SharedPreferences preferenzeLogin;

    ProgressDialog progressAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_account);
        overridePendingTransition(R.anim.right_in,R.anim.left_out);

        // Recupero gli oggetti dalla view
        labelNomeCognome = (TextView) findViewById(R.id.labelNomeCognome);

        // Recupero gli handler per le preferenze
        preferenzeLogin = getSharedPreferences("login", 0);

        // Recupero i dati dalle preferenze
        String username = preferenzeLogin.getString("username","");

        // Controllo se la connessione è disponibile
        if (isNetworkAvailable()) {
            // Dichiaro la variabile server
            String server = "maurizio96.noip.me/~maurizio";

            // Compongo l'url con la query
            String[] dati = {
                    server,
                    URLEncoder.encode(username)
            };

            // Mostro la finestra di progresso
            progressAccount = ProgressDialog.show(ActivityAccount.this, "", "Recupero informazioni account");

            new Login().execute(dati);
        } else {
            // Comunico all'utente che è necessaria una connessione internet
            Toast.makeText(ActivityAccount.this,"E' necessaria una connessione internet",Toast.LENGTH_SHORT).show();
            ActivityAccount.this.finish();
        }
    }

    private boolean isNetworkAvailable() {
        // Controllo se la connessione è attiva
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                overridePendingTransition(R.anim.left_in, R.anim.right_out);
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.left_in, R.anim.right_out);
    }

    class Login extends AsyncTask<String, String, String> {
        String sRisultato;

        // Dichiaro una variabile per salvare l'url
        String url;

        Login()
        {
            // Inizializzo le variabili
            url = "";
            sRisultato = "";
        }
        @Override
        protected String doInBackground(String... dati) {
            try {
                // Imposto i nuovi indirizzi di partenza e arrivo
                String server = dati[0];
                String username = dati[1];

                // Compongo l'url
                url = String.format("http://%s/cgi-bin/informazioni.php?username=%s",server,username);

                // Eseguo la richiesta e elaboro il risultato
                HttpClient httpclient = new DefaultHttpClient();
                HttpResponse response = httpclient.execute(new HttpGet(url));
                HttpEntity entity = response.getEntity();
                InputStream webs = entity.getContent();

                // Definisco il buffer reader
                BufferedReader reader = new BufferedReader(new InputStreamReader(webs, "iso-8859-1"), 8 );

                // Dichiaro una variabile per contenere il risultato
                StringBuilder risultato = new StringBuilder();

                // Leggo tutto il risultato
                String line;
                while ((line = reader.readLine()) != null) {
                    // Aggiungo la riga letta al risultato
                    risultato.append(line);
                }

                // Trasformo il risultato
                sRisultato = risultato.toString();
            } catch (Exception e)
            {
                // Mostro il toast di errore
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ActivityAccount.this, "Errore nella connessione con il server", Toast.LENGTH_SHORT).show();
                    }
                });
                ActivityAccount.this.finish();
            }

            return sRisultato;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                // Controllo se le credenziali sono corrette
                JSONObject utente = new JSONObject(result).getJSONObject("utente");
                if (utente.getString("username").equals(preferenzeLogin.getString("username",""))) {
                    // Recupero i dati dal JSON
                    String username = utente.getString("username");
                    String nome = utente.getString("nome");
                    String cognome = utente.getString("cognome");

                    // Modifico i dati mostrati
                    labelNomeCognome.setText(nome + " " + cognome);
                } else {
                    Toast.makeText(ActivityAccount.this, "Username errato", Toast.LENGTH_SHORT).show();
                    ActivityAccount.this.finish();
                }
            } catch (Exception e)
            {
                ActivityAccount.this.finish();
            }

            // Cancello la finestra di progresso
            progressAccount.dismiss();
        }
    }
}

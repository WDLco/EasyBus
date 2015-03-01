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
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;


public class ActivityLogin extends ActionBarActivity {
    private EditText txtUsername;
    private EditText txtPassword;

    private Button buttonLogin;

    SharedPreferences preferenzeLogin;

    ProgressDialog progressLogin;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_login);
        overridePendingTransition(R.anim.right_in,R.anim.left_out);

        // Recupero gli oggetti dalla view
        buttonLogin = (Button) findViewById(R.id.buttonLogin);
        txtUsername = (EditText) findViewById(R.id.txtUsername);
        txtPassword = (EditText) findViewById(R.id.txtPassword);

        // Recupero gli handler per le preferenze
        preferenzeLogin = getSharedPreferences("login", 0);

        // Imposto l'onclicklistener sul bottone di login
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickLogin();
            }
        });

        // Disabilito il bottone di login
        buttonLogin.setEnabled(false);

        // Chiamo la funzione per lanciare un thread che controllerà i dati
        lanciaThreadControllore();
    }

    private boolean isNetworkAvailable() {
        // Controllo se la connessione è attiva
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }

    private void clickLogin()
    {
        // Controllo se la connessione è disponibile
        if (isNetworkAvailable()) {
            // Mostro la finestra di progresso
            progressLogin = ProgressDialog.show(ActivityLogin.this, "", "Effettuando l'accesso");

            // Dichiaro le variabili per username e password
            String username = txtUsername.getText().toString();
            String password = txtPassword.getText().toString();

            // Dichiaro la variabile server
            String server = "maurizio96.noip.me/~maurizio";

            // Compongo l'url con la query
            String[] dati = {server, URLEncoder.encode(username), URLEncoder.encode(password)};

            new Login().execute(dati);
        } else {
            // Comunico all'utente che è necessaria una connessione internet
            Toast.makeText(ActivityLogin.this,"E' necessaria una connessione internet",Toast.LENGTH_SHORT).show();
        }
    }

    private void lanciaThreadControllore() {
        // Implemento un nuovo thread
        Thread controllore = new Thread() {
            @Override
            public void run() {
                while(true) {
                    try {
                        sleep(500);
                        // Controllo se sono stati inseriti tutti i campi
                        if (txtUsername.getText().length()>0 && txtPassword.getText().length()>0)
                        {
                            // Abilito il bottone
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    buttonLogin.setEnabled(true);
                                }
                            });
                        }
                        else
                        {
                            // Disabilito il bottone
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    buttonLogin.setEnabled(false);
                                }
                            });
                        }
                    } catch (Exception e) {
                        Log.e("ThreadControllore", e.getMessage());
                    }
                }
            }
        };

        // Avvio il thread
        controllore.start();
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
                String password = dati[2];

                // Compongo l'url
                url = String.format("http://%s/cgi-bin/autenticazione.php?username=%s&password=%s",server,username,password);

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
                Log.e("risultatoLogin",sRisultato);
            } catch (Exception e) {
                // Mostro il toast di errore
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ActivityLogin.this, "C'è stato un errore nella connessione con il server", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            return sRisultato;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                // Controllo se è stato prodotto un risultato
                if (result.length() > 0) {
                    // Controllo se le credenziali sono corrette
                    JSONObject utente = new JSONObject(result).getJSONObject("utente");
                    if (!utente.getString("username").equals("")) {
                        // Recupero i dati dal JSON
                        String username = utente.getString("username");

                        // Modifico il valore della variabile booleana
                        SharedPreferences.Editor editor = preferenzeLogin.edit();
                        editor.putBoolean("accessoEffettuato", true);
                        editor.putString("username", username);
                        editor.commit();

                        // Avvio l'activity account
                        Intent intent = new Intent(ActivityLogin.this, ActivityAccount.class);
                        startActivity(intent);

                        // Imposto l'animazione per la nuova activity
                        overridePendingTransition(R.anim.right_in, R.anim.left_out);
                        finish();
                    } else {
                        Toast.makeText(ActivityLogin.this, "Username o password errati", Toast.LENGTH_LONG).show();
                    }
                }
            } catch (Exception e)
            {
                Toast.makeText(ActivityLogin.this, "C'è stato un errore nel login", Toast.LENGTH_SHORT).show();
            }

            // Cancello la finestra di progresso
            progressLogin.dismiss();
        }
    }
}

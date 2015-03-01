package com.example.quintabinf.easybus;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
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
import java.util.Calendar;
import java.util.Date;


public class ActivityRicerca extends ActionBarActivity implements NfcAdapter.CreateNdefMessageCallback {
    private Button buttonData;
    private Button buttonOra;
    private Button buttonRicerca;
    private EditText txtPartenza;
    private EditText txtArrivo;

    // Dichiaro le variabili per data e ora per le dialog
    private int year;
    private int month;
    private int day;
    private int hour;
    private int minute;

    // Dichiaro le variabili per data e ora da usare nella ricerca
    private int searchYear;
    private int searchMonth;
    private int searchDay;
    private int searchHour;
    private int searchMinute;

    // Dichiaro i due ID per le finestre di dialogo per data e ora
    private final int idDateDialog = 998;
    private final int idTimeDialog = 999;

    private ProgressDialog progress;

    SharedPreferences preferenzeLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_ricerca);

        // Recupero la data e l'ora corrente
        final Calendar c = Calendar.getInstance();
        year = c.get(Calendar.YEAR);
        month = c.get(Calendar.MONTH);
        day = c.get(Calendar.DAY_OF_MONTH);
        hour = c.get(Calendar.HOUR_OF_DAY);
        minute = c.get(Calendar.MINUTE);

        // Inizializzo la data e l'ora da utilizzare nella ricerca
        searchYear = year;
        searchMonth = month;
        searchDay = day;
        searchHour = hour;
        searchMinute = minute;

        // Recupero gli oggetti dalla activity
        buttonData = (Button) findViewById(R.id.buttonData);
        buttonOra = (Button) findViewById(R.id.buttonOra);
        buttonRicerca = (Button) findViewById(R.id.buttonRicerca);
        txtPartenza = (EditText) findViewById(R.id.txtPartenza);
        txtArrivo = (EditText) findViewById(R.id.txtArrivo);

        // Imposto il valore di default dei bottoni di ricerca dell'ora e della data
        buttonData.setText(new StringBuilder().append(String.format("%02d",day)).append("-").append(String.format("%02d",month+1)).append("-").append(year));
        buttonOra.setText(new StringBuilder().append(String.format("%02d",hour)).append(':').append(String.format("%02d",minute)));

        // Imposto l'NFC
        NfcAdapter mAdapter = NfcAdapter.getDefaultAdapter(this);

        // Controllo se il dispositivo supporta l'NFC
        if (mAdapter != null) {
            if (!mAdapter.isEnabled()) {
                Toast.makeText(this, "Attivare l'NFC dalle impostazioni", Toast.LENGTH_LONG).show();
            } else {
                mAdapter.setNdefPushMessageCallback(this, this);
            }
        }

        // Recupero gli handler per le preferenze
        preferenzeLogin = getSharedPreferences("login", 0);

        // Imposto i listener sul bottone della data
        buttonData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Mostro la finestra di dialogo per la scelta della data
                showDialog(idDateDialog);
            }
        });

        // Imposto i listener sul bottone dell'ora
        buttonOra.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Mostro la finestra di dialogo per la scelta dell'ora
                showDialog(idTimeDialog);
            }
        });

        // Imposto il listener sul bottone ricerca
        buttonRicerca.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Controllo se la connessione è disponibile
                if (isNetworkAvailable()) {
                    // Controllo se i dati sono accettabili
                    if (searchYear > 0 && searchMonth >= 0 && searchDay > 0 && searchHour >= 0 && searchMinute >= 0 && !txtArrivo.getText().toString().equals("") && !txtPartenza.getText().toString().equals("")) {
                        // Dichiaro la variabile server
                        String server = "maurizio96.noip.me/~maurizio";

                        // Compongo l'url con la query
                        String[] dati = {
                                          server ,
                                          URLEncoder.encode(txtPartenza.getText().toString()),
                                          URLEncoder.encode(txtArrivo.getText().toString()),
                                          "" + searchYear + "-" + String.format("%02d",(searchMonth+1)) + "-" + String.format("%02d",searchDay) + "+" + String.format("%02d",searchHour) + ":" + String.format("%02d",searchMinute) + ":00"
                                        };

                        // Mostro la finestra di attesa
                        progress = ProgressDialog.show(ActivityRicerca.this, "","Ricerca percorso");

                        // Chiamo e eseguo la funzione per inviare la richiesta get al server
                        new ConnessioneServer().execute(dati);


                    }
                    else
                    {
                        // Comunico all'utente che i dati non sono accettabili
                        Toast.makeText(ActivityRicerca.this,"I dati inseriti non sono accettabili",Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Comunico all'utente che è necessaria una connessione internet
                    Toast.makeText(ActivityRicerca.this,"E' necessaria una connessione internet",Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Disabilito il bottone di ricerca
        buttonRicerca.setEnabled(false);

        // Chiamo la funzione per lanciare un thread che controllerà i dati
        lanciaThreadControllore();
    }

    private boolean isNetworkAvailable() {
        // Controllo se la connessione è attiva
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
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
                        if (txtPartenza.getText().length()>0 && txtArrivo.getText().length()>0)
                        {
                            // Dichiaro due stringhe per contenere la data e l'ora di ricerca e quella corrente
                            // String sDataRicerca = String.format("%s/%s/%s %s:%s",searchDay,searchMonth+1,searchYear,searchHour,searchMinute);
                            // String sDataCorrente = (DateFormat.format("dd/MM/yyyy HH:mm", new Date().getTime())).toString();

                            // Abilito il bottone
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    buttonRicerca.setEnabled(true);
                                }
                            });
                        }
                        else
                        {
                            // Disabilito il bottone
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    buttonRicerca.setEnabled(false);
                                }
                            });
                        }
                    } catch (Exception e) {
                        Log.e("ThreadControllore",e.getMessage());
                    }
                }
            }
        };

        // Avvio il thread
        controllore.start();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        // Controllo quale id è stato aperto
        switch (id) {
            case idDateDialog:
                // Apro la finestra per la scelta della data
                DatePickerDialog dialogData = new DatePickerDialog(this, listnerData, year, month,day);
                dialogData.getDatePicker().setMinDate(new Date().getTime()-1000);
                dialogData.getDatePicker().setCalendarViewShown(false);
                dialogData.getDatePicker().setSpinnersShown(true);
                return dialogData;
            case idTimeDialog:
                // Apro la finestra per la scelta dell'ora
                return new TimePickerDialog(this,listnerOra,hour,minute,true);
        }
        return null;
    }

    // Implemento la funzione listner della data
    private DatePickerDialog.OnDateSetListener listnerData = new DatePickerDialog.OnDateSetListener() {
        // Implemento la funzione da eseguire quando viene impostata la data
        @Override
        public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
            // Salvo la data impostata dall'utente in tre variabili
            year = selectedYear;
            month = selectedMonth;
            day = selectedDay;

            // Salvo la data impostata dall'utente nelle variabili dedicate alla ricerca
            searchYear = selectedYear;
            searchMonth = selectedMonth;
            searchDay = selectedDay;

            // Modifico il testo del bottone
            buttonData.setText(new StringBuilder().append(String.format("%02d",day)).append("-").append(String.format("%02d",month+1)).append("-").append(year));
        }
    };

    // Implemento la funzione listner dell'ora
    private TimePickerDialog.OnTimeSetListener listnerOra = new TimePickerDialog.OnTimeSetListener() {
        // Implemento la funzione da eseguire quando viene impostata l'ora
        @Override
        public void onTimeSet(TimePicker view, int selectedHour, int selectedMinute) {
            // Salvo l'ora impostata dall'utente in due variabili
            hour = selectedHour;
            minute = selectedMinute;

            // Salvo l'ora impostata dall'utente nelle variabili dedicate alla ricerca
            searchHour = selectedHour;
            searchMinute = selectedMinute;

            // Modifico il testo del bottone
            buttonOra.setText(new StringBuilder().append(String.format("%02d",hour)).append(':').append(String.format("%02d",minute)));
        }
    };

    // =========================================
    // ================= MENU ==================
    // =========================================
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Aggiungo le opzioni al menu
        menu.clear();
        if (!getSharedPreferences("login",0).getBoolean("accessoEffettuato",false)) {
            menu.add(R.string.login);
        }
        else
        {
            menu.add(R.string.logout);
            menu.add(R.string.myaccount);
        }

        return true;
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu)
//    {
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.menu_activity_ricerca, menu);
//        super.onCreateOptionsMenu(menu);
//        return true;
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getTitle().toString().equals(getResources().getString(R.string.login)))
        {
            // Avvio l'activity login
            Intent intent = new Intent(ActivityRicerca.this, ActivityLogin.class);
            startActivity(intent);

            // Imposto l'animazione per la nuova activity
            overridePendingTransition(R.anim.right_in, R.anim.left_out);
        } else if (item.getTitle().toString().equals(getResources().getString(R.string.logout)))
        {
            // Modifico il valore della variabile booleana
            SharedPreferences.Editor editor = preferenzeLogin.edit();
            editor.putBoolean("accessoEffettuato", false);
            editor.commit();

            // Ricarico le opzioni
            invalidateOptionsMenu();

            Toast.makeText(this,"Logout effettuato",Toast.LENGTH_LONG).show();
        } else if (item.getTitle().toString().equals(getResources().getString(R.string.myaccount)))
        {
            // Avvio l'activity account
            Intent intent = new Intent(ActivityRicerca.this, ActivityAccount.class);
            startActivity(intent);

            // Imposto l'animazione per la nuova activity
            overridePendingTransition(R.anim.right_in, R.anim.left_out);
        }

//        if (item.getItemId() == R.id.actionAccount)
//        {
//            // Avvio l'activity account
//            Intent intent = new Intent(ActivityRicerca.this, ActivityAccount.class);
//            startActivity(intent);
//
//            // Imposto l'animazione per la nuova activity
//            overridePendingTransition(R.anim.right_in, R.anim.left_out);
//        }

        return true;
    }

    // ===============================================================
    // ========================= NFC TAG =============================
    // ===============================================================
    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        String idViaggio = "9834y508237y";
        NdefRecord ndefRecord = NdefRecord.createMime("text/plain", idViaggio.getBytes());
        NdefMessage ndefMessage = new NdefMessage(ndefRecord);
        return ndefMessage;
    }

    @Override
    public void onResume() {
        super.onResume();

        Intent intent = getIntent();

        TextView textBigliettoTimbrato = (TextView) findViewById(R.id.textBigliettoTimbrato);

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMessages = intent.getParcelableArrayExtra(
                    NfcAdapter.EXTRA_NDEF_MESSAGES);

            NdefMessage message = (NdefMessage) rawMessages[0];
            textBigliettoTimbrato.setText(new String(message.getRecords()[0].getPayload()));

        } else
            textBigliettoTimbrato.setText("Waiting for NDEF Message");
    }

    // ========================================================
    // =========== ASYNCTASK CONNESSIONE SERVER ===============
    // ========================================================
    class ConnessioneServer extends AsyncTask<String, Void, String> {
        // Dichiaro il risultato in formato String
        String sRisultato;

        // Dichiaro le variabili per salvare i dati dell'url
        String server;
        String indirizzoPartenza;
        String indirizzoArrivo;
        String ora;

        // Dichiaro una variabile per salvare l'url
        String url;

        ConnessioneServer()
        {
            // Inizializzo le variabili
            sRisultato = "";
            server = "";
            indirizzoPartenza = "";
            indirizzoArrivo = "";
            ora = "";
            url = "";
        }

        @Override
        protected String doInBackground(String... dati)
        {
            try
            {
                // Imposto i nuovi indirizzi di partenza e arrivo
                server = dati[0];
                indirizzoPartenza = dati[1];
                indirizzoArrivo = dati[2];
                ora = dati[3];

                // Compongo l'url
                url = String.format("http://%s/cgi-bin/percorso.Cerca.py?partenza=%s&arrivo=%s&ora=%s",server,indirizzoPartenza,indirizzoArrivo,ora);

                // Definisco i parametri per la richiesta
                HttpClient httpclient = new DefaultHttpClient();
                HttpGet httpget = new HttpGet(url);

                // Eseguo la query e salvo il risultato
                HttpResponse response = httpclient.execute(httpget);
                HttpEntity entity = response.getEntity();
                InputStream webs = entity.getContent();

                try
                {
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
                }
                catch(Exception e)
                {
                    // Mostro il toast di errore
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ActivityRicerca.this,"Errore nella connessione al server",Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            catch(Exception e)
            {
                // Mostro il toast di errore
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ActivityRicerca.this,"Errore nella connessione al server",Toast.LENGTH_SHORT).show();
                    }
                });
            }
            return sRisultato;
        }

        @Override
        protected void onPostExecute(String risultato)
        {
            // Controllo se ho ricevuto un risultato
            if (risultato.length() > 0) {
                // Dichiaro una variabile booleana di appoggio per sapere se ho cambiato indirizzo partenza o indirizzo arrivo
                boolean indirizzoSbagliato = false;
                boolean errore = false;

                // Try-Catch controllo ambiguità indirizzo arrivo
                try {
                    // Controllo se il risultato ha prodotto un solo indirizzo
                    JSONObject appoggio = new JSONObject(risultato).getJSONObject("errore-arrivo");
                    if (appoggio.getString("stato").equals("Ambiguous")) {
                        // Modifico la lista di indirizzi restituita dal server
                        String sIndirizzi = appoggio.getString("indirizzi").replace("[", "").replace("]", "").replaceAll("\', \'", "\", \"");
                        sIndirizzi = sIndirizzi.substring(1, sIndirizzi.length() - 1);

                        // Dichiaro il vettore dove contenere gli indirizzi
                        final String[] vIndirizzi = sIndirizzi.split("\",\"+");

                        // Mostro la finestra con la lista di indirizzi
                        AlertDialog.Builder builder = new AlertDialog.Builder(ActivityRicerca.this);
                        builder.setTitle("Indirizzo arrivo ambiguo")
                                .setItems(vIndirizzi, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Prendo l'indirizzo cliccato dall'utente
                                        ActivityRicerca.this.txtArrivo.setText(vIndirizzi[which]);
                                    }
                                }).show();

                        // Modifico la variabile booleana
                        indirizzoSbagliato = true;
                    }
                    else if (appoggio.getString("stato").equals("Error"))
                    {
                        Toast.makeText(ActivityRicerca.this,"La ricerca dell'indirizzo di arrivo non ha prodotto nessun risultato", Toast.LENGTH_LONG).show();
                        errore = true;
                    }
                } catch (Exception e) {
                    Log.e("INDIRIZZI", "Nessuna ambiguità in arrivo");
                }

                // Try-Catch controllo ambiguità indirizzo partenza
                try {
                    // Controllo se il risultato ha prodotto un solo indirizzo
                    JSONObject appoggio = new JSONObject(risultato).getJSONObject("errore-partenza");
                    if (appoggio.getString("stato").equals("Ambiguous")) {
                        // Modifico la lista di indirizzi restituita dal server
                        String sIndirizzi = appoggio.getString("indirizzi").replace("[", "").replace("]", "").replaceAll("\', \'", "\", \"");
                        sIndirizzi = sIndirizzi.substring(1, sIndirizzi.length() - 1);

                        // Dichiaro il vettore dove contenere gli indirizzi
                        final String[] vIndirizzi = sIndirizzi.split("\",\"+");

                        // Mostro la finestra con la lista di indirizzi
                        AlertDialog.Builder builder = new AlertDialog.Builder(ActivityRicerca.this);
                        builder.setTitle("Indirizzo partenza ambiguo")
                                .setItems(vIndirizzi, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Prendo l'indirizzo cliccato dall'utente
                                        ActivityRicerca.this.txtPartenza.setText(vIndirizzi[which]);
                                    }
                                }).show();

                        // Modifico la variabile booleana
                        indirizzoSbagliato = true;
                    }
                    else if (appoggio.getString("stato").equals("Error"))
                    {
                        Toast.makeText(ActivityRicerca.this,"La ricerca dell'indirizzo di partenza non ha prodotto nessun risultato", Toast.LENGTH_LONG).show();
                        errore = true;
                    }
                } catch (Exception e) {
                    Log.e("INDIRIZZI", "Nessuna ambiguità in partenza");
                }

                // Controllo se sono stati cambiati gli indirizzi
                if (!indirizzoSbagliato && !errore) {
                    // Avvio la nuova activity
                    Intent intent = new Intent(ActivityRicerca.this, ActivityRisultati.class);
                    intent.putExtra("risultato", risultato);
                    startActivity(intent);

                    // Imposto l'animazione per la nuova activity
                    overridePendingTransition(R.anim.right_in, R.anim.left_out);
                }
            }

            // Cancello la finestra di attesa
            progress.dismiss();
        }
    }
}

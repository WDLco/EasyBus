package com.example.quintabinf.easybus;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;


public class ActivityRisultati extends ActionBarActivity {
    // Dichiaro un vettore dove inserire tutti i passi o paline
    Passo[] vettorePassi;

    // Dichiaro un array dove inserire tutti i passi o paline
    ArrayList<Passo> arrayPassiCompleto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_risultati);
        overridePendingTransition(R.anim.right_in,R.anim.left_out);

        // Recupero il risultato dalla precedente activity
        String risultato = getIntent().getExtras().getString("risultato");

        // Istanzio l'array completo
        arrayPassiCompleto = new ArrayList<>();

        try {
            // Parso tutto il risultato
            JSONArray arrayRisultati = new JSONArray(risultato);

            // Recupero la listView
            ListView listView = (ListView) findViewById(R.id.listView);

            // Dichiaro i due oggetti JSON da leggere
            JSONObject palina;
            JSONObject tratto;

            // Dichiaro un vettore di oggetti da me creati
            vettorePassi = new Passo[arrayRisultati.length()];

            // Creo un ciclo per leggere tutto il risultato
            int i = 0;
            while (i < arrayRisultati.length() - 1) {
                // Leggo la palina ed il tratto
                palina = arrayRisultati.getJSONObject(i).getJSONObject("palina");
                tratto = arrayRisultati.getJSONObject(i + 1).getJSONObject("tratto");

                // Aggiungo la palina alla lista
                vettorePassi[i] = new Passo('P', palina.getString("nome"));

                // Controllo a quale mezzo fa riferimento il tratto
                if (tratto.getString("mezzo").equals("B")) {
                    // Aggiungo il tratto in pullman
                    vettorePassi[i + 1] = new Passo('T', 'B', tratto.getString("nome"), tratto.getBoolean("pedana"), tratto.getString("infoTratto"), tratto.getString("tempoAttesa"));
                } else if (tratto.getString("mezzo").equals("P")) {
                    // Aggiungo il tratto a piedi
                    vettorePassi[i + 1] = new Passo('T', 'P', "", false, tratto.getString("infoTratto"), tratto.getString("tempoAttesa"));
                } else if (tratto.getString("mezzo").equals("M")) {
                    // Aggiungo il tratto in metro
                    vettorePassi[i + 1] = new Passo('T', 'M', tratto.getString("nome"), tratto.getBoolean("pedana"), tratto.getString("infoTratto"), tratto.getString("tempoAttesa"));
                }

                // Incremento l'indice
                i = i + 2;
            }

            // Leggo la palina di arrivo
            palina = arrayRisultati.getJSONObject(i).getJSONObject("palina");

            // Aggiungo alla lista la palina
            vettorePassi[i] = new Passo('P', palina.getString("nome"));

            // Dichiaro e istanzio una lista di passi da visualizzare
            ArrayList<Passo> passiVisualizzati = new ArrayList<>();

            // Aggiungo alla lista di passi i passi salvati nel vettore
            i = 0;
            while (i < vettorePassi.length) {
                // Controllo se si tratta di un tratto
                if (!vettorePassi[i].tipo.equals('P')) {
                    // Aggiungo all'array di passi da visualizzare l'elemento in posizione indicata dall'indice
                    passiVisualizzati.add(vettorePassi[i]);
                }

                // Aggiungo l'oggetto all'array completo
                arrayPassiCompleto.add(vettorePassi[i]);

                // Incremento l'indice
                i++;
            }

            // Dichiaro il mio adapter modificato legato alla lista di passi e lo assegno alla listView
            CustomAdapter adapter = new CustomAdapter(this, passiVisualizzati);
            listView.setAdapter(adapter);

            // Imposto il testo del 'titolo' dove viene mostrata partenza e arrivo
            TextView labelPartenzaArrivo = (TextView) findViewById(R.id.labelPartenzaArrivo);
            labelPartenzaArrivo.setText(vettorePassi[0].nome + " → " + vettorePassi[vettorePassi.length - 1].nome);
            labelPartenzaArrivo.setMovementMethod(LinkMovementMethod.getInstance());
        }
        catch (Exception e)
        {
            Toast.makeText(this,"Errore nel parsing del risultato",Toast.LENGTH_SHORT).show();

            // Chiudo l'activity
            finish();
            overridePendingTransition(R.anim.left_in, R.anim.right_out);
        }


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

    class CustomAdapter extends ArrayAdapter<Passo>
    {
        // Dichiaro un array di passi di appoggio
        ArrayList<Passo> passiVisualizzati;

        public CustomAdapter(Context context, ArrayList<Passo> passiVisualizzati)
        {
            super(context, 0, passiVisualizzati);

            // Salvo la lista di passi passati
            this.passiVisualizzati = passiVisualizzati;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // IDK
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.riga_risultato, null);

            // Recupero dalla view gli oggetti
            TextView istruzioni = (TextView) convertView.findViewById(R.id.textViewRowResult);
            ImageView icona = (ImageView) convertView.findViewById(R.id.iconaTratto);

            // Dalla lista di passi prendo quello nella posizione indicata
            Passo c = getItem(position);

            // Se è una palina
            if (c.tipo.equals('P')) {
                istruzioni.setText("Palina " + c.nome);
                icona.setImageResource(0);
            }
            // Se è un tratto a piedi
            else if (c.tipo.equals('T') && c.mezzo.equals('P')) {
                istruzioni.setText("Cammina fino a " + vettorePassi[arrayPassiCompleto.indexOf(c) + 1].nome + "\n" + c.infoTratto);
                icona.setImageResource(R.drawable.piedi_icon_tratto);
            }
            // Se è un tratto con il bus
            else if (c.tipo.equals('T') && c.mezzo.equals('B')) {
                istruzioni.setText("Prendere la linea bus " + c.linea + "\n" + c.infoTratto + "\n" + (c.tempoAttesa.length() > 0 ? "Tempo di attesa: " + c.tempoAttesa : "") + "\n" + (c.pedana ? "Presente pedana" : ""));
                icona.setImageResource(R.drawable.bus_icon_tratto);
            }
            // Se è un tratto con la metro
            else if (c.tipo.equals('T') && c.mezzo.equals('M')) {
                istruzioni.setText("Prendere la linea " + c.linea + "\n" + c.infoTratto + "\n" + (c.tempoAttesa.length() > 0 ? "Tempo di attesa: " + c.tempoAttesa : "") + "\n" + (c.pedana ? "Presente pedana" : ""));
                icona.setImageResource(R.drawable.metro_icon_tratto);
            }

            return convertView;
        }
    }

    class Passo
    {
        Character tipo; // Tratto - Palina
        String nome; // Nome palina
        Character mezzo; // Mezzo
        String linea; // Linea mezzo
        boolean pedana; // Pedana vero/falso
        String infoTratto; // Info tratto
        String tempoAttesa; // Tempo attesa

        // Costruttore tratto
        Passo(Character tipo, Character mezzo, String linea, boolean pedana, String infoTratto, String tempoAttesa)
        {
            // Controllo se il tratto è in bus/metro/piedi
            if (tipo.equals('T') && (mezzo.equals('B') || mezzo.equals('M') || mezzo.equals('P')))
            {
                // Aggiorno i miei dati
                this.tipo = tipo;
                this.mezzo = mezzo;
                this.linea = linea;
                this.pedana = pedana;
                this.infoTratto = infoTratto;
                this.tempoAttesa = tempoAttesa;

                // Imposto a null tutti gli altri parametri
                this.nome = null;
            }
        }

        // Costruttore palina
        Passo(Character tipo, String nome)
        {
            // Controllo se è una palina
            if (tipo.equals('P'))
            {
                // Aggiorno i miei dati
                this.tipo = tipo;
                this.nome = nome;

                // Imposto a null tutti gli altri parametri
                this.mezzo = null;
                this.linea = null;
                this.pedana = false;
                this.infoTratto = null;
                this.tempoAttesa = null;
            }
        }
    }
}

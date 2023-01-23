package com.example.tts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.speech.tts.TextToSpeech;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    TTSListener listener;
    TextToSpeech tts;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        listener = new TTSListener();
        tts = new TextToSpeech(getBaseContext(), listener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void iniciar(View v) {
        EditText et = findViewById(R.id.editTextTextPersonName);
        tts.speak(et.getText(), TextToSpeech.QUEUE_ADD, null, ""+System.nanoTime());
    }

    public void signalStrengh(View v) {
        WifiManager wifiManager = (WifiManager) getBaseContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int maxLevels = 100;
        // Level of current connection
        int rssi = wifiManager.getConnectionInfo().getRssi();
        int level = WifiManager.calculateSignalLevel(rssi, maxLevels);
        // wifi= uai fai :)
        tts.speak("Nivel de la señal es " + level + " porciento.", TextToSpeech.QUEUE_ADD, null, ""+System.nanoTime());
    }

    public void singalSignature(View v) {
        WifiManager wifiManager = (WifiManager) getBaseContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                boolean success = intent.getBooleanExtra(
                        WifiManager.EXTRA_RESULTS_UPDATED, false);
                if (success) {
                    scanSuccess();
                } else {
                    // scan failure handling
                    scanFailure2();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        getBaseContext().getApplicationContext().registerReceiver(wifiScanReceiver, intentFilter);
        boolean success = wifiManager.startScan();
        if (!success) {
            // scan failure handling
            scanFailure1();
        } else {
            EditText locName = findViewById(R.id.editTextLocationName);
            tts.speak("Escaneando intensidades para " + locName.getText(), TextToSpeech.QUEUE_ADD, null, ""+System.nanoTime());
        }
    }

    protected void scanSuccess() {
        WifiManager wifiManager = (WifiManager) getBaseContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        StringBuffer result = new StringBuffer();
        int maxLevels = 100;

        List<ScanResult> wifiList = wifiManager.getScanResults();
        Collections.sort(wifiList, (ScanResult sc1, ScanResult sc2) ->  { return new Integer(sc2.level).compareTo(new Integer(sc1.level)); } );

        for (ScanResult scanResult : wifiList) {
            int level = WifiManager.calculateSignalLevel(scanResult.level, maxLevels);
            result.append("[").append(level).append("%] ");
            result.append(scanResult.BSSID).append("\n");
        }

        EditText et = findViewById(R.id.editTextTextMultiLine);
        et.setText(result.toString());
    }

    protected void scanFailure1() {
        tts.speak("Error al iniciar el scan. Verificar permisos.", TextToSpeech.QUEUE_ADD, null, ""+System.nanoTime());
    }

    protected void scanFailure2() {
        tts.speak("Error en el scan. ", TextToSpeech.QUEUE_ADD, null, ""+System.nanoTime());
    }


    public class TTSListener implements TextToSpeech.OnInitListener {
        @Override
        public void onInit(int status) {
        }
    }

}
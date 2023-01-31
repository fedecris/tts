package com.example.tts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // Niveles de intensidad
    static final int MAX_LEVELS = 100;

    TTSListener listener;
    TextToSpeech tts;
    IntentFilter intentFilter;
    boolean registered = false;

    // Nombre de la ubicacion que se está escaneando actualmente
    EditText currentlocationName;

    // El total de scans completo para una ubicacion.
    // Para un location tenemos una list de scans (cuantos más scans para una ubicacion más preciso).
    // Cada scan tiene su lista de intensidad segun su BSSID: location -> scan list -> intensidades list
    // Ej:
    //  biblioteca  -> Scan 1   -> BBSID_-> 96
    //                          -> BBSID_-> 87
    //  biblioteca  -> Scan 2   -> BBSID_-> 99
    //                          -> BBSID_-> 88
    //  alumnos     -> Scan 1   -> BBSID_-> 73
    //                          -> BBSID_-> 68
    Map<String, List<Map<String, Integer>>> generalScan = new HashMap<>();

    // Se esta evaluando la posicion o se esta realizando lectura?
    boolean evaluatingWhereAmI = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listener = new TTSListener();
        tts = new TextToSpeech(getBaseContext(), listener);
        currentlocationName = findViewById(R.id.editTextLocationName);
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
        speak(et.getText().toString());
    }

    public void signalStrengh(View v) {
        try {
            WifiManager wifiManager = (WifiManager) getBaseContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int maxLevels = 100;
            // Level of current connection
            int rssi = wifiManager.getConnectionInfo().getRssi();
            int level = WifiManager.calculateSignalLevel(rssi, maxLevels);
            // wifi= uai fai :)
            speak("Nivel de la señal es " + level + " porciento.");
        } catch (Exception e) {
            speak("No fue posible determinar el nivel de la señal.");
        }
    }

    /** Dispara la lectura de señales wifi para una ubicacion dada */
    public void singalSignature(View v) {
        evaluatingWhereAmI = false;
        scanWifi();
    }

    /** Configuracion de lecturas de intensidades de señales wifi */
    public void scanWifi() {
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

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        if (!registered) {
            getBaseContext().getApplicationContext().registerReceiver(wifiScanReceiver, intentFilter);
            registered = true;
        }
        boolean success = wifiManager.startScan();
        if (!success) {
            // scan failure handling
            scanFailure1();
        } else {
            if (evaluatingWhereAmI) {
                speak("Analizando tu ubicacion actual");
            } else {
                speak("Escaneando intensidades para " + currentlocationName.getText());
            }
        }
    }

    /** Recoleccion de datos o evaluacion de ubicacion */
    protected void scanSuccess() {
        WifiManager wifiManager = (WifiManager) getBaseContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        StringBuffer result = new StringBuffer();

        List<ScanResult> wifiList = wifiManager.getScanResults();
        Collections.sort(wifiList, (ScanResult sc1, ScanResult sc2) ->  { return new Integer(sc2.level).compareTo(new Integer(sc1.level)); } );
        if (wifiList.size()==0) {
            return;
        }

        if (evaluatingWhereAmI) {
            estimateLocation(wifiList);
        } else {
            // incorporar al maestro de scans el nuevo escaneo en la lista de scans para la ubicacion dada
            if (generalScan.get(currentlocationName.getText().toString().toLowerCase()) == null) {
                generalScan.put(currentlocationName.getText().toString().toLowerCase(), new ArrayList<>());
            }
            //Log.d("TTS", "Size del generalScan es " + generalScan.get(currentlocationName.getText().toString().toLowerCase()).size());

            // Visualizacion de info y carga en la map
            HashMap<String, Integer> pair = new HashMap<>();
            for (ScanResult scanResult : wifiList) {
                int level = WifiManager.calculateSignalLevel(scanResult.level, MAX_LEVELS);
                result.append("[").append(level).append("%] ")
                        .append(scanResult.BSSID)
                        .append(" (").append(scanResult.SSID).append(")\n");

                // Nueva entrada en la map de este escaneo para esta localizacion
                pair.put(scanResult.BSSID, level);
            }
            generalScan.get(currentlocationName.getText().toString().toLowerCase()).add(pair);

            EditText et = findViewById(R.id.editTextTextMultiLine);
            et.setText(result.toString());
        }
    }

    protected void scanFailure1() {
        speak("Error al iniciar el scan. Demasiados intentos o validar permisos.");
    }

    protected void scanFailure2() {
        speak("Error en el scan. ");
    }

    /** Detalles sobre el numero de lecturas realizadas por ubicacion  */
    public void showTotalScans(View v) {
        StringBuffer response = new StringBuffer();

        if (generalScan.keySet().size()==0) {
            speak("No hay informacion recolectada");
            return;
        }

        EditText et = findViewById(R.id.editTextTextMultiLine);
        et.setText(generalScanInfo());

        for(String location : generalScan.keySet()) {
            response.append("En ubicacion " + location + " se realizaron " + generalScan.get(location).size() + " escaneos. ");
        }

        speak(response.toString());
    }

    /** Dispara la evaludacion de ubicacion */
    public void whereAmI(View v) {
        evaluatingWhereAmI = true;
        scanWifi();
    }

    /** Determina cual es la mejor opcion de ubicacion en funcion de los datos registrados y la lectura de señal actual */
    protected void estimateLocation(List<ScanResult> wifiList) {
        String minLoc = null;
        Integer minValue = Integer.MAX_VALUE;

        // Ubicaciones
        for (String location : generalScan.keySet()) {
            String curMinLoc = location;
            Integer curMinValue = 0;
            // Por cada entrada en la lista de intensidades, comparamos con las recolectadas previamente para la location que esta siendo evaluada
            for (ScanResult result : wifiList) {
                // Lecturas guardadas (puede haber varias)
                for (Map<String, Integer> values : generalScan.get(location)) {
                    // calcular el delta, considerando la posibildad que el scaneo actual haya encontrado un BSSID que no tenemos en el historico
                    curMinValue = curMinValue + Math.abs(result.level - (values.get(result.BSSID)==null ? 0 : values.get(result.BSSID)));
                }
            }
            // Tenemos un nuevo mínimo?
            if (curMinValue < minValue) {
                minValue = curMinValue;
                minLoc = curMinLoc;
            }
        }

        if (minLoc == null) {
            speak("No pude determinar la ubicacion actual");
            return;
        }

        speak("Deberias estar proximo a la ubicacion " + minLoc);

    }

    protected String generalScanInfo() {
        StringBuffer ret = new StringBuffer();
        // Por cada ubicacion
        for (String location : generalScan.keySet()) {
            ret.append("Ubicacion: ").append(location).append("\n");
            // Por cada escaneo
            for (int i=0; i<generalScan.get(location).size(); i++) {
                ret.append("  Scan nro: ").append(i).append(("\n"));
                // Por cada hotspot
                for (String aScan : generalScan.get(location).get(i).keySet()) {
                    ret.append("    ").append(aScan).append(": ").append(generalScan.get(location).get(i).get(aScan)).append("\n");
                }
            }
        }
        return ret.toString();
    }

    protected void speak(String text) {
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, ""+System.nanoTime());
    }

    public class TTSListener implements TextToSpeech.OnInitListener {
        @Override
        public void onInit(int status) {
        }
    }

}
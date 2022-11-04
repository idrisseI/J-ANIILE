package com.testconnexion.android.sdk.demo;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;
import com.opencsv.exceptions.CsvException;
import com.speedchecker.android.sdk.Public.SpeedTestListener;
import com.speedchecker.android.sdk.Public.SpeedTestResult;
import com.speedchecker.android.sdk.SpeedcheckerSDK;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements SpeedTestListener {
  private final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss.SSS", Locale.FRANCE);
  private TextView textViewStage;
  private static TextView textViewResult;
  private TextView textViewResultWLAN;
  private TextView textViewConsole;
  private static final String prefFile = "fullPosition";
  private final List<Integer> resultats = new ArrayList<>();
  private int compte = 0;
  private BroadcastReceiver ecouteRSSI;
  private GraphicalView monBeauGraph;
  private XYMultipleSeriesDataset multiplesDonnees;
  private XYMultipleSeriesRenderer renduMultiplesDonnees;
  private XYSeries donneesGraphWLAN;
  private XYSeries donneesGraphCell;
  private TelephonyManager telephonyManager;
  private MonEcouteurTelephonique ecoute;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    ToggleButton startSpeedTestBtn = findViewById(R.id.launch_speedTest);
    Button switchToMapActivity = findViewById(R.id.button_carte);

    textViewStage = findViewById(R.id.textView_stage);
    textViewResultWLAN = findViewById(R.id.textView_result_WLAN);
    textViewResult = findViewById(R.id.textView_result);
    textViewConsole = findViewById(R.id.textView_log);
    RadioGroup choixMesure = findViewById(R.id.choix_mesure);

    textViewResult.setMovementMethod(new ScrollingMovementMethod());
    textViewConsole.setMovementMethod(new ScrollingMovementMethod());


    SpeedcheckerSDK.init(this);
    SpeedcheckerSDK.askPermissions(this);
    SpeedcheckerSDK.SpeedTest.setOnSpeedTestListener(this);

    WifiManager wifiMan=(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    wifiMan.startScan();

    startSpeedTestBtn.setOnClickListener((View v) -> {
      Log.d(TAG, "onCreate: "+startSpeedTestBtn.isChecked());
      compte = 0;
      if (startSpeedTestBtn.isChecked()){   //démarre le test
        resultats.clear();
        textViewResultWLAN.setVisibility(View.VISIBLE);
        textViewConsole.setVisibility(View.GONE);
        SharedPreferences pref = getSharedPreferences(prefFile, Context.MODE_PRIVATE);
        if ("0".equals(pref.getString("position","0"))){    //pas de position enregistrée
          new MaterialAlertDialogBuilder(this)
                  .setMessage("Aucune coordonnée associée à votre mesure. Souhaitez vous localiser votre mesure?")
                  .setNeutralButton("Inutile", (dialogInterface, i) -> dialogInterface.dismiss())
                  .setPositiveButton("Oui", (dialogInterface, i) -> startActivity( new Intent(this, MapActivity.class) ))
                  .show();
        }
        if (choixMesure.getCheckedRadioButtonId()== R.id.choix_intensite) { //intensité
          if (monBeauGraph != null){  //remet à 0 les graphs
            LinearLayout ligneP = findViewById(R.id.ligne_principale);
            ligneP.removeView(monBeauGraph);
            monBeauGraph = null;
          }
          initGraph();  //initialise le nouveau graph
          monBeauGraph = ChartFactory.getCubeLineChartView(this,multiplesDonnees,renduMultiplesDonnees,0);
          monBeauGraph.setBackgroundColor(Color.rgb(50,50,50));
          monBeauGraph.setMinimumHeight(500);
          monBeauGraph.setMinimumWidth(200);
          LinearLayout ligneP = findViewById(R.id.ligne_principale);
          ligneP.addView(monBeauGraph);
          mesureCell();   //mesure intensité 4G
          mesureWLAN();   //mesure intensité WLAN
        }else {   //speedtestSDK, mesure download et upload
          startSpeedTestBtn.setChecked(false);
          textViewResultWLAN.setVisibility(View.GONE);
          textViewConsole.setVisibility(View.VISIBLE);
          if (monBeauGraph != null){
            LinearLayout ligneP = findViewById(R.id.ligne_principale);
            ligneP.removeView(monBeauGraph);
          }
          SpeedcheckerSDK.SpeedTest.startTest(this);
        }
      }
      else{ //arrête le test en cours
        SpeedcheckerSDK.SpeedTest.interruptTest();
        if (telephonyManager!= null) telephonyManager.listen(ecoute,PhoneStateListener.LISTEN_NONE);
        if (ecouteRSSI!= null)getApplicationContext().unregisterReceiver(ecouteRSSI);
        Log.d(TAG, "onCreate: ok"+ecouteRSSI.isOrderedBroadcast());
        String valCell = "--:--";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {   //récupère les résultats
           valCell = String.valueOf(calculeMoyenne(ecoute.getResultats()));
        }
        Log.d(TAG, "onCreate: "+"fghj"+ calculeMoyenne(resultats));
        writeToCSVFile("0","0", "0",String.valueOf(calculeMoyenne(resultats)),valCell); //écrit les résultats test Cell et WLAN
      }
    });
    switchToMapActivity.setOnClickListener((View v) -> startActivity( new Intent(this, MapActivity.class) ));

    //demande l'enregistrement d'un nom d'utilisateur
    SharedPreferences pref = getSharedPreferences(prefFile, Context.MODE_PRIVATE);
    if(Objects.equals(pref.getString("user", "inconnu_au_bataillon"), "inconnu_au_bataillon")){
      AlertDialog.Builder constructeur = new AlertDialog.Builder(this);
      constructeur.setTitle(R.string.welcome);
      constructeur.setMessage(R.string.choix_pseudo);
      EditText pseudo = new EditText(this);
      pseudo.setHint(R.string.pseudo);
      constructeur.setCancelable(false);
      constructeur.setView(pseudo);
      constructeur.setPositiveButton("Valider", (dialog, which) -> pref.edit().putString("user",pseudo.getText().toString()).apply());
      constructeur.show();
    }
  }

  private void mesureWLAN() {   //ecouteur changement intensité (RSSI) du WLAN
    compte = 0;
    ecouteRSSI = new BroadcastReceiver(){
      @Override
      public void onReceive(Context arg0, Intent arg1) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.startScan();
        int newRssi = wifiManager.getConnectionInfo().getRssi();
        compte++;
        resultats.add(newRssi);
        donneesGraphWLAN.add(compte,newRssi);
        renduMultiplesDonnees.setXAxisMax(Math.max(compte + 1,10));
        monBeauGraph.repaint();
        textViewResultWLAN.setText("RSSI WLAN : "+ calculeMoyenne(resultats) +" dBm ("+resultats.size()+" valeurs)");
      }};
    IntentFilter rssiFilter = new IntentFilter(WifiManager.RSSI_CHANGED_ACTION);
    getApplicationContext().registerReceiver(ecouteRSSI, rssiFilter);
  }

  public void mesureCell() {  //ecouteur changement intensité (RSSI) du réseau cellulaire
    telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    ecoute = null;
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
      ecoute = new MonEcouteurTelephonique(donneesGraphCell, monBeauGraph, renduMultiplesDonnees);
      telephonyManager.listen(ecoute, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    switch (item.getItemId()) {
      case R.id.infos:
        voirInfos();
        break;
      case R.id.Aide:
        startActivity( new Intent(this, HelpActivity.class));
        break;
      case R.id.contact:
        startActivity( new Intent(this, ContactActivity.class));
        break;
      case R.id.détruire:
        onExplose();
        break;
      case R.id.nous_soutenir:
        soutient();
        break;
      case R.id.code_bonus:
        onBonusClick();
        break;
      case R.id.reinit:
        getSharedPreferences(prefFile, Context.MODE_PRIVATE).edit().clear().apply();
        finish();
      default:
        return true;
    }
    return false;
  }

  private void writeToCSVFile(String ping, String download, String upload, String WLAN, String Cell) {
    SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy,HH:mm", Locale.FRANCE);

    String defaultPosition = "43.70952055631749,5.506375053917219";
    String defaultRoom = "VDD";
    String defaultFloor = "Rdc";

    SharedPreferences sharedPref = getSharedPreferences(prefFile, Context.MODE_PRIVATE);

    String position = sharedPref.getString("position", defaultPosition);
    String room = sharedPref.getString("room", defaultRoom);
    String floor = sharedPref.getString("floor", defaultFloor);
    String user = sharedPref.getString("user", "inconnu au bataillon");
    sharedPref.edit().remove("position").apply();
    String[] data = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", dateFormatter.format(new Date()), ping,
        download, upload,WLAN,Cell, position, room, floor, user).split(",");

    String csv = (getExternalFilesDir(null) + "/WiFiTestResult.csv");
    CSVParser parser = new CSVParserBuilder().withSeparator(';').build();
    List<String[]> dataList = new ArrayList<>();

    try (Reader br = Files.newBufferedReader(Paths.get(csv)) ; CSVReader reader =
        new CSVReaderBuilder(br).withCSVParser(parser).build()) {

      List<String[]> rows = reader.readAll();
      dataList.addAll(rows);

    } catch (IOException | CsvException e) {
      e.printStackTrace();
    }

    if (dataList.isEmpty()) {
      dataList.add(new String[]{"date", "hour", "ping", "download", "upload","WLAN","Cell", "latitude", "longitude", "room",
          "floor","user"});   //en-tête
    }

    try (CSVWriter writer = new CSVWriter(new FileWriter(csv), ';', ICSVWriter.NO_QUOTE_CHARACTER,
        ICSVWriter.DEFAULT_ESCAPE_CHARACTER, ICSVWriter.RFC4180_LINE_END)) {

      dataList.add(data);
      writer.writeAll(dataList);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void logToConsole(String message) {
    textViewConsole.setText(String.format("%s : %s %n", timeFormatter.format(System.currentTimeMillis()),
        message));

  }

  @Override
  public void onTestStarted() {
    textViewStage.setText(R.string.debut_test);
    textViewResult.setText('-');


    logToConsole("Test Started");
  }

  @Override
  public void onFetchServerFailed() {
    textViewStage.setText(R.string.lien_serveur);
    textViewResult.setText('-');
    logToConsole("Fetch Server Failed");

  }

  @Override
  public void onFindingBestServerStarted() {
    textViewStage.setText(R.string.recherche_serv);
    textViewResult.setText("...");
    logToConsole("Finding best server");

  }

  @Override
  public void onTestFinished(@NonNull SpeedTestResult speedTestResult) {
    textViewStage.setText(R.string.fin);
    textViewResult.setText(String.format("Ping: %s ms  Download speed: %s Mb/s  Upload speed: %s Mb/s  " +
        "Connection type: %s", speedTestResult.getPing(),
        speedTestResult.getDownloadSpeed(), speedTestResult.getUploadSpeed(), speedTestResult.getConnectionTypeHuman()));

    logToConsole(String.format("Test Finished: Server[%s] => %s", speedTestResult.getServerInfo(),
        speedTestResult));

    writeToCSVFile(speedTestResult.getPing().toString(), speedTestResult.getDownloadSpeed().toString(),
        speedTestResult.getUploadSpeed().toString(),"--:--","--:--");
      @SuppressLint("UseSwitchCompatOrMaterialCode") Switch boucle = findViewById(R.id.Boucle);
    if (boucle.isChecked()){
      SpeedcheckerSDK.SpeedTest.startTest(this);
    }
  }

  @Override
  public void onPingStarted() {
    textViewStage.setText(R.string.debut_ping);
    textViewResult.setText("...");
    logToConsole("Ping Started");

  }

  @Override
  public void onPingFinished(int ping, int jitter) {
    textViewStage.setText(R.string.fin_ping);
    textViewResult.setText(String.format("%s ms | jitter: %s", ping, jitter));
    logToConsole(String.format("Ping Finished: %s ms | jitter: %s", ping, jitter));

  }

  @Override
  public void onDownloadTestStarted() {
    textViewStage.setText(R.string.debut_dl);
    textViewResult.setText("...");
    logToConsole("Download Test Started");

  }

  @Override
  public void onDownloadTestProgress(int i, double bandwidth, double dataConsumed) {
    textViewStage.setText(R.string.en_cours_dl);
    textViewResult.setText(String.format("%s%c => %s Mb/s %n Transferred: %s Mb", i, '%',bandwidth, dataConsumed));
    logToConsole(String.format("Download Test Progress: %s%c => %s Mb/s %n Transferred: %s Mb", i, '%', bandwidth,
        dataConsumed));

  }

  @Override
  public void onDownloadTestFinished(double v) {
    textViewStage.setText(R.string.fin_dl);
    textViewResult.setText(String.format("%s Mb/s", v));
    logToConsole(String.format("Download Test Finished: %s Mb/s", v));

  }

  @Override
  public void onUploadTestStarted() {
    textViewStage.setText(R.string.debut_ul);
    textViewResult.setText("...");
    logToConsole("Upload Test Started");

  }

  @Override
  public void onUploadTestProgress(int i, double bandwidth, double dataConsumed) {
    textViewStage.setText(R.string.en_cours_ul);
    textViewResult.setText(String.format("%s%c => %s Mb/s %n Transferred: %s Mb", i, '%',bandwidth, dataConsumed));
    logToConsole(String.format("Upload Test Progress: %s%c => %s Mb/s %n Transferred: %s Mb", i, '%', bandwidth,
        dataConsumed));

  }

  @Override
  public void onUploadTestFinished(double v) {
    textViewStage.setText(R.string.fin_ul);
    textViewResult.setText(String.format("%s Mb/s", v));
    logToConsole(String.format("Upload Test Finished: %s Mb/s", v));

  }

  @Override
  public void onTestWarning(String s) {
    textViewStage.setText(R.string.warn);
    textViewResult.setText(s);
    logToConsole(String.format("The Test Received Warning: %s", s));

  }

  @Override
  protected void onPause() {
    super.onPause();
    if (telephonyManager!= null){
      telephonyManager.listen(ecoute,PhoneStateListener.LISTEN_NONE);
    }
    try {
      if (ecouteRSSI!= null)getApplicationContext().unregisterReceiver(ecouteRSSI);
    }catch (Exception e){
      Log.e(TAG, "onPause: ",e );
    }
  }

  @Override
  public void onTestFatalError(String s) {
    textViewStage.setText(R.string.fatal_error);
    textViewResult.setText(s);
    logToConsole(String.format("The Test Has Encountered A Fatal Error: %s", s));

  }

  @Override
  public void onTestInterrupted(String s) {
    textViewStage.setText(R.string.interruption);
    textViewResult.setText(s);
    logToConsole(String.format("The Test Has Been Interrupted: %s", s));

  }

  public void voirInfos (){
    AlertDialog.Builder constructeur = new AlertDialog.Builder(this);
    constructeur.setTitle("Informations");
    constructeur.setMessage(String.format("J'ANIILE %nversion %s, %nDéveloppée par Noah BAELDE, Idrisse IBRAHIM, Louis PACCOUD et Arthur SIEGEL %nUtilise la technologie PYFOMETRIX",getString(R.string.version)));
    constructeur.setNeutralButton("OK", (dialog, which) -> dialog.dismiss());
    constructeur.show();
  }

  public void soutient (){
    AlertDialog.Builder constructeur = new AlertDialog.Builder(this);
    constructeur.setTitle (":)");
    constructeur.setMessage("Merci pour votre soutient. Toute l'équipe de développement vous souhaite une agréable journée");
    constructeur.show();
  }

  public void onExplose(){
    this.finishAffinity();
  }

  public void onBonusClick(){
    AlertDialog.Builder constructeur = new AlertDialog.Builder(this);
    constructeur.setTitle(getString(R.string.bonus_code));
    EditText code = new EditText(this);
    code.setHint(getString(R.string.bonus_txt));
    constructeur.setView(code);
    constructeur.setPositiveButton(getString(R.string.valider), (dialog, which) -> {
      if (code.getText().toString().equals("DEV")){
        SharedPreferences pref = getSharedPreferences(prefFile, Context.MODE_PRIVATE);
        pref.edit().putBoolean("dev",true).apply();
        Toast.makeText(MainActivity.this, getString(R.string.unlock_dev),Toast.LENGTH_SHORT).show();
      }else if (code.getText().toString().equals("VDD")){
        SharedPreferences pref = getSharedPreferences(prefFile, Context.MODE_PRIVATE);
        pref.edit().putBoolean("VDD",true).apply();
        Toast.makeText(MainActivity.this, getString(R.string.unlock_vdd),Toast.LENGTH_SHORT).show();
      }
      dialog.dismiss();
    });
    constructeur.show();
  }

  private static int calculeMoyenne(List<Integer> marks) {
    return (int)marks.stream()
            .mapToDouble(d -> d)
            .average()
            .orElse(0.0);
  }

  private void initGraph(){
    Log.d(TAG, "initGraph: io");
    multiplesDonnees = new XYMultipleSeriesDataset();
    renduMultiplesDonnees = new XYMultipleSeriesRenderer();
    donneesGraphWLAN = new XYSeries("RSSI WLAN(dBm)");
    donneesGraphCell = new XYSeries("RSSI Cell(dBm)");
    multiplesDonnees.addSeries(donneesGraphWLAN);
    multiplesDonnees.addSeries(donneesGraphCell);
    XYSeriesRenderer renduDonneesGraphWLAN = new XYSeriesRenderer();
    renduDonneesGraphWLAN.setLineWidth(5);
    renduDonneesGraphWLAN.setPointStyle(PointStyle.DIAMOND);
    XYSeriesRenderer renduDonneesGraphCell = new XYSeriesRenderer();
    renduDonneesGraphCell.setLineWidth(5);
    renduDonneesGraphCell.setPointStyle(PointStyle.DIAMOND);
    renduDonneesGraphCell.setColor(Color.rgb(0,200,0));
    //renduDonneesGraph.setGradientEnabled(true);   //ça marche pô
    //renduDonneesGraph.setGradientStart(0, Color.rgb(255,0,0));
    //renduDonneesGraph.setGradientStop(-100, Color.rgb(0,255,255));
    //renduDonneesGraph.addFillOutsideLine(new XYSeriesRenderer.FillOutsideLine(XYSeriesRenderer.FillOutsideLine.Type.BOUNDS_ALL));
    renduMultiplesDonnees.addSeriesRenderer(renduDonneesGraphWLAN);
    renduMultiplesDonnees.addSeriesRenderer(renduDonneesGraphCell);
    renduMultiplesDonnees.setChartTitle("J'ANIILE, et votre réseau devient plus beau");
    renduMultiplesDonnees.setXAxisMin(1);
    renduMultiplesDonnees.setZoomEnabled(true,false);
    renduMultiplesDonnees.setBackgroundColor(Color.rgb(200,220,200));
    renduMultiplesDonnees.setYAxisMin(-150);
    renduMultiplesDonnees.setYAxisMax(0);
    renduMultiplesDonnees.setYLabelsAlign(Paint.Align.RIGHT);
    renduMultiplesDonnees.setBarSpacing(0.5);
    renduMultiplesDonnees.setXTitle("Mesures");
    renduMultiplesDonnees.setLabelsTextSize(15);
    renduMultiplesDonnees.setAxisTitleTextSize(20);
    renduMultiplesDonnees.setYTitle("RSSI");
    renduMultiplesDonnees.setShowGrid(true);
    renduMultiplesDonnees.setGridColor(Color.GRAY);
    renduMultiplesDonnees.setXLabels(0); // sets the number of integer labels to appear
  }

  @RequiresApi(api = Build.VERSION_CODES.Q)
  static class MonEcouteurTelephonique extends PhoneStateListener {
    private int compteur = 0;
    private final List<Integer> resultatsTests = new ArrayList<>();
    private final GraphicalView monBeauGraph;
    private final XYSeries donneesGraph;
    private XYMultipleSeriesRenderer renduMultiplesDonnees = new XYMultipleSeriesRenderer();

    public MonEcouteurTelephonique(XYSeries donneesGraph, GraphicalView monBeauGraph, XYMultipleSeriesRenderer renduMultiplesDonnees){
        this.monBeauGraph = monBeauGraph;
        this.donneesGraph = donneesGraph;
        this.renduMultiplesDonnees = renduMultiplesDonnees;
    }

    @Override
    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
      super.onSignalStrengthsChanged(signalStrength);
      int sign  = signalStrength.getCellSignalStrengths().get(0).getDbm();
      compteur++;
      resultatsTests.add(sign);
      donneesGraph.add(compteur, sign);
      renduMultiplesDonnees.setXAxisMax(Math.max(compteur + 1,10));
      monBeauGraph.repaint();
      textViewResult.setText("RSSI Cellulaire : "+ calculeMoyenne(resultatsTests) +" dBm ("+resultatsTests.size()+" valeurs)");
    }

    private List<Integer> getResultats(){
      return resultatsTests;
    }
  }


  //finir l'implémentation pour les appareils d'API >= 33
  @RequiresApi(api = Build.VERSION_CODES.S)
  static class  RetourCell extends TelephonyCallback implements TelephonyCallback.SignalStrengthsListener{
    private int compteur = 0;
    private final List<Integer> resultatsTests = new ArrayList<>();
    private final GraphicalView monBeauGraph;
    private final XYSeries donneesGraph;
    private XYMultipleSeriesRenderer renduMultiplesDonnees = new XYMultipleSeriesRenderer();

    public RetourCell(XYSeries donneesGraph, GraphicalView monBeauGraph, XYMultipleSeriesRenderer renduMultiplesDonnees) {
      this.monBeauGraph = monBeauGraph;
      this.donneesGraph = donneesGraph;
      this.renduMultiplesDonnees = renduMultiplesDonnees;
    }

    @Override
    public void onSignalStrengthsChanged(@NonNull SignalStrength signalStrength) {
      int x  = signalStrength.getCellSignalStrengths().get(0).getDbm();
      x = (2 * x) - 113; // -> dBm
      x *= -1;
      compteur++;
      resultatsTests.add(x);
      donneesGraph.add(compteur,signalStrength.getCellSignalStrengths().get(0).getDbm());
      renduMultiplesDonnees.setXAxisMax(Math.max(Math.max(compteur + 1,10),renduMultiplesDonnees.getXAxisMax()));
      monBeauGraph.repaint();
      textViewResult.setText("RSSI Cellulaire : "+ calculeMoyenne(resultatsTests) + " "+ signalStrength.getCellSignalStrengths().get(0).getDbm()+"  "+signalStrength.getCdmaDbm()+" dBm ("+resultatsTests.size()+" valeurs)");
    }

    private List<Integer> getResultats(){
      return resultatsTests;
    }
  }
}

package com.testconnexion.android.sdk.demo;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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
  private TextView textViewResult;
  private TextView textViewConsole;
  private static final String prefFile = "fullPosition";
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Button startSpeedTestBtn = findViewById(R.id.launch_speedTest);
    Button switchToMapActivity = findViewById(R.id.button_carte);

    textViewStage = findViewById(R.id.textView_stage);
    textViewResult = findViewById(R.id.textView_result);
    textViewConsole = findViewById(R.id.textView_log);

    textViewResult.setMovementMethod(new ScrollingMovementMethod());
    textViewConsole.setMovementMethod(new ScrollingMovementMethod());


    SpeedcheckerSDK.init(this);
    SpeedcheckerSDK.askPermissions(this);
    SpeedcheckerSDK.SpeedTest.setOnSpeedTestListener(this);

    startSpeedTestBtn.setOnClickListener((View v) -> SpeedcheckerSDK.SpeedTest.startTest(this));
    switchToMapActivity.setOnClickListener((View v) -> startActivity( new Intent(this, MapActivity.class) ));

    SharedPreferences pref = getSharedPreferences(prefFile, Context.MODE_PRIVATE);
    if(Objects.equals(pref.getString("user", "inconnu_au_bataillon"), "inconnu_au_bataillon")){
      AlertDialog.Builder constructeur = new AlertDialog.Builder(this);
      constructeur.setTitle(R.string.welcome);
      constructeur.setMessage(R.string.choix_pseudo);
      EditText pseudo = new EditText(this);
      pseudo.setHint(R.string.pseudo);
      constructeur.setCancelable(false);
      constructeur.setView(pseudo);
      constructeur.setPositiveButton("Valider", (dialog, which) -> {
        pref.edit().putString("user",pseudo.getText().toString()).apply();
      });
      constructeur.show();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // ajouter mes items de menu
    getMenuInflater().inflate(R.menu.menu, menu);
    Log.d(TAG, "onCreateOptionsMenu: "+"ouiiiii");
    // ajouter les items du système s'il y en a
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
        //onBonusClick();
        versJeu();
        break;
      default:break;
    }
    return true;
  }

  private void versJeu() {
    startActivity( new Intent(this, GameActivity.class));
  }

  // TODO: use string values
  @RequiresApi(api = Build.VERSION_CODES.O)
  private void writeToCSVFile(String ping, String download, String upload) {
    SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy,HH:mm", Locale.FRANCE);

    String defaultPosition = "43.70952055631749,5.506375053917219";
    String defaultRoom = "VDD";
    String defaultFloor = "Rdc";

    SharedPreferences sharedPref = getSharedPreferences(prefFile, Context.MODE_PRIVATE);

    String position = sharedPref.getString("position", defaultPosition);
    String room = sharedPref.getString("room", defaultRoom);
    String floor = sharedPref.getString("floor", defaultFloor);
    String user = sharedPref.getString("user", "inconnu au bataillon");

    String[] data = String.format("%s,%s,%s,%s,%s,%s,%s,%s", dateFormatter.format(new Date()), ping,
        download, upload, position, room, floor, user).split(",");

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
      dataList.add(new String[]{"date", "hour", "ping", "download", "upload", "latitude", "longitude", "room",
          "floor","user"});

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

  @RequiresApi(api = Build.VERSION_CODES.O)
  @Override
  public void onTestFinished(@NonNull SpeedTestResult speedTestResult) {
    textViewStage.setText(R.string.fin);
    textViewResult.setText(String.format("Ping: %s ms  Download speed: %s Mb/s  Upload speed: %s Mb/s  " +
        "Connection type: %s", speedTestResult.getPing(),
        speedTestResult.getDownloadSpeed(), speedTestResult.getUploadSpeed(), speedTestResult.getConnectionTypeHuman()));

    logToConsole(String.format("Test Finished: Server[%s] => %s", speedTestResult.getServerInfo(),
        speedTestResult));

    writeToCSVFile(speedTestResult.getPing().toString(), speedTestResult.getDownloadSpeed().toString(),
        speedTestResult.getUploadSpeed().toString());
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

}

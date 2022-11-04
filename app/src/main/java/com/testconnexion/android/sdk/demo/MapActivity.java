package com.testconnexion.android.sdk.demo;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.GroundOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;

public class MapActivity extends AppCompatActivity {
  private MapView map;
  private GroundOverlay vddGroundOverlay;
  private Marker pos;
  private List<Marker> marqueurs = new ArrayList<>();
  private Hashtable<String, List<Integer>> normeDown = new Hashtable<>();
  private Hashtable<String, List<Integer>> normeUp = new Hashtable<>();
  private Hashtable<String, List<Integer>> normeRSSI = new Hashtable<>();
  private String prefFile = "fullPosition";

  @RequiresApi(api = Build.VERSION_CODES.O)
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Context context = getApplicationContext();
    Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));
    GeoPoint startPoint = new GeoPoint(43.70952055631749, 5.506375053917219);

    setContentView(R.layout.activity_carte);

    map = findViewById(R.id.map);
    map.setTileSource(TileSourceFactory.MAPNIK);
    map.setMultiTouchControls(true);
    map.getOverlays().add(new LongPressMapOverlay());

    IMapController mapController = map.getController();
    mapController.setZoom(19F);
    Bitmap vddMapBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.vdd_rdc);

    SharedPreferences pref = getSharedPreferences(prefFile, Context.MODE_PRIVATE);
    if (pref.getBoolean("VDD",false) || pref.getBoolean("dev",false)) {
      mapController.setCenter(startPoint);
      vddGroundOverlay = new GroundOverlay();
      vddGroundOverlay.setImage(vddMapBitmap);
      vddGroundOverlay.setPosition(
              new GeoPoint(43.7104276822253, 5.506906547853047),
              new GeoPoint(43.709497943396585, 5.507623827467327),
              new GeoPoint(43.708850178246514, 5.5059752790959315),
              new GeoPoint(43.70976654379882, 5.50531868006483));
      map.getOverlays().add(vddGroundOverlay);
    }else{
      mapController.setCenter(new GeoPoint(48.8701, 2.31658));
      mapController.setZoom(11F);
      findViewById(R.id.etage).setVisibility(View.GONE);
    }

    pos = new Marker(map);
    pos.setPosition(startPoint);
    pos.setVisible(false);
    map.getOverlayManager().add(pos);

    map.invalidate();

    Button showResultBtn = findViewById(R.id.show_result);
    showResultBtn.setOnClickListener((View v) -> paramResult());

    Button versTest = findViewById(R.id.bouton);
    versTest.setOnClickListener((View v)->goToTest());

    Button ajouterPos = findViewById(R.id.button);
    ajouterPos.setOnClickListener((View v)->ajouterPosition());



    normeDown.put("Noah",List.of(8,33,61,90));
    normeDown.put("Arthur",List.of(8,32,60,88));
    normeDown.put("Louis",List.of(8,34,63,92));
    normeDown.put("Idrisse",List.of(7,26,49,72));
    normeDown.put("Ewan",List.of(10,40,75,110));
    normeDown.put(getSharedPreferences(prefFile, Context.MODE_PRIVATE).getString("user","inconnu au bataillon"),List.of(10,40,75,110));


    normeUp.put("Noah",List.of(7,28,52,77));
    normeUp.put("Arthur",List.of(9,36,69,101));
    normeUp.put("Louis",List.of(7,29,55,81));
    normeUp.put("Idrisse",List.of(7,27,51,75));
    normeUp.put("Ewan",List.of(10,40,75,110));
    normeUp.put(getSharedPreferences(prefFile, Context.MODE_PRIVATE).getString("user","inconnu au bataillon"),List.of(10,40,75,110));

    normeRSSI.put("WLAN",List.of(-78,-66,-54,-42));
    normeRSSI.put("Cell",List.of(-118,-96,-74,-52));
    // calling the action bar
    ActionBar actionBar = getSupportActionBar();
    // showing the back button in action bar
    assert actionBar != null;
    actionBar.setDisplayHomeAsUpEnabled(true);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // ajouter mes items de menu
    getMenuInflater().inflate(R.menu.menu, menu);
    // ajouter les items du système s'il y en a
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        return true;
      case R.id.infos:
        voirInfos();
        return true;
      case R.id.Aide:
        startActivity( new Intent(this, HelpActivity.class));
        return true;
      case R.id.contact:
        startActivity( new Intent(this, ContactActivity.class));
        return true;
      case R.id.détruire:
        onExplose();
        return true;
      case R.id.nous_soutenir:
        soutient();
        return true;
      case R.id.code_bonus:
        onBonusClick();
        return true;
      case R.id.reinit:
        getSharedPreferences(prefFile, Context.MODE_PRIVATE).edit().clear().apply();
        finish();
        return true;
      default:break;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onPause() {
    super.onPause();
    map.onPause();

  }

  @Override
  public void onResume() {
    super.onResume();
    map.onResume();

  }

  public void ajouterPosition() {    // ajoute la position au fichier de préférence
    Button floor = findViewById(R.id.etage);

    SharedPreferences sharedPref = getSharedPreferences(prefFile, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = sharedPref.edit();

    AlertDialog.Builder constructeur = new AlertDialog.Builder(MapActivity.this);
    constructeur.setTitle("Nom position");

    EditText nomPosition =  new EditText(MapActivity.this);
    nomPosition.setText(sharedPref.getString("room",null));

    constructeur.setView(nomPosition);
    constructeur.setNeutralButton("Valider", (dialog, which) -> {
      if (!nomPosition.getText().toString().equals("")) {
        editor.putString("position", String.format("%s,%s", pos.getPosition().getLatitude(),
                pos.getPosition().getLongitude()));
        editor.putString("room", nomPosition.getText().toString());
        editor.putString("floor", floor.getText().toString());

        editor.apply();
        goToTest();
      }
    });

    constructeur.show();
  }

  public void goToTest() {
    Intent intent = new Intent(this,MainActivity.class);
    startActivity(intent);

  }

    // A OPTIMISER
  public void etage(View v) {
    ToggleButton bouton = findViewById(R.id.etage);
    map = findViewById(R.id.map);
    Bitmap vddMapBitmap;

    SharedPreferences pref = getSharedPreferences(prefFile, Context.MODE_PRIVATE);
    if (!(pref.getBoolean("VDD",false) || pref.getBoolean("dev",false))){
      AlertDialog.Builder constructeur = new AlertDialog.Builder(this);
      constructeur.setTitle("Impossible action");
      constructeur.setMessage("Action unavailible, come back to v1.4");
      constructeur.show();
    }else {
      if (bouton.isChecked()) {
        map.getOverlayManager().remove(vddGroundOverlay); // A AMELIORER
        vddMapBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.vdd_n1);
        bouton.setText(getString(R.string.etage1));

      } else {
        map.getOverlayManager().remove(vddGroundOverlay); // A AMELIORER
        vddMapBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.vdd_rdc);
        bouton.setText(getString(R.string.etage0));

      }


      vddGroundOverlay = new GroundOverlay();
      vddGroundOverlay.setImage(vddMapBitmap);
      vddGroundOverlay.setPosition(
              new GeoPoint(43.7104276822253, 5.506906547853047),
              new GeoPoint(43.709497943396585, 5.507623827467327),
              new GeoPoint(43.708850178246514, 5.5059752790959315),
              new GeoPoint(43.70976654379882, 5.50531868006483));
      map.getOverlays().add(vddGroundOverlay);
      map.getOverlayManager().remove(pos);
      map.getOverlayManager().add(pos);
    }

    if (marqueurs != null) {
      for (Marker element : marqueurs) {
        element.setVisible(false);
        map.getOverlayManager().remove(element);
        map.invalidate();
      }
    }

    map.invalidate();

  }

  public void showResult(int choixDonnee, Boolean reference, Boolean Jour, Boolean myTest, Boolean devTest) {
    Button button = findViewById(R.id.etage);
    List<String[]> dataList = new ArrayList<>();
    String csvSplitBy = ",";
    BufferedReader reader = null;
    int indice_donnee;

    switch (choixDonnee){
      case R.id.intensite_WLAN:
        indice_donnee = 5;
        break;
      case R.id.debit_down:
        indice_donnee = 3;
        break;
      case R.id.debit_up:
        indice_donnee = 4;
        break;
      default:
        indice_donnee = 6;
    }

    if (Boolean.TRUE.equals(devTest)) {
      try {
        reader = new BufferedReader(
                new InputStreamReader(getAssets().open("ressources/donnees.csv"), StandardCharsets.UTF_8));
        String mLine;
        reader.readLine(); //supprime l'en-tête
        while ((mLine = reader.readLine()) != null) {
          String[] row = mLine.split(csvSplitBy);
          if (!Objects.equals(row[indice_donnee], "--:--") && !Objects.equals(row[indice_donnee], "0")) dataList.add(row);
        }

      } catch (IOException e) {
        e.printStackTrace();

      } finally {
        if (reader != null) {
          try {
            reader.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }
    if (Boolean.TRUE.equals(myTest)) {
      String csv = (getExternalFilesDir(null) + "/WiFiTestResult.csv");
      CSVParser parser = new CSVParserBuilder().withSeparator(';').build();

      try (Reader br = Files.newBufferedReader(Paths.get(csv)); CSVReader reader2 =
              new CSVReaderBuilder(br).withCSVParser(parser).build()) {

        String[] ligne;
        reader2.readNext(); //supprime l'en-t^te
        while((ligne = reader2.readNext())!=null){
          if (!Objects.equals(ligne[indice_donnee], "--:--") && !Objects.equals(ligne[indice_donnee], "0")) dataList.add(ligne);
        }

      } catch (IOException | CsvException e) {
        e.printStackTrace();
      }
    }
    if (marqueurs != null){
      for(Marker element:marqueurs) {
        element.setVisible(false);
        map.getOverlayManager().remove(element);
        map.invalidate();
      }
    }
    if (!dataList.isEmpty()) {
      Log.d(TAG, "showResult: "+dataList.size());
      for (String[] row : dataList) {
        String pattern = "HH:mm";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        Date heureLimite = null;
        try {
          heureLimite = sdf.parse("08:05");
        } catch (ParseException e) {
          e.printStackTrace();
        }
        Date heureTest = null;
        if (button.getText().toString().equals(row[10])){
          try {
            heureTest = sdf.parse(row[1]);
          } catch (ParseException e) {
            e.printStackTrace();
          }
          Log.d(TAG, "showResult: "+reference+Objects.requireNonNull(heureTest).before(heureLimite)+Jour+Objects.requireNonNull(heureTest).after(heureLimite));
          if (((reference && Objects.requireNonNull(heureTest).before(heureLimite)) || (Jour && Objects.requireNonNull(heureTest).after(heureLimite)))) {
            Log.d(TAG, "showResult: "+ Arrays.toString(row));
            Marker marker = new Marker(map);
            this.marqueurs.add(marker);

            marker.setPosition(new GeoPoint(Double.parseDouble(row[7]), Double.parseDouble(row[8])));
            Log.d(TAG, "showResult: "+ Arrays.toString(row));
            marker.setSnippet(String.format("%s | %s", row[indice_donnee], row[9]));

            switch (choixDonnee){
              case R.id.intensite_WLAN:
                marker.setIcon(quality(Double.parseDouble(row[5]), Objects.requireNonNull(normeRSSI.get("WLAN"))));
                break;
              case R.id.debit_down:
                marker.setIcon(quality(Double.parseDouble(row[3]), Objects.requireNonNull(normeDown.get(row[11]))));
                break;
              case R.id.debit_up:
                marker.setIcon(quality(Double.parseDouble(row[4]), Objects.requireNonNull(normeUp.get(row[11]))));
                break;
              default:
                marker.setIcon(quality(Double.parseDouble(row[6]), Objects.requireNonNull(normeRSSI.get("Cell"))));
            }

            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            map.getOverlayManager().add(marker);
            map.invalidate();
          }
        }
      }
    }
  }

  public Drawable quality (Double debit, List<Integer> valeurs){
    if (debit< valeurs.get(0)){
      
      return (getDrawable(R.drawable.red)); // très mauvais
    }
    else if (debit < valeurs.get(1)){
      return (getDrawable(R.drawable.orange)); // mauvais
    }
    else if (debit< valeurs.get(2)){
      return (getDrawable(R.drawable.yellow)); // moyen
    }
    else if (debit < valeurs.get(3)){
      return (getDrawable(R.drawable.light_green)); // bon
    }
    else{
      return(getDrawable(R.drawable.deep_green)); //excellent
    }
  }

  public void paramResult(){
    AlertDialog.Builder constructeur = new AlertDialog.Builder(this);
    constructeur.setTitle("Paramètres");
    LayoutInflater inflater = this.getLayoutInflater();
    View v = inflater.inflate(R.layout.parametres, null);
    CheckBox testReference = v.findViewById(R.id.test_reference);
    CheckBox testJour = v.findViewById(R.id.test_jour);
    CheckBox myTest = v.findViewById(R.id.mes_tests);
    CheckBox devTest = v.findViewById(R.id.dev_tests);
    RadioGroup choixDonnees = v.findViewById(R.id.choix_donnees);
    devTest.setOnCheckedChangeListener((buttonView, isChecked) -> {
      if (devTest.isChecked()){
        testReference.setVisibility(View.VISIBLE);
        testJour.setVisibility(View.VISIBLE);
      }else{
        testReference.setVisibility(View.GONE);
        testJour.setVisibility(View.GONE);
        testJour.setChecked(true);
        testReference.setChecked(true);
      }
    });
    constructeur.setView(v);
    constructeur.setNeutralButton("Afficher", (dialog, which) -> {
      if ((myTest.isChecked()||devTest.isChecked())&&(testReference.isChecked()||testJour.isChecked())) {
        showResult(choixDonnees.getCheckedRadioButtonId(), testReference.isChecked(), testJour.isChecked(), myTest.isChecked(), devTest.isChecked());
        dialog.dismiss();
      }else{
        Toast.makeText(this, getString(R.string.error_parametres),Toast.LENGTH_LONG).show();
      }
    });
    constructeur.show();
  }



  //place un marqueur
  public class LongPressMapOverlay extends Overlay {
    @Override
    public void draw(Canvas c, MapView m, boolean shadow) {
    }

    @Override
    public boolean onLongPress(MotionEvent event, MapView map) {
      if (event.getAction() == MotionEvent.ACTION_DOWN) {
        Projection projection = map.getProjection();
        GeoPoint position = (GeoPoint) projection.fromPixels( (int) event.getX(), (int) event.getY() );

        pos.setPosition(position);
        pos.setSnippet("Votre position");
        pos.setVisible(true);

        map.invalidate();
      }

      return true;
    }
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
    constructeur.setMessage("Que vous êtes bon!");
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
        Toast.makeText(MapActivity.this, getString(R.string.unlock_dev),Toast.LENGTH_SHORT).show();
      }else if (code.getText().toString().equals("VDD")){
        SharedPreferences pref = getSharedPreferences(prefFile, Context.MODE_PRIVATE);
        pref.edit().putBoolean("VDD",true).apply();
        Toast.makeText(MapActivity.this, getString(R.string.unlock_vdd),Toast.LENGTH_SHORT).show();
      }
      dialog.dismiss();
    });
    constructeur.show();
  }
  
}

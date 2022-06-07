package com.testconnexion.android.sdk.demo;

import static android.content.ContentValues.TAG;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class GameActivity extends Activity {
    private List<String[]> planc = new ArrayList<>();
    private List<String[]> plancO = new ArrayList<>();
    private List<String[]> plancR;
    private final Integer[] depart = {200,200};
    private DrawView drawView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_game);
        /*setContentView(new GameView(this));
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        */

        lecteur();
        Log.d(TAG, "onCreate:0 "+System.currentTimeMillis());
        majCarte(depart,510);
        Log.d(TAG, "onCreate:1 "+System.currentTimeMillis());
        majCarte(new Integer[]{300, 300},280);
        Log.d(TAG, "onCreate:2 "+System.currentTimeMillis());
        drawView = new DrawView(this);
        drawView.setBackgroundColor(Color.WHITE);
        setContentView(drawView);
        Log.d(TAG, "onCreate:3 "+System.currentTimeMillis());
    }

    private void majCarte(Integer[] depart,int puissance) {
        if (!planc.isEmpty()){
            //initialisation
            planc.get(depart[0])[depart[1]] = String.valueOf(puissance);
            List<Integer[]> points = new ArrayList<>();
            points.add(depart);
            int valeur = puissance;
            int intensite = 255;
            //anti-retour
            plancR = new ArrayList<>();
            for(String[] elem : plancO) {
                plancR.add(elem.clone());
            }
            plancR.addAll(plancO);
            while (!points.isEmpty()){
                valeur -= 2;
                intensite -= 1;
                List<Integer[]> nouvPoints = new ArrayList<>();
                if (valeur>1) { //faut pas casser les murs
                    //pour chaque point: existe?, est un mur?
                    for (Integer[] element : points) {
                        //en haut
                        if (element[1] > 0 && Integer.parseInt(planc.get(element[0])[element[1] - 1]) != 1 && Integer.parseInt(plancR.get(element[0])[element[1] - 1]) != 1) {
                            int val = Integer.parseInt(planc.get(element[0])[element[1] - 1]);
                            int intens = Integer.parseInt(plancO.get(element[0])[element[1] - 1]);
                            if(intens==0){
                                planc.get(element[0])[element[1] - 1] = String.valueOf(valeur);
                                plancO.get(element[0])[element[1] - 1] = String.valueOf(intensite);
                            }else{
                                planc.get(element[0])[element[1] - 1] = String.valueOf(((valeur*intensite)+(val*intens))/(intensite+intens));
                                plancO.get(element[0])[element[1] - 1] = String.valueOf((intensite+intens)/2);
                            }
                            plancR.get(element[0])[element[1] - 1] = "1";
                            nouvPoints.add(new Integer[]{element[0], element[1] - 1});
                        }
                        //bas
                        if (element[1] + 1 < planc.get(element[0]).length  && Integer.parseInt(planc.get(element[0])[element[1] + 1]) != 1 && Integer.parseInt(plancR.get(element[0])[element[1] + 1]) != 1) {
                            int val = Integer.parseInt(planc.get(element[0])[element[1] + 1]);
                            int intens = Integer.parseInt(plancO.get(element[0])[element[1] + 1]);
                            if(intens==0){
                                planc.get(element[0])[element[1] + 1] = String.valueOf(valeur);
                                plancO.get(element[0])[element[1] + 1] = String.valueOf(intensite);
                            }else {
                                planc.get(element[0])[element[1] + 1] = String.valueOf(((valeur * intensite) + (val * intens)) / (intensite + intens));
                                plancO.get(element[0])[element[1] + 1] = String.valueOf((intensite + intens) / 2);
                            }
                            plancR.get(element[0])[element[1] + 1] = "1";
                            nouvPoints.add(new Integer[]{element[0], element[1] + 1});
                        }
                        //gauche
                        if (element[0] > 0 && Integer.parseInt(planc.get(element[0] - 1)[element[1]]) != 1 && Integer.parseInt(plancR.get(element[0] - 1)[element[1]]) != 1) {
                            int val = Integer.parseInt(planc.get(element[0] - 1)[element[1]]);
                            int intens = Integer.parseInt(plancO.get(element[0] - 1)[element[1]]);
                            if(intens==0){
                                planc.get(element[0] - 1)[element[1]] = String.valueOf(valeur);
                                plancO.get(element[0] - 1)[element[1]] = String.valueOf(intensite);
                            }else {
                                planc.get(element[0] - 1)[element[1]] = String.valueOf(((valeur * intensite) + (val * intens)) / (intensite + intens));
                                plancO.get(element[0] - 1)[element[1]] = String.valueOf((intensite + intens) / 2);
                            }
                            plancR.get(element[0] - 1)[element[1]] = "1";
                            nouvPoints.add(new Integer[]{element[0] - 1, element[1]});
                        }
                        //doite
                        if (element[0] + 1 < planc.size()  && Integer.parseInt(planc.get(element[0] + 1)[element[1]]) != 1  && Integer.parseInt(plancR.get(element[0] + 1)[element[1]]) != 1) {
                            int val = Integer.parseInt(planc.get(element[0] + 1)[element[1]]);
                            int intens = Integer.parseInt(plancO.get(element[0] + 1)[element[1]]);
                            if(intens==0){
                                planc.get(element[0] + 1)[element[1]] = String.valueOf(valeur);
                                plancO.get(element[0] + 1)[element[1]] = String.valueOf(intensite);
                            }else {
                                planc.get(element[0] + 1)[element[1]] = String.valueOf(((valeur * intensite) + (val * intens)) / (intensite + intens));
                                plancO.get(element[0] + 1)[element[1]] = String.valueOf((intensite + intens) / 2);
                            }
                            plancR.get(element[0] + 1)[element[1]] = "1";
                            nouvPoints.add(new Integer[]{element[0] + 1, element[1]});
                        }
                        Log.d(TAG, "majCarte: "+valeur);
                    }
                }
                points = nouvPoints;
            }
            Log.d(TAG, "majCarte: "+ Arrays.toString(planc.get(depart[0])));
            Log.d(TAG, "majCarte: "+ Arrays.toString(plancO.get(depart[0])));

            /*MonThread cestpartit = new MonThread();
            cestpartit.start();*/
        }

    }

    public void lecteur() {
        BufferedReader reader = null;
        //fichier 1: couleur
        try {
            reader = new BufferedReader(
                    new InputStreamReader(getAssets().open("ressources/confidentiel-2.txt"), StandardCharsets.UTF_8));
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                String[] row = mLine.split("");
                planc.add(row);
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

        //fichier 2: opacité
        try {
            reader = new BufferedReader(
                    new InputStreamReader(getAssets().open("ressources/confidentiel-3.txt"), StandardCharsets.UTF_8));
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                String[] row = mLine.split("");
                plancO.add(row);
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

    public class MonThread extends Thread{
        private static final String TAG = "MonThread";
        @Override
        public void run(){
            super.run();
        }
    }

    public class DrawView extends View {
        Paint paint = new Paint();

        public DrawView(Context context) {
            super(context);
        }

        @Override
        public void onDraw(Canvas canvas) {
            int yColonne = 20;
            for (String[] element:planc){
                int xLigne = 20;
                for (int indix = 0; indix < element.length; indix++){
                    String bloc = element[indix];
                    if (Integer.parseInt(bloc)== 1){
                        paint.setColor(Color.BLACK);
                    }else if (Integer.parseInt(bloc)== 0){
                        paint.setColor(Color.WHITE);
                    }else if (Integer.parseInt(bloc)>255){
                        paint.setColor(Color.rgb(510-Integer.parseInt(bloc),255,0));
                    }else{
                        paint.setColor(Color.rgb(255,Integer.parseInt(bloc),0));
                    }
                    paint.setStrokeWidth(0);
                    if (Integer.parseInt(bloc)== 1){
                        paint.setAlpha(255);
                    }else{
                        paint.setAlpha(Integer.parseInt(plancO.get(planc.indexOf(element))[indix]));
                    }
                    canvas.drawRect(xLigne, yColonne, xLigne + 3F, yColonne + 3F, paint);
                    xLigne +=3;
                }
                yColonne+=3;
            }
        }

    }
}

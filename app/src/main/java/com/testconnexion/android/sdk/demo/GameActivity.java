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

import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameActivity extends Activity {
    private final List<String[]> planc = new ArrayList<>(); //plan conffidentiel du lycée (binaire/valeur dégradé)
    private List<String[]> plancD;  //plan confidentiel du lycée (distances)
    private List<String[]> plancB;  //plan confidentiel du lycée (échelle de puissance du dégradé)

    private ArrayList<List<Integer[]>> points = new ArrayList<>();  //localisation tests wifi
    private ArrayList<Integer[]> pointsB = new ArrayList<>();   //frontières du dégradé
    private DrawView drawView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //listes de valeurs au hasard
        Integer[] p1 = {20, 20};
        Integer[] p2 = {60, 30};
        Integer[] p3 = {20, 70};
        points = new ArrayList<>(Arrays.asList(List.of(p1, p2, p3)));
        Integer[] puissanceW = new Integer[]{510, 5, 255};

        lecteur();  //lit le fichier source (binaire)

        distance(puissanceW);

        remplissage1(puissanceW); //délimite les zones

        remplissage2(); //fait le dégradé

        drawView = new DrawView(this);
        drawView.setBackgroundColor(Color.WHITE);
        setContentView(drawView);

        ecriveur(planc);
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

    }

    public class DrawView extends View {
        Paint paint = new Paint();

        public DrawView(Context context) {
            super(context);
        }

        @Override
        public void onDraw(Canvas canvas) {
            int yColonne = 100;
            for (String[] element : planc) {
                int xLigne = 100;
                for (int indix = 0; indix < element.length / 2; indix++) {
                    String bloc = element[indix];
                    if (Integer.parseInt(bloc) == 1) {
                        paint.setColor(Color.BLACK);
                    } else if (Integer.parseInt(bloc) == 0) {
                        paint.setColor(Color.WHITE);
                    } else if (Integer.parseInt(bloc) > 255) {
                        paint.setColor(Color.rgb(510 - Integer.parseInt(bloc), 255, 0));
                    } else {
                        paint.setColor(Color.rgb(255, Integer.parseInt(bloc), 0));
                    }
                    paint.setStrokeWidth(0);

                    if (Integer.parseInt(bloc) == 1) {
                        paint.setAlpha(255);
                    } else {
                        paint.setAlpha((int) (255 - Integer.parseInt(plancD.get(planc.indexOf(element))[indix]) * 1.4));
                    }
                    canvas.drawRect(xLigne, yColonne, xLigne + 5F, yColonne + 5F, paint);
                    xLigne += 5;
                }
                yColonne += 5;
            }
            /*yColonne = 100;
            for (String[] element:plancC){
                int xLigne = 900;
                for (int indix = 0; indix < element.length/2; indix++){
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

                    canvas.drawRect(xLigne, yColonne, xLigne + 5F, yColonne + 5F, paint);
                    xLigne +=5;
                }
                yColonne+=5;
            }

            yColonne = 550;
            for (String[] element:plancD){
                int xLigne = 100;
                for (int indix = 0; indix < element.length/2; indix++){
                    String bloc = element[indix];
                    if (Integer.parseInt(bloc)== 1){
                        paint.setColor(Color.BLACK);
                    }else if (Integer.parseInt(bloc)== 0){
                        paint.setColor(Color.WHITE);
                    }else if (Integer.parseInt(bloc)*3>255){
                        paint.setColor(Color.rgb(510-Integer.parseInt(bloc)*3,255,0));
                    }else{
                        paint.setColor(Color.rgb(255,Integer.parseInt(bloc)*3,0));
                    }
                    paint.setStrokeWidth(0);

                    canvas.drawRect(xLigne, yColonne, xLigne + 5F, yColonne + 5F, paint);
                    xLigne +=5;
                }
                yColonne+=5;
            }
            yColonne = 550;
            for (String[] element:plancB){
                int xLigne = 900;
                for (int indix = 0; indix < element.length/2; indix++){
                    String bloc = element[indix];
                    if (Integer.parseInt(bloc)== 1){
                        paint.setColor(Color.BLACK);
                    }else if (Integer.parseInt(bloc)== 0){
                        paint.setColor(Color.WHITE);
                    }else if (Integer.parseInt(bloc)*4>255){
                        paint.setColor(Color.rgb(510-Integer.parseInt(bloc)*4,255,0));
                    }else{
                        paint.setColor(Color.rgb(255,Integer.parseInt(bloc)*4,0));
                    }
                    paint.setStrokeWidth(0);

                    canvas.drawRect(xLigne, yColonne, xLigne + 5F, yColonne + 5F, paint);
                    xLigne +=5;
                }
                yColonne+=5;
            }*/
        }

    }

    public void distance(Integer[] puissance) {
        List<Integer[]> pointActuel = points.get(0);
        //initialisation tests wifis
        for (Integer[] element : pointActuel) {
            planc.get(element[0])[element[1]] = String.valueOf(puissance[pointActuel.indexOf(element)]);
        }

        int dist = 1;
        plancD = new ArrayList<>();
        plancB = new ArrayList<>();
        for (String[] elem : planc) {
            plancD.add(elem.clone());
            plancB.add(elem.clone());
        }

        while (dist < 150) {
            List<Integer[]> nouvPoints = new ArrayList<>();
            for (Integer[] element : pointActuel) {
                //en haut
                if (element[1] > 0 && Integer.parseInt(planc.get(element[0])[element[1] - 1]) != 1 && (Integer.parseInt(plancD.get(element[0])[element[1] - 1]) > dist || Integer.parseInt(plancD.get(element[0])[element[1] - 1]) == 0)) {
                    plancD.get(element[0])[element[1] - 1] = String.valueOf(dist);
                    nouvPoints.add(new Integer[]{element[0], element[1] - 1});
                }
                //en bas
                if (element[1] + 1 < planc.get(element[0]).length && Integer.parseInt(planc.get(element[0])[element[1] + 1]) != 1 && (Integer.parseInt(plancD.get(element[0])[element[1] + 1]) > dist || Integer.parseInt(plancD.get(element[0])[element[1] + 1]) == 0)) {
                    plancD.get(element[0])[element[1] + 1] = String.valueOf(dist);
                    nouvPoints.add(new Integer[]{element[0], element[1] + 1});
                }
                //gauche
                if (element[0] > 0 && Integer.parseInt(planc.get(element[0] - 1)[element[1]]) != 1 && (Integer.parseInt(plancD.get(element[0] - 1)[element[1]]) > dist || Integer.parseInt(plancD.get(element[0] - 1)[element[1]]) == 0)) {
                    plancD.get(element[0] - 1)[element[1]] = String.valueOf(dist);
                    nouvPoints.add(new Integer[]{element[0] - 1, element[1]});
                }
                //doite
                if (element[0] + 1 < planc.size() && Integer.parseInt(planc.get(element[0] + 1)[element[1]]) != 1 && (Integer.parseInt(plancD.get(element[0] + 1)[element[1]]) > dist || Integer.parseInt(plancD.get(element[0] + 1)[element[1]]) == 0)) {
                    plancD.get(element[0] + 1)[element[1]] = String.valueOf(dist);
                    nouvPoints.add(new Integer[]{element[0] + 1, element[1]});
                }
            }
            dist += 1;
            pointActuel = nouvPoints;
            points.add(nouvPoints);
        }
    }

    public void remplissage1(Integer[] puissance) {
        for (List<Integer[]> cercle : points) {
            for (Integer[] element : cercle) {
                int somme = 0;
                int nbDiv = 0;
                //en haut
                if (element[1] > 0 && Integer.parseInt(planc.get(element[0])[element[1] - 1]) > 1 && Arrays.asList(puissance).contains(Integer.parseInt(planc.get(element[0])[element[1] - 1]))) {
                    somme += Integer.parseInt(planc.get(element[0])[element[1] - 1]);
                    nbDiv += 1;
                }
                //en bas
                if (element[1] + 1 < planc.get(element[0]).length && Integer.parseInt(planc.get(element[0])[element[1] + 1]) > 1 && Arrays.asList(puissance).contains(Integer.parseInt(planc.get(element[0])[element[1] + 1]))) {
                    somme += Integer.parseInt(planc.get(element[0])[element[1] + 1]);
                    nbDiv += 1;
                }
                //gauche
                if (element[0] > 0 && Integer.parseInt(planc.get(element[0] - 1)[element[1]]) > 1 && Arrays.asList(puissance).contains(Integer.parseInt(planc.get(element[0] - 1)[element[1]]))) {
                    somme += Integer.parseInt(planc.get(element[0] - 1)[element[1]]);
                    nbDiv += 1;
                }
                //doite
                if (element[0] + 1 < planc.size() && Integer.parseInt(planc.get(element[0] + 1)[element[1]]) > 1 && Arrays.asList(puissance).contains(Integer.parseInt(planc.get(element[0] + 1)[element[1]]))) {
                    somme += Integer.parseInt(planc.get(element[0] + 1)[element[1]]);
                    nbDiv += 1;
                }
                if (nbDiv != 0) {
                    planc.get(element[0])[element[1]] = String.valueOf(somme / nbDiv);
                    if (!Arrays.asList(puissance).contains(somme / nbDiv)) {
                        pointsB.add(element);
                    }
                }
            }
        }
        Log.d(TAG, "distance: " + "*********************************************************");
    }

    public void remplissage2() {
        Log.d(TAG, "remplissage2: " + pointsB);
        //initialisation
        List<String[]> plancR = new ArrayList<>();
        for (String[] elem : planc) {
            plancR.add(elem.clone());
        }

        for (Integer[] element : pointsB) {
            plancB.get(element[0])[element[1]] = plancD.get(element[0])[element[1]];
        }

        while (!pointsB.isEmpty()) {
            ArrayList<Integer[]> nouvPoints = new ArrayList<>();
            for (Integer[] element : pointsB) {
                Log.d(TAG, "remplissage2: " + element[0] + " " + element[1] + " " + pointsB.size());
                int ancVal = Integer.parseInt(planc.get(element[0])[element[1]]);
                int dist = Integer.parseInt(plancD.get(element[0])[element[1]]);
                int somme = 0;
                int nbDiv = 0;
                float somme2 = 0;
                //en haut
                if (element[1] > 0 && Integer.parseInt(planc.get(element[0])[element[1] - 1]) > 1) {
                    if (!plancB.get(element[0])[element[1] - 1].equals("0")) {
                        somme += Integer.parseInt(planc.get(element[0])[element[1] - 1]);
                        somme2 += Integer.parseInt(plancB.get(element[0])[element[1] - 1]);
                        nbDiv += 1;
                    }
                    if (!plancR.get(element[0])[element[1] - 1].equals("1")) {
                        nouvPoints.add(new Integer[]{element[0], element[1] - 1});
                        plancR.get(element[0])[element[1] - 1] = "1";
                    }

                }
                //en bas
                if (element[1] + 1 < planc.get(element[0]).length && Integer.parseInt(planc.get(element[0])[element[1] + 1]) > 1) {
                    if (!plancB.get(element[0])[element[1] + 1].equals("0")) {
                        somme += Integer.parseInt(planc.get(element[0])[element[1] + 1]);
                        somme2 += Integer.parseInt(plancB.get(element[0])[element[1] + 1]);
                        nbDiv += 1;
                    }
                    if (!plancR.get(element[0])[element[1] + 1].equals("1")) {
                        nouvPoints.add(new Integer[]{element[0], element[1] + 1});
                        plancR.get(element[0])[element[1] + 1] = "1";
                    }
                }
                //gauche
                if (element[0] > 0 && Integer.parseInt(planc.get(element[0] - 1)[element[1]]) > 1) {
                    if (!plancB.get(element[0] - 1)[element[1]].equals("0")) {
                        somme += Integer.parseInt(planc.get(element[0] - 1)[element[1]]);
                        somme2 += Integer.parseInt(plancB.get(element[0] - 1)[element[1]]);
                        nbDiv += 1;
                    }
                    if (!plancR.get(element[0] - 1)[element[1]].equals("1")) {
                        nouvPoints.add(new Integer[]{element[0] - 1, element[1]});
                        plancR.get(element[0] - 1)[element[1]] = "1";
                    }
                }
                //doite
                if (element[0] + 1 < planc.size() && Integer.parseInt(planc.get(element[0] + 1)[element[1]]) > 1) {
                    if (!plancB.get(element[0] + 1)[element[1]].equals("0")) {
                        somme += Integer.parseInt(planc.get(element[0] + 1)[element[1]]);
                        somme2 += Integer.parseInt(plancB.get(element[0] + 1)[element[1]]);
                        nbDiv += 1;
                    }
                    if (!plancR.get(element[0] + 1)[element[1]].equals("1")) {
                        nouvPoints.add(new Integer[]{element[0] + 1, element[1]});
                        plancR.get(element[0] + 1)[element[1]] = "1";
                    }
                }

                if (nbDiv != 0) {
                    int objectif = somme / nbDiv;
                    float pourcentage = 1 - dist / (somme2 / nbDiv);
                    plancB.get(element[0])[element[1]] = String.valueOf((int) (somme2 / nbDiv + 1));
                    planc.get(element[0])[element[1]] = String.valueOf((int) (objectif - ((objectif - ancVal) * pourcentage)));
                }
            }
            pointsB = nouvPoints;
        }
    }


    public void ecriveur(List<String[]> donnees) {
        String csv = (getExternalFilesDir(null) + "/confidentiel.txt");
        try (CSVWriter writer = new CSVWriter(new FileWriter(csv), ',', ICSVWriter.NO_QUOTE_CHARACTER,
                ICSVWriter.DEFAULT_ESCAPE_CHARACTER, ICSVWriter.RFC4180_LINE_END)) {

            writer.writeAll(donnees);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

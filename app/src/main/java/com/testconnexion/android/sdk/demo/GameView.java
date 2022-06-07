package com.testconnexion.android.sdk.demo;

import android.content.Context;
import android.graphics.Canvas;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

public class GameView extends View {
    private MainThread thread;

    public GameView(Context context) {
        super(context);
    }

    @Override
    public void onDraw(Canvas canvas){
    }
}

package com.example.myfirmwarewriting;

import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import hex2bin.*;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Hex2Bin mybin = null;
        mybin.go("fdsaf");

        //实例化TextView
        TextView textView = findViewById(R.id.textView2);
        AssetManager mgr=getAssets();
        Typeface tf=Typeface.createFromAsset(mgr, "CooperHewitt-Bold.ttf");
        textView.setTypeface(tf);
    }
}

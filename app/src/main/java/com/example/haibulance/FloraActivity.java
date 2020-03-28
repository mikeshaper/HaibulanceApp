package com.example.haibulance;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class FloraActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mammals;
    private Button reptiles;
    private Button birds;
    private Button other;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flora);

        mammals = findViewById(R.id.mammals_flo_butt);
        reptiles = findViewById(R.id.reptiles_flo_butt);
        birds = findViewById(R.id.birds_flo_butt);
        other = findViewById(R.id.other_flo_butt);

        mammals.setOnClickListener(this);
        reptiles.setOnClickListener(this);
        birds.setOnClickListener(this);
        other.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        if (v == mammals){
            Intent intent = new Intent(this, FloraImgsActivity.class);
            startActivity(intent);
        }
    }
}

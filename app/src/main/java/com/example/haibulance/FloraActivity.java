package com.example.haibulance;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class FloraActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mammals;
    private Button reptiles;
    private Button birds;

    private CurrentSession currentSession = new CurrentSession();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flora);

        mammals = findViewById(R.id.mammals_flo_butt);
        reptiles = findViewById(R.id.reptiles_flo_butt);
        birds = findViewById(R.id.birds_flo_butt);

        mammals.setOnClickListener(this);
        reptiles.setOnClickListener(this);
        birds.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        if (v == mammals || v == reptiles || v == birds){
            Map<Button, String> florasDict = new HashMap<>();
            florasDict.put(mammals, "http://www.tatzpit.com/Site/pages/inPage.asp?catID=532");
            florasDict.put(reptiles, "http://www.tatzpit.com/Site/pages/inPage.asp?catID=539");
            florasDict.put(birds, "http://www.tatzpit.com/Site/pages/inPage.asp?catID=9");
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setData(Uri.parse(florasDict.get(v)));
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case MENU_CODE:
                if (currentSession.isMenuActivityFinished()) {
                    finish();
                }
        }
    }

    private final int MENU_CODE = 1;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.items_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.home_button:
                //Intent intent = new Intent(this, MainActivity.class);
                //startActivity(intent);
                //CurrentSession currentSession = new CurrentSession();
                //currentSession.setMenuActivityFinished(true);
                finish();
                return true;
            case R.id.radius:
                Intent intent1 = new Intent(this, ChooseRadiusActivity.class);
                startActivityForResult(intent1, MENU_CODE);
                return true;
            case R.id.more:
                return true;
            case R.id.detailsItem:
                Intent intent2 = new Intent(this, UserDetailsActivity.class);
                startActivityForResult(intent2, MENU_CODE);
                return true;
            case R.id.edDetailsItem:
                //Intent intent3 = new Intent(this, EditDetailsActivity.class);
                //startActivityForResult(intent3, MENU_CODE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

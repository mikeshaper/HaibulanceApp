package com.example.haibulance;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class UserDetailsActivity extends AppCompatActivity implements View.OnClickListener {

    private CurrentSession currentSession;
    private User currentUser;

    private TextView name;
    private TextView email;
    private TextView password;
    private TextView numofreps;
    private TextView numofpicks;
    private TextView radius;
    private Button editBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        currentSession = new CurrentSession();
        currentUser = currentSession.getUser();

        name = findViewById(R.id.dtails_name);
        email = findViewById(R.id.dtails_email);
        password = findViewById(R.id.dtails_password);
        numofpicks = findViewById(R.id.dtails_numofpickups);
        numofreps = findViewById(R.id.dtails_numofreps);
        radius = findViewById(R.id.dtails_radius);
        editBtn = findViewById(R.id.dtails_edit_btn);

        name.setText(name.getText() + currentUser.getName());
        email.setText(email.getText() + currentUser.getEmail());
        password.setText(password.getText() + currentUser.getPassword());
        numofreps.setText(String.format("%s%d", numofreps.getText(), currentUser.getReports()));
        numofpicks.setText(String.format("%s%d", numofpicks.getText(), currentUser.getPickups()));
        radius.setText(String.format("%s%dm", radius.getText(), currentUser.getReportsRadius()));

        editBtn.setOnClickListener(this);
    }


    @Override
    public void onClick(View view) {
        if (view == editBtn) {
            Intent intent = new Intent(this, EditDetailsActivity.class);
            startActivity(intent);
            finish();
        }
    }


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
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
                return true;
            case R.id.radius:
                Intent intent1 = new Intent(this, ChooseRadiusActivity.class);
                startActivity(intent1);
                return true;
            case R.id.more:
                return true;
            case R.id.subitem1:
                //Intent intent2 = new Intent(this, UserDetailsActivity.class);
                //startActivity(intent2);
                return true;
            case R.id.subitem2:
                Intent intent3 = new Intent(this, EditDetailsActivity.class);
                startActivity(intent3);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


}

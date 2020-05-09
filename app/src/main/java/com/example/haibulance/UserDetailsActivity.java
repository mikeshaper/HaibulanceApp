package com.example.haibulance;

import android.annotation.SuppressLint;
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

    private final int MENU_CODE = 0;

    private CurrentSession currentSession;
    private User currentUser;

    private TextView name;
    private TextView email;
    private TextView password;
    private TextView numofreps;
    private TextView numofpicks;
    private TextView radius;
    private Button editBtn;


    /**
     * the first function to be entered when the app runs. includes variables setting.
     */
    @SuppressLint({"DefaultLocale", "SetTextI18n"})
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
        if (currentUser.getReportsRadius() == 0){
            radius.setText(String.format("%s%s", radius.getText(), "all"));
        }
        else radius.setText(String.format("%s%dm", radius.getText(), currentUser.getReportsRadius()));


        editBtn.setOnClickListener(this);
    }

    /**
     * this function is activated when one of the views that I set onclicklistener on is been clicked.
     * @param view the view that was clicked
     */
    @Override
    public void onClick(View view) {
        if (view == editBtn) {
            Intent intent = new Intent(this, EditDetailsActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /**
     * called when an intent that was started for activity result is finished.
     * @param requestCode the code entered when the intent was started
     * @param resultCode the result code of the intent
     * @param data the data returned by the intent (if there was any)
     */
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

    /**
     * activate the option menu at the top of the screen
     * @param menu the menu to activate
     * @return true (the menu was activated successfully)
     */
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
                currentSession.setMenuActivityFinished(true);
                finish();
                return true;
            case R.id.radius:
                Intent intent1 = new Intent(this, ChooseRadiusActivity.class);
                startActivityForResult(intent1, MENU_CODE);
                return true;
            case R.id.more:
                return true;
            case R.id.detailsItem:
                //Intent intent2 = new Intent(this, UserDetailsActivity.class);
                //startActivityForResult(intent2, MENU_CODE);
                return true;
            case R.id.edDetailsItem:
                Intent intent3 = new Intent(this, EditDetailsActivity.class);
                startActivityForResult(intent3, MENU_CODE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


}

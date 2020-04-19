package com.example.haibulance;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class EditDetailsActivity extends AppCompatActivity implements View.OnClickListener {

    private final int MENU_CODE = 1;

    private CurrentSession currentSession;
    private User currentUser;

    private TextView name;
    private TextView email;
    private TextView password;
    private Button updateBtn;
    private Button cancleBtn;
    private Button changeRadBtn;

    private String previousEmail;
    private FirebaseAuth firebaseAuth;

    private ProgressDialog progressDialog;

    /**
     * the first function to be entered when the app runs. includes variables setting.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_details);

        firebaseAuth = FirebaseAuth.getInstance();

        previousEmail = firebaseAuth.getCurrentUser().getEmail();
        currentSession = new CurrentSession();
        currentUser = currentSession.getUser();

        name = findViewById(R.id.eddtails_name);
        email = findViewById(R.id.eddtails_email);
        password = findViewById(R.id.eddtails_password);
        updateBtn = findViewById(R.id.eddtails_update_btn);
        cancleBtn = findViewById(R.id.eddtails_cancle_btn);
        changeRadBtn = findViewById(R.id.eddtails_radius_btn);
        progressDialog = new ProgressDialog(this);

        name.setText(currentUser.getName());
        email.setText(currentUser.getEmail());
        password.setText(currentUser.getPassword());

        updateBtn.setOnClickListener(this);
        cancleBtn.setOnClickListener(this);
        changeRadBtn.setOnClickListener(this);
    }

    /**
     * this function is activated when one of the views that I set onclicklistener on is been clicked.
     * @param view the view that was clicked
     */
    @Override
    public void onClick(View view) {
        if (view == updateBtn){
            updateDtails();
        }
        else if (view == cancleBtn){
            firebaseAuth.getCurrentUser().updateEmail(previousEmail);
            finish();
        }
        else if (view == changeRadBtn){
            Intent intent = new Intent(this, ChooseRadiusActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /**
     * updates the user's details on firebase authentication
     */
    public void updateDtails(){
        if (email.getText() == null) {
            Toast.makeText(EditDetailsActivity.this, "Please enter Email", Toast.LENGTH_LONG).show();
            return;
        }
        if (name.getText() == null){
            Toast.makeText(EditDetailsActivity.this, "Please enter name", Toast.LENGTH_LONG).show();
            return;
        }
        if (password.getText() == null) {
            Toast.makeText(EditDetailsActivity.this, "Please enter password", Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseUser user = firebaseAuth.getCurrentUser();
        user.updatePassword(password.getText().toString())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(EditDetailsActivity.this, "Password updated", Toast.LENGTH_LONG).show();
                        }
                    }
                });
        progressDialog.setMessage("Updating...");
        progressDialog.show();
        firebaseAuth.getCurrentUser().updateEmail("user@example.com")//String.valueOf(email.getText()))
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            firebaseAuth.getCurrentUser().updatePassword(String.valueOf(password.getText()))
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            progressDialog.dismiss();
                                            if (task.isSuccessful()) {
                                                updateOnDatabase();
                                            }
                                            else {
                                                Toast.makeText(EditDetailsActivity.this, "invalid password", Toast.LENGTH_LONG).show();
                                                return;
                                            }
                                        }
                                    });

                        }
                        else {
                            Toast.makeText(EditDetailsActivity.this, "invalid email", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                });
    }

    /**
     * updates the user's details on firebase database (after the authentication details were updated successfully)
     */
    public void updateOnDatabase(){
        FirebaseDatabase.getInstance().getReference("users").child(currentUser._getDatabaseKey()).child("name").setValue(String.valueOf(name.getText()));
        FirebaseDatabase.getInstance().getReference("users").child(currentUser._getDatabaseKey()).child("email").setValue(String.valueOf(email.getText()));
        FirebaseDatabase.getInstance().getReference("users").child(currentUser._getDatabaseKey()).child("password").setValue(String.valueOf(password.getText()));
        finish();
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
                //Intent intent = new Intent(this, MainActivity.class);
                //startActivity(intent);
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




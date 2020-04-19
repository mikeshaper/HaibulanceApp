package com.example.haibulance;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.FirebaseFirestore;


public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    private Button login_button;
    private EditText login_email;
    private EditText login_password;
    private TextView back_to_regi;
    private DatabaseReference mDatabase;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;

    private ProgressDialog progressDialog;

    /**
     * the first function to be entered when the app runs. includes variables setting.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        login_email = (EditText) findViewById(R.id.login_email_edtxt);
        login_password = (EditText) findViewById(R.id.login_password_edtxt);
        login_button = (Button) findViewById(R.id.login_button_reg);
        back_to_regi = (TextView) findViewById(R.id.back_to_reg_textView);

        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        progressDialog = new ProgressDialog(this);

        back_to_regi.setOnClickListener(this);
        login_button.setOnClickListener(this);
    }

    /**
     * this function is activated when one of the views that I set onclicklistener on is been clicked.
     * @param view the view that was clicked
     */
    @Override
    public void onClick(View view) {
        if (view == login_button) {
            Log.d("clicked", "login");
            if (TextUtils.isEmpty(login_password.getText().toString()) && TextUtils.isEmpty(login_email.getText().toString())) {
                if (firebaseAuth.getCurrentUser() != null)
                    createLoginAsDialog();
            }
            else loginUser();
        } else if (view == back_to_regi) {
            finish();
        }
    }

    private void loginUser() {
        String password = login_password.getText().toString();
        String email = login_email.getText().toString();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(LoginActivity.this, "please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(LoginActivity.this, "please enter your password", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Login...");
        progressDialog.show();

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            login();
                        } else {
                            Toast.makeText(LoginActivity.this, "incorrect email or password", Toast.LENGTH_SHORT).show();
                        }
                    }
                })

                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e)
                    {
                        // Error
                        progressDialog.dismiss();
                        Toast.makeText(LoginActivity.this,"Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void login() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * creates a dialog to ask the user if he wants to enter as the user found on this device by firebase authentication
     */
    public void createLoginAsDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(LoginActivity.this)
                .show();
        dialog.setContentView(R.layout.enter_as_dialog);
        TextView t = dialog.findViewById(R.id.login_as);
        t.setText(String.format("Login as: %s?", firebaseAuth.getCurrentUser().getEmail()));
        dialog.findViewById(R.id.left_dialog_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Toast.makeText(LoginActivity.this, String.format("hello %s!", firebaseAuth.getCurrentUser().getEmail()), Toast.LENGTH_SHORT).show();
                login();
            }
        });
        dialog.findViewById(R.id.right_dialog_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }
}
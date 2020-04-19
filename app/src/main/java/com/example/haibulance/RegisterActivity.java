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
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;


public class RegisterActivity extends FragmentActivity implements View.OnClickListener {

    private Button regi_button;
    private EditText regi_name;
    private EditText regi_email;
    private EditText regi_password;
    private TextView already_have;

    private FirebaseAuth firebaseAuth;

    private ProgressDialog progressDialog;

    private DatabaseReference mDatabase;
    private FirebaseFirestore db;

    private GoogleSignInAccount acct;
    private String name;
    private String email;
    private String password;
    private String id;


    /**
     * the first function to be entered when the app runs. includes variables setting.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        regi_name = findViewById(R.id.regi_name_edtxt);
        regi_email = findViewById(R.id.regi_email_edtxt);
        regi_password = findViewById(R.id.reri_password_edtxt);
        regi_button = findViewById(R.id.regi_button_reg);
        already_have = findViewById(R.id.already_have_textView);
        progressDialog = new ProgressDialog(this);
        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        acct = GoogleSignIn.getLastSignedInAccount(this);
        if (firebaseAuth.getCurrentUser() != null)
            createLoginAsDialog();
        already_have.setOnClickListener(this);
        regi_button.setOnClickListener(this);
    }

    /**
     * this function is activated when one of the views that I set onclicklistener on is been clicked.
     * @param view the view that was clicked
     */
    @Override
    public void onClick(View view) {
        if (view == regi_button){
            if (regi_name.getText() == null) createLoginAsDialog();
            else registerUser();
        }
        else if (view == already_have){
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
    }

    public void addUserToDatabase(){
        mDatabase = FirebaseDatabase.getInstance().getReference("users");
        User user = new User(name, email, password);
        mDatabase.child(id).setValue(user);
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Toast.makeText(RegisterActivity.this, "success creating a user database", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(RegisterActivity.this, "error creating a user database: " + error, Toast.LENGTH_SHORT).show();
            }
        });

    }

    /**
     * authentication with email&password to firebase authentication
     */
    public void AuthWithEP(){
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {//this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this, "Registered Successfully", Toast.LENGTH_SHORT).show();
                            id = firebaseAuth.getCurrentUser().getUid();
                            addUserToDatabase();
                            //createUserDatabase();
                            progressDialog.dismiss();
                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        else {
                            Toast.makeText(RegisterActivity.this, "Could not register, try to enter again your email", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    
    private void registerUser() {
        name = regi_name.getText().toString();
        email = regi_email.getText().toString();
        password = regi_password.getText().toString();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(RegisterActivity.this, "please enter a name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(RegisterActivity.this, "please enter an email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6){
            Toast.makeText(RegisterActivity.this, "your password's length must be al least 6 chars", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Registring...");
        progressDialog.show();

        AuthWithEP();
    }

    public void createUserDatabase(){
        User user = new User(name, email, password);
        db.collection("users")
                .add(user)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d("mydatabase", "DocumentSnapshot added with ID: " + documentReference.getId() + ", auth id:" + id);
                        Toast.makeText(RegisterActivity.this, "success creating a user database", Toast.LENGTH_SHORT).show();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("mydatabase", "Error adding document", e);
                        Toast.makeText(RegisterActivity.this, "error creating a user database", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * creates a dialog to ask the user if he wants to enter as the user found on this device by firebase authentication
     */
    public void createLoginAsDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(RegisterActivity.this)
                .show();
        dialog.setContentView(R.layout.enter_as_dialog);
        TextView t = dialog.findViewById(R.id.login_as);
        t.setText("Login as: " + firebaseAuth.getCurrentUser().getEmail() +"?");
        dialog.findViewById(R.id.left_dialog_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Toast.makeText(RegisterActivity.this, "hello " + firebaseAuth.getCurrentUser().getEmail() + "!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
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
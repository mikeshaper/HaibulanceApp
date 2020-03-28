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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
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


public class RegisterActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    private GoogleMap mMap;

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

    private ProgressBar progressBar;


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
        progressBar = findViewById(R.id.progress_circular);
        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        acct = GoogleSignIn.getLastSignedInAccount(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        if (firebaseAuth.getCurrentUser() != null)
            createLoginAsDialog();
        already_have.setOnClickListener(this);
        regi_button.setOnClickListener(this);
    }

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



    public void readFromRealtime(){
        // Read from the database
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String value = dataSnapshot.getValue(String.class);
                Log.d("mydatabase", "Value is: " + value);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w("mydatabase", "Failed to read value.", error.toException());
            }
        });
    }

    public void realtimeDatabase(){
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

    public void AuthWithEP(){
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {//this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this, "Registered Successfully", Toast.LENGTH_SHORT).show();
                            id = firebaseAuth.getCurrentUser().getUid();
                            realtimeDatabase();
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
        //Log.println(Log.INFO ,"RegisterActivity", "name s is: " + name);
        email = regi_email.getText().toString();
        //Log.println(Log.INFO ,"RegisterActivity", "email s is: " + email);
        password = regi_password.getText().toString();
        //Log.println(Log.INFO ,"RegisterActivity", "password s is: " + password);

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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Add a marker in Sydney, Australia, and move the camera.
        LatLng blich = new LatLng(32.056, 34.818);
        mMap.getMaxZoomLevel();
        mMap.addMarker(new MarkerOptions().position(blich).title("Marker in Blich"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(blich));

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                mMap.clear();

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f));
                Marker marker = mMap.addMarker(new MarkerOptions().position(latLng));

            }
        });
    }

    public void setLatitude(double lat){
        LatLng blich = new LatLng(lat, 34.818);
        mMap.getMaxZoomLevel();
        mMap.addMarker(new MarkerOptions().position(blich).title("Marker in Blich"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(blich));
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                mMap.clear();

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f));
                Marker marker = mMap.addMarker(new MarkerOptions().position(latLng));

            }
        });
    }
    public void setLongitude(double lat){
        LatLng blich = new LatLng(lat, 34.818);
        mMap.getMaxZoomLevel();
        mMap.addMarker(new MarkerOptions().position(blich).title("Marker in Blich"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(blich));
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                mMap.clear();

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f));
                Marker marker = mMap.addMarker(new MarkerOptions().position(latLng));

            }
        });
    }


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
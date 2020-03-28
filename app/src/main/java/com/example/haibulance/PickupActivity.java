package com.example.haibulance;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

import java.util.List;

public class PickupActivity extends AppCompatActivity implements View.OnClickListener, OnMapReadyCallback, PermissionsListener {

    private static final int NAVI_CODE = 1;
    private static final int CHOOSE_DEST_CODE = 2;


    private MapView mapView;
    private PermissionsManager permissionsManager;
    private MapboxMap map;
    private Button pickup;
    private Button pickupto;
    private TextView repSpecie;
    private TextView repTime;
    private TextView repDesc;
    private TextView locName;
    private ImageView image;
    private ProgressBar progressBar;
    private ProgressDialog progressDialog;
    private Bitmap bmp;


    // instance for firebase storage and StorageReference
    private FirebaseStorage storage;
    private StorageReference storageReference;

    private LatLng hospitalLoc = new LatLng(32.0452857, 34.82474); ////המיקום של שער הספארי

    private CurrentSession currentSession;
    private Report rep;
    private DatabaseReference mDatabase;
    private FirebaseDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_pickup);

        db = FirebaseDatabase.getInstance();
        pickupto = findViewById(R.id.pickupTo_butt);
        pickup = findViewById(R.id.pickup_butt);
        repDesc = findViewById(R.id.pickup_desc_txt);
        repSpecie = findViewById(R.id.pickup_specie_txt);
        repTime = findViewById(R.id.pickup_time_txt);
        locName = findViewById(R.id.pickup_locname);
        image = findViewById(R.id.img_pickup);
        progressBar = findViewById(R.id.progressBar_pickupImg);
        currentSession = new CurrentSession();
        rep = currentSession.getRep();
        pickupto.setOnClickListener(this);
        pickup.setOnClickListener(this);
        progressDialog = new ProgressDialog(this);

        // get the Firebase storage reference
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        setImgBitmap();

        mapView = findViewById(R.id.mini_map_pickup);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        image.setOnClickListener(this);

        setTexts();
    }


    @Override
    public void onClick(View view) {
        if (view == pickup){
            Log.d("clicked", "pickup");
            rep.setDestination(hospitalLoc);
            Intent intent = new Intent(this, NaviActivity.class);
            startActivityForResult(intent, NAVI_CODE);
        }
        else if (view == pickupto){
            Log.d("clicked", "report");
            chooseDest();
        }
        else if (view == image) {
            Log.d("clicked", "image");
            if (!rep.getImgKey().equals("default") && bmp != null) openIMGDialog();
        }
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        map = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                enableLocationComponent(style);
                Marker marker = mapboxMap.addMarker(new MarkerOptions()
                        .icon(IconFactory.getInstance(PickupActivity.this).fromResource(rep.iconColor()))
                        .position(rep.getLocation())
                        .title("מיקום החיה"));
            }
        });
    }

    public void openIMGDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .show();
        dialog.setContentView(R.layout.open_img_dialog);
        if (bmp != null) {
            ImageView im = dialog.findViewById(R.id.opened_img);
            im.setImageBitmap(Bitmap.createScaledBitmap(bmp, bmp.getWidth()*2, bmp.getHeight()*2, false));
        }
        dialog.findViewById(R.id.open_img_okBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    public void setTexts(){
        repTime.setText(repTime.getText() + rep.getTime());
        repSpecie.setText(repSpecie.getText() + rep.getSpecie());
        repDesc.setText(repDesc.getText() + rep.getDescription());
        locName.setText(rep.getLocationName());
    }

    public void chooseDest(){
        Intent intent = new Intent(this, ChooseDestActivity.class);
        startActivityForResult(intent, CHOOSE_DEST_CODE);
    }

    private void setImgBitmap(){
        if (!rep.getImgKey().equals("default")){
            StorageReference islandRef = storageReference.child("images/" + rep.getImgKey());
            progressBar.setVisibility(View.VISIBLE);
            islandRef.getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    // Use the bytes to display the image
                    progressBar.setVisibility(View.GONE);
                    bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    image.setImageBitmap(bmp);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    progressBar.setVisibility(View.GONE);
                    // Handle any errors
                }
            });
        }
        else
            progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CHOOSE_DEST_CODE:
                if (rep._getDestination() != null){
                    Intent intent = new Intent(this, NaviActivity.class);
                    startActivity(intent);
                    finish();
                }
            case NAVI_CODE:
                if (rep.getStatus() == "picked") finish();
        }
    }

//==============================================================================================
// ===============================================================================================

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
// Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

// Get an instance of the component
            LocationComponent locationComponent = map.getLocationComponent();

// Activate with options
            locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(this, loadedMapStyle).build());

// Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

// Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);

// Set the component's render mode
            locationComponent.setRenderMode(RenderMode.COMPASS);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, "give me your location", Toast.LENGTH_LONG).show();
    }
    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            map.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    enableLocationComponent(style);
                }
            });        } else {
            Toast.makeText(this, "user location permission not granted", Toast.LENGTH_LONG).show();
            finish();
        }
    }
    @Override
    @SuppressWarnings( {"MissingPermission"})
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }
    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }
    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
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
                //Intent intent = new Intent(this, MainActivity.class);
                //startActivity(intent);
                finish();
                return true;
            case R.id.radius:
                Intent intent1 = new Intent(this, UsersList.class);
                startActivity(intent1);
                return true;
            case R.id.more:
                return true;
            case R.id.subitem1:
                return true;
            case R.id.subitem2:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}

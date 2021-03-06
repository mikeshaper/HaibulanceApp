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
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

import java.util.List;

public class ShowStatReportActivity extends AppCompatActivity implements View.OnClickListener, OnMapReadyCallback, PermissionsListener {

    private final int MENU_CODE = 1;

    private MapView mapView;
    private PermissionsManager permissionsManager;
    private MapboxMap map;
    private TextView repSpecie;
    private TextView repTime;
    private TextView repDesc;
    private TextView ogLocName;
    private TextView newLocName;
    private TextView status;
    private ImageView image;
    private ProgressBar progressBar;
    private ProgressDialog progressDialog;
    private Bitmap bmp;
    private Button showNewLocBtn;


    // instance for firebase storage and StorageReference
    private FirebaseStorage storage;
    private StorageReference storageReference;

    private CurrentSession currentSession;
    private Report rep;
    private DatabaseReference mDatabase;
    private FirebaseDatabase db;

    /**
     * the first function to be entered when the app runs. includes variables setting.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_show_stat_report);

        db = FirebaseDatabase.getInstance();
        repDesc = findViewById(R.id.showRep_desc_txt);
        repSpecie = findViewById(R.id.showRep_specie_txt);
        repTime = findViewById(R.id.showRep_time_txt);
        ogLocName = findViewById(R.id.showRep_locname);
        newLocName = findViewById(R.id.showRep_newLoc_txt);
        status = findViewById(R.id.showRep_status_txt);
        image = findViewById(R.id.img_showRep);
        progressBar = findViewById(R.id.progressBar_ShowRepImg);
        showNewLocBtn = findViewById(R.id.showRep_showNewLocBtn);
        currentSession = new CurrentSession();
        rep = currentSession.getRep();
        progressDialog = new ProgressDialog(this);

        //if (rep.sameLoc(rep.getLocation(), rep.getOgLocation())) showNewLocBtn.setVisibility(View.GONE);
        if (!rep.getStatus().equals("picked")) {
            showNewLocBtn.setVisibility(View.GONE);
            Log.d("rep stat", String.valueOf(rep.getStatus()));
        }
        else showNewLocBtn.setOnClickListener(this);
        // get the Firebase storage reference
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        setImgBitmap();

        mapView = findViewById(R.id.mini_map_showRep);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        image.setOnClickListener(this);

        setTexts();
    }

    /**
     * this function is activated when one of the views that I set onclicklistener on is been clicked.
     * @param view the view that was clicked
     */
    @Override
    public void onClick(View view) {
        if (view == image) {
            Log.d("clicked", "image");
            if (!rep.getImgKey().equals("default") && bmp != null) openIMGDialog();
        }
        else if (view == showNewLocBtn){
            if (rep.sameLoc(rep.getLocation(), rep.getOgLocation())) {
                Toast.makeText(this, "the report was never picked up", Toast.LENGTH_LONG).show();
                showNewLocBtn.setVisibility(View.GONE);
            }
            else {
                Intent intent = new Intent(this, ShowRepRouteActivity.class);
                startActivity(intent);
            }
        }
    }

    /**
     * called automatically when the map (mapbox) is ready. includes style loading.
     * @param mapboxMap a reference to the map which the function was called on
     */
    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        map = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                enableLocationComponent(style);
                Marker marker = mapboxMap.addMarker(new MarkerOptions()
                        .icon(IconFactory.getInstance(ShowStatReportActivity.this).fromResource(rep.iconColor()))
                        .position(rep.getOgLocation())
                        .title("מיקום החיה"));
            }
        });
    }

    /**
     * opens the report's img for a larger view
     */
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

    /**
     * sets the report details on the textviews
     */
    public void setTexts(){
        repTime.setText(repTime.getText() + rep.getTime());
        repSpecie.setText(repSpecie.getText() + rep.getSpecie());
        repDesc.setText(repDesc.getText() + rep.getDescription());
        status.setText(status.getText() + rep.getStatus());
        ogLocName.setText(rep.getOgLocationName());
        newLocName.setText(newLocName.getText() + rep.getLocationName());
        if (rep.sameLoc(rep.getLocation(), rep.getOgLocation())) newLocName.setText("");
    }

    /**
     * sets the report img from firebase storage
     */
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


//==============================================================================================
// ===============================================================================================

    /**
     * map methods
     */
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
                if (currentSession.isMenuActivityFinished()) finish();
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
                Intent intent2 = new Intent(this, UserDetailsActivity.class);
                startActivityForResult(intent2, MENU_CODE);
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

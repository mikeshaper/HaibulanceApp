package com.example.haibulance;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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

public class ShowStatisticsOnMapActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener {

    private final int SHOW_REP_CODE = 0;
    private final int MENU_CODE = 1;

    private MapView mapView;
    private PermissionsManager permissionsManager;
    private MapboxMap map;
    private String requestedMonth;
    private String requestedYear;
    private CurrentSession currentSession;


    /**
     * the first function to be entered when the app runs. includes variables setting.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, String.valueOf(R.string.access_token));
        setContentView(R.layout.activity_show_statistics_on_map);

        currentSession = new CurrentSession();

        mapView = findViewById(R.id.statistics_map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync( ShowStatisticsOnMapActivity.this);
    }

    /**
     * called automatically when the map (mapbox) is ready. includes style loading.
     * @param mapboxMap a reference to the map which the function was called on
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        map = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style. OnStyleLoaded() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                enableLocationComponent(style);
                requestedMonth = currentSession.getRequestedMonth();
                requestedYear = currentSession.getRequestedYear();
                addMarkers();
                setOnMarkerClick();
            }
        });
    }

    /**
     * this func adds the reports to the main map as markers colored by the report status
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void addMarkers(){
        map.clear();
        //if (myLocation == null) return;
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("reports").child(requestedYear);

        ValueEventListener reportListener = new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Report object and use the values to update the UI
                for (DataSnapshot ds: dataSnapshot.getChildren()) {
                    Report rep = ds.getValue(Report.class);
                    if (!rep._isNullLoc())
                    {
                        int icon = R.drawable.red_marker;
                        if (rep.getStatus().equals("caseClosed")) icon = R.drawable.green_marker;
                        else if (!rep.sameLoc(rep.getOgLocation(), rep.getLocation())) icon = R.drawable.yellow_marker;
                        LatLng latLng = rep.getOgLocation();
                        map.addMarker(new MarkerOptions()
                                .icon(IconFactory.getInstance(ShowStatisticsOnMapActivity.this).fromResource(icon))
                                .position(latLng)
                                .title(rep.ToString()));
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Report failed
            }
        };
        mDatabase.child(requestedMonth).addValueEventListener(reportListener);
    }

    /**
     * sets onMarkerClickListener for every marker on the map
     */
    public void setOnMarkerClick(){
        map.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                //int id = (int)marker.getId();
                DatabaseReference mDatabase;
                LatLng latLng = marker.getPosition();
                mDatabase = FirebaseDatabase.getInstance().getReference(String.format("reports/%s/%s", requestedYear, requestedMonth));
                ValueEventListener findReportListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            Report rep = ds.getValue(Report.class);
                            if (rep.sameLoc(rep.getOgLocation(), latLng)) {
                                rep.setDatabaseKey(ds.getKey());
                                currentSession.setRep(rep);
                                Intent intent = new Intent(ShowStatisticsOnMapActivity.this, ShowStatReportActivity.class);
                                startActivityForResult(intent, SHOW_REP_CODE);
                                return;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Getting Report failed
                    }
                };
                mDatabase.addValueEventListener(findReportListener);
                return true;
            }
        });
    }

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
            case SHOW_REP_CODE:
                setOnMarkerClick();
            case MENU_CODE:
                if (currentSession.isMenuActivityFinished()) {
                    currentSession.setMenuActivityFinished(false);
                    currentSession.setOnRepActivity(false);
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
                currentSession.setOnRepActivity(false);
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

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
